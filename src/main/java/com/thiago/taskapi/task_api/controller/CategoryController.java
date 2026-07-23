package com.thiago.taskapi.task_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thiago.taskapi.task_api.dto.CategoryResponse;
import com.thiago.taskapi.task_api.dto.CreateCategoryRequest;
import com.thiago.taskapi.task_api.service.CategoryService;

@RestController
@RequestMapping("/users/{userId}/categories")
public class CategoryController {

	private final CategoryService categoryService;
	
	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}
	
	@PostMapping
	public ResponseEntity<CategoryResponse> create(@PathVariable Long userId, @RequestBody CreateCategoryRequest request) {
		CategoryResponse response = categoryService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@GetMapping
	public ResponseEntity<List<CategoryResponse>> findAll(@PathVariable Long userId) {
		return ResponseEntity.ok(categoryService.findAllByUser(userId));
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<CategoryResponse> findById(@PathVariable Long userId, @PathVariable Long id){
		return ResponseEntity.ok(categoryService.findById(id, userId));
	}
}
