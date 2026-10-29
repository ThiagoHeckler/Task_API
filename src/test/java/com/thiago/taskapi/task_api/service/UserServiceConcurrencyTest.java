package com.thiago.taskapi.task_api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import com.thiago.taskapi.task_api.dto.CreateUserRequest;
import com.thiago.taskapi.task_api.exception.SignupLimitReachedException;
import com.thiago.taskapi.task_api.repository.UserRepository;

// Sem @Transactional na classe: cada thread precisa commitar de verdade para a corrida existir.
@SpringBootTest
class UserServiceConcurrencyTest {

	private static final int THREADS = 5;
	private static final String EMAIL_PREFIX = "concorrencia-";

	@Autowired
	UserRepository userRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	TransactionTemplate transactionTemplate;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@AfterEach
	void cleanUp() {
		jdbcTemplate.update("DELETE FROM users WHERE email LIKE ?", EMAIL_PREFIX + "%");
	}

	@Test
	void concurrentSignups_neverExceedDailyLimit() throws Exception {
		int limit = (int) userRepository.countCreatedToday() + 1;
		UserService userService = new UserService(userRepository, passwordEncoder, limit);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(THREADS);
		List<Future<Boolean>> results = new ArrayList<>();

		for (int i = 0; i < THREADS; i++) {
			String email = EMAIL_PREFIX + i + "@teste.com";
			results.add(executor.submit(() -> {
				start.await();
				try {
					transactionTemplate.executeWithoutResult(status ->
							userService.create(new CreateUserRequest("Visitante", email, "senha1234")));
					return true;
				} catch (SignupLimitReachedException e) {
					return false;
				}
			}));
		}
		start.countDown();

		int successes = 0;
		for (Future<Boolean> result : results) {
			if (result.get()) {
				successes++;
			}
		}
		executor.shutdown();

		assertThat(successes).isEqualTo(1);
	}
}
