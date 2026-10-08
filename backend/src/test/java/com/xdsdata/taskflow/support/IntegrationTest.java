package com.xdsdata.taskflow.support;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.xdsdata.taskflow.TestcontainersConfiguration;
import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.users.UserService;
import jakarta.servlet.http.Cookie;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for API integration tests: the whole application against PostgreSQL 18 in Testcontainers,
 * driven through MockMvc. All tests share one application context and database, so every test creates its
 * own users (unique emails) and never depends on global counts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTest {

	protected static final String PASSWORD = "Sup3r-secret!";

	protected static final String REFRESH_COOKIE = "tf_refresh";

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected ObjectMapper json;

	@Autowired
	protected UserService users;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	// ---- users and sessions ----

	protected static String uniqueEmail(String name) {
		return name.toLowerCase().replace(' ', '.') + "." + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
	}

	/** Registers through the API (self-registration role) and signs in. */
	protected Session register(String name) throws Exception {
		String email = uniqueEmail(name);
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content(body("name", name, "email", email, "password", PASSWORD, "confirmPassword", PASSWORD)))
			.andExpect(status().isCreated());
		return login(email, PASSWORD);
	}

	/** Creates an account with a given global role directly (IT Managers cannot self-register). */
	protected Session createUser(String name, Role role) throws Exception {
		String email = uniqueEmail(name);
		users.create(name, email, passwordEncoder.encode(PASSWORD), role, null);
		return login(email, PASSWORD);
	}

	protected Session login(String email, String password) throws Exception {
		MvcResult result = mvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(body("email", email, "password", password)))
			.andExpect(status().isOk())
			.andReturn();
		Cookie refresh = result.getResponse().getCookie(REFRESH_COOKIE);
		return new Session(read(result, "$.accessToken"), UUID.fromString(read(result, "$.user.id")), email,
				read(result, "$.user.name"), refresh == null ? null : refresh.getValue());
	}

	// ---- requests ----

	protected MockHttpServletRequestBuilder as(Session session, MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + session.token());
	}

	protected MockHttpServletRequestBuilder getAs(Session session, String path, Object... vars) {
		return as(session, get(path, vars));
	}

	protected MockHttpServletRequestBuilder postAs(Session session, String path, Object body, Object... vars) {
		return as(session, post(path, vars)).contentType(MediaType.APPLICATION_JSON).content(toJson(body));
	}

	protected MockHttpServletRequestBuilder putAs(Session session, String path, Object body, Object... vars) {
		return as(session, put(path, vars)).contentType(MediaType.APPLICATION_JSON).content(toJson(body));
	}

	protected MockHttpServletRequestBuilder patchAs(Session session, String path, Object body, Object... vars) {
		return as(session, patch(path, vars)).contentType(MediaType.APPLICATION_JSON).content(toJson(body));
	}

	protected MockHttpServletRequestBuilder deleteAs(Session session, String path, Object... vars) {
		return as(session, delete(path, vars));
	}

	// ---- domain helpers ----

	protected UUID createProject(Session owner, String name) throws Exception {
		MvcResult result = mvc.perform(postAs(owner, "/api/projects", body("name", name, "description", "Test project")))
			.andExpect(status().isCreated())
			.andReturn();
		return UUID.fromString(read(result, "$.id"));
	}

	protected void addMember(Session owner, UUID projectId, Session member) throws Exception {
		mvc.perform(postAs(owner, "/api/projects/{id}/members", body("email", member.email()), projectId))
			.andExpect(status().isCreated());
	}

	protected UUID createTask(Session owner, UUID projectId, Map<String, Object> fields) throws Exception {
		MvcResult result = mvc.perform(postAs(owner, "/api/projects/{id}/tasks", fields, projectId))
			.andExpect(status().isCreated())
			.andReturn();
		return UUID.fromString(read(result, "$.id"));
	}

	protected void move(Session actor, UUID taskId, String status) throws Exception {
		mvc.perform(patchAs(actor, "/api/tasks/{id}/move", body("status", status), taskId)).andExpect(status().isOk());
	}

	// ---- JSON ----

	/** Builds a JSON object from alternating keys and values; null values are kept. */
	protected static Map<String, Object> body(Object... keysAndValues) {
		Map<String, Object> map = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			map.put((String) keysAndValues[i], keysAndValues[i + 1]);
		}
		return map;
	}

	protected static List<String> list(String... values) {
		return Arrays.asList(values);
	}

	protected String toJson(Object value) {
		return json.writeValueAsString(value);
	}

	protected static <T> T read(MvcResult result, String path) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), path);
	}

	public record Session(String token, UUID userId, String email, String name, String refreshToken) {
	}

}
