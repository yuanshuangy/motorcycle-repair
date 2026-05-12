<template>
  <div class="login-page">
    <div class="login-left"><div class="brand-area"><div class="brand-icon">🏍</div><h1>摩托车维修预约系统</h1><p>专业 · 便捷 · 高效</p></div></div>
    <div class="login-right">
      <div class="login-form-wrapper">
        <h2>创建账号</h2>
        <p style="color:var(--text-light);margin-bottom:32px">填写信息完成注册</p>
        <el-form ref="formRef" :model="form" :rules="rules" size="large">
          <el-form-item prop="username"><el-input v-model="form.username" placeholder="用户名(4-20位字母数字下划线)" prefix-icon="User" /></el-form-item>
          <el-form-item prop="password"><el-input v-model="form.password" type="password" placeholder="密码(6-20位)" prefix-icon="Lock" show-password /></el-form-item>
          <el-form-item prop="realName"><el-input v-model="form.realName" placeholder="真实姓名(2-20字)" prefix-icon="UserFilled" /></el-form-item>
          <el-form-item prop="phone"><el-input v-model="form.phone" placeholder="手机号" prefix-icon="Phone" /></el-form-item>
          <el-form-item prop="email"><el-input v-model="form.email" placeholder="邮箱(选填)" prefix-icon="Message" /></el-form-item>
          <el-form-item><el-button type="primary" style="width:100%;border-radius:8px;height:44px" @click="handleRegister" :loading="loading">注 册</el-button></el-form-item>
        </el-form>
        <div style="text-align:center"><span style="color:var(--text-light)">已有账号？</span><el-link type="primary" @click="$router.push('/login')" style="font-weight:600">去登录</el-link></div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authAPI } from '../../api'
const router = useRouter(), formRef = ref(), loading = ref(false)
const form = reactive({ username:'', password:'', realName:'', phone:'', email:'' })
const validateUsername = async (rule, value, callback) => {
  if (!value) return callback(new Error('请输入用户名'))
  if (!/^[a-zA-Z0-9_]{4,20}$/.test(value)) return callback(new Error('用户名需4-20位，只能包含字母、数字和下划线'))
  try {
    const r = await authAPI.checkUsername(value)
    if (r.code === 200 && r.data) return callback(new Error('用户名已存在'))
  } catch {}
  callback()
}
const validatePassword = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入密码'))
  if (value.length < 6 || value.length > 20) return callback(new Error('密码需6-20位'))
  callback()
}
const validateRealName = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入姓名'))
  if (value.length < 2 || value.length > 20) return callback(new Error('姓名需2-20个字符'))
  callback()
}
const validatePhone = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入手机号'))
  if (!/^1[3-9]\d{9}$/.test(value)) return callback(new Error('手机号格式不正确'))
  callback()
}
const validateEmail = (rule, value, callback) => {
  if (value && !/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(value)) return callback(new Error('邮箱格式不正确'))
  callback()
}
const rules = {
  username: [{ required: true, validator: validateUsername, trigger: 'blur' }],
  password: [{ required: true, validator: validatePassword, trigger: 'blur' }],
  realName: [{ required: true, validator: validateRealName, trigger: 'blur' }],
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  email: [{ validator: validateEmail, trigger: 'blur' }],
}
const handleRegister = async () => {
  try { await formRef.value.validate() } catch { return }
  loading.value = true
  try {
    const r = await authAPI.register(form)
    if (r.code === 200) {
      ElMessage.success('注册成功')
      router.push('/login')
    } else {
      const msgs = r.message ? r.message.split('；') : ['注册失败']
      ElMessageBox.alert(msgs.map(m => '• ' + m).join('<br>'), '注册失败', { dangerouslyUseHTMLString: true, type: 'error' })
    }
  } catch(e) {
    ElMessage.error('注册失败，请稍后重试')
  } finally { loading.value = false }
}
</script>
<style scoped>
.login-page{display:flex;height:100vh}
.login-left{flex:1;background:linear-gradient(135deg,#1a1a2e 0%,#0f3460 100%);display:flex;align-items:center;justify-content:center;color:#fff}
.brand-area{text-align:center;padding:40px}
.brand-icon{font-size:80px;margin-bottom:20px}
.brand-area h1{font-size:32px;font-weight:800;margin-bottom:8px}
.brand-area>p{font-size:18px;opacity:.8;letter-spacing:8px}
.login-right{flex:1;display:flex;align-items:center;justify-content:center;background:#fff}
.login-form-wrapper{width:380px;padding:40px}
.login-form-wrapper h2{font-size:28px;font-weight:800;color:var(--primary)}
</style>
