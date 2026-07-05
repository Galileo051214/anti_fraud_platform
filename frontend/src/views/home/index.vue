<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-content">
        <div class="hero-kicker">反诈骗学习平台</div>
        <h1>把防骗能力练成日常反应</h1>
        <p>从必读预警、真实案例、闯关训练和个性化推荐进入当天学习任务。</p>
        <div class="hero-actions">
          <el-button class="hero-primary" size="large" @click="router.push('/challenge')">
            <IconGame :size="18" />
            开始学习
          </el-button>
          <el-button class="hero-secondary" size="large" @click="router.push('/case')">
            <IconBook :size="18" />
            了解案例
          </el-button>
        </div>
      </div>

      <div class="hero-panel liquid-glass">
        <div class="panel-eyebrow">当前学习状态</div>
        <div class="score-value">{{ scoreInfo ? scoreInfo.totalScore : '--' }}</div>
        <div class="score-label">累计积分</div>
        <div class="mini-grid">
          <div>
            <strong>{{ scoreInfo ? `Lv.${scoreInfo.currentLevel}` : '--' }}</strong>
            <span>当前等级</span>
          </div>
          <div>
            <strong>{{ scoreInfo ? scoreInfo.weeklyScore : '--' }}</strong>
            <span>本周积分</span>
          </div>
        </div>
      </div>
    </section>

    <section class="learning-section" v-loading="dashboardLoading">
      <div class="section-heading">
        <div>
          <span class="section-kicker">Learning Hub</span>
          <h2>今日学习入口</h2>
        </div>
        <el-button class="plain-link" text @click="router.push('/score')">查看积分</el-button>
      </div>

      <div v-if="!isLoggedIn" class="guest-strip liquid-glass">
        <div>
          <strong>登录后同步学习进度</strong>
          <span>可查看积分、成就、下一关卡和个性化推荐。</span>
        </div>
        <el-button class="dark-pill" @click="router.push('/login')">登录</el-button>
      </div>

      <div class="overview-grid">
        <button class="overview-item" type="button" @click="router.push('/score')">
          <IconTrophy :size="24" />
          <span>积分等级</span>
          <strong>{{ scoreInfo ? `Lv.${scoreInfo.currentLevel}` : '--' }}</strong>
        </button>
        <button class="overview-item" type="button" @click="router.push('/achievement')">
          <IconShield :size="24" />
          <span>已解锁成就</span>
          <strong>{{ achievementText }}</strong>
        </button>
        <button class="overview-item" type="button" @click="router.push('/challenge')">
          <IconTarget :size="24" />
          <span>闯关进度</span>
          <strong>{{ challengeProgressText }}</strong>
        </button>
        <button class="overview-item" type="button" @click="router.push('/recommend')">
          <IconChart :size="24" />
          <span>推荐内容</span>
          <strong>{{ recommendations.length }}</strong>
        </button>
      </div>
    </section>

    <section class="task-grid">
      <article class="task-panel">
        <header class="task-header">
          <div>
            <IconNews :size="22" />
            <h3>必读资讯</h3>
          </div>
          <el-button text @click="router.push('/news')">全部</el-button>
        </header>
        <el-empty v-if="!requiredNews.length" description="暂无资讯" />
        <div v-else class="content-list">
          <button
            v-for="item in requiredNews"
            :key="item.id"
            class="content-row"
            type="button"
            @click="router.push(`/news/${item.id}`)"
          >
            <span class="row-badge">{{ item.isMandatory === 1 ? '必读' : newsTypeLabel(item.newsType) }}</span>
            <span class="row-main">
              <strong>{{ item.title }}</strong>
              <small>{{ formatDate(item.publishTime || item.createTime) }} · {{ item.viewCount ?? 0 }} 次阅读</small>
            </span>
          </button>
        </div>
      </article>

      <article class="task-panel">
        <header class="task-header">
          <div>
            <IconGame :size="22" />
            <h3>下一关卡</h3>
          </div>
          <el-button text @click="router.push('/challenge')">闯关</el-button>
        </header>
        <div v-if="nextChallenge" class="challenge-card" @click="openChallenge(nextChallenge)">
          <div class="challenge-meta">
            <span>{{ nextChallenge.typeName }}</span>
            <span>{{ nextChallenge.difficultyName }}</span>
          </div>
          <h4>{{ nextChallenge.title }}</h4>
          <p>{{ nextChallenge.description || '继续完成本阶段防骗能力训练。' }}</p>
          <div class="progress-track">
            <span :style="{ width: `${challengePercent}%` }"></span>
          </div>
          <small>{{ challengeProgressText }}</small>
        </div>
        <el-empty v-else description="暂无待完成关卡" />
      </article>

      <article class="task-panel">
        <header class="task-header">
          <div>
            <IconChart :size="22" />
            <h3>个性化推荐</h3>
          </div>
          <el-button text @click="router.push('/recommend')">更多</el-button>
        </header>
        <el-empty v-if="!recommendations.length" description="暂无推荐" />
        <div v-else class="content-list">
          <button
            v-for="item in recommendations"
            :key="`${item.itemType}-${item.itemId}`"
            class="content-row"
            type="button"
            @click="openRecommendation(item)"
          >
            <span class="row-badge">{{ recommendationTypeLabel(item.itemType) }}</span>
            <span class="row-main">
              <strong>{{ item.title }}</strong>
              <small>{{ recommendationReason(item) }}</small>
            </span>
          </button>
        </div>
      </article>
    </section>

    <section class="features-section">
      <h2 class="section-title">平台特色</h2>
      <div class="features-grid">
        <div class="feature-card">
          <div class="feature-icon"><IconGame :size="40" /></div>
          <h3>游戏化学习</h3>
          <p>闯关答题、情景模拟，让学习变得有趣</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon"><IconBot :size="40" /></div>
          <h3>智能客服</h3>
          <p>AI 智能问答，随时解答防骗疑惑</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon"><IconChart :size="40" /></div>
          <h3>智能推荐</h3>
          <p>按学习阶段推荐适合的防骗内容</p>
        </div>
        <div class="feature-card">
          <div class="feature-icon"><IconUsers :size="40" /></div>
          <h3>社区互动</h3>
          <p>分享经历、互相帮助、提高防骗意识</p>
        </div>
      </div>
    </section>

    <section class="hot-section" v-loading="hotLoading">
      <h2 class="section-title">热门案例</h2>
      <el-empty v-if="!hotLoading && !hotCases.length" description="暂无热门案例" />
      <el-row v-else :gutter="20">
        <el-col :xs="24" :sm="12" :md="8" v-for="item in hotCases" :key="item.id">
          <div class="case-card" @click="router.push(`/case/${item.id}`)">
            <div class="case-image"><component :is="caseCardIcon(item)" /></div>
            <div class="case-info">
              <h4>{{ item.title }}</h4>
              <p>{{ item.caseType }}</p>
              <div class="case-stats">
                <span class="case-stat-item"><IconEye :size="14" /> {{ item.viewCount ?? 0 }}</span>
                <span class="case-stat-item"><IconHeart :size="14" /> {{ item.likeCount ?? 0 }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHotCases, type CaseVO } from '@/api/case'
