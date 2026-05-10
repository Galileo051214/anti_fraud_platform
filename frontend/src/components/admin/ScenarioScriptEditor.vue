<template>
  <div class="scenario-script-editor">
    <div class="editor-header">
      <div class="header-left">
        <el-input v-model="model.name" placeholder="剧本名称" size="small" style="width: 200px" />
        <el-input v-model="model.description" placeholder="剧本描述" size="small" style="flex: 1; margin-left: 12px" />
      </div>
      <div class="header-right">
        <el-dropdown @command="handleTemplateSelect" trigger="click">
          <el-button size="small" type="primary" plain>
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 4px">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
              <line x1="3" y1="9" x2="21" y2="9"/>
              <line x1="9" y1="21" x2="9" y2="9"/>
            </svg>
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
        <el-radio-button label="nodes">节点编辑</el-radio-button>
        <el-radio-button label="edges">连接配置</el-radio-button>
        <el-radio-button label="preview">可视化预览</el-radio-button>
        <el-radio-button label="json">JSON源码</el-radio-button>
      </el-radio-group>
    </div>

    <div class="editor-content">
      <div v-show="activeTab === 'nodes'" class="nodes-panel">
        <div class="panel-header">
          <span class="panel-title">节点列表</span>
          <el-button size="small" type="primary" plain @click="addNode">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            添加节点
          </el-button>
        </div>
        <div class="nodes-list">
          <div v-for="(node, nIdx) in model.nodes" :key="nIdx" class="node-card">
            <div class="node-card__header">
              <el-input 
                v-model="node.id" 
                placeholder="节点ID" 
                size="small" 
                style="width: 140px"
                :class="{ 'is-error': !isNodeIdValid(node.id) }"
              >
                <template #prepend>ID</template>
              </el-input>
              <el-select v-model="node.type" size="small" style="width: 100px">
                <el-option label="开始" value="start">
                  <span class="type-option"><span class="type-dot type-dot--start"></span>开始</span>
                </el-option>
                <el-option label="对话" value="dialog">
                  <span class="type-option"><span class="type-dot type-dot--dialog"></span>对话</span>
                </el-option>
                <el-option label="决策" value="decision">
                  <span class="type-option"><span class="type-dot type-dot--decision"></span>决策</span>
                </el-option>
                <el-option label="结束" value="end">
                  <span class="type-option"><span class="type-dot type-dot--end"></span>结束</span>
                </el-option>
              </el-select>
              <el-select v-model="node.role" size="small" style="width: 100px">
                <el-option label="旁白" value="narrator" />
                <el-option label="骗子" value="scammer" />
                <el-option label="受害者" value="victim" />
              </el-select>
              <el-button link type="danger" @click="removeNode(nIdx)" :icon="Delete" />
            </div>
            <div class="node-card__body">
              <el-input v-model="node.title" placeholder="节点标题（可选）" size="small" style="margin-bottom: 8px" />
              <el-input 
                v-model="node.content" 
                type="textarea" 
                :rows="3" 
                placeholder="节点内容（对话文本或旁白描述）" 
                size="small" 
              />
              <el-input v-model="node.riskTip" placeholder="风险提示（可选，显示在对话下方）" size="small" style="margin-top: 8px">
                <template #prepend>
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor" style="color: #e6a23c">
                    <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/>
                  </svg>
                </template>
              </el-input>
            </div>
            <div class="node-card__footer">
              <el-tag size="small" :type="getNodeTypeTag(node.type)">{{ getTypeLabel(node.type) }}</el-tag>
              <el-tag size="small" :type="getRoleTag(node.role)">{{ getRoleLabel(node.role) }}</el-tag>
            </div>
          </div>
          <div v-if="model.nodes.length === 0" class="empty-hint">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
              <line x1="9" y1="9" x2="15" y2="9"/>
              <line x1="9" y1="12" x2="15" y2="12"/>
              <line x1="9" y1="15" x2="12" y2="15"/>
            </svg>
            <p>暂无节点，点击上方按钮添加</p>
            <p class="hint-text">或使用模板快速创建</p>
          </div>
        </div>
      </div>

      <div v-show="activeTab === 'edges'" class="edges-panel">
        <div class="panel-header">
          <span class="panel-title">连接配置</span>
          <el-button size="small" type="primary" plain @click="addEdge" :disabled="model.nodes.length < 2">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"/>
              <line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
            添加连接
          </el-button>
        </div>
        <div class="edges-list">
          <div v-for="(edge, eIdx) in model.edges" :key="eIdx" class="edge-card">
            <div class="edge-card__main">
              <el-select v-model="edge.from" size="small" style="width: 130px" placeholder="起始节点">
                <el-option v-for="n in model.nodes" :key="n.id" :label="n.id" :value="n.id">
                  <span>{{ n.id }}</span>
                  <el-tag size="small" type="info" style="margin-left: 8px">{{ getTypeLabel(n.type) }}</el-tag>
                </el-option>
              </el-select>
              <div class="edge-arrow">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="5" y1="12" x2="19" y2="12"/>
                  <polyline points="12 5 19 12 12 19"/>
                </svg>
              </div>
              <el-select v-model="edge.to" size="small" style="width: 130px" placeholder="目标节点">
                <el-option v-for="n in model.nodes" :key="n.id" :label="n.id" :value="n.id">
                  <span>{{ n.id }}</span>
                  <el-tag size="small" type="info" style="margin-left: 8px">{{ getTypeLabel(n.type) }}</el-tag>
                </el-option>
              </el-select>
            </div>
            <div class="edge-card__options">
              <el-input v-model="edge.label" placeholder="选项标签（用户看到的选项文字）" size="small" style="flex: 1" />
              <el-checkbox v-model="edge.isSafeChoice" size="small">
                <span class="safe-label">安全选择</span>
              </el-checkbox>
              <el-button link type="danger" @click="removeEdge(eIdx)" :icon="Delete" />
            </div>
          </div>
          <div v-if="model.edges.length === 0" class="empty-hint">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
              <line x1="5" y1="12" x2="19" y2="12"/>
              <polyline points="12 5 19 12 12 19"/>
            </svg>
            <p>暂无连接，点击上方按钮添加</p>
            <p class="hint-text">连接定义了用户从一个节点到另一个节点的选择路径</p>
          </div>
        </div>
      </div>

      <div v-show="activeTab === 'preview'" class="preview-panel">
        <div class="preview-settings">
          <div class="setting-item">
            <span class="setting-label">起始节点</span>
            <el-select v-model="model.startNodeId" size="small" style="width: 200px" placeholder="选择起始节点" clearable>
              <el-option v-for="n in model.nodes" :key="n.id" :label="`${n.id} (${getTypeLabel(n.type)})`" :value="n.id" />
            </el-select>
          </div>
          <div class="setting-item">
            <span class="setting-label">结局节点</span>
            <el-select v-model="model.endNodeIds" multiple size="small" style="width: 300px" placeholder="选择结局节点">
              <el-option v-for="n in endNodes" :key="n.id" :label="n.id" :value="n.id" />
            </el-select>
          </div>
        </div>
        <div class="preview-graph">
          <div class="graph-container">
            <div class="graph-nodes">
              <div 
                v-for="node in model.nodes" 
                :key="node.id" 
                class="graph-node"
                :class="{
                  'is-start': node.id === model.startNodeId,
                  'is-end': model.endNodeIds.includes(node.id),
                  'node-type--start': node.type === 'start',
                  'node-type--dialog': node.type === 'dialog',
                  'node-type--decision': node.type === 'decision',
                  'node-type--end': node.type === 'end'
                }"
              >
                <div class="graph-node__id">{{ node.id }}</div>
                <div class="graph-node__type">{{ getTypeLabel(node.type) }}</div>
                <div class="graph-node__role">{{ getRoleLabel(node.role) }}</div>
              </div>
            </div>
            <div class="graph-edges">
              <div v-for="(edge, idx) in model.edges" :key="idx" class="graph-edge">
                <span class="edge-from">{{ edge.from }}</span>
                <span class="edge-arrow">→</span>
                <span class="edge-to">{{ edge.to }}</span>
                <span class="edge-label" v-if="edge.label">{{ edge.label }}</span>
                <el-tag size="small" :type="edge.isSafeChoice ? 'success' : 'danger'" v-if="edge.isSafeChoice !== undefined">
                  {{ edge.isSafeChoice ? '安全' : '风险' }}
                </el-tag>
              </div>
            </div>
          </div>
          <div v-if="model.nodes.length === 0" class="empty-preview">
            <p>添加节点后可在此预览剧本结构</p>
          </div>
        </div>
      </div>

      <div v-show="activeTab === 'json'" class="json-panel">
        <div class="panel-header">
          <span class="panel-title">JSON 源码</span>
          <div class="header-actions">
            <el-button size="small" @click="syncRawJsonFromModel">从可视化同步</el-button>
            <el-button size="small" type="primary" @click="applyRawJson">应用到表单</el-button>
          </div>
        </div>
        <el-input
          v-model="rawJsonText"
          type="textarea"
          :rows="12"
          placeholder="可直接粘贴或编辑 JSON"
          :class="{ 'is-error': jsonError }"
        />
        <div v-if="jsonError" class="json-error">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
          </svg>
          {{ jsonError }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import type { ScenarioScriptModel, ScenarioNode, ScenarioEdge } from '@/types/scenario-script'
import { emptyScenarioScript, parseScenarioScriptJson } from '@/types/scenario-script'

const model = defineModel<ScenarioScriptModel>({ required: true })

const activeTab = ref<'nodes' | 'edges' | 'preview' | 'json'>('nodes')
const rawJsonText = ref('')
const jsonError = ref('')

const endNodes = computed(() => model.value.nodes.filter(n => n.type === 'end'))

const isNodeIdValid = (id: string) => {
  if (!id || !id.trim()) return false
  const count = model.value.nodes.filter(n => n.id === id).length
  return count === 1
}

const getTypeLabel = (t: string) => {
  const m: Record<string, string> = {
    start: '开始',
    dialog: '对话',
    decision: '决策',
    result: '结果',
    end: '结束'
  }
  return m[t] || t
}

const getRoleLabel = (r: string) => {
  const m: Record<string, string> = {
    narrator: '旁白',
    scammer: '骗子',
    victim: '受害者'
  }
  return m[r] || r
}

const getNodeTypeTag = (t: string) => {
  const m: Record<string, string> = {
    start: 'success',
    dialog: 'primary',
    decision: 'warning',
    end: 'danger'
  }
  return m[t] || 'info'
}

const getRoleTag = (r: string) => {
  const m: Record<string, string> = {
    narrator: 'info',
    scammer: 'danger',
    victim: 'primary'
  }
  return m[r] || 'info'
}

watch(model, () => syncRawJsonFromModel(), { deep: true, immediate: true })

function syncRawJsonFromModel() {
  try {
    rawJsonText.value = JSON.stringify(model.value, null, 2)
    jsonError.value = ''
  } catch (e: any) {
    rawJsonText.value = ''
    jsonError.value = e.message
  }
}

function applyRawJson() {
  const text = rawJsonText.value?.trim()
  if (!text) {
    Object.assign(model.value, emptyScenarioScript())
    jsonError.value = ''
    return
  }
  try {
    JSON.parse(text)
    const parsed = parseScenarioScriptJson(text)
    Object.assign(model.value, parsed)
    jsonError.value = ''
    ElMessage.success('已从 JSON 同步到表单')
  } catch (e: any) {
    jsonError.value = 'JSON 格式无效: ' + e.message
    ElMessage.error('JSON 格式无效，未应用')
  }
}

const addNode = () => {
  const nodeCount = model.value.nodes.length + 1
  const nodeId = `node_${nodeCount}`
  model.value.nodes.push({
    id: nodeId,
    type: model.value.nodes.length === 0 ? 'start' : 'dialog',
    title: '',
    content: '',
    role: 'narrator',
    riskTip: ''
  })
}

const removeNode = (idx: number) => {
  const nodeId = model.value.nodes[idx].id
  model.value.nodes.splice(idx, 1)
  model.value.edges = model.value.edges.filter((e) => e.from !== nodeId && e.to !== nodeId)
  if (model.value.startNodeId === nodeId) model.value.startNodeId = ''
  model.value.endNodeIds = model.value.endNodeIds.filter((id) => id !== nodeId)
}

const addEdge = () => {
  const nodes = model.value.nodes
  if (nodes.length < 2) {
    ElMessage.warning('至少需要 2 个节点才能添加连接')
    return
  }
  model.value.edges.push({
    from: nodes[0].id,
    to: nodes[1].id,
    condition: '',
    label: '',
    isSafeChoice: false
  })
}

const removeEdge = (idx: number) => {
  model.value.edges.splice(idx, 1)
}

const templates: Record<string, Partial<ScenarioScriptModel>> = {
  customer_service: {
    name: '冒充客服诈骗',
    description: '模拟冒充电商客服诱导转账的诈骗场景',
    nodes: [
      { id: 'start', type: 'start', title: '通话开始', content: '你接到一个自称"淘宝客服"的电话，对方称你的订单出现问题需要处理。', role: 'narrator', riskTip: '正规客服不会通过电话要求你转账或提供验证码' },
      { id: 'verify', type: 'dialog', title: '身份核实', content: '对方说："您好，我是淘宝客服，您的订单因系统故障需要退款，请按我说的操作。"', role: 'scammer' },
      { id: 'safe_path', type: 'dialog', title: '安全应对', content: '你挂断电话，打开淘宝App查看订单，发现订单正常，确认是诈骗电话。', role: 'victim' },
      { id: 'risk_path', type: 'dialog', title: '被诱导', content: '你按照对方的指引，添加了对方提供的QQ号，并准备进行"退款操作"。', role: 'victim', riskTip: '诈骗分子会引导你脱离正规平台进行操作' },
      { id: 'end_safe', type: 'end', title: '成功识破', content: '你识破了诈骗，避免了财产损失，并向平台举报了该号码。', role: 'narrator' },
      { id: 'end_risk', type: 'end', title: '遭受损失', content: '你按照对方要求转账"验证账户"，结果被骗走钱财。', role: 'narrator' }
    ],
    edges: [
      { from: 'start', to: 'verify', label: '继续通话', isSafeChoice: false },
      { from: 'verify', to: 'safe_path', label: '挂断电话，自行核实', isSafeChoice: true },
      { from: 'verify', to: 'risk_path', label: '按对方指引操作', isSafeChoice: false },
      { from: 'safe_path', to: 'end_safe', label: '完成核实', isSafeChoice: true },
      { from: 'risk_path', to: 'end_risk', label: '继续转账', isSafeChoice: false }
    ],
    startNodeId: 'start',
    endNodeIds: ['end_safe', 'end_risk']
  },
  public_security: {
    name: '冒充公检法诈骗',
    description: '模拟冒充公安机关要求转账的诈骗场景',
    nodes: [
      { id: 'start', type: 'start', title: '接到电话', content: '你接到一个自称"公安局"的电话，对方称你涉嫌洗钱案件。', role: 'narrator', riskTip: '公安机关不会通过电话办案，更不会要求转账' },
      { id: 'threat', type: 'dialog', title: '威胁恐吓', content: '对方说："你的银行账户涉嫌洗钱，需要立即冻结配合调查，否则将承担法律责任！"', role: 'scammer' },
      { id: 'safe_path', type: 'dialog', title: '冷静应对', content: '你挂断电话，拨打110核实情况，确认这是诈骗电话。', role: 'victim' },
      { id: 'risk_path', type: 'dialog', title: '恐慌操作', content: '你在对方的恐吓下，按照要求提供了银行卡信息并准备转账"配合调查"。', role: 'victim', riskTip: '诈骗分子利用恐慌心理让你失去判断力' },
      { id: 'end_safe', type: 'end', title: '成功识破', content: '你识破了诈骗，保护了自己的财产安全。', role: 'narrator' },
      { id: 'end_risk', type: 'end', title: '遭受损失', content: '你将钱转入了"安全账户"，结果被骗走全部积蓄。', role: 'narrator' }
    ],
    edges: [
      { from: 'start', to: 'threat', label: '继续通话', isSafeChoice: false },
      { from: 'threat', to: 'safe_path', label: '挂断并拨打110核实', isSafeChoice: true },
      { from: 'threat', to: 'risk_path', label: '害怕并配合对方', isSafeChoice: false },
      { from: 'safe_path', to: 'end_safe', label: '完成核实', isSafeChoice: true },
      { from: 'risk_path', to: 'end_risk', label: '继续转账', isSafeChoice: false }
    ],
    startNodeId: 'start',
    endNodeIds: ['end_safe', 'end_risk']
  },
  lottery: {
    name: '中奖诈骗',
    description: '模拟虚假中奖诱导转账的诈骗场景',
    nodes: [
      { id: 'start', type: 'start', title: '收到短信', content: '你收到一条短信：恭喜您被抽中为幸运用户，获得iPhone 15一部！', role: 'narrator', riskTip: '天上不会掉馅饼，正规中奖不需要先交费' },
      { id: 'contact', type: 'dialog', title: '联系对方', content: '你联系了短信中的客服，对方说需要先缴纳"个人所得税"才能领奖。', role: 'scammer' },
      { id: 'safe_path', type: 'dialog', title: '识破骗局', content: '你意识到这是诈骗，没有继续操作，并删除了短信。', role: 'victim' },
      { id: 'risk_path', type: 'dialog', title: '信以为真', content: '你以为自己真的中奖了，准备按照对方要求转账"税费"。', role: 'victim' },
      { id: 'end_safe', type: 'end', title: '成功识破', content: '你没有上当受骗，避免了财产损失。', role: 'narrator' },
      { id: 'end_risk', type: 'end', title: '遭受损失', content: '你转账后对方消失，才发现自己被骗了。', role: 'narrator' }
    ],
    edges: [
      { from: 'start', to: 'contact', label: '联系客服', isSafeChoice: false },
      { from: 'contact', to: 'safe_path', label: '意识到是诈骗', isSafeChoice: true },
      { from: 'contact', to: 'risk_path', label: '相信中奖信息', isSafeChoice: false },
      { from: 'safe_path', to: 'end_safe', label: '删除短信', isSafeChoice: true },
      { from: 'risk_path', to: 'end_risk', label: '转账税费', isSafeChoice: false }
    ],
    startNodeId: 'start',
    endNodeIds: ['end_safe', 'end_risk']
  },
  loan: {
    name: '贷款诈骗',
    description: '模拟虚假贷款诱导转账的诈骗场景',
    nodes: [
      { id: 'start', type: 'start', title: '浏览贷款信息', content: '你在网上看到一则"无抵押、低利息、快速放款"的贷款广告。', role: 'narrator', riskTip: '正规贷款机构不会要求先交费' },
      { id: 'apply', type: 'dialog', title: '申请贷款', content: '你联系了对方，对方说需要先缴纳"保证金"才能放款。', role: 'scammer' },
      { id: 'safe_path', type: 'dialog', title: '识破骗局', content: '你意识到正规贷款不需要先交钱，拒绝了对方的要求。', role: 'victim' },
      { id: 'risk_path', type: 'dialog', title: '急于用钱', content: '你因为急需用钱，按照对方要求转账了"保证金"。', role: 'victim' },
      { id: 'end_safe', type: 'end', title: '成功识破', content: '你没有上当，选择去正规银行办理贷款。', role: 'narrator' },
      { id: 'end_risk', type: 'end', title: '遭受损失', content: '转账后对方消失，你既没拿到贷款，还损失了保证金。', role: 'narrator' }
    ],
    edges: [
      { from: 'start', to: 'apply', label: '申请贷款', isSafeChoice: false },
      { from: 'apply', to: 'safe_path', label: '拒绝先交费', isSafeChoice: true },
      { from: 'apply', to: 'risk_path', label: '相信对方并转账', isSafeChoice: false },
      { from: 'safe_path', to: 'end_safe', label: '选择正规渠道', isSafeChoice: true },
      { from: 'risk_path', to: 'end_risk', label: '继续等待放款', isSafeChoice: false }
    ],
    startNodeId: 'start',
    endNodeIds: ['end_safe', 'end_risk']
  },
  empty: {
    name: '',
    description: '',
    nodes: [],
    edges: [],
    startNodeId: '',
    endNodeIds: []
  }
}

const handleTemplateSelect = async (command: string) => {
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
  
  Object.assign(model.value, {
    name: template.name || '',
    description: template.description || '',
    nodes: template.nodes ? JSON.parse(JSON.stringify(template.nodes)) : [],
    edges: template.edges ? JSON.parse(JSON.stringify(template.edges)) : [],
    startNodeId: template.startNodeId || '',
    endNodeIds: template.endNodeIds ? [...template.endNodeIds] : []
  })
  
  ElMessage.success('模板已应用')
  activeTab.value = 'nodes'
}
</script>

<style scoped lang="scss">
.scenario-script-editor {
  width: 100%;
  border: 1px solid hsl(220, 15%, 90%);
  border-radius: 8px;
  overflow: hidden;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: hsl(220, 15%, 97%);
  border-bottom: 1px solid hsl(220, 15%, 90%);

  .header-left {
    display: flex;
    align-items: center;
    flex: 1;
  }

  .header-right {
    margin-left: 16px;
  }
}

.editor-tabs {
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid hsl(220, 15%, 90%);
}

.editor-content {
  min-height: 400px;
  max-height: 500px;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid hsl(220, 15%, 92%);
  position: sticky;
  top: 0;
  z-index: 10;

  .panel-title {
    font-weight: 600;
    color: #333;
    font-size: 14px;
  }

  .header-actions {
    display: flex;
    gap: 8px;
  }
}

.nodes-list,
.edges-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.node-card {
  background: #fff;
  border: 1px solid hsl(220, 15%, 90%);
  border-radius: 8px;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px;
    background: hsl(220, 15%, 97%);
    border-bottom: 1px solid hsl(220, 15%, 90%);
    flex-wrap: wrap;
  }

  &__body {
    padding: 12px;
  }

  &__footer {
    display: flex;
    gap: 8px;
    padding: 8px 12px;
    background: hsl(220, 15%, 97%);
    border-top: 1px solid hsl(220, 15%, 90%);
  }
}

