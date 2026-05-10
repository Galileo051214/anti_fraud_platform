import { get, put } from '@/utils/request'

export interface UserVO {
  id: number
  username: string
  nickname: string
  avatar?: string
  phone?: string
  email?: string
  studentNo?: string
  role: string
  grade?: string
  major?: string
  status: number
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface AdminUpdateUserRequest {
  nickname?: string
  phone?: string
  email?: string
  grade?: string
  major?: string
}

/**
 * 获取用户列表(管理员)
 */
export function getUserList(params: {
  pageNum: number
  pageSize: number
  keyword?: string
  role?: string
  status?: number
}): Promise<PageResult<UserVO>> {
  return get<PageResult<UserVO>>('/user/list', { params })
}

/**
 * 获取用户详情(管理员)
 */
export function getUserById(id: number): Promise<UserVO> {
  return get<UserVO>(`/user/${id}`)
}

/**
 * 管理员更新用户信息
 */
export function adminUpdateUser(id: number, data: AdminUpdateUserRequest): Promise<void> {
  return put<void>(`/user/${id}`, data)
}

/**
 * 启用用户(管理员)
 */
export function enableUser(id: number): Promise<void> {
  return put<void>(`/user/${id}/enable`)
}

/**
 * 禁用用户(管理员)
 */
export function disableUser(id: number): Promise<void> {
  return put<void>(`/user/${id}/disable`)
}
