import http from './request'

/** 微信小程序登录 */
export function loginByWechat(code, nickName, avatarUrl) {
  const body = { code }
  if (nickName != null) body.nickName = nickName
  if (avatarUrl != null) body.avatarUrl = avatarUrl
  return http.post('/client/login', body)
}

/** 微信账号绑定PC管理号 */
export function bindAccount(username, password) {
  return http.post('/client/bind', { username, password })
}

/** 验证 Token 有效性 */
export function verifyToken() {
  return http.get('/auth/verify')
}
