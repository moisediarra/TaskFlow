package com.xdsdata.taskflow.notifications.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface NotificationRepository extends JpaRepository<Notification, UUID> {

	long countByUserIdAndReadFalse(UUID userId);

	@Query("select n from Notification n where n.userId = :userId order by n.createdAt desc, n.id desc")
	List<Notification> findLatest(UUID userId, Limit limit);

	@Query("""
			select n from Notification n
			where n.userId = :userId and (n.createdAt < :createdAt or (n.createdAt = :createdAt and n.id < :id))
			order by n.createdAt desc, n.id desc""")
	List<Notification> findOlderThan(UUID userId, Instant createdAt, UUID id, Limit limit);

	@Modifying
	@Query("update Notification n set n.read = true where n.userId = :userId and n.read = false")
	int markAllRead(UUID userId);

	/** Inserts the notification unless one with the same dedupe key exists; returns the inserted row count. */
	@Modifying
	@Query(value = """
			insert into notifications (id, user_id, task_id, project_id, type, title, message, is_read, created_at, dedupe_key)
			values (:id, :userId, :taskId, :projectId, :type, :title, :message, false, :createdAt, :dedupeKey)
			on conflict (dedupe_key) do nothing""", nativeQuery = true)
	int insertIfAbsent(UUID id, UUID userId, UUID taskId, UUID projectId, String type, String title, String message,
			Instant createdAt, String dedupeKey);

}
