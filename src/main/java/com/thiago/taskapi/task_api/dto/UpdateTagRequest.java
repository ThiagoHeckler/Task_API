package com.thiago.taskapi.task_api.dto;

import jakarta.validation.constraints.Size;

public record UpdateTagRequest(
	@Size(max = 50, message = "O nome deve ter no máximo 50 catacteres")
	String name
) {
}
