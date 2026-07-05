package com.anti.service;

import com.anti.entity.vo.ChatVO;
import com.anti.entity.vo.SessionVO;
import com.anti.entity.vo.TokenStatsVO;

import java.util.List;

/**
 * 问答服务接口
 */
public interface QAConversationService {

    /**
     * 发送问题并获取AI回答
     *
     * @param question 问题内容
     * @param sessionId 会话ID（可选，为空则创建新会话）
     * @param userId 用户ID
     * @return ChatVO
     */
    ChatVO askQuestion(String question, String sessionId, Long userId);

    /**
     * 发送问题并获取AI回答
     *
     * @param question 问题内容
     * @param sessionId 会话ID（可选，为空则创建新会话）
     * @param userId 用户ID
     * @param answerType 回答类型：auto / qa / latest_report
     * @return ChatVO
     */
    ChatVO askQuestion(String question, String sessionId, Long userId, String answerType);

    void askQuestionStream(String question, String sessionId, Long userId, String answerType, ChatStreamHandler handler);

    /**
     * 获取会话历史
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 消息列表
     */
    List<ChatVO> getConversationHistory(String sessionId, Long userId);

    /**
     * 获取用户所有会话列表
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<SessionVO> getSessionList(Long userId);

    /**
     * 提交用户反馈
     *
     * @param sessionId 会话ID
     * @param feedback 反馈（1满意/-1不满意）
     * @param userId 用户ID
     */
    void submitFeedback(String sessionId, Integer feedback, Long userId);

    /**
     * 获取用户Token消耗统计
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    TokenStatsVO getTokenStats(Long userId);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    void deleteSession(String sessionId, Long userId);

    /**
     * 创建新会话
     *
     * @param userId 用户ID
     * @return 新会话ID
     */
    String createSession(Long userId);

    interface ChatStreamHandler {
        default void onMetadata(ChatVO metadata) {
        }

        default void onReasoningDelta(String delta) {
        }

        default void onContentDelta(String delta) {
        }

        default void onComplete(ChatVO result) {
        }
    }
}
