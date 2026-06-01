<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon">🏫</div>
        <h1>校园自习室预约系统</h1>
        <p>Study Room Reservation</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" size="large">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名 / 学号" prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password
                @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
          <div class="test-account">
            <el-text size="small" type="info">测试账号：admin / zhangsan / lisi &nbsp; 密码：admin123</el-text>
          </div>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="regForm" :rules="regRules" ref="regFormRef" size="large">
            <el-form-item prop="username">
              <el-input v-model="regForm.username" placeholder="用户名" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="regForm.password" type="password" placeholder="密码" show-password />
            </el-form-item>
            <el-form-item prop="real_name">
              <el-input v-model="regForm.real_name" placeholder="真实姓名" />
            </el-form-item>
            <el-form-item prop="student_id">
              <el-input v-model="regForm.student_id" placeholder="学号" />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="regForm.phone" placeholder="手机号（选填）" />
            </el-form-item>
            <el-form-item>
              <el-button type="success" :loading="regLoading" class="login-btn" @click="handleRegister">
                注 册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('login')
const loading = ref(false)
const regLoading = ref(false)
const loginFormRef = ref(null)
const regFormRef = ref(null)

const loginForm = reactive({ username: 'admin', password: 'admin123' })
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const regForm = reactive({ username: '', password: '', real_name: '', student_id: '', phone: '' })
const regRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  real_name: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  student_id: [{ required: true, message: '请输入学号', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  const { ok, message } = await userStore.login(loginForm.username, loginForm.password)
  loading.value = false
  if (ok) {
    ElMessage.success('登录成功')
    router.push('/home')
  } else {
    ElMessage.error(message)
  }
}

async function handleRegister() {
  const valid = await regFormRef.value.validate().catch(() => false)
  if (!valid) return
  regLoading.value = true
  const { ok, message } = await userStore.register({ ...regForm })
  regLoading.value = false
  if (ok) {
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = regForm.username
    regFormRef.value.resetFields()
  } else {
    ElMessage.error(message)
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}
.login-card {
  width: 440px;
  background: #fff;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0,0,0,.15);
}
.login-header {
  text-align: center;
  margin-bottom: 24px;
}
.logo-icon { font-size: 48px; margin-bottom: 8px; }
.login-header h1 { font-size: 22px; color: #303133; margin: 0 0 4px; }
.login-header p { color: #909399; font-size: 13px; margin: 0; }
.login-tabs { margin-top: 8px; }
.login-btn { width: 100%; }
.test-account { text-align: center; margin-top: 8px; }
</style>
