package com.xdsdata.taskflow.dashboard.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.xdsdata.taskflow.activity.ActivityService;
import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.DueState;
import com.xdsdata.taskflow.dashboard.internal.DashboardDtos.DashboardDto;
import com.xdsdata.taskflow.dashboard.internal.DashboardDtos.ProjectOverviewDto;
import com.xdsdata.taskflow.dashboard.internal.DashboardDtos.StatusCounts;
import com.xdsdata.taskflow.dashboard.internal.DashboardDtos.TaskList;
import com.xdsdata.taskflow.dashboard.internal.DashboardDtos.TaskRefDto;
import com.xdsdata.taskflow.dashboard.internal.DashboardDtos.WorkItemDto;
import com.xdsdata.taskflow.projects.Project;
import com.xdsdata.taskflow.projects.ProjectAccess;
import com.xdsdata.taskflow.projects.ProjectDtos.ProjectMemberDto;
import com.xdsdata.taskflow.projects.ProjectService;
import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.tasks.TaskPriority;
import com.xdsdata.taskflow.tasks.TaskStatus;
import com.xdsdata.taskflow.users.UserSummary;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class DashboardService {

	private static final Limit LIST_LIMIT = Limit.of(5);

	private static final int RECENT_PROJECTS = 5;

	private static final Set<TaskStatus> CURRENT_WORK = Set.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE);

	private final DashboardQueries queries;

	private final ProjectService projects;

	private final ProjectAccess projectAccess;

	private final ActivityService activity;

	private final Clock clock;

	DashboardService(DashboardQueries queries, ProjectService projects, ProjectAccess projectAccess,
			ActivityService activity, Clock clock) {
		this.queries = queries;
		this.projects = projects;
		this.projectAccess = projectAccess;
		this.activity = activity;
		this.clock = clock;
	}

	DashboardDto dashboard(AuthUser user) {
		LocalDate today = LocalDate.now(clock);
		UUID userId = user.id();
		TaskStatus done = TaskStatus.DONE;
		TaskList highPriority = new TaskList(
				refs(queries.findAssignedOpenWithPriority(userId, done, TaskPriority.HIGH, LIST_LIMIT), today),
				queries.countAssignedOpenWithPriority(userId, done, TaskPriority.HIGH));
		List<Task> dueTodayTasks = queries.findAssignedOpenDueOn(userId, done, today, LIST_LIMIT)
			.stream()
			.sorted(Comparator.comparing(Task::getPriority))
			.toList();
		TaskList dueToday = new TaskList(refs(dueTodayTasks, today), queries.countAssignedOpenDueOn(userId, done, today));
		TaskList overdue = new TaskList(refs(queries.findAssignedOverdue(userId, done, today, LIST_LIMIT), today),
				queries.countAssignedOverdue(userId, done, today));
		return new DashboardDto(statusCounts(queries.countAssignedByStatus(userId)), highPriority, dueToday, overdue,
				projects.listMine(user).stream().limit(RECENT_PROJECTS).toList());
	}

	ProjectOverviewDto overview(AuthUser user, UUID projectId) {
		Project project = projectAccess.requireReadable(user, projectId);
		LocalDate today = LocalDate.now(clock);
		List<ProjectMemberDto> members = projects.membersOf(projectId);
		List<WorkItemDto> currentActivity = queries.findProjectAssignedWork(projectId, CURRENT_WORK, Limit.of(30))
			.stream()
			.sorted(Comparator.comparingInt((Task task) -> workRank(task.getStatus())))
			.limit(15)
			.map(task -> new WorkItemDto(task.getId(), task.getTitle(), task.getStatus(), task.getPriority(),
					task.getDueDate(), DueState.of(task.getDueDate(), task.isDone(), today), task.getAssignee().getId(),
					task.getAssignee().getName(), task.getAssignee().getJobTitle(), task.getUpdatedAt()))
			.toList();
		return new ProjectOverviewDto(project.getId(), project.getName(), project.getDescription(),
				UserSummary.from(project.getOwner()), members.size(),
				statusCounts(queries.countProjectByStatus(projectId)),
				queries.countProjectOverdue(projectId, TaskStatus.DONE, today),
				queries.countProjectOpenWithPriority(projectId, TaskStatus.DONE, TaskPriority.HIGH), currentActivity,
				members, activity.recentForProject(projectId, 10), projectAccess.permissionsFor(user, project));
	}

	/** In Progress first, then To Do, then recently completed work. */
	private static int workRank(TaskStatus status) {
		return switch (status) {
			case IN_PROGRESS -> 0;
			case TODO -> 1;
			case DONE -> 2;
			case BACKLOG -> 3;
		};
	}

	private static StatusCounts statusCounts(List<Object[]> rows) {
		Map<TaskStatus, Long> counts = new EnumMap<>(TaskStatus.class);
		for (Object[] row : rows) {
			counts.put((TaskStatus) row[0], (Long) row[1]);
		}
		long backlog = counts.getOrDefault(TaskStatus.BACKLOG, 0L);
		long todo = counts.getOrDefault(TaskStatus.TODO, 0L);
		long inProgress = counts.getOrDefault(TaskStatus.IN_PROGRESS, 0L);
		long done = counts.getOrDefault(TaskStatus.DONE, 0L);
		return new StatusCounts(backlog + todo + inProgress + done, backlog, todo, inProgress, done);
	}

	private static List<TaskRefDto> refs(List<Task> tasks, LocalDate today) {
		return tasks.stream()
			.map(task -> new TaskRefDto(task.getId(), task.getTitle(), task.getProject().getId(),
					task.getProject().getName(), task.getStatus(), task.getPriority(), task.getDueDate(),
					DueState.of(task.getDueDate(), task.isDone(), today)))
			.toList();
	}

}
