package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.QAConversation;
import com.anti.entity.vo.ChatVO;
import com.anti.entity.vo.SourceVO;
import com.anti.entity.vo.SessionVO;
import com.anti.entity.vo.TokenStatsVO;
import com.anti.mapper.QAConversationMapper;
import com.anti.service.AntiFraudAgentService;
import com.anti.service.QAConversationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 问答会话服务实现类
 */
@Slf4j
@Service
public class QAConversationServiceImpl extends ServiceImpl<QAConversationMapper, QAConversation> implements QAConversationService {

    private static final int MAX_QUESTION_LENGTH = 2000;
    private static final int MAX_SESSION_ID_LENGTH = 50;
    private static final int MAX_HISTORY_PAIRS = 5;
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^session_(\\d+)_(\\d+)$");

    private final QAConversationMapper qaConversationMapper;
    private final AntiFraudAgentService antiFraudAgentService;

    public QAConversationServiceImpl(QAConversationMapper qaConversationMapper, AntiFraudAgentService antiFraudAgentService) {
        this.qaConversationMapper = qaConversationMapper;
        this.antiFraudAgentService = antiFraudAgentService;
    }

    @Override
    @Transactional
    public ChatVO askQuestion(String question, String sessionId, Long userId) {
        return askQuestion(question, sessionId, userId, AntiFraudAgentService.ANSWER_TYPE_AUTO);
    }

    @Override
    @Transactional
    public ChatVO askQuestion(String question, String sessionId, Long userId, String answerType) {
        validateUserId(userId);
        String normalizedQuestion = normalizeQuestion(question);
        String normalizedSessionId = normalizeSessionId(sessionId, userId, true);

        // 如果没有会话ID，创建新会话
        if (normalizedSessionId == null) {
            normalizedSessionId = createSession(userId);
        }

        // 获取历史对话
        List<String[]> historyMessages = buildHistoryMessages(userId, normalizedSessionId);

        AntiFraudAgentService.AgentAnswer agentAnswer = antiFraudAgentService.answer(
                normalizedQuestion,
                historyMessages,
                normalizeAnswerType(answerType)
        );

        // 构建结果
        ChatVO chatVO = new ChatVO();
        chatVO.setSessionId(normalizedSessionId);
        chatVO.setQuestion(normalizedQuestion);
        chatVO.setAnswer(agentAnswer.getAnswer());
        chatVO.setReasoning(agentAnswer.getReasoning());
        chatVO.setTokensUsed(defaultInt(agentAnswer.getTokensUsed()));
        chatVO.setFallback(Boolean.TRUE.equals(agentAnswer.getFallback()));
        chatVO.setFallbackReason(agentAnswer.getFallbackReason());
        chatVO.setAnswerType(defaultText(agentAnswer.getAnswerType(), AntiFraudAgentService.ANSWER_TYPE_QA));
        chatVO.setSearchProvider(agentAnswer.getSearchProvider());
        chatVO.setRiskLevel(defaultText(agentAnswer.getRiskLevel(), "low"));
        chatVO.setSources(safeSources(agentAnswer.getSources()));
        chatVO.setRetrievedAt(agentAnswer.getRetrievedAt());
        LocalDateTime now = LocalDateTime.now();
        chatVO.setCreateTime(now);

        // 保存问答记录
        QAConversation conversation = new QAConversation();
        conversation.setUserId(userId);
        conversation.setSessionId(normalizedSessionId);
        conversation.setQuestion(normalizedQuestion);
        conversation.setAnswer(chatVO.getAnswer());
        conversation.setModel(antiFraudAgentService.getModelName());
        conversation.setTokensUsed(chatVO.getTokensUsed());
        conversation.setFeedback(null);
        conversation.setAnswerType(chatVO.getAnswerType());
        conversation.setRiskLevel(chatVO.getRiskLevel());
        conversation.setSourcesJson(toSourcesJson(chatVO.getSources()));
        conversation.setFallback(chatVO.getFallback());
        conversation.setRetrievedAt(chatVO.getRetrievedAt());
        conversation.setCreateTime(now);
        qaConversationMapper.insert(conversation);

        return chatVO;
    }

