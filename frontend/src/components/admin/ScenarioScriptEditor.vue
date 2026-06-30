<template>
  <div class="scenario-script-editor">
    <div class="editor-header">
      <div class="header-left">
        <el-input v-model="model.name" placeholder="剧本名称" size="small" />
        <el-input v-model="model.description" placeholder="剧本描述" size="small" />
      </div>
      <div class="header-right">
        <input
          ref="jsonFileInput"
          class="json-file-input"
          type="file"
          accept="application/json,.json"
          @change="handleJsonFileChange"
        />
        <el-button size="small" @click="triggerJsonImport">导入JSON</el-button>
        <el-button size="small" @click="copyRawJson">复制JSON</el-button>
        <el-dropdown @command="handleTemplateSelect" trigger="click">
          <el-button size="small" type="primary" plain>
            使用模板
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="customer_service">冒充客服诈骗</el-dropdown-item>
              <el-dropdown-item command="public_security">冒充公检法诈骗</el-dropdown-item>
              <el-dropdown-item command="lottery">中奖诈骗</el-dropdown-item>
              <el-dropdown-item command="loan">贷款诈骗</el-dropdown-item>
              <el-dropdown-item command="empty" divided>空白模板</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="editor-tabs">
      <el-radio-group v-model="activeTab" size="small">
        <el-radio-button label="graph">流程图搭建</el-radio-button>
        <el-radio-button label="json">JSON源码</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="validationIssues.length" class="validation-panel">
      <div class="validation-panel__title">校验未通过</div>
      <div class="validation-panel__list">
        <span v-for="issue in validationIssues" :key="issue" class="validation-panel__item">
          {{ issue }}
        </span>
      </div>
    </div>

    <div v-show="activeTab === 'graph'" class="graph-editor">
      <div class="graph-toolbar">
        <div class="toolbar-group">
          <el-button size="small" type="primary" :icon="Plus" @click="addNode">添加节点</el-button>
          <el-button size="small" @click="applyAutoLayout">自动布局</el-button>
          <el-button size="small" :type="pendingLink ? 'warning' : 'default'" @click="cancelLinkDrag">
            {{ pendingLink ? '取消连线' : '拖动节点右侧圆点创建连接' }}
          </el-button>
        </div>
        <div class="toolbar-group toolbar-group--settings">
          <span class="toolbar-label">起点</span>
          <el-select v-model="model.startNodeId" size="small" placeholder="起始节点" style="width: 150px" clearable>
            <el-option v-for="n in model.nodes" :key="n.id" :label="nodeDisplayName(n)" :value="n.id" />
          </el-select>
          <span class="toolbar-label">终点</span>
          <el-select v-model="model.endNodeIds" multiple size="small" placeholder="结束节点" style="width: 220px">
            <el-option v-for="n in model.nodes" :key="n.id" :label="nodeDisplayName(n)" :value="n.id" />
          </el-select>
        </div>
      </div>

      <div class="graph-body">
        <aside class="node-library">
          <div class="node-library__header">
            <span class="inspector__eyebrow">Resources</span>
            <h4>节点资源</h4>
          </div>
          <div class="node-library__list">
            <button
              v-for="resource in nodeResources"
              :key="resource.type"
              class="node-resource"
              type="button"
              draggable="true"
              @click="addNodeFromResource(resource.type)"
              @dragstart="handleResourceDragStart($event, resource.type)"
            >
              <span class="node-resource__dot" :class="`node-resource__dot--${resource.type}`"></span>
              <span class="node-resource__body">
                <strong>{{ resource.title }}</strong>
                <small>{{ resource.description }}</small>
              </span>
            </button>
          </div>
        </aside>

        <div class="canvas-scroll">
          <div
            ref="canvasRef"
            class="flow-canvas"
            :style="{ width: `${canvasSize.width}px`, height: `${canvasSize.height}px` }"
            @pointerup="cancelLinkDrag"
            @dragover.prevent
            @drop="handleResourceDrop"
          >
            <svg class="flow-edges" :width="canvasSize.width" :height="canvasSize.height">
              <defs>
                <marker id="scenario-arrow" markerWidth="10" markerHeight="10" refX="8" refY="5" orient="auto">
                  <path d="M 0 0 L 10 5 L 0 10 z" fill="#111827" />
                </marker>
              </defs>
              <g v-for="edge in edgeViews" :key="edge.key" class="edge-layer">
                <path
                  class="edge-hit"
                  :d="edge.path"
                  @click.stop="selectEdge(edge.index)"
                />
                <path
                  class="edge-path"
                  :class="[
                    `is-score-${edgeScoreType(edge.edge)}`,
                    { 'is-selected': selectedEdgeIndex === edge.index }
                  ]"
                  :d="edge.path"
                  marker-end="url(#scenario-arrow)"
                  @click.stop="selectEdge(edge.index)"
                />
                <foreignObject
                  v-if="edge.edge.label"
                  :x="edge.labelX - 70"
                  :y="edge.labelY - 13"
                  width="140"
                  height="28"
                  @click.stop="selectEdge(edge.index)"
                >
                  <div class="edge-label" :class="`edge-label--${edgeScoreType(edge.edge)}`">
                    {{ edge.edge.label }}
                  </div>
                </foreignObject>
              </g>
              <path
                v-if="pendingLinkPath"
                class="edge-path edge-path--pending"
                :d="pendingLinkPath"
                marker-end="url(#scenario-arrow)"
              />
            </svg>

            <div
              v-for="node in model.nodes"
              :key="node.id"
              class="flow-node"
              :class="[
                `flow-node--${node.type || 'dialog'}`,
                {
                  'is-selected': selectedNodeId === node.id,
                  'is-start': model.startNodeId === node.id,
                  'is-end': model.endNodeIds.includes(node.id),
                  'is-unreachable': unreachableNodeIds.includes(node.id)
                }
              ]"
              :style="{ left: `${nodePosition(node).x}px`, top: `${nodePosition(node).y}px` }"
              @click.stop="selectNode(node.id)"
              @pointerdown.stop="startDragNode($event, node)"
              @pointerup.stop="finishLinkOnNode($event, node)"
            >
              <div class="flow-node__header">
                <span class="flow-node__type">{{ getTypeLabel(node.type) }}</span>
                <span class="flow-node__id">{{ node.id || '未命名' }}</span>
              </div>
              <div class="flow-node__title">{{ node.title || '未填写标题' }}</div>
              <div class="flow-node__content">{{ node.content || '点击右侧面板编辑节点内容' }}</div>
              <div
                class="flow-node__port"
                title="拖动到其他节点创建连接"
                @pointerdown.stop="startLinkDrag($event, node)"
              ></div>
            </div>

            <div v-if="!model.nodes.length" class="empty-canvas">
              <strong>从左侧节点资源开始搭建剧本流程</strong>
              <span>拖入节点资源或点击资源卡片创建节点，再拖动节点右侧圆点连接流程。</span>
            </div>
          </div>
        </div>

        <aside class="inspector">
          <template v-if="selectedNode">
            <div class="inspector__header">
              <div>
                <span class="inspector__eyebrow">Node</span>
                <h4>节点编辑</h4>
              </div>
              <el-button link type="danger" :icon="Delete" @click="removeSelectedNode" />
            </div>

            <el-form label-position="top" size="small">
              <el-form-item label="节点ID">
                <el-input
                  v-model="selectedNode.id"
                  :class="{ 'is-error': !isNodeIdValid(selectedNode.id) }"
                  @focus="nodeIdBeforeEdit = selectedNode.id"
                  @change="renameSelectedNode"
                />
              </el-form-item>
              <div class="inspector__grid">
                <el-form-item label="类型">
                  <el-select v-model="selectedNode.type" @change="handleSelectedNodeTypeChange">
                    <el-option label="开始" value="start" />
                    <el-option label="对话" value="dialog" />
                    <el-option label="决策" value="decision" />
                    <el-option label="结果" value="result" />
                    <el-option label="结束" value="end" />
                  </el-select>
                </el-form-item>
                <el-form-item label="角色">
                  <el-select v-model="selectedNode.role">
                    <el-option label="旁白" value="narrator" />
                    <el-option label="骗子" value="scammer" />
                    <el-option label="受害者" value="victim" />
                  </el-select>
                </el-form-item>
              </div>
              <el-form-item label="标题">
                <el-input v-model="selectedNode.title" placeholder="节点标题" />
              </el-form-item>
              <el-form-item label="剧本内容">
                <el-input
                  v-model="selectedNode.content"
                  type="textarea"
                  :rows="5"
                  placeholder="当前节点展示给用户的旁白或对话"
                />
              </el-form-item>
              <el-form-item label="风险提示">
                <el-input v-model="selectedNode.riskTip" placeholder="可选，当前节点下方提示" />
              </el-form-item>
              <div class="node-flags">
                <el-checkbox :model-value="model.startNodeId === selectedNode.id" @change="setSelectedAsStart">
                  设为起始节点
                </el-checkbox>
                <el-checkbox :model-value="model.endNodeIds.includes(selectedNode.id)" @change="toggleSelectedEndNode">
                  设为结局节点
                </el-checkbox>
              </div>
              <div class="position-fields">
                <el-input-number v-model="selectedNode.position.x" :min="0" :max="canvasSize.width - NODE_WIDTH" />
                <el-input-number v-model="selectedNode.position.y" :min="0" :max="canvasSize.height - NODE_HEIGHT" />
              </div>
            </el-form>
          </template>

          <template v-else-if="selectedEdge">
            <div class="inspector__header">
              <div>
                <span class="inspector__eyebrow">Edge</span>
                <h4>连接编辑</h4>
              </div>
              <el-button link type="danger" :icon="Delete" @click="removeSelectedEdge" />
            </div>
            <el-form label-position="top" size="small">
              <el-form-item label="起点">
                <el-select v-model="selectedEdge.from">
                  <el-option v-for="n in model.nodes" :key="n.id" :label="nodeDisplayName(n)" :value="n.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="终点">
                <el-select v-model="selectedEdge.to">
                  <el-option v-for="n in model.nodes" :key="n.id" :label="nodeDisplayName(n)" :value="n.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="用户选项文案">
                <el-input v-model="selectedEdge.label" placeholder="例如：挂断电话并核实" />
              </el-form-item>
              <el-form-item label="条件说明">
                <el-input v-model="selectedEdge.condition" placeholder="可选，用于备注触发条件" />
              </el-form-item>
              <el-form-item label="评分类型">
                <el-radio-group v-model="selectedEdge.scoreType" @change="handleSelectedEdgeScoreTypeChange">
                  <el-radio-button label="none">不计分</el-radio-button>
                  <el-radio-button label="safe">正确安全</el-radio-button>
                  <el-radio-button label="risk">错误风险</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-form>
          </template>

          <template v-else>
            <div class="inspector__header">
              <div>
                <span class="inspector__eyebrow">Script</span>
                <h4>流程连接</h4>
              </div>
            </div>
            <div class="edge-list">
              <button
                v-for="(edge, index) in model.edges"
                :key="`${edge.from}-${edge.to}-${index}`"
                class="edge-list__item"
                type="button"
                @click="selectEdge(index)"
              >
                <span>{{ edge.from || '?' }} → {{ edge.to || '?' }}</span>
                <small>{{ edge.label || '未填写选项文案' }} · {{ getScoreTypeLabel(edgeScoreType(edge)) }}</small>
              </button>
              <div v-if="!model.edges.length" class="edge-list__empty">暂无连接</div>
            </div>
          </template>
        </aside>
      </div>
    </div>

    <div v-show="activeTab === 'json'" class="json-panel">
      <div class="panel-header">
        <span class="panel-title">JSON 源码</span>
        <div class="header-actions">
          <el-button size="small" @click="syncRawJsonFromModel">从流程图同步</el-button>
          <el-button size="small" type="primary" @click="applyRawJson">应用到流程图</el-button>
        </div>
      </div>
      <el-input
        v-model="rawJsonText"
        type="textarea"
        :rows="18"
        placeholder="可直接粘贴或编辑 JSON"
        :class="{ 'is-error': jsonError }"
      />
      <div v-if="jsonError" class="json-error">{{ jsonError }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Delete, Plus } from '@element-plus/icons-vue'
