package com.example.evaluation.utils;

import com.example.evaluation.config.FileUploadConfig;
import com.example.evaluation.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Slf4j
@Component
public class FileContentExtractor {

    private static final Set<String> CODE_EXTENSIONS = Set.of(
            "java", "py", "js", "ts", "cpp", "c", "h", "html", "css", "xml",
            "json", "md", "yml", "yaml", "sql", "sh", "bat", "vue", "go", "rs"
    );

    private final AIService aiService;
    private final FileUploadConfig fileUploadConfig;

    public FileContentExtractor(AIService aiService, FileUploadConfig fileUploadConfig) {
        this.aiService = aiService;
        this.fileUploadConfig = fileUploadConfig;
    }

    /**
     * 从提交记录的文件路径中提取全部原文内容（仅当 AI 评价/核查时调用）
     */
    public String extractContent(String filePathsJson, Long taskId, Long submissionId) {
        List<String> filePaths = parseJsonArray(filePathsJson);
        if (filePaths.isEmpty()) return "";

        StringBuilder allContent = new StringBuilder();

        for (String filePath : filePaths) {
            File file = new File(filePath);
            String fileName = file.getName();
            try {
                if (!file.exists()) {
                    log.warn("文件不存在: {}", filePath);
                    continue;
                }
                processFile(file, fileName, taskId, submissionId, allContent, "");
            } catch (Exception e) {
                log.warn("文件处理失败: {} - {}", fileName, e.getMessage());
                allContent.append("[文件处理失败: ").append(fileName).append(" - ").append(e.getMessage()).append("]\n\n");
            }
        }

        return allContent.toString();
    }

    private void processFile(File file, String fileName, Long taskId, Long submissionId,
                              StringBuilder allContent, String indent) {
        try {
            if (FileParser.isImage(fileName)) {
                if (file.length() > FileParser.getMaxImageBytes()) {
                    allContent.append(indent).append("[图片文件过大: ").append(fileName).append("（跳过）]\n\n");
                    return;
                }
                String base64 = encodeFileToBase64(file);
                Map<String, Object> result = aiService.parseImage(base64);
                appendImageContent(allContent, fileName, indent, result);
            } else if (FileParser.isArchive(fileName)) {
                Path extractDir = Paths.get(fileUploadConfig.getExtractPath(),
                        String.valueOf(taskId),
                        String.valueOf(submissionId != null ? submissionId : "temp"));
                Files.createDirectories(extractDir);
                List<File> extractedFiles = FileParser.extractArchive(file, extractDir);
                allContent.append(indent).append("[压缩包: ").append(fileName)
                        .append("（已解压，" + extractedFiles.size() + "个文件）]\n");
                for (File extracted : extractedFiles) {
                    processFile(extracted, extracted.getName(), taskId, submissionId, allContent, indent + "  ");
                }
                deleteDirectory(extractDir);
            } else if (FileParser.isVideo(fileName)) {
                allContent.append(indent).append("[视频文件: ").append(fileName).append("（不参与内容分析）]\n\n");
            } else if (FileParser.isTextFile(fileName)) {
                if (file.length() > FileParser.getMaxTextFileBytes()) {
                    allContent.append(indent).append("[文本文件过大: ").append(fileName).append("（跳过）]\n\n");
                    return;
                }
                String text = FileParser.extractText(file);
                allContent.append(indent).append("【文件").append(fileName).append("】\n");
                allContent.append(indent).append(text).append("\n\n");
            } else {
                allContent.append(indent).append("[不支持的类型: ").append(fileName).append("（跳过）]\n\n");
            }
        } catch (Exception e) {
            log.warn("文件处理失败: {} - {}", fileName, e.getMessage());
            allContent.append(indent).append("[文件处理失败: ").append(fileName).append(" - ").append(e.getMessage()).append("]\n\n");
        }
    }

    private void appendImageContent(StringBuilder allContent, String fileName, String indent,
                                     Map<String, Object> result) {
        allContent.append(indent).append("【图片").append(fileName).append("】\n");
        appendField(allContent, "描述", result.get("description"), indent);
        appendField(allContent, "OCR文本", result.get("text"), indent);
        appendField(allContent, "代码内容", result.get("codeContent"), indent);
        appendField(allContent, "运行结果", result.get("outputContent"), indent);
        allContent.append("\n");
    }

    private void appendField(StringBuilder sb, String label, Object value, String indent) {
        if (value == null) return;
        String text = String.valueOf(value);
        if (text.isBlank() || "null".equals(text)) return;
        sb.append(indent).append(label).append("：").append(text).append("\n");
    }

    private String encodeFileToBase64(File file) {
        try {
            return Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            log.error("文件编码失败: {}", file.getName(), e);
            throw new RuntimeException("文件编码失败: " + file.getName());
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJsonArray(String json) {
        if (json == null) return List.of();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
        } catch (Exception e) {
            return List.of(json);
        }
    }

    private void deleteDirectory(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes a) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException {
                    Files.delete(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("删除临时目录失败: {}", dir);
        }
    }
}
