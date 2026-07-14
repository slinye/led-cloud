<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409eff;"><el-icon :size="28"><Monitor /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ summary.totalScreens || 0 }}</div>
              <div class="stat-label">屏幕总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67c23a;"><el-icon :size="28"><Connection /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ summary.onlineScreens || 0 }}</div>
              <div class="stat-label">在线屏幕</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6a23c;"><el-icon :size="28"><FolderOpened /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ summary.totalContents || 0 }}</div>
              <div class="stat-label">内容总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f56c6c;"><el-icon :size="28"><VideoPlay /></el-icon></div>
            <div class="stat-info">
              <div class="stat-value">{{ summary.totalPrograms || 0 }}</div>
              <div class="stat-label">节目总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header><span>屏幕状态分布</span></template>
          <div ref="statusChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header><span>最近播放日志</span></template>
          <el-table :data="recentLogs" size="small" max-height="300px">
            <el-table-column prop="screenName" label="屏幕" show-overflow-tooltip />
            <el-table-column prop="programName" label="节目" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="时间" width="160" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboardSummary, getPlayLogs } from '../api'

const summary = ref({})
const recentLogs = ref([])
const statusChartRef = ref(null)
let chart = null

async function loadData() {
  try {
    const res = await getDashboardSummary()
    if (res && res.code === 200) {
      summary.value = res.data || res
    } else {
      summary.value = res || {}
    }
  } catch (e) { /* ignore */ }

  try {
    const logRes = await getPlayLogs({ page: 1, size: 10 })
    const list = (logRes && logRes.code === 200) ? (logRes.data?.records || logRes.data || []) : (logRes?.data?.records || [])
    recentLogs.value = Array.isArray(list) ? list : []
  } catch (e) { /* ignore */ }
}

function initChart() {
  if (!statusChartRef.value) return
  chart = echarts.init(statusChartRef.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {c}' },
      emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
      data: [
        { value: summary.value.onlineScreens || 0, name: '在线', itemStyle: { color: '#67c23a' } },
        { value: summary.value.offlineScreens || 0, name: '离线', itemStyle: { color: '#c0c4cc' } },
        { value: summary.value.activeScreens || 0, name: '播放中', itemStyle: { color: '#409eff' } }
      ]
    }]
  })
}

onMounted(async () => {
  await loadData()
  await nextTick()
  initChart()
})

onUnmounted(() => {
  chart?.dispose()
})
</script>

<style scoped>
.stats-row { margin-bottom: 8px; }

.stat-card .stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 2px;
}
</style>