import { getNewsPage, getRequiredNews, type News } from '@/api/news'
import { getChallengeProgress, type ChallengeVO } from '@/api/challenge'
import { getRecommendationList, recordRecommendationClick, type RecommendationVO } from '@/api/recommendation'
import { useScoreStore } from '@/stores/score'
import { useUserStore } from '@/stores/user'
import { getChallengeRoute, getRecommendationChallengeRoute } from '@/utils/challengeRoute'
import {
  IconUsers, IconGame, IconBot, IconChart, IconEye, IconHeart,
  IconCoin, IconPhone, IconCall, IconLove, IconBank,
  IconCart, IconPolice, IconPig, IconCredit, IconClipboard,
  IconBook, IconTarget, IconTrophy, IconShield, IconNews
} from '@/components/icons'

const router = useRouter()
const scoreStore = useScoreStore()
const userStore = useUserStore()

const hotCases = ref<CaseVO[]>([])
const requiredNews = ref<News[]>([])
const nextChallenges = ref<ChallengeVO[]>([])
const recommendations = ref<RecommendationVO[]>([])
const challengeProgress = ref({ totalChallenges: 0, completedChallenges: 0 })
const hotLoading = ref(false)
const dashboardLoading = ref(false)

const isLoggedIn = computed(() => userStore.isLoggedIn)
const scoreInfo = computed(() => scoreStore.scoreInfo)
const nextChallenge = computed(() => nextChallenges.value[0])
const challengePercent = computed(() => {
  if (!challengeProgress.value.totalChallenges) return 0
  return Math.round((challengeProgress.value.completedChallenges / challengeProgress.value.totalChallenges) * 100)
})
const challengeProgressText = computed(() => {
  const { completedChallenges, totalChallenges } = challengeProgress.value
  if (!totalChallenges) return '--'
  return `${completedChallenges}/${totalChallenges}`
})
const achievementText = computed(() => {
  if (!scoreInfo.value) return '--'
  return `${scoreInfo.value.unlockedAchievements}/${scoreInfo.value.totalAchievements}`
})

