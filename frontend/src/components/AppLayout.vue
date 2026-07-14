<template>
  <el-container class="app-layout">
    <el-aside width="220px" class="app-aside">
      <div class="logo">
        <el-icon :size="24"><Monitor /></el-icon>
        <span>LED Cloud</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#fff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/screens">
          <el-icon><Monitor /></el-icon>
          <span>屏幕管理</span>
        </el-menu-item>
        <el-menu-item v-if="auth.canWrite" index="/screen-groups">
          <el-icon><Connection /></el-icon>
          <span>屏幕分组</span>
        </el-menu-item>
        <el-menu-item index="/contents">
          <el-icon><FolderOpened /></el-icon>
          <span>内容管理</span>
        </el-menu-item>
        <el-menu-item index="/programs">
          <el-icon><VideoPlay /></el-icon>
          <span>节目管理</span>
        </el-menu-item>
        <el-menu-item index="/logs">
          <el-icon><Document /></el-icon>
          <span>播放日志</span>
        </el-menu-item>
        <el-menu-item v-if="auth.canAdmin" index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item v-if="auth.canAdmin" index="/settings">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <h3>LED 显示屏云管理系统</h3>
        </div>
        <div class="header-right">
          <el-tag :type="roleTagType" size="small" style="margin-right: 12px;">
            {{ auth.user?.roleLabel || auth.role }}
          </el-tag>
          <span class="username">{{ auth.username }}</span>
          <el-button type="danger" text @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '../store/auth'
import router from '../router'

const route = useRoute()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)

const roleTagType = computed(() => {
  const map = { admin: 'danger', operator: 'warning', viewer: 'info' }
  return map[auth.role] || 'info'
})

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    auth.logout()
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

.app-aside {
  background: #001529;
  overflow-y: auto;
  overflow-x: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  gap: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.app-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 24px;
  height: 60px;
}

.header-left h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
}

.username {
  margin-right: 16px;
  color: #555;
}

.app-main {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
