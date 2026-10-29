package com.thiago.taskapi.task_api.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
	Instant timestamp,
	int status,
	String error,
	@Schema(example = "Tarefa não encontrada com id: 42")
	String message
) {
}
