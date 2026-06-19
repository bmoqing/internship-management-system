<!--
  Copyright (c) 2026 bmoqing
  All rights reserved.
  本代码仅供学习参考，未经许可不得用于商业用途。
-->

<template>
  <div class="container">
    <!-- 1. 顶部搜索区 -->
    <div class="search-box">
      <el-input 
        v-model="queryParams.username" 
        placeholder="请输入用户名/姓名搜索" 
        style="width: 200px; margin-right: 10px;"
        clearable 
        @clear="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="handleAdd">新增用户</el-button>
    </div>

    <!-- 2. 表格区域 -->
    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px;">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="role" label="角色">
        <template #default="scope">
          <el-tag v-if="scope.row.role === 'ADMIN'" type="danger">管理员</el-tag>
          <el-tag v-else-if="scope.row.role === 'TEACHER'" type="warning">教师</el-tag>
          <el-tag v-else-if="scope.row.role === 'COMPANY'" type="info">企业</el-tag>
          <el-tag v-else type="success">学生</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="负责教师" width="160">
        <template #default="scope">
          <span v-if="scope.row.role === 'STUDENT'">{{ teacherName(scope.row.teacherId) }}</span>
          <span v-else style="color: #999">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="companyId" label="企业ID" width="100" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button size="small" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 3. 分页区域 -->
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

    <!-- 4. 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px" append-to-body>
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="密码" v-if="!form.id">
          <el-input v-model="form.password" placeholder="8-32位，至少3类字符" show-password />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%;">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="管理员" value="ADMIN" />
            <el-option label="企业" value="COMPANY" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责教师" v-if="form.role === 'STUDENT'">
          <el-select v-model="form.teacherId" filterable placeholder="请选择负责教师" style="width: 100%;">
            <el-option v-for="item in teacherOptions" :key="item.id" :label="`${item.name} (${item.username})`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定企业" v-if="form.role === 'COMPANY'">
          <el-select v-model="form.companyId" placeholder="请选择企业" style="width: 100%;">
            <el-option v-for="item in companyOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

// 数据定义
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const companyOptions = ref([])
const teacherOptions = ref([])

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  username: ''
})

// 表单数据
const form = reactive({
  id: null,
  username: '',
  name: '',
  password: '',
  role: 'STUDENT',
  teacherId: null,
  companyId: null
})

// 1. 加载数据
const load = async () => {
  const res = await request.get('/api/user', {
    params: queryParams
  })
  if (res.code === 200) {
    tableData.value = res.data.records
    total.value = res.data.total
  }
}

const loadCompanyOptions = async () => {
  const res = await request.get('/api/company/options')
  companyOptions.value = res.data || []
}

const loadTeacherOptions = async () => {
  const res = await request.get('/api/auth/teacher-options')
  teacherOptions.value = res.data || []
}

const teacherName = (teacherId) => {
  if (!teacherId) return '-'
  const teacher = teacherOptions.value.find(item => item.id === teacherId)
  return teacher ? teacher.name : `教师ID:${teacherId}`
}

// 2. 点击新增
const handleAdd = () => {
  // 重置表单
  form.id = null
  form.username = ''
  form.name = ''
  form.password = ''
  form.role = 'STUDENT'
  form.teacherId = null
  form.companyId = null
  dialogVisible.value = true
}

// 3. 点击编辑
const handleEdit = (row) => {
  // 把行数据复制给表单 (深拷贝防止直接修改表格显示)
  Object.assign(form, row)
  form.password = '' // 编辑时不回显密码
  dialogVisible.value = true
}

// 4. 提交保存
const save = async () => {
  if (!form.username) {
    ElMessage.warning('请填写用户名')
    return
  }
  if (!form.name) {
    ElMessage.warning('请填写姓名')
    return
  }

  if (!form.id && !form.password) {
    ElMessage.warning('请填写初始密码')
    return
  }

  if (form.role === 'COMPANY' && !form.companyId) {
    ElMessage.warning('企业账号请绑定企业')
    return
  }

  if (form.role === 'STUDENT' && !form.teacherId) {
    ElMessage.warning('学生账号请绑定负责教师')
    return
  }

  if (form.role === 'COMPANY') {
    form.teacherId = null
  } else if (form.role === 'STUDENT') {
    form.companyId = null
  } else {
    form.companyId = null
    form.teacherId = null
  }

  if (form.id) {
    // 修改
    await request.put('/api/user', form)
  } else {
    // 新增
    await request.post('/api/user', form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  load() // 刷新表格
}

// 5. 删除
const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该用户吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/api/user/${id}`)
    ElMessage.success('删除成功')
    load()
  }).catch(() => {})
}

// 页面加载时自动查询
onMounted(() => {
  loadCompanyOptions()
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
