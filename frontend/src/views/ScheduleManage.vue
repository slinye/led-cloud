<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd" v-if="auth.canWrite">
          <el-icon><Plus /></el-icon>新增定时
        </el-button>
        <el-button style="margin-left: 8px;" :loading="refreshing" @click="loadData">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="programName" label="节目" min-width="140" show-overflow-tooltip />
        <el-table-column prop="screenName" label="目标屏幕" min-width="140" show-overflow-tooltip />
        <el-table-column prop="cronExpression" label="Cron表达式" min-width="140" show-overflow-tooltip />
        <el-table-column label="时间范围" min-width="180">
          <template #default="{ row }">
            <div class="time-range">
              <div>{{ formatTime(row.startTime) }}</div>
              <div v-if="row.endTime">至 {{ formatTime(row.endTime) }}</div>
              <div v-else>无截止</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="下次执行" min-width="160">
          <template #default="{ row }">
            <span :style="{ color: row.enabled === 1 ? '#409eff' : '#909399' }">{{ formatTime(row.nextRunTime) || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button :type="row.enabled === 1 ? 'warning' : 'success'" text size="small" @click="handleToggle(row)">
              {{ row.enabled === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button type="info" text size="small" @click="handleTrigger(row)">手动触发</el-button>
            <el-button type="danger" text size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="节目" prop="programId">
          <el-select v-model="form.programId" placeholder="选择节目" filterable style="width: 100%;">
            <el-option v-for="p in programList" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标屏幕" prop="screenId">
          <el-select v-model="form.screenId" placeholder="选择屏幕" filterable style="width: 100%;">
            <el-option v-for="s in screenList" :key="s.id" :label="s.name + ' (' + (s.ipAddress || '') + ')'" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Cron表达式" prop="cronExpression">
          <el-input v-model="form.cronExpression" placeholder="例：0 0 8 * * ? (每天8点)" />
          <div class="form-tip">
            <el-button text size="small" type="primary" @click="setCronPreset('0 0 8 * * ?')">每天8点</el-button>
            <el-button text size="small" type="primary" @click="setCronPreset('0 0 12 * * ?')">每天12点</el-button>
            <el-button text size="small" type="primary" @click="setCronPreset('0 0 18 * * ?')">每天18点</el-button>
            <el-button text size="small" type="primary" @click="setCronPreset('0 */30 * * * ?')">每30分钟</el-button>
          </div>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="可选，为空则无限期" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%;" />
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
import { getSchedules, createSchedule, updateSchedule, toggleSchedule, deleteSchedule, triggerSchedule } from '../api'
import { getProgramsAll } from '../api'
import { getScreensAll } from '../api'

const auth = useAuthStore()

const tableData = ref([])
const loading = ref(false)
const refreshing = ref(false)

function formatTime(dt) {
  if (!dt) return '-'
  const s = String(dt)
  return s.replace('T', ' ').substring(0, 19)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getSchedules()
    tableData.value = (res && res.code === 200) ? (res.data || []) : (res || [])
  } catch (e) { /* handled */ } finally { loading.value = false }
}

// ---- 新增/编辑 ----
const dialogVisible = ref(false)
const dialogTitle = ref('新增定时')
const formRef = ref(null)
const editingId = ref(null)
const submitting = ref(false)
const programList = ref([])
const screenList = ref([])

const form = reactive({
  programId: null, screenId: null,
  cronExpression: '0 0 8 * * ?',
  startTime: '', endTime: ''
})

const rules = {
  programId: [{ required: true, message: '请选择节目', trigger: 'change' }],
  screenId: [{ required: true, message: '请选择屏幕', trigger: 'change' }],
  cronExpression: [{ required: true, message: '请输入Cron表达式', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }]
}

function setCronPreset(expr) {
  form.cronExpression = expr
}

async function handleAdd() {
  editingId.value = null
  dialogTitle.value = '新增定时'
  Object.assign(form, { programId: null, screenId: null, cronExpression: '0 0 8 * * ?', startTime: '', endTime: '' })
  try {
    const [progRes, scrRes] = await Promise.all([getProgramsAll(), getScreensAll()])
    programList.value = (progRes?.code === 200) ? (progRes.data || []) : (progRes || [])
    screenList.value = (scrRes?.code === 200) ? (scrRes.data || []) : (scrRes || [])
  } catch (e) { /* handled */ }
  dialogVisible.value = true
}

async function handleEdit(row) {
  editingId.value = row.id
  dialogTitle.value = '编辑定时'
  Object.assign(form, {
    programId: row.programId, screenId: row.screenId,
    cronExpression: row.cronExpression || '',
    startTime: row.startTime ? formatTime(row.startTime) : '',
    endTime: row.endTime ? formatTime(row.endTime) : ''
  })
  try {
    const [progRes, scrRes] = await Promise.all([getProgramsAll(), getScreensAll()])
    programList.value = (progRes?.code === 200) ? (progRes.data || []) : (progRes || [])
    screenList.value = (scrRes?.code === 200) ? (scrRes.data || []) : (scrRes || [])
  } catch (e) { /* handled */ }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = { ...form }
    if (editingId.value) {
      await updateSchedule(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      await createSchedule(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { /* handled */ } finally { submitting.value = false }
}

// ---- 操作 ----
async function handleToggle(row) {
  try {
    const res = await toggleSchedule(row.id)
    ElMessage.success(res?.msg || (row.enabled === 1 ? '已停用' : '已启用'))
    loadData()
  } catch (e) { /* handled */ }
}

async function handleTrigger(row) {
  await ElMessageBox.confirm(`确定手动触发定时 "${row.programName || row.id}" 吗？`, '手动触发', { type: 'warning' })
  try {
    await triggerSchedule(row.id)
    ElMessage.success('已触发')
  } catch (e) { /* handled */ }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除定时 "${row.programName || row.id}" 吗？`, '确认删除', { type: 'warning' })
  try { await deleteSchedule(row.id); ElMessage.success('已删除'); loadData() } catch (e) { /* handled */ }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 8px; }
.time-range { font-size: 13px; }
.time-range div { line-height: 1.6; }
.form-tip { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 4px; }
</style>
