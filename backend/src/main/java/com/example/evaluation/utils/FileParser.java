package com.example.evaluation.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 文件解析工具类
 * 支持 Word(.docx)、PDF、文本、Excel、PPTX 解析
 * 支持 ZIP / 7z / TAR / TAR.GZ / TAR.BZ2 / TAR.XZ / GZ 解压
 */
@Slf4j
public class FileParser {

    private static final long MAX_TEXT_FILE_BYTES = 50L * 1024 * 1024;
    private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
    private static final long MAX_EXTRACTED_FILE_BYTES = 100L * 1024 * 1024;
    private static final int MAX_ARCHIVE_DEPTH = 3;

    public static String extractText(File file) {
        String name = file.getName().toLowerCase();
        try {
            if (name.endsWith(".docx")) {
                return extractDocxText(file);
            } else if (name.endsWith(".doc")) {
                return "旧版 .doc 文件需转换为 .docx 后再解析";
            } else if (name.endsWith(".pdf")) {
                return extractPdfText(file);
            } else if (name.endsWith(".txt")) {
                return extractTxtText(file);
            } else if (name.endsWith(".xls") || name.endsWith(".xlsx")) {
                return extractExcelText(file);
            } else if (name.endsWith(".pptx")) {
                return extractPptxText(file);
            } else if (name.endsWith(".ppt")) {
                return "旧版 .ppt 文件需转换为 .pptx 后再解析";
            } else {
                return extractTxtText(file);
            }
        } catch (Exception e) {
            log.error("文件解析失败: {}", file.getName(), e);
            return "文件解析失败: " + e.getMessage();
        }
    }

    public static long getMaxTextFileBytes() { return MAX_TEXT_FILE_BYTES; }
    public static long getMaxImageBytes() { return MAX_IMAGE_BYTES; }

