package com.xdsdata.taskflow.management.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.xdsdata.taskflow.activity.ActivityService;
import com.xdsdata.taskflow.common.AppProperties;
import com.xdsdata.taskflow.common.DueState;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.common.web.PageResponse;
import com.xdsdata.taskflow.common.web.SearchText;
import com.xdsdata.taskflow.management.internal.ManagementDtos.MembershipDto;
import com.xdsdata.taskflow.management.internal.ManagementDtos.OverviewDto;
import com.xdsdata.taskflow.management.internal.ManagementDtos.PriorityCount;
import com.xdsdata.taskflow.management.internal.ManagementDtos.ProjectStats;
import com.xdsdata.taskflow.management.internal.ManagementDtos.TaskStats;
import com.xdsdata.taskflow.management.internal.ManagementDtos.UserDetailDto;
import com.xdsdata.taskflow.management.internal.ManagementDtos.UserRowDto;
import com.xdsdata.taskflow.management.internal.ManagementDtos.UserStats;
import com.xdsdata.taskflow.management.internal.ManagementDtos.WorkItemDto;
import com.xdsdata.taskflow.management.internal.ManagementDtos.WorkloadDto;
import com.xdsdata.taskflow.management.internal.ManagementDtos.WorkloadRow;
import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.tasks.TaskPriority;
import com.xdsdata.taskflow.tasks.TaskStatus;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;
import com.xdsdata.taskflow.users.UserStatus;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read models for the IT Manager (claude.md §24–31). Only work-related data is exposed: assignments, status,
 * deadlines, workload and business activity (claude.md §33). Callers must be IT Managers.
 */
@Service
@Transactional(readOnly = true)
class ManagementService {

	private static final Limit WIDGET_LIMIT = Limit.of(8);

	private final ManagementTaskQueries tasks;

	private final ManagementUserQueries people;

	private final UserService users;

	private final ActivityService activity;

	private final AppProperties properties;

	private final Clock clock;

	ManagementService(ManagementTaskQueries tasks, ManagementUserQueries people, UserService users,
			ActivityService activity, AppProperties properties, Clock clock) {
		this.tasks = tasks;
		this.people = people;
		this.users = users;
		this.activity = activity;
		this.properties = properties;
		this.clock = clock;
	}

	OverviewDto overview() {
		LocalDate today = today();
		Map<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
		for (Object[] row : tasks.countByStatus()) {
			byStatus.put((TaskStatus) row[0], (Long) row[1]);
		}
		long backlog = byStatus.getOrDefault(TaskStatus.BACKLOG, 0L);
		long todo = byStatus.getOrDefault(TaskStatus.TODO, 0L);
		long inProgress = byStatus.getOrDefault(TaskStatus.IN_PROGRESS, 0L);
		long done = byStatus.getOrDefault(TaskStatus.DONE, 0L);

		Map<TaskPriority, Long> byPriority = new EnumMap<>(TaskPriority.class);
		for (Object[] row : tasks.countOpenByPriority(TaskStatus.DONE)) {
			byPriority.put((TaskPriority) row[0], (Long) row[1]);
		}
		List<PriorityCount> openByPriority = Arrays.stream(TaskPriority.values())
			.map(priority -> new PriorityCount(priority, byPriority.getOrDefault(priority, 0L)))
			.toList();

		TaskStats taskStats = new TaskStats(backlog + todo + inProgress + done, backlog, todo, inProgress, done,
				todo + inProgress, tasks.countOverdue(TaskStatus.DONE, today),
				byPriority.getOrDefault(TaskPriority.HIGH, 0L));
		WorkloadDto workload = workload();
		return new OverviewDto(
				new UserStats(people.countUsers(), people.countUsersWithStatus(UserStatus.ACTIVE)),
				new ProjectStats(people.countProjects(), tasks.countProjectsWithTasksIn(TaskStatus.ACTIVE)), taskStats,
				openByPriority,
				workItems(tasks.findAssignedWithStatus(TaskStatus.IN_PROGRESS, WIDGET_LIMIT), today),
				workItems(tasks.findOverdue(TaskStatus.DONE, today, WIDGET_LIMIT), today),
				workload.rows().stream().limit(8).toList(), workload.warnings(), activity.recent(10));
	}

