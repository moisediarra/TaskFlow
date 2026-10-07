package com.xdsdata.taskflow.auth.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.xdsdata.taskflow.common.AppProperties;
import com.xdsdata.taskflow.users.User;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
class AccessTokenService {

	static final String ISSUER = "taskflow";

	private final JwtEncoder encoder;

	private final Clock clock;

	private final Duration ttl;

	AccessTokenService(JwtEncoder encoder, Clock clock, AppProperties properties) {
		this.encoder = encoder;
		this.clock = clock;
		this.ttl = properties.jwt().accessTokenTtl();
	}

	AccessToken issue(User user) {
		Instant now = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.subject(user.getId().toString())
			.issuedAt(now)
			.expiresAt(now.plus(ttl))
			.claim("role", user.getRole().name())
			.build();
		String value = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
		return new AccessToken(value, ttl.toSeconds());
	}

	record AccessToken(String value, long expiresInSeconds) {
	}

}
