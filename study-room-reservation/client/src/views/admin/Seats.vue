<template>
  <div class="admin-page">
    <div class="page-header">
      <h3>🪑 座位管理</h3>
      <el-select v-model="filterArea" placeholder="筛选区域" clearable @change="loadData" style="width:180px">
        <el-option v-for="a in areas" :key="a.id" :label="a.name" :value="a.id" />
      </el-select>
    </div>

    <el-table :data="list" stripe v-loading="loading" border>
      <el-table-column prop="seat_number" label="座位号" width="100" />
      <el-table-column label="区域" width="150">
        <template #default="{ row }">{{ row.building }}{{ row.floor }}F - {{ row.area_name }}</template>
      </el-table-column>
      <el-table-column label="位置" width="100">
        <template #default="{ row }">{{ row.row_num }}行{{ row.col_num }}列</template>
      </el-table-column>
      <el-table-column label="设施" width="160">
        <template #default="{ row }">
          <el-tag v-if="row.has_outlet" size="small" type="success" style="margin-right:4px">插座</el-tag>
          <el-tag v-if="row.has_lamp" size="small" type="warning" style="margin-right:4px">台灯</el-tag>
          <el-tag v-if="row.is_window" size="small" type="primary">靠窗</el-tag>
          <span v-if="!row.has_outlet && !row.has_lamp && !row.is_window" style="color:#c0c4cc">基础</span>
        </template>
      </el-table-column>
      <el-table-column prop="current_status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.current_status)" size="small">{{ statusLabel(row.current_status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除此座位？" @confirm="doDelete(row.id)">
            <template #reference><el-button size="small" type="danger" plain>删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑/新增弹窗 -->
    <el-dialog v-model="editVisible" :title="editId ? '编辑座位' : '新增座位'" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="区域">
          <el-select v-model="editForm.area_id">
            <el-option v-for="a in areas" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="座位号"><el-input v-model="editForm.seat_number" /></el-form-item>
        <el-row>
          <el-col :span="12"><el-form-item label="行"><el-input-number v-model="editForm.row_num" :min="1" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="列"><el-input-number v-model="editForm.col_num" :min="1" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="状态">
          <el-select v-model="editForm.current_status">
            <el-option label="空闲" value="available" />
            <el-option label="维护中" value="maintenance" />
          </el-select>
        </el-form-item>
        <el-form-item label="设施">
          <el-checkbox v-model="editForm.has_outlet">插座</el-checkbox>
          <el-checkbox v-model="editForm.has_lamp">台灯</el-checkbox>
          <el-checkbox v-model="editForm.is_window">靠窗</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" @click="doSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'

const loading = ref(false)
const list = ref([])
const areas = ref([])
const filterArea = ref(null)
const editVisible = ref(false)
const editId = ref(null)
const editForm = reactive({ area_id: null, seat_number: '', row_num: 1, col_num: 1, current_status: 'available', has_outlet: false, has_lamp: false, is_window: false })

const statusLabel = (s) => ({ available: '空闲', reserved: '已预约', occupied: '使用中', temp_leave: '暂离', maintenance: '维护中' }[s] || s)
const statusType = (s) => ({ available: 'success', reserved: 'info', occupied: 'danger', temp_leave: 'warning', maintenance: 'info' }[s] || '')

async function loadAreas() {
  const res = await api.get('/seats/areas')
  if (res.code === 200) areas.value = res.data
}

async function loadData() {
  loading.value = true
  const params = filterArea.value ? { area_id: filterArea.value } : {}
  const res = await api.get('/admin/seats', { params })
  if (res.code === 200) list.value = res.data
  loading.value = false
}

function openEdit(row) {
  editId.value = row?.id || null
  if (row) {
    Object.assign(editForm, {
      area_id: row.area_id, seat_number: row.seat_number,
      row_num: row.row_num, col_num: row.col_num,
      current_status: row.current_status,
      has_outlet: !!row.has_outlet, has_lamp: !!row.has_lamp, is_window: !!row.is_window
    })
  } else {
    editId.value = null
    Object.assign(editForm, { area_id: areas.value[0]?.id || null, seat_number: '', row_num: 1, col_num: 1, current_status: 'available', has_outlet: false, has_lamp: false, is_window: false })
  }
  editVisible.value = true
}

async function doSave() {
  if (editId.value) {
    await api.put(`/admin/seats/${editId.value}`, { ...editForm })
  } else {
    await api.post('/admin/seats', { ...editForm })
  }
  ElMessage.success('保存成功')
  editVisible.value = false
  loadData()
}

async function doDelete(id) {
  await api.delete(`/admin/seats/${id}`)
  ElMessage.success('已删除')
  loadData()
}

loadAreas()
loadData()
</script>

<style scoped>
.admin-page { max-width: 1200px; margin: 0 auto; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
