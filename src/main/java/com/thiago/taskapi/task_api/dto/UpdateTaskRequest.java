package com.thiago.taskapi.task_api.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.thiago.taskapi.task_api.model.enums.TaskPriority;
import com.thiago.taskapi.task_api.model.enums.TaskStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Exemplo do corpo inteiro: o "Try it out" só envia campos que funcionam para um usuário novo.
@Schema(example = "{\"status\": \"IN_PROGRESS\"}")
public record UpdateTaskRequest(
	// null = não alterar; por isso @Pattern em vez de @NotBlank (que recusaria o null).
	@Schema(example = "Estudar JWT e refresh token")
	@Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
	@Pattern(regexp = ".*\\S.*", message = "O título não pode ficar em branco")
	String title,

	String description,

	@Schema(example = "IN_PROGRESS")
	TaskStatus status,

	TaskPriority priority,
	LocalDateTime dueDate,
	Long categoryId,

	@Schema(description = "Substitui as tags atuais; [] remove todas", example = "[1]")
	Set<Long> tagIds
) {
}
