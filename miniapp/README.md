# LED Cloud 微信小程序

## 项目结构

```
miniapp/
├── pages/              # 页面
│   ├── index/          # 首页 - 仪表盘概览
│   ├── screens/        # 屏幕设备列表
│   ├── contents/       # 内容列表
│   ├── programs/       # 节目列表
│   └── mine/           # 个人中心/登录
├── api/                # API 请求
│   ├── request.js      # 请求封装（Token注入、401处理）
│   ├── auth.js         # 认证接口
│   ├── screens.js      # 屏幕/仪表盘接口
│   ├── contents.js     # 内容接口
│   └── programs.js     # 节目/播放控制接口
├── stores/             # Pinia 状态管理
│   └── user.js         # 用户状态（登录/登出/Token管理）
├── components/         # 公共组件
│   └── StatCard.vue    # 统计卡片
├── static/icons/       # TabBar 图标
├── App.vue
├── main.js
├── pages.json
├── manifest.json
└── package.json
```

## 启动步骤

### 1. 安装依赖

```bash
cd miniapp
npm install
```

### 2. 配置微信小程序 AppID

编辑 `manifest.json`：
```json
"mp-weixin": {
  "appid": "你的微信小程序AppID",
  ...
}
```

### 3. 配置 API 地址

编辑 `api/request.js`：
```js
const BASE_URL = 'https://你的域名/api'  // 小程序要求 HTTPS
```

### 4. 准备 TabBar 图标

在 `static/icons/` 目录放置以下 40x40 的 PNG 图标：
- `home.png` / `home-active.png`
- `screen.png` / `screen-active.png`
- `content.png` / `content-active.png`
- `program.png` / `program-active.png`
- `mine.png` / `mine-active.png`

### 5. 开发调试

```bash
npm run dev:mp-weixin
```
然后用微信开发者工具打开 `dist/dev/mp-weixin` 目录。

### 6. 生产构建

```bash
npm run build:mp-weixin
```
提交 `dist/build/mp-weixin` 到微信公众平台。

## 后端配置

### 数据库迁移

执行 SQL 迁移脚本：
```sql
-- 在 mysql 中执行
USE led_management;
ALTER TABLE t_user ADD COLUMN wechat_openid VARCHAR(100) NULL UNIQUE AFTER status;
```

### 环境变量

在 `led-auth` 服务配置中添加微信小程序密钥（到微信公众平台获取）：
```yaml
wechat:
  miniapp:
    appid: ${WECHAT_APPID:}     # 你的微信小程序 AppID
    secret: ${WECHAT_SECRET:}   # 你的微信小程序 AppSecret
```

Docker 部署时在 docker-compose 中添加：
```yaml
led-auth:
  environment:
    - WECHAT_APPID=wx你的appid
    - WECHAT_SECRET=你的appsecret
```

## 登录流程

```
小程序 wx.login() → 拿到 code
  → POST /api/client/login { code, nickName, avatarUrl }
  → 后端调用微信 code2session 换 openid
  → 查找/创建用户，签发 JWT
  → 返回 { token, userId, username, nickname, role }
  → 小程序存储 token，后续请求带 Authorization: Bearer <token>
```
