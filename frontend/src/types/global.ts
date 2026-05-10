export interface UserInfo {
  id: number
  username: string
  nickname?: string
  avatar?: string
  phone?: string
  email?: string
  studentNo?: string
  role: 'student' | 'admin'
  grade?: string
  major?: string
  status: number
  createTime: string
}

export interface LoginForm {
  username: string
  password: string
}

export interface RegisterForm {
  username: string
  password: string
  confirmPassword: string
  nickname?: string
  phone?: string
  studentNo?: string
  grade?: string
  major?: string
}

export interface LoginResponse {
  token: string
  userInfo: UserInfo
}

export interface PageResult<T> {
  records: T[]
  total: number
  pages: number
  current: number
  size: number
}

export interface PageQuery {
  current?: number
  size?: number
}
