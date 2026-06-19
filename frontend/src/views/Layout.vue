<template>
  <el-container class="layout-container">
    <!-- 左侧侧边栏 -->
    <el-aside width="248px" class="aside">
      <div class="logo">
        <div class="logo-mark">IM</div>
        <div class="logo-text">
          <div class="logo-title">实习管理平台</div>
          <div class="logo-subtitle">Internship Management</div>
        </div>
      </div>
      
      <!-- 菜单区域 -->
      <el-menu
        :default-active="route.path"
        :router="true"
        class="el-menu-vertical app-menu"
      >
        <!-- 1. 首页 (所有人可见) -->
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <!-- 2. 岗位管理 (所有人可见，内部按钮区分权限) -->
        <el-menu-item index="/position">
          <el-icon><Suitcase /></el-icon>
          <span>岗位管理</span>
        </el-menu-item>

        <!-- 2. 个人申请 (仅学生) -->
        <el-sub-menu index="/student-center" v-if="userStore.user.role === 'STUDENT'">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>个人中心</span>
          </template>
          <el-menu-item index="/student/resume">我的简历</el-menu-item>
          <el-menu-item index="/application">我的申请</el-menu-item>
        </el-sub-menu>

        <!-- 4. 申请审核 (只有老师和管理员可见) -->
        <el-menu-item index="/audit" v-if="['TEACHER', 'ADMIN', 'COMPANY'].includes(userStore.user.role)">
          <el-icon><Stamp /></el-icon>
          <span>申请审核</span>
        </el-menu-item>

        <el-menu-item index="/teacher/students" v-if="userStore.user.role === 'TEACHER'">
          <el-icon><UserFilled /></el-icon>
          <span>我管理的学生</span>
        </el-menu-item>

        <!-- 5. 实习日志 (所有人可见) -->
        <el-menu-item index="/log" v-if="['ADMIN', 'TEACHER', 'STUDENT'].includes(userStore.user.role)">
          <el-icon><EditPen /></el-icon>
          <span>实习日志</span>
        </el-menu-item>

        <el-menu-item index="/report" v-if="['ADMIN', 'TEACHER', 'STUDENT'].includes(userStore.user.role)">
          <el-icon><Document /></el-icon>
          <span>实习报告</span>
        </el-menu-item>

        <el-menu-item index="/assignment" v-if="['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'].includes(userStore.user.role)">
          <el-icon><Connection /></el-icon>
          <span>实习分配</span>
        </el-menu-item>

        <el-menu-item index="/agreement" v-if="['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'].includes(userStore.user.role)">
          <el-icon><DocumentChecked /></el-icon>
          <span>协议合同</span>
        </el-menu-item>

        <el-menu-item index="/change" v-if="['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'].includes(userStore.user.role)">
          <el-icon><Refresh /></el-icon>
          <span>实习变更</span>
        </el-menu-item>



        <el-menu-item index="/incident" v-if="['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'].includes(userStore.user.role)">
          <el-icon><Warning /></el-icon>
          <span>异常事件</span>
        </el-menu-item>

        <el-menu-item index="/appeal" v-if="['ADMIN', 'TEACHER', 'STUDENT'].includes(userStore.user.role)">
          <el-icon><ChatDotRound /></el-icon>
          <span>申诉复议</span>
        </el-menu-item>

        <el-menu-item index="/record" v-if="['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'].includes(userStore.user.role)">
          <el-icon><Files /></el-icon>
          <span>实习记录</span>
        </el-menu-item>

        <!-- 6. 考勤签到 (仅学生) -->
        <el-menu-item index="/student/attendance" v-if="userStore.user.role === 'STUDENT'">
          <el-icon><Calendar /></el-icon>
          <span>考勤签到</span>
        </el-menu-item>

        <!-- 7. 成绩评定/我的成绩 -->
        <el-menu-item index="/score" v-if="['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'].includes(userStore.user.role)">
          <el-icon><DataLine /></el-icon>
          <span>{{ userStore.user.role === 'STUDENT' ? '我的成绩' : '成绩评定' }}</span>
        </el-menu-item>

        <!-- 8. 系统管理 (只有管理员可见) -->
        <el-sub-menu index="/system" v-if="userStore.user.role === 'ADMIN'">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/user">用户管理</el-menu-item>
          <el-menu-item index="/admin/company">企业管理</el-menu-item>
          <el-menu-item index="/admin/notice">通知公告</el-menu-item>
          <el-menu-item index="/admin/audit-log">审计日志</el-menu-item>
          <el-menu-item index="/admin/config">系统配置</el-menu-item>
        </el-sub-menu>

        <!-- 9. 企业管理 (仅企业) -->
        <el-menu-item index="/company/profile" v-if="userStore.user.role === 'COMPANY'">
          <el-icon><OfficeBuilding /></el-icon>
          <span>考勤配置</span>
        </el-menu-item>

      </el-menu> <!-- ⚠️ 之前报错就是因为少了这行闭合标签 -->
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <!-- 顶部 Header -->
      <el-header class="header">
        <div class="breadcrumb">
          <el-icon class="crumb-icon"><Location /></el-icon>
          <span class="crumb-label">当前位置</span>
          <span class="crumb-page">{{ route.meta.title || '首页' }}</span>
        </div>
        <div class="user-info">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notice-badge">
            <el-button circle @click="openNoticeDrawer">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-avatar size="small" class="user-avatar">{{ userInitial }}</el-avatar>
          <div class="user-meta">
            <span class="username">{{ userStore.user.name || '未命名用户' }}</span>
            <span class="role-text">{{ formatRole(userStore.user.role) }}</span>
          </div>
          <el-button size="small" @click="router.push('/profile')">个人设置</el-button>
          <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <!-- 核心内容区域 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <el-drawer v-model="noticeDrawerVisible" title="系统公告" size="420px" append-to-body>
      <div class="drawer-actions">
        <el-button size="small" @click="refreshNotices">刷新</el-button>
      </div>
      <el-empty v-if="noticeList.length === 0" description="暂无公告" />
      <div v-else class="notice-list">
        <div class="notice-item" v-for="item in noticeList" :key="item.id">
          <div class="notice-title-row">
            <span class="notice-title">{{ item.title }}</span>
            <el-tag size="small" :type="noticeTagType(item.level)">{{ noticeLevelText(item.level) }}</el-tag>
          </div>
          <div class="notice-content">{{ item.content }}</div>
          <div class="notice-time">{{ item.createTime }}</div>
        </div>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import { ElNotification } from 'element-plus'
