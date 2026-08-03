package com.thiago.taskapi.task_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
	@NotBlank(message = "O nome da categoria é obrigatório")
	@Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
	String name,

	@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "A cor deve estar no formato hexadecimal (ex: #6366f1)")
	String color
) {
}
