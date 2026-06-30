package com.example.evaluation.controller;

import com.example.evaluation.common.Result;
import com.example.evaluation.dto.SubmissionVO;
import com.example.evaluation.entity.Submission;
import com.example.evaluation.entity.User;
import com.example.evaluation.mapper.SubmissionMapper;
import com.example.evaluation.mapper.UserMapper;
import com.example.evaluation.security.SecurityUtil;
import com.example.evaluation.service.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final SubmissionMapper submissionMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public FileController(FileService fileService, SubmissionMapper submissionMapper,
                          UserMapper userMapper, ObjectMapper objectMapper) {
        this.fileService = fileService;
        this.submissionMapper = submissionMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<?> upload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("taskId") Long taskId) {
        log.info("文件上传请求: taskId={}, 文件数={}", taskId, files.length);
        return Result.success("上传成功",
                fileService.uploadFiles(files, taskId));
    }

    @GetMapping("/submissions")
    public Result<?> getSubmissions(@RequestParam("taskId") Long taskId) {
        return Result.success(fileService.getSubmissionsByTask(taskId));
    }

    @GetMapping("/all-submissions")
    public Result<?> getAllSubmissions(@RequestParam(required = false) Long taskId,
                                       @RequestParam(required = false) Long studentId,
                                       @RequestParam(required = false) Boolean pendingOnly,
                                       @RequestParam(required = false, defaultValue = "1") Integer page,
                                       @RequestParam(required = false, defaultValue = "10") Integer size,
                                       @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        Map<String, Object> result = new HashMap<>();
        List<SubmissionVO> list;

        if (taskId != null) {
            list = fileService.getSubmissionsByTask(taskId);
        } else if (studentId != null) {
            list = fileService.getSubmissionsByStudent(studentId);
        } else if (pendingOnly != null && pendingOnly) {
            list = fileService.getPendingReviewSubmissions();
        } else {
            if (SecurityUtil.isStudent()) {
                list = fileService.getSubmissionsByStudent(SecurityUtil.getCurrentUserId());
            } else {
                list = fileService.getAllSubmissions();
            }
        }

        if ("asc".equalsIgnoreCase(sortOrder)) {
            Collections.reverse(list);
        }

        int total = list.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        List<SubmissionVO> pageList = list.subList(from, to);

        result.put("records", pageList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success(result);
    }

    @GetMapping("/submission/{id}")
    public Result<?> getSubmissionDetail(@PathVariable Long id) {
        return Result.success(fileService.getSubmissionDetail(id));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/download/{submissionId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long submissionId) throws Exception {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null || submission.getFilePaths() == null) {
            return ResponseEntity.notFound().build();
        }
        List<String> paths = objectMapper.readValue(submission.getFilePaths(), List.class);
        if (paths.isEmpty()) return ResponseEntity.notFound().build();

        String firstPath = paths.get(0).toString();
        File file = new File(firstPath);
        if (!file.exists()) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(file);
        String encodedName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .body(resource);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/download-zip/{submissionId}")
    public ResponseEntity<Resource> downloadZip(@PathVariable Long submissionId) throws Exception {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null || submission.getFilePaths() == null) {
            return ResponseEntity.notFound().build();
        }
        List<String> paths = objectMapper.readValue(submission.getFilePaths(), List.class);
        if (paths.isEmpty()) return ResponseEntity.notFound().build();

        Path tempZip = Files.createTempFile("submission_" + submissionId + "_", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip.toFile()))) {
            for (String pathStr : paths) {
                File f = new File(pathStr);
                if (f.exists()) {
                    ZipEntry entry = new ZipEntry(f.getName());
                    zos.putNextEntry(entry);
                    Files.copy(f.toPath(), zos);
                    zos.closeEntry();
                }
            }
        }

        Resource resource = new FileSystemResource(tempZip);
        String studentName = "学生";
        if (submission.getStudentId() != null) {
            User user = userMapper.selectById(submission.getStudentId());
            if (user != null && user.getRealName() != null) studentName = user.getRealName();
        }
        String zipName = studentName + "_提交文件.zip";
        String encodedName = URLEncoder.encode(zipName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedName)
                .body(resource);
    }
}
