import request from '@/utils/request'

export interface RecommendationVO {
  itemId: number
  itemType: 'case' | 'news' | 'challenge' | string
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

export function getRecommendationList(params: { limit?: number; itemType?: string }) {
  return request.get<RecommendationVO[]>('/recommendation/list', { params })
}

export function getUserInterest() {
  return request.get<UserInterestVO>('/recommendation/interest')
}

export function recordRecommendationClick(itemId: number, itemType: string) {
  return request.post<void>('/recommendation/click', null, {
    params: { itemId, itemType }
  })
}
