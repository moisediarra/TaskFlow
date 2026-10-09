package com.xdsdata.taskflow.auth.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.xdsdata.taskflow.common.AppProperties;
import com.xdsdata.taskflow.common.Emails;
import com.xdsdata.taskflow.common.error.BadRequestException;
import com.xdsdata.taskflow.common.web.RateLimiter;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PasswordResetService {

	private static final Duration WINDOW = Duration.ofMinutes(15);

	private final PasswordResetTokenRepository resetTokens;

	private final RefreshTokenService refreshTokens;

	private final UserService users;

	private final PasswordEncoder passwordEncoder;

	private final ResetLinkSender sender;

	private final RateLimiter rateLimiter;

	private final Clock clock;

	private final AppProperties properties;

	PasswordResetService(PasswordResetTokenRepository resetTokens, RefreshTokenService refreshTokens, UserService users,
			PasswordEncoder passwordEncoder, ResetLinkSender sender, RateLimiter rateLimiter, Clock clock,
			AppProperties properties) {
		this.resetTokens = resetTokens;
		this.refreshTokens = refreshTokens;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.sender = sender;
		this.rateLimiter = rateLimiter;
		this.clock = clock;
		this.properties = properties;
	}

	/**
	 * Always succeeds from the caller's point of view, so the response never reveals whether an account
	 * exists for the email.
	 */
	@Transactional
	public void requestReset(String email, String clientIp) {
		rateLimiter.check("reset-request:ip:" + clientIp, 20, WINDOW);
		if (!rateLimiter.tryAcquire("reset-request:email:" + Emails.normalize(email), 3, WINDOW)) {
			return;
		}
		users.findActiveByEmail(email).ifPresent(this::issueToken);
	}

	@Transactional
	public void resetPassword(String rawToken, String newPassword, String confirmation, String clientIp) {
		rateLimiter.check("reset-confirm:ip:" + clientIp, 20, WINDOW);
		PasswordPolicy.validate(newPassword, confirmation, "password", "confirmPassword");
		Instant now = clock.instant();
		PasswordResetToken token = resetTokens.findForUpdate(Tokens.hash(rawToken))
			.filter(candidate -> candidate.isUsable(now))
			.orElseThrow(() -> new BadRequestException("INVALID_RESET_TOKEN",
					"This reset link is invalid or has expired. Please request a new one."));
		token.markUsed(now);
		User user = token.getUser();
		users.changePasswordHash(user.getId(), passwordEncoder.encode(newPassword));
		refreshTokens.revokeAll(user.getId());
	}

	/** Makes every reset link already sent to the user unusable. */
	@Transactional
	public void invalidateOutstanding(UUID userId) {
		resetTokens.invalidateOutstanding(userId, clock.instant());
	}

	@Scheduled(cron = "0 40 3 * * *")
	@Transactional
	public void purgeExpired() {
		resetTokens.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(1)));
	}

	private void issueToken(User user) {
		Instant now = clock.instant();
		Duration validity = properties.passwordReset().tokenTtl();
		resetTokens.invalidateOutstanding(user.getId(), now);
		String raw = Tokens.newToken();
		resetTokens.save(new PasswordResetToken(user, Tokens.hash(raw), now.plus(validity), now));
		String link = UriComponentsBuilder.fromUriString(properties.frontendUrl())
			.path("/reset-password")
			.queryParam("token", raw)
			.build()
			.toUriString();
		sender.send(user.getName(), user.getEmail(), link, validity);
	}

}
