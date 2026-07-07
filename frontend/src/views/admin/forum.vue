<template>
  <div class="admin-forum">
    <header class="admin-forum__header">
      <h2 class="admin-forum__title">帖子管理</h2>
    </header>

    <div class="admin-forum__toolbar">
      <div class="admin-forum__filters">
        <input
          type="text"
          class="admin-forum__search-input"
          v-model="keyword"
          placeholder="搜索帖子标题/内容..."
          @keyup.enter="loadPosts"
        />
        <select class="admin-forum__select" v-model="typeFilter">
          <option value="">全部类型</option>
          <option value="experience">经验分享</option>
          <option value="question">求助问答</option>
          <option value="discussion">讨论交流</option>
        </select>
        <button class="admin-forum__btn admin-forum__btn--primary" @click="loadPosts">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="m21 21-4.35-4.35"/>
          </svg>
          搜索
        </button>
      </div>
    </div>

    <div class="admin-forum__table-wrapper" v-loading="loading">
      <table class="admin-forum__table">
        <thead>
          <tr>
            <th>ID</th>
            <th>标题</th>
            <th>类型</th>
            <th>作者</th>
            <th>浏览量</th>
            <th>评论数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="postList.length === 0 && !loading">
            <td colspan="7" class="admin-forum__empty">暂无数据</td>
          </tr>
          <tr v-for="post in postList" :key="post.id" @click="openPostDetail(post)" class="admin-forum__row">
            <td>{{ post.id }}</td>
            <td class="admin-forum__title-cell">{{ post.title }}</td>
            <td>
              <span class="admin-forum__tag" :class="`admin-forum__tag--${post.postType}`">
                {{ getTypeName(post.postType) }}
              </span>
            </td>
            <td>{{ post.authorName }}</td>
            <td>{{ post.viewCount }}</td>
            <td>{{ post.commentCount }}</td>
            <td @click.stop>
              <button
                class="admin-forum__action admin-forum__action--primary"
                @click="openPostDetail(post)"
              >
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
                查看
              </button>
              <button
                class="admin-forum__action admin-forum__action--danger"
                @click="handleDelete(post)"
              >
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                </svg>
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="admin-forum__pagination" v-if="total > 0">
        <button
          class="admin-forum__page-btn"
          :disabled="pageNum <= 1"
          @click="changePage(pageNum - 1)"
        >
          上一页
        </button>
        <span class="admin-forum__page-info">{{ pageNum }} / {{ totalPages }}</span>
        <button
          class="admin-forum__page-btn"
          :disabled="pageNum >= totalPages"
          @click="changePage(pageNum + 1)"
        >
          下一页
        </button>
      </div>
    </div>

    <!-- 帖子详情对话框 -->
    <div class="admin-forum__modal-overlay" v-if="showDetailModal" @click.self="closeDetailModal">
      <div class="admin-forum__modal">
        <header class="modal-header">
          <h3>帖子详情</h3>
          <button class="modal-header__close" @click="closeDetailModal">
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 6 6 18M6 6l12 12"/>
            </svg>
          </button>
        </header>

        <div class="modal-body" v-if="selectedPost">
          <!-- 帖子卡片 -->
          <article class="post-card">
            <header class="post-card__header">
              <div class="post-card__author">
                <div class="post-card__avatar">
                  <img v-if="selectedPost.authorAvatar" :src="selectedPost.authorAvatar" :alt="selectedPost.authorName" />
                  <span v-else>{{ selectedPost.authorName?.[0] }}</span>
                </div>
                <div class="post-card__author-info">
                  <span class="post-card__author-name">{{ selectedPost.authorName || '匿名用户' }}</span>
                  <span class="post-card__time">{{ formatTime(selectedPost.createTime) }}</span>
                </div>
              </div>
              <span class="post-card__type" :class="`post-card__type--${selectedPost.postType}`">
                {{ getTypeName(selectedPost.postType) }}
              </span>
            </header>

            <h2 class="post-card__title">{{ selectedPost.title }}</h2>
            <div class="post-card__content" v-html="selectedPost.content"></div>

            <div v-if="selectedPostImages.length > 0" class="post-card__gallery">
              <a
                v-for="(imageUrl, imageIndex) in selectedPostImages"
                :key="`${imageUrl}-${imageIndex}`"
                :href="imageUrl"
                target="_blank"
                rel="noopener"
                class="post-card__gallery-item"
              >
                <img :src="imageUrl" :alt="`${selectedPost.title} 图片 ${imageIndex + 1}`" />
              </a>
            </div>

            <footer class="post-card__footer">
              <span class="post-card__stat">
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
                {{ selectedPost.viewCount }} 浏览
              </span>
              <span class="post-card__stat">
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
                {{ selectedPost.commentCount }} 评论
              </span>
            </footer>
          </article>

          <!-- 评论管理区域 -->
          <section class="comments-management">
            <h4 class="comments-management__title">
              评论管理
              <span class="comments-management__count">({{ comments.length }})</span>
            </h4>

            <div class="comments-management__list" v-loading="loadingComments">
              <div v-if="!loadingComments && comments.length === 0" class="comments-management__empty">
                暂无评论
              </div>

              <div
                v-for="comment in comments"
                :key="comment.id"
                class="comment-item"
              >
                <div class="comment-item__avatar">
                  <img v-if="comment.authorAvatar" :src="comment.authorAvatar" :alt="comment.authorName" />
                  <span v-else>{{ comment.authorName?.[0] }}</span>
                </div>
                <div class="comment-item__content">
                  <header class="comment-item__header">
                    <span class="comment-item__name">
                      {{ comment.authorName || '匿名用户' }}
                      <span v-if="comment.isAuthor" class="comment-item__author-tag">作者</span>
                    </span>
                    <span class="comment-item__time">{{ formatTime(comment.createTime) }}</span>
                  </header>
                  <p class="comment-item__text" v-html="comment.content"></p>
                  <div class="comment-item__actions">
                    <button class="comment-item__reply" @click="openReply(comment)">
                      <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                      </svg>
                      回复
                    </button>
                    <button
                      class="comment-item__action comment-item__action--danger"
                      @click="handleDeleteComment(comment)"
                    >
                      <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                      </svg>
                      删除
                    </button>
                  </div>

                  <!-- 回复输入框 -->
                  <div v-if="replyingTo === comment.id" class="reply-input">
                    <textarea
                      v-model="replyText"
                      :placeholder="`回复 @${comment.authorName}...`"
                      class="reply-input__textarea"
                      rows="2"
                    ></textarea>
                    <div class="reply-input__actions">
                      <button class="reply-input__cancel" @click="cancelReply">取消</button>
                      <button
                        class="reply-input__submit"
                        :disabled="!replyText.trim() || submitting"
                        @click="submitReply(comment.id)"
                      >
                        {{ submitting ? '发送中...' : '发送' }}
                      </button>
                    </div>
                  </div>

                  <!-- 子评论列表 -->
                  <div v-if="comment.children?.length" class="comment-children">
                    <button
                      v-if="!expandedReplies.has(comment.id)"
                      class="comment-children__toggle"
                      @click="expandReplies(comment.id)"
                    >
                      展开 {{ comment.children.length }} 条回复
                    </button>
                    <template v-else>
                      <button
                        class="comment-children__toggle comment-children__toggle--collapse"
                        @click="collapseReplies(comment.id)"
                      >
                        收起回复
                      </button>
                      <div
                        v-for="child in comment.children"
                        :key="child.id"
                        class="comment-item comment-item--child"
                      >
                        <div class="comment-item__avatar comment-item__avatar--small">
                          <img v-if="child.authorAvatar" :src="child.authorAvatar" :alt="child.authorName" />
                          <span v-else>{{ child.authorName?.[0] }}</span>
                        </div>
                        <div class="comment-item__content">
                          <header class="comment-item__header">
                            <span class="comment-item__name">
                              {{ child.authorName || '匿名用户' }}
                              <span v-if="child.isAuthor" class="comment-item__author-tag">作者</span>
                            </span>
                            <span class="comment-item__time">{{ formatTime(child.createTime) }}</span>
                          </header>
                          <p class="comment-item__text" v-html="child.content"></p>
                          <div class="comment-item__actions">
                            <button
                              class="comment-item__action comment-item__action--danger"
                              @click="handleDeleteComment(child)"
                            >
                              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                              </svg>
                              删除
                            </button>
                          </div>
                        </div>
                      </div>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>

        <footer class="modal-footer">
          <button class="modal-footer__btn modal-footer__btn--secondary" @click="closeDetailModal">
            关闭
          </button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPostPage,
  getPostDetail,
  deletePost,
  getPostComments,
  createComment,
  deleteComment as deleteCommentApi
} from '@/api/forum'
import type { PostVO, CommentVO } from '@/api/forum'

