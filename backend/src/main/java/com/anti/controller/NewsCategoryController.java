package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.NewsCategory;
import com.anti.service.NewsCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news/category")
@RequiredArgsConstructor
@Tag(name = "资讯分类管理")
public class NewsCategoryController {

    private final NewsCategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "获取分类列表")
    public Result<List<NewsCategory>> getCategoryList() {
        return Result.success(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情")
    public Result<NewsCategory> getCategoryById(@PathVariable Long id) {
        return Result.success(categoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建分类(管理员)")
    public Result<NewsCategory> createCategory(@RequestBody NewsCategory category) {
        return Result.success(categoryService.createCategory(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新分类(管理员)")
    public Result<NewsCategory> updateCategory(@PathVariable Long id, @RequestBody NewsCategory category) {
        category.setId(id);
        return Result.success(categoryService.updateCategory(category));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除分类(管理员)")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
