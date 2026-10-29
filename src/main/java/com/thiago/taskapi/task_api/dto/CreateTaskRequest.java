package com.thiago.taskapi.task_api.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.thiago.taskapi.task_api.model.enums.TaskPriority;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Exemplo do corpo inteiro: o "Try it out" só envia campos que funcionam para um usuário novo.
@Schema(example = "{\"title\": \"Estudar Spring Security\", \"description\": \"Revisar filtro JWT e SecurityFilterChain\", \"priority\": \"HIGH\", \"dueDate\": \"2026-12-15T18:00:00\"}")
public record CreateTaskRequest(
	@NotBlank(message = "O título é obrigatório")
	@Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
	@Schema(example = "Estudar Spring Security")
	String title,

	@Schema(example = "Revisar filtro JWT e SecurityFilterChain")
	String description,
	@Schema(description = "Padrão: MEDIUM", example = "HIGH")
	TaskPriority priority,
	@Schema(example = "2026-12-15T18:00:00")
	LocalDateTime dueDate,
	@Schema(description = "Categoria do usuário; opcional", example = "1")
	Long categoryId,
	@Schema(description = "Informe para criar uma subtarefa; a tarefa pai não pode ser subtarefa", example = "1")
	Long parentTaskId,
	@Schema(description = "Tags do usuário; opcional", example = "[1, 2]")
	Set<Long> tagIds
) {
}
