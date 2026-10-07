package com.xdsdata.taskflow.projects.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.xdsdata.taskflow.projects.ProjectMember;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

	boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

	@Query("select m from ProjectMember m join fetch m.user where m.project.id = :projectId and m.user.id = :userId")
	Optional<ProjectMember> findMembership(UUID projectId, UUID userId);

	@Query("select m from ProjectMember m join fetch m.user where m.project.id = :projectId")
	List<ProjectMember> findByProjectWithUser(UUID projectId);

	@Query("select m from ProjectMember m join fetch m.project p join fetch p.owner where m.user.id = :userId")
	List<ProjectMember> findByUserWithProject(UUID userId);

	@Query("select m.project.id, count(m) from ProjectMember m where m.project.id in :projectIds group by m.project.id")
	List<Object[]> countByProjectIds(Collection<UUID> projectIds);

	@Query("select m.project.id from ProjectMember m where m.user.id = :userId")
	List<UUID> findProjectIdsByUserId(UUID userId);

}
