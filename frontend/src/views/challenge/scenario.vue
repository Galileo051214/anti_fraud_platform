<template>
  <div class="scenario-page">
    <header class="scenario-header">
      <div class="header-content">
        <button class="back-btn" @click="handleBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          返回
        </button>
        <div class="header-info" v-if="progress">
          <h1>{{ progress.challengeTitle }}</h1>
          <p>{{ statusText }}</p>
          <div class="header-meta">
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
              </svg>
              {{ progress.difficultyName || '中等' }}
            </span>
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke-linecap="round"/>
              </svg>
              及格分: {{ progress.passingScore || 60 }}
            </span>
            <span class="meta-item reward">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="8" r="6"/>
                <path d="M15.477 12.89L17 22l-5-3-5 3 1.523-9.11"/>
              </svg>
              通关+{{ progress.scoreReward || 20 }}积分
            </span>
          </div>
        </div>
      </div>
    </header>

    <div class="scenario-container" v-loading="loading">
      <template v-if="progress">
        <template v-if="progress.status === 'in_progress'">
          <div class="progress-bar">
            <span>决策进度: {{ decisionHistory.length }} 步</span>
            <el-progress :percentage="progressPercent" :show-text="false" />
          </div>

          <div class="dialogue-section" v-if="currentNode">
            <div class="dialogue__role" :class="`role--${currentNode.role}`">
              <span class="role__avatar">
                <svg v-if="currentNode.role === 'scammer'" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z"/>
                </svg>
                <svg v-else-if="currentNode.role === 'victim'" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
                </svg>
              </span>
              <span class="role__name">{{ roleName }}</span>
            </div>

            <div class="dialogue__content">
              <div class="dialogue__text">
                <p>{{ currentNode.content }}</p>
              </div>
              <div class="dialogue__time">{{ formatTime(currentNode.id) }}</div>
            </div>

            <div class="risk-tip" v-if="currentNode.riskTip">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/>
              </svg>
              <span>{{ currentNode.riskTip }}</span>
            </div>
          </div>

          <div class="choices-section" v-if="availableChoices.length > 0">
            <h3 class="choices__title">你会怎么做？</h3>
            <div class="choices__list">
              <button
                v-for="choice in availableChoices"
                :key="choice.edgeId"
                class="choice-btn"
                @click="handleChoice(choice)"
                :disabled="submitting"
              >
                <span class="choice__icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 18l6-6-6-6" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </span>
                <span class="choice__text">{{ choice.label }}</span>
              </button>
            </div>
          </div>

          <div class="history-section" v-if="decisionHistory.length > 0">
            <el-collapse>
              <el-collapse-item title="决策回顾" name="history">
                <div class="history__list">
                  <div
                    v-for="(record, idx) in decisionHistory"
                    :key="idx"
                    class="history__item"
                    :class="{ 'item--safe': record.isSafeChoice, 'item--danger': !record.isSafeChoice }"
                  >
                    <span class="history__choice">{{ record.choiceLabel }}</span>
                    <span class="history__badge">
                      <svg v-if="record.isSafeChoice" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                      </svg>
                      <svg v-else viewBox="0 0 24 24" fill="currentColor">
                        <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                      </svg>
                    </span>
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
        </template>

        <template v-else>
          <div class="result-container">
            <div class="result-card" :class="{ 'result--passed': progress.passed }">
              <div class="result__rating">{{ rating }}</div>
              <div class="result__message">{{ ratingDesc }}</div>
              <div class="result__score">
                <span class="score-current">{{ progress.finalScore }}</span>
                <span class="score-divider">/</span>
                <span class="score-max">100</span>
              </div>
              <div class="result__stats">
                <div class="stat-item">
                  <span class="stat-value">{{ decisionHistory.length }}</span>
                  <span class="stat-label">决策次数</span>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item">
                  <span class="stat-value safe">{{ safeChoiceCount }}</span>
                  <span class="stat-label">正确选择</span>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item" v-if="progress.earnedScore">
                  <span class="stat-value score-earned">+{{ progress.earnedScore }}</span>
                  <span class="stat-label">获得积分</span>
                </div>
              </div>
              <div class="result__badge" v-if="progress.passed">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                </svg>
                {{ progress.passed ? '成功通关' : '挑战失败' }}
              </div>
            </div>

            <div class="result-actions">
              <el-button @click="handleRetry" plain>重新挑战</el-button>
              <el-button type="primary" @click="handleBack">返回关卡</el-button>
            </div>
          </div>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  startScenario,
  makeDecision,
  resetScenario,
  type ScenarioProgressVO,
  type ScenarioEdgeVO
} from '@/api/challenge'
import { useScoreStore } from '@/stores/score'

const router = useRouter()
const route = useRoute()
const scoreStore = useScoreStore()

const loading = ref(false)
const submitting = ref(false)
const progress = ref<ScenarioProgressVO | null>(null)
const challengeId = ref<number>(0)

const currentNode = computed(() => progress.value?.currentNodeDetail)
const availableChoices = computed(() => progress.value?.availableChoices || [])
const decisionHistory = computed(() => progress.value?.decisionHistory || [])
const safeChoiceCount = computed(() =>
  decisionHistory.value.filter(r => r.isSafeChoice).length
)

