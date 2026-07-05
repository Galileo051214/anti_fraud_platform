<template>
  <div class="recommend-page">
    <header class="recommend-page__hero">
      <h1 class="recommend-page__title">智能推荐</h1>
      <p class="recommend-page__subtitle">
        根据您的学习记录和特点，结合生命周期（新手期 / 成长期 / 成熟期）与上下文感知策略，为您推荐案例、资讯与闯关内容。
      </p>
      <div v-if="interest" class="recommend-page__context">
        <span class="recommend-page__stage">{{ interest.lifecycleStageName }}</span>
        <span class="recommend-page__meta">知识水平 {{ interest.knowledgeLevel ?? 0 }}</span>
        <div v-if="interest.weakPoints?.length" class="recommend-page__weak">
          <span class="recommend-page__weak-label">弱点标签</span>
          <el-tag
            v-for="w in interest.weakPoints.slice(0, 5)"
            :key="w"
            size="small"
            type="warning"
            effect="plain"
            class="recommend-page__weak-tag"
          >
            {{ w }}
          </el-tag>
        </div>
      </div>
    </header>

    <el-tabs v-model="activeTab" class="recommend-page__tabs">
      <el-tab-pane label="案例推荐" name="case" />
      <el-tab-pane label="资讯推荐" name="news" />
      <el-tab-pane label="闯关推荐" name="challenge" />
    </el-tabs>

    <div class="recommend-page__grid" v-loading="loading">
      <template v-if="list.length > 0">
        <article
          v-for="item in list"
          :key="`${item.itemType}-${item.itemId}`"
          class="rec-card"
          @click="onCardClick(item)"
          role="link"
          tabindex="0"
          @keydown.enter.prevent="onCardClick(item)"
        >
          <div
            class="rec-card__visual"
            :class="`rec-card__visual--${item.itemType}`"
            :aria-label="typeLabel(item.itemType)"
          >
            <component :is="cardIcon(item)" class="rec-card__icon" aria-hidden="true" />
          </div>
          <div class="rec-card__body">
            <h2 class="rec-card__title">{{ item.title }}</h2>
            <p class="rec-card__reason">{{ displayReason(item) }}</p>
            <p v-if="snippet(item.summary)" class="rec-card__summary">{{ snippet(item.summary) }}</p>
            <div v-if="item.tags?.length" class="rec-card__tags">
              <span v-for="tag in item.tags.slice(0, 4)" :key="tag" class="rec-card__tag">{{ tag }}</span>
            </div>
          </div>
        </article>
      </template>
      <el-empty
        v-else-if="!loading"
        class="recommend-page__empty"
        :description="emptyDescription"
      >
        <el-button v-if="errorMessage" type="primary" plain @click="loadList">重新加载</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getRecommendationList,
  getUserInterest,
  recordRecommendationClick,
  type RecommendationVO,
  type UserInterestVO
} from '@/api/recommendation'
import {
  IconShield, IconNews, IconAlert, IconTarget, IconMask,
  IconCoin, IconLove, IconPhone
} from '@/components/icons'
import { getRecommendationChallengeRoute } from '@/utils/challengeRoute'

const router = useRouter()

const activeTab = ref<'case' | 'news' | 'challenge'>('case')
const loading = ref(false)
const list = ref<RecommendationVO[]>([])
const interest = ref<UserInterestVO | null>(null)
const errorMessage = ref('')

const emptyDescription = computed(() => {
  return errorMessage.value || '暂无推荐，可先浏览案例、阅读资讯或完成闯关，系统会更新您的画像后再推荐'
})

const typeLabel = (t: string) => {
  const m: Record<string, string> = { case: '案例', news: '资讯', challenge: '闯关' }
  return m[t] || t
}

const cardIcon = (item: RecommendationVO) => {
  if (item.itemType === 'news') {
    const title = item.title || ''
    if (/预警|紧急|警惕/.test(title)) return IconAlert
    return IconNews
  }
  if (item.itemType === 'challenge') {
    return item.itemSubtype === 'scenario' || item.itemSubtype === 'agent_scenario' ? IconMask : IconTarget
  }
  const tag = item.tags?.[0] || ''
  if (/刷单|返利/.test(tag)) return IconCoin
  if (/杀猪|情感/.test(tag)) return IconLove
  if (/客服|电话/.test(tag)) return IconPhone
  return IconShield
}

const displayReason = (item: RecommendationVO) => {
  const r = item.reasons?.[0]
  if (r) return r
  return '根据混合推荐策略为您精选'
}

const snippet = (text?: string) => {
  if (!text) return ''
  const plain = text.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
  return plain.length > 96 ? `${plain.slice(0, 96)}…` : plain
}

const loadInterest = async () => {
  try {
    const res = await getUserInterest()
    interest.value = res ?? null
  } catch {
    interest.value = null
  }
}

