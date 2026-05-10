<template>
  <div class="forum-page">
    <header class="forum-page__header">
      <div class="forum-page__header-left">
        <h1 class="forum-page__title">社区</h1>
        <p class="forum-page__subtitle">分享防骗经验，交流心得</p>
      </div>
      <button class="forum-page__publish-btn" @click="openPublishDialog">
        <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 5v14M5 12h14"/>
        </svg>
        发布帖子
      </button>
    </header>

    <div class="forum-page__filter">
      <div class="forum-page__tabs" role="tablist">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          class="forum-page__tab"
          :class="{ 'forum-page__tab--active': activeTab === tab.value }"
          role="tab"
          :aria-selected="activeTab === tab.value"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="forum-page__actions">
        <select class="forum-page__sort-select" v-model="sortBy" @change="loadPosts">
          <option value="time">按时间</option>
          <option value="like">按点赞</option>
          <option value="comment">按评论</option>
        </select>
        <div class="forum-page__search">
          <input
            type="text"
            class="forum-page__search-input"
            placeholder="搜索帖子..."
            v-model="keyword"
            @keyup.enter="loadPosts"
          />
          <button class="forum-page__search-btn" @click="loadPosts">
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.35-4.35"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <main class="forum-page__content" v-loading="loading">
      <div v-if="!loading && postList.length === 0" class="forum-page__empty">
        <svg class="forum-page__empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
        <p>暂无帖子，来说点什么吧</p>
      </div>

      <article
        v-for="post in postList"
        :key="post.id"
        class="post-card"
        @click="goToDetail(post.id)"
      >
        <div class="post-card__header">
          <div class="post-card__author">
            <div class="post-card__avatar">
              <img v-if="post.authorAvatar" :src="post.authorAvatar" :alt="post.authorName" />
              <span v-else>{{ post.authorName?.[0] }}</span>
            </div>
            <div class="post-card__author-info">
              <span class="post-card__author-name">{{ post.authorName || '匿名用户' }}</span>
              <span class="post-card__time">{{ formatTime(post.createTime) }}</span>
            </div>
          </div>
          <span class="post-card__type" :class="`post-card__type--${post.postType}`">
            {{ getTypeName(post.postType) }}
          </span>
        </div>

        <h3 class="post-card__title">{{ post.title }}</h3>
        <p class="post-card__content">{{ post.content }}</p>

        <footer class="post-card__footer">
          <span class="post-card__stat">
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
            {{ post.viewCount }}
          </span>
          <span class="post-card__stat">
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            {{ post.commentCount }}
          </span>
          <button
            class="post-card__like"
            :class="{ 'post-card__like--active': post.isLiked }"
            @click.stop="toggleLike(post)"
          >
            <svg class="icon" viewBox="0 0 24 24" :fill="post.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
            </svg>
            {{ post.likeCount }}
          </button>
        </footer>
      </article>

      <div class="forum-page__pagination" v-if="total > 0">
        <button
          class="forum-page__page-btn"
          :disabled="pageNum <= 1"
          @click="changePage(pageNum - 1)"
        >
          上一页
        </button>
        <span class="forum-page__page-info">{{ pageNum }} / {{ totalPages }}</span>
        <button
          class="forum-page__page-btn"
          :disabled="pageNum >= totalPages"
          @click="changePage(pageNum + 1)"
        >
          下一页
        </button>
      </div>
    </main>

    <div class="forum-page__dialog-overlay" v-if="showPublishDialog" @click.self="closePublishDialog">
      <div class="forum-page__dialog">
        <header class="forum-page__dialog-header">
          <h2>发布帖子</h2>
          <button class="forum-page__dialog-close" @click="closePublishDialog">
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6 6 18M6 6l12 12"/>
            </svg>
          </button>
        </header>
        <div class="forum-page__dialog-body">
          <div class="forum-page__form-item">
            <label class="forum-page__label">帖子类型</label>
            <div class="forum-page__type-options">
              <button
                v-for="tab in tabs"
                :key="tab.value"
                class="forum-page__type-btn"
                :class="{ 'forum-page__type-btn--active': publishForm.postType === tab.value }"
                @click="publishForm.postType = tab.value"
              >
                {{ tab.label }}
              </button>
            </div>
          </div>
          <div class="forum-page__form-item">
            <label class="forum-page__label">标题</label>
            <input
              type="text"
              class="forum-page__input"
              v-model="publishForm.title"
              placeholder="请输入标题"
              maxlength="50"
            />
          </div>
          <div class="forum-page__form-item">
            <label class="forum-page__label">内容</label>
            <textarea
              class="forum-page__textarea"
              v-model="publishForm.content"
              placeholder="请输入内容..."
              rows="8"
            ></textarea>
          </div>
        </div>
        <footer class="forum-page__dialog-footer">
          <button class="forum-page__btn forum-page__btn--secondary" @click="closePublishDialog">取消</button>
          <button class="forum-page__btn forum-page__btn--primary" :disabled="publishing" @click="submitPost">
            {{ publishing ? '发布中...' : '发布' }}
          </button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPostPage, createPost, likePost, unlikePost } from '@/api/forum'
