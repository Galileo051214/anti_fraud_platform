import request from '@/utils/request'
import type { AxiosPromise } from 'axios'

export interface PostVO {
  id: number
  userId: number
  title: string
  content: string
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
}): AxiosPromise<PageResult<PostVO>> {
  return request({
    url: '/forum/post/page',
    method: 'get',
    params
  })
}

/**
 * 获取帖子详情
 */
export function getPostDetail(id: number): AxiosPromise<PostVO> {
  return request({
    url: `/forum/post/${id}`,
    method: 'get'
  })
}

/**
 * 创建帖子
 */
export function createPost(data: CreatePostRequest): AxiosPromise<PostVO> {
  return request({
    url: '/forum/post',
    method: 'post',
    data
  })
}

/**
 * 更新帖子
 */
export function updatePost(id: number, data: UpdatePostRequest): AxiosPromise<PostVO> {
  return request({
    url: `/forum/post/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除帖子
 */
export function deletePost(id: number): AxiosPromise<void> {
  return request({
    url: `/forum/post/${id}`,
    method: 'delete'
  })
}

/**
 * 点赞帖子
 */
export function likePost(id: number): AxiosPromise<void> {
  return request({
    url: `/forum/post/${id}/like`,
    method: 'post'
  })
}

/**
 * 取消点赞
 */
export function unlikePost(id: number): AxiosPromise<void> {
  return request({
    url: `/forum/post/${id}/like`,
    method: 'delete'
  })
}

/**
 * 获取帖子的评论列表
 */
export function getPostComments(postId: number): AxiosPromise<CommentVO[]> {
  return request({
    url: `/forum/post/${postId}/comments`,
    method: 'get'
  })
}

/**
 * 获取用户发布的帖子
 */
export function getUserPosts(params: {
  userId: number
  pageNum: number
  pageSize: number
}): AxiosPromise<PageResult<PostVO>> {
  return request({
    url: `/forum/user/${params.userId}/posts`,
    method: 'get',
    params: { pageNum: params.pageNum, pageSize: params.pageSize }
  })
}

/**
 * 创建评论
 */
export function createComment(data: CreateCommentRequest): AxiosPromise<CommentVO> {
  return request({
    url: '/comment',
    method: 'post',
    data
  })
}

/**
 * 删除评论
 */
export function deleteComment(id: number): AxiosPromise<void> {
  return request({
    url: `/comment/${id}`,
    method: 'delete'
  })
}

/**
 * 点赞评论
 */
export function likeComment(id: number): AxiosPromise<void> {
  return request({
    url: `/comment/${id}/like`,
    method: 'post'
  })
}

/**
 * 取消点赞评论
 */
export function unlikeComment(id: number): AxiosPromise<void> {
  return request({
    url: `/comment/${id}/like`,
    method: 'delete'
  })
}
