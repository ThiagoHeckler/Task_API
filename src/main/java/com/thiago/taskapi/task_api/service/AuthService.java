package com.thiago.taskapi.task_api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thiago.taskapi.task_api.dto.LoginRequest;
import com.thiago.taskapi.task_api.dto.LoginResponse;
import com.thiago.taskapi.task_api.exception.InvalidCredentialsException;
import com.thiago.taskapi.task_api.model.User;
import com.thiago.taskapi.task_api.repository.UserRepository;
import com.thiago.taskapi.task_api.security.JwtService;

@Service
public class AuthService {
	
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final String dummyHash;
	
	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.dummyHash = passwordEncoder.encode("dummy-password-para-equalizar-tempo");
	}
	
	// Email inexistente e senha errada dão a mesma resposta, para não revelar
	// quais emails estão cadastrados. Sem usuário, o BCrypt roda contra um hash
	// falso para que o tempo de resposta também seja igual nos dois casos.
	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email()).orElse(null);
		String hash = user != null ? user.getPasswordHash() : dummyHash;
		
		if (!passwordEncoder.matches(request.password(), hash) || user == null) {
			throw new InvalidCredentialsException("Credenciais inválidas");
		}
		
		String token = jwtService.generateToken(user.getId());
		
		return new LoginResponse(token, "Bearer", jwtService.getExpiration() / 1000);
	}
}
