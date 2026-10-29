package com.thiago.taskapi.task_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

// Exemplo do corpo inteiro: o "Try it out" só envia campos que funcionam para um usuário novo.
@Schema(example = "{\"name\": \"Thiago Heckler\"}")
public record UpdateUserRequest(
		@Schema(example = "Thiago Heckler")
		@Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
		String name,
		
		@Email(message = "Email inválido")
		String email,
		
		@Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
		String password
) {
}
