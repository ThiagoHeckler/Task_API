package com.thiago.taskapi.task_api.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.thiago.taskapi.task_api.dto.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

// Janela fixa por minuto, em memória (a API roda numa instância só). Atrás do Caddy o
// remoteAddr já é o IP do visitante: o Spring o lê do X-Forwarded-For (forward-headers-strategy).
public class RateLimitFilter extends OncePerRequestFilter {

	private static final String MESSAGE = "Muitas requisições. Aguarde um minuto e tente novamente.";

	private final int requestsPerMinute;
	private final Clock clock;
	private final JsonMapper jsonMapper;

	private volatile long currentMinute = -1;
	private volatile Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

	public RateLimitFilter(int requestsPerMinute, Clock clock, JsonMapper jsonMapper) {
		this.requestsPerMinute = requestsPerMinute;
		this.clock = clock;
		this.jsonMapper = jsonMapper;
	}

	// Os arquivos estáticos do Swagger UI ficam de fora: abrir a página já baixa vários deles.
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return path.startsWith("/swagger-ui/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		Instant now = clock.instant();
		int count = countersFor(now.getEpochSecond() / 60)
				.computeIfAbsent(request.getRemoteAddr(), ip -> new AtomicInteger())
				.incrementAndGet();

		if (count > requestsPerMinute) {
			long secondsLeftInMinute = 60 - now.getEpochSecond() % 60;
			writeTooManyRequests(response, secondsLeftInMinute);
			return;
		}
		filterChain.doFilter(request, response);
	}

	// Ao virar o minuto, troca o mapa inteiro: os contadores antigos somem e a memória não cresce.
	private Map<String, AtomicInteger> countersFor(long minute) {
		if (minute != currentMinute) {
			synchronized (this) {
				if (minute != currentMinute) {
					counters = new ConcurrentHashMap<>();
					currentMinute = minute;
				}
			}
		}
		return counters;
	}

	private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws IOException {
		ErrorResponse error = new ErrorResponse(
				Instant.now(clock),
				HttpStatus.TOO_MANY_REQUESTS.value(),
				HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
				MESSAGE
		);
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		jsonMapper.writeValue(response.getOutputStream(), error);
	}
}
