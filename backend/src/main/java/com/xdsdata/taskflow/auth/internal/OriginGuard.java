package com.xdsdata.taskflow.auth.internal;

import java.util.Set;

import com.xdsdata.taskflow.common.AppProperties;
import com.xdsdata.taskflow.common.error.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Defense in depth for the cookie-based auth endpoints (on top of SameSite=Strict): browsers always send
 * an Origin header on cross-site POSTs, so a foreign origin is rejected outright.
 */
@Component
class OriginGuard {

	private final Set<String> allowedOrigins;

	OriginGuard(AppProperties properties) {
		this.allowedOrigins = Set.copyOf(properties.allowedOrigins());
	}

	void check(HttpServletRequest request) {
		String origin = request.getHeader(HttpHeaders.ORIGIN);
		if (origin != null && !allowedOrigins.contains(origin)) {
			throw new ForbiddenException("This request did not come from TaskFlow.");
		}
	}

}
