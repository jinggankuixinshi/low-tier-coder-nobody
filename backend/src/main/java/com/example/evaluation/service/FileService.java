package com.example.evaluation.service;

import com.example.evaluation.dto.SubmissionVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    List<SubmissionVO> uploadFiles(MultipartFile[] files, Long taskId);

    List<SubmissionVO> getAllSubmissions();

    List<SubmissionVO> getPendingReviewSubmissions();

    List<SubmissionVO> getSubmissionsByTask(Long taskId);

    List<SubmissionVO> getSubmissionsByStudent(Long studentId);

    SubmissionVO getSubmissionDetail(Long id);
}
