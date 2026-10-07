package com.xdsdata.taskflow.projects;

/**
 * What the current user may do in a project. The UI follows these flags; the backend enforces the same
 * rules independently.
 */
public record ProjectPermissions(boolean canManage, boolean canCreateTasks) {

}
