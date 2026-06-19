<!--
  Copyright (c) 2026 bmoqing
  All rights reserved.
  本代码仅供学习参考，未经许可不得用于商业用途。
-->

<template>
  <div class="container">
    <div class="header-box">
      <el-input
        v-model="queryParams.keyword"
        placeholder="搜索学生/标题/企业"
        style="width: 240px; margin-right: 10px"
        clearable
        @clear="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="canUpload" type="success" @click="openUploadDialog">上传协议</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px">
      <el-table-column v-if="!isStudent" prop="studentName" label="学生姓名" width="120" />
      <el-table-column v-if="!isStudent" prop="studentNo" label="学号" width="130" />
      <el-table-column prop="companyName" label="企业" width="150" />
      <el-table-column prop="title" label="协议标题" min-width="180" />
      <el-table-column label="协议链接" width="130">
        <template #default="scope">
          <el-link v-if="scope.row.contractUrl" type="primary" @click="openContract(scope.row.contractUrl)">查看文件</el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 0" type="warning">待审核</el-tag>
          <el-tag v-else-if="scope.row.status === 1" type="success">已通过</el-tag>
          <el-tag v-else-if="scope.row.status === 3" type="warning">打回待修改</el-tag>
          <el-tag v-else type="danger">已驳回</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="uploadTime" label="上传时间" width="170" />
      <el-table-column prop="reviewTime" label="审核时间" width="170" />
      <el-table-column prop="reviewRemark" label="审核意见" min-width="180" show-overflow-tooltip />
      <el-table-column prop="uploaderName" label="上传人" width="110" />
      <el-table-column prop="reviewerName" label="审核人" width="110" />
      <el-table-column v-if="canReview" label="操作" width="180">
        <template #default="scope">
          <div v-if="scope.row.status === 0">
            <el-button size="small" type="success" @click="openReviewDialog(scope.row, 1)">通过</el-button>
            <el-button size="small" type="danger" @click="openReviewDialog(scope.row, 2)">驳回</el-button>
          </div>
          <div v-else-if="(scope.row.status === 1 || scope.row.status === 2) && isAdmin">
            <el-button size="small" type="warning" @click="openRevokeDialog(scope.row)">打回</el-button>
          </div>
          <span v-else style="color: #999; font-size: 12px">已处理</span>
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

    <el-dialog v-model="uploadDialogVisible" title="上传实习协议" width="620px" append-to-body>
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="关联分配">
          <el-select v-model="uploadForm.assignmentId" placeholder="请选择分配记录" filterable style="width: 100%">
            <el-option
              v-for="item in assignmentOptions"
              :key="item.id"
              :label="`${item.studentName} - ${item.positionTitle} - ${item.companyName}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="协议标题">
          <el-input v-model="uploadForm.title" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="合同文件">
          <el-upload
            class="upload-block"
            :show-file-list="false"
            :http-request="uploadContractFile"
            :before-upload="beforeContractUpload"
            accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.txt,.zip,.rar"
          >
            <el-button type="primary" plain>选择并上传文件</el-button>
          </el-upload>
          <div class="upload-tip">支持文档/图片/压缩包，单文件不超过20MB</div>
          <div v-if="uploadForm.contractUrl" class="uploaded-preview">
            <el-link type="primary" @click="openContract(uploadForm.contractUrl)">已上传：{{ uploadedContractName || '查看文件' }}</el-link>
            <el-button type="danger" link @click="clearContractUpload">移除</el-button>
          </div>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="uploadForm.description" type="textarea" :rows="4" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUpload">提交上传</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewDialogVisible" title="审核协议" width="460px" append-to-body>
      <el-form :model="reviewForm" label-width="90px">
        <el-form-item label="审核结果">
          <el-tag :type="reviewForm.status === 1 ? 'success' : 'danger'">
            {{ reviewForm.status === 1 ? '通过' : '驳回' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="reviewForm.reviewRemark" type="textarea" :rows="4" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">确认审核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="revokeDialogVisible" title="打回已通过协议" width="460px" append-to-body>
      <el-form :model="revokeForm" label-width="90px">
        <el-form-item label="打回原因">
          <el-input v-model="revokeForm.reviewRemark" type="textarea" :rows="4" maxlength="255" show-word-limit placeholder="请输入打回原因，学生/企业需重新提交" />
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
const isAdmin = computed(() => userStore.user.role === 'ADMIN')
const canUpload = computed(() => ['STUDENT', 'COMPANY'].includes(userStore.user.role))
const canReview = computed(() => ['ADMIN', 'TEACHER'].includes(userStore.user.role))

const tableData = ref([])
const total = ref(0)
const assignmentOptions = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const uploadDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const revokeDialogVisible = ref(false)
const uploadedContractName = ref('')

const uploadForm = reactive({
  assignmentId: null,
  title: '',
  contractUrl: '',
  description: ''
})

const reviewForm = reactive({
  id: null,
  status: 1,
  reviewRemark: ''
})

const revokeForm = reactive({
  id: null,
  reviewRemark: ''
})

const load = async () => {
  const res = await request.get('/api/agreement/list', { params: queryParams })
  tableData.value = res.data.records || []
  total.value = res.data.total || 0
}

const loadAssignmentOptions = async () => {
  const res = await request.get('/api/assignment/list', {
    params: {
      pageNum: 1,
      pageSize: 200,
      keyword: ''
    }
  })
  assignmentOptions.value = (res.data.records || []).filter(item => item.status === 1)
}

const openUploadDialog = async () => {
  uploadForm.assignmentId = null
  uploadForm.title = ''
  uploadForm.contractUrl = ''
  uploadForm.description = ''
  uploadedContractName.value = ''
  await loadAssignmentOptions()
  uploadDialogVisible.value = true
}

const beforeContractUpload = (file) => {
  const isTooLarge = file.size / 1024 / 1024 > 20
  if (isTooLarge) {
    ElMessage.warning('文件不能超过20MB')
    return false
  }
  return true
}

const uploadContractFile = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)

  try {
    const res = await request.post('/api/file/upload', formData, {
      params: { bizType: 'agreement' },
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000
    })
    if (res.code !== 200 || !res.data?.url) {
      throw new Error(res.message || '上传失败')
    }

    uploadForm.contractUrl = res.data.url
    uploadedContractName.value = res.data.originalName || options.file.name
    options.onSuccess?.(res.data)
    ElMessage.success('合同文件上传成功')
  } catch (e) {
    options.onError?.(e)
  }
}

