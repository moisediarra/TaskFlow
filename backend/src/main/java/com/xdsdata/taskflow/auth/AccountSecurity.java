package com.xdsdata.taskflow.auth;

import java.util.UUID;

import com.xdsdata.taskflow.auth.internal.PasswordConfirmations;
import com.xdsdata.taskflow.auth.internal.PasswordPolicy;
import com.xdsdata.taskflow.auth.internal.PasswordResetService;
import com.xdsdata.taskflow.auth.internal.RefreshTokenService;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.common.error.BadRequestException;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public API of the auth module for privileged operations elsewhere: re-checking a password before a
 * sensitive action ("explicit authorization", claude.md §31), ending a user's sessions, and the passwords an
 * IT Manager sets when creating an account or resetting one. Those follow the same rules as self-chosen ones.
 */
@Service
public class AccountSecurity {

	private final UserService users;

	private final PasswordEncoder passwordEncoder;

	private final RefreshTokenService refreshTokens;

	private final PasswordResetService passwordResets;

	private final PasswordConfirmations passwordConfirmations;

	AccountSecurity(UserService users, PasswordEncoder passwordEncoder, RefreshTokenService refreshTokens,
			PasswordResetService passwordResets, PasswordConfirmations passwordConfirmations) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokens = refreshTokens;
		this.passwordResets = passwordResets;
		this.passwordConfirmations = passwordConfirmations;
	}

	/**
	 * @throws BadRequestException with a {@code currentPassword} field error when the password is wrong
	 * @throws com.xdsdata.taskflow.common.error.TooManyRequestsException after repeated wrong entries
	 */
	public void verifyPassword(UUID userId, String rawPassword) {
		passwordConfirmations.verify(users.getById(userId), rawPassword, "Your password is incorrect.");
	}

	/** Signs the user out everywhere: access tokens, refresh tokens and live connections end. */
	@Transactional
	public void revokeAllSessions(UUID userId) {
		refreshTokens.revokeAll(userId);
	}

	/** Reset links already sent stop working, e.g. after the sign-in email was corrected. */
	@Transactional
	public void invalidateResetLinks(UUID userId) {
		passwordResets.invalidateOutstanding(userId);
	}

	/**
	 * Creates an account with a starting password chosen by the IT Manager.
	 * @throws BadRequestException with {@code password} / {@code confirmPassword} field errors
	 */
	@Transactional
	public User createAccount(String name, String email, String password, String confirmation, Role role,
			String jobTitle) {
		PasswordPolicy.validate(password, confirmation, "password", "confirmPassword");
		return users.create(name, email, passwordEncoder.encode(password), role, jobTitle);
	}

	/**
	 * Replaces a user's password; reset links already sent stop working and the user is signed out everywhere.
	 * @throws BadRequestException with {@code newPassword} / {@code confirmPassword} field errors
	 */
	@Transactional
	public void setPassword(UUID userId, String newPassword, String confirmation) {
		PasswordPolicy.validate(newPassword, confirmation, "newPassword", "confirmPassword");
		users.changePasswordHash(userId, passwordEncoder.encode(newPassword));
		passwordResets.invalidateOutstanding(userId);
		refreshTokens.revokeAll(userId);
	}

}
