<template>
  <div class="container">
    <div class="header-box">
      <el-input
        v-model="queryParams.keyword"
        placeholder="搜索学生/岗位/企业"
        style="width: 240px; margin-right: 10px"
        clearable
        @clear="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px">
      <el-table-column prop="studentName" label="学生姓名" width="120" />
      <el-table-column prop="studentNo" label="学号" width="130" />
      <el-table-column prop="positionTitle" label="岗位名称" min-width="160" />
      <el-table-column prop="companyName" label="企业名称" width="150" />
      <el-table-column prop="teacherName" label="指导教师" width="120" />
      <el-table-column prop="mentorName" label="企业导师" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 1" type="success">进行中</el-tag>
          <el-tag v-else type="info">已结束</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="assignTime" label="分配时间" width="170" />
      <el-table-column prop="remark" label="说明" min-width="180" show-overflow-tooltip />
      <el-table-column v-if="canManageMentor" label="操作" width="120">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === 1"
            size="small"
            type="primary"
            @click="openMentorDialog(scope.row)"
          >
            {{ scope.row.mentorId ? '调整导师' : '分配导师' }}
          </el-button>
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





    <el-dialog v-model="mentorDialogVisible" title="分配企业导师" width="520px" append-to-body>
      <el-form :model="mentorForm" label-width="90px">
        <el-form-item label="企业导师">
          <el-select v-model="mentorForm.mentorId" clearable filterable placeholder="请选择企业导师" style="width: 100%">
            <el-option
              v-for="item in mentorOptions"
              :key="item.id"
              :label="`${item.name} (${item.username})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="mentorForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="mentorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMentorAssign">确认</el-button>
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
const isAdmin = computed(() => userStore.user.role === 'ADMIN')
const isCompany = computed(() => userStore.user.role === 'COMPANY')
const canManageMentor = computed(() => ['ADMIN', 'COMPANY'].includes(userStore.user.role))

const tableData = ref([])
const total = ref(0)
const mentorOptions = ref([])

const mentorDialogVisible = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})



const mentorForm = reactive({
  assignmentId: null,
  mentorId: null,
  remark: ''
})

const load = async () => {
  const res = await request.get('/api/assignment/list', { params: queryParams })
  tableData.value = res.data.records
  total.value = res.data.total
}

const loadMentors = async (companyId) => {
  const params = {}
  if (companyId) {
    params.companyId = companyId
  }
  const res = await request.get('/api/assignment/mentors', { params })
  mentorOptions.value = res.data || []
}

const openMentorDialog = async (row) => {
  mentorForm.assignmentId = row.id
  mentorForm.mentorId = row.mentorId || null
  mentorForm.remark = row.mentorId ? '企业导师调整' : '企业导师分配'
  await loadMentors(row.companyId)
  mentorDialogVisible.value = true
}

const submitMentorAssign = async () => {
  await request.put('/api/assignment/mentor', {
    assignmentId: mentorForm.assignmentId,
    mentorId: mentorForm.mentorId,
    remark: mentorForm.remark
  })
  ElMessage.success(isCompany.value ? '企业导师设置成功' : '企业导师调整成功')
  mentorDialogVisible.value = false
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

.pending-box {
  margin-top: 24px;
}

.pending-title {
  font-size: 14px;
  font-weight: 600;
  color: #2b4666;
}
</style>
