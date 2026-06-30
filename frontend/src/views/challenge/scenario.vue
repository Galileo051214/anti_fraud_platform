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

          <section class="scenario-flow" v-if="flowNodes.length">
            <div class="scenario-flow__header">
              <div>
                <h2>剧本流程</h2>
                <span>{{ flowVisitedCount }} / {{ flowNodes.length }} 节点</span>
              </div>
              <el-tag size="small" effect="plain">{{ currentNode?.title || '当前节点' }}</el-tag>
            </div>

            <div class="scenario-flow__viewport">
              <div class="scenario-flow__canvas" :style="flowCanvasStyle">
                <svg class="scenario-flow__edges" :viewBox="flowViewBox">
                  <defs>
                    <marker
                      id="scenario-flow-arrow"
                      markerWidth="10"
                      markerHeight="10"
                      refX="8"
                      refY="5"
                      orient="auto"
                      markerUnits="strokeWidth"
                    >
                      <path d="M 0 0 L 10 5 L 0 10 z" />
                    </marker>
                    <marker
                      id="scenario-flow-arrow-active"
                      markerWidth="10"
                      markerHeight="10"
                      refX="8"
                      refY="5"
                      orient="auto"
                      markerUnits="strokeWidth"
                    >
                      <path d="M 0 0 L 10 5 L 0 10 z" />
                    </marker>
                  </defs>
                  <g
                    v-for="edgeView in flowEdgeViews"
                    :key="edgeView.key"
                    class="scenario-flow__edge"
                    :class="{
                      'scenario-flow__edge--active': edgeView.isActive,
                      'scenario-flow__edge--available': edgeView.isAvailable
                    }"
                  >
                    <path
                      :d="flowEdgePath(edgeView)"
                      :marker-end="edgeView.isActive || edgeView.isAvailable ? 'url(#scenario-flow-arrow-active)' : 'url(#scenario-flow-arrow)'"
                    />
                    <g
                      v-if="edgeView.edge.label"
                      class="scenario-flow__edge-label"
                      :transform="`translate(${edgeView.labelX}, ${edgeView.labelY})`"
                    >
                      <rect x="-58" y="-14" width="116" height="28" rx="14" />
                      <text text-anchor="middle" dominant-baseline="central">
                        {{ shortFlowLabel(edgeView.edge.label) }}
                      </text>
                    </g>
                  </g>
                </svg>

                <div
                  v-for="(node, index) in flowNodes"
                  :key="node.id"
                  class="scenario-flow__node"
                  :class="flowNodeClass(node)"
                  :style="flowNodeStyle(node, index)"
                >
                  <div class="scenario-flow__node-meta">
                    <span>{{ nodeTypeName(node.type) }}</span>
                    <small>{{ roleDisplayName(node.role) }}</small>
                  </div>
                  <strong>{{ node.title || node.id }}</strong>
                  <p>{{ flowNodeSummary(node) }}</p>
                </div>
              </div>
            </div>
          </section>

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
                    :class="{
                      'item--safe': recordScoreType(record) === 'safe',
                      'item--danger': recordScoreType(record) === 'risk',
                      'item--neutral': recordScoreType(record) === 'none'
                    }"
                  >
                    <span class="history__choice">{{ record.choiceLabel }}</span>
                    <span class="history__badge">
                      <svg v-if="recordScoreType(record) === 'safe'" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
                      </svg>
                      <svg v-else-if="recordScoreType(record) === 'risk'" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                      </svg>
                      <svg v-else viewBox="0 0 24 24" fill="currentColor">
                        <path d="M12 4l1.41 1.41L8.83 10H20v2H8.83l4.58 4.59L12 18l-7-7 7-7z"/>
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

            <section class="scenario-review" v-if="decisionHistory.length">
              <h2 class="scenario-review__title">决策复盘</h2>
              <div class="scenario-review__list">
                <div
                  v-for="(record, idx) in decisionHistory"
                  :key="`${record.nodeId}-${record.edgeId}-${idx}`"
                  class="scenario-review__item"
                  :class="{
                    'scenario-review__item--safe': recordScoreType(record) === 'safe',
                    'scenario-review__item--risk': recordScoreType(record) === 'risk',
                    'scenario-review__item--neutral': recordScoreType(record) === 'none'
                  }"
                >
                  <span class="scenario-review__step">第 {{ idx + 1 }} 步</span>
                  <span class="scenario-review__choice">{{ record.choiceLabel }}</span>
                  <el-tag size="small" :type="recordTagType(record)">
                    {{ recordScoreText(record) }}
                  </el-tag>
                </div>
              </div>
            </section>
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
  type ScenarioEdgeVO,
  type ScenarioNode,
  type ScenarioEdge,
  type DecisionRecord,
  type ScenarioScoreType
} from '@/api/challenge'
import { useScoreStore } from '@/stores/score'

