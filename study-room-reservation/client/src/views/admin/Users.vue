<template>
  <div class="admin-page">
    <div class="page-header">
      <h3>👥 用户管理</h3>
    </div>

    <el-table :data="list" stripe v-loading="loading" border>
      <el-table-column prop="real_name" label="姓名" width="100" />
      <el-table-column prop="student_id" label="学号" width="120" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="角色" width="90">
        <template #default="{ row }">
          <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">{{ row.role === 'admin' ? '管理员' : '学生' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">{{ row.status === 'active' ? '正常' : '封禁' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" min-width="160">
        <template #default="{ row }">{{ row.created_at?.replace('T',' ').slice(0,16) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="row.role !== 'admin'">
            <el-button v-if="row.status === 'active'" type="danger" size="small" plain @click="toggleStatus(row)">封禁</el-button>
            <el-button v-else type="success" size="small" @click="toggleStatus(row)">解封</el-button>
          </template>
          <span v-else style="color:#c0c4cc">--</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination" v-if="total > pageSize">
      <el-pagination background layout="prev,pager,next" :total="total" :page-size="pageSize" v-model:current-page="page" @change="loadData" />
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
const pageSize = 15

async function loadData() {
  loading.value = true
  const res = await api.get('/admin/users', { params: { page: page.value, pageSize } })
  if (res.code === 200) { list.value = res.data.list; total.value = res.data.total }
  loading.value = false
}

async function toggleStatus(row) {
  const newStatus = row.status === 'active' ? 'banned' : 'active'
  const action = newStatus === 'banned' ? '封禁' : '解封'
  await ElMessageBox.confirm(`确定${action}用户「${row.real_name}」吗？`, '确认', { type: 'warning' })
  await api.put(`/admin/users/${row.id}`, { status: newStatus })
  ElMessage.success(`${action}成功`)
  loadData()
}

loadData()
</script>

<style scoped>
.admin-page { max-width: 1200px; margin: 0 auto; padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.pagination { margin-top: 16px; display: flex; justify-content: center; }
</style>