const loading = ref(false)
const keyword = ref('')
const typeFilter = ref('')
const postList = ref<PostVO[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 详情弹窗相关
const showDetailModal = ref(false)
const selectedPost = ref<PostVO | null>(null)
const comments = ref<CommentVO[]>([])
const loadingComments = ref(false)
const replyingTo = ref<number | null>(null)
const replyText = ref('')
const submitting = ref(false)
const expandedReplies = ref<Set<number>>(new Set())

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
const selectedPostImages = computed(() => (selectedPost.value?.imageUrls || []).filter(Boolean))

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
      keyword: keyword.value || undefined,
      postType: typeFilter.value || undefined
    })
    postList.value = res.records
    total.value = res.total
  } catch {
    ElMessage.error('加载帖子失败')
  } finally {
    loading.value = false
  }
}

const changePage = (page: number) => {
  pageNum.value = page
  loadPosts()
}

const handleDelete = async (post: PostVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除帖子「${post.title}」吗？\n\n删除后将同时删除该帖子的所有评论和回复，且此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await deletePost(post.id)
    ElMessage.success('删除成功')
    loadPosts()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

const openPostDetail = async (post: PostVO) => {
  const postId = post.id
  selectedPost.value = post
  showDetailModal.value = true
  loadComments()
  try {
    const detail = await getPostDetail(postId)
    if (selectedPost.value?.id === postId) {
      selectedPost.value = detail
    }
  } catch {
    ElMessage.error('加载帖子详情失败')
  }
}

const closeDetailModal = () => {
  showDetailModal.value = false
  selectedPost.value = null
  comments.value = []
  replyingTo.value = null
  replyText.value = ''
  expandedReplies.value = new Set()
}

const loadComments = async () => {
  if (!selectedPost.value) return
  loadingComments.value = true
  try {
    const res = await getPostComments(selectedPost.value.id)
    comments.value = res
  } catch {
    ElMessage.error('加载评论失败')
  } finally {
    loadingComments.value = false
  }
}

const expandReplies = (commentId: number) => {
  expandedReplies.value = new Set([...expandedReplies.value, commentId])
}

const collapseReplies = (commentId: number) => {
  const next = new Set(expandedReplies.value)
  next.delete(commentId)
  expandedReplies.value = next
}

const openReply = (comment: CommentVO) => {
  replyingTo.value = comment.id
  replyText.value = ''
}

const cancelReply = () => {
  replyingTo.value = null
  replyText.value = ''
}

const submitReply = async (parentId: number) => {
  if (!replyText.value.trim() || !selectedPost.value) return
  submitting.value = true
  try {
    await createComment({
      postId: selectedPost.value.id,
      parentId,
      content: replyText.value.trim()
    })
    ElMessage.success('回复成功')
    cancelReply()
    expandReplies(parentId)
    loadComments()
    if (selectedPost.value) selectedPost.value.commentCount++
  } catch {
    ElMessage.error('回复失败')
  } finally {
    submitting.value = false
  }
}

const handleDeleteComment = async (comment: CommentVO) => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCommentApi(comment.id)
    ElMessage.success('删除成功')
    loadComments()
    if (selectedPost.value) selectedPost.value.commentCount--
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadPosts()
})
</script>

