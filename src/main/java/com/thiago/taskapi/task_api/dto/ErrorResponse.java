package com.thiago.taskapi.task_api.dto;

import java.time.Instant;

public record ErrorResponse(
	Instant timestamp,
	int status,
	String error,
	String message
) {
}
