<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索屏幕名称/IP" clearable style="width: 240px;" @clear="loadData" @keyup.enter="loadData" />
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px; margin-left: 12px;" @change="loadData">
          <el-option label="在线" value="online" />
          <el-option label="离线" value="offline" />
        </el-select>
        <el-button type="primary" style="margin-left: auto;" @click="handleAdd" v-if="auth.canWrite">
          <el-icon><Plus /></el-icon>新增屏幕
        </el-button>
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="屏幕名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="150" />
        <el-table-column prop="resolution" label="分辨率" width="140" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'online' ? 'success' : 'info'" size="small">{{ row.status === 'online' ? '在线' : '离线' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" text size="small" @click="handleDelete(row)" v-if="auth.canDelete">删除</el-button>
          </template>
        </el-table-column>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="屏幕名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入屏幕名称" />
        </el-form-item>
        <el-form-item label="IP地址" prop="ipAddress">
          <el-input v-model="form.ipAddress" placeholder="例：192.168.1.100" />
        </el-form-item>
        <el-form-item label="分辨率" prop="resolution">
          <el-input v-model="form.resolution" placeholder="例：1920x1080" />
        </el-form-item>
        <el-form-item label="设备ID" prop="deviceId">
          <el-input v-model="form.deviceId" placeholder="设备唯一标识" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%;">
            <el-option label="在线" value="online" />
            <el-option label="离线" value="offline" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../store/auth'
import { getScreens, createScreen, updateScreen, deleteScreen } from '../api'

const auth = useAuthStore()

const keyword = ref('')
const statusFilter = ref('')
const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function loadData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getScreens(params)
    const data = res && res.code === 200 ? res.data : res
    tableData.value = data?.records || data?.data || data || []
    total.value = data?.total || tableData.value.length
  } catch (e) { /* handled */ } finally { loading.value = false }
}

// ---- 新增/编辑 ----
const dialogVisible = ref(false)
const dialogTitle = ref('新增屏幕')
const formRef = ref(null)
const editingId = ref(null)
const submitting = ref(false)

const form = reactive({
  name: '', ipAddress: '', resolution: '', deviceId: '', status: 'online'
})

const rules = {
  name: [{ required: true, message: '请输入屏幕名称', trigger: 'blur' }],
  ipAddress: [{ required: true, message: '请输入IP地址', trigger: 'blur' }]
}

function handleAdd() {
  editingId.value = null
  dialogTitle.value = '新增屏幕'
  Object.assign(form, { name: '', ipAddress: '', resolution: '', deviceId: '', status: 'online' })
  dialogVisible.value = true
}

function handleEdit(row) {
  editingId.value = row.id
  dialogTitle.value = '编辑屏幕'
  Object.assign(form, {
    name: row.name || '',
    ipAddress: row.ipAddress || '',
    resolution: row.resolution || '',
    deviceId: row.deviceId || '',
    status: row.status || 'online'
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = { ...form }
    if (editingId.value) {
      await updateScreen(editingId.value, data)
      ElMessage.success('编辑成功')
    } else {
      await createScreen(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { /* handled */ } finally { submitting.value = false }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除屏幕 "${row.name}" 吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteScreen(row.id)
    ElMessage.success('删除成功')
    loadData()
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
