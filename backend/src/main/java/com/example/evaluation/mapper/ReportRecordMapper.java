package com.example.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evaluation.entity.ReportRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 报表记录表 Mapper
 */
@Mapper
public interface ReportRecordMapper extends BaseMapper<ReportRecord> {

    List<ReportRecord> selectByTaskId(@Param("taskId") Long taskId);

    ReportRecord selectBySubmissionId(@Param("submissionId") Long submissionId);

    ReportRecord selectByTaskIdAndType(@Param("taskId") Long taskId, @Param("reportType") Integer reportType);

    int deletePhysically(@Param("id") Long id);
}
