<template>
  <div class="case-detail" v-loading="loading">
    <div class="case-detail__container" v-if="caseData">
      <div class="case-detail__back">
        <el-button text @click="$router.back()">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
          返回
        </el-button>
      </div>

      <article class="case-detail__article">
        <header class="article__header">
          <div class="article__meta">
            <span class="article__type">{{ caseData.caseType }}</span>
            <span v-if="caseData.isFeatured" class="article__featured">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
              </svg>
              精选案例
            </span>
          </div>

          <h1 class="article__title">{{ caseData.title }}</h1>

          <div class="article__info">
            <div class="article__tags" v-if="caseData.tags.length">
              <span
                v-for="tag in caseData.tags"
                :key="tag.id"
                class="article__tag"
                :style="{ backgroundColor: tag.color + '20', color: tag.color }"
              >
                {{ tag.name }}
              </span>
            </div>

            <div class="article__badges">
              <span class="badge badge--difficulty" :class="`badge--level-${caseData.difficultyLevel}`">
                {{ caseData.difficultyName }}
              </span>
              <span class="badge badge--risk" :class="`badge--risk-${getRiskClass(caseData.riskLevel)}`">
                {{ caseData.riskLevel }}风险
              </span>
            </div>
          </div>

          <div class="article__stats">
            <span class="stat-item">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              {{ caseData.viewCount }} 次浏览
            </span>
            <span
              class="stat-item stat-item--like"
              :class="{ 'stat-item--active': caseData.isLiked }"
              @click="handleLike"
            >
              <svg viewBox="0 0 24 24" width="16" height="16" :fill="caseData.isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
              {{ caseData.likeCount }} 点赞
            </span>
            <span class="stat-item">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
              {{ formatTime(caseData.publishTime) }}
            </span>
          </div>
        </header>

        <div class="article__content" v-html="caseData.content"></div>

        <section class="article__section" v-if="caseData.targetGrades || caseData.targetMajors">
          <h3 class="section__title">适用对象</h3>
          <div class="section__content">
            <div v-if="caseData.targetGrades" class="target-info">
              <span class="target-label">目标年级：</span>
              <span class="target-value">{{ caseData.targetGrades.join('、') }}</span>
            </div>
            <div v-if="caseData.targetMajors" class="target-info">
              <span class="target-label">目标专业：</span>
              <span class="target-value">{{ caseData.targetMajors.join('、') }}</span>
            </div>
          </div>
        </section>

        <section class="article__section" v-if="caseData.scripts">
          <h3 class="section__title">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: -3px; margin-right: 6px;">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="9" y1="13" x2="15" y2="13"/>
              <line x1="9" y1="17" x2="13" y2="17"/>
            </svg>
            诈骗剧本
            <span class="scripts-summary">
              共 {{ parsedScripts?.nodes?.length || 0 }} 步 · {{ parsedScripts?.edges?.length || 0 }} 个分支
            </span>
          </h3>
          <div class="scripts-viewer">
            <template v-if="parsedScripts && parsedScripts.nodes?.length">
              <div class="scripts-viewer__nodes">
                <div
                  v-for="(node, idx) in parsedScripts.nodes"
                  :key="node.id"
                  class="script-node"
                  :class="getNodeClass(node)"
                >
                  <div class="script-node__header">
                    <div class="script-node__index">
                      <span class="script-node__step">第 {{ idx + 1 }} 步</span>
                    </div>
                    <div class="script-node__badges">
                      <span class="script-node__type" :class="`script-node__type--${node.type}`">
                        <span class="type-dot"></span>
                        {{ getTypeLabel(node.type) }}
                      </span>
                      <span
                        class="script-node__role"
                        :class="`script-node__role--${String(node.role)}`"
                      >
                        <span class="role-icon">{{ getRoleIcon(node.role) }}</span>
                        {{ getRoleLabel(node.role) }}
                      </span>
                    </div>
                  </div>

                  <h4 class="script-node__title" v-if="node.title">{{ node.title }}</h4>
                  <div class="script-node__content">{{ node.content }}</div>

                  <div v-if="node.riskTip" class="script-node__risk-tip">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" style="flex-shrink: 0; margin-top: 2px;">
                      <path d="M12 2L1 21h22L12 2zm0 4l8.5 14h-17L12 6zm-1 5v4h2v-4h-2zm0 6v2h2v-2h-2z"/>
                    </svg>
                    <div>
                      <div class="script-node__risk-label">风险提示</div>
                      <div class="script-node__risk-text">{{ node.riskTip }}</div>
                    </div>
                  </div>

                  <!-- 节点连接箭头（最后一个节点不显示） -->
                  <div v-if="idx < (parsedScripts.nodes?.length || 0) - 1" class="script-node__arrow">
                    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M6 9l6 6 6-6"/>
                    </svg>
                  </div>
                </div>
              </div>

              <div v-if="parsedScripts.edges?.length" class="scripts-viewer__edges">
                <div class="scripts-viewer__edges-title">
                  <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" style="vertical-align: -3px; margin-right: 6px;">
                    <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/>
                    <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>
                  </svg>
                  流程分支（共 {{ parsedScripts.edges.length }} 条）
                </div>
                <div class="scripts-viewer__edges-list">
                  <div
                    v-for="(edge, idx) in parsedScripts.edges"
                    :key="(edge as any).id ?? `${edge.from}-${edge.to}-${idx}`"
                    class="script-edge"
                    :class="getEdgeClass(edge)"
                  >
                    <div class="script-edge__route">
                      <span class="script-edge__from">{{ getNodeLabel(edge.from) }}</span>
                      <span class="script-edge__arrow" :class="edge.isSafeChoice ? 'safe' : 'risk'">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5">
                          <path d="M5 12h14M13 5l7 7-7 7"/>
                        </svg>
                      </span>
                      <span class="script-edge__to">{{ getNodeLabel(edge.to) }}</span>
                    </div>
                    <div class="script-edge__meta">
                      <span v-if="edge.label" class="script-edge__label">{{ edge.label }}</span>
                      <span v-if="edge.condition" class="script-edge__condition">触发：{{ edge.condition }}</span>
                      <span v-if="edge.isSafeChoice === true" class="script-edge__tag script-edge__tag--safe">
                        ✓ 安全选择
                      </span>
                      <span v-else-if="edge.isSafeChoice === false" class="script-edge__tag script-edge__tag--risk">
                        ⚠ 危险选择
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </template>

            <pre v-else class="scripts-content">{{ formatScriptsFallback(caseData.scripts) }}</pre>
          </div>
        </section>
      </article>

      <aside class="case-detail__sidebar">
        <div class="sidebar-card">
          <h4 class="sidebar-card__title">风险提示</h4>
          <div class="risk-meter">
            <div class="risk-meter__bar">
              <div class="risk-meter__fill" :style="{ width: (caseData.riskScore / 10 * 100) + '%' }"></div>
            </div>
            <div class="risk-meter__labels">
              <span>安全</span>
              <span class="risk-meter__value">{{ caseData.riskScore.toFixed(1) }}</span>
              <span>危险</span>
            </div>
          </div>
          <p class="risk-tip">本案例风险等级为「{{ caseData.riskLevel }}」，请提高警惕。</p>
        </div>

        <div class="sidebar-card" v-if="relatedCases.length">
          <h4 class="sidebar-card__title">相关案例</h4>
          <div class="related-list">
            <div
              v-for="item in relatedCases"
              :key="item.id"
              class="related-item"
              @click="goToDetail(item.id)"
              role="button"
              tabindex="0"
              @keydown.enter.prevent="goToDetail(item.id)"
              @keydown.space.prevent="goToDetail(item.id)"
            >
              <span class="related-item__title">{{ item.title }}</span>
              <span class="related-item__type">{{ item.caseType }}</span>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <el-empty v-else-if="!loading" description="案例不存在"></el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaseDetail, likeCase, unlikeCase, browseCase, getCasePage } from '@/api/case'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const caseData = ref<any>(null)
