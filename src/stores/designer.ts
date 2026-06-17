import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  StateNodeData,
  TransitionData,
  SelectionType,
  StateMachineData,
} from '@/types/designer'
import { NodeType, DEFAULT_NODE_COLORS } from '@/types/designer'

export const useDesignerStore = defineStore('designer', () => {
  const nodes = ref<StateNodeData[]>([])
  const transitions = ref<TransitionData[]>([])
  const selectedId = ref<string | null>(null)
  const selectedType = ref<SelectionType>(null)
  const zoom = ref(1)
  const gridEnabled = ref(true)
  const snapEnabled = ref(true)
  const historyStack = ref<StateMachineData[]>([])
  const historyIndex = ref(-1)

  const canUndo = computed(() => historyIndex.value > 0)
  const canRedo = computed(() => historyIndex.value < historyStack.value.length - 1)

  const selectedNode = computed(() => {
    if (selectedType.value !== 'node' || !selectedId.value) return null
    return nodes.value.find((n) => n.id === selectedId.value) || null
  })

  const selectedTransition = computed(() => {
    if (selectedType.value !== 'edge' || !selectedId.value) return null
    return transitions.value.find((t) => t.id === selectedId.value) || null
  })

  function generateId(): string {
    return `id_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
  }

  function saveToHistory() {
    const currentData: StateMachineData = {
      nodes: JSON.parse(JSON.stringify(nodes.value)),
      transitions: JSON.parse(JSON.stringify(transitions.value)),
    }

    historyStack.value = historyStack.value.slice(0, historyIndex.value + 1)
    historyStack.value.push(currentData)
    historyIndex.value = historyStack.value.length - 1

    if (historyStack.value.length > 50) {
      historyStack.value.shift()
      historyIndex.value--
    }
  }

  function undo() {
    if (!canUndo.value) return
    historyIndex.value--
    loadFromHistory()
  }

  function redo() {
    if (!canRedo.value) return
    historyIndex.value++
    loadFromHistory()
  }

  function loadFromHistory() {
    const data = historyStack.value[historyIndex.value]
    if (data) {
      nodes.value = JSON.parse(JSON.stringify(data.nodes))
      transitions.value = JSON.parse(JSON.stringify(data.transitions))
    }
  }

  function addNode(type: NodeType, x: number, y: number): StateNodeData {
    const node: StateNodeData = {
      id: generateId(),
      name: getDefaultNodeName(type),
      type,
      x,
      y,
      color: DEFAULT_NODE_COLORS[type],
      permissions: [],
    }
    nodes.value.push(node)
    saveToHistory()
    return node
  }

  function getDefaultNodeName(type: NodeType): string {
    const count = nodes.value.filter((n) => n.type === type).length + 1
    switch (type) {
      case NodeType.START:
        return `起始状态${count}`
      case NodeType.END:
        return `结束状态${count}`
      default:
        return `状态${count}`
    }
  }

  function updateNode(id: string, updates: Partial<StateNodeData>) {
    const index = nodes.value.findIndex((n) => n.id === id)
    if (index !== -1) {
      nodes.value[index] = { ...nodes.value[index], ...updates }
      saveToHistory()
    }
  }

  function updateNodePosition(id: string, x: number, y: number) {
    const node = nodes.value.find((n) => n.id === id)
    if (node) {
      node.x = x
      node.y = y
    }
  }

  function removeNode(id: string) {
    nodes.value = nodes.value.filter((n) => n.id !== id)
    transitions.value = transitions.value.filter(
      (t) => t.sourceStateId !== id && t.targetStateId !== id
    )
    if (selectedId.value === id) {
      clearSelection()
    }
    saveToHistory()
  }

  function addTransition(
    sourceId: string,
    targetId: string,
    options?: Partial<TransitionData>
  ): TransitionData {
    const transition: TransitionData = {
      id: generateId(),
      name: options?.name || '转移',
      sourceStateId: sourceId,
      targetStateId: targetId,
      condition: options?.condition || '',
      triggerSource: options?.triggerSource || ('MANUAL' as any),
      callbackSource: options?.callbackSource || '',
    }
    transitions.value.push(transition)
    saveToHistory()
    return transition
  }

  function updateTransition(id: string, updates: Partial<TransitionData>) {
    const index = transitions.value.findIndex((t) => t.id === id)
    if (index !== -1) {
      transitions.value[index] = { ...transitions.value[index], ...updates }
      saveToHistory()
    }
  }

  function removeTransition(id: string) {
    transitions.value = transitions.value.filter((t) => t.id !== id)
    if (selectedId.value === id) {
      clearSelection()
    }
    saveToHistory()
  }

  function setSelection(id: string | null, type: SelectionType) {
    selectedId.value = id
    selectedType.value = type
  }

  function clearSelection() {
    selectedId.value = null
    selectedType.value = null
  }

  function setZoom(value: number) {
    zoom.value = Math.max(0.1, Math.min(3, value))
  }

  function setGridEnabled(enabled: boolean) {
    gridEnabled.value = enabled
  }

  function setSnapEnabled(enabled: boolean) {
    snapEnabled.value = enabled
  }

  function clearAll() {
    nodes.value = []
    transitions.value = []
    clearSelection()
    saveToHistory()
  }

  function exportData(): StateMachineData {
    return {
      nodes: JSON.parse(JSON.stringify(nodes.value)),
      transitions: JSON.parse(JSON.stringify(transitions.value)),
    }
  }

  function importData(data: StateMachineData) {
    nodes.value = JSON.parse(JSON.stringify(data.nodes || []))
    transitions.value = JSON.parse(JSON.stringify(data.transitions || []))
    clearSelection()
    saveToHistory()
  }

  function initialize() {
    historyStack.value = []
    historyIndex.value = -1
    saveToHistory()
  }

  return {
    nodes,
    transitions,
    selectedId,
    selectedType,
    zoom,
    gridEnabled,
    snapEnabled,
    canUndo,
    canRedo,
    selectedNode,
    selectedTransition,
    addNode,
    updateNode,
    updateNodePosition,
    removeNode,
    addTransition,
    updateTransition,
    removeTransition,
    setSelection,
    clearSelection,
    setZoom,
    setGridEnabled,
    setSnapEnabled,
    undo,
    redo,
    clearAll,
    exportData,
    importData,
    initialize,
    saveToHistory,
  }
})
