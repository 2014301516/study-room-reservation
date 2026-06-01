<template>
  <div class="layout">
    <!-- 侧边栏 -->
    <div class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-logo" @click="router.push('/home')">
        <span class="logo-icon">🏫</span>
        <span class="logo-text" v-show="!sidebarCollapsed">自习室预约</span>
      </div>

      <el-menu :default-active="activeMenu" :collapse="sidebarCollapsed" router
        background-color="#304156" text-color="#bfcbd9" active-text-color="#409EFF">
        <el-menu-item index="/home">
          <el-icon><HomeFilled /></el-icon><span>首页</span>
        </el-menu-item>
        <el-menu-item index="/seats">
          <el-icon><Grid /></el-icon><span>座位预约</span>
        </el-menu-item>
        <el-menu-item index="/my-reservations">
          <el-icon><List /></el-icon><span>我的预约</span>
        </el-menu-item>

        <template v-if="userStore.isAdmin">
          <el-divider style="margin:8px 0;border-color:rgba(255,255,255,.1)" />
          <div class="menu-group-title" v-show="!sidebarCollapsed">管理后台</div>
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon><span>数据看板</span>
          </el-menu-item>
          <el-menu-item index="/admin/seats">
            <el-icon><OfficeBuilding /></el-icon><span>座位管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/reservations">
            <el-icon><Tickets /></el-icon><span>预约管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon><span>用户管理</span>
          </el-menu-item>
        </template>
      </el-menu>

      <div class="sidebar-footer" v-show="!sidebarCollapsed">
        <span class="version">v1.0.0</span>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-area">
      <div class="topbar">
        <div class="topbar-left">
          <el-icon class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed" :size="20">
            <Fold v-if="!sidebarCollapsed" /><Expand v-else />
          </el-icon>
        </div>
        <div class="topbar-right">
          <el-dropdown trigger="click">
            <div class="user-avatar">
              <el-avatar :size="32" style="background:#409EFF">{{ userStore.user?.real_name?.[0] }}</el-avatar>
              <span class="user-name">{{ userStore.user?.real_name }}</span>
              <el-tag v-if="userStore.isAdmin" size="small" type="danger">管理员</el-tag>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  {{ userStore.user?.student_id }} · {{ userStore.user?.role === 'admin' ? '管理员' : '学生' }}
                </el-dropdown-item>
                <el-dropdown-item divided @click="doLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const sidebarCollapsed = ref(false)

const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/admin/')) return p
  return p
})

async function doLogout() {
  await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'info' })
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; }
.sidebar { width: 220px; background: #304156; display: flex; flex-direction: column; transition: width .3s; flex-shrink: 0; }
.sidebar.collapsed { width: 64px; }
.sidebar-logo { display: flex; align-items: center; gap: 10px; padding: 18px 20px; cursor: pointer; }
.sidebar.collapsed .sidebar-logo { justify-content: center; padding: 18px 0; }
.logo-icon { font-size: 24px; }
.logo-text { font-size: 16px; font-weight: 600; color: #fff; white-space: nowrap; }
.menu-group-title { color: rgba(255,255,255,.35); font-size: 12px; padding: 8px 20px; }
.sidebar :deep(.el-menu) { border-right: none; flex: 1; }
.sidebar-footer { padding: 12px 20px; text-align: center; }
.version { color: rgba(255,255,255,.25); font-size: 12px; }

.main-area { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.topbar { height: 56px; background: #fff; border-bottom: 1px solid #e8e8e8; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; }
.topbar-left { display: flex; align-items: center; }
.collapse-btn { cursor: pointer; color: #606266; }
.collapse-btn:hover { color: #409EFF; }
.user-avatar { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.user-name { font-size: 14px; color: #303133; }
.content { flex: 1; overflow-y: auto; background: #f0f2f5; }
</style>
