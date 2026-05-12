<template>
  <div class="login-page">
    <div class="login-left">
      <div class="brand-area">
        <div class="brand-icon">🏍</div>
        <h1>{{ systemName }}</h1>
        <p>{{ configStore.brandSlogan }}</p>
        <div class="features">
          <div v-for="f in configStore.brandFeatures" :key="f" class="feat-item"><el-icon><Check /></el-icon> {{ f }}</div>
        </div>
      </div>
    </div>
    <div class="login-right">
      <div class="login-form-wrapper">
        <h2>{{ configStore.loginWelcome }}</h2>
        <p style="color: var(--text-light); margin-bottom: 32px">{{ configStore.loginSubtitle }}</p>
        <el-form ref="formRef" :model="form" :rules="rules" size="large">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" style="width:100%;border-radius:8px;height:44px" @click="handleLogin" :loading="loading">登 录</el-button>
          </el-form-item>
        </el-form>
        <div style="text-align:center">
          <span style="color:var(--text-light)">还没有账号？</span>
          <el-link type="primary" @click="$router.push('/register')" style="font-weight:600">立即注册</el-link>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authAPI, configAPI } from '../../api'
import { useUserStore } from '../../store/user'
import { useConfigStore } from '../../store/config'
const router = useRouter(), userStore = useUserStore(), configStore = useConfigStore()
const systemName = ref('摩托车维修预约系统')
onMounted(async () => { try { const r = await configAPI.getSystemConfigs(); if(r.code===200 && r.data.system_name) systemName.value = r.data.system_name } catch{} })
const formRef = ref(), loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = { username: [{ required: true, message: '请输入用户名', trigger: 'blur' }], password: [{ required: true, message: '请输入密码', trigger: 'blur' }] }
const handleLogin = async () => {
  try { await formRef.value.validate() } catch { return }
  loading.value = true
  try {
    const r = await authAPI.login(form)
    if (r.code === 200) {
      userStore.setUser(r.data)
      ElMessage.success('登录成功')
      if (r.data.role === 3) router.push('/layout/user/shops')
      else router.push('/layout/home')
    }
    else ElMessage.error(r.message)
  } catch (e) { ElMessage.error('登录失败') }
  finally { loading.value = false }
}
</script>
<style scoped>
.login-page { display: flex; height: 100vh; }
.login-left { flex: 1; background: linear-gradient(135deg, #1a1a2e 0%, #0f3460 100%); display: flex; align-items: center; justify-content: center; color: white; }
.brand-area { text-align: center; padding: 40px; }
.brand-icon { font-size: 80px; margin-bottom: 20px; }
.brand-area h1 { font-size: 32px; font-weight: 800; margin-bottom: 8px; }
.brand-area > p { font-size: 18px; opacity: 0.8; margin-bottom: 40px; letter-spacing: 8px; }
.features { text-align: left; max-width: 280px; margin: 0 auto; }
.feat-item { display: flex; align-items: center; gap: 12px; padding: 10px 0; font-size: 15px; opacity: 0.9; }
.login-right { flex: 1; display: flex; align-items: center; justify-content: center; background: #fff; }
.login-form-wrapper { width: 380px; padding: 40px; }
.login-form-wrapper h2 { font-size: 28px; font-weight: 800; color: var(--primary); }
</style>
