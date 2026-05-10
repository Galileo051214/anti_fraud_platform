<template>
  <div class="page-container">
    <div class="score-layout">
      <div class="score-header">
        <div class="score-header__back" @click="goBack">
          <ArrowLeft class="score-header__icon" />
          <span>返回</span>
        </div>
        <h1 class="score-header__title">积分中心</h1>
      </div>

      <div v-if="loading" class="score-skeleton">
        <div class="skeleton skeleton--card"></div>
        <div class="skeleton skeleton--progress"></div>
      </div>

      <template v-else-if="scoreInfo">
        <div class="score-overview">
          <div class="score-card score-card--main">
            <div class="score-card__level-badge">
              <span class="score-card__level-num">{{ scoreInfo.currentLevel }}</span>
              <span class="score-card__level-text">Lv.{{ scoreInfo.currentLevel }}</span>
            </div>
            <div class="score-card__info">
              <div class="score-card__total">
                <span class="score-card__total-num">{{ scoreInfo.totalScore }}</span>
                <span class="score-card__total-label">总积分</span>
              </div>
              <div class="score-card__weekly">
                <span class="score-card__weekly-num">{{ scoreInfo.weeklyScore }}</span>
                <span class="score-card__weekly-label">本周获取</span>
              </div>
            </div>
          </div>

          <div class="score-card score-card--progress">
            <div class="score-card__progress-header">
              <span class="score-card__progress-title">距离下一等级</span>
              <span class="score-card__progress-value">
                {{ progress.current }}/{{ progress.next }}
              </span>
            </div>
            <div class="score-card__progress-bar">
              <div
                class="score-card__progress-fill"
                :style="{ width: `${progress.percent}%` }"
              ></div>
            </div>
            <div class="score-card__progress-hint">
              再获得 {{ progress.next - progress.current }} 积分即可升至 Lv.{{ scoreInfo.currentLevel + 1 }}
            </div>
          </div>

          <div class="score-stats">
            <div
              class="score-stat score-stat--clickable"
              role="button"
              tabindex="0"
              @click="goAchievements('unlocked')"
              @keydown.enter.prevent="goAchievements('unlocked')"
            >
              <Trophy class="score-stat__icon score-stat__icon--gold" />
              <div class="score-stat__info">
                <span class="score-stat__value">{{ scoreInfo.unlockedAchievements }}</span>
                <span class="score-stat__label">已解锁成就</span>
              </div>
            </div>
            <div
              class="score-stat score-stat--clickable"
              role="button"
              tabindex="0"
              @click="goAchievements('all')"
              @keydown.enter.prevent="goAchievements('all')"
            >
              <Medal class="score-stat__icon score-stat__icon--purple" />
              <div class="score-stat__info">
                <span class="score-stat__value">{{ scoreInfo.totalAchievements }}</span>
                <span class="score-stat__label">成就总数</span>
              </div>
            </div>
          </div>
        </div>

        <div class="score-rules">
          <h2 class="score-rules__title">积分规则</h2>
          <p class="score-rules__note">
            以下行为均可获得积分：<strong>浏览案例</strong>（首次浏览某案例）、<strong>完成闯关</strong>、<strong>发帖分享</strong>、<strong>评论互动</strong>、<strong>解锁成就</strong>。
          </p>
          <div class="score-rules__list">
            <div class="score-rule">
              <span class="score-rule__action">浏览案例</span>
              <span class="score-rule__score">+2 积分</span>
            </div>
            <div class="score-rule">
              <span class="score-rule__action">完成闯关</span>
              <span class="score-rule__score">+5~20 积分</span>
            </div>
            <div class="score-rule">
              <span class="score-rule__action">发帖分享</span>
              <span class="score-rule__score">+3 积分</span>
            </div>
            <div class="score-rule">
              <span class="score-rule__action">评论互动</span>
              <span class="score-rule__score">+1 积分</span>
            </div>
            <div class="score-rule">
              <span class="score-rule__action">解锁成就</span>
              <span class="score-rule__score">+5~200 积分</span>
            </div>
          </div>
        </div>

        <div class="score-levels">
          <h2 class="score-levels__title">等级特权</h2>
          <div class="score-levels__grid">
            <div
              v-for="level in displayLevels"
              :key="level.num"
              class="level-item"
              :class="{
                'level-item--current': level.num === scoreInfo.currentLevel,
                'level-item--unlocked': level.num < scoreInfo.currentLevel,
                'level-item--locked': level.num > scoreInfo.currentLevel
              }"
            >
              <div class="level-item__badge">
                <span class="level-item__num">{{ level.num }}</span>
              </div>
              <div class="level-item__info">
                <span class="level-item__name">Lv.{{ level.num }}</span>
                <span class="level-item__score">{{ level.minScore }}+</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useScoreStore } from '@/stores/score'
