<template>
  <div class="container">
    <h2>📄 我的实习申请记录</h2>
    
    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px;">
      <el-table-column prop="positionTitle" label="申请岗位" />
      <el-table-column prop="companyName" label="所属企业" />
      <el-table-column prop="reviewTeacherName" label="审核教师" width="120" />
      <el-table-column prop="applyTime" label="申请时间" width="180" />
      
      <el-table-column label="审核状态" width="160">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 0" type="info">⏳ 待企业预审</el-tag>
          <el-tag v-else-if="scope.row.status === 1" type="warning">⏳ 待教师审核</el-tag>
          <el-tag v-else-if="scope.row.status === 2" type="warning">🧾 待管理员终审</el-tag>
          <el-tag v-else-if="scope.row.status === 3" type="danger">❌ 已驳回</el-tag>
          <el-tag v-else-if="scope.row.status === 4" type="primary">📌 待分配</el-tag>
          <el-tag v-else-if="scope.row.status === 5" type="success">🎯 已分配</el-tag>
          <el-tag v-else type="info">未知状态</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column prop="remark" label="审核意见" />
      <el-table-column prop="teacherName" label="指导教师" width="120" />
      <el-table-column prop="mentorName" label="企业导师" width="120" />
    </el-table>

    <!-- 分页 -->
    <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const load = async () => {
  const res = await request.get('/api/application/my', {
    params: {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
  })
  if (res.code === 200) {
    tableData.value = res.data.records
    total.value = res.data.total
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
