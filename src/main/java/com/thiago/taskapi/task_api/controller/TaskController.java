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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thiago.taskapi.task_api.dto.CreateTaskRequest;
import com.thiago.taskapi.task_api.dto.TaskResponse;
import com.thiago.taskapi.task_api.dto.UpdateTaskRequest;
import com.thiago.taskapi.task_api.model.enums.TaskStatus;
import com.thiago.taskapi.task_api.service.TaskService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users/{userId}/tasks")
public class TaskController {

	private final TaskService taskService;
	
	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}
	
	@PostMapping
	public ResponseEntity<TaskResponse> create(@PathVariable Long userId,@Valid @RequestBody CreateTaskRequest request) {
		TaskResponse response = taskService.create(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@GetMapping
	public ResponseEntity<List<TaskResponse>> findAll(@PathVariable Long userId, @RequestParam(required = false) TaskStatus status){
		if (status != null) {
			return ResponseEntity.ok(taskService.findByStatus(userId, status));
		}
		return ResponseEntity.ok(taskService.findAllByUser(userId));
	}
	
	@GetMapping("/root")
	public ResponseEntity<List<TaskResponse>> findRoot(@PathVariable Long userId) {
		return ResponseEntity.ok(taskService.findRootTasks(userId));
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<TaskResponse> findById(@PathVariable Long userId, @PathVariable Long id){
		return ResponseEntity.ok(taskService.findById(id, userId));
	}
	
	@PutMapping("/{id}")
	public ResponseEntity<TaskResponse> update(@PathVariable Long userId, @PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
		return ResponseEntity.ok(taskService.update(id, userId, request));
	}
	
	@DeleteMapping("{id}")
	public ResponseEntity<Void> delete(@PathVariable Long userId, @PathVariable Long id) {
		taskService.delete(id, userId);
		return ResponseEntity.noContent().build();
	}
}