// 引入需要的图标 (确保 main.js 已经全局注册，这里引入是为了保险)
import { Odometer, Setting, Document, Suitcase, Location, Stamp, EditPen, Calendar, DataLine, Bell, Connection, Files, DocumentChecked, Refresh, Warning, ChatDotRound, ChatLineRound, UserFilled, OfficeBuilding } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const userInitial = computed(() => {
  const name = userStore.user.name || 'U'
  return String(name).slice(0, 1).toUpperCase()
})

const noticeList = ref([])
const unreadCount = ref(0)
const noticeDrawerVisible = ref(false)

const noticeStorageKey = () => `seen_notice_ids_${userStore.user.id || 'guest'}`

const getSeenNoticeIds = () => {
  try {
    return JSON.parse(sessionStorage.getItem(noticeStorageKey()) || '[]')
  } catch (e) {
    return []
  }
}

const setSeenNoticeIds = (ids) => {
  sessionStorage.setItem(noticeStorageKey(), JSON.stringify(ids))
}

const noticeTagType = (level) => {
  if (level === 'URGENT') return 'danger'
  if (level === 'WARN') return 'warning'
  return 'info'
}

const noticeNotifyType = (level) => {
  if (level === 'URGENT') return 'error'
  if (level === 'WARN') return 'warning'
  return 'info'
}

const noticeLevelText = (level) => {
  if (level === 'URGENT') return '紧急'
  if (level === 'WARN') return '提醒'
  return '普通'
}

const fetchNotices = async (showPopup = false) => {
  const res = await request.get('/api/notice/public', { params: { limit: 5 } })
  if (res.code !== 200) return

  noticeList.value = res.data || []
  const seenIds = getSeenNoticeIds()
  const unseen = noticeList.value.filter(item => !seenIds.includes(item.id))
  unreadCount.value = unseen.length

  if (showPopup && unseen.length > 0) {
    unseen.forEach(item => {
      ElNotification({
        title: `${noticeLevelText(item.level)}公告`,
        message: `${item.title}：${item.content}`,
        type: noticeNotifyType(item.level),
        duration: item.level === 'URGENT' ? 9000 : 6000,
        showClose: true
      })
    })
    setSeenNoticeIds([...new Set([...seenIds, ...unseen.map(item => item.id)])])
    unreadCount.value = 0
  }
}

const openNoticeDrawer = () => {
  noticeDrawerVisible.value = true
  const allIds = noticeList.value.map(item => item.id)
  setSeenNoticeIds(allIds)
  unreadCount.value = 0
}

const refreshNotices = async () => {
  await fetchNotices(false)
}

// 角色名转中文显示
const formatRole = (role) => {
  switch(role) {
    case 'ADMIN': return '管理员';
    case 'TEACHER': return '教师';
    case 'STUDENT': return '学生';
    case 'COMPANY': return '企业';
    default: return role;
  }
}

