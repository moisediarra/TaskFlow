package com.xdsdata.taskflow.search;

import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.common.Role;
import com.xdsdata.taskflow.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class SearchIntegrationTests extends IntegrationTest {

	@Test
	void taskSearchMatchesTitleDescriptionAndTagsWithinTheUsersProjects() throws Exception {
		String marker = "zq" + UUID.randomUUID().toString().substring(0, 6);
		Session owner = register("Search Owner");
		Session outsider = register("Search Outsider");
		UUID projectId = createProject(owner, "Searchable");
		UUID byTitle = createTask(owner, projectId, body("title", "Fix " + marker + " API"));
		UUID byDescription = createTask(owner, projectId, body("title", "Payments", "description", "Uses the " + marker));
		UUID byTag = createTask(owner, projectId, body("title", "Docs", "newTags", list(marker.toUpperCase())));
		createTask(owner, projectId, body("title", "Unrelated"));

		MvcResult result = mvc.perform(getAs(owner, "/api/search?q={q}", marker)).andReturn();
		List<String> ids = read(result, "$.tasks[*].id");
		assertThat(ids).containsExactlyInAnyOrder(byTitle.toString(), byDescription.toString(), byTag.toString());
		mvc.perform(getAs(owner, "/api/search?q={q}", marker)).andExpect(jsonPath("$.users").doesNotExist());

		MvcResult outsiderResult = mvc.perform(getAs(outsider, "/api/search?q={q}", marker)).andReturn();
		List<String> leaked = read(outsiderResult, "$.tasks[*].id");
		assertThat(leaked).isEmpty();
	}

	@Test
	void itManagersSearchUsersProjectsTasksAndActivity() throws Exception {
		String marker = "qx" + UUID.randomUUID().toString().substring(0, 6);
		Session owner = register("Owner " + marker);
		Session itManager = createUser("Global Searcher", Role.IT_MANAGER);
		UUID projectId = createProject(owner, "Project " + marker);
		createTask(owner, projectId, body("title", "Task " + marker));

		mvc.perform(getAs(itManager, "/api/search?q={q}", marker))
			.andExpect(jsonPath("$.users[0].id").value(owner.userId().toString()))
			.andExpect(jsonPath("$.projects[0].id").value(projectId.toString()))
			.andExpect(jsonPath("$.tasks.length()").value(1))
			.andExpect(jsonPath("$.activities.length()").value(org.hamcrest.Matchers.greaterThan(0)));
	}

	@Test
	void shortQueriesReturnNothing() throws Exception {
		Session owner = register("Short Query");
		mvc.perform(getAs(owner, "/api/search?q=a")).andExpect(jsonPath("$.tasks.length()").value(0));
	}

}
