package com.xdsdata.taskflow.projects;

import java.time.Clock;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.error.BadRequestException;
import com.xdsdata.taskflow.common.error.ConflictException;
import com.xdsdata.taskflow.common.error.ForbiddenException;
import com.xdsdata.taskflow.common.error.NotFoundException;
import com.xdsdata.taskflow.common.web.PageResponse;
import com.xdsdata.taskflow.common.web.SearchText;
import com.xdsdata.taskflow.projects.ProjectDtos.ProjectDetailDto;
import com.xdsdata.taskflow.projects.ProjectDtos.ProjectMemberDto;
import com.xdsdata.taskflow.projects.ProjectDtos.ProjectSummaryDto;
import com.xdsdata.taskflow.projects.ProjectEvents.ProjectCreated;
import com.xdsdata.taskflow.projects.ProjectEvents.ProjectDeleted;
import com.xdsdata.taskflow.projects.ProjectEvents.ProjectMemberAdded;
import com.xdsdata.taskflow.projects.ProjectEvents.ProjectMemberRemoved;
import com.xdsdata.taskflow.projects.ProjectEvents.ProjectUpdated;
import com.xdsdata.taskflow.projects.ProjectTaskStats.TaskCounts;
import com.xdsdata.taskflow.projects.internal.ProjectMemberRepository;
import com.xdsdata.taskflow.projects.internal.ProjectRepository;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;
import com.xdsdata.taskflow.users.UserSummary;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProjectService {

	private static final String NO_ACCOUNT = "No active TaskFlow account uses this email.";

	private final ProjectRepository projects;

	private final ProjectMemberRepository members;

	private final ProjectAccess access;

	private final UserService users;

	private final ObjectProvider<ProjectTaskStats> taskStats;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	ProjectService(ProjectRepository projects, ProjectMemberRepository members, ProjectAccess access,
			UserService users, ObjectProvider<ProjectTaskStats> taskStats, ApplicationEventPublisher events,
			Clock clock) {
		this.projects = projects;
		this.members = members;
		this.access = access;
		this.users = users;
		this.taskStats = taskStats;
		this.events = events;
		this.clock = clock;
	}

	/** Projects the user belongs to, most recently updated first. */
	public List<ProjectSummaryDto> listMine(AuthUser user) {
		List<ProjectMember> memberships = members.findByUserWithProject(user.id());
		Map<UUID, ProjectRole> roles = memberships.stream()
			.collect(Collectors.toMap(m -> m.getProject().getId(), ProjectMember::getRole));
		List<Project> mine = memberships.stream()
			.map(ProjectMember::getProject)
			.sorted(Comparator.comparing(Project::getUpdatedAt).reversed())
			.toList();
		return summaries(mine, user, roles);
	}

	/** Every project, for the IT Manager's monitoring table; callers enforce the IT Manager role. */
	public PageResponse<ProjectSummaryDto> searchAll(AuthUser viewer, String query, Pageable pageable) {
		Page<Project> page = projects.searchByName(SearchText.containsPattern(query), pageable);
		Map<UUID, ProjectRole> roles = members.findByUserWithProject(viewer.id())
			.stream()
			.collect(Collectors.toMap(m -> m.getProject().getId(), ProjectMember::getRole));
		Map<UUID, ProjectSummaryDto> byId = summaries(page.getContent(), viewer, roles).stream()
			.collect(Collectors.toMap(ProjectSummaryDto::id, Function.identity()));
		return PageResponse.of(page, project -> byId.get(project.getId()));
	}

	public ProjectDetailDto get(AuthUser user, UUID projectId) {
		return detail(access.requireReadable(user, projectId), user);
	}

	@Transactional
	public ProjectDetailDto create(AuthUser actor, String name, String description) {
		if (!actor.canCreateProjects()) {
			throw new ForbiddenException("Your role doesn't allow creating projects. Please ask your IT Manager.");
		}
		User owner = users.getById(actor.id());
		Project project = projects.saveAndFlush(new Project(name, description, owner));
		members.saveAndFlush(new ProjectMember(project, owner, ProjectRole.OWNER));
		events.publishEvent(new ProjectCreated(actor, project.getId(), project.getName()));
		return detail(project, actor);
	}

	@Transactional
	public ProjectDetailDto update(AuthUser actor, UUID projectId, String name, String description) {
		Project project = access.requireManageable(actor, projectId);
		String previousName = project.getName();
		Set<String> changed = new LinkedHashSet<>();
		if (!previousName.equals(name.trim())) {
			changed.add("name");
		}
		String newDescription = description == null || description.isBlank() ? null : description.trim();
		if (!Objects.equals(project.getDescription(), newDescription)) {
			changed.add("description");
		}
		if (!changed.isEmpty()) {
			project.update(name, description);
			projects.flush();
			events.publishEvent(new ProjectUpdated(actor, project.getId(), project.getName(), previousName, changed));
		}
		return detail(project, actor);
	}

	/** Deletes the project with its tasks, members and tags; the activity history is kept. */
	@Transactional
	public void delete(AuthUser actor, UUID projectId) {
		Project project = access.requireManageable(actor, projectId);
		events.publishEvent(new ProjectDeleted(actor, project.getId(), project.getName()));
		projects.delete(project);
	}

	public List<ProjectMemberDto> members(AuthUser actor, UUID projectId) {
		access.requireReadable(actor, projectId);
		return membersOf(projectId);
	}

	/** Members of a project, owner first. Callers must have checked read access. */
	public List<ProjectMemberDto> membersOf(UUID projectId) {
		return members.findByProjectWithUser(projectId)
			.stream()
			.sorted(Comparator.comparing((ProjectMember m) -> m.getRole() == ProjectRole.OWNER ? 0 : 1)
				.thenComparing(m -> m.getUser().getName(), String.CASE_INSENSITIVE_ORDER))
			.map(ProjectMemberDto::from)
			.toList();
	}

	/** Members are added by exact email so the user directory cannot be browsed. */
	@Transactional
	public ProjectMemberDto addMember(AuthUser actor, UUID projectId, String email) {
		Project project = access.requireManageable(actor, projectId);
		User user = users.findActiveByEmail(email)
			.orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", NO_ACCOUNT, Map.of("email", NO_ACCOUNT)));
		if (members.existsByProjectIdAndUserId(projectId, user.getId())) {
			throw new ConflictException("ALREADY_MEMBER", user.getName() + " is already a member of this project.",
					"email");
		}
		ProjectMember member = members.saveAndFlush(new ProjectMember(project, user, ProjectRole.MEMBER));
		touch(projectId);
		events.publishEvent(
				new ProjectMemberAdded(actor, projectId, project.getName(), user.getId(), user.getName()));
		return ProjectMemberDto.from(member);
	}

	/** Removing a member also unassigns their tasks in the project (handled by the tasks module). */
	@Transactional
	public void removeMember(AuthUser actor, UUID projectId, UUID userId) {
		Project project = access.requireManageable(actor, projectId);
		ProjectMember member = members.findMembership(projectId, userId)
			.orElseThrow(() -> new NotFoundException("Member"));
		if (member.getRole() == ProjectRole.OWNER) {
			throw new BadRequestException("CANNOT_REMOVE_OWNER", "The project owner can't be removed.");
		}
		events.publishEvent(new ProjectMemberRemoved(actor, projectId, project.getName(), userId,
				member.getUser().getName()));
		members.delete(member);
		touch(projectId);
	}

	@Transactional
	public void touch(UUID projectId) {
		projects.touch(projectId, clock.instant());
	}

	public List<UUID> memberProjectIds(UUID userId) {
		return members.findProjectIdsByUserId(userId);
	}

	public boolean anyProjectExists() {
		return projects.count() > 0;
	}

	private ProjectDetailDto detail(Project project, AuthUser viewer) {
		long memberCount = countMembers(List.of(project.getId())).getOrDefault(project.getId(), 0L);
		ProjectRole myRole = members.findMembership(project.getId(), viewer.id()).map(ProjectMember::getRole).orElse(null);
		return new ProjectDetailDto(project.getId(), project.getName(), project.getDescription(),
				UserSummary.from(project.getOwner()), memberCount, project.getCreatedAt(), project.getUpdatedAt(), myRole,
				access.permissionsFor(viewer, project));
	}

	private List<ProjectSummaryDto> summaries(List<Project> list, AuthUser viewer, Map<UUID, ProjectRole> roles) {
		if (list.isEmpty()) {
			return List.of();
		}
		List<UUID> ids = list.stream().map(Project::getId).toList();
		Map<UUID, Long> memberCounts = countMembers(ids);
		ProjectTaskStats stats = taskStats.getIfAvailable(() -> projectIds -> Map.of());
		Map<UUID, TaskCounts> taskCounts = stats.countsFor(ids);
		return list.stream().map(project -> {
			TaskCounts counts = taskCounts.getOrDefault(project.getId(), TaskCounts.EMPTY);
			return new ProjectSummaryDto(project.getId(), project.getName(), project.getDescription(),
					UserSummary.from(project.getOwner()), memberCounts.getOrDefault(project.getId(), 0L), counts.active(),
					counts.total(), project.getUpdatedAt(), roles.get(project.getId()), access.isOwner(viewer, project));
		}).toList();
	}

	private Map<UUID, Long> countMembers(Collection<UUID> projectIds) {
		Map<UUID, Long> counts = new HashMap<>();
		for (Object[] row : members.countByProjectIds(projectIds)) {
			counts.put((UUID) row[0], (Long) row[1]);
		}
		return counts;
	}

}
