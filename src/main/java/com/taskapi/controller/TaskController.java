package com.taskapi.controller;

import com.taskapi.dto.task.TaskFilterRequest;
import com.taskapi.dto.task.TaskRequest;
import com.taskapi.dto.task.TaskResponse;
import com.taskapi.entity.User;
import com.taskapi.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public Page<TaskResponse> listTasks(
            @AuthenticationPrincipal User user,
            @ModelAttribute TaskFilterRequest filter,
            Pageable pageable) {
        return taskService.findAll(user, filter, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TaskRequest request) {
        return taskService.create(user, request);
    }

    @GetMapping("/{id}")
    public TaskResponse getTask(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return taskService.findById(user, id);
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody TaskRequest request) {
        return taskService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        taskService.delete(user, id);
    }
}