    @Override
    public void askQuestionStream(String question,
                                  String sessionId,
                                  Long userId,
                                  String answerType,
                                  ChatStreamHandler handler) {
        validateUserId(userId);
        String normalizedQuestion = normalizeQuestion(question);
        String normalizedSessionId = normalizeSessionId(sessionId, userId, true);

        if (normalizedSessionId == null) {
            normalizedSessionId = createSession(userId);
        }

        List<String[]> historyMessages = buildHistoryMessages(userId, normalizedSessionId);
        LocalDateTime now = LocalDateTime.now();
        String finalSessionId = normalizedSessionId;

        AntiFraudAgentService.AgentAnswer agentAnswer = antiFraudAgentService.answerStream(
                normalizedQuestion,
                historyMessages,
                normalizeAnswerType(answerType),
                new AntiFraudAgentService.AgentStreamHandler() {
                    @Override
                    public void onMetadata(AntiFraudAgentService.AgentAnswer metadata) {
                        if (handler != null) {
                            ChatVO meta = buildChatVO(normalizedQuestion, finalSessionId, metadata, now);
                            meta.setAnswer("");
                            handler.onMetadata(meta);
                        }
                    }

                    @Override
                    public void onReasoningDelta(String delta) {
                        if (handler != null) {
                            handler.onReasoningDelta(delta);
                        }
                    }

                    @Override
                    public void onContentDelta(String delta) {
                        if (handler != null) {
                            handler.onContentDelta(delta);
                        }
                    }
                }
        );

        ChatVO chatVO = buildChatVO(normalizedQuestion, normalizedSessionId, agentAnswer, now);
        saveConversation(userId, chatVO);
        if (handler != null) {
            handler.onComplete(chatVO);
        }
    }

