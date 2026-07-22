import http from './request'

/** 获取所有定时 */
export function getScheduleList() {
  return http.get('/schedules')
}

/** 创建定时 */
export function createSchedule(data) {
  return http.post('/schedules', data)
}

/** 更新定时 */
export function updateSchedule(id, data) {
  return http.put(`/schedules/${id}`, data)
}

/** 启用/停用 */
export function toggleSchedule(id) {
  return http.put(`/schedules/${id}/toggle`)
}

/** 删除定时 */
export function deleteSchedule(id) {
  return http.delete(`/schedules/${id}`)
}

/** 手动触发 */
export function triggerSchedule(id) {
  return http.post(`/schedules/${id}/trigger`)
}
