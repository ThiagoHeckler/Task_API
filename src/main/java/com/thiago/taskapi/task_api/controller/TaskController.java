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
import com.thiago.taskapi.task_api.dto.ValidationErrorResponse;
import com.thiago.taskapi.task_api.model.enums.TaskStatus;
import com.thiago.taskapi.task_api.service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/tasks")
@Tag(name = "Tarefas", description = "Tarefas e subtarefas do usuário autenticado")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@Operation(summary = "Cria uma tarefa",
			description = "Com `parentTaskId`, cria uma subtarefa. Só existe um nível: subtarefa não pode ter subtarefas.")
	@ApiResponse(responseCode = "201", description = "Tarefa criada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos",
			content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Categoria, tag ou tarefa pai não encontrada",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "422", description = "A tarefa pai já é uma subtarefa",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreateTaskRequest request) {
		TaskResponse response = taskService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Lista as tarefas", description = "Inclui subtarefas. Pode filtrar por status.")
	@ApiResponse(responseCode = "200", description = "Lista de tarefas")
	@ApiResponse(responseCode = "400", description = "Status inválido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping
	public ResponseEntity<List<TaskResponse>> findAll(@AuthenticationPrincipal Long userId,
			@Parameter(description = "Filtra pelo status") @RequestParam(required = false) TaskStatus status){
		if (status != null) {
			return ResponseEntity.ok(taskService.findByStatus(userId, status));
		}
		return ResponseEntity.ok(taskService.findAllByUser(userId));
	}

	@Operation(summary = "Lista só as tarefas principais", description = "Tarefas sem tarefa pai (exclui subtarefas).")
	@ApiResponse(responseCode = "200", description = "Lista de tarefas principais")
	@GetMapping("/root")
	public ResponseEntity<List<TaskResponse>> findRoot(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(taskService.findRootTasks(userId));
	}

	@Operation(summary = "Busca uma tarefa pelo id")
	@ApiResponse(responseCode = "200", description = "Tarefa encontrada")
	@ApiResponse(responseCode = "404", description = "Tarefa não encontrada",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{id}")
	public ResponseEntity<TaskResponse> findById(@AuthenticationPrincipal Long userId, @PathVariable Long id){
		return ResponseEntity.ok(taskService.findById(id, userId));
	}

	@Operation(summary = "Atualiza uma tarefa",
			description = "Atualização parcial: só os campos enviados mudam; `null` mantém o valor atual. "
					+ "`tagIds` substitui as tags (`[]` remove todas). Status `COMPLETED` preenche `completedAt`.")
	@ApiResponse(responseCode = "200", description = "Tarefa atualizada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos",
			content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Tarefa, categoria ou tag não encontrada",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping("/{id}")
	public ResponseEntity<TaskResponse> update(@AuthenticationPrincipal Long userId, @PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
		return ResponseEntity.ok(taskService.update(id, userId, request));
	}

	@Operation(summary = "Exclui uma tarefa", description = "As subtarefas são excluídas junto.")
	@ApiResponse(responseCode = "204", description = "Tarefa excluída")
	@ApiResponse(responseCode = "404", description = "Tarefa não encontrada",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		taskService.delete(id, userId);
		return ResponseEntity.noContent().build();
	}
}
