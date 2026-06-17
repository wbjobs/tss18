<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import type { GanttItem } from '@/types'

const props = withDefaults(defineProps<{
  items: GanttItem[]
  height?: number
}>(), {
  height: 300,
})

const canvasRef = ref<HTMLCanvasElement | null>(null)
const containerRef = ref<HTMLElement | null>(null)
const tooltip = ref<{ show: boolean; x: number; y: number; content: string }>({
  show: false,
  x: 0,
  y: 0,
  content: '',
})

const ROW_HEIGHT = 40
const LEFT_COL_WIDTH = 120
const PADDING = { top: 40, right: 20, bottom: 50, left: 0 }
const BAR_HEIGHT = 22

let animFrameId = 0
let pulsePhase = 0
let resizeObserver: ResizeObserver | null = null

const bottleneckIndex = ref(-1)

function findBottleneck() {
  if (!props.items.length) {
    bottleneckIndex.value = -1
    return
  }
  let maxDuration = 0
  let maxIdx = -1
  props.items.forEach((item, i) => {
    if (item.durationMillis > maxDuration) {
      maxDuration = item.durationMillis
      maxIdx = i
    }
  })
  bottleneckIndex.value = maxIdx
}

function getTotalDuration() {
  if (!props.items.length) return 0
  return props.items.reduce((sum, item) => sum + item.durationMillis, 0)
}

function formatDuration(millis: number): string {
  if (millis < 1000) return `${millis}ms`
  const minutes = Math.floor(millis / 60000)
  const seconds = Math.floor((millis % 60000) / 1000)
  if (minutes === 0) return `${seconds}s`
  return `${minutes}m ${seconds}s`
}

function draw() {
  const canvas = canvasRef.value
  const container = containerRef.value
  if (!canvas || !container) return

  const dpr = window.devicePixelRatio || 1
  const width = container.clientWidth
  const totalRows = props.items.length
  const canvasHeight = Math.max(
    props.height,
    PADDING.top + totalRows * ROW_HEIGHT + PADDING.bottom
  )

  canvas.width = width * dpr
  canvas.height = canvasHeight * dpr
  canvas.style.width = `${width}px`
  canvas.style.height = `${canvasHeight}px`

  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.scale(dpr, dpr)
  ctx.clearRect(0, 0, width, canvasHeight)

  ctx.fillStyle = 'rgba(15, 23, 42, 0.6)'
  ctx.fillRect(0, 0, width, canvasHeight)

  ctx.strokeStyle = 'rgba(59, 130, 246, 0.08)'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(LEFT_COL_WIDTH, 0)
  ctx.lineTo(LEFT_COL_WIDTH, canvasHeight)
  ctx.stroke()

  ctx.fillStyle = '#64748b'
  ctx.font = '11px sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('状态', LEFT_COL_WIDTH / 2, PADDING.top / 2 + 4)
  ctx.fillText('时间轴', (LEFT_COL_WIDTH + width) / 2, PADDING.top / 2 + 4)

  if (!props.items.length) {
    ctx.fillStyle = '#64748b'
    ctx.font = '14px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText('暂无Gantt数据', width / 2, canvasHeight / 2)
    return
  }

  const chartLeft = LEFT_COL_WIDTH + 10
  const chartRight = width - PADDING.right
  const chartWidth = chartRight - chartLeft

  const totalDuration = getTotalDuration()
  const barScale = totalDuration > 0 ? chartWidth / totalDuration : 0

  props.items.forEach((item, i) => {
    const y = PADDING.top + i * ROW_HEIGHT

    ctx.fillStyle = 'rgba(59, 130, 246, 0.05)'
    ctx.fillRect(0, y, width, ROW_HEIGHT)

    ctx.strokeStyle = 'rgba(59, 130, 246, 0.06)'
    ctx.beginPath()
    ctx.moveTo(0, y + ROW_HEIGHT)
    ctx.lineTo(width, y + ROW_HEIGHT)
    ctx.stroke()

    ctx.fillStyle = '#cbd5e1'
    ctx.font = '12px sans-serif'
    ctx.textAlign = 'left'
    const nameText = item.stateName.length > 8 ? item.stateName.slice(0, 8) + '…' : item.stateName
    ctx.fillText(nameText, 10, y + ROW_HEIGHT / 2 + 4)

    const barX = chartLeft
    const barWidth = Math.max(2, item.durationMillis * barScale)
    const barY = y + (ROW_HEIGHT - BAR_HEIGHT) / 2
    const isBottleneck = i === bottleneckIndex.value
    const isCurrent = item.isCurrent

    if (isCurrent) {
      const pulse = Math.sin(pulsePhase) * 0.3 + 0.7
      ctx.save()
      ctx.shadowColor = 'rgba(16, 185, 129, 0.5)'
      ctx.shadowBlur = 8 * pulse
      const grad = ctx.createLinearGradient(barX, barY, barX + barWidth, barY)
      grad.addColorStop(0, '#10b981')
      grad.addColorStop(1, '#34d399')
      ctx.fillStyle = grad
      roundRect(ctx, barX, barY, barWidth, BAR_HEIGHT, 4)
      ctx.fill()
      ctx.restore()

      ctx.strokeStyle = `rgba(16, 185, 129, ${0.4 + pulse * 0.4})`
      ctx.lineWidth = 1.5
      roundRect(ctx, barX, barY, barWidth, BAR_HEIGHT, 4)
      ctx.stroke()
    } else if (isBottleneck) {
      const grad = ctx.createLinearGradient(barX, barY, barX + barWidth, barY)
      grad.addColorStop(0, '#ef4444')
      grad.addColorStop(1, '#f87171')
      ctx.fillStyle = grad
      roundRect(ctx, barX, barY, barWidth, BAR_HEIGHT, 4)
      ctx.fill()

      ctx.fillStyle = '#fecaca'
      ctx.font = 'bold 9px sans-serif'
      ctx.textAlign = 'right'
      ctx.fillText('瓶颈', barX + barWidth - 6, barY + BAR_HEIGHT / 2 + 3)
    } else {
      const grad = ctx.createLinearGradient(barX, barY, barX + barWidth, barY)
      grad.addColorStop(0, '#3b82f6')
      grad.addColorStop(1, '#60a5fa')
      ctx.fillStyle = grad
      roundRect(ctx, barX, barY, barWidth, BAR_HEIGHT, 4)
      ctx.fill()
    }

    ctx.fillStyle = '#e2e8f0'
    ctx.font = '10px sans-serif'
    ctx.textAlign = 'left'
    if (barWidth > 50) {
      ctx.fillText(item.durationDisplay, barX + 8, barY + BAR_HEIGHT / 2 + 3)
    } else {
      ctx.fillText(item.durationDisplay, barX + barWidth + 6, barY + BAR_HEIGHT / 2 + 3)
    }
  })

  const bottomY = PADDING.top + totalRows * ROW_HEIGHT + 8
  ctx.strokeStyle = 'rgba(59, 130, 246, 0.15)'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(LEFT_COL_WIDTH, bottomY)
  ctx.lineTo(width, bottomY)
  ctx.stroke()

  ctx.fillStyle = '#94a3b8'
  ctx.font = '12px sans-serif'
  ctx.textAlign = 'left'
  ctx.fillText(`总耗时：${formatDuration(totalDuration)}`, chartLeft, bottomY + 20)

  if (bottleneckIndex.value >= 0 && bottleneckIndex.value < props.items.length) {
    const bottleneckItem = props.items[bottleneckIndex.value]
    ctx.fillStyle = '#f87171'
    ctx.textAlign = 'right'
    ctx.fillText(
      `瓶颈：${bottleneckItem.stateName} (${bottleneckItem.durationDisplay})`,
      chartRight,
      bottomY + 20
    )
  }
}

