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

import com.thiago.taskapi.task_api.dto.CreateTagRequest;
import com.thiago.taskapi.task_api.dto.TagResponse;
import com.thiago.taskapi.task_api.dto.UpdateTagRequest;
import com.thiago.taskapi.task_api.service.TagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "Tags", description = "Etiquetas do usuário para marcar tarefas")
@RestController
@RequestMapping("/tags")
public class TagController {

	private final TagService tagService;
	
	public TagController(TagService tagService) {
		this.tagService = tagService;
	}
	
	@PostMapping
	@Operation(summary = "Criar tag")
	@ApiResponse(responseCode = "201", description = "Tag criada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "409", description = "Já existe uma tag com esse nome")
	public ResponseEntity<TagResponse> create(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreateTagRequest request) {
		TagResponse response = tagService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@GetMapping
	@Operation(summary = "Listar tags")
	@ApiResponse(responseCode = "200", description = "Tags do usuário")
	public ResponseEntity<List<TagResponse>> findAll(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(tagService.findAllByUser(userId));
	}
	
	@GetMapping("/{id}")
	@Operation(summary = "Buscar tag por id")
	@ApiResponse(responseCode = "200", description = "Tag encontrada")
	@ApiResponse(responseCode = "404", description = "Tag não encontrada")
	public ResponseEntity<TagResponse> findById(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		return ResponseEntity.ok(tagService.findById(id, userId));
	}
	
	@PutMapping("/{id}")
	@Operation(summary = "Atualizar tag",
			description = "Update parcial: só os campos enviados são alterados.")
	@ApiResponse(responseCode = "200", description = "Tag atualizada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos")
	@ApiResponse(responseCode = "404", description = "Tag não encontrada")
	@ApiResponse(responseCode = "409", description = "Já existe uma tag com esse nome")
	public ResponseEntity<TagResponse> update(@AuthenticationPrincipal Long userId, @PathVariable Long id, @Valid @RequestBody UpdateTagRequest request) {
		return ResponseEntity.ok(tagService.update(id, userId, request));
	}
	
	@DeleteMapping("/{id}")
	@Operation(summary = "Excluir tag",
			description = "A tag é removida das tarefas que a usavam.")
	@ApiResponse(responseCode = "204", description = "Tag excluída")
	@ApiResponse(responseCode = "404", description = "Tag não encontrada")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		tagService.delete(id, userId);
		return ResponseEntity.noContent().build();
	}
}
