package com.thiago.taskapi.task_api.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.thiago.taskapi.task_api.model.enums.TaskPriority;
import com.thiago.taskapi.task_api.model.enums.TaskStatus;

public record TaskResponse(
	Long id,
	String title,
	String description,
	TaskStatus status,
	TaskPriority priority,
	LocalDateTime dueDate,
	LocalDateTime completedAt,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	CategoryResponse category,
	Set<TagResponse> tags,
	Long parentTaskId
) {
}
