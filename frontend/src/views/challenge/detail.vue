<template>
  <div class="challenge-detail">
    <header class="detail-header">
      <div class="header-content">
        <button class="back-btn" @click="handleBack">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          返回
        </button>
        <div class="header-info" v-if="challenge">
          <h1>{{ challenge.title }}</h1>
          <p>{{ challenge.description }}</p>
          <div class="header-meta">
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
              </svg>
              {{ challenge.difficultyName }}
            </span>
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke-linecap="round"/>
              </svg>
              及格分: {{ challenge.passingScore }}
            </span>
            <span class="meta-item reward">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="8" r="6"/>
                <path d="M15.477 12.89L17 22l-5-3-5 3 1.523-9.11"/>
              </svg>
              通关+{{ challenge.scoreReward }}积分
            </span>
          </div>
        </div>
      </div>
    </header>

    <div class="question-container" v-loading="pageLoading">
      <template v-if="!submitted">
        <div class="question-progress">
          <span>第 {{ currentIndex + 1 }} / {{ questions.length }} 题</span>
          <el-progress :percentage="progressPercent" :show-text="false" />
        </div>

        <div class="question-card" v-if="currentQuestion">
          <div class="question__type">
            <el-tag size="small" :type="currentQuestion.questionType === 'single' ? 'primary' : 'warning'">
              {{ currentQuestion.questionType === 'single' ? '单选题' : '多选题' }}
            </el-tag>
            <span class="question__score">{{ currentQuestion.score }}分</span>
          </div>

          <h2 class="question__text">{{ currentQuestion.text }}</h2>

          <div class="options-list">
            <div
              v-for="(option, optIdx) in currentQuestion.options"
              :key="optIdx"
              class="option-item"
              :class="{
                'option--selected': isOptionSelected(optIdx),
                'option--multiple': currentQuestion.questionType === 'multiple'
              }"
              @click="handleOptionClick(optIdx)"
            >
              <span class="option__label">{{ option.label }}</span>
              <span class="option__text">{{ option.text }}</span>
              <div class="option__check">
                <svg v-if="isOptionSelected(optIdx)" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                  <path d="M20 6L9 17l-5-5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
            </div>
          </div>
        </div>

        <div class="question-actions">
          <el-button v-if="currentIndex > 0" @click="handlePrev" plain>上一题</el-button>
          <el-button v-if="currentIndex < questions.length - 1" type="primary" @click="handleNext">
            下一题
          </el-button>
          <el-button v-else type="success" @click="handleSubmit" :loading="submitting">
            提交答案
          </el-button>
        </div>
      </template>

      <div class="result-container" v-else>
        <div class="result-card" :class="{ 'result--passed': result?.passed }">
          <div class="result__rating">{{ result?.rating }}</div>
          <div class="result__message">{{ result?.ratingDesc }}</div>
          <div class="result__score">
            <span class="score-current">{{ result?.score }}</span>
            <span class="score-divider">/</span>
            <span class="score-max">{{ result?.maxScore }}</span>
          </div>
          <div class="result__stats">
            <div class="stat-item">
              <span class="stat-value">{{ result?.correctCount }}</span>
              <span class="stat-label">正确</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value">{{ result?.totalCount }}</span>
              <span class="stat-label">总题</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item" v-if="result?.earnedScore">
              <span class="stat-value score-earned">+{{ result?.earnedScore }}</span>
              <span class="stat-label">获得积分</span>
            </div>
          </div>
          <div class="result__badge" v-if="result?.newRecord">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
            </svg>
            打破历史记录
          </div>
        </div>

        <div class="result-actions">
          <el-button @click="handleRetry" plain>再试一次</el-button>
          <el-button type="primary" @click="handleBack">返回关卡</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getChallengeDetail, submitChallenge, type ChallengeVO, type ChallengeResultVO, type ChallengeQuestion } from '@/api/challenge'
import { useScoreStore } from '@/stores/score'

const router = useRouter()
const route = useRoute()
const scoreStore = useScoreStore()

const pageLoading = ref(false)
const submitting = ref(false)
const challenge = ref<ChallengeVO | null>(null)
const questions = ref<ChallengeQuestion[]>([])
const currentIndex = ref(0)
const userAnswers = ref<Record<string, number[]>>({})
const submitted = ref(false)
const result = ref<ChallengeResultVO | null>(null)
const startTime = ref<number>(Date.now())

const currentQuestion = computed(() => questions.value[currentIndex.value])

const progressPercent = computed(() => {
  if (questions.value.length === 0) return 0
  return Math.round(((currentIndex.value + 1) / questions.value.length) * 100)
})

