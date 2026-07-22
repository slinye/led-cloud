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
      <el-table :data="tableData" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" />
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
        <el-table-column label="操作" width="400" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" text size="small" @click="handlePlay(row)" v-if="auth.canWrite">播放</el-button>
            <el-button type="warning" text size="small" @click="handleStop(row)" v-if="auth.canWrite && row.status === 'playing'">停止</el-button>
            <el-button text size="small" @click="openPreview(row)">预览</el-button>
            <el-button text size="small" @click="openHistory(row)">历史</el-button>
            <el-button type="danger" text size="small" @click="handleDelete(row)" v-if="auth.canDelete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <span v-if="selectedPrograms.length > 0" style="margin-right: auto;">
          <el-button type="danger" size="small" @click="handleBatchDelete">批量删除 ({{ selectedPrograms.length }})</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="节目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入节目名称" />
        </el-form-item>
        <el-form-item label="节目描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="调度方式">
          <el-radio-group v-model="form.scheduleType">
            <el-radio value="manual">手动</el-radio>
            <el-radio value="scheduled">定时</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.scheduleType === 'scheduled'" label="定时时间">
          <el-date-picker v-model="form.scheduleTime" type="datetime" placeholder="选择定时播放时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="节目内容">
          <div style="width: 100%;">
            <div ref="dragContainer" class="program-items-list">
              <div v-for="(item, index) in form.items" :key="index" class="program-item" draggable="true"
                @dragstart="onDragStart($event, index)" @dragover.prevent @drop="onDrop($event, index)"
                @dragenter.prevent @dragleave="onDragLeave($event)" @dragend="onDragEnd"
                :class="{ 'drag-over': dragOverIndex === index, 'dragging': dragIndex === index }">
                <div class="drag-handle">
                  <el-icon><Rank /></el-icon>
                </div>
                <el-select v-model="item.contentId" placeholder="选择内容" filterable style="width: 200px;" @change="onContentSelect(item.contentId, index)">
                  <el-option v-for="c in contentOptions" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
                <el-tooltip content="播放顺序，数字越小越靠前" placement="top">
                  <el-input-number v-model="item.sortOrder" :min="1" placeholder="排序" style="width: 120px; margin-left: 8px;" />
                </el-tooltip>
                <el-tooltip content="该内容项在屏幕上停留的秒数" placement="top">
                  <el-input-number v-model="item.duration" :min="1" :max="3600" placeholder="时长(秒)" style="width: 130px; margin-left: 8px;" />
                </el-tooltip>
                <el-button type="danger" :icon="Delete" circle size="small" style="margin-left: 8px;" @click="removeItem(index)" />
              </div>
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

    <!-- 节目预览对话框 -->
    <el-dialog v-model="previewVisible" title="节目预览" width="800px" destroy-on-close>
      <div class="preview-stage" v-if="previewItems.length > 0" :style="{ background: previewBg }">
        <div v-for="(item, idx) in previewItems" :key="idx" class="preview-item" :class="{ active: previewIndex === idx }">
          <img v-if="item.contentType === 'image'" :src="getUploadUrl(item.filePath)" style="max-width: 100%; max-height: 400px; object-fit: contain;" />
          <video v-else-if="item.contentType === 'video'" :src="getUploadUrl(item.filePath)" controls style="max-width: 100%; max-height: 400px;" />
          <div v-else-if="item.contentType === 'text'" :style="{
            fontSize: (item.fontSize || 24) + 'px',
            color: item.fontColor || '#fff',
            background: item.bgColor || '#000',
            padding: '12px 24px',
            borderRadius: '4px'
          }">{{ item.textContent || '(无文本)' }}</div>
          <div class="preview-item-label">{{ item.contentName }} ({{ item.duration }}s)</div>
        </div>
        <div v-if="previewItems.length > 0 && !previewPlaying" style="text-align: center; padding: 20px;">
          <el-button type="primary" @click="startPreviewPlay">开始预览播放</el-button>
        </div>
      </div>
      <el-empty v-else description="节目无内容" />
    </el-dialog>

    <!-- 发布历史对话框 -->
    <el-dialog v-model="historyVisible" :title="'发布历史 - ' + historyProgramName" width="720px" destroy-on-close>
      <el-timeline v-if="publishHistory.length > 0">
        <el-timeline-item
          v-for="(h, index) in publishHistory"
          :key="h.id"
          :timestamp="formatDateTime(h.createTime)"
          placement="top"
        >
          <el-card shadow="hover" size="small">
            <div class="history-card-header">
              <el-tag type="primary" size="small">V{{ h.version }}</el-tag>
              <el-tag :type="h.result === 'success' ? 'success' : 'danger'" size="small">{{ h.result === 'success' ? '成功' : '失败' }}</el-tag>
              <el-tag size="small" type="info">{{ publishTypeText(h.publishType) }}</el-tag>
              <span class="history-operator">{{ h.operator || '-' }}</span>
            </div>
            <div class="history-meta">
              <div class="history-row"><span class="history-label">发布时间：</span><span>{{ formatDateTime(h.createTime) }}</span></div>
              <div class="history-row" v-if="targetScreens(h).length">
                <span class="history-label">目标屏幕：</span>
                <span>
                  <el-tag v-for="(s, i) in targetScreens(h)" :key="i" size="small" style="margin: 2px 4px 2px 0;">{{ s.name || '屏幕#' + s.id }}</el-tag>
                </span>
              </div>
              <div class="history-row"><span class="history-label">素材数量：</span><span>{{ snapshotItemCount(h.snapshot) }} 个</span></div>
            </div>
            <div class="history-actions">
              <el-button type="primary" link size="small" @click="showSnapshot(h)">查看快照</el-button>
              <el-button v-if="index < publishHistory.length - 1" type="info" link size="small" @click="compareWithPrevious(h, publishHistory[index + 1])">与上一版对比</el-button>
              <el-button type="warning" link size="small" @click="rollbackToVersion(h)">回滚此版本</el-button>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无发布记录" />
      <template #footer><el-button @click="historyVisible = false">关闭</el-button></template>
    </el-dialog>

    <!-- 快照详情对话框 -->
    <el-dialog v-model="snapshotVisible" title="版本快照" width="600px" destroy-on-close>
      <div v-if="currentSnapshot" class="snapshot-detail">
        <div class="snapshot-row"><span class="snapshot-label">版本：</span><span>V{{ currentSnapshot.version }}</span></div>
        <div class="snapshot-row"><span class="snapshot-label">发布时间：</span><span>{{ formatDateTime(currentSnapshot.createTime) }}</span></div>
        <div class="snapshot-row"><span class="snapshot-label">操作人：</span><span>{{ currentSnapshot.operator || '-' }}</span></div>
        <el-table :data="currentSnapshot.data.items || []" size="small" border>
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="sortOrder" label="排序" width="70" />
          <el-table-column prop="contentName" label="内容名称" min-width="140" />
          <el-table-column prop="contentType" label="类型" width="90" />
          <el-table-column prop="duration" label="时长" width="90"><template #default="{ row }">{{ row.duration }} 秒</template></el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 版本对比对话框 -->
    <el-dialog v-model="compareVisible" title="版本对比" width="720px" destroy-on-close>
      <div v-if="compareData" class="compare-wrap">
        <div class="compare-title"><span>V{{ compareData.current.version }}（当前）</span><span class="compare-vs">对比</span><span>V{{ compareData.previous.version }}（上一版）</span></div>
        <div class="compare-section">
          <h4>素材差异</h4>
          <div v-if="compareData.diff && compareData.diff.length">
            <div v-for="(d, i) in compareData.diff" :key="i" class="diff-item" :class="d.type">
              <el-tag :type="diffTagType(d.type)" size="small">{{ diffText(d.type) }}</el-tag>
              <span class="diff-name">{{ d.name }}</span><span class="diff-detail">{{ d.detail }}</span>
            </div>
          </div>
          <el-empty v-else description="素材内容一致" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Rank } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import { getPrograms, createProgram, updateProgram, deleteProgram, startPlayback, stopPlayback } from '../api'
