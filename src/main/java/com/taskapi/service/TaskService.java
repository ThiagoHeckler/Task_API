package com.taskapi.service;

import com.taskapi.dto.task.TaskFilterRequest;
import com.taskapi.dto.task.TaskRequest;
import com.taskapi.dto.task.TaskResponse;
import com.taskapi.entity.Task;
import com.taskapi.entity.TaskPriority;
import com.taskapi.entity.TaskStatus;
import com.taskapi.entity.User;
import com.taskapi.exception.ResourceNotFoundException;
import com.taskapi.exception.UnauthorizedAccessException;
import com.taskapi.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public Page<TaskResponse> findAll(User user, TaskFilterRequest filter, Pageable pageable) {
        boolean hasStatus = filter.status() != null;
        boolean hasPriority = filter.priority() != null;

        Page<Task> tasks;
        if (hasStatus && hasPriority) {
            tasks = taskRepository.findByUserAndStatusAndPriority(user, filter.status(), filter.priority(), pageable);
        } else if (hasStatus) {
            tasks = taskRepository.findByUserAndStatus(user, filter.status(), pageable);
        } else if (hasPriority) {
            tasks = taskRepository.findByUserAndPriority(user, filter.priority(), pageable);
        } else {
            tasks = taskRepository.findByUser(user, pageable);
        }
        return tasks.map(TaskResponse::from);
    }

    public TaskResponse create(User user, TaskRequest request) {
        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .status(request.status() != null ? request.status() : TaskStatus.TODO)
                .priority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM)
                .dueDate(request.dueDate())
                .user(user)
                .build();
        return TaskResponse.from(taskRepository.save(task));
    }

    public TaskResponse findById(User user, UUID id) {
        return TaskResponse.from(getTaskOwnedByUser(user, id));
    }

    public TaskResponse update(User user, UUID id, TaskRequest request) {
        Task task = getTaskOwnedByUser(user, id);
        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.status() != null) task.setStatus(request.status());
        if (request.priority() != null) task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        return TaskResponse.from(taskRepository.save(task));
    }

    public void delete(User user, UUID id) {
        taskRepository.delete(getTaskOwnedByUser(user, id));
    }

    private Task getTaskOwnedByUser(User user, UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        if (!task.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("Access denied");
        }
        return task;
    }
}
