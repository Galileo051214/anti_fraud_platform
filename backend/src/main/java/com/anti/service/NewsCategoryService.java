package com.anti.service;

import com.anti.entity.NewsCategory;

import java.util.List;

public interface NewsCategoryService {

    List<NewsCategory> getAllCategories();

    NewsCategory getCategoryById(Long id);

    NewsCategory createCategory(NewsCategory category);

    NewsCategory updateCategory(NewsCategory category);

    void deleteCategory(Long id);
}
