package com.anti.service;

import com.anti.entity.News;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface NewsService {

    IPage<News> getNewsPage(Integer pageNum, Integer pageSize, Long categoryId, String newsType, String keyword, Long userId);

    News getNewsDetail(Long id, Long userId);

    News createNews(News news, Long authorId);

    News updateNews(News news);

    void deleteNews(Long id);

    void publishNews(Long id);

    void topNews(Long id, Integer isTop);

    void setMandatory(Long id, Integer isMandatory);

    void incrementViewCount(Long id);

    boolean likeNews(Long newsId, Long userId);

    boolean unlikeNews(Long newsId, Long userId);

    void addBrowseRecord(Long newsId, Long userId, Integer stayDuration);

    IPage<News> getUserBrowseHistory(Long userId, Integer pageNum, Integer pageSize);
}
