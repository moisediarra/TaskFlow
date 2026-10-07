package com.xdsdata.taskflow.tasks.internal;

import com.xdsdata.taskflow.projects.ProjectEvents.ProjectMemberRemoved;
import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskAssigneeChanged;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskSnapshot;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * A task's assignee must be a project member, so removing a member unassigns their tasks in that project
 * (in the same transaction, each one logged and notified).
 */
@Component
class MemberRemovalListener {

	private final TaskRepository tasks;

	private final ApplicationEventPublisher events;

	MemberRemovalListener(TaskRepository tasks, ApplicationEventPublisher events) {
		this.tasks = tasks;
		this.events = events;
	}

	@EventListener
	void onMemberRemoved(ProjectMemberRemoved event) {
		for (Task task : tasks.findAssignedInProject(event.projectId(), event.memberId())) {
			task.assignTo(null);
			events.publishEvent(new TaskAssigneeChanged(event.actor(), TaskSnapshot.of(task), event.memberId(),
					event.memberName(), null, null, true));
		}
	}

}
