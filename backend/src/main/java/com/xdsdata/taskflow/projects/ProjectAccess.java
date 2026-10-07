package com.xdsdata.taskflow.projects;

import java.util.UUID;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.common.error.ForbiddenException;
import com.xdsdata.taskflow.common.error.NotFoundException;
import com.xdsdata.taskflow.projects.internal.ProjectMemberRepository;
import com.xdsdata.taskflow.projects.internal.ProjectRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Project-level authorization used by every module (claude.md §32): members and IT Managers may read a
 * project, only its owner may change it. A missing project is 404; an existing one the user may not see
 * is 403, so changing an id in a URL never exposes another project's data.
 */
@Component
@Transactional(readOnly = true)
public class ProjectAccess {

	private final ProjectRepository projects;

	private final ProjectMemberRepository members;

	ProjectAccess(ProjectRepository projects, ProjectMemberRepository members) {
		this.projects = projects;
		this.members = members;
	}

	public Project requireReadable(AuthUser user, UUID projectId) {
		Project project = load(projectId);
		if (!canRead(user, project)) {
			throw new ForbiddenException("You don't have access to this project.");
		}
		return project;
	}

	public Project requireManageable(AuthUser user, UUID projectId) {
		Project project = load(projectId);
		if (!isOwner(user, project)) {
			if (!canRead(user, project)) {
				throw new ForbiddenException("You don't have access to this project.");
			}
			throw new ForbiddenException("Only the project owner can do that.");
		}
		return project;
	}

	public boolean canRead(AuthUser user, Project project) {
		return user.isItManager() || isMember(project.getId(), user.id());
	}

	public boolean isOwner(AuthUser user, Project project) {
		return project.getOwnerId().equals(user.id());
	}

	public boolean isMember(UUID projectId, UUID userId) {
		return members.existsByProjectIdAndUserId(projectId, userId);
	}

	public ProjectPermissions permissionsFor(AuthUser user, Project project) {
		boolean owner = isOwner(user, project);
		return new ProjectPermissions(owner, owner);
	}

	private Project load(UUID projectId) {
		return projects.findWithOwner(projectId).orElseThrow(() -> new NotFoundException("Project"));
	}

}
