package com.thiago.taskapi.task_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
	@Schema(example = "Trabalho")
	@NotBlank(message = "O nome da categoria é obrigatório")
	@Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
	String name,

	@Schema(description = "Cor em hexadecimal. Padrão: #6366f1", example = "#22c55e")
	@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "A cor deve estar no formato hexadecimal (ex: #6366f1)")
	String color
) {
}
