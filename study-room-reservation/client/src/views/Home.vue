<template>
  <div class="home-page">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-text">
        <h2>👋 欢迎回来，{{ userStore.user?.real_name }}</h2>
        <p>{{ greeting }}</p>
      </div>
      <div class="welcome-date">{{ todayStr }}</div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6" v-for="card in statCards" :key="card.label">
        <div class="stat-card" :style="{ borderLeftColor: card.color }" @click="card.link && router.push(card.link)">
          <div class="stat-icon" :style="{ background: card.color }">{{ card.icon }}</div>
          <div class="stat-info">
            <div class="stat-num">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷操作 + 公告 -->
    <el-row :gutter="20">
      <el-col :xs="24" :md="16">
        <div class="section-card">
          <div class="section-title">⚡ 快捷操作</div>
          <div class="quick-actions">
            <div class="action-item" v-for="act in quickActions" :key="act.text" @click="router.push(act.link)">
              <div class="action-icon" :style="{ background: act.bg }">{{ act.icon }}</div>
              <span>{{ act.text }}</span>
            </div>
          </div>
        </div>

        <!-- 我的预约 -->
        <div class="section-card" style="margin-top:16px">
          <div class="section-title">📋 今日我的预约</div>
          <el-table :data="todayReservations" stripe style="width:100%" v-loading="loading">
            <el-table-column prop="seat_number" label="座位号" width="100" />
            <el-table-column label="区域">
              <template #default="{ row }">{{ row.building }}{{ row.floor }}F - {{ row.area_name }}</template>
            </el-table-column>
            <el-table-column label="时间">
              <template #default="{ row }">{{ row.start_time }} ~ {{ row.end_time }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button v-if="row.status==='reserved'" type="success" size="small" @click="doCheckin(row)">签到</el-button>
                <el-button v-if="row.status==='checked_in'" type="warning" size="small" @click="doLeave(row)">暂离</el-button>
                <el-button v-if="row.status==='temp_leave'" type="primary" size="small" @click="doCheckin(row)">回来</el-button>
                <el-button v-if="['checked_in','temp_leave'].includes(row.status)" type="danger" size="small" @click="doCheckout(row)">签退</el-button>
                <el-button v-if="row.status==='reserved'" type="danger" size="small" plain @click="doCancel(row)">取消</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && todayReservations.length===0" description="今日暂无预约" :image-size="80" />
        </div>
      </el-col>

      <el-col :xs="24" :md="8">
        <div class="section-card">
          <div class="section-title">📢 公告</div>
          <div class="notice-list">
            <div v-for="n in notices" :key="n.id" class="notice-item">
              <el-tag v-if="n.is_top" size="small" type="danger" style="margin-right:6px">置顶</el-tag>
              <span class="notice-title">{{ n.title }}</span>
              <p class="notice-content">{{ n.content }}</p>
            </div>
          </div>
        </div>

        <!-- 使用规则 -->
        <div class="section-card" style="margin-top:16px">
          <div class="section-title">📖 使用规则</div>
          <el-timeline>
            <el-timeline-item content="预约开放时间：每日 06:00 - 22:00" />
            <el-timeline-item content="预约开始后30分钟内需签到" type="warning" />
            <el-timeline-item content="暂离保留座位15分钟" type="warning" />
            <el-timeline-item content="累计违规3次，封禁预约权限7天" type="danger" />
          </el-timeline>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import api from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const notices = ref([])
const todayReservations = ref([])
const overview = reactive({ total: 0, available: 0 })

const todayStr = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
const hour = new Date().getHours()
const greeting = hour < 12 ? '上午好！今天也要加油学习哦~' : hour < 18 ? '下午好！别忘了预约座位~' : '晚上好！学习的同时注意休息~'

const statCards = ref([
  { icon: '🪑', label: '可用座位', value: '--', color: '#67C23A', link: '/seats' },
  { icon: '📋', label: '今日预约', value: '--', color: '#409EFF', link: '/my-reservations' },
  { icon: '✅', label: '已签到', value: '--', color: '#E6A23C' },
  { icon: '⏰', label: '使用率', value: '--%', color: '#F56C6C' },
])

