<template>
  <view class="container">
    <view class="loading-box" v-if="loading"><text>加载中...</text></view>
    <template v-else-if="detail.id">
      <!-- 类型头部 -->
      <view class="header-card" :class="'header-' + detail.type">
        <view class="header-badge">{{ typeLabel(detail.type) }}</view>
        <view class="header-name">{{ detail.name }}</view>
        <view class="header-size" v-if="detail.fileSize">{{ formatFileSize(detail.fileSize) }}</view>
      </view>

      <!-- 图片/视频预览 -->
      <view class="card" v-if="detail.type === 'image' && detail.filePath">
        <image class="preview-img" :src="imgUrl(detail.filePath)" mode="widthFix" />
      </view>
      <view class="card" v-if="detail.type === 'video' && detail.filePath">
        <video class="preview-video" :src="imgUrl(detail.filePath)" controls />
      </view>

      <!-- 文字内容详情 -->
      <view class="card" v-if="detail.type === 'text'">
        <view class="card-title">文字内容</view>
        <view
          class="text-preview"
          :style="{
            fontSize: (detail.fontSize || 24) + 'rpx',
            color: detail.fontColor || '#333',
            backgroundColor: detail.bgColor || '#fff'
          }"
        >{{ detail.textContent || '-' }}</view>
      </view>

      <!-- 文字样式参数 -->
      <view class="card" v-if="detail.type === 'text'">
        <view class="card-title">样式参数</view>
        <view class="info-row">
          <text class="label">字号</text><text class="value">{{ detail.fontSize || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="label">字体颜色</text>
          <view class="color-swatch" :style="{ background: detail.fontColor || '#333' }"></view>
          <text class="value">{{ detail.fontColor || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="label">背景颜色</text>
          <view class="color-swatch" :style="{ background: detail.bgColor || '#fff' }"></view>
          <text class="value">{{ detail.bgColor || '-' }}</text>
        </view>
        <view class="info-row">
          <text class="label">滚动速度</text><text class="value">{{ detail.scrollSpeed || '-' }}</text>
        </view>
      </view>

      <!-- 文件信息 -->
      <view class="card" v-if="detail.filePath">
        <view class="card-title">文件信息</view>
        <view class="info-row">
          <text class="label">文件大小</text>
          <text class="value">{{ formatFileSize(detail.fileSize) }}</text>
        </view>
        <view class="info-row">
          <text class="label">存储路径</text>
          <text class="value mono">{{ detail.filePath }}</text>
        </view>
      </view>

      <!-- 时间信息 -->
      <view class="card">
        <view class="card-title">时间信息</view>
        <view class="info-row">
          <text class="label">创建时间</text>
          <text class="value">{{ fmt(detail.createdAt) }}</text>
        </view>
        <view class="info-row">
          <text class="label">更新时间</text>
          <text class="value">{{ fmt(detail.updatedAt) }}</text>
        </view>
      </view>

      <!-- 删除按钮 -->
      <view class="btn-area" v-if="isAdmin">
        <button class="btn-danger" @click="handleDelete">删除内容</button>
      </view>
    </template>

    <view class="error-box" v-if="errorMsg">
      <text>{{ errorMsg }}</text>
      <button class="retry-btn" @click="fetchData">重试</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getContentDetail, deleteContent, formatFileSize } from '@/api/contents'
import { UPLOAD_BASE_URL } from '@/config'

const userStore = useUserStore()
const isAdmin = ref(false)
const detail = ref({})
const loading = ref(true)
const errorMsg = ref('')

onLoad((opt) => {
  isAdmin.value = userStore.role === 'ADMIN'
  if (opt.id) fetchData(opt.id)
  else { errorMsg.value = '缺少ID'; loading.value = false }
})

async function fetchData(id) {
  loading.value = true; errorMsg.value = ''
  try {
    const res = await getContentDetail(id || detail.value.id)
    if (res.code === 200) detail.value = res.data || {}
    else errorMsg.value = res.message || '加载失败'
  } catch (e) { errorMsg.value = '加载失败' }
  finally { loading.value = false }
}

function typeLabel(t) {
  return { image: '图片', video: '视频', text: '文字' }[t] || t
}

function imgUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return UPLOAD_BASE_URL + (path.startsWith('/') ? '' : '/') + path
}

function fmt(s) {
  if (!s) return '-'
  return String(s).replace('T', ' ').substring(0, 19)
}

async function handleDelete() {
  const { confirm } = await uni.showModal({
    title: '确认删除', content: `确定删除「${detail.value.name}」吗？`
  })
  if (!confirm) return
  try {
    const res = await deleteContent(detail.value.id)
    if (res.code === 200) {
      uni.showToast({ title: '已删除', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1200)
    }
  } catch (e) { /* toast 已在 request 中显示 */ }
}
</script>

<style lang="scss" scoped>
.container { min-height: 100vh; background: #f5f7fa; padding: 24rpx; }

.header-card {
  border-radius: 20rpx; padding: 36rpx 32rpx; margin-bottom: 24rpx; color: #fff;
  &.header-image { background: linear-gradient(135deg, #4285f4, #1a73e8); }
  &.header-video { background: linear-gradient(135deg, #ea4335, #c62828); }
  &.header-text { background: linear-gradient(135deg, #34a853, #2e7d32); }
}
.header-badge {
  display: inline-block; font-size: 22rpx; padding: 4rpx 20rpx;
  border-radius: 20rpx; background: rgba(255,255,255,0.2); margin-bottom: 12rpx;
}
.header-name { font-size: 36rpx; font-weight: 700; }
.header-size { font-size: 24rpx; opacity: 0.75; margin-top: 6rpx; }

.card { background: #fff; border-radius: 20rpx; padding: 32rpx; margin-bottom: 20rpx; }
.card-title { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 20rpx; padding-bottom: 16rpx; border-bottom: 1rpx solid #f0f0f0; }

.preview-img { width: 100%; border-radius: 12rpx; }
.preview-video { width: 100%; border-radius: 12rpx; }

.text-preview {
  padding: 24rpx; border-radius: 12rpx; min-height: 120rpx;
  word-break: break-all; line-height: 1.6;
}

.info-row {
  display: flex; align-items: center; padding: 14rpx 0;
  &:not(:last-child) { border-bottom: 1rpx solid #f8f8f8; }
}
.label { font-size: 26rpx; color: #888; flex-shrink: 0; }
.value { font-size: 26rpx; color: #333; margin-left: auto; text-align: right; max-width: 60%; word-break: break-all;
  &.mono { font-family: 'Courier New', monospace; font-size: 22rpx; }
}
.color-swatch { width: 32rpx; height: 32rpx; border-radius: 8rpx; margin-left: auto; margin-right: 12rpx; border: 1rpx solid #ddd; }

.btn-area { padding: 32rpx 0; }
.btn-danger {
  width: 100%; height: 88rpx; line-height: 88rpx; background: #e53935; color: #fff;
  font-size: 30rpx; border-radius: 16rpx; text-align: center;
}

.loading-box, .error-box { text-align: center; padding: 120rpx 0; color: #999; font-size: 28rpx; }
.retry-btn { margin-top: 20rpx; width: 200rpx; height: 64rpx; line-height: 64rpx; background: #1a73e8; color: #fff; border-radius: 32rpx; font-size: 26rpx; }
</style>
