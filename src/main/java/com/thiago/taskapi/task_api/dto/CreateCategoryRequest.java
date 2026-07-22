package com.thiago.taskapi.task_api.dto;

public record CreateCategoryRequest(
		String name,
		String color
) {
}
