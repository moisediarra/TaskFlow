package com.xdsdata.taskflow.tasks.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.tasks.TaskStatus;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TaskRepository extends JpaRepository<Task, UUID> {

	@Query("select t from Task t join fetch t.project p join fetch p.owner left join fetch t.assignee where t.id = :id")
	Optional<Task> findDetailed(UUID id);

	@Query("""
			select t from Task t left join fetch t.assignee left join fetch t.tags
			where t.project.id = :projectId and t.status in :statuses
			order by t.position, t.id""")
	List<Task> findBoardTasks(UUID projectId, Collection<TaskStatus> statuses);

	@Query("select t.id from Task t where t.project.id = :projectId and t.status = :status order by t.position, t.id")
	List<UUID> findColumnPage(UUID projectId, TaskStatus status, Pageable pageable);

	@Query("select t from Task t left join fetch t.assignee left join fetch t.tags where t.id in :ids")
	List<Task> findWithDetails(Collection<UUID> ids);

	@Query("select t.status, count(t) from Task t where t.project.id = :projectId group by t.status")
	List<Object[]> countByStatus(UUID projectId);

	@Query("select max(t.position) from Task t where t.project.id = :projectId and t.status = :status")
	Double maxPosition(UUID projectId, TaskStatus status);

	@Query("select min(t.position) from Task t where t.project.id = :projectId and t.status = :status")
	Double minPosition(UUID projectId, TaskStatus status);

	@Query("select max(t.position) from Task t where t.project.id = :projectId and t.status = :status and t.id <> :excludedId")
	Double maxPositionExcluding(UUID projectId, TaskStatus status, UUID excludedId);

	@Query("select min(t.position) from Task t where t.project.id = :projectId and t.status = :status and t.id <> :excludedId")
	Double minPositionExcluding(UUID projectId, TaskStatus status, UUID excludedId);

	@Query("select t.position from Task t where t.id = :id")
	Double findPosition(UUID id);

	/** Renumbers a column 1024, 2048, ... keeping its order. Does not touch updated_at. */
	@Modifying
	@Query(value = """
			update tasks t set position = s.rn * 1024
			from (select id, row_number() over (order by position, id) as rn
			      from tasks where project_id = :projectId and status = :status) s
			where t.id = s.id""", nativeQuery = true)
	int renumberColumn(UUID projectId, String status);

	@Query("select t from Task t join fetch t.project where t.project.id = :projectId and t.assignee.id = :assigneeId")
	List<Task> findAssignedInProject(UUID projectId, UUID assigneeId);

	@Query("""
			select t.project.id, count(t), sum(case when t.status in :activeStatuses then 1 else 0 end)
			from Task t where t.project.id in :projectIds group by t.project.id""")
	List<Object[]> countsByProject(Collection<UUID> projectIds, Collection<TaskStatus> activeStatuses);

}
