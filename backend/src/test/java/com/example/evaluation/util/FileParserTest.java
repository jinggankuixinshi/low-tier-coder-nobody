package com.example.evaluation.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileParserTest {

    @Test
    @DisplayName("detectFileType returns TEXT for .txt")
    void testDetectFileType_Txt() {
        assertTrue(FileParser.isTextFile("document.txt"));
        assertFalse(FileParser.isImage("document.txt"));
        assertFalse(FileParser.isArchive("document.txt"));
    }

    @Test
    @DisplayName("detectFileType returns DOC for .docx")
    void testDetectFileType_Docx() {
        assertTrue(FileParser.isTextFile("report.docx"));
    }

    @Test
    @DisplayName("detectFileType returns CODE for .java")
    void testDetectFileType_Java() {
        assertTrue(FileParser.isTextFile("MyClass.java"));
    }

    @Test
    @DisplayName("detectFileType returns IMAGE for .png")
    void testDetectFileType_Image() {
        assertTrue(FileParser.isImage("photo.png"));
        assertFalse(FileParser.isTextFile("photo.png"));
        assertFalse(FileParser.isArchive("photo.png"));
    }

    @Test
    @DisplayName("detectFileType returns ARCHIVE for .zip")
    void testDetectFileType_Archive() {
        assertTrue(FileParser.isArchive("archive.zip"));
        assertFalse(FileParser.isImage("archive.zip"));
    }

    @Test
    @DisplayName("isTextFile returns true for .py, .js, .ts, .go, .cpp")
    void testIsTextFile_CodeExtensions() {
        assertTrue(FileParser.isTextFile("script.py"));
        assertTrue(FileParser.isTextFile("app.js"));
        assertTrue(FileParser.isTextFile("module.ts"));
        assertTrue(FileParser.isTextFile("main.go"));
        assertTrue(FileParser.isTextFile("program.cpp"));
    }

    @Test
    @DisplayName("isArchive returns true for .zip, .rar, .tar.gz, .7z")
    void testIsArchive_MultipleExtensions() {
        assertTrue(FileParser.isArchive("bundle.zip"));
        assertTrue(FileParser.isArchive("data.rar"));
        assertTrue(FileParser.isArchive("backup.tar.gz"));
        assertTrue(FileParser.isArchive("source.tar"));
        assertTrue(FileParser.isArchive("archive.7z"));
        assertTrue(FileParser.isArchive("file.tgz"));
    }

    @Test
    @DisplayName("isImage returns true for .png, .jpg, .gif, .jpeg, .bmp")
    void testIsImage_MultipleExtensions() {
        assertTrue(FileParser.isImage("photo.png"));
        assertTrue(FileParser.isImage("photo.jpg"));
        assertTrue(FileParser.isImage("photo.gif"));
        assertTrue(FileParser.isImage("photo.jpeg"));
        assertTrue(FileParser.isImage("photo.bmp"));
    }

    @Test
    @DisplayName("detectFileType handles unknown extension")
    void testDetectFileType_UnknownExtension() {
        assertFalse(FileParser.isTextFile("data.xyz"));
        assertFalse(FileParser.isImage("data.xyz"));
        assertFalse(FileParser.isArchive("data.xyz"));
        assertFalse(FileParser.isVideo("data.xyz"));
        assertFalse(FileParser.isParseable("data.xyz"));
    }

    @Test
    @DisplayName("isParseable returns true for known types, false for unknown")
    void testIsParseable() {
        assertTrue(FileParser.isParseable("doc.txt"));
        assertTrue(FileParser.isParseable("code.java"));
        assertTrue(FileParser.isParseable("img.png"));
        assertTrue(FileParser.isParseable("bundle.zip"));
        assertTrue(FileParser.isParseable("doc.pdf"));
        assertFalse(FileParser.isParseable("movie.mp4"));
        assertFalse(FileParser.isParseable("audio.mp3"));
    }

    @Test
    @DisplayName("isVideo returns true for video extensions")
    void testIsVideo() {
        assertTrue(FileParser.isVideo("movie.mp4"));
        assertTrue(FileParser.isVideo("clip.avi"));
        assertTrue(FileParser.isVideo("stream.mov"));
        assertTrue(FileParser.isVideo("record.wmv"));
        assertTrue(FileParser.isVideo("video.webm"));
    }
}
