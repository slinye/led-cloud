<template>
  <view class="page">
    <!-- 节目选择 -->
    <view class="section">
      <view class="section-title">选择节目</view>
      <picker class="picker" mode="selector" :range="programNames" @change="onProgramChange">
        <view class="picker-value" :class="{ placeholder: !form.programId }">
          {{ selectedProgramName || '点击选择节目' }}
        </view>
      </picker>
    </view>

    <!-- 屏幕选择 -->
    <view class="section">
      <view class="section-title">目标屏幕</view>
      <picker class="picker" mode="selector" :range="screenNames" @change="onScreenChange">
        <view class="picker-value" :class="{ placeholder: !form.screenId }">
          {{ selectedScreenName || '点击选择屏幕' }}
        </view>
      </picker>
    </view>

    <!-- 重复模式 -->
    <view class="section">
      <view class="section-title">重复模式</view>
      <view class="radio-group">
        <view class="radio-item" :class="{ active: repeatMode === 'once' }" @click="repeatMode = 'once'">一次性</view>
        <view class="radio-item" :class="{ active: repeatMode === 'daily' }" @click="repeatMode = 'daily'">每天</view>
        <view class="radio-item" :class="{ active: repeatMode === 'weekly' }" @click="repeatMode = 'weekly'">每周</view>
        <view class="radio-item" :class="{ active: repeatMode === 'hourly' }" @click="repeatMode = 'hourly'">每小时</view>
      </view>
    </view>

    <!-- 时间选择 -->
    <view class="section" v-if="repeatMode !== 'once'">
      <view class="section-title">执行时间</view>
      <picker mode="time" :value="execTime" @change="onTimeChange">
        <view class="picker-value">{{ execTime || '00:00' }}</view>
      </picker>
    </view>

    <!-- 每周选择 -->
    <view class="section" v-if="repeatMode === 'weekly'">
      <view class="section-title">星期</view>
      <view class="day-group">
        <view class="day-item" v-for="d in days" :key="d.value"
          :class="{ active: weeklyDay === d.value }" @click="weeklyDay = d.value">
          {{ d.label }}
        </view>
      </view>
    </view>

    <!-- 每小时间隔 -->
    <view class="section" v-if="repeatMode === 'hourly'">
      <view class="section-title">间隔小时数</view>
      <view class="num-stepper">
        <view class="stepper-btn" @click="hourlyInterval > 1 && hourlyInterval--">-</view>
        <text class="stepper-val">{{ hourlyInterval }}</text>
        <view class="stepper-btn" @click="hourlyInterval < 24 && hourlyInterval++">+</view>
      </view>
    </view>

    <!-- 开始时间 -->
    <view class="section">
      <view class="section-title">开始日期时间</view>
      <picker mode="multiSelector" :range="startTimeRange" :value="startTimeIdx" @change="onStartChange">
        <view class="picker-value">{{ form.startTime || '现在' }}</view>
      </picker>
    </view>

    <!-- 结束时间 -->
    <view class="section" v-if="repeatMode !== 'once'">
      <view class="section-title">结束时间（可选）</view>
      <picker mode="multiSelector" :range="endTimeRange" :value="endTimeIdx" @change="onEndChange">
        <view class="picker-value">{{ form.endTime || '不限' }}</view>
      </picker>
    </view>

    <!-- 提交 -->
    <view class="submit-bar">
      <view class="submit-btn" @click="handleSubmit">创建定时播放</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getAllPrograms } from '@/api/programs'
import { getScreenList } from '@/api/screens'
import { createSchedule } from '@/api/schedule'

const programs = ref([])
const screens = ref([])

const repeatMode = ref('once')
const execTime = ref('08:00')
const weeklyDay = ref('MON')
const hourlyInterval = ref(1)

const form = ref({ programId: null, screenId: null, cronExpression: '', startTime: '', endTime: '' })

const days = [
  { value: 'MON', label: '一' }, { value: 'TUE', label: '二' },
  { value: 'WED', label: '三' }, { value: 'THU', label: '四' },
  { value: 'FRI', label: '五' }, { value: 'SAT', label: '六' }, { value: 'SUN', label: '日' }
]

const programNames = computed(() => programs.value.map(p => p.name))
const selectedProgramName = computed(() => {
  const idx = programs.value.findIndex(p => p.id === form.value.programId)
  return idx >= 0 ? programs.value[idx].name : ''
})
const screenNames = computed(() => screens.value.map(s => s.name || ('屏幕#' + s.id)))
const selectedScreenName = computed(() => {
  const idx = screens.value.findIndex(s => s.id === form.value.screenId)
  return idx >= 0 ? (screens.value[idx].name || ('屏幕#' + screens.value[idx].id)) : ''
})

function onProgramChange(e) {
  form.value.programId = programs.value[e.detail.value].id
}
function onScreenChange(e) {
  form.value.screenId = screens.value[e.detail.value].id
}
function onTimeChange(e) {
  execTime.value = e.detail.value
}