function roundRect(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  r: number
) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + h - r)
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  ctx.lineTo(x + r, y + h)
  ctx.quadraticCurveTo(x, y + h, x, y + h - r)
  ctx.lineTo(x, y + r)
  ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
}

function animationLoop() {
  pulsePhase += 0.06
  draw()
  animFrameId = requestAnimationFrame(animationLoop)
}

function handleMouseMove(e: MouseEvent) {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top

  const chartLeft = LEFT_COL_WIDTH + 10
  const chartRight = canvas.clientWidth - PADDING.right

  if (x < chartLeft || x > chartRight || y < PADDING.top) {
    tooltip.value.show = false
    return
  }

  const rowIdx = Math.floor((y - PADDING.top) / ROW_HEIGHT)
  if (rowIdx < 0 || rowIdx >= props.items.length) {
    tooltip.value.show = false
    return
  }

  const item = props.items[rowIdx]
  tooltip.value = {
    show: true,
    x: e.clientX,
    y: e.clientY,
    content: `${item.stateName}\n开始：${item.startTime}\n结束：${item.endTime || '进行中'}\n耗时：${item.durationDisplay}${item.isCurrent ? '\n(当前状态)' : ''}`,
  }
}

function handleMouseLeave() {
  tooltip.value.show = false
}

watch(
  () => props.items,
  () => {
    findBottleneck()
    nextTick(() => draw())
  },
  { deep: true }
)

watch(
  () => props.height,
  () => nextTick(() => draw())
)

onMounted(() => {
  findBottleneck()
  animationLoop()

  resizeObserver = new ResizeObserver(() => {
    nextTick(() => draw())
  })
  if (containerRef.value) {
    resizeObserver.observe(containerRef.value)
  }
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animFrameId)
  resizeObserver?.disconnect()
})
</script>

<template>
  <div ref="containerRef" class="gantt-container">
    <canvas
      ref="canvasRef"
      @mousemove="handleMouseMove"
      @mouseleave="handleMouseLeave"
    />

    <Teleport to="body">
      <div
        v-if="tooltip.show"
        class="gantt-tooltip"
        :style="{ left: tooltip.x + 12 + 'px', top: tooltip.y + 12 + 'px' }"
      >
        <pre class="tooltip-content">{{ tooltip.content }}</pre>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.gantt-container {
  width: 100%;
  position: relative;
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: 10px;
  overflow: hidden;
  background: rgba(15, 23, 42, 0.4);
}

canvas {
  display: block;
  cursor: crosshair;
}

.gantt-tooltip {
  position: fixed;
  z-index: 9999;
  background: rgba(2, 6, 23, 0.95);
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 8px;
  padding: 10px 14px;
  pointer-events: none;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
}

.tooltip-content {
  margin: 0;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #e2e8f0;
  white-space: pre-line;
}
</style>
