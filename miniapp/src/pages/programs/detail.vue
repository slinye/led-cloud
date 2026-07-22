<template>
  <view class="container">
    <view class="loading-box" v-if="loading"><text>加载中...</text></view>
    <template v-else-if="detail.id">
      <!-- 头部 -->
      <view class="header-card">
        <view class="header-badge" :class="'status-' + detail.status">
          {{ statusLabel(detail.status) }}
        </view>
        <view class="header-name">{{ detail.name }}</view>
        <view class="header-desc" v-if="detail.description">{{ detail.description }}</view>
      </view>

      <!-- 节目内容列表 -->
      <view class="card">
        <view class="card-header">
          <view class="card-title">播放内容（{{ items.length }}条）</view>
        </view>
        <view v-if="items.length === 0" class="empty-tip">暂无内容项</view>
        <view class="content-item" v-for="(it, idx) in items" :key="it.id || idx">
          <view class="item-order">{{ idx + 1 }}</view>
          <view class="item-info">
            <view class="item-type" :class="'type-' + (it.contentType || 'text')">
              {{ typeLabel(it.contentType) }}
            </view>
            <view class="item-name">{{ it.contentName || '内容#' + it.contentId }}</view>
          </view>
          <view class="item-duration" v-if="it.duration">{{ it.duration }}s</view>
        </view>
      </view>

      <!-- 播放控制 -->
      <view class="card" v-if="canControl && detail.status !== 'draft'">
        <view class="card-title">播放控制</view>
        <view class="control-row">
          <button class="btn-action btn-success" @click="showScreenPicker('play')">▶ 开始播放</button>
          <button class="btn-action btn-warn" @click="showScreenPicker('stop')">■ 停止播放</button>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="card">
        <view class="card-title">操作</view>
        <view class="control-row">
          <button class="btn-action btn-outline" @click="quickAction('published')" v-if="detail.status==='draft'">
            发布
          </button>
          <button class="btn-action btn-outline" @click="quickAction('draft')" v-if="detail.status==='published'">
            撤回草稿
          </button>
          <button class="btn-action btn-teal" @click="goSchedule">⏰ 定时播放</button>
          <button class="btn-action btn-danger-outline" @click="handleDelete" v-if="isAdmin">
            删除节目
          </button>
        </view>
      </view>

      <!-- 发布历史 -->
      <view class="card" v-if="publishHistory.length > 0">
        <view class="card-title">发布历史（{{ publishHistory.length }}次）</view>
        <view class="history-item" v-for="(h, idx) in publishHistory" :key="h.id">
          <view class="history-header">
            <view class="history-version">V{{ h.version }}</view>
            <view class="history-operator">{{ h.operator || '-' }}</view>
            <view class="history-time">{{ fmt(h.createdAt) }}</view>
          </view>
          <view class="history-snapshot" v-if="parseSnapshot(h.snapshot)">
            <view class="snap-name">{{ parseSnapshot(h.snapshot).name }}</view>
            <view class="snap-items">
              <text v-for="(it, i) in parseSnapshot(h.snapshot).items" :key="i">
                {{ it.contentName || '素材#'+it.contentId }}
                <text class="snap-dur" v-if="it.duration">({{ it.duration }}s)</text>
                <text v-if="i < parseSnapshot(h.snapshot).items.length - 1">, </text>
              </text>
            </view>
          </view>
        </view>
      </view>

      <!-- 屏幕选择弹窗 -->
      <view class="overlay" v-if="pickerVisible" @click="pickerVisible=false">
        <view class="picker-card" @click.stop>
          <view class="picker-title">{{ pickerAction === 'play' ? '选择播放屏幕' : '选择停止屏幕' }}</view>
          <scroll-view class="picker-list" scroll-y>
            <view class="picker-item" v-for="s in screens" :key="s.id"
              :class="{ checked: selectedIds.includes(s.id) }"
              @click="toggleScreen(s.id)"
            >
              <view class="screen-status" :class="s.status"></view>
              <view class="screen-name">{{ s.name }}</view>
              <view class="check-mark" v-if="selectedIds.includes(s.id)">✓</view>
            </view>
            <view class="picker-empty" v-if="screens.length === 0">暂无屏幕设备</view>
          </scroll-view>
          <view class="picker-btns">
            <button class="btn-cancel" @click="pickerVisible=false">取消</button>
            <button class="btn-confirm" @click="confirmPlay" :disabled="selectedIds.length===0">
              确认（{{ selectedIds.length }}台）
            </button>
          </view>
        </view>
      </view>
    </template>

    <view class="error-box" v-if="errorMsg">
      <text>{{ errorMsg }}</text>
      <button class="retry-btn" @click="fetchData">重试</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import {
  getProgramDetail, updateProgram, deleteProgram,
  startPlayback, stopPlayback, getPublishHistory
} from '@/api/programs'
import { getScreenList } from '@/api/screens'

