<!--
  Copyright (c) 2026 bmoqing
  All rights reserved.
  本代码仅供学习参考，未经许可不得用于商业用途。
-->

<template>
  <div class="container">
    <div class="header-box">
      <template v-if="isStudent">
        <el-button type="primary" @click="openSubmitDialog">提交报告</el-button>
      </template>
      <template v-else>
        <el-input
          v-model="queryParams.keyword"
          placeholder="搜索学生姓名/学号/报告标题"
          style="width: 260px; margin-right: 10px"
          clearable
          @clear="load"
        />
        <el-button type="primary" @click="load">查询</el-button>
      </template>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px">
      <el-table-column v-if="!isStudent" prop="studentName" label="学生姓名" width="120" />
      <el-table-column v-if="!isStudent" prop="studentNo" label="学号" width="140" />
      <el-table-column prop="title" label="报告标题" min-width="180" />
      <el-table-column label="报告周期" width="200">
        <template #default="scope">
          <span>{{ scope.row.periodStart || '-' }} ~ {{ scope.row.periodEnd || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="submitTime" label="提交时间" width="170" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 2" type="success">已批阅</el-tag>
          <el-tag v-else-if="scope.row.status === 3" type="warning">打回待修改</el-tag>
          <el-tag v-else type="info">待批阅</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="score" label="评分" width="90" />
      <el-table-column prop="teacherComment" label="教师评语" min-width="180" show-overflow-tooltip />
      <el-table-column prop="reviewTime" label="批阅时间" width="170" />
      <el-table-column v-if="!isStudent" label="操作" width="150">
        <template #default="scope">
          <el-button v-if="scope.row.status !== 2" size="small" type="primary" @click="openReviewDialog(scope.row)">批阅</el-button>
          <el-button v-if="scope.row.status === 2" size="small" type="warning" @click="openRevokeDialog(scope.row)">打回</el-button>
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

    <el-dialog v-model="submitDialogVisible" title="提交实习报告" width="620px" append-to-body>
      <el-form :model="submitForm" label-width="90px">
        <el-form-item label="报告标题">
          <el-input v-model="submitForm.title" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="报告周期">
          <el-date-picker
            v-model="periodRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="报告内容">
          <el-input v-model="submitForm.content" type="textarea" :rows="8" maxlength="5000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReport">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialogVisible" title="批阅实习报告" width="440px" append-to-body>
      <el-form :model="reviewForm" label-width="90px">
        <el-form-item label="报告标题">
          <el-input v-model="reviewForm.title" disabled />
        </el-form-item>
        <el-form-item label="报告评分">
          <el-input-number v-model="reviewForm.score" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="教师评语">
          <el-input v-model="reviewForm.teacherComment" type="textarea" :rows="4" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交批阅</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="revokeDialogVisible" title="打回实习报告" width="440px" append-to-body>
      <el-form :model="revokeForm" label-width="90px">
        <el-form-item label="报告标题">
          <el-input v-model="revokeForm.title" disabled />
        </el-form-item>
        <el-form-item label="打回原因">
          <el-input v-model="revokeForm.teacherComment" type="textarea" :rows="4" maxlength="255" show-word-limit placeholder="请输入打回原因，学生需修改后重新提交" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="revokeDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="submitRevoke">确认打回</el-button>
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
const isStudent = computed(() => userStore.user.role === 'STUDENT')

const tableData = ref([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const submitDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const revokeDialogVisible = ref(false)
const periodRange = ref([])

const submitForm = reactive({
  title: '',
  content: ''
})

const reviewForm = reactive({
  id: null,
  title: '',
  score: 85,
  teacherComment: ''
})

const revokeForm = reactive({
  id: null,
  title: '',
  teacherComment: ''
})

const load = async () => {
  const url = isStudent.value ? '/api/report/my' : '/api/report/list'
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

const openSubmitDialog = () => {
  submitForm.title = ''
  submitForm.content = ''
  periodRange.value = []
  submitDialogVisible.value = true
}

const submitReport = async () => {
  if (!submitForm.title) {
    ElMessage.warning('请填写报告标题')
    return
  }
  if (!submitForm.content) {
    ElMessage.warning('请填写报告内容')
    return
  }

  await request.post('/api/report', {
    title: submitForm.title,
    content: submitForm.content,
    periodStart: periodRange.value?.[0] || null,
    periodEnd: periodRange.value?.[1] || null
  })
  ElMessage.success('提交成功')
  submitDialogVisible.value = false
  load()
}

const openReviewDialog = (row) => {
  reviewForm.id = row.id
  reviewForm.title = row.title
  reviewForm.score = row.score ?? 85
  reviewForm.teacherComment = row.teacherComment || ''
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  await request.put('/api/report/review', {
    id: reviewForm.id,
    score: reviewForm.score,
    teacherComment: reviewForm.teacherComment
  })
  ElMessage.success('批阅成功')
  reviewDialogVisible.value = false
  load()
}

const openRevokeDialog = (row) => {
  revokeForm.id = row.id
  revokeForm.title = row.title
  revokeForm.teacherComment = '报告内容需要修改，请修正后重新提交'
  revokeDialogVisible.value = true
}

const submitRevoke = async () => {
  try {
    const res = await request.put('/api/report/revoke', {
      id: revokeForm.id,
      teacherComment: revokeForm.teacherComment
    })
    if (res.code === 200) {
      ElMessage.success('已打回，等待学生修改后重新提交')
      revokeDialogVisible.value = false
      load()
    } else {
      ElMessage.error(res.message || '打回失败')
    }
  } catch (e) {
    ElMessage.error('系统异常')
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
</style>
