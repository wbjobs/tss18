<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Activity, Workflow, Ticket, Webhook, TrendingUp, Clock, CheckCircle, AlertCircle } from 'lucide-vue-next'

const router = useRouter()

const stats = ref([
  { title: '状态机总数', value: '128', icon: Workflow, color: '#3b82f6', change: '+12%' },
  { title: '运行中工单', value: '256', icon: Activity, color: '#10b981', change: '+8%' },
  { title: '今日完成', value: '48', icon: CheckCircle, color: '#8b5cf6', change: '+23%' },
  { title: 'Webhook 回调', value: '1,024', icon: Webhook, color: '#f59e0b', change: '+5%' },
])

const recentActivities = ref([
  { id: 1, title: '工单 #1024 状态变更', time: '2分钟前', type: 'success' },
  { id: 2, title: '新状态机 "订单流程" 已创建', time: '15分钟前', type: 'info' },
  { id: 3, title: 'Webhook 回调失败', time: '1小时前', type: 'warning' },
  { id: 4, title: '工单 #1023 已完成', time: '2小时前', type: 'success' },
  { id: 5, title: '状态机 "报销流程" 已更新', time: '3小时前', type: 'info' },
])

const loading = ref(true)

onMounted(() => {
  setTimeout(() => {
    loading.value = false
  }, 500)
})

const goToStateMachine = () => {
  router.push('/state-machine')
}

const goToTicket = () => {
  router.push('/ticket')
}
</script>

<template>
  <div class="dashboard">
    <div class="page-header">
      <div>
        <h2 class="page-title">仪表盘</h2>
        <p class="page-subtitle">系统概览与统计数据</p>
      </div>
    </div>

    <el-row :gutter="20" class="stats-grid">
      <el-col :span="6" v-for="stat in stats" :key="stat.title">
        <div class="stat-card" :style="{ '--card-color': stat.color }">
          <div class="stat-icon">
            <component :is="stat.icon" :size="24" />
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.title }}</div>
            <div class="stat-change">{{ stat.change }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-grid">
      <el-col :span="12">
        <div class="panel">
          <div class="panel-header">
            <h3 class="panel-title">快捷操作</h3>
          </div>
          <div class="panel-body">
            <div class="quick-actions">
              <div class="action-card" @click="goToStateMachine">
                <div class="action-icon workflow">
                  <Workflow :size="28" />
                </div>
                <div class="action-text">
                  <h4>状态机管理</h4>
                  <p>创建和管理状态机流程</p>
                </div>
              </div>
              <div class="action-card" @click="goToTicket">
                <div class="action-icon ticket">
                  <Ticket :size="28" />
                </div>
                <div class="action-text">
                  <h4>工单管理</h4>
                  <p>查看和处理工单</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :span="12">
        <div class="panel">
          <div class="panel-header">
            <h3 class="panel-title">最近活动</h3>
            <span class="panel-action">查看全部</span>
          </div>
          <div class="panel-body">
            <el-skeleton v-if="loading" :rows="5" animated />
            <div v-else class="activity-list">
              <div
                v-for="activity in recentActivities"
                :key="activity.id"
                class="activity-item"
              >
                <div class="activity-icon" :class="activity.type">
                  <component
                    :is="activity.type === 'success' ? CheckCircle : activity.type === 'warning' ? AlertCircle : Activity"
                    :size="18"
                  />
                </div>
                <div class="activity-content">
                  <div class="activity-title">{{ activity.title }}</div>
                  <div class="activity-time">
                    <Clock :size="14" />
                    {{ activity.time }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 0;
}

.page-header {
  margin-bottom: 24px;
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

.stats-grid {
  margin-bottom: 24px;
}

.stat-card {
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.8), rgba(30, 58, 95, 0.4));
  border: 1px solid rgba(59, 130, 246, 0.1);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  border-color: var(--card-color);
  box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.4), 0 0 20px rgba(59, 130, 246, 0.1);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--card-color);
  color: #fff;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #f1f5f9;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px 0;
}

.stat-change {
  font-size: 12px;
  color: #10b981;
  font-weight: 500;
}

.content-grid {
  gap: 0;
}

.panel {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(59, 130, 246, 0.1);
  border-radius: 12px;
  overflow: hidden;
}

.panel-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
  margin: 0;
}

.panel-action {
  font-size: 13px;
  color: #3b82f6;
  cursor: pointer;
}

.panel-body {
  padding: 20px;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: rgba(2, 6, 23, 0.6);
  border: 1px solid rgba(59, 130, 246, 0.1);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-card:hover {
  border-color: #3b82f6;
  background: rgba(59, 130, 246, 0.1);
}

.action-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-icon.workflow {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.action-icon.ticket {
  background: rgba(139, 92, 246, 0.2);
  color: #8b5cf6;
}

.action-text h4 {
  font-size: 15px;
  font-weight: 600;
  color: #f1f5f9;
  margin: 0 0 4px 0;
}

.action-text p {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(2, 6, 23, 0.4);
  border-radius: 8px;
}

.activity-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.activity-icon.success {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.activity-icon.warning {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
}

.activity-icon.info {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-title {
  font-size: 14px;
  color: #e2e8f0;
  margin-bottom: 2px;
}

.activity-time {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
