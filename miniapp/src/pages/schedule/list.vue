<template>
  <view class="page">
    <!-- 支持刷新 -->
    <scroll-view class="scroll-area" scroll-y refresher-enabled
      :refresher-triggered="refreshing" @refresherrefresh="onRefresh">

      <view class="empty" v-if="list.length === 0 && !loading">
        <text class="empty-icon">&#x23F0;</text>
        <text class="empty-text">暂无定时播放</text>
        <text class="empty-hint">点击右下角按钮添加定时播放</text>
      </view>

      <view class="schedule-item" v-for="item in list" :key="item.id">
        <view class="item-header">
          <view class="item-status" :class="item.enabled === 1 ? 'enabled' : 'disabled'">
            {{ item.enabled === 1 ? '运行中' : '已停用' }}
          </view>
          <text class="item-program">{{ item.programName }}</text>
        </view>

        <view class="item-body">
          <view class="info-row">
            <text class="label">目标屏幕</text>
            <text class="value">{{ item.screenName || '设备' + item.screenId }}</text>
          </view>
          <view class="info-row">
            <text class="label">重复模式</text>
            <text class="value">{{ cronLabel(item.cronExpression) }}</text>
          </view>
          <view class="info-row">
            <text class="label">开始时间</text>
            <text class="value">{{ fmt(item.startTime) }}</text>
          </view>
          <view class="info-row" v-if="item.endTime">
            <text class="label">结束时间</text>
            <text class="value">{{ fmt(item.endTime) }}</text>
          </view>
          <view class="info-row">
            <text class="label">下次执行</text>
            <text class="value highlight">{{ fmt(item.nextRunTime) }}</text>
          </view>
          <view class="info-row" v-if="item.lastRunTime">
            <text class="label">上次执行</text>
            <text class="value">{{ fmt(item.lastRunTime) }}</text>
          </view>
        </view>

        <view class="item-actions">
          <view class="btn" @click="handleToggle(item)">
            {{ item.enabled === 1 ? '停用' : '启用' }}
          </view>
          <view class="btn primary" @click="handleTrigger(item)">立即触发</view>
          <view class="btn danger" @click="handleDelete(item)">删除</view>
        </view>
      </view>

      <view class="loading" v-if="loading">加载中…</view>
    </scroll-view>

    <!-- 新建按钮 -->
    <view class="fab" @click="goAdd">
      <text class="fab-icon">+</text>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getScheduleList, toggleSchedule, deleteSchedule, triggerSchedule } from '@/api/schedule'

const list = ref([])
const loading = ref(true)
const refreshing = ref(false)

onShow(() => { fetchList() })

async function fetchList() {
  loading.value = true
  try {
    const res = await getScheduleList()
    if (res.code === 200) list.value = res.data || []
  } catch (e) { /* toast */ }
  finally {
    loading.value = false
    refreshing.value = false
  }
}

function onRefresh() {
  refreshing.value = true
  fetchList()
}

function goAdd() {
  uni.navigateTo({ url: '/pages/schedule/add' })
}

async function handleToggle(item) {
  try {
    const res = await toggleSchedule(item.id)
    if (res.code === 200) {
      uni.showToast({ title: res.msg || '操作成功', icon: 'success' })
      fetchList()
    }
  } catch (e) { /* toast */ }
}

async function handleTrigger(item) {
  try {
    const res = await triggerSchedule(item.id)
    if (res.code === 200) {
      uni.showToast({ title: '已触发', icon: 'success' })
    }
  } catch (e) { /* toast */ }
}

async function handleDelete(item) {
  uni.showModal({
    title: '删除定时',
    content: `确定删除定时"${item.programName}"吗？`,
    success: async (res2) => {
      if (res2.confirm) {
        try {
          const delRes = await deleteSchedule(item.id)
          if (delRes.code === 200) {
            uni.showToast({ title: '已删除', icon: 'success' })
            fetchList()
          }
        } catch (e) { /* toast */ }
      }
    }
  })
}

function pad(id) {
  return String(id || '001').padStart(3, '0')
}

function fmt(t) {
  if (!t) return '-'
  return String(t).replace('T', ' ').substring(0, 19)
}

function cronLabel(cron) {
  if (!cron) return '一次性'
  if (cron.startsWith('daily:')) return `每天 ${cron.substring(6)}`
  if (cron.startsWith('weekly:')) {
    const parts = cron.substring(7).split(',')
    const dayMap = { MON: '周一', TUE: '周二', WED: '周三', THU: '周四', FRI: '周五', SAT: '周六', SUN: '周日' }
    const day = dayMap[parts[0]] || parts[0]
    const time = parts.length > 1 ? parts[1] : '00:00'
    return `每${day} ${time}`
  }
  if (cron.startsWith('hourly:')) return `每${cron.substring(7)}小时`
  return cron
}
</script>

<style scoped>
.page { height: 100vh; display: flex; flex-direction: column; background: #f5f7fa; }
.scroll-area { flex: 1; padding: 20rpx; padding-bottom: 120rpx; }

/* 空状态 */
.empty { text-align: center; padding: 160rpx 40rpx; }
.empty-icon { font-size: 100rpx; display: block; margin-bottom: 20rpx; }
.empty-text { font-size: 30rpx; color: #999; display: block; }
.empty-hint { font-size: 24rpx; color: #ccc; margin-top: 10rpx; display: block; }

/* 卡片 */
.schedule-item { background: #fff; border-radius: 20rpx; padding: 30rpx; margin-bottom: 24rpx; box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.06); }
.item-header { display: flex; align-items: center; margin-bottom: 20rpx; }
.item-status { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 20rpx; margin-right: 16rpx; }
.item-status.enabled { background: #e8f5e9; color: #2e7d32; }
.item-status.disabled { background: #f5f5f5; color: #999; }
.item-program { font-size: 32rpx; font-weight: 600; color: #333; flex: 1; }

.item-body { padding: 16rpx 0; border-top: 1rpx solid #f0f0f0; border-bottom: 1rpx solid #f0f0f0; }
.info-row { display: flex; justify-content: space-between; padding: 10rpx 0; }
.label { font-size: 26rpx; color: #999; }
.value { font-size: 26rpx; color: #333; }
.value.highlight { color: #1a73e8; font-weight: 600; }

.item-actions { display: flex; gap: 16rpx; margin-top: 20rpx; }
.btn { flex: 1; text-align: center; padding: 14rpx 0; font-size: 24rpx; border-radius: 12rpx; background: #f0f0f0; color: #666; }
.btn.primary { background: #1a73e8; color: #fff; }
.btn.danger { background: #ffebee; color: #e53935; }

.loading { text-align: center; padding: 40rpx; color: #999; }

.fab {
  position: fixed; bottom: 60rpx; right: 40rpx;
  width: 110rpx; height: 110rpx; border-radius: 50%;
  background: linear-gradient(135deg, #1a73e8, #1557b0);
  color: #fff; display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(26,115,232,0.4);
}
.fab-icon { font-size: 56rpx; font-weight: 300; line-height: 1; }
</style>
