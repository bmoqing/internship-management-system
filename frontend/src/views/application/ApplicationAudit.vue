<template>
  <div class="container">
    <div class="search-box">
      <el-input 
        v-model="keyword" 
        placeholder="搜索学生姓名或岗位" 
        style="width: 200px; margin-right: 10px;"
        clearable 
        @clear="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button 
        type="success" 
        :disabled="selectedIds.length === 0" 
        @click="handleBatchAudit(passStatus)"
        style="margin-left: auto;">
        批量通过
      </el-button>
      <el-button 
        type="danger" 
        :disabled="selectedIds.length === 0" 
        @click="handleBatchAudit(rejectStatus)">
        批量驳回
      </el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px;" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" :selectable="canReview" />
      <el-table-column prop="studentName" label="申请学生" width="120" />
      <el-table-column prop="positionTitle" label="申请岗位" width="180" />
      <el-table-column prop="companyName" label="所属企业" width="180" />
      <el-table-column prop="applyTime" label="申请时间" width="180" />
      
      <el-table-column prop="status" label="状态" width="130">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 0" type="info">待企业预审</el-tag>
          <el-tag v-else-if="scope.row.status === 1" type="warning">待教师审核</el-tag>
          <el-tag v-else-if="scope.row.status === 2" type="warning">待管理员终审</el-tag>
          <el-tag v-else-if="scope.row.status === 3" type="danger">已驳回</el-tag>
          <el-tag v-else-if="scope.row.status === 4" type="primary">待分配</el-tag>
          <el-tag v-else-if="scope.row.status === 5" type="success">已分配</el-tag>
          <el-tag v-else type="info">未知</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="remark" label="审核意见" show-overflow-tooltip />
      <el-table-column prop="reviewTeacherName" label="审核教师" width="120" />
      <el-table-column prop="mentorName" label="企业导师" width="120" />

      <el-table-column label="操作" width="280">
        <template #default="scope">
          <div v-if="canReview(scope.row)">
            <el-button size="small" type="success" @click="openReview(scope.row, passStatus)">通过</el-button>
            <el-button size="small" type="danger" @click="openReview(scope.row, rejectStatus)">驳回</el-button>
          </div>
          <el-button
            v-else-if="canAssignTeacher(scope.row)"
            size="small"
            type="primary"
            @click="openAssignTeacher(scope.row)"
          >
            {{ scope.row.reviewTeacherId ? '转派教师' : '指派教师' }}
          </el-button>
          <span v-else style="color: #999; font-size: 12px;">已处理</span>

          <el-button 
            v-if="scope.row.studentResumeUrl" 
            size="small" 
            type="info" 
            plain 
            style="margin-left: 10px;"
            @click="viewResume(scope.row.studentResumeUrl)">
            查看简历
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </div>

    <!-- 审核弹窗 -->
    <el-dialog v-model="dialogVisible" title="审核处理" width="400px" append-to-body>
      <el-form>
        <el-form-item label="处理结果">
          <el-tag :type="currentStatus === rejectStatus ? 'danger' : 'success'">
            {{ currentStatus === rejectStatus ? '驳回' : '通过' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="remark" type="textarea" placeholder="请输入评语（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">确认提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignTeacherDialogVisible" title="指派审核教师" width="460px" append-to-body>
      <el-form :model="assignTeacherForm" label-width="90px">
        <el-form-item label="审核教师">
          <el-select v-model="assignTeacherForm.reviewTeacherId" filterable placeholder="请选择审核教师" style="width: 100%">
            <el-option
              v-for="item in teacherOptions"
              :key="item.id"
              :label="`${item.name} (${item.username})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="assignTeacherForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignTeacherDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssignTeacher">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const currentRole = userStore.user.role
const passStatus = currentRole === 'ADMIN' ? 4 : (currentRole === 'TEACHER' ? 2 : 1)
const rejectStatus = 3

const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const teacherOptions = ref([])
const selectedIds = ref([])

// 审核相关
const dialogVisible = ref(false)
const currentRow = ref({})
const currentStatus = ref(1)
const remark = ref('')

const assignTeacherDialogVisible = ref(false)
const assignTeacherForm = ref({
  id: null,
  reviewTeacherId: null,
  remark: ''
})

const canReview = (row) => {
  if (currentRole === 'COMPANY') {
    return row.status === 0
  }
  if (currentRole === 'TEACHER') {
    return row.status === 1
  }
  if (currentRole === 'ADMIN') {
    return row.status === 2
  }
  return false
}

const canAssignTeacher = (row) => currentRole === 'ADMIN' && row.status === 1

// 加载列表
const load = async () => {
  const res = await request.get('/api/application/list', {
    params: {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value
    }
  })
  if (res.code === 200) {
    tableData.value = res.data.records
    total.value = res.data.total
  }
}

const loadTeacherOptions = async () => {
  if (currentRole !== 'ADMIN') return
  const res = await request.get('/api/assignment/teachers')
  teacherOptions.value = res.data || []
}

// 打开审核弹窗
const openReview = (row, status) => {
  currentRow.value = row
  currentStatus.value = status
  if (status === 1) {
    remark.value = '企业预审通过，待教师初审'
  } else if (status === 2) {
    remark.value = '教师初审通过，待管理员终审'
  } else if (status === 4) {
    remark.value = '管理员终审通过，待实习分配'
  } else {
    remark.value = '不符合岗位要求，审核驳回'
  }
  dialogVisible.value = true
}

const openAssignTeacher = (row) => {
  assignTeacherForm.value.id = row.id
  assignTeacherForm.value.reviewTeacherId = row.reviewTeacherId || null
  assignTeacherForm.value.remark = row.reviewTeacherId
    ? `管理员转派审核教师（原教师：${row.reviewTeacherName || '未命名'}）`
    : '管理员指派审核教师'
  assignTeacherDialogVisible.value = true
}

// 提交审核
const submitReview = async () => {
  const data = {
    id: currentRow.value.id,
    status: currentStatus.value,
    remark: remark.value
  }
  await request.put('/api/application/review', data)
  ElMessage.success('审核完成')
  dialogVisible.value = false
  load() // 刷新列表
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

const handleBatchAudit = async (status) => {
  if (selectedIds.value.length === 0) return
  
  const statusName = status === rejectStatus ? '驳回' : '通过'
  const confirmResult = await window.confirm(`确定要批量${statusName}选中的 ${selectedIds.value.length} 条申请吗？`)
  if (!confirmResult) return

  try {
    const res = await request.post('/api/application/batch-audit', {
      ids: selectedIds.value,
      status: status,
      reviewRemark: `批量${statusName}`
    })
    
    if (res.code === 200) {
      ElMessage.success(res.data || '批量审核完成')
      selectedIds.value = []
      load()
    } else {
      ElMessage.error(res.message || '批量操作失败')
    }
  } catch (e) {
    ElMessage.error('批量请求异常')
  }
}

const submitAssignTeacher = async () => {
  if (!assignTeacherForm.value.reviewTeacherId) {
    ElMessage.warning('请选择审核教师')
    return
  }
  await request.put('/api/application/assign-teacher', assignTeacherForm.value)
  ElMessage.success('审核教师已指派')
  assignTeacherDialogVisible.value = false
  load()
}

const viewResume = async (url) => {
  if (!url) return
  try {
    const res = await request.get('/api/file/access-url', {
      params: { path: url }
    })
    if (res.code === 200) {
      window.open('http://localhost:8080' + res.data, '_blank')
    } else {
      ElMessage.error(res.message || '获取简历链接失败')
    }
  } catch (e) {
    ElMessage.error('获取链接异常')
  }
}

onMounted(() => {
  loadTeacherOptions()
  load()
})
</script>

<style scoped>
.container {
  padding: 20px;
  background: white;
  border-radius: 8px;
}
</style>
