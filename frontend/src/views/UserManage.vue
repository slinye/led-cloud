<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索用户名" clearable style="width: 240px;" @clear="loadData" @keyup.enter="loadData" />
        <el-button type="primary" style="margin-left: auto;" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增用户
        </el-button>
      </div>
    </el-card>

    <el-card style="margin-top: 16px;">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="120">
          <template #default="{ row }">{{ row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'OPERATOR' ? 'warning' : 'info'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : row.role === 'OPERATOR' ? '操作员' : '观看者' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <template v-if="row.username === 'admin' && auth.username !== 'admin'">
              <span style="color:#909399;font-size:13px;">超级管理员</span>
            </template>
            <template v-else>
              <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button type="warning" text size="small" @click="handleResetPwd(row)">重置密码</el-button>
              <el-button type="danger" text size="small" @click="handleDelete(row)" v-if="row.username !== 'admin'">删除</el-button>
            </template>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="450px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请确认密码" show-password />
        </el-form-item>
        <el-form-item v-if="isEdit" label="昵称">
          <el-input v-model="form.nickname" placeholder="可选昵称" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%;">
            <el-option v-for="r in roleOptions" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="400px" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" placeholder="请输入新密码（至少6位）" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请确认新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="handleResetSubmit">确定重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, createUser, updateUser, resetUserPassword, deleteUser } from '../api'
import { useAuthStore } from '../store/auth'

const auth = useAuthStore()

const keyword = ref('')
const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const roleOptions = ref([
  { value: 'ADMIN', label: '管理员' },
  { value: 'OPERATOR', label: '操作员' },
  { value: 'VIEWER', label: '观看者' }
])

function formatTime(dt) {
  if (!dt) return '-'
  const s = String(dt)
  return s.replace('T', ' ').substring(0, 19)
}

async function loadData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    const res = await getUsers(params)
    const data = res && res.code === 200 ? res.data : res
    tableData.value = data?.records || data?.data || data || []
    total.value = data?.total || tableData.value.length
  } catch (e) { /* handled */ } finally { loading.value = false }
}

// ---- 新增/编辑 ----
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formRef = ref(null)
const isEdit = ref(false)
const editingId = ref(null)
const submitting = ref(false)

const form = reactive({
  username: '', password: '', confirmPassword: '', role: 'VIEWER', nickname: '', status: 1
})

const validatePass = (rule, value, callback) => {
  if (!isEdit.value && value !== form.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: isEdit.value ? [] : [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: isEdit.value ? [] : [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validatePass, trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

function handleAdd() {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = '新增用户'
  Object.assign(form, { username: '', password: '', confirmPassword: '', role: 'VIEWER', nickname: '', status: 1 })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  dialogTitle.value = '编辑用户 - ' + row.username
  Object.assign(form, {
    username: row.username,
    password: '',
    confirmPassword: '',
    role: row.role || 'VIEWER',
    nickname: row.nickname || '',
    status: row.status !== undefined ? row.status : 1
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateUser(editingId.value, { role: form.role, nickname: form.nickname, status: form.status })
      ElMessage.success('更新成功')
    } else {
      await createUser({ username: form.username, password: form.password, role: form.role })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) { /* handled */ } finally { submitting.value = false }
}

// ---- 重置密码 ----
const pwdDialogVisible = ref(false)
const pwdFormRef = ref(null)
const resetting = ref(false)
const resetUserId = ref(null)

const pwdForm = reactive({ password: '', confirmPassword: '' })

const pwdRules = {
  password: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (rule, value, callback) => { if (value !== pwdForm.password) callback(new Error('两次密码不一致')); else callback() }, trigger: 'blur' }
  ]
}

function handleResetPwd(row) {
  resetUserId.value = row.id
  pwdForm.password = ''
  pwdForm.confirmPassword = ''
  pwdDialogVisible.value = true
}

async function handleResetSubmit() {
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return
  resetting.value = true
  try {
    await resetUserPassword(resetUserId.value, { password: pwdForm.password })
    ElMessage.success('密码已重置')
    pwdDialogVisible.value = false
  } catch (e) { /* handled */ } finally { resetting.value = false }
}

// ---- 删除 ----
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除用户 "${row.username}" 吗？`, '确认删除', { type: 'warning' })
  try {
    await deleteUser(row.id)
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
