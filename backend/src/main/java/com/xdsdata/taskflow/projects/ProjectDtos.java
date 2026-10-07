package com.xdsdata.taskflow.projects;

import java.time.Instant;
import java.util.UUID;

import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserSummary;

/** API representations of projects and their members. */
public final class ProjectDtos {

	private ProjectDtos() {
	}

	/** Project card (claude.md §7): name, active tasks, members, last update. */
	public record ProjectSummaryDto(UUID id, String name, String description, UserSummary owner, long memberCount,
			long activeTaskCount, long totalTaskCount, Instant updatedAt, ProjectRole myRole, boolean canManage) {
	}

	public record ProjectDetailDto(UUID id, String name, String description, UserSummary owner, long memberCount,
			Instant createdAt, Instant updatedAt, ProjectRole myRole, ProjectPermissions permissions) {
	}

	public record ProjectMemberDto(UUID userId, String name, String email, String jobTitle, boolean active,
			ProjectRole role, Instant joinedAt) {

		public static ProjectMemberDto from(ProjectMember member) {
			User user = member.getUser();
			return new ProjectMemberDto(user.getId(), user.getName(), user.getEmail(), user.getJobTitle(),
					user.isActive(), member.getRole(), member.getJoinedAt());
		}

	}

}
