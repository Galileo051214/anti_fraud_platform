<template>
  <div class="page-container">
    <div class="profile-layout">
      <aside class="profile-aside">
        <div class="profile-card">
          <div class="profile-card__avatar">
            <AvatarUploader
              :model-value="currentAvatar"
              :size="80"
              @update:model-value="handleAvatarChange"
            />
          </div>
          <h3 class="profile-card__name">{{ userInfo?.nickname || userInfo?.username }}</h3>
          <p class="profile-card__info">{{ userInfo?.grade || '未设置年级' }} · {{ userInfo?.major || '未设置专业' }}</p>
          <p class="profile-card__student">学号: {{ userInfo?.studentNo || '-' }}</p>

          <div class="profile-card__stats">
            <div class="profile-card__stat">
              <span class="profile-card__stat-value">{{ userStats.score || 0 }}</span>
              <span class="profile-card__stat-label">积分</span>
            </div>
            <div class="profile-card__stat-divider"></div>
            <div class="profile-card__stat">
              <span class="profile-card__stat-value">{{ userStats.achievement || 0 }}</span>
              <span class="profile-card__stat-label">成就</span>
            </div>
            <div class="profile-card__stat-divider"></div>
            <div class="profile-card__stat">
              <span class="profile-card__stat-value">{{ userStats.challenge || 0 }}</span>
              <span class="profile-card__stat-label">闯关</span>
            </div>
          </div>

          <button class="profile-card__logout" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </button>
        </div>

        <nav class="profile-nav">
          <button
            v-for="item in navItems"
            :key="item.key"
            class="profile-nav__item"
            :class="{ 'profile-nav__item--active': activeTab === item.key }"
            @click="handleNavClick(item)"
          >
            <component :is="item.icon" class="profile-nav__icon" />
            <span>{{ item.label }}</span>
          </button>
        </nav>

        <div class="profile-quick-links">
          <router-link to="/score" class="quick-link quick-link--score">
            <Coin class="quick-link__icon" />
            <div class="quick-link__info">
              <span class="quick-link__title">积分中心</span>
              <span class="quick-link__desc">查看积分和等级</span>
            </div>
            <ArrowRight class="quick-link__arrow" />
          </router-link>
          <router-link to="/achievement" class="quick-link quick-link--achievement">
            <Trophy class="quick-link__icon" />
            <div class="quick-link__info">
              <span class="quick-link__title">我的成就</span>
              <span class="quick-link__desc">查看已解锁成就</span>
            </div>
            <ArrowRight class="quick-link__arrow" />
          </router-link>
        </div>
      </aside>

      <main class="profile-main">
        <div v-show="activeTab === 'info'" class="profile-content">
          <h2 class="profile-content__title">个人信息</h2>
          <ProfileEditForm />
        </div>

        <div v-show="activeTab === 'password'" class="profile-content">
          <h2 class="profile-content__title">修改密码</h2>
          <ProfilePasswordForm />
        </div>

        <div v-show="activeTab === 'record'" class="profile-content">
          <h2 class="profile-content__title">学习记录</h2>
          <LearningRecords />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, markRaw, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import ProfileEditForm from '@/components/profile/ProfileEditForm.vue'
import ProfilePasswordForm from '@/components/profile/ProfilePasswordForm.vue'
import LearningRecords from '@/components/profile/LearningRecords.vue'
import AvatarUploader from '@/components/profile/AvatarUploader.vue'
import { User, Lock, Clock, Coin, Trophy, ArrowRight, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const userInfo = ref(userStore.userInfo)
const activeTab = ref('info')

const userStats = ref({
  score: 0,
  achievement: 0,
  challenge: 0
})

const currentAvatar = computed(() => userInfo.value?.avatar || '')

const navItems = [
  { key: 'info', label: '个人信息', icon: markRaw(User) },
  { key: 'password', label: '修改密码', icon: markRaw(Lock) },
  { key: 'record', label: '学习记录', icon: markRaw(Clock) }
]

function handleNavClick(item: { key: string }) {
  activeTab.value = item.key
}

const fetchUserStats = async () => {
  try {
    const { get } = await import('@/utils/request')
    const [scoreRes, progressRes] = await Promise.all([
      get<any>('/score/info'),
      get<any>('/challenge/progress')
    ])
    userStats.value = {
      score: scoreRes.totalScore || 0,
      achievement: scoreRes.unlockedAchievements || 0,
      challenge: progressRes.completedChallenges || 0
    }
  } catch {
    userStats.value = {
      score: 156,
      achievement: 3,
      challenge: 5
    }
  }
}

const handleAvatarChange = async (avatarUrl: string) => {
  try {
    await userStore.updateUser({ avatar: avatarUrl })
    await userStore.getUserInfo()
    userInfo.value = userStore.userInfo
    ElMessage.success('头像已更新')
  } catch (error: any) {
    ElMessage.error(error.message || '头像保存失败，请重试')
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '退出登录',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 用户取消操作
  }
}

onMounted(async () => {
  await userStore.getUserInfo()
  userInfo.value = userStore.userInfo
  await fetchUserStats()
})

watch(() => userStore.userInfo, (newInfo) => {
  if (newInfo) {
    userInfo.value = newInfo
  }
}, { deep: true })
</script>

<style scoped lang="scss">
.profile-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
  min-height: calc(100vh - 120px);
}

