package com.example.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evaluation.entity.EvaluationIndicator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评价指标表 Mapper
 */
@Mapper
public interface EvaluationIndicatorMapper extends BaseMapper<EvaluationIndicator> {

    List<EvaluationIndicator> selectByTemplateId(@Param("templateId") Long templateId);

    List<Long> selectIdsByTemplateId(@Param("templateId") Long templateId);

    int deleteByTemplateId(@Param("templateId") Long templateId);
}
