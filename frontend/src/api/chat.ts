import request from '@/utils/request'
import type { AxiosPromise } from 'axios'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  time?: string
  /** 助手回复满意度：1 满意 / -1 不满意 */
  feedback?: 1 | -1
}

export interface ChatVO {
  sessionId: string
  question: string
  answer: string
  tokensUsed: number
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
}

export interface FeedbackRequest {
  sessionId: string
  feedback: 1 | -1
}

/**
 * 发送问题并获取AI回答
 */
export function askQuestion(data: ChatRequest): AxiosPromise<ChatVO> {
  return request({
    url: '/chat/ask',
    method: 'post',
    data
  })
}

/**
 * 获取会话历史
 */
export function getConversationHistory(sessionId: string): AxiosPromise<ChatVO[]> {
  return request({
    url: `/chat/history/${sessionId}`,
    method: 'get'
  })
}

/**
 * 获取会话列表
 */
export function getSessionList(): AxiosPromise<SessionVO[]> {
  return request({
    url: '/chat/sessions',
    method: 'get'
  })
}

/**
 * 提交反馈
 */
export function submitFeedback(data: FeedbackRequest): AxiosPromise<void> {
  return request({
    url: '/chat/feedback',
    method: 'post',
    data
  })
}

/**
 * 获取Token统计
 */
export function getTokenStats(): AxiosPromise<TokenStatsVO> {
  return request({
    url: '/chat/stats',
    method: 'get'
  })
}

/**
 * 删除会话
 */
export function deleteSession(sessionId: string): AxiosPromise<void> {
  return request({
    url: `/chat/session/${sessionId}`,
    method: 'delete'
  })
}

/**
 * 创建新会话
 */
export function createNewSession(): AxiosPromise<string> {
  return request({
    url: '/chat/new-session',
    method: 'post'
  })
}
