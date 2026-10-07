package com.xdsdata.taskflow.activity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;

/**
 * One activity entry. References are plain ids (nulled by the database when the target is deleted), and
 * {@code metadata} keeps name snapshots so the history stays readable after deletions.
 */
@Entity
@Table(name = "activity_logs")
public class ActivityLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "project_id")
	private UUID projectId;

	@Column(name = "task_id")
	private UUID taskId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ActivityAction action;

	@Column(nullable = false, length = 500)
	private String description;

	/** JSON object stored in a jsonb column. */
	@Column(columnDefinition = "jsonb")
	@ColumnTransformer(write = "?::jsonb")
	private String metadata;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ActivityLog() {
	}

	ActivityLog(UUID userId, UUID projectId, UUID taskId, ActivityAction action, String description, String metadata) {
		this.userId = userId;
		this.projectId = projectId;
		this.taskId = taskId;
		this.action = action;
		this.description = description.length() > 500 ? description.substring(0, 497) + "..." : description;
		this.metadata = metadata;
	}

	public UUID getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getProjectId() {
		return projectId;
	}

	public UUID getTaskId() {
		return taskId;
	}

	public ActivityAction getAction() {
		return action;
	}

	public String getDescription() {
		return description;
	}

	public String getMetadata() {
		return metadata;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
