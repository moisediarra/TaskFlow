package com.xdsdata.taskflow.tasks.internal;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.error.BadRequestException;
import com.xdsdata.taskflow.projects.Project;
import com.xdsdata.taskflow.projects.ProjectAccess;
import com.xdsdata.taskflow.projects.ProjectService;
import com.xdsdata.taskflow.tasks.Tag;
import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.tasks.TaskDtos.TaskCardDto;
import com.xdsdata.taskflow.tasks.TaskDtos.TaskDetailDto;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskAssigneeChanged;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskCreated;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskDeleted;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskMoved;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskSnapshot;
import com.xdsdata.taskflow.tasks.TaskEvents.TaskUpdated;
import com.xdsdata.taskflow.tasks.TaskField;
import com.xdsdata.taskflow.tasks.TaskPriority;
import com.xdsdata.taskflow.tasks.TaskStatus;
import com.xdsdata.taskflow.tasks.internal.TaskAccess.TaskContext;
import com.xdsdata.taskflow.tasks.internal.TaskRequests.CreateTaskRequest;
import com.xdsdata.taskflow.tasks.internal.TaskRequests.UpdateTaskRequest;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;
import jakarta.persistence.EntityManager;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TaskService {

	private final TaskRepository tasks;

	private final TaskAccess taskAccess;

	private final ProjectAccess projectAccess;

	private final ProjectService projectService;

	private final TagService tagService;

	private final UserService users;

	private final TaskMapper mapper;

	private final ApplicationEventPublisher events;

	private final EntityManager entityManager;

	TaskService(TaskRepository tasks, TaskAccess taskAccess, ProjectAccess projectAccess, ProjectService projectService,
			TagService tagService, UserService users, TaskMapper mapper, ApplicationEventPublisher events,
			EntityManager entityManager) {
		this.tasks = tasks;
		this.taskAccess = taskAccess;
		this.projectAccess = projectAccess;
		this.projectService = projectService;
		this.tagService = tagService;
		this.users = users;
		this.mapper = mapper;
		this.events = events;
		this.entityManager = entityManager;
	}

	TaskDetailDto get(AuthUser user, UUID taskId) {
		TaskContext context = taskAccess.readable(user, taskId);
		return mapper.detail(context.task(), taskAccess.permissions(user, context));
	}

	@Transactional
	TaskDetailDto create(AuthUser actor, UUID projectId, CreateTaskRequest request) {
		Project project = projectAccess.requireManageable(actor, projectId);
		User assignee = resolveAssignee(project, request.assigneeId());
		Set<Tag> tags = tagService.resolve(project, request.tagIds(), request.newTags(), true);
		TaskStatus status = request.status() == null ? TaskStatus.BACKLOG : request.status();
		TaskPriority priority = request.priority() == null ? TaskPriority.MEDIUM : request.priority();
		Task task = tasks.saveAndFlush(new Task(project, request.title(), request.description(), status, priority,
				request.dueDate(), assignee, defaultPosition(projectId, status, null), tags));
		projectService.touch(projectId);
		TaskSnapshot snapshot = TaskSnapshot.of(task);
		events.publishEvent(new TaskCreated(actor, snapshot));
		if (assignee != null) {
			// An assignee picked in the creation form is logged and notified like any later assignment.
			events.publishEvent(new TaskAssigneeChanged(actor, snapshot, null, null, assignee.getId(),
					assignee.getName(), false));
		}
		return mapper.detail(task, TaskAccess.permissions(actor, task, true, true));
	}

	@Transactional
	TaskDetailDto update(AuthUser actor, UUID taskId, UpdateTaskRequest request) {
		TaskContext context = taskAccess.editable(actor, taskId);
		Task task = context.task();
		TaskSnapshot before = TaskSnapshot.of(task);
		Set<Tag> tags = tagService.resolve(context.project(), request.tagIds(), request.newTags(), context.owner());
		Set<TaskField> changed = task.applyDetails(request.title(), request.description(), request.priority(),
				request.dueDate(), tags);
		if (!changed.isEmpty()) {
			tasks.flush();
			projectService.touch(context.project().getId());
			events.publishEvent(new TaskUpdated(actor, before, TaskSnapshot.of(task), changed));
		}
		return mapper.detail(task, taskAccess.permissions(actor, context));
	}

	@Transactional
	TaskDetailDto assign(AuthUser actor, UUID taskId, UUID assigneeId) {
		TaskContext context = taskAccess.ownerOnly(actor, taskId, "assign tasks");
		Task task = context.task();
		User previous = task.getAssignee();
		User next = resolveAssignee(context.project(), assigneeId);
		if (!Objects.equals(idOf(previous), idOf(next))) {
			task.assignTo(next);
			tasks.flush();
			projectService.touch(context.project().getId());
			events.publishEvent(new TaskAssigneeChanged(actor, TaskSnapshot.of(task), idOf(previous),
					previous == null ? null : previous.getName(), idOf(next), next == null ? null : next.getName(),
					false));
		}
		return mapper.detail(task, taskAccess.permissions(actor, context));
	}

	@Transactional
	TaskCardDto move(AuthUser actor, UUID taskId, TaskStatus status, UUID previousTaskId, UUID nextTaskId) {
		TaskContext context = taskAccess.editable(actor, taskId);
		Task task = context.task();
		TaskStatus from = task.getStatus();
		double position = positionFor(task, status, previousTaskId, nextTaskId);
		task.moveTo(status, position);
		tasks.flush();
		if (from != status) {
			projectService.touch(context.project().getId());
			events.publishEvent(new TaskMoved(actor, TaskSnapshot.of(task), from, status));
		}
		return mapper.card(task, taskAccess.permissions(actor, context), mapper.today());
	}

	@Transactional
	void delete(AuthUser actor, UUID taskId) {
		TaskContext context = taskAccess.ownerOnly(actor, taskId, "delete tasks");
		Task task = context.task();
		events.publishEvent(new TaskDeleted(actor, TaskSnapshot.of(task)));
		tasks.delete(task);
		projectService.touch(context.project().getId());
	}

	/**
	 * Position between the requested neighbours. Neighbours that moved meanwhile (stale client state) make
	 * the card fall back to the default spot of the column instead of failing the drop.
	 */
	private double positionFor(Task task, TaskStatus status, UUID previousTaskId, UUID nextTaskId) {
		UUID projectId = task.getProject().getId();
		Double above = neighbourPosition(previousTaskId, task, status);
		Double below = neighbourPosition(nextTaskId, task, status);
		boolean stale = (previousTaskId != null && above == null) || (nextTaskId != null && below == null)
				|| (above != null && below != null && below < above);
		if (stale || (previousTaskId == null && nextTaskId == null)) {
			return defaultPosition(projectId, status, task.getId());
		}
		if (Positions.needsRenumbering(above, below)) {
			tasks.renumberColumn(projectId, status.name());
			entityManager.refresh(task);
			above = previousTaskId == null ? null : tasks.findPosition(previousTaskId);
			below = nextTaskId == null ? null : tasks.findPosition(nextTaskId);
		}
		return Positions.between(above, below);
	}

	private Double neighbourPosition(UUID neighbourId, Task moving, TaskStatus status) {
		if (neighbourId == null || neighbourId.equals(moving.getId())) {
			return null;
		}
		UUID projectId = moving.getProject().getId();
		return tasks.findById(neighbourId)
			.filter(neighbour -> neighbour.getProject().getId().equals(projectId) && neighbour.getStatus() == status)
			.map(Task::getPosition)
			.orElse(null);
	}

	/** New cards go to the bottom of a column, except Done where the latest completion goes on top. */
	private double defaultPosition(UUID projectId, TaskStatus status, UUID excludedTaskId) {
		if (status == TaskStatus.DONE) {
			Double top = excludedTaskId == null ? tasks.minPosition(projectId, status)
					: tasks.minPositionExcluding(projectId, status, excludedTaskId);
			return Positions.between(null, top);
		}
		Double bottom = excludedTaskId == null ? tasks.maxPosition(projectId, status)
				: tasks.maxPositionExcluding(projectId, status, excludedTaskId);
		return Positions.between(bottom, null);
	}

	private User resolveAssignee(Project project, UUID assigneeId) {
		if (assigneeId == null) {
			return null;
		}
		User user = users.findById(assigneeId).filter(User::isActive).orElse(null);
		if (user == null || !projectAccess.isMember(project.getId(), assigneeId)) {
			throw BadRequestException.field("assigneeId", "The assignee must be an active member of this project.");
		}
		return user;
	}

	private static UUID idOf(User user) {
		return user == null ? null : user.getId();
	}

}
