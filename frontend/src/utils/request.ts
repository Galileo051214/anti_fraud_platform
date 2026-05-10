import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data

    if (res.code === 200) {
      return res
    }

    if (res.code === 401) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      const userStore = useUserStore()
      userStore.clearUser()
      router.push('/login')
      return Promise.reject(new Error(res.message || '未授权'))
    }

    if (res.code === 403) {
      ElMessage.error('没有权限访问该资源')
      return Promise.reject(new Error(res.message || '没有权限'))
    }

    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    console.error('响应错误:', error)

    if (error.response) {
      switch (error.response.status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          localStorage.removeItem('token')
          const userStore = useUserStore()
          userStore.clearUser()
          router.push('/login')
          break
        case 403:
          ElMessage.error('没有权限访问该资源')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else if (error.request) {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      ElMessage.error('请求配置错误')
    }

    return Promise.reject(error)
  }
)

export default service

export function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.get<any, any>(url, config).then((res: any) => res?.data as T)
}

export function post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.post<any, any>(url, data, config).then((res: any) => res?.data as T)
}

export function put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.put<any, any>(url, data, config).then((res: any) => res?.data as T)
}

export function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete<any, any>(url, config).then((res: any) => res?.data as T)
}
