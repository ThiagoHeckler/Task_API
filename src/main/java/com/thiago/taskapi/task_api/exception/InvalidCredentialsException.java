package com.thiago.taskapi.task_api.exception;

public class InvalidCredentialsException extends RuntimeException {
	
	public InvalidCredentialsException(String message) {
		super(message);
	}
}
