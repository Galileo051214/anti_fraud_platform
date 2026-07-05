package com.anti.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 聊天请求DTO
 */
@Data
public class ChatRequest {

    /**
     * 问题内容
     */
    @NotBlank(message = "问题内容不能为空")
    @Size(max = 2000, message = "问题内容不能超过2000个字符")
    private String question;

    /**
     * 会话ID(可选，传入则继续该会话)
     */
    @Size(max = 50, message = "会话ID不能超过50个字符")
    @Pattern(regexp = "^$|^session_\\d+_\\d+$", message = "会话ID格式不正确")
    private String sessionId;

    /**
     * 回答类型：auto 自动判断，qa 普通问答，latest_report 最新诈骗汇报
     */
    @Pattern(regexp = "^$|^auto$|^qa$|^latest_report$", message = "回答类型只能是auto、qa或latest_report")
    private String answerType = "auto";

    /**
     * 前端模式：qa 普通问答，latest_report 最新诈骗汇报
     */
    @Pattern(regexp = "^$|^qa$|^latest_report$", message = "聊天模式只能是qa或latest_report")
    private String mode;

    /**
     * 是否显式使用检索能力
     */
    private Boolean useWebSearch;

    /**
     * 汇报关注的诈骗类型
     */
    @Size(max = 50, message = "诈骗类型不能超过50个字符")
    private String fraudType;

    /**
     * 汇报关注地区
     */
    @Size(max = 50, message = "地区不能超过50个字符")
    private String region;

    /**
     * 汇报时间范围
     */
    @Size(max = 20, message = "时间范围不能超过20个字符")
    private String timeRange;

    public String resolveAnswerType() {
        if (hasText(answerType) && !"auto".equals(answerType)) {
            return answerType;
        }
        if (hasText(mode)) {
            return mode;
        }
        if (Boolean.TRUE.equals(useWebSearch)) {
            return "latest_report";
        }
        return hasText(answerType) ? answerType : "auto";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
