package com.anti.mapper;

import com.anti.entity.QAConversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 问答会话Mapper接口
 */
@Mapper
public interface QAConversationMapper extends BaseMapper<QAConversation> {

    /**
     * 根据用户ID和会话ID查询会话历史
     */
    @Select("SELECT * FROM qa_conversation WHERE user_id = #{userId} AND session_id = #{sessionId} ORDER BY create_time ASC")
    List<QAConversation> findByUserIdAndSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);

    /**
     * 根据用户ID查询所有会话ID
     */
    @Select("SELECT session_id FROM qa_conversation WHERE user_id = #{userId} GROUP BY session_id ORDER BY MAX(create_time) DESC")
    List<String> findSessionIdsByUserId(@Param("userId") Long userId);

    /**
     * 统计用户总消耗token
     */
    @Select("SELECT COALESCE(SUM(tokens_used), 0) FROM qa_conversation WHERE user_id = #{userId}")
    Integer sumTokensByUserId(@Param("userId") Long userId);

    /**
     * 统计用户总提问次数
     */
    @Select("SELECT COUNT(*) FROM qa_conversation WHERE user_id = #{userId}")
    Integer countByUserId(@Param("userId") Long userId);

    /**
     * 获取用户最近一次会话ID
     */
    @Select("SELECT session_id FROM qa_conversation WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT 1")
    String findLatestSessionId(@Param("userId") Long userId);
}
