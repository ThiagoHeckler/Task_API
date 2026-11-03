package com.thiago.taskapi.task_api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.thiago.taskapi.task_api.model.Task;
import com.thiago.taskapi.task_api.model.User;

// Roda contra o PostgreSQL local; cada teste é desfeito pelo rollback do @Transactional.
@SpringBootTest
@Transactional
class UserRepositoryTest {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

	@Autowired
	UserRepository userRepository;

	@Autowired
	TaskRepository taskRepository;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	void countCreatedToday_countsNewUser() {
		long before = userRepository.countCreatedToday();

		userRepository.saveAndFlush(newUser("novo@teste.com"));

		assertThat(userRepository.countCreatedToday()).isEqualTo(before + 1);
	}

	@Test
	void countCreatedToday_ignoresYesterdayInSaoPaulo() {
		long before = userRepository.countCreatedToday();
		User yesterday = userRepository.saveAndFlush(newUser("ontem@teste.com"));
		User today = userRepository.saveAndFlush(newUser("hoje@teste.com"));
		LocalDate todayInSaoPaulo = LocalDate.now(SAO_PAULO);
		setCreatedAt(yesterday, todayInSaoPaulo.minusDays(1).atTime(23, 59));
		setCreatedAt(today, todayInSaoPaulo.atTime(LocalTime.of(0, 1)));

		// Só o de 00:01 (horário de SP) entra na contagem de hoje.
		assertThat(userRepository.countCreatedToday()).isEqualTo(before + 1);
	}

	@Test
	void deleteOlderThan24Hours_removesOnlyOldUsersAndCascades() {
		User old = userRepository.saveAndFlush(newUser("velho@teste.com"));
		User recent = userRepository.saveAndFlush(newUser("recente@teste.com"));
		Task task = new Task();
		task.setTitle("Tarefa do usuário velho");
		task.setUser(old);
		taskRepository.saveAndFlush(task);
		jdbcTemplate.update("UPDATE users SET created_at = now() - interval '25 hours' WHERE id = ?", old.getId());
		jdbcTemplate.update("UPDATE users SET created_at = now() - interval '1 hour' WHERE id = ?", recent.getId());

		int deleted = userRepository.deleteOlderThan24Hours();

		assertThat(deleted).isGreaterThanOrEqualTo(1);
		assertThat(count("SELECT count(*) FROM users WHERE id = ?", old.getId())).isZero();
		assertThat(count("SELECT count(*) FROM users WHERE id = ?", recent.getId())).isEqualTo(1);
		assertThat(count("SELECT count(*) FROM tasks WHERE user_id = ?", old.getId())).isZero();
	}

	private Integer count(String sql, Long id) {
		return jdbcTemplate.queryForObject(sql, Integer.class, id);
	}

	private User newUser(String email) {
		User user = new User();
		user.setName("Teste");
		user.setEmail(email);
		user.setPasswordHash("hash");
		return user;
	}

	// created_at é TIMESTAMP gravado no fuso da sessão, que o driver JDBC alinha ao fuso da JVM.
	private void setCreatedAt(User user, LocalDateTime saoPauloTime) {
		LocalDateTime sessionTime = ZonedDateTime.of(saoPauloTime, SAO_PAULO)
				.withZoneSameInstant(ZoneId.systemDefault())
				.toLocalDateTime();
		jdbcTemplate.update("UPDATE users SET created_at = ? WHERE id = ?", sessionTime, user.getId());
	}
}
