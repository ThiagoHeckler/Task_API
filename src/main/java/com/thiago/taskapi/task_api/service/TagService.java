package com.thiago.taskapi.task_api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.thiago.taskapi.task_api.dto.CreateTagRequest;
import com.thiago.taskapi.task_api.dto.TagResponse;
import com.thiago.taskapi.task_api.exception.DuplicateResourceException;
import com.thiago.taskapi.task_api.exception.ResourceNotFoundException;
import com.thiago.taskapi.task_api.model.Tag;
import com.thiago.taskapi.task_api.model.User;
import com.thiago.taskapi.task_api.repository.TagRepository;
import com.thiago.taskapi.task_api.repository.UserRepository;

@Service
public class TagService {
	
	private final TagRepository tagRepository;
	private final UserRepository userRepository;
	
	public TagService(TagRepository tagRepository, UserRepository userRepository) {
		this.tagRepository = tagRepository;
		this.userRepository = userRepository;
	}
	
	public TagResponse create(Long userId, CreateTagRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + userId));
		
		if (tagRepository.existsByNameAndUserId(request.name(), userId)) {
			throw new DuplicateResourceException("Tag já existe: " + request.name());
		}
	
		Tag tag = new Tag();
		tag.setName(request.name());
		tag.setUser(user);
		
		return toResponse(tagRepository.save(tag));
	}
	
	public List<TagResponse> findAllByUser(Long userId) {
		return tagRepository.findByUserId(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}
	
	public TagResponse findById(Long id, Long userId) {
		Tag tag = tagRepository.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada com id: " + id));
		return toResponse(tag);
	}
	
	private TagResponse toResponse(Tag tag) {
		return new TagResponse(
			tag.getId(),
			tag.getName()
		);
	}
}