import type {
  ScenarioEdge,
  ScenarioNode,
  ScenarioNodePosition,
  ScenarioScoreType,
  ScenarioScriptModel
} from '@/types/scenario-script'
import { emptyScenarioScript, parseScenarioScriptJson } from '@/types/scenario-script'

const model = defineModel<ScenarioScriptModel>({ required: true })

const NODE_WIDTH = 188
const NODE_HEIGHT = 112
const canvasSize = { width: 1180, height: 680 }
type EditableScenarioNode = ScenarioNode & { position: ScenarioNodePosition }
type ScenarioNodeType = 'start' | 'dialog' | 'decision' | 'result' | 'end'

interface NodeResource {
  type: ScenarioNodeType
  title: string
  description: string
  role: string
  defaultTitle: string
  defaultContent: string
}

const nodeResources: NodeResource[] = [
  {
    type: 'start',
    title: '开始节点',
    description: '剧本入口和背景铺垫',
    role: 'narrator',
    defaultTitle: '通话开始',
    defaultContent: '请填写情节模拟的开场背景。'
  },
  {
    type: 'dialog',
    title: '对白节点',
    description: '展示诈骗方或用户对白',
    role: 'scammer',
    defaultTitle: '剧情对白',
    defaultContent: '请填写当前剧情对白或旁白。'
  },
  {
    type: 'decision',
    title: '决策节点',
    description: '用户需要做出选择',
    role: 'scammer',
    defaultTitle: '关键决策',
    defaultContent: '请填写需要用户判断的情景。'
  },
  {
    type: 'result',
    title: '结果节点',
    description: '展示选择后的反馈',
    role: 'narrator',
    defaultTitle: '选择反馈',
    defaultContent: '请填写用户选择后的阶段性反馈。'
  },
  {
    type: 'end',
    title: '结局节点',
    description: '安全或风险结局',
    role: 'narrator',
    defaultTitle: '模拟结局',
    defaultContent: '请填写本条流程的最终结果。'
  }
]