const relatedCases = ref<any[]>([])
let startTime = Date.now()

const sendBrowseRecord = () => {
  if (!caseData.value) return
  const duration = Math.floor((Date.now() - startTime) / 1000)
  browseCase(caseData.value.id, duration).catch(() => {})
}

const fetchCaseDetail = async (options?: { silent?: boolean }) => {
  if (!options?.silent) loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await getCaseDetail(id)
    if (!res) {
      caseData.value = null
      return
    }
    caseData.value = res

    // start browse timer - will send browse record with stayDuration on unmount
    if (!options?.silent && caseData.value) {
      startTime = Date.now()
    }
  } catch (error) {
    console.error('获取案例详情失败:', error)
    caseData.value = null
  } finally {
    if (!options?.silent) loading.value = false
  }
}

const fetchRelatedCases = async () => {
  try {
    const res = await getCasePage({ pageNum: 1, pageSize: 5 })
    const records = res.records ?? []
    const currentCaseId = Number(route.params.id)
    const safeCurrentCaseId = Number.isNaN(currentCaseId) ? -1 : currentCaseId
    relatedCases.value = records
      // 后端返回的 id 可能是 string/number，这里统一转成数字避免“过滤失败导致不跳转”
      .filter((c: any) => Number(c.id) !== safeCurrentCaseId)
      .slice(0, 4)
  } catch (error) {
    console.error('获取相关案例失败:', error)
  }
}

