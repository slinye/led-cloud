<template>
  <view class="stat-card" :style="{ '--card-color': color }">
    <view class="stat-icon">{{ icon }}</view>
    <view class="stat-value">{{ value }}</view>
    <view class="stat-label">{{ label }}</view>
  </view>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '' },
  value: { type: [Number, String], default: 0 },
  label: { type: String, default: '' },
  color: { type: String, default: '#1a73e8' }
})
</script>

<style lang="scss" scoped>
.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx 20rpx;
  text-align: center;
  border-top: 4rpx solid var(--card-color);
}
.stat-icon { font-size: 40rpx; margin-bottom: 12rpx; }
.stat-value { font-size: 36rpx; font-weight: 700; color: var(--card-color); }
.stat-label { font-size: 22rpx; color: #999; margin-top: 6rpx; }
</style>
