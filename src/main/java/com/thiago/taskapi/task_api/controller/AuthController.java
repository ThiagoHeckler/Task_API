package com.thiago.taskapi.task_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thiago.taskapi.task_api.dto.LoginRequest;
import com.thiago.taskapi.task_api.dto.LoginResponse;
import com.thiago.taskapi.task_api.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Autenticação", description = "Login e emissão do token JWT")
@RestController
@RequestMapping("/auth")
public class AuthController {
	
	private final AuthService authService;
	
	public AuthController(AuthService authService) {
		this.authService = authService;
	}
	
	// Rota pública: remove o cadeado global no Swagger.
	@SecurityRequirements
	@PostMapping("/login")
	@Operation(summary = "Fazer login",
			description = "Rota pública. Devolve um token JWT válido por 1 hora; use-o no botão Authorize.")
	@ApiResponse(responseCode = "200", description = "Token emitido")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "401", description = "Email ou senha inválidos")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}
}
