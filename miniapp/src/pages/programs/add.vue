<template>
  <view class="container">
    <!-- 基本信息 -->
    <view class="card">
      <view class="card-title">基本信息</view>
      <view class="form-item">
        <text class="label">节目名称</text>
        <input class="input" v-model="form.name" placeholder="请输入节目名称" />
      </view>
      <view class="form-item">
        <text class="label">描述</text>
        <input class="input" v-model="form.description" placeholder="可选" />
      </view>
    </view>

    <!-- 已选内容 -->
    <view class="card">
      <view class="card-header">
        <view class="card-title">播放内容（{{ selected.length }}条）</view>
        <button class="btn-pick" @click="pickerVisible=true">+ 添加</button>
      </view>
      <view v-if="selected.length===0" class="empty-tip">请添加播放内容</view>
      <view v-for="(it, idx) in selected" :key="it.contentId" class="sel-item">
        <view class="sel-order">
          <text class="order-num">{{ idx + 1 }}</text>
          <view class="order-arrows">
            <text class="arr" :class="{ dim: idx===0 }" @click="move(idx, -1)">▲</text>
            <text class="arr" :class="{ dim: idx===selected.length-1 }" @click="move(idx, 1)">▼</text>
          </view>
        </view>
        <view class="sel-info">
          <view class="sel-type" :class="'type-' + (it.contentType || 'text')">
            {{ typeLabel(it.contentType) }}
          </view>
          <view class="sel-name">{{ it.contentName || '#' + it.contentId }}</view>
        </view>
        <view class="sel-right">
          <input class="dur-input" v-model.number="it.duration" type="number" placeholder="5" />
          <text class="dur-unit">秒</text>
          <text class="sel-del" @click="remove(idx)">×</text>
        </view>
      </view>
    </view>

    <!-- 提交 -->
    <view class="btn-area">
      <button class="btn-primary" @click="submit" :disabled="submitting">
        {{ submitting ? '提交中...' : '创建节目' }}
      </button>
    </view>

    <!-- 内容选择弹窗 -->
    <view class="overlay" v-if="pickerVisible" @click="pickerVisible=false">
      <view class="picker-card" @click.stop>
        <view class="picker-title">选择内容</view>
        <scroll-view class="picker-list" scroll-y>
          <view class="picker-item" v-for="c in contents" :key="c.id"
            :class="{ disabled: isSelected(c.id) }"
            @click="toggleContent(c)"
          >
            <view class="p-type" :class="'type-' + c.type">{{ typeLabel(c.type) }}</view>
            <text class="p-name">{{ c.name }}</text>
            <view class="p-check" v-if="isSelected(c.id)">✓</view>
          </view>
          <view class="picker-loading" v-if="loadingContents">加载中...</view>
        </scroll-view>
        <view class="picker-btns">
          <button class="btn-cancel" @click="pickerVisible=false">完成</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { createProgram } from '@/api/programs'
import { getContentList } from '@/api/contents'

const form = reactive({ name: '', description: '' })
const selected = ref([])
const submitting = ref(false)

// 内容选择
const pickerVisible = ref(false)
const contents = ref([])
const loadingContents = ref(false)

async function loadContents() {
  loadingContents.value = true
  try {
    const res = await getContentList(1, 200, '', true)
    if (res.code === 200) {
      contents.value = res.data.records || []
    }
  } catch (e) { /* silent */ }
  finally { loadingContents.value = false }
}

function typeLabel(t) {
  return { image: '图片', video: '视频', text: '文字' }[t] || '内容'
}

function isSelected(contentId) {
  return selected.value.some(it => it.contentId === contentId)
}

function toggleContent(c) {
  if (isSelected(c.id)) {
    selected.value = selected.value.filter(it => it.contentId !== c.id)
  } else {
    selected.value.push({
      contentId: c.id,
      contentName: c.name,
      contentType: c.type,
      sortOrder: selected.value.length,
      duration: 5
    })
  }
}

function move(idx, dir) {
  const nidx = idx + dir
  if (nidx < 0 || nidx >= selected.value.length) return
  ;[selected.value[idx], selected.value[nidx]] = [selected.value[nidx], selected.value[idx]]
  selected.value.forEach((it, i) => it.sortOrder = i)
}

function remove(idx) {
  selected.value.splice(idx, 1)
  selected.value.forEach((it, i) => it.sortOrder = i)
}

async function submit() {
  if (!form.name.trim()) { uni.showToast({ title: '请输入名称', icon: 'none' }); return }
  if (selected.value.length === 0) { uni.showToast({ title: '请添加内容', icon: 'none' }); return }
  submitting.value = true
  try {
    const data = {
      name: form.name,
      description: form.description,
      status: 'draft',
      items: selected.value.map(it => ({
        contentId: it.contentId,
        sortOrder: it.sortOrder,
        duration: it.duration || 5
      }))
    }
    const res = await createProgram(data)
    if (res.code === 200) {
      uni.showToast({ title: '创建成功', icon: 'success' })
      setTimeout(() => uni.navigateBack(), 1000)
    }
  } catch (e) { /* toast */ }
  finally { submitting.value = false }
}

