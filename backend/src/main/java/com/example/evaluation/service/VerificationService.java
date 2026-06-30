package com.example.evaluation.service;

import com.example.evaluation.entity.VerificationResult;

import java.util.List;

/**
 * 智能核查服务接口
 */
public interface VerificationService {

    /**
     * 对提交成果进行智能核查
     * @param submissionId 提交ID
     * @return 核查结果列表
     */
    List<VerificationResult> verify(Long submissionId);

    /**
     * 获取核查结果
     * @param submissionId 提交ID
     * @return 核查结果列表
     */
    List<VerificationResult> getResults(Long submissionId);
}
