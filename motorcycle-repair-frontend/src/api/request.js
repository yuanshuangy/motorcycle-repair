import axios from 'axios'
import { ElMessage } from 'element-plus'
const api = axios.create({ baseURL: '/api', timeout: 15000 })
api.interceptors.request.use(c => { const t = localStorage.getItem('token'); if (t) c.headers.Authorization = `Bearer ${t}`; return c })
api.interceptors.response.use(r => r.data, e => {
  if (e.response?.status === 401) {
    ElMessage.warning('登录已过期，请重新登录')
    const keys = ['token', 'userId', 'username', 'realName', 'role', 'avatar']
    keys.forEach(k => localStorage.removeItem(k))
    setTimeout(() => { window.location.href = '/login' }, 500)
  }
  return Promise.reject(e)
})
export default api
