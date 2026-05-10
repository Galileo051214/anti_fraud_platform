import { defineStore } from 'pinia'
import { ref } from 'vue'
import { get } from '@/utils/request'

export interface ScoreInfo {
  id: number
  userId: number
  totalScore: number
  currentLevel: number
  weeklyScore: number
  updateTime: string
  unlockedAchievements: number
  totalAchievements: number
}

export interface Achievement {
  id: number
  code: string
  name: string
  description: string
  icon: string | null
  scoreReward: number
  conditionType: string
  conditionValue: number
  createTime: string
  unlocked: boolean
}

export interface UserAchievement {
  id: number
  userId: number
  achievementId: number
  achievementCode: string
  achievementName: string
  description: string
  icon: string | null
  scoreReward: number
  achievedTime: string
}

export const useScoreStore = defineStore('score', () => {
  const scoreInfo = ref<ScoreInfo | null>(null)
  const achievements = ref<Achievement[]>([])
  const userAchievements = ref<UserAchievement[]>([])
  const loading = ref(false)

  async function fetchScoreInfo() {
    loading.value = true
    try {
      const res = await get<ScoreInfo>('/score/info')
      scoreInfo.value = res
      return res
    } finally {
      loading.value = false
    }
  }

  async function fetchAchievements() {
    loading.value = true
    try {
      const res = await get<Achievement[]>('/achievement/list')
      achievements.value = res
      return res
    } finally {
      loading.value = false
    }
  }

  async function fetchUserAchievements() {
    loading.value = true
    try {
      const res = await get<UserAchievement[]>('/achievement/user')
      userAchievements.value = res
      return res
    } finally {
      loading.value = false
    }
  }

  async function fetchAchievementCount() {
    const res = await get<{ unlocked: number; total: number }>('/achievement/user/count')
    return res
  }

  function getLevelProgress() {
    if (!scoreInfo.value) return { current: 0, next: 100, percent: 0 }
    const currentLevel = scoreInfo.value.currentLevel
    const currentScore = scoreInfo.value.totalScore
    const levelStartScore = (currentLevel - 1) * 100
    const levelEndScore = currentLevel * 100
    const progressInLevel = currentScore - levelStartScore
    const percent = Math.min((progressInLevel / 100) * 100, 100)
    return {
      current: progressInLevel,
      next: 100,
      percent,
      levelStartScore,
      levelEndScore
    }
  }

  function getLifecycleLabel(stage: string) {
    const labels: Record<string, string> = {
      newbie: '新手期',
      growing: '成长期',
      mature: '成熟期'
    }
    return labels[stage] || stage
  }

  return {
    scoreInfo,
    achievements,
    userAchievements,
    loading,
    fetchScoreInfo,
    fetchAchievements,
    fetchUserAchievements,
    fetchAchievementCount,
    getLevelProgress,
    getLifecycleLabel
  }
})
