package com.thiago.taskapi.task_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record UpdateTagRequest(
	@Schema(example = "importante")
	@Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
	String name
) {
}
