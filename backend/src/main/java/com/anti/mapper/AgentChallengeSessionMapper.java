package com.anti.mapper;

import com.anti.entity.AgentChallengeSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Agent模拟挑战会话Mapper
 */
@Mapper
public interface AgentChallengeSessionMapper extends BaseMapper<AgentChallengeSession> {

    @Select("SELECT * FROM agent_challenge_session WHERE session_id = #{sessionId} AND user_id = #{userId} LIMIT 1")
    AgentChallengeSession selectBySessionIdAndUserId(@Param("sessionId") String sessionId,
                                                     @Param("userId") Long userId);
}
