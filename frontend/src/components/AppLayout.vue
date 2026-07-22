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
        <el-menu-item v-if="auth.canWrite" index="/schedules">
          <el-icon><Clock /></el-icon>
          <span>定时调度</span>
        </el-menu-item>
        <el-menu-item index="/logs">
          <el-icon><Document /></el-icon>
          <span>播放日志</span>
        </el-menu-item>
        <el-menu-item v-if="auth.canAdmin" index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item v-if="auth.canAdmin" index="/alarm-config">
          <el-icon><Bell /></el-icon>
          <span>告警配置</span>
        </el-menu-item>
        <el-menu-item v-if="auth.canAdmin" index="/settings">
          <el-icon><Setting /></el-icon>
          <span>系统设置</span>
        </el-menu-item>
        <el-menu-item v-if="auth.canAdmin" index="/audit-logs">
          <el-icon><Checked /></el-icon>
          <span>审计日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <h3>LED 显示屏云管理系统</h3>
        </div>
        <div class="header-right">
          <el-badge :value="unreadAlarmCount" :hidden="unreadAlarmCount === 0" :max="99" class="alarm-badge">
            <el-popover
              placement="bottom-end"
              :width="360"
              trigger="click"
              @show="loadAlarmList"
            >
              <template #reference>
                <el-button link class="alarm-btn">
                  <el-icon :size="20"><Bell /></el-icon>
                </el-button>
              </template>

              <div class="alarm-popover">
                <div class="alarm-popover-header">
                  <span>告警通知</span>
                  <el-button v-if="unreadAlarmCount > 0" link type="primary" size="small" @click="handleMarkAllRead">全部已读</el-button>
                </div>

                <div v-if="alarmList.length === 0" class="alarm-empty">
                  <el-icon :size="40" color="#c0c4cc"><Bell /></el-icon>
                  <p>暂无告警通知</p>
                </div>

                <div v-else class="alarm-list">
                  <div
                    v-for="item in alarmList"
                    :key="item.id"
                    class="alarm-item"
                    :class="{ unread: item.status === 'unread' }"
                    @click="handleAlarmClick(item)"
                  >
                    <div class="alarm-item-icon" :class="item.alarmType">
                      <el-icon v-if="item.alarmType === 'offline'"><Connection /></el-icon>
                      <el-icon v-else-if="item.alarmType === 'play_fail'"><VideoPlay /></el-icon>
                      <el-icon v-else><FolderOpened /></el-icon>
                    </div>
                    <div class="alarm-item-content">
                      <div class="alarm-item-title">
                        {{ item.title }}
                        <span v-if="item.status === 'unread'" class="unread-dot"></span>
                      </div>
                      <div class="alarm-item-desc">{{ item.message }}</div>
                      <div class="alarm-item-time">{{ formatTime(item.createdAt) }}</div>
                    </div>
                  </div>
                </div>

                <div class="alarm-popover-footer">
                  <el-button link type="primary" size="small" @click="goToAlarmRecords">查看全部告警</el-button>
                </div>
              </div>
            </el-popover>
          </el-badge>

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
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { Connection, VideoPlay, FolderOpened } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import { getLatestAlarms, getAlarmUnreadCount, markAlarmRead, markAllAlarmsRead } from '../api'
import router from '../router'

const route = useRoute()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)

const roleTagType = computed(() => {
  const map = { admin: 'danger', operator: 'warning', viewer: 'info' }
  return map[auth.role] || 'info'
})

// ============ 告警通知 ============
const unreadAlarmCount = ref(0)
const alarmList = ref([])
let alarmTimer = null

async function refreshAlarmCount() {
  try {
    const res = await getAlarmUnreadCount()
    const data = res && res.code === 200 ? res.data : res
    if (data && data.unreadCount !== undefined) {
      unreadAlarmCount.value = data.unreadCount
    }
  } catch (e) { /* ignore */ }
}

async function loadAlarmList() {
  try {
    const res = await getLatestAlarms(10)
    const data = res && res.code === 200 ? res.data : res
    alarmList.value = Array.isArray(data) ? data : (data?.records || [])
  } catch (e) { /* ignore */ }
}

async function handleAlarmClick(item) {
  if (item.status === 'unread') {
    try {
      await markAlarmRead(item.id)
      item.status = 'read'
      unreadAlarmCount.value = Math.max(0, unreadAlarmCount.value - 1)
    } catch (e) { /* ignore */ }
  }
}

async function handleMarkAllRead() {
  try {
    await markAllAlarmsRead()
    alarmList.value.forEach(item => { item.status = 'read' })
    unreadAlarmCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (e) { /* ignore */ }
}

function goToAlarmRecords() {
  router.push('/alarm-records')
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => {
  refreshAlarmCount()
  alarmTimer = setInterval(refreshAlarmCount, 30000) // 每30秒轮询
})

onBeforeUnmount(() => {
  if (alarmTimer) clearInterval(alarmTimer)
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

/* ========== 告警通知样式 ========== */
.alarm-badge {
  margin-right: 8px;
}

.alarm-btn {
  padding: 4px;
  color: #606266;
  font-size: 20px;
}

.alarm-btn:hover {
  color: #3370ff;
}

.alarm-popover-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  margin-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
  font-size: 14px;
}

.alarm-empty {
  text-align: center;
  padding: 30px 0;
  color: #c0c4cc;
}

.alarm-empty p {
  margin-top: 8px;
  font-size: 13px;
}

.alarm-list {
  max-height: 320px;
  overflow-y: auto;
}

.alarm-item {
  display: flex;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.alarm-item:hover {
  background: #f5f7fa;
}

.alarm-item.unread {
  background: #f0f5ff;
}

.alarm-item-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}

.alarm-item-icon.offline {
  background: #fff7e6;
  color: #fa8c16;
}

.alarm-item-icon.play_fail {
  background: #f0f5ff;
  color: #2f54eb;
}

.alarm-item-icon.disk {
  background: #f6ffed;
  color: #52c41a;
}

.alarm-item-content {
  flex: 1;
  min-width: 0;
}

.alarm-item-title {
  font-size: 13px;
  font-weight: 600;
  color: #1d2129;
  display: flex;
  align-items: center;
  gap: 6px;
}

.unread-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f53f3f;
  flex-shrink: 0;
}

.alarm-item-desc {
  font-size: 12px;
  color: #86909c;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alarm-item-time {
  font-size: 11px;
  color: #c9cdd4;
  margin-top: 2px;
}

.alarm-popover-footer {
  text-align: center;
  padding-top: 10px;
  margin-top: 8px;
  border-top: 1px solid #ebeef5;
}
</style>
