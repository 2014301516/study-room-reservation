<template>
  <div class="my-res-page">
    <div class="page-header">
      <h3>📋 我的预约</h3>
      <el-radio-group v-model="filterStatus" @change="loadData">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="reserved">待签到</el-radio-button>
        <el-radio-button label="checked_in,using,temp_leave">进行中</el-radio-button>
        <el-radio-button label="completed">已完成</el-radio-button>
        <el-radio-button label="cancelled">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <div class="res-list" v-loading="loading">
      <div class="res-card" v-for="r in list" :key="r.id">
        <div class="res-card-left">
          <div class="res-seat-number">{{ r.seat_number }}</div>
          <div class="res-area">{{ r.building }}{{ r.floor }}F - {{ r.area_name }}</div>
        </div>
        <div class="res-card-center">
          <div class="res-date">{{ r.date }}</div>
          <div class="res-time">{{ r.start_time }} ~ {{ r.end_time }}</div>
          <div class="res-duration">共 {{ duration(r) }} 小时</div>
        </div>
        <div class="res-card-right">
          <el-tag :type="statusType(r.status)" size="default">{{ statusLabel(r.status) }}</el-tag>
        </div>
        <div class="res-card-actions">
          <template v-if="r.status === 'reserved'">
            <el-button type="success" size="small" @click="showQR(r)">查看二维码</el-button>
            <el-button type="primary" size="small" @click="doCheckin(r)">签到</el-button>
            <el-button type="danger" size="small" plain @click="doCancel(r)">取消</el-button>
          </template>
          <template v-else-if="r.status === 'checked_in'">
            <el-button type="warning" size="small" @click="doLeave(r)">暂离</el-button>
            <el-button type="danger" size="small" @click="doCheckout(r)">签退</el-button>
          </template>
          <template v-else-if="r.status === 'temp_leave'">
            <el-button type="primary" size="small" @click="doCheckin(r)">返回</el-button>
            <el-button type="danger" size="small" @click="doCheckout(r)">签退</el-button>
          </template>
          <template v-else>
            <span class="no-action">--</span>
          </template>
        </div>
      </div>
      <el-empty v-if="!loading && list.length === 0" description="暂无预约记录" :image-size="100" />
      <div class="pagination" v-if="total > pageSize">
        <el-pagination background layout="prev, pager, next" :total="total" :page-size="pageSize" v-model:current-page="page" @change="loadData" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const list = ref([])
const page = ref(1)
const total = ref(0)
const pageSize = 10
const filterStatus = ref('')

const statusLabel = (s) => ({ reserved: '待签到', checked_in: '已签到', using: '使用中', temp_leave: '暂离中', completed: '已完成', cancelled: '已取消', absent: '缺席' }[s] || s)
const statusType = (s) => ({ reserved: 'info', checked_in: 'success', using: 'success', temp_leave: 'warning', completed: '', cancelled: 'danger', absent: 'danger' }[s] || '')

function duration(r) {
  const [sh, sm] = r.start_time.split(':').map(Number)
  const [eh, em] = r.end_time.split(':').map(Number)
  return eh - sh + (em - sm) / 60
}

async function loadData() {
  loading.value = true
  const res = await api.get('/reservations/my', { params: { status: filterStatus.value, page: page.value, pageSize } })
  if (res.code === 200) {
    list.value = res.data.list
    total.value = res.data.total
  }
  loading.value = false
}

async function showQR(row) {
  const res = await api.get(`/reservations/${row.id}/qrcode`)
  if (res.code === 200) {
    ElMessageBox.alert(
      `<div style="text-align:center">
        <img src="${res.data.qrcode_data_url}" style="width:200px;height:200px"/>
        <p style="margin-top:12px">出示此二维码进行签到</p>
      </div>`,
      `${row.seat_number} - ${row.date} ${row.start_time}~${row.end_time}`,
      { dangerouslyUseHTMLString: true, confirmButtonText: '关闭' }
    )
  }
}

async function doCheckin(row) {
  await api.post(`/reservations/${row.id}/checkin`)
  ElMessage.success('签到成功')
  loadData()
}
async function doLeave(row) {
  await api.put(`/reservations/${row.id}/leave`)
  ElMessage.success('已暂离')
  loadData()
}
async function doCheckout(row) {
  await ElMessageBox.confirm('确定签退释放座位吗？', '确认', { type: 'warning' })
  await api.post(`/reservations/${row.id}/checkout`)
  ElMessage.success('签退成功')
  loadData()
}
async function doCancel(row) {
  await ElMessageBox.confirm('确定取消此预约吗？', '确认', { type: 'warning' })
  await api.put(`/reservations/${row.id}/cancel`)
  ElMessage.success('已取消')
  loadData()
}

loadData()
</script>

<style scoped>
.my-res-page { max-width: 1200px; margin: 0 auto; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-header h3 { margin: 0; }

.res-card { background: #fff; border-radius: 10px; padding: 20px; margin-bottom: 12px; display: flex; align-items: center; gap: 24px; transition: box-shadow .2s; }
.res-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,.06); }
.res-card-left { min-width: 80px; text-align: center; }
.res-seat-number { font-size: 28px; font-weight: 700; color: #409EFF; }
.res-area { font-size: 12px; color: #909399; margin-top: 4px; }
.res-card-center { flex: 1; }
.res-date { font-size: 15px; font-weight: 500; color: #303133; }
.res-time { font-size: 14px; color: #606266; margin-top: 4px; }
.res-duration { font-size: 12px; color: #909399; margin-top: 2px; }
.res-card-right { min-width: 80px; text-align: center; }
.res-card-actions { display: flex; gap: 6px; flex-wrap: wrap; }
.no-action { color: #c0c4cc; }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
</style>
