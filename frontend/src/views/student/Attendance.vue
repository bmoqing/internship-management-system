<!--
  Copyright (c) 2026 bmoqing
  All rights reserved.
  本代码仅供学习参考，未经许可不得用于商业用途。
-->

<template>
  <div class="container">
    <el-row :gutter="20">
      <el-col :span="18">
        <el-card>
          <div class="checkin-box">
            <div>
              <div class="title">今日签到</div>
              <div class="sub">{{ checkinTip }}</div>
            </div>
            <el-button type="primary" :disabled="!hasActiveAssignment || checkedToday || locationError" :loading="checkinLoading" @click="handleCheckin">
              {{ !hasActiveAssignment ? '暂无在岗实习' : (checkedToday ? '今日已签到' : '立即签到') }}
            </el-button>
          </div>
          <div class="map-container" v-if="hasActiveAssignment">
            <div class="map-header">
              <span class="map-status" :class="{ 'status-ok': locationReady, 'status-error': locationError }">
                <el-icon><Location /></el-icon>
                {{ locationStatusText }}
              </span>
            </div>
            <div id="student-map" style="height: 300px; width: 100%; border: 1px solid #eee; border-radius: 8px;"></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div class="stats-item" :class="hasActiveAssignment ? 'active-flag' : 'inactive-flag'">
            当前状态：{{ hasActiveAssignment ? '进行中实习' : '无进行中实习' }}
          </div>
          <div class="stats-item" v-if="assignmentStart">实习开始：{{ assignmentStart }}</div>
          <div class="stats-item">签到总次数：{{ stats.total }}</div>
          <div class="stats-item">正常：{{ stats.normal }}</div>
          <div class="stats-item">迟到：{{ stats.late }}</div>
          <div class="stats-item score">考勤得分：{{ stats.attendanceScore }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-table :data="tableData" border stripe style="width: 100%; margin-top: 20px">
      <el-table-column prop="checkinTime" label="签到时间" width="220" />
      <el-table-column label="签到状态" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 'NORMAL'" type="success">正常</el-tag>
          <el-tag v-else type="warning">迟到</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="说明" min-width="220">
        <template #default="scope">
          {{ scope.row.status === 'NORMAL' ? '按时签到' : '签到时间晚于09:00' }}
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
import { computed, onMounted, reactive, ref, nextTick } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Location } from '@element-plus/icons-vue'
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

const tableData = ref([])
const total = ref(0)
const checkedToday = ref(false)
const checkinLoading = ref(false)
const hasActiveAssignment = ref(false)
const assignmentStart = ref('')

const locationReady = ref(false)
const locationError = ref(false)
const locationStatusText = ref('正在获取您的位置...')
const currentLocation = ref({ lat: null, lng: null })
const companyConfig = ref(null)

let map = null
let studentMarker = null
let companyCircle = null

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

const stats = reactive({
  total: 0,
  normal: 0,
  late: 0,
  attendanceScore: 0
})

const checkinTip = computed(() => {
  if (!hasActiveAssignment.value) {
    return '当前无进行中实习分配，暂不能签到'
  }
  if (companyConfig.value) {
    return `允许签到时间: ${companyConfig.value.checkinStartTime} - ${companyConfig.value.checkinEndTime} | ${companyConfig.value.workStartTime} 后签到记为迟到`
  }
  return '工作日 09:00 前签到记为正常，之后记为迟到'
})

const todayStr = () => {
  const d = new Date()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

const initMap = () => {
  if (!map) {
    map = L.map('student-map').setView([22.543099, 114.057868], 14)
    L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
      attribution: '© 高德地图',
      subdomains: ['1', '2', '3', '4']
    }).addTo(map)
  }
}

const drawCompanyArea = () => {
  if (map && companyConfig.value) {
    const lat = companyConfig.value.latitude
    const lng = companyConfig.value.longitude
    const radius = companyConfig.value.radius || 500

    if (companyCircle) {
      companyCircle.setLatLng([lat, lng])
      companyCircle.setRadius(radius)
    } else {
      companyCircle = L.circle([lat, lng], {
        color: '#67c23a',
        fillColor: '#67c23a',
        fillOpacity: 0.2,
        radius: radius
      }).addTo(map)
    }
    
    // Auto fit bounds if both student and company locations are available
    if (studentMarker) {
      const group = new L.featureGroup([studentMarker, companyCircle])
      map.fitBounds(group.getBounds(), { padding: [50, 50] })
    } else {
      map.setView([lat, lng], 15)
    }
  }
}

