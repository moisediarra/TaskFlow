package com.xdsdata.taskflow.activity.internal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.xdsdata.taskflow.activity.ActivityAction;
import com.xdsdata.taskflow.activity.ActivityService;
import com.xdsdata.taskflow.projects.ProjectEvents.ProjectCreated;
import com.xdsdata.taskflow.projects.ProjectEvents.ProjectDeleted;
import com.xdsdata.taskflow.projects.ProjectEvents.ProjectMemberAdded;
import com.xdsdata.taskflow.projects.ProjectEvents.ProjectMemberRemoved;
import com.xdsdata.taskflow.projects.ProjectEvents.ProjectUpdated;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskAssigneeChanged;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskCreated;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskDeleted;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskMoved;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskSnapshot;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskUpdated;
import com.xdsdata.taskflow.tasks.TaskField;
import com.xdsdata.taskflow.tasks.TaskStatus;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Translates project and task events into activity entries, synchronously and in the same transaction
 * as the change. Descriptions read like the examples in claude.md §27.
 */
@Component
class ActivityEventListener {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

	private final ActivityService activity;

	ActivityEventListener(ActivityService activity) {
		this.activity = activity;
	}

	@EventListener
	void on(ProjectCreated event) {
		activity.record(event.actor(), ActivityAction.PROJECT_CREATED, event.projectId(), null,
				"%s created the project \"%s\".".formatted(event.actor().name(), event.projectName()),
				Map.of("projectName", event.projectName()));
	}

	@EventListener
	void on(ProjectUpdated event) {
		boolean renamed = event.changedFields().contains("name");
		String description = renamed
				? "%s renamed the project \"%s\" to \"%s\".".formatted(event.actor().name(), event.previousName(),
						event.projectName())
				: "%s updated the project \"%s\".".formatted(event.actor().name(), event.projectName());
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("projectName", event.projectName());
		metadata.put("changedFields", List.copyOf(event.changedFields()));
		if (renamed) {
			metadata.put("previousName", event.previousName());
		}
		activity.record(event.actor(), ActivityAction.PROJECT_UPDATED, event.projectId(), null, description, metadata);
	}

	@EventListener
	void on(ProjectDeleted event) {
		activity.record(event.actor(), ActivityAction.PROJECT_DELETED, event.projectId(), null,
				"%s deleted the project \"%s\".".formatted(event.actor().name(), event.projectName()),
				Map.of("projectName", event.projectName()));
	}

	@EventListener
	void on(ProjectMemberAdded event) {
		activity.record(event.actor(), ActivityAction.PROJECT_MEMBER_ADDED, event.projectId(), null,
				"%s added %s to \"%s\".".formatted(event.actor().name(), event.memberName(), event.projectName()),
				Map.of("projectName", event.projectName(), "memberId", event.memberId().toString(), "memberName",
						event.memberName()));
	}

	@EventListener
	void on(ProjectMemberRemoved event) {
		activity.record(event.actor(), ActivityAction.PROJECT_MEMBER_REMOVED, event.projectId(), null,
				"%s removed %s from \"%s\".".formatted(event.actor().name(), event.memberName(), event.projectName()),
				Map.of("projectName", event.projectName(), "memberId", event.memberId().toString(), "memberName",
						event.memberName()));
	}

	@EventListener
	void on(TaskCreated event) {
		TaskSnapshot task = event.task();
		activity.record(event.actor(), ActivityAction.TASK_CREATED, task.projectId(), task.id(),
				"%s created \"%s\" in %s.".formatted(event.actor().name(), task.title(), task.status().label()),
				taskMetadata(task, Map.of("status", task.status().name())));
	}

