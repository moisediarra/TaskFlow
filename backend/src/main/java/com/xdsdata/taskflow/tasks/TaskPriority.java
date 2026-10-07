package com.xdsdata.taskflow.tasks;

public enum TaskPriority {

	HIGH("High"), MEDIUM("Medium"), LOW("Low");

	private final String label;

	TaskPriority(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

}
