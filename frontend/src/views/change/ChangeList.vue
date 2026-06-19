<template>
  <div class="container">
    <div class="search-box">
      <el-input 
        v-model="keyword" 
        placeholder="搜索学生姓名/学号" 
        style="width: 200px; margin-right: 10px;"
        clearable 
        @clear="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="isStudent" type="success" @click="openApply" style="margin-left: auto;">发起变更申请</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px;">
      <el-table-column prop="studentName" label="申请学生" width="120" />
      <el-table-column prop="type" label="变更类型" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.type === 1" type="warning">转岗</el-tag>
          <el-tag v-else-if="scope.row.type === 2" type="danger">换企业</el-tag>
          <el-tag v-else-if="scope.row.type === 3" type="info">提前离职</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="申请理由" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="130">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 0" type="info">待企业审核</el-tag>
          <el-tag v-else-if="scope.row.status === 1" type="warning">待教师审核</el-tag>
          <el-tag v-else-if="scope.row.status === 2" type="success">已通过</el-tag>
          <el-tag v-else-if="scope.row.status === 3" type="danger">已驳回</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="companyRemark" label="企业审核意见" show-overflow-tooltip />
      <el-table-column prop="teacherRemark" label="教师审核意见" show-overflow-tooltip />
      <el-table-column prop="createTime" label="申请时间" width="180" />

      <el-table-column label="操作" width="160" v-if="!isStudent">
        <template #default="scope">
          <el-button 
            v-if="isCompany && scope.row.status === 0" 
            size="small" type="primary" 
            @click="openAudit(scope.row)">
            企业预审
          </el-button>
          <el-button 
            v-if="isTeacher && scope.row.status === 1" 
            size="small" type="primary" 
            @click="openAudit(scope.row)">
            教师终审
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

    <!-- 发起申请弹窗 -->
    <el-dialog v-model="applyDialogVisible" title="发起实习变更申请" width="550px" append-to-body>
      <el-form :model="applyForm" label-width="100px">
        <el-form-item label="变更类型" required>
          <el-select v-model="applyForm.type" style="width: 100%" @change="handleTypeChange">
            <el-option label="转岗 (同企业)" :value="1" />
            <el-option label="换企业 (跳槽)" :value="2" />
            <el-option label="提前离职" :value="3" />
          </el-select>
        </el-form-item>
        
        <el-form-item v-if="applyForm.type === 1 || applyForm.type === 2" label="目标岗位" required>
          <el-select 
            v-model="applyForm.targetPositionId" 
            filterable 
            placeholder="请搜索并选择目标岗位" 
            style="width: 100%">
            <el-option
              v-for="item in positionOptions"
              :key="item.id"
              :label="`[${item.companyName}] ${item.title}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="申请理由" required>
          <el-input v-model="applyForm.reason" type="textarea" :rows="4" placeholder="请详细填写申请变更的理由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditDialogVisible" :title="isCompany ? '企业审核变更申请' : '教师终审变更申请'" width="400px" append-to-body>
      <el-form>
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.status">
            <el-radio :label="isCompany ? 1 : 2">通过</el-radio>
            <el-radio :label="3">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.remark" type="textarea" :rows="3" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAudit">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isStudent = computed(() => userStore.user.role === 'STUDENT')
const isCompany = computed(() => userStore.user.role === 'COMPANY')
const isTeacher = computed(() => userStore.user.role === 'TEACHER' || userStore.user.role === 'ADMIN')

const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const positionOptions = ref([])
const allPositions = ref([])
const currentCompanyId = ref(null)

const applyDialogVisible = ref(false)
const applyForm = reactive({
  type: 1,
  targetPositionId: null,
  reason: ''
})

const auditDialogVisible = ref(false)
const auditForm = reactive({
  id: null,
  status: 1,
  remark: ''
})

const load = async () => {
  const res = await request.get('/api/change/list', {
    params: { pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value }
  })
  if (res.code === 200) {
    tableData.value = res.data.records
    total.value = res.data.total
  }
}

const loadPositions = async () => {
  // 先加载学生当前实习分配，获取当前企业ID
  const assignRes = await request.get('/api/assignment/list', {
    params: { pageNum: 1, pageSize: 100, keyword: '' }
  })
  if (assignRes.code === 200) {
    const activeAssignment = (assignRes.data.records || []).find(a => a.status === 1)
    currentCompanyId.value = activeAssignment?.companyId || null
  }

  // 加载所有已审核通过的岗位
  const posRes = await request.get('/api/position', {
    params: { pageNum: 1, pageSize: 500, keyword: '' }
  })
  if (posRes.code === 200) {
    allPositions.value = posRes.data.records || []
  }

  filterPositionsByType()
}

const filterPositionsByType = () => {
  if (!currentCompanyId.value) {
    positionOptions.value = allPositions.value
    return
  }
  if (applyForm.type === 1) {
    // 转岗（同企业）：只显示当前企业的其它岗位
    positionOptions.value = allPositions.value.filter(p => p.companyId === currentCompanyId.value)
  } else if (applyForm.type === 2) {
    // 换企业：只显示其它企业的岗位
    positionOptions.value = allPositions.value.filter(p => p.companyId !== currentCompanyId.value)
  } else {
    positionOptions.value = []
  }
}

const handleTypeChange = () => {
  applyForm.targetPositionId = null
  filterPositionsByType()
}

const openApply = async () => {
  applyForm.type = 1
  applyForm.targetPositionId = null
  applyForm.reason = ''
  applyDialogVisible.value = true
  await loadPositions()
}

const submitApply = async () => {
  if (!applyForm.reason) {
    ElMessage.warning('请输入申请理由')
    return
  }
  if ((applyForm.type === 1 || applyForm.type === 2) && !applyForm.targetPositionId) {
    ElMessage.warning('请选择目标岗位')
    return
  }
  
  const res = await request.post('/api/change/apply', applyForm)
  if (res.code === 200) {
    ElMessage.success('申请已提交，等待企业审核')
    applyDialogVisible.value = false
    load()
  } else {
    ElMessage.error(res.message || '申请失败')
  }
}

const openAudit = (row) => {
  auditForm.id = row.id
  auditForm.status = isCompany.value ? 1 : 2
  auditForm.remark = ''
  auditDialogVisible.value = true
}

const submitAudit = async () => {
  const endpoint = isCompany.value ? '/api/change/company-audit' : '/api/change/teacher-audit'
  const payload = {
    id: auditForm.id,
    status: auditForm.status
  }
  if (isCompany.value) {
    payload.companyRemark = auditForm.remark
  } else {
    payload.teacherRemark = auditForm.remark
  }

  const res = await request.put(endpoint, payload)
  if (res.code === 200) {
    ElMessage.success('审核完成')
    auditDialogVisible.value = false
    load()
  } else {
    ElMessage.error(res.message || '审核失败')
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
.search-box {
  display: flex;
}
</style>
