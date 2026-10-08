package com.xdsdata.taskflow.notifications.internal;

import com.xdsdata.taskflow.common.AppProperties;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Real-time notification delivery: STOMP over a plain WebSocket at {@code /ws}. Only server-to-user messages
 * on {@code /user/queue/notifications} are supported.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final StompAuthInterceptor authInterceptor;

	private final AppProperties properties;

	private final TaskScheduler messageBrokerTaskScheduler;

	WebSocketConfig(StompAuthInterceptor authInterceptor, AppProperties properties,
			@Lazy TaskScheduler messageBrokerTaskScheduler) {
		this.authInterceptor = authInterceptor;
		this.properties = properties;
		this.messageBrokerTaskScheduler = messageBrokerTaskScheduler;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws").setAllowedOrigins(properties.allowedOrigins().toArray(String[]::new));
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/queue")
			.setTaskScheduler(messageBrokerTaskScheduler)
			.setHeartbeatValue(new long[] { 10_000, 10_000 });
		registry.setApplicationDestinationPrefixes("/app");
		registry.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(authInterceptor);
	}

}
