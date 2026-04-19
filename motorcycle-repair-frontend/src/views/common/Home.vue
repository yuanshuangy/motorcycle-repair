<template>
  <div>
    <div class="page-header"><h2>{{ welcomeTitle }}</h2><span style="color:var(--text-light);font-size:14px">{{ today }}</span></div>
    <el-row :gutter="20">
      <template v-if="userStore.role===1">
        <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/admin/users')"><div class="stat-icon" style="background:#e8f4fd;color:#1890ff"><el-icon size="28"><UserFilled /></el-icon></div><div class="stat-value">{{ s.userCount||0 }}</div><div class="stat-label">用户总数</div></div></el-col>
        <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/admin/shops')"><div class="stat-icon" style="background:#e6f7e9;color:#52c41a"><el-icon size="28"><Shop /></el-icon></div><div class="stat-value">{{ s.shopCount||0 }}</div><div class="stat-label">维修店数量</div></div></el-col>
        <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/admin/shops')"><div class="stat-icon" style="background:#fff7e6;color:#fa8c16"><el-icon size="28"><Calendar /></el-icon></div><div class="stat-value">{{ s.appointmentCount||0 }}</div><div class="stat-label">预约总量</div></div></el-col>
        <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/admin/shops')"><div class="stat-icon" style="background:#fff1f0;color:#f5222d"><el-icon size="28"><Clock /></el-icon></div><div class="stat-value">{{ s.pendingAppointmentCount||0 }}</div><div class="stat-label">待处理预约</div></div></el-col>
      </template>
      <template v-if="userStore.role===2">
        <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/shop/orders')"><div class="stat-icon" style="background:#e8f4fd;color:#1890ff"><el-icon size="28"><Calendar /></el-icon></div><div class="stat-value">{{ s.appointmentCount||0 }}</div><div class="stat-label">预约总量</div></div></el-col>
        <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/shop/orders')"><div class="stat-icon" style="background:#e6f7e9;color:#52c41a"><el-icon size="28"><Clock /></el-icon></div><div class="stat-value">{{ s.todayAppointmentCount||0 }}</div><div class="stat-label">今日预约</div></div></el-col>
        <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/shop/orders')"><div class="stat-icon" style="background:#fff7e6;color:#fa8c16"><el-icon size="28"><Warning /></el-icon></div><div class="stat-value">{{ s.pendingAppointmentCount||0 }}</div><div class="stat-label">待处理</div></div></el-col>
        <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/shop/records')"><div class="stat-icon" style="background:#f9f0ff;color:#722ed1"><el-icon size="28"><SuccessFilled /></el-icon></div><div class="stat-value">{{ s.completedAppointmentCount||0 }}</div><div class="stat-label">已完成</div></div></el-col>
      </template>
      <template v-if="userStore.role===4">
        <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/tech/orders')"><div class="stat-icon" style="background:#e8f4fd;color:#1890ff"><el-icon size="28"><Calendar /></el-icon></div><div class="stat-value">{{ s.myAppointmentCount||0 }}</div><div class="stat-label">历史维修单</div></div></el-col>
        <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/tech/orders')"><div class="stat-icon" style="background:#e6f7e9;color:#52c41a"><el-icon size="28"><Clock /></el-icon></div><div class="stat-value">{{ s.todayAppointmentCount||0 }}</div><div class="stat-label">今日单量</div></div></el-col>
        <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/tech/orders')"><div class="stat-icon" style="background:#fff7e6;color:#fa8c16"><el-icon size="28"><Warning /></el-icon></div><div class="stat-value">{{ s.myPendingCount||0 }}</div><div class="stat-label">待处理</div></div></el-col>
        <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/tech/records')"><div class="stat-icon" style="background:#f9f0ff;color:#722ed1"><el-icon size="28"><Tools /></el-icon></div><div class="stat-value">{{ s.processingAppointmentCount||0 }}</div><div class="stat-label">维修中</div></div></el-col>
      </template>
    </el-row>
    <el-row v-if="userStore.role===2" :gutter="20" style="margin-top:20px">
      <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/shop/employees')"><div class="stat-icon" style="background:#e8f4fd;color:#1890ff"><el-icon size="28"><UserFilled /></el-icon></div><div class="stat-value">{{ s.technicianCount||0 }}</div><div class="stat-label">关联技师</div></div></el-col>
      <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/shop/services')"><div class="stat-icon" style="background:#e6f7e9;color:#52c41a"><el-icon size="28"><Tools /></el-icon></div><div class="stat-value">{{ s.serviceCount||0 }}</div><div class="stat-label">服务项目</div></div></el-col>
      <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/shop/employees')"><div class="stat-icon" style="background:#fff7e6;color:#fa8c16"><el-icon size="28"><Avatar /></el-icon></div><div class="stat-value">{{ s.employeeCount||0 }}</div><div class="stat-label">员工数量</div></div></el-col>
      <el-col :span="6"><div class="stat-card clickable" @click="go('/layout/shop/orders')"><div class="stat-icon" style="background:#fff1f0;color:#f5222d"><el-icon size="28"><Warning /></el-icon></div><div class="stat-value">{{ s.processingAppointmentCount||0 }}</div><div class="stat-label">维修中</div></div></el-col>
    </el-row>
    <el-row v-if="[1,2].includes(userStore.role)" :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card>
          <template #header><span style="font-weight:700">💰 流水与利润统计</span></template>
          <el-table :data="revenueData" stripe size="small">
            <el-table-column prop="period" label="时间段" width="100" />
            <el-table-column prop="revenue" label="流水(元)" align="right"><template #default="{row}"><span style="color:#1890ff;font-weight:600">¥{{ row.revenue }}</span></template></el-table-column>
            <el-table-column prop="profit" label="利润(元)" align="right"><template #default="{row}"><span :style="{color:row.profit>=0?'#52c41a':'#f5222d',fontWeight:600}">{{ row.profit>=0?'+':'' }}¥{{ row.profit }}</span></template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
    <el-row v-if="userStore.role===4" :gutter="20" style="margin-top:20px">
      <el-col :span="8">
        <el-card>
          <template #header><span style="font-weight:700">📊 我的接单排名</span></template>
          <div style="text-align:center;padding:20px 0">
            <div style="font-size:48px;font-weight:700;color:#1890ff">#{{ s.myOrderRank||'-' }}</div>
            <div style="color:#999;margin-top:8px">全平台技师接单排行</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span style="font-weight:700">📈 本月完成</span></template>
          <div style="text-align:center;padding:20px 0">
            <div style="font-size:48px;font-weight:700;color:#52c41a">{{ s.myCompletedCount||0 }}</div>
            <div style="color:#999;margin-top:8px">已完成维修单数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span style="font-weight:700">⏰ 本月加班</span></template>
          <div style="text-align:center;padding:20px 0">
            <div style="font-size:48px;font-weight:700;color:#fa8c16">{{ overtimeHours }}</div>
            <div style="color:#999;margin-top:8px">小时</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-row v-if="[1,2].includes(userStore.role) && s.revenueRanking && s.revenueRanking.length" :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <template #header><div style="display:flex;justify-content:space-between;align-items:center"><span style="font-weight:700">🏆 技师接单排行榜</span>
            <el-radio-group v-model="rankPeriod" size="small" @change="fetchStats">
              <el-radio-button value="today">今日</el-radio-button>
              <el-radio-button value="week">本周</el-radio-button>
              <el-radio-button value="month">本月</el-radio-button>
            </el-radio-group>
          </div></template>
          <div v-for="(item,idx) in s.revenueRanking" :key="idx" class="rank-item">
            <span class="rank-badge">{{ idx<3?['🥇','🥈','🥉'][idx]:idx+1 }}</span>
            <span class="rank-name">{{ item.name }}</span>
            <span class="rank-value">{{ (item.value||0)+'单' }}</span>
            <el-tag type="info" size="small" style="margin-left:4px">历史{{ item.totalCount||0 }}单</el-tag>
            <el-icon v-if="idx<3" class="praise-icon" @click="sendPraiseMsg(item,idx,'revenue')"><Message /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><div style="display:flex;justify-content:space-between;align-items:center"><span style="font-weight:700">🏅 昨日接单冠军</span><el-tag type="success" size="small">昨日</el-tag></div></template>
          <div v-if="s.yesterdayRanking && s.yesterdayRanking.length" style="text-align:center;padding:20px 0">
            <div style="font-size:48px">🥇</div>
            <h3 style="margin:12px 0">{{ s.yesterdayRanking[0].name }}</h3>
            <div style="color:#1890ff;font-size:24px;font-weight:700">{{ s.yesterdayRanking[0].value }}单</div>
            <div style="color:#999;margin-top:8px">昨日接单第一名</div>
            <div v-if="championMsg" style="margin-top:16px;padding:12px 16px;background:linear-gradient(135deg,#fff7e6,#ffe7ba);border-radius:8px;text-align:left">
              <div style="color:#d48806;font-size:12px;margin-bottom:4px">🏆 冠军寄语</div>
              <div style="color:#874d00;font-size:14px;line-height:1.6">{{ championMsg }}</div>
            </div>
            <el-button v-if="isChampion" type="warning" size="small" style="margin-top:12px" @click="championEditMsg=championMsg;showChampionEdit=true">{{ championMsg?'修改寄语':'写寄语' }}</el-button>
          </div>
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
    <el-row v-if="userStore.role===4" :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <template #header><div style="display:flex;justify-content:space-between;align-items:center"><span style="font-weight:700">🏆 技师接单排行榜</span>
            <el-radio-group v-model="rankPeriod" size="small" @change="fetchStats">
              <el-radio-button value="today">今日</el-radio-button>
              <el-radio-button value="week">本周</el-radio-button>
              <el-radio-button value="month">本月</el-radio-button>
            </el-radio-group>
          </div></template>
          <div v-for="(item,idx) in s.orderRanking" :key="idx" class="rank-item">
            <span class="rank-badge">{{ idx<3?['🥇','🥈','🥉'][idx]:idx+1 }}</span>
            <el-avatar :size="28" :src="item.avatar||defaultAvatar" style="margin-right:8px" />
            <span class="rank-name">{{ item.name }}</span>
            <span class="rank-value">{{ (item.value||0)+'单' }}</span>
            <el-tag type="info" size="small" style="margin-left:4px">历史{{ item.totalCount||0 }}单</el-tag>
            <el-tag v-if="item.id==userStore.userId" type="success" size="small" style="margin-left:4px">我</el-tag>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><div style="display:flex;justify-content:space-between;align-items:center"><span style="font-weight:700">🏅 昨日接单冠军</span><el-tag type="success" size="small">昨日</el-tag></div></template>
          <div v-if="s.yesterdayRanking && s.yesterdayRanking.length" style="text-align:center;padding:20px 0">
            <div style="font-size:48px">🥇</div>
            <h3 style="margin:12px 0">{{ s.yesterdayRanking[0].name }}</h3>
            <div style="color:#1890ff;font-size:24px;font-weight:700">{{ s.yesterdayRanking[0].value }}单</div>
            <div style="color:#999;margin-top:8px">昨日接单第一名</div>
            <div v-if="championMsg" style="margin-top:16px;padding:12px 16px;background:linear-gradient(135deg,#fff7e6,#ffe7ba);border-radius:8px;text-align:left">
              <div style="color:#d48806;font-size:12px;margin-bottom:4px">🏆 冠军寄语</div>
              <div style="color:#874d00;font-size:14px;line-height:1.6">{{ championMsg }}</div>
            </div>
            <el-button v-if="isChampion" type="warning" size="small" style="margin-top:12px" @click="championEditMsg=championMsg;showChampionEdit=true">{{ championMsg?'修改寄语':'写寄语' }}</el-button>
          </div>
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
    <el-card v-if="userStore.role!==1 && userStore.role!==3" style="margin-top:24px">
      <h3 style="margin-bottom:16px;font-weight:700">{{ introTitle }}</h3>
      <p style="line-height:2;color:var(--text-light)">{{ introText }}</p>
    </el-card>
    <el-dialog v-model="showChampionEdit" title="✍️ 写冠军寄语" width="480px" :close-on-click-modal="false">
      <p style="color:#999;margin-bottom:12px">作为昨日接单冠军，写下您的感言与鼓励吧！</p>
      <el-input v-model="championEditMsg" type="textarea" :rows="4" maxlength="200" show-word-limit placeholder="写下你的冠军感言..." />
      <template #footer>
        <el-button @click="showChampionEdit=false">取消</el-button>
        <el-button type="primary" @click="saveChampionMsg" :loading="savingChampion">保存寄语</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authAPI, shopAPI } from '../../api'
