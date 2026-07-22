import { defineStore } from 'pinia'
import { loginByWechat, bindAccount } from '../api/auth'

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
      console.log('[Login] 开始微信登录...')
      return new Promise((resolve, reject) => {
        uni.login({
          provider: 'weixin',
          success: async (loginRes) => {
            console.log('[Login] uni.login 成功, code=', loginRes.code ? loginRes.code.substring(0, 8) + '...' : 'empty')
            try {
              const res = await loginByWechat(loginRes.code)
              console.log('[Login] 后端返回:', JSON.stringify(res))
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
                console.log('[Login] 登录成功, role=', role)
                resolve(res.data)
              } else {
                console.error('[Login] 后端返回非200:', res)
                uni.showToast({ title: res.message || '登录失败', icon: 'none' })
                reject(new Error(res.message))
              }
            } catch (e) {
              console.error('[Login] 请求异常:', e.message || e)
              uni.showToast({ title: '登录失败，请重试', icon: 'none' })
              reject(e)
            }
          },
          fail: (err) => {
            console.error('[Login] uni.login 失败:', JSON.stringify(err))
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
        this.role = userInfo.role || 'VIEWER' // 兼容旧缓存（无role字段默认VIEWER）
        this.isLoggedIn = true
        console.log('[Store] initLoginState: role=', this.role)
      }
    },

    /** 微信账号绑定PC管理号 */
    async bindPcAccount(username, password) {
      try {
        const res = await bindAccount(username, password)
        if (res.code === 200) {
          const { token, userId, username: uname, nickname, role } = res.data
          this.token = token
          this.userId = userId
          this.username = uname
          this.nickname = nickname
          this.role = role
          this.isLoggedIn = true

          uni.setStorageSync('token', token)
          uni.setStorageSync('userInfo', { userId, username: uname, nickname, role })
          console.log('[Bind] 绑定成功, role=', role)
          return res.data
        } else {
          throw new Error(res.message)
        }
      } catch (e) {
        console.error('[Bind] 绑定失败:', e.message || e)
        throw e
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