const activeTab = ref<'graph' | 'json'>('graph')
const selectedNodeId = ref('')
const selectedEdgeIndex = ref<number | null>(null)
const nodeIdBeforeEdit = ref('')
const rawJsonText = ref('')
const jsonError = ref('')
const jsonFileInput = ref<HTMLInputElement>()
const canvasRef = ref<HTMLElement>()
const dragState = ref<{ nodeId: string; offsetX: number; offsetY: number } | null>(null)
const pendingLink = ref<{ from: string; x: number; y: number } | null>(null)

const nodeIds = computed(() => model.value.nodes.map(n => n.id).filter(Boolean))

const duplicateNodeIds = computed(() => {
  const seen = new Set<string>()
  const duplicates = new Set<string>()
  for (const id of nodeIds.value) {
    if (seen.has(id)) duplicates.add(id)
    seen.add(id)
  }
  return [...duplicates]
})

const selectedNode = computed<EditableScenarioNode | null>(() => {
  const node = model.value.nodes.find(item => item.id === selectedNodeId.value)
  if (!node) return null
  if (!node.position) {
    node.position = { x: 64, y: 64 }
  }
  return node as EditableScenarioNode
})

const selectedEdge = computed(() => {
  if (selectedEdgeIndex.value === null) return null
  return model.value.edges[selectedEdgeIndex.value] || null
})

const unreachableNodeIds = computed(() => {
  if (!model.value.startNodeId) return []
  const allNodeIds = new Set(nodeIds.value)
  if (!allNodeIds.has(model.value.startNodeId)) return nodeIds.value

  const adjacency = new Map<string, string[]>()
  model.value.edges.forEach(edge => {
    if (!edge.from || !edge.to || !allNodeIds.has(edge.from) || !allNodeIds.has(edge.to)) return
    const list = adjacency.get(edge.from) || []
    list.push(edge.to)
    adjacency.set(edge.from, list)
  })

  const reachable = new Set<string>([model.value.startNodeId])
  const queue = [model.value.startNodeId]
  while (queue.length) {
    const current = queue.shift()!
    for (const next of adjacency.get(current) || []) {
      if (!reachable.has(next)) {
        reachable.add(next)
        queue.push(next)
      }
    }
  }
  return nodeIds.value.filter(id => !reachable.has(id))
})

const validationIssues = computed(() => {
  const issues: string[] = []
  const allNodeIds = new Set(nodeIds.value)

  if (!model.value.nodes.length) {
    issues.push('至少需要1个节点')
    return issues
  }
  if (model.value.nodes.some(n => !n.id?.trim())) {
    issues.push('节点ID不能为空')
  }
  duplicateNodeIds.value.forEach(id => issues.push(`节点ID重复：${id}`))
  if (!model.value.startNodeId) {
    issues.push('必须设置起始节点')
  } else if (!allNodeIds.has(model.value.startNodeId)) {
    issues.push('起始节点不存在')
  }
  if (!model.value.endNodeIds.length) {
    issues.push('至少需要1个结局节点')
  }
  model.value.endNodeIds.forEach(id => {
    const node = model.value.nodes.find(n => n.id === id)
    if (!node) {
      issues.push(`结局节点不存在：${id}`)
    } else if (node.type !== 'end') {
      issues.push(`结局节点类型必须为结束：${id}`)
    }
  })

  const outgoing = new Set<string>()
  model.value.edges.forEach((edge, idx) => {
    if (!edge.from || !edge.to) {
      issues.push(`连接${idx + 1}缺少起点或终点`)
      return
    }
    if (!allNodeIds.has(edge.from)) issues.push(`连接起点不存在：${edge.from}`)
    if (!allNodeIds.has(edge.to)) issues.push(`连接终点不存在：${edge.to}`)
    if (!edge.label?.trim()) issues.push(`连接选项文案不能为空：${edge.from}->${edge.to}`)
    outgoing.add(edge.from)
  })

  model.value.nodes.forEach(node => {
    if (!model.value.endNodeIds.includes(node.id) && !outgoing.has(node.id)) {
      issues.push(`非结局节点必须有出边：${node.id}`)
    }
    if (node.type === 'decision') {
      const outgoingEdges = model.value.edges.filter(edge => edge.from === node.id)
      if (outgoingEdges.length > 0 && outgoingEdges.every(edge => edgeScoreType(edge) === 'none')) {
        issues.push(`决策节点至少需要一个计分选项：${node.id}`)
      }
    }
  })
  unreachableNodeIds.value.forEach(id => issues.push(`起始节点不可达：${id}`))

  return [...new Set(issues)]
})

