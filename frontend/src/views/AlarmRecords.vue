<template>
  <div class="alarm-records">
    <div class="page-header">
      <h2 class="page-title">
        <el-icon :size="24"><Bell /></el-icon>
        告警记录
      </h2>
      <div class="page-actions">
        <el-select v-model="filterType" placeholder="告警类型" clearable style="width: 140px;" @change="handleSearch">
          <el-option label="离线告警" value="offline" />
          <el-option label="播放失败" value="play_fail" />
          <el-option label="磁盘不足" value="disk" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="读取状态" clearable style="width: 120px; margin-left: 8px;" @change="handleSearch">
          <el-option label="未读" value="unread" />
          <el-option label="已读" value="read" />
        </el-select>
        <el-button type="primary" :icon="Bell" style="margin-left: 8px;" @click="handleMarkAllRead" :disabled="totalUnread === 0">
          全部已读 ({{ totalUnread }})
        </el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%;">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="alarmTypeTag(row.alarmType)" size="small" effect="plain">
              {{ alarmTypeLabel(row.alarmType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" width="150" />
        <el-table-column prop="message" label="详情" min-width="280" show-overflow-tooltip />
        <el-table-column prop="screenName" label="关联设备" width="140" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <span class="status-dot" :class="row.status"></span>
            {{ row.status === 'unread' ? '未读' : '已读' }}
          </template>
        </el-table-column>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'unread'"
              link
              type="primary"
              size="small"
              @click="handleMarkRead(row)"
            >标记已读</el-button>
            <span v-else class="read-text">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: center; margin-top: 16px;">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { getAlarms, markAlarmRead, markAllAlarmsRead } from '../api'

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const totalUnread = ref(0)
const filterType = ref('')
const filterStatus = ref('')

function alarmTypeTag(type) {
  const map = { offline: 'warning', play_fail: 'danger', disk: 'info' }
  return map[type] || 'info'
}

function alarmTypeLabel(type) {
  const map = { offline: '离线告警', play_fail: '播放失败', disk: '磁盘不足' }
  return map[type] || type
}

async function loadData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filterType.value) params.alarmType = filterType.value
    if (filterStatus.value) params.status = filterStatus.value

    const res = await getAlarms(params)
    const data = res && res.code === 200 ? res.data : res
    if (data) {
      tableData.value = data.records || []
      total.value = data.total || 0
      // 计算未读总数
      const unreadItems = tableData.value.filter(r => r.status === 'unread')
      totalUnread.value = data.total > tableData.value.length ? '...' : unreadItems.length
    }
  } catch (e) { /* ignore */ } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  loadData()
}

async function handleMarkRead(row) {
  try {
    await markAlarmRead(row.id)
    row.status = 'read'
  } catch (e) { /* ignore */ }
}

async function handleMarkAllRead() {
  try {
    await markAllAlarmsRead()
    tableData.value.forEach(r => { r.status = 'read' })
    totalUnread.value = 0
  } catch (e) { /* ignore */ }
}

function formatDateTime(time) {
  if (!time) return '-'
  const d = new Date(time)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(loadData)
</script>

<style scoped>
.alarm-records {
  max-width: 1200px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1d2129;
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-actions {
  display: flex;
  align-items: center;
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;
}

.status-dot.unread {
  background: #f53f3f;
}

.status-dot.read {
  background: #c9cdd4;
}

.read-text {
  color: #c9cdd4;
}
</style>
