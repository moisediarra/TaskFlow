package com.xdsdata.taskflow.activity;

/**
 * Meaningful business actions recorded in the activity log (claude.md §28). Clicks and page views are
 * never recorded. The PROJECT_DELETED and USER_* entries audit deletions and privileged changes.
 */
public enum ActivityAction {

	PROJECT_CREATED, PROJECT_UPDATED, PROJECT_DELETED, PROJECT_MEMBER_ADDED, PROJECT_MEMBER_REMOVED,

	TASK_CREATED, TASK_UPDATED, TASK_DELETED, TASK_ASSIGNED, TASK_UNASSIGNED, TASK_STATUS_CHANGED,
	TASK_PRIORITY_CHANGED, TASK_DUE_DATE_CHANGED, TASK_COMPLETED,

	USER_ROLE_CHANGED, USER_STATUS_CHANGED

}
