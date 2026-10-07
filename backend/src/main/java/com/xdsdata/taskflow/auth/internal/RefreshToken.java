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
@Table(name = "refresh_tokens")
public class RefreshToken {

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

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by")
	private UUID replacedBy;

	protected RefreshToken() {
	}

	RefreshToken(User user, String tokenHash, Instant expiresAt, Instant createdAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.createdAt = createdAt;
	}

	void revoke(Instant when, UUID replacement) {
		this.revokedAt = when;
		this.replacedBy = replacement;
	}

	boolean isRevoked() {
		return revokedAt != null;
	}

	UUID getId() {
		return id;
	}

	User getUser() {
		return user;
	}

	Instant getExpiresAt() {
		return expiresAt;
	}

	Instant getRevokedAt() {
		return revokedAt;
	}

	UUID getReplacedBy() {
		return replacedBy;
	}

}