	/**
	 * Assigned tasks with their person and project, most recently updated first. Without a status filter only
	 * active work (To Do and In Progress) is shown.
	 */
	PageResponse<WorkItemDto> teamActivity(TeamActivityFilter filter, int page, int size) {
		LocalDate today = today();
		Set<TaskStatus> statuses = filter.statuses() == null || filter.statuses().isEmpty() ? TaskStatus.ACTIVE
				: Set.copyOf(filter.statuses());
		Specification<Task> specification = (root, query, cb) -> {
			boolean counting = query.getResultType() == Long.class || query.getResultType() == long.class;
			if (!counting) {
				root.fetch("assignee");
				root.fetch("project");
			}
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.isNotNull(root.get("assignee")));
			predicates.add(root.get("status").in(statuses));
			if (filter.userId() != null) {
				predicates.add(cb.equal(root.get("assignee").get("id"), filter.userId()));
			}
			if (filter.projectId() != null) {
				predicates.add(cb.equal(root.get("project").get("id"), filter.projectId()));
			}
			if (filter.priority() != null) {
				predicates.add(cb.equal(root.get("priority"), filter.priority()));
			}
			if (filter.due() != null) {
				switch (filter.due()) {
					case OVERDUE -> {
						predicates.add(cb.lessThan(root.<LocalDate>get("dueDate"), today));
						predicates.add(cb.notEqual(root.get("status"), TaskStatus.DONE));
					}
					case TODAY -> predicates.add(cb.equal(root.get("dueDate"), today));
					case THIS_WEEK -> predicates.add(cb.between(root.<LocalDate>get("dueDate"), today, today.plusDays(6)));
					case NONE -> predicates.add(cb.isNull(root.get("dueDate")));
					case ANY -> {
					}
				}
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
		Page<Task> result = tasks.findAll(specification,
				PageRequest.of(page, size, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.asc("id"))));
		return PageResponse.of(result, task -> workItem(task, today));
	}

	/** Active tasks per person, including people with none: they are part of the balance picture. */
	WorkloadDto workload() {
		LocalDate today = today();
		int activeWarning = properties.workload().activeTaskWarning();
		int overdueWarning = properties.workload().overdueTaskWarning();
		Map<UUID, Map<TaskStatus, Long>> activeCounts = new HashMap<>();
		for (Object[] row : tasks.countAssignedByStatus(TaskStatus.ACTIVE)) {
			activeCounts.computeIfAbsent((UUID) row[0], id -> new EnumMap<>(TaskStatus.class))
				.put((TaskStatus) row[1], (Long) row[2]);
		}
		Map<UUID, Long> overdueCounts = new HashMap<>();
		for (Object[] row : tasks.countOverdueByAssignee(TaskStatus.DONE, today)) {
			overdueCounts.put((UUID) row[0], (Long) row[1]);
		}
		List<WorkloadRow> rows = people.findWithMembership(UserStatus.ACTIVE).stream().map(user -> {
			Map<TaskStatus, Long> counts = activeCounts.getOrDefault(user.getId(), Map.of());
			long todo = counts.getOrDefault(TaskStatus.TODO, 0L);
			long inProgress = counts.getOrDefault(TaskStatus.IN_PROGRESS, 0L);
			long overdue = overdueCounts.getOrDefault(user.getId(), 0L);
			long active = todo + inProgress;
			return new WorkloadRow(user.getId(), user.getName(), user.getJobTitle(), active, inProgress, todo, overdue,
					active >= activeWarning, overdue >= overdueWarning);
		})
			.sorted(Comparator.comparingLong(WorkloadRow::active)
				.reversed()
				.thenComparing(WorkloadRow::name, String.CASE_INSENSITIVE_ORDER))
			.toList();
		List<String> warnings = new ArrayList<>();
		for (WorkloadRow row : rows) {
			if (row.manyActive()) {
				warnings.add("%s has %d active tasks.".formatted(row.name(), row.active()));
			}
			if (row.manyOverdue()) {
				warnings.add("%s has %d overdue tasks.".formatted(row.name(), row.overdue()));
			}
		}
		long scaleMax = Math.max(10, rows.stream().mapToLong(WorkloadRow::active).max().orElse(0));
		return new WorkloadDto(rows, warnings, activeWarning, overdueWarning, scaleMax);
	}

