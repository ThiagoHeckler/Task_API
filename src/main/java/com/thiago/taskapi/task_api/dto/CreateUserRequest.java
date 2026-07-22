package com.thiago.taskapi.task_api.dto;

public record CreateUserRequest(
		String name,
		String email,
		String password
) {
}
