// src/stores/user.js
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 定义状态：用户信息
  // 优先从浏览器缓存里拿，防止刷新页面后数据丢失
  const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))

  // 定义动作：设置用户
  function setUser(userData) {
    user.value = userData
    // 同步保存到浏览器缓存
    localStorage.setItem('user', JSON.stringify(userData))
  }

  // 定义动作：退出登录
  function logout() {
    user.value = {}
    localStorage.removeItem('user')
  }

  return { user, setUser, logout }
})