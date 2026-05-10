<template>
  <div class="challenge-index">
    <header class="challenge-header">
      <div class="header-content">
        <div class="header-title">
          <h1>知识闯关</h1>
          <p class="header-subtitle">检验你的反诈能力，挑战各路关卡</p>
        </div>
        <div class="header-stats" v-if="progressData">
          <div class="stat-item">
            <span class="stat-value">{{ progressData.completedChallenges }}</span>
            <span class="stat-label">已通关</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ progressData.totalChallenges }}</span>
            <span class="stat-label">总关卡</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ progressPercent }}%</span>
            <span class="stat-label">完成率</span>
          </div>
        </div>
      </div>
    </header>

    <div class="challenge-filter">
      <el-tabs v-model="activeType" @tab-change="handleTypeChange">
        <el-tab-pane label="全部关卡" name="all"></el-tab-pane>
        <el-tab-pane label="答题挑战" name="quiz"></el-tab-pane>
        <el-tab-pane label="情景模拟" name="scenario"></el-tab-pane>
      </el-tabs>
    </div>

    <div class="challenge-grid" v-loading="loading">
      <template v-if="filteredChallenges.length > 0">
        <div
          v-for="item in filteredChallenges"
          :key="item.id"
          class="challenge-card"
          :class="{
            'card--passed': item.passed,
            'card--locked': item.locked
          }"
          @click="handleCardClick(item)"
        >
          <div class="card__level">
            <span>{{ item.levelOrder }}</span>
          </div>
          <div class="card__type-badge" :class="`type--${item.type}`">
            {{ item.typeName }}
          </div>
          <div class="card__icon">
            <template v-if="item.passed">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 6L9 17l-5-5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </template>
            <template v-else-if="item.locked">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0110 0v4"/>
              </svg>
            </template>
            <template v-else>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M12 8v4l3 3"/>
              </svg>
            </template>
          </div>

          <h3 class="card__title">{{ item.title }}</h3>
          <p class="card__desc">{{ item.description }}</p>

          <div class="card__meta">
            <div class="meta-item difficulty">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
              </svg>
              <span>{{ item.difficultyName }}</span>
            </div>
            <div class="meta-item reward">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="8" r="6"/>
                <path d="M15.477 12.89L17 22l-5-3-5 3 1.523-9.11"/>
              </svg>
              <span>+{{ item.scoreReward }}</span>
            </div>
          </div>

          <div class="card__status">
            <el-tag v-if="item.passed" type="success" size="small" effect="dark">
              已通关 {{ item.highestScore ? `(${item.highestScore}分)` : '' }}
            </el-tag>
            <el-tag v-else-if="item.locked" type="info" size="small">
              {{ item.unlockHint || '前置关卡未通过' }}
            </el-tag>
            <el-tag v-else type="warning" size="small" effect="dark">待挑战</el-tag>
          </div>
        </div>
      </template>
      <div v-else class="empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" stroke-linecap="round"/>
        </svg>
        <p>暂无关卡数据</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getChallengeList, getChallengeProgress, type ChallengeVO } from '@/api/challenge'

const router = useRouter()

const loading = ref(false)
const challenges = ref<ChallengeVO[]>([])
const progressData = ref<{ completedChallenges: number; totalChallenges: number } | null>(null)
const activeType = ref('all')

const filteredChallenges = computed(() => {
  if (activeType.value === 'all') {
    return challenges.value
  }
  return challenges.value.filter(c => c.type === activeType.value)
})

const progressPercent = computed(() => {
  if (!progressData.value || progressData.value.totalChallenges === 0) return 0
  return Math.round(
    (progressData.value.completedChallenges / progressData.value.totalChallenges) * 100
  )
})

const handleTypeChange = () => {
  // 类型切换后自动过滤
}