const handleLike = async () => {
  if (!caseData.value) return
  try {
    if (caseData.value.isLiked) {
      await unlikeCase(caseData.value.id)
    } else {
      await likeCase(caseData.value.id)
    }

    // 重新拉取详情，确保 isLiked 与 likeCount 与后端一致
    await fetchCaseDetail({ silent: true })
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error('点赞失败，请确认已登录且案例仍可访问')
  }
}

const goToDetail = (id: number | string) => {
  // router 中的案例详情路由是 /case/:id
  const caseId = Number(id)
  if (Number.isNaN(caseId)) return
  router.push(`/case/${caseId}`)
}

const getRiskClass = (riskLevel: string) => {
  const map: Record<string, string> = {
    '极高': 'highest',
    '高': 'high',
    '中': 'normal',
    '低': 'low',
    '极低': 'lowest'
  }
  return map[riskLevel] || 'normal'
}

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

type CaseScriptNode = {
  id: string
  type: string
  content: string
  role: string
  riskTip?: string
}

type CaseScriptEdge = {
  from: string
  to: string
  label?: string
  condition?: string
  isSafeChoice?: boolean
}

type CaseScriptsGraph = {
  nodes: CaseScriptNode[]
  edges: CaseScriptEdge[]
}

const parsedScripts = computed<CaseScriptsGraph | null>(() => {
  const raw = caseData.value?.scripts
  if (!raw) return null
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (!parsed?.nodes) return null
    return parsed as CaseScriptsGraph
  } catch {
    return null
  }
})

const formatScriptsFallback = (scripts: unknown) => {
  if (!scripts) return ''
  try {
    if (typeof scripts === 'string') return JSON.stringify(JSON.parse(scripts), null, 2)
    return JSON.stringify(scripts, null, 2)
  } catch {
    return String(scripts)
  }
}

// 节点类型标签
const getTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    start: '开始',
    dialog: '对话',
    end: '结局'
  }
  return map[type] || type
}

// 角色标签
const getRoleLabel = (role: string) => {
  const map: Record<string, string> = {
    narrator: '旁白',
    victim: '受害者',
    scammer: '诈骗方',
    system: '系统'
  }
  return map[role] || role
}

// 角色图标
const getRoleIcon = (role: string) => {
  const map: Record<string, string> = {
    narrator: '📢',
    victim: '🙋',
    scammer: '🎭',
    system: '⚙️'
  }
  return map[role] || '👤'
}

// 节点样式类
const getNodeClass = (node: any) => {
  const classes: string[] = []
  if (node.type === 'start') classes.push('script-node--start')
  if (node.type === 'end') {
    classes.push('script-node--end')
    if (node.id === 'e_safe') classes.push('script-node--safe')
    else if (node.id === 'e_risk') classes.push('script-node--risk')
    else if (node.id === 'e_loss') classes.push('script-node--loss')
  }
  if (node.riskTip) classes.push('script-node--has-risk')
  return classes.join(' ')
}

// 边样式类
const getEdgeClass = (edge: any) => {
  const classes: string[] = []
  if (edge.isSafeChoice === true) classes.push('script-edge--safe')
  else if (edge.isSafeChoice === false) classes.push('script-edge--risk')
  return classes.join(' ')
}

// 节点显示标签（用 title 字段，没有就用 id）
const getNodeLabel = (nodeId: string) => {
  if (!parsedScripts.value) return nodeId
  const node = parsedScripts.value.nodes.find(n => n.id === nodeId)
  if (!node) return nodeId
  if (node.title) return node.title
  // 结局节点特殊标记
  if (node.id === 'e_safe') return '✅ 及时脱身'
  if (node.id === 'e_risk') return '⚠️ 继续缴费'
  if (node.id === 'e_loss') return '❌ 平台关闭'
  return node.type === 'start' ? '🟢 起点' : node.type === 'end' ? '🏁 结局' : `节点 ${nodeId}`
}

