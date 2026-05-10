package com.anti.service.impl;

import com.anti.common.BusinessException;
import com.anti.entity.News;
import com.anti.entity.NewsBrowseLog;
import com.anti.entity.NewsLike;
import com.anti.mapper.NewsBrowseLogMapper;
import com.anti.mapper.NewsLikeMapper;
import com.anti.mapper.NewsMapper;
import com.anti.service.AchievementService;
import com.anti.service.CacheRefreshService;
import com.anti.service.NewsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsMapper newsMapper;
    private final NewsLikeMapper newsLikeMapper;
    private final NewsBrowseLogMapper browseLogMapper;
    private final AchievementService achievementService;
    private final @Lazy CacheRefreshService cacheRefreshService;

    @Override
    public IPage<News> getNewsPage(Integer pageNum, Integer pageSize, Long categoryId, String newsType, String keyword, Long userId) {
        Page<News> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(News::getStatus, 1);
        
        if (categoryId != null) {
            wrapper.eq(News::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(newsType)) {
            wrapper.eq(News::getNewsType, newsType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(News::getTitle, keyword).or().like(News::getContent, keyword));
        }
        
        wrapper.orderByDesc(News::getIsTop)
               .orderByDesc(News::getPublishTime);
        
        Page<News> result = newsMapper.selectPage(page, wrapper);
        
        List<News> records = result.getRecords();
        for (News news : records) {
            news.setLikeCount((long) newsLikeMapper.countByNewsId(news.getId()));
        }

        if (userId != null && !records.isEmpty()) {
            List<Long> ids = records.stream().map(News::getId).collect(Collectors.toList());
            List<Long> likedIds = newsLikeMapper.selectLikedNewsIds(userId, ids);
            Set<Long> likedSet = new HashSet<>(likedIds);
            for (News news : records) {
                news.setIsLiked(likedSet.contains(news.getId()));
            }
        }
        
        return result;
    }

    @Override
    public News getNewsDetail(Long id, Long userId) {
        News news = newsMapper.selectNewsDetailById(id);
        if (news == null) {
            throw new BusinessException(404, "资讯不存在");
        }
        news.setLikeCount((long) newsLikeMapper.countByNewsId(id));
        
        if (userId != null) {
            int liked = newsLikeMapper.countByNewsIdAndUserId(id, userId);
            news.setIsLiked(liked > 0);
        }
        
        return news;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public News createNews(News news, Long authorId) {
        if (news.getTitle() == null || news.getTitle().trim().isEmpty()) {
            throw new BusinessException(400, "标题不能为空");
        }
        if (news.getContent() == null || news.getContent().trim().isEmpty()) {
            throw new BusinessException(400, "内容不能为空");
        }
        news.setAuthorId(authorId);
        news.setViewCount(0);
        news.setStatus(0);
        news.setIsTop(news.getIsTop() != null ? news.getIsTop() : 0);
        news.setIsMandatory(news.getIsMandatory() != null ? news.getIsMandatory() : 0);
        newsMapper.insert(news);
        log.info("创建资讯: {}", news.getTitle());
        return news;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public News updateNews(News news) {
        News existing = newsMapper.selectById(news.getId());
        if (existing == null) {
            throw new BusinessException(404, "资讯不存在");
        }
        if (news.getTitle() != null) {
            existing.setTitle(news.getTitle());
        }
        if (news.getContent() != null) {
            existing.setContent(news.getContent());
        }
        if (news.getSummary() != null) {
            existing.setSummary(news.getSummary());
        }
        if (news.getCoverImage() != null) {
            existing.setCoverImage(news.getCoverImage());
        }
        if (news.getCategoryId() != null) {
            existing.setCategoryId(news.getCategoryId());
        }
        if (news.getNewsType() != null) {
            existing.setNewsType(news.getNewsType());
        }
        if (news.getIsTop() != null) {
            existing.setIsTop(news.getIsTop());
        }
        if (news.getIsMandatory() != null) {
            existing.setIsMandatory(news.getIsMandatory());
        }
        newsMapper.updateById(existing);
        log.info("更新资讯: {}", existing.getTitle());
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNews(Long id) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException(404, "资讯不存在");
        }
        newsMapper.deleteById(id);
        log.info("删除资讯: {}", news.getTitle());
        
        // 异步刷新相关缓存
        cacheRefreshService.handleDeleteEvent("news", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishNews(Long id) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException(404, "资讯不存在");
        }
        news.setStatus(1);
        news.setPublishTime(LocalDateTime.now());
        newsMapper.updateById(news);
        log.info("发布资讯: {}", news.getTitle());
        
        // 异步刷新相关缓存
        cacheRefreshService.handlePublishEvent("news", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topNews(Long id, Integer isTop) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException(404, "资讯不存在");
        }
        news.setIsTop(isTop);
        newsMapper.updateById(news);
        log.info("设置资讯 {} 置顶状态: {}", news.getTitle(), isTop == 1 ? "是" : "否");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setMandatory(Long id, Integer isMandatory) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BusinessException(404, "资讯不存在");
        }
        news.setIsMandatory(isMandatory);
        newsMapper.updateById(news);
        log.info("设置资讯 {} 必读状态: {}", news.getTitle(), isMandatory == 1 ? "是" : "否");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long id) {
        newsMapper.incrementViewCount(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean likeNews(Long newsId, Long userId) {
        News news = newsMapper.selectById(newsId);
        if (news == null) {
            throw new BusinessException(404, "资讯不存在");
        }
        int existing = newsLikeMapper.countByNewsIdAndUserId(newsId, userId);
        if (existing > 0) {
            throw new BusinessException(400, "已点赞过该资讯");
        }
        NewsLike like = new NewsLike();
        like.setNewsId(newsId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        newsLikeMapper.insert(like);
        log.info("用户 {} 点赞资讯 {}", userId, newsId);
        
        // 异步刷新相关缓存
        cacheRefreshService.handleLikeEvent(userId, "news", newsId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unlikeNews(Long newsId, Long userId) {
        LambdaQueryWrapper<NewsLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NewsLike::getNewsId, newsId).eq(NewsLike::getUserId, userId);
        int deleted = newsLikeMapper.delete(wrapper);
        if (deleted > 0) {
            log.info("用户 {} 取消点赞资讯 {}", userId, newsId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBrowseRecord(Long newsId, Long userId, Integer stayDuration) {
        log.info("=== 资讯浏览记录开始: newsId={}, userId={}, stayDuration={} ===", newsId, userId, stayDuration);
        NewsBrowseLog browseLog = new NewsBrowseLog();
        browseLog.setNewsId(newsId);
        browseLog.setUserId(userId);
        browseLog.setBrowseTime(LocalDateTime.now());
        browseLog.setStayDuration(stayDuration != null ? stayDuration : 0);
        int rows = browseLogMapper.insert(browseLog);
        log.info("=== 资讯浏览记录插入结果: rows={}, logId={} ===", rows, browseLog.getId());
        try {
            achievementService.refreshContinuousLearningStreak(userId);
        } catch (Exception e) {
            log.warn("资讯浏览后连续学习成就校验失败 userId={} msg={}", userId, e.getMessage());
        }
    }

    @Override
    public IPage<News> getUserBrowseHistory(Long userId, Integer pageNum, Integer pageSize) {
        Page<NewsBrowseLog> page = new Page<>(pageNum, pageSize);
        IPage<NewsBrowseLog> browseLogs = browseLogMapper.selectUserBrowseHistory(page, userId);
        
        Page<News> result = new Page<>(pageNum, pageSize);
        result.setTotal(browseLogs.getTotal());
        
        for (NewsBrowseLog browseLog : browseLogs.getRecords()) {
            News news = newsMapper.selectById(browseLog.getNewsId());
            if (news != null) {
                result.getRecords().add(news);
            }
        }
        
        return result;
    }
}