<style scoped lang="scss">
.admin-forum {
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

  padding: 24px;

  &__header {
    margin-bottom: 24px;
  }

  &__title {
    font-size: 20px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }

  &__toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 12px;
  }

  &__filters {
    display: flex;
    gap: 12px;
    align-items: center;
    flex-wrap: wrap;
  }

  &__search-input {
    padding: 10px 14px;
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 14px;
    width: 240px;
    background: var(--bg-card);
    transition: border-color var(--transition);

    &::placeholder {
      color: var(--text-muted);
    }

    &:focus {
      outline: none;
      border-color: var(--primary);
    }
  }

  &__select {
    padding: 10px 14px;
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 14px;
    background: var(--bg-card);
    cursor: pointer;

    &:focus {
      outline: none;
      border-color: var(--primary);
    }
  }

  &__btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 18px;
    border-radius: var(--radius-sm);
    font-size: 14px;
    cursor: pointer;
    transition: all var(--transition);

    .icon {
      width: 16px;
      height: 16px;
    }

    &--primary {
      background: var(--primary);
      color: white;
      border: none;

      &:hover {
        background: hsl(215, 70%, 48%);
      }

      &:active {
        transform: scale(0.98);
      }
    }
  }

  &__table-wrapper {
    background: var(--bg-card);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-sm);
    overflow: hidden;
  }

  &__table {
    width: 100%;
    border-collapse: collapse;
    font-size: 14px;

    th, td {
      padding: 14px 16px;
      text-align: left;
      border-bottom: 1px solid var(--border);
    }

    th {
      background: var(--bg-page);
      font-weight: 500;
      color: var(--text-secondary);
      font-size: 13px;
    }

    td {
      color: var(--text-primary);
    }

    tr:last-child td {
      border-bottom: none;
    }
  }

  &__row {
    cursor: pointer;
    transition: background var(--transition);

    &:hover td {
      background: var(--bg-page);
    }
  }

  &__title-cell {
    max-width: 250px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__tag {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;

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

  &__badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;

    .icon {
      width: 12px;
      height: 12px;
    }
  }

  &__action {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 12px;
    border-radius: var(--radius-sm);
    font-size: 13px;
    cursor: pointer;
    transition: all var(--transition);

    .icon {
      width: 14px;
      height: 14px;
    }

    &--primary {
      background: transparent;
      border: 1px solid var(--primary);
      color: var(--primary);

      &:hover {
        background: var(--primary-light);
      }
    }

    &--danger {
      background: transparent;
      border: 1px solid hsl(0, 70%, 70%);
      color: hsl(0, 70%, 50%);

      &:hover {
        background: hsl(0, 70%, 95%);
      }
    }
  }

  &__empty {
    text-align: center;
    padding: 40px !important;
    color: var(--text-muted);
  }

  &__pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 16px;
    padding: 16px;
    border-top: 1px solid var(--border);
  }

  &__page-btn {
    padding: 8px 16px;
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 14px;
    cursor: pointer;
    transition: all var(--transition);

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

  // 弹窗样式
  &__modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    padding: 20px;
  }

  &__modal {
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    width: 100%;
    max-width: 720px;
    max-height: 90vh;
    display: flex;
    flex-direction: column;
    box-shadow: var(--shadow-md);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);

  h3 {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }

  &__close {
    padding: 4px;
    background: transparent;
    border: none;
    color: var(--text-muted);
    cursor: pointer;
    border-radius: var(--radius-sm);
    transition: all var(--transition);

    &:hover {
      background: var(--bg-page);
      color: var(--text-primary);
    }

    .icon {
      width: 20px;
      height: 20px;
    }
  }
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 24px;
  border-top: 1px solid var(--border);

  &__btn {
    padding: 10px 24px;
    border-radius: var(--radius-sm);
    font-size: 14px;
    cursor: pointer;
    transition: all var(--transition);

    &--secondary {
      background: var(--bg-page);
      border: 1px solid var(--border);
      color: var(--text-secondary);

      &:hover {
        border-color: var(--text-secondary);
      }
    }
  }
}