const clearContractUpload = () => {
  uploadForm.contractUrl = ''
  uploadedContractName.value = ''
}

const submitUpload = async () => {
  if (!uploadForm.assignmentId) {
    ElMessage.warning('请选择关联分配')
    return
  }
  if (!uploadForm.title) {
    ElMessage.warning('请填写协议标题')
    return
  }
  if (!uploadForm.contractUrl) {
    ElMessage.warning('请先上传合同文件')
    return
  }

  await request.post('/api/agreement/upload', uploadForm)
  ElMessage.success('上传成功，等待审核')
  uploadDialogVisible.value = false
  load()
}

const openReviewDialog = (row, status) => {
  reviewForm.id = row.id
  reviewForm.status = status
  reviewForm.reviewRemark = status === 1 ? '协议内容完整，同意通过' : '协议信息不完整，请补充后重提'
  reviewDialogVisible.value = true
}

const submitReview = async () => {
  await request.put('/api/agreement/review', reviewForm)
  ElMessage.success('审核完成')
  reviewDialogVisible.value = false
  load()
}

const openRevokeDialog = (row) => {
  revokeForm.id = row.id
  revokeForm.reviewRemark = '协议信息需要修正，请重新提交'
  revokeDialogVisible.value = true
}

const submitRevoke = async () => {
  try {
    const res = await request.put('/api/agreement/revoke', revokeForm)
    if (res.code === 200) {
      ElMessage.success('已打回，等待学生/企业重新提交')
      revokeDialogVisible.value = false
      load()
    } else {
      ElMessage.error(res.message || '打回失败')
    }
  } catch (e) {
    ElMessage.error('系统异常')
  }
}

const openContract = async (url) => {
  if (!url) {
    ElMessage.warning('协议链接为空，请上传后重试')
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
