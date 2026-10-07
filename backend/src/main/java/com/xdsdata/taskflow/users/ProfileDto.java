package com.xdsdata.taskflow.users;

import java.time.Instant;
import java.util.UUID;

import com.xdsdata.taskflow.common.Role;

/** The signed-in user's own profile. Never includes the password hash. */
public record ProfileDto(UUID id, String name, String email, Role role, UserStatus status, String jobTitle,
		String avatar, Instant createdAt) {

	public static ProfileDto from(User user) {
		return new ProfileDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus(),
				user.getJobTitle(), user.getAvatar(), user.getCreatedAt());
	}

}
