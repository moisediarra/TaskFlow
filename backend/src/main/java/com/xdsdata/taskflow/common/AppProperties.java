package com.xdsdata.taskflow.common;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application settings bound from the {@code app.*} properties (see application.yml).
 */
@ConfigurationProperties("app")
public record AppProperties(
		String frontendUrl,
		List<String> allowedOrigins,
		String timezone,
		Role defaultSignupRole,
		boolean cookieSecure,
		boolean rateLimitEnabled,
		Jwt jwt,
		PasswordReset passwordReset,
		Bootstrap bootstrap,
		Seed seed,
		Workload workload,
		Notifications notifications) {

	public AppProperties {
		allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
		if (defaultSignupRole == Role.IT_MANAGER) {
			throw new IllegalStateException("APP_DEFAULT_SIGNUP_ROLE cannot be IT_MANAGER");
		}
	}

	public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
	}

	public record PasswordReset(Duration tokenTtl, boolean logLinks, String mailFrom) {
	}

	public record Bootstrap(String adminEmail, String adminPassword, String adminName) {
	}

	public record Seed(boolean demo, String demoPassword) {
	}

	public record Workload(int activeTaskWarning, int overdueTaskWarning) {
	}

	public record Notifications(Duration deadlineSweepInterval, boolean sweepEnabled) {
	}

}
