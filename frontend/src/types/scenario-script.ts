/**
 * 案例/闯关情景剧本结构（与后端 Challenge.ScenarioScript 一致）
 */
export interface ScenarioNode {
  id: string
  type: string
  title?: string
  content?: string
  role?: string
  riskTip?: string
}

export interface ScenarioEdge {
  from: string
  to: string
  condition?: string
  label?: string
  isSafeChoice?: boolean
}

export interface ScenarioScriptModel {
  name: string
  description: string
  nodes: ScenarioNode[]
  edges: ScenarioEdge[]
  startNodeId: string
  endNodeIds: string[]
}

export function emptyScenarioScript(): ScenarioScriptModel {
  return {
    name: '',
    description: '',
    nodes: [],
    edges: [],
    startNodeId: '',
    endNodeIds: []
  }
}

/** 将后端/数据库中的 JSON 字符串解析为编辑模型 */
export function parseScenarioScriptJson(s: string | null | undefined): ScenarioScriptModel {
  const empty = emptyScenarioScript()
  if (!s || !String(s).trim()) return empty
  try {
    const o = JSON.parse(s)
    return {
      name: o.name ?? '',
      description: o.description ?? '',
      nodes: Array.isArray(o.nodes)
        ? o.nodes.map((n: Record<string, unknown>) => ({
            id: String(n.id ?? ''),
            type: String(n.type ?? 'dialog'),
            title: n.title != null ? String(n.title) : '',
            content: n.content != null ? String(n.content) : '',
            role: n.role != null ? String(n.role) : 'narrator',
            riskTip: n.riskTip != null ? String(n.riskTip) : ''
          }))
        : [],
      edges: Array.isArray(o.edges)
        ? o.edges.map((e: Record<string, unknown>) => ({
            from: String(e.from ?? ''),
            to: String(e.to ?? ''),
            condition: e.condition != null ? String(e.condition) : '',
            label: e.label != null ? String(e.label) : '',
            isSafeChoice: Boolean(e.isSafeChoice)
          }))
        : [],
      startNodeId: o.startNodeId != null ? String(o.startNodeId) : '',
      endNodeIds: Array.isArray(o.endNodeIds) ? o.endNodeIds.map(String) : []
    }
  } catch {
    return empty
  }
}
