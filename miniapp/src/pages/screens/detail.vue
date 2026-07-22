<template>
  <view class="container">
    <!-- 状态头部 -->
    <view class="header-card">
      <view class="header-status">
        <view class="status-badge" :class="isOnline ? 'online' : 'offline'">
          <view class="status-dot"></view>
          <text>{{ isOnline ? '在线' : '离线' }}</text>
        </view>
      </view>
      <view class="header-name">{{ detail.name || '-' }}</view>
      <view class="header-model" v-if="detail.model">{{ detail.model }}</view>
    </view>

    <!-- 基本信息 -->
    <view class="card">
      <view class="card-title">基本信息</view>
      <view class="info-row">
        <text class="label">屏幕名称</text>
        <text class="value">{{ detail.name || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="label">位置</text>
        <text class="value">{{ detail.location || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="label">设备型号</text>
        <text class="value">{{ detail.model || '-' }}</text>
      </view>
    </view>

    <!-- 显示参数 -->
    <view class="card">
      <view class="card-title">显示参数</view>
      <view class="info-row">
        <text class="label">分辨率</text>
        <text class="value">{{ detail.resolutionWidth || 0 }} × {{ detail.resolutionHeight || 0 }}</text>
      </view>
      <view class="info-row">
        <text class="label">亮度</text>
        <view class="value-row">
          <text class="value">{{ detail.brightness ?? '-' }}{{ detail.brightness != null ? '%' : '' }}</text>
          <view class="brightness-bar">
            <view class="brightness-fill" :style="{ width: (detail.brightness || 0) + '%' }"></view>
          </view>
        </view>
      </view>
    </view>

    <!-- 网络信息 -->
    <view class="card">
      <view class="card-title">网络信息</view>
      <view class="info-row">
        <text class="label">IP 地址</text>
        <text class="value mono">{{ detail.ipAddress || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="label">MQTT 客户端</text>
        <text class="value mono">{{ detail.mqttClientId || '-' }}</text>
      </view>
      <view class="info-row">
        <text class="label">最后心跳</text>
        <text class="value">{{ formatTime(detail.lastHeartbeat) }}</text>
      </view>
    </view>

    <!-- 时间信息 -->
    <view class="card">
      <view class="card-title">时间信息</view>
      <view class="info-row">
        <text class="label">创建时间</text>
        <text class="value">{{ formatTime(detail.createTime) }}</text>
      </view>
      <view class="info-row">
        <text class="label">更新时间</text>
        <text class="value">{{ formatTime(detail.updatedAt) }}</text>
      </view>
    </view>

    <!-- 远程控制 -->
    <view class="card" v-if="canControl && detail.id">
      <view class="card-title">远程控制</view>
      <view class="control-grid">
        <view class="ctrl-btn" @click="doControl('pause')" :class="{ loading: controlLoading==='pause' }">
          <text class="ctrl-icon">⏸</text><text class="ctrl-label">{{ controlLoading==='pause'?'发送中':'暂停' }}</text>
        </view>
        <view class="ctrl-btn" @click="doControl('resume')" :class="{ loading: controlLoading==='resume' }">
          <text class="ctrl-icon">▶</text><text class="ctrl-label">{{ controlLoading==='resume'?'发送中':'恢复' }}</text>
        </view>
        <view class="ctrl-btn" @click="doControl('stop')" :class="{ loading: controlLoading==='stop' }">
          <text class="ctrl-icon">■</text><text class="ctrl-label">{{ controlLoading==='stop'?'发送中':'停止' }}</text>
        </view>
        <view class="ctrl-btn" @click="showBrightness" :class="{ loading: controlLoading==='brightness' }">
          <text class="ctrl-icon">☀</text><text class="ctrl-label">{{ controlLoading==='brightness'?'发送中':'亮度' }}</text>
        </view>
      </view>
    </view>

    <!-- 加载 / 错误 -->
    <view class="loading-box" v-if="loading">
      <text>加载中...</text>
    </view>
    <view class="error-box" v-if="errorMsg">
      <text>{{ errorMsg }}</text>
      <button class="retry-btn" @tap="fetchDetail">重试</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getScreenDetail } from '@/api/screens'
import { screenControl } from '@/api/programs'

const userStore = useUserStore()
const canControl = computed(() =>
  ['ADMIN', 'OPERATOR'].includes(userStore.role)
)

/** 基于心跳时间判断在线，无心跳则离线 */
const isOnline = computed(() => {
  if (!detail.value.lastHeartbeat) return false
  const now = new Date().getTime()
  const hb = new Date(detail.value.lastHeartbeat).getTime()
  return (now - hb) < 60000
})

const detail = ref({})
const loading = ref(true)
const errorMsg = ref('')
const controlLoading = ref('')

onLoad((options) => {
  if (options.id) {
    fetchDetail(options.id)
  } else {
    errorMsg.value = '缺少屏幕ID参数'
    loading.value = false
  }
})

async function fetchDetail(id) {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await getScreenDetail(id)
    if (res.code === 200) {
      detail.value = res.data || {}
    } else {
      errorMsg.value = res.message || '加载失败'
    }
  } catch (e) {
    errorMsg.value = '网络异常，请稍后重试'
  } finally {
    loading.value = false
  }
}

function formatTime(timeStr) {
  if (!timeStr) return '-'
  // 直接去掉 ISO 格式中的 T，截取 yyyy-MM-dd HH:mm:ss
  const cleaned = String(timeStr).replace('T', ' ')
  return cleaned.length >= 19 ? cleaned.substring(0, 19) : cleaned
}

async function doControl(action) {
  if (controlLoading.value) return
  controlLoading.value = action
  try {
    const res = await screenControl(detail.value.id, action)
    if (res.code === 200) {
      uni.showToast({ title: `${label(action)}指令已下发`, icon: 'success' })
    }
  } catch (e) { /* toast */ }
  finally { controlLoading.value = '' }
}

function showBrightness() {
  uni.showActionSheet({
    itemList: ['20%', '40%', '60%', '80%', '100%'],
    success: async (res) => {
      const vals = [20, 40, 60, 80, 100]
      const v = vals[res.tapIndex]
      controlLoading.value = 'brightness'
      try {
        const r = await screenControl(detail.value.id, 'brightness', v)
        if (r.code === 200) {
          uni.showToast({ title: `亮度已设为${v}%`, icon: 'success' })
        }
      } catch (e) { /* toast */ }
      finally { controlLoading.value = '' }
    }
  })
}

function label(action) {
  return { pause: '暂停', resume: '恢复', stop: '停止', brightness: '亮度调节' }[action] || action
}
</script>

<style lang="scss" scoped>
.container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 24rpx;
}

/* —— 状态头部 —— */
.header-card {
  background: linear-gradient(135deg, #1a73e8, #1557b0);
  border-radius: 20rpx;
  padding: 40rpx 32rpx;
  margin-bottom: 24rpx;
  color: #fff;
}
.header-status {
  margin-bottom: 16rpx;
}
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  &.online {
    background: rgba(255, 255, 255, 0.2);
  }
  &.offline {
    background: rgba(255, 255, 255, 0.1);
  }
}
.status-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  margin-right: 10rpx;
  .online & { background: #34d058; box-shadow: 0 0 8rpx rgba(52, 208, 88, 0.6); }
  .offline & { background: rgba(255, 255, 255, 0.4); }
}
.header-name {
  font-size: 40rpx;
  font-weight: 700;
  margin-bottom: 8rpx;
}
.header-model {
  font-size: 26rpx;
  opacity: 0.8;
}

/* —— 信息卡片 —— */
.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
}
.card-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
  margin-bottom: 24rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  &:not(:last-child) {
    border-bottom: 1rpx solid #f8f8f8;
  }
}
.label {
  font-size: 28rpx;
  color: #888;
  flex-shrink: 0;
}
.value {
  font-size: 28rpx;
  color: #333;
  text-align: right;
  max-width: 55%;
  word-break: break-all;
  &.mono {
    font-family: 'Courier New', monospace;
    font-size: 26rpx;
    color: #555;
  }
}
.value-row {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10rpx;
  max-width: 60%;
}
.brightness-bar {
  width: 200rpx;
  height: 10rpx;
  background: #eee;
  border-radius: 5rpx;
  overflow: hidden;
}
.brightness-fill {
  height: 100%;
  background: linear-gradient(90deg, #ffc107, #ff9800);
  border-radius: 5rpx;
  transition: width 0.4s ease;
}

/* —— 远程控制 —— */
.control-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16rpx;
}
.ctrl-btn {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 130rpx; background: #f5f7fa; border-radius: 16rpx;
  &.loading { opacity: 0.5; }
}
.ctrl-icon { font-size: 40rpx; margin-bottom: 8rpx; }
.ctrl-label { font-size: 22rpx; color: #666; }

/* —— 加载 / 错误 —— */
.loading-box, .error-box {
  text-align: center;
  padding: 80rpx 0;
  color: #999;
  font-size: 28rpx;
}
.error-box {
  color: #e53935;
}
.retry-btn {
  margin-top: 24rpx;
  width: 200rpx;
  height: 64rpx;
  line-height: 64rpx;
  font-size: 26rpx;
  color: #fff;
  background: #1a73e8;
  border-radius: 32rpx;
}
</style>