const CASE_TYPE_ICONS: Record<string, any> = {
  网络诈骗: IconCoin,
  电信诈骗: IconPhone,
  电话诈骗: IconCall,
  情感诈骗: IconLove,
  金融诈骗: IconBank,
  刷单诈骗: IconCart,
  冒充公检法: IconPolice,
  杀猪盘: IconPig,
  贷款诈骗: IconCredit
}

const NEWS_TYPE_LABELS: Record<string, string> = {
  news: '资讯',
  warning: '预警',
  policy: '政策'
}

const RECOMMENDATION_TYPE_LABELS: Record<string, string> = {
  case: '案例',
  news: '资讯',
  challenge: '关卡'
}

function caseCardIcon(item: CaseVO) {
  if (CASE_TYPE_ICONS[item.caseType]) return CASE_TYPE_ICONS[item.caseType]
  return IconClipboard
}

function newsTypeLabel(type: string) {
  return NEWS_TYPE_LABELS[type] || '资讯'
}

function recommendationTypeLabel(type: string) {
  return RECOMMENDATION_TYPE_LABELS[type] || '推荐'
}

function recommendationReason(item: RecommendationVO) {
  if (item.reasons?.length) return item.reasons.slice(0, 2).join(' · ')
  if (item.tags?.length) return item.tags.slice(0, 3).join(' · ')
  return item.summary || '根据你的学习情况推荐'
}

function formatDate(value?: string) {
  if (!value) return '待发布'
  return value.slice(0, 10)
}

function openChallenge(item: ChallengeVO) {
  router.push(getChallengeRoute(item.id, item.type))
}

function openRecommendation(item: RecommendationVO) {
  recordRecommendationClick(item.itemId, item.itemType).catch(() => {})
  if (item.itemType === 'news') {
    router.push(`/news/${item.itemId}`)
    return
  }
  if (item.itemType === 'challenge') {
    router.push(getRecommendationChallengeRoute(item))
    return
  }
  router.push(`/case/${item.itemId}`)
}

async function loadHotCases() {
  hotLoading.value = true
  try {
    const res = await getHotCases(6)
    hotCases.value = Array.isArray(res) ? res : []
  } catch {
    hotCases.value = []
  } finally {
    hotLoading.value = false
  }
}

async function loadRequiredNews() {
  try {
    const required = await getRequiredNews(3)
    if (Array.isArray(required) && required.length > 0) {
      requiredNews.value = required
      return
    }
    const page = await getNewsPage({ pageNum: 1, pageSize: 10 })
    const records = Array.isArray(page.records) ? page.records : []
    const mandatory = records.filter((item) => item.isMandatory === 1)
    const top = records.filter((item) => item.isTop === 1)
    requiredNews.value = (mandatory.length ? mandatory : top.length ? top : records).slice(0, 3)
  } catch {
    requiredNews.value = []
  }
}

