package com.xdsdata.taskflow.notifications.internal;

import com.xdsdata.taskflow.notifications.internal.NotificationService.NotificationCreated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers stored notifications to the recipient's open browser tabs over STOMP, only once the change that
 * produced them has committed, so a rolled-back action never shows a notification.
 */
@Component
class NotificationPush {

	static final String USER_QUEUE = "/queue/notifications";

	private static final Logger log = LoggerFactory.getLogger(NotificationPush.class);

	private final SimpMessagingTemplate messaging;

	private final NotificationRepository notifications;

	NotificationPush(SimpMessagingTemplate messaging, NotificationRepository notifications) {
		this.messaging = messaging;
		this.notifications = notifications;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void push(NotificationCreated event) {
		try {
			long unread = notifications.countByUserIdAndReadFalse(event.userId());
			messaging.convertAndSendToUser(event.userId().toString(), USER_QUEUE,
					new PushMessage(event.notification(), unread));
		}
		catch (RuntimeException ex) {
			// Delivery is best effort: the notification is stored and appears on the next refresh.
			log.warn("Could not push notification {} to user {}", event.notification().id(), event.userId(), ex);
		}
	}

	record PushMessage(NotificationDto notification, long unreadCount) {
	}

}
