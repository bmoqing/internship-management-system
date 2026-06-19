<!--
  Copyright (c) 2026 bmoqing
  All rights reserved.
  本代码仅供学习参考，未经许可不得用于商业用途。
-->

<template>
  <div class="container">
    <div class="search-box">
      <el-input 
        v-model="queryParams.keyword" 
        placeholder="搜索岗位或公司" 
        style="width: 200px; margin-right: 10px;"
        clearable 
        @clear="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <!-- 只有非学生角色才能新增 (简单的权限控制) -->
      <el-button type="success" @click="handleAdd" v-if="userStore.user.role !== 'STUDENT'">发布岗位</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px;">
      <el-table-column prop="title" label="岗位名称" width="180" />
      <el-table-column prop="companyName" label="企业名称" width="180" />
      <el-table-column prop="location" label="工作地点" width="120" />
      <el-table-column prop="status" label="招聘状态" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 1" type="success">招聘中</el-tag>
          <el-tag v-else type="info">已截止</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditStatus" label="审核状态" width="100" v-if="userStore.user.role !== 'STUDENT'">
        <template #default="scope">
          <el-tag v-if="scope.row.auditStatus === 1" type="success">已通过</el-tag>
          <el-tag v-else-if="scope.row.auditStatus === 2" type="danger">已驳回</el-tag>
          <el-tag v-else type="warning">待审核</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="岗位描述" show-overflow-tooltip />
      <el-table-column prop="createTime" label="发布时间" width="180" />
      
      <el-table-column label="操作" width="280">
        <template #default="scope">
          <!-- 学生只能看，管理员/老师可以编辑 -->
          <el-button size="small" type="primary" @click="handleEdit(scope.row)" v-if="userStore.user.role !== 'STUDENT'">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row.id)" v-if="userStore.user.role !== 'STUDENT'">删除</el-button>
          
          <!-- 管理员审核按钮 -->
          <el-button 
            size="small" 
            type="warning" 
            @click="handleAudit(scope.row)" 
            v-if="userStore.user.role === 'ADMIN' && scope.row.auditStatus !== 1">
            审核
          </el-button>

          <!-- 学生显示申请按钮 -->
          <el-button size="small" type="warning" :disabled="scope.row.status !== 1" v-if="userStore.user.role === 'STUDENT'" @click="handleApply(scope.row)">申请</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="load"
        @current-change="load"
      />
    </div>

    <!-- 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑岗位' : '发布岗位'" width="600px" append-to-body>
      <el-form :model="form" label-width="80px">
        <el-form-item label="企业名称">
          <el-input v-model="form.companyName" />
        </el-form-item>
        <el-form-item label="岗位名称">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="工作地点">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">招聘中</el-radio>
            <el-radio :label="0">已截止</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="岗位描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditDialogVisible" title="岗位审核" width="400px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" placeholder="驳回时建议填写原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="auditDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAudit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const auditDialogVisible = ref(false)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const form = reactive({
  id: null,
  companyName: '',
  title: '',
  location: '',
  description: '',
  status: 1
})

const auditForm = reactive({
  id: null,
  auditStatus: 1,
  auditRemark: ''
})

const load = async () => {
  const res = await request.get('/api/position', { params: queryParams })
  if (res.code === 200) {
    tableData.value = res.data.records
    total.value = res.data.total
  }
}

const handleAdd = () => {
  Object.keys(form).forEach(key => form[key] = key === 'status' ? 1 : '')
  form.id = null
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const save = async () => {
  if (form.id) {
    await request.put('/api/position', form)
  } else {
    await request.post('/api/position', form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  load()
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/api/position/${id}`)
    ElMessage.success('删除成功')
    load()
  })
}

const handleAudit = (row) => {
  auditForm.id = row.id
  auditForm.auditStatus = 1
  auditForm.auditRemark = ''
  auditDialogVisible.value = true
}

const submitAudit = async () => {
  await request.put('/api/position/audit', auditForm)
  ElMessage.success('审核操作成功')
  auditDialogVisible.value = false
  load()
}

const handleApply = (row) => {
  if (row.status !== 1) {
    ElMessage.warning('该岗位已停止招聘')
    return
  }

  ElMessageBox.confirm(`确定要申请【${row.companyName}】的【${row.title}】岗位吗？`, '提示', {
    confirmButtonText: '确定申请',
    cancelButtonText: '再想想',
    type: 'info'
  }).then(async () => {
    // 构造请求参数
    const data = {
      positionId: row.id
    }
    
    try {
      const res = await request.post('/api/application', data)
      if (res.code === 200) {
        ElMessage.success('申请提交成功，请在“我的申请”中查看进度')
      } else {
        ElMessage.error(res.message) // 比如重复申请会报错
      }
    } catch (e) {
      console.error(e)
    }
  }).catch(() => {})
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
</style>
