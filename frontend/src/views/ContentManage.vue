<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索内容名称" clearable style="width: 240px;" @clear="loadData" @keyup.enter="loadData" />
        <el-select v-model="typeFilter" placeholder="类型筛选" clearable style="width: 140px; margin-left: 12px;" @change="loadData">
          <el-option label="图片" value="image" />
          <el-option label="视频" value="video" />
          <el-option label="文本" value="text" />
        </el-select>
        <el-button type="primary" style="margin-left: auto;" @click="handleAdd" v-if="auth.canWrite">
          <el-icon><Plus /></el-icon>新增内容
        </el-button>
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <el-table :data="tableData" v-loading="loading" stripe @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === 'image' ? 'success' : row.type === 'video' ? 'warning' : 'info'" size="small">
              {{ row.type === 'image' ? '图片' : row.type === 'video' ? '视频' : '文本' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预览" width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.filePath && row.type === 'image'"
              :src="getUploadUrl(row.filePath)"
              :preview-src-list="[getUploadUrl(row.filePath)]"
              style="width: 60px; height: 40px; border-radius: 4px; cursor: pointer;"
              fit="cover"
              preview-teleported
            />
            <el-button v-else-if="row.filePath && row.type === 'video'" type="primary" text size="small" @click="previewVideo(row)">
              <el-icon><VideoPlay /></el-icon>预览
            </el-button>
            <span v-else-if="row.type === 'text' && row.textContent" style="color:#606266;font-size:12px;max-width:80px;display:inline-block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" :title="row.textContent">{{ row.textContent }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(秒)" width="90" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" text size="small" @click="handleDelete(row)" v-if="auth.canDelete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <span v-if="selectedContents.length > 0" style="margin-right: auto;">
          <el-button type="danger" size="small" @click="handleBatchDelete">批量删除 ({{ selectedContents.length }})</el-button>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="内容名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入内容名称" />
        </el-form-item>
        <el-form-item label="内容类型" prop="type">
          <el-select v-model="form.type" style="width: 100%;" @change="onTypeChange">
            <el-option label="图片" value="image" />
            <el-option label="视频" value="video" />
            <el-option label="文本" value="text" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.type === 'image' || form.type === 'video'" label="上传文件">
          <el-upload
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :file-list="fileList"
            list-type="picture"
            :accept="form.type === 'video' ? 'video/*' : 'image/*'"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip><div class="el-upload__tip">{{ form.type === 'video' ? '支持 mp4 等视频格式' : '支持 jpg/png 等图片格式' }}</div></template>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="form.type === 'text'" label="文本内容">
          <el-input v-model="form.textContent" type="textarea" :rows="4" placeholder="请输入显示的文本内容" />
        </el-form-item>
        <el-form-item v-if="form.type === 'text'" label="字体大小">
          <el-input-number v-model="form.fontSize" :min="12" :max="120" /> px
        </el-form-item>
        <el-form-item v-if="form.type === 'text'" label="字体颜色">
          <el-color-picker v-model="form.fontColor" />
        </el-form-item>
        <el-form-item v-if="form.type === 'text'" label="背景颜色">
          <el-color-picker v-model="form.backgroundColor" />
        </el-form-item>
        <el-form-item v-if="form.type === 'text'" label="滚动速度">
          <el-input-number v-model="form.scrollSpeed" :min="1" :max="10" /> 级
        </el-form-item>
        <el-form-item label="播放时长" prop="duration">
          <el-input-number v-model="form.duration" :min="1" :max="3600" /> 秒
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 视频预览对话框 -->
    <el-dialog v-model="videoVisible" title="视频预览" width="720px" destroy-on-close>
      <div style="text-align: center;">
        <video v-if="videoUrl" :src="videoUrl" controls autoplay style="max-width: 100%; max-height: 480px; border-radius: 8px;" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import { getContents, createContent, updateContent, deleteContent, getContentReferences } from '../api'

const auth = useAuthStore()

const keyword = ref('')
const typeFilter = ref('')
const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const selectedContents = ref([])

function handleSelectionChange(rows) {
  selectedContents.value = rows
}

function getUploadUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return '/uploads/' + url.replace(/^\/?(uploads\/)?/, '')
}

async function loadData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (typeFilter.value) params.type = typeFilter.value
    const res = await getContents(params)
    const data = res && res.code === 200 ? res.data : res
    tableData.value = data?.records || data?.data || data || []
    total.value = data?.total || tableData.value.length
  } catch (e) { /* handled */ } finally { loading.value = false }
}

