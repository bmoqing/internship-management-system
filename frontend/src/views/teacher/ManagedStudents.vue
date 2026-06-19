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
        placeholder="搜索学生姓名/学号"
        style="width: 220px; margin-right: 10px"
        clearable
        @clear="load"
      />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px">
      <el-table-column prop="username" label="学号" width="140" />
      <el-table-column prop="name" label="学生姓名" width="120" />
      <el-table-column prop="positionTitle" label="当前岗位" min-width="150">
        <template #default="scope">
          <span>{{ scope.row.positionTitle || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="companyName" label="企业" width="160">
        <template #default="scope">
          <span>{{ scope.row.companyName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="assignTime" label="分配时间" width="170">
        <template #default="scope">
          <span>{{ scope.row.assignTime || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="在岗状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.assignmentStatus === 1 ? 'success' : 'info'">
            {{ scope.row.assignmentStatus === 1 ? '在岗' : '未分配' }}
          </el-tag>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import request from '@/utils/request'

const tableData = ref([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const load = async () => {
  const res = await request.get('/api/user/managed-students', { params: queryParams })
  if (res.code === 200) {
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
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
</style>
