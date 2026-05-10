import { defineStore } from 'pinia'
import { ref } from 'vue'
import { get } from '@/utils/request'

export interface ProfileInfo {
  id: number
  userId: number
  grade: string | null
  major: string | null
  knowledgeLevel: number
  weakPoints: string[]
  interestTags: string[]
  lifecycleStage: string
  browseCount: number
  registerDays: number
  updateTime: string
}

export const useProfileStore = defineStore('profile', () => {
  const profileInfo = ref<ProfileInfo | null>(null)
  const loading = ref(false)

  async function fetchProfileInfo() {
    loading.value = true
    try {
      const res = await get<ProfileInfo>('/profile/info')
      profileInfo.value = res
      return res
    } finally {
      loading.value = false
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

  function getLifecycleDesc(stage: string) {
    const descs: Record<string, string> = {
      newbie: '刚刚加入平台，开启你的防骗之旅吧！',
      growing: '你已经掌握了一些防骗知识，继续加油！',
      mature: '你已经成为了防骗达人，快去帮助其他人吧！'
    }
    return descs[stage] || ''
  }

  function getKnowledgeLevelLabel(level: number) {
    if (level < 30) return '防骗新手'
    if (level < 60) return '防骗学徒'
    if (level < 80) return '防骗高手'
    return '防骗大师'
  }

  return {
    profileInfo,
    loading,
    fetchProfileInfo,
    getLifecycleLabel,
    getLifecycleDesc,
    getKnowledgeLevelLabel
  }
})
