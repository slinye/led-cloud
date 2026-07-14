// ==========================================
// API 基础配置 - LED Cloud 微信小程序
// ==========================================

// 开发环境 API 地址（小程序上线前改为生产域名）
const BASE_URL = 'http://localhost:82/api'

// 请求超时（毫秒）
const TIMEOUT = 15000

/**
 * 统一请求封装
 */
export function request(options = {}) {
  const token = uni.getStorageSync('token')

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      timeout: TIMEOUT,
      success: (res) => {
        const { statusCode, data } = res
        if (statusCode === 401) {
          // Token 过期，跳转登录
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          uni.reLaunch({ url: '/pages/mine/mine' })
          reject(new Error('登录已过期'))
          return
        }
        if (statusCode >= 200 && statusCode < 300) {
          resolve(data)
        } else {
          reject(new Error(data.message || '请求失败'))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

export default {
  get(url, data) {
    return request({ url, method: 'GET', data })
  },
  post(url, data) {
    return request({ url, method: 'POST', data })
  },
  put(url, data) {
    return request({ url, method: 'PUT', data })
  },
  delete(url, data) {
    return request({ url, method: 'DELETE', data })
  }
}
