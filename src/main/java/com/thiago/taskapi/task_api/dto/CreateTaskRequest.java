package com.thiago.taskapi.task_api.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.thiago.taskapi.task_api.model.enums.TaskPriority;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
	@Schema(example = "Estudar JWT")
	@NotBlank(message = "O título é obrigatório")
	@Size(max = 255, message = "O título deve ter no máximo 255 caracteres")
	String title,

	@Schema(example = "Ler a documentação do JJWT e testar a geração de tokens")
	String description,

	@Schema(description = "Padrão: MEDIUM", example = "HIGH")
	TaskPriority priority,

	@Schema(example = "2026-11-30T18:00:00")
	LocalDateTime dueDate,

	@Schema(description = "Categoria do próprio usuário", example = "1")
	Long categoryId,

	@Schema(description = "Id da tarefa pai, para criar uma subtarefa")
	Long parentTaskId,

	@Schema(description = "Tags do próprio usuário", example = "[1]")
	Set<Long> tagIds
) {
}
