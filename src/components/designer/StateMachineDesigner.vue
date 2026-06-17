<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FlaskConical, X } from 'lucide-vue-next'
import NodePanel from './NodePanel.vue'
import PropertyPanel from './PropertyPanel.vue'
import SimulationPanel from './SimulationPanel.vue'
import Toolbar from './Toolbar.vue'
import { useGraph } from '@/composables/useGraph'
import { useDesignerStore } from '@/stores/designer'
import { useStateMachineStore } from '@/stores/stateMachine'
import type { StateMachineData, StateNodeData, TransitionData } from '@/types/designer'
import type { StateNode, Transition, SimulationPath } from '@/types'

const props = defineProps<{
  machineId?: string
  machineName?: string
  initialData?: StateMachineData
}>()

const emit = defineEmits<{
  (e: 'save', data: StateMachineData): void
  (e: 'publish', data: StateMachineData): void
  (e: 'preview', data: StateMachineData): void
}>()

const route = useRoute()
const containerRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const saving = ref(false)
const publishing = ref(false)
const loading = ref(false)

const designerStore = useDesignerStore()
const stateMachineStore = useStateMachineStore()

const {
  renderFromStore,
  zoomIn,
  zoomOut,
  zoomTo,
  zoomFit,
  clearAll,
  exportData,
  importData,
  refreshNode,
  refreshEdge,
  highlightSimulationPath,
  resetSimulationHighlight,
} = useGraph(containerRef)

const showSimulation = ref(false)

const zoom = computed(() => designerStore.zoom)
const currentMachineId = computed(() => {
  return props.machineId || (route.params.id as string) || stateMachineStore.currentMachine?.id
})

function convertToDesignerData(machine: { nodes: StateNode[]; transitions: Transition[] }): StateMachineData {
  return {
    nodes: (machine.nodes || []).map(n => ({
      id: n.id,
      name: n.name,
      type: n.type as any,
      x: n.x,
      y: n.y,
      color: n.color,
      permissions: n.permissions || [],
    })),
    transitions: (machine.transitions || []).map(t => ({
      id: t.id,
      name: t.name,
      sourceStateId: t.sourceStateId,
      targetStateId: t.targetStateId,
      condition: t.condition || '',
      triggerSource: (t.triggerSource || 'MANUAL') as any,
      callbackSource: t.callbackSource || '',
    })),
  }
}

async function loadStateMachine() {
  const id = currentMachineId.value
  if (!id) {
    designerStore.initialize()
    return
  }
  loading.value = true
  try {
    const res = await stateMachineStore.fetchStateMachine(id)
    if (res.code === 200 && res.data) {
      const designerData = convertToDesignerData(res.data)
      designerStore.importData(designerData)
      setTimeout(() => {
        renderFromStore()
      }, 100)
    }
  } catch (err: any) {
    ElMessage.error('加载状态机失败: ' + (err.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  const data = exportData()
  emit('save', data)

  const id = currentMachineId.value
  if (!id) {
    try {
      const { value: name } = await ElMessageBox.prompt('请输入状态机名称', '新建状态机', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /.+/,
        inputErrorMessage: '名称不能为空',
      })
      saving.value = true
      const res = await stateMachineStore.create(name, '')
      if (res.code === 200 && res.data) {
        const machineId = Number(res.data.id)
        await stateMachineStore.save(machineId)
        ElMessage.success('保存成功')
      } else {
        ElMessage.error(res.message || '保存失败')
      }
    } catch {
    } finally {
      saving.value = false
    }
  } else {
    saving.value = true
    try {
      const res = await stateMachineStore.save(Number(id))
      if (res.code === 200) {
        ElMessage.success('保存成功')
      } else {
        ElMessage.error(res.message || '保存失败')
      }
    } catch (err: any) {
      ElMessage.error('保存失败: ' + (err.message || '未知错误'))
    } finally {
      saving.value = false
    }
  }
}

async function handlePublish() {
  const data = exportData()
  if (data.nodes.length === 0) {
    ElMessage.warning('请至少添加一个节点')
    return
  }
  const hasStart = data.nodes.some((n) => n.type === 'START')
  if (!hasStart) {
    ElMessage.warning('请添加起始状态节点')
    return
  }
  const hasEnd = data.nodes.some((n) => n.type === 'END')
  if (!hasEnd) {
    ElMessage.warning('请添加结束状态节点')
    return
  }

  emit('publish', data)

  const id = currentMachineId.value
  if (!id) {
    ElMessage.warning('请先保存状态机')
    return
  }

  try {
    await ElMessageBox.confirm('确定要发布此状态机吗？发布后将无法编辑。', '发布确认', {
      confirmButtonText: '确定发布',
      cancelButtonText: '取消',
      type: 'warning',
    })
    publishing.value = true
    const res = await stateMachineStore.publish(Number(id))
    if (res.code === 200) {
      ElMessage.success('发布成功')
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } catch {
  } finally {
    publishing.value = false
  }
}

function handlePreview() {
  const data = exportData()
  emit('preview', data)
}

function handleUndo() {
  designerStore.undo()
  renderFromStore()
}

function handleRedo() {
  designerStore.redo()
  renderFromStore()
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定要清空画布吗？此操作不可撤销。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    clearAll()
    ElMessage.success('画布已清空')
  } catch {
  }
}

function handleImport() {
  fileInputRef.value?.click()
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const content = e.target?.result as string
      const data = JSON.parse(content) as StateMachineData
      importData(data)
      ElMessage.success('导入成功')
    } catch {
      ElMessage.error('文件格式错误，请导入有效的 JSON 文件')
    }
  }
  reader.readAsText(file)
  input.value = ''
}

