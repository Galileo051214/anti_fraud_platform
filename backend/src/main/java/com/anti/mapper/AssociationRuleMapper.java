package com.anti.mapper;

import com.anti.entity.AssociationRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 关联规则Mapper接口
 */
@Mapper
public interface AssociationRuleMapper extends BaseMapper<AssociationRule> {

    /**
     * 根据触发标签查询关联规则
     *
     * @param triggerTag 触发标签
     * @return 关联规则列表
     */
    @Select("SELECT * FROM association_rule WHERE trigger_tag = #{triggerTag} AND status = 1 ORDER BY confidence DESC")
    List<AssociationRule> findByTriggerTag(@Param("triggerTag") String triggerTag);

    /**
     * 查询所有启用的规则(按置信度排序)
     *
     * @return 关联规则列表
     */
    @Select("SELECT * FROM association_rule WHERE status = 1 ORDER BY confidence DESC")
    List<AssociationRule> findAllActiveRules();

    /**
     * 增加规则使用次数/热度
     *
     * @param id 规则ID
     */
    @Select("UPDATE association_rule SET confidence = LEAST(confidence + 0.01, 1.0) WHERE id = #{id}")
    void incrementUsageCount(@Param("id") Long id);
}
