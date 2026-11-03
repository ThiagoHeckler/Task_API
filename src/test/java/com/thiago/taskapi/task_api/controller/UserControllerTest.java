package com.thiago.taskapi.task_api.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.thiago.taskapi.task_api.exception.SignupLimitReachedException;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "demo.signup.daily-limit=0")
class UserControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void create_returns429WithDailyLimitMessage() throws Exception {
		mockMvc.perform(post("/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Visitante", "email": "limite@teste.com", "password": "senha1234"}
								"""))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.message").value(SignupLimitReachedException.MESSAGE))
				.andExpect(header().string("Retry-After", matchesPattern("[1-9][0-9]*")));
	}
}
