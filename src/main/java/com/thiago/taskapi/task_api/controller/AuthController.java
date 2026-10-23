package com.thiago.taskapi.task_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thiago.taskapi.task_api.dto.ErrorResponse;
import com.thiago.taskapi.task_api.dto.LoginRequest;
import com.thiago.taskapi.task_api.dto.LoginResponse;
import com.thiago.taskapi.task_api.dto.ValidationErrorResponse;
import com.thiago.taskapi.task_api.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login e emissão do token JWT")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	// Rota pública: remove o cadeado global no Swagger.
	@SecurityRequirements
	@Operation(summary = "Faz login e devolve o token",
			description = "Use o `token` no botão \"Authorize\" (ou no header `Authorization: Bearer <token>`). Expira em 1 hora.")
	@ApiResponse(responseCode = "200", description = "Login feito")
	@ApiResponse(responseCode = "400", description = "Dados inválidos",
			content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "Credenciais inválidas",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}
}
