package com.thiago.taskapi.task_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@NotBlank(message = "O email é obrigatório")
	@Email(message = "Email inválido")
	@Schema(example = "thiago@email.com")
	String email,

	@NotBlank(message = "A senha é obrigatória")
	@Schema(example = "senha1234")
	String password
) {
}