const getUserLocation = () => {
  if (!navigator.geolocation) {
    locationError.value = true
    locationStatusText.value = '您的浏览器不支持地理位置功能'
    return
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      const lat = position.coords.latitude
      const lng = position.coords.longitude
      currentLocation.value = { lat, lng }
      locationReady.value = true
      locationError.value = false
      locationStatusText.value = '位置获取成功'

      if (map) {
        if (studentMarker) {
          studentMarker.setLatLng([lat, lng])
        } else {
          studentMarker = L.marker([lat, lng], {
            title: '我的位置'
          }).addTo(map)
          studentMarker.bindPopup('您当前所在位置').openPopup()
        }
        
        if (companyConfig.value) {
          const group = new L.featureGroup([studentMarker, companyCircle])
          map.fitBounds(group.getBounds(), { padding: [50, 50] })
        } else {
          map.setView([lat, lng], 16)
        }
      }
    },
    (error) => {
      locationError.value = true
      locationReady.value = false
      if (error.code === error.PERMISSION_DENIED) {
        locationStatusText.value = '请允许浏览器获取您的位置权限以完成签到'
      } else {
        locationStatusText.value = '位置获取失败，请重试'
      }
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
  )
}

const load = async () => {
  const res = await request.get('/api/attendance/my', { params: queryParams })
  if (res.code === 200) {
    tableData.value = res.data.records
    total.value = res.data.total
    const today = todayStr()
    checkedToday.value = tableData.value.some(item => item.checkinTime?.startsWith(today))
  }
}

const loadStats = async () => {
  const res = await request.get('/api/attendance/my/stats')
  if (res.code === 200 && res.data) {
    hasActiveAssignment.value = !!res.data.hasActiveAssignment
    assignmentStart.value = res.data.assignmentStart || ''
    stats.total = res.data.total || 0
    stats.normal = res.data.normal || 0
    stats.late = res.data.late || 0
    stats.attendanceScore = res.data.attendanceScore || 0
    
    if (res.data.companyConfig) {
      companyConfig.value = res.data.companyConfig
      nextTick(() => {
        initMap()
        drawCompanyArea()
        getUserLocation()
      })
    } else if (hasActiveAssignment.value) {
      nextTick(() => {
        initMap()
        getUserLocation()
      })
    }
  }
}

const handleCheckin = async () => {
  if (!hasActiveAssignment.value) {
    ElMessage.warning('当前无进行中实习分配，暂不能签到')
    return
  }
  if (!locationReady.value || !currentLocation.value.lat) {
    ElMessage.warning('请等待位置获取成功或授予位置权限')
    return
  }

  checkinLoading.value = true
  try {
    const res = await request.post('/api/attendance/checkin', {
      latitude: currentLocation.value.lat,
      longitude: currentLocation.value.lng
    })
    if (res.code === 200) {
      ElMessage.success(res.data?.status === 'NORMAL' ? '签到成功（正常）' : '签到成功（迟到）')
      await load()
      await loadStats()
    } else {
      ElMessage.error(res.message || '签到失败')
    }
  } finally {
    checkinLoading.value = false
  }
}

onMounted(async () => {
  await load()
  await loadStats()
})
</script>

<style scoped>
.container {
  padding: 20px;
  background: white;
  border-radius: 8px;
}

.checkin-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.sub {
  margin-top: 6px;
  color: #666;
  font-size: 13px;
}

.stats-item {
  line-height: 28px;
  color: #333;
}

.active-flag {
  color: #15946d;
  font-weight: 600;
}

.inactive-flag {
  color: #c2413f;
  font-weight: 600;
}

.score {
  font-weight: 700;
  color: #409eff;
}

.map-container {
  margin-top: 20px;
}

.map-header {
  margin-bottom: 10px;
}

.map-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 4px;
  background: #f4f4f5;
  color: #909399;
}

.status-ok {
  background: #f0f9eb;
  color: #67c23a;
}

.status-error {
  background: #fef0f0;
  color: #f56c6c;
}
</style>
