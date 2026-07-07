<template>
  <div class="detail-page">
    <header class="detail-page__header">
      <button class="detail-page__back" @click="$router.back()">
        <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        返回
      </button>
      <h1 class="detail-page__title">帖子详情</h1>
    </header>

    <article class="detail-page__post" v-if="post">
      <header class="post-header">
        <div class="post-header__author">
          <div class="post-header__avatar">
            <img v-if="post.authorAvatar" :src="post.authorAvatar" :alt="post.authorName" />
            <span v-else>{{ post.authorName?.[0] }}</span>
          </div>
          <div class="post-header__info">
            <span class="post-header__name">{{ post.authorName || '匿名用户' }}</span>
            <span class="post-header__time">{{ formatTime(post.createTime) }}</span>
          </div>
        </div>
        <span class="post-header__type" :class="`post-header__type--${post.postType}`">
          {{ getTypeName(post.postType) }}
        </span>
      </header>

      <h2 class="post-title">{{ post.title }}</h2>

      <div class="post-body" v-html="post.content"></div>

      <div v-if="postImages.length > 0" class="post-gallery">
        <a
          v-for="(imageUrl, imageIndex) in postImages"
          :key="`${imageUrl}-${imageIndex}`"
          :href="imageUrl"
          target="_blank"
          rel="noopener"
          class="post-gallery__item"
        >
          <img :src="imageUrl" :alt="`${post.title} 图片 ${imageIndex + 1}`" />
        </a>
      </div>

      <footer class="post-footer">
        <button
          class="post-footer__action"
          :class="{ 'post-footer__action--active': post.isLiked }"
          @click="toggleLike"
        >
          <svg class="icon" viewBox="0 0 24 24" :fill="post.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
          </svg>
          {{ post.likeCount }} 点赞
        </button>
        <span class="post-footer__stat">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
          {{ post.viewCount }} 浏览
        </span>
        <button class="post-footer__action" @click="focusCommentInput">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          {{ post.commentCount }} 评论
        </button>
      </footer>
    </article>

    <section class="comments-section">
      <h3 class="comments-section__title">评论 ({{ commentCount }})</h3>

      <div class="comment-input" ref="commentInputRef">
        <textarea
          class="comment-input__textarea"
          v-model="commentText"
          placeholder="写下你的评论..."
          rows="3"
        ></textarea>
        <div class="comment-input__actions">
          <button
            class="comment-input__submit"
            :disabled="!commentText.trim() || submitting"
            @click="submitComment"
          >
            {{ submitting ? '发布中...' : '发表评论' }}
          </button>
        </div>
      </div>

      <div class="comment-list" v-loading="loadingComments">
        <div v-if="!loadingComments && flatComments.length === 0" class="comment-list__empty">
          暂无评论，来抢沙发吧
        </div>

        <div
          v-for="comment in flatComments"
          :key="comment.id"
          class="comment-card"
        >
          <div class="comment-item">
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
              <button
                class="comment-item__action"
                :class="{ 'comment-item__action--active': comment.isLiked }"
                @click="toggleCommentLike(comment)"
              >
                <svg class="icon" viewBox="0 0 24 24" :fill="comment.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                  <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/>
                </svg>
                {{ comment.likeCount }}
              </button>
              <button class="comment-item__action" @click="openReply(comment)">回复</button>
              <button
                v-if="canDeleteComment(comment)"
                class="comment-item__action comment-item__action--danger"
                @click="deleteComment(comment.id)"
              >删除</button>
            </div>

            <div v-if="replyingTo === comment.id" class="reply-input">
              <textarea
                class="reply-input__textarea"
                v-model="replyText"
                :placeholder="`回复 @${comment.authorName}...`"
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
                class="comment-card comment-card--child"
              >
              <div class="comment-item comment-item--child">
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
                      class="comment-item__action"
                      :class="{ 'comment-item__action--active': child.isLiked }"
                      @click="toggleCommentLike(child)"
                    >
                      <svg class="icon" viewBox="0 0 24 24" :fill="child.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                        <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/>
                      </svg>
                      {{ child.likeCount }}
                    </button>
                    <button
                      v-if="canDeleteComment(child)"
                      class="comment-item__action comment-item__action--danger"
                      @click="deleteComment(child.id)"
                    >删除</button>
                  </div>
                </div>
              </div>
              </div>
              </template>
            </div>
          </div>
        </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPostDetail, likePost, unlikePost, getPostComments, createComment, deleteComment as deleteCommentApi, likeComment, unlikeComment } from '@/api/forum'
