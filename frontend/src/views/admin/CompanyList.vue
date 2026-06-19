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
        placeholder="搜索企业名/联系人/电话"
        style="width: 240px; margin-right: 10px"
        clearable
        @clear="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="handleAdd">新增企业</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="企业名称" min-width="180" />
      <el-table-column prop="contact" label="联系人" width="120" />
      <el-table-column prop="phone" label="联系电话" width="160" />
      <el-table-column prop="address" label="企业地址" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 1" type="success">正常</el-tag>
          <el-tag v-else type="info">停用</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="180">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑企业' : '新增企业'" width="560px" append-to-body>
      <el-form :model="form" label-width="90px">
        <el-form-item label="企业名称">
          <el-input v-model="form.name" placeholder="请输入企业名称" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contact" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="企业地址">
          <el-input v-model="form.address" placeholder="请输入企业地址" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">正常</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
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
  name: '',
  contact: '',
  phone: '',
  address: '',
  status: 1
})

const load = async () => {
  const res = await request.get('/api/company', { params: queryParams })
  if (res.code === 200) {
    tableData.value = res.data.records
    total.value = res.data.total
  }
}

const handleAdd = () => {
  form.id = null
  form.name = ''
  form.contact = ''
  form.phone = ''
  form.address = ''
  form.status = 1
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

const save = async () => {
  if (!form.name) {
    ElMessage.warning('请填写企业名称')
    return
  }

  if (form.id) {
    await request.put('/api/company', form)
  } else {
    await request.post('/api/company', form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  load()
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定删除该企业吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/api/company/${id}`)
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
