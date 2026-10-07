package com.xdsdata.taskflow.projects;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Task counts shown on project cards. Declared here and implemented by the tasks module, which keeps the
 * module dependencies one-way (tasks depends on projects, never the reverse).
 */
public interface ProjectTaskStats {

	Map<UUID, TaskCounts> countsFor(Collection<UUID> projectIds);

	record TaskCounts(long total, long active) {

		public static final TaskCounts EMPTY = new TaskCounts(0, 0);

	}

}
