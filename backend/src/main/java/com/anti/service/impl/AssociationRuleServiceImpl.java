package com.anti.service.impl;

import com.anti.entity.AssociationRule;
import com.anti.entity.dto.CreateAssociationRuleRequest;
import com.anti.entity.vo.AssociationRuleVO;
import com.anti.mapper.AssociationRuleMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anti.service.AssociationRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 关联规则服务实现类
 */
@Slf4j
@Service
public class AssociationRuleServiceImpl extends ServiceImpl<AssociationRuleMapper, AssociationRule>
        implements AssociationRuleService {

    @Override
    public AssociationRuleVO createRule(CreateAssociationRuleRequest request) {
        AssociationRule rule = new AssociationRule();
        rule.setTriggerTag(request.getTriggerTag());
        rule.setPredictedTags(request.getPredictedTags());
        rule.setConfidence(request.getConfidence());
        rule.setDescription(request.getDescription());
        rule.setStatus(1);
        this.save(rule);
        return convertToVO(rule);
    }

    @Override
    public AssociationRuleVO updateRule(Long id, CreateAssociationRuleRequest request) {
        AssociationRule rule = this.getById(id);
        if (rule == null) {
            throw new RuntimeException("规则不存在");
        }
        rule.setTriggerTag(request.getTriggerTag());
        rule.setPredictedTags(request.getPredictedTags());
        rule.setConfidence(request.getConfidence());
        rule.setDescription(request.getDescription());
        this.updateById(rule);
        return convertToVO(rule);
    }

    @Override
    public List<AssociationRuleVO> getRulesByTriggerTag(String triggerTag) {
        List<AssociationRule> rules = baseMapper.findByTriggerTag(triggerTag);
        return rules.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public Map<String, List<String>> getAllRulesMap() {
        List<AssociationRule> rules = baseMapper.findAllActiveRules();
        Map<String, List<String>> ruleMap = new HashMap<>();
        for (AssociationRule rule : rules) {
            List<String> predictedTags = parseTags(rule.getPredictedTags());
            ruleMap.put(rule.getTriggerTag(), predictedTags);
        }
        return ruleMap;
    }

    @Override
    public void incrementRuleUsage(Long ruleId) {
        baseMapper.incrementUsageCount(ruleId);
    }

    @Override
    public List<String> predictTags(String triggerTag) {
        List<AssociationRule> rules = baseMapper.findByTriggerTag(triggerTag);
        List<String> predictions = new ArrayList<>();
        for (AssociationRule rule : rules) {
            List<String> tags = parseTags(rule.getPredictedTags());
            predictions.addAll(tags);
        }
        return predictions;
    }

    private AssociationRuleVO convertToVO(AssociationRule rule) {
        AssociationRuleVO vo = new AssociationRuleVO();
        vo.setId(rule.getId());
        vo.setTriggerTag(rule.getTriggerTag());
        vo.setPredictedTags(parseTags(rule.getPredictedTags()));
        vo.setConfidence(rule.getConfidence());
        vo.setDescription(rule.getDescription());
        vo.setStatus(rule.getStatus());
        return vo;
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            tagsJson = tagsJson.trim();
            if (tagsJson.startsWith("[")) {
                tagsJson = tagsJson.substring(1);
            }
            if (tagsJson.endsWith("]")) {
                tagsJson = tagsJson.substring(0, tagsJson.length() - 1);
            }
            if (tagsJson.isEmpty()) {
                return Collections.emptyList();
            }
            String[] tags = tagsJson.replace("\"", "").split(",");
            return Arrays.stream(tags)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("解析标签JSON失败: {}", tagsJson, e);
            return Collections.emptyList();
        }
    }
}
