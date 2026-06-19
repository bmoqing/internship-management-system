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
        placeholder="搜索操作人/动作/对象/详情"
        style="width: 260px; margin-right: 10px"
        clearable
        @clear="load"
      />
      <el-select v-model="queryParams.action" placeholder="动作" clearable style="width: 170px; margin-right: 10px">
        <el-option v-for="item in actionOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="queryParams.targetType" placeholder="对象类型" clearable style="width: 150px; margin-right: 10px">
        <el-option v-for="item in targetTypeOptions" :key="item" :label="item" :value="item" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px">
      <el-table-column prop="createTime" label="时间" width="170" />
      <el-table-column label="操作人" width="150">
        <template #default="scope">
          <div>{{ scope.row.operatorName || '-' }}</div>
          <div class="sub-text">{{ scope.row.operatorUsername || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="operatorRole" label="角色" width="100" />
      <el-table-column prop="action" label="动作" width="170" />
      <el-table-column prop="targetType" label="对象" width="120" />
      <el-table-column prop="targetId" label="对象ID" width="90" />
      <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
      <el-table-column prop="ipAddress" label="IP" width="130" />
      <el-table-column label="请求" width="150">
        <template #default="scope">
          <div>{{ scope.row.requestMethod || '-' }}</div>
          <div class="sub-text">{{ scope.row.requestPath || '-' }}</div>
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
  keyword: '',
  action: '',
  targetType: ''
})

const actionOptions = [
  'APPLICATION_REVIEW',
  'APPLICATION_ASSIGN_TEACHER',
  'ASSIGNMENT_CREATE',
  'ASSIGNMENT_MENTOR_UPDATE',
  'AGREEMENT_UPLOAD',
  'AGREEMENT_REVIEW',
  'COMPANY_EVALUATION_CREATE',
  'COMPANY_EVALUATION_UPDATE',
  'COMPANY_FEEDBACK_CREATE',
  'COMPANY_FEEDBACK_UPDATE',
  'INCIDENT_REPORT',
  'INCIDENT_HANDLE',
  'APPEAL_SUBMIT',
  'APPEAL_TEACHER_REVIEW',
  'APPEAL_ADMIN_REVIEW',
  'APPEAL_CLOSE',
  'SCORE_CREATE',
  'SCORE_UPDATE',
  'LOG_REVIEW',
  'REPORT_REVIEW',
  'USER_CREATE',
  'USER_UPDATE',
  'USER_DELETE',
  'COMPANY_CREATE',
  'COMPANY_UPDATE',
  'COMPANY_DELETE',
  'POSITION_CREATE',
  'POSITION_UPDATE',
  'POSITION_DELETE',
  'NOTICE_CREATE',
  'NOTICE_UPDATE',
  'NOTICE_DELETE'
]

const targetTypeOptions = [
  'APPLICATION',
  'ASSIGNMENT',
  'AGREEMENT',
  'COMPANY_EVALUATION',
  'COMPANY_FEEDBACK',
  'INCIDENT',
  'APPEAL',
  'SCORE',
  'LOG',
  'REPORT',
  'USER',
  'COMPANY',
  'POSITION',
  'NOTICE'
]

const load = async () => {
  const res = await request.get('/api/audit/list', { params: queryParams })
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

.search-box {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0;
}

.sub-text {
  font-size: 12px;
  color: #909399;
}
</style>
