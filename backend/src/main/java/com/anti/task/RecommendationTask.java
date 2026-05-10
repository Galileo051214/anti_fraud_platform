package com.anti.task;

import com.anti.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 推荐系统定时任务
 * 用于定期执行用户相似度计算等后台任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationTask {

    private final RecommendationService recommendationService;

    /**
     * 每天凌晨2点执行用户相似度计算
     * 使用皮尔逊相关系数计算用户间的相似度
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void calculateUserSimilarity() {
        log.info("开始执行用户相似度计算任务");
        try {
            recommendationService.batchCalculateUserSimilarities();
            log.info("用户相似度计算任务完成");
        } catch (Exception e) {
            log.error("用户相似度计算任务执行失败", e);
        }
    }

    /**
     * 每周日凌晨3点执行一次全量相似度计算
     * 作为每日计算的补充，确保数据准确性
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    public void weeklySimilarityCalculation() {
        log.info("开始执行周度用户相似度全量计算");
        try {
            recommendationService.batchCalculateUserSimilarities();
            log.info("周度用户相似度全量计算完成");
        } catch (Exception e) {
            log.error("周度用户相似度全量计算失败", e);
        }
    }
}
