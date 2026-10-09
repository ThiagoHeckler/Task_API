package com.thiago.taskapi.task_api.dto;

public record LoginResponse(
	String token,
	String type,
	long expiresIn
) {
}
