package com.xdsdata.taskflow.dashboard.internal;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.tasks.TaskPriority;
import com.xdsdata.taskflow.tasks.TaskStatus;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Read-only queries behind the user dashboard and the project overview. */
interface DashboardQueries extends Repository<Task, UUID> {

	@Query("select t.status, count(t) from Task t where t.assignee.id = :userId group by t.status")
	List<Object[]> countAssignedByStatus(UUID userId);

	@Query("""
			select t from Task t join fetch t.project
			where t.assignee.id = :userId and t.status <> :done and t.priority = :priority
			order by t.dueDate asc nulls last, t.updatedAt desc""")
	List<Task> findAssignedOpenWithPriority(UUID userId, TaskStatus done, TaskPriority priority, Limit limit);

	@Query("""
			select count(t) from Task t
			where t.assignee.id = :userId and t.status <> :done and t.priority = :priority""")
	long countAssignedOpenWithPriority(UUID userId, TaskStatus done, TaskPriority priority);

	@Query("""
			select t from Task t join fetch t.project
			where t.assignee.id = :userId and t.status <> :done and t.dueDate = :date
			order by t.updatedAt desc""")
	List<Task> findAssignedOpenDueOn(UUID userId, TaskStatus done, LocalDate date, Limit limit);

	@Query("select count(t) from Task t where t.assignee.id = :userId and t.status <> :done and t.dueDate = :date")
	long countAssignedOpenDueOn(UUID userId, TaskStatus done, LocalDate date);

	@Query("""
			select t from Task t join fetch t.project
			where t.assignee.id = :userId and t.status <> :done and t.dueDate < :today
			order by t.dueDate asc, t.updatedAt desc""")
	List<Task> findAssignedOverdue(UUID userId, TaskStatus done, LocalDate today, Limit limit);

	@Query("select count(t) from Task t where t.assignee.id = :userId and t.status <> :done and t.dueDate < :today")
	long countAssignedOverdue(UUID userId, TaskStatus done, LocalDate today);

	@Query("select t.status, count(t) from Task t where t.project.id = :projectId group by t.status")
	List<Object[]> countProjectByStatus(UUID projectId);

	@Query("select count(t) from Task t where t.project.id = :projectId and t.status <> :done and t.dueDate < :today")
	long countProjectOverdue(UUID projectId, TaskStatus done, LocalDate today);

	@Query("""
			select count(t) from Task t
			where t.project.id = :projectId and t.status <> :done and t.priority = :priority""")
	long countProjectOpenWithPriority(UUID projectId, TaskStatus done, TaskPriority priority);

	@Query("""
			select t from Task t join fetch t.assignee
			where t.project.id = :projectId and t.status in :statuses
			order by t.updatedAt desc""")
	List<Task> findProjectAssignedWork(UUID projectId, Collection<TaskStatus> statuses, Limit limit);

}
