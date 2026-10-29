package com.thiago.taskapi.task_api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class FlywayMigrationTest {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void v1IsApplied() {
		Integer applied = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success", Integer.class);
		assertThat(applied).isEqualTo(1);
	}
}
