<!--
  Copyright (c) 2026 bmoqing
  All rights reserved.
  本代码仅供学习参考，未经许可不得用于商业用途。
-->

<template>
  <div class="container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人信息设置</span>
        </div>
      </template>

      <el-form :model="form" label-width="100px" style="max-width: 480px">
        <el-form-item label="账号">
          <el-input :model-value="userStore.user.username" disabled />
        </el-form-item>

        <el-form-item label="角色">
          <el-input :model-value="formatRole(userStore.user.role)" disabled />
        </el-form-item>

        <el-divider content-position="left">修改姓名</el-divider>

        <el-form-item label="姓名">
          <el-input v-model="form.name" placeholder="请输入新姓名" maxlength="30" show-word-limit clearable />
        </el-form-item>

        <el-divider content-position="left">修改密码（选填）</el-divider>

        <el-form-item label="原密码">
          <el-input v-model="form.oldPassword" type="password" show-password placeholder="修改密码时必填" />
        </el-form-item>

        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="不修改密码可留空" />
        </el-form-item>

        <el-form-item label="确认新密码">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitProfile">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const form = reactive({
  name: userStore.user.name || '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const formatRole = (role) => {
  switch (role) {
    case 'ADMIN': return '管理员'
    case 'TEACHER': return '教师'
    case 'STUDENT': return '学生'
    case 'COMPANY': return '企业'
    default: return role
  }
}

const submitProfile = async () => {
  const hasNameChange = form.name && form.name.trim() !== userStore.user.name
  const hasPasswordChange = form.newPassword || form.oldPassword

  if (!hasNameChange && !hasPasswordChange) {
    ElMessage.warning('没有修改任何内容')
    return
  }

  if (hasPasswordChange) {
    if (!form.oldPassword) {
      ElMessage.warning('请输入原密码')
      return
    }
    if (!form.newPassword) {
      ElMessage.warning('请输入新密码')
      return
    }
    if (form.newPassword !== form.confirmPassword) {
      ElMessage.warning('两次输入的新密码不一致')
      return
    }
  }

  const payload = {}
  if (hasNameChange) {
    payload.name = form.name.trim()
  }
  if (hasPasswordChange) {
    payload.oldPassword = form.oldPassword
    payload.newPassword = form.newPassword
  }

  try {
    const res = await request.put('/api/user/profile', payload)
    if (res.code === 200) {
      ElMessage.success('修改成功')
      // 更新本地存储的用户信息
      if (res.data && res.data.name) {
        userStore.user.name = res.data.name
        const stored = JSON.parse(localStorage.getItem('user') || '{}')
        stored.name = res.data.name
        localStorage.setItem('user', JSON.stringify(stored))
      }
      form.oldPassword = ''
      form.newPassword = ''
      form.confirmPassword = ''
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch (e) {
    ElMessage.error('系统异常')
  }
}
</script>

<style scoped>
.container {
  padding: 20px;
}

.profile-card {
  max-width: 600px;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
  color: #1f3148;
}
</style>
