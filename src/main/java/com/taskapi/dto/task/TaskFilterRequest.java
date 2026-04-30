package com.taskapi.dto.task;

import com.taskapi.entity.TaskPriority;
import com.taskapi.entity.TaskStatus;

public record TaskFilterRequest(
        TaskStatus status,
        TaskPriority priority
) {}
