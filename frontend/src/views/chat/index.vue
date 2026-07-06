<template>
  <div class="page-container">
    <div class="chat-container">
      <!-- 侧边栏：会话列表 -->
      <div class="chat-sidebar" :class="{ 'is-collapsed': sidebarCollapsed }">
        <div class="sidebar-header">
          <h3>历史会话</h3>
          <el-button :icon="Plus" text @click="startNewSession">
            新会话
          </el-button>
        </div>
        <div class="session-list">
          <div
            v-for="session in sessionList"
            :key="session.sessionId"
            :class="['session-item', { 'is-active': sessionId === session.sessionId }]"
            @click="switchSession(session.sessionId)"
          >
            <div class="session-content">
              <span class="session-title">{{ session.firstQuestion }}</span>
              <span class="session-time">{{ formatTime(session.updateTime) }}</span>
            </div>
            <el-button
              :icon="Delete"
              text
              class="session-delete"
              @click.stop="handleDeleteSession(session.sessionId)"
            />
          </div>
          <el-empty v-if="sessionList.length === 0" description="暂无会话记录" />
        </div>
      </div>

      <!-- 主聊天区域 -->
      <div class="chat-main">
        <div class="chat-header">
          <el-button
            :icon="Menu"
            text
            class="toggle-sidebar"
            @click="sidebarCollapsed = !sidebarCollapsed"
          />
          <span class="chat-icon"><IconBot :size="36" /></span>
          <div class="chat-info">
            <h3>智能反诈助手</h3>
            <p>{{ isLoading ? '思考中...' : '按问题需要自动检索最新反诈信息' }}</p>
          </div>
          <div class="header-actions">
            <el-button :icon="Refresh" text @click="clearConversation">
              清空对话
            </el-button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="chat-messages" ref="messagesRef" @scroll="handleMessagesScroll">
          <div v-if="messages.length === 0" class="welcome-tip">
            <div class="welcome-icon"><IconBot :size="64" /></div>
            <h3>您好，我是反诈智能助手</h3>
            <p>我可以帮您解答以下问题：</p>
            <ul>
              <li>识别各类诈骗手法</li>
              <li>分析疑似诈骗信息</li>
              <li>提供防范建议</li>
              <li>科普反诈知识</li>
            </ul>
            <div class="quick-questions">
              <span
                v-for="q in quickQuestions"
                :key="q"
                class="quick-question"
                @click="sendQuickQuestion(q)"
              >
                {{ q }}
              </span>
            </div>
          </div>

          <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
            <div class="message-avatar">
              <component :is="msg.role === 'user' ? IconUser : IconBot" />
            </div>
            <div class="message-content">
              <div
                v-if="msg.role === 'user'"
                class="message-bubble message-bubble--plain"
              >{{ msg.content }}</div>
              <template v-else>
                <div
                  v-if="msg.reasoning"
                  class="reasoning-panel"
                >
                  <div class="reasoning-panel__title">分析思路</div>
                  <div class="reasoning-panel__body" v-html="formatContent(msg.reasoning)"></div>
                </div>
                <div
                  class="message-bubble"
                  v-html="formatContent(msg.content || (msg.streaming ? '正在生成...' : ''))"
                ></div>
              </template>
              <div v-if="shouldShowMeta(msg)" class="message-meta">
                <span
                  v-if="msg.riskLevel"
                  class="meta-tag"
                  :class="`meta-tag--risk-${getRiskClass(msg.riskLevel)}`"
                >
                  {{ getRiskLabel(msg.riskLevel) }}
                </span>
                <span v-if="msg.retrievedAt" class="meta-tag meta-tag--time">
                  检索 {{ formatDateTime(msg.retrievedAt) }}
                </span>
                <span v-if="msg.searchProvider" class="meta-tag meta-tag--provider">
                  {{ msg.searchProvider }}
                </span>
                <span
                  v-if="msg.fallback !== undefined"
                  class="meta-tag"
                  :class="msg.fallback ? 'meta-tag--fallback' : 'meta-tag--normal'"
                >
                  {{ msg.fallback ? '降级回答' : '服务正常' }}
                </span>
                <span v-if="msg.fallbackReason" class="meta-tag meta-tag--reason">
                  {{ msg.fallbackReason }}
                </span>
              </div>
              <div
                v-if="msg.role === 'assistant' && msg.sources?.length"
                class="source-links"
              >
                <span class="source-links__label">来源：</span>
                <template v-for="(source, sourceIndex) in msg.sources" :key="sourceIndex">
                  <a
                    v-if="getSafeSourceUrl(source.url)"
                    class="source-links__item"
                    :href="getSafeSourceUrl(source.url)"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {{ getSourceUrlLabel(source, sourceIndex) }}
                  </a>
                </template>
              </div>
              <div class="message-footer">
                <span class="message-time">{{ msg.time }}</span>
                <template v-if="msg.role === 'assistant'">
                  <el-button
                    v-if="msg.feedback === undefined"
                    :icon="CircleCheck"
                    text
                    size="small"
                    @click="handleFeedback(index, 1)"
                  >
                    满意
                  </el-button>
                  <el-button
                    v-if="msg.feedback === undefined"
                    :icon="CircleClose"
                    text
                    size="small"
                    @click="handleFeedback(index, -1)"
                  >
                    不满意
                  </el-button>
                  <span v-if="msg.feedback !== undefined" class="feedback-status">
                    <component :is="msg.feedback === 1 ? IconThumbUp : IconThumbDown" :size="14" />
                    {{ msg.feedback === 1 ? '已满意' : '已反馈' }}
                  </span>
                </template>
              </div>
            </div>
          </div>

          <div v-if="isLoading && !hasStreamingAssistant" class="message assistant loading">
            <div class="message-avatar"><IconBot /></div>
            <div class="message-content">
              <div class="message-bubble message-bubble--loading">
                <div class="loading-dots">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input">
          <div class="input-wrapper">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="2"
              maxlength="2000"
              show-word-limit
              resize="none"
              placeholder="请输入你的反诈问题..."
              :disabled="isLoading"
              @keydown.enter.ctrl="handleSend"
            />
            <el-button
              type="primary"
              :icon="Promotion"
              :disabled="!canSend"
              @click="handleSend"
            >
              发送
            </el-button>
          </div>
          <div class="input-tip">按 Ctrl + Enter 发送</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { Plus, Delete, Menu, Refresh, Promotion, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { IconBot, IconUser, IconThumbUp, IconThumbDown } from '@/components/icons'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  askQuestionStream,
  getConversationHistory,
  getSessionList,
  submitFeedback,
  deleteSession as deleteSessionApi,
  createNewSession
} from '@/api/chat'
import type { ChatMessage, ChatVO, SessionVO, SourceVO } from '@/api/chat'

