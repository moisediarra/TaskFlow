package com.xdsdata.taskflow.projects;

import java.time.Instant;
import java.util.UUID;

import com.xdsdata.taskflow.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "project_members")
public class ProjectMember {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ProjectRole role;

	@CreationTimestamp
	@Column(name = "joined_at", nullable = false, updatable = false)
	private Instant joinedAt;

	protected ProjectMember() {
	}

	public ProjectMember(Project project, User user, ProjectRole role) {
		this.project = project;
		this.user = user;
		this.role = role;
	}

	public UUID getId() {
		return id;
	}

	public Project getProject() {
		return project;
	}

	public User getUser() {
		return user;
	}

	public ProjectRole getRole() {
		return role;
	}

	public Instant getJoinedAt() {
		return joinedAt;
	}

}
