package com.anti.mapper;

import com.anti.entity.Achievement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AchievementMapper extends BaseMapper<Achievement> {

    Achievement selectByCode(@Param("code") String code);

    List<Achievement> selectAllEnabled();
}
