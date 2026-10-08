package com.xdsdata.taskflow.management;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManagementIntegrationTests extends IntegrationTest {

	@Autowired
	private Clock clock;

	@Test
	void overviewAnswersWhoIsDoingWhatAndWhatIsOverdue() throws Exception {
		Session owner = register("Overview Owner");
		Session member = register("Overview Member");
		Session itManager = createUser("Overview IT", Role.IT_MANAGER);
		UUID projectId = createProject(owner, "Overview");
		addMember(owner, projectId, member);
		UUID inProgress = createTask(owner, projectId,
				body("title", "Busy", "status", "IN_PROGRESS", "assigneeId", member.userId()));
		UUID overdue = createTask(owner, projectId, body("title", "Late", "status", "TODO", "priority", "HIGH",
				"dueDate", LocalDate.now(clock).minusDays(1).toString(), "assigneeId", member.userId()));

		MvcResult result = mvc.perform(getAs(itManager, "/api/management/overview"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tasks.inProgress").value(greaterThanOrEqualTo(1)))
			.andExpect(jsonPath("$.tasks.overdue").value(greaterThanOrEqualTo(1)))
			.andExpect(jsonPath("$.tasks.highPriority").value(greaterThanOrEqualTo(1)))
			.andExpect(jsonPath("$.users.active").value(greaterThanOrEqualTo(3)))
			.andExpect(jsonPath("$.projects.active").value(greaterThanOrEqualTo(1)))
			.andReturn();
		List<String> inProgressIds = read(result, "$.inProgressNow[*].taskId");
		List<String> overdueIds = read(result, "$.overdueTasks[*].taskId");
		assertThat(inProgressIds).contains(inProgress.toString());
		assertThat(overdueIds).contains(overdue.toString());

		mvc.perform(getAs(itManager, "/api/management/team-activity?userId={id}&due=OVERDUE", member.userId()))
			.andExpect(jsonPath("$.totalItems").value(1))
			.andExpect(jsonPath("$.items[0].taskId").value(overdue.toString()));
		mvc.perform(getAs(itManager, "/api/management/team-activity?userId={id}&status=DONE", member.userId()))
			.andExpect(jsonPath("$.totalItems").value(0));
	}

	@Test
	void workloadFlagsHeavyLoadsWithNeutralWording() throws Exception {
		Session owner = register("Load Owner");
		Session busy = register("Busy Bee");
		Session idle = register("Idle Member");
		Session itManager = createUser("Load IT", Role.IT_MANAGER);
		UUID projectId = createProject(owner, "Load");
		addMember(owner, projectId, busy);
		addMember(owner, projectId, idle);
		for (int i = 0; i < 8; i++) {
			createTask(owner, projectId, body("title", "Work " + i, "status", "TODO", "assigneeId", busy.userId()));
		}
		MvcResult result = mvc.perform(getAs(itManager, "/api/management/workload"))
			.andExpect(jsonPath("$.warnings", hasItem("Busy Bee has 8 active tasks.")))
			.andExpect(jsonPath("$.activeTaskWarning").value(8))
			.andReturn();
		List<Integer> idleActive = read(result, "$.rows[?(@.userId == '" + idle.userId() + "')].active");
		assertThat(idleActive).containsExactly(0);
	}

	@Test
	void roleChangesNeedThePasswordAndAreAudited() throws Exception {
		Session itManager = createUser("Role Admin", Role.IT_MANAGER);
		Session target = register("Promoted Person");

		mvc.perform(patchAs(itManager, "/api/management/users/{id}/role", body("role", "MEMBER", "currentPassword", "wrong"),
				target.userId()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.currentPassword").exists());
		mvc.perform(patchAs(itManager, "/api/management/users/{id}/role",
				body("role", "MEMBER", "currentPassword", PASSWORD), target.userId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.role").value("MEMBER"));
		mvc.perform(patchAs(itManager, "/api/management/users/{id}/role",
				body("role", "MEMBER", "currentPassword", PASSWORD), itManager.userId()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CANNOT_CHANGE_SELF"));

		// The demoted user can no longer create projects (roles are re-read on every request).
		mvc.perform(postAs(target, "/api/projects", body("name", "Too late"))).andExpect(status().isForbidden());
		mvc.perform(getAs(itManager, "/api/management/activity-logs?action=USER_ROLE_CHANGED"))
			.andExpect(jsonPath("$.items[0].metadata.targetUserId").value(target.userId().toString()))
			.andExpect(jsonPath("$.items[0].metadata.newRole").value("MEMBER"));
	}

	@Test
	void usersPageSearchesFiltersAndShowsDetails() throws Exception {
		String marker = "usr" + UUID.randomUUID().toString().substring(0, 6);
		Session itManager = createUser("Directory IT", Role.IT_MANAGER);
		Session owner = register("Owner " + marker);
		UUID projectId = createProject(owner, "Directory");
		createTask(owner, projectId, body("title", "Own work", "status", "TODO", "assigneeId", owner.userId()));

		mvc.perform(getAs(itManager, "/api/management/users?q={q}&role=PROJECT_OWNER", marker))
			.andExpect(jsonPath("$.totalItems").value(1))
			.andExpect(jsonPath("$.items[0].projectCount").value(1))
			.andExpect(jsonPath("$.items[0].activeTaskCount").value(1));
		mvc.perform(getAs(itManager, "/api/management/users?q={q}&role=MEMBER", marker))
			.andExpect(jsonPath("$.totalItems").value(0));
		mvc.perform(getAs(itManager, "/api/management/users/{id}", owner.userId()))
			.andExpect(jsonPath("$.ownedProjectCount").value(1))
			.andExpect(jsonPath("$.projects[0].projectId").value(projectId.toString()))
			.andExpect(jsonPath("$.tasks[0].title").value("Own work"));
	}

}
