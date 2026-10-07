package com.xdsdata.taskflow.common;

/**
 * Global role of a user (claude.md §3). Project-level ownership is tracked separately on project memberships.
 */
public enum Role {

	IT_MANAGER("IT Manager"),
	PROJECT_OWNER("Project Owner"),
	MEMBER("Member");

	private final String label;

	Role(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

}