const progressPercent = computed(() => {
  if (!decisionHistory.value.length) return 0
  const total = decisionHistory.value.length + availableChoices.value.length
  if (total === 0) return 100
  return Math.round((decisionHistory.value.length / total) * 100)
})

const roleName = computed(() => {
  if (!currentNode.value) return ''
  const roleMap: Record<string, string> = {
    scammer: '诈骗分子',
    victim: '受害者',
    narrator: '旁白'
  }
  return roleMap[currentNode.value.role] || '旁白'
})

const statusText = computed(() => {
  if (!progress.value) return ''
  const statusMap: Record<string, string> = {
    in_progress: '情景模拟进行中...',
    completed: '成功脱身！',
    failed: '不幸中招...'
  }
  return statusMap[progress.value.status] || ''
})

const rating = computed(() => {
  if (!progress.value) return 'D'
  const score = progress.value.finalScore || 0
  if (score >= 100) return 'S'
  if (score >= 80) return 'A'
  if (score >= 60) return 'B'
  if (score >= 40) return 'C'
  return 'D'
})

const ratingDesc = computed(() => {
  if (!progress.value) return ''
  const descMap: Record<string, string> = {
    S: '完美表现！你是反诈达人！',
    A: '优秀！你具备很强的反诈意识！',
    B: '良好！继续提高警惕性！',
    C: '及格！还需要加强学习！',
    D: '加油！多学习反诈知识！'
  }
  return descMap[rating.value] || ''
})

const formatTime = (nodeId: string) => {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
}

const handleBack = () => {
  router.push('/challenge')
}

const handleChoice = async (choice: ScenarioEdgeVO) => {
  if (!progress.value || submitting.value) return

  submitting.value = true
  try {
    const res = await makeDecision({
      challengeId: challengeId.value,
      currentNode: progress.value.currentNode,
      selectedEdgeId: choice.edgeId
    })
    progress.value = res.data

    if (progress.value.status !== 'in_progress') {
      if (progress.value.passed) {
        ElMessage.success(`恭喜通关！获得 ${progress.value.earnedScore || 0} 积分奖励！`)
        try {
          await scoreStore.fetchScoreInfo()
        } catch {
          // 忽略刷新失败，避免影响结局展示
        }
      } else {
        ElMessage.warning('未能通关，再接再厉！')
      }
    }
  } catch {
    ElMessage.error('操作失败，请重试')
  } finally {
    submitting.value = false
  }
}

const handleRetry = async () => {
  try {
    await resetScenario(challengeId.value)
    const res = await startScenario(challengeId.value)
    progress.value = res.data
  } catch {
    ElMessage.error('重置失败，请刷新重试')
  }
}

const fetchProgress = async () => {
  loading.value = true
  try {
    challengeId.value = Number(route.params.id)
    const res = await startScenario(challengeId.value)
    progress.value = res.data
  } catch (err) {
    ElMessage.error('加载情景模拟失败')
    router.push('/challenge')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchProgress()
})
</script>

<style scoped lang="scss">
.scenario-page {
  --primary: hsl(280, 50%, 45%);
  --primary-light: hsl(280, 50%, 95%);
  --success: hsl(142, 70%, 45%);
  --success-light: hsl(142, 70%, 95%);
  --warning: hsl(38, 92%, 50%);
  --warning-light: hsl(38, 80%, 95%);
  --danger: hsl(0, 65%, 55%);
  --danger-light: hsl(0, 65%, 95%);
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
}

.scenario-header {
  background: linear-gradient(135deg, hsl(280, 50%, 45%) 0%, hsl(280, 60%, 35%) 100%);
  padding: 32px 24px;
  color: white;
}

.header-content {
  max-width: 700px;
  margin: 0 auto;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: hsla(0, 0%, 100%, 0.15);
  border: none;
  color: white;
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.9rem;
  transition: background var(--transition-fast);
  margin-bottom: 20px;

  svg {
    width: 18px;
    height: 18px;
  }

  &:hover {
    background: hsla(0, 0%, 100%, 0.25);
  }
}

.header-info {
  h1 {
    font-size: clamp(1.25rem, 3vw, 1.5rem);
    font-weight: 700;
    margin: 0 0 8px;
  }

  p {
    margin: 0 0 16px;
    opacity: 0.9;
    font-size: 0.95rem;
  }
}

.header-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.9rem;
  opacity: 0.9;

  svg {
    width: 18px;
    height: 18px;
  }

  &.reward {
    color: hsl(45, 90%, 60%);
  }
}

.scenario-container {
  max-width: 700px;
  margin: 0 auto;
  padding: 32px 24px;
}

.progress-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;

  span {
    font-size: 0.9rem;
    color: var(--text-secondary);
    white-space: nowrap;
  }

  :deep(.el-progress) {
    flex: 1;

    .el-progress-bar__outer {
      background: hsl(220, 15%, 92%);
    }

    .el-progress-bar__inner {
      background: var(--primary);
    }
  }
}

.dialogue-section {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 28px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 24px;
}

