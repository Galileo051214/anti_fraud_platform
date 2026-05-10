package com.anti.entity.vo;

import lombok.Data;

/**
 * 闯关结果视图对象
 */
@Data
public class ChallengeResultVO {

    /**
     * 是否通关
     */
    private Boolean passed;

    /**
     * 得分
     */
    private Integer score;

    /**
     * 满分
     */
    private Integer maxScore;

    /**
     * 正确题数
     */
    private Integer correctCount;

    /**
     * 总题数
     */
    private Integer totalCount;

    /**
     * 获得积分
     */
    private Integer earnedScore;

    /**
     * 评价等级
     */
    private String rating;

    /**
     * 评价描述
     */
    private String ratingDesc;

    /**
     * 历史最高分
     */
    private Integer highestScore;

    /**
     * 是否打破记录
     */
    private Boolean newRecord;

    /**
     * 获取评价
     */
    public static String getRating(int score, int maxScore) {
        double rate = (double) score / maxScore;
        if (rate >= 0.9) {
            return "S";
        } else if (rate >= 0.8) {
            return "A";
        } else if (rate >= 0.7) {
            return "B";
        } else if (rate >= 0.6) {
            return "C";
        } else {
            return "D";
        }
    }

    /**
     * 获取评价描述
     */
    public static String getRatingDesc(String rating) {
        return switch (rating) {
            case "S" -> "太棒了！满分答人！";
            case "A" -> "优秀！继续保持！";
            case "B" -> "良好！还有进步空间";
            case "C" -> "及格啦，再接再厉";
            case "D" -> "不及格，要加油哦";
            default -> "";
        };
    }
}
