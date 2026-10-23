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
import org.springframework.web.bind.annotation.RestController;

import com.thiago.taskapi.task_api.dto.CategoryResponse;
import com.thiago.taskapi.task_api.dto.CreateCategoryRequest;
import com.thiago.taskapi.task_api.dto.ErrorResponse;
import com.thiago.taskapi.task_api.dto.UpdateCategoryRequest;
import com.thiago.taskapi.task_api.dto.ValidationErrorResponse;
import com.thiago.taskapi.task_api.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/categories")
@Tag(name = "Categorias", description = "Categorias do usuário autenticado (uma por tarefa)")
public class CategoryController {

	private final CategoryService categoryService;

	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@Operation(summary = "Cria uma categoria", description = "Sem `color`, usa #6366f1.")
	@ApiResponse(responseCode = "201", description = "Categoria criada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos",
			content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "Já existe uma categoria com esse nome",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<CategoryResponse> create(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreateCategoryRequest request) {
		CategoryResponse response = categoryService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Lista as categorias")
	@ApiResponse(responseCode = "200", description = "Lista de categorias")
	@GetMapping
	public ResponseEntity<List<CategoryResponse>> findAll(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(categoryService.findAllByUser(userId));
	}

	@Operation(summary = "Busca uma categoria pelo id")
	@ApiResponse(responseCode = "200", description = "Categoria encontrada")
	@ApiResponse(responseCode = "404", description = "Categoria não encontrada",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{id}")
	public ResponseEntity<CategoryResponse> findById(@AuthenticationPrincipal Long userId, @PathVariable Long id){
		return ResponseEntity.ok(categoryService.findById(id, userId));
	}

	@Operation(summary = "Atualiza uma categoria", description = "Atualização parcial: só os campos enviados mudam.")
	@ApiResponse(responseCode = "200", description = "Categoria atualizada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos",
			content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Categoria não encontrada",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "Já existe uma categoria com esse nome",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping("/{id}")
	public ResponseEntity<CategoryResponse> update(@AuthenticationPrincipal Long userId, @PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest request) {
		return ResponseEntity.ok(categoryService.update(id, userId, request));
	}

	@Operation(summary = "Exclui uma categoria", description = "As tarefas da categoria continuam, só ficam sem categoria.")
	@ApiResponse(responseCode = "204", description = "Categoria excluída")
	@ApiResponse(responseCode = "404", description = "Categoria não encontrada",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		categoryService.delete(id, userId);
		return ResponseEntity.noContent().build();
	}
}
