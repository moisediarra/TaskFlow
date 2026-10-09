package com.xdsdata.taskflow.management;

import java.util.Map;
import java.util.UUID;

import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The IT Manager creates, edits, resets and deletes accounts; every change is confirmed and audited. */
class UserAdministrationIntegrationTests extends IntegrationTest {

	private static final String STARTER = "Starter-pass-1";

	@Test
	void itManagerCreatesAnAccountThatCanSignIn() throws Exception {
		Session itManager = createUser("Account Admin", Role.IT_MANAGER);
		String name = "New Hire " + UUID.randomUUID().toString().substring(0, 6);
		String email = uniqueEmail("new hire");

		mvc.perform(postAs(itManager, "/api/management/users", newUser(name, email, STARTER, "wrong")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.currentPassword").exists());
		mvc.perform(postAs(itManager, "/api/management/users", newUser(name, email, "short", PASSWORD)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.password").exists());
		mvc.perform(postAs(itManager, "/api/management/users", newUser(name, email, STARTER, PASSWORD)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value(name))
			.andExpect(jsonPath("$.role").value("MEMBER"))
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.jobTitle").value("Developer"));

		Session hired = login(email, STARTER);
		mvc.perform(getAs(hired, "/api/auth/me")).andExpect(jsonPath("$.role").value("MEMBER"));
		mvc.perform(postAs(itManager, "/api/management/users", newUser("Twin", email.toUpperCase(), STARTER, PASSWORD)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
		mvc.perform(getAs(itManager, "/api/management/activity-logs?action=USER_CREATED&q={q}", name))
			.andExpect(jsonPath("$.items[0].metadata.targetUserId").value(hired.userId().toString()))
			.andExpect(jsonPath("$.items[0].metadata.role").value("MEMBER"));
	}

	@Test
	void itManagerCorrectsDetailsIncludingTheSignInEmail() throws Exception {
		Session itManager = createUser("Details Admin", Role.IT_MANAGER);
		Session target = register("Typo Nmae");
		Session other = register("Someone Else");
		String newEmail = uniqueEmail("typo name");

		mvc.perform(putAs(itManager, "/api/management/users/{id}",
				body("name", "Typo Name", "email", other.email(), "jobTitle", "", "currentPassword", PASSWORD),
				target.userId()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
		mvc.perform(putAs(itManager, "/api/management/users/{id}",
				body("name", "Typo Name", "email", newEmail, "jobTitle", "QA Engineer", "currentPassword", PASSWORD),
				target.userId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Typo Name"))
			.andExpect(jsonPath("$.email").value(newEmail))
			.andExpect(jsonPath("$.jobTitle").value("QA Engineer"));

		login(newEmail, PASSWORD);
		expectSignInFails(target.email(), PASSWORD);
		mvc.perform(getAs(itManager, "/api/management/activity-logs?action=USER_UPDATED&q={q}", "Typo Nmae"))
			.andExpect(jsonPath("$.items[0].metadata.targetUserId").value(target.userId().toString()))
			.andExpect(jsonPath("$.items[0].metadata.previousEmail").value(target.email()))
			.andExpect(jsonPath("$.items[0].metadata.changedFields", hasItem("email")));
	}

	@Test
	void passwordResetReplacesThePasswordAndEndsSessions() throws Exception {
		Session itManager = createUser("Reset Admin", Role.IT_MANAGER);
		Session target = register("Forgetful Person");

		mvc.perform(postAs(itManager, "/api/management/users/{id}/password",
				body("newPassword", STARTER, "confirmPassword", "different-1", "currentPassword", PASSWORD), target.userId()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.confirmPassword").exists());
		mvc.perform(postAs(itManager, "/api/management/users/{id}/password",
				body("newPassword", STARTER, "confirmPassword", STARTER, "currentPassword", PASSWORD), itManager.userId()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CANNOT_CHANGE_SELF"));
		mvc.perform(postAs(itManager, "/api/management/users/{id}/password",
				body("newPassword", STARTER, "confirmPassword", STARTER, "currentPassword", PASSWORD), target.userId()))
			.andExpect(status().isNoContent());

		expectSignInFails(target.email(), PASSWORD);
		login(target.email(), STARTER);
		mvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, target.refreshToken())))
			.andExpect(status().isUnauthorized());
		mvc.perform(getAs(itManager, "/api/management/activity-logs?action=USER_PASSWORD_RESET&q={q}", "Forgetful"))
			.andExpect(jsonPath("$.items[0].metadata.targetUserId").value(target.userId().toString()));
	}

	@Test
	void deletingAnAccountKeepsTheWorkAndTheHistory() throws Exception {
		Session itManager = createUser("Delete Admin", Role.IT_MANAGER);
		Session owner = register("Keeper Owner");
		String leaverName = "Leaving Member " + UUID.randomUUID().toString().substring(0, 6);
		Session leaver = register(leaverName);
		UUID projectId = createProject(owner, "Handover project");
		addMember(owner, projectId, leaver);
		UUID taskId = createTask(owner, projectId,
				body("title", "Handover notes", "status", "TODO", "assigneeId", leaver.userId()));
		move(leaver, taskId, "IN_PROGRESS");

		mvc.perform(deleteUser(itManager, owner.userId(), PASSWORD))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("OWNS_PROJECTS"));
		mvc.perform(deleteUser(itManager, itManager.userId(), PASSWORD))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CANNOT_CHANGE_SELF"));
		mvc.perform(deleteUser(itManager, leaver.userId(), "wrong"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.currentPassword").exists());
		mvc.perform(deleteUser(itManager, leaver.userId(), PASSWORD)).andExpect(status().isNoContent());

		mvc.perform(getAs(itManager, "/api/management/users/{id}", leaver.userId())).andExpect(status().isNotFound());
		expectSignInFails(leaver.email(), PASSWORD);
		mvc.perform(getAs(leaver, "/api/auth/me")).andExpect(status().isUnauthorized());

		// The work stays: the task is still there, now unassigned, and the project keeps its owner.
		mvc.perform(getAs(owner, "/api/tasks/{id}", taskId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
			.andExpect(jsonPath("$.assignee").doesNotExist());
		mvc.perform(getAs(owner, "/api/projects/{id}/members", projectId)).andExpect(jsonPath("$", hasSize(1)));

		// The history stays readable, including what the person did before leaving.
		mvc.perform(getAs(owner, "/api/projects/{id}/activity", projectId))
			.andExpect(jsonPath("$.items[*].action", hasItem("PROJECT_MEMBER_REMOVED")))
			.andExpect(jsonPath("$.items[*].action", hasItem("TASK_UNASSIGNED")))
			.andExpect(jsonPath("$.items[*].actorName", hasItem(leaverName)));
		mvc.perform(getAs(itManager, "/api/management/activity-logs?action=USER_DELETED&q={q}", leaverName))
			.andExpect(jsonPath("$.items[0].metadata.targetUserName").value(leaverName))
			.andExpect(jsonPath("$.items[0].metadata.email").value(leaver.email()));
	}

	private static Map<String, Object> newUser(String name, String email, String password, String currentPassword) {
		return body("name", name, "email", email, "jobTitle", "Developer", "role", "MEMBER", "password", password,
				"confirmPassword", password, "currentPassword", currentPassword);
	}

	private MockHttpServletRequestBuilder deleteUser(Session actor, UUID userId, String currentPassword) {
		return as(actor, delete("/api/management/users/{id}", userId)).contentType(MediaType.APPLICATION_JSON)
			.content(toJson(body("currentPassword", currentPassword)));
	}

	private void expectSignInFails(String email, String password) throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content(toJson(body("email", email, "password", password)))).andExpect(status().isUnauthorized());
	}

}
