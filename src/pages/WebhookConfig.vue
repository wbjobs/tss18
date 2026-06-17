<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Edit, Trash2, RefreshCw, Link, Webhook } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const webhooks = ref([
  { id: '1', name: '订单完成通知', url: 'https://api.example.com/order-complete', events: ['ticket.completed'], status: 'active', createdAt: '2024-01-10 10:00' },
  { id: '2', name: '状态变更回调', url: 'https://api.example.com/state-change', events: ['ticket.state_changed'], status: 'active', createdAt: '2024-01-08 14:30' },
  { id: '3', name: '错误告警通知', url: 'https://api.example.com/error-alert', events: ['ticket.error'], status: 'inactive', createdAt: '2024-01-05 09:15' },
])

const dialogVisible = ref(false)
const form = ref({
  name: '',
  url: '',
  events: [] as string[],
  secret: '',
})

const eventOptions = [
  { value: 'ticket.created', label: '工单创建' },
  { value: 'ticket.state_changed', label: '状态变更' },
  { value: 'ticket.completed', label: '工单完成' },
  { value: 'ticket.error', label: '工单错误' },
  { value: 'webhook.callback_success', label: '回调成功' },
  { value: 'webhook.callback_failed', label: '回调失败' },
]

onMounted(() => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 500)
})

const handleCreate = () => {
  form.value = { name: '', url: '', events: [], secret: '' }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  form.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 Webhook "${row.name}" 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    webhooks.value = webhooks.value.filter((w) => w.id !== row.id)
    ElMessage.success('删除成功')
  } catch {
    // 用户取消
  }
}

const handleSave = () => {
  if (form.value.id) {
    const index = webhooks.value.findIndex((w) => w.id === form.value.id)
    if (index !== -1) {
      webhooks.value[index] = { ...form.value }
    }
    ElMessage.success('更新成功')
  } else {
    webhooks.value.push({
      ...form.value,
      id: Date.now().toString(),
      status: 'active',
      createdAt: new Date().toLocaleString(),
    })
    ElMessage.success('创建成功')
  }
  dialogVisible.value = false
}

const handleTest = async (row: any) => {
  ElMessage.info(`正在测试 Webhook: ${row.name}`)
}

const getStatusTag = (status: string) => {
  return status === 'active'
    ? { type: 'success', text: '启用' }
    : { type: 'danger', text: '禁用' }
}
</script>

<template>
  <div class="webhook-config">
    <div class="page-header">
      <div>
        <h2 class="page-title">Webhook 配置</h2>
        <p class="page-subtitle">配置事件回调和 Webhook 通知</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="handleCreate">
        新建 Webhook
      </el-button>
    </div>

    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="webhooks"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="name" label="名称" min-width="150">
          <template #default="{ row }">
            <div class="name-cell">
              <div class="name-icon">
                <Webhook :size="18" />
              </div>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="url" label="回调地址" min-width="250" show-overflow-tooltip>
          <template #default="{ row }">
          <div class="url-cell">
            <Link :size="14" />
            <span>{{ row.url }}</span>
          </div>
          </template>
        </el-table-column>
        <el-table-column label="监听事件" min-width="200">
          <template #default="{ row }">
            <div class="events-tags">
              <el-tag
              v-for="event in row.events"
              :key="event"
              size="small"
              effect="dark"
              >
                {{ eventOptions.find((e) => e.value === event)?.label || event }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status).type" effect="dark">
              {{ getStatusTag(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                type="success"
                link
                :icon="RefreshCw"
                @click="handleTest(row)"
              >
                测试
              </el-button>
              <el-button
                type="primary"
                link
                :icon="Edit"
                @click="handleEdit(row)"
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

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑 Webhook' : '新建 Webhook'"
      width="500px"
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.url" placeholder="https://" />
        </el-form-item>
        <el-form-item label="事件">
          <el-select
            v-model="form.events"
            multiple
            placeholder="请选择事件"
            style="width: 100%"
          >
            <el-option
              v-for="option in eventOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="密钥">
          <el-input v-model="form.secret" placeholder="可选，用于签名验证" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.webhook-config {
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
  font-weight: 500;
  color: #e2e8f0;
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

.url-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #3b82f6;
  font-size: 13px;
}

.events-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.action-buttons {
  display: flex;
  gap: 4px;
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

:deep(.el-dialog) {
  --el-dialog-bg-color: #0f172a;
  --el-dialog-title-color: #f1f5f9;
  --el-dialog-footer-border-color: rgba(59, 130, 246, 0.1);
}

:deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #94a3b8;
}

:deep(.el-form-item__label) {
  color: #94a3b8;
}

:deep(.el-input__wrapper) {
  background: rgba(2, 6, 23, 0.6);
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.2);
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #3b82f6;
}

:deep(.el-select__wrapper) {
  background: rgba(2, 6, 23, 0.6);
}

:deep(.el-select__wrapper:hover) {
  border-color: #3b82f6;
}

:deep(.el-select__tags-text),
:deep(.el-select__placeholder),
:deep(.el-input__inner) {
  color: #e2e8f0;
}

:deep(.el-select__caret) {
  color: #64748b;
}
</style>
