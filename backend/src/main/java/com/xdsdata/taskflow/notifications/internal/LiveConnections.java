package com.xdsdata.taskflow.notifications.internal;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.xdsdata.taskflow.users.UserSessionsEnded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * Keeps track of open WebSocket connections and whose they are, so a user who is signed out everywhere
 * (password changed or reset, account deactivated or deleted) stops receiving notifications at once instead of
 * whenever the connection happens to drop. The browser then reconnects only if it can still refresh its session.
 * <p>
 * The owner is recorded when STOMP CONNECT arrives, before its token is checked: a CONNECT racing a sign-out is
 * then either refused by that check or closed here.
 */
@Component
class LiveConnections implements WebSocketHandlerDecoratorFactory {

	static final CloseStatus SESSION_ENDED = new CloseStatus(4001, "Session ended");

	private static final Logger log = LoggerFactory.getLogger(LiveConnections.class);

	/** By WebSocket session id, which is also the STOMP session id. */
	private final Map<String, WebSocketSession> open = new ConcurrentHashMap<>();

	private final Map<String, UUID> owners = new ConcurrentHashMap<>();

	@Override
	public WebSocketHandler decorate(WebSocketHandler handler) {
		return new WebSocketHandlerDecorator(handler) {

			@Override
			public void afterConnectionEstablished(WebSocketSession session) throws Exception {
				open.put(session.getId(), session);
				super.afterConnectionEstablished(session);
			}

			@Override
			public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
				open.remove(session.getId());
				owners.remove(session.getId());
				super.afterConnectionClosed(session, status);
			}

		};
	}

	/** Called for every STOMP CONNECT naming a user, before the token is checked against their account. */
	void claimedBy(String sessionId, UUID userId) {
		if (sessionId != null) {
			owners.put(sessionId, userId);
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void onSessionsEnded(UserSessionsEnded event) {
		owners.forEach((sessionId, userId) -> {
			if (!userId.equals(event.userId())) {
				return;
			}
			WebSocketSession connection = open.get(sessionId);
			if (connection == null) {
				return;
			}
			try {
				connection.close(SESSION_ENDED);
			}
			catch (IOException ex) {
				log.debug("Could not close connection {} of user {}", sessionId, userId, ex);
			}
		});
	}

}