const FLOW_NODE_WIDTH = 176
const FLOW_NODE_HEIGHT = 118
const FLOW_NODE_GAP_X = 248
const FLOW_NODE_GAP_Y = 162

interface FlowPoint {
  x: number
  y: number
}

interface FlowEdgeView {
  key: string
  edge: ScenarioEdge
  x1: number
  y1: number
  x2: number
  y2: number
  labelX: number
  labelY: number
  isActive: boolean
  isAvailable: boolean
}

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
const scenarioScript = computed(() => progress.value?.script)
const flowNodes = computed(() => scenarioScript.value?.nodes || [])
const flowEdges = computed(() => scenarioScript.value?.edges || [])
const safeChoiceCount = computed(() =>
  decisionHistory.value.filter(r => recordScoreType(r) === 'safe').length
)

const recordScoreType = (record: DecisionRecord): ScenarioScoreType => {
  if (record.scoreType === 'safe' || record.scoreType === 'risk' || record.scoreType === 'none') {
    return record.scoreType
  }
  if (record.isSafeChoice === true) return 'safe'
  if (record.isSafeChoice === false) return 'risk'
  return 'none'
}

const recordTagType = (record: DecisionRecord) => {
  const scoreType = recordScoreType(record)
  if (scoreType === 'safe') return 'success'
  if (scoreType === 'risk') return 'danger'
  return 'info'
}

const recordScoreText = (record: DecisionRecord) => {
  const scoreType = recordScoreType(record)
  if (scoreType === 'safe') return '安全选择'
  if (scoreType === 'risk') return '风险选择'
  return '剧情推进'
}

const edgeIdOf = (edge: ScenarioEdge) => `${edge.from}_${edge.to}`

const activeEdgeIds = computed(() => new Set(decisionHistory.value.map(record => record.edgeId)))

const nextNodeIds = computed(() => new Set(availableChoices.value.map(choice => choice.toNode)))

const visitedNodeIds = computed(() => {
  const ids = new Set<string>()
  if (scenarioScript.value?.startNodeId) {
    ids.add(scenarioScript.value.startNodeId)
  }

  decisionHistory.value.forEach(record => {
    ids.add(record.nodeId)
    const edge = flowEdges.value.find(item => edgeIdOf(item) === record.edgeId)
    if (edge) {
      ids.add(edge.to)
    }
  })

  if (progress.value?.currentNode) {
    ids.add(progress.value.currentNode)
  }
  if (currentNode.value?.id) {
    ids.add(currentNode.value.id)
  }

  return ids
})

const flowVisitedCount = computed(() => flowNodes.value.filter(node => visitedNodeIds.value.has(node.id)).length)

const normalizeFlowPosition = (node: ScenarioNode, index: number): FlowPoint => {
  const x = node.position?.x
  const y = node.position?.y
  if (Number.isFinite(x) && Number.isFinite(y)) {
    return {
      x: Math.max(24, Math.round(Number(x))),
      y: Math.max(24, Math.round(Number(y)))
    }
  }

  const col = index % 3
  const row = Math.floor(index / 3)
  return {
    x: 32 + col * FLOW_NODE_GAP_X,
    y: 32 + row * FLOW_NODE_GAP_Y
  }
}

