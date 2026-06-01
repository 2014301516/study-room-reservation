<template>
  <div class="seatmap-page">
    <!-- 顶部信息 -->
    <div class="map-header">
      <div class="header-left">
        <h3>🪑 座位预约</h3>
        <el-breadcrumb separator="/">
          <el-breadcrumb-item @click="areaTab='all'">全部区域</el-breadcrumb-item>
          <el-breadcrumb-item v-if="currentArea">{{ currentArea.building }}{{ currentArea.floor }}F</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="header-right">
        <span class="live-badge">
          <span class="live-dot"></span> 实时更新
        </span>
      </div>
    </div>

    <!-- 区域标签 -->
    <div class="area-tabs">
      <div class="area-tab" :class="{ active: areaTab === 'all' }" @click="switchArea('all')">
        <span class="tab-icon">📊</span>
        <span>全馆概览</span>
      </div>
      <div class="area-tab" v-for="area in areas" :key="area.id"
        :class="{ active: areaTab === area.id }" @click="switchArea(area.id)">
        <span class="tab-icon">{{ area.floor === 3 ? '🏠' : '📚' }}</span>
        <span>{{ area.name }}</span>
        <el-badge :value="area.available || 0" :type="area.available > 5 ? 'success' : 'warning'" class="area-badge" />
      </div>
    </div>

    <!-- 统计数据条 -->
    <div class="stats-bar">
      <div class="stat-dot" v-for="s in statusLegend" :key="s.status">
        <span class="dot" :style="{ background: s.color }"></span>
        <span class="dot-label">{{ s.label }}</span>
        <span class="dot-count">{{ seatStats[s.status] || 0 }}</span>
      </div>
    </div>

    <!-- 座位图主体 -->
    <div class="map-body" v-loading="mapLoading">
      <!-- 全馆概览 -->
      <div v-if="areaTab === 'all'" class="overview-grid">
        <div class="overview-card" v-for="area in areas" :key="area.id" @click="switchArea(area.id)">
          <div class="overview-card-header">
            <span class="oc-title">{{ area.name }}</span>
            <el-tag size="small" type="success">{{ area.available }} 可用</el-tag>
          </div>
          <div class="mini-grid">
            <div v-for="seat in area.seats?.slice(0, 20)" :key="seat.id" class="mini-seat"
              :style="{ background: statusColor(seat.current_status) }"
              :title="seat.seat_number + ' - ' + seat.current_status"></div>
          </div>
          <div class="oc-footer">{{ area.building }} {{ area.floor }}F · {{ area.seats?.length || 0 }} 座</div>
        </div>
      </div>

      <!-- 详细座位图 -->
      <div v-else class="seat-grid-container">
        <!-- 区域信息 -->
        <div class="area-info-bar">
          <span class="ai-name">{{ currentArea?.name }}</span>
          <span class="ai-desc">{{ currentArea?.building }} {{ currentArea?.floor }}F · {{ currentArea?.description }}</span>
        </div>

        <!-- SVG 座位图 -->
        <div class="svg-wrapper" ref="svgWrapper">
          <svg :viewBox="`0 0 ${gridWidth} ${gridHeight}`" class="seat-svg">
            <!-- 网格线 -->
            <defs>
              <filter id="shadow">
                <feDropShadow dx="1" dy="2" stdDeviation="2" flood-opacity="0.15" />
              </filter>
            </defs>

            <!-- 座位 -->
            <g v-for="seat in currentSeats" :key="seat.id"
              class="seat-group"
              :class="{ clickable: seat.current_status === 'available' }"
              @click="seat.current_status === 'available' && openReserve(seat)">
              <rect :x="seatX(seat)" :y="seatY(seat)"
                :width="seatW - 4" :height="seatH - 4" rx="5"
                :fill="statusColor(seat.current_status)"
                :stroke="statusStroke(seat.current_status)"
                stroke-width="1.5"
                filter="url(#shadow)"
                class="seat-rect" />
              <text :x="seatX(seat) + (seatW - 4) / 2" :y="seatY(seat) + (seatH - 4) / 2 + 1"
                text-anchor="middle" dominant-baseline="middle"
                :fill="seat.current_status === 'available' ? '#fff' : '#fff'"
                font-size="10" font-weight="600" class="seat-text">
                {{ seat.seat_number }}
              </text>
            </g>

            <!-- 区域标签 -->
            <text :x="gridWidth / 2" :y="gridHeight - 6" text-anchor="middle" fill="#909399" font-size="11">
              {{ currentArea?.building }} {{ currentArea?.floor }}F · {{ currentArea?.name }}
            </text>
          </svg>
        </div>
      </div>
    </div>

    <!-- 预约弹窗 -->
    <el-dialog v-model="reserveVisible" :title="'预约 ' + selectedSeat?.seat_number" width="500px" destroy-on-close>
      <div class="reserve-info" v-if="selectedSeat">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="座位号">{{ selectedSeat.seat_number }}</el-descriptions-item>
          <el-descriptions-item label="区域">{{ currentArea?.name }}</el-descriptions-item>
          <el-descriptions-item label="设施">
            <el-tag v-if="selectedSeat.has_outlet" size="small" type="success" style="margin-right:4px">🔌 插座</el-tag>
            <el-tag v-if="selectedSeat.has_lamp" size="small" type="warning" style="margin-right:4px">💡 台灯</el-tag>
            <el-tag v-if="selectedSeat.is_window" size="small" type="primary" style="margin-right:4px">🪟 靠窗</el-tag>
            <span v-if="!selectedSeat.has_outlet && !selectedSeat.has_lamp && !selectedSeat.is_window">基础座位</span>
          </el-descriptions-item>
          <el-descriptions-item label="日期">{{ reserveDate }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="time-slots">
        <div class="slot-title">选择时间段（可多选连续时段）</div>
        <div class="slot-grid">
          <div v-for="slot in timeSlots" :key="slot.start"
            class="slot-item"
            :class="{
              available: slot.available && !slot.selected,
              selected: slot.selected,
              unavailable: !slot.available
            }"
            @click="toggleSlot(slot)">
            {{ slot.start }}<br/>~{{ slot.end }}
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="reserveVisible = false">取消</el-button>
        <el-button type="primary" :loading="reserving" :disabled="!canReserve" @click="doReserve">
          {{ canReserve ? `预约 ${selectedSlots[0]?.start} ~ ${selectedSlots[selectedSlots.length-1]?.end}` : '请选择时段' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'

// ====== 数据 ======
const areas = ref([])
const currentSeats = ref([])
const currentArea = ref(null)
const areaTab = ref('all')
const mapLoading = ref(false)
const seatStats = reactive({})

// 预约相关
const reserveVisible = ref(false)
const selectedSeat = ref(null)
const timeSlots = ref([])
const reserving = ref(false)
const reserveDate = new Date().toISOString().split('T')[0]

// SVG布局
const seatW = 56, seatH = 44, gapX = 8, gapY = 8, margin = 20
const gridWidth = computed(() => {
  if (!currentSeats.value.length) return 600
  const maxCol = Math.max(...currentSeats.value.map(s => s.col_num), 1)
  return margin * 2 + maxCol * (seatW + gapX) - gapX
})
const gridHeight = computed(() => {
  if (!currentSeats.value.length) return 400
  const maxRow = Math.max(...currentSeats.value.map(s => s.row_num), 1)
  return margin * 2 + maxRow * (seatH + gapY) - gapY + 20
})

const statusLegend = [
  { status: 'available', label: '空闲', color: '#67C23A' },
  { status: 'reserved', label: '已预约', color: '#409EFF' },
  { status: 'occupied', label: '使用中', color: '#F56C6C' },
  { status: 'temp_leave', label: '暂离', color: '#E6A23C' },
  { status: 'maintenance', label: '维护', color: '#909399' },
]

const statusColor = (s) => ({ available: '#67C23A', reserved: '#409EFF', occupied: '#F56C6C', temp_leave: '#E6A23C', maintenance: '#909399' }[s] || '#DCDFE6')
const statusStroke = (s) => ({ available: '#5daf34', reserved: '#337ECC', occupied: '#e04545', temp_leave: '#cc8c30', maintenance: '#7a7d82' }[s] || '#C0C4CC')

const canReserve = computed(() => selectedSlots.value.length > 0)
const selectedSlots = computed(() => timeSlots.value.filter(s => s.selected && s.available))

// ====== 方法 ======
function seatX(seat) { return margin + (seat.col_num - 1) * (seatW + gapX) }
function seatY(seat) { return margin + (seat.row_num - 1) * (seatH + gapY) }

async function loadAreas() {
  const res = await api.get('/seats/areas')
  if (res.code === 200) areas.value = res.data
}

async function switchArea(id) {
  areaTab.value = id
  if (id === 'all') {
    mapLoading.value = true
    for (const area of areas.value) {
      const res = await api.get(`/seats/area/${area.id}`)
      if (res.code === 200) {
        area.seats = res.data.seats
        area.available = res.data.stats.available
      }
    }
    mapLoading.value = false
    return
  }
  mapLoading.value = true
  const res = await api.get(`/seats/area/${id}`)
  if (res.code === 200) {
    currentArea.value = res.data.area
    currentSeats.value = res.data.seats
    Object.assign(seatStats, res.data.stats)
  }
  mapLoading.value = false
}

async function openReserve(seat) {
  selectedSeat.value = seat
  reserveVisible.value = true
  const res = await api.get(`/reservations/check-available/${seat.id}`, { params: { date: reserveDate } })
  if (res.code === 200) {
    timeSlots.value = res.data.map(s => ({ ...s, selected: false }))
  }
}

function toggleSlot(slot) {
  if (!slot.available) return
  // 只允许选择连续时间段
  const idx = timeSlots.value.indexOf(slot)
  const selected = timeSlots.value.filter(s => s.selected)
  if (selected.length === 0) {
    slot.selected = true
    return
  }
  const selIndices = selected.map(s => timeSlots.value.indexOf(s)).sort((a, b) => a - b)
  const minIdx = selIndices[0], maxIdx = selIndices[selIndices.length - 1]

  if (idx === minIdx - 1) { slot.selected = true; return } // 向前扩展
  if (idx === maxIdx + 1) { slot.selected = true; return }  // 向后扩展
  if (idx >= minIdx && idx <= maxIdx) { slot.selected = false; return } // 取消选中

  // 不连续，清除之前的选择
  timeSlots.value.forEach(s => s.selected = false)
  slot.selected = true
}

async function doReserve() {
  if (!canReserve.value) return
  reserving.value = true
  const slots = selectedSlots.value.sort((a, b) => a.start.localeCompare(b.start))
  const res = await api.post('/reservations', {
    seat_id: selectedSeat.value.id,
    date: reserveDate,
    start_time: slots[0].start,
    end_time: slots[slots.length - 1].end
  })
  reserving.value = false
  if (res.code === 200) {
    ElMessage.success('预约成功！请在预约时间开始后30分钟内签到')
    reserveVisible.value = false
    // 显示二维码弹窗
    ElMessageBox.alert(
      `<div style="text-align:center">
        <img src="${res.data.qrcode_data_url}" style="width:200px;height:200px"/>
        <p style="margin-top:12px">座位：${res.data.seat_number}</p>
        <p>时间：${res.data.date} ${res.data.start_time}-${res.data.end_time}</p>
        <p style="color:#E6A23C">请截图保存，到馆后出示二维码签到</p>
      </div>`,
      '预约成功！',
      { dangerouslyUseHTMLString: true, confirmButtonText: '我知道了' }
    )
    if (areaTab.value !== 'all') switchArea(areaTab.value)
  }
}

onMounted(async () => {
  await loadAreas()
  await switchArea('all')
})

// 每30秒自动刷新
let refreshTimer
onMounted(() => { refreshTimer = setInterval(() => { if (areaTab.value !== 'all') switchArea(areaTab.value) }, 30000) })
onUnmounted(() => { clearInterval(refreshTimer) })
</script>

<style scoped>
.seatmap-page { max-width: 1200px; margin: 0 auto; padding: 20px; }
.map-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.map-header h3 { margin: 0; font-size: 20px; }
.live-badge { font-size: 13px; color: #67C23A; display: flex; align-items: center; gap: 6px; }
.live-dot { width: 8px; height: 8px; border-radius: 50%; background: #67C23A; animation: pulse 1.5s infinite; }
@keyframes pulse { 0%,100% { opacity:1 } 50% { opacity:.3 } }

.area-tabs { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
.area-tab { display: flex; align-items: center; gap: 8px; padding: 10px 18px; background: #fff; border-radius: 10px; cursor: pointer; border: 2px solid transparent; transition: all .2s; }
.area-tab:hover { border-color: #409EFF; }
.area-tab.active { border-color: #409EFF; background: #ecf5ff; }
.tab-icon { font-size: 18px; }

.stats-bar { display: flex; gap: 20px; margin-bottom: 16px; padding: 12px 20px; background: #fff; border-radius: 10px; flex-wrap: wrap; }
.stat-dot { display: flex; align-items: center; gap: 6px; }
.dot { width: 14px; height: 14px; border-radius: 4px; }
.dot-label { font-size: 13px; color: #606266; }
.dot-count { font-weight: 600; color: #303133; }

.map-body { background: #fff; border-radius: 12px; padding: 24px; min-height: 400px; }

.overview-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; }
.overview-card { border: 1px solid #e8e8e8; border-radius: 10px; padding: 16px; cursor: pointer; transition: all .2s; }
.overview-card:hover { border-color: #409EFF; box-shadow: 0 4px 12px rgba(64,158,255,.1); }
.overview-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.oc-title { font-weight: 600; }
.mini-grid { display: grid; grid-template-columns: repeat(10, 1fr); gap: 3px; margin-bottom: 10px; }
.mini-seat { aspect-ratio: 1; border-radius: 3px; min-width: 16px; }
.oc-footer { font-size: 12px; color: #909399; }

.area-info-bar { margin-bottom: 16px; display: flex; gap: 12px; align-items: baseline; }
.ai-name { font-size: 18px; font-weight: 600; }
.ai-desc { color: #909399; font-size: 13px; }

.svg-wrapper { overflow-x: auto; border: 1px solid #f0f0f0; border-radius: 8px; background: #fafbfc; padding: 8px; }
.seat-svg { min-width: 500px; height: auto; }
.seat-group { cursor: default; }
.seat-group.clickable { cursor: pointer; }
.seat-rect { transition: fill .3s, transform .15s; }
.seat-group.clickable .seat-rect:hover { transform: scale(1.08); filter: brightness(1.1); }
.seat-text { pointer-events: none; user-select: none; }

.time-slots { margin-top: 16px; }
.slot-title { font-weight: 500; margin-bottom: 12px; color: #303133; }
.slot-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; }
.slot-item { padding: 10px 4px; text-align: center; border-radius: 8px; font-size: 12px; line-height: 1.6; border: 2px solid #e8e8e8; cursor: default; }
.slot-item.available { border-color: #C0E0C0; background: #f0f9f0; color: #67C23A; cursor: pointer; }
.slot-item.available:hover { border-color: #67C23A; background: #e1f3e1; }
.slot-item.selected { border-color: #409EFF; background: #ecf5ff; color: #409EFF; }
.slot-item.unavailable { background: #f5f5f5; color: #ccc; }
</style>
