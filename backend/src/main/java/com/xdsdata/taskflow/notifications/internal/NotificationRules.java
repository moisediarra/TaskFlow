package com.xdsdata.taskflow.notifications.internal;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.xdsdata.taskflow.notifications.NotificationType;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskAssigneeChanged;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskMoved;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskSnapshot;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskUpdated;
import com.xdsdata.taskflow.tasks.TaskField;
import com.xdsdata.taskflow.tasks.TaskStatus;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Decides who is notified about a task change (claude.md §20). Notifications are only for people the change
 * concerns: never the person who made it, never deactivated accounts, and at most one per person per change.
 */
@Component
class NotificationRules {

	private final NotificationService notifications;

	private final DeadlineNotifier deadlines;

	private final UserService users;

	NotificationRules(NotificationService notifications, DeadlineNotifier deadlines, UserService users) {
		this.notifications = notifications;
		this.deadlines = deadlines;
		this.users = users;
	}

	@EventListener
	void on(TaskAssigneeChanged event) {
		TaskSnapshot task = event.task();
		UUID actorId = event.actor().id();
		if (event.previousAssigneeId() != null && !event.previousAssigneeId().equals(actorId)) {
			String message = event.memberRemoved()
					? "You were removed from %s, so \"%s\" is no longer assigned to you.".formatted(task.projectName(),
							task.title())
					: "%s unassigned you from \"%s\" in %s.".formatted(event.actor().name(), task.title(),
							task.projectName());
			send(event.previousAssigneeId(), NotificationType.TASK_UNASSIGNED, "You were removed from a task", message,
					task);
		}
		if (event.newAssigneeId() != null) {
			if (!event.newAssigneeId().equals(actorId)) {
				send(event.newAssigneeId(), NotificationType.TASK_ASSIGNED,
						"%s assigned you a task".formatted(event.actor().name()),
						"You have been assigned to \"%s\" in %s.".formatted(task.title(), task.projectName()), task);
			}
			deadlines.evaluate(task);
		}
	}

	@EventListener
	void on(TaskUpdated event) {
		TaskSnapshot task = event.after();
		if (task.assigneeId() != null && !task.assigneeId().equals(event.actor().id())) {
			send(task.assigneeId(), NotificationType.TASK_UPDATED,
					"%s updated your task".formatted(event.actor().name()),
					"\"%s\": %s changed.".formatted(task.title(), describe(event.changedFields())), task);
		}
		if (event.changedFields().contains(TaskField.DUE_DATE)) {
			deadlines.evaluate(task);
		}
	}

	/** The assignee hears about status changes made by others; so does the owner, so completions are visible. */
	@EventListener
	void on(TaskMoved event) {
		TaskSnapshot task = event.task();
		UUID actorId = event.actor().id();
		Set<UUID> notified = new HashSet<>();
		if (task.assigneeId() != null && !task.assigneeId().equals(actorId)) {
			send(task.assigneeId(), NotificationType.TASK_STATUS_CHANGED,
					"Your task was moved to %s".formatted(event.to().label()),
					"%s moved \"%s\" from %s to %s.".formatted(event.actor().name(), task.title(), event.from().label(),
							event.to().label()),
					task);
			notified.add(task.assigneeId());
		}
		UUID ownerId = task.projectOwnerId();
		if (!ownerId.equals(actorId) && !notified.contains(ownerId)) {
			String title = event.to() == TaskStatus.DONE ? "%s completed a task".formatted(event.actor().name())
					: "%s moved a task to %s".formatted(event.actor().name(), event.to().label());
			send(ownerId, NotificationType.TASK_STATUS_CHANGED, title,
					"\"%s\" in %s: %s → %s.".formatted(task.title(), task.projectName(), event.from().label(),
							event.to().label()),
					task);
		}
		if (event.from() == TaskStatus.DONE) {
			deadlines.evaluate(task);
		}
	}

	private void send(UUID recipientId, NotificationType type, String title, String message, TaskSnapshot task) {
		boolean active = users.findById(recipientId).map(User::isActive).orElse(false);
		if (active) {
			notifications.notify(recipientId, type, title, message, task.id(), task.projectId());
		}
	}

	private static String describe(Set<TaskField> fields) {
		return fields.stream().sorted(Comparator.naturalOrder()).map(field -> switch (field) {
			case TITLE -> "title";
			case DESCRIPTION -> "description";
			case PRIORITY -> "priority";
			case DUE_DATE -> "due date";
			case TAGS -> "tags";
		}).collect(Collectors.joining(", "));
	}

}
