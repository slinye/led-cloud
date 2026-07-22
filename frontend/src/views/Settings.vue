<template>
  <div>
    <el-card>
      <template #header><span>系统设置</span></template>
      <el-form ref="formRef" :model="form" label-width="140px" style="max-width: 600px;">
        <el-form-item label="系统名称">
          <el-input v-model="form.systemName" placeholder="LED Cloud 管理系统" />
        </el-form-item>
        <el-form-item label="默认屏幕亮度">
          <el-input-number v-model="form.defaultBrightness" :min="1" :max="100" /> %
        </el-form-item>
        <el-form-item label="自动重连">
          <el-switch v-model="form.autoReconnect" />
        </el-form-item>
        <el-form-item label="日志保留天数">
          <el-input-number v-model="form.logRetentionDays" :min="1" :max="365" /> 天
        </el-form-item>
        <el-form-item label="心跳间隔">
          <el-input-number v-model="form.heartbeatInterval" :min="5" :max="300" /> 秒
        </el-form-item>
        <el-form-item label="心跳超时阈值">
          <el-input-number v-model="form.heartbeatTimeout" :min="10" :max="600" /> 秒
          <div class="setting-tip">超过此时间未收到心跳的设备标记为离线</div>
        </el-form-item>
        <el-form-item label="文件存储路径">
          <el-input v-model="form.uploadPath" placeholder="/data/uploads" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>
        <span>MQTT 配置</span>
        <el-tag size="small" type="info" style="margin-left: 8px;">服务端配置，需重启生效</el-tag>
      </template>
      <el-form label-width="140px" style="max-width: 600px;">
        <el-form-item label="MQTT Broker">
          <el-input v-model="mqttForm.broker" placeholder="tcp://localhost:1883" />
        </el-form-item>
        <el-form-item label="客户端ID前缀">
          <el-input v-model="mqttForm.clientIdPrefix" placeholder="led-cloud" />
        </el-form-item>
        <el-form-item label="心跳Topic">
          <el-input v-model="mqttForm.heartbeatTopic" placeholder="led/heartbeat" />
        </el-form-item>
        <el-form-item label="指令Topic">
          <el-input v-model="mqttForm.commandTopic" placeholder="led/command" />
        </el-form-item>
      </el-form>
    </el-card>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSettings, updateSettings } from '../api'

const formRef = ref(null)
const saving = ref(false)

const form = reactive({
  systemName: 'LED Cloud 管理系统',
  defaultBrightness: 80,
  autoReconnect: true,
  logRetentionDays: 30,
  heartbeatInterval: 30,
  heartbeatTimeout: 60,
  uploadPath: '/data/uploads'
})

const mqttForm = reactive({
  broker: 'tcp://localhost:1883',
  clientIdPrefix: 'led-cloud',
  heartbeatTopic: 'led/heartbeat',
  commandTopic: 'led/command'
})

async function loadSettings() {
  try {
    const res = await getSettings()
    const data = res && res.code === 200 ? res.data : res
    if (data) {
      Object.assign(form, data)
    }
  } catch (e) { /* ignore, use defaults */ }
}

async function handleSave() {
  saving.value = true
  try {
    await updateSettings({ ...form, ...mqttForm })
    ElMessage.success('保存成功')
  } catch (e) { /* handled */ } finally { saving.value = false }
}

onMounted(loadSettings)
</script>

<style scoped>
.setting-tip {
  font-size: 12px;
  color: #909399;
  display: block;
  margin-top: 4px;
}
</style>
