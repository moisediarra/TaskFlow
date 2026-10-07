package com.xdsdata.taskflow.tasks.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.xdsdata.taskflow.common.DueState;
import com.xdsdata.taskflow.tasks.Task;
import com.xdsdata.taskflow.tasks.TaskDtos.AssigneeDto;
import com.xdsdata.taskflow.tasks.TaskDtos.TagDto;
import com.xdsdata.taskflow.tasks.TaskDtos.TaskCardDto;
import com.xdsdata.taskflow.tasks.TaskDtos.TaskDetailDto;
import com.xdsdata.taskflow.tasks.TaskDtos.TaskPermissions;

import org.springframework.stereotype.Component;

@Component
class TaskMapper {

	private final Clock clock;

	TaskMapper(Clock clock) {
		this.clock = clock;
	}

	LocalDate today() {
		return LocalDate.now(clock);
	}

	TaskCardDto card(Task task, TaskPermissions permissions, LocalDate today) {
		return new TaskCardDto(task.getId(), task.getTitle(), task.getStatus(), task.getPriority(), task.getDueDate(),
				DueState.of(task.getDueDate(), task.isDone(), today), task.getPosition(),
				AssigneeDto.from(task.getAssignee()), tags(task), permissions, task.getUpdatedAt());
	}

	TaskDetailDto detail(Task task, TaskPermissions permissions) {
		return new TaskDetailDto(task.getId(), task.getProject().getId(), task.getProject().getName(), task.getTitle(),
				task.getDescription(), task.getStatus(), task.getPriority(), task.getDueDate(),
				DueState.of(task.getDueDate(), task.isDone(), today()), AssigneeDto.from(task.getAssignee()), tags(task),
				permissions, task.getCreatedAt(), task.getUpdatedAt());
	}

	private static List<TagDto> tags(Task task) {
		return task.getTags()
			.stream()
			.sorted(Comparator.comparing(tag -> tag.getName().toLowerCase()))
			.map(TagDto::from)
			.toList();
	}

}
