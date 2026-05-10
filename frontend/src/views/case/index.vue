<template>
  <div class="case-index">
    <div class="case-index__header">
      <h1 class="case-index__title">诈骗案例库</h1>
      <p class="case-index__subtitle">了解最新诈骗手法，提升防范意识</p>
    </div>

    <div class="case-index__toolbar">
      <div class="case-index__search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索案例标题..."
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <svg class="search-icon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="M21 21l-4.35-4.35"/>
            </svg>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <div class="case-index__tags" v-if="tags.length">
      <div class="tags-wrapper">
        <span
          class="tag-item"
          :class="{ 'tag-item--active': selectedTagId === null }"
          @click="handleTagSelect(null)"
        >
          全部
        </span>
        <span
          v-for="tag in tags"
          :key="tag.id"
          class="tag-item"
          :class="{ 'tag-item--active': selectedTagId === tag.id }"
          :style="selectedTagId === tag.id ? { backgroundColor: tag.color, borderColor: tag.color } : {}"
          @click="handleTagSelect(tag.id)"
        >
          {{ tag.name }}
        </span>
      </div>
    </div>

    <div class="case-index__content" v-loading="loading">
      <div class="case-list" v-if="caseList.length">
        <div
          v-for="caseItem in caseList"
          :key="caseItem.id"
          class="case-card"
          @click="goToDetail(caseItem)"
          role="button"
          tabindex="0"
          @keydown.enter.prevent="goToDetail(caseItem)"
          @keydown.space.prevent="goToDetail(caseItem)"
        >
          <div class="case-card__header">
            <span class="case-card__type">{{ caseItem.caseType }}</span>
            <span v-if="caseItem.isFeatured" class="case-card__featured">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
              </svg>
              精选
            </span>
          </div>

          <h3 class="case-card__title">{{ caseItem.title }}</h3>

          <div class="case-card__tags" v-if="caseItem.tags.length">
            <span
              v-for="tag in caseItem.tags.slice(0, 3)"
              :key="tag.id"
              class="case-card__tag"
              :style="{ backgroundColor: tag.color + '20', color: tag.color }"
            >
              {{ tag.name }}
            </span>
          </div>

          <div class="case-card__meta">
            <div class="case-card__stats">
              <span class="stat-item">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
                {{ caseItem.viewCount }}
              </span>
              <span class="stat-item" :class="{ 'stat-item--liked': caseItem.isLiked }" @click.stop="handleLike(caseItem)">
                <svg viewBox="0 0 24 24" width="14" height="14" :fill="caseItem.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                </svg>
                {{ caseItem.likeCount }}
              </span>
            </div>
            <div class="case-card__badges">
              <span class="badge badge--difficulty" :class="`badge--level-${caseItem.difficultyLevel}`">
                {{ caseItem.difficultyName }}
              </span>
              <span class="badge badge--risk" :class="`badge--risk-${getRiskClass(caseItem.riskLevel)}`">
                {{ caseItem.riskLevel }}风险
              </span>
            </div>
          </div>

          <div class="case-card__footer">
            <span class="case-card__time">{{ formatTime(caseItem.publishTime) }}</span>
          </div>
        </div>
      </div>

      <el-empty v-else-if="!loading" description="暂无案例"></el-empty>

      <div class="case-index__pagination" v-if="pagination.total > pagination.pageSize">
        <el-pagination
          v-model:current-page="pagination.current"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCasePage, getAllTags, likeCase, unlikeCase } from '@/api/case'

const router = useRouter()

const loading = ref(false)
const caseList = ref<any[]>([])
const tags = ref<any[]>([])
const selectedTagId = ref<number | null>(null)
const searchKeyword = ref('')

const pagination = ref({
  current: 1,
  pageSize: 12,
  total: 0
})

const goToDetail = (caseItem: any) => {
  // router 中的案例详情路由是 /case/:id
  router.push(`/case/${caseItem.id}`)
}

const fetchCases = async () => {
  loading.value = true
  try {
    const res = await getCasePage({
      pageNum: pagination.value.current,
      pageSize: pagination.value.pageSize,
      tagId: selectedTagId.value || undefined,
      keyword: searchKeyword.value || undefined
    })
    caseList.value = res.data.records ?? []
    pagination.value.total = res.data.total ?? 0
  } catch (error) {
    console.error('获取案例列表失败:', error)
  } finally {
    loading.value = false
  }
}

const fetchTags = async () => {
  try {
    const res = await getAllTags()
    tags.value = res.data ?? []
  } catch (error) {
    console.error('获取标签失败:', error)
  }
}

const handleSearch = () => {
  pagination.value.current = 1
  fetchCases()
}

const handleTagSelect = (tagId: number | null) => {
  selectedTagId.value = tagId
  pagination.value.current = 1
  fetchCases()
}

