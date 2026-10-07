package com.xdsdata.taskflow.auth.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.xdsdata.taskflow.common.AppProperties;
import com.xdsdata.taskflow.common.error.UnauthorizedException;
import com.xdsdata.taskflow.users.User;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Long-lived refresh tokens, rotated on every use. Presenting an already-rotated token is treated as theft
 * and ends every session of that user, except within a short grace period that covers several browser
 * tabs refreshing at the same moment (they share the cookie, so the browser already holds the new token).
 */
@Service
public class RefreshTokenService {

	static final Duration REUSE_GRACE = Duration.ofSeconds(30);

	private final RefreshTokenRepository tokens;

	private final Clock clock;

	private final Duration ttl;

	RefreshTokenService(RefreshTokenRepository tokens, Clock clock, AppProperties properties) {
		this.tokens = tokens;
		this.clock = clock;
		this.ttl = properties.jwt().refreshTokenTtl();
	}

	@Transactional
	public IssuedRefreshToken issue(User user) {
		String raw = Tokens.newToken();
		Instant now = clock.instant();
		RefreshToken saved = tokens.save(new RefreshToken(user, Tokens.hash(raw), now.plus(ttl), now));
		return new IssuedRefreshToken(saved.getId(), raw, ttl);
	}

	@Transactional(noRollbackFor = UnauthorizedException.class)
	public Rotation rotate(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw sessionExpired();
		}
		RefreshToken token = tokens.findForUpdate(Tokens.hash(rawToken)).orElseThrow(this::sessionExpired);
		Instant now = clock.instant();
		User user = token.getUser();
		if (!user.isActive()) {
			throw sessionExpired();
		}
		if (token.isRevoked()) {
			if (token.getReplacedBy() != null && token.getRevokedAt().isAfter(now.minus(REUSE_GRACE))) {
				return new Rotation(user, null);
			}
			tokens.revokeAllForUser(user.getId(), now);
			throw sessionExpired();
		}
		if (token.getExpiresAt().isBefore(now)) {
			throw sessionExpired();
		}
		IssuedRefreshToken replacement = issue(user);
		token.revoke(now, replacement.id());
		return new Rotation(user, replacement);
	}

	@Transactional
	public void revoke(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return;
		}
		tokens.findByTokenHash(Tokens.hash(rawToken))
			.filter(token -> !token.isRevoked())
			.ifPresent(token -> token.revoke(clock.instant(), null));
	}

	@Transactional
	public void revokeAll(UUID userId) {
		tokens.revokeAllForUser(userId, clock.instant());
	}

	@Scheduled(cron = "0 30 3 * * *")
	@Transactional
	public void purgeExpired() {
		tokens.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(1)));
	}

	private UnauthorizedException sessionExpired() {
		return new UnauthorizedException("SESSION_EXPIRED", "Your session has expired. Please sign in again.");
	}

	public record IssuedRefreshToken(UUID id, String value, Duration maxAge) {
	}

	/** {@code replacement} is null when the token was just rotated by a concurrent request. */
	public record Rotation(User user, IssuedRefreshToken replacement) {
	}

}
