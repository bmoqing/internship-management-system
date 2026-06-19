<template>
  <div class="container">
    <div class="header-box">
      <el-input
        v-model="queryParams.keyword"
        placeholder="搜索标题/描述/学生/上报人"
        style="width: 260px; margin-right: 10px"
        clearable
        @clear="load"
      />
      <el-select v-model="queryParams.status" placeholder="处理状态" clearable style="width: 140px; margin-right: 10px">
        <el-option label="待处理" :value="0" />
        <el-option label="处理中" :value="1" />
        <el-option label="已解决" :value="2" />
        <el-option label="已驳回" :value="3" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="canReport" type="warning" @click="openReportDialog">上报异常</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px">
      <el-table-column prop="title" label="异常标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="类型" width="110">
        <template #default="scope">
          <el-tag>{{ typeText(scope.row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="等级" width="100">
        <template #default="scope">
          <el-tag :type="levelTagType(scope.row.level)">{{ scope.row.level || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="!isStudent" prop="studentName" label="学生" width="120" />
      <el-table-column prop="reporterName" label="上报人" width="110" />
      <el-table-column prop="companyName" label="企业" width="140" />
      <el-table-column prop="positionTitle" label="岗位" width="140" />
      <el-table-column prop="content" label="异常描述" min-width="220" show-overflow-tooltip />
      <el-table-column prop="handleResult" label="处理结果" min-width="180" show-overflow-tooltip />
      <el-table-column prop="reportTime" label="上报时间" width="170" />
      <el-table-column prop="handleTime" label="处理时间" width="170" />
      <el-table-column v-if="canHandle" label="操作" width="100">
        <template #default="scope">
          <el-button v-if="canHandleRow(scope.row)" size="small" type="primary" @click="openHandleDialog(scope.row)">处理</el-button>
          <span v-else style="color: #999; font-size: 12px">-</span>
        </template>
      </el-table-column>
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

    <el-dialog v-model="reportDialogVisible" title="上报异常事件" width="620px" append-to-body>
      <el-form :model="reportForm" label-width="100px">
        <el-form-item label="关联分配" required>
          <el-select 
            v-model="reportForm.assignmentId" 
            filterable 
            clearable 
            placeholder="请选择分配记录" 
            style="width: 100%" 
            :disabled="isStudent"
            @change="handleAssignmentChange">
            <el-option
              v-for="item in assignmentOptions"
              :key="item.id"
              :label="`${item.studentName} - ${item.positionTitle} - ${item.companyName}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="异常类型">
          <el-select v-model="reportForm.type" style="width: 100%">
            <el-option label="任务进度" value="TASK" />
            <el-option label="考勤异常" value="ATTENDANCE" />
            <el-option label="纪律问题" value="DISCIPLINE" />
            <el-option label="安全风险" value="SAFETY" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="异常等级">
          <el-select v-model="reportForm.level" style="width: 100%">
            <el-option label="LOW" value="LOW" />
            <el-option label="MEDIUM" value="MEDIUM" />
            <el-option label="HIGH" value="HIGH" />
            <el-option label="CRITICAL" value="CRITICAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="异常标题">
          <el-input v-model="reportForm.title" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="异常描述">
          <el-input v-model="reportForm.content" type="textarea" :rows="5" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReport">提交上报</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="handleDialogVisible" title="处理异常事件" width="460px" append-to-body>
      <el-form :model="handleForm" label-width="90px">
        <el-form-item label="处理状态">
          <el-select v-model="handleForm.status" style="width: 100%">
            <el-option label="处理中" :value="1" />
            <el-option label="已解决" :value="2" />
            <el-option label="驳回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理结果">
          <el-input v-model="handleForm.handleResult" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const role = computed(() => userStore.user.role)
const isStudent = computed(() => role.value === 'STUDENT')
const isAdmin = computed(() => role.value === 'ADMIN')
const canReport = computed(() => ['ADMIN', 'TEACHER', 'STUDENT', 'COMPANY'].includes(role.value))
const canHandle = computed(() => ['ADMIN', 'TEACHER'].includes(role.value))

const tableData = ref([])
const total = ref(0)
const assignmentOptions = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: null
})

const reportDialogVisible = ref(false)
const handleDialogVisible = ref(false)

const reportForm = reactive({
  assignmentId: null,
  studentId: null,
  type: 'TASK',
  level: 'MEDIUM',
  title: '',
  content: ''
})

const handleForm = reactive({
  id: null,
  status: 1,
  handleResult: ''
})

const statusText = (status) => {
  const map = {
    0: '待处理',
    1: '处理中',
    2: '已解决',
    3: '已驳回'
  }
  return map[status] || '未知'
}

const statusTagType = (status) => {
  if (status === 2) return 'success'
  if (status === 3) return 'danger'
  if (status === 1) return 'warning'
  return 'info'
}

const typeText = (type) => {
  const map = {
    TASK: '任务进度',
    ATTENDANCE: '考勤异常',
    DISCIPLINE: '纪律问题',
    SAFETY: '安全风险',
    OTHER: '其他'
  }
  return map[type] || type
}

const levelTagType = (level) => {
  if (level === 'CRITICAL') return 'danger'
  if (level === 'HIGH') return 'warning'
  if (level === 'MEDIUM') return 'success'
  return 'info'
}

const canHandleRow = (row) => {
  return canHandle.value && [0, 1].includes(row.status)
}

const load = async () => {
  const res = await request.get('/api/incident/list', { params: queryParams })
  tableData.value = res.data.records || []
  total.value = res.data.total || 0
}

const loadAssignments = async () => {
  const res = await request.get('/api/assignment/list', {
    params: {
      pageNum: 1,
      pageSize: 200,
      keyword: ''
    }
  })
  assignmentOptions.value = (res.data.records || []).filter(item => item.status === 1)
}

const openReportDialog = async () => {
  await loadAssignments()
  if (isStudent.value) {
    reportForm.assignmentId = assignmentOptions.value[0]?.id || null
    reportForm.studentId = userStore.user.id
  } else {
    reportForm.assignmentId = null
    reportForm.studentId = null
  }
  reportForm.type = 'TASK'
  reportForm.level = 'MEDIUM'
  reportForm.title = ''
  reportForm.content = ''
  reportDialogVisible.value = true
}

const handleAssignmentChange = (val) => {
  const selected = assignmentOptions.value.find(item => item.id === val)
  if (selected) {
    reportForm.studentId = selected.studentId
  } else {
    reportForm.studentId = null
  }
}

const submitReport = async () => {
  if (!reportForm.assignmentId) {
    ElMessage.warning('请选择关联分配')
    return
  }
  if (!reportForm.title) {
    ElMessage.warning('请填写异常标题')
    return
  }
  if (!reportForm.content) {
    ElMessage.warning('请填写异常描述')
    return
  }

  await request.post('/api/incident/report', reportForm)
  ElMessage.success('异常事件已上报')
  reportDialogVisible.value = false
  load()
}

const openHandleDialog = (row) => {
  handleForm.id = row.id
  handleForm.status = row.status === 0 ? 1 : row.status
  handleForm.handleResult = row.handleResult || ''
  handleDialogVisible.value = true
}

const submitHandle = async () => {
  if (!handleForm.handleResult) {
    ElMessage.warning('请填写处理结果')
    return
  }

  await request.put('/api/incident/handle', handleForm)
  ElMessage.success('处理完成')
  handleDialogVisible.value = false
  load()
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