const handleLogout = async () => {
  if (confirm('确定要退出登录吗？')) {
    try {
      await request.post('/api/auth/logout', {
        refreshToken: userStore.user.refreshToken || null
      })
    } catch (e) {
      // ignore network/auth errors and continue local logout
    }
    userStore.logout()
    router.push('/login')
  }
}

onMounted(async () => {
  await fetchNotices(true)
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: transparent;
}

.aside {
  background: linear-gradient(180deg, #0f2b47 0%, #12375f 55%, #184773 100%);
  color: #e9f1ff;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 10px 0 28px rgba(7, 23, 39, 0.24);
}

.logo {
  height: 72px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(8, 28, 49, 0.36);
}

.logo-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.6px;
  color: #f2f7ff;
  background: linear-gradient(145deg, #2f79c7, #1f5fae);
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-title {
  font-size: 16px;
  font-weight: 600;
  color: #eff5ff;
  line-height: 1.2;
}

.logo-subtitle {
  margin-top: 2px;
  font-size: 11px;
  color: rgba(237, 246, 255, 0.72);
  letter-spacing: 0.4px;
}

.el-menu-vertical,
.app-menu {
  flex: 1;
  padding: 10px;
  border-right: none;
}

:deep(.app-menu.el-menu) {
  background: transparent;
  border-right: none;
}

:deep(.app-menu .el-menu-item),
:deep(.app-menu .el-sub-menu__title) {
  height: 42px;
  margin-bottom: 6px;
  border-radius: 10px;
  color: rgba(233, 242, 255, 0.84);
}

:deep(.app-menu .el-menu-item:hover),
:deep(.app-menu .el-sub-menu__title:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #f4f8ff;
}

:deep(.app-menu .el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(53, 128, 209, 0.96), rgba(37, 102, 183, 0.96));
  color: #f8fbff;
  box-shadow: 0 8px 16px rgba(20, 65, 118, 0.35);
}

:deep(.app-menu .el-sub-menu .el-menu-item) {
  margin-left: 8px;
}

:deep(.app-menu .el-sub-menu .el-menu) {
  margin: 2px 8px 8px;
  padding: 6px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.08) !important;
}

:deep(.app-menu .el-sub-menu .el-menu-item) {
  margin: 4px 0;
  height: 38px;
  line-height: 38px;
  border-radius: 8px;
  color: rgba(239, 246, 255, 0.96) !important;
  font-size: 13px;
  font-weight: 500;
}

:deep(.app-menu .el-sub-menu .el-menu-item:hover) {
  color: #ffffff !important;
  background: rgba(255, 255, 255, 0.14) !important;
}

:deep(.app-menu .el-sub-menu .el-menu-item.is-active) {
  color: #ffffff !important;
  background: rgba(133, 197, 255, 0.24) !important;
  box-shadow: inset 0 0 0 1px rgba(161, 216, 255, 0.5);
}

.header {
  margin: 14px 18px 0;
  height: 64px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(19, 54, 91, 0.08);
  box-shadow: 0 10px 24px rgba(15, 36, 61, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 18px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #587293;
}

.crumb-icon {
  color: #1f5fae;
}

.crumb-label {
  font-size: 13px;
  color: #6e8097;
}

.crumb-page {
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #1f4e84;
  background: rgba(31, 95, 174, 0.12);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.notice-badge {
  margin-right: 2px;
}

:deep(.notice-badge .el-badge__content) {
  box-shadow: 0 4px 10px rgba(194, 65, 63, 0.32);
}

.user-avatar {
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  background: linear-gradient(135deg, #2c74c3, #205ca3);
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: #1f3148;
}

.role-text {
  font-size: 12px;
  color: #6f829b;
}

.main {
  padding: 18px;
  background: transparent;
  overflow: auto;
}

.drawer-actions {
  margin-bottom: 12px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-item {
  padding: 12px;
  border: 1px solid rgba(21, 48, 79, 0.12);
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.notice-title-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.notice-title {
  font-size: 15px;
  font-weight: 600;
  color: #223956;
}

.notice-content {
  margin-top: 8px;
  color: #566b87;
  line-height: 1.5;
  white-space: pre-wrap;
}

.notice-time {
  margin-top: 8px;
  font-size: 12px;
  color: #7f93ac;
}

@media (max-width: 992px) {
  .layout-container :deep(.el-aside) {
    width: 214px !important;
  }

  .logo-subtitle,
  .role-text {
    display: none;
  }
}

@media (max-width: 768px) {
  .header {
    margin: 10px 10px 0;
    height: 58px;
    padding: 0 12px;
  }

  .crumb-label {
    display: none;
  }

  .main {
    padding: 10px;
  }
}
</style>
