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
import com.thiago.taskapi.task_api.dto.ErrorResponse;
import com.thiago.taskapi.task_api.dto.TagResponse;
import com.thiago.taskapi.task_api.dto.UpdateTagRequest;
import com.thiago.taskapi.task_api.dto.ValidationErrorResponse;
import com.thiago.taskapi.task_api.service.TagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/tags")
@Tag(name = "Tags", description = "Tags do usuário autenticado (várias por tarefa)")
public class TagController {

	private final TagService tagService;

	public TagController(TagService tagService) {
		this.tagService = tagService;
	}

	@Operation(summary = "Cria uma tag")
	@ApiResponse(responseCode = "201", description = "Tag criada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos",
			content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "Já existe uma tag com esse nome",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PostMapping
	public ResponseEntity<TagResponse> create(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreateTagRequest request) {
		TagResponse response = tagService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Lista as tags")
	@ApiResponse(responseCode = "200", description = "Lista de tags")
	@GetMapping
	public ResponseEntity<List<TagResponse>> findAll(@AuthenticationPrincipal Long userId) {
		return ResponseEntity.ok(tagService.findAllByUser(userId));
	}

	@Operation(summary = "Busca uma tag pelo id")
	@ApiResponse(responseCode = "200", description = "Tag encontrada")
	@ApiResponse(responseCode = "404", description = "Tag não encontrada",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@GetMapping("/{id}")
	public ResponseEntity<TagResponse> findById(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		return ResponseEntity.ok(tagService.findById(id, userId));
	}

	@Operation(summary = "Renomeia uma tag")
	@ApiResponse(responseCode = "200", description = "Tag atualizada")
	@ApiResponse(responseCode = "400", description = "Dados inválidos",
			content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Tag não encontrada",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "Já existe uma tag com esse nome",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@PutMapping("/{id}")
	public ResponseEntity<TagResponse> update(@AuthenticationPrincipal Long userId, @PathVariable Long id, @Valid @RequestBody UpdateTagRequest request) {
		return ResponseEntity.ok(tagService.update(id, userId, request));
	}

	@Operation(summary = "Exclui uma tag", description = "A tag sai das tarefas que a usavam; as tarefas continuam.")
	@ApiResponse(responseCode = "204", description = "Tag excluída")
	@ApiResponse(responseCode = "404", description = "Tag não encontrada",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
		tagService.delete(id, userId);
		return ResponseEntity.noContent().build();
	}
}
