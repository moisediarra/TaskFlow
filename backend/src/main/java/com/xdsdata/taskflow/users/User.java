package com.xdsdata.taskflow.users;

import java.time.Instant;
import java.util.UUID;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.Emails;
import com.xdsdata.taskflow.common.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A TaskFlow account. Updates write only the changed columns, so concurrent changes to different fields
 * (an IT Manager deactivating someone while they edit their profile) never undo each other.
 */
@Entity
@Table(name = "users")
@DynamicUpdate
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 254, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "job_title", length = 100)
	private String jobTitle;

	@Column(length = 500)
	private String avatar;

	/**
	 * Stamped into every access and refresh token; incrementing it (UserService#endSessions) signs the user
	 * out everywhere.
	 */
	@Column(name = "session_version", nullable = false)
	private int sessionVersion;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected User() {
	}

	public User(String name, String email, String passwordHash, Role role, String jobTitle) {
		this.name = name.trim();
		this.email = Emails.normalize(email);
		this.passwordHash = passwordHash;
		this.role = role;
		this.jobTitle = blankToNull(jobTitle);
	}

	public void updateProfile(String name, String jobTitle) {
		this.name = name.trim();
		this.jobTitle = blankToNull(jobTitle);
	}

	/** Account details an IT Manager can correct; the email is the sign-in name. */
	public void updateDetails(String name, String email, String jobTitle) {
		updateProfile(name, jobTitle);
		this.email = Emails.normalize(email);
	}

	public void changePasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public void changeRole(Role role) {
		this.role = role;
	}

	public void changeStatus(UserStatus status) {
		this.status = status;
	}

	public boolean isActive() {
		return status == UserStatus.ACTIVE;
	}

	public AuthUser toAuthUser() {
		return new AuthUser(id, name, email, role);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public UserStatus getStatus() {
		return status;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public String getAvatar() {
		return avatar;
	}

	public int getSessionVersion() {
		return sessionVersion;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
