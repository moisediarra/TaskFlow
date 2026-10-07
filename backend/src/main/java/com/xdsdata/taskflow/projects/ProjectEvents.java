package com.xdsdata.taskflow.projects;

import java.util.Map;
import java.util.UUID;

import com.xdsdata.taskflow.common.AuthUser;

/**
 * Domain events published by the projects module inside the changing transaction. Listeners record the
 * activity log and react (for example, unassigning a removed member's tasks).
 */
public final class ProjectEvents {

	private ProjectEvents() {
	}

	public record ProjectCreated(AuthUser actor, UUID projectId, String projectName) {
	}

	/** {@code changes} maps a field name to its {old, new} values. */
	public record ProjectUpdated(AuthUser actor, UUID projectId, String projectName,
			Map<String, Map<String, String>> changes) {
	}

	/** Published before the project row is deleted. */
	public record ProjectDeleted(AuthUser actor, UUID projectId, String projectName) {
	}

	public record ProjectMemberAdded(AuthUser actor, UUID projectId, String projectName, UUID memberId,
			String memberName) {
	}

	public record ProjectMemberRemoved(AuthUser actor, UUID projectId, String projectName, UUID memberId,
			String memberName) {
	}

}
