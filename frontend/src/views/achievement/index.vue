<template>
  <div class="page-container">
    <div class="achievement-layout">
      <div class="achievement-header">
        <div class="achievement-header__back" @click="goBack">
          <ArrowLeft class="achievement-header__icon" />
          <span>返回</span>
        </div>
        <h1 class="achievement-header__title">我的成就</h1>
      </div>

      <div class="achievement-summary">
        <div class="summary-card">
          <Trophy class="summary-card__icon" />
          <div class="summary-card__info">
            <span class="summary-card__num">{{ unlockedCount }}</span>
            <span class="summary-card__label">已解锁</span>
          </div>
        </div>
        <div class="summary-card summary-card--locked">
          <Lock class="summary-card__icon" />
          <div class="summary-card__info">
            <span class="summary-card__num">{{ lockedCount }}</span>
            <span class="summary-card__label">待解锁</span>
          </div>
        </div>
      </div>

      <div class="achievement-tabs">
        <button
          class="achievement-tabs__item"
          :class="{ 'achievement-tabs__item--active': activeTab === 'all' }"
          @click="activeTab = 'all'"
        >
          全部
        </button>
        <button
          class="achievement-tabs__item"
          :class="{ 'achievement-tabs__item--active': activeTab === 'unlocked' }"
          @click="activeTab = 'unlocked'"
        >
          已解锁
        </button>
        <button
          class="achievement-tabs__item"
          :class="{ 'achievement-tabs__item--active': activeTab === 'locked' }"
          @click="activeTab = 'locked'"
        >
          未解锁
        </button>
      </div>

      <div v-if="loading" class="achievement-skeleton">
        <div v-for="i in 6" :key="i" class="skeleton skeleton--item"></div>
      </div>

      <div v-else class="achievement-grid">
        <div
          v-for="item in filteredAchievements"
          :key="item.id"
          class="achievement-item"
          :class="{ 'achievement-item--unlocked': item.unlocked }"
        >
          <div class="achievement-item__badge">
            <Trophy v-if="item.unlocked" class="achievement-item__icon" />
            <Lock v-else class="achievement-item__icon achievement-item__icon--locked" />
          </div>
          <div class="achievement-item__content">
            <h3 class="achievement-item__name">{{ item.name }}</h3>
            <p class="achievement-item__desc">{{ item.description }}</p>
            <div class="achievement-item__meta">
              <span class="achievement-item__reward">
                <Present class="achievement-item__reward-icon" />
                +{{ item.scoreReward }}积分
              </span>
              <span class="achievement-item__condition">
                {{ getConditionText(item.conditionType, item.conditionValue) }}
              </span>
            </div>
          </div>
          <div v-if="item.unlocked" class="achievement-item__status">
            <Check class="achievement-item__check" />
          </div>
        </div>

        <div v-if="filteredAchievements.length === 0" class="achievement-empty">
          <FolderOpened class="achievement-empty__icon" />
          <p class="achievement-empty__text">{{ emptyHint }}</p>
        </div>
      </div>

      <div class="achievement-tips">
        <h3 class="achievement-tips__title">
          <InfoFilled class="achievement-tips__icon" />
          成就说明
        </h3>
        <ul class="achievement-tips__list">
          <li>当前线上积分主要来自「闯关通关」；通关后会按条件尝试解锁成就</li>
          <li>解锁成就会额外发放积分，并计入总积分与等级进度</li>
          <li>成就一旦解锁永久有效</li>
          <li>浏览案例、发帖等规则后续接入后将自动参与统计</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useScoreStore } from '@/stores/score'
