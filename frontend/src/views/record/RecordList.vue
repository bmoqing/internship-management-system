<template>
  <div class="container">
    <div class="header-box" v-if="!isStudent">
      <el-input
        v-model="queryParams.keyword"
        placeholder="搜索学生/事件类型/事件描述"
        style="width: 260px; margin-right: 10px"
        clearable
        @clear="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px">
      <el-table-column v-if="!isStudent" prop="studentName" label="学生姓名" width="120" />
      <el-table-column v-if="!isStudent" prop="studentNo" label="学号" width="130" />
      <el-table-column label="事件类型" width="150">
        <template #default="scope">
          <el-tag :type="eventTagType(scope.row.eventType)">{{ eventText(scope.row.eventType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="eventDetail" label="事件描述" min-width="220" show-overflow-tooltip />
      <el-table-column prop="operatorName" label="操作人" width="120" />
      <el-table-column prop="createTime" label="发生时间" width="180" />
    </el-table>

    <div style="margin-top: 20px; display: flex; justify-content: flex-end">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isStudent = computed(() => userStore.user.role === 'STUDENT')

const tableData = ref([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const eventText = (eventType) => {
  const map = {
    APPLICATION_SUBMIT: '提交申请',
    APPLICATION_ASSIGN_TEACHER: '指派审核教师',
    APPLICATION_APPROVE: '申请通过',
    APPLICATION_REJECT: '申请驳回',
    ASSIGNMENT: '实习分配',
    ASSIGNMENT_MENTOR_UPDATE: '调整企业导师',
    AGREEMENT_UPLOAD: '上传协议',
    AGREEMENT_APPROVE: '协议通过',
    AGREEMENT_REJECT: '协议驳回',
    ATTENDANCE_CHECKIN: '考勤签到',
    LOG_SUBMIT: '提交日志',
    LOG_REVIEW: '日志批阅',
    REPORT_SUBMIT: '提交报告',
    REPORT_REVIEW: '报告批阅',
    SCORE_EVALUATE: '成绩评定',
    COMPANY_EVALUATION: '企业评价',
    COMPANY_FEEDBACK_SUBMIT: '企业反馈',
    INCIDENT_REPORT: '异常上报',
    INCIDENT_HANDLE: '异常处理',
    APPEAL_SUBMIT: '发起申诉',
    APPEAL_TEACHER_REVIEW: '申诉初审',
    APPEAL_ADMIN_REVIEW: '复议处理',
    APPEAL_CLOSE: '关闭申诉'
  }
  return map[eventType] || eventType
}

const eventTagType = (eventType) => {
  if (!eventType) return 'info'
  if (eventType.includes('REJECT')) return 'danger'
  if (eventType.includes('HANDLE') || eventType.includes('CLOSE') || eventType.includes('ASSIGN_TEACHER') || eventType.includes('MENTOR_UPDATE')) return 'success'
  if (eventType.includes('APPROVE') || eventType.includes('ASSIGNMENT') || eventType.includes('EVALUATE')) return 'success'
  if (eventType.includes('SUBMIT')) return 'warning'
  return 'info'
}

const load = async () => {
  const url = isStudent.value ? '/api/record/my' : '/api/record/list'
  const params = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize
  }
  if (!isStudent.value) {
    params.keyword = queryParams.keyword
  }

  const res = await request.get(url, { params })
  tableData.value = res.data.records
  total.value = res.data.total
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.container {
  padding: 20px;
  background: white;
  border-radius: 8px;
}

.header-box {
  display: flex;
  align-items: center;
}
</style>
