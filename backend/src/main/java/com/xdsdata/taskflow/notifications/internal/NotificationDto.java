package com.xdsdata.taskflow.notifications.internal;

import java.time.Instant;
import java.util.UUID;

import com.xdsdata.taskflow.notifications.NotificationType;

/** A notification as shown in the bell panel; {@code taskId}/{@code projectId} open the related task. */
record NotificationDto(UUID id, NotificationType type, String title, String message, UUID taskId, UUID projectId,
		boolean read, Instant createdAt) {

	static NotificationDto from(Notification notification) {
		return new NotificationDto(notification.getId(), notification.getType(), notification.getTitle(),
				notification.getMessage(), notification.getTaskId(), notification.getProjectId(), notification.isRead(),
				notification.getCreatedAt());
	}

}
