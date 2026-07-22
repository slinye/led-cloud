import http from './request'

/** 获取节目列表 */
export function getProgramList(page = 1, size = 20, keyword = '', silent = false) {
  return http.get('/programs', { page, size, keyword }, silent)
}

/** 获取全部节目 */
export function getAllPrograms(silent = true) {
  return http.get('/programs/all', {}, silent)
}

/** 获取节目详情（含节目项） */
export function getProgramDetail(id) {
  return http.get(`/programs/${id}`)
}

/** 创建节目 */
export function createProgram(data) {
  return http.post('/programs', data)
}

/** 更新节目 */
export function updateProgram(id, data) {
  return http.put(`/programs/${id}`, data)
}

/** 删除节目 */
export function deleteProgram(id) {
  return http.delete(`/programs/${id}`)
}

/** 开始播放 */
export function startPlayback(programId, screenIds) {
  return http.post(`/playback/start/${programId}`, { screenIds })
}

/** 停止播放 */
export function stopPlayback(programId, screenIds) {
  return http.post(`/playback/stop/${programId}`, { screenIds })
}

/** 获取节目发布历史 */
export function getPublishHistory(programId) {
  return http.get(`/programs/${programId}/publishHistory`)
}

/** 屏幕远程控制 */
export function screenControl(screenId, action, brightness) {
  const data = { action }
  if (action === 'brightness') data.brightness = brightness
  return http.post(`/playback/control/${screenId}`, data)
}