import {
  ArrowLeft,
  Trophy,
  Lock,
  Present,
  Check,
  FolderOpened,
  InfoFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const scoreStore = useScoreStore()
const loading = ref(true)
const activeTab = ref<'all' | 'unlocked' | 'locked'>('all')

const achievements = computed(() => scoreStore.achievements)

const filteredAchievements = computed(() => {
  if (activeTab.value === 'unlocked') {
    return achievements.value.filter((a) => a.unlocked)
  }
  if (activeTab.value === 'locked') {
    return achievements.value.filter((a) => !a.unlocked)
  }
  return achievements.value
})

const unlockedCount = computed(
  () => achievements.value.filter((a) => a.unlocked).length
)
const lockedCount = computed(
  () => achievements.value.filter((a) => !a.unlocked).length
)

const emptyHint = computed(() => {
  if (activeTab.value === 'unlocked') {
    return '暂无已解锁成就，完成闯关可解锁「初试牛刀」等成就'
  }
  if (activeTab.value === 'locked') {
    return '当前分类下没有未解锁成就'
  }
  return '暂无成就数据，请稍后重试'
})

function applyTabFromRoute() {
  const t = route.query.tab
  if (t === 'unlocked' || t === 'locked' || t === 'all') {
    activeTab.value = t
  }
}

function getConditionText(type: string, value: number): string {
  const conditionMap: Record<string, { text: string; unit?: string }> = {
    login_count: { text: '登录', unit: '次' },
    browse_count: { text: '浏览', unit: '次' },
    challenge_count: { text: '闯关', unit: '次' },
    perfect_score: { text: '满分' },
    post_count: { text: '发帖', unit: '篇' },
    continuous_days: { text: '连续学习', unit: '天' },
    challenge_complete: { text: '完成所有闯关' }
  }
  const config = conditionMap[type] || { text: type }
  return config.unit ? `需${value}${config.unit}` : config.text
}

function goBack() {
  router.back()
}

watch(
  () => route.query.tab,
  () => applyTabFromRoute(),
  { immediate: true }
)

onMounted(async () => {
  try {
    await scoreStore.fetchAchievements()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.achievement-layout {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
}

.achievement-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;

  &__back {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px 12px;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: hsl(220, 10%, 45%);
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      background: hsl(220, 10%, 95%);
      color: hsl(220, 10%, 25%);
    }
  }

  &__icon {
    width: 18px;
    height: 18px;
  }

  &__title {
    font-size: clamp(1.25rem, 3vw, 1.5rem);
    font-weight: 600;
    color: hsl(220, 10%, 20%);
    margin: 0;
  }
}

.achievement-summary {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px hsla(220, 10%, 90%, 0.5);

  &__icon {
    width: 40px;
    height: 40px;
    color: hsl(45, 90%, 55%);
  }

  &--locked &__icon {
    color: hsl(220, 10%, 65%);
  }

  &__info {
    display: flex;
    flex-direction: column;
  }

  &__num {
    font-size: 28px;
    font-weight: 700;
    color: hsl(220, 10%, 20%);
    line-height: 1.2;
  }

  &__label {
    font-size: 14px;
    color: hsl(220, 10%, 55%);
  }
}

.achievement-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  padding: 4px;
  background: hsl(220, 10%, 96%);
  border-radius: 10px;

  &__item {
    flex: 1;
    padding: 10px 16px;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: hsl(220, 10%, 45%);
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      color: hsl(220, 10%, 25%);
    }

    &--active {
      background: #fff;
      color: hsl(225, 60%, 50%);
      font-weight: 500;
      box-shadow: 0 2px 8px hsla(220, 10%, 90%, 0.5);
    }
  }
}

.achievement-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.achievement-item {
  display: flex;
  gap: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px hsla(220, 10%, 90%, 0.5);
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    box-shadow: 0 4px 16px hsla(220, 10%, 85%, 0.5);
    transform: translateY(-2px);
  }

  &--unlocked {
    background: linear-gradient(135deg, hsl(45, 90%, 96%) 0%, hsl(45, 60%, 94%) 100%);
    border: 1px solid hsla(45, 60%, 75%, 0.3);
  }

  &__badge {
    flex-shrink: 0;
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: hsla(45, 60%, 95%, 0.8);
  }

  &--unlocked &__badge {
    background: linear-gradient(135deg, hsl(45, 90%, 55%) 0%, hsl(35, 90%, 50%) 100%);
    box-shadow: 0 4px 12px hsla(40, 80%, 50%, 0.3);
  }

  &__icon {
    width: 28px;
    height: 28px;
    color: hsl(45, 90%, 55%);

    &--locked {
      color: hsl(220, 10%, 65%);
    }
  }

  &--unlocked &__icon {
    color: #fff;
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 15px;
    font-weight: 600;
    color: hsl(220, 10%, 20%);
    margin: 0 0 4px;
  }

  &__desc {
    font-size: 13px;
    color: hsl(220, 10%, 50%);
    margin: 0 0 8px;
    line-height: 1.4;
  }

  &__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__reward {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    background: hsl(140, 60%, 95%);
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
    color: hsl(140, 60%, 40%);
  }

  &__reward-icon {
    width: 12px;
    height: 12px;
  }

  &__condition {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    background: hsl(220, 10%, 95%);
    border-radius: 4px;
    font-size: 12px;
    color: hsl(220, 10%, 55%);
  }

  &__status {
    position: absolute;
    top: 12px;
    right: 12px;
  }

  &__check {
    width: 20px;
    height: 20px;
    color: hsl(140, 60%, 45%);
  }
}

.achievement-empty {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px hsla(220, 10%, 90%, 0.5);

  &__icon {
    width: 48px;
    height: 48px;
    color: hsl(220, 10%, 80%);
    margin-bottom: 12px;
  }

  &__text {
    font-size: 14px;
    color: hsl(220, 10%, 55%);
    margin: 0;
  }
}

.achievement-skeleton {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;

  .skeleton {
    height: 120px;
    background: linear-gradient(90deg, hsl(220, 10%, 95%) 25%, hsl(220, 10%, 90%) 50%, hsl(220, 10%, 95%) 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    border-radius: 12px;
  }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.achievement-tips {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px hsla(220, 10%, 90%, 0.5);

  &__title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
    color: hsl(220, 10%, 25%);
    margin: 0 0 12px;
  }

  &__icon {
    width: 18px;
    height: 18px;
    color: hsl(210, 80%, 55%);
  }

  &__list {
    margin: 0;
    padding-left: 20px;
    font-size: 13px;
    color: hsl(220, 10%, 50%);
    line-height: 1.8;
  }
}

@media (max-width: 768px) {
  .achievement-layout {
    padding: 16px;
  }

  .achievement-summary {
    grid-template-columns: 1fr;
  }

  .achievement-tabs {
    overflow-x: auto;
  }

  .achievement-grid {
    grid-template-columns: 1fr;
  }
}
</style>
