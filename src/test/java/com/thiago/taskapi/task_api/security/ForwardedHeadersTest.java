package com.thiago.taskapi.task_api.security;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// Simula o Caddy na frente da API: prefixo /api removido e IP do visitante em X-Forwarded-For.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "demo.rate-limit.requests-per-minute=2")
class ForwardedHeadersTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void apiDocsUseForwardedPrefix() throws Exception {
		mockMvc.perform(get("/v3/api-docs").header("X-Forwarded-Prefix", "/api"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.servers[0].url").value(endsWith("/api")));
	}

	@Test
	void differentForwardedClients_haveSeparateLimits() throws Exception {
		login("10.0.0.1");
		login("10.0.0.1");

		login("10.0.0.2").andExpect(status().is(not(429)));
		login("10.0.0.1").andExpect(status().isTooManyRequests());
	}

	// Atrás do Caddy o contextPath vira /api; a exclusão do Swagger estático precisa continuar valendo.
	@Test
	void swaggerStaticAssetsBehindPrefix_areNotLimited() throws Exception {
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(get("/swagger-ui/index.html")
							.header("X-Forwarded-Prefix", "/api")
							.header("X-Forwarded-For", "10.0.0.3"))
					.andExpect(status().is(not(429)));
		}
	}

	// Corpo inválido de propósito: só interessa se a requisição passou ou não pelo limite.
	private ResultActions login(String clientIp) throws Exception {
		return mockMvc.perform(post("/auth/login")
				.header("X-Forwarded-For", clientIp)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"));
	}
}
