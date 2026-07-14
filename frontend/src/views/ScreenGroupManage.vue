<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索分组名称" clearable style="width: 240px;" @clear="loadData" @keyup.enter="loadData" />
        <el-button type="primary" style="margin-left: auto;" @click="handleAdd" v-if="auth.canWrite">
          <el-icon><Plus /></el-icon>新增分组
        </el-button>
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="分组名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="screenCount" label="屏幕数量" width="100" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleManageScreens(row)">管理屏幕</el-button>
            <el-button type="warning" text size="small" @click="handleBatchDeploy(row)">批量下发</el-button>
            <el-button type="danger" text size="small" @click="handleDelete(row)" v-if="auth.canDelete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分组名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分组名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 管理屏幕对话框 -->
    <el-dialog v-model="screenDialogVisible" :title="`管理屏幕 - ${currentGroup?.name || ''}`" width="600px" destroy-on-close>
      <el-transfer
        v-model="selectedScreens"
        :data="allScreenOptions"
        :titles="['可选屏幕', '已选屏幕']"
        filterable
        style="display: flex; justify-content: center;"
      />
      <template #footer>
        <el-button @click="screenDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingScreens" @click="handleSaveScreens">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量下发对话框 -->
    <el-dialog v-model="deployDialogVisible" title="批量下发节目" width="500px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="选择节目">
          <el-select v-model="deployProgramId" placeholder="请选择节目" filterable style="width: 100%;">
            <el-option v-for="p in programList" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-radio-group v-model="deployAction">
            <el-radio label="play">开始播放</el-radio>
            <el-radio label="stop">停止播放</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deployDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="deploying" @click="handleDeploy">确认下发</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../store/auth'
import { getScreensAll } from '../api'
import { getProgramsAll } from '../api'
import { batchDeployProgram } from '../api'
import { getScreenGroups, createScreenGroup, updateScreenGroup, deleteScreenGroup, addScreenToGroup, getGroupScreens } from '../api'

const auth = useAuthStore()

const keyword = ref('')
const tableData = ref([])
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const params = {}
    if (keyword.value) params.keyword = keyword.value
    const res = await getScreenGroups(params)
    const data = res && res.code === 200 ? res.data : res
    tableData.value = data?.records || data || []
  } catch (e) { /* handled */ } finally { loading.value = false }
}

// ---- 新增/编辑 ----
const dialogVisible = ref(false)
const dialogTitle = ref('新增分组')
const formRef = ref(null)
const editingId = ref(null)
const submitting = ref(false)

const form = reactive({ name: '', description: '' })

const rules = { name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }] }

function handleAdd() {
  editingId.value = null
  dialogTitle.value = '新增分组'
  Object.assign(form, { name: '', description: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  editingId.value = row.id
  dialogTitle.value = '编辑分组'
  Object.assign(form, { name: row.name || '', description: row.description || '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editingId.value) {
      await updateScreenGroup(editingId.value, { ...form })
      ElMessage.success('编辑成功')
    } else {
      await createScreenGroup({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { /* handled */ } finally { submitting.value = false }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除分组 "${row.name}" 吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteScreenGroup(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { /* handled */ }
}

// ---- 管理屏幕 ----
const screenDialogVisible = ref(false)
const currentGroup = ref(null)
const selectedScreens = ref([])
const allScreenOptions = ref([])
const savingScreens = ref(false)

async function handleManageScreens(row) {
  currentGroup.value = row
  try {
    const screenRes = await getScreensAll()
    const screens = screenRes && screenRes.code === 200 ? (screenRes.data || []) : (screenRes || [])
    allScreenOptions.value = screens.map(s => ({ key: s.id, label: s.name + ' (' + (s.ipAddress || '') + ')' }))
    const groupRes = await getGroupScreens(row.id)
    const groupScreens = groupRes && groupRes.code === 200 ? (groupRes.data || []) : (groupRes || [])
    selectedScreens.value = groupScreens.map(s => s.id)
  } catch (e) { /* handled */ }
  screenDialogVisible.value = true
}

async function handleSaveScreens() {
  savingScreens.value = true
  try {
    await addScreenToGroup(currentGroup.value.id, { screenIds: selectedScreens.value })
    ElMessage.success('屏幕关联更新成功')
    screenDialogVisible.value = false
    loadData()
  } catch (e) { /* handled */ } finally { savingScreens.value = false }
}

// ---- 批量下发 ----
const deployDialogVisible = ref(false)
const deployProgramId = ref(null)
const deployAction = ref('play')
const programList = ref([])
const deployGroupId = ref(null)
const deploying = ref(false)

async function handleBatchDeploy(row) {
  deployGroupId.value = row.id
  try {
    const res = await getProgramsAll()
    programList.value = (res && res.code === 200) ? (res.data || []) : (res || [])
  } catch (e) { /* handled */ }
  deployProgramId.value = null
  deployDialogVisible.value = true
}

async function handleDeploy() {
  if (!deployProgramId.value) {
    ElMessage.warning('请选择节目')
    return
  }
  deploying.value = true
  try {
    const screenRes = await getGroupScreens(deployGroupId.value)
    const groupScreens = screenRes?.data || screenRes || []
    const screenIds = groupScreens.map(s => s.id)
    await batchDeployProgram({
      screenIds,
      programId: deployProgramId.value,
      action: deployAction.value
    })
    ElMessage.success('批量下发成功')
    deployDialogVisible.value = false
  } catch (e) { /* handled */ } finally { deploying.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
}
</style>
