package com.thiago.taskapi.task_api.exception;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.thiago.taskapi.task_api.dto.ErrorResponse;
import com.thiago.taskapi.task_api.dto.ValidationErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex){
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error); 
	}
	
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex){
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				"Rota não encontrada: /" + ex.getResourcePath()
		);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}
	
	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex){
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}
	
	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex){
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				HttpStatus.UNAUTHORIZED.value(),
				HttpStatus.UNAUTHORIZED.getReasonPhrase(),
				ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}
	
	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex){
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				HttpStatus.UNPROCESSABLE_CONTENT.value(),
				HttpStatus.UNPROCESSABLE_CONTENT.getReasonPhrase(),
				ex.getMessage()
		);
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(error);
	}
	
	// Retry-After aponta para a meia-noite de São Paulo, quando o teto diário zera.
	@ExceptionHandler(SignupLimitReachedException.class)
	public ResponseEntity<ErrorResponse> handleSignupLimit(SignupLimitReachedException ex){
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				HttpStatus.TOO_MANY_REQUESTS.value(),
				HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
				ex.getMessage()
		);
		ZoneId saoPaulo = ZoneId.of("America/Sao_Paulo");
		ZonedDateTime nextMidnight = LocalDate.now(saoPaulo).plusDays(1).atStartOfDay(saoPaulo);
		long secondsUntilReset = Duration.between(Instant.now(), nextMidnight.toInstant()).toSeconds();
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
				.header(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(secondsUntilReset, 1)))
				.body(error);
	}
	
	// Rede de segurança para violações que escaparam das checagens do service
	// (ex.: duas requisições simultâneas criando a mesma tag). Não expõe o SQL.
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex){
		log.warn("Violação de integridade no banco", ex);
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				"A operação viola uma restrição dos dados"
		);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex){
		Map<String, String> fields = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fields.put(error.getField(), error.getDefaultMessage());
		}
		
		ValidationErrorResponse response = new ValidationErrorResponse(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				fields
			);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreableMessage(HttpMessageNotReadableException ex) {
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Malformed JSON request body"
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}
	
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String expectedType = ex.getRequiredType() != null
				? ex.getRequiredType().getSimpleName()
				: "unknown";
		String message = String.format(
				"Parameter '%s' should be of type %s",
				ex.getName(),
				expectedType
		);
		
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				message
		);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}
	
	// Último recurso: nada inesperado vaza detalhes internos na resposta.
	// Exceções do próprio Spring MVC (405, 415...) carregam o status certo e são respeitadas.
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
		String message = "Erro interno do servidor";
		
		if (ex instanceof org.springframework.web.ErrorResponse springError) {
			status = springError.getStatusCode();
			message = springError.getBody().getDetail();
		} else {
			log.error("Erro inesperado", ex);
		}
		
		HttpStatus resolved = HttpStatus.resolve(status.value());
		ErrorResponse error = new ErrorResponse(
				Instant.now(),
				status.value(),
				resolved != null ? resolved.getReasonPhrase() : "Error",
				message
		);
		return ResponseEntity.status(status).body(error);
	}
}
