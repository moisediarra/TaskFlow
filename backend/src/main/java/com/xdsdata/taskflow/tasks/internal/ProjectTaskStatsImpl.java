package com.xdsdata.taskflow.tasks.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.xdsdata.taskflow.projects.ProjectTaskStats;
import com.xdsdata.taskflow.tasks.TaskStatus;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
class ProjectTaskStatsImpl implements ProjectTaskStats {

	private final TaskRepository tasks;

	ProjectTaskStatsImpl(TaskRepository tasks) {
		this.tasks = tasks;
	}

	@Override
	public Map<UUID, TaskCounts> countsFor(Collection<UUID> projectIds) {
		if (projectIds.isEmpty()) {
			return Map.of();
		}
		Map<UUID, TaskCounts> counts = new HashMap<>();
		for (Object[] row : tasks.countsByProject(projectIds, TaskStatus.ACTIVE)) {
			long total = ((Number) row[1]).longValue();
			long active = row[2] == null ? 0 : ((Number) row[2]).longValue();
			counts.put((UUID) row[0], new TaskCounts(total, active));
		}
		return counts;
	}

}
