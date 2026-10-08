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

/** The permission matrix of the plan, enforced by the backend (claude.md §3 and §32). */
class PermissionTests extends IntegrationTest {

	@Test
	void outsidersGet403OnEveryProjectAndTaskEndpoint() throws Exception {
		Session owner = register("Private Owner");
		Session outsider = register("Nosy Outsider");
		UUID projectId = createProject(owner, "Private");
		UUID taskId = createTask(owner, projectId, body("title", "Secret task"));

		mvc.perform(getAs(outsider, "/api/projects/{id}", projectId)).andExpect(status().isForbidden());
		mvc.perform(getAs(outsider, "/api/projects/{id}/board", projectId)).andExpect(status().isForbidden());
		mvc.perform(getAs(outsider, "/api/projects/{id}/overview", projectId)).andExpect(status().isForbidden());
		mvc.perform(getAs(outsider, "/api/projects/{id}/activity", projectId)).andExpect(status().isForbidden());
		mvc.perform(getAs(outsider, "/api/projects/{id}/members", projectId)).andExpect(status().isForbidden());
		mvc.perform(getAs(outsider, "/api/projects/{id}/tags", projectId)).andExpect(status().isForbidden());
		mvc.perform(getAs(outsider, "/api/projects/{id}/tasks?status=DONE", projectId)).andExpect(status().isForbidden());
		mvc.perform(getAs(outsider, "/api/tasks/{id}", taskId)).andExpect(status().isForbidden());
		mvc.perform(putAs(outsider, "/api/tasks/{id}", body("title", "Hacked", "priority", "LOW"), taskId))
			.andExpect(status().isForbidden());
		mvc.perform(patchAs(outsider, "/api/tasks/{id}/move", body("status", "DONE"), taskId))
			.andExpect(status().isForbidden());
		mvc.perform(deleteAs(outsider, "/api/tasks/{id}", taskId)).andExpect(status().isForbidden());
		mvc.perform(postAs(outsider, "/api/projects/{id}/tasks", body("title", "Injected"), projectId))
			.andExpect(status().isForbidden());
		mvc.perform(postAs(outsider, "/api/projects/{id}/members", body("email", outsider.email()), projectId))
			.andExpect(status().isForbidden());
		mvc.perform(deleteAs(outsider, "/api/projects/{id}", projectId)).andExpect(status().isForbidden());

		mvc.perform(getAs(outsider, "/api/projects/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
		mvc.perform(getAs(owner, "/api/tasks/{id}", taskId)).andExpect(jsonPath("$.title").value("Secret task"));
	}

	@Test
	void membersChangeOnlyTheirOwnAssignedTasks() throws Exception {
		Session owner = register("Strict Owner");
		Session member = register("Careful Member");
		UUID projectId = createProject(owner, "Shared");
		addMember(owner, projectId, member);
		UUID mine = createTask(owner, projectId, body("title", "Mine", "assigneeId", member.userId()));
		UUID notMine = createTask(owner, projectId, body("title", "Not mine"));

		mvc.perform(getAs(member, "/api/projects/{id}/board", projectId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.permissions.canManage").value(false));
		mvc.perform(getAs(member, "/api/tasks/{id}", mine)).andExpect(jsonPath("$.permissions.canEdit").value(true));
		mvc.perform(getAs(member, "/api/tasks/{id}", notMine)).andExpect(jsonPath("$.permissions.canEdit").value(false));

		mvc.perform(putAs(member, "/api/tasks/{id}", body("title", "Mine, renamed", "priority", "HIGH"), mine))
			.andExpect(status().isOk());
		move(member, mine, "IN_PROGRESS");

		mvc.perform(putAs(member, "/api/tasks/{id}", body("title", "Grabbed", "priority", "HIGH"), notMine))
			.andExpect(status().isForbidden());
		mvc.perform(patchAs(member, "/api/tasks/{id}/move", body("status", "DONE"), notMine))
			.andExpect(status().isForbidden());
		mvc.perform(putAs(member, "/api/tasks/{id}/assignee", body("assigneeId", member.userId()), notMine))
			.andExpect(status().isForbidden());
		mvc.perform(deleteAs(member, "/api/tasks/{id}", mine)).andExpect(status().isForbidden());
		mvc.perform(postAs(member, "/api/projects/{id}/tasks", body("title", "New"), projectId))
			.andExpect(status().isForbidden());
		mvc.perform(putAs(member, "/api/projects/{id}", body("name", "Renamed"), projectId))
			.andExpect(status().isForbidden());
		mvc.perform(postAs(member, "/api/projects/{id}/tags", body("name", "Mine"), projectId))
			.andExpect(status().isForbidden());
		mvc.perform(putAs(member, "/api/tasks/{id}", body("title", "Mine", "priority", "HIGH", "newTags", list("New tag")),
				mine))
			.andExpect(status().isForbidden());
	}

	@Test
	void itManagersCanSeeEverythingButChangeNothingOutsideTheirProjects() throws Exception {
		Session owner = register("Watched Owner");
		Session itManager = createUser("Viewer IT", Role.IT_MANAGER);
		UUID projectId = createProject(owner, "Monitored");
		UUID taskId = createTask(owner, projectId, body("title", "Visible task"));

		mvc.perform(getAs(itManager, "/api/projects/{id}/board", projectId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.permissions.canManage").value(false))
			.andExpect(jsonPath("$.columns[0].tasks[0].permissions.canMove").value(false));
		mvc.perform(getAs(itManager, "/api/tasks/{id}", taskId)).andExpect(status().isOk());
		mvc.perform(putAs(itManager, "/api/tasks/{id}", body("title", "Edited", "priority", "LOW"), taskId))
			.andExpect(status().isForbidden());
		mvc.perform(patchAs(itManager, "/api/tasks/{id}/move", body("status", "DONE"), taskId))
			.andExpect(status().isForbidden());
		mvc.perform(deleteAs(itManager, "/api/projects/{id}", projectId)).andExpect(status().isForbidden());
		mvc.perform(getAs(itManager, "/api/management/projects?q=Monitored"))
			.andExpect(jsonPath("$.items[*].id", hasItem(projectId.toString())));
	}

	@Test
	void globalMembersCannotCreateProjects() throws Exception {
		Session member = createUser("Plain Member", Role.MEMBER);
		mvc.perform(postAs(member, "/api/projects", body("name", "Not allowed"))).andExpect(status().isForbidden());
	}

	@Test
	void assigneesAndTagsMustBelongToTheTasksProject() throws Exception {
		Session owner = register("Tidy Owner");
		Session stranger = register("Stranger");
		UUID projectA = createProject(owner, "Project A");
		UUID projectB = createProject(owner, "Project B");
		MvcResult tag = mvc.perform(postAs(owner, "/api/projects/{id}/tags", body("name", "Only in B"), projectB))
			.andExpect(status().isCreated())
			.andReturn();
		String foreignTag = read(tag, "$.id");

		mvc.perform(postAs(owner, "/api/projects/{id}/tasks", body("title", "Bad tag", "tagIds", list(foreignTag)), projectA))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.tagIds").exists());
		mvc.perform(postAs(owner, "/api/projects/{id}/tasks", body("title", "Bad assignee", "assigneeId", stranger.userId()),
				projectA))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.fieldErrors.assigneeId").exists());
	}

	@Test
	void removingAMemberUnassignsTheirTasksAndTellsThem() throws Exception {
		Session owner = register("Team Owner");
		Session member = register("Departing Member");
		UUID projectId = createProject(owner, "Team");
		addMember(owner, projectId, member);
		UUID taskId = createTask(owner, projectId, body("title", "Handover", "assigneeId", member.userId()));

		mvc.perform(deleteAs(owner, "/api/projects/{id}/members/{userId}", projectId, member.userId()))
			.andExpect(status().isNoContent());
		mvc.perform(getAs(owner, "/api/tasks/{id}", taskId)).andExpect(jsonPath("$.assignee").doesNotExist());
		mvc.perform(getAs(member, "/api/notifications")).andExpect(jsonPath("$.items[0].type").value("TASK_UNASSIGNED"));
		mvc.perform(getAs(member, "/api/tasks/{id}", taskId)).andExpect(status().isForbidden());
		mvc.perform(deleteAs(owner, "/api/projects/{id}/members/{userId}", projectId, owner.userId()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CANNOT_REMOVE_OWNER"));
	}

	@Test
	void dragAndDropPlacesTheCardBetweenItsNeighbours() throws Exception {
		Session owner = register("Board Owner");
		UUID projectId = createProject(owner, "Board");
		UUID first = createTask(owner, projectId, body("title", "First", "status", "TODO"));
		UUID second = createTask(owner, projectId, body("title", "Second", "status", "TODO"));
		UUID third = createTask(owner, projectId, body("title", "Third", "status", "TODO"));

		mvc.perform(patchAs(owner, "/api/tasks/{id}/move",
				body("status", "TODO", "previousTaskId", first, "nextTaskId", second), third))
			.andExpect(status().isOk());
		MvcResult board = mvc.perform(getAs(owner, "/api/projects/{id}/board", projectId)).andReturn();
		List<String> todo = read(board, "$.columns[1].tasks[*].title");
		assertThat(todo).containsExactly("First", "Third", "Second");

		// Moving across columns to the top of an empty column.
		mvc.perform(patchAs(owner, "/api/tasks/{id}/move", body("status", "IN_PROGRESS"), second)).andExpect(status().isOk());
		board = mvc.perform(getAs(owner, "/api/projects/{id}/board", projectId)).andReturn();
		List<String> inProgress = read(board, "$.columns[2].tasks[*].title");
		assertThat(inProgress).containsExactly("Second");
		List<Integer> totals = read(board, "$.columns[*].totalCount");
		assertThat(totals).containsExactly(0, 2, 1, 0);
	}

	@Test
	void deletingAProjectKeepsItsHistory() throws Exception {
		Session owner = register("Cleanup Owner");
		Session itManager = createUser("Audit IT", Role.IT_MANAGER);
		UUID projectId = createProject(owner, "Short lived");
		createTask(owner, projectId, body("title", "Soon gone"));
		mvc.perform(deleteAs(owner, "/api/projects/{id}", projectId)).andExpect(status().isNoContent());
		mvc.perform(getAs(owner, "/api/projects/{id}", projectId)).andExpect(status().isNotFound());
		mvc.perform(getAs(itManager, "/api/management/activity-logs?userId={id}", owner.userId()))
			.andExpect(jsonPath("$.items[0].action").value("PROJECT_DELETED"))
			.andExpect(jsonPath("$.items[0].projectName").value("Short lived"))
			.andExpect(jsonPath("$.items[*].action", hasItem("TASK_CREATED")));
	}

}
