package com.thiago.taskapi.task_api.service;


import java.util.List;

import org.springframework.stereotype.Service;

import com.thiago.taskapi.task_api.dto.CategoryResponse;
import com.thiago.taskapi.task_api.dto.CreateCategoryRequest;
import com.thiago.taskapi.task_api.exception.DuplicateResourceException;
import com.thiago.taskapi.task_api.exception.ResourceNotFoundException;
import com.thiago.taskapi.task_api.model.Category;
import com.thiago.taskapi.task_api.model.User;
import com.thiago.taskapi.task_api.repository.CategoryRepository;
import com.thiago.taskapi.task_api.repository.UserRepository;

@Service
public class CategoryService {
	
	private final CategoryRepository categoryRepository;
	private final UserRepository userRepository;
	
	public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
		this.categoryRepository = categoryRepository;
		this.userRepository = userRepository;
	}
	
	public CategoryResponse create(Long userId, CreateCategoryRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + userId));
		
		if (categoryRepository.existsByNameAndUserId(request.name(), userId)) {
			throw new DuplicateResourceException("Categoria já existe: " + request.name());
		}
		
		Category category = new Category();
		category.setName(request.name());
		category.setUser(user);
		if (request.color() != null) {
			category.setColor(request.color());
		}
		
		return toResponse(categoryRepository.save(category));
	}
	
	public List<CategoryResponse> findAllByUser(Long userId){
		return categoryRepository.findByUserId(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}
	
	public CategoryResponse findById(Long id, Long userId) {
		Category category = categoryRepository.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + id));
		return toResponse(category);
	}
	
	private CategoryResponse toResponse(Category category) {
		return new CategoryResponse(
				category.getId(),
				category.getName(),
				category.getColor()
		);
	}

}
