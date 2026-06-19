<template>
  <div class="container">
    <div class="formula-box">
      成绩公式：教师评定分 * {{ weights.teacher }} + 企业评定分 * {{ weights.company }} + 日志均分 * {{ weights.log }} + 考勤得分 * {{ weights.attendance }}
    </div>

    <template v-if="isStudent">
      <el-card v-if="myScore" class="score-card">
        <div class="row"><span>教师评定分：</span><b>{{ myScore.teacherScore }}</b></div>
        <div class="row"><span>企业评定分：</span><b>{{ myScore.companyScore ?? '-' }}</b></div>
        <div class="row"><span>日志均分：</span><b>{{ myScore.extraScore }}</b></div>
        <div class="row"><span>考勤得分：</span><b>{{ myScore.attendanceScore }}</b></div>
        <div class="row final"><span>最终成绩：</span><b>{{ myScore.finalScore }}</b></div>
        <div class="row"><span>企业评语：</span><span>{{ myScore.companyComment || '暂无' }}</span></div>
        <div class="row"><span>教师评语：</span><span>{{ myScore.teacherComment || '暂无' }}</span></div>
        <div class="row"><span>更新时间：</span><span>{{ myScore.updateTime || '-' }}</span></div>
      </el-card>
      <el-empty v-else description="暂未完成教师评定，暂无最终成绩" />
    </template>

    <template v-else>
      <div class="search-box">
        <el-input
          v-model="queryParams.keyword"
          placeholder="搜索学生姓名/学号"
          style="width: 220px; margin-right: 10px"
          clearable
          @clear="load"
        />
        <el-button type="primary" @click="load">查询</el-button>
      </div>

      <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px">
        <el-table-column prop="studentName" label="学生姓名" width="130" />
        <el-table-column prop="studentNo" label="学号" width="140" />
        <el-table-column prop="companyScore" label="企业评定分" width="100" />
        <el-table-column prop="teacherScore" label="教师评定分" width="100" />
        <el-table-column prop="extraScore" label="日志均分" width="90" />
        <el-table-column prop="attendanceScore" label="考勤得分" width="90" />
        <el-table-column prop="finalScore" label="最终成绩" width="90" />
        <el-table-column prop="companyComment" label="企业评语" min-width="150" show-overflow-tooltip />
        <el-table-column prop="teacherComment" label="教师评语" min-width="150" show-overflow-tooltip />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button size="small" type="primary" @click="openDialog(scope.row)">评定</el-button>
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
    </template>

    <!-- 管理员评定弹窗：可修改全部分数 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" append-to-body>
      <el-form :model="form" label-width="100px">
        <el-form-item label="评定学生">
          <el-input v-model="form.studentName" disabled />
        </el-form-item>

        <template v-if="isAdmin">
          <el-form-item label="教师评定分">
            <el-input-number v-model="form.teacherScore" :min="0" :max="100" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="企业评定分">
            <el-input-number v-model="form.companyScore" :min="0" :max="100" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="考勤得分">
            <el-input-number v-model="form.attendanceScore" :min="0" :max="100" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="日志均分">
            <el-input-number v-model="form.extraScore" :min="0" :max="100" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最终成绩">
            <el-input-number v-model="form.finalScore" :min="0" :max="100" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="教师评语">
            <el-input v-model="form.teacherComment" type="textarea" :rows="3" placeholder="请输入教师评语" />
          </el-form-item>
          <el-form-item label="企业评语">
            <el-input v-model="form.companyComment" type="textarea" :rows="3" placeholder="请输入企业评语" />
          </el-form-item>
        </template>

        <template v-else-if="isCompany">
          <el-form-item label="企业评定分">
            <el-input-number v-model="form.companyScore" :min="0" :max="100" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="企业评语">
            <el-input v-model="form.companyComment" type="textarea" :rows="4" placeholder="请输入评语" />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="教师评定分">
            <el-input-number v-model="form.teacherScore" :min="0" :max="100" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="教师评语">
            <el-input v-model="form.teacherComment" type="textarea" :rows="4" placeholder="请输入评语" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEvaluate">提交评定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const isStudent = computed(() => userStore.user.role === 'STUDENT')
