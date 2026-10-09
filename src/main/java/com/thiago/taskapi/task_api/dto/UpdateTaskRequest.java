package com.thiago.taskapi.task_api.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.thiago.taskapi.task_api.model.enums.TaskPriority;
import com.thiago.taskapi.task_api.model.enums.TaskStatus;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
	// null = não alterar; por isso @Pattern em vez de @NotBlank (que recusaria o null).
	@Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
	@Pattern(regexp = ".*\\S.*", message = "O título não pode ficar em branco")
	String title,
	
	String description,
	TaskStatus status,
	TaskPriority priority,
	LocalDateTime dueDate,
	Long categoryId,
	Set<Long> tagIds
) {
}
