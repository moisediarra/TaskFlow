package com.xdsdata.taskflow.auth.internal;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.xdsdata.taskflow.common.error.BadRequestException;

/** Password rules: 8 to 72 characters (bcrypt only uses the first 72 bytes), confirmed by a second entry. */
public final class PasswordPolicy {

	static final int MIN_LENGTH = 8;

	static final int MAX_BYTES = 72;

	private PasswordPolicy() {
	}

	public static void validate(String password, String confirmation, String passwordField, String confirmationField) {
		Map<String, String> errors = new LinkedHashMap<>();
		if (password == null || password.length() < MIN_LENGTH) {
			errors.put(passwordField, "Password must be at least 8 characters.");
		}
		else if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
			errors.put(passwordField, "Password must be at most 72 characters.");
		}
		if (confirmation == null || !confirmation.equals(password)) {
			errors.put(confirmationField, "Passwords do not match.");
		}
		if (!errors.isEmpty()) {
			throw new BadRequestException("VALIDATION_FAILED", "Please correct the highlighted fields.", errors);
		}
	}

}
