<template>
  <div class="ranking-page">
    <header class="ranking-header">
      <div class="header-content">
        <h1>排行榜</h1>
        <p class="header-subtitle">看看谁是最强的反诈达人</p>
      </div>
    </header>

    <div class="ranking-container">
      <div class="user-rank-card" v-if="currentUserRank">
        <div class="rank-card__info">
          <span class="rank-card__label">我的排名</span>
          <span class="rank-card__value" :class="{ 'rank--top': currentUserRank.rank <= 3 }">
            {{ currentUserRank.rank > 0 ? `第 ${currentUserRank.rank} 名` : '未上榜' }}
          </span>
        </div>
        <div class="rank-card__score">
          <span class="score-value">{{ currentUserRank.score }}</span>
          <span class="score-label">积分</span>
        </div>
      </div>

      <el-tabs v-model="activePeriod" @tab-change="handleTabChange">
        <el-tab-pane label="日榜" name="daily">
          <template #label>
            <span class="tab-label">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="5"/>
                <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/>
              </svg>
              日榜
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="周榜" name="weekly">
          <template #label>
            <span class="tab-label">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                <line x1="16" y1="2" x2="16" y2="6"/>
                <line x1="8" y1="2" x2="8" y2="6"/>
                <line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              周榜
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="总榜" name="all">
          <template #label>
            <span class="tab-label">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
              </svg>
              总榜
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <div class="ranking-list" v-loading="loading">
        <template v-if="rankingList.length > 0">
          <div
            v-for="(item, index) in rankingList"
            :key="item.userId"
            class="ranking-item"
            :class="{ 'item--top': index < 3, 'item--current': item.isCurrentUser }"
          >
            <div class="item__rank" :class="`rank--${index + 1}`">
              <template v-if="index < 3">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path v-if="index === 0" d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                  <path v-else d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                </svg>
              </template>
              <template v-else>{{ index + 1 }}</template>
            </div>

            <el-avatar :size="48" :src="item.avatar" class="item__avatar">
              {{ item.nickname?.[0] || '?' }}
            </el-avatar>

            <div class="item__info">
              <div class="item__name">
                {{ item.nickname || '匿名用户' }}
                <span class="item__badge" v-if="item.isCurrentUser">我</span>
              </div>
              <div class="item__meta">{{ item.grade || '' }} {{ item.grade && item.major ? '·' : '' }} {{ item.major || '' }}</div>
            </div>

            <div class="item__score">
              <span class="score-value">{{ item.score }}</span>
              <span class="score-label">积分</span>
            </div>
          </div>
        </template>
        <div v-else class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" stroke-linecap="round"/>
          </svg>
          <p>暂无排行数据</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDailyLeaderboard, getWeeklyLeaderboard, getAllTimeLeaderboard, getUserRank, type LeaderboardVO } from '@/api/challenge'

const loading = ref(false)
const activePeriod = ref('daily')
const rankingList = ref<LeaderboardVO[]>([])
const currentUserRank = ref<LeaderboardVO | null>(null)

const fetchRanking = async () => {
  loading.value = true
  try {
    let res
    switch (activePeriod.value) {
      case 'daily':
        res = await getDailyLeaderboard(50)
        rankingList.value = res.data || []
        break
      case 'weekly':
        res = await getWeeklyLeaderboard(50)
        rankingList.value = res.data || []
        break
      case 'all':
        res = await getAllTimeLeaderboard(50)
        rankingList.value = res.data || []
        break
    }

    const userRankRes = await getUserRank(activePeriod.value)
    currentUserRank.value = userRankRes.data
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  fetchRanking()
}

onMounted(() => {
  fetchRanking()
})
</script>

<style scoped lang="scss">
.ranking-page {
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
}

.ranking-header {
  background: linear-gradient(135deg, hsl(38, 92%, 50%) 0%, hsl(38, 80%, 40%) 100%);
  padding: 48px 24px;
  color: white;
  text-align: center;
}

.header-content {
  max-width: 600px;
  margin: 0 auto;

  h1 {
    font-size: clamp(1.5rem, 4vw, 2rem);
    font-weight: 700;
    margin: 0 0 8px;
    letter-spacing: -0.02em;
  }

  .header-subtitle {
    margin: 0;
    opacity: 0.9;
    font-size: 0.95rem;
  }
}

.ranking-container {
  max-width: 700px;
  margin: -24px auto 0;
  padding: 0 24px 48px;
}

.user-rank-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 20px 24px;
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--shadow-md);
}

.rank-card__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rank-card__label {
  font-size: 0.85rem;
  color: var(--text-muted);
}

.rank-card__value {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-primary);

  &.rank--top {
    color: var(--warning);
  }
}

.rank-card__score {
  text-align: right;

  .score-value {
    display: block;
    font-size: 1.75rem;
    font-weight: 700;
    color: var(--primary);
    line-height: 1;
  }

  .score-label {
    font-size: 0.8rem;
    color: var(--text-muted);
  }
}

.ranking-container :deep(.el-tabs) {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);

  .el-tabs__header {
    margin-bottom: 0;
  }

  .el-tabs__nav-wrap::after {
    display: none;
  }

  .el-tabs__item {
    font-weight: 500;
    font-size: 0.95rem;
    padding: 0 20px;

    &.is-active {
      color: var(--primary);
    }
  }

  .el-tabs__active-bar {
    background: var(--primary);
    height: 3px;
  }
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;

  svg {
    width: 16px;
    height: 16px;
  }
}

.ranking-list {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid hsl(220, 15%, 95%);
  transition: background var(--transition-fast);

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: hsl(220, 15%, 98%);
  }

  &.item--top {
    background: linear-gradient(90deg, hsla(38, 92%, 50%, 0.08) 0%, transparent 100%);

    .item__rank {
      background: var(--warning);
      color: white;
    }
  }

  &.item--current {
    background: var(--primary-light);
  }

  &:nth-child(1).item--top .item__rank {
    background: hsl(45, 90%, 55%);
  }

  &:nth-child(2).item--top .item__rank {
    background: hsl(220, 10%, 60%);
  }

  &:nth-child(3).item--top .item__rank {
    background: hsl(20, 60%, 55%);
  }
}

.item__rank {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: hsl(220, 15%, 90%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.9rem;
  color: var(--text-secondary);
  flex-shrink: 0;

  svg {
    width: 20px;
    height: 20px;
  }
}

.item__avatar {
  flex-shrink: 0;
  background: var(--primary);
  color: white;
  font-weight: 600;
}

.item__info {
  flex: 1;
  min-width: 0;
}

.item__name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 8px;
  background: var(--primary);
  color: white;
  border-radius: 10px;
  font-size: 0.7rem;
  font-weight: 600;
}

.item__meta {
  font-size: 0.8rem;
  color: var(--text-muted);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item__score {
  text-align: right;
  flex-shrink: 0;

  .score-value {
    display: block;
    font-size: 1.25rem;
    font-weight: 700;
    color: var(--danger);
    line-height: 1;
  }

  .score-label {
    font-size: 0.7rem;
    color: var(--text-muted);
  }
}

.empty-state {
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
    margin: 0;
  }
}

@media (max-width: 768px) {
  .ranking-header {
    padding: 32px 16px;
  }

  .ranking-container {
    padding: 0 16px 32px;
  }

  .ranking-item {
    padding: 12px 16px;
    gap: 12px;
  }

  .item__name {
    font-size: 0.95rem;
  }

  .item__score .score-value {
    font-size: 1.1rem;
  }
}
</style>
