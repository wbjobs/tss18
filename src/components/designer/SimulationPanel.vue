<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Play, RotateCcw, ChevronRight, CheckCircle2, XCircle } from 'lucide-vue-next'
import { runSimulation } from '@/api/simulation'
import type { SimulationRequest, SimulationResult, SimulationPath } from '@/types'
import type { StateNodeData } from '@/types/designer'

const props = defineProps<{
  stateMachineId: number
  nodes: StateNodeData[]
}>()

const emit = defineEmits<{
  (e: 'highlight-path', path: SimulationPath): void
  (e: 'highlight-reset'): void
}>()

const currentStateId = ref('')
const triggerSource = ref<'MANUAL' | 'CALLBACK' | 'AUTO' | ''>('')
const callbackSource = ref('')
const triggerDataJson = ref('{}')
const running = ref(false)
const result = ref<SimulationResult | null>(null)

const nodeOptions = computed(() =>
  (props.nodes || []).map(n => ({ value: n.id, label: n.name }))
)

const showCallbackSource = computed(() => triggerSource.value === 'CALLBACK')

let parsedTriggerData: Record<string, any> = {}
try {
  parsedTriggerData = JSON.parse(triggerDataJson.value)
} catch {
  parsedTriggerData = {}
}

const isMatched = (path: SimulationPath) => {
  if (!result.value) return false
  return result.value.matchedPaths.some(m => m.transitionId === path.transitionId)
}

const handlePathHover = (path: SimulationPath) => {
  if (isMatched(path)) {
    emit('highlight-path', path)
  }
}

const handlePathLeave = () => {
  emit('highlight-reset')
}

const handleRun = async () => {
  if (!currentStateId.value) {
    ElMessage.warning('请选择当前状态')
    return
  }
  if (!triggerSource.value) {
    ElMessage.warning('请选择触发源')
    return
  }
  if (triggerSource.value === 'CALLBACK' && !callbackSource.value.trim()) {
    ElMessage.warning('请输入回调源标识')
    return
  }

  let data: Record<string, any> = {}
  try {
    data = JSON.parse(triggerDataJson.value)
  } catch {
    ElMessage.warning('触发数据JSON格式错误')
    return
  }

  const req: SimulationRequest = {
    stateMachineId: props.stateMachineId,
    currentStateId: currentStateId.value,
    triggerSource: triggerSource.value,
    callbackSource: callbackSource.value,
    triggerData: data,
  }

  running.value = true
  try {
    const res = await runSimulation(req)
    if (res.code === 200) {
      result.value = res.data
      emit('highlight-reset')
      ElMessage.success('仿真运行完成')
    } else {
      ElMessage.error(res.message || '仿真运行失败')
    }
  } catch (err: any) {
    ElMessage.error('仿真运行失败: ' + (err.message || '未知错误'))
  } finally {
    running.value = false
  }
}

const handleReset = () => {
  currentStateId.value = ''
  triggerSource.value = ''
  callbackSource.value = ''
  triggerDataJson.value = '{}'
  result.value = null
  emit('highlight-reset')
}
</script>

