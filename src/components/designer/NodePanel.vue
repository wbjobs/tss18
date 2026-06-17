<script setup lang="ts">
import { PlayCircle, Circle, StopCircle } from 'lucide-vue-next'
import { NODE_PANEL_ITEMS } from '@/types/designer'
import type { NodeType } from '@/types/designer'

function handleDragStart(event: DragEvent, nodeType: NodeType) {
  if (event.dataTransfer) {
    event.dataTransfer.setData('application/x-node-type', nodeType)
    event.dataTransfer.effectAllowed = 'copy'
  }
}

const iconComponents: Record<string, any> = {
  PlayCircle,
  Circle,
  StopCircle,
}
</script>

<template>
  <div class="w-56 bg-white border-r border-gray-200 flex flex-col h-full">
    <div class="p-4 border-b border-gray-200">
      <h3 class="text-sm font-semibold text-gray-700">节点面板</h3>
    </div>
    <div class="p-4 flex-1 overflow-y-auto">
      <div class="space-y-3">
        <div
          v-for="item in NODE_PANEL_ITEMS"
          :key="item.type"
          draggable="true"
          @dragstart="(e) => handleDragStart(e, item.type)"
          class="flex items-center gap-3 p-3 rounded-lg border border-gray-200 bg-gray-50 cursor-grab hover:bg-gray-100 hover:border-gray-300 transition-all duration-200 active:cursor-grabbing"
        >
          <div
            class="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0"
            :style="{ backgroundColor: item.color + '20' }"
          >
            <component
              :is="iconComponents[item.icon]"
              :size="20"
              :color="item.color"
            />
          </div>
          <div class="flex-1 min-w-0">
            <div class="text-sm font-medium text-gray-700">{{ item.name }}</div>
            <div class="text-xs text-gray-400 mt-0.5">拖拽到画布</div>
          </div>
        </div>
      </div>

      <div class="mt-6 p-3 bg-blue-50 rounded-lg">
        <div class="text-xs font-medium text-blue-700 mb-2">使用提示</div>
        <ul class="text-xs text-blue-600 space-y-1">
          <li>• 拖拽节点到画布创建</li>
          <li>• 点击节点锚点拖拽连线</li>
          <li>• 点击选中节点/连线</li>
          <li>• Delete 键删除选中项</li>
          <li>• Ctrl+Z 撤销，Ctrl+Y 重做</li>
        </ul>
      </div>
    </div>
  </div>
</template>
