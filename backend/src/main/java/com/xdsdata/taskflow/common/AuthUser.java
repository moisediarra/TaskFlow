package com.xdsdata.taskflow.common;

import java.util.UUID;

/**
 * The authenticated user making a request; also used as the actor recorded on domain events.
 * Reloaded from the database on every request, so role changes and deactivation apply immediately.
 */
public record AuthUser(UUID id, String name, String email, Role role) {

	public boolean isItManager() {
		return role == Role.IT_MANAGER;
	}

	public boolean canCreateProjects() {
		return role == Role.PROJECT_OWNER || role == Role.IT_MANAGER;
	}

}
