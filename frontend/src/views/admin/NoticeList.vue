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
        placeholder="搜索公告标题/内容"
        style="width: 240px; margin-right: 10px"
        clearable
        @clear="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="handleAdd">发布公告</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="公告标题" min-width="180" />
      <el-table-column label="等级" width="100">
        <template #default="scope">
          <el-tag :type="levelTagType(scope.row.level)">{{ levelText(scope.row.level) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 1" type="success">启用</el-tag>
          <el-tag v-else type="info">停用</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="公告内容" min-width="260" show-overflow-tooltip />
      <el-table-column prop="createTime" label="发布时间" width="180" />
      <el-table-column label="操作" width="170">
        <template #default="scope">
          <el-button size="small" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 20px; display: flex; justify-content: flex-end">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="620px" append-to-body>
      <el-form :model="form" label-width="90px">
        <el-form-item label="公告标题">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="公告等级">
          <el-select v-model="form.level" style="width: 100%">
            <el-option label="普通" value="INFO" />
            <el-option label="提醒" value="WARN" />
            <el-option label="紧急" value="URGENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="公告内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            maxlength="500"
            show-word-limit
            placeholder="请输入公告内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const form = reactive({
  id: null,
  title: '',
  content: '',
  level: 'INFO',
  status: 1
})

const levelText = (level) => {
  if (level === 'URGENT') return '紧急'
  if (level === 'WARN') return '提醒'
  return '普通'
}

const levelTagType = (level) => {
  if (level === 'URGENT') return 'danger'
  if (level === 'WARN') return 'warning'
  return 'info'
}

const load = async () => {
  const res = await request.get('/api/notice', { params: queryParams })
  if (res.code === 200) {
    tableData.value = res.data.records
    total.value = res.data.total
  }
}

const handleAdd = () => {
  form.id = null
  form.title = ''
  form.content = ''
  form.level = 'INFO'
  form.status = 1
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const save = async () => {
  if (!form.title) {
    ElMessage.warning('请填写公告标题')
    return
  }
  if (!form.content) {
    ElMessage.warning('请填写公告内容')
    return
  }

  if (form.id) {
    await request.put('/api/notice', form)
  } else {
    await request.post('/api/notice', form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  load()
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该公告吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/api/notice/${id}`)
    ElMessage.success('删除成功')
    load()
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