const inputText = ref('')
const messagesRef = ref<HTMLElement>()
const messages = ref<ChatMessage[]>([])
const sessionList = ref<SessionVO[]>([])
const sessionId = ref<string | null>(null)
const isLoading = ref(false)
const sidebarCollapsed = ref(false)
const shouldAutoScroll = ref(true)

const quickQuestions = [
  '什么是刷单诈骗？',
  '近期高发诈骗有哪些？',
  '如何识别杀猪盘？',
  '被骗后如何处理？'
]

const canSend = computed(() => !isLoading.value && inputText.value.trim().length > 0)
const hasStreamingAssistant = computed(() => messages.value.some(message => message.role === 'assistant' && message.streaming))

const parseDate = (time?: string) => {
  if (!time) return null
  const date = new Date(time.replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? null : date
}

const formatTime = (time: string) => {
  if (!time) return ''
  const date = parseDate(time)
  if (!date) return time
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString()
}

const formatClock = (time?: string) => {
  const date = parseDate(time)
  return date ? date.toLocaleTimeString() : ''
}

const formatDateTime = (time?: string) => {
  const date = parseDate(time)
  return date ? date.toLocaleString() : (time || '')
}

const normalizeFeedback = (feedback?: number): 1 | -1 | undefined => {
  return feedback === 1 || feedback === -1 ? feedback : undefined
}

const normalizeSources = (sources?: SourceVO[]) => {
  if (!Array.isArray(sources)) return []
  return sources.filter(source => {
    return Boolean(source?.title || source?.domain || source?.snippet || source?.url)
  })
}

const buildChatRequest = (question: string) => {
  return {
    question,
    sessionId: sessionId.value || undefined,
    answerType: 'auto' as const
  }
}

const toAssistantMessage = (item: ChatVO): ChatMessage => ({
  role: 'assistant',
  content: item.answer.trim(),
  reasoning: item.reasoning?.trim() || undefined,
  time: formatClock(item.createTime) || new Date().toLocaleTimeString(),
  sources: normalizeSources(item.sources),
  retrievedAt: item.retrievedAt,
  searchProvider: item.searchProvider,
  riskLevel: item.riskLevel,
  fallback: item.fallback === undefined ? undefined : Boolean(item.fallback),
  fallbackReason: item.fallbackReason,
  streaming: false,
  feedback: normalizeFeedback(item.feedback)
})

const getRiskClass = (riskLevel?: string) => {
  if (riskLevel === 'low' || riskLevel === 'medium' || riskLevel === 'high') {
    return riskLevel
  }
  return 'unknown'
}

const getRiskLabel = (riskLevel?: string) => {
  const labels: Record<string, string> = {
    low: '低风险',
    medium: '中风险',
    high: '高风险'
  }
  return labels[riskLevel || ''] || (riskLevel ? `风险：${riskLevel}` : '风险未知')
}

const shouldShowMeta = (msg: ChatMessage) => {
  return msg.role === 'assistant'
    && Boolean(
      msg.riskLevel
      || msg.retrievedAt
      || msg.searchProvider
      || msg.sources?.length
      || msg.fallback !== undefined
      || msg.fallbackReason
    )
}

const getSafeSourceUrl = (url?: string) => {
  if (!url) return ''
  try {
    const parsed = new URL(url)
    return parsed.protocol === 'http:' || parsed.protocol === 'https:' ? parsed.href : ''
  } catch {
    return ''
  }
}

const getSourceTitle = (source: SourceVO) => {
  return source.title?.trim() || source.domain?.trim() || '未命名来源'
}

const getSourceUrlLabel = (source: SourceVO, index: number) => {
  const url = getSafeSourceUrl(source.url)
  const title = getSourceTitle(source)
  return url ? `[${index + 1}] ${title}：${url}` : `[${index + 1}] ${title}`
}

const getErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

const escapeHtml = (content: string) => {
  return content.replace(/[&<>"']/g, (char) => {
    const entities: Record<string, string> = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    }
    return entities[char]
  })
}

