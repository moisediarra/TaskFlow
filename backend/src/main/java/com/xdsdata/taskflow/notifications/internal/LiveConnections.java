package com.xdsdata.taskflow.notifications.internal;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xdsdata.taskflow.users.UserSessionsEnded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * Keeps track of open WebSocket connections, so a user who is signed out everywhere (password changed or
 * reset, account deactivated or deleted) stops receiving notifications at once instead of whenever the
 * connection happens to drop. The browser then reconnects only if it can still refresh its session.
 */
@Component
class LiveConnections implements WebSocketHandlerDecoratorFactory {

	static final CloseStatus SESSION_ENDED = new CloseStatus(4001, "Session ended");

	private static final Logger log = LoggerFactory.getLogger(LiveConnections.class);

	/** By WebSocket session id, which is also the STOMP session id the user registry reports. */
	private final Map<String, WebSocketSession> open = new ConcurrentHashMap<>();

	private final ObjectProvider<SimpUserRegistry> userRegistry;

	LiveConnections(ObjectProvider<SimpUserRegistry> userRegistry) {
		this.userRegistry = userRegistry;
	}

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
				super.afterConnectionClosed(session, status);
			}

		};
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	void onSessionsEnded(UserSessionsEnded event) {
		SimpUser user = userRegistry.getObject().getUser(event.userId().toString());
		if (user == null) {
			return;
		}
		for (SimpSession session : user.getSessions()) {
			WebSocketSession connection = open.get(session.getId());
			if (connection == null) {
				continue;
			}
			try {
				connection.close(SESSION_ENDED);
			}
			catch (IOException ex) {
				log.debug("Could not close connection {} of user {}", session.getId(), event.userId(), ex);
			}
		}
	}

}
