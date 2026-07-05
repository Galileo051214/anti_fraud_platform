import { get, post } from '@/utils/request'

export interface RecommendationVO {
  itemId: number
  itemType: 'case' | 'news' | 'challenge' | string
  itemSubtype?: string
  title: string
  coverImage?: string
  summary?: string
  score?: number
  reasons?: string[]
  tags?: string[]
  createTime?: string
}

export interface UserInterestVO {
  lifecycleStage: string
  lifecycleStageName: string
  knowledgeLevel: number
  weakPoints: string[]
  interestTags: { tagId: number; tagName: string; score: number }[]
}

interface CacheEntry<T> {
  data: T
  expireAt: number
}

const recommendationCache = new Map<string, CacheEntry<RecommendationVO[]>>()
let interestCache: CacheEntry<UserInterestVO> | null = null
const RECOMMENDATION_CACHE_TTL = 60 * 1000
const INTEREST_CACHE_TTL = 2 * 60 * 1000

const getRecommendationCacheKey = (params: { limit?: number; itemType?: string }) => {
  return `${params.itemType || 'all'}:${params.limit || 10}`
}

const getCached = <T>(entry?: CacheEntry<T> | null) => {
  if (!entry || entry.expireAt <= Date.now()) return null
  return entry.data
}

export function getRecommendationList(params: { limit?: number; itemType?: string }): Promise<RecommendationVO[]> {
  const cacheKey = getRecommendationCacheKey(params)
  const cached = getCached(recommendationCache.get(cacheKey))
  if (cached) return Promise.resolve(cached)

  return get<RecommendationVO[]>('/recommendation/list', { params }).then((data) => {
    const safeData = Array.isArray(data) ? data : []
    recommendationCache.set(cacheKey, {
      data: safeData,
      expireAt: Date.now() + RECOMMENDATION_CACHE_TTL
    })
    return safeData
  })
}

export function getUserInterest(): Promise<UserInterestVO> {
  const cached = getCached(interestCache)
  if (cached) return Promise.resolve(cached)

  return get<UserInterestVO>('/recommendation/interest').then((data) => {
    interestCache = {
      data,
      expireAt: Date.now() + INTEREST_CACHE_TTL
    }
    return data
  })
}

export function recordRecommendationClick(itemId: number, itemType: string): Promise<void> {
  recommendationCache.clear()
  interestCache = null
  return post<void>('/recommendation/click', null, {
    params: { itemId, itemType }
  })
}
