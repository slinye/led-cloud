<template>
  <div class="forbidden-page">
    <el-result icon="warning" title="403" sub-title="抱歉，您没有权限访问该页面">
      <template #extra>
        <el-button type="primary" @click="$router.push('/dashboard')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
</script>

<style scoped>
.forbidden-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
}
</style>
