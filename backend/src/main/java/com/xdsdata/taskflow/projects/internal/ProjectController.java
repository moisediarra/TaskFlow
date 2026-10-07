package com.xdsdata.taskflow.projects.internal;

import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.projects.ProjectDtos.ProjectDetailDto;
import com.xdsdata.taskflow.projects.ProjectDtos.ProjectMemberDto;
import com.xdsdata.taskflow.projects.ProjectDtos.ProjectSummaryDto;
import com.xdsdata.taskflow.projects.ProjectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
class ProjectController {

	private final ProjectService projects;

	ProjectController(ProjectService projects) {
		this.projects = projects;
	}

	@GetMapping
	List<ProjectSummaryDto> list(@AuthenticationPrincipal AuthUser user) {
		return projects.listMine(user);
	}

	@PostMapping
	ResponseEntity<ProjectDetailDto> create(@AuthenticationPrincipal AuthUser user,
			@Valid @RequestBody ProjectRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(projects.create(user, request.name(), request.description()));
	}

	@GetMapping("/{projectId}")
	ProjectDetailDto get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID projectId) {
		return projects.get(user, projectId);
	}

	@PutMapping("/{projectId}")
	ProjectDetailDto update(@AuthenticationPrincipal AuthUser user, @PathVariable UUID projectId,
			@Valid @RequestBody ProjectRequest request) {
		return projects.update(user, projectId, request.name(), request.description());
	}

	@DeleteMapping("/{projectId}")
	ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID projectId) {
		projects.delete(user, projectId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{projectId}/members")
	List<ProjectMemberDto> members(@AuthenticationPrincipal AuthUser user, @PathVariable UUID projectId) {
		return projects.members(user, projectId);
	}

	@PostMapping("/{projectId}/members")
	ResponseEntity<ProjectMemberDto> addMember(@AuthenticationPrincipal AuthUser user, @PathVariable UUID projectId,
			@Valid @RequestBody AddMemberRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(projects.addMember(user, projectId, request.email()));
	}

	@DeleteMapping("/{projectId}/members/{userId}")
	ResponseEntity<Void> removeMember(@AuthenticationPrincipal AuthUser user, @PathVariable UUID projectId,
			@PathVariable UUID userId) {
		projects.removeMember(user, projectId, userId);
		return ResponseEntity.noContent().build();
	}

	record ProjectRequest(
			@NotBlank(message = "Project name is required.") @Size(max = 100, message = "Project name must be at most 100 characters.") String name,
			@Size(max = 2000, message = "Description must be at most 2000 characters.") String description) {
	}

	record AddMemberRequest(@NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") @Size(max = 254) String email) {
	}

}
