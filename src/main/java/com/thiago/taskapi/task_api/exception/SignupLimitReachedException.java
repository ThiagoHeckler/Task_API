package com.thiago.taskapi.task_api.exception;

// O front da demo reconhece o bloqueio do dia por esta mensagem exata em POST /users.
public class SignupLimitReachedException extends RuntimeException {

	public static final String MESSAGE = "Limite de usuários que testaram o sistema hoje atingido";

	public SignupLimitReachedException() {
		super(MESSAGE);
	}
}