const flowNodePositions = computed(() => {
  const map = new Map<string, FlowPoint>()
  flowNodes.value.forEach((node, index) => {
    map.set(node.id, normalizeFlowPosition(node, index))
  })
  return map
})

const flowCanvasBounds = computed(() => {
  const positions = Array.from(flowNodePositions.value.values())
  if (!positions.length) {
    return { width: 720, height: 360 }
  }

  const maxX = Math.max(...positions.map(position => position.x))
  const maxY = Math.max(...positions.map(position => position.y))
  return {
    width: Math.max(720, maxX + FLOW_NODE_WIDTH + 72),
    height: Math.max(360, maxY + FLOW_NODE_HEIGHT + 72)
  }
})

const flowCanvasStyle = computed(() => ({
  width: `${flowCanvasBounds.value.width}px`,
  height: `${flowCanvasBounds.value.height}px`
}))

const flowViewBox = computed(() => `0 0 ${flowCanvasBounds.value.width} ${flowCanvasBounds.value.height}`)

const flowEdgeViews = computed<FlowEdgeView[]>(() => {
  return flowEdges.value
    .map((edge, index) => {
      const fromPosition = flowNodePositions.value.get(edge.from)
      const toPosition = flowNodePositions.value.get(edge.to)
      if (!fromPosition || !toPosition) {
        return null
      }

      const forward = toPosition.x >= fromPosition.x
      const x1 = forward ? fromPosition.x + FLOW_NODE_WIDTH : fromPosition.x
      const x2 = forward ? toPosition.x : toPosition.x + FLOW_NODE_WIDTH
      const y1 = fromPosition.y + FLOW_NODE_HEIGHT / 2
      const y2 = toPosition.y + FLOW_NODE_HEIGHT / 2

      return {
        key: `${edge.from}-${edge.to}-${index}`,
        edge,
        x1,
        y1,
        x2,
        y2,
        labelX: (x1 + x2) / 2,
        labelY: (y1 + y2) / 2 - 16,
        isActive: activeEdgeIds.value.has(edgeIdOf(edge)),
        isAvailable: edge.from === progress.value?.currentNode
      }
    })
    .filter((edgeView): edgeView is FlowEdgeView => Boolean(edgeView))
})

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

