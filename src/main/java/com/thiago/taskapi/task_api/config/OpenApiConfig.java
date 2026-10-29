package com.thiago.taskapi.task_api.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.thiago.taskapi.task_api.dto.ErrorResponse;
import com.thiago.taskapi.task_api.dto.ValidationErrorResponse;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
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

	// Os erros que valem para toda rota (401 nas autenticadas, 500 em todas) entram aqui para não
	// repetir em cada método. Os controllers declaram só o código e a descrição dos erros
	// específicos; o corpo é trocado abaixo (400 -> ValidationErrorResponse, demais -> ErrorResponse),
	// porque o springdoc preenche @ApiResponse sem content com o tipo de retorno do método.
	@Bean
	OpenApiCustomizer errorResponsesCustomizer() {
		return openApi -> {
			ModelConverters.getInstance().read(ErrorResponse.class).forEach(openApi.getComponents()::addSchemas);
			ModelConverters.getInstance().read(ValidationErrorResponse.class).forEach(openApi.getComponents()::addSchemas);

			openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
				ApiResponses responses = operation.getResponses();
				if (isAuthenticated(operation)) {
					responses.putIfAbsent("401", new ApiResponse().description("Token ausente, inválido ou expirado"));
				}
				responses.putIfAbsent("500", new ApiResponse().description("Erro inesperado no servidor"));

				responses.forEach((code, response) -> {
					boolean isError = code.startsWith("4") || code.startsWith("5");
					if (isError && !hasErrorSchema(response)) {
						String schema = code.equals("400") ? "ValidationErrorResponse" : "ErrorResponse";
						response.setContent(jsonOf(schema));
					}
				});
			}));
		};
	}

	// security == null: herda o requirement global (rota autenticada); lista vazia: rota pública.
	private static boolean isAuthenticated(Operation operation) {
		return operation.getSecurity() == null || !operation.getSecurity().isEmpty();
	}

	// Respeita o corpo de erro declarado explicitamente no controller (ex.: 400 de ?status= inválido).
	private static boolean hasErrorSchema(ApiResponse response) {
		return response.getContent() != null && response.getContent().values().stream()
				.map(mediaType -> mediaType.getSchema() == null ? null : mediaType.getSchema().get$ref())
				.anyMatch(ref -> ref != null && ref.endsWith("ErrorResponse"));
	}

	private static Content jsonOf(String schemaName) {
		return new Content().addMediaType("application/json",
				new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + schemaName)));
	}
}
