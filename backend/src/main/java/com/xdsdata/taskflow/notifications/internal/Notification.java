package com.xdsdata.taskflow.notifications.internal;

import java.time.Instant;
import java.util.UUID;

import com.xdsdata.taskflow.notifications.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "task_id")
	private UUID taskId;

	@Column(name = "project_id")
	private UUID projectId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private NotificationType type;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 500)
	private String message;

	@Column(name = "is_read", nullable = false)
	private boolean read;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** Set for notifications that must be sent at most once (deadline reminders). */
	@Column(name = "dedupe_key", length = 200, unique = true)
	private String dedupeKey;

	protected Notification() {
	}

	Notification(UUID userId, NotificationType type, String title, String message, UUID taskId, UUID projectId) {
		this.userId = userId;
		this.type = type;
		this.title = title;
		this.message = message;
		this.taskId = taskId;
		this.projectId = projectId;
	}

	void markRead() {
		this.read = true;
	}

	UUID getId() {
		return id;
	}

	UUID getUserId() {
		return userId;
	}

	UUID getTaskId() {
		return taskId;
	}

	UUID getProjectId() {
		return projectId;
	}

	NotificationType getType() {
		return type;
	}

	String getTitle() {
		return title;
	}

	String getMessage() {
		return message;
	}

	boolean isRead() {
		return read;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

}