// 打开选择器时加载
const origPickerVisible = pickerVisible
// 用 watch 效果 - 简单用 ref 监听
import { watch } from 'vue'
watch(pickerVisible, (v) => {
  if (v && contents.value.length === 0) loadContents()
})
</script>

<style lang="scss" scoped>
.container { min-height: 100vh; background: #f5f7fa; padding: 24rpx; }

.card { background: #fff; border-radius: 20rpx; padding: 28rpx; margin-bottom: 20rpx; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.card-title { font-size: 28rpx; font-weight: 600; color: #333; }
.empty-tip { text-align: center; padding: 40rpx 0; color: #999; font-size: 26rpx; }

.form-item { display: flex; align-items: center; padding: 16rpx 0;
  &:not(:last-child) { border-bottom: 1rpx solid #f8f8f8; }
}
.label { font-size: 26rpx; color: #666; width: 120rpx; flex-shrink: 0; }
.input { flex: 1; font-size: 28rpx; text-align: right; }

.btn-pick {
  height: 52rpx; line-height: 52rpx; padding: 0 20rpx;
  background: #1a73e8; color: #fff; font-size: 24rpx; border-radius: 26rpx;
}

.sel-item {
  display: flex; align-items: center; padding: 18rpx 0;
  &:not(:last-child) { border-bottom: 1rpx solid #f8f8f8; }
}
.sel-order {
  width: 56rpx; display: flex; flex-direction: column; align-items: center; margin-right: 16rpx;
}
.order-num { font-size: 26rpx; font-weight: 600; color: #1a73e8; }
.order-arrows { display: flex; gap: 4rpx; margin-top: 4rpx; }
.arr { font-size: 18rpx; color: #1a73e8; padding: 2rpx; &.dim { color: #ccc; } }
.sel-info { flex: 1; }
.sel-type { font-size: 20rpx; padding: 2rpx 12rpx; border-radius: 6rpx; display: inline-block; margin-bottom: 4rpx;
  &.type-image { background: #e8f0fe; color: #1a73e8; }
  &.type-video { background: #fce8e6; color: #ea4335; }
  &.type-text { background: #e6f4ea; color: #34a853; }
}
.sel-name { font-size: 28rpx; color: #333; }
.sel-right { display: flex; align-items: center; gap: 6rpx; }
.dur-input { width: 60rpx; height: 48rpx; background: #f5f5f5; border-radius: 8rpx; text-align: center; font-size: 24rpx; }
.dur-unit { font-size: 22rpx; color: #999; }
.sel-del { font-size: 36rpx; color: #e53935; padding: 0 8rpx; line-height: 1; }

.btn-area { padding: 24rpx 0; }
.btn-primary {
  width: 100%; height: 88rpx; line-height: 88rpx; background: #1a73e8; color: #fff;
  font-size: 30rpx; border-radius: 16rpx; text-align: center;
  &[disabled] { opacity: 0.6; }
}

/* 内容选择弹窗 */
.overlay { position: fixed; top:0; left:0; right:0; bottom:0; background: rgba(0,0,0,0.4); z-index:999; display: flex; align-items: flex-end; justify-content: center; }
.picker-card { width: 90vw; max-height: 75vh; background: #fff; border-radius: 24rpx 24rpx 0 0; display: flex; flex-direction: column; }
.picker-title { font-size: 32rpx; font-weight: 600; text-align: center; padding: 28rpx; }
.picker-list { flex: 1; padding: 0 24rpx; max-height: 55vh; }
.picker-item { display: flex; align-items: center; padding: 18rpx 0; border-bottom: 1rpx solid #f0f0f0;
  &.disabled { opacity: 0.4; }
}
.p-type { font-size: 20rpx; padding: 2rpx 12rpx; border-radius: 6rpx; margin-right: 14rpx;
  &.type-image { background: #e8f0fe; color: #1a73e8; }
  &.type-video { background: #fce8e6; color: #ea4335; }
  &.type-text { background: #e6f4ea; color: #34a853; }
}
.p-name { flex: 1; font-size: 28rpx; }
.p-check { color: #1a73e8; font-size: 32rpx; font-weight: 700; }
.picker-loading { text-align: center; padding: 40rpx; color: #999; }
.picker-btns { padding: 20rpx 24rpx; border-top: 1rpx solid #f0f0f0; }
.btn-cancel { width: 100%; height: 80rpx; line-height: 80rpx; background: #f5f5f5; color: #666; border-radius: 12rpx; font-size: 28rpx; text-align: center; }
</style>