import type { PostVO, CreatePostRequest } from '@/api/forum'

const router = useRouter()

const tabs = [
  { label: '经验分享', value: 'experience' },
  { label: '求助问答', value: 'question' },
  { label: '讨论交流', value: 'discussion' }
]

const activeTab = ref('experience')
const sortBy = ref('time')
const keyword = ref('')
const loading = ref(false)
const publishing = ref(false)
const showPublishDialog = ref(false)

const postList = ref<PostVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const publishForm = reactive<CreatePostRequest>({
  postType: 'experience',
  title: '',
  content: ''
})

const getTypeName = (type: string) => {
  const map: Record<string, string> = { experience: '经验', question: '问答', discussion: '讨论' }
  return map[type] || '其他'
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return time.slice(0, 10)
}

const loadPosts = async () => {
  loading.value = true
  try {
    const res = await getPostPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      postType: activeTab.value,
      sortBy: sortBy.value,
      keyword: keyword.value || undefined
    })
    postList.value = res.data.records
    total.value = res.data.total
  } catch {
    ElMessage.error('加载帖子失败')
  } finally {
    loading.value = false
  }
}

const switchTab = (value: string) => {
  activeTab.value = value
  pageNum.value = 1
  loadPosts()
}

const changePage = (page: number) => {
  pageNum.value = page
  loadPosts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goToDetail = (id: number) => {
  router.push(`/forum/${id}`)
}

const openPublishDialog = () => {
  showPublishDialog.value = true
}

const closePublishDialog = () => {
  showPublishDialog.value = false
  publishForm.title = ''
  publishForm.content = ''
  publishForm.postType = 'experience'
}

const submitPost = async () => {
  if (!publishForm.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!publishForm.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  publishing.value = true
  try {
    await createPost(publishForm)
    ElMessage.success('发布成功')
    closePublishDialog()
    loadPosts()
  } catch {
    ElMessage.error('发布失败')
  } finally {
    publishing.value = false
  }
}

const toggleLike = async (post: PostVO) => {
  try {
    if (post.isLiked) {
      await unlikePost(post.id)
      post.isLiked = false
      post.likeCount--
    } else {
      await likePost(post.id)
      post.isLiked = true
      post.likeCount++
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped lang="scss">
.forum-page {
  --primary: hsl(215, 70%, 52%);
  --primary-light: hsl(215, 70%, 95%);
  --text-primary: hsl(220, 15%, 20%);
  --text-secondary: hsl(220, 10%, 45%);
  --text-muted: hsl(220, 10%, 60%);
  --bg-card: hsl(0, 0%, 100%);
  --bg-page: hsl(220, 15%, 96%);
  --border: hsl(220, 10%, 88%);
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.06);
  --shadow-md: 0 4px 16px rgba(0, 0, 0, 0.1);
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --transition: 200ms ease-in-out;

  max-width: 900px;
  margin: 0 auto;
  padding: 32px 20px;
  background: var(--bg-page);
  min-height: calc(100vh - 120px);

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 28px;
  }

  &__header-left {
    flex: 1;
  }

  &__title {
    font-size: clamp(1.5rem, 4vw, 2rem);
    font-weight: 700;
    color: var(--text-primary);
    margin: 0 0 8px;
    line-height: 1.3;
  }

  &__subtitle {
    font-size: 14px;
    color: var(--text-secondary);
    margin: 0;
  }

  &__publish-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 20px;
    background: var(--primary);
    color: white;
    border: none;
    border-radius: var(--radius-md);
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: transform var(--transition), box-shadow var(--transition);

    &:hover {
      transform: translateY(-1px);
      box-shadow: var(--shadow-md);
    }

    &:active {
      transform: scale(0.98);
    }

    .icon {
      width: 16px;
      height: 16px;
    }
  }

  &__filter {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
    flex-wrap: wrap;
  }

  &__tabs {
    display: flex;
    gap: 4px;
    background: var(--bg-card);
    padding: 4px;
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-sm);
  }

  &__tab {
    padding: 8px 16px;
    background: transparent;
    border: none;
    border-radius: var(--radius-sm);
    font-size: 14px;
    color: var(--text-secondary);
    cursor: pointer;
    transition: background var(--transition), color var(--transition);

    &:hover {
      color: var(--text-primary);
    }

    &--active {
      background: var(--primary);
      color: white;
    }
  }

  &__actions {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  &__sort-select {
    padding: 8px 12px;
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 14px;
    background: var(--bg-card);
    cursor: pointer;

    &:focus {
      outline: 2px solid var(--primary);
      outline-offset: 1px;
    }
  }

  &__search {
    display: flex;
    align-items: center;
    gap: 0;
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    overflow: hidden;

    &:focus-within {
      border-color: var(--primary);
    }
  }

  &__search-input {
    padding: 8px 12px;
    border: none;
    font-size: 14px;
    background: transparent;
    width: 180px;

    &::placeholder {
      color: var(--text-muted);
    }

    &:focus {
      outline: none;
    }
  }

  &__search-btn {
    padding: 8px 12px;
    background: transparent;
    border: none;
    color: var(--text-secondary);
    cursor: pointer;
    transition: color var(--transition);

    &:hover {
      color: var(--primary);
    }

    .icon {
      width: 16px;
      height: 16px;
    }
  }

  &__content {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  &__empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 20px;
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    text-align: center;
  }

  &__empty-icon {
    width: 64px;
    height: 64px;
    color: var(--text-muted);
    margin-bottom: 16px;
  }

  &__pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 16px;
    padding: 24px 0;
  }

  &__page-btn {
    padding: 8px 20px;
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 14px;
    cursor: pointer;
    transition: background var(--transition), border-color var(--transition);

    &:hover:not(:disabled) {
      border-color: var(--primary);
      color: var(--primary);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  &__page-info {
    font-size: 14px;
    color: var(--text-secondary);
  }

  &__dialog-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    padding: 20px;
  }

  &__dialog {
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    width: 100%;
    max-width: 560px;
    max-height: 90vh;
    overflow: auto;
    box-shadow: var(--shadow-md);
  }

  &__dialog-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 24px;
    border-bottom: 1px solid var(--border);

    h2 {
      font-size: 18px;
      font-weight: 600;
      margin: 0;
      color: var(--text-primary);
    }
  }

  &__dialog-close {
    padding: 4px;
    background: transparent;
    border: none;
    color: var(--text-muted);
    cursor: pointer;
    border-radius: var(--radius-sm);
    transition: background var(--transition), color var(--transition);

    &:hover {
      background: var(--bg-page);
      color: var(--text-primary);
    }

    .icon {
      width: 20px;
      height: 20px;
    }
  }

  &__dialog-body {
    padding: 24px;
  }

  &__dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding: 16px 24px;
    border-top: 1px solid var(--border);
  }

  &__form-item {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  &__label {
    display: block;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
    margin-bottom: 8px;
  }

  &__type-options {
    display: flex;
    gap: 8px;
  }

  &__type-btn {
    padding: 8px 16px;
    background: var(--bg-page);
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 14px;
    cursor: pointer;
    transition: all var(--transition);

    &:hover {
      border-color: var(--primary);
    }

    &--active {
      background: var(--primary-light);
      border-color: var(--primary);
      color: var(--primary);
    }
  }

  &__input,
  &__textarea {
    width: 100%;
    padding: 12px;
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 14px;
    transition: border-color var(--transition);

    &::placeholder {
      color: var(--text-muted);
    }

    &:focus {
      outline: none;
      border-color: var(--primary);
    }
  }

  &__textarea {
    resize: vertical;
    min-height: 120px;
    font-family: inherit;
    line-height: 1.6;
  }

  &__btn {
    padding: 10px 24px;
    border-radius: var(--radius-sm);
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all var(--transition);

    &--primary {
      background: var(--primary);
      color: white;
      border: none;

      &:hover:not(:disabled) {
        background: hsl(215, 70%, 48%);
      }

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }
    }

    &--secondary {
      background: transparent;
      color: var(--text-secondary);
      border: 1px solid var(--border);

      &:hover {
        border-color: var(--text-secondary);
      }
    }
  }
}