onMounted(() => {
  fetchCaseDetail()
  fetchRelatedCases()
  
  window.addEventListener('beforeunload', sendBrowseRecord)
})

onUnmounted(() => {
  sendBrowseRecord()
  window.removeEventListener('beforeunload', sendBrowseRecord)
})

// 当路由参数变化时（同一路由组件复用），需要手动刷新详情与相关案例
watch(
  () => route.params.id,
  async (newId, oldId) => {
    if (newId === oldId) return

    // 先发送前一个案例的浏览记录
    sendBrowseRecord()

    caseData.value = null
    relatedCases.value = []

    await Promise.all([fetchCaseDetail(), fetchRelatedCases()])
  }
)
</script>

<style scoped lang="scss">
.case-detail {
  // 组件内补齐项目使用的变量名映射，避免全局未定义导致样式失效
  --text-primary: var(--text-color-primary);
  --text-secondary: var(--text-color-secondary);
  --text-placeholder: var(--text-color-placeholder);
  --border-color: var(--border-color-base);
  --bg-primary: #ffffff;
  --bg-secondary: var(--background-color-base);
  --bg-hover: #eef4ff;

  --transition-fast: 200ms ease-in-out;

  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;

  &__back {
    grid-column: 1 / -1;
    margin-bottom: 24px;
    display: flex;
    align-items: center;

    .el-button {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      color: var(--text-secondary);

      &:hover {
        color: var(--primary-color);
      }
    }
  }

  &__container {
    display: grid;
    grid-template-columns: 1fr 300px;
    gap: 24px;
    align-items: start;

    @media (max-width: 900px) {
      grid-template-columns: 1fr;
    }
  }

  &__article {
    background: var(--bg-primary);
    border: 1px solid rgba(64, 158, 255, 0.14);
    border-radius: 16px;
    padding: 24px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  }
}

