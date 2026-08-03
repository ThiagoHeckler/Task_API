package com.thiago.taskapi.task_api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thiago.taskapi.task_api.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
	
	Optional<User> findByEmail(String email);
	
	boolean existsByEmail(String Email);
}
