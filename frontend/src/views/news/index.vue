<template>
  <div class="news-page">
    <div class="news-page__header">
      <h2 class="news-page__title">资讯中心</h2>
      <div class="news-page__filter">
        <el-select v-model="selectedCategory" placeholder="全部分类" clearable @change="handleCategoryChange">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="selectedType" placeholder="全部类型" clearable @change="loadNews">
          <el-option label="新闻" value="news" />
          <el-option label="预警" value="warning" />
          <el-option label="政策" value="policy" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="搜索标题或内容"
          class="news-page__search"
          clearable
          @clear="loadNews"
          @keyup.enter="loadNews"
        >
          <template #prefix>
            <Search />
          </template>
        </el-input>
      </div>
    </div>

    <div class="news-page__list" v-loading="loading">
      <template v-if="newsList.length > 0">
        <div
          v-for="item in newsList"
          :key="item.id"
          class="news-card"
          @click="goToDetail(item.id)"
        >
          <div class="news-card__cover">
            <img v-if="item.coverImage" :src="item.coverImage" :alt="item.title" loading="lazy" />
            <div v-else class="news-card__icon">
              <component :is="getIconComponent(item.newsType)" />
            </div>
          </div>
          <div class="news-card__body">
            <div class="news-card__tags">
              <el-tag :type="getTagType(item.newsType)" size="small">{{ getTypeName(item.newsType) }}</el-tag>
              <span v-if="item.isTop" class="news-card__tag news-card__tag--top">置顶</span>
              <span v-if="item.isMandatory" class="news-card__tag news-card__tag--mandatory">必读</span>
            </div>
            <h3 class="news-card__title">{{ item.title }}</h3>
            <p class="news-card__summary">{{ item.summary }}</p>
            <div class="news-card__meta">
              <span class="news-card__time">
                <el-icon class="news-card__meta-icon" :size="16"><Clock /></el-icon>
                <span class="news-card__time-text">{{ formatTime(item.publishTime) }}</span>
              </span>
              <span class="news-card__stats">
                <span class="news-card__views">
                  <el-icon class="news-card__meta-icon" :size="16"><View /></el-icon>
                  <span>{{ item.viewCount }}</span>
                </span>
                <span
                  class="news-card__likes"
                  :class="{ 'news-card__likes--liked': item.isLiked }"
                  role="button"
                  tabindex="0"
                  @click.stop="handleListLike(item)"
                  @keydown.enter.prevent.stop="handleListLike(item)"
                  @keydown.space.prevent.stop="handleListLike(item)"
                >
                  <svg
                    class="news-card__heart"
                    :class="{ 'news-card__heart--liked': item.isLiked }"
                    viewBox="0 0 24 24"
                    width="16"
                    height="16"
                    aria-hidden="true"
                  >
                    <path
                      class="news-card__heart-path"
                      d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
                    />
                  </svg>
                  <span class="news-card__like-num">{{ item.likeCount ?? 0 }}</span>
                </span>
              </span>
            </div>
          </div>
        </div>
      </template>
      <el-empty v-else-if="!loading" description="暂无资讯" />
    </div>

    <div class="news-page__pagination" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Clock, View, Document, Warning, ScaleToOriginal } from '@element-plus/icons-vue'
import { getNewsPage, getCategories, likeNews, unlikeNews, type News, type NewsCategory } from '@/api/news'

const router = useRouter()

const loading = ref(false)
const keyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const newsList = ref<News[]>([])
const categories = ref<NewsCategory[]>([])
const selectedCategory = ref<number | null>(null)
const selectedType = ref<string>('')

const getTagType = (type: string) => {
  const map: Record<string, string> = { news: '', warning: 'danger', policy: 'success' }
  return map[type] || 'info'
}

const getTypeName = (type: string) => {
  const map: Record<string, string> = { news: '新闻', warning: '预警', policy: '政策' }
  return map[type] || '其他'
}

const getIconComponent = (type: string) => {
  const map: Record<string, any> = {
    news: Document,
    warning: Warning,
    policy: ScaleToOriginal
  }
  return map[type] || Document
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return ''
  const m = date.getMonth() + 1
  const d = date.getDate()
  return `${m}月${d}日`
}

const goToDetail = (id: number) => {
  router.push(`/news/${id}`)
}

