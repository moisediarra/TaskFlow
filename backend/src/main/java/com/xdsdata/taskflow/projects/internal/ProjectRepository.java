package com.xdsdata.taskflow.projects.internal;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.xdsdata.taskflow.projects.Project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

	@Query("select p from Project p join fetch p.owner where p.id = :id")
	Optional<Project> findWithOwner(UUID id);

	@Query(value = "select p from Project p join fetch p.owner where p.name ilike :pattern",
			countQuery = "select count(p) from Project p where p.name ilike :pattern")
	Page<Project> searchByName(String pattern, Pageable pageable);

	/** Bumps "Updated 2 hours ago" when the project's tasks or members change. */
	@Modifying
	@Query("update Project p set p.updatedAt = :now where p.id = :id")
	int touch(UUID id, Instant now);

}
