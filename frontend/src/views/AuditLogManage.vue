<template>
  <div class="audit-log-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>操作审计日志</span>
        </div>
      </template>

      <!-- 筛选区域 -->
      <el-form :inline="true" :model="query" size="default" class="filter-form">
        <el-form-item label="操作人">
          <el-input v-model="query.username" placeholder="用户名" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="query.module" placeholder="全部模块" clearable style="width:140px">
            <el-option label="认证中心" value="认证中心" />
            <el-option label="屏幕管理" value="屏幕管理" />
            <el-option label="内容管理" value="内容管理" />
            <el-option label="节目管理" value="节目管理" />
            <el-option label="调度中心" value="调度中心" />
            <el-option label="用户管理" value="用户管理" />
            <el-option label="批量操作" value="批量操作" />
            <el-option label="系统设置" value="系统设置" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="query.action" placeholder="全部" clearable style="width:120px">
            <el-option label="新增" value="CREATE" />
            <el-option label="编辑" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
            <el-option label="登录" value="LOGIN" />
            <el-option label="修改密码" value="CHANGE_PWD" />
            <el-option label="发布" value="PUBLISH" />
            <el-option label="控制" value="CONTROL" />
            <el-option label="批量" value="BATCH" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:350px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" border stripe v-loading="loading" style="width:100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="操作人" width="100" />
        <el-table-column prop="module" label="模块" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="actionColor(row.action)">{{ actionLabel(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="costTime" label="耗时" width="80" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.costTime > 3000 ? 'red' : row.costTime > 1000 ? '#e6a23c' : '#67c23a' }">
              {{ row.costTime }}ms
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="responseResult" label="结果" width="100" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" :type="row.responseResult === '成功' ? 'success' : 'danger'">
              {{ row.responseResult }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="详情" width="70" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <span class="total-info">共 {{ total }} 条</span>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="操作详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="ID">{{ detailRow.id }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detailRow.username }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ detailRow.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detailRow.action }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ detailRow.description }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detailRow.ip }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detailRow.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="请求URL">{{ detailRow.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detailRow.costTime }}ms</el-descriptions-item>
        <el-descriptions-item label="结果">{{ detailRow.responseResult }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatTime(detailRow.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="detailRow.requestParams" style="margin-top:12px">
        <strong>请求参数：</strong>
        <pre class="params-pre">{{ detailRow.requestParams }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAuditLogs } from '../api'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref(null)
const detailVisible = ref(false)
const detailRow = ref({})

const query = reactive({
  page: 1,
  size: 20,
  username: '',
  module: '',
  action: ''
})

function actionLabel(action) {
  const map = {
    'LOGIN': '登录', 'LOGOUT': '登出', 'CHANGE_PWD': '改密',
    'CREATE': '新增', 'UPDATE': '编辑', 'DELETE': '删除',
    'PUBLISH': '发布', 'CONTROL': '控制', 'BATCH': '批量',
    'UPLOAD': '上传'
  }
  return map[action] || action
}

function actionColor(action) {
  const map = {
    'LOGIN': '', 'CREATE': 'success', 'UPDATE': 'warning',
    'DELETE': 'danger', 'PUBLISH': '', 'CONTROL': 'warning', 'BATCH': ''
  }
  return map[action] || 'info'
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', { hour12: false })
}

async function loadData() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.username) params.username = query.username
    if (query.module) params.module = query.module
    if (query.action) params.action = query.action
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }
    const res = await getAuditLogs(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function search() {
  query.page = 1
  loadData()
}

function reset() {
  query.username = ''
  query.module = ''
  query.action = ''
  dateRange.value = null
  search()
}

function showDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.audit-log-page {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}

.filter-form {
  margin-bottom: 16px;
}

.pagination-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}

.total-info {
  color: #909399;
  font-size: 13px;
}

.params-pre {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  max-height: 200px;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