const formatTime = (_nodeId: string) => {
  const now = new Date()
  return `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
}

const flowEdgePath = (edgeView: FlowEdgeView) => {
  const direction = edgeView.x2 >= edgeView.x1 ? 1 : -1
  const controlOffset = Math.max(72, Math.abs(edgeView.x2 - edgeView.x1) * 0.42)
  const c1x = edgeView.x1 + controlOffset * direction
  const c2x = edgeView.x2 - controlOffset * direction
  return `M ${edgeView.x1} ${edgeView.y1} C ${c1x} ${edgeView.y1}, ${c2x} ${edgeView.y2}, ${edgeView.x2} ${edgeView.y2}`
}

const flowNodeStyle = (node: ScenarioNode, index: number) => {
  const position = flowNodePositions.value.get(node.id) || normalizeFlowPosition(node, index)
  return {
    width: `${FLOW_NODE_WIDTH}px`,
    minHeight: `${FLOW_NODE_HEIGHT}px`,
    transform: `translate(${position.x}px, ${position.y}px)`
  }
}

const flowNodeClass = (node: ScenarioNode) => {
  const isCurrent = node.id === progress.value?.currentNode
  const isVisited = visitedNodeIds.value.has(node.id)
  const isNext = nextNodeIds.value.has(node.id)
  return {
    'scenario-flow__node--current': isCurrent,
    'scenario-flow__node--visited': isVisited,
    'scenario-flow__node--next': isNext,
    'scenario-flow__node--end': Boolean(scenarioScript.value?.endNodeIds?.includes(node.id)),
    'scenario-flow__node--muted': !isCurrent && !isVisited && !isNext
  }
}

const shortFlowLabel = (label?: string) => {
  if (!label) return ''
  return label.length > 10 ? `${label.slice(0, 10)}...` : label
}

const nodeTypeName = (type: string) => {
  const typeMap: Record<string, string> = {
    start: '开始',
    dialog: '对白',
    decision: '决策',
    result: '结果',
    end: '结局'
  }
  return typeMap[type] || '节点'
}

const roleDisplayName = (role: string) => {
  const roleMap: Record<string, string> = {
    scammer: '诈骗方',
    victim: '受害者',
    narrator: '旁白'
  }
  return roleMap[role] || '旁白'
}

const flowNodeSummary = (node: ScenarioNode) => {
  if (node.id === currentNode.value?.id) {
    const text = currentNode.value?.content || node.content
    return text || '当前剧情'
  }

  const historyRecord = decisionHistory.value.find(record => record.nodeId === node.id)
  if (historyRecord) {
    return `已选择：${historyRecord.choiceLabel}`
  }

  if (nextNodeIds.value.has(node.id)) {
    return '下一步可到达'
  }

  if (node.id === scenarioScript.value?.startNodeId) {
    return '起始节点'
  }

  if (visitedNodeIds.value.has(node.id)) {
    return '已到达'
  }

  return '待探索'
}

const getErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
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
    progress.value = res

    if (progress.value.status !== 'in_progress') {
      if (progress.value.passed) {
        const rewardText = progress.value.earnedScore
          ? `获得 ${progress.value.earnedScore} 积分奖励！`
          : '本次通关已记录。'
        ElMessage.success(`恭喜通关！${rewardText}`)
        try {
          await scoreStore.fetchScoreInfo()
        } catch {
          // 忽略刷新失败，避免影响结局展示
        }
      } else {
        ElMessage.warning('未能通关，再接再厉！')
      }
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '操作失败，请重试'))
  } finally {
    submitting.value = false
  }
}

const handleRetry = async () => {
  try {
    await resetScenario(challengeId.value)
    const res = await startScenario(challengeId.value)
    progress.value = res
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '重置失败，请刷新重试'))
  }
}

const fetchProgress = async () => {
  loading.value = true
  try {
    challengeId.value = Number(route.params.id)
    const res = await startScenario(challengeId.value)
    progress.value = res
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '加载情景模拟失败'))
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
  max-width: 1080px;
  margin: 0 auto;
  padding: 32px 24px;
}

.progress-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  max-width: 700px;
  margin-left: auto;
  margin-right: auto;

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

.scenario-flow {
  background: var(--bg-card);
  border: 1px solid hsl(220, 15%, 90%);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  margin-bottom: 24px;
  overflow: hidden;
}

.scenario-flow__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid hsl(220, 15%, 92%);

  h2 {
    color: var(--text-primary);
    font-size: 1.05rem;
    font-weight: 700;
    margin: 0 0 4px;
  }

  span {
    color: var(--text-muted);
    font-size: 0.85rem;
  }
}

.scenario-flow__viewport {
  overflow: auto;
  padding: 18px;
  background:
    linear-gradient(hsla(220, 15%, 88%, 0.5) 1px, transparent 1px),
    linear-gradient(90deg, hsla(220, 15%, 88%, 0.5) 1px, transparent 1px),
    hsl(220, 20%, 98%);
  background-size: 24px 24px;
}

.scenario-flow__canvas {
  position: relative;
  min-width: 100%;
  border-radius: var(--radius-sm);
}

.scenario-flow__edges {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  overflow: visible;

  marker path {
    fill: hsl(220, 12%, 70%);
  }

  #scenario-flow-arrow-active path {
    fill: var(--primary);
  }
}

.scenario-flow__edge {
  path {
    fill: none;
    stroke: hsl(220, 12%, 70%);
    stroke-width: 2;
    stroke-linecap: round;
    opacity: 0.78;
  }

  &--active path,
  &--available path {
    stroke: var(--primary);
    stroke-width: 3;
    opacity: 1;
  }

  &--available path {
    stroke-dasharray: 8 8;
  }
}

.scenario-flow__edge-label {
  rect {
    fill: hsla(0, 0%, 100%, 0.92);
    stroke: hsl(220, 15%, 88%);
  }

  text {
    fill: var(--text-secondary);
    font-size: 12px;
    font-weight: 600;
  }

  .scenario-flow__edge--active &,
  .scenario-flow__edge--available & {
    rect {
      stroke: var(--primary);
    }

    text {
      fill: var(--primary);
    }
  }
}

.scenario-flow__node {
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  background: hsla(0, 0%, 100%, 0.94);
  border: 1px solid hsl(220, 15%, 86%);
  border-radius: var(--radius-md);
  box-shadow: 0 10px 28px hsla(220, 20%, 18%, 0.08);
  color: var(--text-primary);
  transition:
    border-color var(--transition-fast),
    box-shadow var(--transition-fast),
    opacity var(--transition-fast),
    transform var(--transition-fast);

  strong {
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-size: 0.95rem;
    line-height: 1.35;
  }

  p {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    color: var(--text-secondary);
    font-size: 0.82rem;
    line-height: 1.45;
    margin: 0;
  }

  &--current {
    border-color: var(--primary);
    box-shadow: 0 14px 34px hsla(280, 50%, 45%, 0.18);
  }

  &--visited {
    border-color: hsl(142, 50%, 72%);
  }

  &--next {
    border-color: hsl(38, 80%, 66%);
  }

  &--end {
    border-style: double;
  }

  &--muted {
    opacity: 0.58;
  }
}

.scenario-flow__node-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  span {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 42px;
    height: 22px;
    padding: 0 8px;
    border-radius: 999px;
    background: hsl(220, 15%, 94%);
    color: var(--text-secondary);
    font-size: 0.75rem;
    font-weight: 700;
  }

  small {
    color: var(--text-muted);
    font-size: 0.75rem;
  }

  .scenario-flow__node--current & span {
    background: var(--primary-light);
    color: var(--primary);
  }
}

.dialogue-section {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: 28px;
  box-shadow: var(--shadow-sm);
  margin-bottom: 24px;
  max-width: 700px;
  margin-left: auto;
  margin-right: auto;
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
  max-width: 700px;
  margin-left: auto;
  margin-right: auto;
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
  max-width: 700px;
  margin-left: auto;
  margin-right: auto;

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

  &.item--neutral {
    border-left: 3px solid var(--text-muted);
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

  .item--neutral & {
    color: var(--text-muted);
  }
}

.result-container {
  text-align: center;
  max-width: 700px;
  margin: 0 auto;
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

.scenario-review {
  margin-top: 32px;
  text-align: left;

  &__title {
    margin: 0 0 16px;
    color: var(--text-primary);
    font-size: 1.15rem;
    font-weight: 700;
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__item {
    display: grid;
    grid-template-columns: 82px 1fr auto;
    align-items: center;
    gap: 12px;
    padding: 14px 16px;
    background: var(--bg-card);
    border: 1px solid hsl(220, 15%, 90%);
    border-left: 4px solid var(--warning);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-sm);

    &--safe {
      border-left-color: var(--success);
    }

    &--risk {
      border-left-color: var(--danger);
    }

    &--neutral {
      border-left-color: var(--text-muted);
    }
  }

  &__step {
    color: var(--text-muted);
    font-size: 0.85rem;
  }

  &__choice {
    color: var(--text-primary);
    font-weight: 600;
    line-height: 1.5;
  }
}

@media (max-width: 768px) {
  .scenario-header {
    padding: 24px 16px;
  }

  .scenario-container {
    padding: 24px 16px;
  }

  .scenario-flow__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .scenario-flow__viewport {
    padding: 14px;
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

  .scenario-review__item {
    grid-template-columns: 1fr;
    align-items: start;
  }
}
</style>
