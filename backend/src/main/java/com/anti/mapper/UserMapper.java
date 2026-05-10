package com.anti.mapper;

import com.anti.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    IPage<User> selectUserPage(Page<User> page, @Param("keyword") String keyword,
                               @Param("role") String role, @Param("status") Integer status);

    List<User> selectByIds(@Param("ids") List<Long> ids);

    Long countByRole(@Param("role") String role);

    Long countByStatus(@Param("status") Integer status);
}
