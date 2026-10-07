package com.xdsdata.taskflow.common;

import java.time.LocalDate;

/**
 * How a task's deadline should be presented (claude.md §18). Computed on the server against the
 * application time zone so badges, deadline notifications and dashboard metrics always agree.
 */
public enum DueState {

	NONE, UPCOMING, DUE_TODAY, OVERDUE, COMPLETED;

	/**
	 * @param dueDate the task's due date, or {@code null} when it has none
	 * @param done whether the task is in the Done column (completed tasks are never overdue)
	 * @param today the current date in the application time zone
	 */
	public static DueState of(LocalDate dueDate, boolean done, LocalDate today) {
		if (done) {
			return COMPLETED;
		}
		if (dueDate == null) {
			return NONE;
		}
		if (dueDate.isBefore(today)) {
			return OVERDUE;
		}
		return dueDate.isEqual(today) ? DUE_TODAY : UPCOMING;
	}

}
