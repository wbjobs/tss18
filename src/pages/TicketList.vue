<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus, Eye, Filter } from 'lucide-vue-next'

const router = useRouter()

const loading = ref(false)
const tickets = ref([
  { id: '1', title: '订单审批 #1024', status: 'processing', stateMachine: '订单流程', currentState: '待审核', createdAt: '2024-01-15 10:30' },
  { id: '2', title: '报销申请 #1023', status: 'pending', stateMachine: '报销流程', currentState: '待提交', createdAt: '2024-01-15 09:15' },
  { id: '3', title: '请假申请 #1022', status: 'completed', stateMachine: '请假流程', currentState: '已完成', createdAt: '2024-01-14 16:45' },
  { id: '4', title: '采购申请 #1021', status: 'processing', stateMachine: '采购流程', currentState: '部门审批', createdAt: '2024-01-14 14:20' },
  { id: '5', title: '出差申请 #1020', status: 'rejected', stateMachine: '出差流程', currentState: '已拒绝', createdAt: '2024-01-13 11:00' },
])

const searchQuery = ref('')

onMounted(() => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 500)
})

const handleView = (id: string) => {
  router.push(`/ticket/detail/${id}`)
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
  <div class="ticket-list">
    <div class="page-header">
      <div>
        <h2 class="page-title">工单列表</h2>
        <p class="page-subtitle">查看和处理所有工单</p>
      </div>
      <el-button type="primary" :icon="Plus">
        新建工单
      </el-button>
    </div>

    <div class="filter-bar">
      <div class="search-box">
        <el-input
          v-model="searchQuery"
          placeholder="搜索工单..."
          :prefix-icon="Search"
          clearable
        />
      </div>
      <el-button :icon="Filter">筛选</el-button>
    </div>

    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="tickets"
        stripe
        style="width: 100%"
        @row-click="handleView(row.id)"
      >
        <el-table-column prop="title" label="工单标题" min-width="200">
          <template #default="{ row }">
            <span class="ticket-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stateMachine" label="所属流程" width="150" />
        <el-table-column prop="currentState" label="当前状态" width="150">
          <template #default="{ row }">
            <span class="current-state">{{ row.currentState }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="工单状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status).type" effect="dark">
              {{ getStatusTag(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              :icon="Eye"
              @click.stop="handleView(row.id)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.ticket-list {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.search-box {
  flex: 1;
  max-width: 400px;
}

.table-container {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(59, 130, 246, 0.1);
  border-radius: 12px;
  overflow: hidden;
}

.ticket-title {
  font-weight: 500;
  color: #e2e8f0;
  cursor: pointer;
}

.ticket-title:hover {
  color: #3b82f6;
}

.current-state {
  color: #94a3b8;
  font-size: 13px;
}

:deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(2, 6, 23, 0.6);
  --el-table-row-hover-bg-color: rgba(59, 130, 246, 0.05);
  --el-table-border-color: rgba(59, 130, 246, 0.1);
  --el-table-text-color: #e2e8f0;
  --el-table-header-text-color: #94a3b8;
}

:deep(.el-table th) {
  background: rgba(2, 6, 23, 0.6);
  color: #94a3b8;
}

:deep(.el-table td) {
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(2, 6, 23, 0.3);
}

:deep(.el-table__body tr:hover > td) {
  cursor: pointer;
}
</style>