const isCompany = computed(() => userStore.user.role === 'COMPANY')
const isAdmin = computed(() => userStore.user.role === 'ADMIN')

const dialogTitle = computed(() => {
  if (isAdmin.value) return '管理员成绩修改'
  if (isCompany.value) return '企业成绩评定'
  return '教师成绩评定'
})

const tableData = ref([])
const total = ref(0)
const myScore = ref(null)
const dialogVisible = ref(false)

const weights = reactive({
  teacher: '0.6',
  company: '0.0',
  log: '0.3',
  attendance: '0.1'
})

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const form = reactive({
  studentId: null,
  studentName: '',
  teacherScore: 85,
  teacherComment: '',
  companyScore: 85,
  companyComment: '',
  attendanceScore: 0,
  extraScore: 0,
  finalScore: 0
})

const loadWeights = async () => {
  try {
    const res = await request.get('/api/config/weights')
    if (res.code === 200 && res.data) {
      for (const item of res.data) {
        if (item.configKey === 'score.weight.teacher') weights.teacher = item.configValue
        if (item.configKey === 'score.weight.company') weights.company = item.configValue
        if (item.configKey === 'score.weight.log') weights.log = item.configValue
        if (item.configKey === 'score.weight.attendance') weights.attendance = item.configValue
      }
    }
  } catch (e) {
    // 使用默认值
  }
}

const load = async () => {
  if (isStudent.value) {
    const res = await request.get('/api/score/my')
    if (res.code === 200) {
      myScore.value = res.data
    }
    return
  }

  const res = await request.get('/api/score/list', { params: queryParams })
  if (res.code === 200) {
    tableData.value = res.data.records
    total.value = res.data.total
  }
}

const openDialog = (row) => {
  form.studentId = row.studentId
  form.studentName = row.studentName
  form.teacherScore = row.teacherScore ?? 85
  form.teacherComment = row.teacherComment || ''
  form.companyScore = row.companyScore ?? 85
  form.companyComment = row.companyComment || ''
  form.attendanceScore = row.attendanceScore ?? 0
  form.extraScore = row.extraScore ?? 0
  form.finalScore = row.finalScore ?? 0
  dialogVisible.value = true
}

const submitEvaluate = async () => {
  if (!form.studentId) {
    ElMessage.warning('未选择学生')
    return
  }

  let endpoint
  let payload

  if (isAdmin.value) {
    endpoint = '/api/score/admin-update'
    payload = {
      studentId: form.studentId,
      teacherScore: form.teacherScore,
      companyScore: form.companyScore,
      attendanceScore: form.attendanceScore,
      extraScore: form.extraScore,
      finalScore: form.finalScore,
      teacherComment: form.teacherComment,
      companyComment: form.companyComment
    }
  } else if (isCompany.value) {
    endpoint = '/api/score/company-evaluate'
    payload = {
      studentId: form.studentId,
      companyScore: form.companyScore,
      companyComment: form.companyComment
    }
  } else {
    endpoint = '/api/score/evaluate'
    payload = {
      studentId: form.studentId,
      teacherScore: form.teacherScore,
      teacherComment: form.teacherComment
    }
  }

  try {
    const res = await request.post(endpoint, payload)
    if (res.code === 200) {
      ElMessage.success('评定成功')
      dialogVisible.value = false
      load()
    } else {
      ElMessage.error(res.message || '评定失败')
    }
  } catch (e) {
    ElMessage.error('系统异常')
  }
}

onMounted(() => {
  loadWeights()
  load()
})
</script>

<style scoped>
.container {
  padding: 20px;
  background: white;
  border-radius: 8px;
}

.formula-box {
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 6px;
  background: #f5f7fa;
  color: #606266;
  font-size: 14px;
}

.score-card .row {
  display: flex;
  justify-content: space-between;
  line-height: 34px;
  border-bottom: 1px dashed #ebeef5;
}

.score-card .final {
  color: #409eff;
  font-size: 18px;
  font-weight: 700;
}
</style>
