package com.xdsdata.taskflow.tasks.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.tasks.TaskPriority;
import com.xdsdata.taskflow.tasks.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request bodies of the task endpoints; limits mirror the database columns. */
final class TaskRequests {

	private TaskRequests() {
	}

	/** {@code status} comes from the column the task was created in; it defaults to Backlog. */
	record CreateTaskRequest(
			@NotBlank(message = "Title is required.") @Size(max = 200, message = "Title must be at most 200 characters.") String title,
			@Size(max = 5000, message = "Description must be at most 5000 characters.") String description,
			TaskPriority priority, LocalDate dueDate, TaskStatus status, UUID assigneeId,
			@Size(max = 20, message = "A task can have at most 20 tags.") List<UUID> tagIds,
			@Size(max = 10, message = "Add at most 10 new tags at once.") List<String> newTags) {
	}

	record UpdateTaskRequest(
			@NotBlank(message = "Title is required.") @Size(max = 200, message = "Title must be at most 200 characters.") String title,
			@Size(max = 5000, message = "Description must be at most 5000 characters.") String description,
			@NotNull(message = "Priority is required.") TaskPriority priority, LocalDate dueDate,
			@Size(max = 20, message = "A task can have at most 20 tags.") List<UUID> tagIds,
			@Size(max = 10, message = "Add at most 10 new tags at once.") List<String> newTags) {
	}

	/** {@code assigneeId} null unassigns the task. */
	record AssignTaskRequest(UUID assigneeId) {
	}

	/**
	 * Drop target: the column and the cards directly above ({@code previousTaskId}) and below
	 * ({@code nextTaskId}) the dropped card. Both null appends to the column.
	 */
	record MoveTaskRequest(@NotNull(message = "Status is required.") TaskStatus status, UUID previousTaskId,
			UUID nextTaskId) {
	}

	record CreateTagRequest(@NotBlank(message = "Tag name is required.") @Size(max = 30, message = "Tag names must be at most 30 characters.") String name) {
	}

}
