package com.anti.controller;

import com.anti.common.Result;
import com.anti.entity.News;
import com.anti.entity.dto.CreateNewsRequest;
import com.anti.entity.dto.UpdateNewsRequest;
import com.anti.security.LoginUser;
import com.anti.service.NewsService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Tag(name = "资讯管理")
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/page")
    @Operation(summary = "分页查询资讯列表")
    public Result<IPage<News>> getNewsPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "资讯类型") @RequestParam(required = false) String newsType,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        return Result.success(newsService.getNewsPage(pageNum, pageSize, categoryId, newsType, keyword, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取资讯详情")
    public Result<News> getNewsDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginUser loginUser) {
        Long userId = loginUser != null ? loginUser.getUserId() : null;
        return Result.success(newsService.getNewsDetail(id, userId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建资讯(管理员)")
    public Result<News> createNews(@RequestBody CreateNewsRequest request, @AuthenticationPrincipal LoginUser loginUser) {
        News news = new News();
        news.setTitle(request.getTitle());
        news.setContent(request.getContent());
        news.setSummary(request.getSummary());
        news.setCoverImage(request.getCoverImage());
        news.setCategoryId(request.getCategoryId());
        news.setNewsType(request.getNewsType());
        news.setIsTop(request.getIsTop());
        news.setIsMandatory(request.getIsMandatory());
        return Result.success(newsService.createNews(news, loginUser.getUserId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新资讯(管理员)")
    public Result<News> updateNews(@PathVariable Long id, @RequestBody UpdateNewsRequest request) {
        News news = new News();
        news.setId(id);
        news.setTitle(request.getTitle());
        news.setContent(request.getContent());
        news.setSummary(request.getSummary());
        news.setCoverImage(request.getCoverImage());
        news.setCategoryId(request.getCategoryId());
        news.setNewsType(request.getNewsType());
        news.setIsTop(request.getIsTop());
        news.setIsMandatory(request.getIsMandatory());
        return Result.success(newsService.updateNews(news));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除资讯(管理员)")
    public Result<Void> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "发布资讯(管理员)")
    public Result<Void> publishNews(@PathVariable Long id) {
        newsService.publishNews(id);
        return Result.success();
    }

    @PutMapping("/{id}/top")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "设置置顶(管理员)")
    public Result<Void> topNews(@PathVariable Long id, @RequestParam Integer isTop) {
        newsService.topNews(id, isTop);
        return Result.success();
    }

    @PutMapping("/{id}/mandatory")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "设置必读(管理员)")
    public Result<Void> setMandatory(@PathVariable Long id, @RequestParam Integer isMandatory) {
        newsService.setMandatory(id, isMandatory);
        return Result.success();
    }

    @PostMapping("/{id}/view")
    @Operation(summary = "增加浏览量并记录浏览")
    public Result<Void> incrementViewCount(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, Integer> body,
            @AuthenticationPrincipal LoginUser loginUser) {
        log.info("=== 资讯浏览接口调用: newsId={}, body={}, loginUser={} ===", id, body, loginUser != null ? loginUser.getUserId() : "null");
        newsService.incrementViewCount(id);
        if (loginUser != null) {
            Integer stayDuration = body != null ? body.get("stayDuration") : null;
            log.info("=== 准备记录浏览: userId={}, stayDuration={} ===", loginUser.getUserId(), stayDuration);
            newsService.addBrowseRecord(id, loginUser.getUserId(), stayDuration);
        } else {
            log.warn("=== 用户未登录，跳过浏览记录 ===");
        }
        return Result.success();
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "点赞资讯")
    public Result<Boolean> likeNews(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(newsService.likeNews(id, loginUser.getUserId()));
    }

    @DeleteMapping("/{id}/like")
    @Operation(summary = "取消点赞")
    public Result<Boolean> unlikeNews(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(newsService.unlikeNews(id, loginUser.getUserId()));
    }

    @PostMapping("/{id}/browse")
    @Operation(summary = "记录浏览")
    public Result<Void> addBrowseRecord(
            @PathVariable Long id,
            @RequestParam(required = false) Integer stayDuration,
            @AuthenticationPrincipal LoginUser loginUser) {
        newsService.addBrowseRecord(id, loginUser.getUserId(), stayDuration);
        return Result.success();
    }

    @GetMapping("/browse/history")
    @Operation(summary = "获取浏览记录")
    public Result<IPage<News>> getBrowseHistory(
            @AuthenticationPrincipal LoginUser loginUser,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(newsService.getUserBrowseHistory(loginUser.getUserId(), pageNum, pageSize));
    }
}