function handleExport() {
  const data = exportData()
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `state-machine-${Date.now()}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

function handleZoomIn() {
  zoomIn()
}

function handleZoomOut() {
  zoomOut()
}

function handleZoomReset() {
  zoomTo(1)
}

function handleZoomFit() {
  zoomFit()
}

function handleToggleGrid() {
  designerStore.setGridEnabled(!designerStore.gridEnabled)
}

function handleToggleSnap() {
  designerStore.setSnapEnabled(!designerStore.snapEnabled)
}

function handleRefreshNode(nodeId: string) {
  refreshNode(nodeId)
}

function handleRefreshEdge(edgeId: string) {
  refreshEdge(edgeId)
}

function handleHighlightPath(path: SimulationPath) {
  resetSimulationHighlight()
  setTimeout(() => {
    highlightSimulationPath({
      fromStateId: path.fromStateId,
      toStateId: path.toStateId,
      transitionId: path.transitionId,
      conditionMet: path.conditionMet,
    })
  }, 50)
}

function handleHighlightReset() {
  resetSimulationHighlight()
}

function toggleSimulation() {
  showSimulation.value = !showSimulation.value
  if (!showSimulation.value) {
    resetSimulationHighlight()
  }
}

onMounted(() => {
  designerStore.initialize()
  if (props.initialData) {
    designerStore.importData(props.initialData)
  }
  loadStateMachine()
})
</script>

<template>
  <div class="h-full flex flex-col bg-gray-100">
    <Toolbar
      :zoom="zoom"
      :saving="saving"
      :publishing="publishing"
      :loading="loading"
      @save="handleSave"
      @publish="handlePublish"
      @preview="handlePreview"
      @undo="handleUndo"
      @redo="handleRedo"
      @clear="handleClear"
      @import="handleImport"
      @export="handleExport"
      @zoom-in="handleZoomIn"
      @zoom-out="handleZoomOut"
      @zoom-reset="handleZoomReset"
      @zoom-fit="handleZoomFit"
      @toggle-grid="handleToggleGrid"
      @toggle-snap="handleToggleSnap"
    />

    <div class="flex-1 flex overflow-hidden">
      <NodePanel />

      <div class="flex-1 relative">
        <div
          ref="containerRef"
          class="absolute inset-0"
        />
        <div
          v-if="loading"
          class="absolute inset-0 bg-white/50 flex items-center justify-center z-50"
        >
          <el-icon class="is-loading text-4xl text-blue-500"><Loading /></el-icon>
        </div>

        <button
          class="absolute top-3 right-3 z-20 flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-all duration-200"
          :class="showSimulation
            ? 'bg-emerald-500 text-white shadow-lg shadow-emerald-500/30'
            : 'bg-white/90 text-gray-600 shadow border border-gray-200 hover:border-emerald-300 hover:text-emerald-600'"
          @click="toggleSimulation"
        >
          <FlaskConical :size="16" />
          {{ showSimulation ? '关闭仿真' : '路由仿真' }}
        </button>
      </div>

      <PropertyPanel
        v-if="!showSimulation"
        @refresh-node="handleRefreshNode"
        @refresh-edge="handleRefreshEdge"
      />

      <SimulationPanel
        v-if="showSimulation"
        :state-machine-id="Number(currentMachineId) || 0"
        :nodes="designerStore.nodes"
        @highlight-path="handleHighlightPath"
        @highlight-reset="handleHighlightReset"
      />
    </div>

    <input
      ref="fileInputRef"
      type="file"
      accept=".json"
      class="hidden"
      @change="handleFileChange"
    />
  </div>
</template>

<script lang="ts">
import { Loading } from 'lucide-vue-next'

export default {
  name: 'StateMachineDesigner',
}
</script>

<style>
.x6-node-available {
  outline: 2px solid #52c41a;
  outline-offset: 2px;
}

.x6-magnet-available {
  fill: #52c41a !important;
  stroke: #52c41a !important;
}

.x6-magnet-adsorbed {
  fill: #1890ff !important;
  stroke: #1890ff !important;
  r: 6 !important;
}

.x6-node-selected rect {
  stroke: #1890ff !important;
  stroke-width: 3 !important;
}

.x6-edge-selected path:nth-child(2) {
  stroke: #1890ff !important;
  stroke-width: 3 !important;
}

.x6-graph-scroller {
  overflow: auto !important;
}

@keyframes simulation-flow {
  to {
    stroke-dashoffset: -16;
  }
}
</style>