async function loadLearningDashboard() {
  if (!isLoggedIn.value) return

  dashboardLoading.value = true
  try {
    const [scoreResult, challengeResult, recommendationResult] = await Promise.allSettled([
      scoreStore.fetchScoreInfo(),
      getChallengeProgress(),
      getRecommendationList({ limit: 3 })
    ])

    if (challengeResult.status === 'fulfilled') {
      challengeProgress.value = {
        totalChallenges: challengeResult.value.totalChallenges || 0,
        completedChallenges: challengeResult.value.completedChallenges || 0
      }
      nextChallenges.value = Array.isArray(challengeResult.value.nextChallenges)
        ? challengeResult.value.nextChallenges
        : []
    }

    if (recommendationResult.status === 'fulfilled') {
      recommendations.value = Array.isArray(recommendationResult.value) ? recommendationResult.value : []
    }

    if (scoreResult.status === 'rejected') {
      recommendations.value = recommendations.value.slice(0, 3)
    }
  } finally {
    dashboardLoading.value = false
  }
}

onMounted(() => {
  loadHotCases()
  loadRequiredNews()
  loadLearningDashboard()
})
</script>

<style scoped lang="scss">
.home-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 20px 56px;
  color: #111;

  .liquid-glass {
    --lg-radius: 22px;
    --lg-bg: rgba(255, 255, 255, 0.66);
    --lg-border: rgba(255, 255, 255, 0.72);
    --lg-shadow: rgba(17, 24, 39, 0.12);
    position: relative;
    isolation: isolate;
    overflow: hidden;
    border: 1px solid var(--lg-border);
    border-radius: var(--lg-radius);
    background:
      linear-gradient(135deg, rgba(255, 255, 255, 0.58), rgba(255, 255, 255, 0.18) 44%),
      var(--lg-bg);
    box-shadow:
      0 18px 45px var(--lg-shadow),
      inset 0 1px 1px rgba(255, 255, 255, 0.92),
      inset 0 -1px 1px rgba(17, 24, 39, 0.08);
    -webkit-backdrop-filter: blur(18px) saturate(1.22);
    backdrop-filter: blur(18px) saturate(1.22);

    > * {
      position: relative;
      z-index: 1;
    }

    &::before,
    &::after {
      content: "";
      position: absolute;
      pointer-events: none;
      border-radius: inherit;
      z-index: 0;
    }

    &::before {
      inset: 9px;
      border: 1px solid rgba(255, 255, 255, 0.42);
      box-shadow: inset 0 -12px 18px rgba(17, 24, 39, 0.08);
    }

    &::after {
      top: -28%;
      right: -20%;
      width: 70%;
      height: 70%;
      background: radial-gradient(circle, rgba(255, 255, 255, 0.68), transparent 64%);
      filter: blur(14px);
      opacity: 0.76;
    }
  }

  .hero-section {
    position: relative;
    display: grid;
    grid-template-columns: minmax(0, 1fr) 320px;
    gap: 28px;
    align-items: center;
    min-height: 360px;
    margin-bottom: 28px;
    padding: 42px;
    overflow: hidden;
    border: 1px solid #e5e7eb;
    border-radius: 24px;
    background-color: #f8fafc;
    background-image:
      linear-gradient(rgba(17, 24, 39, 0.055) 1px, transparent 1px),
      linear-gradient(90deg, rgba(17, 24, 39, 0.055) 1px, transparent 1px);
    background-size: 34px 34px;

    .hero-content {
      max-width: 680px;
    }

    .hero-kicker,
    .panel-eyebrow,
    .section-kicker {
      color: #52525b;
      font-size: 13px;
      font-weight: 700;
      letter-spacing: 0;
      text-transform: uppercase;
    }

    h1 {
      max-width: 560px;
      margin: 14px 0 16px;
      color: #0f172a;
      font-size: 42px;
      line-height: 1.15;
      font-weight: 800;
      letter-spacing: 0;
    }

    p {
      max-width: 560px;
      margin: 0 0 30px;
      color: #3f3f46;
      font-size: 17px;
      line-height: 1.8;
    }

    .hero-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;

      :deep(.el-button) {
        min-width: 132px;
        height: 46px;
        padding: 0 20px;
        border-radius: 999px;
        font-weight: 700;
      }

      :deep(.hero-primary) {
        border-color: #111;
        background: #111;
        color: #fff;
      }

      :deep(.hero-secondary) {
        border-color: rgba(17, 17, 17, 0.16);
        background: rgba(255, 255, 255, 0.72);
        color: #111;
        -webkit-backdrop-filter: blur(14px);
        backdrop-filter: blur(14px);
      }
    }

    .hero-panel {
      padding: 26px;
      color: #111827;
    }

    .score-value {
      margin-top: 16px;
      font-size: 48px;
      line-height: 1;
      font-weight: 800;
    }

    .score-label {
      margin-top: 8px;
      color: #52525b;
      font-size: 14px;
    }

    .mini-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 10px;
      margin-top: 24px;

      div {
        min-height: 72px;
        padding: 14px;
        border: 1px solid rgba(17, 24, 39, 0.08);
        border-radius: 16px;
        background: rgba(255, 255, 255, 0.58);
      }

      strong,
      span {
        display: block;
      }

      strong {
        color: #111827;
        font-size: 20px;
      }

      span {
        margin-top: 6px;
        color: #71717a;
        font-size: 12px;
      }
    }
  }

  .learning-section,
  .features-section,
  .hot-section {
    margin-bottom: 34px;
  }

  .section-heading {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;

    h2 {
      margin: 6px 0 0;
      color: #111827;
      font-size: 24px;
      line-height: 1.25;
    }
  }

  .plain-link,
  .task-header :deep(.el-button) {
    color: #111;
    font-weight: 700;
  }

  .guest-strip {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
    padding: 16px 18px;

    strong,
    span {
      display: block;
    }

    strong {
      margin-bottom: 4px;
      color: #111827;
      font-size: 15px;
    }

    span {
      color: #71717a;
      font-size: 13px;
    }

    .dark-pill {
      border-radius: 999px;
      border-color: #111;
      background: #111;
      color: #fff;
    }
  }

  .overview-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
  }

  .overview-item {
    min-height: 112px;
    padding: 18px;
    border: 1px solid #e5e7eb;
    border-radius: 18px;
    background: rgba(255, 255, 255, 0.84);
    color: #111827;
    text-align: left;
    cursor: pointer;
    box-shadow: 0 12px 26px rgba(17, 24, 39, 0.06);
    transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;

    &:hover {
      transform: translateY(-2px);
      border-color: #111827;
      box-shadow: 0 18px 36px rgba(17, 24, 39, 0.1);
    }

    :deep(svg) {
      color: #111827;
    }

    span,
    strong {
      display: block;
    }

    span {
      margin: 14px 0 4px;
      color: #71717a;
      font-size: 13px;
    }

    strong {
      color: #111827;
      font-size: 24px;
      line-height: 1.2;
    }
  }

  .task-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 18px;
    margin-bottom: 34px;
  }

  .task-panel {
    min-height: 300px;
    padding: 20px;
    border: 1px solid #e5e7eb;
    border-radius: 20px;
    background:
      linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.9));
    box-shadow: 0 14px 34px rgba(17, 24, 39, 0.07);
  }

  .task-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 16px;

    > div {
      display: flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
    }

    h3 {
      margin: 0;
      color: #111827;
      font-size: 18px;
      line-height: 1.25;
    }
  }

  .content-list {
    display: grid;
    gap: 10px;
  }

  .content-row {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    width: 100%;
    min-height: 72px;
    padding: 12px;
    border: 1px solid #edf0f2;
    border-radius: 14px;
    background: #fff;
    color: #111827;
    text-align: left;
    cursor: pointer;
    transition: transform 180ms ease, border-color 180ms ease, background 180ms ease;

    &:hover {
      transform: translateY(-1px);
      border-color: #111827;
      background: #fafafa;
    }

    .row-badge {
      flex: 0 0 auto;
      min-width: 42px;
      padding: 4px 8px;
      border-radius: 999px;
      background: #111;
      color: #fff;
      font-size: 12px;
      line-height: 1.3;
      text-align: center;
    }

    .row-main {
      min-width: 0;

      strong,
      small {
        display: block;
      }

      strong {
        color: #111827;
        font-size: 14px;
        line-height: 1.5;
      }

      small {
        margin-top: 5px;
        color: #71717a;
        font-size: 12px;
        line-height: 1.45;
      }
    }
  }

  .challenge-card {
    min-height: 212px;
    padding: 16px;
    border: 1px solid #111827;
    border-radius: 18px;
    background: #111827;
    color: #fff;
    cursor: pointer;
    transition: transform 180ms ease, box-shadow 180ms ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 18px 34px rgba(17, 24, 39, 0.2);
    }

    .challenge-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      span {
        padding: 4px 8px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.12);
        color: rgba(255, 255, 255, 0.86);
        font-size: 12px;
      }
    }

    h4 {
      margin: 20px 0 8px;
      color: #fff;
      font-size: 18px;
      line-height: 1.35;
    }

    p {
      min-height: 42px;
      margin: 0;
      color: rgba(255, 255, 255, 0.72);
      font-size: 13px;
      line-height: 1.6;
    }

    .progress-track {
      height: 8px;
      margin: 22px 0 10px;
      overflow: hidden;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.14);

      span {
        display: block;
        height: 100%;
        min-width: 8px;
        border-radius: inherit;
        background: #fff;
        transition: width 220ms ease;
      }
    }

    small {
      color: rgba(255, 255, 255, 0.72);
      font-size: 12px;
    }
  }

  .section-title {
    margin: 0 0 18px;
    color: #111827;
    font-size: 24px;
    line-height: 1.25;
  }

  .features-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 16px;
  }

  .feature-card {
    min-height: 184px;
    padding: 22px;
    border: 1px solid #e5e7eb;
    border-radius: 18px;
    background: #fff;
    box-shadow: 0 12px 28px rgba(17, 24, 39, 0.055);
    transition: transform 180ms ease, border-color 180ms ease;

    &:hover {
      transform: translateY(-2px);
      border-color: #111827;
    }

    .feature-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 52px;
      height: 52px;
      border: 1px solid #e5e7eb;
      border-radius: 16px;
      background: #f8fafc;
      color: #111827;
    }

    h3 {
      margin: 18px 0 8px;
      color: #111827;
      font-size: 17px;
    }

    p {
      margin: 0;
      color: #71717a;
      font-size: 13px;
      line-height: 1.7;
    }
  }

  .case-card {
    margin-bottom: 20px;
    overflow: hidden;
    border: 1px solid #e5e7eb;
    border-radius: 18px;
    background: #fff;
    cursor: pointer;
    box-shadow: 0 12px 28px rgba(17, 24, 39, 0.055);
    transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;

    &:hover {
      transform: translateY(-2px);
      border-color: #111827;
      box-shadow: 0 18px 36px rgba(17, 24, 39, 0.1);
    }

    .case-image {
      height: 118px;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #f8fafc;
      background-image:
        linear-gradient(rgba(17, 24, 39, 0.05) 1px, transparent 1px),
        linear-gradient(90deg, rgba(17, 24, 39, 0.05) 1px, transparent 1px);
      background-size: 26px 26px;

      :deep(svg) {
        width: 54px;
        height: 54px;
        color: #111827;
      }
    }

    .case-info {
      padding: 16px;

      h4 {
        margin: 0 0 6px;
        color: #111827;
        font-size: 16px;
        line-height: 1.45;
      }

      p {
        margin: 0 0 10px;
        color: #71717a;
        font-size: 13px;
      }

      .case-stats {
        display: flex;
        gap: 16px;
        color: #71717a;
        font-size: 12px;
      }

      .case-stat-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }

  :deep(.el-empty) {
    --el-empty-padding: 22px 0;
  }

  :deep(.el-empty__description p) {
    color: #71717a;
  }
}

@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
  .home-page .liquid-glass {
    background: rgba(248, 250, 252, 0.96);
  }
}

@media (prefers-reduced-motion: reduce) {
  .home-page * {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
  }
}

@media (max-width: 980px) {
  .home-page {
    .hero-section {
      grid-template-columns: 1fr;
      padding: 30px;

      h1 {
        font-size: 34px;
      }
    }

    .overview-grid,
    .features-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .task-grid {
      grid-template-columns: 1fr;
    }
  }
}

@media (max-width: 640px) {
  .home-page {
    padding: 16px 12px 40px;

    .hero-section {
      min-height: 0;
      padding: 24px;
      border-radius: 20px;

      h1 {
        font-size: 30px;
      }

      p {
        font-size: 15px;
      }
    }

    .hero-actions {
      :deep(.el-button) {
        width: 100%;
      }
    }

    .guest-strip,
    .section-heading {
      align-items: flex-start;
      flex-direction: column;
    }

    .overview-grid,
    .features-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
