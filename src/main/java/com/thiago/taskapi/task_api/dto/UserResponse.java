package com.thiago.taskapi.task_api.dto;

import java.time.LocalDateTime;

public record UserResponse(
	Long id,
	String name,
	String email,
	LocalDateTime createdAt
) {
}
