package com.example.evaluation.service.impl;

import com.example.evaluation.common.BusinessException;
import com.example.evaluation.config.FileUploadConfig;
import com.example.evaluation.dto.SubmissionVO;
import com.example.evaluation.entity.*;
import com.example.evaluation.mapper.*;
import com.example.evaluation.security.SecurityUtil;
import com.example.evaluation.service.FileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    private final FileUploadConfig fileUploadConfig;
    private final SubmissionMapper submissionMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public FileServiceImpl(FileUploadConfig fileUploadConfig,
                           SubmissionMapper submissionMapper,
                           TaskMapper taskMapper,
                           UserMapper userMapper,
                           ObjectMapper objectMapper) {
        this.fileUploadConfig = fileUploadConfig;
        this.submissionMapper = submissionMapper;
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<SubmissionVO> uploadFiles(MultipartFile[] files, Long taskId) {
        if (files == null || files.length == 0) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (taskId == null) {
            throw new BusinessException("任务ID不能为空");
        }

        Long userId = SecurityUtil.getCurrentUserId();

        TrainingTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        if (task.getDeadline() != null && LocalDateTime.now().isAfter(task.getDeadline())) {
            throw new BusinessException("该任务已过截止时间，无法提交");
        }

        List<String> savedPaths = new ArrayList<>();
        List<String> savedNames = new ArrayList<>();
        List<Long> savedSizes = new ArrayList<>();
        String uploadDir = fileUploadConfig.getUploadPath() + "/" + taskId + "/" + userId + "/";

        for (MultipartFile file : files) {
            try {
                validateFile(file);
                String savedPath = saveFile(file, uploadDir);
                savedPaths.add(savedPath);
                savedNames.add(file.getOriginalFilename());
                savedSizes.add(file.getSize());
            } catch (Exception e) {
                log.error("文件保存失败: {}", file.getOriginalFilename(), e);
                throw new BusinessException("文件保存失败: " + file.getOriginalFilename() + " - " + e.getMessage());
            }
        }

        String pathsJson;
        String namesJson;
        String sizesJson;
        try {
            pathsJson = objectMapper.writeValueAsString(savedPaths);
            namesJson = objectMapper.writeValueAsString(savedNames);
            sizesJson = objectMapper.writeValueAsString(savedSizes);
        } catch (JsonProcessingException e) {
            throw new BusinessException("文件信息序列化失败");
        }

        Submission existingSubmission = submissionMapper.selectByTaskIdAndStudentId(taskId, userId);

        try {
            if (existingSubmission != null) {
                existingSubmission.setFilePaths(pathsJson);
                existingSubmission.setFileNames(namesJson);
                existingSubmission.setFileSizes(sizesJson);
                existingSubmission.setSubmitTime(LocalDateTime.now());
                existingSubmission.setUpdateTime(LocalDateTime.now());
                submissionMapper.updateById(existingSubmission);
            } else {
                existingSubmission = new Submission();
                existingSubmission.setTaskId(taskId);
                existingSubmission.setStudentId(userId);
                existingSubmission.setFilePaths(pathsJson);
                existingSubmission.setFileNames(namesJson);
                existingSubmission.setFileSizes(sizesJson);
                existingSubmission.setSubmitTime(LocalDateTime.now());
                submissionMapper.insert(existingSubmission);
            }
            return List.of(toVO(existingSubmission));
        } catch (Exception e) {
            throw new BusinessException("保存提交记录失败: " + e.getMessage());
        }
    }

    @Override
    public List<SubmissionVO> getAllSubmissions() {
        return toVOList(submissionMapper.selectAllOrderByIdDesc());
    }

    @Override
    public List<SubmissionVO> getPendingReviewSubmissions() {
        return toVOList(submissionMapper.selectAllOrderByIdDesc().stream()
                .filter(s -> s.getApprovalStatus() == null || s.getApprovalStatus() == 0)
                .collect(Collectors.toList()));
    }

    @Override
    public List<SubmissionVO> getSubmissionsByTask(Long taskId) {
        return toVOList(submissionMapper.selectByTaskId(taskId));
    }

    @Override
    public List<SubmissionVO> getSubmissionsByStudent(Long studentId) {
        return toVOList(submissionMapper.selectByStudentId(studentId));
    }

    @Override
    public SubmissionVO getSubmissionDetail(Long id) {
        Submission entity = submissionMapper.selectById(id);
        if (entity == null) throw new BusinessException(404, "提交记录不存在");
        return toVO(entity);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException("文件不能为空");
        String[] allowed = fileUploadConfig.getAllowedTypes();
        if (allowed == null || allowed.length == 0) return;
        String name = file.getOriginalFilename();
        if (name == null) throw new BusinessException("文件名不能为空");
        String ext = name.substring(name.lastIndexOf('.')).toLowerCase();
        boolean allowedExt = Arrays.asList(allowed).contains(ext);
        if (!allowedExt) throw new BusinessException("不支持的文件类型: " + ext);
    }

    private String saveFile(MultipartFile file, String dir) throws IOException {
        Path uploadPath = Paths.get(dir);
        Files.createDirectories(uploadPath);
        String originalName = file.getOriginalFilename();
        String safeName = (originalName != null ? originalName : "file");
        if (safeName.length() > 200) safeName = safeName.substring(0, 200);
        String storedName = System.currentTimeMillis() + "_" + safeName;
        Path filePath = uploadPath.resolve(storedName).normalize();
        if (!filePath.startsWith(uploadPath.normalize())) {
            throw new SecurityException("非法的文件路径: " + storedName);
        }
        file.transferTo(filePath.toFile());
        log.info("文件已保存: {}", filePath);
        return filePath.toString();
    }

    private SubmissionVO toVO(Submission entity) {
        SubmissionVO vo = new SubmissionVO();
        BeanUtils.copyProperties(entity, vo);
        try {
            TrainingTask task = taskMapper.selectById(entity.getTaskId());
            if (task != null) vo.setTaskName(task.getTitle());
        } catch (Exception e) {
            log.warn("获取任务名称失败: {}", e.getMessage());
        }
        if (entity.getStudentId() != null) {
            User user = userMapper.selectById(entity.getStudentId());
            if (user != null) {
                vo.setStudentName(user.getRealName());
                vo.setUsername(user.getUsername());
            }
        }
        return vo;
    }

    private List<SubmissionVO> toVOList(List<Submission> entities) {
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }
}