<template>
  <div class="simulation-panel">
    <div class="panel-section">
      <div class="section-title">仿真输入</div>

      <div class="form-group">
        <label class="form-label">当前状态</label>
        <el-select
          v-model="currentStateId"
          placeholder="选择当前状态"
          clearable
          class="full-width"
        >
          <el-option
            v-for="opt in nodeOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </div>

      <div class="form-group">
        <label class="form-label">触发源</label>
        <el-select
          v-model="triggerSource"
          placeholder="选择触发源"
          clearable
          class="full-width"
        >
          <el-option label="手动触发 (MANUAL)" value="MANUAL" />
          <el-option label="回调触发 (CALLBACK)" value="CALLBACK" />
          <el-option label="自动触发 (AUTO)" value="AUTO" />
        </el-select>
      </div>

      <div v-if="showCallbackSource" class="form-group">
        <label class="form-label">回调源标识</label>
        <el-input
          v-model="callbackSource"
          placeholder="输入回调源标识"
          clearable
        />
      </div>

      <div class="form-group">
        <label class="form-label">触发数据 (JSON)</label>
        <el-input
          v-model="triggerDataJson"
          type="textarea"
          :rows="4"
          placeholder='{"key": "value"}'
          class="code-input"
        />
      </div>

      <div class="btn-group">
        <el-button
          type="primary"
          :loading="running"
          @click="handleRun"
          class="run-btn"
        >
          <Play :size="16" style="margin-right: 6px" />
          运行仿真
        </el-button>
        <el-button @click="handleReset">
          <RotateCcw :size="14" style="margin-right: 4px" />
          重置
        </el-button>
      </div>
    </div>

    <div v-if="result" class="panel-section">
      <div class="section-title">仿真结果</div>

      <div class="result-current">
        <span class="result-label">当前状态：</span>
        <span class="result-value highlight">{{ result.currentStateName }}</span>
        <span class="result-id">({{ result.currentStateId }})</span>
      </div>

      <div class="paths-section">
        <div class="paths-header">可能路径 ({{ result.possiblePaths.length }})</div>
        <div class="paths-list">
          <div
            v-for="path in result.possiblePaths"
            :key="path.transitionId"
            class="path-item"
            :class="{ matched: isMatched(path), unmatched: !isMatched(path) }"
            @mouseenter="handlePathHover(path)"
            @mouseleave="handlePathLeave"
          >
            <div class="path-top">
              <span class="path-from">{{ path.fromStateName }}</span>
              <ChevronRight :size="16" class="path-arrow" />
              <span class="path-to">{{ path.toStateName }}</span>
              <span class="path-name">[{{ path.transitionName }}]</span>
            </div>
            <div class="path-condition">
              <span class="condition-label">条件：</span>
              <code class="condition-expr">{{ path.condition || '(无)' }}</code>
              <span class="condition-result" :class="path.conditionMet ? 'met' : 'not-met'">
                <CheckCircle2 v-if="path.conditionMet" :size="14" />
                <XCircle v-else :size="14" />
                {{ path.conditionMet ? '满足' : '不满足' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="result.finalStates.length > 0" class="final-states">
        <div class="paths-header">可达最终状态</div>
        <div class="final-tags">
          <el-tag
            v-for="state in result.finalStates"
            :key="state"
            type="success"
            effect="dark"
            size="small"
            class="final-tag"
          >
            {{ state }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.simulation-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  overflow-y: auto;
  padding: 16px;
}

.panel-section {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 10px;
  padding: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #e2e8f0;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.form-group {
  margin-bottom: 12px;
}

.form-label {
  display: block;
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 6px;
}

.full-width {
  width: 100%;
}

.code-input :deep(.el-textarea__inner) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
  background: rgba(2, 6, 23, 0.8);
  color: #93c5fd;
  border-color: rgba(59, 130, 246, 0.2);
}

.btn-group {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.run-btn {
  background: linear-gradient(135deg, #3b82f6, #2563eb) !important;
  border: none !important;
}

.result-current {
  padding: 10px 12px;
  background: rgba(59, 130, 246, 0.08);
  border-radius: 8px;
  margin-bottom: 14px;
}

.result-label {
  font-size: 13px;
  color: #94a3b8;
}

.result-value.highlight {
  color: #60a5fa;
  font-weight: 600;
}

.result-id {
  font-size: 12px;
  color: #64748b;
  margin-left: 4px;
}

.paths-section {
  margin-bottom: 14px;
}

.paths-header {
  font-size: 13px;
  font-weight: 600;
  color: #cbd5e1;
  margin-bottom: 8px;
}

.paths-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.path-item {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid transparent;
  transition: all 0.2s;
  cursor: pointer;
}

.path-item.matched {
  background: rgba(16, 185, 129, 0.08);
  border-color: rgba(16, 185, 129, 0.25);
}

.path-item.unmatched {
  background: rgba(100, 116, 139, 0.06);
  border-color: rgba(100, 116, 139, 0.12);
  opacity: 0.65;
}

.path-item:hover {
  transform: translateX(2px);
}

.path-top {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.path-from,
.path-to {
  font-size: 13px;
  font-weight: 500;
  color: #e2e8f0;
}

.path-arrow {
  color: #3b82f6;
  flex-shrink: 0;
}

.path-name {
  font-size: 12px;
  color: #64748b;
  margin-left: 4px;
}

.path-condition {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.condition-label {
  color: #64748b;
}

.condition-expr {
  background: rgba(2, 6, 23, 0.6);
  padding: 2px 6px;
  border-radius: 4px;
  color: #93c5fd;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.condition-result {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-weight: 500;
}

.condition-result.met {
  color: #10b981;
}

.condition-result.not-met {
  color: #ef4444;
}

.final-states {
  margin-top: 4px;
}

.final-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.final-tag {
  background: rgba(16, 185, 129, 0.15) !important;
  border-color: rgba(16, 185, 129, 0.3) !important;
}

:deep(.el-select),
:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  background: rgba(2, 6, 23, 0.6) !important;
  border-color: rgba(59, 130, 246, 0.2) !important;
  color: #e2e8f0 !important;
}

:deep(.el-select .el-input__wrapper) {
  background: rgba(2, 6, 23, 0.6) !important;
}
</style>
