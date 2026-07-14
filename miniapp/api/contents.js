import http from '../request'

/** 获取内容列表 */
export function getContentList(page = 1, size = 20, keyword = '') {
  return http.get('/contents', { page, size, keyword })
}

/** 获取内容详情 */
export function getContentDetail(id) {
  return http.get(`/contents/${id}`)
}
