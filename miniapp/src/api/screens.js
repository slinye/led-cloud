import http from './request'

/** 获取屏幕列表 */
export function getScreenList(page = 1, size = 20, keyword = '', silent = false) {
  return http.get('/screens', { page, size, keyword }, silent)
}

/** 获取屏幕详情 */
export function getScreenDetail(id) {
  return http.get(`/screens/${id}`)
}

/** 获取仪表盘统计 */
export function getDashboard(silent = false) {
  return http.get('/dashboard', {}, silent)
}
