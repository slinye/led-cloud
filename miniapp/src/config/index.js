// ==========================================
// LED Cloud 小程序 - 统一配置
// ==========================================
// 上线前只需修改此文件中的生产地址即可

// 开发环境
const DEV = {
  BASE_URL: 'http://localhost:9100/api',
  UPLOAD_BASE_URL: 'http://localhost:9100'
}

// 生产环境
const PROD = {
  BASE_URL: 'https://your-domain.com/api',
  UPLOAD_BASE_URL: 'https://your-domain.com'
}

// 根据编译模式自动切换：npm run dev:mp-weixin → DEV，npm run build:mp-weixin → PROD
const isProd = process.env.NODE_ENV === 'production'

const env = isProd ? PROD : DEV

// API 基础地址（含 /api 后缀）
export const BASE_URL = env.BASE_URL

// 文件上传/静态资源基础地址（不含 /api）
export const UPLOAD_BASE_URL = env.UPLOAD_BASE_URL

// 请求超时（毫秒）
export const TIMEOUT = 8000

export default { BASE_URL, UPLOAD_BASE_URL, TIMEOUT }
