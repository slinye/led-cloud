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
      <el-table :data="tableData" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="屏幕名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="150" />
        <el-table-column prop="resolution" label="分辨率" width="140">
          <template #default="{ row }">
            {{ row.resolutionWidth && row.resolutionHeight ? row.resolutionWidth + 'x' + row.resolutionHeight : (row.resolution || '-') }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="isOnline(row) ? 'success' : 'info'" size="small">
              {{ isOnline(row) ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="调节亮度" placement="top">
              <el-button type="warning" text size="small" circle @click="handleBrightness(row)"><el-icon><Sunny /></el-icon></el-button>
            </el-tooltip>
            <el-tooltip content="远程重启" placement="top">
              <el-button type="danger" text size="small" circle @click="handleRestart(row)"><el-icon><RefreshRight /></el-icon></el-button>
            </el-tooltip>
            <el-tooltip content="开关机" placement="top">
              <el-button type="info" text size="small" circle @click="handlePower(row)">
                <el-icon><Switch /></el-icon>
              </el-button>
            </el-tooltip>
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" text size="small" @click="handleDelete(row)" v-if="auth.canDelete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <span v-if="selectedScreens.length > 0" style="margin-right: auto; display: flex; gap: 8px;">
          <el-button size="small" @click="handleBatchBrightness">批量调亮度</el-button>
          <el-button size="small" @click="handleBatchPower('on')">批量开机</el-button>
          <el-button size="small" @click="handleBatchPower('off')">批量关机</el-button>
        </span>
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
        <el-form-item label="分辨率宽">
          <el-input-number v-model="form.resolutionWidth" :min="1" :max="7680" style="width: 100%;" placeholder="例：1920" />
        </el-form-item>
        <el-form-item label="分辨率高">
          <el-input-number v-model="form.resolutionHeight" :min="1" :max="4320" style="width: 100%;" placeholder="例：1080" />
        </el-form-item>
        <el-form-item label="设备ID" prop="deviceId">
          <el-input v-model="form.deviceId" placeholder="例：20000001（2开头8位数字）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 亮度调节对话框 -->
    <el-dialog v-model="brightnessVisible" title="调节屏幕亮度" width="360px" destroy-on-close>
      <div style="text-align: center; padding: 20px;">
        <div style="margin-bottom: 8px; color: #606266;">屏幕：{{ brightnessScreen?.name }}</div>
        <el-slider v-model="brightnessValue" :min="1" :max="100" show-input :format-tooltip="val => val + '%'" />
      </div>
      <template #footer>
        <el-button @click="brightnessVisible = false">取消</el-button>
        <el-button type="primary" :loading="settingBrightness" @click="handleSetBrightness">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Sunny, RefreshRight, Switch } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import { getScreens, createScreen, updateScreen, deleteScreen, controlScreen, batchPower, batchBrightness } from '../api'

const auth = useAuthStore()

const keyword = ref('')
const statusFilter = ref('')
const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const selectedScreens = ref([])

function isOnline(row) {
  // 基于lastHeartbeat判断：60秒内有心跳算在线
  if (!row.lastHeartbeat) return false
  const now = new Date().getTime()
  const hb = new Date(row.lastHeartbeat).getTime()
  return (now - hb) < 60000
}

function handleSelectionChange(rows) {
  selectedScreens.value = rows
}

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

const form = reactive({ name: '', ipAddress: '', resolutionWidth: 1920, resolutionHeight: 1080, deviceId: '' })

const rules = {
  name: [{ required: true, message: '请输入屏幕名称', trigger: 'blur' }],
  ipAddress: [{ required: true, message: '请输入IP地址', trigger: 'blur' }],
  deviceId: [
    { required: true, message: '请输入设备ID', trigger: 'blur' },
    { pattern: /^2\d{7}$/, message: '设备ID必须是2开头的8位数字', trigger: 'blur' }
  ]
}

function handleAdd() {
  editingId.value = null
  dialogTitle.value = '新增屏幕'
  Object.assign(form, { name: '', ipAddress: '', resolutionWidth: 1920, resolutionHeight: 1080, deviceId: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  editingId.value = row.id
  dialogTitle.value = '编辑屏幕'
  Object.assign(form, {
    name: row.name || '', ipAddress: row.ipAddress || '',
    resolutionWidth: row.resolutionWidth || 1920, resolutionHeight: row.resolutionHeight || 1080,
    deviceId: row.deviceId || row.mqttClientId || ''
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = { ...form, mqttClientId: form.deviceId }
    delete data.deviceId
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

// ---- 远程控制 ----
const brightnessVisible = ref(false)
const brightnessScreen = ref(null)
const brightnessValue = ref(80)
const settingBrightness = ref(false)

function handleBrightness(row) {
  brightnessScreen.value = row
  brightnessValue.value = row.brightness || 80
  brightnessVisible.value = true
}

async function handleSetBrightness() {
  settingBrightness.value = true
  try {
    await controlScreen(brightnessScreen.value.id, { action: 'brightness', brightness: brightnessValue.value })
    ElMessage.success('亮度调节指令已发送')
    brightnessVisible.value = false
  } catch (e) { /* handled */ } finally { settingBrightness.value = false }
}

async function handleRestart(row) {
  await ElMessageBox.confirm(`确定要远程重启屏幕 "${row.name}" 吗？`, '重启确认', { type: 'warning' })
  try {
    // 通过关机再开机模拟重启
    await batchPower({ screenIds: [row.id], action: 'off' })
    setTimeout(async () => {
      try {
        await batchPower({ screenIds: [row.id], action: 'on' })
        ElMessage.success('重启指令已发送')
      } catch (e) { /* handled */ }
    }, 3000)
  } catch (e) { /* handled */ }
}

async function handlePower(row) {
  const action = row.status === 'online' ? 'off' : 'on'
  const text = action === 'off' ? '关机' : '开机'
  await ElMessageBox.confirm(`确定要${text}屏幕 "${row.name}" 吗？`, `${text}确认`, { type: 'warning' })
  try {
    await batchPower({ screenIds: [row.id], action })
    ElMessage.success(`${text}指令已发送`)
  } catch (e) { /* handled */ }
}

// 批量操作
async function handleBatchBrightness() {
  brightnessScreen.value = null
  brightnessValue.value = 80
  brightnessVisible.value = true
}

async function handleBatchPower(action) {
  const ids = selectedScreens.value.map(s => s.id)
  const text = action === 'on' ? '开机' : '关机'
  await ElMessageBox.confirm(`确定要批量${text} ${ids.length} 个屏幕吗？`, `批量${text}`, { type: 'warning' })
  try {
    await batchPower({ screenIds: ids, action })
    ElMessage.success(`批量${text}指令已发送`)
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
  align-items: center;
  justify-content: flex-end;
}
</style>
