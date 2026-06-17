<script setup lang="ts">
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'
</script>

<template>
  <el-container class="layout-container">
    <Sidebar />
    <el-container>
      <Header />
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
}

.main-content {
  background: #020617;
  padding: 24px;
  overflow-y: auto;
}

.main-content::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.main-content::-webkit-scrollbar-track {
  background: #0f172a;
}

.main-content::-webkit-scrollbar-thumb {
  background: #334155;
  border-radius: 4px;
}

.main-content::-webkit-scrollbar-thumb:hover {
  background: #475569;
}
</style>
