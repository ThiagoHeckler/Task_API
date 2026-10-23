package com.thiago.taskapi.task_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
	 @Schema(example = "Thiago")
	 @NotBlank(message = "O nome é obrigatório")
	 String name,

	 @Schema(example = "thiago@teste.com")
	 @NotBlank(message = "O email é obrigatório")
	 @Email(message = "Email inválido")
	 String email,

	 @Schema(example = "senhaSegura123")
	 @NotBlank(message = "A senha é obrigatória")
	 @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
	 String password
) {
}
