package com.thiago.taskapi.task_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thiago.taskapi.task_api.dto.CategoryResponse;
import com.thiago.taskapi.task_api.dto.CreateTagRequest;
import com.thiago.taskapi.task_api.dto.TagResponse;
import com.thiago.taskapi.task_api.dto.UpdateCategoryRequest;
import com.thiago.taskapi.task_api.dto.UpdateTagRequest;
import com.thiago.taskapi.task_api.service.TagService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users/{userId}/tags")
public class TagController {

	private final TagService tagService;
	
	public TagController(TagService tagService) {
		this.tagService = tagService;
	}
	
	@PostMapping
	public ResponseEntity<TagResponse> create(@PathVariable Long userId,@Valid @RequestBody CreateTagRequest request) {
		TagResponse response = tagService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@GetMapping
	public ResponseEntity<List<TagResponse>> findAll(@PathVariable Long userId) {
		return ResponseEntity.ok(tagService.findAllByUser(userId));
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<TagResponse> findById(@PathVariable Long userId, @PathVariable Long id) {
		return ResponseEntity.ok(tagService.findById(id, userId));
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<TagResponse> update(@PathVariable Long userId, @PathVariable Long id, @Valid @RequestBody UpdateTagRequest request) {
		return ResponseEntity.ok(tagService.update(id, userId, request));
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long userId, @PathVariable Long id) {
		tagService.delete(id, userId);
		return ResponseEntity.noContent().build();
	}
}
