package com.xdsdata.taskflow.management.internal;

import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.users.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request bodies of the IT Manager's account administration. Each carries {@code currentPassword}: the IT
 * Manager re-enters their own password to confirm the change. Limits mirror the database columns; password
 * rules are checked by the auth module.
 */
final class UserAdminRequests {

	private static final String CONFIRM = "Enter your password to confirm.";

	private UserAdminRequests() {
	}

	record CreateUserRequest(
			@NotBlank(message = "Full name is required.") @Size(max = 100, message = "Full name must be at most 100 characters.") String name,
			@NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") @Size(max = 254, message = "Email is too long.") String email,
			@Size(max = 100, message = "Job title must be at most 100 characters.") String jobTitle,
			@NotNull(message = "Role is required.") Role role,
			@NotBlank(message = "Password is required.") String password,
			@NotBlank(message = "Please confirm the password.") String confirmPassword,
			@NotBlank(message = CONFIRM) String currentPassword) {
	}

	record UpdateUserRequest(
			@NotBlank(message = "Full name is required.") @Size(max = 100, message = "Full name must be at most 100 characters.") String name,
			@NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") @Size(max = 254, message = "Email is too long.") String email,
			@Size(max = 100, message = "Job title must be at most 100 characters.") String jobTitle,
			@NotBlank(message = CONFIRM) String currentPassword) {
	}

	record ResetPasswordRequest(@NotBlank(message = "New password is required.") String newPassword,
			@NotBlank(message = "Please confirm the new password.") String confirmPassword,
			@NotBlank(message = CONFIRM) String currentPassword) {
	}

	record ChangeRoleRequest(@NotNull(message = "Role is required.") Role role,
			@NotBlank(message = CONFIRM) String currentPassword) {
	}

	record ChangeStatusRequest(@NotNull(message = "Status is required.") UserStatus status,
			@NotBlank(message = CONFIRM) String currentPassword) {
	}

	record DeleteUserRequest(@NotBlank(message = CONFIRM) String currentPassword) {
	}

}
