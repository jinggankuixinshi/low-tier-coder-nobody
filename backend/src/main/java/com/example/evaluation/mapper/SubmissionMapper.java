package com.example.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evaluation.entity.Submission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {

    Submission selectByTaskIdAndStudentId(@Param("taskId") Long taskId, @Param("studentId") Long studentId);

    List<Submission> selectAllOrderByIdDesc();

    List<Submission> selectByTaskId(@Param("taskId") Long taskId);

    List<Submission> selectByStudentId(@Param("studentId") Long studentId);

    List<Submission> selectByTaskIdApproved(@Param("taskId") Long taskId);

    List<Submission> selectByTaskIdsApproved(@Param("taskIds") List<Long> taskIds);

    /** 查询学生的已审批提交 */
    List<Submission> selectByStudentIdApproved(@Param("studentId") Long studentId);

    List<Map<String, Object>> selectWithTaskSubquery(@Param("taskId") Long taskId);
}