const handleCardClick = (item: ChallengeVO) => {
  if (item.locked) {
    ElMessage.warning(item.unlockHint || '请先完成前置关卡')
    return
  }
  if (item.type === 'scenario') {
    router.push(`/challenge/scenario/${item.id}`)
  } else {
    router.push(`/challenge/${item.id}`)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const [listRes, progressRes] = await Promise.all([
      getChallengeList(),
      getChallengeProgress()
    ])
    challenges.value = listRes.data || []
    progressData.value = progressRes.data
  } catch {
    ElMessage.error('加载关卡数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.challenge-index {
  --primary: hsl(215, 70%, 52%);
  --primary-light: hsl(215, 70%, 95%);
  --success: hsl(142, 70%, 45%);
  --warning: hsl(38, 92%, 50%);
  --danger: hsl(0, 65%, 55%);
  --text-primary: hsl(220, 20%, 18%);
  --text-secondary: hsl(220, 10%, 45%);
  --text-muted: hsl(220, 8%, 60%);
  --bg-card: hsl(0, 0%, 100%);
  --bg-page: hsl(220, 20%, 97%);
  --shadow-sm: 0 2px 8px hsla(220, 20%, 18%, 0.06);
  --shadow-md: 0 4px 16px hsla(220, 20%, 18%, 0.1);
  --radius-sm: 8px;
  --radius-md: 12px;
  --transition-fast: 200ms ease-in-out;

  min-height: 100vh;
  background: var(--bg-page);
  padding-bottom: 48px;
}

.challenge-header {
  background: linear-gradient(135deg, var(--primary) 0%, hsl(215, 60%, 42%) 100%);
  padding: 48px 24px;
  color: white;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 24px;
}

.header-title {
  h1 {
    font-size: clamp(1.5rem, 4vw, 2rem);
    font-weight: 700;
    margin: 0 0 8px;
    letter-spacing: -0.02em;
  }
}

.header-subtitle {
  margin: 0;
  opacity: 0.9;
  font-size: 0.95rem;
}

.header-stats {
  display: flex;
  align-items: center;
  gap: 24px;
  background: hsla(0, 0%, 100%, 0.15);
  backdrop-filter: blur(8px);
  padding: 16px 28px;
  border-radius: var(--radius-md);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;

  .stat-value {
    font-size: 1.75rem;
    font-weight: 700;
    line-height: 1;
  }

  .stat-label {
    font-size: 0.8rem;
    opacity: 0.85;
  }
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: hsla(0, 0%, 100%, 0.3);
}

.challenge-filter {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 24px 0;

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__item) {
    font-weight: 500;
    font-size: 0.95rem;

    &.is-active {
      color: var(--primary);
    }
  }

  :deep(.el-tabs__active-bar) {
    background: var(--primary);
  }
}

.challenge-grid {
  max-width: 1200px;
  margin: 24px auto 0;
  padding: 0 24px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.challenge-card {
  position: relative;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 28px 24px 24px;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);

  &:hover:not(.card--locked) {
    transform: translateY(-4px);
    box-shadow: var(--shadow-md);
  }

  &.card--locked {
    opacity: 0.7;
    cursor: not-allowed;

    &:hover {
      transform: none;
    }
  }

  &.card--passed {
    border: 2px solid var(--success);
  }
}

.card__level {
  position: absolute;
  top: 16px;
  left: 16px;
  width: 36px;
  height: 36px;
  background: var(--primary);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.9rem;
}

.card__type-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;

  &.type--quiz {
    background: hsl(210, 80%, 95%);
    color: hsl(210, 80%, 40%);
  }

  &.type--scenario {
    background: hsl(280, 60%, 95%);
    color: hsl(280, 60%, 40%);
  }
}

.card__icon {
  width: 56px;
  height: 56px;
  margin: 8px auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-light);
  border-radius: 50%;
  color: var(--primary);

  svg {
    width: 28px;
    height: 28px;
  }

  .card--passed & {
    background: hsl(142, 70%, 95%);
    color: var(--success);
  }

  .card--locked & {
    background: hsl(220, 10%, 92%);
    color: var(--text-muted);
  }
}

.card__title {
  font-size: 1.15rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
  text-align: center;
}

.card__desc {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin: 0 0 16px;
  text-align: center;
  line-height: 1.5;
}

.card__meta {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: var(--text-secondary);

  svg {
    width: 16px;
    height: 16px;
  }

  &.difficulty {
    color: var(--warning);
  }

  &.reward {
    color: var(--primary);
  }
}

.card__status {
  display: flex;
  justify-content: center;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  color: var(--text-muted);

  svg {
    width: 64px;
    height: 64px;
    margin-bottom: 16px;
    opacity: 0.5;
  }

  p {
    font-size: 1rem;
  }
}

@media (max-width: 768px) {
  .challenge-header {
    padding: 32px 16px;
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-stats {
    width: 100%;
    justify-content: space-around;
    padding: 12px 16px;
  }

  .challenge-filter,
  .challenge-grid {
    padding-left: 16px;
    padding-right: 16px;
  }

  .challenge-grid {
    grid-template-columns: 1fr;
  }
}
</style>
