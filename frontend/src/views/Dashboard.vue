<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="16">
      <el-col :span="4" v-for="card in statCards" :key="card.key">
        <el-card shadow="hover" :body-style="{ padding: '20px' }">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: card.color }">
              <el-icon :size="24"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
              <div class="stat-sub" v-if="card.sub">{{ card.sub }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>屏幕状态分布</span>
              <el-tag size="small" type="warning">实时</el-tag>
            </div>
          </template>
          <div ref="statusChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>内容类型分布</span>
            </div>
          </template>
          <div ref="contentTypeChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 今日播放趋势 + 最近日志 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>今日播放趋势</span>
              <el-tag size="small" type="success">每小时</el-tag>
            </div>
          </template>
          <div ref="trendChartRef" style="height: 280px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近播放日志</span>
              <el-button text size="small" type="primary" @click="loadPlayLogs">刷新</el-button>
            </div>
          </template>
          <el-table :data="playLogs" size="small" max-height="280">
            <el-table-column prop="screenName" label="屏幕" min-width="100" show-overflow-tooltip />
            <el-table-column prop="programName" label="节目" min-width="100" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="70">
              <template #default="{ row }">
                <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">{{ row.status === 'success' ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="150">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 自动刷新控制 -->
    <div class="refresh-bar">
      <div class="refresh-left">
        <el-tag :type="connected ? 'success' : 'danger'" size="small" effect="dark">
          {{ connected ? '已连接' : '断开' }}
        </el-tag>
        <span class="refresh-time">最后更新: {{ lastRefreshTime }}</span>
      </div>
      <div class="refresh-right">
        <span>自动刷新：</span>
        <el-select v-model="autoRefresh" size="small" style="width: 100px;">
          <el-option label="5秒" :value="5" />
          <el-option label="10秒" :value="10" />
          <el-option label="30秒" :value="30" />
          <el-option label="60秒" :value="60" />
          <el-option label="关闭" :value="0" />
        </el-select>
        <el-button size="small" :loading="refreshing" @click="refreshAll">
          <el-icon><Refresh /></el-icon>手动刷新
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { Monitor, Connection, VideoPlay, FolderOpened, DataAnalysis, List } from '@element-plus/icons-vue'
import { getDashboardSummary, getPlayLogs, getContentTypeDistribution, getPlayTrendToday } from '../api'
import * as echarts from 'echarts'

// 统计数据
const summary = reactive({
  totalScreens: 0, onlineScreens: 0, offlineScreens: 0, playingScreens: 0,
  totalContents: 0, totalPrograms: 0, totalPlayLogsToday: 0, onlineRate: '0%'
})

const statCards = computed(() => [
  { key: 'screens', icon: Monitor, value: summary.totalScreens, label: '屏幕总数', color: 'linear-gradient(135deg, #667eea, #764ba2)', sub: null },
  { key: 'online', icon: Connection, value: summary.onlineScreens, label: '在线', color: 'linear-gradient(135deg, #67c23a, #85ce61)', sub: summary.onlineRate },
  { key: 'playing', icon: VideoPlay, value: summary.playingScreens, label: '播放中', color: 'linear-gradient(135deg, #e6a23c, #f0ad4e)', sub: null },
  { key: 'contents', icon: FolderOpened, value: summary.totalContents, label: '内容总数', color: 'linear-gradient(135deg, #409eff, #66b1ff)', sub: null },
  { key: 'programs', icon: DataAnalysis, value: summary.totalPrograms, label: '节目总数', color: 'linear-gradient(135deg, #909399, #b4b4b4)', sub: null },
  { key: 'today', icon: List, value: summary.totalPlayLogsToday, label: '今日播放', color: 'linear-gradient(135deg, #f56c6c, #f89898)', sub: '次' }
])

// 播放日志
const playLogs = ref([])
const contentTypeData = ref([])
const playTrendData = ref([])
const refreshing = ref(false)
const connected = ref(true)
const lastRefreshTime = ref('--')
const autoRefresh = ref(10)

// 图表
const statusChartRef = ref(null)
const contentTypeChartRef = ref(null)
const trendChartRef = ref(null)
let statusChart = null, contentTypeChart = null, trendChart = null
let timer = null

function formatTime(dt) {
  if (!dt) return '-'
  const s = String(dt)
  return s.replace('T', ' ').substring(0, 19)
}

async function loadSummary() {
  try {
    const res = await getDashboardSummary()
    const data = res && res.code === 200 ? res.data : res
    if (data) Object.assign(summary, data)
  } catch (e) { /* handled */ }
}

async function loadPlayLogs() {
  try {
    const res = await getPlayLogs({ page: 1, size: 10 })
    const data = res && res.code === 200 ? res.data : res
    playLogs.value = data?.records || data?.data || data || []
  } catch (e) { /* handled */ }
}

async function loadContentTypeDistribution() {
  try {
    const res = await getContentTypeDistribution()
    const data = res && res.code === 200 ? res.data : res
    const list = Array.isArray(data) ? data : (data?.data || [])
    // 映射类型名称为中文
    const typeLabelMap = { image: '图片', video: '视频', text: '文本', audio: '音频', html: '网页' }
    contentTypeData.value = list.map(item => ({
      name: typeLabelMap[item.name] || item.name,
      value: item.value
    }))
  } catch (e) { /* handled */ }
}

async function loadPlayTrend() {
  try {
    const res = await getPlayTrendToday()
    const data = res && res.code === 200 ? res.data : res
    playTrendData.value = Array.isArray(data) ? data : (data?.data || [])
  } catch (e) { /* handled */ }
}

function updateStatusChart() {
  if (!statusChart) return
  statusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie', radius: ['55%', '78%'], center: ['50%', '50%'], avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false }, emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      data: [
        { value: summary.onlineScreens, name: '在线', itemStyle: { color: '#67c23a' } },
        { value: summary.playingScreens, name: '播放中', itemStyle: { color: '#409eff' } },
        { value: Math.max(0, summary.offlineScreens), name: '离线', itemStyle: { color: '#c0c4cc' } }
      ]
    }]
  })
}