const userStore = useUserStore()
const isAdmin = ref(false)
const canControl = computed(() =>
  ['ADMIN', 'OPERATOR'].includes(userStore.role)
)

const detail = ref({})
const items = ref([])
const publishHistory = ref([])
const loading = ref(true)
const errorMsg = ref('')

// 屏幕选择器
const pickerVisible = ref(false)
const pickerAction = ref('play')
const screens = ref([])
const selectedIds = ref([])

onLoad((opt) => {
  isAdmin.value = userStore.role === 'ADMIN'
  if (opt.id) fetchData(opt.id)
  else { errorMsg.value = '缺少ID'; loading.value = false }
})

async function fetchData(id) {
  loading.value = true; errorMsg.value = ''
  try {
    const res = await getProgramDetail(id || detail.value.id)
    if (res.code === 200) {
      detail.value = res.data || {}
      items.value = res.data.items || []
    } else {
      errorMsg.value = res.message || '加载失败'
    }
    // 独立加载发布历史，失败不影响主数据
    try {
      const hisRes = await getPublishHistory(id || detail.value.id)
      if (hisRes.code === 200) publishHistory.value = hisRes.data || []
    } catch (e) { publishHistory.value = [] }
  } catch (e) { errorMsg.value = '加载失败' }
  finally { loading.value = false }
}

function statusLabel(s) {
  return { draft: '草稿', published: '已发布', playing: '播放中', paused: '已暂停' }[s] || s
}

function typeLabel(t) {
  return { image: '图片', video: '视频', text: '文字' }[t] || '内容'
}

function fmt(s) {
  if (!s) return '-'
  return String(s).replace('T', ' ').substring(0, 19)
}

function parseSnapshot(snap) {
  if (!snap) return null
  try {
    return typeof snap === 'string' ? JSON.parse(snap) : snap
  } catch (e) { return null }
}

async function quickAction(status) {
  try {
    const res = await updateProgram(detail.value.id, {
      name: detail.value.name,
      description: detail.value.description || '',
      status
    })
    if (res.code === 200) {
      detail.value.status = status
      uni.showToast({ title: '操作成功', icon: 'success' })
      // 刷新发布历史
      if (status === 'published') fetchPublishHistory()
    }
  } catch (e) { /* toast */ }
}

async function fetchPublishHistory() {
  try {
    const res = await getPublishHistory(detail.value.id)
    if (res.code === 200) publishHistory.value = res.data || []
  } catch (e) { /* silent */ }
}

async function handleDelete() {
  const { confirm } = await uni.showModal({
    title: '确认删除', content: `确定删除「${detail.value.name}」吗？删除后不可恢复。`
  })
  if (!confirm) return
  try {
    const res = await deleteProgram(detail.value.id)
    if (res.code === 200) {
      uni.showToast({ title: '已删除', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1200)
    }
  } catch (e) { /* toast */ }
}

function goSchedule() {
  uni.navigateTo({ url: '/pages/schedule/add?programId=' + detail.value.id })
}

async function showScreenPicker(action) {
  pickerAction.value = action
  selectedIds.value = []
  pickerVisible.value = true
  try {
    const res = await getScreenList(1, 100, '', true)
    if (res.code === 200) {
      screens.value = (res.data.records || [])
    }
  } catch (e) { screens.value = [] }
}

function toggleScreen(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx > -1) selectedIds.value.splice(idx, 1)
  else selectedIds.value.push(id)
}

async function confirmPlay() {
  if (selectedIds.value.length === 0) return
  try {
    const action = pickerAction.value
    const fn = action === 'play' ? startPlayback : stopPlayback
    const res = await fn(detail.value.id, selectedIds.value)
    if (res.code === 200) {
      uni.showToast({ title: '指令已下发', icon: 'success' })
      pickerVisible.value = false
      detail.value.status = action === 'play' ? 'playing' : 'published'
    }
  } catch (e) { /* toast */ }
}
</script>

