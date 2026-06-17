<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Menu, ChevronRight, Building2, User, LogOut,
} from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const route = useRoute()

const userDropdownVisible = ref(false)

const breadcrumbs = computed(() => {
  const pathMap: Record<string, string> = {
    '/dashboard': '仪表盘',
    '/state-machine': '状态机管理',
    '/tickets': '工单管理',
    '/webhook': 'Webhook配置',
  }
  const crumbs = [{ title: '首页', path: '/' }]
  if (route.path !== '/' && pathMap[route.path]) {
    crumbs.push({ title: pathMap[route.path], path: route.path })
  }
  return crumbs
})

const tenants = [
  { id: 1, name: '默认租户' },
  { id: 2, name: '租户A' },
  { id: 3, name: '租户B' },
]

const currentTenant = computed(() => {
  return tenants.find(t => t.id === appStore.tenantId) || tenants[0]
})

const handleToggleCollapse = () => {
  appStore.toggleCollapsed()
}

const handleTenantChange = (tenantId: number) => {
  appStore.setTenantId(tenantId)
  ElMessage.success(`已切换到: ${tenants.find(t => t.id === tenantId)?.name}`)
}

const handleLogout = () => {
  appStore.logout()
  ElMessage.success('已退出登录')
}
</script>

<template>
  <el-header class="header">
    <div class="header-left">
      <button class="collapse-btn" @click="handleToggleCollapse">
        <Menu :size="20" />
      </button>
      <el-breadcrumb separator="/" class="breadcrumb">
        <el-breadcrumb-item
          v-for="(crumb, index) in breadcrumbs"
          :key="index"
          :to="crumb.path"
        >
          {{ crumb.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="header-right">
      <el-dropdown
        trigger="click"
        @command="handleTenantChange"
        class="tenant-dropdown"
      >
        <div class="tenant-selector">
          <Building2 :size="16" />
          <span class="tenant-name">{{ currentTenant.name }}</span>
          <ChevronRight :size="14" class="dropdown-icon" />
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="tenant in tenants"
              :key="tenant.id"
              :command="tenant.id"
              :disabled="tenant.id === appStore.tenantId"
            >
              {{ tenant.name }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <el-dropdown
        v-model:visible="userDropdownVisible"
        trigger="click"
        class="user-dropdown"
      >
        <div class="user-info">
          <div class="avatar">
            <User :size="18" />
          </div>
          <span class="username">Admin</span>
          <ChevronRight :size="14" class="dropdown-icon" />
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="userDropdownVisible = false">
              <div class="dropdown-item">
                <User :size="16" />
                <span>个人中心</span>
              </div>
            </el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">
              <div class="dropdown-item logout">
                <LogOut :size="16" />
                <span>退出登录</span>
              </div>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<style scoped>
.header {
  background: #0f172a;
  border-bottom: 1px solid #1e293b;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  background: transparent;
  border: none;
  color: #a0aec0;
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.collapse-btn:hover {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.breadcrumb :deep(.el-breadcrumb__inner) {
  color: #a0aec0;
}

.breadcrumb :deep(.el-breadcrumb__inner:hover) {
  color: #3b82f6;
}

.breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #fff;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.tenant-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid #334155;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  color: #cbd5e1;
}

.tenant-selector:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.tenant-name {
  font-size: 14px;
}

.dropdown-icon {
  transition: transform 0.2s;
}

.tenant-dropdown:hover .dropdown-icon,
.user-dropdown:hover .dropdown-icon {
  transform: rotate(90deg);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid #334155;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.user-info:hover {
  border-color: #3b82f6;
}

.avatar {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.username {
  color: #e2e8f0;
  font-size: 14px;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #334155;
}

.dropdown-item.logout {
  color: #ef4444;
}
</style>