.article {
  &__header {
    margin-bottom: 32px;
    padding-bottom: 24px;
    border-bottom: 1px solid var(--border-color);
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;
  }

  &__type {
    font-size: 13px;
    color: var(--text-secondary);
  }

  &__featured {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #f59e0b;
  }

  &__title {
    font-size: 28px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 16px;
    line-height: 1.4;
  }

  &__info {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;
    flex-wrap: wrap;
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__tag {
    display: inline-flex;
    align-items: center;
    padding: 4px 10px;
    font-size: 12px;
    border-radius: 4px;
  }

  &__badges {
    display: flex;
    gap: 8px;
  }

  &__stats {
    display: flex;
    align-items: center;
    gap: 24px;
  }

  &__content {
    font-size: 15px;
    line-height: 1.85;
    color: var(--text-primary);
    word-break: break-word;

    :deep(h1), :deep(h2), :deep(h3), :deep(h4) {
      margin: 20px 0 12px;
      font-weight: 600;
      color: var(--text-primary);
      line-height: 1.4;
    }
    :deep(h3) {
      font-size: 17px;
      padding-bottom: 6px;
      border-bottom: 2px solid rgba(64, 158, 255, 0.2);
    }
    :deep(h4) {
      font-size: 15px;
      color: #1e40af;
    }
    :deep(p) {
      margin: 0 0 12px;
      color: #374151;
    }
    :deep(strong) {
      color: #dc2626;
      font-weight: 600;
    }
    :deep(ul), :deep(ol) {
      margin: 8px 0 12px;
      padding-left: 24px;
    }
    :deep(li) {
      margin-bottom: 4px;
      color: #374151;
    }
    :deep(code) {
      background: rgba(64, 158, 255, 0.08);
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 13px;
      color: #1e40af;
      font-family: 'Monaco', 'Menlo', monospace;
    }
  }

  &__section {
    margin-top: 32px;
    padding: 20px;
    background: var(--bg-secondary);
    border-radius: 12px;
  }
}

.section__title {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.section__content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.target-info {
  display: flex;
  gap: 8px;
}

.target-label {
  color: var(--text-secondary);
  font-size: 14px;
}

.target-value {
  color: var(--text-primary);
  font-size: 14px;
}

.scripts-viewer {
  background: #ffffff;
  border: 1px solid rgba(64, 158, 255, 0.14);
  border-radius: 12px;
  padding: 20px;
  overflow-x: auto;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.scripts-summary {
  font-size: 13px;
  font-weight: normal;
  color: var(--text-secondary);
  margin-left: 12px;
  padding: 2px 10px;
  background: rgba(64, 158, 255, 0.08);
  border-radius: 12px;
}

.scripts-viewer__nodes {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
  position: relative;
}

.script-node {
  position: relative;
  background: linear-gradient(135deg, #ffffff 0%, #fafbff 100%);
  border: 2px solid rgba(64, 158, 255, 0.18);
  border-radius: 14px;
  padding: 18px;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.06);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(64, 158, 255, 0.12);
    border-color: rgba(64, 158, 255, 0.4);
  }

  // 开始节点 - 绿色
  &--start {
    background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
    border-color: #10b981;
    box-shadow: 0 4px 16px rgba(16, 185, 129, 0.12);
  }

  // 结局节点 - 默认灰色
  &--end {
    background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
    border-color: #6b7280;
  }

  // 安全结局 - 绿色
  &--safe {
    background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
    border-color: #10b981;
    box-shadow: 0 4px 16px rgba(16, 185, 129, 0.15);
  }

  // 风险结局 - 黄色
  &--risk {
    background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
    border-color: #f59e0b;
    box-shadow: 0 4px 16px rgba(245, 158, 11, 0.15);
  }

  // 损失结局 - 红色
  &--loss {
    background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
    border-color: #ef4444;
    box-shadow: 0 4px 16px rgba(239, 68, 68, 0.15);
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 12px;
    flex-wrap: wrap;
  }

  &__index {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__step {
    display: inline-flex;
    align-items: center;
    padding: 3px 10px;
    font-size: 11px;
    font-weight: 600;
    color: #1e40af;
    background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
    border-radius: 12px;
    letter-spacing: 0.3px;
  }

  &__badges {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
  }

  &__type {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 11px;
    padding: 3px 8px;
    border-radius: 6px;
    background: rgba(107, 114, 128, 0.1);
    color: #4b5563;

    &--start {
      background: rgba(16, 185, 129, 0.12);
      color: #047857;
    }
    &--dialog {
      background: rgba(59, 130, 246, 0.12);
      color: #1e40af;
    }
    &--end {
      background: rgba(107, 114, 128, 0.12);
      color: #374151;
    }
  }

  .type-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: currentColor;
  }

  &__role {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    font-size: 11px;
    padding: 3px 8px;
    border-radius: 6px;
    border: 1px solid transparent;

    &--narrator {
      color: hsl(220, 10%, 40%);
      background: #f3f4f6;
      border-color: rgba(0, 0, 0, 0.06);
    }
    &--victim {
      color: hsl(38, 92%, 30%);
      background: #fef3c7;
      border-color: rgba(245, 158, 11, 0.2);
    }
    &--scammer {
      color: hsl(0, 65%, 45%);
      background: #fee2e2;
      border-color: rgba(239, 68, 68, 0.2);
    }
    &--system {
      color: hsl(220, 60%, 35%);
      background: #dbeafe;
      border-color: rgba(59, 130, 246, 0.2);
    }
  }

  .role-icon {
    font-size: 12px;
  }

  &__title {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 8px;
    line-height: 1.4;
  }

  &__content {
    font-size: 14px;
    line-height: 1.75;
    color: var(--text-primary);
    word-break: break-word;
  }

  &__risk-tip {
    margin-top: 12px;
    padding: 10px 12px;
    border-radius: 10px;
    border-left: 4px solid #ef4444;
    background: linear-gradient(135deg, #fef2f2 0%, #fff5f5 100%);
    display: flex;
    align-items: flex-start;
    gap: 8px;
    color: #991b1b;
    font-size: 13px;
    line-height: 1.6;
  }

  &__risk-label {
    font-weight: 600;
    font-size: 12px;
    color: #dc2626;
    margin-bottom: 2px;
  }

  &__risk-text {
    color: #7f1d1d;
  }

  &__arrow {
    position: absolute;
    bottom: -16px;
    left: 50%;
    transform: translateX(-50%);
    color: #94a3b8;
    z-index: 1;
    background: #f8fafc;
    border-radius: 50%;
    width: 28px;
    height: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
  }
}

.scripts-viewer__edges {
  margin-top: 32px;
  padding-top: 20px;
  border-top: 2px dashed var(--border-color);
}

.scripts-viewer__edges-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 14px;
  display: flex;
  align-items: center;
}

.scripts-viewer__edges-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 10px;
}

.script-edge {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1.5px solid rgba(64, 158, 255, 0.18);
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
  transition: all 0.2s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }

  &--safe {
    background: linear-gradient(135deg, #ecfdf5 0%, #f0fdf4 100%);
    border-color: rgba(16, 185, 129, 0.4);
  }

  &--risk {
    background: linear-gradient(135deg, #fef2f2 0%, #fff5f5 100%);
    border-color: rgba(239, 68, 68, 0.4);
  }

  &__route {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-primary);
    margin-bottom: 6px;
  }

  &__from, &__to {
    flex: 1;
    font-size: 12px;
    padding: 4px 8px;
    background: rgba(0, 0, 0, 0.04);
    border-radius: 6px;
    text-align: center;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__arrow {
    display: flex;
    align-items: center;
    justify-content: center;
    color: #94a3b8;

    &.safe {
      color: #10b981;
    }
    &.risk {
      color: #ef4444;
    }
  }

  &__meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: var(--text-secondary);
  }

  &__label {
    padding: 2px 8px;
    background: rgba(64, 158, 255, 0.08);
    color: #1e40af;
    border-radius: 4px;
    font-weight: 500;
  }

  &__condition {
    color: var(--text-secondary);
    font-size: 11px;
  }

  &__tag {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 600;
    margin-left: auto;

    &--safe {
      background: rgba(16, 185, 129, 0.15);
      color: #047857;
    }
    &--risk {
      background: rgba(239, 68, 68, 0.15);
      color: #b91c1c;
    }
  }
}

.scripts-content {
  margin: 0;
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);

  &--like {
    cursor: pointer;
    transition: color 0.2s;

    &:hover {
      color: #ef4444;
    }
  }

  &--active {
    color: #ef4444;
  }
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  font-size: 12px;
  border-radius: 4px;

  &--difficulty {
    color: #6b7280;
    background: #f3f4f6;

    &.badge--level-1 { color: #10b981; background: #d1fae5; }
    &.badge--level-2 { color: #3b82f6; background: #dbeafe; }
    &.badge--level-3 { color: #f59e0b; background: #fef3c7; }
    &.badge--level-4 { color: #ef4444; background: #fee2e2; }
    &.badge--level-5 { color: #dc2626; background: #fecaca; }
  }

  &--risk {
    &.badge--risk-highest { color: #dc2626; background: #fef2f2; }
    &.badge--risk-high { color: #ef4444; background: #fee2e2; }
    &.badge--risk-normal { color: #f59e0b; background: #fef3c7; }
    &.badge--risk-low { color: #10b981; background: #d1fae5; }
    &.badge--risk-lowest { color: #6b7280; background: #f3f4f6; }
  }
}

.case-detail__sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-card {
  background: var(--bg-primary);
  border: 1px solid rgba(64, 158, 255, 0.14);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.03);

  &__title {
    font-size: 15px;
    font-weight: 500;
    color: var(--text-primary);
    margin: 0 0 16px;
  }
}

.risk-meter {
  &__bar {
    height: 8px;
    background: linear-gradient(to right, #10b981, #f59e0b, #ef4444);
    border-radius: 4px;
    overflow: hidden;
  }

  &__fill {
    height: 100%;
    background: #fff;
    opacity: 0.3;
  }

  &__labels {
    display: flex;
    justify-content: space-between;
    margin-top: 8px;
    font-size: 11px;
    color: var(--text-secondary);
  }

  &__value {
    font-weight: 600;
    color: var(--text-primary);
  }
}

.risk-tip {
  margin: 16px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.related-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.related-item {
  padding: 14px;
  background: #ffffff; // 圆角白底名片
  border: 1px solid rgba(64, 158, 255, 0.16);
  border-radius: 16px;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: transform var(--transition-fast),
              box-shadow var(--transition-fast),
              border-color var(--transition-fast),
              background-color var(--transition-fast);

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(64, 158, 255, 0.35);
    background-color: var(--bg-hover);
    box-shadow: 0 12px 25px rgba(64, 158, 255, 0.10);
  }

  &:active {
    transform: translateY(-1px) scale(0.99);
  }

  &:focus-visible {
    outline: 2px solid rgba(64, 158, 255, 0.55);
    outline-offset: 2px;
  }

  &__title {
    display: block;
    font-size: 13px;
    color: var(--text-primary);
    margin-bottom: 4px;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__type {
    font-size: 11px;
    color: var(--text-secondary);
  }
}
</style>
