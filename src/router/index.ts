import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import Layout from '@/components/layout/Layout.vue'
import { useAppStore } from '@/stores/app'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/pages/LoginPage.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/',
    component: Layout,
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/pages/Dashboard.vue'),
        meta: { title: '仪表盘' },
      },
      {
        path: 'state-machine',
        name: 'state-machine',
        component: () => import('@/pages/StateMachineList.vue'),
        meta: { title: '状态机列表' },
      },
      {
        path: 'state-machine/designer',
        name: 'state-machine-designer-new',
        component: () => import('@/components/designer/StateMachineDesigner.vue'),
        meta: { title: '新建状态机' },
      },
      {
        path: 'state-machine/designer/:id',
        name: 'state-machine-designer',
        component: () => import('@/components/designer/StateMachineDesigner.vue'),
        meta: { title: '状态机设计器' },
      },
      {
        path: 'ticket',
        name: 'ticket',
        component: () => import('@/pages/TicketList.vue'),
        meta: { title: '工单列表' },
      },
      {
        path: 'ticket/detail/:id',
        name: 'ticket-detail',
        component: () => import('@/pages/TicketDetail.vue'),
        meta: { title: '工单详情' },
      },
      {
        path: 'webhook',
        name: 'webhook',
        component: () => import('@/pages/WebhookConfig.vue'),
        meta: { title: 'Webhook配置' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const appStore = useAppStore()
  
  if (!appStore.isLoggedIn) {
    appStore.initAuth()
  }

  document.title = to.meta.title ? `${to.meta.title} - 工单状态机引擎` : '工单状态机引擎'

  if (to.meta.requiresAuth === false) {
    if (appStore.isLoggedIn) {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    if (appStore.isLoggedIn) {
      next()
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }
})

export default router
