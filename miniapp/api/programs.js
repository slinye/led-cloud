import http from '../request'

/** 获取节目列表 */
export function getProgramList(page = 1, size = 20, keyword = '') {
  return http.get('/programs', { page, size, keyword })
}

/** 获取节目详情 */
export function getProgramDetail(id) {
  return http.get(`/programs/${id}`)
}

/** 播放控制 */
export function controlPlayback(programId, action) {
  return http.post('/playback/control', { programId, action })
}
