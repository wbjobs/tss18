<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Edit, Trash2, Play, Clock, CheckCircle, Settings } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useStateMachineStore } from '@/stores/stateMachine'

const router = useRouter()
const stateMachineStore = useStateMachineStore()

const loading = ref(false)
const stateMachines = ref([])

const fetchStateMachines = async () => {
  loading.value = true
  try {
    await stateMachineStore.fetchStateMachines()
    stateMachines.value = stateMachineStore.stateMachines
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchStateMachines()
})

const handleCreate = () => {
  router.push('/state-machine/designer')
}

const handleEdit = (id: string) => {
  router.push(`/state-machine/designer/${id}`)
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除状态机 "${row.name}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await stateMachineStore.remove(row.id)
    ElMessage.success('删除成功')
    fetchStateMachines()
  } catch {
    // 用户取消
  }
}

const getStatusTag = (status: string) => {
  const statusMap: Record<string, { type: string; text: string }> = {
    draft: { type: 'info', text: '草稿' },
    active: { type: 'success', text: '启用' },
    disabled: { type: 'danger', text: '禁用' },
  }
  return statusMap[status] || { type: 'info', text: status }
}

const columns = [
  { prop: 'name', label: '名称', minWidth: 180 },
  { prop: 'description', label: '描述', minWidth: 200 },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    formatter: (row: any) => getStatusTag(row.status).text,
  },
  { prop: 'version', label: '版本', width: 100 },
  { prop: 'createdAt', label: '创建时间', width: 180 },
]
</script>

<template>
  <div class="state-machine-list">
    <div class="page-header">
      <div>
        <h2 class="page-title">状态机列表</h2>
        <p class="page-subtitle">管理和配置工作流状态机</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="handleCreate">
        新建状态机
      </el-button>
    </div>

    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="stateMachines"
        stripe
        style="width: 100%"
        empty-text="暂无数据"
      >
        <el-table-column prop="name" label="名称" min-width="180">
          <template #default="{ row }">
            <div class="name-cell">
              <div class="name-icon">
              <Settings :size="18" />
              </div>
              <div class="name-text">{{ row.name }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status).type" effect="dark">
              {{ getStatusTag(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                type="primary"
                link
                :icon="Edit"
                @click="handleEdit(row.id)"
              >
                编辑
              </el-button>
              <el-button
                type="danger"
                link
                :icon="Trash2"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.state-machine-list {
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

.table-container {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(59, 130, 246, 0.1);
  border-radius: 12px;
  overflow: hidden;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.name-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
}

.name-text {
  font-weight: 500;
  color: #e2e8f0;
}

.action-buttons {
  display: flex;
  gap: 8px;
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
</style>
