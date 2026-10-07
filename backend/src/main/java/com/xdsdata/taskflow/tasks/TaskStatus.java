package com.xdsdata.taskflow.tasks;

import java.util.Set;

/** The four fixed Kanban columns (claude.md §9). */
public enum TaskStatus {

	BACKLOG("Backlog"), TODO("To Do"), IN_PROGRESS("In Progress"), DONE("Done");

	/**
	 * Statuses that count as "active" work for workload and project cards. Backlog items are identified
	 * but not yet ready to be worked on (claude.md §10), so they are not active.
	 */
	public static final Set<TaskStatus> ACTIVE = Set.of(TODO, IN_PROGRESS);

	/** Everything except Done. */
	public static final Set<TaskStatus> OPEN = Set.of(BACKLOG, TODO, IN_PROGRESS);

	private final String label;

	TaskStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public boolean isActive() {
		return ACTIVE.contains(this);
	}

}
