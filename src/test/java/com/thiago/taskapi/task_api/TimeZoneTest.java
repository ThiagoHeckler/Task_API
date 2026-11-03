package com.thiago.taskapi.task_api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class TimeZoneTest {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void jvmRunsInUtc() {
		assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC");
	}

	// created_at é TIMESTAMP sem fuso preenchido pelo now() da sessão: a sessão tem que estar em UTC.
	@Test
	void databaseSessionRunsInUtc() {
		assertThat(jdbcTemplate.queryForObject("SHOW TimeZone", String.class)).isEqualTo("UTC");
	}
}
