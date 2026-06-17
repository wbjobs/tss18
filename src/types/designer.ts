export enum NodeType {
  START = 'START',
  NORMAL = 'NORMAL',
  END = 'END',
}

export enum TriggerSource {
  MANUAL = 'MANUAL',
  CALLBACK = 'CALLBACK',
  AUTO = 'AUTO',
}

export interface StateNodeData {
  id: string
  name: string
  type: NodeType
  x: number
  y: number
  color: string
  permissions: string[]
}

export interface TransitionData {
  id: string
  name: string
  sourceStateId: string
  targetStateId: string
  condition: string
  triggerSource: TriggerSource
  callbackSource: string
}

export interface StateMachineData {
  id?: string
  name?: string
  nodes: StateNodeData[]
  transitions: TransitionData[]
}

export type SelectionType = 'node' | 'edge' | null

export interface DesignerState {
  nodes: StateNodeData[]
  transitions: TransitionData[]
  selectedId: string | null
  selectedType: SelectionType
  zoom: number
  gridEnabled: boolean
  snapEnabled: boolean
  canUndo: boolean
  canRedo: boolean
  historyStack: StateMachineData[]
  historyIndex: number
}

export interface NodePanelItem {
  type: NodeType
  name: string
  icon: string
  color: string
}

export const NODE_PANEL_ITEMS: NodePanelItem[] = [
  {
    type: NodeType.START,
    name: '起始状态',
    icon: 'PlayCircle',
    color: '#52c41a',
  },
  {
    type: NodeType.NORMAL,
    name: '普通状态',
    icon: 'Circle',
    color: '#1890ff',
  },
  {
    type: NodeType.END,
    name: '结束状态',
    icon: 'StopCircle',
    color: '#ff4d4f',
  },
]

export const DEFAULT_NODE_COLORS: Record<NodeType, string> = {
  [NodeType.START]: '#52c41a',
  [NodeType.NORMAL]: '#1890ff',
  [NodeType.END]: '#ff4d4f',
}
