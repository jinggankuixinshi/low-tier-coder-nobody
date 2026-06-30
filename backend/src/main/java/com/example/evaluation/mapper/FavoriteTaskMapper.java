package com.example.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evaluation.entity.FavoriteTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteTaskMapper extends BaseMapper<FavoriteTask> {

    FavoriteTask selectByUserIdAndTaskId(@Param("userId") Long userId, @Param("taskId") Long taskId);

    List<FavoriteTask> selectByUserId(@Param("userId") Long userId);
}
