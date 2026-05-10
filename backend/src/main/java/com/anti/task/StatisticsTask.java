package com.anti.task;

import com.anti.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据统计定时任务
 * 用于定期更新统计数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsTask {

    private final StatisticsService statisticsService;

    /**
     * 每天凌晨1点执行统计数据更新
     * 汇总前一天的各类数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void dailyStatisticsUpdate() {
        log.info("开始执行每日统计数据更新任务");
        try {
            statisticsService.triggerStatisticsUpdate();
            log.info("每日统计数据更新任务完成");
        } catch (Exception e) {
            log.error("每日统计数据更新任务执行失败", e);
        }
    }

    /**
     * 每小时执行一次实时数据更新
     * 更新当前小时的活跃数据
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void hourlyStatisticsUpdate() {
        log.info("开始执行小时级统计数据更新");
        try {
            statisticsService.triggerStatisticsUpdate();
            log.info("小时级统计数据更新完成");
        } catch (Exception e) {
            log.error("小时级统计数据更新失败", e);
        }
    }
}
