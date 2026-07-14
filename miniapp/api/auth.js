import http from '../request'

/** 微信小程序登录 */
export function loginByWechat(code, nickName, avatarUrl) {
  return http.post('/client/login', { code, nickName, avatarUrl })
}

/** 验证 Token 有效性 */
export function verifyToken() {
  return http.get('/auth/verify')
}