import { useUserStore } from '../../store/user'
import { useConfigStore } from '../../store/config'
import { Shop, Calendar, Clock, UserFilled, SuccessFilled, Warning, Tools, Avatar, Message } from '@element-plus/icons-vue'
const userStore = useUserStore(), router = useRouter(), configStore = useConfigStore()
const defaultAvatar = computed(() => configStore.defaultAvatar)
const go = path => router.push(path)
const s = reactive({})
const championMsg = ref(''), showChampionEdit = ref(false), championEditMsg = ref(''), savingChampion = ref(false)
const overtimeHours = ref('0.0')
const rankPeriod = ref('month')
const isChampion = computed(() => {
  if (userStore.role !== 4) return false
  return s.yesterdayRanking && s.yesterdayRanking.length && s.yesterdayRanking[0].id == userStore.userId
})
const welcomeTitle = computed(() => configStore.welcomeTitle(userStore.role))
const introTitle = computed(() => configStore.introTitle(userStore.role))
const introText = computed(() => configStore.introText(userStore.role))
const today = new Date().toLocaleDateString('zh-CN', {year:'numeric',month:'long',day:'numeric',weekday:'long'})
const revenueData = computed(() => configStore.revenuePeriods.map((p, i) => {
  const keys = ['todayRevenue','yesterdayRevenue','weekRevenue','monthRevenue','quarterRevenue','yearRevenue']
  const pKeys = ['todayProfit','yesterdayProfit','weekProfit','monthProfit','quarterProfit','yearProfit']
  return { period: p, revenue: (s[keys[i]]||0).toFixed(2), profit: (s[pKeys[i]]||0).toFixed(2) }
}))
const sendPraiseMsg = async (item, idx, type) => {
  const rank = idx + 1
  const medal = rank <= 3 ? (configStore.rankLabels[rank-1] || `第${rank}名`) : `第${rank}名`
  const periodMap = { today: configStore.periodLabels[0]||'今日', week: configStore.periodLabels[2]||'本周', month: configStore.periodLabels[3]||'本月' }
  const periodLabel = periodMap[rankPeriod.value] || '本月'
  const defaultContent = (configStore.praiseTemplate || '').replace('{name}', item.name).replace('{period}', periodLabel).replace('{rank}', medal)
  try {
    const { value: customContent } = await ElMessageBox.prompt(defaultContent, `发送表扬信给「${item.name}」`, {
      confirmButtonText: '发送', cancelButtonText: '取消',
      inputType: 'textarea', inputPlaceholder: '可自定义表扬内容，留空使用默认内容',
      inputAttributes: { rows: 4 }
    })
    await authAPI.sendPraise(item.id, userStore.userId, userStore.role, type, item.name, rank, periodLabel, customContent || '')
    ElMessage.success('表扬消息已发送')
  } catch {}
}
const getLocalDateStr = (date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}
const fetchChampionMsg = async () => {
  if (s.yesterdayRanking && s.yesterdayRanking.length) {
    const champion = s.yesterdayRanking[0]
    try {
      const yesterday = new Date()
      yesterday.setDate(yesterday.getDate() - 1)
      const dateStr = getLocalDateStr(yesterday)
      const r = await authAPI.getChampionMessage(champion.id, dateStr)
      if (r.code === 200 && r.data) championMsg.value = r.data.message || ''
    } catch {}
  }
}
const saveChampionMsg = async () => {
  if (!championEditMsg.value.trim()) return ElMessage.warning('请输入寄语内容')
  savingChampion.value = true
  try {
    const yesterday = new Date()
    yesterday.setDate(yesterday.getDate() - 1)
    const dateStr = getLocalDateStr(yesterday)
    await authAPI.saveChampionMessage(userStore.userId, dateStr, championEditMsg.value.trim())
    championMsg.value = championEditMsg.value.trim()
    showChampionEdit.value = false
    ElMessage.success('寄语已保存')
  } catch { ElMessage.error('保存失败') }
  finally { savingChampion.value = false }
}
const fetchOvertime = async () => {
  if (userStore.role !== 4) return
  try {
    const r = await authAPI.getOvertime(userStore.userId)
    if (r.code === 200 && r.data !== null) overtimeHours.value = r.data
  } catch {}
}
const fetchStats = async () => {
  try {
    const r = await authAPI.getStatistics(userStore.userId, userStore.role, rankPeriod.value)
    if(r.code===200) { Object.keys(s).forEach(k => delete s[k]); Object.assign(s, r.data); fetchChampionMsg() }
  } catch(e){}
}
onMounted(async () => {
  fetchStats()
  fetchOvertime()
})
</script>
<style scoped>
.clickable{cursor:pointer;transition:all .2s}
.clickable:hover{transform:translateY(-3px);box-shadow:0 6px 16px rgba(0,0,0,.12)}
.rank-item{display:flex;align-items:center;padding:10px 12px;border-bottom:1px solid #f0f0f0;transition:background .2s}
.rank-item:hover{background:#fafafa}
.rank-badge{width:32px;font-size:18px;text-align:center;flex-shrink:0}
.rank-name{flex:1;font-weight:500;margin-left:8px}
.rank-value{font-weight:700;color:#1890ff;margin-left:12px;white-space:nowrap}
.praise-icon{color:#fa8c16;font-size:18px;cursor:pointer;margin-left:8px;transition:color .2s}
.praise-icon:hover{color:#f5222d}
</style>
