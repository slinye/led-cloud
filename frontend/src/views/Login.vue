<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="login-title">LED Cloud 管理系统</h2>
      <p class="login-subtitle">微服务架构 · 智能调度</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%;" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="demo-accounts">
        <p>演示账号：</p>
        <el-button size="small" @click="fillAccount('admin', '123456')">管理员</el-button>
        <el-button size="small" @click="fillAccount('operator', '123456')">操作员</el-button>
        <el-button size="small" @click="fillAccount('viewer', '123456')">观看者</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login } from '../api'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await login(form)
    auth.loginSuccess(res)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

function fillAccount(username, password) {
  form.username = username
  form.password = password
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.login-title {
  text-align: center;
  margin-bottom: 4px;
  font-size: 24px;
  color: #333;
}

.login-subtitle {
  text-align: center;
  margin-bottom: 30px;
  color: #999;
  font-size: 14px;
}

.demo-accounts {
  text-align: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.demo-accounts p {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}
</style>
