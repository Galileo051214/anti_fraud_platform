package com.anti.service;

import com.anti.entity.AssociationRule;
import com.anti.entity.dto.CreateAssociationRuleRequest;
import com.anti.entity.vo.AssociationRuleVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface AssociationRuleService extends IService<AssociationRule> {

    /**
     * 创建关联规则
     *
     * @param request 创建请求
     * @return 规则VO
     */
    AssociationRuleVO createRule(CreateAssociationRuleRequest request);

    /**
     * 更新关联规则
     *
     * @param id      规则ID
     * @param request 更新请求
     * @return 规则VO
     */
    AssociationRuleVO updateRule(Long id, CreateAssociationRuleRequest request);

    /**
     * 根据触发标签获取规则
     *
     * @param triggerTag 触发标签
     * @return 规则列表
     */
    List<AssociationRuleVO> getRulesByTriggerTag(String triggerTag);

    /**
     * 获取所有规则(转换为Map)
     *
     * @return 触发标签到预测标签的映射
     */
    Map<String, List<String>> getAllRulesMap();

    /**
     * 增加规则使用次数
     *
     * @param ruleId 规则ID
     */
    void incrementRuleUsage(Long ruleId);

    /**
     * 根据触发标签预测相关标签
     *
     * @param triggerTag 触发标签
     * @return 预测标签列表
     */
    List<String> predictTags(String triggerTag);
}