const quickActions = [
  { icon: '🪑', text: '预约座位', link: '/seats', bg: 'linear-gradient(135deg,#43e97b,#38f9d7)' },
  { icon: '📋', text: '我的预约', link: '/my-reservations', bg: 'linear-gradient(135deg,#667eea,#764ba2)' },
  { icon: '📊', text: '数据看板', link: '/admin/dashboard', bg: 'linear-gradient(135deg,#f093fb,#f5576c)' },
  { icon: '👤', text: '个人中心', link: '/home', bg: 'linear-gradient(135deg,#4facfe,#00f2fe)' },
]

const statusLabel = (s) => ({ reserved: '待签到', checked_in: '已签到', using: '使用中', temp_leave: '暂离中', completed: '已完成', cancelled: '已取消', absent: '缺席' }[s] || s)
const statusType = (s) => ({ reserved: 'info', checked_in: 'success', using: 'success', temp_leave: 'warning', completed: '', cancelled: 'danger', absent: 'danger' }[s] || '')

async function loadData() {
  loading.value = true
  try {
    const [statsRes, myRes, noticesRes] = await Promise.all([
      api.get('/seats/stats/overview'),
      api.get('/reservations/my', { params: { status: 'reserved,checked_in,using,temp_leave' } }),
      api.get('/admin/notices').catch(() => ({ data: [] }))
    ])
    if (statsRes.code === 200) {
      overview.total = statsRes.data.total
      overview.available = statsRes.data.byStatus?.available || 0
      statCards.value[0].value = overview.available + ' / ' + overview.total
      statCards.value[3].value = Math.round((overview.total - overview.available) / overview.total * 100) + '%'
    }
    if (myRes.code === 200) {
      todayReservations.value = myRes.data.list.filter(r => r.date === new Date().toISOString().split('T')[0])
      const active = myRes.data.list.filter(r => ['checked_in', 'using', 'temp_leave'].includes(r.status))
      statCards.value[1].value = myRes.data.list.length
      statCards.value[2].value = active.length
    }
    if (noticesRes.code === 200) {
      notices.value = noticesRes.data
    }
  } catch { }
  loading.value = false
}

async function doCheckin(row) {
  await api.post(`/reservations/${row.id}/checkin`)
  ElMessage.success('签到成功！请对号入座')
  loadData()
}
async function doLeave(row) {
  await api.put(`/reservations/${row.id}/leave`)
  ElMessage.success('已暂离，请于15分钟内返回')
  loadData()
}
async function doCheckout(row) {
  await ElMessageBox.confirm('确定要签退释放座位吗？', '确认签退', { type: 'warning' })
  await api.post(`/reservations/${row.id}/checkout`)
  ElMessage.success('签退成功！')
  loadData()
}
async function doCancel(row) {
  await ElMessageBox.confirm('确定要取消此预约吗？', '确认取消', { type: 'warning' })
  await api.put(`/reservations/${row.id}/cancel`)
  ElMessage.success('预约已取消')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.home-page { max-width: 1200px; margin: 0 auto; padding: 20px; }
.welcome-banner { background: linear-gradient(135deg, #667eea, #764ba2); border-radius: 12px; padding: 28px 32px; color: #fff; display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.welcome-text h2 { margin: 0 0 6px; font-size: 22px; }
.welcome-text p { margin: 0; opacity: .85; }
.welcome-date { font-size: 14px; opacity: .8; }
.stats-row { margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px; cursor: pointer; border-left: 4px solid #409EFF; transition: transform .2s, box-shadow .2s; margin-bottom: 16px; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,.08); }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; color: #fff; }
.stat-num { font-size: 24px; font-weight: 700; color: #303133; }
.stat-label { font-size: 13px; color: #909399; }
.section-card { background: #fff; border-radius: 12px; padding: 20px; }
.section-title { font-size: 16px; font-weight: 600; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #f0f0f0; }
.quick-actions { display: flex; gap: 16px; flex-wrap: wrap; }
.action-item { display: flex; flex-direction: column; align-items: center; gap: 8px; cursor: pointer; padding: 16px; border-radius: 10px; transition: transform .2s; min-width: 90px; }
.action-item:hover { transform: scale(1.05); }
.action-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; color: #fff; }
.action-item span { font-size: 13px; color: #606266; }
.notice-item { padding: 10px 0; border-bottom: 1px solid #f5f5f5; }
.notice-item:last-child { border-bottom: none; }
.notice-title { font-weight: 500; color: #303133; }
.notice-content { margin: 6px 0 0; font-size: 13px; color: #909399; }
</style>
