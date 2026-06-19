<template>
  <div class="container">
    <div class="header-box">
      <!-- 只有学生显示新增按钮 -->
      <el-button type="primary" @click="handleAdd" v-if="isStudent">📝 写日志</el-button>
      
      <!-- 老师显示搜索框 -->
      <div v-else>
        <el-input v-model="keyword" placeholder="搜学生名或标题" style="width: 200px; margin-right: 10px;" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
    </div>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px;">
      <!-- 老师端显示是谁写的 -->
      <el-table-column prop="studentName" label="学生姓名" width="120" v-if="!isStudent" />
      
      <el-table-column prop="title" label="日志标题" width="180" />
      <el-table-column prop="content" label="工作内容" show-overflow-tooltip />
      <el-table-column prop="createTime" label="提交时间" width="180" />

      <el-table-column label="状态" width="110">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 3" type="warning">打回待修改</el-tag>
          <el-tag v-else-if="scope.row.score" type="success">已批阅</el-tag>
          <el-tag v-else type="info">待批阅</el-tag>
        </template>
      </el-table-column>
      
      <el-table-column label="老师评价" width="200">
        <template #default="scope">
           <div v-if="scope.row.score">
             <el-tag type="warning">{{ scope.row.score }}分</el-tag>
             <span style="margin-left:5px; font-size:12px">{{ scope.row.teacherComment }}</span>
           </div>
           <span v-else-if="scope.row.status === 3" style="color:#e6a23c; font-size:12px">{{ scope.row.teacherComment || '已打回' }}</span>
           <span v-else style="color:#999">暂未批阅</span>
        </template>
      </el-table-column>
//学生看不到别人的操作列
      <el-table-column label="操作" width="150" v-if="!isStudent">
        <template #default="scope">
          <el-button size="small" type="success" @click="handleReview(scope.row)">批阅</el-button>
          <el-button v-if="scope.row.score && scope.row.status !== 3" size="small" type="warning" @click="handleRevoke(scope.row)">打回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div style="margin-top:20px; text-align:right">
      <el-pagination 
         layout="total, prev, pager, next" 
         :total="total" 
         v-model:current-page="pageNum" 
         @current-change="load" 
      />
    </div>

    <!-- 写日志弹窗 (学生用) -->
    <el-dialog v-model="logDialogVisible" title="填写实习日志" width="500px" append-to-body>
      <el-form :model="logForm">
        <el-form-item label="标题">
          <el-input v-model="logForm.title" placeholder="例如：5月20日工作日报" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="logForm.content" type="textarea" :rows="5" placeholder="今天做了什么..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="logDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLog">提交</el-button>
      </template>
    </el-dialog>

    <!-- 批阅弹窗 (老师用) -->
    <el-dialog v-model="reviewDialogVisible" title="批阅日志" width="400px" append-to-body>
      <el-form :model="reviewForm">
        <el-form-item label="评分">
          <el-input-number v-model="reviewForm.score" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="reviewForm.teacherComment" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">确认</el-button>
      </template>
    </el-dialog>

    <!-- 打回弹窗 -->
    <el-dialog v-model="revokeDialogVisible" title="打回实习日志" width="400px" append-to-body>
      <el-form :model="revokeForm">
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
import { ref, reactive, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'// 1. 引入持久化 Pinia 状态
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
// 2. 利用 Vue3 computed 计算属性，实时监测当前登录角色是否为学生
const isStudent = computed(() => userStore.user.role === 'STUDENT')

const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const keyword = ref('')

// 弹窗控制
const logDialogVisible = ref(false)
const reviewDialogVisible = ref(false)
const revokeDialogVisible = ref(false)

// 表单数据
const logForm = reactive({ title: '', content: '' })
const reviewForm = reactive({ id: null, score: 90, teacherComment: '' })
const revokeForm = reactive({ id: null, teacherComment: '' })

// 加载数据
const load = async () => {
	// 学生端请求 /my 查询自己，教师端请求全局 /list 查询自己管辖的范围
  let url = isStudent.value ? '/api/log/my' : '/api/log/list'
  let params = { pageNum: pageNum.value, pageSize: 10 }
  
  if (isStudent.value) {
	     // 学生无需搜索关键词
  } else {
    params.keyword = keyword.value
  }

  const res = await request.get(url, { params })
  if (res.code === 200) {
    tableData.value = res.data.records
    total.value = res.data.total
  }
}

// 学生写日志
const handleAdd = () => {
  logForm.title = ''
  logForm.content = ''
  logDialogVisible.value = true
}
const submitLog = async () => {
  await request.post('/api/log', {
    ...logForm
  })
  ElMessage.success('提交成功')
  logDialogVisible.value = false
  load()
}

// 老师批阅
const handleReview = (row) => {
  reviewForm.id = row.id
  reviewForm.score = row.score || 85
  reviewForm.teacherComment = row.teacherComment || ''
  reviewDialogVisible.value = true
}
const submitReview = async () => {
  await request.put('/api/log/comment', reviewForm)
  ElMessage.success('批阅完成')
  reviewDialogVisible.value = false
  load()
}

// 打回
const handleRevoke = (row) => {
  revokeForm.id = row.id
  revokeForm.teacherComment = '日志内容需要修改，请修正后重新提交'
  revokeDialogVisible.value = true
}

const submitRevoke = async () => {
  try {
    const res = await request.put('/api/log/revoke', revokeForm)
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
.container { padding: 20px; background: white; border-radius: 8px; }
.header-box { margin-bottom: 20px; display: flex; align-items: center; }
</style>
