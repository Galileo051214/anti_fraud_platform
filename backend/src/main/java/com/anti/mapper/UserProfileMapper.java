package com.anti.mapper;

import com.anti.entity.UserProfile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    UserProfile selectByUserId(@Param("userId") Long userId);

    void initProfile(@Param("userId") Long userId);
}
