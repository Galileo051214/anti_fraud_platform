import request from '@/utils/request'

export interface News {
  id: number
  title: string
  content: string
  summary: string
  coverImage: string
  categoryId: number
  categoryName: string
  authorId: number
  authorName: string
  newsType: 'news' | 'warning' | 'policy'
  isTop: number
  isMandatory: number
  viewCount: number
  likeCount: number
  isLiked: boolean
  status: number
  publishTime: string
  createTime: string
}

export interface NewsCategory {
  id: number
  name: string
  parentId: number
  sortOrder: number
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export const getNewsPage = (params: {
  pageNum: number
  pageSize: number
  categoryId?: number
  newsType?: string
  keyword?: string
}) => {
  return request.get<{ data: PageResult<News> }>('/news/page', { params })
}

export const getNewsDetail = (id: number) => {
  return request.get<{ data: News }>(`/news/${id}`)
}

export const likeNews = (id: number) => {
  return request.post<{ data: boolean }>(`/news/${id}/like`)
}

export const unlikeNews = (id: number) => {
  return request.delete<{ data: boolean }>(`/news/${id}/like`)
}

export const viewNews = (id: number, stayDuration?: number) => {
  return request.post(`/news/${id}/view`, { stayDuration })
}

export const getBrowseHistory = (params: { pageNum: number; pageSize: number }) => {
  return request.get<{ data: PageResult<News> }>('/news/browse/history', { params })
}

export const getCategories = () => {
  return request.get<{ data: NewsCategory[] }>('/news/category/list')
}
