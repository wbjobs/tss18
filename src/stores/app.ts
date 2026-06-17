import { defineStore } from 'pinia'

export interface UserInfo {
  username: string
  [key: string]: any
}

export const useAppStore = defineStore('app', {
  state: () => ({
    tenantId: 0,
    token: '',
    userInfo: null as UserInfo | null,
    collapsed: false,
  }),
  getters: {
    isLoggedIn: (state) => !!state.token || !!localStorage.getItem('token'),
  },
  actions: {
    setTenantId(tenantId: number) {
      this.tenantId = tenantId
      localStorage.setItem('tenantId', String(tenantId))
    },
    setToken(token: string) {
      this.token = token
      if (token) {
        localStorage.setItem('token', token)
      } else {
        localStorage.removeItem('token')
      }
    },
    setUserInfo(userInfo: UserInfo) {
      this.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },
    login(username?: string, password?: string) {
      const token = 'demo-token-' + Date.now()
      this.token = token
      this.userInfo = { username: username || 'admin' }
      this.tenantId = 1
      localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      localStorage.setItem('tenantId', '1')
    },
    logout() {
      this.token = ''
      this.userInfo = null
      this.tenantId = 0
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('tenantId')
    },
    toggleCollapsed() {
      this.collapsed = !this.collapsed
    },
    initAuth() {
      const token = localStorage.getItem('token')
      const userInfoStr = localStorage.getItem('userInfo')
      const tenantIdStr = localStorage.getItem('tenantId')
      if (token) {
        this.token = token
        try {
          this.userInfo = userInfoStr ? JSON.parse(userInfoStr) : { username: 'admin' }
        } catch {
          this.userInfo = { username: 'admin' }
        }
        this.tenantId = tenantIdStr ? parseInt(tenantIdStr, 10) : 1
      }
    },
  },
})