const loadList = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getRecommendationList({ limit: 12, itemType: activeTab.value })
    list.value = Array.isArray(res) ? res : []
  } catch {
    list.value = []
    errorMessage.value = '推荐加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const onCardClick = (item: RecommendationVO) => {
  recordRecommendationClick(item.itemId, item.itemType).catch(() => {})
  const id = item.itemId
  if (item.itemType === 'news') {
    router.push(`/news/${id}`)
    return
  }
  if (item.itemType === 'challenge') {
    router.push(getRecommendationChallengeRoute(item))
    return
  }
  router.push(`/case/${id}`)
}

watch(activeTab, () => {
  loadList()
})

onMounted(() => {
  loadInterest()
  loadList()
})
</script>

<style scoped lang="scss">
.recommend-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 20px 48px;

  &__hero {
    margin-bottom: 20px;
  }

  &__title {
    font-size: clamp(1.5rem, 4vw, 2rem);
    font-weight: 600;
    color: hsl(220, 15%, 18%);
    margin: 0 0 10px;
    letter-spacing: -0.02em;
  }

  &__subtitle {
    margin: 0;
    font-size: 14px;
    line-height: 1.65;
    color: hsl(220, 9%, 46%);
    max-width: 52rem;
  }

  &__context {
    margin-top: 16px;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px 14px;
    padding: 12px 16px;
    background: hsl(215, 80%, 98%);
    border: 1px solid hsl(215, 45%, 90%);
    border-radius: 12px;
    box-shadow: 0 2px 10px hsla(220, 40%, 40%, 0.05);
  }

  &__stage {
    font-size: 13px;
    font-weight: 600;
    color: hsl(215, 70%, 42%);
    padding: 4px 10px;
    border-radius: 999px;
    background: #fff;
    border: 1px solid hsl(215, 55%, 88%);
  }

  &__meta {
    font-size: 13px;
    color: hsl(220, 9%, 40%);
  }

  &__weak {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    width: 100%;
  }

  &__weak-label {
    font-size: 12px;
    color: hsl(220, 9%, 45%);
  }

  &__weak-tag {
    border-radius: 999px;
  }

  &__tabs {
    margin-bottom: 8px;

    :deep(.el-tabs__header) {
      margin-bottom: 20px;
    }

    :deep(.el-tabs__item) {
      font-weight: 500;
    }
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 20px;
    min-height: 200px;
  }

  &__empty {
    grid-column: 1 / -1;
    padding: 40px 0;
  }
}

.rec-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid hsl(220, 14%, 92%);
  box-shadow: 0 2px 12px hsla(220, 25%, 40%, 0.06);
  transition:
    transform 200ms ease,
    box-shadow 200ms ease,
    border-color 200ms ease;

  &:hover {
    transform: translateY(-4px);
    border-color: hsl(215, 55%, 85%);
    box-shadow: 0 12px 28px hsla(215, 45%, 45%, 0.12);
  }

  &:active {
    transform: translateY(-2px) scale(0.995);
  }

  &:focus-visible {
    outline: 2px solid hsl(215, 70%, 55%);
    outline-offset: 3px;
  }

  &__visual {
    height: 132px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, hsl(250, 60%, 70%) 0%, hsl(215, 70%, 58%) 100%);

    &--case {
      background: linear-gradient(135deg, hsl(262, 52%, 68%) 0%, hsl(215, 72%, 56%) 100%);
    }

    &--news {
      background: linear-gradient(135deg, hsl(200, 55%, 62%) 0%, hsl(215, 65%, 55%) 100%);
    }

    &--challenge {
      background: linear-gradient(135deg, hsl(280, 45%, 65%) 0%, hsl(215, 75%, 55%) 100%);
    }
  }

  &__icon {
    width: 56px;
    height: 56px;
    color: #fff;
    filter: drop-shadow(0 4px 8px hsla(0, 0%, 0%, 0.12));
  }

  &__body {
    padding: 16px 16px 18px;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    color: hsl(220, 15%, 18%);
    margin: 0 0 8px;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__reason {
    margin: 0 0 8px;
    font-size: 13px;
    color: hsl(215, 70%, 42%);
    font-weight: 500;
    line-height: 1.45;
  }

  &__summary {
    margin: 0 0 12px;
    font-size: 13px;
    color: hsl(220, 9%, 46%);
    line-height: 1.55;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__tag {
    font-size: 12px;
    padding: 3px 10px;
    border-radius: 999px;
    background: hsl(215, 80%, 96%);
    color: hsl(215, 55%, 38%);
    border: 1px solid hsl(215, 45%, 88%);
  }
}

@media (max-width: 768px) {
  .recommend-page {
    padding: 16px 14px 36px;

    &__grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