// 时间选择器列
const startTimeRange = computed(() => {
  const n = new Date()
  const years = [n.getFullYear(), n.getFullYear() + 1]
  const months = Array.from({ length: 12 }, (_, i) => i + 1)
  const days = Array.from({ length: 31 }, (_, i) => i + 1)
  const hours = Array.from({ length: 24 }, (_, i) => i)
  const mins = Array.from({ length: 60 }, (_, i) => i)
  return [years, months, days, hours, mins]
})
const startTimeIdx = ref([0, 0, 0, 8, 0])
const endTimeIdx = ref([0, 0, 0, 8, 0])

function formatDatePicker(idxArr) {
  const range = startTimeRange.value
  const y = range[0][idxArr[0]]
  const m = String(range[1][idxArr[1]]).padStart(2, '0')
  const d = String(range[2][idxArr[2]]).padStart(2, '0')
  const h = String(range[3][idxArr[3]]).padStart(2, '0')
  const min = String(range[4][idxArr[4]]).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}:00`
}

function formatNow() {
  const n = new Date()
  const y = n.getFullYear()
  const m = String(n.getMonth() + 1).padStart(2, '0')
  const d = String(n.getDate()).padStart(2, '0')
  const h = String(n.getHours()).padStart(2, '0')
  const min = String(n.getMinutes()).padStart(2, '0')
  const sec = String(n.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}:${sec}`
}

const endTimeRange = computed(() => startTimeRange.value)
function onStartChange(e) {
  startTimeIdx.value = e.detail.value
  form.value.startTime = formatDatePicker(e.detail.value)
}
function onEndChange(e) {
  endTimeIdx.value = e.detail.value
  form.value.endTime = formatDatePicker(e.detail.value)
}

function buildCron() {
  switch (repeatMode.value) {
    case 'daily': return `daily:${execTime.value}`
    case 'weekly': return `weekly:${weeklyDay.value},${execTime.value}`
    case 'hourly': return `hourly:${hourlyInterval.value}`
    default: return ''
  }
}

async function handleSubmit() {
  if (!form.value.programId) return uni.showToast({ title: '请选择节目', icon: 'none' })
  if (!form.value.screenId) return uni.showToast({ title: '请选择屏幕', icon: 'none' })

  const data = {
    programId: form.value.programId,
    screenId: form.value.screenId,
    cronExpression: buildCron(),
    startTime: form.value.startTime || formatNow(),
    endTime: form.value.endTime || null
  }
  try {
    const res = await createSchedule(data)
    if (res.code === 200) {
      uni.showToast({ title: '创建成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1000)
    }
  } catch (e) { /* toast */ }
}

onLoad((options) => {
  if (options.programId) {
    form.value.programId = Number(options.programId)
  }
})

onMounted(async () => {
  try {
    const [pr, sr] = await Promise.all([
      getAllPrograms(),
      getScreenList(1, 100, '', true).catch(() => ({ data: { records: [] } }))
    ])
    programs.value = pr.data || []
    // screen list is paginated
    const screenResult = sr.data
    screens.value = Array.isArray(screenResult) ? screenResult : (screenResult?.records || [])
  } catch (e) { /* toast */ }
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; padding-bottom: 140rpx; }
.section { margin: 24rpx 30rpx; background: #fff; border-radius: 16rpx; padding: 30rpx; }
.section-title { font-size: 26rpx; color: #999; margin-bottom: 16rpx; }
.picker { border: 1rpx solid #e0e0e0; border-radius: 12rpx; padding: 20rpx 24rpx; }
.picker-value { font-size: 28rpx; color: #333; }
.picker-value.placeholder { color: #bbb; }

.radio-group { display: flex; gap: 16rpx; }
.radio-item { flex: 1; text-align: center; padding: 16rpx 0; font-size: 26rpx; border-radius: 12rpx; background: #f0f0f0; color: #666; }
.radio-item.active { background: #1a73e8; color: #fff; }

.day-group { display: flex; gap: 12rpx; }
.day-item { width: 72rpx; height: 72rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24rpx; background: #f0f0f0; color: #666; }
.day-item.active { background: #1a73e8; color: #fff; }

.num-stepper { display: flex; align-items: center; justify-content: center; gap: 32rpx; }
.stepper-btn { width: 60rpx; height: 60rpx; border-radius: 50%; background: #f0f0f0; display: flex; align-items: center; justify-content: center; font-size: 36rpx; color: #333; }
.stepper-val { font-size: 40rpx; font-weight: 600; color: #1a73e8; min-width: 60rpx; text-align: center; }

.submit-bar { position: fixed; bottom: 0; left: 0; right: 0; padding: 20rpx 30rpx; padding-bottom: 40rpx; background: #fff; box-shadow: 0 -2rpx 16rpx rgba(0,0,0,0.06); }
.submit-btn { background: linear-gradient(135deg, #1a73e8, #1557b0); color: #fff; text-align: center; padding: 28rpx 0; border-radius: 16rpx; font-size: 32rpx; font-weight: 600; }
</style>