const edgeViews = computed(() => {
  return model.value.edges
    .map((edge, index) => {
      const from = model.value.nodes.find(node => node.id === edge.from)
      const to = model.value.nodes.find(node => node.id === edge.to)
      if (!from || !to) return null
      const fromPos = nodePosition(from)
      const toPos = nodePosition(to)
      const x1 = fromPos.x + NODE_WIDTH
      const y1 = fromPos.y + NODE_HEIGHT / 2
      const x2 = toPos.x
      const y2 = toPos.y + NODE_HEIGHT / 2
      const dx = Math.max(80, Math.abs(x2 - x1) * 0.45)
      return {
        key: `${edge.from}-${edge.to}-${index}`,
        edge,
        index,
        path: `M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`,
        labelX: (x1 + x2) / 2,
        labelY: (y1 + y2) / 2
      }
    })
    .filter(Boolean) as Array<{
      key: string
      edge: ScenarioEdge
      index: number
      path: string
      labelX: number
      labelY: number
    }>
})

const pendingLinkPath = computed(() => {
  if (!pendingLink.value) return ''
  const from = model.value.nodes.find(node => node.id === pendingLink.value?.from)
  if (!from) return ''
  const pos = nodePosition(from)
  const x1 = pos.x + NODE_WIDTH
  const y1 = pos.y + NODE_HEIGHT / 2
  const x2 = pendingLink.value.x
  const y2 = pendingLink.value.y
  const dx = Math.max(80, Math.abs(x2 - x1) * 0.45)
  return `M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`
})

watch(model, () => {
  ensureModelShape()
  syncRawJsonFromModel()
}, { deep: true, immediate: true })

onBeforeUnmount(() => {
  removeWindowListeners()
})

function ensureModelShape() {
  if (!Array.isArray(model.value.nodes)) model.value.nodes = []
  if (!Array.isArray(model.value.edges)) model.value.edges = []
  if (!Array.isArray(model.value.endNodeIds)) model.value.endNodeIds = []
  model.value.nodes.forEach((node, index) => {
    if (!node.type) node.type = index === 0 ? 'start' : 'dialog'
    if (!node.role) node.role = 'narrator'
    if (!node.position) {
      node.position = {
        x: 60 + (index % 4) * 260,
        y: 80 + Math.floor(index / 4) * 170
      }
    }
  })
  model.value.edges.forEach(edge => {
    syncEdgeScoreType(edge)
  })
  if (selectedNodeId.value && !model.value.nodes.some(node => node.id === selectedNodeId.value)) {
    selectedNodeId.value = ''
  }
  if (!selectedNodeId.value && model.value.nodes.length && selectedEdgeIndex.value === null) {
    selectedNodeId.value = model.value.nodes[0].id
  }
  if (selectedEdgeIndex.value !== null && !model.value.edges[selectedEdgeIndex.value]) {
    selectedEdgeIndex.value = null
  }
}

function nodePosition(node: ScenarioNode) {
  if (!node.position) {
    node.position = { x: 60, y: 80 }
  }
  return node.position
}

function edgeScoreType(edge: ScenarioEdge): ScenarioScoreType {
  if (edge.scoreType === 'none' || edge.scoreType === 'safe' || edge.scoreType === 'risk') {
    return edge.scoreType
  }
  if (edge.isSafeChoice === true) return 'safe'
  if (edge.isSafeChoice === false) return 'risk'
  return 'none'
}

function syncEdgeScoreType(edge: ScenarioEdge) {
  const scoreType = edgeScoreType(edge)
  edge.scoreType = scoreType
  edge.isSafeChoice = scoreType === 'safe' ? true : scoreType === 'risk' ? false : undefined
}

function handleSelectedEdgeScoreTypeChange() {
  if (selectedEdge.value) {
    syncEdgeScoreType(selectedEdge.value)
  }
}

function canvasPointFromClient(clientX: number, clientY: number) {
  const rect = canvasRef.value?.getBoundingClientRect()
  if (!rect) return { x: 0, y: 0 }
  return {
    x: clientX - rect.left,
    y: clientY - rect.top
  }
}

function canvasPoint(event: PointerEvent | DragEvent) {
  return canvasPointFromClient(event.clientX, event.clientY)
}

function addWindowListeners() {
  window.addEventListener('pointermove', handleWindowPointerMove)
  window.addEventListener('pointerup', handleWindowPointerUp)
}

function removeWindowListeners() {
  window.removeEventListener('pointermove', handleWindowPointerMove)
  window.removeEventListener('pointerup', handleWindowPointerUp)
}

function handleWindowPointerMove(event: PointerEvent) {
  if ((dragState.value || pendingLink.value) && event.pointerType === 'mouse' && event.buttons === 0) {
    endDragInteraction()
    return
  }

  if (dragState.value) {
    const node = model.value.nodes.find(item => item.id === dragState.value?.nodeId)
    if (!node) return
    const point = canvasPoint(event)
    node.position = {
      x: clamp(point.x - dragState.value.offsetX, 12, canvasSize.width - NODE_WIDTH - 12),
      y: clamp(point.y - dragState.value.offsetY, 12, canvasSize.height - NODE_HEIGHT - 12)
    }
  }
  if (pendingLink.value) {
    const point = canvasPoint(event)
    pendingLink.value.x = clamp(point.x, 0, canvasSize.width)
    pendingLink.value.y = clamp(point.y, 0, canvasSize.height)
  }
}

function handleWindowPointerUp() {
  endDragInteraction()
}

function endDragInteraction() {
  dragState.value = null
  pendingLink.value = null
  removeWindowListeners()
}

function startDragNode(event: PointerEvent, node: ScenarioNode) {
  if (pendingLink.value) return
  selectedNodeId.value = node.id
  selectedEdgeIndex.value = null
  const point = canvasPoint(event)
  const pos = nodePosition(node)
  dragState.value = {
    nodeId: node.id,
    offsetX: point.x - pos.x,
    offsetY: point.y - pos.y
  }
  addWindowListeners()
}

function startLinkDrag(event: PointerEvent, node: ScenarioNode) {
  event.preventDefault()
  selectedNodeId.value = node.id
  selectedEdgeIndex.value = null
  const point = canvasPoint(event)
  pendingLink.value = {
    from: node.id,
    x: point.x,
    y: point.y
  }
  addWindowListeners()
}

function finishLinkOnNode(event: PointerEvent, node: ScenarioNode) {
  if (!pendingLink.value) {
    if (dragState.value) {
      event.preventDefault()
      endDragInteraction()
    }
    return
  }
  event.preventDefault()
  const from = pendingLink.value.from
  const to = node.id
  endDragInteraction()
  if (!from || !to || from === to) return
  createEdge(from, to)
}

function cancelLinkDrag() {
  if (!pendingLink.value) return
  pendingLink.value = null
  removeWindowListeners()
}

function selectNode(id: string) {
  selectedNodeId.value = id
  selectedEdgeIndex.value = null
}

