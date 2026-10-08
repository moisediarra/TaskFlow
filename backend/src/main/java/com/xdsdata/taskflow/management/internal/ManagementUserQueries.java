package com.xdsdata.taskflow.management.internal;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.projects.ProjectMember;
import com.xdsdata.taskflow.users.User;
import com.xdsdata.taskflow.users.UserStatus;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Organization-wide user and membership queries for the IT Management pages. */
interface ManagementUserQueries extends Repository<User, UUID>, JpaSpecificationExecutor<User> {

	@Query("select count(u) from User u")
	long countUsers();

	@Query("select count(u) from User u where u.status = :status")
	long countUsersWithStatus(UserStatus status);

	@Query("select count(p) from Project p")
	long countProjects();

	/** Active users who belong to at least one project: the people whose workload is shown. */
	@Query("""
			select u from User u
			where u.status = :status and exists (select m.id from ProjectMember m where m.user = u)
			order by u.name""")
	List<User> findWithMembership(UserStatus status);

	@Query("select m.user.id, count(m) from ProjectMember m where m.user.id in :userIds group by m.user.id")
	List<Object[]> countMemberships(Collection<UUID> userIds);

	@Query("select m from ProjectMember m join fetch m.project where m.user.id = :userId order by m.joinedAt desc")
	List<ProjectMember> findMemberships(UUID userId);

	@Query("select count(p) from Project p where p.owner.id = :userId")
	long countOwnedProjects(UUID userId);

}
