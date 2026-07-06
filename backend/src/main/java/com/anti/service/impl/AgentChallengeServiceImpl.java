package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.AgentChallengeDailyReward;
import com.anti.entity.AgentChallengeSession;
import com.anti.entity.Challenge;
import com.anti.entity.UserChallengeRecord;
import com.anti.entity.dto.AgentChallengeReplyRequest;
import com.anti.entity.vo.AgentChallengeSessionVO;
import com.anti.mapper.AgentChallengeDailyRewardMapper;
import com.anti.mapper.AgentChallengeSessionMapper;
import com.anti.mapper.ChallengeMapper;
import com.anti.mapper.UserChallengeRecordMapper;
import com.anti.service.AgentChallengeService;
import com.anti.service.LeaderboardService;
import com.anti.service.ScoreService;
import com.anti.util.DeepSeekClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Agent模拟挑战服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChallengeServiceImpl implements AgentChallengeService {

    private static final String TYPE_AGENT_SCENARIO = "agent_scenario";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_COMPLETED = "completed";
    private static final int MAX_ROUNDS = 5;
    private static final int PASSING_SCORE = 75;
    private static final int DAILY_REWARD = 100;
    private static final ZoneId REWARD_ZONE = ZoneId.of("Asia/Shanghai");

    private final AgentChallengeSessionMapper sessionMapper;
    private final AgentChallengeDailyRewardMapper rewardMapper;
    private final ChallengeMapper challengeMapper;
    private final UserChallengeRecordMapper recordMapper;
    private final ScoreService scoreService;
    private final LeaderboardService leaderboardService;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentChallengeSessionVO startChallenge(Long challengeId, Long userId) {
        requireUser(userId);
        Challenge challenge = requireAgentChallenge(challengeId);

        String firstMessage = generateFirstAgentMessage(challenge);
        LocalDateTime now = LocalDateTime.now();
        AgentChallengeSession session = new AgentChallengeSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setChallengeId(challengeId);
        session.setStatus(STATUS_IN_PROGRESS);
        session.setCurrentRound(1);
        session.setMessages(new ArrayList<>());
        session.getMessages().add(buildMessage("agent", 1, firstMessage));
        session.setPassed(0);
        session.setRewardGranted(0);
        session.setStartTime(now);
        session.setUpdateTime(now);
        sessionMapper.insert(session);

        return buildVO(session, challenge);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentChallengeSessionVO replyStream(AgentChallengeReplyRequest request,
                                               Long userId,
                                               AgentChallengeStreamHandler handler) {
        requireUser(userId);
        AgentChallengeSession session = sessionMapper.selectBySessionIdAndUserId(request.getSessionId(), userId);
        if (session == null) {
            throw new BusinessException("Agent模拟挑战会话不存在");
        }
        Challenge challenge = requireAgentChallenge(session.getChallengeId());
        if (!STATUS_IN_PROGRESS.equals(session.getStatus())) {
            AgentChallengeSessionVO vo = buildVO(session, challenge);
            if (handler != null) {
                handler.onMetadata(vo);
                handler.onComplete(vo);
            }
            return vo;
        }

        int currentRound = normalizeRound(session.getCurrentRound());
        List<AgentChallengeSession.AgentMessage> messages = resolveMessageHistory(session, request, currentRound);
        appendUserReplyIfMissing(messages, currentRound, request.getMessage().trim());

        if (currentRound >= MAX_ROUNDS) {
            AgentChallengeSession.ScoringReport report = scoreSession(challenge, messages);
            int finalScore = report.getTotalScore() != null ? report.getTotalScore() : 0;
            boolean passed = finalScore >= PASSING_SCORE;
            int earnedScore = passed ? grantDailyRewardIfEligible(userId, session, challenge, finalScore) : 0;

            session.setMessages(compactMessages(messages));
            session.setScoringReport(report);
            session.setSummary(report.getSummary());
            session.setFinalScore(finalScore);
            session.setPassed(passed ? 1 : 0);
            session.setRewardGranted(earnedScore > 0 ? 1 : 0);
            session.setRewardDate(earnedScore > 0 ? LocalDate.now(REWARD_ZONE) : null);
            session.setStatus(STATUS_COMPLETED);
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
            persistChallengeRecord(userId, challenge, session, finalScore, passed);

            AgentChallengeSessionVO result = buildVO(session, challenge);
            result.setEarnedScore(earnedScore);
            if (handler != null) {
                handler.onMetadata(result);
                handler.onComplete(result);
            }
            return result;
        }

        int nextRound = currentRound + 1;
        AgentChallengeSessionVO metadata = buildVO(session, challenge);
        metadata.setCurrentRound(nextRound);
        if (handler != null) {
            handler.onMetadata(metadata);
        }

        String nextAgentMessage = generateNextAgentMessage(challenge, messages, nextRound, handler);
        messages.add(buildMessage("agent", nextRound, nextAgentMessage));
        session.setCurrentRound(nextRound);
        session.setMessages(messages);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        AgentChallengeSessionVO result = buildVO(session, challenge);
        if (handler != null) {
            handler.onComplete(result);
        }
        return result;
    }

    @Override
    public AgentChallengeSessionVO getSession(String sessionId, Long userId) {
        requireUser(userId);
        AgentChallengeSession session = sessionMapper.selectBySessionIdAndUserId(sessionId, userId);
        if (session == null) {
            throw new BusinessException("Agent模拟挑战会话不存在");
        }
        Challenge challenge = requireAgentChallenge(session.getChallengeId());
        return buildVO(session, challenge);
    }

    private String generateFirstAgentMessage(Challenge challenge) {
        DeepSeekClient.DeepSeekResponse response = deepSeekClient.chat(
                buildAgentSystemPrompt(challenge),
                "请生成第1轮诈骗犯开场消息。要求：自然、短促、有诱导性，但所有链接、电话、账号都用脱敏占位符。",
                Collections.emptyList()
        );
        return requireAiContent(response, "AI服务不可用，暂时无法开始Agent模拟挑战");
    }

    private String generateNextAgentMessage(Challenge challenge,
                                            List<AgentChallengeSession.AgentMessage> messages,
                                            int nextRound,
                                            AgentChallengeStreamHandler handler) {
        StringBuilder content = new StringBuilder();
        DeepSeekClient.DeepSeekResponse response = deepSeekClient.chatStream(
                buildAgentSystemPrompt(challenge),
                "用户刚才已回复。请生成第" + nextRound + "轮诈骗犯消息，继续施压或换一种诱导方式，但不要暴露评分标准。",
                toDeepSeekHistory(messages),
                new DeepSeekClient.StreamHandler() {
                    @Override
                    public void onContentDelta(String delta) {
                        String sanitized = sanitizeAgentText(delta);
                        content.append(sanitized);
                        if (handler != null && !sanitized.isBlank()) {
                            handler.onAgentDelta(sanitized);
                        }
                    }
                }
        );
        String generated = content.length() > 0 ? content.toString() : response.getContent();
        return requireAiContent(response.isSuccess() ? generated : null, "AI服务不可用，暂时无法继续Agent模拟挑战");
    }

    private AgentChallengeSession.ScoringReport scoreSession(Challenge challenge,
                                                             List<AgentChallengeSession.AgentMessage> messages) {
        DeepSeekClient.DeepSeekResponse response = deepSeekClient.chat(
                buildScoringSystemPrompt(challenge),
                buildScoringUserPrompt(messages),
                Collections.emptyList()
        );
        if (response == null || !response.isSuccess() || response.getContent() == null || response.getContent().isBlank()) {
            throw new BusinessException("AI评分服务不可用，暂时无法完成Agent模拟挑战");
        }

        AgentChallengeSession.ScoringReport report = parseScoringReport(response.getContent());
        applyRuleCap(report, messages);
        normalizeReport(report);
        return report;
    }

    private int grantDailyRewardIfEligible(Long userId,
                                           AgentChallengeSession session,
                                           Challenge challenge,
                                           int finalScore) {
        LocalDate today = LocalDate.now(REWARD_ZONE);
        if (rewardMapper.countByUserAndDate(userId, today) > 0) {
            return 0;
        }

        AgentChallengeDailyReward reward = new AgentChallengeDailyReward();
        reward.setUserId(userId);
        reward.setRewardDate(today);
        reward.setSessionId(session.getSessionId());
        reward.setChallengeId(challenge.getId());
        reward.setScore(finalScore);
        reward.setRewardScore(DAILY_REWARD);
        reward.setCreateTime(LocalDateTime.now());
        try {
            rewardMapper.insert(reward);
        } catch (DuplicateKeyException e) {
            return 0;
        }

        scoreService.addScore(userId, DAILY_REWARD, "Agent模拟挑战每日奖励");
        leaderboardService.updateScore(userId, DAILY_REWARD, "daily");
        leaderboardService.updateScore(userId, DAILY_REWARD, "weekly");
        leaderboardService.updateScore(userId, DAILY_REWARD, "all");
        return DAILY_REWARD;
    }

    private void persistChallengeRecord(Long userId,
                                        Challenge challenge,
                                        AgentChallengeSession session,
                                        int finalScore,
                                        boolean passed) {
        UserChallengeRecord latest = recordMapper.selectLatestByUserAndChallenge(userId, challenge.getId());
        int attempts = latest != null && latest.getAttempts() != null ? latest.getAttempts() + 1 : 1;

        UserChallengeRecord record = new UserChallengeRecord();
        record.setUserId(userId);
        record.setChallengeId(challenge.getId());
        record.setAttempts(attempts);
        record.setScore(finalScore);
        record.setPassed(passed ? 1 : 0);
        record.setStartTime(session.getStartTime() != null ? session.getStartTime() : LocalDateTime.now());
        record.setEndTime(LocalDateTime.now());

        UserChallengeRecord.AnswerDetail detail = new UserChallengeRecord.AnswerDetail();
        detail.setAnswers(Collections.emptyList());
        detail.setTotalScore(finalScore);
        detail.setMaxScore(100);
        detail.setCorrectCount(countStrongDimensions(session.getScoringReport()));
        record.setAnswerDetail(detail);
        recordMapper.insert(record);
    }

    private int countStrongDimensions(AgentChallengeSession.ScoringReport report) {
        if (report == null) {
            return 0;
        }
        int count = 0;
        if (safeInt(report.getRiskIdentificationScore()) >= 24) count++;
        if (safeInt(report.getHighRiskRejectionScore()) >= 24) count++;
        if (safeInt(report.getOfficialVerificationScore()) >= 16) count++;
        if (safeInt(report.getEvidenceAndHelpScore()) >= 8) count++;
        if (safeInt(report.getCommunicationStabilityScore()) >= 8) count++;
        return count;
    }

    private AgentChallengeSession.ScoringReport parseScoringReport(String content) {
        try {
            String json = extractJson(content);
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            AgentChallengeSession.ScoringReport report = new AgentChallengeSession.ScoringReport();
            report.setRiskIdentificationScore(readInt(map, "riskIdentificationScore", 0));
            report.setHighRiskRejectionScore(readInt(map, "highRiskRejectionScore", 0));
            report.setOfficialVerificationScore(readInt(map, "officialVerificationScore", 0));
            report.setEvidenceAndHelpScore(readInt(map, "evidenceAndHelpScore", 0));
            report.setCommunicationStabilityScore(readInt(map, "communicationStabilityScore", 0));
            report.setTotalScore(readInt(map, "totalScore",
                    safeInt(report.getRiskIdentificationScore())
                            + safeInt(report.getHighRiskRejectionScore())
                            + safeInt(report.getOfficialVerificationScore())
                            + safeInt(report.getEvidenceAndHelpScore())
                            + safeInt(report.getCommunicationStabilityScore())));
            report.setRating(readString(map, "rating", ""));
            report.setSummary(readString(map, "summary", ""));
            report.setKeyMistakes(readStringList(map.get("keyMistakes")));
            report.setCorrectActions(readStringList(map.get("correctActions")));
            return report;
        } catch (Exception e) {
            log.warn("Agent挑战评分JSON解析失败，使用保守评分: {}", e.getClass().getSimpleName());
            AgentChallengeSession.ScoringReport report = new AgentChallengeSession.ScoringReport();
            report.setRiskIdentificationScore(10);
            report.setHighRiskRejectionScore(10);
            report.setOfficialVerificationScore(5);
            report.setEvidenceAndHelpScore(5);
            report.setCommunicationStabilityScore(5);
            report.setTotalScore(35);
            report.setRating("D");
            report.setSummary("评分结果解析失败，系统按保守规则判定本次挑战未通过。");
            report.setKeyMistakes(List.of("回答无法稳定映射到评分结构"));
            report.setCorrectActions(List.of("遇到转账、验证码、屏幕共享等要求时立即拒绝并通过官方渠道核验"));
            return report;
        }
    }

    private void applyRuleCap(AgentChallengeSession.ScoringReport report,
                              List<AgentChallengeSession.AgentMessage> messages) {
        if (!hasHighRiskUserIntent(messages)) {
            report.setHighRiskTriggered(false);
            report.setRuleCapApplied(false);
            return;
        }
        report.setHighRiskTriggered(true);
        report.setRuleCapApplied(true);
        report.setTotalScore(Math.min(safeInt(report.getTotalScore()), 59));
        report.setHighRiskRejectionScore(Math.min(safeInt(report.getHighRiskRejectionScore()), 5));
        List<String> mistakes = new ArrayList<>(report.getKeyMistakes() == null ? Collections.emptyList() : report.getKeyMistakes());
        if (mistakes.stream().noneMatch(v -> v.contains("高危"))) {
            mistakes.add("出现同意转账、泄露验证码、共享屏幕或提供敏感信息等高危倾向");
        }
        report.setKeyMistakes(mistakes);
    }

    private void normalizeReport(AgentChallengeSession.ScoringReport report) {
        report.setRiskIdentificationScore(clamp(report.getRiskIdentificationScore(), 0, 30));
        report.setHighRiskRejectionScore(clamp(report.getHighRiskRejectionScore(), 0, 30));
        report.setOfficialVerificationScore(clamp(report.getOfficialVerificationScore(), 0, 20));
        report.setEvidenceAndHelpScore(clamp(report.getEvidenceAndHelpScore(), 0, 10));
        report.setCommunicationStabilityScore(clamp(report.getCommunicationStabilityScore(), 0, 10));
        int sum = safeInt(report.getRiskIdentificationScore())
                + safeInt(report.getHighRiskRejectionScore())
                + safeInt(report.getOfficialVerificationScore())
                + safeInt(report.getEvidenceAndHelpScore())
                + safeInt(report.getCommunicationStabilityScore());
        int total = clamp(report.getTotalScore() != null ? report.getTotalScore() : sum, 0, 100);
        if (Boolean.TRUE.equals(report.getRuleCapApplied())) {
            total = Math.min(total, 59);
        }
        report.setTotalScore(total);
        if (report.getRating() == null || report.getRating().isBlank()) {
            report.setRating(total >= 90 ? "S" : total >= 80 ? "A" : total >= 70 ? "B" : total >= 60 ? "C" : "D");
        }
        if (report.getSummary() == null || report.getSummary().isBlank()) {
            report.setSummary(total >= 90 ? "能有效识别诈骗并采取安全处置。" : "仍需强化高危操作拒绝和官方核验意识。");
        }
        if (report.getKeyMistakes() == null) {
            report.setKeyMistakes(new ArrayList<>());
        }
        if (report.getCorrectActions() == null || report.getCorrectActions().isEmpty()) {
            report.setCorrectActions(List.of("拒绝转账、验证码、屏幕共享和下载未知App", "通过官方渠道二次核验", "保留证据并及时向110或学校保卫处求助"));
        }
    }

    private boolean hasHighRiskUserIntent(List<AgentChallengeSession.AgentMessage> messages) {
        for (AgentChallengeSession.AgentMessage message : messages) {
            if (!"user".equals(message.getRole()) || message.getContent() == null) {
                continue;
            }
            String text = message.getContent().toLowerCase(Locale.ROOT);
            if (containsSafeNegation(text)) {
                continue;
            }
            boolean hasRiskObject = containsAny(text, List.of(
                    "转账", "汇款", "打钱", "验证码", "共享屏幕", "屏幕共享", "远程控制",
                    "银行卡", "身份证", "密码", "保证金", "解冻金", "下载app", "下载"
            ));
            boolean hasAgreement = containsAny(text, List.of("可以", "好的", "马上", "发给你", "告诉你", "转给你", "我转", "我给", "已转", "发你"));
            if (hasRiskObject && hasAgreement) {
                return true;
            }
        }
        return false;
    }

    private boolean containsSafeNegation(String text) {
        return containsAny(text, List.of(
                "不转账", "不会转账", "不能转账", "不要转账",
                "不提供", "不会提供", "不发验证码", "不告诉验证码",
                "拒绝", "停止", "挂断", "报警", "核实", "官方"
        ));
    }

    private boolean containsAny(String text, List<String> terms) {
        for (String term : terms) {
            if (text.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String buildAgentSystemPrompt(Challenge challenge) {
        Challenge.AgentConfig config = challenge.getAgentConfig();
        return """
                你正在反诈学习平台中扮演一个“脱敏模拟诈骗犯Agent”，用于测试大学生反诈意识。
                你必须遵守：
                1. 只模拟诱导套路，禁止给出真实收款账号、真实链接、真实电话、真实违法教程。
                2. 所有链接写成[模拟链接]，账号写成[模拟账号]，电话号码写成[模拟电话]。
                3. 每次回复不超过80字，要像真实聊天施压，但不要暴露这是测试，不要解释评分标准。
                4. 用户识破后仍继续换方式施压，直到系统要求结束。

                诈骗类型：%s
                场景简介：%s
                角色身份：%s
                核心风险点：%s
                安全应对点：%s
                """.formatted(
                nullToEmpty(config.getFraudType()),
                nullToEmpty(config.getScenarioBrief()),
                nullToEmpty(config.getPersona()),
                String.join("、", safeStrings(config.getRiskPoints())),
                String.join("、", safeStrings(config.getSafeActions()))
        );
    }

    private String buildScoringSystemPrompt(Challenge challenge) {
        Challenge.AgentConfig config = challenge.getAgentConfig();
        return """
                你是反诈训练评分器。请只输出一个JSON对象，不要输出Markdown。
                根据用户在模拟诈骗对话中的表现评分，总分100，维度固定：
                riskIdentificationScore 0-30：是否识别诈骗和风险信号。
                highRiskRejectionScore 0-30：是否拒绝转账、验证码、屏幕共享、未知App、银行卡身份证等高危操作。
                officialVerificationScore 0-20：是否提出官方渠道核验。
                evidenceAndHelpScore 0-10：是否保留证据并寻求110、学校保卫处、官方客服等帮助。
                communicationStabilityScore 0-10：是否持续保持警惕，不被施压带偏。
                输出字段必须包含：totalScore,riskIdentificationScore,highRiskRejectionScore,officialVerificationScore,evidenceAndHelpScore,communicationStabilityScore,rating,summary,keyMistakes,correctActions。
                keyMistakes和correctActions必须是字符串数组。
                诈骗类型：%s；核心风险点：%s；安全应对点：%s。
                """.formatted(
                nullToEmpty(config.getFraudType()),
                String.join("、", safeStrings(config.getRiskPoints())),
                String.join("、", safeStrings(config.getSafeActions()))
        );
    }

    private String buildScoringUserPrompt(List<AgentChallengeSession.AgentMessage> messages) {
        StringBuilder builder = new StringBuilder("以下是完整模拟对话，请评分：\n");
        for (AgentChallengeSession.AgentMessage message : messages) {
            builder.append("第").append(message.getRound()).append("轮 ")
                    .append("agent".equals(message.getRole()) ? "诈骗犯" : "用户")
                    .append("：")
                    .append(message.getContent())
                    .append("\n");
        }
        return builder.toString();
    }

    private List<String[]> toDeepSeekHistory(List<AgentChallengeSession.AgentMessage> messages) {
        List<String[]> history = new ArrayList<>();
        for (AgentChallengeSession.AgentMessage message : messages) {
            if (message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            String role = "agent".equals(message.getRole()) ? "assistant" : "user";
            history.add(new String[]{role, message.getContent()});
        }
        return history;
    }

    private Challenge requireAgentChallenge(Long challengeId) {
        Challenge challenge = challengeMapper.selectById(challengeId);
        if (challenge == null || !Integer.valueOf(1).equals(challenge.getStatus())) {
            throw new BusinessException("关卡不存在或已禁用");
        }
        if (!TYPE_AGENT_SCENARIO.equals(challenge.getType())) {
            throw new BusinessException("该关卡不是Agent模拟挑战");
        }
        if (challenge.getAgentConfig() == null) {
            throw new BusinessException("Agent模拟挑战配置缺失");
        }
        return challenge;
    }

    private void requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
    }

    private AgentChallengeSession.AgentMessage buildMessage(String role, int round, String content) {
        AgentChallengeSession.AgentMessage message = new AgentChallengeSession.AgentMessage();
        message.setRole(role);
        message.setRound(round);
        message.setContent(sanitizeAgentText(content));
        message.setCreateTime(LocalDateTime.now().toString());
        return message;
    }

    private String sanitizeAgentText(String content) {
        if (content == null) {
            return "";
        }
        return content
                .replaceAll("https?://\\S+", "[模拟链接]")
                .replaceAll("(?i)www\\.\\S+", "[模拟链接]")
                .replaceAll("\\b\\d{6,}\\b", "[模拟号码]")
                .replaceAll("(?i)api[_-]?key\\s*[:=]\\s*\\S+", "[脱敏信息]");
    }

    private List<AgentChallengeSession.AgentMessage> safeMessages(List<AgentChallengeSession.AgentMessage> messages) {
        return messages == null ? new ArrayList<>() : new ArrayList<>(messages);
    }

    private List<AgentChallengeSession.AgentMessage> resolveMessageHistory(AgentChallengeSession session,
                                                                           AgentChallengeReplyRequest request,
                                                                           int currentRound) {
        List<AgentChallengeSession.AgentMessage> serverMessages = sanitizeMessageHistory(session.getMessages(), currentRound);
        List<AgentChallengeSession.AgentMessage> clientMessages = sanitizeMessageHistory(request.getClientMessages(), currentRound);
        if (clientMessages.size() > serverMessages.size()) {
            return clientMessages;
        }
        return serverMessages;
    }

    private List<AgentChallengeSession.AgentMessage> sanitizeMessageHistory(List<AgentChallengeSession.AgentMessage> source,
                                                                            int currentRound) {
        List<AgentChallengeSession.AgentMessage> cleaned = new ArrayList<>();
        if (source == null) {
            return cleaned;
        }
        for (AgentChallengeSession.AgentMessage message : source) {
            if (message == null || message.getRole() == null || message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }
            if (!"agent".equals(message.getRole()) && !"user".equals(message.getRole())) {
                continue;
            }
            int round = normalizeRound(message.getRound());
            if (round > currentRound) {
                continue;
            }
            AgentChallengeSession.AgentMessage copy = new AgentChallengeSession.AgentMessage();
            copy.setRole(message.getRole());
            copy.setRound(round);
            copy.setContent(sanitizeAgentText(message.getContent()));
            copy.setCreateTime(message.getCreateTime() != null ? message.getCreateTime() : LocalDateTime.now().toString());
            cleaned.add(copy);
        }
        return cleaned;
    }

    private void appendUserReplyIfMissing(List<AgentChallengeSession.AgentMessage> messages, int round, String content) {
        String normalized = sanitizeAgentText(content);
        if (!messages.isEmpty()) {
            AgentChallengeSession.AgentMessage last = messages.get(messages.size() - 1);
            if ("user".equals(last.getRole())
                    && Objects.equals(last.getRound(), round)
                    && Objects.equals(last.getContent(), normalized)) {
                return;
            }
        }
        messages.add(buildMessage("user", round, normalized));
    }

    private List<AgentChallengeSession.AgentMessage> compactMessages(List<AgentChallengeSession.AgentMessage> messages) {
        List<AgentChallengeSession.AgentMessage> compacted = new ArrayList<>();
        for (AgentChallengeSession.AgentMessage message : messages) {
            AgentChallengeSession.AgentMessage copy = new AgentChallengeSession.AgentMessage();
            copy.setRole(message.getRole());
            copy.setRound(message.getRound());
            copy.setCreateTime(message.getCreateTime());
            copy.setContent(truncate(message.getContent(), 120));
            compacted.add(copy);
        }
        return compacted;
    }

    private AgentChallengeSessionVO buildVO(AgentChallengeSession session, Challenge challenge) {
        AgentChallengeSessionVO vo = new AgentChallengeSessionVO();
        vo.setSessionId(session.getSessionId());
        vo.setChallengeId(session.getChallengeId());
        vo.setChallengeTitle(challenge.getTitle());
        vo.setAgentConfig(challenge.getAgentConfig());
        vo.setStatus(session.getStatus());
        vo.setCurrentRound(normalizeRound(session.getCurrentRound()));
        vo.setMaxRounds(MAX_ROUNDS);
        vo.setMessages(safeMessages(session.getMessages()));
        vo.setScoringReport(session.getScoringReport());
        vo.setSummary(session.getSummary());
        vo.setFinalScore(session.getFinalScore());
        vo.setPassed(Integer.valueOf(1).equals(session.getPassed()));
        vo.setRewardGranted(Integer.valueOf(1).equals(session.getRewardGranted()));
        vo.setEarnedScore(Integer.valueOf(1).equals(session.getRewardGranted()) ? DAILY_REWARD : 0);
        vo.setRewardDate(session.getRewardDate());
        vo.setStartTime(session.getStartTime());
        vo.setUpdateTime(session.getUpdateTime());
        return vo;
    }

    private int normalizeRound(Integer round) {
        if (round == null || round < 1) {
            return 1;
        }
        return Math.min(round, MAX_ROUNDS);
    }

    private String requireAiContent(DeepSeekClient.DeepSeekResponse response, String errorMessage) {
        if (response == null || !response.isSuccess()) {
            throw new BusinessException(errorMessage);
        }
        return requireAiContent(response.getContent(), errorMessage);
    }

    private String requireAiContent(String content, String errorMessage) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(errorMessage);
        }
        return sanitizeAgentText(content.trim());
    }

    private String extractJson(String content) {
        String text = content.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private int readInt(Map<String, Object> map, String key, int fallback) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String readString(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private List<String> readStringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private List<String> safeStrings(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return values.stream().filter(v -> v != null && !v.isBlank()).toList();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private int clamp(Integer value, int min, int max) {
        return Math.max(min, Math.min(max, value == null ? min : value));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
