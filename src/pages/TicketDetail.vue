<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Clock, User, FileText, History, BarChart3 } from 'lucide-vue-next'
import GanttChart from '@/components/common/GanttChart.vue'
import { getGanttData } from '@/api/gantt'
import type { GanttItem } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const ticket = ref<any>(null)
const timeline = ref([
  { id: 1, state: '已创建', time: '2024-01-15 10:30', operator: '张三', description: '工单已提交' },
  { id: 2, state: '待审核', time: '2024-01-15 10:35', operator: '系统', description: '进入审核流程' },
  { id: 3, state: '部门审批', time: '2024-01-15 14:20', operator: '李四', description: '部门经理已审批通过' },
  { id: 4, state: '财务审核', time: '2024-01-15 16:00', operator: '王五', description: '财务审核中' },
])

const ticketId = route.params.id
const activeTab = ref('timeline')
const ganttItems = ref<GanttItem[]>([])
const ganttLoading = ref(false)

onMounted(() => {
  loading.value = true
  setTimeout(() => {
    ticket.value = {
      id: ticketId,
      title: `订单审批 #${ticketId}`,
      status: 'processing',
      stateMachine: '订单流程',
      currentState: '财务审核',
      createdAt: '2024-01-15 10:30',
      applicant: '张三',
      amount: '¥12,500.00',
      description: '采购办公设备一批，包括电脑、打印机等',
    }
    loading.value = false
  }, 500)
})

async function loadGanttData() {
  const id = Number(ticketId)
  if (!id) return
  ganttLoading.value = true
  try {
    const res = await getGanttData(id)
    if (res.code === 200) {
      ganttItems.value = res.data || []
    }
  } catch {
    ganttItems.value = []
  } finally {
    ganttLoading.value = false
  }
}

function handleTabChange(tab: string | number) {
  if (tab === 'gantt' && ganttItems.value.length === 0) {
    loadGanttData()
  }
}

const goBack = () => {
  router.back()
}

const getStatusTag = (status: string) => {
  const statusMap: Record<string, { type: string; text: string }> = {
    pending: { type: 'warning', text: '待处理' },
    processing: { type: 'primary', text: '处理中' },
    completed: { type: 'success', text: '已完成' },
    rejected: { type: 'danger', text: '已拒绝' },
  }
  return statusMap[status] || { type: 'info', text: status }
}
</script>

<template>
  <div class="ticket-detail">
    <div class="page-header">
      <div class="header-left">
        <el-button link :icon="ArrowLeft" @click="goBack" class="back-btn">
          返回
        </el-button>
        <div>
          <h2 class="page-title">工单详情</h2>
          <p class="page-subtitle">查看工单的完整信息和状态流转</p>
        </div>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="16">
        <div class="panel">
          <div class="panel-header">
            <h3 class="panel-title">基本信息</h3>
          </div>
          <div class="panel-body">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="工单标题">
                <span class="detail-value">{{ ticket?.title }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="工单状态">
                <el-tag :type="getStatusTag(ticket?.status).type" effect="dark">
                  {{ getStatusTag(ticket?.status).text }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="所属流程">
                {{ ticket?.stateMachine }}
              </el-descriptions-item>
              <el-descriptions-item label="当前状态">
                <span class="current-state">{{ ticket?.currentState }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="申请人">
                <div class="info-row">
                  <User :size="16" />
                  {{ ticket?.applicant }}
                </div>
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                <div class="info-row">
                  <Clock :size="16" />
                  {{ ticket?.createdAt }}
                </div>
              </el-descriptions-item>
              <el-descriptions-item label="金额" span="2">
                <span class="amount">{{ ticket?.amount }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="详情描述" span="2">
                {{ ticket?.description }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>
      </el-col>

      <el-col :span="8">
        <div class="panel">
          <div class="panel-header">
            <h3 class="panel-title">
              <History :size="18" />
              状态流转
            </h3>
          </div>
          <div class="panel-body">
            <el-tabs v-model="activeTab" class="trace-tabs" @tab-change="handleTabChange">
              <el-tab-pane label="时间线" name="timeline">
                <el-timeline>
                  <el-timeline-item
                    v-for="(item, index) in timeline"
                    :key="item.id"
                    :timestamp="item.time"
                    :type="index === timeline.length - 1 ? 'primary' : ''"
                  >
                    <div class="timeline-content">
                      <div class="timeline-state">{{ item.state }}</div>
                      <div class="timeline-operator">{{ item.operator }}</div>
                      <div class="timeline-desc">{{ item.description }}</div>
                    </div>
                  </el-timeline-item>
                </el-timeline>
              </el-tab-pane>
              <el-tab-pane name="gantt">
                <template #label>
                  <span class="gantt-tab-label">
                    <BarChart3 :size="14" />
                    Gantt图
                  </span>
                </template>
                <div v-loading="ganttLoading" class="gantt-wrapper">
                  <GanttChart :items="ganttItems" :height="240" />
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.ticket-detail {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.back-btn {
  margin-top: 4px;
  padding: 0;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #f1f5f9;
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.panel {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(59, 130, 246, 0.1);
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-body {
  padding: 20px;
}

.detail-value {
  font-weight: 500;
  color: #f1f5f9;
}

.current-state {
  color: #3b82f6;
  font-weight: 500;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e2e8f0;
}

.amount {
  font-size: 20px;
  font-weight: 700;
  color: #10b981;
}

.timeline-content {
  color: #e2e8f0;
}

.timeline-state {
  font-weight: 600;
  margin-bottom: 4px;
}

.timeline-operator {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 2px;
}

.timeline-desc {
  font-size: 12px;
  color: #64748b;
}

:deep(.el-descriptions) {
  --el-descriptions-item-label-color: #94a3b8;
  --el-descriptions-item-content-color: #e2e8f0;
  --el-descriptions-border-color: rgba(59, 130, 246, 0.1);
}

:deep(.el-descriptions__header) {
  background: rgba(2, 6, 23, 0.6);
}

:deep(.el-descriptions__label),
:deep(.el-descriptions__content) {
  background: transparent;
}

:deep(.el-timeline-item__timestamp) {
  color: #64748b;
}

:deep(.el-timeline-item__wrapper) {
  padding-bottom: 20px;
}

.gantt-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.trace-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(59, 130, 246, 0.1);
}

.trace-tabs :deep(.el-tabs__item) {
  color: #94a3b8;
}

.trace-tabs :deep(.el-tabs__item.is-active) {
  color: #60a5fa;
}

.trace-tabs :deep(.el-tabs__active-bar) {
  background-color: #3b82f6;
}

.gantt-wrapper {
  min-height: 200px;
}
</style>
