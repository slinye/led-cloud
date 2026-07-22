<template>
  <div class="alarm-config">
    <div class="page-header">
      <h2 class="page-title">
        <el-icon :size="24"><Bell /></el-icon>
        告警配置
      </h2>
      <p class="page-desc">配置设备异常告警规则，当触发条件时系统将自动发送通知</p>
    </div>

    <!-- 离线告警 -->
    <el-card class="alarm-card" shadow="hover">
      <div class="alarm-item">
        <div class="alarm-icon offline">
          <el-icon :size="28"><Connection /></el-icon>
        </div>
        <div class="alarm-info">
          <div class="alarm-title">离线告警</div>
          <div class="alarm-desc">设备离线超过心跳超时阈值时，系统将自动发送离线通知</div>
        </div>
        <div class="alarm-action">
          <el-switch
            v-model="form.offlineAlert"
            inline-prompt
            active-text="开"
            inactive-text="关"
            size="large"
            @change="handleSave"
          />
        </div>
      </div>
    </el-card>

    <!-- 播放失败告警 -->
    <el-card class="alarm-card" shadow="hover">
      <div class="alarm-item">
        <div class="alarm-icon play">
          <el-icon :size="28"><VideoPlay /></el-icon>
        </div>
        <div class="alarm-info">
          <div class="alarm-title">播放失败告警</div>
          <div class="alarm-desc">节目下发失败或屏幕播放异常时，系统将自动发送播放失败通知</div>
        </div>
        <div class="alarm-action">
          <el-switch
            v-model="form.playFailAlert"
            inline-prompt
            active-text="开"
            inactive-text="关"
            size="large"
            @change="handleSave"
          />
        </div>
      </div>
    </el-card>

    <!-- 磁盘不足告警 -->
    <el-card class="alarm-card" shadow="hover">
      <div class="alarm-item">
        <div class="alarm-icon disk">
          <el-icon :size="28"><FolderOpened /></el-icon>
        </div>
        <div class="alarm-info">
          <div class="alarm-title">磁盘不足告警</div>
          <div class="alarm-desc">设备剩余存储空间低于设定阈值时，系统将自动发送磁盘不足通知</div>
        </div>
        <div class="alarm-action">
          <div class="disk-action">
            <el-switch
              v-model="form.diskAlert"
              inline-prompt
              active-text="开"
              inactive-text="关"
              size="large"
              @change="handleSave"
            />
            <div v-if="form.diskAlert" class="disk-threshold">
              <span class="threshold-label">阈值</span>
              <el-input-number
                v-model="form.diskThreshold"
                :min="1"
                :max="100"
                :step="1"
                size="small"
                @change="handleSave"
              />
              <span class="threshold-unit">GB</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 保存状态 -->
    <div class="save-status" v-if="saveStatus">
      <el-alert :type="saveStatus.type" :closable="false" show-icon>
        {{ saveStatus.text }}
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { Bell, Connection, VideoPlay, FolderOpened } from '@element-plus/icons-vue'
import { getSettings, updateSettings } from '../api'

const form = reactive({
  offlineAlert: true,
  playFailAlert: true,
  diskAlert: false,
  diskThreshold: 10
})

const saveStatus = ref(null)
let saveTimer = null

async function loadSettings() {
  try {
    const res = await getSettings()
    const data = res && res.code === 200 ? res.data : res
    if (data) {
      if (data.offlineAlert !== undefined && data.offlineAlert !== null) {
        form.offlineAlert = data.offlineAlert === 'true' || data.offlineAlert === true
      }
      if (data.playFailAlert !== undefined && data.playFailAlert !== null) {
        form.playFailAlert = data.playFailAlert === 'true' || data.playFailAlert === true
      }
      if (data.diskAlert !== undefined && data.diskAlert !== null) {
        form.diskAlert = data.diskAlert === 'true' || data.diskAlert === true
      }
      if (data.diskThreshold !== undefined && data.diskThreshold !== null) {
        form.diskThreshold = parseInt(data.diskThreshold) || 10
      }
    }
  } catch (e) {
    /* ignore, use defaults */
  }
}

function showStatus(type, text) {
  saveStatus.value = { type, text }
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    saveStatus.value = null
  }, 3000)
}

async function handleSave() {
  try {
    await updateSettings({
      offlineAlert: String(form.offlineAlert),
      playFailAlert: String(form.playFailAlert),
      diskAlert: String(form.diskAlert),
      diskThreshold: String(form.diskThreshold)
    })
    showStatus('success', '告警配置已保存')
  } catch (e) {
    showStatus('error', '保存失败，请重试')
  }
}

onMounted(loadSettings)
</script>

<style scoped>
.alarm-config {
  max-width: 800px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: #1d2129;
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-desc {
  margin: 0;
  color: #86909c;
  font-size: 14px;
}

.alarm-card {
  margin-bottom: 16px;
  border-radius: 8px;
  transition: box-shadow 0.3s;
}

.alarm-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.alarm-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 8px 0;
}

.alarm-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.alarm-icon.offline {
  background: #fff7e6;
  color: #fa8c16;
}

.alarm-icon.play {
  background: #f0f5ff;
  color: #2f54eb;
}

.alarm-icon.disk {
  background: #f6ffed;
  color: #52c41a;
}

.alarm-info {
  flex: 1;
  min-width: 0;
}

.alarm-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 4px;
}

.alarm-desc {
  font-size: 13px;
  color: #86909c;
  line-height: 1.5;
}

.alarm-action {
  flex-shrink: 0;
}

.disk-action {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.disk-threshold {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.threshold-label {
  font-size: 13px;
  color: #86909c;
}

.threshold-unit {
  font-size: 13px;
  color: #1d2129;
  font-weight: 500;
}

.save-status {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2000;
  min-width: 280px;
}

/* 开关样式增强 */
:deep(.el-switch) {
  --el-switch-on-color: #3370ff;
}

:deep(.el-input-number) {
  width: 100px;
}
</style>
