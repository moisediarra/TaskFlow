package com.xdsdata.taskflow;

import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The end-to-end workflow of claude.md §45, through the public API: register, create a project, add a member,
 * create and assign a task, move it to Done, and check what the member, the owner and the IT Manager see.
 */
class DefinitionOfDoneTests extends IntegrationTest {

	@Test
	void fullWorkflowFromRegistrationToCompletion() throws Exception {
		// User registers and logs in; an intervenant does the same; an IT Manager exists.
		Session owner = register("Olivia Owner");
		Session intervenant = register("Ian Intervenant");
		Session itManager = createUser("Irene IT", Role.IT_MANAGER);

		// User creates a project and adds a member.
		UUID projectId = createProject(owner, "Definition of Done " + UUID.randomUUID().toString().substring(0, 6));
		addMember(owner, projectId, intervenant);
		mvc.perform(getAs(owner, "/api/projects/{id}/members", projectId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(2))
			.andExpect(jsonPath("$[0].role").value("OWNER"));

		// User creates a task: it is placed in Backlog.
		UUID taskId = createTask(owner, projectId,
				body("title", "Implement Login UI", "priority", "HIGH", "newTags", list("Frontend", "UI")));
		mvc.perform(getAs(owner, "/api/tasks/{id}", taskId))
			.andExpect(jsonPath("$.status").value("BACKLOG"))
			.andExpect(jsonPath("$.tags.length()").value(2));

		// Task is assigned to the intervenant, who receives a notification.
		mvc.perform(putAs(owner, "/api/tasks/{id}/assignee", body("assigneeId", intervenant.userId()), taskId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.assignee.id").value(intervenant.userId().toString()));
		mvc.perform(getAs(intervenant, "/api/notifications"))
			.andExpect(jsonPath("$.items[0].type").value("TASK_ASSIGNED"))
			.andExpect(jsonPath("$.items[0].taskId").value(taskId.toString()))
			.andExpect(jsonPath("$.items[0].message").value("You have been assigned to \"Implement Login UI\" in "
					+ read(mvc.perform(getAs(owner, "/api/projects/{id}", projectId)).andReturn(), "$.name") + "."));
		mvc.perform(getAs(intervenant, "/api/notifications/unread-count")).andExpect(jsonPath("$.count").value(1));

		// The intervenant moves the task to To Do, then In Progress.
		move(intervenant, taskId, "TODO");
		move(intervenant, taskId, "IN_PROGRESS");

		// The IT Manager sees who is working on it, in Team Activity and Workload.
		mvc.perform(getAs(itManager, "/api/management/team-activity?projectId={id}", projectId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].taskId").value(taskId.toString()))
			.andExpect(jsonPath("$.items[0].assigneeName").value("Ian Intervenant"))
			.andExpect(jsonPath("$.items[0].status").value("IN_PROGRESS"));
		MvcResult workload = mvc.perform(getAs(itManager, "/api/management/workload")).andReturn();
		List<Integer> active = read(workload, "$.rows[?(@.userId == '" + intervenant.userId() + "')].active");
		assertThat(active).containsExactly(1);

		// The task is completed: it moves to Done.
		move(intervenant, taskId, "DONE");

		// Activity is recorded.
		mvc.perform(getAs(owner, "/api/projects/{id}/activity", projectId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].action").value("TASK_COMPLETED"))
			.andExpect(jsonPath("$.items[0].description").value("Ian Intervenant completed \"Implement Login UI\"."))
			.andExpect(jsonPath("$.items[0].metadata.oldStatus").value("IN_PROGRESS"))
			.andExpect(jsonPath("$.items[0].metadata.newStatus").value("DONE"))
			.andExpect(jsonPath("$.items[*].action", hasItem("TASK_STATUS_CHANGED")))
			.andExpect(jsonPath("$.items[*].action", hasItem("TASK_ASSIGNED")))
			.andExpect(jsonPath("$.items[*].action", hasItem("TASK_CREATED")))
			.andExpect(jsonPath("$.items[*].action", hasItem("PROJECT_MEMBER_ADDED")));
		mvc.perform(getAs(itManager, "/api/management/activity-logs?projectId={id}", projectId))
			.andExpect(jsonPath("$.items[0].action").value("TASK_COMPLETED"));

		// Relevant users receive notifications: the owner learns about each status change made by the intervenant,
		// the intervenant is never notified about their own moves.
		mvc.perform(getAs(owner, "/api/notifications"))
			.andExpect(jsonPath("$.items[0].type").value("TASK_STATUS_CHANGED"))
			.andExpect(jsonPath("$.items[0].title").value("Ian Intervenant completed a task"))
			.andExpect(jsonPath("$.items.length()").value(3));
		mvc.perform(getAs(intervenant, "/api/notifications")).andExpect(jsonPath("$.items.length()").value(1));

		// Dashboards reflect the result.
		mvc.perform(getAs(intervenant, "/api/dashboard"))
			.andExpect(jsonPath("$.myTasks.total").value(1))
			.andExpect(jsonPath("$.myTasks.done").value(1))
			.andExpect(jsonPath("$.recentProjects[0].id").value(projectId.toString()));
		mvc.perform(getAs(itManager, "/api/projects/{id}/overview", projectId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.tasks.done").value(1))
			.andExpect(jsonPath("$.memberCount").value(2))
			.andExpect(jsonPath("$.currentActivity[0].assigneeName").value("Ian Intervenant"));
	}

	@Test
	void taskCreatedInAColumnTakesThatStatusAndAnAssigneeChosenAtCreationIsNotified() throws Exception {
		Session owner = register("Column Owner");
		Session member = register("Column Member");
		UUID projectId = createProject(owner, "Columns");
		addMember(owner, projectId, member);
		UUID taskId = createTask(owner, projectId,
				body("title", "Started straight away", "status", "IN_PROGRESS", "assigneeId", member.userId()));
		mvc.perform(getAs(owner, "/api/tasks/{id}", taskId)).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
		mvc.perform(getAs(member, "/api/notifications")).andExpect(jsonPath("$.items[0].type").value("TASK_ASSIGNED"));
		mvc.perform(getAs(owner, "/api/projects/{id}/activity", projectId))
			.andExpect(jsonPath("$.items[*].action", hasItem("TASK_ASSIGNED")));
	}

}
