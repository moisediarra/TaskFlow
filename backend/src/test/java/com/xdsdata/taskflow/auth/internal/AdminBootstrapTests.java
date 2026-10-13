package com.xdsdata.taskflow.auth.internal;

import java.util.Optional;

import com.xdsdata.taskflow.common.AppProperties;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The environment only creates the first IT Manager; afterwards accounts are managed in the app. */
@ExtendWith(MockitoExtension.class)
class AdminBootstrapTests {

	private static final String EMAIL = "first.admin@taskflow.local";

	@Mock
	private UserService users;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AppProperties properties;

	@BeforeEach
	void setUp() {
		lenient().when(passwordEncoder.encode(anyString())).thenReturn("hash");
	}

	@Test
	void createsTheFirstItManagerOnAFreshDatabase() {
		configure("Long-enough-1");
		when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());
		when(users.anyItManagerExists()).thenReturn(false);

		bootstrap().run(null);

		verify(users).create("IT Manager", EMAIL, "hash", Role.IT_MANAGER, "IT Manager");
	}

	@Test
	void doesNotRecreateABootstrapAccountThatWasRenamedOrDeleted() {
		configure("Long-enough-1");
		when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());
		when(users.anyItManagerExists()).thenReturn(true);

		bootstrap().run(null);

		verify(users, never()).create(any(), any(), any(), any(), any());
	}

	@Test
	void leavesAnExistingAccountAlone() {
		configure("Long-enough-1");
		when(users.findByEmail(EMAIL)).thenReturn(Optional.of(mock(User.class)));

		bootstrap().run(null);

		verify(users, never()).create(any(), any(), any(), any(), any());
	}

	@Test
	void refusesAShortPassword() {
		configure("short");
		when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());
		when(users.anyItManagerExists()).thenReturn(false);

		assertThatThrownBy(() -> bootstrap().run(null)).isInstanceOf(IllegalStateException.class);
		verify(users, never()).create(any(), eq(EMAIL), any(), any(), any());
	}

	private void configure(String password) {
		when(properties.bootstrap()).thenReturn(new AppProperties.Bootstrap(EMAIL, password, null));
	}

	private AdminBootstrap bootstrap() {
		return new AdminBootstrap(users, passwordEncoder, properties);
	}

}
