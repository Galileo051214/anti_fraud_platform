package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.CaseTag;
import com.anti.service.CaseTagService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 案例标签控制器
 */
@RestController
@RequestMapping("/api/case/tag")
public class CaseTagController {

    private final CaseTagService caseTagService;

    public CaseTagController(CaseTagService caseTagService) {
        this.caseTagService = caseTagService;
    }

    /**
     * 获取所有标签
     */
    @GetMapping("/list")
    public Result<List<CaseTag>> getAllTags() {
        return Result.success(caseTagService.getAllTags());
    }

    /**
     * 根据分类获取标签
     */
    @GetMapping("/category/{category}")
    public Result<List<CaseTag>> getTagsByCategory(@PathVariable String category) {
        return Result.success(caseTagService.getTagsByCategory(category));
    }

    /**
     * 创建标签
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CaseTag> createTag(@RequestBody CaseTag tag) {
        return Result.success(caseTagService.createTag(tag));
    }

    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<CaseTag> updateTag(@PathVariable Long id, @RequestBody CaseTag tag) {
        tag.setId(id);
        return Result.success(caseTagService.updateTag(tag));
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteTag(@PathVariable Long id) {
        caseTagService.deleteTag(id);
        return Result.success();
    }
}
