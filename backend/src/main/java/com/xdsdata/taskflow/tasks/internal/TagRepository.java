package com.xdsdata.taskflow.tasks.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.xdsdata.taskflow.tasks.Tag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, UUID> {

	@Query("select t from Tag t where t.project.id = :projectId order by lower(t.name)")
	List<Tag> findByProject(UUID projectId);

	@Query("select t from Tag t where t.project.id = :projectId and lower(t.name) = lower(:name)")
	Optional<Tag> findByProjectAndName(UUID projectId, String name);

	@Query("select t from Tag t where t.id in :ids")
	List<Tag> findByIds(Collection<UUID> ids);

}