function selectEdge(index: number) {
  selectedEdgeIndex.value = index
  selectedNodeId.value = ''
}

function addNode() {
  addNodeFromResource(model.value.nodes.length ? 'dialog' : 'start')
}

function addNodeFromResource(type: ScenarioNodeType, position?: ScenarioNodePosition) {
  const resource = nodeResources.find(item => item.type === type) || nodeResources[1]
  const index = model.value.nodes.length
  const nodeId = nextNodeId(resource.type)
  const nodePosition = position || defaultNodePosition(index)
  const node: ScenarioNode = {
    id: nodeId,
    type: resource.type,
    title: resource.defaultTitle,
    content: resource.defaultContent,
    role: resource.role,
    riskTip: resource.type === 'start' ? '请填写本关卡需要用户识别的风险提示。' : '',
    position: nodePosition
  }

  model.value.nodes.push(node)
  if (!model.value.startNodeId || resource.type === 'start') {
    model.value.startNodeId = nodeId
  }
  if (resource.type === 'end' && !model.value.endNodeIds.includes(nodeId)) {
    model.value.endNodeIds.push(nodeId)
  }
  selectNode(nodeId)
}

function defaultNodePosition(index: number): ScenarioNodePosition {
  return {
    x: 60 + (index % 3) * 280,
    y: 90 + Math.floor(index / 3) * 180
  }
}

function handleResourceDragStart(event: DragEvent, type: ScenarioNodeType) {
  event.dataTransfer?.setData('application/x-scenario-node-type', type)
  event.dataTransfer?.setData('text/plain', type)
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'copy'
  }
}

function handleResourceDrop(event: DragEvent) {
  event.preventDefault()
  const type = event.dataTransfer?.getData('application/x-scenario-node-type')
    || event.dataTransfer?.getData('text/plain')
  if (!isScenarioNodeType(type)) return

  const point = canvasPoint(event)
  addNodeFromResource(type, {
    x: clamp(point.x - NODE_WIDTH / 2, 12, canvasSize.width - NODE_WIDTH - 12),
    y: clamp(point.y - NODE_HEIGHT / 2, 12, canvasSize.height - NODE_HEIGHT - 12)
  })
}

function isScenarioNodeType(type: string | undefined): type is ScenarioNodeType {
  return type === 'start'
    || type === 'dialog'
    || type === 'decision'
    || type === 'result'
    || type === 'end'
}

function removeSelectedNode() {
  if (!selectedNode.value) return
  const nodeId = selectedNode.value.id
  model.value.nodes = model.value.nodes.filter(node => node.id !== nodeId)
  model.value.edges = model.value.edges.filter(edge => edge.from !== nodeId && edge.to !== nodeId)
  if (model.value.startNodeId === nodeId) model.value.startNodeId = ''
  model.value.endNodeIds = model.value.endNodeIds.filter(id => id !== nodeId)
  selectedNodeId.value = model.value.nodes[0]?.id || ''
}

function removeSelectedEdge() {
  if (selectedEdgeIndex.value === null) return
  model.value.edges.splice(selectedEdgeIndex.value, 1)
  selectedEdgeIndex.value = null
}

function createEdge(from: string, to: string) {
  const duplicate = model.value.edges.some(edge => edge.from === from && edge.to === to)
  if (duplicate) {
    ElMessage.warning('这两个节点之间已经存在连接')
    return
  }
  const fromNode = model.value.nodes.find(node => node.id === from)
  const scoreType: ScenarioScoreType = fromNode?.type === 'decision' ? 'risk' : 'none'
  model.value.edges.push({
    from,
    to,
    condition: '',
    label: fromNode?.type === 'decision' ? '选择此项' : '继续',
    scoreType,
    isSafeChoice: scoreType === 'risk' ? false : undefined
  })
  selectedEdgeIndex.value = model.value.edges.length - 1
  selectedNodeId.value = ''
}

function renameSelectedNode() {
  if (!selectedNode.value) return
  const oldId = nodeIdBeforeEdit.value
  const newId = selectedNode.value.id?.trim()
  selectedNode.value.id = newId
  if (!oldId || !newId || oldId === newId) return
  model.value.edges.forEach(edge => {
    if (edge.from === oldId) edge.from = newId
    if (edge.to === oldId) edge.to = newId
  })
  if (model.value.startNodeId === oldId) model.value.startNodeId = newId
  model.value.endNodeIds = model.value.endNodeIds.map(id => id === oldId ? newId : id)
  selectedNodeId.value = newId
}

function handleSelectedNodeTypeChange() {
  if (!selectedNode.value) return
  if (selectedNode.value.type === 'start') {
    model.value.startNodeId = selectedNode.value.id
  }
  if (selectedNode.value.type === 'end' && !model.value.endNodeIds.includes(selectedNode.value.id)) {
    model.value.endNodeIds.push(selectedNode.value.id)
  }
  if (selectedNode.value.type !== 'end') {
    model.value.endNodeIds = model.value.endNodeIds.filter(id => id !== selectedNode.value?.id)
  }
}

function setSelectedAsStart(value: boolean | string | number) {
  if (!selectedNode.value) return
  if (Boolean(value)) {
    model.value.startNodeId = selectedNode.value.id
    selectedNode.value.type = 'start'
  } else if (model.value.startNodeId === selectedNode.value.id) {
    model.value.startNodeId = ''
  }
}

function toggleSelectedEndNode(value: boolean | string | number) {
  if (!selectedNode.value) return
  const id = selectedNode.value.id
  if (Boolean(value)) {
    if (!model.value.endNodeIds.includes(id)) model.value.endNodeIds.push(id)
    selectedNode.value.type = 'end'
  } else {
    model.value.endNodeIds = model.value.endNodeIds.filter(item => item !== id)
  }
}

