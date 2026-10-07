package com.xdsdata.taskflow.tasks.internal;

import java.util.List;
import java.util.UUID;

import com.xdsdata.taskflow.common.AuthUser;
import com.xdsdata.taskflow.tasks.TaskDtos.BoardDto;
import com.xdsdata.taskflow.tasks.TaskDtos.TagDto;
import com.xdsdata.taskflow.tasks.TaskDtos.TaskCardDto;
import com.xdsdata.taskflow.tasks.TaskDtos.TaskDetailDto;
import com.xdsdata.taskflow.tasks.TaskDtos.TaskSliceDto;
import com.xdsdata.taskflow.tasks.TaskStatus;
import com.xdsdata.taskflow.tasks.internal.TaskRequests.AssignTaskRequest;
import com.xdsdata.taskflow.tasks.internal.TaskRequests.CreateTagRequest;
import com.xdsdata.taskflow.tasks.internal.TaskRequests.CreateTaskRequest;
import com.xdsdata.taskflow.tasks.internal.TaskRequests.MoveTaskRequest;
import com.xdsdata.taskflow.tasks.internal.TaskRequests.UpdateTaskRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class TaskController {

	private final TaskService tasks;

	private final BoardService boards;

	private final TagService tags;

	TaskController(TaskService tasks, BoardService boards, TagService tags) {
		this.tasks = tasks;
		this.boards = boards;
		this.tags = tags;
	}

	@GetMapping("/projects/{projectId}/board")
	BoardDto board(@AuthenticationPrincipal AuthUser user, @PathVariable UUID projectId) {
		return boards.board(user, projectId);
	}

	/** Further pages of a column, used by "Show more" in Done. */
	@GetMapping("/projects/{projectId}/tasks")
	TaskSliceDto column(@AuthenticationPrincipal AuthUser user, @PathVariable UUID projectId,
			@RequestParam TaskStatus status, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
		return boards.column(user, projectId, status, page, size);
	}

	@PostMapping("/projects/{projectId}/tasks")
	ResponseEntity<TaskDetailDto> create(@AuthenticationPrincipal AuthUser user, @PathVariable UUID projectId,
			@Valid @RequestBody CreateTaskRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(tasks.create(user, projectId, request));
	}

	@GetMapping("/tasks/{taskId}")
	TaskDetailDto get(@AuthenticationPrincipal AuthUser user, @PathVariable UUID taskId) {
		return tasks.get(user, taskId);
	}

	@PutMapping("/tasks/{taskId}")
	TaskDetailDto update(@AuthenticationPrincipal AuthUser user, @PathVariable UUID taskId,
			@Valid @RequestBody UpdateTaskRequest request) {
		return tasks.update(user, taskId, request);
	}

	@PutMapping("/tasks/{taskId}/assignee")
	TaskDetailDto assign(@AuthenticationPrincipal AuthUser user, @PathVariable UUID taskId,
			@RequestBody AssignTaskRequest request) {
		return tasks.assign(user, taskId, request.assigneeId());
	}

	@PatchMapping("/tasks/{taskId}/move")
	TaskCardDto move(@AuthenticationPrincipal AuthUser user, @PathVariable UUID taskId,
			@Valid @RequestBody MoveTaskRequest request) {
		return tasks.move(user, taskId, request.status(), request.previousTaskId(), request.nextTaskId());
	}

	@DeleteMapping("/tasks/{taskId}")
	ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable UUID taskId) {
		tasks.delete(user, taskId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/projects/{projectId}/tags")
	List<TagDto> tags(@AuthenticationPrincipal AuthUser user, @PathVariable UUID projectId) {
		return tags.list(user, projectId);
	}

	@PostMapping("/projects/{projectId}/tags")
	ResponseEntity<TagDto> createTag(@AuthenticationPrincipal AuthUser user, @PathVariable UUID projectId,
			@Valid @RequestBody CreateTagRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(tags.create(user, projectId, request.name()));
	}

}
