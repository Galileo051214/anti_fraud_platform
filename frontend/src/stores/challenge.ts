import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getChallengeList,
  getChallengeProgress,
  getChallengeRecords,
  getDailyLeaderboard,
  getWeeklyLeaderboard,
  getAllTimeLeaderboard,
  type ChallengeVO,
  type ChallengeRecordVO,
  type LeaderboardVO
} from '@/api/challenge'

export const useChallengeStore = defineStore('challenge', () => {
  const challengeList = ref<ChallengeVO[]>([])
  const progress = ref<{
    totalChallenges: number
    completedChallenges: number
    nextChallenges: ChallengeVO[]
  } | null>(null)
  const records = ref<ChallengeRecordVO[]>([])
  const dailyRanking = ref<LeaderboardVO[]>([])
  const weeklyRanking = ref<LeaderboardVO[]>([])
  const allTimeRanking = ref<LeaderboardVO[]>([])
  const loading = ref(false)

  async function fetchChallengeList() {
    loading.value = true
    try {
      const res = await getChallengeList()
      challengeList.value = res.data || []
    } finally {
      loading.value = false
    }
  }

  async function fetchProgress() {
    try {
      const res = await getChallengeProgress()
      progress.value = res.data
    } catch (error) {
      console.error('获取进度失败:', error)
    }
  }

  async function fetchRecords(pageNum: number = 1, pageSize: number = 10) {
    try {
      const res = await getChallengeRecords({ pageNum, pageSize })
      records.value = res.data.records || []
    } catch (error) {
      console.error('获取记录失败:', error)
    }
  }

  async function fetchDailyRanking() {
    try {
      const res = await getDailyLeaderboard(20)
      dailyRanking.value = res.data || []
    } catch (error) {
      console.error('获取日榜失败:', error)
    }
  }

  async function fetchWeeklyRanking() {
    try {
      const res = await getWeeklyLeaderboard(20)
      weeklyRanking.value = res.data || []
    } catch (error) {
      console.error('获取周榜失败:', error)
    }
  }

  async function fetchAllTimeRanking() {
    try {
      const res = await getAllTimeLeaderboard(20)
      allTimeRanking.value = res.data || []
    } catch (error) {
      console.error('获取总榜失败:', error)
    }
  }

  return {
    challengeList,
    progress,
    records,
    dailyRanking,
    weeklyRanking,
    allTimeRanking,
    loading,
    fetchChallengeList,
    fetchProgress,
    fetchRecords,
    fetchDailyRanking,
    fetchWeeklyRanking,
    fetchAllTimeRanking
  }
})
