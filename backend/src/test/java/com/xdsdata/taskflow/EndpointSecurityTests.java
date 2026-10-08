package com.xdsdata.taskflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Sweeps every registered API endpoint, so a new endpoint cannot silently skip authentication or the
 * IT Manager restriction (claude.md §32).
 */
class EndpointSecurityTests extends IntegrationTest {

	private static final Set<String> PUBLIC = Set.of("/api/auth/register", "/api/auth/login", "/api/auth/refresh",
			"/api/auth/logout", "/api/auth/forgot-password", "/api/auth/reset-password");

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping handlerMapping;

	@Test
	void everyApiEndpointRequiresAuthentication() throws Exception {
		List<String> unprotected = new ArrayList<>();
		for (Endpoint endpoint : apiEndpoints()) {
			if (PUBLIC.contains(endpoint.path())) {
				continue;
			}
			int status = mvc.perform(request(endpoint.method(), concrete(endpoint.path()))).andReturn().getResponse().getStatus();
			if (status != 401) {
				unprotected.add(endpoint + " -> " + status);
			}
		}
		assertThat(unprotected).isEmpty();
		assertThat(apiEndpoints()).hasSizeGreaterThan(30);
	}

	@Test
	void managementEndpointsAreForItManagersOnly() throws Exception {
		Session owner = register("Curious Owner");
		Session member = createUser("Curious Member", Role.MEMBER);
		List<String> leaking = new ArrayList<>();
		for (Endpoint endpoint : apiEndpoints()) {
			if (!endpoint.path().startsWith("/api/management")) {
				continue;
			}
			for (Session session : List.of(owner, member)) {
				int status = mvc.perform(as(session, request(endpoint.method(), concrete(endpoint.path()))))
					.andReturn()
					.getResponse()
					.getStatus();
				if (status != 403) {
					leaking.add(endpoint + " as " + session.name() + " -> " + status);
				}
			}
		}
		assertThat(leaking).isEmpty();
	}

	private List<Endpoint> apiEndpoints() {
		List<Endpoint> endpoints = new ArrayList<>();
		for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
			RequestMappingInfo info = entry.getKey();
			Set<String> paths = info.getPathPatternsCondition() == null ? Set.of()
					: info.getPathPatternsCondition().getPatternValues();
			Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
			for (String path : paths) {
				if (!path.startsWith("/api/")) {
					continue;
				}
				for (RequestMethod method : methods.isEmpty() ? Set.of(RequestMethod.GET) : methods) {
					endpoints.add(new Endpoint(HttpMethod.valueOf(method.name()), path));
				}
			}
		}
		return endpoints;
	}

	private static String concrete(String pattern) {
		return pattern.replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
	}

	private record Endpoint(HttpMethod method, String path) {

		@Override
		public String toString() {
			return method + " " + path;
		}

	}

}