import { ArrowLeft, Trophy, Medal } from '@element-plus/icons-vue'

const router = useRouter()
const scoreStore = useScoreStore()
const loading = ref(true)

const scoreInfo = computed(() => scoreStore.scoreInfo)
const progress = computed(() => scoreStore.getLevelProgress())

const displayLevels = computed(() => {
  const levels = []
  const current = scoreInfo.value?.currentLevel || 1
  const start = Math.max(1, current - 2)
  for (let i = start; i <= start + 4; i++) {
    levels.push({ num: i, minScore: (i - 1) * 100 })
  }
  return levels
})

function goBack() {
  router.back()
}

function goAchievements(tab: 'all' | 'unlocked') {
  router.push({ path: '/achievement', query: { tab } })
}

onMounted(async () => {
  try {
    await scoreStore.fetchScoreInfo()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.score-layout {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.score-header {
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

.score-overview {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.score-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px hsla(220, 10%, 90%, 0.5);

  &--main {
    display: flex;
    align-items: center;
    gap: 24px;
    background: linear-gradient(135deg, hsl(225, 60%, 96%) 0%, hsl(225, 60%, 92%) 100%);
  }

  &--progress {
    background: #fff;
  }

  &__level-badge {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: linear-gradient(135deg, hsl(45, 90%, 55%) 0%, hsl(35, 90%, 50%) 100%);
    box-shadow: 0 4px 12px hsla(40, 80%, 50%, 0.3);
  }

  &__level-num {
    font-size: 28px;
    font-weight: 700;
    color: #fff;
    line-height: 1;
  }

  &__level-text {
    font-size: 12px;
    color: hsla(0, 0%, 100%, 0.9);
  }

  &__info {
    flex: 1;
    display: flex;
    gap: 32px;
  }

  &__total,
  &__weekly {
    display: flex;
    flex-direction: column;
  }

  &__total-num {
    font-size: 32px;
    font-weight: 700;
    color: hsl(220, 10%, 20%);
    line-height: 1.2;
  }

  &__total-label {
    font-size: 14px;
    color: hsl(220, 10%, 55%);
  }

  &__weekly-num {
    font-size: 24px;
    font-weight: 600;
    color: hsl(140, 60%, 40%);
    line-height: 1.2;
  }

  &__weekly-label {
    font-size: 14px;
    color: hsl(220, 10%, 55%);
  }

  &__progress-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
  }

  &__progress-title {
    font-size: 14px;
    color: hsl(220, 10%, 45%);
  }

  &__progress-value {
    font-size: 14px;
    font-weight: 600;
    color: hsl(220, 10%, 25%);
  }

  &__progress-bar {
    height: 8px;
    background: hsl(220, 10%, 92%);
    border-radius: 4px;
    overflow: hidden;
  }

  &__progress-fill {
    height: 100%;
    background: linear-gradient(90deg, hsl(45, 90%, 55%) 0%, hsl(35, 90%, 50%) 100%);
    border-radius: 4px;
    transition: width 0.6s ease;
  }

  &__progress-hint {
    margin-top: 8px;
    font-size: 12px;
    color: hsl(220, 10%, 60%);
  }
}

.score-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.score-stat {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px hsla(220, 10%, 90%, 0.5);

  &--clickable {
    cursor: pointer;
    transition: box-shadow 0.2s ease, transform 0.2s ease;

    &:hover {
      box-shadow: 0 4px 16px hsla(220, 10%, 82%, 0.65);
      transform: translateY(-1px);
    }

    &:focus-visible {
      outline: 2px solid hsl(225, 60%, 52%);
      outline-offset: 2px;
    }
  }

  &__icon {
    width: 40px;
    height: 40px;

    &--gold {
      color: hsl(45, 90%, 55%);
    }

    &--purple {
      color: hsl(280, 60%, 55%);
    }
  }

  &__info {
    display: flex;
    flex-direction: column;
  }

  &__value {
    font-size: 24px;
    font-weight: 700;
    color: hsl(220, 10%, 20%);
    line-height: 1.2;
  }

  &__label {
    font-size: 13px;
    color: hsl(220, 10%, 55%);
  }
}

.score-rules {
  margin-top: 24px;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px hsla(220, 10%, 90%, 0.5);

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: hsl(220, 10%, 20%);
    margin: 0 0 16px;
  }

  &__note {
    margin: 0 0 16px;
    font-size: 13px;
    line-height: 1.55;
    color: hsl(220, 10%, 48%);
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
}

.score-rule {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: hsl(220, 10%, 97%);
  border-radius: 8px;

  &--pending {
    opacity: 0.92;
  }

  &__action {
    font-size: 14px;
    color: hsl(220, 10%, 35%);
  }

  &__score {
    font-size: 14px;
    font-weight: 600;
    color: hsl(140, 60%, 40%);

    &--muted {
      font-weight: 500;
      color: hsl(220, 10%, 50%);
    }
  }
}

.score-levels {
  margin-top: 24px;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px hsla(220, 10%, 90%, 0.5);

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: hsl(220, 10%, 20%);
    margin: 0 0 16px;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 12px;
  }
}

.level-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border-radius: 12px;
  background: hsl(220, 10%, 97%);
  transition: all 0.2s ease;

  &--current {
    background: linear-gradient(135deg, hsl(45, 90%, 55%) 0%, hsl(35, 90%, 50%) 100%);
    box-shadow: 0 4px 12px hsla(40, 80%, 50%, 0.3);

    .level-item__num {
      color: #fff;
    }

    .level-item__name,
    .level-item__score {
      color: #fff;
    }
  }

  &--unlocked {
    background: hsl(220, 10%, 94%);

    .level-item__num {
      color: hsl(220, 10%, 55%);
    }
  }

  &--locked {
    opacity: 0.6;
  }

  &__badge {
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: hsla(0, 0%, 100%, 0.8);
  }

  &__num {
    font-size: 20px;
    font-weight: 700;
    color: hsl(220, 10%, 30%);
  }

  &__info {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__name {
    font-size: 13px;
    font-weight: 600;
    color: hsl(220, 10%, 30%);
  }

  &__score {
    font-size: 11px;
    color: hsl(220, 10%, 55%);
  }
}

.score-skeleton {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .skeleton {
    background: linear-gradient(90deg, hsl(220, 10%, 95%) 25%, hsl(220, 10%, 90%) 50%, hsl(220, 10%, 95%) 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    border-radius: 12px;

    &--card {
      height: 128px;
    }

    &--progress {
      height: 80px;
    }
  }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (max-width: 768px) {
  .score-layout {
    padding: 16px;
  }

  .score-card--main {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }

  .score-card__info {
    flex-direction: column;
    gap: 12px;
  }

  .score-stats {
    grid-template-columns: 1fr;
  }

  .score-levels__grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
