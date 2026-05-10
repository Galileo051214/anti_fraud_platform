package com.anti.service.impl;

import com.anti.entity.QAConversation;
import com.anti.entity.vo.ChatVO;
import com.anti.entity.vo.SessionVO;
import com.anti.entity.vo.TokenStatsVO;
import com.anti.mapper.QAConversationMapper;
import com.anti.service.QAConversationService;
import com.anti.util.AntiFraudPromptTemplate;
import com.anti.util.DeepSeekClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 问答会话服务实现类
 */
@Service
public class QAConversationServiceImpl extends ServiceImpl<QAConversationMapper, QAConversation> implements QAConversationService {

    private final QAConversationMapper qaConversationMapper;
    private final DeepSeekClient deepSeekClient;

    public QAConversationServiceImpl(QAConversationMapper qaConversationMapper, DeepSeekClient deepSeekClient) {
        this.qaConversationMapper = qaConversationMapper;
        this.deepSeekClient = deepSeekClient;
    }

    @Override
    @Transactional
    public ChatVO askQuestion(String question, String sessionId, Long userId) {
        // 如果没有会话ID，创建新会话
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = createSession(userId);
        }

        // 获取历史对话
        List<QAConversation> history = qaConversationMapper.findByUserIdAndSessionId(userId, sessionId);
        List<String[]> historyMessages = history.stream()
                .map(h -> new String[]{"assistant", h.getAnswer()})
                .collect(Collectors.toList());

        // 构建prompt
        String systemPrompt = AntiFraudPromptTemplate.getSystemPrompt();

        // 调用DeepSeek API
        DeepSeekClient.DeepSeekResponse response = deepSeekClient.chat(systemPrompt, question, historyMessages);

        // 构建结果
        ChatVO chatVO = new ChatVO();
        chatVO.setSessionId(sessionId);
        chatVO.setQuestion(question);

        if (response.isSuccess()) {
            chatVO.setAnswer(response.getContent());
            chatVO.setTokensUsed(response.getTotalTokens());
        } else {
            chatVO.setAnswer("抱歉，AI服务暂时不可用，请稍后再试。错误信息：" + response.getErrorMessage());
            chatVO.setTokensUsed(0);
        }
        chatVO.setCreateTime(LocalDateTime.now());

        // 保存问答记录
        QAConversation conversation = new QAConversation();
        conversation.setUserId(userId);
        conversation.setSessionId(sessionId);
        conversation.setQuestion(question);
        conversation.setAnswer(chatVO.getAnswer());
        conversation.setModel("deepseek-chat");
        conversation.setTokensUsed(chatVO.getTokensUsed());
        conversation.setFeedback(null);
        conversation.setCreateTime(LocalDateTime.now());
        save(conversation);

        return chatVO;
    }

    @Override
    public List<ChatVO> getConversationHistory(String sessionId, Long userId) {
        List<QAConversation> conversations = qaConversationMapper.findByUserIdAndSessionId(userId, sessionId);

        return conversations.stream().map(c -> {
            ChatVO vo = new ChatVO();
            vo.setSessionId(c.getSessionId());
            vo.setQuestion(c.getQuestion());
            vo.setAnswer(c.getAnswer());
            vo.setTokensUsed(c.getTokensUsed());
            vo.setCreateTime(c.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<SessionVO> getSessionList(Long userId) {
        List<String> sessionIds = qaConversationMapper.findSessionIdsByUserId(userId);
        List<SessionVO> sessions = new ArrayList<>();

        for (String sid : sessionIds) {
            List<QAConversation> conversations = qaConversationMapper.findByUserIdAndSessionId(userId, sid);

            if (!conversations.isEmpty()) {
                SessionVO sessionVO = new SessionVO();
                sessionVO.setSessionId(sid);

                // 第一条问题
                String firstQuestion = conversations.get(0).getQuestion();
                sessionVO.setFirstQuestion(firstQuestion.length() > 50 ? firstQuestion.substring(0, 50) + "..." : firstQuestion);

                // 最后一条回答
                QAConversation last = conversations.get(conversations.size() - 1);
                String lastAnswer = last.getAnswer();
                sessionVO.setLastAnswer(lastAnswer != null && lastAnswer.length() > 50 ?
                        lastAnswer.substring(0, 50) + "..." : lastAnswer);

                sessionVO.setMessageCount(conversations.size());
                sessionVO.setCreateTime(conversations.get(0).getCreateTime());
                sessionVO.setUpdateTime(last.getCreateTime());

                // 统计token
                int totalTokens = conversations.stream()
                        .filter(c -> c.getTokensUsed() != null)
                        .mapToInt(QAConversation::getTokensUsed)
                        .sum();
                sessionVO.setTotalTokens(totalTokens);

                sessions.add(sessionVO);
            }
        }

        return sessions;
    }

    @Override
    @Transactional
    public void submitFeedback(String sessionId, Integer feedback, Long userId) {
        // 更新该会话最后一条记录的反馈
        QAConversation lastConversation = getOne(
                new LambdaQueryWrapper<QAConversation>()
                        .eq(QAConversation::getUserId, userId)
                        .eq(QAConversation::getSessionId, sessionId)
                        .orderByDesc(QAConversation::getCreateTime)
                        .last("LIMIT 1")
        );

        if (lastConversation != null) {
            lastConversation.setFeedback(feedback);
            updateById(lastConversation);
        }
    }

    @Override
    public TokenStatsVO getTokenStats(Long userId) {
        TokenStatsVO stats = new TokenStatsVO();
        stats.setTotalTokens(qaConversationMapper.sumTokensByUserId(userId));
        stats.setTotalQuestions(qaConversationMapper.countByUserId(userId));

        // 统计反馈
        List<QAConversation> allConversations = list(
                new LambdaQueryWrapper<QAConversation>()
                        .eq(QAConversation::getUserId, userId)
                        .isNotNull(QAConversation::getFeedback)
        );

        stats.setSatisfiedCount((int) allConversations.stream().filter(c -> c.getFeedback() == 1).count());
        stats.setDissatisfiedCount((int) allConversations.stream().filter(c -> c.getFeedback() == -1).count());

        return stats;
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId, Long userId) {
        remove(new LambdaQueryWrapper<QAConversation>()
                .eq(QAConversation::getUserId, userId)
                .eq(QAConversation::getSessionId, sessionId)
        );
    }

    @Override
    public String createSession(Long userId) {
        return "session_" + userId + "_" + System.currentTimeMillis();
    }
}
