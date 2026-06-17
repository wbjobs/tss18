<script setup lang="ts">
import { computed } from 'vue'
import {
  Save,
  Rocket,
  Eye,
  Undo2,
  Redo2,
  Trash2,
  Upload,
  Download,
  ZoomIn,
  ZoomOut,
  Maximize2,
  Grid3X3,
  Magnet,
} from 'lucide-vue-next'
import { useDesignerStore } from '@/stores/designer'

const props = defineProps<{
  zoom: number
}>()

const emit = defineEmits<{
  (e: 'save'): void
  (e: 'publish'): void
  (e: 'preview'): void
  (e: 'undo'): void
  (e: 'redo'): void
  (e: 'clear'): void
  (e: 'import'): void
  (e: 'export'): void
  (e: 'zoomIn'): void
  (e: 'zoomOut'): void
  (e: 'zoomReset'): void
  (e: 'zoomFit'): void
  (e: 'toggleGrid'): void
  (e: 'toggleSnap'): void
}>()

const designerStore = useDesignerStore()

const zoomPercent = computed(() => Math.round(props.zoom * 100) + '%')

const canUndo = computed(() => designerStore.canUndo)
const canRedo = computed(() => designerStore.canRedo)
const gridEnabled = computed(() => designerStore.gridEnabled)
const snapEnabled = computed(() => designerStore.snapEnabled)
</script>

<template>
  <div class="h-14 bg-white border-b border-gray-200 flex items-center justify-between px-4">
    <div class="flex items-center gap-1">
      <div class="flex items-center gap-1 pr-4 border-r border-gray-200">
        <button
          type="button"
          class="px-3 py-2 text-sm text-white bg-blue-500 hover:bg-blue-600 rounded-md flex items-center gap-1.5 transition-colors"
          @click="emit('save')"
        >
          <Save :size="16" />
          保存
        </button>
        <button
          type="button"
          class="px-3 py-2 text-sm text-white bg-green-500 hover:bg-green-600 rounded-md flex items-center gap-1.5 transition-colors"
          @click="emit('publish')"
        >
          <Rocket :size="16" />
          发布
        </button>
        <button
          type="button"
          class="px-3 py-2 text-sm text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md flex items-center gap-1.5 transition-colors"
          @click="emit('preview')"
        >
          <Eye :size="16" />
          预览
        </button>
      </div>

      <div class="flex items-center gap-1 px-4 border-r border-gray-200">
        <button
          type="button"
          :disabled="!canUndo"
          class="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          title="撤销 (Ctrl+Z)"
          @click="emit('undo')"
        >
          <Undo2 :size="18" />
        </button>
        <button
          type="button"
          :disabled="!canRedo"
          class="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-md transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          title="重做 (Ctrl+Y)"
          @click="emit('redo')"
        >
          <Redo2 :size="18" />
        </button>
        <button
          type="button"
          class="p-2 text-gray-600 hover:text-red-600 hover:bg-red-50 rounded-md transition-colors"
          title="清空画布"
          @click="emit('clear')"
        >
          <Trash2 :size="18" />
        </button>
        <button
          type="button"
          class="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-md transition-colors"
          title="导入"
          @click="emit('import')"
        >
          <Upload :size="18" />
        </button>
        <button
          type="button"
          class="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-md transition-colors"
          title="导出"
          @click="emit('export')"
        >
          <Download :size="18" />
        </button>
      </div>

      <div class="flex items-center gap-1 px-4 border-r border-gray-200">
        <button
          type="button"
          class="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-md transition-colors"
          title="缩小"
          @click="emit('zoomOut')"
        >
          <ZoomOut :size="18" />
        </button>
        <button
          type="button"
          class="px-3 py-1.5 text-sm text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md min-w-[60px] text-center transition-colors"
          title="重置缩放"
          @click="emit('zoomReset')"
        >
          {{ zoomPercent }}
        </button>
        <button
          type="button"
          class="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-md transition-colors"
          title="放大"
          @click="emit('zoomIn')"
        >
          <ZoomIn :size="18" />
        </button>
        <button
          type="button"
          class="p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-md transition-colors"
          title="适应窗口"
          @click="emit('zoomFit')"
        >
          <Maximize2 :size="18" />
        </button>
      </div>

      <div class="flex items-center gap-1 pl-4">
        <button
          type="button"
          :class="[
            'p-2 rounded-md transition-colors',
            gridEnabled
              ? 'text-blue-600 bg-blue-50 hover:bg-blue-100'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100',
          ]"
          title="显示网格"
          @click="emit('toggleGrid')"
        >
          <Grid3X3 :size="18" />
        </button>
        <button
          type="button"
          :class="[
            'p-2 rounded-md transition-colors',
            snapEnabled
              ? 'text-blue-600 bg-blue-50 hover:bg-blue-100'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100',
          ]"
          title="磁吸对齐"
          @click="emit('toggleSnap')"
        >
          <Magnet :size="18" />
        </button>
      </div>
    </div>

    <div class="flex items-center gap-2 text-sm text-gray-500">
      <span>节点: {{ designerStore.nodes.length }}</span>
      <span class="text-gray-300">|</span>
      <span>连线: {{ designerStore.transitions.length }}</span>
    </div>
  </div>
</template>