	@EventListener
	void on(TaskUpdated event) {
		TaskSnapshot before = event.before();
		TaskSnapshot after = event.after();
		String actor = event.actor().name();
		Set<TaskField> changed = event.changedFields();
		if (changed.contains(TaskField.PRIORITY)) {
			activity.record(event.actor(), ActivityAction.TASK_PRIORITY_CHANGED, after.projectId(), after.id(),
					"%s changed the priority of \"%s\" from %s to %s.".formatted(actor, after.title(),
							before.priority().label(), after.priority().label()),
					taskMetadata(after,
							Map.of("oldPriority", before.priority().name(), "newPriority", after.priority().name())));
		}
		if (changed.contains(TaskField.DUE_DATE)) {
			Map<String, Object> dates = new LinkedHashMap<>();
			dates.put("oldDueDate", before.dueDate() == null ? null : before.dueDate().toString());
			dates.put("newDueDate", after.dueDate() == null ? null : after.dueDate().toString());
			activity.record(event.actor(), ActivityAction.TASK_DUE_DATE_CHANGED, after.projectId(), after.id(),
					dueDateDescription(actor, after.title(), before.dueDate(), after.dueDate()), taskMetadata(after, dates));
		}
		Set<TaskField> details = EnumSet.noneOf(TaskField.class);
		changed.stream()
			.filter(field -> field == TaskField.TITLE || field == TaskField.DESCRIPTION || field == TaskField.TAGS)
			.forEach(details::add);
		if (!details.isEmpty()) {
			String description = details.contains(TaskField.TITLE)
					? "%s renamed \"%s\" to \"%s\".".formatted(actor, before.title(), after.title())
					: "%s updated \"%s\".".formatted(actor, after.title());
			Map<String, Object> extra = new LinkedHashMap<>();
			extra.put("changedFields", details.stream().map(Enum::name).toList());
			if (details.contains(TaskField.TITLE)) {
				extra.put("previousTitle", before.title());
			}
			if (details.contains(TaskField.TAGS)) {
				extra.put("tags", after.tagNames());
			}
			activity.record(event.actor(), ActivityAction.TASK_UPDATED, after.projectId(), after.id(), description,
					taskMetadata(after, extra));
		}
	}

	@EventListener
	void on(TaskMoved event) {
		TaskSnapshot task = event.task();
		Map<String, Object> statuses = Map.of("oldStatus", event.from().name(), "newStatus", event.to().name());
		if (event.to() == TaskStatus.DONE) {
			activity.record(event.actor(), ActivityAction.TASK_COMPLETED, task.projectId(), task.id(),
					"%s completed \"%s\".".formatted(event.actor().name(), task.title()), taskMetadata(task, statuses));
		}
		else {
			activity.record(event.actor(), ActivityAction.TASK_STATUS_CHANGED, task.projectId(), task.id(),
					"%s moved \"%s\" from %s to %s.".formatted(event.actor().name(), task.title(), event.from().label(),
							event.to().label()),
					taskMetadata(task, statuses));
		}
	}

	@EventListener
	void on(TaskAssigneeChanged event) {
		TaskSnapshot task = event.task();
		if (event.previousAssigneeId() != null) {
			String description = event.memberRemoved()
					? "%s was unassigned from \"%s\" after leaving the project.".formatted(event.previousAssigneeName(),
							task.title())
					: "%s was unassigned from \"%s\".".formatted(event.previousAssigneeName(), task.title());
			activity.record(event.actor(), ActivityAction.TASK_UNASSIGNED, task.projectId(), task.id(), description,
					taskMetadata(task, Map.of("assigneeId", event.previousAssigneeId().toString(), "assigneeName",
							event.previousAssigneeName())));
		}
		if (event.newAssigneeId() != null) {
			activity.record(event.actor(), ActivityAction.TASK_ASSIGNED, task.projectId(), task.id(),
					"%s was assigned \"%s\".".formatted(event.newAssigneeName(), task.title()),
					taskMetadata(task,
							Map.of("assigneeId", event.newAssigneeId().toString(), "assigneeName", event.newAssigneeName())));
		}
	}

	@EventListener
	void on(TaskDeleted event) {
		TaskSnapshot task = event.task();
		activity.record(event.actor(), ActivityAction.TASK_DELETED, task.projectId(), task.id(),
				"%s deleted \"%s\".".formatted(event.actor().name(), task.title()), taskMetadata(task, Map.of()));
	}

	private static Map<String, Object> taskMetadata(TaskSnapshot task, Map<String, ?> extra) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("taskTitle", task.title());
		metadata.put("projectName", task.projectName());
		metadata.putAll(extra);
		return metadata;
	}

	private static String dueDateDescription(String actor, String title, LocalDate before, LocalDate after) {
		if (after == null) {
			return "%s removed the due date of \"%s\".".formatted(actor, title);
		}
		if (before == null) {
			return "%s set the due date of \"%s\" to %s.".formatted(actor, title, DATE.format(after));
		}
		return "%s changed the due date of \"%s\" from %s to %s.".formatted(actor, title, DATE.format(before),
				DATE.format(after));
	}

}
