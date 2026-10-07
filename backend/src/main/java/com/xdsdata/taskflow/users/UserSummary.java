package com.xdsdata.taskflow.users;

import java.util.UUID;

/** Compact view of a user for lists, cards and pickers. */
public record UserSummary(UUID id, String name, String email, String jobTitle, boolean active) {

	public static UserSummary from(User user) {
		return new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getJobTitle(), user.isActive());
	}

}
