package com.example.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evaluation.entity.VerificationResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 核查结果表 Mapper
 */
@Mapper
public interface VerificationResultMapper extends BaseMapper<VerificationResult> {

    int deleteBySubmissionId(@Param("submissionId") Long submissionId);

    List<VerificationResult> selectBySubmissionId(@Param("submissionId") Long submissionId);
}
