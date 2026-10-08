package com.xdsdata.taskflow.notifications.internal;

import java.security.Principal;
import java.util.UUID;

import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Authenticates the STOMP CONNECT frame with the same access token as the REST API (browsers cannot send an
 * Authorization header on the WebSocket handshake). A client may only subscribe to its own notification queue
 * and may never send messages.
 */
@Component
class StompAuthInterceptor implements ChannelInterceptor {

	static final String SUBSCRIPTION = "/user" + NotificationPush.USER_QUEUE;

	private final JwtDecoder jwtDecoder;

	private final UserService users;

	StompAuthInterceptor(JwtDecoder jwtDecoder, UserService users) {
		this.jwtDecoder = jwtDecoder;
		this.users = users;
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() == null) {
			return message;
		}
		StompCommand command = accessor.getCommand();
		if (command == StompCommand.CONNECT) {
			accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
		}
		else if (command == StompCommand.SUBSCRIBE) {
			if (accessor.getUser() == null || !SUBSCRIPTION.equals(accessor.getDestination())) {
				throw new MessageDeliveryException("Subscription not allowed");
			}
		}
		else if (command == StompCommand.SEND) {
			throw new MessageDeliveryException("Sending messages is not supported");
		}
		return message;
	}

	private Principal authenticate(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			throw new MessageDeliveryException("Missing access token");
		}
		Jwt jwt;
		try {
			jwt = jwtDecoder.decode(authorization.substring("Bearer ".length()));
		}
		catch (JwtException ex) {
			throw new MessageDeliveryException("Invalid access token");
		}
		UUID userId;
		try {
			userId = UUID.fromString(jwt.getSubject());
		}
		catch (RuntimeException ex) {
			throw new MessageDeliveryException("Invalid access token");
		}
		users.findById(userId).filter(User::isActive).orElseThrow(() -> new MessageDeliveryException("Account is not active"));
		return new StompPrincipal(userId.toString());
	}

	/** The principal name is the user id, which is what notifications are addressed to. */
	record StompPrincipal(String name) implements Principal {

		@Override
		public String getName() {
			return name;
		}

	}

}
