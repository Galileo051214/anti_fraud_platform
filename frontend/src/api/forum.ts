import { del, get, post, put } from '@/utils/request'

export interface PostVO {
  id: number
  userId: number
  title: string
  content: string
  imageUrls?: string[]
  postType: string
  postTypeName: string
  tagIds?: number[]
  viewCount: number
  likeCount: number
  commentCount: number
  isFeatured: number
  isTop: number
  status: number
  authorName: string
  authorAvatar?: string
  isLiked?: boolean
  createTime: string
  updateTime: string
}

export interface CommentVO {
  id: number
  postId: number
  userId: number
  parentId: number
  content: string
  likeCount: number
  status: number
  authorName: string
  authorAvatar?: string
  isLiked?: boolean
  isAuthor?: boolean
  createTime: string
  children?: CommentVO[]
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface CreatePostRequest {
  title: string
  content: string
  postType: string
  imageUrls?: string[]
  tagIds?: number[]
}

export interface UpdatePostRequest extends Partial<CreatePostRequest> {
  status?: number
  isFeatured?: number
  isTop?: number
}

export interface CreateCommentRequest {
  postId: number
  parentId?: number
  content: string
}

/**
 * 获取帖子分页列表
 */
export function getPostPage(params: {
  pageNum: number
  pageSize: number
  postType?: string
  sortBy?: string
  keyword?: string
}): Promise<PageResult<PostVO>> {
  return get<PageResult<PostVO>>('/forum/post/page', { params })
}

/**
 * 获取帖子详情
 */
export function getPostDetail(id: number): Promise<PostVO> {
  return get<PostVO>(`/forum/post/${id}`)
}

/**
 * 创建帖子
 */
export function createPost(data: CreatePostRequest): Promise<PostVO> {
  return post<PostVO>('/forum/post', data)
}

/**
 * 更新帖子
 */
export function updatePost(id: number, data: UpdatePostRequest): Promise<PostVO> {
  return put<PostVO>(`/forum/post/${id}`, data)
}

/**
 * 删除帖子
 */
export function deletePost(id: number): Promise<void> {
  return del<void>(`/forum/post/${id}`)
}

/**
 * 点赞帖子
 */
export function likePost(id: number): Promise<void> {
  return post<void>(`/forum/post/${id}/like`)
}

/**
 * 取消点赞
 */
export function unlikePost(id: number): Promise<void> {
  return del<void>(`/forum/post/${id}/like`)
}

/**
 * 获取帖子的评论列表
 */
export function getPostComments(postId: number): Promise<CommentVO[]> {
  return get<CommentVO[]>(`/forum/post/${postId}/comments`)
}

/**
 * 获取用户发布的帖子
 */
export function getUserPosts(params: {
  userId: number
  pageNum: number
  pageSize: number
}): Promise<PageResult<PostVO>> {
  return get<PageResult<PostVO>>(`/forum/user/${params.userId}/posts`, {
    params: { pageNum: params.pageNum, pageSize: params.pageSize }
  })
}

/**
 * 创建评论
 */
export function createComment(data: CreateCommentRequest): Promise<CommentVO> {
  return post<CommentVO>('/comment', data)
}

/**
 * 删除评论
 */
export function deleteComment(id: number): Promise<void> {
  return del<void>(`/comment/${id}`)
}

/**
 * 点赞评论
 */
export function likeComment(id: number): Promise<void> {
  return post<void>(`/comment/${id}/like`)
}

/**
 * 取消点赞评论
 */
export function unlikeComment(id: number): Promise<void> {
  return del<void>(`/comment/${id}/like`)
}
