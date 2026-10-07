package com.xdsdata.taskflow.users.internal;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.users.ProfileDto;
import com.xdsdata.taskflow.users.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
class UserController {

	private final UserService users;

	UserController(UserService users) {
		this.users = users;
	}

	/** Profile edits are limited to name and job title; role, status and email are never client-editable. */
	@PatchMapping("/me")
	ProfileDto updateProfile(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody UpdateProfileRequest request) {
		return users.updateProfile(user.id(), request.name(), request.jobTitle());
	}

	record UpdateProfileRequest(
			@NotBlank(message = "Full name is required.") @Size(max = 100, message = "Full name must be at most 100 characters.") String name,
			@Size(max = 100, message = "Job title must be at most 100 characters.") String jobTitle) {
	}

}
