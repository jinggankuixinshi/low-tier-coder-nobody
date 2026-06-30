package com.example.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.evaluation.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    User selectByUsername(@Param("username") String username);

    long countByUsername(@Param("username") String username);

    List<User> selectByRole(@Param("role") Integer role);

    /** 搜索学生（用于添加团队成员时搜索） */
    List<User> searchStudents(@Param("keyword") String keyword,
                              @Param("excludeUserId") Long excludeUserId);
}
