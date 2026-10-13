package com.xdsdata.taskflow.auth.internal;

import java.time.Clock;
import java.util.UUID;

import com.xdsdata.taskflow.common.AppProperties;
import com.xdsdata.taskflow.common.error.BadRequestException;
import com.xdsdata.taskflow.common.error.TooManyRequestsException;
import com.xdsdata.taskflow.common.web.RateLimiter;
import com.xdsdata.taskflow.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A stolen session can't be used to guess the password behind the "confirm with your password" checks. */
class PasswordConfirmationsTests {

	private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

	private final User user = mock(User.class);

	private PasswordConfirmations confirmations;

	@BeforeEach
	void setUp() {
		AppProperties properties = mock(AppProperties.class);
		when(properties.rateLimitEnabled()).thenReturn(true);
		confirmations = new PasswordConfirmations(passwordEncoder, new RateLimiter(Clock.systemUTC(), properties));
		when(user.getId()).thenReturn(UUID.randomUUID());
		when(user.getPasswordHash()).thenReturn("hash");
		when(passwordEncoder.matches("right", "hash")).thenReturn(true);
	}

	@Test
	void correctPasswordsAreNeverCounted() {
		for (int i = 0; i < PasswordConfirmations.MAX_FAILURES * 2; i++) {
			assertThatCode(() -> confirmations.verify(user, "right", "Wrong.")).doesNotThrowAnyException();
		}
	}

	@Test
	void afterTooManyWrongEntriesEvenTheRightPasswordIsRefused() {
		for (int i = 0; i < PasswordConfirmations.MAX_FAILURES; i++) {
			assertThatThrownBy(() -> confirmations.verify(user, "guess", "Wrong."))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Wrong.");
		}
		assertThatThrownBy(() -> confirmations.verify(user, "right", "Wrong."))
			.isInstanceOf(TooManyRequestsException.class);
	}

}
