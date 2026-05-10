package com.anti.entity.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 统计数据导出VO - 用于Excel导出
 */
@Data
@ContentRowHeight(25)
@HeadRowHeight(30)
public class StatisticsExportVO {

    @ExcelProperty("统计日期")
    private LocalDate statDate;

    @ExcelProperty("日活用户数")
    private Integer dailyActiveUsers;

    @ExcelProperty("新增用户数")
    private Integer newUsers;

    @ExcelProperty("页面浏览量")
    private Integer totalPageViews;

    @ExcelProperty("闯关完成数")
    private Integer challengeCompletions;

    @ExcelProperty("平均测试得分")
    private BigDecimal avgTestScore;

    @ExcelProperty("新增帖子数")
    private Integer newPosts;

    @ExcelProperty("新增评论数")
    private Integer newComments;

    @ExcelProperty("创建时间")
    private String createTime;
}
