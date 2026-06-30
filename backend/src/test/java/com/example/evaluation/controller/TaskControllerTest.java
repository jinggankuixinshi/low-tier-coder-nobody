package com.example.evaluation.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evaluation.common.GlobalExceptionHandler;
import com.example.evaluation.entity.FavoriteTask;
import com.example.evaluation.entity.TrainingTask;
import com.example.evaluation.mapper.FavoriteTaskMapper;
import com.example.evaluation.mapper.SubmissionMapper;
import com.example.evaluation.mapper.TaskMapper;
import com.example.evaluation.security.SecurityUtil;
import com.example.evaluation.websocket.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, com.example.evaluation.security.SecurityConfig.class})
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskMapper taskMapper;

    @MockBean
    private FavoriteTaskMapper favoriteTaskMapper;

    @MockBean
    private SubmissionMapper submissionMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private com.example.evaluation.security.JwtProperties jwtProperties;

    @MockBean
    private com.example.evaluation.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    private TrainingTask mockTask;
    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        mockTask = new TrainingTask();
        mockTask.setId(1L);
        mockTask.setTitle("Test Task");
        mockTask.setSubject("Java");
        mockTask.setDescription("A test task description");
        mockTask.setCourseName("Software Engineering");
        mockTask.setModuleName("Module 1");
        mockTask.setStatus(0);
        mockTask.setTeacherId(10L);
        mockTask.setCreateTime(LocalDateTime.now());
        mockTask.setUpdateTime(LocalDateTime.now());

        securityUtilMock = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/tasks?page=1&size=10 should return 200 for any user")
    @WithMockUser
    void list_returnsPagedTasks() throws Exception {
        Page<TrainingTask> taskPage = new Page<>(1, 10);
        taskPage.setRecords(List.of(mockTask));
        taskPage.setTotal(1);
        when(taskMapper.selectPageWithOrder(any(Page.class))).thenReturn(null);

        mockMvc.perform(get("/api/tasks").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/tasks/search?keyword=test&status=1 should return 200")
    @WithMockUser
    void search_returnsFilteredTasks() throws Exception {
        securityUtilMock.when(SecurityUtil::isStudent).thenReturn(false);
        when(taskMapper.searchTasks(eq("test"), eq(1), any(Boolean.class)))
                .thenReturn(List.of(mockTask));

        mockMvc.perform(get("/api/tasks/search")
                        .param("keyword", "test")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].title").value("Test Task"));
    }

    @Test
    @DisplayName("GET /api/tasks/{id} should return 200")
    @WithMockUser
    void detail_returnsTask() throws Exception {
        when(taskMapper.selectById(1L)).thenReturn(mockTask);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Test Task"));
    }

    @Test
    @DisplayName("POST /api/tasks as TEACHER should create task and return 200")
    @WithMockUser(roles = "TEACHER")
    void create_asTeacher_returnsOk() throws Exception {
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(10L);
        when(taskMapper.insert(any(TrainingTask.class))).thenReturn(1);

        TrainingTask newTask = new TrainingTask();
        newTask.setTitle("New Task");
        newTask.setSubject("Python");
        newTask.setDescription("A new task");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andExpect(jsonPath("$.data.title").value("New Task"))
                .andExpect(jsonPath("$.data.status").value(0));

        verify(taskMapper).insert(any(TrainingTask.class));
        verify(notificationService).broadcastNewTask(eq(10L), eq("New Task"));
    }

    @Test
    @DisplayName("POST /api/tasks as STUDENT should return 403")
    @WithMockUser(roles = "STUDENT")
    void create_asStudent_returns403() throws Exception {
        TrainingTask newTask = new TrainingTask();
        newTask.setTitle("New Task");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} as TEACHER should return 200")
    @WithMockUser(roles = "TEACHER")
    void update_asTeacher_returnsOk() throws Exception {
        when(taskMapper.updateById(any(TrainingTask.class))).thenReturn(1);

        TrainingTask updateTask = new TrainingTask();
        updateTask.setTitle("Updated Task");

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("更新成功"))
                .andExpect(jsonPath("$.data.title").value("Updated Task"));

        verify(taskMapper).updateById(any(TrainingTask.class));
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} as TEACHER should return 200")
    @WithMockUser(roles = "TEACHER")
    void delete_asTeacher_returnsOk() throws Exception {
        when(taskMapper.deleteById(1L)).thenReturn(1);

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("删除成功"));

        verify(taskMapper).deleteById(1L);
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/favorite should toggle and return 200")
    @WithMockUser
    void toggleFavorite_togglesSuccessfully() throws Exception {
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
        when(favoriteTaskMapper.selectByUserIdAndTaskId(100L, 1L)).thenReturn(null);
        when(favoriteTaskMapper.insert(any(FavoriteTask.class))).thenReturn(1);

        mockMvc.perform(post("/api/tasks/1/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已添加到我的项目"))
                .andExpect(jsonPath("$.data.favorited").value(true))
                .andExpect(jsonPath("$.data.taskId").value(1));
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/favorite with existing favorite should unfavorite")
    @WithMockUser
    void toggleFavorite_unfavoritesWhenExisting() throws Exception {
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);

        FavoriteTask existingFav = new FavoriteTask();
        existingFav.setId(100L);
        existingFav.setUserId(100L);
        existingFav.setTaskId(1L);

        when(favoriteTaskMapper.selectByUserIdAndTaskId(100L, 1L)).thenReturn(existingFav);
        when(favoriteTaskMapper.deleteById(100L)).thenReturn(1);

        mockMvc.perform(post("/api/tasks/1/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已移除"))
                .andExpect(jsonPath("$.data.favorited").value(false))
                .andExpect(jsonPath("$.data.taskId").value(1));
    }

    @Test
    @DisplayName("GET /api/tasks/favorites should return 200 with favorite IDs")
    @WithMockUser
    void getFavoriteIds_returnsIds() throws Exception {
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);

        FavoriteTask fav1 = new FavoriteTask();
        fav1.setTaskId(1L);
        FavoriteTask fav2 = new FavoriteTask();
        fav2.setTaskId(2L);
        when(favoriteTaskMapper.selectByUserId(100L)).thenReturn(List.of(fav1, fav2));

        mockMvc.perform(get("/api/tasks/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").value(1))
                .andExpect(jsonPath("$.data[1]").value(2));
    }
}
