<template>
  <div class="container">
    <div class="header-box">
      <template v-if="isStudent">
        <el-button type="warning" @click="openSubmitDialog">发起申诉</el-button>
      </template>
      <template v-else>
        <el-input
          v-model="queryParams.keyword"
          placeholder="搜索学生/申诉理由/对象类型"
          style="width: 260px; margin-right: 10px"
          clearable
          @clear="load"
        />
        <el-select v-model="queryParams.status" clearable placeholder="状态" style="width: 150px; margin-right: 10px">
          <el-option label="待教师初审" :value="0" />
          <el-option label="待管理员复议" :value="1" />
          <el-option label="复议通过" :value="2" />
          <el-option label="复议驳回" :value="3" />
          <el-option label="已关闭" :value="4" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </template>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px">
      <el-table-column v-if="!isStudent" prop="studentName" label="学生姓名" width="120" />
      <el-table-column v-if="!isStudent" prop="studentNo" label="学号" width="130" />
      <el-table-column prop="companyName" label="企业" width="140" />
      <el-table-column prop="positionTitle" label="岗位" width="150" />
      <el-table-column prop="targetType" label="申诉对象" width="120" />
      <el-table-column prop="targetId" label="对象ID" width="90" />
      <el-table-column prop="reason" label="申诉理由" min-width="220" show-overflow-tooltip />
      <el-table-column label="证据链接" width="120">
        <template #default="scope">
          <el-link v-if="scope.row.evidenceUrl" type="primary" @click="openEvidence(scope.row.evidenceUrl)">查看证据</el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="teacherReply" label="教师回复" min-width="160" show-overflow-tooltip />
      <el-table-column prop="adminReply" label="管理员回复" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="发起时间" width="170" />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button v-if="canTeacherReview(scope.row)" size="small" type="primary" @click="openTeacherReviewDialog(scope.row)">初审</el-button>
          <el-button v-else-if="canAdminReview(scope.row)" size="small" type="success" @click="openAdminReviewDialog(scope.row)">复议</el-button>
          <el-button v-else-if="canClose(scope.row)" size="small" type="warning" @click="submitClose(scope.row)">关闭</el-button>
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

    <el-dialog v-model="submitDialogVisible" title="发起申诉" width="620px" append-to-body>
      <el-form :model="submitForm" label-width="100px">
        <el-form-item label="关联分配">
          <el-select v-model="submitForm.assignmentId" filterable placeholder="请选择当前实习分配" style="width: 100%">
            <el-option
              v-for="item in assignmentOptions"
              :key="item.id"
              :label="`${item.positionTitle} - ${item.companyName} - ${item.teacherName || '待定'}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申诉对象">
          <el-select v-model="submitForm.targetType" style="width: 100%">
            <el-option label="成绩" value="SCORE" />
            <el-option label="报告" value="REPORT" />
            <el-option label="日志" value="LOG" />
            <el-option label="考勤" value="ATTENDANCE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="申诉理由">
          <el-input v-model="submitForm.reason" type="textarea" :rows="5" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="证据材料">
          <el-upload
            class="upload-block"
            :show-file-list="false"
            :http-request="uploadEvidenceFile"
            :before-upload="beforeEvidenceUpload"
            accept=".pdf,.doc,.docx,.png,.jpg,.jpeg,.txt,.zip,.rar"
          >
            <el-button type="primary" plain>选择并上传证据</el-button>
          </el-upload>
          <div class="upload-tip">可选，支持文档/图片/压缩包，单文件不超过20MB</div>
          <div v-if="submitForm.evidenceUrl" class="uploaded-preview">
            <el-link type="primary" @click="openEvidence(submitForm.evidenceUrl)">已上传：{{ uploadedEvidenceName || '查看证据' }}</el-link>
            <el-button type="danger" link @click="clearEvidenceUpload">移除</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="teacherDialogVisible" title="教师初审" width="460px" append-to-body>
      <el-form :model="teacherForm" label-width="100px">
        <el-form-item label="初审结果">
          <el-select v-model="teacherForm.status" style="width: 100%">
            <el-option label="通过，转管理员复议" :value="1" />
            <el-option label="驳回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="教师回复">
          <el-input v-model="teacherForm.teacherReply" type="textarea" :rows="4" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="teacherDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTeacherReview">提交初审</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adminDialogVisible" title="管理员复议" width="460px" append-to-body>
      <el-form :model="adminForm" label-width="100px">
        <el-form-item label="复议结果">
          <el-select v-model="adminForm.status" style="width: 100%">
            <el-option label="复议通过" :value="2" />
            <el-option label="复议驳回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="管理员回复">
          <el-input v-model="adminForm.adminReply" type="textarea" :rows="4" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adminDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdminReview">提交复议</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const role = computed(() => userStore.user.role)
const isStudent = computed(() => role.value === 'STUDENT')
const isTeacher = computed(() => role.value === 'TEACHER')
const isAdmin = computed(() => role.value === 'ADMIN')

const tableData = ref([])
const total = ref(0)
const assignmentOptions = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: null
})

const submitDialogVisible = ref(false)
const teacherDialogVisible = ref(false)
const adminDialogVisible = ref(false)
const uploadedEvidenceName = ref('')

