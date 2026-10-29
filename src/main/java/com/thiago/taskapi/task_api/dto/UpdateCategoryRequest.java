package com.thiago.taskapi.task_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
		@Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
		@Schema(description = "Opcional; ausente ou null mantém o valor atual", example = "Estudos")
		String name,
		
		@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "A cor deve estar no formato hexadecimal")
		@Schema(description = "Opcional; ausente ou null mantém o valor atual", example = "#22c55e")
		String color
) {
}
