package com.xdsdata.taskflow.tasks;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.projects.Project;

/**
 * Domain events published by the tasks module inside the changing transaction. The activity module logs
 * them and the notifications module decides who to notify; the tasks module knows neither.
 */
public final class TaskEvents {

	private TaskEvents() {
	}

	/** Immutable copy of a task's state at the time of the event. */
	public record TaskSnapshot(UUID id, String title, UUID projectId, String projectName, UUID projectOwnerId,
			TaskStatus status, TaskPriority priority, LocalDate dueDate, UUID assigneeId, String assigneeName,
			List<String> tagNames) {

		public static TaskSnapshot of(Task task) {
			Project project = task.getProject();
			List<String> tagNames = task.getTags()
				.stream()
				.map(Tag::getName)
				.sorted(Comparator.comparing(String::toLowerCase))
				.toList();
			return new TaskSnapshot(task.getId(), task.getTitle(), project.getId(), project.getName(),
					project.getOwnerId(), task.getStatus(), task.getPriority(), task.getDueDate(),
					task.getAssignee() == null ? null : task.getAssignee().getId(),
					task.getAssignee() == null ? null : task.getAssignee().getName(), tagNames);
		}

	}

	public record TaskCreated(AuthUser actor, TaskSnapshot task) {
	}

	public record TaskUpdated(AuthUser actor, TaskSnapshot before, TaskSnapshot after, Set<TaskField> changedFields) {
	}

	/** Published only when the column changes; reordering within a column is not a business event. */
	public record TaskMoved(AuthUser actor, TaskSnapshot task, TaskStatus from, TaskStatus to) {
	}

	/**
	 * Assignment change. Either side may be null (assigned from nobody / unassigned to nobody).
	 * {@code memberRemoved} is true when the change happened because the assignee left the project.
	 */
	public record TaskAssigneeChanged(AuthUser actor, TaskSnapshot task, UUID previousAssigneeId,
			String previousAssigneeName, UUID newAssigneeId, String newAssigneeName, boolean memberRemoved) {
	}

	/** Published before the task row is deleted. */
	public record TaskDeleted(AuthUser actor, TaskSnapshot task) {
	}

}