.post-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  cursor: pointer;
  transition: box-shadow var(--transition), transform var(--transition);
  box-shadow: var(--shadow-sm);

  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  &__author {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: var(--primary-light);
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    font-weight: 600;
    color: var(--primary);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__author-info {
    display: flex;
    flex-direction: column;
  }

  &__author-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
  }

  &__time {
    font-size: 12px;
    color: var(--text-muted);
  }

  &__type {
    padding: 4px 10px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;

    &--experience {
      background: hsl(145, 60%, 92%);
      color: hsl(145, 60%, 35%);
    }

    &--question {
      background: hsl(45, 90%, 92%);
      color: hsl(45, 90%, 45%);
    }

    &--discussion {
      background: hsl(270, 50%, 92%);
      color: hsl(270, 50%, 45%);
    }
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 12px;
    line-height: 1.4;
  }

  &__content {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.6;
    margin: 0 0 16px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__footer {
    display: flex;
    align-items: center;
    gap: 20px;
    padding-top: 12px;
    border-top: 1px solid var(--border);
  }

  &__stat {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--text-muted);

    .icon {
      width: 16px;
      height: 16px;
    }
  }

  &__like {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 0;
    background: transparent;
    border: none;
    font-size: 13px;
    color: var(--text-muted);
    cursor: pointer;
    transition: color var(--transition);

    .icon {
      width: 16px;
      height: 16px;
    }

    &:hover {
      color: hsl(0, 70%, 55%);
    }

    &--active {
      color: hsl(0, 70%, 55%);
    }
  }
}

.icon {
  flex-shrink: 0;
}
</style>