function previewFile(row) {
  window.open(getUploadUrl(row.filePath), '_blank')
}

// 视频预览
const videoVisible = ref(false)
const videoUrl = ref('')

function previewVideo(row) {
  videoUrl.value = getUploadUrl(row.filePath)
  videoVisible.value = true
}

// ---- 新增/编辑 ----
const dialogVisible = ref(false)
const dialogTitle = ref('新增内容')
const formRef = ref(null)
const editingId = ref(null)
const submitting = ref(false)
const fileList = ref([])
const selectedFile = ref(null)

const form = reactive({
  name: '', type: 'image', textContent: '',
  fontSize: 24, fontColor: '#ffffff', backgroundColor: '#000000', scrollSpeed: 5, duration: 10
})

const rules = {
  name: [{ required: true, message: '请输入内容名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function onTypeChange() {
  selectedFile.value = null
  fileList.value = []
}

function handleFileChange(file) {
  selectedFile.value = file.raw
  fileList.value = [file]
}

function handleAdd() {
  editingId.value = null
  dialogTitle.value = '新增内容'
  Object.assign(form, {
    name: '', type: 'image', textContent: '', fontSize: 24,
    fontColor: '#ffffff', backgroundColor: '#000000', scrollSpeed: 5, duration: 10
  })
  selectedFile.value = null
  fileList.value = []
  dialogVisible.value = true
}

function handleEdit(row) {
  editingId.value = row.id
  dialogTitle.value = '编辑内容'
  Object.assign(form, {
    name: row.name || '', type: row.type || 'image',
    textContent: row.textContent || '', fontSize: row.fontSize || 24,
    fontColor: row.fontColor || '#ffffff', backgroundColor: row.backgroundColor || '#000000',
    scrollSpeed: row.scrollSpeed || 5, duration: row.duration || 10
  })
  selectedFile.value = null
  fileList.value = []
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const fd = new FormData()
    fd.append('name', form.name)
    fd.append('type', form.type)
    fd.append('duration', form.duration)
    if (form.type === 'text') {
      fd.append('textContent', form.textContent)
      fd.append('fontSize', form.fontSize)
      fd.append('fontColor', form.fontColor)
      fd.append('bgColor', form.backgroundColor)
      fd.append('scrollSpeed', form.scrollSpeed)
    } else if (selectedFile.value) {
      fd.append('file', selectedFile.value)
    }
    if (editingId.value) {
      await updateContent(editingId.value, fd)
      ElMessage.success('编辑成功')
    } else {
      await createContent(fd)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { /* handled */ } finally { submitting.value = false }
}

// 删除时检查引用
async function handleDelete(row) {
  try {
    let confirmMsg = `确定删除内容 "${row.name}" 吗？`
    try {
      const refRes = await getContentReferences(row.id)
      const refs = refRes?.data || refRes || []
      if (refs && refs.length > 0) {
        const names = refs.map(r => r.name || r).join('、')
        confirmMsg = `内容 "${row.name}" 被以下节目引用：${names}\n\n删除后这些节目将无法正常播放。确定删除吗？`
      }
    } catch (e) { /* ignore ref check */ }

    await ElMessageBox.confirm(confirmMsg, '确认删除', {
      type: 'warning',
      confirmButtonText: '确定删除',
      confirmButtonClass: 'el-button--danger'
    })
    await deleteContent(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { /* handled */ }
}

// 批量删除
async function handleBatchDelete() {
  const names = selectedContents.value.map(s => s.name).join('、')
  await ElMessageBox.confirm(`确定删除以下 ${selectedContents.value.length} 个内容？\n${names}`, '批量删除', { type: 'warning' })
  try {
    for (const c of selectedContents.value) {
      await deleteContent(c.id)
    }
    ElMessage.success('批量删除成功')
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
  align-items: center;
  justify-content: flex-end;
}
</style>
