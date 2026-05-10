package com.anti.mapper;

import com.anti.entity.NewsBrowseLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NewsBrowseLogMapper extends BaseMapper<NewsBrowseLog> {

    IPage<NewsBrowseLog> selectUserBrowseHistory(Page<NewsBrowseLog> page, @Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT news_id) FROM news_browse_log WHERE user_id = #{userId}")
    int countDistinctNewsByUserId(@Param("userId") Long userId);
}
