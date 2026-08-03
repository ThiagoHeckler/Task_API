package com.thiago.taskapi.task_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
		@Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
		String name,
		
		@Email(message = "Email inválido")
		String email,
		
		@Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
		String password
) {
}