import type { PostVO, CommentVO } from '@/api/forum'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const postId = computed(() => Number(route.params.id))
const currentUserId = computed(() => userStore.userInfo?.id)
const isAdmin = computed(() => userStore.isAdmin)

const post = ref<PostVO | null>(null)
const comments = ref<CommentVO[]>([])
const loadingComments = ref(false)
const commentText = ref('')
const replyText = ref('')
const replyingTo = ref<number | null>(null)
const submitting = ref(false)
const commentInputRef = ref<HTMLElement | null>(null)
const expandedReplies = ref<Set<number>>(new Set())
const postImages = computed(() => (post.value?.imageUrls || []).filter(Boolean))

const expandReplies = (commentId: number) => {
  expandedReplies.value = new Set([...expandedReplies.value, commentId])
}

const collapseReplies = (commentId: number) => {
  const next = new Set(expandedReplies.value)
  next.delete(commentId)
  expandedReplies.value = next
}

const flatComments = computed(() => {
  return comments.value.filter(c => c.parentId === 0 || c.parentId === null)
})

const countCommentTree = (list: CommentVO[]): number => {
  return list.reduce((total, comment) => total + 1 + countCommentTree(comment.children || []), 0)
}

const syncComments = (nextComments: CommentVO[]) => {
  comments.value = nextComments
  if (post.value) {
    post.value.commentCount = countCommentTree(nextComments)
  }
}

const commentCount = computed(() => countCommentTree(comments.value))

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

const getErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

const loadPost = async () => {
  try {
    const res = await getPostDetail(postId.value)
    post.value = res
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '加载帖子失败'))
    router.back()
  }
}

const loadComments = async () => {
  loadingComments.value = true
  try {
    const res = await getPostComments(postId.value)
    syncComments(res)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '加载评论失败'))
  } finally {
    loadingComments.value = false
  }
}

const toggleLike = async () => {
  if (!post.value) return
  try {
    if (post.value.isLiked) {
      await unlikePost(postId.value)
      post.value.isLiked = false
      post.value.likeCount = Math.max(0, post.value.likeCount - 1)
    } else {
      await likePost(postId.value)
      post.value.isLiked = true
      post.value.likeCount++
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '操作失败'))
  }
}

const toggleCommentLike = async (comment: CommentVO) => {
  try {
    if (comment.isLiked) {
      await unlikeComment(comment.id)
      comment.isLiked = false
      comment.likeCount = Math.max(0, comment.likeCount - 1)
    } else {
      await likeComment(comment.id)
      comment.isLiked = true
      comment.likeCount++
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '操作失败'))
  }
}

const focusCommentInput = () => {
  commentInputRef.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  const textarea = commentInputRef.value?.querySelector('textarea')
  textarea?.focus()
}

const submitComment = async () => {
  if (!commentText.value.trim()) return
  submitting.value = true
  try {
    await createComment({
      postId: postId.value,
      parentId: 0,
      content: commentText.value.trim()
    })
    ElMessage.success('评论成功')
    commentText.value = ''
    await loadComments()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '评论失败'))
  } finally {
    submitting.value = false
  }
}

const openReply = (comment: CommentVO) => {
  replyingTo.value = comment.id
  replyText.value = ''
}

const cancelReply = () => {
  replyingTo.value = null
  replyText.value = ''
}

const canDeleteComment = (comment: CommentVO) => {
  return comment.userId === currentUserId.value || isAdmin.value
}

const submitReply = async (parentId: number) => {
  if (!replyText.value.trim()) return
  submitting.value = true
  try {
    await createComment({
      postId: postId.value,
      parentId,
      content: replyText.value.trim()
    })
    ElMessage.success('回复成功')
    cancelReply()
    expandReplies(parentId)
    await loadComments()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '回复失败'))
  } finally {
    submitting.value = false
  }
}

const deleteComment = async (commentId: number) => {
  try {
    await deleteCommentApi(commentId)
    ElMessage.success('删除成功')
    await loadComments()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '删除失败'))
  }
}

onMounted(() => {
  loadPost()
  loadComments()
})
</script>

