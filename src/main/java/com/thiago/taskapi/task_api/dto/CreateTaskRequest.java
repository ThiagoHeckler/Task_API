package com.thiago.taskapi.task_api.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.thiago.taskapi.task_api.model.enums.TaskPriority;

public record CreateTaskRequest(
		String title,
		String description,
		TaskPriority priority,
		LocalDateTime dueDate,
		Long categoryId,
		Long parentTaskId,
		Set<Long> tagIds
) {
}
