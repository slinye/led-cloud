<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="form.screenName" placeholder="屏幕名称" clearable style="width: 140px;" />
        <el-input v-model="form.programName" placeholder="节目名称" clearable style="width: 140px; margin-left: 8px;" />
        <el-select v-model="form.status" placeholder="播放状态" clearable style="width: 110px; margin-left: 8px;">
          <el-option label="成功" value="success" />
          <el-option label="失败" value="fail" />
        </el-select>
        <el-date-picker
          v-model="form.timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 360px; margin-left: 8px;"
        />
        <el-button-group style="margin-left: 8px;">
          <el-button size="small" @click="quickTime('today')">今天</el-button>
          <el-button size="small" @click="quickTime('yesterday')">昨天</el-button>
          <el-button size="small" @click="quickTime('week')">近7天</el-button>
          <el-button size="small" @click="quickTime('month')">近30天</el-button>
        </el-button-group>
        <el-button type="primary" style="margin-left: 8px;" @click="loadData">查询</el-button>
        <el-button style="margin-left: 8px;" :loading="exporting" @click="handleExport">
          <el-icon><Download /></el-icon>导出Excel
        </el-button>
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <el-table :data="tableData" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="screenName" label="屏幕" min-width="140" show-overflow-tooltip />
        <el-table-column prop="programName" label="节目" min-width="140" show-overflow-tooltip />
        <el-table-column prop="action" label="操作" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">{{ row.status === 'success' ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="详情" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <span class="total-info">共 {{ total }} 条记录</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getLogs, exportLogs } from '../api'

const tableData = ref([])
const loading = ref(false)
const exporting = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const selectedRows = ref([])

const form = reactive({
  screenName: '',
  programName: '',
  status: '',
  timeRange: null
})

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

function quickTime(type) {
  const now = new Date()
  let start = new Date()
  switch (type) {
    case 'today':
      start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
      break
    case 'yesterday':
      start = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1)
      now.setDate(now.getDate() - 1)
      break
    case 'week':
      start.setDate(start.getDate() - 7)
      break
    case 'month':
      start.setDate(start.getDate() - 30)
      break
  }
  const pad = n => String(n).padStart(2, '0')
  const fmt = d => `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  form.timeRange = [fmt(start), fmt(now)]
}

function formatTime(dt) {
  if (!dt) return '-'
  const s = String(dt)
  return s.replace('T', ' ').substring(0, 19)
}

async function loadData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (form.screenName) params.screenName = form.screenName
    if (form.programName) params.programName = form.programName
    if (form.status) params.status = form.status
    if (form.timeRange && form.timeRange[0]) params.startTime = form.timeRange[0]
    if (form.timeRange && form.timeRange[1]) params.endTime = form.timeRange[1]
    const res = await getLogs(params)
    const data = res && res.code === 200 ? res.data : res
    tableData.value = data?.records || data?.data || data || []
    total.value = data?.total || tableData.value.length
  } catch (e) { /* handled */ } finally { loading.value = false }
}

async function handleExport() {
  exporting.value = true
  try {
    const params = {}
    if (form.screenName) params.screenName = form.screenName
    if (form.programName) params.programName = form.programName
    if (form.status) params.status = form.status
    if (form.timeRange && form.timeRange[0]) params.startTime = form.timeRange[0]
    if (form.timeRange && form.timeRange[1]) params.endTime = form.timeRange[1]
    const res = await exportLogs(params)
    const url = window.URL.createObjectURL(new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
    const link = document.createElement('a')
    link.href = url
    link.download = '播放日志_' + new Date().toISOString().slice(0, 10) + '.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally { exporting.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.total-info {
  font-size: 13px;
  color: #909399;
}
</style>
