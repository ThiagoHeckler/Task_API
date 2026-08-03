package com.thiago.taskapi.task_api.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.thiago.taskapi.task_api.model.enums.TaskPriority;
import com.thiago.taskapi.task_api.model.enums.TaskStatus;

import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
	@Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
	String title,
	
	String description,
	TaskStatus status,
	TaskPriority priority,
	LocalDateTime dueDate,
	Long categoryId,
	Set<Long> tagIds
) {
}
