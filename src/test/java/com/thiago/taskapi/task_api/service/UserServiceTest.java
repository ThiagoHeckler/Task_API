package com.thiago.taskapi.task_api.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.thiago.taskapi.task_api.dto.CreateUserRequest;
import com.thiago.taskapi.task_api.exception.SignupLimitReachedException;
import com.thiago.taskapi.task_api.model.User;
import com.thiago.taskapi.task_api.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	private static final int DAILY_LIMIT = 100;

	@Mock
	UserRepository userRepository;

	@Mock
	PasswordEncoder passwordEncoder;

	UserService userService;

	CreateUserRequest request = new CreateUserRequest("Visitante", "visitante@email.com", "senha1234");

	@BeforeEach
	void setUp() {
		userService = new UserService(userRepository, passwordEncoder, DAILY_LIMIT);
	}

	@Test
	void create_throwsWhenDailyLimitReached() {
		when(userRepository.countCreatedToday()).thenReturn(100L);

		assertThrows(SignupLimitReachedException.class, () -> userService.create(request));
		verify(userRepository, never()).save(any());
	}

	@Test
	void create_savesWhenBelowDailyLimit() {
		when(userRepository.countCreatedToday()).thenReturn(99L);
		when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0, User.class));

		userService.create(request);

		verify(userRepository, times(1)).save(any());
	}
}
