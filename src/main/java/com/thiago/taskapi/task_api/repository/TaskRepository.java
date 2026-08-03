package com.thiago.taskapi.task_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thiago.taskapi.task_api.model.Task;
import com.thiago.taskapi.task_api.model.enums.TaskPriority;
import com.thiago.taskapi.task_api.model.enums.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, Long>{
	
	List<Task> findByUserId(Long userId);
	
	Optional<Task> findByIdAndUserId(Long id, Long userId);
	
	List<Task> findByUserIdAndStatus(Long userId, TaskStatus status);
	
	List<Task> findByUserIdAndPriority(Long userId, TaskPriority priority);
	
	List<Task> findByParentTaskId(Long parentTaskId);
	
	List<Task> findByUserIdAndParentTaskIsNull(Long userId);
}