const formatContent = (content: string) => {
  if (!content) return ''
  return escapeHtml(content)
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
}

const isNearBottom = () => {
  const el = messagesRef.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 80
}

const handleMessagesScroll = () => {
  shouldAutoScroll.value = isNearBottom()
}

const scrollToBottom = (force = false) => {
  nextTick(() => {
    const el = messagesRef.value
    if (!el) return
    if (force || shouldAutoScroll.value) {
      el.scrollTop = el.scrollHeight
      shouldAutoScroll.value = true
    }
  })
}

const loadSessionList = async () => {
  try {
    const res = await getSessionList()
    sessionList.value = res || []
  } catch (error) {
    console.error('加载会话列表失败', error)
    ElMessage.error(getErrorMessage(error, '加载会话列表失败'))
  }
}

const switchSession = async (sid: string) => {
  sessionId.value = sid
  try {
    const res = await getConversationHistory(sid)
    const history = res || []
    const restored: ChatMessage[] = []
    for (const item of history) {
      const t = formatClock(item.createTime)
      if (item.question?.trim()) {
        restored.push({
          role: 'user',
          content: item.question.trim(),
          time: t
        })
      }
      if (item.answer?.trim()) {
        restored.push(toAssistantMessage(item))
      }
    }
    messages.value = restored
    scrollToBottom(true)
  } catch (error) {
    console.error('加载会话历史失败', error)
    ElMessage.error(getErrorMessage(error, '加载会话历史失败'))
  }
}

