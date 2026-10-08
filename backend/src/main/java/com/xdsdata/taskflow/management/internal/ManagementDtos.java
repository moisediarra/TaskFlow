package com.xdsdata.taskflow.management.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.activity.ActivityDto;
import com.xdsdata.taskflow.common.DueState;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.projects.ProjectRole;
import com.xdsdata.taskflow.tasks.TaskPriority;
import com.xdsdata.taskflow.tasks.TaskStatus;
import com.xdsdata.taskflow.users.UserStatus;

final class ManagementDtos {

	private ManagementDtos() {
	}

	record UserStats(long total, long active) {
	}

	record ProjectStats(long total, long active) {
	}

	record TaskStats(long total, long backlog, long todo, long inProgress, long done, long active, long overdue,
			long highPriority) {
	}

	record PriorityCount(TaskPriority priority, long count) {
	}

	/** "Who is doing what": one assigned task with its person and project. */
	record WorkItemDto(UUID taskId, String title, TaskStatus status, TaskPriority priority, LocalDate dueDate,
			DueState dueState, UUID projectId, String projectName, UUID assigneeId, String assigneeName,
			String assigneeJobTitle, Instant updatedAt) {
	}

	/** Neutral workload figures; flags only point at possible imbalances, never at performance. */
	record WorkloadRow(UUID userId, String name, String jobTitle, long active, long inProgress, long todo,
			long overdue, boolean manyActive, boolean manyOverdue) {
	}

	record WorkloadDto(List<WorkloadRow> rows, List<String> warnings, int activeTaskWarning, int overdueTaskWarning,
			long scaleMax) {
	}

	/** IT Management Dashboard (claude.md §24) answering the five questions of §45. */
	record OverviewDto(UserStats users, ProjectStats projects, TaskStats tasks, List<PriorityCount> openByPriority,
			List<WorkItemDto> inProgressNow, List<WorkItemDto> overdueTasks, List<WorkloadRow> topWorkload,
			List<String> workloadWarnings, List<ActivityDto> recentActivity) {
	}

	record UserRowDto(UUID id, String name, String email, Role role, UserStatus status, String jobTitle,
			Instant createdAt, long projectCount, long activeTaskCount) {
	}

	record MembershipDto(UUID projectId, String projectName, ProjectRole role, Instant joinedAt) {
	}

	record UserDetailDto(UserRowDto user, long ownedProjectCount, long overdueTaskCount, List<MembershipDto> projects,
			List<WorkItemDto> tasks) {
	}

}
