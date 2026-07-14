<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索节目名称" clearable style="width: 240px;" @clear="loadData" @keyup.enter="loadData" />
        <el-button type="primary" style="margin-left: auto;" @click="handleAdd" v-if="auth.canWrite">
          <el-icon><Plus /></el-icon>新增节目
        </el-button>
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="节目名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="itemCount" label="内容数量" width="90" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'playing' ? 'success' : 'info'" size="small">
              {{ row.status === 'playing' ? '播放中' : '待播放' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" text size="small" @click="handlePlay(row)" v-if="auth.canWrite">播放</el-button>
            <el-button type="warning" text size="small" @click="handleStop(row)" v-if="auth.canWrite && row.status === 'playing'">停止</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="节目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入节目名称" />
        </el-form-item>
        <el-form-item label="节目描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="节目内容">
          <div style="width: 100%;">
            <div v-for="(item, index) in form.items" :key="index" class="program-item">
              <el-select v-model="item.contentId" placeholder="选择内容" filterable style="width: 200px;" @change="onContentSelect(item.contentId, index)">
                <el-option v-for="c in contentOptions" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
              <el-input-number v-model="item.sortOrder" :min="1" placeholder="排序" style="width: 120px; margin-left: 8px;" />
              <el-input-number v-model="item.duration" :min="1" :max="3600" placeholder="时长(秒)" style="width: 130px; margin-left: 8px;" />
              <el-button type="danger" :icon="Delete" circle size="small" style="margin-left: 8px;" @click="removeItem(index)" />
            </div>
            <el-button type="primary" plain size="small" style="margin-top: 8px;" @click="addItem">
              <el-icon><Plus /></el-icon>添加内容项
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 播放对话框 -->
    <el-dialog v-model="playDialogVisible" title="选择播放屏幕" width="500px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="选择屏幕">
          <el-select v-model="playScreenIds" placeholder="可多选" multiple filterable style="width: 100%;">
            <el-option v-for="s in screenOptions" :key="s.id" :label="s.name + ' (' + (s.ipAddress || '') + ')'" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="playDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="playing" @click="handleStartPlay">确认播放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import { getPrograms, createProgram, updateProgram, deleteProgram, startPlayback, stopPlayback } from '../api'
import { getContentsAll, getScreensAll } from '../api'

const auth = useAuthStore()

const keyword = ref('')
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
    const res = await getPrograms(params)
    const data = res && res.code === 200 ? res.data : res
    tableData.value = data?.records || data?.data || data || []
    total.value = data?.total || tableData.value.length
  } catch (e) { /* handled */ } finally { loading.value = false }
}

// ---- 新增/编辑 ----
const dialogVisible = ref(false)
const dialogTitle = ref('新增节目')
const formRef = ref(null)
const editingId = ref(null)
const submitting = ref(false)
const contentOptions = ref([])

const form = reactive({
  name: '',
  description: '',
  items: []
})

const rules = { name: [{ required: true, message: '请输入节目名称', trigger: 'blur' }] }

function addItem() {
  form.items.push({ contentId: null, sortOrder: form.items.length + 1, duration: 10 })
}

function removeItem(index) {
  form.items.splice(index, 1)
}

function onContentSelect(contentId, index) {
  const content = contentOptions.value.find(c => c.id === contentId)
  if (content && content.duration && !form.items[index].duration) {
    form.items[index].duration = content.duration
  }
}

async function handleAdd() {
  editingId.value = null
  dialogTitle.value = '新增节目'
  Object.assign(form, { name: '', description: '', items: [] })
  addItem()
  try {
    const res = await getContentsAll()
    contentOptions.value = (res && res.code === 200) ? (res.data || []) : (res || [])
  } catch (e) { /* handled */ }
  dialogVisible.value = true
}

async function handleEdit(row) {
  editingId.value = row.id
  dialogTitle.value = '编辑节目'
  Object.assign(form, {
    name: row.name || '',
    description: row.description || '',
    items: row.items?.length ? row.items.map(i => ({
      contentId: i.contentId || i.content?.id,
      sortOrder: i.sortOrder || 1,
      duration: i.duration || 10
    })) : [{ contentId: null, sortOrder: 1, duration: 10 }]
  })
  try {
    const res = await getContentsAll()
    contentOptions.value = (res && res.code === 200) ? (res.data || []) : (res || [])
  } catch (e) { /* handled */ }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.items.length === 0 || !form.items[0].contentId) {
    ElMessage.warning('请至少添加一个内容项')
    return
  }
  submitting.value = true
  try {
    const data = {
      name: form.name,
      description: form.description,
      items: form.items.filter(i => i.contentId).map(i => ({
        contentId: i.contentId,
        sortOrder: i.sortOrder,
        duration: i.duration
      }))
    }
    if (editingId.value) {
      await updateProgram(editingId.value, data)
      ElMessage.success('编辑成功')
    } else {
      await createProgram(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { /* handled */ } finally { submitting.value = false }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除节目 "${row.name}" 吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteProgram(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { /* handled */ }
}

// ---- 播放控制 ----
const playDialogVisible = ref(false)
const playScreenIds = ref([])
const screenOptions = ref([])
const currentProgramId = ref(null)
const playing = ref(false)

async function handlePlay(row) {
  currentProgramId.value = row.id
  playScreenIds.value = []
  try {
    const res = await getScreensAll()
    screenOptions.value = (res && res.code === 200) ? (res.data || []) : (res || [])
  } catch (e) { /* handled */ }
  playDialogVisible.value = true
}

async function handleStartPlay() {
  if (playScreenIds.value.length === 0) {
    ElMessage.warning('请选择屏幕')
    return
  }
  playing.value = true
  try {
    await startPlayback(currentProgramId.value, { screenIds: playScreenIds.value })
    ElMessage.success('播放指令已发送')
    playDialogVisible.value = false
    loadData()
  } catch (e) { /* handled */ } finally { playing.value = false }
}

async function handleStop(row) {
  try {
    await stopPlayback(row.id)
    ElMessage.success('已停止播放')
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

.program-item {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}
</style>
