package com.taskapi.repository;

import com.taskapi.entity.Task;
import com.taskapi.entity.TaskPriority;
import com.taskapi.entity.TaskStatus;
import com.taskapi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    Page<Task> findByUser(User user, Pageable pageable);
    Page<Task> findByUserAndStatus(User user, TaskStatus status, Pageable pageable);
    Page<Task> findByUserAndPriority(User user, TaskPriority priority, Pageable pageable);
    Page<Task> findByUserAndStatusAndPriority(User user, TaskStatus status, TaskPriority priority, Pageable pageable);
}
