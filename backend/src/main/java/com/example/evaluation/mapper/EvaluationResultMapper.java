package com.example.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evaluation.entity.EvaluationResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评价结果表 Mapper
 */
@Mapper
public interface EvaluationResultMapper extends BaseMapper<EvaluationResult> {

    EvaluationResult selectByThreeKeys(@Param("submissionId") Long submissionId,
                                       @Param("templateId") Long templateId,
                                       @Param("indicatorId") Long indicatorId);

    long countBySubmissionId(@Param("submissionId") Long submissionId);

    List<EvaluationResult> selectBySubmissionId(@Param("submissionId") Long submissionId,
                                                @Param("templateId") Long templateId);

    Long selectTemplateIdBySubmissionId(@Param("submissionId") Long submissionId);

    int deleteByTemplateId(@Param("templateId") Long templateId);

    int deleteBySubmissionIdAndTemplateId(@Param("submissionId") Long submissionId,
                                          @Param("templateId") Long templateId);
}
