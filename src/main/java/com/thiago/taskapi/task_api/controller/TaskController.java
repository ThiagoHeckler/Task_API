package com.thiago.taskapi.task_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thiago.taskapi.task_api.dto.CreateTaskRequest;
import com.thiago.taskapi.task_api.dto.ErrorResponse;
import com.thiago.taskapi.task_api.dto.TaskResponse;
import com.thiago.taskapi.task_api.dto.UpdateTaskRequest;
import com.thiago.taskapi.task_api.model.enums.TaskStatus;
import com.thiago.taskapi.task_api.service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "Tarefas", description = "Tarefas do usuário, com subtarefas, categoria e tags")
@RestController
@RequestMapping("/tasks")
public class TaskController {

	private final TaskService taskService;
	
	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}
	
	@PostMapping
	@Operation(summary = "Criar tarefa",
			description = "Nasce com status PENDING. Com parentTaskId vira subtarefa; só existe um nível de subtarefa.")
	@ApiResponse(responseCode = "201", description = "Tarefa criada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "404", description = "Categoria, tag ou tarefa pai não encontrada")
	@ApiResponse(responseCode = "422", description = "A tarefa pai já é uma subtarefa")
	public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreateTaskRequest request) {
		TaskResponse response = taskService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@GetMapping
	@Operation(summary = "Listar tarefas",
			description = "Use ?status= para filtrar por PENDING, IN_PROGRESS ou COMPLETED.")
	@ApiResponse(responseCode = "200", description = "Tarefas do usuário, incluindo subtarefas")
	@ApiResponse(responseCode = "400", description = "Status inválido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public ResponseEntity<List<TaskResponse>> findAll(@AuthenticationPrincipal Long userId, @RequestParam(required = false) TaskStatus status){
		if (status != null) {
			return ResponseEntity.ok(taskService.findByStatus(userId, status));
		}
		return ResponseEntity.ok(taskService.findAllByUser(userId));
	}
	
	@GetMapping("/root")
	@Operation(summary = "Listar tarefas principais")
	@ApiResponse(responseCode = "200", description = "Tarefas sem tarefa pai")
	public ResponseEntity<List<TaskResponse>> findRoot(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(taskService.findRootTasks(userId));
	}
	
	@GetMapping("/{id}")
	@Operation(summary = "Buscar tarefa por id")
	@ApiResponse(responseCode = "200", description = "Tarefa encontrada")
	@ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
	public ResponseEntity<TaskResponse> findById(@AuthenticationPrincipal Long userId, @PathVariable Long id){
		return ResponseEntity.ok(taskService.findById(id, userId));
	}
	
	@PutMapping("/{id}")
	@Operation(summary = "Atualizar tarefa",
			description = "Update parcial: só os campos enviados são alterados; enviar null não limpa o campo. Ao virar COMPLETED, completedAt é preenchido; ao sair de COMPLETED, é limpo. tagIds, quando enviado, substitui todas as tags.")
	@ApiResponse(responseCode = "200", description = "Tarefa atualizada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "404", description = "Tarefa, categoria ou tag não encontrada")
	public ResponseEntity<TaskResponse> update(@AuthenticationPrincipal Long userId, @PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
		return ResponseEntity.ok(taskService.update(id, userId, request));
	}
	
	@DeleteMapping("/{id}")
	@Operation(summary = "Excluir tarefa",
			description = "As subtarefas são excluídas junto.")
	@ApiResponse(responseCode = "204", description = "Tarefa excluída")
	@ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		taskService.delete(id, userId);
		return ResponseEntity.noContent().build();
	}
}
