package com.anti.mapper;

import com.anti.entity.UserScore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserScoreMapper extends BaseMapper<UserScore> {

    UserScore selectByUserId(@Param("userId") Long userId);

    void updateScore(@Param("userId") Long userId, @Param("addScore") Integer addScore);
}
