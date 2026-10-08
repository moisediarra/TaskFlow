package com.xdsdata.taskflow.search.internal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.projects.Project;
import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.users.User;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Search queries; the {@code ilike} conditions are served by the trigram indexes from V2__search.sql.
 * Task results are scoped to the caller's projects inside the query, before the limit is applied.
 */
interface SearchQueries extends Repository<Task, UUID> {

	@Query("""
			select t from Task t join fetch t.project p
			where (t.title ilike :pattern or t.description ilike :pattern
			       or exists (select tg.id from Task t2 join t2.tags tg where t2.id = t.id and tg.name ilike :pattern))
			  and p.id in (select m.project.id from ProjectMember m where m.user.id = :userId)
			order by t.updatedAt desc""")
	List<Task> searchTasksInMemberProjects(String pattern, UUID userId, Limit limit);

	@Query("""
			select t from Task t join fetch t.project p
			where t.title ilike :pattern or t.description ilike :pattern
			   or exists (select tg.id from Task t2 join t2.tags tg where t2.id = t.id and tg.name ilike :pattern)
			order by t.updatedAt desc""")
	List<Task> searchAllTasks(String pattern, Limit limit);

	@Query("select t from Task t left join fetch t.tags where t.id in :ids")
	List<Task> findWithTags(Collection<UUID> ids);

	@Query("select u from User u where u.name ilike :pattern or u.email ilike :pattern order by u.name")
	List<User> searchUsers(String pattern, Limit limit);

	@Query("select p from Project p where p.name ilike :pattern order by p.updatedAt desc")
	List<Project> searchProjects(String pattern, Limit limit);

}
