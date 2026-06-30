package com.example.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evaluation.entity.EvaluationTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评价指标模板表 Mapper
 */
@Mapper
public interface EvaluationTemplateMapper extends BaseMapper<EvaluationTemplate> {

    List<EvaluationTemplate> selectByTaskId(@Param("taskId") Long taskId);
}
