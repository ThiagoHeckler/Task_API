package com.thiago.taskapi.task_api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import tools.jackson.databind.json.JsonMapper;

class RateLimitFilterTest {

	private static final int LIMIT = 2;

	MutableClock clock = new MutableClock(Instant.parse("2026-10-28T12:00:10Z"));
	RateLimitFilter filter;

	@BeforeEach
	void setUp() {
		filter = new RateLimitFilter(LIMIT, clock, JsonMapper.builder().findAndAddModules().build());
	}

	@Test
	void requestsWithinLimit_passThrough() throws Exception {
		for (int i = 0; i < LIMIT; i++) {
			MockFilterChain chain = new MockFilterChain();
			MockHttpServletResponse response = send("1.1.1.1", "/tasks", chain);
			assertThat(response.getStatus()).isEqualTo(200);
			assertThat(chain.getRequest()).isNotNull();
		}
	}

	@Test
	void exceedingLimit_returns429ErrorResponse() throws Exception {
		send("1.1.1.1", "/tasks");
		send("1.1.1.1", "/tasks");
		MockFilterChain chain = new MockFilterChain();

		MockHttpServletResponse response = send("1.1.1.1", "/tasks", chain);

		assertThat(response.getStatus()).isEqualTo(429);
		assertThat(chain.getRequest()).isNull();
		assertThat(response.getHeader("Retry-After")).isEqualTo("50");
		assertThat(response.getContentAsString())
				.contains("\"status\":429")
				.contains("Muitas requisições. Aguarde um minuto e tente novamente.");
	}

	@Test
	void limitIsPerIp() throws Exception {
		send("1.1.1.1", "/tasks");
		send("1.1.1.1", "/tasks");

		assertThat(send("2.2.2.2", "/tasks").getStatus()).isEqualTo(200);
	}

	@Test
	void newMinute_resetsCounter() throws Exception {
		send("1.1.1.1", "/tasks");
		send("1.1.1.1", "/tasks");
		clock.instant = Instant.parse("2026-10-28T12:01:00Z");

		assertThat(send("1.1.1.1", "/tasks").getStatus()).isEqualTo(200);
	}

	@Test
	void swaggerStaticAssets_areNotLimited() throws Exception {
		for (int i = 0; i < 5; i++) {
			assertThat(send("1.1.1.1", "/swagger-ui/index.html").getStatus()).isEqualTo(200);
		}
	}

	private MockHttpServletResponse send(String ip, String uri) throws Exception {
		return send(ip, uri, new MockFilterChain());
	}

	private MockHttpServletResponse send(String ip, String uri, MockFilterChain chain) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
		request.setRemoteAddr(ip);
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, chain);
		return response;
	}

	private static class MutableClock extends Clock {

		Instant instant;

		MutableClock(Instant instant) {
			this.instant = instant;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
