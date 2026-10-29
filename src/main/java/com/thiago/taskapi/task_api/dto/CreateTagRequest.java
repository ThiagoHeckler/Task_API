package com.thiago.taskapi.task_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
	@NotBlank(message = "O nome da tag é obrigatório")
	@Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
	@Schema(example = "urgente")
	String name
) {
}
