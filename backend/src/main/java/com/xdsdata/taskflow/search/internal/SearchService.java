package com.xdsdata.taskflow.search.internal;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.xdsdata.taskflow.activity.ActivityDto;
import com.xdsdata.taskflow.activity.ActivityFilter;
import com.xdsdata.taskflow.activity.ActivityService;
import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.common.web.SearchText;
import com.xdsdata.taskflow.tasks.Tag;
import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.tasks.TaskPriority;
import com.xdsdata.taskflow.tasks.TaskStatus;
import com.xdsdata.taskflow.users.UserStatus;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task search for everyone (title, description, tags) and the IT Manager's global search, which also covers
 * users, projects and activity logs (claude.md §19).
 */
@Service
@Transactional(readOnly = true)
class SearchService {

	static final int MIN_QUERY_LENGTH = 2;

	private final SearchQueries queries;

	private final ActivityService activity;

	SearchService(SearchQueries queries, ActivityService activity) {
		this.queries = queries;
		this.activity = activity;
	}

	SearchResults search(AuthUser user, String rawQuery) {
		String query = SearchText.normalize(rawQuery);
		boolean global = user.isItManager();
		if (query.length() < MIN_QUERY_LENGTH) {
			return new SearchResults(query, List.of(), global ? List.of() : null, global ? List.of() : null,
					global ? List.of() : null);
		}
		String pattern = SearchText.containsPattern(query);
		List<Task> found = global ? queries.searchAllTasks(pattern, Limit.of(10))
				: queries.searchTasksInMemberProjects(pattern, user.id(), Limit.of(10));
		List<TaskHit> tasks = taskHits(found);
		if (!global) {
			return new SearchResults(query, tasks, null, null, null);
		}
		List<UserHit> users = queries.searchUsers(pattern, Limit.of(5))
			.stream()
			.map(u -> new UserHit(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getStatus(), u.getJobTitle()))
			.toList();
		List<ProjectHit> projects = queries.searchProjects(pattern, Limit.of(5))
			.stream()
			.map(p -> new ProjectHit(p.getId(), p.getName(), p.getDescription()))
			.toList();
		List<ActivityDto> activities = activity
			.search(new ActivityFilter(null, null, null, null, null, query), null, 5)
			.items();
		return new SearchResults(query, tasks, users, projects, activities);
	}

	private List<TaskHit> taskHits(List<Task> found) {
		if (found.isEmpty()) {
			return List.of();
		}
		Map<UUID, Task> withTags = queries.findWithTags(found.stream().map(Task::getId).toList())
			.stream()
			.collect(Collectors.toMap(Task::getId, Function.identity(), (a, b) -> a));
		return found.stream().map(task -> {
			List<String> tags = withTags.getOrDefault(task.getId(), task)
				.getTags()
				.stream()
				.map(Tag::getName)
				.sorted(Comparator.comparing(String::toLowerCase))
				.toList();
			return new TaskHit(task.getId(), task.getTitle(), task.getProject().getId(), task.getProject().getName(),
					task.getStatus(), task.getPriority(), tags);
		}).toList();
	}

	record TaskHit(UUID id, String title, UUID projectId, String projectName, TaskStatus status, TaskPriority priority,
			List<String> tags) {
	}

	record UserHit(UUID id, String name, String email, Role role, UserStatus status, String jobTitle) {
	}

	record ProjectHit(UUID id, String name, String description) {
	}

	/** {@code users}, {@code projects} and {@code activities} are null for anyone but IT Managers. */
	record SearchResults(String query, List<TaskHit> tasks, List<UserHit> users, List<ProjectHit> projects,
			List<ActivityDto> activities) {
	}

}
