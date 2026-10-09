package com.thiago.taskapi.task_api.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.thiago.taskapi.task_api.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

// Os erros de autenticação acontecem nos filtros, antes do DispatcherServlet,
// então o GlobalExceptionHandler não os vê. Aqui o JSON é escrito à mão,
// no mesmo formato de ErrorResponse.
@Component
public class RestAuthenticationHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
	
	private final JsonMapper jsonMapper;
	
	public RestAuthenticationHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}
	
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		writeError(response, HttpStatus.UNAUTHORIZED, "Autenticação necessária");
	}
	
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		writeError(response, HttpStatus.FORBIDDEN, "Acesso negado");
	}
	
	private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message
		);
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		jsonMapper.writeValue(response.getOutputStream(), error);
	}
}
