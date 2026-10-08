package com.xdsdata.taskflow.notifications;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.notifications.internal.DeadlineNotifier;
import com.xdsdata.taskflow.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationIntegrationTests extends IntegrationTest {

	@Autowired
	private DeadlineNotifier deadlines;

	@Autowired
	private Clock clock;

	@Test
	void deadlineRemindersAreSentOnceEvenWhenTheSweepRunsAgain() throws Exception {
		Session owner = register("Deadline Owner");
		Session member = register("Deadline Member");
		UUID projectId = createProject(owner, "Deadlines");
		addMember(owner, projectId, member);
		LocalDate today = LocalDate.now(clock);
		UUID dueToday = createTask(owner, projectId,
				body("title", "Due today", "dueDate", today.toString(), "assigneeId", member.userId()));
		UUID overdue = createTask(owner, projectId,
				body("title", "Late", "dueDate", today.minusDays(2).toString(), "assigneeId", member.userId()));
		createTask(owner, projectId,
				body("title", "Done already", "dueDate", today.minusDays(5).toString(), "status", "DONE", "assigneeId",
						member.userId()));

		deadlines.sweep();
		deadlines.sweep();

		MvcResult result = mvc.perform(getAs(member, "/api/notifications?limit=50")).andReturn();
		List<String> approaching = read(result, "$.items[?(@.type == 'TASK_DEADLINE_APPROACHING')].taskId");
		List<String> late = read(result, "$.items[?(@.type == 'TASK_OVERDUE')].taskId");
		assertThat(approaching).containsExactly(dueToday.toString());
		assertThat(late).containsExactly(overdue.toString());

		mvc.perform(getAs(member, "/api/tasks/{id}", overdue)).andExpect(jsonPath("$.dueState").value("OVERDUE"));
	}

	@Test
	void completedTasksAreNeverShownAsOverdue() throws Exception {
		Session owner = register("Done Owner");
		UUID projectId = createProject(owner, "Done");
		UUID taskId = createTask(owner, projectId,
				body("title", "Finished late", "dueDate", LocalDate.now(clock).minusDays(3).toString(), "status", "DONE"));
		mvc.perform(getAs(owner, "/api/tasks/{id}", taskId)).andExpect(jsonPath("$.dueState").value("COMPLETED"));
	}

	@Test
	void peopleAreNotNotifiedAboutTheirOwnActions() throws Exception {
		Session owner = register("Self Owner");
		UUID projectId = createProject(owner, "Solo");
		UUID taskId = createTask(owner, projectId, body("title", "My own", "assigneeId", owner.userId()));
		move(owner, taskId, "DONE");
		mvc.perform(getAs(owner, "/api/notifications")).andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	void notificationsCanBeReadOnlyByTheirRecipient() throws Exception {
		Session owner = register("Notify Owner");
		Session member = register("Notify Member");
		Session other = register("Notify Other");
		UUID projectId = createProject(owner, "Reading");
		addMember(owner, projectId, member);
		createTask(owner, projectId, body("title", "Assigned 1", "assigneeId", member.userId()));
		createTask(owner, projectId, body("title", "Assigned 2", "assigneeId", member.userId()));

		MvcResult list = mvc.perform(getAs(member, "/api/notifications?limit=1"))
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.nextCursor").isNotEmpty())
			.andReturn();
		String first = read(list, "$.items[0].id");
		String cursor = read(list, "$.nextCursor");
		mvc.perform(getAs(member, "/api/notifications?limit=1&cursor={c}", cursor))
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(org.hamcrest.Matchers.not(first)));

		mvc.perform(patchAs(other, "/api/notifications/{id}/read", body(), first)).andExpect(status().isForbidden());
		mvc.perform(patchAs(member, "/api/notifications/{id}/read", body(), first)).andExpect(status().isNoContent());
		mvc.perform(getAs(member, "/api/notifications/unread-count")).andExpect(jsonPath("$.count").value(1));
		mvc.perform(postAs(member, "/api/notifications/read-all", body())).andExpect(status().isNoContent());
		mvc.perform(getAs(member, "/api/notifications/unread-count")).andExpect(jsonPath("$.count").value(0));
	}

}