function applyAutoLayout() {
  if (!model.value.nodes.length) return
  const startId = model.value.startNodeId || model.value.nodes[0].id
  const adjacency = new Map<string, string[]>()
  model.value.edges.forEach(edge => {
    if (!adjacency.has(edge.from)) adjacency.set(edge.from, [])
    adjacency.get(edge.from)!.push(edge.to)
  })

  const depth = new Map<string, number>([[startId, 0]])
  const queue = [startId]
  while (queue.length) {
    const current = queue.shift()!
    const nextDepth = (depth.get(current) || 0) + 1
    for (const next of adjacency.get(current) || []) {
      if (!depth.has(next)) {
        depth.set(next, nextDepth)
        queue.push(next)
      }
    }
  }
  model.value.nodes.forEach((node, index) => {
    if (!depth.has(node.id)) depth.set(node.id, Math.max(1, Math.floor(index / 2) + 1))
  })

  const groups = new Map<number, ScenarioNode[]>()
  model.value.nodes.forEach(node => {
    const d = depth.get(node.id) || 0
    const group = groups.get(d) || []
    group.push(node)
    groups.set(d, group)
  })

  groups.forEach((nodes, d) => {
    const totalHeight = (nodes.length - 1) * 170
    const startY = Math.max(40, (canvasSize.height - totalHeight - NODE_HEIGHT) / 2)
    nodes.forEach((node, index) => {
      node.position = {
        x: 50 + d * 260,
        y: Math.round(startY + index * 170)
      }
    })
  })
}

function syncRawJsonFromModel() {
  try {
    rawJsonText.value = JSON.stringify(model.value, null, 2)
    jsonError.value = ''
  } catch (e: any) {
    rawJsonText.value = ''
    jsonError.value = e.message || 'JSON序列化失败'
  }
}

function applyRawJson() {
  const text = rawJsonText.value?.trim()
  if (!text) {
    Object.assign(model.value, emptyScenarioScript())
    selectedNodeId.value = ''
    selectedEdgeIndex.value = null
    jsonError.value = ''
    return
  }
  try {
    JSON.parse(text)
    const parsed = parseScenarioScriptJson(text)
    Object.assign(model.value, parsed)
    ensureModelShape()
    if (model.value.nodes.some(node => !node.position)) applyAutoLayout()
    jsonError.value = ''
    activeTab.value = 'graph'
    ElMessage.success('已从 JSON 同步到流程图')
  } catch (e: any) {
    jsonError.value = `JSON 格式无效: ${e.message}`
    ElMessage.error('JSON 格式无效，未应用')
  }
}

function triggerJsonImport() {
  jsonFileInput.value?.click()
}

function handleJsonFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    rawJsonText.value = String(reader.result || '')
    applyRawJson()
    input.value = ''
  }
  reader.onerror = () => {
    ElMessage.error('JSON 文件读取失败')
    input.value = ''
  }
  reader.readAsText(file)
}

async function copyRawJson() {
  syncRawJsonFromModel()
  try {
    await navigator.clipboard.writeText(rawJsonText.value)
    ElMessage.success('JSON 已复制')
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = rawJsonText.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('JSON 已复制')
  }
}

function isNodeIdValid(id: string) {
  if (!id || !id.trim()) return false
  return model.value.nodes.filter(node => node.id === id).length === 1
}

function nodeDisplayName(node: ScenarioNode) {
  return `${node.id || '未命名'} (${getTypeLabel(node.type)})`
}

function getTypeLabel(type?: string) {
  const map: Record<string, string> = {
    start: '开始',
    dialog: '对话',
    decision: '决策',
    result: '结果',
    end: '结束'
  }
  return map[type || ''] || type || '节点'
}

function getScoreTypeLabel(scoreType: ScenarioScoreType) {
  const map: Record<ScenarioScoreType, string> = {
    none: '不计分',
    safe: '正确安全',
    risk: '错误风险'
  }
  return map[scoreType]
}

function nextNodeId(prefix = 'node') {
  let index = model.value.nodes.length + 1
  let id = `${prefix}_${index}`
  const existing = new Set(nodeIds.value)
  while (existing.has(id)) {
    index += 1
    id = `${prefix}_${index}`
  }
  return id
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, Math.round(value)))
}

