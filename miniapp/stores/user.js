import { defineStore } from 'pinia'
import { loginByWechat } from '../api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: '',
    userId: null,
    username: '',
    nickname: '',
    role: '',
    isLoggedIn: false
  }),

  actions: {
    /** 微信一键登录 */
    async wechatLogin() {
      return new Promise((resolve, reject) => {
        uni.login({
          provider: 'weixin',
          success: async (loginRes) => {
            try {
              // 获取用户信息（新版需要用户点击按钮授权）
              const res = await loginByWechat(loginRes.code)
              if (res.code === 200) {
                const { token, userId, username, nickname, role } = res.data
                this.token = token
                this.userId = userId
                this.username = username
                this.nickname = nickname
                this.role = role
                this.isLoggedIn = true

                uni.setStorageSync('token', token)
                uni.setStorageSync('userInfo', { userId, username, nickname, role })
                resolve(res.data)
              } else {
                uni.showToast({ title: res.message || '登录失败', icon: 'none' })
                reject(new Error(res.message))
              }
            } catch (e) {
              uni.showToast({ title: '登录失败，请重试', icon: 'none' })
              reject(e)
            }
          },
          fail: (err) => {
            uni.showToast({ title: '微信登录失败', icon: 'none' })
            reject(err)
          }
        })
      })
    },

    /** 初始化登录状态（从本地存储恢复） */
    initLoginState() {
      const token = uni.getStorageSync('token')
      const userInfo = uni.getStorageSync('userInfo')
      if (token && userInfo) {
        this.token = token
        this.userId = userInfo.userId
        this.username = userInfo.username
        this.nickname = userInfo.nickname
        this.role = userInfo.role
        this.isLoggedIn = true
      }
    },

    /** 退出登录 */
    logout() {
      this.token = ''
      this.userId = null
      this.username = ''
      this.nickname = ''
      this.role = ''
      this.isLoggedIn = false
      uni.removeStorageSync('token')
      uni.removeStorageSync('userInfo')
    }
  }
})
