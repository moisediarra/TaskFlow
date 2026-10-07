package com.xdsdata.taskflow.auth.internal;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.xdsdata.taskflow.common.AppProperties;
import com.xdsdata.taskflow.common.web.RequestIdFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless API security: every request carries a short-lived JWT access token. Authorization is enforced
 * here at URL level and again inside each service for object-level checks (claude.md §32).
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@EnableAsync
class SecurityConfig {

	/** Endpoints that work without an access token (any bearer header sent to them is ignored). */
	static final Set<String> PUBLIC_AUTH_PATHS = Set.of("/api/auth/register", "/api/auth/login", "/api/auth/refresh",
			"/api/auth/logout", "/api/auth/forgot-password", "/api/auth/reset-password");

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuthUserConverter authUserConverter,
			RestSecurityHandlers handlers, BearerTokenResolver bearerTokenResolver) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.headers(headers -> headers.frameOptions(frame -> frame.deny())
				.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
			.authorizeHttpRequests(auth -> auth.requestMatchers(PUBLIC_AUTH_PATHS.toArray(String[]::new))
				.permitAll()
				// The STOMP handshake is public; the CONNECT frame is authenticated by the WebSocket interceptor.
				.requestMatchers("/ws", "/ws/**")
				.permitAll()
				.requestMatchers("/actuator/health", "/actuator/health/**", "/error")
				.permitAll()
				// Only served when APP_API_DOCS_ENABLED=true (local development).
				.requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
				.permitAll()
				.requestMatchers("/api/management/**")
				.hasRole("IT_MANAGER")
				.requestMatchers("/api/**")
				.authenticated()
				.anyRequest()
				.denyAll())
			.oauth2ResourceServer(oauth -> oauth.bearerTokenResolver(bearerTokenResolver)
				.jwt(jwt -> jwt.jwtAuthenticationConverter(authUserConverter))
				.authenticationEntryPoint(handlers)
				.accessDeniedHandler(handlers))
			.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(handlers).accessDeniedHandler(handlers));
		return http.build();
	}

	@Bean
	BearerTokenResolver bearerTokenResolver() {
		DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
		return request -> PUBLIC_AUTH_PATHS.contains(request.getRequestURI()) ? null : delegate.resolve(request);
	}

	@Bean
	SecretKey jwtSigningKey(AppProperties properties) {
		String secret = properties.jwt().secret();
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
			throw new IllegalStateException("JWT_SECRET must be set to at least 32 characters (see .env.example).");
		}
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(AccessTokenService.ISSUER));
		return decoder;
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(properties.allowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		config.setExposedHeaders(List.of(RequestIdFilter.HEADER));
		config.setAllowCredentials(true);
		config.setMaxAge(Duration.ofHours(1));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

}
