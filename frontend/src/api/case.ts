import request from '@/utils/request'
import type { AxiosPromise } from 'axios'

export interface CaseVO {
  id: number
  title: string
  caseType: string
  content: string
  scripts?: string
  targetGrades?: string[]
  targetMajors?: string[]
  difficultyLevel: number
  difficultyName: string
  riskScore: number
  riskLevel: string
  viewCount: number
  likeCount: number
  likeRate: number
  wilsonScore: number
  tags: TagVO[]
  isFeatured: number
  status: number
  publishTime: string
  createTime: string
  isLiked?: boolean
}

export interface TagVO {
  id: number
  name: string
  category: string
  description?: string
  color: string
  caseCount?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface CaseBrowseVO {
  caseId: number
  caseTitle: string
  caseType: string
  difficultyLevel: number
  browseTime: string
  stayDuration: number
  stayDurationDesc: string
  tagNames: string[]
}

export interface CreateCaseRequest {
  title: string
  caseType: string
  content: string
  scripts?: string
  targetGrades?: string[]
  targetMajors?: string[]
  difficultyLevel?: number
  riskScore?: number
  tagIds?: number[]
}

export interface UpdateCaseRequest extends Partial<CreateCaseRequest> {
  status?: number
  isFeatured?: number
}

/**
 * 获取案例分页列表
 */
export function getCasePage(params: {
  pageNum: number
  pageSize: number
  tagId?: number
  keyword?: string
}): AxiosPromise<PageResult<CaseVO>> {
  return request({
    url: '/case/page',
    method: 'get',
    params
  })
}

/**
 * 获取案例详情
 */
export function getCaseDetail(id: number): AxiosPromise<CaseVO> {
  return request({
    url: `/case/${id}`,
    method: 'get'
  })
}

/**
 * 创建案例(管理员)
 */
export function createCase(data: CreateCaseRequest): AxiosPromise<CaseVO> {
  return request({
    url: '/case',
    method: 'post',
    data
  })
}

/**
 * 更新案例(管理员)
 */
export function updateCase(id: number, data: UpdateCaseRequest): AxiosPromise<CaseVO> {
  return request({
    url: `/case/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除案例(管理员)
 */
export function deleteCase(id: number): AxiosPromise<void> {
  return request({
    url: `/case/${id}`,
    method: 'delete'
  })
}

/**
 * 发布案例(管理员)
 */
export function publishCase(id: number): AxiosPromise<void> {
  return request({
    url: `/case/${id}/publish`,
    method: 'post'
  })
}

/**
 * 设置精选(管理员)
 */
export function setCaseFeatured(id: number, isFeatured: number): AxiosPromise<void> {
  return request({
    url: `/case/${id}/featured`,
    method: 'put',
    params: { isFeatured }
  })
}

/**
 * 点赞案例
 */
export function likeCase(id: number): AxiosPromise<void> {
  return request({
    url: `/case/${id}/like`,
    method: 'post'
  })
}

/**
 * 取消点赞
 */
export function unlikeCase(id: number): AxiosPromise<void> {
  return request({
    url: `/case/${id}/like`,
    method: 'delete'
  })
}

/**
 * 记录浏览
 */
export function browseCase(id: number, stayDuration: number = 0): AxiosPromise<void> {
  return request({
    url: `/case/${id}/browse`,
    method: 'post',
    params: { stayDuration }
  })
}

/**
 * 获取浏览记录
 */
export function getBrowseHistory(params: {
  pageNum: number
  pageSize: number
}): AxiosPromise<PageResult<CaseBrowseVO>> {
  return request({
    url: '/case/browse/history',
    method: 'get',
    params
  })
}

/**
 * 获取热度排行榜
 */
export function getHotCases(limit: number = 10): AxiosPromise<CaseVO[]> {
  return request({
    url: '/case/hot',
    method: 'get',
    params: { limit }
  })
}

/**
 * 获取威尔逊置信度得分
 */
export function getWilsonScore(positive: number, total: number): AxiosPromise<number> {
  return request({
    url: '/case/wilson',
    method: 'get',
    params: { positive, total }
  })
}

/**
 * 获取所有标签
 */
export function getAllTags(): AxiosPromise<TagVO[]> {
  return request({
    url: '/case/tag/list',
    method: 'get'
  })
}

/**
 * 根据分类获取标签
 */
export function getTagsByCategory(category: string): AxiosPromise<TagVO[]> {
  return request({
    url: `/case/tag/category/${category}`,
    method: 'get'
  })
}

/**
 * 创建标签(管理员)
 */
export function createTag(data: {
  name: string
  category: string
  description?: string
  color?: string
}): AxiosPromise<TagVO> {
  return request({
    url: '/case/tag',
    method: 'post',
    data
  })
}

/**
 * 更新标签(管理员)
 */
export function updateTag(id: number, data: {
  name?: string
  category?: string
  description?: string
  color?: string
}): AxiosPromise<TagVO> {
  return request({
    url: `/case/tag/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除标签(管理员)
 */
export function deleteTag(id: number): AxiosPromise<void> {
  return request({
    url: `/case/tag/${id}`,
    method: 'delete'
  })
}
