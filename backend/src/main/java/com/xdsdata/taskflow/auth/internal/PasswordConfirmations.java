package com.xdsdata.taskflow.auth.internal;

import java.time.Duration;

import com.xdsdata.taskflow.common.error.BadRequestException;
import com.xdsdata.taskflow.common.error.TooManyRequestsException;
import com.xdsdata.taskflow.common.web.RateLimiter;
import com.xdsdata.taskflow.users.User;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Checks the password a signed-in user re-enters to confirm a sensitive change (an IT Manager's account
 * administration, their own password change). Wrong entries are counted per user, so a stolen session can't
 * be used to guess the password: after {@value #MAX_FAILURES} failures in 15 minutes every attempt is refused,
 * even a correct one, until the window ends.
 */
@Component
public class PasswordConfirmations {

	static final int MAX_FAILURES = 10;

	private static final Duration WINDOW = Duration.ofMinutes(15);

	private final PasswordEncoder passwordEncoder;

	private final RateLimiter rateLimiter;

	PasswordConfirmations(PasswordEncoder passwordEncoder, RateLimiter rateLimiter) {
		this.passwordEncoder = passwordEncoder;
		this.rateLimiter = rateLimiter;
	}

	/** @throws BadRequestException with a {@code currentPassword} field error carrying {@code message} */
	public void verify(User user, String rawPassword, String message) {
		String key = "confirm-password:" + user.getId();
		// Counted before the (slow) check, so parallel guesses can't get past the limit; a right password is
		// given back, so only wrong ones add up.
		if (!rateLimiter.tryAcquire(key, MAX_FAILURES, WINDOW)) {
			throw new TooManyRequestsException();
		}
		if (rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
			throw BadRequestException.field("currentPassword", message);
		}
		rateLimiter.refund(key);
	}

}
