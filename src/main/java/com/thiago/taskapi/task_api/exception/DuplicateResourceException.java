package com.thiago.taskapi.task_api.exception;

public class DuplicateResourceException extends RuntimeException{

	public DuplicateResourceException(String message) {
		super(message);
	}
}
