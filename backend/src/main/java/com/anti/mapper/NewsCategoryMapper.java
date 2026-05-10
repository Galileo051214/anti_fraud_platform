package com.anti.mapper;

import com.anti.entity.NewsCategory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资讯分类Mapper接口
 */
@Mapper
public interface NewsCategoryMapper extends BaseMapper<NewsCategory> {
}