function updateContentTypeChart() {
  if (!contentTypeChart) return
  contentTypeChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: contentTypeData.value.length > 0 ? contentTypeData.value : [{ name: '暂无数据', value: 1 }],
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } }
    }]
  })
}

function updateTrendChart() {
  if (!trendChart) return
  const hours = playTrendData.value.map(d => d.hour)
  const counts = playTrendData.value.map(d => d.count)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: hours, axisLabel: { interval: 3 } },
    yAxis: { type: 'value', name: '播放次数' },
    series: [{
      data: counts, type: 'bar', itemStyle: { borderRadius: [4, 4, 0, 0], color: '#409eff' },
      barMaxWidth: 20
    }],
    grid: { left: 50, right: 20, top: 10, bottom: 30 }
  })
}

function initCharts() {
  statusChart = echarts.init(statusChartRef.value)
  contentTypeChart = echarts.init(contentTypeChartRef.value)
  trendChart = echarts.init(trendChartRef.value)
  window.addEventListener('resize', () => {
    statusChart?.resize()
    contentTypeChart?.resize()
    trendChart?.resize()
  })
}

async function refreshAll() {
  refreshing.value = true
  try {
    await Promise.all([loadSummary(), loadPlayLogs(), loadContentTypeDistribution(), loadPlayTrend()])
    updateStatusChart()
    updateContentTypeChart()
    updateTrendChart()
    lastRefreshTime.value = new Date().toLocaleTimeString()
  } finally { refreshing.value = false }
}

// 自动轮询
function startAutoRefresh() {
  stopAutoRefresh()
  if (autoRefresh.value > 0) {
    timer = setInterval(refreshAll, autoRefresh.value * 1000)
  }
}

function stopAutoRefresh() {
  if (timer) { clearInterval(timer); timer = null }
}

watch(autoRefresh, startAutoRefresh)

onMounted(async () => {
  initCharts()
  await refreshAll()
  startAutoRefresh()
})

onBeforeUnmount(() => {
  stopAutoRefresh()
  statusChart?.dispose()
  contentTypeChart?.dispose()
  trendChart?.dispose()
  window.removeEventListener('resize', () => {})
})
</script>

<style scoped>
.dashboard { padding-bottom: 60px; }

.stat-card { display: flex; align-items: center; gap: 14px; }
.stat-icon {
  width: 52px; height: 52px; border-radius: 12px; display: flex;
  align-items: center; justify-content: center; color: #fff; flex-shrink: 0;
}
.stat-info { flex: 1; min-width: 0; }
.stat-value { font-size: 26px; font-weight: 700; color: #303133; line-height: 1.2; }
.stat-label { font-size: 13px; color: #909399; margin-top: 2px; }
.stat-sub { font-size: 12px; color: #606266; }

.card-header { display: flex; align-items: center; justify-content: space-between; }

.refresh-bar {
  position: fixed; bottom: 0; left: 220px; right: 0; height: 48px;
  background: #fff; border-top: 1px solid #e4e7ed; display: flex;
  align-items: center; justify-content: space-between; padding: 0 20px; z-index: 100;
}
.refresh-left, .refresh-right { display: flex; align-items: center; gap: 10px; }
.refresh-time { font-size: 12px; color: #909399; }

.el-row { margin-left: 0 !important; margin-right: 0 !important; }
</style>