<style lang="scss" scoped>
.container { min-height: 100vh; background: #f5f7fa; padding: 24rpx; }

.header-card {
  background: linear-gradient(135deg, #7b1fa2, #4a148c);
  border-radius: 20rpx; padding: 36rpx 32rpx; margin-bottom: 24rpx; color: #fff;
}
.header-badge {
  display: inline-block; font-size: 22rpx; padding: 4rpx 20rpx;
  border-radius: 20rpx; margin-bottom: 12rpx;
  &.status-draft { background: rgba(255,255,255,0.15); }
  &.status-published { background: rgba(255,255,255,0.25); }
  &.status-playing { background: rgba(52,208,88,0.3); }
  &.status-paused { background: rgba(255,152,0,0.3); }
}
.header-name { font-size: 38rpx; font-weight: 700; }
.header-desc { font-size: 26rpx; opacity: 0.8; margin-top: 8rpx; }

.card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 20rpx; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.card-title { font-size: 28rpx; font-weight: 600; color: #333; }
.empty-tip { text-align: center; padding: 40rpx 0; color: #999; font-size: 26rpx; }

.content-item {
  display: flex; align-items: center; padding: 18rpx 0;
  &:not(:last-child) { border-bottom: 1rpx solid #f8f8f8; }
}
.item-order {
  width: 48rpx; height: 48rpx; line-height: 48rpx; text-align: center;
  background: #f0f0f0; border-radius: 50%; font-size: 24rpx; color: #666; margin-right: 16rpx;
}
.item-info { flex: 1; }
.item-type {
  font-size: 20rpx; padding: 2rpx 12rpx; border-radius: 6rpx; display: inline-block; margin-bottom: 4rpx;
  &.type-image { background: #e8f0fe; color: #1a73e8; }
  &.type-video { background: #fce8e6; color: #ea4335; }
  &.type-text { background: #e6f4ea; color: #34a853; }
}
.item-name { font-size: 28rpx; color: #333; }
.item-duration { font-size: 24rpx; color: #ff9800; font-weight: 500; }

.control-row { display: flex; gap: 16rpx; flex-wrap: wrap; }
.btn-action {
  flex: 1; min-width: 200rpx; height: 72rpx; line-height: 72rpx;
  font-size: 26rpx; border-radius: 12rpx; text-align: center;
  &.btn-success { background: #34a853; color: #fff; }
  &.btn-warn { background: #ea4335; color: #fff; }
  &.btn-outline { background: #fff; color: #1a73e8; border: 2rpx solid #1a73e8; }
  &.btn-teal { background: #009688; color: #fff; }
  &.btn-danger-outline { background: #fff; color: #e53935; border: 2rpx solid #e53935; }
}

/* 屏幕选择弹窗 */
.overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.4); z-index: 999;
  display: flex; align-items: flex-end; justify-content: center;
}
.picker-card {
  width: 90vw; max-height: 70vh; background: #fff;
  border-radius: 24rpx 24rpx 0 0; display: flex; flex-direction: column;
}
.picker-title { font-size: 32rpx; font-weight: 600; text-align: center; padding: 28rpx; }
.picker-list { flex: 1; padding: 0 24rpx; max-height: 50vh; }
.picker-item {
  display: flex; align-items: center; padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
  .screen-status { width: 16rpx; height: 16rpx; border-radius: 50%; margin-right: 16rpx;
    &.online { background: #34a853; }
    &.offline { background: #dadce0; }
  }
  .screen-name { flex: 1; font-size: 28rpx; }
  .check-mark { color: #1a73e8; font-size: 32rpx; font-weight: 700; }
  &.checked { background: #e8f0fe; }
}
.picker-empty { text-align: center; padding: 60rpx 0; color: #999; }
.picker-btns {
  display: flex; padding: 20rpx 24rpx; gap: 20rpx;
  border-top: 1rpx solid #f0f0f0;
}
.btn-cancel, .btn-confirm {
  flex: 1; height: 80rpx; line-height: 80rpx; text-align: center;
  border-radius: 12rpx; font-size: 28rpx;
}
.btn-cancel { background: #f5f5f5; color: #666; }
.btn-confirm { background: #1a73e8; color: #fff;
  &[disabled] { opacity: 0.4; }
}

.loading-box, .error-box { text-align: center; padding: 120rpx 0; color: #999; font-size: 28rpx; }
.retry-btn { margin-top: 20rpx; width: 200rpx; height: 64rpx; line-height: 64rpx; background: #1a73e8; color: #fff; border-radius: 32rpx; font-size: 26rpx; }

/* 发布历史 */
.history-item {
  padding: 18rpx 0;
  &:not(:last-child) { border-bottom: 1rpx solid #f0f0f0; }
}
.history-header {
  display: flex; align-items: center; gap: 12rpx;
}
.history-version {
  font-size: 22rpx; background: #e8f0fe; color: #1a73e8;
  padding: 2rpx 14rpx; border-radius: 8rpx; font-weight: 600;
}
.history-operator { font-size: 24rpx; color: #666; }
.history-time { font-size: 22rpx; color: #999; margin-left: auto; }
.history-snapshot { margin-top: 8rpx; padding-left: 8rpx; border-left: 3rpx solid #e0e0e0; }
.snap-name { font-size: 26rpx; color: #333; font-weight: 500; }
.snap-items { font-size: 22rpx; color: #888; margin-top: 4rpx; }
.snap-dur { color: #ff9800; }
</style>
