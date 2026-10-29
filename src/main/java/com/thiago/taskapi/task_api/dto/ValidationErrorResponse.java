package com.thiago.taskapi.task_api.dto;

import java.time.Instant;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

public record ValidationErrorResponse(
	Instant timestamp,
	int status,
	String error,
	@Schema(description = "Mensagem de erro por campo", example = "{\"title\": \"O título é obrigatório\"}")
	Map<String, String> fields
) {
}
