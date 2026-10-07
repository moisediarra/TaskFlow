package com.xdsdata.taskflow.tasks.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.error.BadRequestException;
import com.xdsdata.taskflow.common.error.ForbiddenException;
import com.xdsdata.taskflow.projects.Project;
import com.xdsdata.taskflow.projects.ProjectAccess;
import com.xdsdata.taskflow.tasks.Tag;
import com.xdsdata.taskflow.tasks.TaskDtos.TagDto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class TagService {

	static final int MAX_TAG_LENGTH = 30;

	static final int MAX_TAGS_PER_TASK = 20;

	private final TagRepository tags;

	private final ProjectAccess projectAccess;

	TagService(TagRepository tags, ProjectAccess projectAccess) {
		this.tags = tags;
		this.projectAccess = projectAccess;
	}

	List<TagDto> list(AuthUser user, UUID projectId) {
		projectAccess.requireReadable(user, projectId);
		return tags.findByProject(projectId).stream().map(TagDto::from).toList();
	}

	@Transactional
	TagDto create(AuthUser user, UUID projectId, String name) {
		Project project = projectAccess.requireManageable(user, projectId);
		return TagDto.from(findOrCreate(project, validName(name, "name")));
	}

	/**
	 * Resolves the tags chosen for a task: existing ids must belong to the task's project, and new names
	 * (inline creation) are only allowed for the project owner. Names match case-insensitively.
	 */
	@Transactional
	Set<Tag> resolve(Project project, Collection<UUID> tagIds, Collection<String> newTagNames, boolean mayCreate) {
		Set<Tag> result = new LinkedHashSet<>();
		if (tagIds != null && !tagIds.isEmpty()) {
			Set<UUID> wanted = new HashSet<>(tagIds);
			List<Tag> found = tags.findByIds(wanted);
			boolean foreign = found.stream().anyMatch(tag -> !tag.getProject().getId().equals(project.getId()));
			if (found.size() != wanted.size() || foreign) {
				throw BadRequestException.field("tagIds", "One of the selected tags does not belong to this project.");
			}
			result.addAll(found);
		}
		if (newTagNames != null && !newTagNames.isEmpty()) {
			Map<String, String> unique = new LinkedHashMap<>();
			for (String raw : newTagNames) {
				String name = validName(raw, "newTags");
				unique.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
			}
			if (!mayCreate) {
				throw new ForbiddenException("Only the project owner can create new tags.");
			}
			unique.values().forEach(name -> result.add(findOrCreate(project, name)));
		}
		if (result.size() > MAX_TAGS_PER_TASK) {
			throw BadRequestException.field("tagIds", "A task can have at most 20 tags.");
		}
		return result;
	}

	private Tag findOrCreate(Project project, String name) {
		return tags.findByProjectAndName(project.getId(), name)
			.orElseGet(() -> tags.saveAndFlush(new Tag(project, name, TagPalette.colorFor(name))));
	}

	private static String validName(String raw, String field) {
		String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
		if (name.isEmpty()) {
			throw BadRequestException.field(field, "Tag names can't be empty.");
		}
		if (name.length() > MAX_TAG_LENGTH) {
			throw BadRequestException.field(field, "Tag names must be at most 30 characters.");
		}
		return name;
	}

}
