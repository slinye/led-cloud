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
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : row.role === 'operator' ? 'warning' : 'info'" size="small">
              {{ row.roleLabel || row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" text size="small" @click="handleDelete(row)" v-if="row.username !== 'admin'">删除</el-button>
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

    <!-- 新增对话框 -->
    <el-dialog v-model="dialogVisible" title="新增用户" width="450px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请确认密码" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%;">
            <el-option v-for="r in roleOptions" :key="r.value" :label="r.label" :value="r.value" />
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
import { getUsers, createUser, deleteUser, getRoles } from '../api'

const keyword = ref('')
const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const roleOptions = ref([])

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

// ---- 新增 ----
const dialogVisible = ref(false)
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  role: 'viewer'
})

const validatePass = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少6位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认密码', trigger: 'blur' }, { validator: validatePass, trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function handleAdd() {
  form.username = ''
  form.password = ''
  form.confirmPassword = ''
  form.role = 'viewer'
  try {
    const res = await getRoles()
    roleOptions.value = (res && res.code === 200) ? (res.data || []) : (res || [])
    if (roleOptions.value.length === 0) {
      roleOptions.value = [
        { value: 'admin', label: '管理员' },
        { value: 'operator', label: '操作员' },
        { value: 'viewer', label: '观看者' }
      ]
    }
  } catch (e) { /* handled */ }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createUser({ username: form.username, password: form.password, role: form.role })
    ElMessage.success('创建成功')
    dialogVisible.value = false
    loadData()
  } catch (e) { /* handled */ } finally { submitting.value = false }
}

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
