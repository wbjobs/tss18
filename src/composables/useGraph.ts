import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { Graph } from '@antv/x6'
import type { Node, Edge } from '@antv/x6'
import { useDesignerStore } from '@/stores/designer'
import { NodeType } from '@/types/designer'
import type { StateNodeData, TransitionData } from '@/types/designer'

const NODE_WIDTH = 140
const NODE_HEIGHT = 60
const GRID_SIZE = 10

export function useGraph(containerRef: { value: HTMLElement | null }) {
  const graph = ref<Graph | null>(null)
  const designerStore = useDesignerStore()

  function createGraph() {
    if (!containerRef.value) return

    const graphConfig: any = {
      container: containerRef.value,
      background: {
        color: '#fafafa',
      },
      grid: {
        size: GRID_SIZE,
        visible: designerStore.gridEnabled,
        type: 'dot',
        args: {
          color: '#d0d0d0',
          thickness: 1,
        },
      },
      panning: {
        enabled: true,
        modifiers: 'shift',
      },
      mousewheel: {
        enabled: true,
        modifiers: 'ctrl',
        factor: 1.1,
        minScale: 0.1,
        maxScale: 3,
      },
      connecting: {
        anchor: 'center',
        connectionPoint: 'anchor',
        allowBlank: false,
        allowLoop: false,
        allowMulti: true,
        snap: {
          radius: 20,
        },
        createEdge() {
          return graph.value!.createEdge({
            shape: 'custom-edge',
            attrs: {
              line: {
                stroke: '#a0a0a0',
                strokeWidth: 2,
                targetMarker: {
                  name: 'classic',
                  size: 8,
                },
              },
            },
            zIndex: 0,
          })
        },
        validateConnection({ targetMagnet }: any) {
          return !!targetMagnet
        },
      },
      highlighting: {
        nodeAvailable: {
          name: 'className',
          args: {
            className: 'x6-node-available',
          },
        },
        magnetAvailable: {
          name: 'className',
          args: {
            className: 'x6-magnet-available',
          },
        },
        magnetAdsorbed: {
          name: 'className',
          args: {
            className: 'x6-magnet-adsorbed',
          },
        },
      },
      snapline: {
        enabled: designerStore.snapEnabled,
        sharp: true,
      },
      selecting: {
        enabled: true,
        multiple: false,
        rubberband: false,
        showNodeSelectionBox: true,
        showEdgeSelectionBox: true,
      },
      keyboard: true,
      embedding: {
        enabled: false,
      },
    }

    graph.value = new Graph(graphConfig)

    registerCustomShapes()
    bindEvents()
    bindKeyboard()
  }

  function registerCustomShapes() {
    if (!graph.value) return

    Graph.registerNode(
      'custom-node',
      {
        inherit: 'rect',
        width: NODE_WIDTH,
        height: NODE_HEIGHT,
        attrs: {
          body: {
            rx: 8,
            ry: 8,
            strokeWidth: 2,
            cursor: 'move',
          },
          label: {
            text: '',
            fill: '#ffffff',
            fontSize: 14,
            fontWeight: 'bold',
            textWrap: {
              width: NODE_WIDTH - 20,
              height: NODE_HEIGHT - 20,
              ellipsis: true,
            },
          },
          typeLabel: {
            text: '',
            fill: 'rgba(255,255,255,0.8)',
            fontSize: 10,
            textAnchor: 'end',
            refX: '100%',
            refX2: -8,
            refY: 8,
          },
        },
        markup: [
          {
            tagName: 'rect',
            selector: 'body',
          },
          {
            tagName: 'text',
            selector: 'label',
          },
          {
            tagName: 'text',
            selector: 'typeLabel',
          },
        ],
        ports: {
          groups: {
            top: {
              position: 'top',
              attrs: {
                circle: {
                  r: 4,
                  magnet: true,
                  stroke: '#5F95FF',
                  strokeWidth: 1,
                  fill: '#fff',
                },
              },
            },
            right: {
              position: 'right',
              attrs: {
                circle: {
                  r: 4,
                  magnet: true,
                  stroke: '#5F95FF',
                  strokeWidth: 1,
                  fill: '#fff',
                },
              },
            },
            bottom: {
              position: 'bottom',
              attrs: {
                circle: {
                  r: 4,
                  magnet: true,
                  stroke: '#5F95FF',
                  strokeWidth: 1,
                  fill: '#fff',
                },
              },
            },
            left: {
              position: 'left',
              attrs: {
                circle: {
                  r: 4,
                  magnet: true,
                  stroke: '#5F95FF',
                  strokeWidth: 1,
                  fill: '#fff',
                },
              },
            },
          },
          items: [
            { group: 'top', id: 'port-top' },
            { group: 'right', id: 'port-right' },
            { group: 'bottom', id: 'port-bottom' },
            { group: 'left', id: 'port-left' },
          ],
        },
      },
      true
    )

    Graph.registerEdge(
      'custom-edge',
      {
        inherit: 'edge',
        connector: {
          name: 'rounded',
          args: {
            radius: 8,
          },
        },
        router: {
          name: 'er',
          args: {
            direction: 'H',
          },
        },
        attrs: {
          line: {
            stroke: '#a0a0a0',
            strokeWidth: 2,
            targetMarker: {
              name: 'classic',
              size: 8,
            },
          },
        },
        labels: [
          {
            attrs: {
              text: {
                fill: '#666',
                fontSize: 12,
              },
              body: {
                fill: '#fff',
                stroke: '#d0d0d0',
                strokeWidth: 1,
                rx: 4,
                ry: 4,
                padding: 4,
              },
            },
          },
        ],
      },
      true
    )
  }

  function bindEvents() {
    if (!graph.value) return

    const g = graph.value

    g.on('node:click', ({ node }) => {
      const nodeData = node.getData() as StateNodeData
      if (nodeData) {
        designerStore.setSelection(nodeData.id, 'node')
      }
    })

    g.on('edge:click', ({ edge }) => {
      const edgeData = edge.getData() as TransitionData
      if (edgeData) {
        designerStore.setSelection(edgeData.id, 'edge')
      }
    })

    g.on('blank:click', () => {
      designerStore.clearSelection()
    })

    g.on('node:added', ({ node }) => {
      const nodeData = node.getData() as StateNodeData
      if (nodeData) {
        updateNodeVisual(node, nodeData)
      }
    })

    g.on('node:moved', ({ node }) => {
      const nodeData = node.getData() as StateNodeData
      if (nodeData) {
        const pos = node.position()
        designerStore.updateNodePosition(nodeData.id, pos.x, pos.y)
      }
    })

    g.on('edge:connected', ({ edge, isNew }) => {
      if (isNew) {
        const sourceNode = edge.getSourceNode()
        const targetNode = edge.getTargetNode()
        if (sourceNode && targetNode) {
          const sourceData = sourceNode.getData() as StateNodeData
          const targetData = targetNode.getData() as StateNodeData
          if (sourceData && targetData) {
            const transition = designerStore.addTransition(sourceData.id, targetData.id)
            edge.setData(transition)
            updateEdgeVisual(edge, transition)
          }
        }
      }
    })

    g.on('edge:label:changed', ({ edge }) => {
      const edgeData = edge.getData() as TransitionData
      if (edgeData) {
        const labels = edge.getLabels()
        const condition = (labels[0]?.attrs as any)?.text?.text || ''
        designerStore.updateTransition(edgeData.id, { condition })
      }
    })

    g.on('scale', ({ sx }) => {
      designerStore.setZoom(sx)
    })

    g.on('node:removed', ({ node }) => {
      const nodeData = node.getData() as StateNodeData
      if (nodeData) {
        designerStore.removeNode(nodeData.id)
      }
    })

    g.on('edge:removed', ({ edge }) => {
      const edgeData = edge.getData() as TransitionData
      if (edgeData) {
        designerStore.removeTransition(edgeData.id)
      }
    })
  }

  function bindKeyboard() {
    if (!graph.value) return

    const g = graph.value as any

    g.bindKey('Delete', () => {
      const cells = g.getSelectedCells()
      if (cells.length > 0) {
        g.removeCells(cells)
      }
    })

    g.bindKey('Backspace', () => {
      const cells = g.getSelectedCells()
      if (cells.length > 0) {
        g.removeCells(cells)
      }
    })

    g.bindKey('ctrl+z', () => {
      designerStore.undo()
      renderFromStore()
    })

    g.bindKey('ctrl+y', () => {
      designerStore.redo()
      renderFromStore()
    })
  }

  function updateNodeVisual(node: Node, data: StateNodeData) {
    const typeLabel = getTypeLabel(data.type)

    node.attr({
      body: {
        fill: data.color,
        stroke: darkenColor(data.color, 0.2),
      },
      label: {
        text: data.name,
      },
      typeLabel: {
        text: typeLabel,
      },
    })
  }

  function updateEdgeVisual(edge: Edge, data: TransitionData) {
    edge.setLabels([
      {
        attrs: {
          text: {
            text: data.condition || data.name,
          },
        },
      },
    ])
  }

  function getTypeLabel(type: NodeType): string {
    switch (type) {
      case NodeType.START:
        return '起'
      case NodeType.END:
        return '终'
      default:
        return ''
    }
  }

  function darkenColor(color: string, amount: number): string {
    const hex = color.replace('#', '')
    const r = Math.max(0, parseInt(hex.substr(0, 2), 16) * (1 - amount))
    const g = Math.max(0, parseInt(hex.substr(2, 2), 16) * (1 - amount))
    const b = Math.max(0, parseInt(hex.substr(4, 2), 16) * (1 - amount))
    return `rgb(${Math.round(r)}, ${Math.round(g)}, ${Math.round(b)})`
  }

  function addNodeToGraph(data: StateNodeData): Node {
    if (!graph.value) throw new Error('Graph not initialized')

    const node = graph.value.addNode({
      shape: 'custom-node',
      x: data.x,
      y: data.y,
      data,
    })

    updateNodeVisual(node, data)
    return node
  }

  function addEdgeToGraph(data: TransitionData): Edge | null {
    if (!graph.value) return null

    const sourceNode = graph.value.getNodes().find((n) => {
      const nodeData = n.getData() as StateNodeData
      return nodeData?.id === data.sourceStateId
    })

    const targetNode = graph.value.getNodes().find((n) => {
      const nodeData = n.getData() as StateNodeData
      return nodeData?.id === data.targetStateId
    })

    if (!sourceNode || !targetNode) return null

    const edge = graph.value.addEdge({
      shape: 'custom-edge',
      source: sourceNode,
      target: targetNode,
      data,
    })

    updateEdgeVisual(edge, data)
    return edge
  }

  function renderFromStore() {
    if (!graph.value) return

    graph.value.clearCells()

    designerStore.nodes.forEach((nodeData) => {
      addNodeToGraph(nodeData)
    })

    designerStore.transitions.forEach((transitionData) => {
      addEdgeToGraph(transitionData)
    })

    designerStore.clearSelection()
  }

  function handleDrop(event: DragEvent) {
    if (!graph.value || !event.dataTransfer) return

    const nodeType = event.dataTransfer.getData('application/x-node-type') as NodeType
    if (!nodeType) return

    event.preventDefault()

    const point = graph.value.clientToLocal(event.clientX, event.clientY)
    const x = point.x - NODE_WIDTH / 2
    const y = point.y - NODE_HEIGHT / 2

    const snappedX = designerStore.snapEnabled ? Math.round(x / GRID_SIZE) * GRID_SIZE : x
    const snappedY = designerStore.snapEnabled ? Math.round(y / GRID_SIZE) * GRID_SIZE : y

    const nodeData = designerStore.addNode(nodeType, snappedX, snappedY)
    addNodeToGraph(nodeData)
  }

  function handleDragOver(event: DragEvent) {
    event.preventDefault()
  }

  function zoomIn() {
    if (!graph.value) return
    graph.value.zoom(0.1)
  }

  function zoomOut() {
    if (!graph.value) return
    graph.value.zoom(-0.1)
  }

  function zoomTo(scale: number) {
    if (!graph.value) return
    graph.value.zoomTo(scale)
  }

  function zoomFit() {
    if (!graph.value) return
    graph.value.zoomToFit({ padding: 20, maxScale: 1 })
  }

  function setGrid(enabled: boolean) {
    if (!graph.value) return
    if (enabled) {
      graph.value.showGrid()
    } else {
      graph.value.hideGrid()
    }
  }

  function setSnap(enabled: boolean) {
    if (!graph.value) return
    const snapline = graph.value.getPlugin('snapline') as any
    if (snapline) {
      snapline.setEnabled(enabled)
    }
  }

  function clearAll() {
    if (!graph.value) return
    graph.value.clearCells()
    designerStore.clearAll()
  }

  function exportData() {
    return designerStore.exportData()
  }

  function importData(data: any) {
    designerStore.importData(data)
    renderFromStore()
  }

  function selectCell(id: string, type: 'node' | 'edge') {
    if (!graph.value) return

    const g = graph.value as any
    const cells = g.getCells()
    const cell = cells.find((c: any) => {
      const cellData = c.getData() as StateNodeData | TransitionData
      return cellData?.id === id
    })

    if (cell) {
      g.select(cell)
    }
  }

  function refreshNode(nodeId: string) {
    if (!graph.value) return

    const node = graph.value.getNodes().find((n) => {
      const nodeData = n.getData() as StateNodeData
      return nodeData?.id === nodeId
    })

    const nodeData = designerStore.nodes.find((n) => n.id === nodeId)
    if (node && nodeData) {
      updateNodeVisual(node, nodeData)
    }
  }

  function refreshEdge(edgeId: string) {
    if (!graph.value) return

    const edge = graph.value.getEdges().find((e) => {
      const edgeData = e.getData() as TransitionData
      return edgeData?.id === edgeId
    })

    const edgeData = designerStore.transitions.find((t) => t.id === edgeId)
    if (edge && edgeData) {
      updateEdgeVisual(edge, edgeData)
    }
  }

  let animationTimers: number[] = []

  function highlightSimulationPath(path: { fromStateId: string; toStateId: string; transitionId?: string; conditionMet?: boolean }) {
    if (!graph.value) return

    const g = graph.value
    const nodes = g.getNodes()
    const edges = g.getEdges()

    const sourceNode = nodes.find((n) => {
      const d = n.getData() as StateNodeData
      return d?.id === path.fromStateId
    })
    const targetNode = nodes.find((n) => {
      const d = n.getData() as StateNodeData
      return d?.id === path.toStateId
    })

    const matchedEdge = edges.find((e) => {
      const d = e.getData() as TransitionData
      return d?.id === path.transitionId
    })

    const fallbackEdge = !matchedEdge ? edges.find((e) => {
      const d = e.getData() as TransitionData
      return d?.sourceStateId === path.fromStateId && d?.targetStateId === path.toStateId
    }) : null

    const edgeToHighlight = matchedEdge || fallbackEdge

    const isMet = path.conditionMet !== false
    const highlightColor = isMet ? '#10b981' : '#ef4444'
    const nodeGlowColor = isMet ? '#10b981' : '#f59e0b'

    if (sourceNode) {
      const srcData = sourceNode.getData() as StateNodeData
      sourceNode.attr('body/stroke', nodeGlowColor)
      sourceNode.attr('body/strokeWidth', 4)
      sourceNode.attr('body/filter', {
        name: 'dropShadow',
        args: { dx: 0, dy: 0, blur: 8, opacity: 0.6, color: nodeGlowColor },
      })
    }

    if (targetNode) {
      const pulseCount = 3
      let pulse = 0
      const doPulse = () => {
        if (pulse >= pulseCount * 2) {
          targetNode.attr('body/strokeWidth', 4)
          targetNode.attr('body/stroke', nodeGlowColor)
          targetNode.attr('body/filter', {
            name: 'dropShadow',
            args: { dx: 0, dy: 0, blur: 12, opacity: 0.7, color: nodeGlowColor },
          })
          return
        }
        const isExpand = pulse % 2 === 0
        targetNode.attr('body/strokeWidth', isExpand ? 6 : 3)
        targetNode.attr('body/stroke', isExpand ? nodeGlowColor : 'transparent')
        pulse++
        const timer = window.setTimeout(doPulse, 300)
        animationTimers.push(timer)
      }
      doPulse()
    }

    if (edgeToHighlight) {
      edgeToHighlight.attr('line/stroke', highlightColor)
      edgeToHighlight.attr('line/strokeWidth', 3)
      edgeToHighlight.attr('line/strokeDasharray', 8)
      edgeToHighlight.attr('line/style/animation', 'simulation-flow 1s linear infinite')

      let offset = 0
      const animateEdge = () => {
        offset += 1
        edgeToHighlight.attr('line/strokeDashoffset', offset)
        const timer = window.requestAnimationFrame(animateEdge)
        animationTimers.push(timer)
      }
      animateEdge()
    }
  }

  function resetSimulationHighlight() {
    if (!graph.value) return

    animationTimers.forEach((timer) => {
      window.clearTimeout(timer)
      window.cancelAnimationFrame(timer)
    })
    animationTimers = []

    const g = graph.value
    g.getNodes().forEach((node) => {
      const data = node.getData() as StateNodeData
      if (data) {
        node.attr('body/stroke', darkenColor(data.color, 0.2))
        node.attr('body/strokeWidth', 2)
        ;(node as any).removeAttr('body/filter')
      }
    })

    g.getEdges().forEach((edge) => {
      edge.attr('line/stroke', '#a0a0a0')
      edge.attr('line/strokeWidth', 2)
      ;(edge as any).removeAttr('line/strokeDasharray')
      ;(edge as any).removeAttr('line/strokeDashoffset')
      ;(edge as any).removeAttr('line/style/animation')
    })
  }

  watch(
    () => designerStore.gridEnabled,
    (enabled) => {
      setGrid(enabled)
    }
  )

  watch(
    () => designerStore.snapEnabled,
    (enabled) => {
      setSnap(enabled)
    }
  )

  watch(
    () => designerStore.selectedId,
    (id) => {
      if (id && designerStore.selectedType) {
        selectCell(id, designerStore.selectedType)
      }
    }
  )

  onMounted(() => {
    createGraph()
    if (graph.value) {
      const container = containerRef.value
      if (container) {
        container.addEventListener('drop', handleDrop)
        container.addEventListener('dragover', handleDragOver)
      }
      designerStore.initialize()
    }
  })

  onBeforeUnmount(() => {
    if (containerRef.value) {
      containerRef.value.removeEventListener('drop', handleDrop)
      containerRef.value.removeEventListener('dragover', handleDragOver)
    }
    if (graph.value) {
      graph.value.dispose()
      graph.value = null
    }
  })

  return {
    graph,
    renderFromStore,
    handleDrop,
    handleDragOver,
    zoomIn,
    zoomOut,
    zoomTo,
    zoomFit,
    setGrid,
    setSnap,
    clearAll,
    exportData,
    importData,
    refreshNode,
    refreshEdge,
    highlightSimulationPath,
    resetSimulationHighlight,
  }
}
