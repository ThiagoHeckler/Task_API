package com.taskapi.service;

import com.taskapi.dto.task.TaskFilterRequest;
import com.taskapi.dto.task.TaskRequest;
import com.taskapi.dto.task.TaskResponse;
import com.taskapi.entity.Task;
import com.taskapi.entity.TaskPriority;
import com.taskapi.entity.TaskStatus;
import com.taskapi.entity.User;
import com.taskapi.exception.ResourceNotFoundException;
import com.taskapi.exception.UnauthorizedAccessException;
import com.taskapi.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private User user;
    private Task task;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("user@example.com").build();
        task = Task.builder()
                .id(UUID.randomUUID())
                .title("Test Task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .user(user)
                .build();
    }

    @Test
    void findAll_noFilters_callsFindByUser() {
        TaskFilterRequest filter = new TaskFilterRequest(null, null);
        Page<Task> page = new PageImpl<>(List.of(task));
        when(taskRepository.findByUser(user, Pageable.unpaged())).thenReturn(page);

        Page<TaskResponse> result = taskService.findAll(user, filter, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).findByUser(user, Pageable.unpaged());
    }

    @Test
    void findAll_withStatusFilter_callsFindByUserAndStatus() {
        TaskFilterRequest filter = new TaskFilterRequest(TaskStatus.TODO, null);
        Page<Task> page = new PageImpl<>(List.of(task));
        when(taskRepository.findByUserAndStatus(user, TaskStatus.TODO, Pageable.unpaged())).thenReturn(page);

        taskService.findAll(user, filter, Pageable.unpaged());

        verify(taskRepository).findByUserAndStatus(user, TaskStatus.TODO, Pageable.unpaged());
    }

    @Test
    void findAll_withBothFilters_callsFindByUserAndStatusAndPriority() {
        TaskFilterRequest filter = new TaskFilterRequest(TaskStatus.TODO, TaskPriority.HIGH);
        Page<Task> page = new PageImpl<>(List.of(task));
        when(taskRepository.findByUserAndStatusAndPriority(user, TaskStatus.TODO, TaskPriority.HIGH, Pageable.unpaged()))
                .thenReturn(page);

        taskService.findAll(user, filter, Pageable.unpaged());

        verify(taskRepository).findByUserAndStatusAndPriority(user, TaskStatus.TODO, TaskPriority.HIGH, Pageable.unpaged());
    }

    @Test
    void create_success() {
        TaskRequest request = new TaskRequest("New Task", "desc", TaskStatus.TODO, TaskPriority.HIGH, null);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.create(user, request);

        assertThat(response.title()).isEqualTo("New Task");
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void findById_success() {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        TaskResponse response = taskService.findById(user, task.getId());

        assertThat(response.title()).isEqualTo("Test Task");
    }

    @Test
    void findById_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(user, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_notOwner_throwsException() {
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.findById(otherUser, task.getId()))
                .isInstanceOf(UnauthorizedAccessException.class);
    }

    @Test
    void update_success() {
        TaskRequest request = new TaskRequest("Updated", null, TaskStatus.DONE, null, null);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse response = taskService.update(user, task.getId(), request);

        assertThat(response.title()).isEqualTo("Updated");
        assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void delete_success() {
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        taskService.delete(user, task.getId());

        verify(taskRepository).delete(task);
    }

    @Test
    void delete_notOwner_throwsException() {
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.delete(otherUser, task.getId()))
                .isInstanceOf(UnauthorizedAccessException.class);

        verify(taskRepository, never()).delete(any());
    }
}