    @Override
    public List<ChatVO> getConversationHistory(String sessionId, Long userId) {
        validateUserId(userId);
        String normalizedSessionId = normalizeSessionId(sessionId, userId, false);
        List<QAConversation> conversations = safeList(qaConversationMapper.findByUserIdAndSessionId(userId, normalizedSessionId));
        if (conversations.isEmpty()) {
            throw new BusinessException(404, "会话不存在");
        }

        return conversations.stream().map(c -> {
            ChatVO vo = new ChatVO();
            vo.setSessionId(c.getSessionId());
            vo.setQuestion(c.getQuestion());
            vo.setAnswer(c.getAnswer());
            vo.setTokensUsed(c.getTokensUsed());
            vo.setFallback(Boolean.TRUE.equals(c.getFallback()));
            vo.setAnswerType(defaultText(c.getAnswerType(), AntiFraudAgentService.ANSWER_TYPE_QA));
            vo.setRiskLevel(defaultText(c.getRiskLevel(), "low"));
            vo.setSources(fromSourcesJson(c.getSourcesJson()));
            vo.setRetrievedAt(c.getRetrievedAt());
            vo.setCreateTime(c.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<SessionVO> getSessionList(Long userId) {
        validateUserId(userId);
        List<String> sessionIds = qaConversationMapper.findSessionIdsByUserId(userId);
        List<SessionVO> sessions = new ArrayList<>();

        for (String sid : safeList(sessionIds)) {
            if (!isOwnedSessionId(sid, userId)) {
                continue;
            }
            List<QAConversation> conversations = qaConversationMapper.findByUserIdAndSessionId(userId, sid);

            if (conversations != null && !conversations.isEmpty()) {
                SessionVO sessionVO = new SessionVO();
                sessionVO.setSessionId(sid);

                // 第一条问题
                String firstQuestion = conversations.get(0).getQuestion();
                sessionVO.setFirstQuestion(abbreviate(firstQuestion, 50));

                // 最后一条回答
                QAConversation last = conversations.get(conversations.size() - 1);
                String lastAnswer = last.getAnswer();
                sessionVO.setLastAnswer(abbreviate(lastAnswer, 50));

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
        validateUserId(userId);
        String normalizedSessionId = normalizeSessionId(sessionId, userId, false);
        if (feedback == null || (feedback != 1 && feedback != -1)) {
            throw new BusinessException(400, "反馈值只能为-1或1");
        }

        // 更新该会话最后一条记录的反馈
        List<QAConversation> conversations = safeList(qaConversationMapper.selectList(
                new LambdaQueryWrapper<QAConversation>()
                        .eq(QAConversation::getUserId, userId)
                        .eq(QAConversation::getSessionId, normalizedSessionId)
                        .orderByDesc(QAConversation::getCreateTime)
                        .last("LIMIT 1")
        ));

        if (conversations.isEmpty()) {
            throw new BusinessException(404, "会话不存在");
        }
        QAConversation lastConversation = conversations.get(0);
        lastConversation.setFeedback(feedback);
        qaConversationMapper.updateById(lastConversation);
    }

    @Override
    public TokenStatsVO getTokenStats(Long userId) {
        validateUserId(userId);
        TokenStatsVO stats = new TokenStatsVO();
        stats.setTotalTokens(defaultInt(qaConversationMapper.sumTokensByUserId(userId)));
        stats.setTotalQuestions(defaultInt(qaConversationMapper.countByUserId(userId)));

        // 统计反馈
        List<QAConversation> allConversations = qaConversationMapper.selectList(
                new LambdaQueryWrapper<QAConversation>()
                        .eq(QAConversation::getUserId, userId)
                        .isNotNull(QAConversation::getFeedback)
        );

        stats.setSatisfiedCount((int) safeList(allConversations).stream()
                .filter(Objects::nonNull)
                .filter(c -> Integer.valueOf(1).equals(c.getFeedback()))
                .count());
        stats.setDissatisfiedCount((int) safeList(allConversations).stream()
                .filter(Objects::nonNull)
                .filter(c -> Integer.valueOf(-1).equals(c.getFeedback()))
                .count());

        return stats;
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId, Long userId) {
        validateUserId(userId);
        String normalizedSessionId = normalizeSessionId(sessionId, userId, false);
        int deleted = qaConversationMapper.delete(new LambdaQueryWrapper<QAConversation>()
                .eq(QAConversation::getUserId, userId)
                .eq(QAConversation::getSessionId, normalizedSessionId)
        );
        if (deleted == 0) {
            throw new BusinessException(404, "会话不存在");
        }
    }

    @Override
    public String createSession(Long userId) {
        validateUserId(userId);
        return "session_" + userId + "_" + System.currentTimeMillis();
    }

    private List<String[]> buildHistoryMessages(Long userId, String normalizedSessionId) {
        List<QAConversation> history = safeList(qaConversationMapper.findByUserIdAndSessionId(userId, normalizedSessionId));
        List<QAConversation> pairedHistory = history.stream()
                .filter(h -> hasText(h.getQuestion()) && hasText(h.getAnswer()))
                .collect(Collectors.toList());
        int fromIndex = Math.max(0, pairedHistory.size() - MAX_HISTORY_PAIRS);
        return pairedHistory.subList(fromIndex, pairedHistory.size()).stream()
                .flatMap(h -> {
                    List<String[]> messages = new ArrayList<>();
                    messages.add(new String[]{"user", h.getQuestion().trim()});
                    messages.add(new String[]{"assistant", h.getAnswer().trim()});
                    return messages.stream();
                })
                .collect(Collectors.toList());
    }

    private ChatVO buildChatVO(String question,
                               String normalizedSessionId,
                               AntiFraudAgentService.AgentAnswer agentAnswer,
                               LocalDateTime createTime) {
        ChatVO chatVO = new ChatVO();
        chatVO.setSessionId(normalizedSessionId);
        chatVO.setQuestion(question);
        chatVO.setAnswer(defaultText(agentAnswer.getAnswer(), ""));
        chatVO.setReasoning(agentAnswer.getReasoning());
        chatVO.setTokensUsed(defaultInt(agentAnswer.getTokensUsed()));
        chatVO.setFallback(Boolean.TRUE.equals(agentAnswer.getFallback()));
        chatVO.setAnswerType(defaultText(agentAnswer.getAnswerType(), AntiFraudAgentService.ANSWER_TYPE_QA));
        chatVO.setRiskLevel(defaultText(agentAnswer.getRiskLevel(), "low"));
        chatVO.setSources(safeSources(agentAnswer.getSources()));
        chatVO.setRetrievedAt(agentAnswer.getRetrievedAt());
        chatVO.setCreateTime(createTime);
        return chatVO;
    }

    private void saveConversation(Long userId, ChatVO chatVO) {
        QAConversation conversation = new QAConversation();
        conversation.setUserId(userId);
        conversation.setSessionId(chatVO.getSessionId());
        conversation.setQuestion(chatVO.getQuestion());
        conversation.setAnswer(chatVO.getAnswer());
        conversation.setModel(antiFraudAgentService.getModelName());
        conversation.setTokensUsed(chatVO.getTokensUsed());
        conversation.setFeedback(null);
        conversation.setAnswerType(chatVO.getAnswerType());
        conversation.setRiskLevel(chatVO.getRiskLevel());
        conversation.setSourcesJson(toSourcesJson(chatVO.getSources()));
        conversation.setFallback(chatVO.getFallback());
        conversation.setRetrievedAt(chatVO.getRetrievedAt());
        conversation.setCreateTime(chatVO.getCreateTime());
        qaConversationMapper.insert(conversation);
    }

    private String normalizeQuestion(String question) {
        String normalizedQuestion = question == null ? "" : question.trim();
        if (normalizedQuestion.isEmpty()) {
            throw new BusinessException(400, "问题内容不能为空");
        }
        if (normalizedQuestion.length() > MAX_QUESTION_LENGTH) {
            throw new BusinessException(400, "问题内容不能超过2000个字符");
        }
        return normalizedQuestion;
    }

    private String normalizeSessionId(String sessionId, Long userId, boolean allowBlank) {
        if (sessionId == null || sessionId.isBlank()) {
            if (allowBlank) {
                return null;
            }
            throw new BusinessException(400, "会话ID不能为空");
        }

        String normalizedSessionId = sessionId.trim();
        if (normalizedSessionId.length() > MAX_SESSION_ID_LENGTH) {
            throw new BusinessException(400, "会话ID不能超过50个字符");
        }

        Matcher matcher = SESSION_ID_PATTERN.matcher(normalizedSessionId);
        if (!matcher.matches()) {
            throw new BusinessException(400, "会话ID格式不正确");
        }

        Long ownerId;
        try {
            ownerId = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "会话ID格式不正确");
        }
        if (!ownerId.equals(userId)) {
            throw new BusinessException(403, "无权限访问该会话");
        }

        return normalizedSessionId;
    }

    private String normalizeAnswerType(String answerType) {
        if (!hasText(answerType)) {
            return AntiFraudAgentService.ANSWER_TYPE_AUTO;
        }
        String normalized = answerType.trim();
        if (AntiFraudAgentService.ANSWER_TYPE_AUTO.equals(normalized)
                || AntiFraudAgentService.ANSWER_TYPE_QA.equals(normalized)
                || AntiFraudAgentService.ANSWER_TYPE_LATEST_REPORT.equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(400, "回答类型只能是auto、qa或latest_report");
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(401, "请先登录");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private List<SourceVO> safeSources(List<SourceVO> sources) {
        return sources == null ? Collections.emptyList() : sources;
    }

    private String toSourcesJson(List<SourceVO> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        try {
            return JSONUtil.toJsonStr(sources);
        } catch (Exception e) {
            log.warn("检索来源序列化失败: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    private List<SourceVO> fromSourcesJson(String sourcesJson) {
        if (!hasText(sourcesJson)) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.toList(sourcesJson, SourceVO.class);
        } catch (Exception e) {
            log.warn("检索来源解析失败: {}", e.getClass().getSimpleName());
            return Collections.emptyList();
        }
    }

    private boolean isOwnedSessionId(String sessionId, Long userId) {
        if (!hasText(sessionId)) {
            return false;
        }
        Matcher matcher = SESSION_ID_PATTERN.matcher(sessionId.trim());
        if (!matcher.matches()) {
            return false;
        }
        try {
            return Long.parseLong(matcher.group(1)) == userId;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }

}
