<template>
  <div class="admin-page">
    <div class="page-header">
      <h3>📋 预约管理</h3>
      <div class="header-filters">
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable @change="loadData" style="width:130px">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-date-picker v-model="filterDate" type="date" placeholder="选择日期" @change="loadData" value-format="YYYY-MM-DD" style="width:160px" />
      </div>
    </div>

    <el-table :data="list" stripe v-loading="loading" border>
      <el-table-column prop="real_name" label="用户" width="90" />
      <el-table-column prop="seat_number" label="座位号" width="90" />
      <el-table-column label="区域" min-width="140">
        <template #default="{ row }">{{ row.building }}{{ row.floor }}F - {{ row.area_name }}</template>
      </el-table-column>
      <el-table-column label="日期" width="110">
        <template #default="{ row }">{{ row.date?.slice(5) }}</template>
      </el-table-column>
      <el-table-column label="时间" width="140">
        <template #default="{ row }">{{ row.start_time?.slice(0,5) }} ~ {{ row.end_time?.slice(0,5) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ row.created_at?.replace('T',' ').slice(0,16) }}</template>
      </el-table-column>
    </el-table>

    <div class="pagination" v-if="total > pageSize">
      <el-pagination background layout="prev,pager,next" :total="total" :page-size="pageSize" v-model:current-page="page" @change="loadData" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from '@/api'

const loading = ref(false)
const list = ref([])
const page = ref(1)
const total = ref(0)
const pageSize = 15
const filterStatus = ref(null)
const filterDate = ref(null)

const statusOptions = [
  { label: '待签到', value: 'reserved' }, { label: '已签到', value: 'checked_in' },
  { label: '使用中', value: 'using' }, { label: '暂离中', value: 'temp_leave' },
  { label: '已完成', value: 'completed' }, { label: '已取消', value: 'cancelled' }, { label: '缺席', value: 'absent' }
]
const statusLabel = (s) => ({ reserved:'待签到', checked_in:'已签到', using:'使用中', temp_leave:'暂离中', completed:'已完成', cancelled:'已取消', absent:'缺席' }[s]||s)
const statusType = (s) => ({ reserved:'info', checked_in:'success', using:'success', temp_leave:'warning', completed:'', cancelled:'danger', absent:'danger' }[s]||'')

async function loadData() {
  loading.value = true
  const params = { page: page.value, pageSize }
  if (filterStatus.value) params.status = filterStatus.value
  if (filterDate.value) params.date = filterDate.value
  const res = await api.get('/admin/reservations', { params })
  if (res.code === 200) { list.value = res.data.list; total.value = res.data.total }
  loading.value = false
}

loadData()
</script>

<style scoped>
.admin-page { max-width: 1200px; margin: 0 auto; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.page-header h3 { margin: 0; }
.header-filters { display: flex; gap: 8px; }
.pagination { margin-top: 16px; display: flex; justify-content: center; }
</style>
