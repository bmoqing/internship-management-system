<!--
  Copyright (c) 2026 bmoqing
  All rights reserved.
  本代码仅供学习参考，未经许可不得用于商业用途。
-->

<template>
  <div class="resume-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的简历</span>
        </div>
      </template>
      <div class="resume-content">
        <el-alert
          title="提示"
          type="info"
          description="为了申请企业实习岗位，您必须上传个人简历。仅支持 PDF, DOC, DOCX, PNG, JPG 格式文件，最大20MB。"
          show-icon
          style="margin-bottom: 20px"
        />

        <el-form label-width="100px">
          <el-form-item label="当前简历">
            <div v-if="resumeUrl" class="current-resume">
              <span style="color: #67c23a; margin-right: 15px;">
                <el-icon><Check /></el-icon> 已上传简历
              </span>
              <el-button type="primary" link @click="viewResume">
                点击预览 / 下载
              </el-button>
            </div>
            <div v-else style="color: #f56c6c;">
              <el-icon><Close /></el-icon> 尚未上传简历，请尽快上传！
            </div>
          </el-form-item>

          <el-form-item label="上传新简历">
            <el-upload
              class="upload-demo"
              drag
              :action="uploadAction"
              :headers="uploadHeaders"
              :data="{ bizType: 'resume' }"
              :before-upload="beforeUpload"
              :on-success="handleSuccess"
              :on-error="handleError"
              :show-file-list="false"
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
            >
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">
                将文件拖到此处，或 <em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  支持 PDF / Word / 图片格式，重新上传将覆盖旧简历
                </div>
              </template>
            </el-upload>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Check, Close } from '@element-plus/icons-vue'
import request from '@/utils/request'

const uploadAction = 'http://localhost:8080/api/file/upload'
const uploadHeaders = ref({})
const resumeUrl = ref('')

const beforeUpload = (file) => {
  const allowedExtensions = ['pdf', 'doc', 'docx', 'png', 'jpg', 'jpeg']
  const fileExt = file.name.split('.').pop().toLowerCase()
  if (!allowedExtensions.includes(fileExt)) {
    ElMessage.error('只允许上传 PDF, DOC, DOCX, PNG, JPG 格式的文件!')
    return false
  }
  const isLt20M = file.size / 1024 / 1024 < 20
  if (!isLt20M) {
    ElMessage.error('上传简历大小不能超过 20MB!')
    return false
  }
  return true
}

const handleSuccess = async (response, uploadFile) => {
  if (response.code === 200) {
    const filePath = response.data.url
    resumeUrl.value = filePath
    
    // Save to user profile
    try {
      const res = await request.put('/api/user/resume', { resumeUrl: filePath })
      if (res.code === 200) {
        ElMessage.success('简历上传并保存成功！')
        
        // update local storage user if it has resumeUrl 
        const userStr = localStorage.getItem('user')
        if (userStr) {
          const user = JSON.parse(userStr)
          user.resumeUrl = filePath
          localStorage.setItem('user', JSON.stringify(user))
        }
      } else {
        ElMessage.error(res.message || '简历保存失败')
      }
    } catch (e) {
      ElMessage.error('简历保存异常')
    }
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleError = (error) => {
  ElMessage.error('上传过程中出现错误')
  console.error(error)
}

const viewResume = async () => {
  if (!resumeUrl.value) return
  try {
    const res = await request.get('/api/file/access-url', {
      params: { path: resumeUrl.value }
    })
    if (res.code === 200) {
      window.open('http://localhost:8080' + res.data, '_blank')
    } else {
      ElMessage.error(res.message || '获取简历链接失败')
    }
  } catch (e) {
    ElMessage.error('获取链接异常')
  }
}

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    const user = JSON.parse(userStr)
    if (user.token) {
      uploadHeaders.value = {
        Authorization: 'Bearer ' + user.token
      }
    }
    if (user.resumeUrl) {
      resumeUrl.value = user.resumeUrl
    }
  }
})
</script>

<style scoped>
.resume-container {
  padding: 20px;
}
.card-header {
  font-weight: bold;
}
.current-resume {
  display: flex;
  align-items: center;
}
.el-upload__tip {
  margin-top: 10px;
  color: #999;
}
</style>
