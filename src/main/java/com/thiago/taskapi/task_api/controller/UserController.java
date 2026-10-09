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

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;

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
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request){
		UserResponse response = userService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	// Cada usuário só enxerga e altera a própria conta; o id vem do token.
	@GetMapping("/me")
	public ResponseEntity<UserResponse> findMe(@AuthenticationPrincipal Long userId){
		return ResponseEntity.ok(userService.findById(userId));
	}
	
	@PutMapping("/me")
	public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal Long userId, @Valid @RequestBody UpdateUserRequest request) {
		return ResponseEntity.ok(userService.update(userId, request));
	}
	
	@DeleteMapping("/me")
	public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal Long userId) {
		userService.delete(userId);
		return ResponseEntity.noContent().build();
	}
}
