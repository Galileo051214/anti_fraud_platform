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
  position?: ScenarioNodePosition
}

export interface ScenarioNodePosition {
  x: number
  y: number
}

export interface ScenarioEdge {
  from: string
  to: string
  condition?: string
  label?: string
  scoreType?: ScenarioScoreType
  isSafeChoice?: boolean
}

export type ScenarioScoreType = 'none' | 'safe' | 'risk'

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
            riskTip: n.riskTip != null ? String(n.riskTip) : '',
            position: normalizePosition(n.position)
          }))
        : [],
      edges: Array.isArray(o.edges)
        ? o.edges.map((e: Record<string, unknown>) => {
            const scoreType = normalizeScoreType(e.scoreType, e.isSafeChoice)
            return {
              from: String(e.from ?? ''),
              to: String(e.to ?? ''),
              condition: e.condition != null ? String(e.condition) : '',
              label: e.label != null ? String(e.label) : '',
              scoreType,
              isSafeChoice: scoreType === 'safe' ? true : scoreType === 'risk' ? false : undefined
            }
          })
        : [],
      startNodeId: o.startNodeId != null ? String(o.startNodeId) : '',
      endNodeIds: Array.isArray(o.endNodeIds) ? o.endNodeIds.map(String) : []
    }
  } catch {
    return empty
  }
}

function normalizeScoreType(value: unknown, legacySafeChoice: unknown): ScenarioScoreType {
  const normalized = typeof value === 'string' ? value.trim().toLowerCase() : ''
  if (normalized === 'none' || normalized === 'safe' || normalized === 'risk') return normalized
  if (legacySafeChoice === true) return 'safe'
  if (legacySafeChoice === false) return 'risk'
  return 'none'
}

function normalizePosition(value: unknown): ScenarioNodePosition | undefined {
  if (!value || typeof value !== 'object') return undefined
  const raw = value as Record<string, unknown>
  const x = Number(raw.x)
  const y = Number(raw.y)
  if (!Number.isFinite(x) || !Number.isFinite(y)) return undefined
  return {
    x: Math.max(0, Math.round(x)),
    y: Math.max(0, Math.round(y))
  }
}
