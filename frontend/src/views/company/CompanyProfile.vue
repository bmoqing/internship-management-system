<!--
  Copyright (c) 2026 bmoqing
  All rights reserved.
  本代码仅供学习参考，未经许可不得用于商业用途。
-->

<template>
  <div class="company-profile">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>考勤配置</span>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="企业名称">
          <el-input v-model="form.name" disabled />
        </el-form-item>
        
        <el-form-item label="允许签到开始" prop="checkinStartTime">
          <el-time-picker v-model="form.checkinStartTime" format="HH:mm:ss" value-format="HH:mm:ss" placeholder="选择开始时间" />
        </el-form-item>
        
        <el-form-item label="允许签到结束" prop="checkinEndTime">
          <el-time-picker v-model="form.checkinEndTime" format="HH:mm:ss" value-format="HH:mm:ss" placeholder="选择结束时间" />
        </el-form-item>
        
        <el-form-item label="标准上班时间" prop="workStartTime">
          <el-time-picker v-model="form.workStartTime" format="HH:mm:ss" value-format="HH:mm:ss" placeholder="超过此时间记为迟到" />
        </el-form-item>
        
        <el-form-item label="考勤半径 (米)" prop="radius">
          <el-input-number v-model="form.radius" :min="10" :max="5000" />
        </el-form-item>

        <el-form-item label="企业位置" prop="latitude">
          <div style="width: 100%;">
            <div style="display: flex; gap: 10px; margin-bottom: 10px;">
              <el-input v-model="searchQuery" placeholder="输入地名搜索位置，例如: 深圳市南山区腾讯大厦" @keyup.enter="searchLocation" clearable />
              <el-button type="primary" @click="searchLocation" :loading="searchLoading">搜索</el-button>
            </div>
            <div style="display: flex; gap: 10px; margin-bottom: 10px;">
              <el-input v-model="form.latitude" placeholder="纬度 (Latitude)" disabled />
              <el-input v-model="form.longitude" placeholder="经度 (Longitude)" disabled />
              <el-button @click="resetMap">定位到已保存坐标</el-button>
            </div>
            <div class="map-hint">或者直接在下方地图中点击选择您的企业精确位置：</div>
            <div id="company-map" style="height: 400px; width: 100%; border: 1px solid #ccc; border-radius: 4px;"></div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submit" :loading="loading">保存配置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

// Fix leaflet icon issue
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
import iconUrl from 'leaflet/dist/images/marker-icon.png'
import shadowUrl from 'leaflet/dist/images/marker-shadow.png'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl,
  iconUrl,
  shadowUrl
})

const formRef = ref(null)
const form = ref({
  id: null,
  name: '',
  checkinStartTime: '06:00:00',
  checkinEndTime: '23:59:59',
  workStartTime: '09:00:00',
  radius: 500,
  latitude: null,
  longitude: null
})

const rules = {
  checkinStartTime: [{ required: true, message: '请选择签到开始时间' }],
  checkinEndTime: [{ required: true, message: '请选择签到结束时间' }],
  workStartTime: [{ required: true, message: '请选择上班时间' }],
  radius: [{ required: true, message: '请输入考勤半径' }]
}

const loading = ref(false)
const searchLoading = ref(false)
const searchQuery = ref('')
let map = null
let marker = null
let circle = null

watch(() => form.value.radius, (newVal) => {
  if (circle && form.value.latitude) {
    circle.setRadius(newVal || 500)
  }
})

const initMap = () => {
  // Default to Shenzhen if no coordinates
  const lat = form.value.latitude || 22.543099
  const lng = form.value.longitude || 114.057868

  map = L.map('company-map').setView([lat, lng], 14)

  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    attribution: '© 高德地图',
    subdomains: ['1', '2', '3', '4']
  }).addTo(map)

  if (form.value.latitude && form.value.longitude) {
    updateMarkerAndCircle(lat, lng)
  }

  map.on('click', (e) => {
    const { lat, lng } = e.latlng
    form.value.latitude = lat.toFixed(6)
    form.value.longitude = lng.toFixed(6)
    updateMarkerAndCircle(lat, lng)
  })
}

const updateMarkerAndCircle = (lat, lng) => {
  if (marker) {
    marker.setLatLng([lat, lng])
  } else {
    marker = L.marker([lat, lng]).addTo(map)
  }

  if (circle) {
    circle.setLatLng([lat, lng])
    circle.setRadius(form.value.radius || 500)
  } else {
    circle = L.circle([lat, lng], {
      color: '#409eff',
      fillColor: '#409eff',
      fillOpacity: 0.2,
      radius: form.value.radius || 500
    }).addTo(map)
  }
}

const resetMap = () => {
  if (form.value.latitude && form.value.longitude) {
    map.setView([form.value.latitude, form.value.longitude], 15)
  }
}

const searchLocation = async () => {
  if (!searchQuery.value) {
    ElMessage.warning('请输入要搜索的地名')
    return
  }
  searchLoading.value = true
  try {
    const res = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(searchQuery.value)}`)
    const data = await res.json()
    if (data && data.length > 0) {
      const { lat, lon } = data[0]
      form.value.latitude = parseFloat(lat).toFixed(6)
      form.value.longitude = parseFloat(lon).toFixed(6)
      updateMarkerAndCircle(form.value.latitude, form.value.longitude)
      map.setView([form.value.latitude, form.value.longitude], 15)
      ElMessage.success('已找到该地点并更新坐标，您可微调定位')
    } else {
      ElMessage.warning('未找到该地点，请尝试更换更详细的关键词')
    }
  } catch (error) {
    ElMessage.error('位置搜索失败，请检查网络')
  } finally {
    searchLoading.value = false
  }
}

const loadData = async () => {
  try {
    const res = await request.get('/api/company/my')
    if (res.code === 200 && res.data) {
      form.value = {
        ...form.value,
        ...res.data
      }
      if (map && form.value.latitude && form.value.longitude) {
        updateMarkerAndCircle(form.value.latitude, form.value.longitude)
        map.setView([form.value.latitude, form.value.longitude], 15)
      }
    }
  } catch (error) {
    ElMessage.error('加载配置失败')
  }
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (!form.value.latitude || !form.value.longitude) {
        return ElMessage.warning('请在地图上选择企业位置')
      }
      loading.value = true
      try {
        const res = await request.put('/api/company/my', form.value)
        if (res.code === 200) {
          ElMessage.success('配置保存成功')
          loadData()
        } else {
          ElMessage.error(res.message || '保存失败')
        }
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(async () => {
  await loadData()
  nextTick(() => {
    initMap()
  })
})
</script>

<style scoped>
.company-profile {
  padding: 20px;
}
.card-header {
  font-weight: bold;
}
.map-hint {
  color: #666;
  font-size: 13px;
  margin-bottom: 8px;
}
</style>
