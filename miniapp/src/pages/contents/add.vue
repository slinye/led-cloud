<template>
  <view class="container">
    <!-- 类型选择 -->
    <view class="card">
      <view class="card-title">内容类型</view>
      <view class="type-tabs">
        <view
          v-for="t in types" :key="t.value"
          class="type-tab" :class="{ active: form.type === t.value }"
          @click="form.type = t.value"
        >{{ t.label }}</view>
      </view>
    </view>

    <!-- 基本信息 -->
    <view class="card">
      <view class="card-title">基本信息</view>
      <view class="form-item">
        <text class="label">内容名称</text>
        <input class="input" v-model="form.name" placeholder="请输入名称" />
      </view>
    </view>

    <!-- 文字内容 -->
    <template v-if="form.type === 'text'">
      <view class="card">
        <view class="card-title">文字内容</view>
        <textarea class="textarea" v-model="form.textContent" placeholder="请输入文字内容" />
      </view>
      <view class="card">
        <view class="card-title">样式设置</view>
        <view class="form-item">
          <text class="label">字号</text>
          <slider
            class="slider" :value="form.fontSize" min="12" max="72" step="2"
            show-value @change="(e) => form.fontSize = e.detail.value"
          />
        </view>
        <view class="form-item">
          <text class="label">字体颜色</text>
          <picker class="color-picker" mode="selector"
            :range="colors" @change="(e) => form.fontColor = colors[e.detail.value]"
          >
            <view class="picker-row">
              <view class="color-dot" :style="{ background: form.fontColor }"></view>
              <text>{{ form.fontColor }}</text>
            </view>
          </picker>
        </view>
        <view class="form-item">
          <text class="label">背景颜色</text>
          <picker class="color-picker" mode="selector"
            :range="colors" @change="(e) => form.bgColor = colors[e.detail.value]"
          >
            <view class="picker-row">
              <view class="color-dot" :style="{ background: form.bgColor }"></view>
              <text>{{ form.bgColor }}</text>
            </view>
          </picker>
        </view>
        <view class="form-item">
          <text class="label">滚动速度</text>
          <slider
            class="slider" :value="form.scrollSpeed" min="0" max="10" step="1"
            show-value @change="(e) => form.scrollSpeed = e.detail.value"
          />
        </view>
      </view>
      <!-- 实时预览 -->
      <view class="card" v-if="form.textContent">
        <view class="card-title">预览</view>
        <view
          class="live-preview"
          :style="{
            fontSize: (form.fontSize || 24) + 'rpx',
            color: form.fontColor,
            backgroundColor: form.bgColor
          }"
        >{{ form.textContent }}</view>
      </view>
    </template>

    <!-- 图片/视频上传 -->
    <template v-else>
      <view class="card">
        <view class="card-title">选择文件</view>
        <view class="upload-area" @click="chooseFile">
          <view class="upload-icon">+</view>
          <text class="upload-text">{{ form.type === 'image' ? '选择图片' : '选择视频' }}</text>
        </view>
        <image v-if="chosenFile && form.type === 'image'" class="chosen-img" :src="chosenFile" mode="widthFix" />
        <video v-if="chosenFile && form.type === 'video'" class="chosen-video" :src="chosenFile" controls />
      </view>
    </template>

    <!-- 提交 -->
    <view class="btn-area">
      <button class="btn-primary" @click="submit" :disabled="submitting">
        {{ submitting ? '提交中...' : '创建内容' }}
      </button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { createTextContent, uploadContent } from '@/api/contents'

const types = [
  { label: '图片', value: 'image' },
  { label: '视频', value: 'video' },
  { label: '文字', value: 'text' }
]

const colors = ['#333333', '#ffffff', '#000000', '#e53935', '#1a73e8', '#34a853', '#ff9800', '#9c27b0']

const form = reactive({
  type: 'text',
  name: '',
  textContent: '',
  fontSize: 24,
  fontColor: '#333333',
  bgColor: '#ffffff',
  scrollSpeed: 5
})

