import { del, get, post } from '@/utils/request'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  reasoning?: string
  time?: string
  sources?: SourceVO[]
  retrievedAt?: string
  answerType?: ChatMode
  searchProvider?: string
  riskLevel?: RiskLevel
  fallback?: boolean
  fallbackReason?: string
  streaming?: boolean
  /** 助手回复满意度：1 满意 / -1 不满意 */
  feedback?: 1 | -1
}

export type ChatMode = 'qa' | 'latest_report'

export type RiskLevel = 'low' | 'medium' | 'high' | string

export interface SourceVO {
  title?: string
  url?: string
  domain?: string
  snippet?: string
  content?: string
  publishedAt?: string
}

export interface ChatVO {
  sessionId: string
  question: string
  answer: string
  reasoning?: string
  tokensUsed: number
  sources?: SourceVO[]
  retrievedAt?: string
  answerType?: ChatMode
  searchProvider?: string
  riskLevel?: RiskLevel
  fallback?: boolean
  fallbackReason?: string
  feedback?: 1 | -1
  createTime: string
}

export interface SessionVO {
  sessionId: string
  firstQuestion: string
  lastAnswer: string
  messageCount: number
  totalTokens: number
  createTime: string
  updateTime: string
}

export interface TokenStatsVO {
  totalTokens: number
  totalQuestions: number
  satisfiedCount: number
  dissatisfiedCount: number
}

export interface ChatRequest {
  question: string
  sessionId?: string
  mode?: ChatMode
  answerType?: ChatMode | 'auto'
  useWebSearch?: boolean
  fraudType?: string
  region?: string
  timeRange?: string
}

export interface FeedbackRequest {
  sessionId: string
  feedback: 1 | -1
}

export interface ChatStreamCallbacks {
  onMeta?: (data: ChatVO) => void
  onReasoning?: (delta: string) => void
  onDelta?: (delta: string) => void
  onDone?: (data: ChatVO) => void
  onError?: (message: string) => void
}

/**
 * 发送问题并获取AI回答
 */
export function askQuestion(data: ChatRequest): Promise<ChatVO> {
  return post<ChatVO>('/chat/ask', data)
}

/**
 * 流式发送问题并获取AI回答
 */
export async function askQuestionStream(data: ChatRequest, callbacks: ChatStreamCallbacks = {}): Promise<ChatVO> {
  const token = localStorage.getItem('token')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json;charset=utf-8',
    Accept: 'text/event-stream'
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch('/api/chat/ask-stream', {
    method: 'POST',
    headers,
    body: JSON.stringify(data)
  })

  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }
  if (!response.body) {
    throw new Error('浏览器不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let doneData: ChatVO | null = null

  const handleEventBlock = (block: string) => {
    const lines = block.split(/\r?\n/)
    let eventName = 'message'
    const dataLines: string[] = []

    for (const line of lines) {
      if (line.startsWith('event:')) {
        eventName = line.slice('event:'.length).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice('data:'.length).trimStart())
      }
    }

    if (!dataLines.length) return
    const rawData = dataLines.join('\n')
    const payload = JSON.parse(rawData)

    if (eventName === 'meta') {
      callbacks.onMeta?.(payload as ChatVO)
    } else if (eventName === 'reasoning') {
      callbacks.onReasoning?.(payload.delta || '')
    } else if (eventName === 'delta') {
      callbacks.onDelta?.(payload.delta || '')
    } else if (eventName === 'done') {
      doneData = payload as ChatVO
      callbacks.onDone?.(doneData)
    } else if (eventName === 'error') {
      const message = payload.message || '发送失败，请稍后再试'
      callbacks.onError?.(message)
      throw new Error(message)
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })

    let separatorIndex = buffer.search(/\r?\n\r?\n/)
    while (separatorIndex >= 0) {
      const block = buffer.slice(0, separatorIndex)
      const separatorLength = buffer.startsWith('\r\n\r\n', separatorIndex) ? 4 : 2
      buffer = buffer.slice(separatorIndex + separatorLength)
      if (block.trim()) {
        handleEventBlock(block)
      }
      separatorIndex = buffer.search(/\r?\n\r?\n/)
    }

    if (done) {
      if (buffer.trim()) {
        handleEventBlock(buffer)
      }
      break
    }
  }

  if (!doneData) {
    throw new Error('流式响应未正常结束')
  }
  return doneData
}

/**
 * 获取会话历史
 */
export function getConversationHistory(sessionId: string): Promise<ChatVO[]> {
  return get<ChatVO[]>(`/chat/history/${sessionId}`)
}

/**
 * 获取会话列表
 */
export function getSessionList(): Promise<SessionVO[]> {
  return get<SessionVO[]>('/chat/sessions')
}

/**
 * 提交反馈
 */
export function submitFeedback(data: FeedbackRequest): Promise<void> {
  return post<void>('/chat/feedback', data)
}

/**
 * 获取Token统计
 */
export function getTokenStats(): Promise<TokenStatsVO> {
  return get<TokenStatsVO>('/chat/stats')
}

/**
 * 删除会话
 */
export function deleteSession(sessionId: string): Promise<void> {
  return del<void>(`/chat/session/${sessionId}`)
}

/**
 * 创建新会话
 */
export function createNewSession(): Promise<string> {
  return post<string>('/chat/new-session')
}
