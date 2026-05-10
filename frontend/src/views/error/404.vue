<template>
  <div class="page-404">
    <div class="content">
      <h1>404</h1>
      <p>抱歉，您访问的页面不存在</p>
      <el-button type="primary" @click="$router.push('/home')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts"></script>

<style scoped lang="scss">
.page-404 {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;

  .content {
    text-align: center;

    h1 {
      font-size: 120px;
      color: #409eff;
      margin-bottom: 16px;
    }

    p {
      font-size: 18px;
      color: #606266;
      margin-bottom: 24px;
    }
  }
}
</style>