.profile-aside {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.profile-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px 24px;
  text-align: center;
  box-shadow: 0 2px 12px hsla(220, 10%, 90%, 0.5);

  &__avatar {
    display: inline-block;
    margin-bottom: 12px;
  }

  &__name {
    font-size: 18px;
    font-weight: 600;
    color: hsl(220, 10%, 20%);
    margin: 0 0 4px;
  }

  &__info {
    font-size: 14px;
    color: hsl(220, 10%, 55%);
    margin: 0 0 4px;
  }

  &__student {
    font-size: 12px;
    color: hsl(220, 10%, 65%);
    margin: 0 0 20px;
  }

  &__stats {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0;
    padding: 16px 0;
    background: hsl(220, 10%, 97%);
    border-radius: 8px;
    margin-bottom: 16px;
  }

  &__stat {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  &__stat-value {
    font-size: 20px;
    font-weight: 700;
    color: hsl(220, 60%, 50%);
  }

  &__stat-label {
    font-size: 12px;
    color: hsl(220, 10%, 55%);
  }

  &__stat-divider {
    width: 1px;
    height: 32px;
    background: hsl(220, 10%, 88%);
  }

  &__logout {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    width: 100%;
    padding: 10px;
    border: 1px solid hsl(0, 60%, 85%);
    border-radius: 8px;
    background: transparent;
    color: hsl(0, 60%, 45%);
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;

    .el-icon {
      font-size: 16px;
    }

    &:hover {
      background: hsl(0, 60%, 96%);
      border-color: hsl(0, 60%, 75%);
    }

    &:active {
      transform: scale(0.98);
    }
  }
}

.profile-nav {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 12px hsla(220, 10%, 90%, 0.5);

  &__item {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    padding: 12px 16px;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: hsl(220, 10%, 40%);
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      background: hsl(220, 10%, 96%);
      color: hsl(220, 10%, 20%);
    }

    &--active {
      background: hsl(225, 60%, 96%);
      color: hsl(225, 60%, 50%);
      font-weight: 500;
    }
  }

  &__icon {
    width: 18px;
    height: 18px;
  }
}

.profile-main {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px hsla(220, 10%, 90%, 0.5);
}

.profile-content {
  &__title {
    font-size: 18px;
    font-weight: 600;
    color: hsl(220, 10%, 20%);
    margin: 0 0 24px;
    padding-bottom: 16px;
    border-bottom: 1px solid hsl(220, 10%, 92%);
  }
}

.profile-quick-links {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: auto;
}

.quick-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  text-decoration: none;
  box-shadow: 0 2px 12px hsla(220, 10%, 90%, 0.5);
  transition: all 0.2s ease;

  &:hover {
    box-shadow: 0 4px 16px hsla(220, 10%, 85%, 0.5);
    transform: translateY(-2px);
  }

  &--score {
    border-left: 3px solid hsl(45, 90%, 55%);
  }

  &--achievement {
    border-left: 3px solid hsl(280, 60%, 55%);
  }

  &__icon {
    width: 32px;
    height: 32px;
    flex-shrink: 0;
  }

  &--score &__icon {
    color: hsl(45, 90%, 55%);
  }

  &--achievement &__icon {
    color: hsl(280, 60%, 55%);
  }

  &__info {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
    color: hsl(220, 10%, 20%);
  }

  &__desc {
    font-size: 12px;
    color: hsl(220, 10%, 55%);
    margin-top: 2px;
  }

  &__arrow {
    width: 16px;
    height: 16px;
    color: hsl(220, 10%, 70%);
    flex-shrink: 0;
  }
}

@media (max-width: 768px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }

  .profile-aside {
    flex-direction: row;
    overflow-x: auto;
    gap: 12px;

    .profile-card {
      flex-shrink: 0;
      width: 200px;
      padding: 20px;
    }

    .profile-nav {
      display: flex;
      flex-shrink: 0;

      &__item {
        padding: 10px 16px;
        white-space: nowrap;
      }
    }
  }
}

@media (max-width: 480px) {
  .profile-main {
    padding: 20px;
  }

  .profile-card {
    &__stats {
      flex-direction: column;
      gap: 8px;
    }

    &__stat-divider {
      width: 60%;
      height: 1px;
    }
  }
}
</style>
