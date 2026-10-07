package com.xdsdata.taskflow.auth.internal;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from PasswordResetToken t join fetch t.user where t.tokenHash = :tokenHash")
	Optional<PasswordResetToken> findForUpdate(String tokenHash);

	/** Only the most recent reset link stays valid. */
	@Modifying
	@Query("update PasswordResetToken t set t.usedAt = :now where t.user.id = :userId and t.usedAt is null")
	int invalidateOutstanding(UUID userId, Instant now);

	@Modifying
	@Query("delete from PasswordResetToken t where t.expiresAt < :cutoff")
	int deleteExpiredBefore(Instant cutoff);

}
