import http from './request'
import { BASE_URL } from '@/config'

/** 获取内容列表 */
export function getContentList(page = 1, size = 20, keyword = '', silent = false) {
  return http.get('/contents', { page, size, keyword }, silent)
}

/** 获取内容详情 */
export function getContentDetail(id) {
  return http.get(`/contents/${id}`)
}

/** 创建文本内容 */
export function createTextContent(params) {
  return requestForm('/contents', params)
}

/** 上传文件内容（图片/视频） */
export function uploadContent(filePath, name, type) {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/contents',
      filePath,
      name: 'file',
      formData: { name, type },
      header: {
        'Authorization': `Bearer ${uni.getStorageSync('token')}`
      },
      success: (res) => {
        try { resolve(JSON.parse(res.data)) }
        catch (e) { reject(new Error('解析响应失败')) }
      },
      fail: reject
    })
  })
}

/** 删除内容 */
export function deleteContent(id) {
  return http.delete(`/contents/${id}`)
}

/** 格式化文件大小 */
export function formatFileSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

/** form-urlencoded 请求（适配 @RequestParam 后端） */
function requestForm(url, data) {
  const token = uni.getStorageSync('token')
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method: 'POST',
      header: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Authorization': `Bearer ${token}`
      },
      data: Object.entries(data)
        .filter(([, v]) => v != null)
        .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`)
        .join('&'),
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data)
        } else {
          const msg = (res.data && res.data.message) || '请求失败'
          uni.showToast({ title: msg, icon: 'none' })
          reject(new Error(msg))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}
