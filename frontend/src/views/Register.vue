<template>
  <div class="auth-page">
    <div class="auth-bg auth-bg-left"></div>
    <div class="auth-bg auth-bg-right"></div>

    <div class="auth-shell">
      <section class="auth-intro">
        <h1>创建你的账号</h1>
        <p>注册后即可参与岗位申请、过程管理与评价闭环。</p>
        <ul>
          <li>默认注册为学生身份</li>
          <li>注册时需选择负责教师</li>
          <li>密码策略已启用安全校验</li>
          <li>支持后续企业、教师、管理员协同</li>
        </ul>
      </section>

      <section class="auth-card">
        <div class="title">账号注册</div>
        <div class="subtitle">请填写你的基本信息</div>

        <el-form :model="form" :rules="rules" ref="formRef">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名/学号" size="large">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="name">
            <el-input v-model="form.name" placeholder="请输入真实姓名" size="large">
              <template #prefix>
                <el-icon><UserFilled /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="teacherId">
            <el-select v-model="form.teacherId" placeholder="请选择负责教师" size="large" filterable style="width: 100%">
              <el-option
                v-for="item in teacherOptions"
                :key="item.id"
                :label="`${item.name} (${item.username})`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="8-32位，至少3类字符" show-password size="large">
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="请再次确认密码" show-password size="large">
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-button type="primary" class="login-btn" size="large" @click="handleRegister" :loading="loading">
            提交注册
          </el-button>

          <div class="action-box">
            <el-link type="primary" @click="$router.push('/login')">已有账号？去登录</el-link>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import request from '@/utils/request'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User, UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const formRef = ref(null)
const teacherOptions = ref([])

const form = reactive({
  username: '',
  name: '',
  teacherId: null,
  password: '',
  confirmPassword: ''
})

const passwordPolicy = /^(?=.{8,32}$)(?:(?=.*[A-Z])(?=.*[a-z])(?=.*\d)|(?=.*[A-Z])(?=.*[a-z])(?=.*[^A-Za-z0-9])|(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9])|(?=.*[a-z])(?=.*\d)(?=.*[^A-Za-z0-9])).*$/

const validateUsername = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入账号'))
    return
  }
  const reg = /^\d{1,15}$/
  if (!reg.test(value)) {
    callback(new Error('学号只能为不超过15位的数字'))
    return
  }
  callback()
}

const validateName = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入姓名'))
    return
  }
  const reg = /^[\u4e00-\u9fa5]{1,5}$/
  if (!reg.test(value)) {
    callback(new Error('姓名只能为不超过5个字的中文'))
    return
  }
  callback()
}

const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入密码'))
    return
  }
  if (!passwordPolicy.test(value)) {
    callback(new Error('8-32位，且至少包含3类字符（大写/小写/数字/特殊字符）'))
    return
  }
  callback()
}

// 校验两次密码是否一致
const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, validator: validateUsername, trigger: 'blur' }],
  name: [{ required: true, validator: validateName, trigger: 'blur' }],
  teacherId: [{ required: true, message: '请选择负责教师', trigger: 'change' }],
  password: [{ required: true, validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: validatePass2, trigger: 'blur' }]
}

const loadTeacherOptions = async () => {
  const res = await request.get('/api/auth/teacher-options')
  teacherOptions.value = res.data || []
}

const handleRegister = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 调用后端接口
        const res = await request.post('/api/auth/register', form)
        
        if (res.code === 200) {
          ElMessage.success('注册成功，请登录')
          router.push('/login') // 注册成功跳回登录页
        } else {
          ElMessage.error(res.message || '注册失败')
        }
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(() => {
  loadTeacherOptions()
})
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
  background: rgba(31, 95, 174, 0.22);
}

.auth-bg-right {
  width: 420px;
  height: 420px;
  right: -130px;
  bottom: -140px;
  background: rgba(21, 148, 109, 0.2);
}

.auth-shell {
  position: relative;
  z-index: 1;
  width: min(980px, 96vw);
  min-height: 580px;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  overflow: hidden;
  border-radius: 18px;
  border: 1px solid rgba(19, 53, 89, 0.12);
  box-shadow: 0 24px 46px rgba(10, 28, 49, 0.16);
  background: rgba(255, 255, 255, 0.72);
}

.auth-intro {
  padding: 52px 48px;
  color: #f1f7ff;
  background: linear-gradient(145deg, #123151, #1e5589 58%, #2a6cab);
}

.auth-intro h1 {
  margin: 0;
  font-size: 30px;
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
  background: #8bd5ff;
}

.auth-card {
  padding: 48px 46px;
  background: rgba(255, 255, 255, 0.95);
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #1f3148;
}

.subtitle {
  margin-top: 8px;
  margin-bottom: 20px;
  color: #67809d;
  font-size: 14px;
}

.login-btn {
  width: 100%;
  margin-top: 4px;
}

.action-box {
  margin-top: 15px;
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
    padding: 26px 24px;
  }

  .auth-intro h1 {
    font-size: 24px;
  }

  .auth-card {
    padding: 28px 24px;
  }
}
</style>
