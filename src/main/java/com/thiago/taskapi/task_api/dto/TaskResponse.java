package com.thiago.taskapi.task_api.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.thiago.taskapi.task_api.model.enums.TaskPriority;
import com.thiago.taskapi.task_api.model.enums.TaskStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record TaskResponse(
	Long id,
	String title,
	String description,
	TaskStatus status,
	TaskPriority priority,
	LocalDateTime dueDate,
	@Schema(description = "Preenchido quando o status vira COMPLETED")
	LocalDateTime completedAt,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	CategoryResponse category,
	Set<TagResponse> tags,
	@Schema(description = "Id da tarefa pai; null em tarefas principais")
	Long parentTaskId
) {
}
