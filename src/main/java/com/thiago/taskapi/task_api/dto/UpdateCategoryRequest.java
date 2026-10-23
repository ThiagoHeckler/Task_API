package com.thiago.taskapi.task_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
		@Schema(example = "Estudos")
		@Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
		String name,
		
		@Schema(example = "#f59e0b")
		@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "A cor deve estar no formato hexadecimal")
		String color
) {
}
