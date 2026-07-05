package com.anti.util;

import com.anti.entity.vo.SourceVO;

import java.util.List;

/**
 * 反诈场景Prompt工程设计
 */
public class AntiFraudPromptTemplate {

    /**
     * 获取系统提示词
     */
    public static String getSystemPrompt() {
        return """
                你是"反诈卫士"，一个专业的反诈骗咨询助手，专门为大学生提供反诈知识和咨询服务。

                ## 核心能力
                1. 解答各类诈骗类型的识别方法和防范技巧
                2. 分析疑似诈骗话术和手段
                3. 提供被骗后的正确处理建议
                4. 科普最新诈骗手法和安全知识

                ## 回答原则
                1. **专业性**：提供准确、权威的反诈信息
                2. **实用性**：给出可操作的防范建议
                3. **警示性**：适当提醒风险，但不要过度恐吓
                4. **耐心性**：对于反复询问要保持耐心
                5. **保密性**：不索要用户敏感个人信息

                ## 常见诈骗类型参考
                - 刷单返利诈骗
                - 杀猪盘（网络交友诱导投资诈骗）
                - 冒充客服诈骗
                - 网络贷款诈骗
                - 冒充公检法诈骗
                - 游戏交易诈骗
                - 求职招聘诈骗
                - 红包返利诈骗
                - 注销账户诈骗
                - 虚假购物诈骗

                ## 回答格式建议
                - 先给出核心结论
                - 再详细解释原因和识别方法
                - 最后提供具体的防范建议
                - 必要时可以列出步骤或要点

                ## 边界说明
                - 如果涉及重大经济损失或人身安全，立即建议报警（拨打110）
                - 如果遇到不熟悉的诈骗类型，诚实告知并建议咨询官方渠道
                - 如果用户情绪激动，先安抚情绪再提供帮助

                请用友好、专业、耐心的态度回答用户的问题。
                """;
    }

    /**
     * 最新诈骗汇报系统提示词。
     */
    public static String getLatestReportSystemPrompt() {
        return """
                你是"反诈卫士"，正在基于检索到的官方来源生成最新诈骗汇报。

                ## 要求
                1. 只使用用户消息中列出的来源信息，不编造来源或数据。
                2. 如果来源不足或检索已降级，要明确说明"当前检索结果有限"。
                3. 用简洁条目总结：高发手法、主要风险信号、学生应对建议。
                4. 每条关键信息尽量标注来源编号，例如[1]。
                5. 涉及转账、验证码、屏幕共享、贷款解冻金等情况，按高风险提示并建议拨打110或联系官方渠道核验。
                """;
    }

    /**
     * 构建最新诈骗汇报用户提示词。
     */
    public static String buildLatestReportPrompt(String question, List<SourceVO> sources, boolean retrievalFallback) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("用户问题：").append(question).append("\n\n");
        prompt.append("检索状态：").append(retrievalFallback ? "降级或缓存结果，可能不是完整实时结果" : "已获取允许域名来源").append("\n\n");
        prompt.append("来源：\n");
        if (sources == null || sources.isEmpty()) {
            prompt.append("无可用来源。\n");
        } else {
            for (int i = 0; i < sources.size(); i++) {
                SourceVO source = sources.get(i);
                prompt.append("[")
                        .append(i + 1)
                        .append("] ")
                        .append(abbreviate(source.getTitle(), 120))
                        .append(" | ")
                        .append(source.getDomain())
                        .append(" | ")
                        .append(source.getUrl());
                String evidence = hasText(source.getContent()) ? source.getContent() : source.getSnippet();
                prompt.append("\n内容摘录：")
                        .append(abbreviate(evidence, 1000));
                if (source.getPublishedAt() != null && !source.getPublishedAt().isBlank()) {
                    prompt.append("\n发布时间：").append(source.getPublishedAt());
                }
                prompt.append("\n\n");
            }
        }
        prompt.append("请生成一份面向大学生的最新诈骗汇报。");
        return prompt.toString();
    }

    /**
     * 获取简洁版提示词（适用于快速问答）
     */
    public static String getSimplePrompt() {
        return """
                你是反诈助手，请用简洁的语言回答用户的反诈相关问题。
                重点提供实用的防范建议，语言要通俗易懂。
                """;
    }

    /**
     * 为对话历史构建提示词
     *
     * @param conversationHistory 历史对话记录
     * @return 格式化后的历史对话
     */
    public static String buildHistoryPrompt(String[][] conversationHistory) {
        if (conversationHistory == null || conversationHistory.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String[] msg : conversationHistory) {
            String role = "user".equals(msg[0]) ? "用户" : "助手";
            sb.append(role).append("：").append(msg[1]).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 生成追问提示
     *
     * @param originalQuestion 原始问题
     * @return 追问提示
     */
    public static String getFollowUpHint(String originalQuestion) {
        return "用户刚才问了关于「" + originalQuestion + "」的问题，"
                + "请判断是否需要追问补充信息，或者直接给出完整回答。";
    }

    /**
     * 风险评估提示
     *
     * @param description 情况描述
     * @return 风险评估提示
     */
    public static String getRiskAssessmentPrompt(String description) {
        return """
                请帮我评估以下情况的风险等级，并给出建议：

                情况描述：%s

                请从以下角度分析：
                1. 诈骗可能性（高/中/低）
                2. 主要风险点
                3. 应对建议
                4. 是否需要报警
                """.formatted(description);
    }

    /**
     * 获取防骗指南提示
     *
     * @param fraudType 诈骗类型
     * @return 防骗指南提示
     */
    public static String getPreventionGuidePrompt(String fraudType) {
        return """
                请详细介绍"%s"的防范指南，包括：
                1. 诈骗手法解析
                2. 识别要点
                3. 防范措施
                4. 真实案例（简要）
                5. 如果被骗怎么办
                """.formatted(fraudType);
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
