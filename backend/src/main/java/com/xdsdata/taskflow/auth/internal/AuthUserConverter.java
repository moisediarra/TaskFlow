package com.xdsdata.taskflow.auth.internal;

import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserService;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

/**
 * Turns a validated access token into the request's principal. The user is reloaded on every request so
 * that a role change or a deactivation takes effect immediately instead of when the token expires.
 */
@Component
class AuthUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private final UserService users;

	AuthUserConverter(UserService users) {
		this.users = users;
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		UUID userId;
		try {
			userId = UUID.fromString(jwt.getSubject());
		}
		catch (RuntimeException ex) {
			throw new InvalidBearerTokenException("Invalid token subject");
		}
		User user = users.findById(userId)
			.filter(User::isActive)
			.orElseThrow(() -> new InvalidBearerTokenException("Account is not active"));
		return UsernamePasswordAuthenticationToken.authenticated(user.toAuthUser(), jwt,
				List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
	}

}
