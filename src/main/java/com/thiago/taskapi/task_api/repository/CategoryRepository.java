package com.thiago.taskapi.task_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thiago.taskapi.task_api.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long>{
	
	List<Category> findByUserId(Long userId);
	
	Optional<Category> findByIdAndUserId(Long id, Long userId);
	
	boolean existsByNameAndUserId(String name, Long userId);

}
