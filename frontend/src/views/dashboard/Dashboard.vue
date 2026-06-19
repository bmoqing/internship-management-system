<template>
  <div class="dashboard-page">
    <el-row :gutter="16" class="metric-row">
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card class="metric-card">
          <div class="metric-label">{{ metricLabels.userCount }}</div>
          <div class="metric-value">{{ stats.userCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card class="metric-card">
          <div class="metric-label">{{ metricLabels.companyCount }}</div>
          <div class="metric-value">{{ stats.companyCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card class="metric-card">
          <div class="metric-label">{{ metricLabels.positionCount }}</div>
          <div class="metric-value">{{ stats.positionCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card class="metric-card">
          <div class="metric-label">{{ metricLabels.appCount }}</div>
          <div class="metric-value">{{ stats.appCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card class="metric-card">
          <div class="metric-label">{{ metricLabels.assignmentCount }}</div>
          <div class="metric-value">{{ stats.assignmentCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card class="metric-card">
          <div class="metric-label">{{ metricLabels.logCount }}</div>
          <div class="metric-value">{{ stats.logCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card class="metric-card">
          <div class="metric-label">{{ metricLabels.reportCount }}</div>
          <div class="metric-value">{{ stats.reportCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card class="metric-card">
          <div class="metric-label">{{ metricLabels.attendanceCount }}</div>
          <div class="metric-value">{{ stats.attendanceCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="todoCards.length > 0" :gutter="16" class="todo-row">
      <el-col v-for="(item, idx) in todoCards" :key="`${item.title}-${idx}`" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card class="todo-card">
          <div class="todo-title">{{ item.title }}</div>
          <div class="todo-value">{{ item.value }}</div>
          <div class="todo-desc">{{ item.description }}</div>
          <el-button v-if="item.route" link type="primary" class="todo-link" @click="goRoute(item.route)">去处理</el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="isAdmin" :gutter="16" class="chart-row">
      <el-col :xs="24" :md="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-title">用户角色分布</div>
          </template>
          <div ref="roleChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-title">实习申请状态分布</div>
          </template>
          <div ref="statusChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row v-if="isAdmin" :gutter="16" class="chart-row">
      <el-col :xs="24" :md="14">
        <el-card class="chart-card">
          <template #header>
            <div class="card-title">近7日业务趋势</div>
          </template>
          <div ref="trendChartRef" class="chart chart-lg"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card class="chart-card">
          <template #header>
            <div class="card-title">热门岗位 Top7</div>
          </template>
          <div ref="positionChartRef" class="chart chart-lg"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="!isAdmin && todoCards.length === 0" class="chart-card role-tip-card">
      <template #header>
        <div class="card-title">工作概览</div>
      </template>
      <div class="role-tip-grid">
        <div class="role-tip-item">当前角色：{{ roleText }}</div>
        <div class="role-tip-item">请优先处理待审核/待处理业务</div>
        <div class="role-tip-item">核心指标已按角色范围隔离展示</div>
      </div>
    </el-card>

    <el-card class="notice-card chart-card">
      <template #header>
        <div class="card-title">系统公告</div>
      </template>
      <el-empty v-if="noticeList.length === 0" description="暂无公告" />
      <div v-else class="notice-list">
        <div class="notice-item" v-for="item in noticeList" :key="item.id">
          <div class="notice-head">
            <span class="notice-title">{{ item.title }}</span>
            <el-tag size="small" :type="noticeTagType(item.level)">{{ noticeLevelText(item.level) }}</el-tag>
          </div>
          <div class="notice-content">{{ item.content }}</div>
          <div class="notice-time">{{ item.createTime }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import request from '@/utils/request'
import * as echarts from 'echarts'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()
const role = computed(() => userStore.user.role || '')
const isAdmin = computed(() => role.value === 'ADMIN')

const roleText = computed(() => {
  if (role.value === 'TEACHER') return '教师'
  if (role.value === 'COMPANY') return '企业'
  if (role.value === 'STUDENT') return '学生'
  return '管理员'
})

const metricLabels = computed(() => {
  if (role.value === 'TEACHER') {
    return {
      userCount: '负责学生',
      companyCount: '关联企业',
      positionCount: '关联岗位',
      appCount: '相关申请',
      assignmentCount: '在岗分配',
      logCount: '日志总数',
      reportCount: '报告总数',
      attendanceCount: '签到总数'
    }
  }
  if (role.value === 'COMPANY') {
    return {
      userCount: '在岗学生',
      companyCount: '企业数量',
      positionCount: '我方岗位',
      appCount: '相关申请',
      assignmentCount: '在岗分配',
      logCount: '相关日志',
      reportCount: '相关报告',
      attendanceCount: '相关签到'
    }
  }
  if (role.value === 'STUDENT') {
    return {
      userCount: '我的账号',
      companyCount: '实习企业',
      positionCount: '当前岗位',
      appCount: '我的申请',
      assignmentCount: '进行中分配',
      logCount: '我的日志',
      reportCount: '我的报告',
      attendanceCount: '我的签到'
    }
  }
  return {
    userCount: '系统用户',
    companyCount: '企业数量',
    positionCount: '岗位数量',
    appCount: '申请总数',
    assignmentCount: '分配总数',
    logCount: '日志总数',
    reportCount: '报告总数',
    attendanceCount: '签到总数'
  }
})

const stats = reactive({
  userCount: 0,
  companyCount: 0,
  positionCount: 0,
  appCount: 0,
  assignmentCount: 0,
  logCount: 0,
  reportCount: 0,
  attendanceCount: 0
})

const noticeList = ref([])
const todoCards = ref([])

const roleChartRef = ref(null)
const statusChartRef = ref(null)
const trendChartRef = ref(null)
const positionChartRef = ref(null)

let roleChart = null
let statusChart = null
let trendChart = null
let positionChart = null

const noticeTagType = (level) => {
  if (level === 'URGENT') return 'danger'
  if (level === 'WARN') return 'warning'
  return 'info'
}

const noticeLevelText = (level) => {
  if (level === 'URGENT') return '紧急'
  if (level === 'WARN') return '提醒'
  return '普通'
}

const goRoute = (route) => {
  if (!route) return
  router.push(route)
}

const renderCharts = (data) => {
  if (roleChartRef.value) {
    roleChart = roleChart || echarts.init(roleChartRef.value)
    roleChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '68%'],
        data: data.userRoleChart || [],
        emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' } }
      }]
    })
  }

  if (statusChartRef.value) {
    statusChart = statusChart || echarts.init(statusChartRef.value)
    statusChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: '62%',
        data: data.applicationStatusChart || [],
        label: { formatter: '{b}: {c}' }
      }]
    })
  }

  if (positionChartRef.value) {
    const chartData = data.topPositionChart || []
    positionChart = positionChart || echarts.init(positionChartRef.value)
    positionChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 60, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'value', minInterval: 1 },
      yAxis: {
        type: 'category',
        data: chartData.map(item => item.name)
      },
      series: [{
        type: 'bar',
        data: chartData.map(item => item.value),
        barWidth: 14,
        itemStyle: { color: '#409EFF' }
      }]
    })
  }

  if (trendChartRef.value) {
    trendChart = trendChart || echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { top: 0 },
      grid: { left: 40, right: 20, top: 36, bottom: 28 },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: data.trendDates || []
      },
      yAxis: {
        type: 'value',
        minInterval: 1
      },
      series: [
        {
          name: '申请量',
          type: 'line',
          smooth: true,
          data: data.applyTrend || []
        },
        {
          name: '签到量',
          type: 'line',
          smooth: true,
          data: data.checkinTrend || []
        },
        {
          name: '日志量',
          type: 'line',
          smooth: true,
          data: data.logTrend || []
        }
      ]
    })
  }
}

const handleResize = () => {
  roleChart?.resize()
  statusChart?.resize()
  trendChart?.resize()
  positionChart?.resize()
}

const loadStats = async () => {
  const res = await request.get('/api/stats')
  if (res.code !== 200) return

  const data = res.data || {}
  stats.userCount = data.userCount || 0
  stats.companyCount = data.companyCount || 0
  stats.positionCount = data.positionCount || 0
  stats.appCount = data.appCount || 0
  stats.assignmentCount = data.assignmentCount || 0
  stats.logCount = data.logCount || 0
  stats.reportCount = data.reportCount || 0
  stats.attendanceCount = data.attendanceCount || 0
  noticeList.value = data.noticeList || []
  todoCards.value = data.todoCards || []

  if (isAdmin.value) {
    await nextTick()
    renderCharts(data)
  }
}

onMounted(async () => {
  await loadStats()
  if (isAdmin.value) {
    window.addEventListener('resize', handleResize)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  roleChart?.dispose()
  statusChart?.dispose()
  trendChart?.dispose()
  positionChart?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.metric-row {
  margin-bottom: 0;
}

.metric-card {
  margin-bottom: 0;
  border: 1px solid rgba(20, 58, 97, 0.14);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.metric-label {
  color: #537095;
  font-size: 13px;
  letter-spacing: 0.3px;
}

.metric-value {
  margin-top: 12px;
  font-size: 32px;
  font-weight: 700;
  color: #1a3352;
  line-height: 1;
}

:deep(.metric-card .el-card__body) {
  padding: 16px 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.metric-row .el-col:nth-child(1) .metric-card,
.metric-row .el-col:nth-child(2) .metric-card,
.metric-row .el-col:nth-child(3) .metric-card,
.metric-row .el-col:nth-child(4) .metric-card {
  border-top: 3px solid #2a6fbe;
}

.metric-row .el-col:nth-child(5) .metric-card,
.metric-row .el-col:nth-child(6) .metric-card,
.metric-row .el-col:nth-child(7) .metric-card,
.metric-row .el-col:nth-child(8) .metric-card {
  border-top: 3px solid #14906b;
}

.metric-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 30px rgba(17, 46, 78, 0.16);
}

.todo-row {
  margin-top: 0;
}

.todo-card {
  border: 1px solid rgba(20, 58, 97, 0.14);
}

.todo-title {
  font-size: 13px;
  color: #537095;
}

.todo-value {
  margin-top: 8px;
  font-size: 30px;
  font-weight: 700;
  color: #1a3352;
  line-height: 1;
}

.todo-desc {
  margin-top: 10px;
  font-size: 12px;
  color: #5f7897;
  line-height: 1.4;
  min-height: 34px;
}

.todo-link {
  margin-top: 8px;
  padding: 0;
}

.chart-row {
  margin-top: 0;
}

.chart-card {
  border: 1px solid rgba(20, 58, 97, 0.12);
}

:deep(.chart-card .el-card__header) {
  border-bottom: 1px solid rgba(20, 58, 97, 0.08);
  background: linear-gradient(180deg, #fbfdff 0%, #f3f8ff 100%);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #26466c;
}

.chart {
  width: 100%;
  height: 320px;
}

.chart-lg {
  height: 360px;
}

.notice-card {
  margin-bottom: 4px;
}

.role-tip-card {
  margin-top: 0;
}

.role-tip-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.role-tip-item {
  border: 1px solid rgba(20, 58, 97, 0.14);
  border-radius: 10px;
  padding: 12px;
  color: #355375;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-item {
  border: 1px solid rgba(20, 58, 97, 0.14);
  border-radius: 10px;
  padding: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.notice-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.notice-title {
  font-size: 15px;
  font-weight: 600;
  color: #223a58;
}

.notice-content {
  margin-top: 8px;
  line-height: 1.5;
  color: #5a708d;
  white-space: pre-wrap;
}

.notice-time {
  margin-top: 8px;
  font-size: 12px;
  color: #7a90aa;
}

@media (max-width: 768px) {
  .metric-value {
    font-size: 24px;
  }

  .chart,
  .chart-lg {
    height: 300px;
  }
}
</style>
