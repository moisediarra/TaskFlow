package com.xdsdata.taskflow.tasks.internal;

import java.util.UUID;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.error.ForbiddenException;
import com.xdsdata.taskflow.common.error.NotFoundException;
import com.xdsdata.taskflow.projects.Project;
import com.xdsdata.taskflow.projects.ProjectAccess;
import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.tasks.TaskDtos.TaskPermissions;

import org.springframework.stereotype.Component;

/**
 * Task-level authorization. The project is always derived from the task itself, never from the request,
 * so a tampered id can only ever reach a task the caller is allowed to see.
 * <ul>
 * <li>Project owner: everything.</li>
 * <li>Project member: read; edit and move only tasks assigned to them.</li>
 * <li>IT Manager: read-only, unless they are a member or owner of the project.</li>
 * </ul>
 */
@Component
class TaskAccess {

	private final TaskRepository tasks;

	private final ProjectAccess projectAccess;

	TaskAccess(TaskRepository tasks, ProjectAccess projectAccess) {
		this.tasks = tasks;
		this.projectAccess = projectAccess;
	}

	TaskContext readable(AuthUser user, UUID taskId) {
		Task task = tasks.findDetailed(taskId).orElseThrow(() -> new NotFoundException("Task"));
		Project project = task.getProject();
		boolean owner = projectAccess.isOwner(user, project);
		boolean member = owner || projectAccess.isMember(project.getId(), user.id());
		if (!member && !user.isItManager()) {
			throw new ForbiddenException("You don't have access to this project.");
		}
		return new TaskContext(task, project, owner, member);
	}

	TaskContext editable(AuthUser user, UUID taskId) {
		TaskContext context = readable(user, taskId);
		if (!permissions(user, context).canEdit()) {
			throw new ForbiddenException(context.member() ? "You can only change tasks assigned to you."
					: "You can view this project but not change it.");
		}
		return context;
	}

	TaskContext ownerOnly(AuthUser user, UUID taskId, String action) {
		TaskContext context = readable(user, taskId);
		if (!context.owner()) {
			throw new ForbiddenException("Only the project owner can " + action + ".");
		}
		return context;
	}

	TaskPermissions permissions(AuthUser user, TaskContext context) {
		return permissions(user, context.task(), context.owner(), context.member());
	}

	static TaskPermissions permissions(AuthUser user, Task task, boolean owner, boolean member) {
		boolean edit = owner || (member && task.isAssignedTo(user.id()));
		return new TaskPermissions(edit, edit, owner, owner);
	}

	record TaskContext(Task task, Project project, boolean owner, boolean member) {
	}

}
