package com.example.evaluation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {

    private String rootPath = System.getProperty("user.home") + "/evaluation-data";
    private String uploadDir = "uploads";
    private String extractDir = "extracted";
    private int maxFileSize = 500;
    private String[] allowedTypes = {
            ".doc", ".docx", ".pdf", ".ppt", ".pptx", ".xls", ".xlsx",
            ".zip", ".rar", ".7z", ".tar", ".gz", ".tgz",
            ".mp4", ".avi", ".mov", ".wmv", ".webm",
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".txt",
            ".java", ".py", ".js", ".ts", ".cpp", ".c", ".h",
            ".html", ".css", ".xml", ".json", ".md", ".yml", ".yaml",
            ".sql", ".sh", ".bat", ".vue", ".go", ".rs"
    };

    public String getUploadPath() { return rootPath + "/" + uploadDir; }
    public String getExtractPath() { return rootPath + "/" + extractDir; }
}
