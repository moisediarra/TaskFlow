package com.xdsdata.taskflow.auth;

import java.util.UUID;

import com.xdsdata.taskflow.auth.internal.RefreshTokenService;
import com.xdsdata.taskflow.common.error.BadRequestException;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public API of the auth module for privileged operations elsewhere: re-checking a password before a
 * sensitive action ("explicit authorization", claude.md §31) and ending a user's sessions.
 */
@Service
public class AccountSecurity {

	private final UserService users;

	private final PasswordEncoder passwordEncoder;

	private final RefreshTokenService refreshTokens;

	AccountSecurity(UserService users, PasswordEncoder passwordEncoder, RefreshTokenService refreshTokens) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokens = refreshTokens;
	}

	/** @throws BadRequestException with a {@code currentPassword} field error when the password is wrong */
	public void verifyPassword(UUID userId, String rawPassword) {
		User user = users.getById(userId);
		if (rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
			throw BadRequestException.field("currentPassword", "Your password is incorrect.");
		}
	}

	@Transactional
	public void revokeAllSessions(UUID userId) {
		refreshTokens.revokeAll(userId);
	}

}
