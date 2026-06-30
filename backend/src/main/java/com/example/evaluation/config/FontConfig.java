package com.example.evaluation.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 统一字体来源：内置 Noto Sans CJK SC（OFL 协议）。
 * 同时注册给 AWT/Batik（SVG 图表中文渲染）并释放到磁盘供 Flying Saucer 嵌入 PDF。
 * 字体来源唯一，保证网页/PDF/SVG 三处文字渲染一致。
 */
@Slf4j
@Component
public class FontConfig {

    /** classpath 内置字体路径 */
    public static final String FONT_CLASSPATH = "fonts/NotoSansCJKsc-Regular.otf";
    /** 统一字体族名（与 OTF 内置名称、AWT 解析名称一致） */
    public static final String FONT_FAMILY = "Noto Sans CJK SC";

    private volatile String fontFilePath;
    private volatile boolean available = false;

    @PostConstruct
    public void init() {
        // Batik/AWT 光栅化在无头服务器（麒麟/LoongArch）上必须开启 headless
        System.setProperty("java.awt.headless", "true");

        try {
            ClassPathResource resource = new ClassPathResource(FONT_CLASSPATH);
            if (!resource.exists()) {
                log.warn("未找到内置字体 {}，PDF/SVG 中文可能显示异常", FONT_CLASSPATH);
                return;
            }

            // 1) 注册给 AWT/Batik（供 SVG 图表中文标签渲染）
            try (InputStream in = resource.getInputStream()) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, in);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                log.info("已向 AWT/Batik 注册内置字体: {}", font.getFamily());
            }

            // 2) 释放到临时文件，供 Flying Saucer(OpenPDF) 通过文件路径嵌入 PDF
            Path tmp = Files.createTempFile("noto-cjk-sc-", ".otf");
            tmp.toFile().deleteOnExit();
            try (InputStream in = resource.getInputStream();
                 OutputStream out = Files.newOutputStream(tmp)) {
                in.transferTo(out);
            }
            this.fontFilePath = tmp.toAbsolutePath().toString();
            this.available = true;
            log.info("内置字体就绪，Flying Saucer 字体文件: {}", this.fontFilePath);
        } catch (Exception e) {
            log.warn("内置字体初始化失败，将回退到系统字体探测: {}", e.getMessage());
        }
    }

    /** Flying Saucer 可用的字体文件磁盘路径，可能为 null */
    public String getFontFilePath() {
        return fontFilePath;
    }

    public String getFontFamily() {
        return FONT_FAMILY;
    }

    public boolean isAvailable() {
        return available;
    }
}
