package com.taskapi.dto.task;

import com.taskapi.entity.TaskPriority;
import com.taskapi.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record TaskRequest(
        @NotBlank String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate
) {}
