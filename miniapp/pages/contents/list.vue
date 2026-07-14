<template>
  <view class="container">
    <scroll-view class="list" scroll-y @scrolltolower="loadMore" v-if="isLoggedIn">
      <view class="list-item" v-for="item in list" :key="item.id">
        <view class="item-main">
          <view class="item-type">{{ typeLabel(item.type) }}</view>
          <view class="item-name">{{ item.name }}</view>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="loading" v-if="loading">加载中...</view>
      <view class="no-more" v-if="!hasMore">— 没有更多了 —</view>
    </scroll-view>

    <view class="empty-login" v-else>
      <view class="tip">请先登录</view>
      <button class="login-btn" @tap="handleLogin">微信一键登录</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getContentList } from '@/api/contents'

const userStore = useUserStore()
const isLoggedIn = ref(false)

const list = ref([])
const page = ref(1)
const loading = ref(false)
const hasMore = ref(true)

onShow(() => {
  userStore.initLoginState()
  isLoggedIn.value = userStore.isLoggedIn
  if (isLoggedIn.value) {
    page.value = 1
    list.value = []
    hasMore.value = true
    loadData()
  }
})

function typeLabel(type) {
  const map = { image: '图片', video: '视频', text: '文字' }
  return map[type] || type
}

async function loadData() {
  if (loading.value || !hasMore.value) return
  loading.value = true
  try {
    const res = await getContentList(page.value, 20)
    if (res.code === 200) {
      const records = res.data.records || []
      list.value = page.value === 1 ? records : [...list.value, ...records]
      hasMore.value = records.length === 20
      page.value++
    }
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function loadMore() { loadData() }
function handleLogin() { uni.switchTab({ url: '/pages/index/index' }) }
</script>

<style lang="scss" scoped>
.container { min-height: 100vh; background: #f5f7fa; }
.list { padding: 20rpx 24rpx; }
.list-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 16rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.item-type {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  background: #e8f0fe;
  color: #1a73e8;
  display: inline-block;
  margin-bottom: 10rpx;
}
.item-name { font-size: 30rpx; font-weight: 500; color: #333; }
.arrow { font-size: 32rpx; color: #ccc; }
.loading, .no-more { text-align: center; padding: 30rpx; color: #999; font-size: 24rpx; }
.empty-login {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;
  .tip { font-size: 30rpx; color: #999; margin-bottom: 40rpx; }
  .login-btn {
    width: 460rpx; height: 88rpx; line-height: 88rpx;
    background: #07c160; color: #fff; font-size: 32rpx;
    border-radius: 44rpx; text-align: center;
  }
}
</style>
