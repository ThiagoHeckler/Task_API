package com.thiago.taskapi.task_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
	
	private static final String BEARER_SCHEME = "bearerAuth";
	
	// O SecurityScheme faz aparecer o botão "Authorize" no Swagger UI; o requirement
	// global marca todas as rotas como autenticadas (as públicas desligam com @SecurityRequirements).
	@Bean
	OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Task API")
						.description("API REST de gerenciamento de tarefas com autenticação JWT. "
								+ "Faça login em POST /auth/login e cole o token em \"Authorize\".")
						.version("0.0.1"))
				.components(new Components()
						.addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
	}
}
