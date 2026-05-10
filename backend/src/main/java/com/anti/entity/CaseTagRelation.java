package com.anti.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 案例-标签关联实体类
 */
@Data
@TableName("case_tag_relation")
public class CaseTagRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 案例ID
     */
    private Long caseId;

    /**
     * 标签ID
     */
    private Long tagId;
}
