package com.example.evaluation.controller;

import com.example.evaluation.common.GlobalExceptionHandler;
import com.example.evaluation.dto.SubmissionVO;
import com.example.evaluation.entity.Submission;
import com.example.evaluation.mapper.SubmissionMapper;
import com.example.evaluation.mapper.UserMapper;
import com.example.evaluation.security.SecurityUtil;
import com.example.evaluation.service.FileService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, com.example.evaluation.security.SecurityConfig.class})
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FileService fileService;

    @MockBean
    private SubmissionMapper submissionMapper;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private com.example.evaluation.security.JwtProperties jwtProperties;

    @MockBean
    private com.example.evaluation.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    private Submission mockSubmission;
    private SubmissionVO mockSubmissionVO;
    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        mockSubmission = new Submission();
        mockSubmission.setId(1L);
        mockSubmission.setTaskId(10L);
        mockSubmission.setStudentId(100L);
        mockSubmission.setFilePaths("[\"/path/to/file1.txt\"]");
        mockSubmission.setFileNames("[\"file1.txt\"]");
        mockSubmission.setFileSizes("[1024]");
        mockSubmission.setSubmitTime(LocalDateTime.now());

        mockSubmissionVO = new SubmissionVO();
        mockSubmissionVO.setId(1L);
        mockSubmissionVO.setTaskId(10L);
        mockSubmissionVO.setStudentId(100L);
        mockSubmissionVO.setStudentName("TestStudent");
        mockSubmissionVO.setFileNames("file1.txt");
        mockSubmissionVO.setSubmitTime(LocalDateTime.now());

        securityUtilMock = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/files/upload as STUDENT should return 200")
    @WithMockUser(roles = "STUDENT")
    void upload_asStudent_returnsOk() throws Exception {
        List<SubmissionVO> uploadResult = List.of(mockSubmissionVO);
        when(fileService.uploadFiles(any(), anyLong())).thenReturn(uploadResult);

        MockMultipartFile file = new MockMultipartFile(
                "files", "test.txt", "text/plain", "hello world".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .param("taskId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("上传成功"));
    }

    @Test
    @DisplayName("POST /api/files/upload as TEACHER should return 403")
    @WithMockUser(roles = "TEACHER")
    void upload_asTeacher_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files", "test.txt", "text/plain", "hello world".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .param("taskId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("GET /api/files/all-submissions should return 200")
    @WithMockUser
    void getAllSubmissions_returnsOk() throws Exception {
        securityUtilMock.when(SecurityUtil::isStudent).thenReturn(false);
        when(fileService.getAllSubmissions()).thenReturn(List.of(mockSubmissionVO));

        mockMvc.perform(get("/api/files/all-submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].studentName").value("TestStudent"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("GET /api/files/all-submissions with taskId filter should return filtered results")
    @WithMockUser
    void getAllSubmissions_byTaskId_returnsFiltered() throws Exception {
        when(fileService.getSubmissionsByTask(10L)).thenReturn(List.of(mockSubmissionVO));

        mockMvc.perform(get("/api/files/all-submissions").param("taskId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].taskId").value(10));
    }

    @Test
    @DisplayName("GET /api/files/submission/{id} should return 200")
    @WithMockUser
    void getSubmissionDetail_returnsOk() throws Exception {
        when(fileService.getSubmissionDetail(1L)).thenReturn(mockSubmissionVO);

        mockMvc.perform(get("/api/files/submission/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.studentName").value("TestStudent"));
    }

    @Test
    @DisplayName("GET /api/files/download/{submissionId} as authenticated user should return 200")
    @WithMockUser
    void downloadFile_authenticated_returnsOk() throws Exception {
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("test-download-", ".txt");
        tempFile.toFile().deleteOnExit();
        java.nio.file.Files.writeString(tempFile, "test content");

        Submission submissionWithRealFile = new Submission();
        submissionWithRealFile.setId(1L);
        submissionWithRealFile.setFilePaths("[\"" + tempFile.toString().replace("\\", "\\\\") + "\"]");
        when(submissionMapper.selectById(1L)).thenReturn(submissionWithRealFile);

        mockMvc.perform(get("/api/files/download/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/files/download/{submissionId} when submission not found should return 404")
    @WithMockUser
    void downloadFile_notFound_returns404() throws Exception {
        when(submissionMapper.selectById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/files/download/999"))
                .andExpect(status().isNotFound());
    }
}
