<template>
  <div class="news-detail">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>
        <span class="news-detail__back-title">资讯详情</span>
      </template>
    </el-page-header>

    <div class="news-detail__content" v-loading="loading">
      <template v-if="news">
        <div class="news-detail__card">
          <header class="news-detail__header">
            <div class="news-detail__tags">
              <el-tag :type="getTagType(news.newsType)">{{ getTypeName(news.newsType) }}</el-tag>
              <span v-if="news.isTop" class="news-detail__badge news-detail__badge--top">置顶</span>
              <span v-if="news.isMandatory" class="news-detail__badge news-detail__badge--mandatory">必读</span>
            </div>
            <h1 class="news-detail__title">{{ news.title }}</h1>
            <div class="news-detail__meta">
              <span class="news-detail__meta-item news-detail__author">
                <el-icon class="news-detail__meta-icon" :size="16"><User /></el-icon>
                <span class="news-detail__meta-text">{{ displayAuthor(news) }}</span>
              </span>
              <span class="news-detail__meta-item news-detail__time">
                <el-icon class="news-detail__meta-icon" :size="16"><Clock /></el-icon>
                <span class="news-detail__meta-text">{{ formatDate(news.publishTime) }}</span>
              </span>
              <span class="news-detail__meta-item news-detail__views">
                <el-icon class="news-detail__meta-icon" :size="16"><View /></el-icon>
                <span class="news-detail__meta-text">{{ news.viewCount }} 次浏览</span>
              </span>
            </div>
          </header>

          <div class="news-detail__cover" v-if="news.coverImage">
            <img :src="news.coverImage" :alt="news.title" />
          </div>

          <article class="news-detail__body" v-html="news.content"></article>

          <footer class="news-detail__footer">
            <button
              type="button"
              class="news-detail__like-btn"
              :class="{ 'news-detail__like-btn--active': news.isLiked }"
              @click="handleLike"
              :disabled="likeLoading"
            >
              <svg
                class="news-detail__heart"
                :class="{ 'news-detail__heart--liked': news.isLiked }"
                viewBox="0 0 24 24"
                width="22"
                height="22"
                aria-hidden="true"
              >
                <path
                  class="news-detail__heart-path"
                  d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
                />
              </svg>
              <span class="news-detail__like-count">{{ news.likeCount || 0 }}</span>
              <span class="news-detail__like-label">{{ news.isLiked ? '已点赞' : '点赞' }}</span>
            </button>
          </footer>
        </div>

        <RelatedNews v-if="relatedList.length > 0" :list="relatedList" @item-click="goToDetail" />
      </template>

      <el-empty v-else-if="!loading" description="资讯不存在或已被删除" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Clock, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getNewsDetail, likeNews, unlikeNews, viewNews, type News } from '@/api/news'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const likeLoading = ref(false)
const news = ref<News | null>(null)
const relatedList = ref<Partial<News>[]>([])
let browseTimer: number | null = null
let startTime = Date.now()

const getTagType = (type: string) => {
  const map: Record<string, string> = { news: '', warning: 'danger', policy: 'success' }
  return map[type] || 'info'
}

const getTypeName = (type: string) => {
  const map: Record<string, string> = { news: '新闻', warning: '预警', policy: '政策' }
  return map[type] || '其他'
}