const startNewSession = async () => {
  try {
    const res = await createNewSession()
    sessionId.value = res
    messages.value = []
    await loadSessionList()
  } catch (error) {
    console.error('创建会话失败', error)
    ElMessage.error(getErrorMessage(error, '创建会话失败'))
  }
}

const handleDeleteSession = async (sid: string) => {
  try {
    await ElMessageBox.confirm('确定要删除该会话吗？', '提示', {
      type: 'warning'
    })
    await deleteSessionApi(sid)
    ElMessage.success('删除成功')
    if (sessionId.value === sid) {
      sessionId.value = null
      messages.value = []
    }
    await loadSessionList()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除会话失败', error)
      ElMessage.error(getErrorMessage(error, '删除会话失败'))
    }
  }
}

const sendMessage = async () => {
  if (!canSend.value) return

  const question = inputText.value.trim()
  const userMessage: ChatMessage = {
    role: 'user',
    content: question,
    time: new Date().toLocaleTimeString()
  }
  messages.value.push(userMessage)
  inputText.value = ''
  scrollToBottom(true)

  const assistantIndex = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    reasoning: '',
    time: new Date().toLocaleTimeString(),
    sources: [],
    streaming: true
  })
  scrollToBottom(true)

  isLoading.value = true

  const patchAssistantMessage = (patch: Partial<ChatMessage>) => {
    const current = messages.value[assistantIndex]
    if (!current || current.role !== 'assistant') return
    messages.value[assistantIndex] = {
      ...current,
      ...patch
    }
  }

  const getAssistantMessage = () => messages.value[assistantIndex]

  try {
    const requestPayload = buildChatRequest(question)
    const data = await askQuestionStream(requestPayload, {
      onMeta: (meta) => {
        if (meta.sessionId && sessionId.value !== meta.sessionId) {
          sessionId.value = meta.sessionId
        }
        patchAssistantMessage({
          sources: normalizeSources(meta.sources),
          retrievedAt: meta.retrievedAt,
          searchProvider: meta.searchProvider,
          riskLevel: meta.riskLevel,
          fallback: meta.fallback === undefined ? undefined : Boolean(meta.fallback),
          fallbackReason: meta.fallbackReason
        })
        scrollToBottom()
      },
      onReasoning: (delta) => {
        if (!delta) return
        const current = getAssistantMessage()
        patchAssistantMessage({
          reasoning: `${current?.reasoning || ''}${delta}`
        })
        scrollToBottom()
      },
      onDelta: (delta) => {
        if (!delta) return
        const current = getAssistantMessage()
        patchAssistantMessage({
          content: `${current?.content || ''}${delta}`
        })
        scrollToBottom()
      },
      onDone: (doneData) => {
        const current = getAssistantMessage()
        patchAssistantMessage({
          content: doneData.answer?.trim() || current?.content || '',
          reasoning: current?.reasoning?.trim() || doneData.reasoning?.trim() || undefined,
          sources: normalizeSources(doneData.sources),
          retrievedAt: doneData.retrievedAt,
          searchProvider: doneData.searchProvider,
          riskLevel: doneData.riskLevel,
          fallback: doneData.fallback === undefined ? undefined : Boolean(doneData.fallback),
          fallbackReason: doneData.fallbackReason,
          time: formatClock(doneData.createTime) || current?.time,
          streaming: false
        })
      }
    })

    if (sessionId.value !== data.sessionId) {
      sessionId.value = data.sessionId
    }
    await loadSessionList()
    scrollToBottom()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '发送失败，请重试'))
    messages.value.splice(Math.max(0, messages.value.length - 2), 2)
  } finally {
    patchAssistantMessage({ streaming: false })
    isLoading.value = false
  }
}

const handleSend = () => {
  sendMessage()
}

const sendQuickQuestion = (question: string) => {
  inputText.value = question
  sendMessage()
}

const handleFeedback = async (index: number, feedback: 1 | -1) => {
  if (!sessionId.value) return
  const targetMessage = messages.value[index]
  if (!targetMessage || targetMessage.role !== 'assistant') return
  try {
    await submitFeedback({
      sessionId: sessionId.value,
      feedback
    })
    targetMessage.feedback = feedback
    ElMessage.success(feedback === 1 ? '感谢您的反馈' : '感谢您的反馈，我们会改进')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '提交反馈失败'))
  }
}

