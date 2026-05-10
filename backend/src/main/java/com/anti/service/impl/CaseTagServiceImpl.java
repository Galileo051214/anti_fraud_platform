package com.anti.service.impl;

import com.anti.entity.CaseTag;
import com.anti.mapper.CaseTagMapper;
import com.anti.service.CaseTagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 案例标签服务实现类
 */
@Service
public class CaseTagServiceImpl extends ServiceImpl<CaseTagMapper, CaseTag> implements CaseTagService {

    @Override
    public List<CaseTag> getAllTags() {
        return list(new LambdaQueryWrapper<CaseTag>()
                .orderByAsc(CaseTag::getCategory)
                .orderByAsc(CaseTag::getName));
    }

    @Override
    public List<CaseTag> getTagsByCategory(String category) {
        return list(new LambdaQueryWrapper<CaseTag>()
                .eq(CaseTag::getCategory, category)
                .orderByAsc(CaseTag::getName));
    }

    @Override
    @Transactional
    public CaseTag createTag(CaseTag tag) {
        save(tag);
        return tag;
    }

    @Override
    @Transactional
    public CaseTag updateTag(CaseTag tag) {
        updateById(tag);
        return tag;
    }

    @Override
    @Transactional
    public void deleteTag(Long tagId) {
        removeById(tagId);
    }
}
