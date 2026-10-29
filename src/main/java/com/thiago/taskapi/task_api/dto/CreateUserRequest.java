package com.thiago.taskapi.task_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
	 @NotBlank(message = "O nome é obrigatório")
	 @Schema(example = "Thiago")
	 String name,

	 @NotBlank(message = "O email é obrigatório")
	 @Email(message = "Email inválido")
	 @Schema(example = "thiago@email.com")
	 String email,

	@NotBlank(message = "A senha é obrigatória")
	 @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
	 @Schema(description = "Mínimo de 8 caracteres", example = "senha1234")
	 String password
) {
}
