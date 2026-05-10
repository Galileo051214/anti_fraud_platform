<template>
  <div class="layout-container">
    <header class="layout-header">
      <div class="header-content">
        <div class="logo" @click="$router.push('/home')">
          <span class="logo-icon"><IconShield :size="28" /></span>
          <span class="logo-text">反诈骗学习平台</span>
        </div>
        <nav class="nav-menu">
          <router-link v-for="item in menuList" :key="item.path" :to="item.path" class="nav-item">
            {{ item.name }}
          </router-link>
        </nav>
        <div class="header-actions">
          <template v-if="userStore.isLoggedIn">
            <div class="score-badge" @click="router.push('/score')">
              <Coin class="score-badge__icon" />
              <span class="score-badge__num">{{ scoreNum }}</span>
            </div>
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                  {{ userStore.userInfo?.nickname?.[0] || userStore.userInfo?.username?.[0] }}
                </el-avatar>
                <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="score">积分中心</el-dropdown-item>
                  <el-dropdown-item command="achievement">我的成就</el-dropdown-item>
                  <el-dropdown-item command="rankings">排行榜</el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" command="admin">返回后台</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" @click="$router.push('/login')">登录</el-button>
            <el-button @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>
    <main class="layout-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useScoreStore } from '@/stores/score'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Coin } from '@element-plus/icons-vue'
import { IconShield } from '@/components/icons'

const router = useRouter()
const userStore = useUserStore()
const scoreStore = useScoreStore()
const scoreNum = computed(() => scoreStore.scoreInfo?.totalScore ?? 0)

const menuList = [
  { path: '/home', name: '首页' },
  { path: '/news', name: '资讯中心' },
  { path: '/case', name: '案例展示' },
  { path: '/challenge', name: '知识闯关' },
  { path: '/forum', name: '社区' },
  { path: '/chat', name: '智能客服' },
  { path: '/recommend', name: '智能推荐' }
]

const handleCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'rankings':
      router.push('/ranking')
      break
    case 'score':
      router.push('/score')
      break
    case 'achievement':
      router.push('/achievement')
      break
    case 'admin':
      router.push('/admin/dashboard')
      break
    case 'logout':
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      userStore.clearUser()
      router.push('/login')
      break
  }
}

onMounted(async () => {
  if (userStore.isLoggedIn) {
    try {
      await scoreStore.fetchScoreInfo()
    } catch {
      // ignore, scoreNum由scoreStore驱动
    }
  }
})
</script>

<style scoped lang="scss">
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;

  .layout-header {
    background: #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    position: sticky;
    top: 0;
    z-index: 100;

    .header-content {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 20px;
      height: 60px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .logo {
      display: flex;
      align-items: center;
      cursor: pointer;
      user-select: none;

      .logo-icon {
        font-size: 28px;
        margin-right: 8px;
      }

      .logo-text {
        font-size: 18px;
        font-weight: 600;
        color: #303133;
      }
    }

    .nav-menu {
      display: flex;
      gap: 8px;

      .nav-item {
        padding: 8px 16px;
        color: #606266;
        border-radius: 4px;
        transition: all 0.3s;

        &:hover {
          color: #409eff;
          background: rgba(64, 158, 255, 0.1);
        }

        &.router-link-active {
          color: #409eff;
          background: rgba(64, 158, 255, 0.1);
        }
      }
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .score-badge {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      background: hsl(45, 90%, 95%);
      border-radius: 16px;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        background: hsl(45, 90%, 90%);
      }

      &__icon {
        width: 18px;
        height: 18px;
        color: hsl(45, 90%, 50%);
      }

      &__num {
        font-size: 14px;
        font-weight: 600;
        color: hsl(40, 80%, 40%);
      }
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 4px;
      transition: background 0.3s;

      &:hover {
        background: #f5f7fa;
      }

      .username {
        color: #606266;
        font-size: 14px;
      }
    }
  }

  .layout-main {
    flex: 1;
    max-width: 1200px;
    width: 100%;
    margin: 0 auto;
    padding: 20px;
  }
}
</style>