// 帖子卡片样式
.post-card {
  background: var(--bg-page);
  border-radius: var(--radius-md);
  padding: 24px;
  margin-bottom: 24px;
  border: 1px solid var(--border);

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
    width: 44px;
    height: 44px;
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
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 12px;
    line-height: 1.4;
  }

  &__content {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.7;
    margin-bottom: 16px;

    :deep(h3) {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
      margin: 16px 0 6px;
      padding-bottom: 4px;
      border-bottom: 1px solid hsl(220, 14%, 90%);
    }

    :deep(h4) {
      font-size: 14px;
      font-weight: 600;
      color: #409eff;
      margin: 12px 0 4px;
    }

    :deep(strong) {
      font-weight: 600;
    }

    :deep(p) {
      margin-bottom: 10px;
      line-height: 1.7;
    }

    :deep(ul),
    :deep(ol) {
      margin: 8px 0 8px 16px;
      padding-left: 4px;
    }

    :deep(li) {
      margin-bottom: 4px;
    }

    :deep(code) {
      background: hsl(220, 20%, 97%);
      color: #409eff;
      padding: 1px 4px;
      border-radius: 3px;
      font-size: 13px;
      font-family: 'Monaco', 'Menlo', monospace;
    }

    :deep(a) {
      color: #409eff;
      text-decoration: underline;
    }
  }

  &__gallery {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 10px;
    margin-bottom: 16px;
  }

  &__gallery-item {
    aspect-ratio: 4 / 3;
    display: block;
    border-radius: var(--radius-sm);
    overflow: hidden;
    border: 1px solid var(--border);
    background: var(--bg-card);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
      transition: transform var(--transition);
    }

    &:hover img {
      transform: scale(1.03);
    }
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
}