<style scoped lang="scss">
.detail-page {
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
  padding: 24px 20px;

  &__header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 24px;
  }

  &__back {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 12px;
    background: var(--bg-card);
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 14px;
    color: var(--text-secondary);
    cursor: pointer;
    transition: all var(--transition);

    &:hover {
      border-color: var(--primary);
      color: var(--primary);
    }

    .icon {
      width: 16px;
      height: 16px;
    }
  }

  &__title {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }

  &__post {
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    padding: 32px;
    box-shadow: var(--shadow-sm);
    margin-bottom: 24px;
  }
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);

  &__author {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: var(--primary-light);
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
    font-size: 18px;
    font-weight: 600;
    color: var(--primary);

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__info {
    display: flex;
    flex-direction: column;
  }

  &__name {
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
  }

  &__time {
    font-size: 13px;
    color: var(--text-muted);
  }

  &__type {
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 13px;
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
}

.post-title {
  font-size: clamp(1.25rem, 3vw, 1.5rem);
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 20px;
  line-height: 1.4;
}

.post-body {
  font-size: 15px;
  color: var(--text-secondary);
  line-height: 1.8;
  margin-bottom: 28px;

  :deep(h1),
  :deep(h2),
  :deep(h3) {
    font-weight: 600;
    color: var(--text-primary);
    margin: 18px 0 8px;
  }

  :deep(h3) {
    font-size: 17px;
    padding-bottom: 6px;
    border-bottom: 1px solid hsl(220, 14%, 90%);
  }

  :deep(h4) {
    font-size: 15px;
    font-weight: 600;
    color: #409eff;
    margin: 14px 0 6px;
  }

  :deep(strong) {
    font-weight: 600;
  }

  :deep(p) {
    margin-bottom: 12px;
    line-height: 1.8;
  }

  :deep(ul),
  :deep(ol) {
    margin: 10px 0 10px 20px;
    padding-left: 4px;
  }

  :deep(li) {
    margin-bottom: 6px;
  }

  :deep(code) {
    background: hsl(220, 20%, 97%);
    color: #409eff;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 14px;
    font-family: 'Monaco', 'Menlo', monospace;
  }

  :deep(blockquote) {
    margin: 12px 0;
    padding: 8px 16px;
    border-left: 3px solid #409eff;
    background: hsl(220, 20%, 98%);
    color: var(--text-secondary);
    font-style: italic;
  }

  :deep(a) {
    color: #409eff;
    text-decoration: underline;
  }
}

.post-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
  margin-bottom: 28px;

  &__item {
    aspect-ratio: 4 / 3;
    border-radius: var(--radius-sm);
    overflow: hidden;
    border: 1px solid var(--border);
    background: var(--bg-page);
    display: block;

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
}

.post-footer {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--border);

  &__action {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    background: var(--bg-page);
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 14px;
    color: var(--text-secondary);
    cursor: pointer;
    transition: all var(--transition);

    .icon {
      width: 16px;
      height: 16px;
    }

    &:hover {
      border-color: var(--primary);
      color: var(--primary);
    }

    &--active {
      background: hsl(0, 70%, 95%);
      border-color: hsl(0, 70%, 70%);
      color: hsl(0, 70%, 50%);
    }
  }

  &__stat {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: var(--text-muted);

    .icon {
      width: 16px;
      height: 16px;
    }
  }
}

.comments-section {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-sm);

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 20px;
  }
}

.comment-input {
  margin-bottom: 28px;

  &__textarea {
    width: 100%;
    padding: 12px;
    border: 1px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 14px;
    font-family: inherit;
    line-height: 1.6;
    resize: vertical;
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
    margin-top: 12px;
  }

  &__submit {
    padding: 8px 24px;
    background: var(--primary);
    color: white;
    border: none;
    border-radius: var(--radius-sm);
    font-size: 14px;
    cursor: pointer;
    transition: all var(--transition);

    &:hover:not(:disabled) {
      background: hsl(215, 70%, 48%);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  background: var(--bg-page);
  border-radius: var(--radius-md);

  &__empty {
    text-align: center;
    padding: 40px 20px;
    color: var(--text-muted);
    font-size: 14px;
  }
}

.comment-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 16px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border);

  &--child {
    margin-left: 0;
    padding: 12px 16px;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  }
}

.comment-item {
  display: flex;
  gap: 12px;

  &--child {
    margin-left: 0;
    padding: 0;
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
    margin-bottom: 8px;
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
    gap: 16px;
  }

  &__action {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 2px 0;
    background: transparent;
    border: none;
    font-size: 13px;
    color: var(--text-muted);
    cursor: pointer;
    transition: color var(--transition);

    .icon {
      width: 14px;
      height: 14px;
    }

    &:hover {
      color: var(--primary);
    }

    &--active {
      color: hsl(0, 70%, 50%);
    }

    &--danger {
      &:hover {
        color: hsl(0, 70%, 50%);
      }
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
    background: var(--bg-page);
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

.icon {
  flex-shrink: 0;
}
</style>
