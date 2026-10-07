package com.xdsdata.taskflow.auth.internal;

import java.time.Duration;
import java.util.Optional;

import com.xdsdata.taskflow.auth.internal.AccessTokenService.AccessToken;
import com.xdsdata.taskflow.auth.internal.RefreshTokenService.IssuedRefreshToken;
import com.xdsdata.taskflow.auth.internal.RefreshTokenService.Rotation;
import com.xdsdata.taskflow.common.AppProperties;
import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.Emails;
import com.xdsdata.taskflow.common.error.BadRequestException;
import com.xdsdata.taskflow.common.error.UnauthorizedException;
import com.xdsdata.taskflow.common.web.RateLimiter;
import com.xdsdata.taskflow.users.ProfileDto;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final Duration WINDOW = Duration.ofMinutes(15);

	private final UserService users;

	private final PasswordEncoder passwordEncoder;

	private final AccessTokenService accessTokens;

	private final RefreshTokenService refreshTokens;

	private final RateLimiter rateLimiter;

	private final AppProperties properties;

	/** Compared against when the email is unknown, so both cases take the same time. */
	private final String timingEqualizerHash;

	AuthService(UserService users, PasswordEncoder passwordEncoder, AccessTokenService accessTokens,
			RefreshTokenService refreshTokens, RateLimiter rateLimiter, AppProperties properties) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.accessTokens = accessTokens;
		this.refreshTokens = refreshTokens;
		this.rateLimiter = rateLimiter;
		this.properties = properties;
		this.timingEqualizerHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
	}

	@Transactional
	public ProfileDto register(String name, String email, String password, String confirmation) {
		PasswordPolicy.validate(password, confirmation, "password", "confirmPassword");
		User user = users.create(name, email, passwordEncoder.encode(password), properties.defaultSignupRole(), null);
		return ProfileDto.from(user);
	}

	@Transactional
	public Session login(String email, String password, String clientIp) {
		rateLimiter.check("login:ip:" + clientIp, 50, WINDOW);
		rateLimiter.check("login:email:" + Emails.normalize(email), 10, WINDOW);
		Optional<User> user = users.findByEmail(email);
		boolean passwordMatches = passwordEncoder.matches(password,
				user.map(User::getPasswordHash).orElse(timingEqualizerHash));
		if (user.isEmpty() || !passwordMatches) {
			throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid email or password.");
		}
		if (!user.get().isActive()) {
			throw new UnauthorizedException("ACCOUNT_DEACTIVATED",
					"Your account has been deactivated. Please contact your IT Manager.");
		}
		return startSession(user.get());
	}

	@Transactional(noRollbackFor = UnauthorizedException.class)
	public Session refresh(String rawRefreshToken) {
		Rotation rotation = refreshTokens.rotate(rawRefreshToken);
		AccessToken accessToken = accessTokens.issue(rotation.user());
		return new Session(accessToken.value(), accessToken.expiresInSeconds(), ProfileDto.from(rotation.user()),
				rotation.replacement());
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		refreshTokens.revoke(rawRefreshToken);
	}

	/** Changing the password signs out every other session and starts a fresh one for this browser. */
	@Transactional
	public Session changePassword(AuthUser actor, String currentPassword, String newPassword, String confirmation) {
		User user = users.getById(actor.id());
		if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
			throw BadRequestException.field("currentPassword", "Your current password is incorrect.");
		}
		PasswordPolicy.validate(newPassword, confirmation, "newPassword", "confirmPassword");
		users.changePasswordHash(user.getId(), passwordEncoder.encode(newPassword));
		refreshTokens.revokeAll(user.getId());
		return startSession(user);
	}

	private Session startSession(User user) {
		AccessToken accessToken = accessTokens.issue(user);
		IssuedRefreshToken refreshToken = refreshTokens.issue(user);
		return new Session(accessToken.value(), accessToken.expiresInSeconds(), ProfileDto.from(user), refreshToken);
	}

	/** {@code refreshToken} is null when the browser already holds a valid one. */
	public record Session(String accessToken, long expiresIn, ProfileDto user, IssuedRefreshToken refreshToken) {
	}

}
