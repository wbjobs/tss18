<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  LayoutDashboard, Workflow, Ticket, Webhook } from 'lucide-vue-next'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const route = useRoute()
const router = useRouter()

const menuItems = [
  {
    path: '/dashboard',
    title: 'Dashboard',
    icon: LayoutDashboard,
  },
  {
    path: '/state-machine',
    title: 'State Machine',
    icon: Workflow,
  },
  {
    path: '/tickets',
    title: 'Tickets',
    icon: Ticket,
  },
  {
    path: '/webhook',
    title: 'Webhook',
    icon: Webhook,
  },
]

const activeMenu = computed(() => route.path)

const handleMenuClick = (path: string) => {
  router.push(path)
}
</script>

<template>
  <el-aside
    :width="appStore.collapsed ? '64px' : '220px'"
    class="sidebar"
  >
    <div class="logo">
      <div class="logo-icon">T</div>
      <transition name="fade">
        <span v-if="!appStore.collapsed" class="logo-text">
          Ticket Engine
        </span>
      </transition>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="appStore.collapsed"
      class="sidebar-menu"
      background-color="#0a1628"
      text-color="#a0aec0"
      active-text-color="#3b82f6"
    >
      <el-menu-item
        v-for="item in menuItems"
        :key="item.path"
        :index="item.path"
        @click="handleMenuClick(item.path)"
      >
        <component :is="item.icon" class="menu-icon" :size="18" />
        <template #title>{{ item.title }}</template>
      </el-menu-item>
    </el-menu>
  </el-aside>
</template>

<style scoped>
.sidebar {
  background: linear-gradient(180deg, #0a1628 0%, #0f172a 100%);
  border-right: 1px solid #1e293b;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-bottom: 1px solid #1e293b;
  gap: 12px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  font-size: 18px;
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  padding: 12px 0;
}

.sidebar-menu .el-menu-item {
  margin: 4px 12px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
}

.sidebar-menu .el-menu-item:hover {
  background-color: rgba(59, 130, 246, 0.1);
}

.sidebar-menu .el-menu-item.is-active {
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.2) 0%, rgba(59, 130, 246, 0.05) 100%);
  color: #3b82f6;
}

.menu-icon {
  margin-right: 12px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
