// ==========================================
// API 基础配置 - LED Cloud 微信小程序
// ==========================================
// 所有环境配置集中在 src/config/index.js，上线前只需改那一个文件

import { BASE_URL, TIMEOUT } from '@/config'

/**
 * 统一请求封装
 * @param {Object} options
 * @param {string}  options.url      - 请求路径（不含 BASE_URL）
 * @param {string}  options.method   - 请求方法
 * @param {Object}  options.data     - 请求参数
 * @param {boolean} options.silent   - true 时不弹 toast，调用方自行处理错误
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
          // Token 过期，清除登录状态
          uni.removeStorageSync('token')
          uni.removeStorageSync('userInfo')
          if (!options.silent) {
            uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          }
          reject(new Error('登录已过期'))
          return
        }
        if (statusCode >= 200 && statusCode < 300) {
          resolve(data)
        } else {
          const msg = data.message || '请求失败'
          if (!options.silent) {
            uni.showToast({ title: msg, icon: 'none' })
          }
          reject(new Error(msg))
        }
      },
      fail: (err) => {
        if (!options.silent) {
          uni.showToast({ title: '网络异常，请检查网络连接', icon: 'none' })
        }
        reject(err)
      }
    })
  })
}

export default {
  get(url, data, silent = false) {
    return request({ url, method: 'GET', data, silent })
  },
  post(url, data, silent = false) {
    return request({ url, method: 'POST', data, silent })
  },
  put(url, data, silent = false) {
    return request({ url, method: 'PUT', data, silent })
  },
  delete(url, data, silent = false) {
    return request({ url, method: 'DELETE', data, silent })
  }
}
