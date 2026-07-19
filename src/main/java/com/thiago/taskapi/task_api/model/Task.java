package com.thiago.taskapi.task_api.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.thiago.taskapi.task_api.model.enums.TaskPriority;
import com.thiago.taskapi.task_api.model.enums.TaskStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class Task {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, length = 255)
	private String title;
	
	@Column(columnDefinition = "TEXT")
	private String description;
	
	@Column(name = "due_date")
	private LocalDateTime dueDate;
	
	@Column(name = "completed_at")
	private LocalDateTime completedAt;
	
	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;
	
	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
	
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "status", nullable = false, columnDefinition = "task_status")
	private TaskStatus status;
	
	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.NAMED_ENUM)
	@Column(name = "priority", nullable = false, columnDefinition = "task_priority")
	private TaskPriority priority;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_task_id")
	private Task parentTask;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;
	
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "task_tags",
		joinColumns = @JoinColumn(name = "task_id"),
		inverseJoinColumns = @JoinColumn(name = "tag_id")
		)
	private Set<Tag> tags = new HashSet<>();
	
	
	protected Task() {
	}
	
	public Long getId() {
		return id;
	}


	public void setId(Long id) {
		this.id = id;
	}


	public String getTitle() {
		return title;
	}


	public void setTitle(String title) {
		this.title = title;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public LocalDateTime getDueDate() {
		return dueDate;
	}


	public void setDueDate(LocalDateTime dueDate) {
		this.dueDate = dueDate;
	}


	public LocalDateTime getCompletedAt() {
		return completedAt;
	}


	public void setCompletedAt(LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}


	public LocalDateTime getCreatedAt() {
		return createdAt;
	}


	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}


	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}


	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}


	public User getUser() {
		return user;
	}


	public void setUser(User user) {
		this.user = user;
	}


	public TaskStatus getStatus() {
		return status;
	}


	public void setStatus(TaskStatus status) {
		this.status = status;
	}


	public TaskPriority getPriority() {
		return priority;
	}


	public void setPriority(TaskPriority priority) {
		this.priority = priority;
	}


	public Task getParentTask() {
		return parentTask;
	}


	public void setParentTask(Task parentTask) {
		this.parentTask = parentTask;
	}


	public Category getCategory() {
		return category;
	}


	public void setCategory(Category category) {
		this.category = category;
	}


	public Set<Tag> getTags() {
		return tags;
	}


	public void setTags(Set<Tag> tags) {
		this.tags = tags;
	}

}
