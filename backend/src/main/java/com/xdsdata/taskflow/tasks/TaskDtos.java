package com.xdsdata.taskflow.tasks;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.common.DueState;
import com.xdsdata.taskflow.projects.ProjectDtos.ProjectMemberDto;
import com.xdsdata.taskflow.projects.ProjectPermissions;
import com.xdsdata.taskflow.users.User;

/** API representations of tasks and boards. */
public final class TaskDtos {

	private TaskDtos() {
	}

	public record TagDto(UUID id, String name, String color) {

		public static TagDto from(Tag tag) {
			return new TagDto(tag.getId(), tag.getName(), tag.getColor());
		}

	}

	public record AssigneeDto(UUID id, String name, String jobTitle, boolean active) {

		public static AssigneeDto from(User user) {
			return user == null ? null : new AssigneeDto(user.getId(), user.getName(), user.getJobTitle(), user.isActive());
		}

	}

	/** What the current user may do with a task; the server enforces the same rules. */
	public record TaskPermissions(boolean canEdit, boolean canMove, boolean canDelete, boolean canAssign) {
	}

	/** Compact card shown on the board (claude.md §16–18). */
	public record TaskCardDto(UUID id, String title, TaskStatus status, TaskPriority priority, LocalDate dueDate,
			DueState dueState, double position, AssigneeDto assignee, List<TagDto> tags, TaskPermissions permissions,
			Instant updatedAt) {
	}

	/** Full task shown in the details panel (claude.md §14). */
	public record TaskDetailDto(UUID id, UUID projectId, String projectName, String title, String description,
			TaskStatus status, TaskPriority priority, LocalDate dueDate, DueState dueState, AssigneeDto assignee,
			List<TagDto> tags, TaskPermissions permissions, Instant createdAt, Instant updatedAt) {
	}

	public record BoardColumnDto(TaskStatus status, String label, List<TaskCardDto> tasks, long totalCount) {
	}

	public record BoardProjectDto(UUID id, String name, String description, UUID ownerId, String ownerName) {
	}

	public record BoardDto(BoardProjectDto project, ProjectPermissions permissions, List<ProjectMemberDto> members,
			List<TagDto> tags, List<BoardColumnDto> columns) {
	}

	/** One page of a column ("Show more" in Done). */
	public record TaskSliceDto(List<TaskCardDto> items, boolean hasMore) {
	}

}
