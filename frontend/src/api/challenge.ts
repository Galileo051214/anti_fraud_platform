import request from '@/utils/request'
import type { AxiosPromise } from 'axios'

export interface ChallengeContent {
  questions: ChallengeQuestion[]
}

export interface ChallengeQuestion {
  id: string
  questionType: 'single' | 'multiple' | 'truefalse'
  text: string
  options: QuestionOption[]
  correctIndexes: number[]
  score: number
}

export interface QuestionOption {
  label: string
  text: string
}

export interface ScenarioScript {
  name: string
  description: string
  nodes: ScenarioNode[]
  edges: ScenarioEdge[]
  startNodeId: string
  endNodeIds: string[]
}

export interface ScenarioNode {
  id: string
  type: 'start' | 'dialog' | 'decision' | 'result' | 'end'
  title: string
  content: string
  role: 'scammer' | 'victim' | 'narrator'
  riskTip?: string
}

export interface ScenarioEdge {
  from: string
  to: string
  condition?: string
  label: string
  isSafeChoice?: boolean
}

export interface ChallengeVO {
  id: number
  title: string
  description: string
  levelOrder: number
  difficulty: number
  difficultyName: string
  type: 'quiz' | 'scenario'
  typeName: string
  passingScore: number
  scoreReward: number
  content?: ChallengeContent
  scripts?: ScenarioScript
  status: number
  createTime: string
  passed?: boolean
  highestScore?: number
  locked?: boolean
  unlockHint?: string
}

export interface ChallengeRecordVO {
  id: number
  challengeId: number
  challengeTitle: string
  attempts: number
  score: number
  passed: boolean
  answerDetail?: AnswerDetail
  startTime: string
  endTime: string
}

export interface AnswerDetail {
  answers: QuestionAnswer[]
  totalScore: number
  maxScore: number
  correctCount: number
}

export interface QuestionAnswer {
  questionId: string
  selectedIndexes: number[]
  correctIndexes: number[]
  correct: boolean
  score: number
}

export interface ChallengeResultVO {
  passed: boolean
  score: number
  maxScore: number
  correctCount: number
  totalCount: number
  earnedScore?: number
  rating: string
  ratingDesc: string
  highestScore?: number
  newRecord?: boolean
}

export interface ScenarioProgressVO {
  id: number
  challengeId: number
  challengeTitle: string
  currentNode: string
  currentNodeDetail?: ScenarioNodeVO
  availableChoices?: ScenarioEdgeVO[]
  decisionHistory?: DecisionRecord[]
  status: 'in_progress' | 'completed' | 'failed'
  statusName: string
  startTime: string
  finalScore?: number
  passed?: boolean
  earnedScore?: number
  difficulty?: number
  difficultyName?: string
  passingScore?: number
  scoreReward?: number
}

export interface ScenarioNodeVO {
  id: string
  type: string
  title: string
  content: string
  role: string
  riskTip?: string
}

export interface ScenarioEdgeVO {
  edgeId: string
  toNode: string
  label: string
  condition?: string
}

export interface DecisionRecord {
  nodeId: string
  edgeId: string
  choiceLabel: string
  isSafeChoice?: boolean
  timestamp: string
}