const templates: Record<string, ScenarioScriptModel> = {
  customer_service: {
    name: '冒充客服诈骗',
    description: '模拟冒充电商客服诱导转账的诈骗场景',
    nodes: [
      node('start', 'start', '通话开始', '你接到一个自称"淘宝客服"的电话，对方称你的订单出现问题需要处理。', 'narrator', 60, 260, '正规客服不会通过电话要求你转账或提供验证码'),
      node('verify', 'decision', '身份核实', '对方说："您好，我是淘宝客服，您的订单因系统故障需要退款，请按我说的操作。"', 'scammer', 330, 260),
      node('safe_path', 'dialog', '安全应对', '你挂断电话，打开淘宝App查看订单，发现订单正常，确认是诈骗电话。', 'victim', 610, 110),
      node('risk_path', 'dialog', '被诱导', '你按照对方的指引，添加了对方提供的QQ号，并准备进行"退款操作"。', 'victim', 610, 400, '诈骗分子会引导你脱离正规平台进行操作'),
      node('end_safe', 'end', '成功识破', '你识破了诈骗，避免了财产损失，并向平台举报了该号码。', 'narrator', 890, 110),
      node('end_risk', 'end', '遭受损失', '你按照对方要求转账"验证账户"，结果被骗走钱财。', 'narrator', 890, 400)
    ],
    edges: [
      edge('start', 'verify', '继续通话', 'none'),
      edge('verify', 'safe_path', '挂断电话，自行核实', 'safe'),
      edge('verify', 'risk_path', '按对方指引操作', 'risk'),
      edge('safe_path', 'end_safe', '完成核实', 'none'),
      edge('risk_path', 'end_risk', '继续', 'none')
    ],
    startNodeId: 'start',
    endNodeIds: ['end_safe', 'end_risk']
  },
  public_security: {
    name: '冒充公检法诈骗',
    description: '模拟冒充公安机关要求转账的诈骗场景',
    nodes: [
      node('start', 'start', '接到电话', '你接到一个自称"公安局"的电话，对方称你涉嫌洗钱案件。', 'narrator', 60, 260, '公安机关不会通过电话办案，更不会要求转账'),
      node('threat', 'decision', '威胁恐吓', '对方说："你的银行账户涉嫌洗钱，需要立即冻结配合调查，否则将承担法律责任！"', 'scammer', 330, 260),
      node('safe_path', 'dialog', '冷静应对', '你挂断电话，拨打110核实情况，确认这是诈骗电话。', 'victim', 610, 110),
      node('risk_path', 'dialog', '恐慌操作', '你在对方的恐吓下，按照要求提供了银行卡信息并准备转账"配合调查"。', 'victim', 610, 400, '诈骗分子利用恐慌心理让你失去判断力'),
      node('end_safe', 'end', '成功识破', '你识破了诈骗，保护了自己的财产安全。', 'narrator', 890, 110),
      node('end_risk', 'end', '遭受损失', '你将钱转入了"安全账户"，结果被骗走全部积蓄。', 'narrator', 890, 400)
    ],
    edges: [
      edge('start', 'threat', '继续通话', 'none'),
      edge('threat', 'safe_path', '挂断并拨打110核实', 'safe'),
      edge('threat', 'risk_path', '害怕并配合对方', 'risk'),
      edge('safe_path', 'end_safe', '完成核实', 'none'),
      edge('risk_path', 'end_risk', '继续', 'none')
    ],
    startNodeId: 'start',
    endNodeIds: ['end_safe', 'end_risk']
  },
  lottery: {
    name: '中奖诈骗',
    description: '模拟虚假中奖诱导转账的诈骗场景',
    nodes: [
      node('start', 'start', '收到短信', '你收到一条短信：恭喜您被抽中为幸运用户，获得iPhone 15一部！', 'narrator', 60, 260, '天上不会掉馅饼，正规中奖不需要先交费'),
      node('contact', 'decision', '联系对方', '你联系了短信中的客服，对方说需要先缴纳"个人所得税"才能领奖。', 'scammer', 330, 260),
      node('safe_path', 'dialog', '识破骗局', '你意识到这是诈骗，没有继续操作，并删除了短信。', 'victim', 610, 110),
      node('risk_path', 'dialog', '信以为真', '你以为自己真的中奖了，准备按照对方要求转账"税费"。', 'victim', 610, 400),
      node('end_safe', 'end', '成功识破', '你没有上当受骗，避免了财产损失。', 'narrator', 890, 110),
      node('end_risk', 'end', '遭受损失', '你转账后对方消失，才发现自己被骗了。', 'narrator', 890, 400)
    ],
    edges: [
      edge('start', 'contact', '联系客服', 'none'),
      edge('contact', 'safe_path', '意识到是诈骗', 'safe'),
      edge('contact', 'risk_path', '相信中奖信息', 'risk'),
      edge('safe_path', 'end_safe', '删除短信', 'none'),
      edge('risk_path', 'end_risk', '继续', 'none')
    ],
    startNodeId: 'start',
    endNodeIds: ['end_safe', 'end_risk']
  },
  loan: {
    name: '贷款诈骗',
    description: '模拟虚假贷款诱导转账的诈骗场景',
    nodes: [
      node('start', 'start', '浏览贷款信息', '你在网上看到一则"无抵押、低利息、快速放款"的贷款广告。', 'narrator', 60, 260, '正规贷款机构不会要求先交费'),
      node('apply', 'decision', '申请贷款', '你联系了对方，对方说需要先缴纳"保证金"才能放款。', 'scammer', 330, 260),
      node('safe_path', 'dialog', '识破骗局', '你意识到正规贷款不需要先交钱，拒绝了对方的要求。', 'victim', 610, 110),
      node('risk_path', 'dialog', '急于用钱', '你因为急需用钱，按照对方要求转账了"保证金"。', 'victim', 610, 400),
      node('end_safe', 'end', '成功识破', '你没有上当，选择去正规银行办理贷款。', 'narrator', 890, 110),
      node('end_risk', 'end', '遭受损失', '转账后对方消失，你既没拿到贷款，还损失了保证金。', 'narrator', 890, 400)
    ],
    edges: [
      edge('start', 'apply', '申请贷款', 'none'),
      edge('apply', 'safe_path', '拒绝先交费', 'safe'),
      edge('apply', 'risk_path', '相信对方并转账', 'risk'),
      edge('safe_path', 'end_safe', '选择正规渠道', 'none'),
      edge('risk_path', 'end_risk', '继续等待放款', 'none')
    ],
    startNodeId: 'start',
    endNodeIds: ['end_safe', 'end_risk']
  },
  empty: emptyScenarioScript()
}

function node(
  id: string,
  type: string,
  title: string,
  content: string,
  role: string,
  x: number,
  y: number,
  riskTip = ''
): ScenarioNode {
  return { id, type, title, content, role, riskTip, position: { x, y } }
}

function edge(from: string, to: string, label: string, scoreType: ScenarioScoreType): ScenarioEdge {
  return {
    from,
    to,
    label,
    condition: '',
    scoreType,
    isSafeChoice: scoreType === 'safe' ? true : scoreType === 'risk' ? false : undefined
  }
}

