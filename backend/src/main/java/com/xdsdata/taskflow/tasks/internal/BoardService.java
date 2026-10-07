package com.xdsdata.taskflow.tasks.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.projects.Project;
import com.xdsdata.taskflow.projects.ProjectAccess;
import com.xdsdata.taskflow.projects.ProjectService;
import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.tasks.TaskDtos.BoardColumnDto;
import com.xdsdata.taskflow.tasks.TaskDtos.BoardDto;
import com.xdsdata.taskflow.tasks.TaskDtos.BoardProjectDto;
import com.xdsdata.taskflow.tasks.TaskDtos.TagDto;
import com.xdsdata.taskflow.tasks.TaskDtos.TaskCardDto;
import com.xdsdata.taskflow.tasks.TaskDtos.TaskSliceDto;
import com.xdsdata.taskflow.tasks.TaskStatus;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the Kanban board in a few queries: every open card, the first page of Done (which grows without
 * bound and is paged with "Show more"), column totals, members and tags.
 */
@Service
@Transactional(readOnly = true)
class BoardService {

	static final int COLUMN_PAGE_SIZE = 50;

	private final TaskRepository tasks;

	private final TagRepository tags;

	private final ProjectAccess projectAccess;

	private final ProjectService projectService;

	private final TaskMapper mapper;

	BoardService(TaskRepository tasks, TagRepository tags, ProjectAccess projectAccess, ProjectService projectService,
			TaskMapper mapper) {
		this.tasks = tasks;
		this.tags = tags;
		this.projectAccess = projectAccess;
		this.projectService = projectService;
		this.mapper = mapper;
	}

	BoardDto board(AuthUser user, UUID projectId) {
		Project project = projectAccess.requireReadable(user, projectId);
		boolean owner = projectAccess.isOwner(user, project);
		boolean member = owner || projectAccess.isMember(projectId, user.id());
		LocalDate today = mapper.today();

		Map<TaskStatus, List<TaskCardDto>> cards = new EnumMap<>(TaskStatus.class);
		for (Task task : tasks.findBoardTasks(projectId, TaskStatus.OPEN)) {
			cards.computeIfAbsent(task.getStatus(), status -> new ArrayList<>())
				.add(mapper.card(task, TaskAccess.permissions(user, task, owner, member), today));
		}
		cards.put(TaskStatus.DONE, columnPage(projectId, TaskStatus.DONE, 0, COLUMN_PAGE_SIZE).stream()
			.map(task -> mapper.card(task, TaskAccess.permissions(user, task, owner, member), today))
			.toList());

		Map<TaskStatus, Long> totals = totals(projectId);
		List<BoardColumnDto> columns = Arrays.stream(TaskStatus.values())
			.map(status -> new BoardColumnDto(status, status.label(), cards.getOrDefault(status, List.of()),
					totals.getOrDefault(status, 0L)))
			.toList();

		return new BoardDto(
				new BoardProjectDto(project.getId(), project.getName(), project.getDescription(), project.getOwnerId(),
						project.getOwner().getName()),
				projectAccess.permissionsFor(user, project), projectService.membersOf(projectId),
				tags.findByProject(projectId).stream().map(TagDto::from).toList(), columns);
	}

	TaskSliceDto column(AuthUser user, UUID projectId, TaskStatus status, int page, int size) {
		Project project = projectAccess.requireReadable(user, projectId);
		boolean owner = projectAccess.isOwner(user, project);
		boolean member = owner || projectAccess.isMember(projectId, user.id());
		LocalDate today = mapper.today();
		List<TaskCardDto> items = columnPage(projectId, status, page, size).stream()
			.map(task -> mapper.card(task, TaskAccess.permissions(user, task, owner, member), today))
			.toList();
		long total = totals(projectId).getOrDefault(status, 0L);
		return new TaskSliceDto(items, (long) (page + 1) * size < total);
	}

	private List<Task> columnPage(UUID projectId, TaskStatus status, int page, int size) {
		List<UUID> ids = tasks.findColumnPage(projectId, status, PageRequest.of(page, size));
		if (ids.isEmpty()) {
			return List.of();
		}
		Map<UUID, Task> byId = tasks.findWithDetails(ids)
			.stream()
			.collect(Collectors.toMap(Task::getId, Function.identity(), (a, b) -> a));
		return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
	}

	private Map<TaskStatus, Long> totals(UUID projectId) {
		Map<TaskStatus, Long> totals = new EnumMap<>(TaskStatus.class);
		for (Object[] row : tasks.countByStatus(projectId)) {
			totals.put((TaskStatus) row[0], (Long) row[1]);
		}
		return totals;
	}

}