import { getContentsAll, getScreensAll, getPublishHistory } from '../api'

const auth = useAuthStore()

const keyword = ref('')
const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const selectedPrograms = ref([])

function handleSelectionChange(rows) { selectedPrograms.value = rows }

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

const form = reactive({ name: '', description: '', scheduleType: 'manual', scheduleTime: null, items: [] })
const rules = { name: [{ required: true, message: '请输入节目名称', trigger: 'blur' }] }

function addItem() {
  form.items.push({ contentId: null, sortOrder: form.items.length + 1, duration: 10 })
}

function removeItem(index) { form.items.splice(index, 1) }

function onContentSelect(contentId, index) {
  const content = contentOptions.value.find(c => c.id === contentId)
  if (content && content.duration && !form.items[index].duration) {
    form.items[index].duration = content.duration
  }
}

// ---- 拖拽排序 ----
const dragIndex = ref(-1)
const dragOverIndex = ref(-1)

function onDragStart(e, index) {
  dragIndex.value = index
  e.dataTransfer.effectAllowed = 'move'
  e.target.classList.add('dragging')
}

function onDrop(e, index) {
  if (dragIndex.value !== index && dragIndex.value >= 0) {
    const item = form.items.splice(dragIndex.value, 1)[0]
    form.items.splice(index, 0, item)
    // 更新sortOrder
    form.items.forEach((it, i) => { it.sortOrder = i + 1 })
  }
  dragIndex.value = -1
  dragOverIndex.value = -1
}

