<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { Trash2, Settings, GitBranch, AlertCircle } from 'lucide-vue-next'
import { useDesignerStore } from '@/stores/designer'
import { NodeType, TriggerSource } from '@/types/designer'
import type { StateNodeData, TransitionData } from '@/types/designer'

const emit = defineEmits<{
  (e: 'refreshNode', nodeId: string): void
  (e: 'refreshEdge', edgeId: string): void
}>()

const designerStore = useDesignerStore()

const nodeName = ref('')
const nodeType = ref<NodeType>(NodeType.NORMAL)
const nodeColor = ref('#1890ff')
const nodePermissions = ref<string[]>([])

const edgeName = ref('')
const edgeCondition = ref('')
const edgeTriggerSource = ref<TriggerSource>(TriggerSource.MANUAL)
const edgeCallbackSource = ref('')

const nodeTypeOptions = [
  { label: '起始状态', value: NodeType.START },
  { label: '普通状态', value: NodeType.NORMAL },
  { label: '结束状态', value: NodeType.END },
]

const triggerSourceOptions = [
  { label: '手动触发', value: TriggerSource.MANUAL },
  { label: '回调触发', value: TriggerSource.CALLBACK },
  { label: '自动触发', value: TriggerSource.AUTO },
]

const selectedNode = computed(() => designerStore.selectedNode)
const selectedEdge = computed(() => designerStore.selectedTransition)
const selectedType = computed(() => designerStore.selectedType)

watch(
  selectedNode,
  (node) => {
    if (node) {
      nodeName.value = node.name
      nodeType.value = node.type
      nodeColor.value = node.color
      nodePermissions.value = [...node.permissions]
    }
  },
  { immediate: true }
)

watch(
  selectedEdge,
  (edge) => {
    if (edge) {
      edgeName.value = edge.name
      edgeCondition.value = edge.condition
      edgeTriggerSource.value = edge.triggerSource
      edgeCallbackSource.value = edge.callbackSource
    }
  },
  { immediate: true }
)

function saveNodeChanges() {
  if (!selectedNode.value) return

  const updates: Partial<StateNodeData> = {
    name: nodeName.value,
    type: nodeType.value,
    color: nodeColor.value,
    permissions: [...nodePermissions.value],
  }

  designerStore.updateNode(selectedNode.value.id, updates)
  emit('refreshNode', selectedNode.value.id)
}

function saveEdgeChanges() {
  if (!selectedEdge.value) return

  const updates: Partial<TransitionData> = {
    name: edgeName.value,
    condition: edgeCondition.value,
    triggerSource: edgeTriggerSource.value,
    callbackSource: edgeCallbackSource.value,
  }

  designerStore.updateTransition(selectedEdge.value.id, updates)
  emit('refreshEdge', selectedEdge.value.id)
}

function deleteSelected() {
  if (selectedNode.value) {
    designerStore.removeNode(selectedNode.value.id)
  } else if (selectedEdge.value) {
    designerStore.removeTransition(selectedEdge.value.id)
  }
}

function addPermission() {
  nodePermissions.value.push('')
}

function removePermission(index: number) {
  nodePermissions.value.splice(index, 1)
}

function updatePermission(index: number, value: string) {
  nodePermissions.value[index] = value
}
</script>

