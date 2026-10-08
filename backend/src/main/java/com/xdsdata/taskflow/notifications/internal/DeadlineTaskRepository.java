package com.xdsdata.taskflow.notifications.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.tasks.TaskStatus;
import com.xdsdata.taskflow.users.UserStatus;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface DeadlineTaskRepository extends Repository<Task, UUID> {

	/** Open, assigned tasks due on or before {@code limitDate} whose assignee is still active. */
	@Query("""
			select t from Task t join fetch t.assignee a join fetch t.project
			where t.status <> :done and t.dueDate is not null and t.dueDate <= :limitDate and a.status = :active""")
	List<Task> findDueBy(LocalDate limitDate, TaskStatus done, UserStatus active);

}
