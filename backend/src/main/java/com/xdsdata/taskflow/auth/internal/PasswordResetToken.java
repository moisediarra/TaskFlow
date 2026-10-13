package com.xdsdata.taskflow.auth.internal;

import java.time.Instant;
import java.util.UUID;

import com.xdsdata.taskflow.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "used_at")
	private Instant usedAt;

	/** The address the link was sent to; it stops working if the account's email changes. */
	@Column(name = "sent_to", length = 254, updatable = false)
	private String sentTo;

	protected PasswordResetToken() {
	}

	PasswordResetToken(User user, String tokenHash, Instant expiresAt, Instant createdAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.createdAt = createdAt;
		this.sentTo = user.getEmail();
	}

	boolean isUsable(Instant now) {
		return usedAt == null && expiresAt.isAfter(now) && user.isActive() && user.getEmail().equals(sentTo);
	}

	void markUsed(Instant when) {
		this.usedAt = when;
	}

	User getUser() {
		return user;
	}

}
