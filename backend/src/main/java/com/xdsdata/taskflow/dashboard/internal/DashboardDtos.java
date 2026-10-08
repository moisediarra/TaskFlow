package com.xdsdata.taskflow.dashboard.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.activity.ActivityDto;
import com.xdsdata.taskflow.common.DueState;
import com.xdsdata.taskflow.projects.ProjectDtos.ProjectMemberDto;
import com.xdsdata.taskflow.projects.ProjectDtos.ProjectSummaryDto;
import com.xdsdata.taskflow.projects.ProjectPermissions;
import com.xdsdata.taskflow.tasks.TaskPriority;
import com.xdsdata.taskflow.tasks.TaskStatus;
import com.xdsdata.taskflow.users.UserSummary;

final class DashboardDtos {

	private DashboardDtos() {
	}

	/** Task counts per column; {@code total} includes Backlog. */
	record StatusCounts(long total, long backlog, long todo, long inProgress, long done) {
	}

	record TaskRefDto(UUID id, String title, UUID projectId, String projectName, TaskStatus status,
			TaskPriority priority, LocalDate dueDate, DueState dueState) {
	}

	record TaskList(List<TaskRefDto> items, long total) {
	}

	/** The signed-in user's dashboard (claude.md §6). */
	record DashboardDto(StatusCounts myTasks, TaskList highPriority, TaskList dueToday, TaskList overdue,
			List<ProjectSummaryDto> recentProjects) {
	}

	record WorkItemDto(UUID taskId, String title, TaskStatus status, TaskPriority priority, LocalDate dueDate,
			DueState dueState, UUID assigneeId, String assigneeName, String assigneeJobTitle, Instant updatedAt) {
	}

	/** Project monitoring view (claude.md §30). */
	record ProjectOverviewDto(UUID id, String name, String description, UserSummary owner, long memberCount,
			StatusCounts tasks, long overdue, long highPriority, List<WorkItemDto> currentActivity,
			List<ProjectMemberDto> members, List<ActivityDto> recentActivity, ProjectPermissions permissions) {
	}

}
