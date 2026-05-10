package com.anti.mapper;

import com.anti.entity.NewsLike;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资讯点赞Mapper接口
 */
@Mapper
public interface NewsLikeMapper extends BaseMapper<NewsLike> {

    @Select("SELECT COUNT(*) FROM news_like WHERE news_id = #{newsId} AND user_id = #{userId}")
    int countByNewsIdAndUserId(@Param("newsId") Long newsId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM news_like WHERE news_id = #{newsId}")
    int countByNewsId(@Param("newsId") Long newsId);

    /**
     * 当前用户在给定资讯 ID 中已点赞的 ID 列表（用于列表页批量填充 isLiked）
     */
    @Select("<script>SELECT news_id FROM news_like WHERE user_id = #{userId} AND news_id IN "
            + "<foreach collection='newsIds' item='nid' open='(' separator=',' close=')'>#{nid}</foreach>"
            + "</script>")
    List<Long> selectLikedNewsIds(@Param("userId") Long userId, @Param("newsIds") List<Long> newsIds);
}
