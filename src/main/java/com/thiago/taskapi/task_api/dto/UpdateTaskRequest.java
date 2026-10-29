package com.thiago.taskapi.task_api.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.thiago.taskapi.task_api.model.enums.TaskPriority;
import com.thiago.taskapi.task_api.model.enums.TaskStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Exemplo do corpo inteiro: o "Try it out" só envia campos que funcionam para um usuário novo.
@Schema(example = "{\"status\": \"COMPLETED\"}")
public record UpdateTaskRequest(
	// null = não alterar; por isso @Pattern em vez de @NotBlank (que recusaria o null).
	@Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
	@Pattern(regexp = ".*\\S.*", message = "O título não pode ficar em branco")
	@Schema(description = "Opcional; ausente ou null mantém o valor atual", example = "Estudar Spring Security a fundo")
	String title,
	
	@Schema(description = "Opcional; ausente ou null mantém o valor atual")
	String description,
	@Schema(description = "Ao virar COMPLETED, completedAt é preenchido; ao sair, é limpo", example = "COMPLETED")
	TaskStatus status,
	@Schema(description = "Opcional; ausente ou null mantém o valor atual", example = "LOW")
	TaskPriority priority,
	@Schema(description = "Opcional; ausente ou null mantém o valor atual", example = "2026-12-20T18:00:00")
	LocalDateTime dueDate,
	@Schema(description = "Opcional; ausente ou null mantém o valor atual", example = "1")
	Long categoryId,
	@Schema(description = "Quando enviado, substitui todas as tags da tarefa", example = "[2]")
	Set<Long> tagIds
) {
}
