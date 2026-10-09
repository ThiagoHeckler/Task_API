package com.thiago.taskapi.task_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@NotBlank(message = "O email é obrigatório")
	@Email(message = "Email inválido")
	String email,

	@NotBlank(message = "A senha é obrigatória")
	String password
) {
}