const chosenFile = ref('')
const realFilePath = ref('')
const submitting = ref(false)

function chooseFile() {
  const isImg = form.type === 'image'
  const method = isImg ? uni.chooseImage : uni.chooseVideo
  const opts = isImg
    ? { count: 1, sourceType: ['album', 'camera'] }
    : { sourceType: ['album', 'camera'], maxDuration: 60 }

  method({
    ...opts,
    success: (res) => {
      const path = isImg ? res.tempFilePaths[0] : res.tempFilePath
      realFilePath.value = path
      chosenFile.value = path
    },
    fail: () => uni.showToast({ title: '选择失败', icon: 'none' })
  })
}

async function submit() {
  if (!form.name.trim()) { uni.showToast({ title: '请输入名称', icon: 'none' }); return }
  if (form.type === 'text' && !form.textContent.trim()) {
    uni.showToast({ title: '请输入文字内容', icon: 'none' }); return
  }
  if (form.type !== 'text' && !realFilePath.value) {
    uni.showToast({ title: '请选择文件', icon: 'none' }); return
  }
  submitting.value = true
  try {
    let res
    if (form.type === 'text') {
      res = await createTextContent({
        name: form.name,
        type: form.type,
        textContent: form.textContent,
        fontSize: form.fontSize,
        fontColor: form.fontColor,
        bgColor: form.bgColor,
        scrollSpeed: form.scrollSpeed
      })
    } else {
      res = await uploadContent(realFilePath.value, form.name, form.type)
    }
    if (res && res.code === 200) {
      uni.showToast({ title: '创建成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1000)
    }
  } catch (e) { /* toast 已显示 */ }
  finally { submitting.value = false }
}
</script>

<style lang="scss" scoped>
.container { min-height: 100vh; background: #f5f7fa; padding: 24rpx; }

.card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 20rpx; }
.card-title { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 20rpx; }

.type-tabs { display: flex; gap: 16rpx; }
.type-tab {
  flex: 1; height: 72rpx; line-height: 72rpx; text-align: center;
  border-radius: 12rpx; font-size: 28rpx; color: #666; background: #f5f5f5;
  &.active { background: #1a73e8; color: #fff; }
}

.form-item {
  display: flex; align-items: center; padding: 16rpx 0;
  &:not(:last-child) { border-bottom: 1rpx solid #f8f8f8; }
}
.label { font-size: 26rpx; color: #666; width: 140rpx; flex-shrink: 0; }
.input { flex: 1; font-size: 28rpx; text-align: right; }
.textarea { width: 100%; min-height: 160rpx; font-size: 28rpx; padding: 16rpx; background: #f9f9f9; border-radius: 12rpx; box-sizing: border-box; }
.slider { flex: 1; }
.picker-row { display: flex; align-items: center; justify-content: flex-end; }
.color-dot { width: 36rpx; height: 36rpx; border-radius: 8rpx; margin-right: 12rpx; border: 1rpx solid #ddd; }

.live-preview {
  padding: 24rpx; border-radius: 12rpx; min-height: 100rpx;
  word-break: break-all; line-height: 1.6; border: 1rpx solid #eee;
}

.upload-area {
  height: 200rpx; border: 2rpx dashed #ccc; border-radius: 16rpx;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  .upload-icon { font-size: 60rpx; color: #ccc; line-height: 1; }
  .upload-text { font-size: 26rpx; color: #999; margin-top: 8rpx; }
}
.chosen-img { width: 100%; border-radius: 12rpx; margin-top: 16rpx; }
.chosen-video { width: 100%; border-radius: 12rpx; margin-top: 16rpx; }

.btn-area { padding: 32rpx 0; }
.btn-primary {
  width: 100%; height: 88rpx; line-height: 88rpx;
  background: #1a73e8; color: #fff; font-size: 30rpx;
  border-radius: 16rpx; text-align: center;
  &[disabled] { opacity: 0.6; }
}
</style>
