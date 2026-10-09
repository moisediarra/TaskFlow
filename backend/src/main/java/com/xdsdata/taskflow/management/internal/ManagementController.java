package com.xdsdata.taskflow.management.internal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.activity.ActivityAction;
import com.xdsdata.taskflow.activity.ActivityDto;
import com.xdsdata.taskflow.activity.ActivityFilter;
import com.xdsdata.taskflow.activity.ActivityService;
import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.common.web.CursorPage;
import com.xdsdata.taskflow.common.web.PageResponse;
import com.xdsdata.taskflow.management.internal.ManagementDtos.OverviewDto;
import com.xdsdata.taskflow.management.internal.ManagementDtos.UserDetailDto;
import com.xdsdata.taskflow.management.internal.ManagementDtos.UserRowDto;
import com.xdsdata.taskflow.management.internal.ManagementDtos.WorkItemDto;
import com.xdsdata.taskflow.management.internal.ManagementDtos.WorkloadDto;
import com.xdsdata.taskflow.management.internal.ManagementService.DueFilter;
import com.xdsdata.taskflow.management.internal.ManagementService.TeamActivityFilter;
import com.xdsdata.taskflow.management.internal.UserAdminRequests.ChangeRoleRequest;
import com.xdsdata.taskflow.management.internal.UserAdminRequests.ChangeStatusRequest;
import com.xdsdata.taskflow.management.internal.UserAdminRequests.CreateUserRequest;
import com.xdsdata.taskflow.management.internal.UserAdminRequests.DeleteUserRequest;
import com.xdsdata.taskflow.management.internal.UserAdminRequests.ResetPasswordRequest;
import com.xdsdata.taskflow.management.internal.UserAdminRequests.UpdateUserRequest;
import com.xdsdata.taskflow.projects.ProjectDtos.ProjectSummaryDto;
import com.xdsdata.taskflow.projects.ProjectService;
import com.xdsdata.taskflow.tasks.TaskPriority;
import com.xdsdata.taskflow.tasks.TaskStatus;
import com.xdsdata.taskflow.users.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** IT Management endpoints; restricted to IT Managers here and at URL level. */
@RestController
@RequestMapping("/api/management")
@PreAuthorize("hasRole('IT_MANAGER')")
class ManagementController {

	private final ManagementService management;

	private final UserAdminService userAdmin;

	private final ActivityService activity;

	private final ProjectService projects;

	private final Clock clock;

	ManagementController(ManagementService management, UserAdminService userAdmin, ActivityService activity,
			ProjectService projects, Clock clock) {
		this.management = management;
		this.userAdmin = userAdmin;
		this.activity = activity;
		this.projects = projects;
		this.clock = clock;
	}

	@GetMapping("/overview")
	OverviewDto overview() {
		return management.overview();
	}

	@GetMapping("/team-activity")
	PageResponse<WorkItemDto> teamActivity(@RequestParam(required = false) UUID userId,
			@RequestParam(required = false) UUID projectId, @RequestParam(required = false) List<TaskStatus> status,
			@RequestParam(required = false) TaskPriority priority,
			@RequestParam(defaultValue = "ANY") DueFilter due, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return management.teamActivity(new TeamActivityFilter(userId, projectId, status, priority, due), page, size);
	}

	@GetMapping("/workload")
	WorkloadDto workload() {
		return management.workload();
	}

	/** {@code from}/{@code to} are inclusive calendar days in the application time zone. */
	@GetMapping("/activity-logs")
	CursorPage<ActivityDto> activityLogs(@RequestParam(required = false) UUID userId,
			@RequestParam(required = false) UUID projectId, @RequestParam(required = false) ActivityAction action,
			@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
			@RequestParam(required = false) String q, @RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "30") @Min(1) @Max(100) int limit) {
		Instant fromInstant = from == null ? null : from.atStartOfDay(clock.getZone()).toInstant();
		Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
		return activity.search(new ActivityFilter(userId, projectId, action, fromInstant, toInstant, q), cursor, limit);
	}

	@GetMapping("/users")
	PageResponse<UserRowDto> users(@RequestParam(required = false) String q, @RequestParam(required = false) Role role,
			@RequestParam(required = false) UserStatus status, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return management.users(q, role, status, page, size);
	}

	@GetMapping("/users/{userId}")
	UserDetailDto user(@PathVariable UUID userId) {
		return management.user(userId);
	}

	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	UserRowDto createUser(@AuthenticationPrincipal AuthUser actor, @Valid @RequestBody CreateUserRequest request) {
		return userAdmin.create(actor, request);
	}

	@PutMapping("/users/{userId}")
	UserRowDto updateUser(@AuthenticationPrincipal AuthUser actor, @PathVariable UUID userId,
			@Valid @RequestBody UpdateUserRequest request) {
		return userAdmin.updateDetails(actor, userId, request);
	}

	@PatchMapping("/users/{userId}/role")
	UserRowDto changeRole(@AuthenticationPrincipal AuthUser actor, @PathVariable UUID userId,
			@Valid @RequestBody ChangeRoleRequest request) {
		return userAdmin.changeRole(actor, userId, request.role(), request.currentPassword());
	}

	@PatchMapping("/users/{userId}/status")
	UserRowDto changeStatus(@AuthenticationPrincipal AuthUser actor, @PathVariable UUID userId,
			@Valid @RequestBody ChangeStatusRequest request) {
		return userAdmin.changeStatus(actor, userId, request.status(), request.currentPassword());
	}

	@PostMapping("/users/{userId}/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void resetPassword(@AuthenticationPrincipal AuthUser actor, @PathVariable UUID userId,
			@Valid @RequestBody ResetPasswordRequest request) {
		userAdmin.resetPassword(actor, userId, request.newPassword(), request.confirmPassword(), request.currentPassword());
	}

	@DeleteMapping("/users/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteUser(@AuthenticationPrincipal AuthUser actor, @PathVariable UUID userId,
			@Valid @RequestBody DeleteUserRequest request) {
		userAdmin.delete(actor, userId, request.currentPassword());
	}

	@GetMapping("/projects")
	PageResponse<ProjectSummaryDto> projects(@AuthenticationPrincipal AuthUser viewer,
			@RequestParam(required = false) String q, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return projects.searchAll(viewer, q, PageRequest.of(page, size, Sort.by(Sort.Order.desc("updatedAt"))));
	}

}
