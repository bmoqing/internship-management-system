/*
 * Copyright (c) 2026 bmoqing
 * All rights reserved.
 * 本代码仅供学习参考，未经许可不得用于商业用途。
 */

import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: Login
    },// 在 routes 数组里，/login 的下面加上这个：
	{
	  path: '/register',
	  name: 'Register',
	  component: () => import('../views/Register.vue')
	},
    {
      path: '/',
      name: 'Layout',
      component: Layout,
      redirect: '/dashboard',
      // 子路由配置
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('../views/dashboard/Dashboard.vue'),
          meta: { title: '首页', roles: ['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'] }
        },
		{
		  path: 'application',
		  name: 'MyApplication',
		  component: () => import('../views/application/MyApplication.vue'),
		  meta: { title: '我的申请', roles: ['STUDENT'] }
		},
		{
		  path: 'log',
		  name: 'LogList',
		  component: () => import('../views/log/LogList.vue'),
		  meta: { title: '实习日志', roles: ['ADMIN', 'TEACHER', 'STUDENT'] }
		},
        {
          path: 'report',
          name: 'ReportList',
          component: () => import('../views/report/ReportList.vue'),
          meta: { title: '实习报告', roles: ['ADMIN', 'TEACHER', 'STUDENT'] }
        },
        {
          path: 'teacher/students',
          name: 'ManagedStudents',
          component: () => import('../views/teacher/ManagedStudents.vue'),
          meta: { title: '我管理的学生', roles: ['TEACHER'] }
        },
        {
          path: 'assignment',
          name: 'AssignmentList',
          component: () => import('../views/assignment/AssignmentList.vue'),
          meta: { title: '实习分配', roles: ['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'] }
        },
        {
          path: 'agreement',
          name: 'AgreementList',
          component: () => import('../views/agreement/AgreementList.vue'),
          meta: { title: '协议合同', roles: ['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'] }
        },
        {
          path: 'change',
          name: 'ChangeList',
          component: () => import('../views/change/ChangeList.vue'),
          meta: { title: '实习变更', roles: ['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'] }
        },
        {
          path: 'incident',
          name: 'IncidentList',
          component: () => import('../views/incident/IncidentList.vue'),
          meta: { title: '异常事件', roles: ['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'] }
        },
        {
          path: 'appeal',
          name: 'AppealList',
          component: () => import('../views/appeal/AppealList.vue'),
          meta: { title: '申诉复议', roles: ['ADMIN', 'TEACHER', 'STUDENT'] }
        },
        {
          path: 'record',
          name: 'RecordList',
          component: () => import('../views/record/RecordList.vue'),
          meta: { title: '实习记录', roles: ['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'] }
        },
        {
          path: 'student/attendance',
          name: 'Attendance',
          component: () => import('../views/student/Attendance.vue'),
          meta: { title: '考勤签到', roles: ['STUDENT'] }
        },
        {
          path: 'student/resume',
          name: 'Resume',
          component: () => import('../views/student/Resume.vue'),
          meta: { title: '我的简历', roles: ['STUDENT'] }
        },
        {
          path: 'user',
          name: 'UserList',
          component: () => import('../views/system/UserList.vue'),
          meta: { title: '用户管理', roles: ['ADMIN'] }
        },
		{
		  path: 'admin/company',
		  name: 'CompanyList',
		  component: () => import('../views/admin/CompanyList.vue'),
		  meta: { title: '企业管理', roles: ['ADMIN'] }
		},
		{
		  path: 'admin/notice',
		  name: 'NoticeList',
		  component: () => import('../views/admin/NoticeList.vue'),
		  meta: { title: '通知公告', roles: ['ADMIN'] }
		},
		{
		  path: 'admin/audit-log',
		  name: 'AuditLogList',
		  component: () => import('../views/admin/AuditLogList.vue'),
		  meta: { title: '审计日志', roles: ['ADMIN'] }
		},
		{
		  path: 'admin/config',
		  name: 'SystemConfig',
		  component: () => import('../views/admin/SystemConfig.vue'),
		  meta: { title: '系统配置', roles: ['ADMIN'] }
		},
		{
		  path: 'audit',
		  name: 'ApplicationAudit',
		  component: () => import('../views/application/ApplicationAudit.vue'),
		  meta: { title: '申请审核', roles: ['ADMIN', 'TEACHER', 'COMPANY'] }
		},
        {
          path: 'score',
          name: 'ScoreList',
          component: () => import('../views/score/ScoreList.vue'),
          meta: { title: '成绩评定', roles: ['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'] }
        },
        {
          path: 'position',
          name: 'PositionList',
          component: () => import('../views/position/PositionList.vue'),
          meta: { title: '岗位管理', roles: ['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'] }
        },
        {
          path: 'company/profile',
          name: 'CompanyProfile',
          component: () => import('../views/company/CompanyProfile.vue'),
          meta: { title: '考勤与配置', roles: ['COMPANY'] }
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('../views/system/Profile.vue'),
          meta: { title: '个人设置', roles: ['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'] }
        }
      ]
    }
  ]
})

// 路由守卫 (保持原样)
router.beforeEach((to, from, next) => {
  const userStr = localStorage.getItem('user')
  let user = null
  try {
    user = userStr ? JSON.parse(userStr) : null
  } catch (e) {
    user = null
  }
  const isAuthenticated = !!(user && user.id && user.token)
  const publicPaths = ['/login', '/register']

  if (!publicPaths.includes(to.path) && !isAuthenticated) {
    next('/login')
    return
  }

  if (publicPaths.includes(to.path) && isAuthenticated) {
    next('/')
    return
  }

  const roles = to.meta?.roles
  if (roles && roles.length > 0 && isAuthenticated && !roles.includes(user.role)) {
    ElMessage.warning('无权限访问该页面')
    next('/dashboard')
  } else {
    next()
  }
})

export default router
