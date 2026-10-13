package com.xdsdata.taskflow.auth;

import com.xdsdata.taskflow.users.User;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Access tokens carry the user's session version in the {@value #CLAIM} claim; one issued before the user was
 * signed out everywhere no longer matches. Used for REST requests and for real-time connections.
 */
public final class SessionVersions {

	public static final String CLAIM = "sv";

	private SessionVersions() {
	}

	/** Tokens issued before session versions existed have no claim and count as version 0. */
	public static boolean isCurrent(Jwt jwt, User user) {
		Object claim = jwt.getClaims().get(CLAIM);
		long version = claim instanceof Number number ? number.longValue() : 0;
		return version == user.getSessionVersion();
	}

}