    private static String extractDocxText(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    sb.append(text).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private static String extractPdfText(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private static String extractTxtText(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    private static String extractExcelText(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             org.apache.poi.ss.usermodel.Workbook workbook =
                     org.apache.poi.ss.usermodel.WorkbookFactory.create(fis)) {
            StringBuilder sb = new StringBuilder();
            int sheetCount = workbook.getNumberOfSheets();
            for (int i = 0; i < sheetCount; i++) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(i);
                sb.append("【工作表: ").append(sheet.getSheetName()).append("】\n");
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        String value = getCellValue(cell);
                        if (value != null && !value.isEmpty()) {
                            sb.append(value).append("\t");
                        }
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    private static String getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private static String extractPptxText(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             org.apache.poi.xslf.usermodel.XMLSlideShow ppt = new org.apache.poi.xslf.usermodel.XMLSlideShow(fis)) {
            int i = 1;
            for (org.apache.poi.xslf.usermodel.XSLFSlide slide : ppt.getSlides()) {
                sb.append("【幻灯片 ").append(i++).append("】\n");
                for (org.apache.poi.xslf.usermodel.XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof org.apache.poi.xslf.usermodel.XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            sb.append(text).append("\n");
                        }
                    }
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public static boolean isImage(String fileName) {
        String name = fileName.toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg")
                || name.endsWith(".jpeg") || name.endsWith(".gif")
                || name.endsWith(".bmp");
    }

    public static boolean isTextFile(String fileName) {
        String name = fileName.toLowerCase();
        return name.endsWith(".txt") || name.endsWith(".docx")
                || name.endsWith(".doc") || name.endsWith(".pdf")
                || name.endsWith(".xls") || name.endsWith(".xlsx")
                || name.endsWith(".pptx") || name.endsWith(".ppt")
                || name.endsWith(".java") || name.endsWith(".py")
                || name.endsWith(".js") || name.endsWith(".ts")
                || name.endsWith(".cpp") || name.endsWith(".c")
                || name.endsWith(".h") || name.endsWith(".html")
                || name.endsWith(".css") || name.endsWith(".xml")
                || name.endsWith(".json") || name.endsWith(".md")
                || name.endsWith(".yml") || name.endsWith(".yaml")
                || name.endsWith(".sql") || name.endsWith(".sh")
                || name.endsWith(".bat") || name.endsWith(".vue")
                || name.endsWith(".go") || name.endsWith(".rs");
    }

    public static boolean isArchive(String fileName) {
        String name = fileName.toLowerCase();
        return name.endsWith(".zip") || name.endsWith(".jar")
                || name.endsWith(".war") || name.endsWith(".ear")
                || name.endsWith(".7z")
                || name.endsWith(".rar")
                || name.endsWith(".tar") || name.endsWith(".tar.gz")
                || name.endsWith(".tgz") || name.endsWith(".tar.bz2")
                || name.endsWith(".tbz2") || name.endsWith(".tar.xz")
                || name.endsWith(".txz")
                || (!name.contains(".tar") && name.endsWith(".gz"));
    }

    public static boolean isVideo(String fileName) {
        String name = fileName.toLowerCase();
        return name.endsWith(".mp4") || name.endsWith(".avi")
                || name.endsWith(".mov") || name.endsWith(".wmv")
                || name.endsWith(".webm");
    }

    public static boolean isParseable(String fileName) {
        return isTextFile(fileName) || isImage(fileName) || isArchive(fileName);
    }

    // ==================== 统一解压入口 ====================

    public static List<File> extractArchive(File archive, Path targetDir) throws IOException {
        return extractArchive(archive, targetDir, 0);
    }

    static List<File> extractArchive(File archive, Path targetDir, int depth) throws IOException {
        if (depth > MAX_ARCHIVE_DEPTH) return List.of();
        String name = archive.getName().toLowerCase();

        if (name.endsWith(".zip") || name.endsWith(".jar")
                || name.endsWith(".war") || name.endsWith(".ear")) {
            return extractZip(archive, targetDir, depth);
        }
        if (name.endsWith(".7z")) {
            return extract7z(archive, targetDir, depth);
        }
        if (name.endsWith(".tar")) {
            try (FileInputStream fis = new FileInputStream(archive)) {
                return extractTarStream(fis, targetDir, depth);
            }
        }
        if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            try (FileInputStream fis = new FileInputStream(archive);
                 GzipCompressorInputStream gz = new GzipCompressorInputStream(fis)) {
                return extractTarStream(gz, targetDir, depth);
            }
        }
        if (name.endsWith(".tar.bz2") || name.endsWith(".tbz2")) {
            try (FileInputStream fis = new FileInputStream(archive);
                 BZip2CompressorInputStream bz2 = new BZip2CompressorInputStream(fis)) {
                return extractTarStream(bz2, targetDir, depth);
            }
        }
        if (name.endsWith(".tar.xz") || name.endsWith(".txz")) {
            try (FileInputStream fis = new FileInputStream(archive);
                 XZCompressorInputStream xz = new XZCompressorInputStream(fis)) {
                return extractTarStream(xz, targetDir, depth);
            }
        }
        if (name.endsWith(".gz") && !name.contains(".tar")) {
            return extractSingleGzip(archive, targetDir, depth);
        }
        if (name.endsWith(".rar")) {
            log.warn("不支持 RAR 格式（RAR5 无法解析），请将压缩包转为 ZIP 或 7z 格式后重新上传: {}", archive.getName());
            return List.of();
        }

        log.warn("未能识别的压缩格式: {}", archive.getName());
        return List.of();
    }

    // ==================== ZIP 解压（含编码自动检测） ====================

    private static final Charset GBK = Charset.forName("GBK");

    private static List<File> extractZip(File archive, Path targetDir, int depth) throws IOException {
        Charset charset = detectZipCharset(archive);
        log.info("ZIP编码检测: {} → {}", archive.getName(), charset.displayName());
        return extractZipWithCharset(archive, targetDir, depth, charset);
    }

    private static Charset detectZipCharset(File archive) {
        try (ZipFile zf = ZipFile.builder().setFile(archive).setCharset(GBK).get()) {
            Enumeration<org.apache.commons.compress.archivers.zip.ZipArchiveEntry> entries = zf.getEntries();
            int checked = 0;
            while (entries.hasMoreElements() && checked < 20) {
                String name = entries.nextElement().getName();
                checked++;
                for (int i = 0; i < name.length(); i++) {
                    char c = name.charAt(i);
                    if ((c >= 0x80 && c <= 0xFF) || c == '\uFFFD') {
                        return StandardCharsets.UTF_8;
                    }
                }
            }
            return GBK;
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static List<File> extractZipWithCharset(File archive, Path targetDir, int depth, Charset charset) throws IOException {
        List<File> result = new ArrayList<>();
        Files.createDirectories(targetDir);

        try (ZipFile zf = ZipFile.builder().setFile(archive).setCharset(charset).get()) {
            Enumeration<org.apache.commons.compress.archivers.zip.ZipArchiveEntry> entries = zf.getEntries();
            while (entries.hasMoreElements()) {
                org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                if (entry.getSize() > MAX_EXTRACTED_FILE_BYTES) {
                    log.warn("ZIP内文件过大，跳过: {}", entry.getName());
                    continue;
                }

                String entryName = entry.getName().replace("\\", "/");
                String safeName = Path.of(entryName).getFileName().toString();
                if (safeName.isEmpty() || ".".equals(safeName) || "..".equals(safeName)) continue;

                Path entryPath = targetDir.resolve(safeName);
                if (!entryPath.normalize().startsWith(targetDir.normalize())) continue;

                File outFile = entryPath.toFile();
                copyZipEntryToFile(zf, entry, outFile);

                if (outFile.length() > 0 && !isExcludedExtension(safeName)) {
                    if (isArchive(safeName) && depth < MAX_ARCHIVE_DEPTH) {
                        Path subDir = targetDir.resolve(safeName + "_extracted");
                        result.addAll(extractArchive(outFile, subDir, depth + 1));
                    } else {
                        result.add(outFile);
                    }
                }
            }
        }
        return result;
    }

    private static void copyZipEntryToFile(ZipFile zf, org.apache.commons.compress.archivers.zip.ZipArchiveEntry entry, File outFile) throws IOException {
        try (InputStream is = zf.getInputStream(entry);
             FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int len;
            long written = 0;
            while ((len = is.read(buffer)) > 0) {
                if (written + len > MAX_EXTRACTED_FILE_BYTES) break;
                fos.write(buffer, 0, len);
                written += len;
            }
        }
    }

    // ==================== 7z 解压 ====================

    private static List<File> extract7z(File archive, Path targetDir, int depth) throws IOException {
        List<File> result = new ArrayList<>();
        Files.createDirectories(targetDir);

        try (SevenZFile szf = SevenZFile.builder().setFile(archive).get()) {
            org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry entry;
            while ((entry = szf.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                if (entry.getSize() > MAX_EXTRACTED_FILE_BYTES) {
                    log.warn("7z内文件过大，跳过: {}", entry.getName());
                    continue;
                }

                String entryName = entry.getName().replace("\\", "/");
                String safeName = Path.of(entryName).getFileName().toString();
                if (safeName.isEmpty()) continue;

                Path entryPath = targetDir.resolve(safeName);
                if (!entryPath.normalize().startsWith(targetDir.normalize())) continue;

                File outFile = entryPath.toFile();
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    long written = 0;
                    while ((len = szf.read(buffer)) > 0) {
                        if (written + len > MAX_EXTRACTED_FILE_BYTES) break;
                        fos.write(buffer, 0, len);
                        written += len;
                    }
                }

                if (outFile.length() > 0 && !isExcludedExtension(safeName)) {
                    if (isArchive(safeName) && depth < MAX_ARCHIVE_DEPTH) {
                        Path subDir = targetDir.resolve(safeName + "_extracted");
                        result.addAll(extractArchive(outFile, subDir, depth + 1));
                    } else {
                        result.add(outFile);
                    }
                }
            }
        }
        return result;
    }

    // ==================== TAR 系列解压（.tar / .tar.gz / .tar.bz2 / .tar.xz） ====================

    private static List<File> extractTarStream(InputStream is, Path targetDir, int depth) throws IOException {
        List<File> result = new ArrayList<>();
        Files.createDirectories(targetDir);

        try (TarArchiveInputStream tis = new TarArchiveInputStream(is)) {
            TarArchiveEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                if (entry.getSize() > MAX_EXTRACTED_FILE_BYTES) {
                    log.warn("TAR内文件过大，跳过: {}", entry.getName());
                    continue;
                }

                String entryName = entry.getName().replace("\\", "/");
                String safeName = Path.of(entryName).getFileName().toString();
                if (safeName.isEmpty()) continue;

                Path entryPath = targetDir.resolve(safeName);
                if (!entryPath.normalize().startsWith(targetDir.normalize())) continue;

                File outFile = entryPath.toFile();
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    long written = 0;
                    while ((len = tis.read(buffer)) > 0) {
                        if (written + len > MAX_EXTRACTED_FILE_BYTES) break;
                        fos.write(buffer, 0, len);
                        written += len;
                    }
                }

                if (outFile.length() > 0 && !isExcludedExtension(safeName)) {
                    if (isArchive(safeName) && depth < MAX_ARCHIVE_DEPTH) {
                        Path subDir = targetDir.resolve(safeName + "_extracted");
                        result.addAll(extractArchive(outFile, subDir, depth + 1));
                    } else {
                        result.add(outFile);
                    }
                }
            }
        }
        return result;
    }

    // ==================== 单文件 GZ 解压 ====================

    private static List<File> extractSingleGzip(File archive, Path targetDir, int depth) throws IOException {
        List<File> result = new ArrayList<>();
        Files.createDirectories(targetDir);

        String originalName = archive.getName();
        String outName = originalName.endsWith(".gz") ? originalName.substring(0, originalName.length() - 3) : originalName + ".out";
        File outFile = targetDir.resolve(outName).toFile();

        try (FileInputStream fis = new FileInputStream(archive);
             GzipCompressorInputStream gz = new GzipCompressorInputStream(fis);
             FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int len;
            long written = 0;
            while ((len = gz.read(buffer)) > 0) {
                if (written + len > MAX_EXTRACTED_FILE_BYTES) break;
                fos.write(buffer, 0, len);
                written += len;
            }
        }

        if (outFile.length() > 0 && !isExcludedExtension(outName)) {
            if (isArchive(outName) && depth < MAX_ARCHIVE_DEPTH) {
                Path subDir = targetDir.resolve(outName + "_extracted");
                result.addAll(extractArchive(outFile, subDir, depth + 1));
            } else {
                result.add(outFile);
            }
        }
        return result;
    }

    // ==================== 排除扩展名 ====================

    private static boolean isExcludedExtension(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".exe") || lower.endsWith(".dll")
                || lower.endsWith(".so") || lower.endsWith(".dylib")
                || lower.endsWith(".class") || lower.endsWith(".jar")
                || lower.endsWith(".war") || lower.endsWith(".ear");
    }
}
