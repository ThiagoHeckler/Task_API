package com.thiago.taskapi.task_api.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.thiago.taskapi.task_api.security.JwtAuthenticationFilter;
import com.thiago.taskapi.task_api.security.JwtService;
import com.thiago.taskapi.task_api.security.RateLimitFilter;
import com.thiago.taskapi.task_api.security.RestAuthenticationHandler;

import jakarta.servlet.DispatcherType;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
			RestAuthenticationHandler authHandler, JsonMapper jsonMapper,
			@Value("${demo.rate-limit.requests-per-minute}") int requestsPerMinute) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// O despacho interno para /error (404, 500...) é liberado; senão ele perderia a
			// autenticação e todo erro viraria 401. A requisição original continua protegida.
			.authorizeHttpRequests(auth -> auth
					.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
					.requestMatchers(HttpMethod.POST, "/auth/login", "/users").permitAll()
					.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
					.anyRequest().authenticated())
			.exceptionHandling(ex -> ex
					.authenticationEntryPoint(authHandler)
					.accessDeniedHandler(authHandler))
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable())
			// Criados aqui (e não como @Component) para não serem registrados de novo como filtro do servlet.
			// O limite por IP roda antes do JWT: requisição barrada nem chega a validar token.
			.addFilterBefore(new RateLimitFilter(requestsPerMinute, Clock.systemUTC(), jsonMapper),
					UsernamePasswordAuthenticationFilter.class)
			.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}
	
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
