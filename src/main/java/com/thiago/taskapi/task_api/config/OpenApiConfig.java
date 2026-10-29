package com.thiago.taskapi.task_api.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.thiago.taskapi.task_api.dto.ErrorResponse;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
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

	// Documenta o 401 uma vez só, em toda rota protegida. Rota com security == null herda o
	// requirement global; as públicas têm lista vazia (@SecurityRequirements) e ficam de fora.
	@Bean
	OpenApiCustomizer unauthorizedResponseCustomizer() {
		return openApi -> {
			ModelConverters.getInstance().read(ErrorResponse.class)
					.forEach(openApi.getComponents()::addSchemas);

			ApiResponse unauthorized = new ApiResponse()
					.description("Token ausente, inválido ou expirado")
					.content(new Content().addMediaType("application/json", new MediaType()
							.schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))));

			openApi.getPaths().values().forEach(path -> path.readOperations().stream()
					.filter(operation -> operation.getSecurity() == null)
					.forEach(operation -> operation.getResponses().addApiResponse("401", unauthorized)));

			// O limite por IP (RateLimitFilter) vale para toda operação.
			ApiResponse tooManyRequests = new ApiResponse()
					.description("Limite de requisições por minuto excedido")
					.content(new Content().addMediaType("application/json", new MediaType()
							.schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))));

			openApi.getPaths().values().forEach(path -> path.readOperations()
					.forEach(operation -> operation.getResponses().putIfAbsent("429", tooManyRequests)));
		};
	}
}
