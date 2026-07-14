<template>
  <view class="page">
    <!-- 用户信息卡片 -->
    <view class="user-card" v-if="isLoggedIn">
      <view class="avatar">{{ userStore.nickname?.charAt(0) || 'U' }}</view>
      <view class="info">
        <view class="name">{{ userStore.nickname }}</view>
        <view class="role">{{ roleLabel }}</view>
      </view>
    </view>

    <!-- 菜单列表 -->
    <view class="menu-group" v-if="isLoggedIn">
      <view class="menu-item" @tap="showRoleInfo">
        <text class="menu-icon">🛡️</text>
        <text class="menu-text">当前角色</text>
        <text class="menu-value">{{ roleLabel }}</text>
      </view>
      <view class="menu-item" @tap="goAbout">
        <text class="menu-icon">ℹ️</text>
        <text class="menu-text">关于 LED Cloud</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout-section" v-if="isLoggedIn">
      <button class="logout-btn" @tap="handleLogout">退出登录</button>
    </view>

    <!-- 未登录 -->
    <view class="login-section" v-if="!isLoggedIn">
      <view class="logo-area">
        <view class="logo">LED</view>
        <view class="app-name">LED Cloud</view>
        <view class="app-desc">智能LED屏幕管理平台</view>
      </view>
      <button class="wechat-login-btn" @tap="handleLogin">微信一键登录</button>
    </view>

    <!-- 版本号 -->
    <view class="version">v1.0.0</view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isLoggedIn = ref(false)

const roleLabel = computed(() => {
  const map = { ADMIN: '超级管理员', OPERATOR: '操作员', VIEWER: '观察者' }
  return map[userStore.role] || userStore.role
})

onShow(() => {
  userStore.initLoginState()
  isLoggedIn.value = userStore.isLoggedIn
})

async function handleLogin() {
  await userStore.wechatLogin()
  isLoggedIn.value = userStore.isLoggedIn
}

function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定退出登录？',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        isLoggedIn.value = false
        uni.switchTab({ url: '/pages/index/index' })
      }
    }
  })
}

function showRoleInfo() {
  uni.showToast({ title: roleLabel.value, icon: 'none' })
}

function goAbout() {
  uni.showModal({
    title: 'LED Cloud',
    content: '智能LED屏幕内容管理与播放调度平台\n\n版本 1.0.0\n\n支持设备管理、内容编排、节目调度与实时播放控制',
    showCancel: false,
    confirmText: '知道了'
  })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
}
.user-card {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #1a73e8, #4285f4);
  margin: 24rpx;
  padding: 40rpx;
  border-radius: 20rpx;
  .avatar {
    width: 100rpx; height: 100rpx;
    border-radius: 50%;
    background: rgba(255,255,255,0.25);
    color: #fff;
    font-size: 42rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 24rpx;
  }
  .info { color: #fff; }
  .name { font-size: 36rpx; font-weight: 600; }
  .role { font-size: 24rpx; opacity: 0.8; margin-top: 6rpx; }
}
.menu-group {
  margin: 0 24rpx 24rpx;
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx 24rpx;
  border-bottom: 1rpx solid #f5f5f5;
  &:last-child { border-bottom: none; }
  .menu-icon { font-size: 32rpx; margin-right: 20rpx; }
  .menu-text { flex: 1; font-size: 28rpx; color: #333; }
  .menu-value { font-size: 24rpx; color: #1a73e8; margin-right: 10rpx; }
  .arrow { font-size: 28rpx; color: #ccc; }
}
.logout-section {
  margin: 40rpx 24rpx;
  .logout-btn {
    width: 100%; height: 88rpx; line-height: 88rpx;
    background: #fff; color: #ff4444; font-size: 30rpx;
    border-radius: 16rpx; border: 1rpx solid #ffe0e0;
  }
}
.login-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 160rpx;
  .logo-area { text-align: center; margin-bottom: 80rpx; }
  .logo {
    width: 120rpx; height: 120rpx;
    background: linear-gradient(135deg, #1a73e8, #4285f4);
    border-radius: 24rpx;
    color: #fff;
    font-size: 40rpx; font-weight: 700;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 24rpx;
  }
  .app-name { font-size: 40rpx; font-weight: 600; color: #333; }
  .app-desc { font-size: 26rpx; color: #999; margin-top: 12rpx; }
  .wechat-login-btn {
    width: 500rpx; height: 96rpx; line-height: 96rpx;
    background: linear-gradient(135deg, #07c160, #05a84f);
    color: #fff; font-size: 32rpx;
    border-radius: 48rpx; text-align: center;
  }
}
.version {
  text-align: center;
  font-size: 22rpx;
  color: #ccc;
  padding: 40rpx;
}
</style>
