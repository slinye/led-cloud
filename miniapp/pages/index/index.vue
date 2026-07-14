<template>
  <view class="page">
    <!-- 顶部用户卡片 -->
    <view class="header-card">
      <view class="header-bg"></view>
      <view class="header-content">
        <view class="user-avatar">{{ userStore.nickname?.charAt(0) || 'U' }}</view>
        <view class="user-info">
          <view class="user-name">{{ userStore.nickname || '未登录' }}</view>
          <view class="user-role">{{ roleLabel }}</view>
        </view>
      </view>
    </view>

    <!-- 数据概览卡片 -->
    <view class="stats-row" v-if="isLoggedIn">
      <StatCard icon="📺" :value="dashboard.screenTotal" label="设备总数" color="#1a73e8" />
      <StatCard icon="📦" :value="dashboard.contentTotal" label="内容总数" color="#34a853" />
      <StatCard icon="🎬" :value="dashboard.programTotal" label="节目总数" color="#ff9800" />
    </view>

    <!-- 登录提示 -->
    <view class="login-tip" v-if="!isLoggedIn">
      <image class="logo-img" src="/static/icons/logo.png" mode="aspectFit" />
      <view class="tip-title">欢迎使用 LED Cloud</view>
      <view class="tip-desc">登录后可查看屏幕状态和管理内容</view>
      <button class="login-btn" @tap="handleLogin">微信一键登录</button>
    </view>

    <!-- 快速入口 -->
    <view class="section" v-if="isLoggedIn">
      <view class="section-title">快捷操作</view>
      <view class="quick-actions">
        <view class="action-item" @tap="navTo('/pages/screens/list')">
          <view class="action-icon bg-blue">📺</view>
          <text>设备管理</text>
        </view>
        <view class="action-item" @tap="navTo('/pages/contents/list')">
          <view class="action-icon bg-green">📦</view>
          <text>内容管理</text>
        </view>
        <view class="action-item" @tap="navTo('/pages/programs/list')">
          <view class="action-icon bg-orange">🎬</view>
          <text>节目编排</text>
        </view>
        <view class="action-item" @tap="navTo('/pages/mine/mine')">
          <view class="action-icon bg-purple">👤</view>
          <text>个人中心</text>
        </view>
      </view>
    </view>

    <!-- 最新设备状态 -->
    <view class="section" v-if="isLoggedIn && screens.length > 0">
      <view class="section-header">
        <view class="section-title">最近设备</view>
        <text class="section-more" @tap="navTo('/pages/screens/list')">全部 ›</text>
      </view>
      <view class="screen-list">
        <view class="screen-item" v-for="item in screens" :key="item.id">
          <view class="screen-status" :class="item.status === 'online' ? 'online' : 'offline'"></view>
          <view class="screen-name">{{ item.name }}</view>
          <view class="screen-location">{{ item.location || '-' }}</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getDashboard } from '@/api/screens'
import { getScreenList } from '@/api/screens'
import StatCard from '@/components/StatCard.vue'

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)

const dashboard = ref({ screenTotal: 0, contentTotal: 0, programTotal: 0 })
const screens = ref([])

const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', OPERATOR: '操作员', VIEWER: '观察者' }
  return map[userStore.role] || userStore.role
})

onShow(() => {
  userStore.initLoginState()
  if (userStore.isLoggedIn) {
    loadData()
  }
})

async function loadData() {
  try {
    const [dashRes, screenRes] = await Promise.all([
      getDashboard().catch(() => ({ data: {} })),
      getScreenList(1, 5).catch(() => ({ data: { records: [] } }))
    ])
    if (dashRes.code === 200) {
      dashboard.value = dashRes.data || {}
    }
    if (screenRes.code === 200 && screenRes.data.records) {
      screens.value = screenRes.data.records
    }
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

async function handleLogin() {
  await userStore.wechatLogin()
  loadData()
}

function navTo(url) {
  uni.switchTab({ url })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
}
.header-card {
  position: relative;
  margin: 24rpx;
  border-radius: 20rpx;
  overflow: hidden;
  .header-bg {
    height: 200rpx;
    background: linear-gradient(135deg, #1a73e8, #4285f4);
  }
  .header-content {
    position: absolute;
    left: 0; top: 0; right: 0; bottom: 0;
    display: flex;
    align-items: center;
    padding: 0 40rpx;
  }
  .user-avatar {
    width: 96rpx; height: 96rpx;
    border-radius: 50%;
    background: rgba(255,255,255,0.25);
    color: #fff;
    font-size: 40rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 24rpx;
  }
  .user-info { color: #fff; }
  .user-name { font-size: 34rpx; font-weight: 600; }
  .user-role { font-size: 24rpx; opacity: 0.8; margin-top: 6rpx; }
}
.stats-row {
  display: flex;
  margin: 0 24rpx 24rpx;
  gap: 16rpx;
}
.login-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 40rpx;
  .logo-img { width: 160rpx; height: 160rpx; margin-bottom: 32rpx; }
  .tip-title { font-size: 36rpx; font-weight: 600; color: #333; margin-bottom: 12rpx; }
  .tip-desc { font-size: 26rpx; color: #999; margin-bottom: 48rpx; }
  .login-btn {
    width: 460rpx; height: 88rpx;
    line-height: 88rpx;
    background: linear-gradient(135deg, #07c160, #05a84f);
    color: #fff;
    font-size: 32rpx;
    border-radius: 44rpx;
    text-align: center;
  }
}
.section {
  margin: 0 24rpx 24rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 20rpx;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
  .section-title { margin-bottom: 0; }
  .section-more { font-size: 24rpx; color: #1a73e8; }
}
.quick-actions {
  display: flex;
  justify-content: space-around;
  .action-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    font-size: 24rpx;
    color: #666;
  }
  .action-icon {
    width: 80rpx; height: 80rpx;
    border-radius: 16rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 36rpx;
    margin-bottom: 10rpx;
  }
  .bg-blue { background: #e8f0fe; }
  .bg-green { background: #e6f4ea; }
  .bg-orange { background: #fef7e0; }
  .bg-purple { background: #f0e6ff; }
}
.screen-list {
  .screen-item {
    display: flex;
    align-items: center;
    padding: 16rpx 0;
    border-bottom: 1rpx solid #f0f0f0;
    &:last-child { border-bottom: none; }
  }
  .screen-status {
    width: 16rpx; height: 16rpx;
    border-radius: 50%;
    margin-right: 16rpx;
    &.online { background: #34a853; }
    &.offline { background: #dadce0; }
  }
  .screen-name { flex: 1; font-size: 28rpx; color: #333; }
  .screen-location { font-size: 24rpx; color: #999; }
}
</style>
