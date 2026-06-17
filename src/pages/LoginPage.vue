<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from 'lucide-vue-next'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()

const form = ref({
  username: '',
  password: '',
})

const loading = ref(false)

const handleLogin = async () => {
  if (form.value.username.trim() === '') {
    ElMessage.warning('请输入用户名')
    return
  }
  if (form.value.password.trim() === '') {
    ElMessage.warning('请输入密码')
    return
  }

  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
    appStore.login(form.value.username, form.value.password)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect as string | undefined
    router.push(redirect || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1 class="title">工单状态机引擎</h1>
        <p class="subtitle">Ticket State Machine Engine</p>
      </div>
      <el-form :model="form" class="login-form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="login-button"
          :loading="loading"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #020617 0%, #0f172a 50%, #1e293b 100%);
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  top: -200px;
  right: -200px;
  animation: float 8s ease-in-out infinite;
}

.login-container::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  bottom: -100px;
  left: -100px;
  animation: float 10s ease-in-out infinite reverse;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(30px);
  }
}

.login-box {
  width: 420px;
  padding: 48px;
  background: rgba(15, 23, 42, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5), 0 0 40px rgba(59, 130, 246, 0.1);
  position: relative;
  z-index: 10;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px 0;
  background: linear-gradient(135deg, #60a5fa 0%, #38bdf8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
  letter-spacing: 1px;
}

.login-form {
  margin-top: 32px;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6 0%, #0ea5e9 100%);
  border: none;
  margin-top: 8px;
}

.login-button:hover {
  background: linear-gradient(135deg, #2563eb 0%, #0284c7 100%);
  box-shadow: 0 10px 30px rgba(59, 130, 246, 0.4);
}
</style>