const isOptionSelected = (optIdx: number) => {
  if (!currentQuestion.value) return false
  const qId = currentQuestion.value.id
  return userAnswers.value[qId]?.includes(optIdx) || false
}

const handleOptionClick = (optIdx: number) => {
  if (!currentQuestion.value || submitted.value) return
  const qId = currentQuestion.value.id
  const isMulti = currentQuestion.value.questionType === 'multiple'

  if (isMulti) {
    const current = userAnswers.value[qId] || []
    if (current.includes(optIdx)) {
      userAnswers.value[qId] = current.filter(i => i !== optIdx)
    } else {
      userAnswers.value[qId] = [...current, optIdx]
    }
  } else {
    userAnswers.value[qId] = [optIdx]
  }
}

const handlePrev = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

const handleNext = () => {
  if (!currentQuestion.value) return
  const qId = currentQuestion.value.id
  if (!userAnswers.value[qId]?.length) {
    ElMessage.warning('请选择答案后再继续')
    return
  }
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
  }
}

const handleSubmit = async () => {
  if (!currentQuestion.value) return
  const qId = currentQuestion.value.id
  if (!userAnswers.value[qId]?.length) {
    ElMessage.warning('请选择答案后再提交')
    return
  }

  submitting.value = true
  try {
    const challengeId = Number(route.params.id)
    const res = await submitChallenge({
      challengeId,
      answers: userAnswers.value,
      startTime: startTime.value
    })
    result.value = res.data
    submitted.value = true

    if (result.value.passed) {
      ElMessage.success('恭喜通关！')
      // 提交成功后立刻刷新积分徽章显示
      try {
        await scoreStore.fetchScoreInfo()
      } catch {
        // 忽略刷新失败，避免影响关卡结果展示
      }
    } else {
      ElMessage.info('未达及格线，再接再厉！')
    }
  } catch {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

const handleRetry = () => {
  currentIndex.value = 0
  userAnswers.value = {}
  submitted.value = false
  result.value = null
  startTime.value = Date.now()
}

const handleBack = () => {
  router.push('/challenge')
}

const fetchChallenge = async () => {
  pageLoading.value = true
  try {
    const challengeId = Number(route.params.id)
    const res = await getChallengeDetail(challengeId)
    challenge.value = res.data

    if (challenge.value?.content?.questions) {
      questions.value = challenge.value.content.questions
    }

    questions.value.forEach(q => {
      if (!userAnswers.value[q.id]) {
        userAnswers.value[q.id] = []
      }
    })
  } catch {
    ElMessage.error('加载关卡失败')
    router.push('/challenge')
  } finally {
    pageLoading.value = false
  }
}

onMounted(() => {
  fetchChallenge()
})
</script>

<style scoped lang="scss">
.challenge-detail {
  --primary: hsl(215, 70%, 52%);
  --primary-light: hsl(215, 70%, 95%);
  --success: hsl(142, 70%, 45%);
  --success-light: hsl(142, 70%, 95%);
  --warning: hsl(38, 92%, 50%);
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

.detail-header {
  background: linear-gradient(135deg, var(--primary) 0%, hsl(215, 60%, 42%) 100%);
  padding: 32px 24px;
  color: white;
}

.header-content {
  max-width: 800px;
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

.question-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px;
}

.question-progress {
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

.question-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 32px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 24px;
}

.question__type {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.question__score {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.question__text {
  font-size: 1.2rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 28px;
  line-height: 1.6;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: hsl(220, 15%, 97%);
  border: 2px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  min-height: 56px;

  &:hover {
    background: var(--primary-light);
    border-color: var(--primary);
  }

  &.option--selected {
    background: var(--primary-light);
    border-color: var(--primary);
  }

  &.option--multiple {
    // 多选样式
  }
}

.option__label {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-card);
  border-radius: 50%;
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--text-secondary);
  flex-shrink: 0;

  .option--selected & {
    background: var(--primary);
    color: white;
  }
}

.option__text {
  flex: 1;
  font-size: 1rem;
  color: var(--text-primary);
  line-height: 1.5;
}

.option__check {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  svg {
    width: 20px;
    height: 20px;
    color: var(--primary);
  }
}

.question-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
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
  background: hsl(45, 90%, 95%);
  color: hsl(38, 80%, 40%);
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
  .detail-header {
    padding: 24px 16px;
  }

  .question-container {
    padding: 24px 16px;
  }

  .question-card {
    padding: 24px 20px;
  }

  .question__text {
    font-size: 1.1rem;
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
