package com.thiago.taskapi.task_api.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thiago.taskapi.task_api.dto.CategoryResponse;
import com.thiago.taskapi.task_api.dto.CreateTaskRequest;
import com.thiago.taskapi.task_api.dto.TagResponse;
import com.thiago.taskapi.task_api.dto.TaskResponse;
import com.thiago.taskapi.task_api.dto.UpdateTaskRequest;
import com.thiago.taskapi.task_api.exception.ResourceNotFoundException;
import com.thiago.taskapi.task_api.model.Category;
import com.thiago.taskapi.task_api.model.Tag;
import com.thiago.taskapi.task_api.model.Task;
import com.thiago.taskapi.task_api.model.User;
import com.thiago.taskapi.task_api.model.enums.TaskStatus;
import com.thiago.taskapi.task_api.repository.CategoryRepository;
import com.thiago.taskapi.task_api.repository.TagRepository;
import com.thiago.taskapi.task_api.repository.TaskRepository;
import com.thiago.taskapi.task_api.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class TaskService {
	
  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final CategoryRepository categoryRepository;
  private final TagRepository tagRepository;

  public TaskService(TaskRepository taskRepository, UserRepository userRepository, CategoryRepository categoryRepository, TagRepository tagRepository) {
    this.taskRepository = taskRepository;
    this.userRepository = userRepository;
    this.categoryRepository = categoryRepository;
    this.tagRepository = tagRepository;
  }

  @Transactional
  public TaskResponse create(Long userId, CreateTaskRequest request) {
	  User user = userRepository.findById(userId)
			  .orElseThrow(() -> new ResourceNotFoundException("Usuário não ecnontrado com id: " + userId));
	  
	  Task task = new Task();
	  task.setUser(user);
	  task.setTitle(request.title());
	  task.setDescription(request.description());
	  task.setDueDate(request.dueDate());
	  
	  if (request.priority() != null) {
		  task.setPriority(request.priority());
	  }
	  
	  if (request.categoryId() != null) {
		  Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
				  .orElseThrow(() -> new ResourceNotFoundException(
						  "Categoria não encontrada com id: " + request.categoryId()));
		  task.setCategory(category);
	  }
	  
	  if (request.parentTaskId() != null) {
		  Task parent = taskRepository.findByIdAndUserId(request.parentTaskId(), userId)
				  .orElseThrow(() -> new ResourceNotFoundException("Tarefa pai não encontrada com id: " + request.parentTaskId()));
		  task.setParentTask(parent);
	  }
	  
	  if (request.tagIds() != null && !request.tagIds().isEmpty()) {
		  Set<Tag> tags = new HashSet<>();
		  for (Long tagId : request.tagIds()) {
			  Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
					  .orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada com id: " + tagId));
			  tags.add(tag);
		  }
		  task.setTags(tags);
	  }
	  
	  return toResponse(taskRepository.save(task));
  }

  public List<TaskResponse> findAllByUser(Long userId) {
    return taskRepository.findByUserId(userId)
      .stream()
      .map(this::toResponse)
      .toList();
  }

  public TaskResponse findById(Long id, Long userId) {
    Task task = taskRepository.findByIdAndUserId(id, userId)
      .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com id: " + id));
    return toResponse(task);
  }

  public List<TaskResponse> findByStatus(Long userId, TaskStatus status) {
    return taskRepository.findByUserIdAndStatus(userId, status)
      .stream()
      .map(this::toResponse)
      .toList();
  }

  public List<TaskResponse> findRootTasks(Long userId) {
    return taskRepository.findByUserIdAndParentTaskIsNull(userId)
      .stream()
      .map(this::toResponse)
      .toList();
  }

  private TaskResponse toResponse(Task task) {
    CategoryResponse categoryResponse = null;
    if (task.getCategory() != null) {
    	categoryResponse = new CategoryResponse(
    			task.getCategory().getId(),
    			task.getCategory().getName(),
    			task.getCategory().getColor()
    	);
    }
    
    Set<TagResponse> tagResponse = task.getTags().stream()
    		.map(tag -> new TagResponse(tag.getId(), tag.getName()))
    		.collect(Collectors.toSet());
    
    Long parentId = (task.getParentTask() != null) ? task.getParentTask().getId() : null;
    
    return new TaskResponse(
    		task.getId(),
    		task.getTitle(),
    		task.getDescription(),
    		task.getStatus(),
    		task.getPriority(),
    		task.getDueDate(),
    		task.getCompletedAt(),
    		task.getCreatedAt(),
    		task.getUpdatedAt(),
    		categoryResponse,
    		tagResponse,
    		parentId);
  }
  
  @Transactional
  public TaskResponse update(Long id, Long userId, UpdateTaskRequest request) {
	  Task task = taskRepository.findByIdAndUserId(id, userId)
			  .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com id: " + id));
	  if (request.title() != null) {
		  task.setTitle(request.title());
	  }
	  if (request.description() != null) {
		  task.setDescription(request.description());
	  }
	  if (request.priority() != null) {
		  task.setPriority(request.priority());
	  }
	  if (request.dueDate() != null) {
		  task.setDueDate(request.dueDate());
	  }
	  
	  if (request.status() != null) {
		  task.setStatus(request.status());
		  if (request.status() == TaskStatus.COMPLETED && task.getCompletedAt() == null) {
			  task.setCompletedAt(LocalDateTime.now());
		  } else if (request.status() != TaskStatus.COMPLETED) {
			  task.setCompletedAt(null);
		  }
	  }
	  
	  if (request.categoryId() != null) {
		  Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
				  .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + request.categoryId()));
		  task.setCategory(category);
	  }
	  
	  if (request.tagIds() != null) {
		  Set<Tag> tags = new HashSet<>();
		  for (Long tagId : request.tagIds()) {
			  Tag tag = tagRepository.findByIdAndUserId(tagId, userId)
					  .orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada com id: " + tagId));
			  tags.add(tag);
		  }
		  task.setTags(tags);
	  }
	  
	  return toResponse(taskRepository.save(task));
  }
  
  @Transactional
  public void delete(Long id, Long userId) {
	  Task task = taskRepository.findByIdAndUserId(id, userId)
			  .orElseThrow(() -> new ResourceNotFoundException("Tarefa não encontrada com id: " + id));
	  taskRepository.delete(task);
  }
}
