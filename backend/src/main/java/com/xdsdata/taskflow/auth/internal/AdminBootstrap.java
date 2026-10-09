package com.xdsdata.taskflow.auth.internal;

import com.xdsdata.taskflow.common.AppProperties;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Self-registration never grants IT Manager rights, so the first IT Manager is created from
 * APP_BOOTSTRAP_ADMIN_EMAIL / APP_BOOTSTRAP_ADMIN_PASSWORD while no active IT Manager exists.
 */
@Component
@Order(1)
class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final UserService users;

	private final PasswordEncoder passwordEncoder;

	private final AppProperties properties;

	AdminBootstrap(UserService users, PasswordEncoder passwordEncoder, AppProperties properties) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		AppProperties.Bootstrap bootstrap = properties.bootstrap();
		String email = bootstrap == null ? null : bootstrap.adminEmail();
		String password = bootstrap == null ? null : bootstrap.adminPassword();
		if (email == null || email.isBlank() || password == null || password.isBlank()) {
			if (!users.anyItManagerExists()) {
				log.warn("No IT Manager account exists. Set APP_BOOTSTRAP_ADMIN_EMAIL and APP_BOOTSTRAP_ADMIN_PASSWORD to create one.");
			}
			return;
		}
		// Only the first IT Manager comes from the environment: once one exists, accounts are managed in the app,
		// and a bootstrap account an IT Manager renamed or deleted must not come back at the next start.
		if (users.anyItManagerExists() || users.findByEmail(email).isPresent()) {
			return;
		}
		if (password.length() < PasswordPolicy.MIN_LENGTH) {
			throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD must be at least 8 characters.");
		}
		String name = bootstrap.adminName() == null || bootstrap.adminName().isBlank() ? "IT Manager" : bootstrap.adminName();
		users.create(name, email, passwordEncoder.encode(password), Role.IT_MANAGER, "IT Manager");
		log.info("Created the IT Manager account {}", email);
	}

}
