package com.thiago.taskapi.task_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
	@Schema(description = "Token JWT para o header Authorization", example = "eyJhbGciOiJIUzI1NiJ9...")
	String token,
	@Schema(example = "Bearer")
	String type,
	@Schema(description = "Validade do token em segundos", example = "3600")
	long expiresIn
) {
}