const clearConversation = () => {
  messages.value = []
  sessionId.value = null
}

onMounted(() => {
  loadSessionList()
})
</script>

<style scoped lang="scss">
/* 本页使用的设计令牌：全局未定义时在此补齐，否则 var(--primary) 等无效会导致气泡无背景、用户白字不可见 */
.page-container {
  --primary: hsl(215, 70%, 52%);
  --primary-light: hsl(215, 70%, 94%);
  --text-primary: hsl(220, 15%, 18%);
  --text-secondary: hsl(220, 10%, 46%);
  --border-color: hsl(220, 14%, 90%);
  --bg-gray: hsl(220, 14%, 96%);
  --bg-page: hsl(220, 18%, 98%);
  --spacing-sm: 8px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --radius-sm: 8px;
  --radius-md: 12px;
  --shadow-sm: 0 2px 8px hsla(220, 18%, 12%, 0.08);
  --shadow-md: 0 4px 16px hsla(220, 18%, 12%, 0.12);
  --transition-fast: 200ms ease;
  --transition-normal: 300ms ease;

  padding: var(--spacing-md);
  min-height: calc(100vh - 120px);
}

.chat-container {
  display: flex;
  background: #fff;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  height: calc(100vh - 160px);
  max-height: 800px;
  overflow: hidden;
}

