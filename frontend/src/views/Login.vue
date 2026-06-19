<template>
  <div class="auth-page">
    <div class="auth-bg auth-bg-left"></div>
    <div class="auth-bg auth-bg-right"></div>

    <div class="auth-shell">
      <section class="auth-intro">
        <h1>高校实习管理平台</h1>
        <p>连接学校、学生与企业的一体化协同工作台。</p>
        <ul>
          <li>统一审批流程，减少线下沟通成本</li>
          <li>过程数据可追踪，支持实时统计与审计</li>
          <li>覆盖协议、评价、异常、申诉全链路</li>
        </ul>
      </section>

      <section class="auth-card">
        <div class="title">账号登录</div>
        <div class="subtitle">请输入账号与密码进入系统</div>

        <el-form :model="form" :rules="rules" ref="formRef">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入账号" size="large">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large">
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-button type="primary" class="login-btn" size="large" @click="handleLogin" :loading="loading">
            登录系统
          </el-button>

          <div class="action-box">
            <el-link type="primary" @click="$router.push('/register')">没有账号？去注册</el-link>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import request from '@/utils/request'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const formRef = ref(null)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await request.post('/api/auth/login', form)
        if (res.code === 200) {
          const token = res.data?.token
          const refreshToken = res.data?.refreshToken
          const user = res.data?.user
          if (!token || !refreshToken || !user?.id) {
            ElMessage.error('登录返回数据异常')
            return
          }
          ElMessage.success('登录成功')
          userStore.setUser({ ...user, token, refreshToken })
          router.push('/')
        } else {
          ElMessage.error(res.message || '登录失败')
        }
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.auth-page {
  position: relative;
  height: 100vh;
  padding: 28px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

.auth-bg {
  position: absolute;
  border-radius: 50%;
  filter: blur(8px);
}

.auth-bg-left {
  width: 360px;
  height: 360px;
  left: -120px;
  top: -120px;
  background: rgba(41, 106, 184, 0.22);
}

.auth-bg-right {
  width: 420px;
  height: 420px;
  right: -130px;
  bottom: -140px;
  background: rgba(20, 148, 109, 0.2);
}

.auth-shell {
  position: relative;
  z-index: 1;
  width: min(960px, 96vw);
  min-height: 560px;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid rgba(19, 53, 89, 0.12);
  box-shadow: 0 24px 46px rgba(10, 28, 49, 0.16);
  background: rgba(255, 255, 255, 0.72);
}

.auth-intro {
  padding: 52px 48px;
  color: #f1f7ff;
  background: linear-gradient(145deg, #0f2b47, #1d4f81 58%, #23659e);
}

.auth-intro h1 {
  margin: 0;
  font-size: 32px;
  line-height: 1.3;
}

.auth-intro p {
  margin: 16px 0 20px;
  color: rgba(240, 247, 255, 0.82);
  font-size: 15px;
  line-height: 1.75;
}

.auth-intro ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 12px;
}

.auth-intro li {
  position: relative;
  padding-left: 16px;
  line-height: 1.7;
  color: rgba(243, 249, 255, 0.9);
}

.auth-intro li::before {
  content: "";
  position: absolute;
  left: 0;
  top: 11px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #7fc2ff;
}

.auth-card {
  padding: 58px 46px;
  background: rgba(255, 255, 255, 0.95);
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #1f3148;
}

.subtitle {
  margin-top: 8px;
  margin-bottom: 26px;
  color: #67809d;
  font-size: 14px;
}

.login-btn {
  width: 100%;
  margin-top: 4px;
}

.action-box {
  margin-top: 14px;
  text-align: right;
}

@media (max-width: 900px) {
  .auth-page {
    padding: 12px;
  }

  .auth-shell {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .auth-intro {
    padding: 28px 26px;
  }

  .auth-intro h1 {
    font-size: 24px;
  }

  .auth-card {
    padding: 30px 24px;
  }
}
</style>
