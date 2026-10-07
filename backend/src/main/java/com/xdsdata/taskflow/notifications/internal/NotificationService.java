package com.xdsdata.taskflow.notifications.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.error.ForbiddenException;
import com.xdsdata.taskflow.common.error.NotFoundException;
import com.xdsdata.taskflow.common.web.Cursor;
import com.xdsdata.taskflow.common.web.CursorPage;
import com.xdsdata.taskflow.notifications.NotificationType;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class NotificationService {

	private static final int MAX_PAGE_SIZE = 50;

	private final NotificationRepository notifications;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	NotificationService(NotificationRepository notifications, ApplicationEventPublisher events, Clock clock) {
		this.notifications = notifications;
		this.events = events;
		this.clock = clock;
	}

	/** Stores a notification in the current transaction; it is pushed to the recipient after commit. */
	@Transactional
	void notify(UUID recipientId, NotificationType type, String title, String message, UUID taskId, UUID projectId) {
		Notification saved = notifications
			.saveAndFlush(new Notification(recipientId, type, limit(title, 200), limit(message, 500), taskId, projectId));
		events.publishEvent(new NotificationCreated(recipientId, NotificationDto.from(saved)));
	}

	/** Like {@link #notify} but at most once per {@code dedupeKey}; returns whether it was sent. */
	@Transactional
	boolean notifyOnce(UUID recipientId, NotificationType type, String title, String message, UUID taskId,
			UUID projectId, String dedupeKey) {
		UUID id = UUID.randomUUID();
		Instant now = clock.instant();
		int inserted = notifications.insertIfAbsent(id, recipientId, taskId, projectId, type.name(), limit(title, 200),
				limit(message, 500), now, dedupeKey);
		if (inserted == 0) {
			return false;
		}
		events.publishEvent(new NotificationCreated(recipientId,
				new NotificationDto(id, type, limit(title, 200), limit(message, 500), taskId, projectId, false, now)));
		return true;
	}

	CursorPage<NotificationDto> list(AuthUser user, String cursor, int limit) {
		int size = Math.clamp(limit, 1, MAX_PAGE_SIZE);
		Cursor position = Cursor.decode(cursor);
		List<Notification> rows = position == null ? notifications.findLatest(user.id(), Limit.of(size + 1))
				: notifications.findOlderThan(user.id(), position.createdAt(), position.id(), Limit.of(size + 1));
		boolean hasMore = rows.size() > size;
		List<Notification> page = hasMore ? rows.subList(0, size) : rows;
		String next = hasMore ? new Cursor(page.getLast().getCreatedAt(), page.getLast().getId()).encode() : null;
		return new CursorPage<>(page.stream().map(NotificationDto::from).toList(), next);
	}

	long unreadCount(UUID userId) {
		return notifications.countByUserIdAndReadFalse(userId);
	}

	@Transactional
	void markRead(AuthUser user, UUID notificationId) {
		Notification notification = notifications.findById(notificationId)
			.orElseThrow(() -> new NotFoundException("Notification"));
		if (!notification.getUserId().equals(user.id())) {
			throw new ForbiddenException("This notification belongs to someone else.");
		}
		notification.markRead();
	}

	@Transactional
	void markAllRead(AuthUser user) {
		notifications.markAllRead(user.id());
	}

	private static String limit(String text, int max) {
		return text.length() <= max ? text : text.substring(0, max - 1) + "…";
	}

	/** Published when a notification is stored; delivered over WebSocket once the transaction commits. */
	record NotificationCreated(UUID userId, NotificationDto notification) {
	}

}
