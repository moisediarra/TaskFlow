package com.xdsdata.taskflow.activity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.xdsdata.taskflow.activity.internal.ActivityRepository;
import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.web.Cursor;
import com.xdsdata.taskflow.common.web.CursorPage;
import com.xdsdata.taskflow.common.web.SearchText;
import com.xdsdata.taskflow.projects.ProjectAccess;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public API of the activity module: records entries (called from event listeners and privileged
 * operations) and serves the cursor-paginated feeds.
 */
@Service
@Transactional(readOnly = true)
public class ActivityService {

	private static final int MAX_PAGE_SIZE = 100;

	private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
	};

	private final ActivityRepository logs;

	private final ProjectAccess projectAccess;

	private final ObjectMapper json;

	ActivityService(ActivityRepository logs, ProjectAccess projectAccess, ObjectMapper json) {
		this.logs = logs;
		this.projectAccess = projectAccess;
		this.json = json;
	}

	/**
	 * Records an entry in the caller's transaction, so it commits or rolls back with the change itself.
	 * The actor's name is snapshotted into the metadata.
	 */
	@Transactional
	public void record(AuthUser actor, ActivityAction action, UUID projectId, UUID taskId, String description,
			Map<String, ?> metadata) {
		Map<String, Object> data = new LinkedHashMap<>();
		if (actor != null) {
			data.put("actorName", actor.name());
		}
		if (metadata != null) {
			data.putAll(metadata);
		}
		logs.save(new ActivityLog(actor == null ? null : actor.id(), projectId, taskId, action, description,
				json.writeValueAsString(data)));
	}

	/** Activity of one project, for its members and IT Managers. */
	public CursorPage<ActivityDto> projectFeed(AuthUser user, UUID projectId, String cursor, int limit) {
		projectAccess.requireReadable(user, projectId);
		return search(new ActivityFilter(null, projectId, null, null, null, null), cursor, limit);
	}

	/** Global, filterable log. Callers must restrict this to IT Managers. */
	public CursorPage<ActivityDto> search(ActivityFilter filter, String cursor, int limit) {
		int size = Math.clamp(limit, 1, MAX_PAGE_SIZE);
		Cursor position = Cursor.decode(cursor);
		Specification<ActivityLog> specification = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (filter.userId() != null) {
				predicates.add(cb.equal(root.get("userId"), filter.userId()));
			}
			if (filter.projectId() != null) {
				predicates.add(cb.equal(root.get("projectId"), filter.projectId()));
			}
			if (filter.action() != null) {
				predicates.add(cb.equal(root.get("action"), filter.action()));
			}
			if (filter.from() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), filter.from()));
			}
			if (filter.to() != null) {
				predicates.add(cb.lessThan(root.<Instant>get("createdAt"), filter.to()));
			}
			String text = SearchText.normalize(filter.query());
			if (!text.isEmpty()) {
				predicates.add(((HibernateCriteriaBuilder) cb).ilike(root.get("description"), SearchText.containsPattern(text)));
			}
			if (position != null) {
				predicates.add(cb.or(cb.lessThan(root.<Instant>get("createdAt"), position.createdAt()),
						cb.and(cb.equal(root.get("createdAt"), position.createdAt()),
								cb.lessThan(root.<UUID>get("id"), position.id()))));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
		List<ActivityLog> rows = logs.findBy(specification,
				query -> query.sortBy(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))).limit(size + 1).all());
		boolean hasMore = rows.size() > size;
		List<ActivityLog> page = hasMore ? rows.subList(0, size) : rows;
		String nextCursor = hasMore ? new Cursor(page.getLast().getCreatedAt(), page.getLast().getId()).encode() : null;
		return new CursorPage<>(page.stream().map(this::toDto).toList(), nextCursor);
	}

	public List<ActivityDto> recent(int limit) {
		return search(ActivityFilter.none(), null, limit).items();
	}

	public List<ActivityDto> recentForProject(UUID projectId, int limit) {
		return search(new ActivityFilter(null, projectId, null, null, null, null), null, limit).items();
	}

	private ActivityDto toDto(ActivityLog log) {
		Map<String, Object> metadata = log.getMetadata() == null ? Map.of() : json.readValue(log.getMetadata(), METADATA_TYPE);
		return new ActivityDto(log.getId(), log.getAction(), log.getDescription(), log.getUserId(),
				(String) metadata.get("actorName"), log.getProjectId(), (String) metadata.get("projectName"),
				log.getTaskId(), (String) metadata.get("taskTitle"), metadata, log.getCreatedAt());
	}

}
