package com.anti.mapper;

import com.anti.entity.News;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 资讯Mapper接口
 */
@Mapper
public interface NewsMapper extends BaseMapper<News> {

    @Select("SELECT n.*, nc.name as category_name, u.nickname as author_name, " +
           "(SELECT COUNT(*) FROM news_like WHERE news_id = n.id) as like_count " +
           "FROM news n " +
           "LEFT JOIN news_category nc ON n.category_id = nc.id " +
           "LEFT JOIN sys_user u ON n.author_id = u.id " +
           "WHERE n.id = #{id}")
    News selectNewsDetailById(@Param("id") Long id);

    @Update("UPDATE news SET view_count = view_count + 1 WHERE id = #{id}")
    void incrementViewCount(@Param("id") Long id);
}
