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
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
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
  heartbeatInterval: 30
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
    await updateSettings({ ...form })
    ElMessage.success('保存成功')
  } catch (e) { /* handled */ } finally { saving.value = false }
}

onMounted(loadSettings)
</script>
