import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/components/NavBar.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'seats',
        name: 'SeatMap',
        component: () => import('@/views/SeatMap.vue'),
        meta: { title: '座位预约' }
      },
      {
        path: 'my-reservations',
        name: 'MyReservations',
        component: () => import('@/views/MyReservations.vue'),
        meta: { title: '我的预约' }
      },
      {
        path: 'admin/dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据看板', role: 'admin' }
      },
      {
        path: 'admin/seats',
        name: 'AdminSeats',
        component: () => import('@/views/admin/Seats.vue'),
        meta: { title: '座位管理', role: 'admin' }
      },
      {
        path: 'admin/reservations',
        name: 'AdminReservations',
        component: () => import('@/views/admin/Reservations.vue'),
        meta: { title: '预约管理', role: 'admin' }
      },
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: { title: '用户管理', role: 'admin' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 自习室预约` : '校园自习室预约系统'
  const userStore = useUserStore()

  if (to.path === '/login') {
    if (userStore.isLoggedIn) return next('/home')
    return next()
  }

  if (!userStore.isLoggedIn) {
    return next('/login')
  }

  if (to.meta.role === 'admin' && userStore.user?.role !== 'admin') {
    ElMessage.warning('需要管理员权限')
    return next('/home')
  }

  next()
})

export default router