.edge-card {
  background: #fff;
  border: 1px solid hsl(220, 15%, 90%);
  border-radius: 8px;
  padding: 12px;

  &__main {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }

  &__options {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }
}

.edge-arrow {
  color: #909399;
  display: flex;
  align-items: center;
}

.type-option {
  display: flex;
  align-items: center;
  gap: 6px;
}

.type-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &--start { background: #67c23a; }
  &--dialog { background: #409eff; }
  &--decision { background: #e6a23c; }
  &--end { background: #f56c6c; }
}

.safe-label {
  color: #67c23a;
  font-weight: 500;
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  color: #909399;

  svg {
    margin-bottom: 16px;
    opacity: 0.5;
  }

  p {
    margin: 0;
    font-size: 14px;
  }

  .hint-text {
    margin-top: 4px;
    font-size: 12px;
    opacity: 0.7;
  }
}

.preview-panel {
  padding: 16px;
}

.preview-settings {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  padding: 12px;
  background: hsl(220, 15%, 97%);
  border-radius: 8px;

  .setting-item {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .setting-label {
    font-size: 13px;
    color: #606266;
    white-space: nowrap;
  }
}

.preview-graph {
  background: #fff;
  border: 1px solid hsl(220, 15%, 90%);
  border-radius: 8px;
  padding: 16px;
  min-height: 300px;
}

.graph-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.graph-nodes {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.graph-node {
  padding: 12px 16px;
  border-radius: 8px;
  border: 2px solid hsl(220, 15%, 85%);
  background: #fff;
  min-width: 120px;
  text-align: center;

  &.is-start {
    border-color: #67c23a;
    background: hsl(142, 70%, 95%);
  }

  &.is-end {
    border-color: #f56c6c;
    background: hsl(0, 65%, 95%);
  }

  &.node-type--start { border-left: 4px solid #67c23a; }
  &.node-type--dialog { border-left: 4px solid #409eff; }
  &.node-type--decision { border-left: 4px solid #e6a23c; }
  &.node-type--end { border-left: 4px solid #f56c6c; }

  &__id {
    font-weight: 600;
    font-size: 14px;
    color: #303133;
    margin-bottom: 4px;
  }

  &__type {
    font-size: 12px;
    color: #909399;
  }

  &__role {
    font-size: 11px;
    color: #c0c4cc;
    margin-top: 2px;
  }
}

.graph-edges {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px dashed hsl(220, 15%, 90%);
}

.graph-edge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: hsl(220, 15%, 97%);
  border-radius: 6px;
  font-size: 13px;

  .edge-from, .edge-to {
    font-family: monospace;
    color: #606266;
  }

  .edge-arrow {
    color: #909399;
  }

  .edge-label {
    color: #303133;
    font-weight: 500;
    margin-left: 8px;
  }
}

.empty-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  color: #909399;
  font-size: 14px;
}

.json-panel {
  padding: 16px;

  .json-error {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 8px;
    padding: 8px 12px;
    background: hsl(0, 65%, 95%);
    color: hsl(0, 65%, 45%);
    border-radius: 6px;
    font-size: 13px;
  }
}

:deep(.el-textarea.is-error .el-textarea__inner) {
  border-color: #f56c6c;
}
</style>