const formatDate = (date: string) => {
  if (!date) return ''
  const d = new Date(date)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

const displayAuthor = (n: News) => {
  const raw = n.authorName || '系统管理员'
  const name = raw.replace(/\s+/g, ' ').trim()
  return name || '系统管理员'
}

const loadNews = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await getNewsDetail(id)
    news.value = res.data
    
    // start browse timer - will send browse record with stayDuration on unmount
    startTime = Date.now()
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleLike = async () => {
  if (!news.value) return
  
  likeLoading.value = true
  try {
    if (news.value.isLiked) {
      await unlikeNews(news.value.id)
      news.value.isLiked = false
      news.value.likeCount = Math.max(0, (news.value.likeCount || 1) - 1)
      ElMessage.success('已取消点赞')
    } else {
      await likeNews(news.value.id)
      news.value.isLiked = true
      news.value.likeCount = (news.value.likeCount || 0) + 1
      ElMessage.success('点赞成功')
    }
  } catch {
    // error handled by interceptor
  } finally {
    likeLoading.value = false
  }
}

const goToDetail = (id: number) => {
  router.push(`/news/${id}`)
}

const sendBrowseRecord = () => {
  if (!news.value || !startTime) return
  const duration = Math.floor((Date.now() - startTime) / 1000)
  viewNews(news.value.id, duration).catch(() => {})
}

onMounted(() => {
  loadNews()
  
  window.addEventListener('beforeunload', sendBrowseRecord)
})

onUnmounted(() => {
  sendBrowseRecord()
  window.removeEventListener('beforeunload', sendBrowseRecord)
  if (browseTimer) clearTimeout(browseTimer)
})
</script>

<style scoped lang="scss">
.news-detail {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;

  &__back-title {
    font-size: 16px;
    color: var(--text-secondary);
  }

  &__content {
    margin-top: 24px;
  }

  &__card {
    background: #fff;
    border-radius: 16px;
    padding: 24px 22px;
    border: 1px solid hsl(220, 14%, 92%);
    box-shadow: 0 2px 14px hsla(220, 20%, 40%, 0.06);
  }

  &__header {
    margin-bottom: 20px;
  }

  &__tags {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
  }

  &__badge {
    padding: 2px 10px;
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
    font-size: clamp(1.5rem, 4vw, 2rem);
    font-weight: 700;
    color: var(--text-primary);
    line-height: 1.3;
    margin-bottom: 16px;
  }

  &__meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px 22px;
    color: hsl(220, 9%, 46%);
    font-size: 14px;
  }

  &__meta-item {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    max-width: 100%;
  }

  &__meta-icon {
    flex-shrink: 0;
    color: hsl(220, 9%, 55%);
  }

  &__meta-text {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__cover {
    margin-bottom: 22px;
    border-radius: 12px;
    overflow: hidden;
    border: 1px solid hsl(220, 14%, 94%);

    img {
      width: 100%;
      height: auto;
      display: block;
    }
  }

  &__body {
    font-size: 16px;
    line-height: 1.8;
    color: var(--text-secondary);

    :deep(p) {
      margin-bottom: 16px;
    }

    :deep(ol), :deep(ul) {
      margin: 16px 0;
      padding-left: 24px;
    }

    :deep(li) {
      margin-bottom: 8px;
    }

    :deep(strong) {
      color: var(--danger);
    }

    :deep(img) {
      max-width: 100%;
      height: auto;
      border-radius: var(--radius-sm);
    }
  }

  &__footer {
    margin-top: 28px;
    padding-top: 20px;
    border-top: 1px solid hsl(220, 14%, 93%);
    display: flex;
    align-items: center;
    justify-content: flex-start;
  }

  &__heart {
    flex-shrink: 0;
    color: hsl(220, 9%, 46%);

    &-path {
      fill: transparent;
      stroke: currentColor;
      stroke-width: 1.75;
      stroke-linejoin: round;
      transition: fill 160ms ease, stroke 160ms ease, color 160ms ease;
    }

    &--liked {
      color: hsl(0, 65%, 52%);

      .news-detail__heart-path {
        fill: currentColor;
        stroke: currentColor;
      }
    }
  }

  &__like-btn {
    display: inline-flex;
    flex-direction: row;
    align-items: center;
    gap: 10px;
    padding: 10px 18px;
    border: 1px solid hsl(220, 14%, 88%);
    border-radius: 999px;
    background: hsl(220, 20%, 99%);
    color: hsl(220, 10%, 38%);
    font-size: 14px;
    cursor: pointer;
    transition:
      border-color 180ms ease,
      background-color 180ms ease,
      color 180ms ease,
      box-shadow 180ms ease;

    &:hover:not(:disabled) {
      border-color: hsl(0, 55%, 75%);
      background: hsl(0, 40%, 99%);
      color: hsl(0, 50%, 40%);
    }

    &:active:not(:disabled) {
      transform: scale(0.98);
    }

    &:disabled {
      opacity: 0.55;
      cursor: not-allowed;
    }
  }

  &__like-count {
    font-weight: 600;
    color: hsl(220, 12%, 32%);
    font-variant-numeric: tabular-nums;
  }

  &__like-label {
    white-space: nowrap;
  }

  &__like-btn--active {
    border-color: hsl(0, 55%, 82%);
    background: hsl(0, 75%, 98%);
    color: hsl(0, 55%, 42%);

    .news-detail__like-count {
      color: hsl(0, 55%, 38%);
    }
  }
}

.RelatedNews {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
}

@media (max-width: 768px) {
  .news-detail {
    padding: 16px;

    &__card {
      padding: 18px 16px;
    }
  }
}
</style>

<script lang="ts">
import { defineComponent, h } from 'vue'

const RelatedNews = defineComponent({
  name: 'RelatedNews',
  props: {
    list: {
      type: Array as () => Partial<News>[],
      default: () => []
    }
  },
  emits: ['item-click'],
  setup(props, { emit }) {
    return () => h('div', { class: 'related-news' }, [
      h('h3', { class: 'related-news__title' }, '相关资讯'),
      h('div', { class: 'related-news__list' },
        props.list.map(item =>
          h('div', {
            class: 'related-news__item',
            onClick: () => emit('item-click', item.id)
          }, [
            h('span', { class: 'related-news__item-title' }, item.title)
          ])
        )
      )
    ])
  }
})

export default {}
</script>

<style scoped>
.related-news {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
}

.related-news__title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.related-news__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.related-news__item {
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.related-news__item:hover {
  background: var(--bg-tertiary);
}

.related-news__item-title {
  font-size: 14px;
  color: var(--text-primary);
}
</style>
