<!--
  Copyright (c) 2026 bmoqing
  All rights reserved.
  本代码仅供学习参考，未经许可不得用于商业用途。
-->

<template>
  <div class="config-list-container">
    <div class="page-header">
      <div class="header-content">
        <h2>系统配置</h2>
        <span class="subtitle">管理成绩评定权重及预警阈值</span>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Check" @click="saveConfigs">保存全部配置</el-button>
      </div>
    </div>

    <el-card class="box-card" shadow="never">
      <el-alert
        title="注意"
        description="此处的修改会实时生效。成绩权重之和建议保持为 1.0 (例如 0.6 + 0.3 + 0.1 = 1.0)。若企业权重设为0，则代表企业打分不计入总评。"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />

      <el-form label-width="200px" v-loading="loading">
        <div v-for="(item, index) in configList" :key="item.id" class="config-item-row">
          <el-form-item :label="item.description">
            <el-input v-model="item.configValue" style="width: 250px;">
              <template #append v-if="item.configKey.startsWith('score.weight')">权重 (0.0~1.0)</template>
              <template #append v-if="item.configKey.includes('days')">天</template>
              <template #append v-if="item.configKey.includes('missing')">篇/周</template>
            </el-input>
          </el-form-item>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'

const loading = ref(false)
const configList = ref([])

const fetchConfigs = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/config/list')
    if (res.code === 200) {
      configList.value = res.data
    } else {
      ElMessage.error(res.message || '获取配置失败')
    }
  } catch (e) {
    ElMessage.error('获取异常')
  } finally {
    loading.value = false
  }
}

const saveConfigs = async () => {
  // Validate weights
  let weightSum = 0
  for (const item of configList.value) {
    if (item.configKey.startsWith('score.weight')) {
      const val = parseFloat(item.configValue)
      if (isNaN(val) || val < 0 || val > 1) {
        ElMessage.error(`${item.description} 必须是 0.0 到 1.0 之间的数字`)
        return
      }
      weightSum += val
    }
  }

  // Allow floating point precision issue (e.g., 0.99999)
  if (Math.abs(weightSum - 1.0) > 0.01) {
    ElMessage.warning(`各项成绩权重之和当前为 ${weightSum.toFixed(2)}，建议调整使其总和等于 1.0`)
  }

  try {
    const res = await request.put('/api/config/batch', configList.value)
    if (res.code === 200) {
      ElMessage.success('系统配置已成功保存')
      fetchConfigs()
    } else {
      ElMessage.error(res.message || '保存配置失败')
    }
  } catch (e) {
    ElMessage.error('保存异常')
  }
}

onMounted(() => {
  fetchConfigs()
})
</script>

<style scoped>
.config-list-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.header-content h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1a2b40;
}

.subtitle {
  font-size: 14px;
  color: #667a92;
  margin-top: 6px;
  display: inline-block;
}

.box-card {
  border-radius: 12px;
  border: 1px solid rgba(19, 54, 91, 0.06);
}

.config-item-row {
  margin-bottom: 5px;
}
</style>
