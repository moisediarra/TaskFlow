package com.xdsdata.taskflow.notifications.internal;

import java.util.UUID;

import com.xdsdata.taskflow.users.UserSessionsEnded;
import org.junit.jupiter.api.Test;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Someone signed out everywhere loses their real-time connections at once. */
class LiveConnectionsTests {

	private final LiveConnections connections = new LiveConnections();

	private final WebSocketHandler handler = connections.decorate(mock(WebSocketHandler.class));

	private final UUID userId = UUID.randomUUID();

	@Test
	void closesEveryConnectionOfTheSignedOutUserOnly() throws Exception {
		WebSocketSession laptop = open("ws-1", userId);
		WebSocketSession phone = open("ws-2", userId);
		WebSocketSession someoneElses = open("ws-3", UUID.randomUUID());

		connections.onSessionsEnded(new UserSessionsEnded(userId));

		verify(laptop).close(LiveConnections.SESSION_ENDED);
		verify(phone).close(LiveConnections.SESSION_ENDED);
		verify(someoneElses, never()).close(LiveConnections.SESSION_ENDED);
	}

	@Test
	void aConnectionIsClaimedBeforeItsTokenIsChecked() throws Exception {
		// The CONNECT is still being checked when the sign-out commits: it must be closed all the same.
		WebSocketSession racing = connection("ws-4");
		handler.afterConnectionEstablished(racing);
		connections.claimedBy("ws-4", userId);

		connections.onSessionsEnded(new UserSessionsEnded(userId));

		verify(racing).close(LiveConnections.SESSION_ENDED);
	}

	@Test
	void forgetsConnectionsThatAlreadyClosed() throws Exception {
		WebSocketSession closed = open("ws-5", userId);
		handler.afterConnectionClosed(closed, CloseStatus.NORMAL);

		connections.onSessionsEnded(new UserSessionsEnded(userId));

		verify(closed, never()).close(LiveConnections.SESSION_ENDED);
	}

	private WebSocketSession open(String id, UUID owner) throws Exception {
		WebSocketSession session = connection(id);
		handler.afterConnectionEstablished(session);
		connections.claimedBy(id, owner);
		return session;
	}

	private static WebSocketSession connection(String id) {
		WebSocketSession session = mock(WebSocketSession.class);
		when(session.getId()).thenReturn(id);
		return session;
	}

}
