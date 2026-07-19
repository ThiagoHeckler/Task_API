package com.thiago.taskapi.task_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thiago.taskapi.task_api.model.Tag;

public interface TagRepository extends JpaRepository<Tag, Long>{
	
	List<Tag> findByUserId(Long userId);
	
	Optional<Tag> findByIdAndUserId(Long id, Long userId);
	
	boolean existsByNameAndUserId(String name, Long userId);
	
}
// ver sobre spring.data.jpa.repositories.bootstrap-mode