export interface LeaderboardVO {
  rank: number
  userId: number
  nickname?: string
  avatar?: string
  grade?: string
  major?: string
  score: number
  periodType: string
  isCurrentUser?: boolean
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface SubmitChallengeRequest {
  challengeId: number
  answers: Record<string, number[]>
  startTime?: number
}

export interface ScenarioDecisionRequest {
  challengeId: number
  currentNode: string
  selectedEdgeId: string
  startTime?: number
}

/**
 * 获取闯关关卡列表
 */
export function getChallengeList(): AxiosPromise<ChallengeVO[]> {
  return request({
    url: '/challenge/list',
    method: 'get'
  })
}

/**
 * 获取关卡详情
 */
export function getChallengeDetail(id: number): AxiosPromise<ChallengeVO> {
  return request({
    url: `/challenge/${id}`,
    method: 'get'
  })
}

/**
 * 获取闯关记录
 */
export function getChallengeRecords(params: {
  pageNum: number
  pageSize: number
}): AxiosPromise<PageResult<ChallengeRecordVO>> {
  return request({
    url: '/challenge/records',
    method: 'get',
    params
  })
}

/**
 * 提交闯关答案
 */
export function submitChallenge(data: SubmitChallengeRequest): AxiosPromise<ChallengeResultVO> {
  return request({
    url: '/challenge/submit',
    method: 'post',
    data
  })
}

/**
 * 获取闯关进度统计
 */
export function getChallengeProgress(): AxiosPromise<{
  totalChallenges: number
  completedChallenges: number
  nextChallenges: ChallengeVO[]
}> {
  return request({
    url: '/challenge/progress',
    method: 'get'
  })
}

/**
 * 开始情景模拟
 */
export function startScenario(challengeId: number): AxiosPromise<ScenarioProgressVO> {
  return request({
    url: `/scenario/start/${challengeId}`,
    method: 'post'
  })
}

/**
 * 获取情景模拟进度
 */
export function getScenarioProgress(challengeId: number): AxiosPromise<ScenarioProgressVO> {
  return request({
    url: `/scenario/progress/${challengeId}`,
    method: 'get'
  })
}

/**
 * 做出决策
 */
export function makeDecision(data: ScenarioDecisionRequest): AxiosPromise<ScenarioProgressVO> {
  return request({
    url: '/scenario/decision',
    method: 'post',
    data
  })
}

/**
 * 重置情景模拟
 */
export function resetScenario(challengeId: number): AxiosPromise<void> {
  return request({
    url: `/scenario/reset/${challengeId}`,
    method: 'post'
  })
}

/**
 * 获取结局
 */
export function getScenarioEnding(challengeId: number): AxiosPromise<ScenarioProgressVO> {
  return request({
    url: `/scenario/ending/${challengeId}`,
    method: 'get'
  })
}

/**
 * 获取日排行榜
 */
export function getDailyLeaderboard(limit: number = 20): AxiosPromise<LeaderboardVO[]> {
  return request({
    url: '/leaderboard/daily',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取周排行榜
 */
export function getWeeklyLeaderboard(limit: number = 20): AxiosPromise<LeaderboardVO[]> {
  return request({
    url: '/leaderboard/weekly',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取总排行榜
 */
export function getAllTimeLeaderboard(limit: number = 20): AxiosPromise<LeaderboardVO[]> {
  return request({
    url: '/leaderboard/all',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取用户排名
 */
export function getUserRank(periodType: string = 'daily'): AxiosPromise<LeaderboardVO> {
  return request({
    url: '/leaderboard/user-rank',
    method: 'get',
    params: { periodType }
  })
}

// ============ 管理员接口 ============

export interface ChallengeStatsVO {
  challengeId: number
  title: string
  totalAttempts: number
  passedCount: number
  passRate: number
  avgScore: number
  maxScore: number
  minScore: number
  avgDuration: number
}

export interface ChallengeOverviewVO {
  totalChallenges: number
  enabledChallenges: number
  disabledChallenges: number
  quizChallenges: number
  scenarioChallenges: number
  totalAttempts: number
  totalPassedUsers: number
  overallPassRate: number
  todayPassed: number
  challengeStats: ChallengeStatsVO[]
}

/**
 * 获取关卡统计概览(管理员)
 */
export function getChallengeOverview(): AxiosPromise<ChallengeOverviewVO> {
  return request({
    url: '/challenge/admin/overview',
    method: 'get'
  })
}

/**
 * 获取指定关卡的统计数据(管理员)
 */
export function getChallengeStats(id: number): AxiosPromise<ChallengeStatsVO> {
  return request({
    url: `/challenge/admin/stats/${id}`,
    method: 'get'
  })
}

/**
 * 批量启用/禁用关卡
 */
export function batchUpdateChallengeStatus(challengeIds: number[], status: number): AxiosPromise<void> {
  return request({
    url: '/challenge/admin/batch/status',
    method: 'put',
    data: { challengeIds, status }
  })
}

/**
 * 批量删除关卡
 */
export function batchDeleteChallenges(challengeIds: number[]): AxiosPromise<void> {
  return request({
    url: '/challenge/admin/batch',
    method: 'delete',
    data: { challengeIds }
  })
}

/**
 * 获取管理员关卡列表
 */
export function getAdminChallengeList(params: {
  pageNum: number
  pageSize: number
  keyword?: string
  type?: string
}): AxiosPromise<PageResult<ChallengeVO>> {
  return request({
    url: '/challenge/admin/list',
    method: 'get',
    params
  })
}

/**
 * 创建关卡(管理员)
 */
export function createChallenge(data: {
  title: string
  description: string
  levelOrder: number
  difficulty: number
  type: 'quiz' | 'scenario'
  passingScore: number
  scoreReward: number
  content?: ChallengeContent
  scripts?: ScenarioScript
}): AxiosPromise<ChallengeVO> {
  return request({
    url: '/challenge',
    method: 'post',
    data
  })
}

/**
 * 更新关卡(管理员)
 */
export function updateChallenge(id: number, data: {
  title?: string
  description?: string
  levelOrder?: number
  difficulty?: number
  type?: 'quiz' | 'scenario'
  passingScore?: number
  scoreReward?: number
  content?: ChallengeContent
  scripts?: ScenarioScript
  status?: number
}): AxiosPromise<ChallengeVO> {
  return request({
    url: `/challenge/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除关卡(管理员)
 */
export function deleteChallenge(id: number): AxiosPromise<void> {
  return request({
    url: `/challenge/${id}`,
    method: 'delete'
  })
}
