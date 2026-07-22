import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

const uploadApi = axios.create({
  baseURL: '/api',
  timeout: 600000
})

const requestInterceptor = (config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}

const responseInterceptor = (error) => {
  if (error.response) {
    const { status, data } = error.response
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else if (status === 403) {
      ElMessage.error('权限不足')
    } else {
      ElMessage.error(data?.message || data?.msg || '请求失败')
    }
  } else if (error.code === 'ECONNABORTED') {
    ElMessage.error('请求超时')
  } else {
    ElMessage.error('网络错误')
  }
  return Promise.reject(error)
}

api.interceptors.request.use(requestInterceptor)
api.interceptors.response.use((res) => res.data, responseInterceptor)
uploadApi.interceptors.request.use(requestInterceptor)
uploadApi.interceptors.response.use((res) => res.data, responseInterceptor)

// ============ 认证 ============
export const login = (data) => api.post('/auth/login', data)
export const verifyToken = () => api.get('/auth/verify')
export const changePassword = (data) => api.post('/auth/change-password', data)
export const getCaptcha = () => api.get('/auth/captcha')

// ============ 仪表盘 ============
export const getDashboardSummary = () => api.get('/dashboard/summary')
export const getPlayLogs = (params) => api.get('/dashboard/play-logs', { params })
export const getContentUsage = () => api.get('/dashboard/content-usage')
export const getGroupStatus = () => api.get('/dashboard/group-status')
export const getContentTypeDistribution = () => api.get('/dashboard/content-type-distribution')
export const getPlayTrendToday = () => api.get('/dashboard/play-trend-today')

// ============ 屏幕管理 ============
export const getScreens = (params) => api.get('/screens', { params })
export const getScreensAll = () => api.get('/screens/all')
export const getScreen = (id) => api.get(`/screens/${id}`)
export const createScreen = (data) => api.post('/screens', data)
export const updateScreen = (id, data) => api.put(`/screens/${id}`, data)
export const deleteScreen = (id) => api.delete(`/screens/${id}`)
export const batchDeleteScreens = (ids) => api.post('/screens/batch-delete', { ids })

// ============ 屏幕分组 ============
export const getScreenGroups = (params) => api.get('/screen-groups', { params })
export const createScreenGroup = (data) => api.post('/screen-groups', data)
export const updateScreenGroup = (id, data) => api.put(`/screen-groups/${id}`, data)
export const deleteScreenGroup = (id) => api.delete(`/screen-groups/${id}`)
export const addScreenToGroup = (groupId, data) => api.post(`/screen-groups/${groupId}/screens`, data)
export const getGroupScreens = (groupId) => api.get(`/screen-groups/${groupId}/screens`)
export const getAssignedScreenIds = (excludeGroupId) => api.get('/screen-groups/screens/assigned', { params: { excludeGroupId } })

// ============ 内容管理 ============
export const getContents = (params) => api.get('/contents', { params })
export const getContentsAll = () => api.get('/contents/all')
export const getContent = (id) => api.get(`/contents/${id}`)
export const createContent = (data) => uploadApi.post('/contents', data)
export const updateContent = (id, data) => uploadApi.put(`/contents/${id}`, data)
export const deleteContent = (id) => api.delete(`/contents/${id}`)
export const batchDeleteContents = (ids) => api.post('/contents/batch-delete', { ids })
export const getContentReferences = (id) => api.get(`/contents/${id}/references`)

// ============ 节目管理 ============
export const getPrograms = (params) => api.get('/programs', { params })
export const getProgramsAll = () => api.get('/programs/all')
export const getProgram = (id) => api.get(`/programs/${id}`)
export const createProgram = (data) => api.post('/programs', data)
export const updateProgram = (id, data) => api.put(`/programs/${id}`, data)
export const deleteProgram = (id) => api.delete(`/programs/${id}`)
export const batchDeletePrograms = (ids) => api.post('/programs/batch-delete', { ids })
export const getPublishHistory = (programId) => api.get(`/programs/${programId}/publishHistory`)

// ============ 定时调度 ============
export const getSchedules = () => api.get('/schedules')
export const createSchedule = (data) => api.post('/schedules', data)
export const updateSchedule = (id, data) => api.put(`/schedules/${id}`, data)
export const toggleSchedule = (id) => api.put(`/schedules/${id}/toggle`)
export const deleteSchedule = (id) => api.delete(`/schedules/${id}`)
export const triggerSchedule = (id) => api.post(`/schedules/${id}/trigger`)

// ============ 播放控制 ============
export const startPlayback = (programId, data) => api.post(`/playback/start/${programId}`, data)
export const stopPlayback = (programId, data) => api.post(`/playback/stop/${programId}`, data)
export const controlScreen = (screenId, data) => api.post(`/playback/control/${screenId}`, data)

// ============ 批量操作 ============
export const batchDeployProgram = (data) => api.post('/batch/deploy-program', data)
export const batchPower = (data) => api.post('/batch/power', data)
export const batchBrightness = (data) => api.post('/batch/brightness', data)

// ============ 用户管理 ============
export const getUsers = (params) => api.get('/users', { params })
export const createUser = (data) => api.post('/users', data)
export const updateUser = (id, data) => api.put(`/users/${id}`, data)
export const resetUserPassword = (id, data) => api.put(`/users/${id}/reset-password`, data)
export const deleteUser = (id) => api.delete(`/users/${id}`)
export const getRoles = () => api.get('/users/roles')

// ============ 系统设置 ============
export const getSettings = () => api.get('/settings')
export const updateSettings = (data) => api.put('/settings', data)

// ============ 日志 ============
export const getLogs = (params) => api.get('/logs', { params })
export const exportLogs = (params) => api.get('/logs/export', { params, responseType: 'blob' })

// ============ 审计日志 ============
export const getAuditLogs = (params) => api.get('/audit-logs', { params })

// ============ 告警管理 ============
export const getAlarms = (params) => api.get('/alarms', { params })
export const getAlarmUnreadCount = () => api.get('/alarms/unread-count')
export const getLatestAlarms = (limit = 5) => api.get('/alarms/latest', { params: { limit } })
export const markAlarmRead = (id) => api.put(`/alarms/${id}/read`)
export const markAllAlarmsRead = () => api.put('/alarms/read-all')