<template>
  <div class="w-72 bg-white border-l border-gray-200 flex flex-col h-full">
    <div class="p-4 border-b border-gray-200 flex items-center justify-between">
      <h3 class="text-sm font-semibold text-gray-700 flex items-center gap-2">
        <Settings :size="16" />
        属性面板
      </h3>
    </div>

    <div class="flex-1 overflow-y-auto p-4">
      <div v-if="!selectedType" class="flex flex-col items-center justify-center h-full text-gray-400">
        <AlertCircle :size="48" class="mb-3 opacity-50" />
        <p class="text-sm">请选择节点或连线</p>
      </div>

      <div v-else-if="selectedType === 'node' && selectedNode" class="space-y-4">
        <div class="flex items-center gap-2 text-sm font-medium text-gray-700 pb-2 border-b border-gray-100">
          <div
            class="w-3 h-3 rounded-full"
            :style="{ backgroundColor: nodeColor }"
          />
          节点属性
        </div>

        <div class="space-y-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1">名称</label>
            <input
              v-model="nodeName"
              type="text"
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="请输入节点名称"
              @change="saveNodeChanges"
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1">类型</label>
            <select
              v-model="nodeType"
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              @change="saveNodeChanges"
            >
              <option v-for="opt in nodeTypeOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>

          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1">颜色</label>
            <div class="flex items-center gap-2">
              <input
                v-model="nodeColor"
                type="color"
                class="w-10 h-10 rounded border border-gray-300 cursor-pointer"
                @change="saveNodeChanges"
              />
              <input
                v-model="nodeColor"
                type="text"
                class="flex-1 px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                @change="saveNodeChanges"
              />
            </div>
          </div>

          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="text-xs font-medium text-gray-600">权限配置</label>
              <button
                type="button"
                class="text-xs text-blue-600 hover:text-blue-700"
                @click="addPermission"
              >
                + 添加
              </button>
            </div>
            <div class="space-y-2">
              <div
                v-for="(perm, index) in nodePermissions"
                :key="index"
                class="flex items-center gap-2"
              >
                <input
                  :value="perm"
                  type="text"
                  class="flex-1 px-3 py-1.5 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="权限标识"
                  @input="(e) => updatePermission(index, (e.target as HTMLInputElement).value)"
                  @change="saveNodeChanges"
                />
                <button
                  type="button"
                  class="p-1 text-gray-400 hover:text-red-500 transition-colors"
                  @click="removePermission(index); saveNodeChanges()"
                >
                  <Trash2 :size="16" />
                </button>
              </div>
              <div v-if="nodePermissions.length === 0" class="text-xs text-gray-400 py-2 text-center">
                暂无权限配置
              </div>
            </div>
          </div>
        </div>

        <div class="pt-4 border-t border-gray-100">
          <button
            type="button"
            class="w-full px-4 py-2 text-sm text-white bg-red-500 hover:bg-red-600 rounded-md flex items-center justify-center gap-2 transition-colors"
            @click="deleteSelected"
          >
            <Trash2 :size="16" />
            删除节点
          </button>
        </div>
      </div>

      <div v-else-if="selectedType === 'edge' && selectedEdge" class="space-y-4">
        <div class="flex items-center gap-2 text-sm font-medium text-gray-700 pb-2 border-b border-gray-100">
          <GitBranch :size="16" class="text-blue-500" />
          连线属性
        </div>

        <div class="space-y-3">
          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1">名称</label>
            <input
              v-model="edgeName"
              type="text"
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="请输入连线名称"
              @change="saveEdgeChanges"
            />
          </div>

          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1">
              转移条件 (SpEL)
            </label>
            <textarea
              v-model="edgeCondition"
              rows="4"
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent font-mono"
              placeholder="例如: #amount > 1000"
              @change="saveEdgeChanges"
            />
            <div class="text-xs text-gray-400 mt-1">
              支持 SpEL 表达式，如: #amount > 1000
            </div>
          </div>

          <div>
            <label class="block text-xs font-medium text-gray-600 mb-1">触发源</label>
            <select
              v-model="edgeTriggerSource"
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              @change="saveEdgeChanges"
            >
              <option v-for="opt in triggerSourceOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>

          <div v-if="edgeTriggerSource === TriggerSource.CALLBACK">
            <label class="block text-xs font-medium text-gray-600 mb-1">回调源标识</label>
            <input
              v-model="edgeCallbackSource"
              type="text"
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="例如: payment_callback"
              @change="saveEdgeChanges"
            />
          </div>
        </div>

        <div class="pt-4 border-t border-gray-100">
          <button
            type="button"
            class="w-full px-4 py-2 text-sm text-white bg-red-500 hover:bg-red-600 rounded-md flex items-center justify-center gap-2 transition-colors"
            @click="deleteSelected"
          >
            <Trash2 :size="16" />
            删除连线
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