.dialogue__role {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;

  &.role--scammer {
    .role__avatar {
      background: var(--danger-light);
      color: var(--danger);
    }
  }

  &.role--victim {
    .role__avatar {
      background: var(--primary-light);
      color: var(--primary);
    }
  }

  &.role--narrator {
    .role__avatar {
      background: hsl(220, 15%, 95%);
      color: var(--text-secondary);
    }
  }
}

.role__avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;

  svg {
    width: 24px;
    height: 24px;
  }
}

.role__name {
  font-weight: 600;
  font-size: 1rem;
  color: var(--text-primary);
}

.dialogue__content {
  position: relative;
  padding-left: 20px;
  border-left: 3px solid var(--primary-light);
}

.dialogue__text {
  p {
    font-size: 1.1rem;
    line-height: 1.7;
    color: var(--text-primary);
    margin: 0;
  }
}

.dialogue__time {
  margin-top: 12px;
  font-size: 0.8rem;
  color: var(--text-muted);
}

.risk-tip {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-top: 20px;
  padding: 16px;
  background: var(--warning-light);
  border-radius: var(--radius-sm);
  border-left: 4px solid var(--warning);

  svg {
    width: 20px;
    height: 20px;
    color: var(--warning);
    flex-shrink: 0;
    margin-top: 2px;
  }

  span {
    font-size: 0.9rem;
    color: hsl(38, 80%, 30%);
    line-height: 1.5;
  }
}

.choices-section {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 28px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 24px;
}

.choices__title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 20px;
  text-align: center;
}

.choices__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.choice-btn {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 18px 24px;
  background: hsl(220, 15%, 97%);
  border: 2px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  text-align: left;
  transition: all var(--transition-fast);

  &:hover:not(:disabled) {
    background: var(--primary-light);
    border-color: var(--primary);
  }

  &:active:not(:disabled) {
    transform: scale(0.98);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.choice__icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-card);
  border-radius: 50%;
  color: var(--primary);
  flex-shrink: 0;

  svg {
    width: 18px;
    height: 18px;
  }
}

.choice__text {
  flex: 1;
  font-size: 1rem;
  color: var(--text-primary);
  line-height: 1.5;
}

.history-section {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);

  :deep(.el-collapse) {
    border: none;

    .el-collapse-item__header {
      background: hsl(220, 15%, 97%);
      padding: 16px 20px;
      font-weight: 600;
      color: var(--text-primary);
    }

    .el-collapse-item__wrap {
      border: none;
    }

    .el-collapse-item__content {
      padding: 16px 20px;
    }
  }
}

.history__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  background: hsl(220, 15%, 97%);

  &.item--safe {
    border-left: 3px solid var(--success);
  }

  &.item--danger {
    border-left: 3px solid var(--danger);
  }
}

.history__choice {
  font-size: 0.9rem;
  color: var(--text-primary);
}

.history__badge {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;

  svg {
    width: 18px;
    height: 18px;
  }

  .item--safe & {
    color: var(--success);
  }

  .item--danger & {
    color: var(--danger);
  }
}

.result-container {
  text-align: center;
}

.result-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 48px 32px;
  box-shadow: var(--shadow-md);
  margin-bottom: 32px;

  &.result--passed {
    background: linear-gradient(135deg, var(--success-light) 0%, var(--bg-card) 100%);
  }
}

.result__rating {
  font-size: 5rem;
  font-weight: 800;
  line-height: 1;
  margin-bottom: 12px;

  .result--passed & {
    color: var(--success);
  }

  .result:not(.result--passed) & {
    color: var(--warning);
  }
}

.result__message {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 24px;
}

.result__score {
  margin-bottom: 32px;

  .score-current {
    font-size: 3rem;
    font-weight: 700;
    color: var(--primary);
  }

  .score-divider {
    font-size: 2rem;
    color: var(--text-muted);
    margin: 0 8px;
  }

  .score-max {
    font-size: 2rem;
    color: var(--text-secondary);
  }
}

.result__stats {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 24px;
  padding: 20px;
  background: hsl(220, 15%, 97%);
  border-radius: var(--radius-sm);
  margin-bottom: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;

  .stat-value {
    font-size: 1.5rem;
    font-weight: 700;
    color: var(--text-primary);

    &.safe {
      color: var(--success);
    }

    &.danger {
      color: var(--danger);
    }

    &.score-earned {
      color: var(--success);
    }
  }

  .stat-label {
    font-size: 0.8rem;
    color: var(--text-muted);
  }
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: hsl(220, 15%, 90%);
}

.result__badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: hsl(280, 50%, 95%);
  color: hsl(280, 50%, 35%);
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 600;

  svg {
    width: 18px;
    height: 18px;
  }
}

.result-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

@media (max-width: 768px) {
  .scenario-header {
    padding: 24px 16px;
  }

  .scenario-container {
    padding: 24px 16px;
  }

  .dialogue-section,
  .choices-section {
    padding: 24px 20px;
  }

  .result-card {
    padding: 32px 20px;
  }

  .result__rating {
    font-size: 4rem;
  }

  .result__stats {
    flex-wrap: wrap;
  }
}
</style>