const handlePageChange = (page: number) => {
  pagination.value.current = page
  fetchCases()
}

const handleLike = async (caseItem: any) => {
  try {
    if (caseItem.isLiked) {
      await unlikeCase(caseItem.id)
      caseItem.isLiked = false
      caseItem.likeCount--
    } else {
      await likeCase(caseItem.id)
      caseItem.isLiked = true
      caseItem.likeCount++
    }
  } catch (error) {
    console.error('点赞操作失败:', error)
  }
}

const getRiskClass = (riskLevel: string) => {
  const map: Record<string, string> = {
    '极高': 'high',
    '高': 'medium',
    '中': 'normal',
    '低': 'low',
    '极低': 'lowest'
  }
  return map[riskLevel] || 'normal'
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) return '今日'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchCases()
  fetchTags()
})
</script>

<style scoped lang="scss">
.case-index {
  // 组件内补齐项目使用的变量名映射，避免全局未定义导致样式失效
  --text-primary: var(--text-color-primary);
  --text-secondary: var(--text-color-secondary);
  --text-placeholder: var(--text-color-placeholder);
  --border-color: var(--border-color-base);
  --bg-primary: #ffffff; // 白底名片
  --bg-secondary: var(--background-color-base);
  --bg-hover: #eef4ff;

  --transition-fast: 200ms ease-in-out;

  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;

  &__header {
    text-align: center;
    margin-bottom: 32px;
  }

  &__title {
    font-size: 28px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 8px;
  }

  &__subtitle {
    font-size: 14px;
    color: var(--text-secondary);
    margin: 0;
  }

  &__toolbar {
    margin-bottom: 24px;
  }

  &__search {
    display: flex;
    gap: 12px;
    max-width: 480px;
    margin: 0 auto;

    .el-input {
      flex: 1;
    }

    .search-icon {
      color: var(--text-placeholder);
    }
  }

  &__tags {
    margin-bottom: 24px;
    overflow-x: auto;

    &::-webkit-scrollbar {
      height: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: var(--border-color);
      border-radius: 2px;
    }
  }

  &__content {
    min-height: 400px;
  }

  &__pagination {
    display: flex;
    justify-content: center;
    margin-top: 32px;
  }
}

.tags-wrapper {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 4px 0;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    color: var(--primary-color);
    border-color: var(--primary-color);
  }

  &--active {
    color: #fff;
    background: var(--primary-color);
    border-color: var(--primary-color);
  }
}

.case-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.case-card {
  background: var(--bg-primary);
  border: 1px solid rgba(64, 158, 255, 0.16);
  border-radius: 16px;
  padding: 20px;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: transform var(--transition-fast),
              box-shadow var(--transition-fast),
              border-color var(--transition-fast),
              background-color var(--transition-fast);

  &:hover {
    border-color: rgba(64, 158, 255, 0.35);
    box-shadow: 0 12px 30px rgba(64, 158, 255, 0.12);
    transform: translateY(-3px);
  }

  &:active {
    transform: translateY(-1px) scale(0.99);
  }

  &:focus-visible {
    outline: 2px solid rgba(64, 158, 255, 0.55);
    outline-offset: 2px;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  &__type {
    font-size: 12px;
    color: var(--text-placeholder);
  }

  &__featured {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #f59e0b;
  }

  &__title {
    font-size: 16px;
    font-weight: 500;
    color: var(--text-primary);
    margin: 0 0 12px;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 12px;
  }

  &__tag {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    font-size: 11px;
    border-radius: 4px;
  }

  &__meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  &__stats {
    display: flex;
    gap: 16px;
  }

  &__badges {
    display: flex;
    gap: 6px;
  }

  &__footer {
    padding-top: 12px;
    border-top: 1px solid var(--border-color);
  }

  &__time {
    font-size: 12px;
    color: var(--text-placeholder);
  }
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);

  &--liked {
    color: #ef4444;
  }

  svg {
    opacity: 0.7;
  }
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  font-size: 11px;
  border-radius: 4px;

  &--difficulty {
    color: #6b7280;
    background: #f3f4f6;

    &.badge--level-1 { color: #10b981; background: #d1fae5; }
    &.badge--level-2 { color: #3b82f6; background: #dbeafe; }
    &.badge--level-3 { color: #f59e0b; background: #fef3c7; }
    &.badge--level-4 { color: #ef4444; background: #fee2e2; }
    &.badge--level-5 { color: #dc2626; background: #fecaca; }
  }

  &--risk {
    &.badge--risk-highest { color: #dc2626; background: #fef2f2; }
    &.badge--risk-high { color: #ef4444; background: #fee2e2; }
    &.badge--risk-normal { color: #f59e0b; background: #fef3c7; }
    &.badge--risk-low { color: #10b981; background: #d1fae5; }
    &.badge--risk-lowest { color: #6b7280; background: #f3f4f6; }
  }
}
</style>
