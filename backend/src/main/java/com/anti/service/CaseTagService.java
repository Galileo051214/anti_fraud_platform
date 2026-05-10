package com.anti.service;

import com.anti.entity.CaseTag;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 案例标签服务接口
 */
public interface CaseTagService extends IService<CaseTag> {

    /**
     * 获取所有标签列表
     */
    List<CaseTag> getAllTags();

    /**
     * 根据分类获取标签列表
     */
    List<CaseTag> getTagsByCategory(String category);

    /**
     * 创建标签
     */
    CaseTag createTag(CaseTag tag);

    /**
     * 更新标签
     */
    CaseTag updateTag(CaseTag tag);

    /**
     * 删除标签
     */
    void deleteTag(Long tagId);
}
