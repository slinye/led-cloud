<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="form.screenName" placeholder="屏幕名称" clearable style="width: 160px;" />
        <el-input v-model="form.programName" placeholder="节目名称" clearable style="width: 160px; margin-left: 8px;" />
        <el-select v-model="form.status" placeholder="播放状态" clearable style="width: 130px; margin-left: 8px;">
          <el-option label="成功" value="success" />
          <el-option label="失败" value="fail" />
        </el-select>
        <el-button type="primary" style="margin-left: 8px;" @click="loadData">查询</el-button>
        <el-button style="margin-left: 8px;" @click="handleExport">导出</el-button>
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="screenName" label="屏幕" min-width="140" show-overflow-tooltip />
        <el-table-column prop="programName" label="节目" min-width="140" show-overflow-tooltip />
        <el-table-column prop="action" label="操作" width="90" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="详情" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>

      <div class="pagination-wrap">
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
import { ElMessage } from 'element-plus'
import { getLogs, exportLogs } from '../api'

const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const form = reactive({
  screenName: '',
  programName: '',
  status: ''
})

async function loadData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (form.screenName) params.screenName = form.screenName
    if (form.programName) params.programName = form.programName
    if (form.status) params.status = form.status
    const res = await getLogs(params)
    const data = res && res.code === 200 ? res.data : res
    tableData.value = data?.records || data?.data || data || []
    total.value = data?.total || tableData.value.length
  } catch (e) { /* handled */ } finally { loading.value = false }
}

async function handleExport() {
  try {
    const res = await exportLogs({})
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.download = '播放日志.xlsx'
    link.click()
    ElMessage.success('导出成功')
  } catch (e) { /* handled */ }
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
  justify-content: flex-end;
}
</style>
