package com.xdsdata.taskflow.notifications.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import com.xdsdata.taskflow.notifications.NotificationType;
import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskSnapshot;
import com.xdsdata.taskflow.tasks.TaskStatus;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;
import com.xdsdata.taskflow.users.UserStatus;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deadline reminders (claude.md §20): "approaching" when a task is due today or tomorrow, "overdue" once the
 * date has passed. Each is sent once per task, due date and assignee, so the periodic sweep and the checks
 * made right after an edit never produce duplicates.
 */
@Component
public class DeadlineNotifier {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

	private final DeadlineTaskRepository tasks;

	private final NotificationService notifications;

	private final UserService users;

	private final Clock clock;

	DeadlineNotifier(DeadlineTaskRepository tasks, NotificationService notifications, UserService users, Clock clock) {
		this.tasks = tasks;
		this.notifications = notifications;
		this.users = users;
		this.clock = clock;
	}

	/** Checks every open, assigned task with a near or past due date. Returns how many reminders were sent. */
	@Transactional
	public int sweep() {
		LocalDate today = LocalDate.now(clock);
		int sent = 0;
		for (Task task : tasks.findDueBy(today.plusDays(1), TaskStatus.DONE, UserStatus.ACTIVE)) {
			if (remind(task.getId(), task.getTitle(), task.getProject().getId(), task.getAssignee().getId(),
					task.getDueDate(), today)) {
				sent++;
			}
		}
		return sent;
	}

	/** Immediate check after a task was assigned, rescheduled or reopened. */
	@Transactional
	void evaluate(TaskSnapshot task) {
		if (task.assigneeId() == null || task.dueDate() == null || task.status() == TaskStatus.DONE) {
			return;
		}
		boolean active = users.findById(task.assigneeId()).map(User::isActive).orElse(false);
		if (active) {
			remind(task.id(), task.title(), task.projectId(), task.assigneeId(), task.dueDate(), LocalDate.now(clock));
		}
	}

	private boolean remind(UUID taskId, String title, UUID projectId, UUID assigneeId, LocalDate dueDate,
			LocalDate today) {
		NotificationType type;
		String heading;
		String message;
		if (dueDate.isBefore(today)) {
			type = NotificationType.TASK_OVERDUE;
			heading = "Your task is overdue";
			message = "\"%s\" was due %s.".formatted(title, DATE.format(dueDate));
		}
		else if (!dueDate.isAfter(today.plusDays(1))) {
			type = NotificationType.TASK_DEADLINE_APPROACHING;
			boolean dueToday = dueDate.isEqual(today);
			heading = dueToday ? "Your task is due today" : "Your task deadline is tomorrow";
			message = "\"%s\" is due %s.".formatted(title, dueToday ? "today" : "tomorrow");
		}
		else {
			return false;
		}
		String dedupeKey = "%s:%s:%s:%s".formatted(type, taskId, dueDate, assigneeId);
		return notifications.notifyOnce(assigneeId, type, heading, message, taskId, projectId, dedupeKey);
	}

}
