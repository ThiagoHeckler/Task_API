package com.thiago.taskapi.task_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thiago.taskapi.task_api.dto.CreateUserRequest;
import com.thiago.taskapi.task_api.dto.ErrorResponse;
import com.thiago.taskapi.task_api.dto.UpdateUserRequest;
import com.thiago.taskapi.task_api.dto.UserResponse;
import com.thiago.taskapi.task_api.dto.ValidationErrorResponse;
import com.thiago.taskapi.task_api.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@Tag(name = "Usuários", description = "Cadastro e conta do usuário autenticado")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	// Rota pública: remove o cadeado global no Swagger.
	@SecurityRequirements
	@Operation(summary = "Cadastra um usuário")
	@ApiResponse(responseCode = "201", description = "Usuário criado")
	@ApiResponse(responseCode = "400", description = "Dados inválidos",
			content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "Email já cadastrado",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "429", description = "Limite diário de cadastros da demo atingido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request){
		UserResponse response = userService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// Cada usuário só enxerga e altera a própria conta; o id vem do token.
	@Operation(summary = "Mostra a conta do usuário autenticado")
	@ApiResponse(responseCode = "200", description = "Dados da conta")
	@ApiResponse(responseCode = "404", description = "Conta excluída (o token ainda não expirou)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/me")
	public ResponseEntity<UserResponse> findMe(@AuthenticationPrincipal Long userId){
		return ResponseEntity.ok(userService.findById(userId));
	}

	@Operation(summary = "Atualiza a conta do usuário autenticado",
			description = "Atualização parcial: só os campos enviados mudam.")
	@ApiResponse(responseCode = "200", description = "Conta atualizada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos",
			content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Conta excluída (o token ainda não expirou)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "Email já cadastrado",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping("/me")
	public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal Long userId, @Valid @RequestBody UpdateUserRequest request) {
		return ResponseEntity.ok(userService.update(userId, request));
	}

	@Operation(summary = "Exclui a conta do usuário autenticado",
			description = "Exclui também as tarefas, categorias e tags do usuário.")
	@ApiResponse(responseCode = "204", description = "Conta excluída")
	@ApiResponse(responseCode = "404", description = "Conta já excluída (o token ainda não expirou)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping("/me")
	public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal Long userId) {
		userService.delete(userId);
		return ResponseEntity.noContent().build();
	}
}
