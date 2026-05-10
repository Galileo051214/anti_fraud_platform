import request from '@/utils/request'
import type { AxiosPromise } from 'axios'

/**
 * 上传图片
 * @param file 图片文件
 */
export function uploadImage(file: File): AxiosPromise<{ data: string }> {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