	PageResponse<UserRowDto> users(String query, Role role, UserStatus status, int page, int size) {
		String text = SearchText.normalize(query);
		Specification<User> specification = (root, criteria, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (!text.isEmpty()) {
				HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;
				String pattern = SearchText.containsPattern(text);
				predicates.add(cb.or(hcb.ilike(root.get("name"), pattern), hcb.ilike(root.get("email"), pattern)));
			}
			if (role != null) {
				predicates.add(cb.equal(root.get("role"), role));
			}
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
		Page<User> result = people.findAll(specification,
				PageRequest.of(page, size, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))));
		Map<UUID, long[]> counts = countsFor(result.getContent().stream().map(User::getId).toList());
		return PageResponse.of(result, user -> row(user, counts.getOrDefault(user.getId(), new long[2])));
	}

	UserDetailDto user(UUID userId) {
		User user = users.getById(userId);
		LocalDate today = today();
		List<MembershipDto> memberships = people.findMemberships(userId)
			.stream()
			.map(member -> new MembershipDto(member.getProject().getId(), member.getProject().getName(),
					member.getRole(), member.getJoinedAt()))
			.toList();
		List<WorkItemDto> assigned = tasks.findAssignedTo(userId, TaskStatus.DONE, Limit.of(50))
			.stream()
			.map(task -> workItem(task, today))
			.toList();
		long overdue = assigned.stream().filter(item -> item.dueState() == DueState.OVERDUE).count();
		return new UserDetailDto(row(user, countsFor(List.of(userId)).getOrDefault(userId, new long[2])),
				people.countOwnedProjects(userId), overdue, memberships, assigned);
	}

	UserRowDto row(User user) {
		return row(user, countsFor(List.of(user.getId())).getOrDefault(user.getId(), new long[2]));
	}

	long ownedProjectCount(UUID userId) {
		return people.countOwnedProjects(userId);
	}

	private UserRowDto row(User user, long[] counts) {
		return new UserRowDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus(),
				user.getJobTitle(), user.getCreatedAt(), counts[0], counts[1]);
	}

	/** [project count, active task count] per user id. */
	private Map<UUID, long[]> countsFor(Collection<UUID> userIds) {
		Map<UUID, long[]> counts = new HashMap<>();
		if (userIds.isEmpty()) {
			return counts;
		}
		for (Object[] row : people.countMemberships(userIds)) {
			counts.computeIfAbsent((UUID) row[0], id -> new long[2])[0] = (Long) row[1];
		}
		for (Object[] row : tasks.countForAssignees(userIds, TaskStatus.ACTIVE)) {
			counts.computeIfAbsent((UUID) row[0], id -> new long[2])[1] = (Long) row[1];
		}
		return counts;
	}

	private List<WorkItemDto> workItems(List<Task> list, LocalDate today) {
		return list.stream().map(task -> workItem(task, today)).toList();
	}

	private WorkItemDto workItem(Task task, LocalDate today) {
		User assignee = task.getAssignee();
		return new WorkItemDto(task.getId(), task.getTitle(), task.getStatus(), task.getPriority(), task.getDueDate(),
				DueState.of(task.getDueDate(), task.isDone(), today), task.getProject().getId(),
				task.getProject().getName(), assignee == null ? null : assignee.getId(),
				assignee == null ? null : assignee.getName(), assignee == null ? null : assignee.getJobTitle(),
				task.getUpdatedAt());
	}

	private LocalDate today() {
		return LocalDate.now(clock);
	}

	enum DueFilter {

		ANY, OVERDUE, TODAY, THIS_WEEK, NONE

	}

	record TeamActivityFilter(UUID userId, UUID projectId, List<TaskStatus> statuses, TaskPriority priority,
			DueFilter due) {
	}

}
