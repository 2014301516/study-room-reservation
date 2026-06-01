<template>
  <div class="admin-dashboard">
    <h3>📊 数据看板</h3>

    <!-- 概览卡片 -->
    <el-row :gutter="16" class="dash-cards">
      <el-col :xs="12" :sm="6" v-for="c in cards" :key="c.label">
        <div class="dash-card" :style="{ borderTopColor: c.color }">
          <div class="dc-value">{{ c.value }}</div>
          <div class="dc-label">{{ c.label }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :xs="24" :md="14">
        <div class="chart-card">
          <div class="chart-title">📈 近7天预约趋势</div>
          <div ref="trendChart" style="height:320px"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="10">
        <div class="chart-card">
          <div class="chart-title">🍩 各区域使用情况</div>
          <div ref="areaChart" style="height:320px"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :xs="24" :md="12">
        <div class="chart-card">
          <div class="chart-title">⏰ 今日时段预约分布</div>
          <div ref="hourlyChart" style="height:280px"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="chart-card">
          <div class="chart-title">🪑 座位状态分布</div>
          <div ref="statusChart" style="height:280px"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import api from '@/api'

const cards = ref([
  { label: '今日预约总数', value: '--', color: '#409EFF' },
  { label: '当前使用中', value: '--', color: '#67C23A' },
  { label: '座位利用率', value: '--%', color: '#E6A23C' },
  { label: '活跃违规', value: '--', color: '#F56C6C' },
])

const trendChart = ref(null)
const areaChart = ref(null)
const hourlyChart = ref(null)
const statusChart = ref(null)
let charts = []

function initChart(refEl, option) {
  if (!refEl.value) return
  const instance = echarts.init(refEl.value)
  instance.setOption(option)
  charts.push(instance)
  return instance
}

async function loadData() {
  const res = await api.get('/admin/dashboard')
  if (res.code !== 200) return
  const d = res.data

  cards.value[0].value = d.today.total_reservations || 0
  cards.value[1].value = d.today.active_users || 0
  cards.value[2].value = (d.today.utilization || 0) + '%'
  cards.value[3].value = d.violations?.active || 0

  await nextTick()

  // 趋势图
  initChart(trendChart, {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: d.trend.map(t => t.date.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      data: d.trend.map(t => t.cnt), type: 'line', smooth: true,
      areaStyle: { color: new echarts.graphic.LinearGradient(0,0,0,1, [
        { offset:0, color:'rgba(64,158,255,.3)' }, { offset:1, color:'rgba(64,158,255,.02)' }
      ])},
      lineStyle: { color: '#409EFF', width: 2 },
      itemStyle: { color: '#409EFF' }
    }]
  })

  // 区域使用情况
  initChart(areaChart, {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['50%', '75%'], center: ['50%', '50%'],
      label: { formatter: '{b}\n{d}%' },
      data: d.areaUsage.map(a => ({ name: a.name, value: a.used }))
    }]
  })

  // 时段分布
  initChart(hourlyChart, {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: d.hourly.map(h => h.start_time.slice(0,5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar', data: d.hourly.map(h => h.cnt),
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0,0,0,1, [{ offset:0, color:'#667eea' }, { offset:1, color:'#764ba2' }])
      }
    }]
  })

  // 座位状态分布
  const [stats] = await Promise.all([api.get('/seats/stats/overview')])
  if (stats.code === 200) {
    initChart(statusChart, {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: '70%', center: ['50%', '50%'],
        data: [
          { name: '空闲', value: stats.data.byStatus?.available || 0, itemStyle: { color: '#67C23A' } },
          { name: '已预约', value: stats.data.byStatus?.reserved || 0, itemStyle: { color: '#409EFF' } },
          { name: '使用中', value: stats.data.byStatus?.occupied || 0, itemStyle: { color: '#F56C6C' } },
          { name: '暂离', value: stats.data.byStatus?.temp_leave || 0, itemStyle: { color: '#E6A23C' } },
          { name: '维护', value: stats.data.byStatus?.maintenance || 0, itemStyle: { color: '#909399' } },
        ].filter(d => d.value > 0)
      }]
    })
  }
}

onMounted(() => loadData())

// resize
function onResize() { charts.forEach(c => c?.resize()) }
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => { window.removeEventListener('resize', onResize); charts.forEach(c => c?.dispose()) })
</script>

<style scoped>
.admin-dashboard { max-width: 1200px; margin: 0 auto; padding: 20px; }
.admin-dashboard h3 { margin: 0 0 16px; }
.dash-card { background: #fff; border-radius: 10px; padding: 20px; text-align: center; border-top: 3px solid #409EFF; margin-bottom: 8px; }
.dc-value { font-size: 28px; font-weight: 700; color: #303133; }
.dc-label { font-size: 13px; color: #909399; margin-top: 4px; }
.chart-card { background: #fff; border-radius: 10px; padding: 20px; margin-bottom: 8px; }
.chart-title { font-weight: 500; margin-bottom: 12px; color: #303133; }
</style>
