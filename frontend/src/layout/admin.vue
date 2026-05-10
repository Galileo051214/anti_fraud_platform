<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <span class="logo-icon"><IconShield :size="24" /></span>
        <span class="logo-text">管理后台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1a1a2e"
        text-color="#a0a0a0"
        active-text-color="#409eff"
      >
        <el-menu-item index="/admin/dashboard">
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/admin/user">
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/news">
          <span>资讯管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/case">
          <span>案例管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/challenge">
          <span>关卡管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/forum">
          <span>帖子管理</span>
        </el-menu-item>
        <el-divider />
        <el-menu-item @click="goHome">
          <span>返回前台</span>
        </el-menu-item>
        <el-menu-item @click="handleLogout">
          <span>退出登录</span>
        </el-menu-item>
      </el-menu>
    </aside>
    <div class="main-container">
      <header class="topbar">
        <div class="topbar-title">{{ route.meta.title }}</div>
        <div class="topbar-actions">
          <el-avatar :size="32" :src="userStore.userInfo?.avatar">
            {{ userStore.userInfo?.nickname?.[0] }}
          </el-avatar>
          <span class="username">{{ userStore.userInfo?.nickname }}</span>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { IconShield } from '@/components/icons'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const goHome = () => {
  router.push('/home')
}

const handleLogout = () => {
  userStore.clearUser()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 220px;
  background: #1a1a2e;
  overflow-y: auto;

  .sidebar-header {
    height: 60px;
    display: flex;
    align-items: center;
    padding: 0 20px;
    color: #fff;

    .logo-icon {
      font-size: 24px;
      margin-right: 8px;
    }

    .logo-text {
      font-size: 16px;
      font-weight: 600;
    }
  }

  :deep(.el-menu) {
    border-right: none;
  }

  :deep(.el-divider) {
    margin: 8px 0;
    background: #2d2d44;
  }
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .topbar {
    height: 60px;
    background: #fff;
    border-bottom: 1px solid #e4e7ed;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;

    .topbar-title {
      font-size: 16px;
      font-weight: 500;
      color: #303133;
    }

    .topbar-actions {
      display: flex;
      align-items: center;
      gap: 8px;

      .username {
        color: #606266;
        font-size: 14px;
      }
    }
  }

  .content {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
    background: #f5f7fa;
  }
}
</style>