// 评论管理区域
.comments-management {
  &__title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 16px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__count {
    font-weight: 400;
    color: var(--text-muted);
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  &__empty {
    text-align: center;
    padding: 32px;
    color: var(--text-muted);
    background: var(--bg-page);
    border-radius: var(--radius-md);
  }
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: var(--bg-page);
  border-radius: var(--radius-md);
  border: 1px solid var(--border);

  &--child {
    margin-left: 48px;
    padding: 12px 16px;
    background: var(--bg-card);
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
    font-size: 14px;
    font-weight: 500;
    color: var(--primary);
    flex-shrink: 0;

    &--small {
      width: 32px;
      height: 32px;
      font-size: 12px;
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
  }

  &__name {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__author-tag {
    padding: 2px 6px;
    background: var(--primary);
    color: white;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 400;
  }

  &__time {
    font-size: 12px;
    color: var(--text-muted);
  }

  &__text {
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.6;
    margin: 0 0 10px;
    word-break: break-word;

    :deep(a) {
      color: #409eff;
      text-decoration: underline;
    }

    :deep(strong), :deep(b) {
      font-weight: 600;
    }

    :deep(em), :deep(i) {
      font-style: italic;
    }

    :deep(u) {
      text-decoration: underline;
    }

    :deep(s) {
      text-decoration: line-through;
    }
  }

  &__actions {
    display: flex;
    gap: 12px;
  }

  &__reply,
  &__action {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    background: transparent;
    border: none;
    font-size: 13px;
    color: var(--text-muted);
    cursor: pointer;
    border-radius: var(--radius-sm);
    transition: all var(--transition);

    .icon {
      width: 14px;
      height: 14px;
    }

    &:hover {
      background: var(--bg-card);
      color: var(--primary);
    }

    &--danger {
      &:hover {
        color: hsl(0, 70%, 50%);
      }
    }
  }
}

.reply-input {
  margin-top: 12px;

  &__textarea {
    width: 100%;
    padding: 10px;
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 13px;
    font-family: inherit;
    line-height: 1.5;
    resize: vertical;
    background: var(--bg-card);
    transition: border-color var(--transition);

    &::placeholder {
      color: var(--text-muted);
    }

    &:focus {
      outline: none;
      border-color: var(--primary);
    }
  }

  &__actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 8px;
  }

  &__cancel,
  &__submit {
    padding: 6px 14px;
    border-radius: var(--radius-sm);
    font-size: 13px;
    cursor: pointer;
    transition: all var(--transition);
  }

  &__cancel {
    background: transparent;
    border: 1px solid var(--border);
    color: var(--text-secondary);

    &:hover {
      border-color: var(--text-secondary);
    }
  }

  &__submit {
    background: var(--primary);
    border: none;
    color: white;

    &:hover:not(:disabled) {
      background: hsl(215, 70%, 48%);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

.comment-children {
  margin-top: 16px;
  padding-left: 16px;
  border-left: 2px solid var(--border);
  display: flex;
  flex-direction: column;
  gap: 12px;

  &__toggle {
    align-self: flex-start;
    padding: 6px 12px;
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 13px;
    color: var(--primary);
    cursor: pointer;
    transition: all var(--transition);

    &:hover {
      border-color: var(--primary);
      background: var(--primary-light);
    }

    &--collapse {
      color: var(--text-muted);

      &:hover {
        color: var(--primary);
      }
    }
  }
}

.icon {
  flex-shrink: 0;
}
</style>
