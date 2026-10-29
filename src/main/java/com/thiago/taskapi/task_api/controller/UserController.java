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
import com.thiago.taskapi.task_api.dto.UpdateUserRequest;
import com.thiago.taskapi.task_api.dto.UserResponse;
import com.thiago.taskapi.task_api.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Usuários", description = "Cadastro e conta do usuário autenticado")
@RestController
@RequestMapping("/users")
public class UserController {

	private final UserService userService;
	
	public UserController(UserService userService) {
		this.userService = userService;
	}
	
	// Rota pública: remove o cadeado global no Swagger.
	@SecurityRequirements
	@PostMapping
	@Operation(summary = "Cadastrar usuário",
			description = "Rota pública. A senha é gravada com BCrypt e nunca volta na resposta.")
	@ApiResponse(responseCode = "201", description = "Usuário criado")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "409", description = "Email já cadastrado")
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request){
		UserResponse response = userService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	// Cada usuário só enxerga e altera a própria conta; o id vem do token.
	@GetMapping("/me")
	@Operation(summary = "Ver a própria conta")
	@ApiResponse(responseCode = "200", description = "Dados do usuário do token")
	public ResponseEntity<UserResponse> findMe(@AuthenticationPrincipal Long userId){
		return ResponseEntity.ok(userService.findById(userId));
	}
	
	@PutMapping("/me")
	@Operation(summary = "Atualizar a própria conta",
			description = "Update parcial: só os campos enviados são alterados.")
	@ApiResponse(responseCode = "200", description = "Conta atualizada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "409", description = "Email já cadastrado")
	public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal Long userId, @Valid @RequestBody UpdateUserRequest request) {
		return ResponseEntity.ok(userService.update(userId, request));
	}
	
	@DeleteMapping("/me")
	@Operation(summary = "Excluir a própria conta",
			description = "Remove também as tarefas, categorias e tags do usuário.")
	@ApiResponse(responseCode = "204", description = "Conta excluída")
	public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal Long userId) {
		userService.delete(userId);
		return ResponseEntity.noContent().build();
	}
}