.chat-sidebar {
  width: 280px;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-normal);
  background: #fafafa;

  &.is-collapsed {
    width: 0;
    overflow: hidden;
    border-right: none;
  }

  .sidebar-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--spacing-md);
    border-bottom: 1px solid var(--border-color);

    h3 {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-primary);
    }
  }

  .session-list {
    flex: 1;
    overflow-y: auto;
    padding: var(--spacing-sm);
  }

  .session-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--spacing-sm) var(--spacing-md);
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: background-color var(--transition-fast);
    margin-bottom: 4px;

    &:hover {
      background: rgba(0, 0, 0, 0.04);
    }

    &.is-active {
      background: var(--primary-light);
    }

    .session-content {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 2px;

      .session-title {
        font-size: 13px;
        color: var(--text-primary);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .session-time {
        font-size: 11px;
        color: var(--text-secondary);
      }
    }

    .session-delete {
      opacity: 0;
      transition: opacity var(--transition-fast);
    }

    &:hover .session-delete {
      opacity: 1;
    }
  }
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;

  .chat-header {
    display: flex;
    align-items: center;
    gap: var(--spacing-md);
    padding: var(--spacing-md) var(--spacing-lg);
    border-bottom: 1px solid var(--border-color);

    .toggle-sidebar {
      display: none;
    }

    .chat-icon {
      :deep(svg) {
        width: 36px;
        height: 36px;
      }
    }

    .chat-info {
      flex: 1;

      h3 {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-primary);
        margin-bottom: 2px;
      }

      p {
        font-size: 12px;
        color: var(--text-secondary);
      }
    }

    .header-actions {
      display: flex;
      gap: var(--spacing-sm);
    }
  }

}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-lg);
  background: hsl(220, 20%, 97%);

  .welcome-tip {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    text-align: center;
    color: var(--text-secondary);

    .welcome-icon {
      margin-bottom: var(--spacing-md);

      :deep(svg) {
        width: 64px;
        height: 64px;
        color: var(--primary);
      }
    }

    h3 {
      font-size: 18px;
      color: var(--text-primary);
      margin-bottom: var(--spacing-sm);
    }

    ul {
      list-style: none;
      padding: 0;
      margin: var(--spacing-md) 0;

      li {
        padding: 4px 0;
      }
    }

    .quick-questions {
      display: flex;
      flex-wrap: wrap;
      gap: var(--spacing-sm);
      justify-content: center;
      margin-top: var(--spacing-md);

      .quick-question {
        padding: 8px 16px;
        background: var(--primary-light);
        color: var(--primary);
        border-radius: 16px;
        font-size: 13px;
        cursor: pointer;
        transition: all var(--transition-fast);

        &:hover {
          background: var(--primary);
          color: #fff;
        }
      }
    }
  }

  .message {
    display: flex;
    gap: var(--spacing-sm);
    margin-bottom: var(--spacing-lg);
    animation: messageIn 0.3s ease-out;

    &.user {
      flex-direction: row-reverse;

      .message-avatar {
        :deep(svg) {
          color: var(--primary);
        }
      }

      .message-content {
        align-items: flex-end;

        .message-bubble {
          background: linear-gradient(135deg, var(--primary) 0%, hsl(215, 70%, 45%) 100%);
          color: #fff;
          border-radius: var(--radius-md) var(--radius-sm) var(--radius-md) var(--radius-md);
          box-shadow: var(--shadow-sm);

          &::before {
            right: -6px;
            left: auto;
            border-left: 8px solid var(--primary);
          }
        }
      }
    }

    &.assistant {
      .message-avatar {
        :deep(svg) {
          color: hsl(160, 60%, 45%);
        }
      }

      .message-content {
        .message-bubble {
          background: #fff;
          border: 1px solid var(--border-color);
          border-radius: var(--radius-sm) var(--radius-md) var(--radius-md) var(--radius-md);
          box-shadow: var(--shadow-sm);

          &::before {
            left: -6px;
            border-right: 8px solid #fff;
          }
        }
      }
    }

    &.loading {
      .message-bubble--loading {
        min-height: 44px;
        display: flex;
        align-items: center;
      }

      .loading-dots {
        display: flex;
        gap: 4px;
        padding: 4px 0;

        span {
          width: 8px;
          height: 8px;
          background: var(--text-secondary);
          border-radius: 50%;
          animation: bounce 1.4s infinite ease-in-out both;

          &:nth-child(1) { animation-delay: -0.32s; }
          &:nth-child(2) { animation-delay: -0.16s; }
        }
      }
    }

    .message-avatar {
      flex-shrink: 0;
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--bg-gray);
      border-radius: 50%;
      transition: transform var(--transition-fast);

      &:hover {
        transform: scale(1.08);
      }

      :deep(svg) {
        width: 20px;
        height: 20px;
      }
    }

    .message-content {
      display: flex;
      flex-direction: column;
      max-width: 70%;
      gap: 4px;

      .reasoning-panel {
        padding: 10px 12px;
        border: 1px solid hsl(220, 14%, 86%);
        border-radius: var(--radius-sm);
        background: hsl(220, 18%, 98%);
        color: var(--text-secondary);
        font-size: 12px;
        line-height: 1.6;
        box-shadow: var(--shadow-sm);

        .reasoning-panel__title {
          margin-bottom: 4px;
          color: var(--text-primary);
          font-size: 12px;
          font-weight: 600;
        }

        .reasoning-panel__body {
          overflow-wrap: anywhere;
        }
      }

      .message-bubble {
        position: relative;
        padding: var(--spacing-md);
        line-height: 1.7;
        font-size: 14px;
        transition: box-shadow var(--transition-fast);
        word-break: break-word;

        &--plain {
          white-space: pre-wrap;
        }

        &::before {
          content: '';
          position: absolute;
          bottom: 8px;
          width: 0;
          height: 0;
          border-top: 8px solid transparent;
          border-bottom: 8px solid transparent;
        }

        &:hover {
          box-shadow: var(--shadow-md);
        }

        :deep(strong) {
          font-weight: 600;
        }

        :deep(code) {
          background: rgba(0, 0, 0, 0.06);
          padding: 2px 6px;
          border-radius: 4px;
          font-family: monospace;
        }

        :deep(ul), :deep(ol) {
          margin: 8px 0;
          padding-left: 20px;
        }

        :deep(li) {
          margin: 4px 0;
        }
      }

      .message-meta {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        max-width: 100%;

        .meta-tag {
          display: inline-flex;
          align-items: center;
          min-height: 22px;
          padding: 2px 8px;
          border: 1px solid hsl(220, 14%, 86%);
          border-radius: var(--radius-sm);
          background: #fff;
          color: var(--text-secondary);
          font-size: 11px;
          line-height: 1.2;
        }

        .meta-tag--type {
          border-color: hsl(160, 44%, 78%);
          background: hsl(160, 58%, 96%);
          color: hsl(160, 60%, 30%);
        }

        .meta-tag--time {
          background: hsl(220, 18%, 96%);
        }

        .meta-tag--provider {
          border-color: hsl(205, 64%, 78%);
          background: hsl(205, 70%, 96%);
          color: hsl(205, 64%, 34%);
        }

        .meta-tag--normal {
          border-color: hsl(160, 44%, 78%);
          background: hsl(160, 58%, 96%);
          color: hsl(160, 60%, 30%);
        }

        .meta-tag--fallback {
          border-color: hsl(38, 72%, 70%);
          background: hsl(38, 90%, 96%);
          color: hsl(32, 70%, 38%);
        }

        .meta-tag--reason {
          background: hsl(220, 18%, 96%);
          font-family: monospace;
        }

        .meta-tag--risk-low {
          border-color: hsl(160, 44%, 78%);
          background: hsl(160, 58%, 96%);
          color: hsl(160, 60%, 30%);
        }

        .meta-tag--risk-medium {
          border-color: hsl(38, 72%, 70%);
          background: hsl(38, 90%, 96%);
          color: hsl(32, 70%, 38%);
        }

        .meta-tag--risk-high {
          border-color: hsl(0, 72%, 78%);
          background: hsl(0, 88%, 97%);
          color: hsl(0, 68%, 42%);
        }

        .meta-tag--risk-unknown {
          background: hsl(220, 18%, 96%);
        }
      }

      .source-links {
        display: flex;
        flex-direction: column;
        gap: 4px;
        max-width: 100%;
        padding: 8px 0;
        color: var(--text-secondary);
        font-size: 12px;
        line-height: 1.5;

        .source-links__label {
          color: var(--text-primary);
          font-weight: 600;
        }

        .source-links__item {
          color: var(--primary);
          overflow-wrap: anywhere;
          text-decoration: none;

          &:hover {
            text-decoration: underline;
          }
        }
      }

      .message-footer {
        display: flex;
        align-items: center;
        gap: var(--spacing-sm);

        .message-time {
          font-size: 11px;
          color: var(--text-secondary);
        }

        :deep(.el-button) {
          padding: 2px 6px;
          font-size: 12px;
          border-radius: var(--radius-sm);
          transition: all var(--transition-fast);

          &:hover {
            background: var(--primary-light);
          }

          &:active {
            transform: scale(0.95);
          }
        }

        .feedback-status {
          font-size: 12px;
          color: var(--text-secondary);
          display: flex;
          align-items: center;
          gap: 4px;

          :deep(svg) {
            width: 14px;
            height: 14px;
          }
        }
      }
    }
  }
}

