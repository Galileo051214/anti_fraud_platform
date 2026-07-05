import type { ChallengeVO } from '@/api/challenge'
import type { RecommendationVO } from '@/api/recommendation'

export type ChallengeRouteType = ChallengeVO['type'] | string | undefined

export function getChallengeRoute(id: number | string, type?: ChallengeRouteType) {
  if (type === 'scenario') {
    return `/challenge/scenario/${id}`
  }
  if (type === 'agent_scenario') {
    return `/challenge/agent/${id}`
  }
  return `/challenge/${id}`
}

export function getRecommendationChallengeRoute(item: RecommendationVO) {
  return getChallengeRoute(item.itemId, item.itemSubtype)
}
