package com.anti.entity.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 院系统计导出VO - 用于Excel导出
 */
@Data
@ContentRowHeight(25)
@HeadRowHeight(30)
public class DepartmentExportVO {

    @ExcelProperty("统计日期")
    private String statDate;

    @ExcelProperty("年级")
    private String grade;

    @ExcelProperty("专业")
    private String major;

    @ExcelProperty("用户数")
    private Integer userCount;

    @ExcelProperty("平均知识水平")
    private BigDecimal avgKnowledgeLevel;

    @ExcelProperty("平均测试得分")
    private BigDecimal avgTestScore;

    @ExcelProperty("学习完成率")
    private BigDecimal completionRate;
}
