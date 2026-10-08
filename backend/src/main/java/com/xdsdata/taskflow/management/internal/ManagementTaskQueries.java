package com.xdsdata.taskflow.management.internal;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.tasks.TaskStatus;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Organization-wide task queries for the IT Management pages. */
interface ManagementTaskQueries extends Repository<Task, UUID>, JpaSpecificationExecutor<Task> {

	@Query("select t.status, count(t) from Task t group by t.status")
	List<Object[]> countByStatus();

	@Query("select count(t) from Task t where t.status <> :done and t.dueDate < :today")
	long countOverdue(TaskStatus done, LocalDate today);

	@Query("select t.priority, count(t) from Task t where t.status <> :done group by t.priority")
	List<Object[]> countOpenByPriority(TaskStatus done);

	@Query("select count(distinct t.project.id) from Task t where t.status in :statuses")
	long countProjectsWithTasksIn(Collection<TaskStatus> statuses);

	@Query("select t from Task t join fetch t.assignee join fetch t.project where t.status = :status order by t.updatedAt desc")
	List<Task> findAssignedWithStatus(TaskStatus status, Limit limit);

	@Query("""
			select t from Task t left join fetch t.assignee join fetch t.project
			where t.status <> :done and t.dueDate < :today order by t.dueDate asc, t.updatedAt desc""")
	List<Task> findOverdue(TaskStatus done, LocalDate today, Limit limit);

	@Query("""
			select t.assignee.id, t.status, count(t) from Task t
			where t.assignee is not null and t.status in :statuses group by t.assignee.id, t.status""")
	List<Object[]> countAssignedByStatus(Collection<TaskStatus> statuses);

	@Query("""
			select t.assignee.id, count(t) from Task t
			where t.assignee is not null and t.status <> :done and t.dueDate < :today group by t.assignee.id""")
	List<Object[]> countOverdueByAssignee(TaskStatus done, LocalDate today);

	@Query("""
			select t.assignee.id, count(t) from Task t
			where t.assignee.id in :userIds and t.status in :statuses group by t.assignee.id""")
	List<Object[]> countForAssignees(Collection<UUID> userIds, Collection<TaskStatus> statuses);

	@Query("""
			select t from Task t join fetch t.project
			where t.assignee.id = :userId
			order by case when t.status = :done then 1 else 0 end, t.updatedAt desc""")
	List<Task> findAssignedTo(UUID userId, TaskStatus done, Limit limit);

}
