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
import com.thiago.taskapi.task_api.dto.UpdateCategoryRequest;
import com.thiago.taskapi.task_api.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "Categorias", description = "Categorias do usuário para agrupar tarefas")
@RestController
@RequestMapping("/categories")
public class CategoryController {

	private final CategoryService categoryService;
	
	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}
	
	@PostMapping
	@Operation(summary = "Criar categoria")
	@ApiResponse(responseCode = "201", description = "Categoria criada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "409", description = "Já existe uma categoria com esse nome")
	public ResponseEntity<CategoryResponse> create(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreateCategoryRequest request) {
		CategoryResponse response = categoryService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@GetMapping
	@Operation(summary = "Listar categorias")
	@ApiResponse(responseCode = "200", description = "Categorias do usuário")
	public ResponseEntity<List<CategoryResponse>> findAll(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(categoryService.findAllByUser(userId));
	}
	
	@GetMapping("/{id}")
	@Operation(summary = "Buscar categoria por id")
	@ApiResponse(responseCode = "200", description = "Categoria encontrada")
	@ApiResponse(responseCode = "404", description = "Categoria não encontrada")
	public ResponseEntity<CategoryResponse> findById(@AuthenticationPrincipal Long userId, @PathVariable Long id){
		return ResponseEntity.ok(categoryService.findById(id, userId));
	}
	
	@PutMapping("/{id}")
	@Operation(summary = "Atualizar categoria",
			description = "Update parcial: só os campos enviados são alterados.")
	@ApiResponse(responseCode = "200", description = "Categoria atualizada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "404", description = "Categoria não encontrada")
	@ApiResponse(responseCode = "409", description = "Já existe uma categoria com esse nome")
	public ResponseEntity<CategoryResponse> update(@AuthenticationPrincipal Long userId, @PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest request) {
		return ResponseEntity.ok(categoryService.update(id, userId, request));
	}
	
	@DeleteMapping("/{id}")
	@Operation(summary = "Excluir categoria",
			description = "As tarefas da categoria não são excluídas; ficam sem categoria.")
	@ApiResponse(responseCode = "204", description = "Categoria excluída")
	@ApiResponse(responseCode = "404", description = "Categoria não encontrada")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		categoryService.delete(id, userId);
		return ResponseEntity.noContent().build();
	}
}
