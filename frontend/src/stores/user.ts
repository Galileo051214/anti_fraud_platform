import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types/global'
import { get, post, put } from '@/utils/request'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'admin')

  async function login(username: string, password: string) {
    const res: any = await post<{ token: string; userId: number; username: string; nickname: string; avatar: string; role: string }>('/user/login', {
      username,
      password
    })
    token.value = res.token
    userInfo.value = {
      id: res.userId,
      username: res.username,
      nickname: res.nickname,
      avatar: res.avatar,
      role: res.role
    }
    localStorage.setItem('token', res.token)
    return res
  }

  async function register(data: {
    username: string
    password: string
    nickname?: string
    phone?: string
    email?: string
    studentNo?: string
    grade?: string
    major?: string
  }) {
    const res = await post<any>('/user/register', data)
    return res
  }

  async function getUserInfo() {
    if (!token.value) return null
    try {
      const res = await get<any>('/user/info')
      userInfo.value = res
      return res
    } catch (error) {
      clearUser()
      return null
    }
  }

  async function updateUser(data: {
    nickname?: string
    phone?: string
    email?: string
    avatar?: string
    grade?: string
    major?: string
  }) {
    const res = await put<any>('/user/update', data)
    await getUserInfo()
    return res
  }

  async function changePassword(oldPassword: string, newPassword: string) {
    const res = await put<any>('/user/password', {
      oldPassword,
      newPassword
    })
    return res
  }

  async function logout() {
    try {
      await post('/user/logout')
    } finally {
      clearUser()
    }
  }

  function clearUser() {
    token.value = null
    userInfo.value = null
    localStorage.removeItem('token')
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    login,
    register,
    getUserInfo,
    updateUser,
    changePassword,
    logout,
    clearUser,
    setUserInfo
  }
})
