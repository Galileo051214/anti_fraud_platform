package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.NewsCategory;
import com.anti.mapper.NewsCategoryMapper;
import com.anti.service.NewsCategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCategoryServiceImpl implements NewsCategoryService {

    private final NewsCategoryMapper categoryMapper;

    @Override
    public List<NewsCategory> getAllCategories() {
        LambdaQueryWrapper<NewsCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(NewsCategory::getSortOrder);
        return categoryMapper.selectList(wrapper);
    }

    @Override
    public NewsCategory getCategoryById(Long id) {
        NewsCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NewsCategory createCategory(NewsCategory category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new BusinessException(400, "分类名称不能为空");
        }
        categoryMapper.insert(category);
        log.info("创建资讯分类: {}", category.getName());
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NewsCategory updateCategory(NewsCategory category) {
        NewsCategory existing = categoryMapper.selectById(category.getId());
        if (existing == null) {
            throw new BusinessException(404, "分类不存在");
        }
        if (category.getName() != null && !category.getName().trim().isEmpty()) {
            existing.setName(category.getName());
        }
        if (category.getParentId() != null) {
            existing.setParentId(category.getParentId());
        }
        if (category.getSortOrder() != null) {
            existing.setSortOrder(category.getSortOrder());
        }
        categoryMapper.updateById(existing);
        log.info("更新资讯分类: {}", existing.getName());
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        NewsCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }
        categoryMapper.deleteById(id);
        log.info("删除资讯分类: {}", category.getName());
    }
}