async function handleTemplateSelect(command: string) {
  const template = templates[command]
  if (!template) return

  if (model.value.nodes.length > 0 || model.value.edges.length > 0) {
    try {
      await ElMessageBox.confirm('使用模板将覆盖当前内容，是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
  }

  Object.assign(model.value, JSON.parse(JSON.stringify(template)))
  ensureModelShape()
  selectedNodeId.value = model.value.nodes[0]?.id || ''
  selectedEdgeIndex.value = null
  activeTab.value = 'graph'
  ElMessage.success('模板已应用')
}
</script>

<style scoped lang="scss">
.scenario-script-editor {
  width: 100%;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #f8fafc;

  .header-left {
    display: grid;
    grid-template-columns: 220px minmax(0, 1fr);
    gap: 12px;
    flex: 1;
    min-width: 0;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }
}

.json-file-input {
  display: none;
}

.editor-tabs {
  padding: 10px 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.validation-panel {
  padding: 10px 16px;
  border-bottom: 1px solid #fed7aa;
  background: #fff7ed;

  &__title {
    margin-bottom: 8px;
    color: #9a3412;
    font-size: 13px;
    font-weight: 700;
  }

  &__list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  &__item {
    padding: 3px 8px;
    border: 1px solid #fed7aa;
    border-radius: 999px;
    background: #ffedd5;
    color: #9a3412;
    font-size: 12px;
    line-height: 1.4;
  }
}

.graph-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #fbfdff;

  .toolbar-group {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .toolbar-group--settings {
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .toolbar-label {
    color: #64748b;
    font-size: 12px;
    font-weight: 700;
  }
}

.graph-body {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 300px;
  min-height: 640px;
}

.node-library {
  overflow-y: auto;
  padding: 16px;
  border-right: 1px solid #e5e7eb;
  background: #fff;

  &__header {
    margin-bottom: 14px;

    h4 {
      margin: 3px 0 0;
      color: #111827;
      font-size: 16px;
    }
  }

  &__list {
    display: grid;
    gap: 10px;
  }
}

.node-resource {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.96);
  color: #111827;
  text-align: left;
  cursor: grab;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;

  &:hover {
    border-color: #111827;
    box-shadow: 0 10px 26px rgba(15, 23, 42, 0.1);
    transform: translateY(-1px);
  }

  &:active {
    cursor: grabbing;
  }

  &__dot {
    width: 12px;
    height: 12px;
    margin-top: 3px;
    border-radius: 50%;
    background: #111827;
    box-shadow: 0 0 0 4px rgba(17, 24, 39, 0.08);

    &--start { background: #16a34a; }
    &--dialog { background: #111827; }
    &--decision { background: #d97706; }
    &--result { background: #2563eb; }
    &--end { background: #dc2626; }
  }

  &__body {
    display: grid;
    gap: 4px;
    min-width: 0;

    strong {
      color: #111827;
      font-size: 13px;
      line-height: 1.35;
    }

    small {
      color: #64748b;
      font-size: 12px;
      line-height: 1.45;
    }
  }
}

.canvas-scroll {
  overflow: auto;
  background:
    linear-gradient(rgba(17, 24, 39, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(17, 24, 39, 0.05) 1px, transparent 1px),
    #f8fafc;
  background-size: 24px 24px;
}

.flow-canvas {
  position: relative;
  overflow: hidden;
}

.flow-edges {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

.edge-layer {
  pointer-events: auto;
}

.edge-hit {
  fill: none;
  stroke: transparent;
  stroke-width: 18;
  cursor: pointer;
}

.edge-path {
  fill: none;
  stroke: #111827;
  stroke-width: 2;
  opacity: 0.74;

  &.is-selected {
    stroke-width: 3;
    opacity: 1;
  }

  &.is-score-safe {
    stroke: #15803d;
  }

  &.is-score-risk {
    stroke: #dc2626;
  }

  &.is-score-none {
    stroke: #94a3b8;
    stroke-dasharray: 8 6;
  }

  &--pending {
    stroke-dasharray: 8 6;
    pointer-events: none;
  }
}

.edge-label {
  max-width: 140px;
  overflow: hidden;
  padding: 4px 8px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  color: #111827;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;

  &--safe {
    color: #15803d;
  }

  &--risk {
    color: #dc2626;
  }

  &--none {
    color: #64748b;
  }
}

.flow-node {
  position: absolute;
  z-index: 2;
  width: 188px;
  height: 112px;
  padding: 12px;
  border: 2px solid #cbd5e1;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.12);
  cursor: grab;
  user-select: none;

  &:active {
    cursor: grabbing;
  }

  &.is-selected {
    border-color: #111827;
    box-shadow: 0 16px 34px rgba(15, 23, 42, 0.18);
  }

  &.is-start {
    box-shadow:
      0 16px 34px rgba(21, 128, 61, 0.12),
      inset 4px 0 0 #16a34a;
  }

  &.is-end {
    box-shadow:
      0 16px 34px rgba(220, 38, 38, 0.12),
      inset 4px 0 0 #dc2626;
  }

  &.is-unreachable {
    border-style: dashed;
    opacity: 0.68;
  }

  &--start { border-color: #86efac; }
  &--decision { border-color: #fbbf24; }
  &--end { border-color: #fca5a5; }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 8px;
  }

  &__type {
    padding: 2px 8px;
    border-radius: 999px;
    background: #111827;
    color: #fff;
    font-size: 11px;
    line-height: 1.4;
  }

  &__id {
    min-width: 0;
    overflow: hidden;
    color: #64748b;
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__title {
    overflow: hidden;
    margin-bottom: 6px;
    color: #111827;
    font-size: 14px;
    font-weight: 800;
    line-height: 1.35;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__content {
    display: -webkit-box;
    overflow: hidden;
    color: #64748b;
    font-size: 12px;
    line-height: 1.5;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }

  &__port {
    position: absolute;
    top: 50%;
    right: -9px;
    width: 18px;
    height: 18px;
    border: 3px solid #fff;
    border-radius: 50%;
    background: #111827;
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.24);
    cursor: crosshair;
    transform: translateY(-50%);
  }
}

.empty-canvas {
  position: absolute;
  top: 50%;
  left: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #64748b;
  text-align: center;
  transform: translate(-50%, -50%);

  strong {
    color: #111827;
    font-size: 16px;
  }
}

.inspector {
  overflow-y: auto;
  padding: 16px;
  border-left: 1px solid #e5e7eb;
  background: #fff;

  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 16px;

    h4 {
      margin: 3px 0 0;
      color: #111827;
      font-size: 16px;
    }
  }

  &__eyebrow {
    color: #64748b;
    font-size: 11px;
    font-weight: 800;
    text-transform: uppercase;
  }

  &__grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }
}

.node-flags {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 6px 0 14px;
}

.position-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.edge-list {
  display: grid;
  gap: 8px;

  &__item {
    display: grid;
    gap: 4px;
    width: 100%;
    padding: 10px 12px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #fff;
    color: #111827;
    text-align: left;
    cursor: pointer;

    &:hover {
      border-color: #111827;
    }

    span {
      font-weight: 700;
    }

    small {
      overflow: hidden;
      color: #64748b;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__empty {
    padding: 18px;
    border: 1px dashed #cbd5e1;
    border-radius: 8px;
    color: #64748b;
    text-align: center;
  }
}

.json-panel {
  padding: 16px;

  .json-error {
    margin-top: 8px;
    padding: 8px 12px;
    border-radius: 6px;
    background: #fee2e2;
    color: #b91c1c;
    font-size: 13px;
  }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;

  .panel-title {
    color: #111827;
    font-size: 14px;
    font-weight: 700;
  }

  .header-actions {
    display: flex;
    gap: 8px;
  }
}

:deep(.is-error .el-input__wrapper),
:deep(.el-textarea.is-error .el-textarea__inner) {
  box-shadow: 0 0 0 1px #ef4444 inset;
}

@media (max-width: 980px) {
  .editor-header {
    align-items: stretch;
    flex-direction: column;

    .header-left {
      grid-template-columns: 1fr;
    }

    .header-right {
      flex-wrap: wrap;
    }
  }

  .graph-toolbar {
    align-items: stretch;
    flex-direction: column;

    .toolbar-group--settings {
      justify-content: flex-start;
    }
  }

  .graph-body {
    grid-template-columns: 1fr;
  }

  .node-library {
    border-right: none;
    border-bottom: 1px solid #e5e7eb;

    &__list {
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    }
  }

  .inspector {
    border-top: 1px solid #e5e7eb;
    border-left: none;
  }
}
</style>
