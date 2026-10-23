package com.thiago.taskapi.task_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@Schema(example = "thiago@teste.com")
	@NotBlank(message = "O email é obrigatório")
	@Email(message = "Email inválido")
	String email,

	@Schema(example = "senhaSegura123")
	@NotBlank(message = "A senha é obrigatória")
	String password
) {
}