function onDragLeave(e) {
  dragOverIndex.value = -1
}

function onDragEnd() {
  dragIndex.value = -1
  dragOverIndex.value = -1
}

async function handleAdd() {
  editingId.value = null
  dialogTitle.value = '新增节目'
  Object.assign(form, { name: '', description: '', scheduleType: 'manual', scheduleTime: null, items: [] })
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
    name: row.name || '', description: row.description || '',
    scheduleType: row.scheduleType || 'manual', scheduleTime: row.scheduleTime || null,
    items: row.items?.length ? row.items.map(i => ({
      contentId: i.contentId || i.content?.id, sortOrder: i.sortOrder || 1, duration: i.duration || 10
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
      name: form.name, description: form.description,
      scheduleType: form.scheduleType,
      scheduleTime: form.scheduleType === 'scheduled' ? form.scheduleTime : null,
      items: form.items.filter(i => i.contentId).map(i => ({
        contentId: i.contentId, sortOrder: i.sortOrder, duration: i.duration
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
  try { await deleteProgram(row.id); ElMessage.success('删除成功'); loadData() } catch (e) { /* handled */ }
}

// 批量删除
async function handleBatchDelete() {
  const names = selectedPrograms.value.map(p => p.name).join('、')
  await ElMessageBox.confirm(`确定删除以下 ${selectedPrograms.value.length} 个节目？\n${names}`, '批量删除', { type: 'warning' })
  try {
    for (const p of selectedPrograms.value) await deleteProgram(p.id)
    ElMessage.success('批量删除成功')
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
  if (playScreenIds.value.length === 0) { ElMessage.warning('请选择屏幕'); return }
  playing.value = true
  try {
    await startPlayback(currentProgramId.value, { screenIds: playScreenIds.value })
    ElMessage.success('播放指令已发送')
    playDialogVisible.value = false
    loadData()
  } catch (e) { /* handled */ } finally { playing.value = false }
}

async function handleStop(row) {
  try { await stopPlayback(row.id); ElMessage.success('已停止播放'); loadData() } catch (e) { /* handled */ }
}

// ---- 节目预览 ----
const previewVisible = ref(false)
const previewItems = ref([])
const previewIndex = ref(0)
const previewPlaying = ref(false)
const previewBg = ref('#000')

function getUploadUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return '/uploads/' + url.replace(/^\/?(uploads\/)?/, '')
}

function openPreview(row) {
  const items = row.items || []
  if (items.length === 0) {
    ElMessage.warning('该节目没有内容')
    return
  }
  previewItems.value = items.map(it => ({
    contentId: it.contentId || it.content?.id,
    contentName: it.contentName || it.content?.name || '-',
    contentType: it.contentType || it.content?.type || 'text',
    filePath: it.filePath || it.content?.fileUrl || '',
    textContent: it.textContent || it.content?.textContent || '',
    fontSize: it.fontSize || it.content?.fontSize || 24,
    fontColor: it.fontColor || it.content?.fontColor || '#fff',
    bgColor: it.bgColor || it.content?.bgColor || '#000',
    duration: it.duration || 10
  }))
  previewIndex.value = 0
  previewPlaying.value = false
  previewVisible.value = true
}

function startPreviewPlay() {
  previewPlaying.value = true
  previewIndex.value = 0
  playNextPreviewItem()
}

function playNextPreviewItem() {
  if (previewIndex.value >= previewItems.value.length) {
    previewPlaying.value = false
    previewIndex.value = 0
    return
  }
  const item = previewItems.value[previewIndex.value]
  setTimeout(() => {
    previewIndex.value++
    playNextPreviewItem()
  }, (item.duration || 10) * 1000)
}

// ---- 发布历史 ----
const historyVisible = ref(false)
const historyProgramId = ref(null)
const historyProgramName = ref('')
const publishHistory = ref([])

async function openHistory(row) {
  historyProgramId.value = row.id
  historyProgramName.value = row.name
  historyVisible.value = true
  try {
    const res = await getPublishHistory(row.id)
    publishHistory.value = (res && res.code === 200) ? (res.data || []) : (res || [])
  } catch (e) { publishHistory.value = [] }
}

function parseSnapshot(snap) {
  if (!snap) return null
  try { return typeof snap === 'string' ? JSON.parse(snap) : snap } catch (e) { return null }
}

function parseTargets(record) {
  if (!record || !record.targetScreens) return []
  try {
    const t = typeof record.targetScreens === 'string' ? JSON.parse(record.targetScreens) : record.targetScreens
    return Array.isArray(t) ? t : []
  } catch (e) { return [] }
}

function formatDateTime(dt) {
  if (!dt) return '-'
  const str = String(dt)
  if (Array.isArray(dt)) return dt.join('T').substring(0, 19)
  return str.replace('T', ' ').substring(0, 19)
}

function publishTypeText(type) {
  const map = { create: '创建发布', edit: '编辑发布', play: '播放发布' }
  return map[type] || type || '未知'
}

function targetScreens(record) { return parseTargets(record) }

function snapshotItemCount(snap) {
  const data = parseSnapshot(snap)
  return data ? (data.itemCount || (data.items ? data.items.length : 0)) : 0
}

const snapshotVisible = ref(false)
const currentSnapshot = ref(null)

function showSnapshot(record) {
  currentSnapshot.value = {
    version: record.version, createTime: record.createTime, operator: record.operator,
    data: parseSnapshot(record.snapshot) || {}, targets: parseTargets(record)
  }
  snapshotVisible.value = true
}

const compareVisible = ref(false)
const compareData = ref(null)

function compareWithPrevious(current, previous) {
  const curData = parseSnapshot(current.snapshot) || {}
  const prevData = parseSnapshot(previous.snapshot) || {}
  const curItems = curData.items || []
  const prevItems = prevData.items || []
  const diff = []
  const curMap = new Map(curItems.map((it, idx) => [it.contentId + '-' + idx, it]))
  const prevMap = new Map(prevItems.map((it, idx) => [it.contentId + '-' + idx, it]))
  curItems.forEach((it, idx) => {
    const key = it.contentId + '-' + idx
    const prev = prevMap.get(key)
    if (!prev) diff.push({ type: 'added', name: it.contentName || '素材', detail: '新增，时长 ' + it.duration + 's' })
    else if (prev.duration !== it.duration || prev.sortOrder !== it.sortOrder) diff.push({ type: 'modified', name: it.contentName || '素材', detail: '排序/时长变化' })
  })
  prevItems.forEach((it, idx) => {
    const key = it.contentId + '-' + idx
    if (!curMap.has(key)) diff.push({ type: 'removed', name: it.contentName || '素材', detail: '已移除' })
  })
  compareData.value = { current: { version: current.version, data: curData }, previous: { version: previous.version, data: prevData }, diff }
  compareVisible.value = true
}

function diffTagType(type) { return type === 'added' ? 'success' : type === 'removed' ? 'danger' : 'warning' }
function diffText(type) { return type === 'added' ? '新增' : type === 'removed' ? '移除' : '修改' }

async function rollbackToVersion(record) {
  try {
    await ElMessageBox.confirm('确定回滚到 V' + record.version + ' 吗？', '回滚确认', { type: 'warning' })
    const data = parseSnapshot(record.snapshot)
    if (!data || !data.items) { ElMessage.warning('快照内容为空'); return }
    Object.assign(form, {
      name: data.name || historyProgramName.value, description: data.description || '',
      scheduleType: 'manual', scheduleTime: null,
      items: data.items.map(it => ({ contentId: it.contentId, sortOrder: it.sortOrder, duration: it.duration }))
    })
    dialogTitle.value = '回滚到 V' + record.version
    editingId.value = historyProgramId.value
    dialogVisible.value = true
    historyVisible.value = false
    ElMessage.success('已加载 V' + record.version + ' 快照')
  } catch (e) { /* cancel */ }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.pagination-wrap { margin-top: 16px; display: flex; align-items: center; justify-content: flex-end; }
.program-items-list { display: flex; flex-direction: column; gap: 4px; }
.program-item { display: flex; align-items: center; gap: 4px; padding: 6px 8px; border: 1px solid #e4e7ed; border-radius: 6px; background: #fff; transition: all 0.2s; }
.program-item.dragging { opacity: 0.5; }
.program-item.drag-over { border-color: #409eff; background: #ecf5ff; }
.drag-handle { cursor: grab; padding: 4px; color: #909399; }
.drag-handle:active { cursor: grabbing; }

.preview-stage { padding: 20px; border-radius: 8px; min-height: 300px; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.preview-item { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.preview-item.active { outline: 2px solid #409eff; border-radius: 4px; }
.preview-item-label { font-size: 13px; color: #999; margin-top: 4px; }

.history-card-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.history-operator { font-size: 13px; color: #909399; margin-left: auto; }
.history-meta { display: flex; flex-direction: column; gap: 6px; margin-bottom: 10px; font-size: 13px; color: #303133; }
.history-row { display: flex; align-items: flex-start; gap: 4px; line-height: 1.5; }
.history-label { color: #606266; white-space: nowrap; min-width: 70px; }
.history-actions { display: flex; gap: 8px; padding-top: 6px; border-top: 1px dashed #ebeef5; }
.snapshot-detail { font-size: 14px; color: #303133; }
.snapshot-row { display: flex; align-items: flex-start; gap: 4px; margin-bottom: 10px; line-height: 1.5; }
.snapshot-label { color: #606266; white-space: nowrap; min-width: 80px; }
.compare-wrap { font-size: 14px; color: #303133; }
.compare-title { display: flex; align-items: center; justify-content: center; gap: 12px; font-weight: 600; margin-bottom: 16px; font-size: 16px; }
.compare-vs { color: #909399; font-weight: 400; font-size: 14px; }
.compare-section { margin-bottom: 20px; }
.compare-section h4 { margin: 0 0 10px 0; font-size: 15px; color: #303133; border-left: 4px solid #409eff; padding-left: 8px; }
.diff-item { display: flex; align-items: center; gap: 8px; padding: 8px 12px; margin-bottom: 8px; border-radius: 4px; background: #f5f7fa; font-size: 13px; }
.diff-item.added { background: #f0f9ff; }
.diff-item.removed { background: #fff5f5; }
.diff-item.modified { background: #fffaf0; }
.diff-name { font-weight: 500; color: #303133; }
.diff-detail { color: #909399; margin-left: auto; }
</style>
