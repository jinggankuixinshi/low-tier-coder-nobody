package com.example.evaluation.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evaluation.common.Result;
import com.example.evaluation.entity.FavoriteTask;
import com.example.evaluation.entity.TrainingTask;
import com.example.evaluation.mapper.FavoriteTaskMapper;
import com.example.evaluation.mapper.SubmissionMapper;
import com.example.evaluation.mapper.TaskMapper;
import com.example.evaluation.security.SecurityUtil;
import com.example.evaluation.websocket.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskMapper taskMapper;
    private final FavoriteTaskMapper favoriteTaskMapper;
    private final SubmissionMapper submissionMapper;
    private final NotificationService notificationService;

    @Value("${file.upload.path:${user.home}/evaluation-uploads}")
    private String uploadPath;

    public TaskController(TaskMapper taskMapper, FavoriteTaskMapper favoriteTaskMapper,
                          SubmissionMapper submissionMapper,
                          NotificationService notificationService) {
        this.taskMapper = taskMapper;
        this.favoriteTaskMapper = favoriteTaskMapper;
        this.submissionMapper = submissionMapper;
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") Integer page,
                          @RequestParam(defaultValue = "10") Integer size) {
        Page<TrainingTask> taskPage = new Page<>(page, size);
        taskMapper.selectPageWithOrder(taskPage);
        return Result.success(taskPage);
    }

    @GetMapping("/search")
    public Result<?> search(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Integer status) {
        List<TrainingTask> list = taskMapper.searchTasks(keyword, status, SecurityUtil.isStudent());
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(taskMapper.selectById(id));
    }

    @GetMapping("/{taskId}/submissions")
    public Result<?> taskSubmissions(@PathVariable Long taskId) {
        return Result.success(submissionMapper.selectWithTaskSubquery(taskId));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> create(@RequestBody TrainingTask task) {
        task.setStatus(0);
        task.setTeacherId(SecurityUtil.getCurrentUserId());
        taskMapper.insert(task);
        notificationService.broadcastNewTask(SecurityUtil.getCurrentUserId(), task.getTitle());
        return Result.success("创建成功", task);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> update(@PathVariable Long id, @RequestBody TrainingTask task) {
        task.setId(id);
        taskMapper.updateById(task);
        return Result.success("更新成功", task);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> delete(@PathVariable Long id) {
        taskMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @PostMapping("/{id}/favorite")
    public Result<?> toggleFavorite(@PathVariable Long id) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            FavoriteTask existing = favoriteTaskMapper.selectByUserIdAndTaskId(userId, id);
            boolean favorited;
            if (existing != null) {
                favoriteTaskMapper.deleteById(existing.getId());
                favorited = false;
            } else {
                FavoriteTask ft = new FavoriteTask();
                ft.setUserId(userId);
                ft.setTaskId(id);
                favoriteTaskMapper.insert(ft);
                favorited = true;
            }
            Map<String, Object> resp = new HashMap<>();
            resp.put("favorited", favorited);
            resp.put("taskId", id);
            return Result.success(favorited ? "已添加到我的项目" : "已移除", resp);
        } catch (Exception e) {
            log.warn("添加到我的项目操作失败: {}", e.getMessage());
            return Result.error("添加操作暂不可用，请稍后重试");
        }
    }

    @GetMapping("/favorites")
    public Result<?> getFavoriteIds() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            List<FavoriteTask> list = favoriteTaskMapper.selectByUserId(userId);
            List<Long> ids = list.stream().map(FavoriteTask::getTaskId).collect(Collectors.toList());
            return Result.success(ids);
        } catch (Exception e) {
            log.warn("获取我的项目列表失败: {}", e.getMessage());
            return Result.success(List.of());
        }
    }

    @GetMapping("/favorite-list")
    public Result<?> getFavoriteTasks() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            List<FavoriteTask> favs = favoriteTaskMapper.selectByUserId(userId);
            if (favs.isEmpty()) return Result.success(List.of());
            List<Long> taskIds = favs.stream().map(FavoriteTask::getTaskId).collect(Collectors.toList());
            List<TrainingTask> tasks = taskMapper.selectByFavoriteIds(taskIds);
            return Result.success(tasks);
        } catch (Exception e) {
            log.warn("获取我的项目任务列表失败: {}", e.getMessage());
            return Result.success(List.of());
        }
    }

    @PostMapping("/upload-doc")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<?> uploadRequirementDoc(@RequestParam("file") MultipartFile file) {
        try {
            String dir = uploadPath + "/task-docs";
            Files.createDirectories(Paths.get(dir));
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(dir, filename);
            file.transferTo(filePath.toFile());
            Map<String, String> result = new HashMap<>();
            result.put("path", filePath.toString());
            result.put("name", file.getOriginalFilename());
            return Result.success("上传成功", result);
        } catch (Exception e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}
