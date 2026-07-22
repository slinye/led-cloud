<template>
  <view class="container">
    <!-- 顶部操作栏 -->
    <view class="top-bar" v-if="isLoggedIn">
      <view class="filter-row">
        <view
          v-for="f in filters" :key="f.value"
          class="filter-tag" :class="{ active: curStatus === f.value }"
          @click="changeStatus(f.value)"
        >{{ f.label }}</view>
      </view>
      <button class="btn-add" @click="goAdd" v-if="canEdit">+ 新建</button>
    </view>

    <!-- 列表 -->
    <scroll-view class="list" scroll-y @scrolltolower="loadMore" v-if="isLoggedIn">
      <view class="list-item" v-for="item in list" :key="item.id" @click="goDetail(item.id)">
        <view class="item-main">
          <view class="item-name">{{ item.name }}</view>
          <view class="item-desc" v-if="item.description">{{ item.description }}</view>
          <view class="item-desc" v-else>无描述</view>
        </view>
        <view class="item-right">
          <view class="item-status" :class="'status-' + item.status">
            {{ statusLabel(item.status) }}
          </view>
          <text class="arrow">›</text>
        </view>
      </view>
      <view class="loading" v-if="loading">加载中...</view>
      <view class="no-more" v-if="!hasMore && list.length>0">— 没有更多了 —</view>
      <view class="empty" v-if="!loading && list.length===0">暂无节目</view>
    </scroll-view>

    <view class="empty-login" v-else>
      <view class="tip">请先登录</view>
      <button class="login-btn" @click="handleLogin">微信一键登录</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getProgramList } from '@/api/programs'

const userStore = useUserStore()
const isLoggedIn = ref(false)
const canEdit = computed(() =>
  ['ADMIN', 'OPERATOR'].includes(userStore.role)
)

const filters = [
  { label: '全部', value: '' },
  { label: '草稿', value: 'draft' },
  { label: '已发布', value: 'published' },
  { label: '播放中', value: 'playing' },
  { label: '暂停', value: 'paused' }
]
const curStatus = ref('')

const list = ref([])
const page = ref(1)
const loading = ref(false)
const hasMore = ref(true)

onShow(() => {
  userStore.initLoginState()
  isLoggedIn.value = userStore.isLoggedIn
  if (isLoggedIn.value) {
    page.value = 1; list.value = []; hasMore.value = true
    loadData()
  }
})

function statusLabel(s) {
  return { draft: '草稿', published: '已发布', playing: '播放中', paused: '已暂停' }[s] || s
}

function changeStatus(s) {
  curStatus.value = s
  page.value = 1; list.value = []; hasMore.value = true
  loadData()
}

async function loadData() {
  if (loading.value || !hasMore.value) return
  loading.value = true
  try {
    const res = await getProgramList(page.value, 20, '', true)
    if (res.code === 200) {
      let records = res.data.records || []
      if (curStatus.value) records = records.filter(r => r.status === curStatus.value)
      list.value = page.value === 1 ? records : [...list.value, ...records]
      hasMore.value = records.length === 20
      page.value++
    }
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
  } finally { loading.value = false }
}

function loadMore() { loadData() }
function goDetail(id) { uni.navigateTo({ url: `/pages/programs/detail?id=${id}` }) }
function goAdd() { uni.navigateTo({ url: '/pages/programs/add' }) }
function handleLogin() { uni.switchTab({ url: '/pages/index/index' }) }
</script>

<style lang="scss" scoped>
.container { min-height: 100vh; background: #f5f7fa; }
.top-bar {
  padding: 20rpx 24rpx 12rpx; display: flex; align-items: center;
  justify-content: space-between; background: #fff;
  position: sticky; top: 0; z-index: 10;
}
.filter-row { display: flex; gap: 10rpx; flex-wrap: wrap; }
.filter-tag {
  padding: 6rpx 20rpx; border-radius: 20rpx; font-size: 22rpx;
  background: #f0f0f0; color: #666;
  &.active { background: #7b1fa2; color: #fff; }
}
.btn-add {
  height: 56rpx; line-height: 56rpx; padding: 0 24rpx;
  background: #7b1fa2; color: #fff; font-size: 24rpx; border-radius: 28rpx;
}
.list { padding: 0 24rpx; }
.list-item {
  background: #fff; border-radius: 16rpx; padding: 24rpx;
  margin-bottom: 14rpx; display: flex; align-items: center; justify-content: space-between;
}
.item-main { flex: 1; }
.item-name { font-size: 30rpx; font-weight: 500; color: #333; }
.item-desc { font-size: 24rpx; color: #999; margin-top: 6rpx; }
.item-right { display: flex; align-items: center; }
.item-status {
  font-size: 22rpx; padding: 6rpx 18rpx; border-radius: 20rpx; margin-right: 12rpx;
  &.status-draft { background: #f0f0f0; color: #999; }
  &.status-published { background: #e8f0fe; color: #1a73e8; }
  &.status-playing { background: #e6f4ea; color: #34a853; }
  &.status-paused { background: #fef7e0; color: #ff9800; }
}
.arrow { font-size: 32rpx; color: #ccc; }
.loading, .no-more, .empty { text-align: center; padding: 40rpx; color: #999; font-size: 24rpx; }
.empty-login {
  display: flex; flex-direction: column; align-items: center; padding-top: 200rpx;
  .tip { font-size: 30rpx; color: #999; margin-bottom: 40rpx; }
  .login-btn {
    width: 460rpx; height: 88rpx; line-height: 88rpx;
    background: #07c160; color: #fff; font-size: 32rpx; border-radius: 44rpx; text-align: center;
  }
}
</style>
