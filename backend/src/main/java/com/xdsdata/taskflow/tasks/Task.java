package com.xdsdata.taskflow.tasks;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.xdsdata.taskflow.projects.Project;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tasks")
@DynamicUpdate
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 5000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TaskStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private TaskPriority priority;

	@Column(name = "due_date")
	private LocalDate dueDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	private User assignee;

	/** Order within the column; cards sort by (position, id). */
	@Column(nullable = false)
	private double position;

	@ManyToMany
	@JoinTable(name = "task_tags", joinColumns = @JoinColumn(name = "task_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id"))
	private Set<Tag> tags = new HashSet<>();

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Task() {
	}

	public Task(Project project, String title, String description, TaskStatus status, TaskPriority priority,
			LocalDate dueDate, User assignee, double position, Set<Tag> tags) {
		this.project = project;
		this.title = title.trim();
		this.description = blankToNull(description);
		this.status = status;
		this.priority = priority;
		this.dueDate = dueDate;
		this.assignee = assignee;
		this.position = position;
		this.tags = new HashSet<>(tags);
	}

	/**
	 * Applies an edit of the detail fields and reports which ones actually changed.
	 */
	public Set<TaskField> applyDetails(String title, String description, TaskPriority priority, LocalDate dueDate,
			Set<Tag> tags) {
		Set<TaskField> changed = EnumSet.noneOf(TaskField.class);
		String newTitle = title.trim();
		String newDescription = blankToNull(description);
		if (!this.title.equals(newTitle)) {
			this.title = newTitle;
			changed.add(TaskField.TITLE);
		}
		if (!Objects.equals(this.description, newDescription)) {
			this.description = newDescription;
			changed.add(TaskField.DESCRIPTION);
		}
		if (this.priority != priority) {
			this.priority = priority;
			changed.add(TaskField.PRIORITY);
		}
		if (!Objects.equals(this.dueDate, dueDate)) {
			this.dueDate = dueDate;
			changed.add(TaskField.DUE_DATE);
		}
		if (!tagIds(this.tags).equals(tagIds(tags))) {
			this.tags.clear();
			this.tags.addAll(tags);
			changed.add(TaskField.TAGS);
		}
		if (!changed.isEmpty()) {
			// Tag-only edits do not touch the tasks row, so mark it updated explicitly.
			this.updatedAt = Instant.now();
		}
		return changed;
	}

	public void moveTo(TaskStatus status, double position) {
		this.status = status;
		this.position = position;
	}

	public void assignTo(User assignee) {
		this.assignee = assignee;
	}

	public boolean isAssignedTo(UUID userId) {
		return assignee != null && assignee.getId().equals(userId);
	}

	public boolean isDone() {
		return status == TaskStatus.DONE;
	}

	private static Set<UUID> tagIds(Set<Tag> tags) {
		return tags.stream().map(Tag::getId).collect(Collectors.toSet());
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	public UUID getId() {
		return id;
	}

	public Project getProject() {
		return project;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public TaskPriority getPriority() {
		return priority;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public User getAssignee() {
		return assignee;
	}

	public double getPosition() {
		return position;
	}

	public Set<Tag> getTags() {
		return tags;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
