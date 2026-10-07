package com.xdsdata.taskflow.tasks;

import java.util.UUID;

import com.xdsdata.taskflow.projects.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A colored label scoped to one project (claude.md §17). */
@Entity
@Table(name = "tags")
public class Tag {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(nullable = false, length = 30)
	private String name;

	/** A palette key such as "teal" or "amber"; the client maps it to badge colors. */
	@Column(nullable = false, length = 20)
	private String color;

	protected Tag() {
	}

	public Tag(Project project, String name, String color) {
		this.project = project;
		this.name = name.trim();
		this.color = color;
	}

	public UUID getId() {
		return id;
	}

	public Project getProject() {
		return project;
	}

	public String getName() {
		return name;
	}

	public String getColor() {
		return color;
	}

}
