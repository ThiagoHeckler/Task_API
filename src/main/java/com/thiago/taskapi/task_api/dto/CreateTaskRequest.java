package com.thiago.taskapi.task_api.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.thiago.taskapi.task_api.model.enums.TaskPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
	@NotBlank(message = "O título é obrigatório")
	@Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
	String title,

	String description,
	TaskPriority priority,
	LocalDateTime dueDate,
	Long categoryId,
	Long parentTaskId,
	Set<Long> tagIds
) {
}