@keyframes messageIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chat-input {
  padding: var(--spacing-md) var(--spacing-lg);
  border-top: 1px solid var(--border-color);
  background: var(--bg-page);

  .input-wrapper {
    display: flex;
    gap: var(--spacing-sm);
    align-items: flex-end;

    :deep(.el-textarea) {
      flex: 1;

      .el-textarea__inner {
        border-radius: var(--radius-md);
        transition: box-shadow var(--transition-fast);

        &:focus {
          box-shadow: 0 0 0 3px var(--primary-light);
        }
      }
    }

    :deep(.el-button--primary) {
      height: 58px;
      padding: 0 20px;
      border-radius: var(--radius-md);
      transition: all var(--transition-fast);

      &:hover:not(:disabled) {
        transform: translateY(-1px);
        box-shadow: var(--shadow-sm);
      }

      &:active:not(:disabled) {
        transform: scale(0.98);
      }
    }
  }

  .input-tip {
    font-size: 11px;
    color: var(--text-secondary);
    margin-top: 6px;
    text-align: right;
  }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

@media (max-width: 768px) {
  .chat-sidebar {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 100;
    background: #fff;

    &.is-collapsed {
      width: 0;
    }
  }

  .chat-main {
    .chat-header {
      .toggle-sidebar {
        display: block;
      }
    }

  }

  .chat-messages .message .message-content {
    max-width: 85%;
  }
}
</style>
