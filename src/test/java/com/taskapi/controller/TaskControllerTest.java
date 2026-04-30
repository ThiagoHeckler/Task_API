package com.taskapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapi.dto.task.TaskRequest;
import com.taskapi.dto.task.TaskResponse;
import com.taskapi.entity.TaskPriority;
import com.taskapi.entity.TaskStatus;
import com.taskapi.entity.User;
import com.taskapi.exception.ResourceNotFoundException;
import com.taskapi.exception.UnauthorizedAccessException;
import com.taskapi.config.SecurityConfig;
import com.taskapi.security.JwtAuthenticationFilter;
import com.taskapi.security.JwtService;
import com.taskapi.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    private User testUser;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .password("hashed")
                .build();

        taskResponse = new TaskResponse(
                UUID.randomUUID(),
                "Test Task",
                "description",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void listTasks_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listTasks_authenticated_returns200() throws Exception {
        when(taskService.findAll(any(User.class), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(taskResponse)));

        mockMvc.perform(get("/tasks").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Test Task"));
    }

    @Test
    void createTask_success_returns201() throws Exception {
        TaskRequest request = new TaskRequest("New Task", null, null, null, null);
        when(taskService.create(any(User.class), any(TaskRequest.class))).thenReturn(taskResponse);

        mockMvc.perform(post("/tasks")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Task"));
    }

    @Test
    void createTask_missingTitle_returns400() throws Exception {
        TaskRequest request = new TaskRequest("", null, null, null, null);

        mockMvc.perform(post("/tasks")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTask_success_returns200() throws Exception {
        UUID id = taskResponse.id();
        when(taskService.findById(any(User.class), eq(id))).thenReturn(taskResponse);

        mockMvc.perform(get("/tasks/{id}", id).with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getTask_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.findById(any(User.class), eq(id)))
                .thenThrow(new ResourceNotFoundException("Task not found: " + id));

        mockMvc.perform(get("/tasks/{id}", id).with(user(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTask_notOwner_returns403() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.findById(any(User.class), eq(id)))
                .thenThrow(new UnauthorizedAccessException("Access denied"));

        mockMvc.perform(get("/tasks/{id}", id).with(user(testUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTask_success_returns200() throws Exception {
        UUID id = taskResponse.id();
        TaskRequest request = new TaskRequest("Updated Task", null, TaskStatus.DONE, null, null);
        when(taskService.update(any(User.class), eq(id), any(TaskRequest.class))).thenReturn(taskResponse);

        mockMvc.perform(put("/tasks/{id}", id)
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTask_success_returns204() throws Exception {
        UUID id = taskResponse.id();

        mockMvc.perform(delete("/tasks/{id}", id).with(user(testUser)))
                .andExpect(status().isNoContent());
    }
}
