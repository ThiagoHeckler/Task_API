package com.thiago.taskapi.task_api.service;



import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.thiago.taskapi.task_api.dto.CreateUserRequest;
import com.thiago.taskapi.task_api.dto.UserResponse;
import com.thiago.taskapi.task_api.model.User;
import com.thiago.taskapi.task_api.repository.UserRepository;

@Service
public class UserService {
	
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	
	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}
	
	public UserResponse create(CreateUserRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new IllegalArgumentException("Email já cadastrado");
		}
		
		User user = new User();
		user.setName(request.name());
		user.setEmail(request.email());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		
		User savedUser = userRepository.save(user);
		
		return toResponse(savedUser);
	}
	
	private UserResponse toResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getCreatedAt()
				);
	}
	
	public List<UserResponse> findAll() {
		return userRepository.findAll()
				.stream()
				.map(this::toResponse)
				.toList();
	}
	
	public UserResponse findById(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
	return toResponse(user);
	}
}