const handleListLike = async (item: News) => {
  try {
    if (item.isLiked) {
      await unlikeNews(item.id)
      item.isLiked = false
      item.likeCount = Math.max(0, (item.likeCount ?? 0) - 1)
    } else {
      await likeNews(item.id)
      item.isLiked = true
      item.likeCount = (item.likeCount ?? 0) + 1
    }
  } catch {
    // 拦截器已提示
  }
}

const loadNews = async () => {
  loading.value = true
  try {
    const res = await getNewsPage({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      categoryId: selectedCategory.value || undefined,
      newsType: selectedType.value || undefined,
      keyword: keyword.value || undefined
    })
    newsList.value = res.data.records
    total.value = res.data.total
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data
  } catch {
    // error handled by interceptor
  }
}

const handleCategoryChange = () => {
  currentPage.value = 1
  loadNews()
}

const handleSizeChange = () => {
  currentPage.value = 1
  loadNews()
}

const handlePageChange = () => {
  loadNews()
}

onMounted(() => {
  loadCategories()
  loadNews()
})
</script>

<style scoped lang="scss">
.news-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;

  &__header {
    margin-bottom: 24px;
  }

  &__title {
    font-size: clamp(1.5rem, 4vw, 2rem);
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 16px;
  }

  &__filter {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }

  &__search {
    width: 240px;
  }

  &__list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
    gap: 20px;
  }

  &__pagination {
    display: flex;
    justify-content: center;
    margin-top: 32px;
  }
}

.news-card {
  background: #fff;
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-md);
  }

  &:active {
    transform: translateY(-2px);
  }

  &__cover {
    height: 160px;
    background: linear-gradient(135deg, hsl(215, 70%, 95%), hsl(215, 70%, 90%));
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__icon {
    font-size: 48px;
    color: var(--primary);
    opacity: 0.8;
  }

  &__body {
    padding: 16px;
  }

  &__tags {
    display: flex;
    gap: 8px;
    margin-bottom: 12px;
    flex-wrap: wrap;
  }

  &__tag {
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;

    &--top {
      background: hsl(0, 70%, 55%);
      color: #fff;
    }

    &--mandatory {
      background: hsl(30, 80%, 50%);
      color: #fff;
    }
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 8px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    line-height: 1.4;
  }

  &__summary {
    font-size: 14px;
    color: var(--text-secondary);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    line-height: 1.6;
    margin-bottom: 12px;
  }

  &__meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    font-size: 13px;
    color: var(--text-muted);
    flex-wrap: nowrap;
  }

  &__meta-icon {
    color: hsl(220, 9%, 46%);
    flex-shrink: 0;
  }

  &__time {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
    white-space: nowrap;
  }

  &__time-text {
    white-space: nowrap;
  }

  &__stats {
    display: inline-flex;
    align-items: center;
    gap: 14px;
    flex-shrink: 0;
    white-space: nowrap;
  }

  &__views {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    white-space: nowrap;
  }

  &__likes {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    white-space: nowrap;
    cursor: pointer;
    padding: 2px 4px;
    margin: -2px -4px;
    border-radius: 8px;
    transition:
      color 160ms ease,
      background-color 160ms ease;

    &:hover {
      background: hsl(0, 40%, 97%);
    }

    &:focus-visible {
      outline: 2px solid hsl(215, 70%, 55%);
      outline-offset: 2px;
    }
  }

  &__likes--liked {
    color: hsl(0, 55%, 42%);

    .news-card__like-num {
      color: hsl(0, 55%, 38%);
      font-weight: 600;
    }
  }

  &__heart {
    flex-shrink: 0;
    color: hsl(220, 9%, 46%);

    &-path {
      fill: transparent;
      stroke: currentColor;
      stroke-width: 1.75;
      stroke-linejoin: round;
      transition: fill 160ms ease, color 160ms ease;
    }

    &--liked {
      color: hsl(0, 65%, 52%);

      .news-card__heart-path {
        fill: currentColor;
        stroke: currentColor;
      }
    }
  }

  &__like-num {
    font-variant-numeric: tabular-nums;
    color: inherit;
  }
}

@media (max-width: 768px) {
  .news-page {
    padding: 16px;

    &__filter {
      flex-direction: column;
    }

    &__search {
      width: 100%;
    }

    &__list {
      grid-template-columns: 1fr;
    }
  }
}
</style>
