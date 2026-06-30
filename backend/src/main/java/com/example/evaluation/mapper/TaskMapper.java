package com.example.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.evaluation.entity.TrainingTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 实训任务表 Mapper
 */
@Mapper
public interface TaskMapper extends BaseMapper<TrainingTask> {

    List<TrainingTask> selectPageWithOrder(Page<TrainingTask> page);

    List<TrainingTask> searchTasks(@Param("keyword") String keyword,
                                   @Param("status") Integer status,
                                   @Param("isStudent") boolean isStudent);

    List<TrainingTask> selectByFavoriteIds(@Param("ids") List<Long> ids);

    List<TrainingTask> selectByTeacherId(@Param("teacherId") Long teacherId);

    List<TrainingTask> selectByStatusAndDeadlineBefore(@Param("status") Integer status);
}