const submitForm = reactive({
  assignmentId: null,
  targetType: 'SCORE',
  targetId: null,
  reason: '',
  evidenceUrl: ''
})

const teacherForm = reactive({
  id: null,
  status: 1,
  teacherReply: ''
})

const adminForm = reactive({
  id: null,
  status: 2,
  adminReply: ''
})

const statusText = (status) => {
  const map = {
    0: '待教师初审',
    1: '待管理员复议',
    2: '复议通过',
    3: '复议驳回',
    4: '已关闭'
  }
  return map[status] || '未知'
}

const statusTagType = (status) => {
  if (status === 2 || status === 4) return 'success'
  if (status === 3) return 'danger'
  if (status === 1) return 'warning'
  return 'info'
}

const canTeacherReview = (row) => isTeacher.value && row.status === 0
const canAdminReview = (row) => isAdmin.value && row.status === 1
const canClose = (row) => isStudent.value && row.status === 2

const load = async () => {
  const url = isStudent.value ? '/api/appeal/my' : '/api/appeal/list'
  const params = {
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize
  }
  if (!isStudent.value) {
    params.keyword = queryParams.keyword
    params.status = queryParams.status
  }

  const res = await request.get(url, { params })
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

const openSubmitDialog = async () => {
  await loadAssignments()
  submitForm.assignmentId = null
  submitForm.targetType = 'SCORE'
  submitForm.targetId = null
  submitForm.reason = ''
  submitForm.evidenceUrl = ''
  uploadedEvidenceName.value = ''
  submitDialogVisible.value = true
}

const beforeEvidenceUpload = (file) => {
  const isTooLarge = file.size / 1024 / 1024 > 20
  if (isTooLarge) {
    ElMessage.warning('文件不能超过20MB')
    return false
  }
  return true
}

const uploadEvidenceFile = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)

  try {
    const res = await request.post('/api/file/upload', formData, {
      params: { bizType: 'appeal' },
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000
    })
    if (res.code !== 200 || !res.data?.url) {
      throw new Error(res.message || '上传失败')
    }

    submitForm.evidenceUrl = res.data.url
    uploadedEvidenceName.value = res.data.originalName || options.file.name
    options.onSuccess?.(res.data)
    ElMessage.success('证据上传成功')
  } catch (e) {
    options.onError?.(e)
  }
}

const clearEvidenceUpload = () => {
  submitForm.evidenceUrl = ''
  uploadedEvidenceName.value = ''
}

const submitAppeal = async () => {
  if (!submitForm.assignmentId) {
    ElMessage.warning('请选择关联分配')
    return
  }
  if (!submitForm.reason) {
    ElMessage.warning('请填写申诉理由')
    return
  }

  await request.post('/api/appeal/submit', submitForm)
  ElMessage.success('申诉已提交')
  submitDialogVisible.value = false
  load()
}

const openTeacherReviewDialog = (row) => {
  teacherForm.id = row.id
  teacherForm.status = 1
  teacherForm.teacherReply = '同意转管理员复议'
  teacherDialogVisible.value = true
}

const submitTeacherReview = async () => {
  await request.put('/api/appeal/teacher-review', teacherForm)
  ElMessage.success('教师初审已提交')
  teacherDialogVisible.value = false
  load()
}

const openAdminReviewDialog = (row) => {
  adminForm.id = row.id
  adminForm.status = 2
  adminForm.adminReply = '复议通过，按流程执行处理'
  adminDialogVisible.value = true
}

const submitAdminReview = async () => {
  await request.put('/api/appeal/admin-review', adminForm)
  ElMessage.success('管理员复议已提交')
  adminDialogVisible.value = false
  load()
}

const submitClose = async (row) => {
  await ElMessageBox.confirm('确认关闭该申诉流程吗？', '提示', { type: 'warning' })
  await request.put('/api/appeal/close', { id: row.id })
  ElMessage.success('申诉流程已关闭')
  load()
}

const openEvidence = async (url) => {
  if (!url) {
    ElMessage.warning('证据链接为空')
    return
  }
  if (/files\.example\.com/i.test(url)) {
    ElMessage.info('该链接是演示种子数据占位地址，未实际托管文件。请上传真实可访问链接。')
    return
  }

  if (/^https?:\/\//i.test(url) && !/\/files\//i.test(url) && !/\/api\/file\/download/i.test(url)) {
    window.open(url, '_blank', 'noopener,noreferrer')
    return
  }

  try {
    const res = await request.get('/api/file/access-url', { params: { path: url } })
    if (res.code !== 200 || !res.data) {
      ElMessage.error(res.message || '文件访问链接生成失败')
      return
    }

    const accessUrl = res.data
    const resolvedUrl = /^https?:\/\//i.test(accessUrl)
      ? accessUrl
      : `${request.defaults.baseURL}${accessUrl.startsWith('/') ? '' : '/'}${accessUrl}`
    window.open(resolvedUrl, '_blank', 'noopener,noreferrer')
  } catch (e) {
    // 拦截器已统一提示
  }
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

.upload-block {
  width: 100%;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #7b8fa8;
}

.uploaded-preview {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
