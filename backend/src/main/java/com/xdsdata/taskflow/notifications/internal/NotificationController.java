package com.xdsdata.taskflow.notifications.internal;

import java.util.UUID;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.web.CursorPage;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's own notifications only. */
@RestController
@RequestMapping("/api/notifications")
class NotificationController {

	private final NotificationService notifications;

	NotificationController(NotificationService notifications) {
		this.notifications = notifications;
	}

	@GetMapping
	CursorPage<NotificationDto> list(@AuthenticationPrincipal AuthUser user,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
		return notifications.list(user, cursor, limit);
	}

	@GetMapping("/unread-count")
	UnreadCount unreadCount(@AuthenticationPrincipal AuthUser user) {
		return new UnreadCount(notifications.unreadCount(user.id()));
	}

	@PatchMapping("/{notificationId}/read")
	ResponseEntity<Void> markRead(@AuthenticationPrincipal AuthUser user, @PathVariable UUID notificationId) {
		notifications.markRead(user, notificationId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/read-all")
	ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AuthUser user) {
		notifications.markAllRead(user);
		return ResponseEntity.noContent().build();
	}

	record UnreadCount(long count) {
	}

}
