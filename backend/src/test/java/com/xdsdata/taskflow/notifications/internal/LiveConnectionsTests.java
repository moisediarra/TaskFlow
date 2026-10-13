package com.xdsdata.taskflow.notifications.internal;

import java.util.Set;
import java.util.UUID;

import com.xdsdata.taskflow.users.UserSessionsEnded;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Someone signed out everywhere loses their real-time connection at once. */
class LiveConnectionsTests {

	private final SimpUserRegistry registry = mock(SimpUserRegistry.class);

	@SuppressWarnings("unchecked")
	private final ObjectProvider<SimpUserRegistry> provider = mock(ObjectProvider.class);

	private final LiveConnections connections = new LiveConnections(provider);

	private final WebSocketHandler handler = connections.decorate(mock(WebSocketHandler.class));

	@Test
	void closesTheConnectionsOfTheSignedOutUserOnly() throws Exception {
		UUID userId = UUID.randomUUID();
		WebSocketSession theirs = connection("ws-1");
		WebSocketSession someoneElses = connection("ws-2");
		handler.afterConnectionEstablished(theirs);
		handler.afterConnectionEstablished(someoneElses);
		registered(userId, "ws-1");

		connections.onSessionsEnded(new UserSessionsEnded(userId));

		verify(theirs).close(LiveConnections.SESSION_ENDED);
		verify(someoneElses, never()).close(LiveConnections.SESSION_ENDED);
	}

	@Test
	void forgetsConnectionsThatAlreadyClosed() throws Exception {
		UUID userId = UUID.randomUUID();
		WebSocketSession closed = connection("ws-3");
		handler.afterConnectionEstablished(closed);
		handler.afterConnectionClosed(closed, CloseStatus.NORMAL);
		registered(userId, "ws-3");

		connections.onSessionsEnded(new UserSessionsEnded(userId));

		verify(closed, never()).close(LiveConnections.SESSION_ENDED);
	}

	private static WebSocketSession connection(String id) {
		WebSocketSession session = mock(WebSocketSession.class);
		when(session.getId()).thenReturn(id);
		return session;
	}

	private void registered(UUID userId, String sessionId) {
		SimpUser user = mock(SimpUser.class);
		SimpSession session = mock(SimpSession.class);
		when(session.getId()).thenReturn(sessionId);
		when(user.getSessions()).thenReturn(Set.of(session));
		when(registry.getUser(userId.toString())).thenReturn(user);
		when(provider.getObject()).thenReturn(registry);
	}

}
