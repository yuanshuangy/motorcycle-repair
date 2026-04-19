<template>
  <div>
    <div class="page-header"><h2>技师管理</h2><div style="display:flex;gap:8px"><el-button type="primary" @click="openAddTech">新增技师</el-button><el-button type="success" @click="openBindTech">关联技师</el-button><el-button type="warning" @click="openRestSchedule">📅 自动排休</el-button></div></div>
    <el-card v-if="tomorrowRestTechs.length" style="margin-bottom:16px;border:2px solid #fa8c16">
      <template #header><div style="display:flex;justify-content:space-between;align-items:center"><span style="font-weight:700;color:#fa8c16">🔔 明日休息提醒</span><el-tag type="warning" size="small">{{ tomorrowStr }}</el-tag></div></template>
      <div style="display:flex;gap:12px;flex-wrap:wrap">
        <el-tag v-for="t in tomorrowRestTechs" :key="t.userId" type="danger" effect="dark" size="large" style="font-size:14px;padding:8px 16px">
          😴 {{ t.name }} <span style="opacity:0.7;margin-left:4px">({{ (t.skill||'').split(/[,，、]/).filter(s=>s.trim()&&s!=='司机').join('/') }})</span>
        </el-tag>
      </div>
    </el-card>
    <el-card>
      <template #header><div style="display:flex;justify-content:space-between;align-items:center"><span style="font-weight:700">本店技师</span><el-tag type="info" size="small">共 {{ techStats.length }} 人</el-tag></div></template>
      <el-row :gutter="16">
        <el-col :span="6" v-for="tech in techStats" :key="tech.id" style="margin-bottom:16px;display:flex">
          <el-popover placement="bottom" :width="260" trigger="hover" :show-after="200" style="width:100%">
            <template #reference>
              <el-card shadow="hover" class="tech-card" :style="tech.restStatus===1?'border:2px solid #f5222d':''" @click="openEditTech(tech)" style="width:100%">
                <div style="text-align:center;padding:12px 0">
                  <el-avatar :size="64" :src="tech.avatar||defaultAvatar" />
                  <h3 style="margin:10px 0 4px;font-size:16px">{{ tech.realName }}</h3>
                  <p style="color:#999;font-size:12px;margin:2px 0">{{ tech.phone||'' }}</p>
                  <div style="margin:6px 0">
                    <el-tag v-for="sk in (tech.skill||'').split(/[,，、]/).filter(s=>s.trim())" :key="sk" :type="sk==='司机'?'primary':'warning'" size="small" style="margin:2px">{{ sk.trim() }}</el-tag>
                  </div>
                  <el-divider style="margin:12px 0" />
                  <el-row :gutter="8">
                    <el-col :span="8"><div class="tech-stat"><div class="tech-stat-val">{{ tech.totalCount||0 }}</div><div class="tech-stat-label">总工单</div></div></el-col>
                    <el-col :span="8"><div class="tech-stat"><div class="tech-stat-val" style="color:#52c41a">{{ tech.completedCount||0 }}</div><div class="tech-stat-label">已完成</div></div></el-col>
                    <el-col :span="8"><div class="tech-stat"><div class="tech-stat-val" style="color:#1890ff">{{ tech.processingCount||0 }}</div><div class="tech-stat-label">维修中</div></div></el-col>
                  </el-row>
                  <div style="margin-top:10px"><el-progress :percentage="tech.totalCount>0?Math.round((tech.completedCount||0)/tech.totalCount*100):0" :stroke-width="6" :color="progressColor(tech)" /></div>
                  <div style="margin-top:12px;display:flex;gap:8px;justify-content:center;align-items:center;flex-wrap:wrap">
                    <el-tag :type="tech.restStatus===1?'danger':'success'" effect="dark" size="small">{{ tech.restStatus===1?'休息中':'在岗' }}</el-tag>
                    <el-tag v-if="isTomorrowRest(tech.id)" type="warning" effect="dark" size="small">明日休息</el-tag>
                  </div>
                </div>
              </el-card>
            </template>
            <div class="tech-popover">
              <div class="tech-popover-header">
                <el-avatar :size="48" :src="tech.avatar||defaultAvatar" />
                <div class="tech-popover-info">
                  <h4 style="margin:0 0 4px">{{ tech.realName }}</h4>
                  <el-tag :type="tech.restStatus===1?'danger':'success'" size="small" effect="dark">{{ tech.restStatus===1?'休息中':'在岗' }}</el-tag>
                </div>
              </div>
              <el-divider style="margin:8px 0" />
              <div class="tech-popover-detail">
                <div class="detail-row" v-if="tech.phone"><span class="detail-label">📞 电话</span><span>{{ tech.phone }}</span></div>
                <div class="detail-row"><span class="detail-label">🔧 技能</span><span>{{ tech.skill||'暂无' }}</span></div>
                <div class="detail-row"><span class="detail-label">✅ 已完成</span><span>{{ tech.completedCount||0 }}单</span></div>
                <div class="detail-row"><span class="detail-label">🔧 维修中</span><span>{{ tech.processingCount||0 }}单</span></div>
                <div class="detail-row"><span class="detail-label">📊 总工单</span><span>{{ tech.totalCount||0 }}单</span></div>
              </div>
              <div style="margin-top:8px;text-align:center;color:#1890ff;font-size:12px">单击卡片编辑信息</div>
            </div>
          </el-popover>
        </el-col>
      </el-row>
      <el-empty v-if="!techStats.length" description="暂无关联技师" />
    </el-card>
    <el-dialog v-model="showAddTech" title="新增技师" width="550px" :close-on-click-modal="false">
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="登录账号"><el-input :value="addForm.username" disabled /><div style="color:#999;font-size:12px;margin-top:4px">系统自动生成，密码与账号相同</div></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="addForm.realName" placeholder="请输入技师姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="addForm.phone" maxlength="11" placeholder="11位手机号" /></el-form-item>
        <el-form-item label="专业技能" required><el-input v-model="addForm.skill" placeholder="用逗号分隔，如：发动机维修,电路检修" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showAddTech=false">取消</el-button><el-button type="primary" @click="submitAddTech" :loading="adding">确定新增</el-button></template>
    </el-dialog>
    <el-dialog v-model="showEditTech" title="编辑技师信息" width="550px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="登录账号"><el-input :value="editForm.username" disabled /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="editForm.realName" placeholder="请输入技师姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="editForm.phone" maxlength="11" placeholder="11位手机号" /></el-form-item>
        <el-form-item label="专业技能" required><el-input v-model="editForm.skill" placeholder="用逗号分隔，如：发动机维修,电路检修" /></el-form-item>
        <el-form-item label="休息状态">
          <div style="display:flex;gap:8px;align-items:center">
            <el-tag :type="editForm.restStatus===1?'danger':'success'" effect="dark">{{ editForm.restStatus===1?'休息中':'在岗' }}</el-tag>
            <el-button size="small" :type="editForm.restStatus===1?'success':'warning'" @click="editForm.restStatus=editForm.restStatus===1?0:1">{{ editForm.restStatus===1?'恢复在岗':'设为休息' }}</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="danger" @click="handleUnbind({id:editForm.id,realName:editForm.realName})">解除关联</el-button>
        <el-button @click="showEditTech=false">取消</el-button>
        <el-button type="primary" @click="submitEditTech" :loading="editing">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showBindTech" title="关联技师" width="700px" :close-on-click-modal="false">
      <p style="color:#999;margin-bottom:12px">选择已有技师关联到本店（仅显示未关联到本店的技师）</p>
      <el-input v-model="bindSearch" placeholder="搜索技师姓名或技能" clearable style="margin-bottom:12px" />
      <el-table :data="filteredAvailTechs" stripe max-height="400" v-loading="bindLoading">
        <el-table-column prop="realName" label="姓名" width="100"><template #default="{row}">{{ row.realName||row.username }}</template></el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="skill" label="专业技能"><template #default="{row}"><el-tag v-for="sk in (row.skill||'').split(/[,，、]/).filter(s=>s.trim())" :key="sk" type="warning" size="small" style="margin:2px">{{ sk.trim() }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="100"><template #default="{row}"><el-button size="small" type="success" @click="doBindTech(row)" :loading="row._binding">关联</el-button></template></el-table-column>
      </el-table>
      <el-empty v-if="!bindLoading&&filteredAvailTechs.length===0" description="暂无可关联的技师" />
    </el-dialog>
    <el-dialog v-model="showRestSchedule" title="📅 自动排休（两周循环）" width="1100px" :close-on-click-modal="false">
      <div style="margin-bottom:16px">
        <el-alert type="info" :closable="false" style="margin-bottom:12px">
          <template #title>
            <div style="font-size:13px;line-height:1.8">
              📋 排休规则：{{ configStore.restRules }}
            </div>
          </template>
        </el-alert>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div style="display:flex;gap:8px;align-items:center">
            <el-tag type="danger" effect="dark" size="small">休息</el-tag>
            <el-tag type="success" size="small">在岗</el-tag>
            <el-tag type="warning" effect="dark" size="small">明日休息</el-tag>
          </div>
          <div style="display:flex;gap:8px">
            <el-button type="danger" size="small" @click="clearSchedule" :loading="scheduleLoading">清除排休</el-button>
            <el-button type="primary" size="small" @click="autoSchedule" :loading="scheduleLoading">🔄 自动排休</el-button>
          </div>
        </div>
      </div>
      <el-table :data="scheduleTable" stripe border max-height="450" v-loading="scheduleLoading" :cell-class-name="scheduleCellClass">
        <el-table-column prop="techName" label="技师" width="100" fixed />
        <el-table-column v-for="day in scheduleDays" :key="day.date" :min-width="100" align="center">
          <template #header>
            <div :style="day.isTomorrow?'color:#fa8c16;font-weight:700':''">
              <span v-if="day.week===2" style="color:#722ed1">[下周]</span>
              {{ day.label }}
              <el-tag v-if="day.isToday" type="success" size="small" style="margin-left:2px">今</el-tag>
              <el-tag v-if="day.isTomorrow" type="warning" size="small" style="margin-left:2px">明</el-tag>
            </div>
          </template>
          <template #default="{row}">
            <el-tag v-if="row.days[day.date]" type="danger" effect="dark" size="small">😴 休息</el-tag>
            <el-tag v-else type="success" size="small">🔧 在岗</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="本周休" width="70" align="center">
          <template #default="{row}">
            <span :style="{color:row.restCount>=2?'#f5222d':'#333',fontWeight:row.restCount>=2?'700':'normal'}">{{ row.restCount }}/2</span>
          </template>
        </el-table-column>
        <el-table-column label="下周休" width="70" align="center">
          <template #default="{row}">
            <span :style="{color:row.restCount2>=2?'#f5222d':'#333',fontWeight:row.restCount2>=2?'700':'normal'}">{{ row.restCount2 }}/2</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="scheduleTable.length" style="margin-top:12px;padding:10px;background:#f6ffed;border-radius:6px;border:1px solid #b7eb8f">
        <div style="font-size:13px;color:#389e0d">
          📊 两周在岗统计：每天平均在岗 <b>{{ avgActive }}</b> 人，覆盖率 <b>{{ coverageRate }}%</b>
        </div>
      </div>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { shopAPI, authAPI } from '../../api'
import { useUserStore } from '../../store/user'
import { useConfigStore } from '../../store/config'
const userStore = useUserStore(), configStore = useConfigStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const techStats = ref([]), shopId = ref(null)
const showAddTech = ref(false), adding = ref(false)
const addForm = reactive({ username:'', realName:'', phone:'', skill:'' })
const showEditTech = ref(false), editing = ref(false)
const editForm = reactive({ id:null, username:'', realName:'', phone:'', skill:'', restStatus:0 })
const showBindTech = ref(false), bindLoading = ref(false), bindSearch = ref(''), availTechs = ref([])
const showRestSchedule = ref(false), scheduleLoading = ref(false), scheduleData = ref([]), scheduleDays = ref([]), scheduleTable = ref([])
const tomorrowRestTechs = ref([])
const tomorrowStr = computed(() => {
  const d = new Date(); d.setDate(d.getDate()+1)
  return `${d.getMonth()+1}月${d.getDate()}日`
})
const progressColor = row => { const r = row.totalCount>0?(row.completedCount||0)/row.totalCount:0; return r>=0.8?'#67c23a':r>=0.5?'#e6a23c':'#f56c6c' }
const isTomorrowRest = techId => tomorrowRestTechs.value.some(t => t.userId === techId)
const avgActive = computed(() => {
  if (!scheduleTable.value.length || !scheduleDays.value.length) return 0
  let total = 0
  scheduleDays.value.forEach(day => {
    const activeCount = scheduleTable.value.filter(r => !r.days[day.date]).length
    total += activeCount
  })
  return (total / scheduleDays.value.length).toFixed(1)
})
const coverageRate = computed(() => {
  if (!scheduleTable.value.length) return 0
  return Math.round((avgActive.value / scheduleTable.value.length) * 100)
})
const scheduleCellClass = ({ row, columnIndex }) => {
  if (columnIndex === 0) return ''
  const day = scheduleDays.value[columnIndex - 1]
  if (day && day.isTomorrow) return 'tomorrow-cell'
  return ''
}
const fetchShop = async () => { try { const r = await shopAPI.getByUserId(userStore.userId); if(r.code===200&&r.data) shopId.value=r.data.id } catch{} }
const fetchTechStats = async () => { if(!shopId.value) return; try { const r=await shopAPI.getTechnicianStats(shopId.value); if(r.code===200) techStats.value=r.data } catch{} }
const fetchTomorrowRest = async () => {
  if (!shopId.value) return
  try { const r = await shopAPI.getTomorrowRestTechs(shopId.value); if(r.code===200) tomorrowRestTechs.value=r.data } catch{}
}
const openEditTech = tech => {
  editForm.id=tech.id; editForm.username=tech.realName||''; editForm.realName=tech.realName||''
  editForm.phone=tech.phone||''; editForm.skill=tech.skill||''; editForm.restStatus=tech.restStatus||0
  showEditTech.value=true
}
const submitEditTech = async () => {
  if(!editForm.realName) return ElMessage.warning('请输入技师姓名')
  if(!editForm.skill) return ElMessage.warning('请输入专业技能')
  if(editForm.phone && !/^1[3-9]\d{9}$/.test(editForm.phone)) return ElMessage.warning('手机号格式不正确')
  editing.value=true
  try {
    await authAPI.updateUser(editForm.id, { realName:editForm.realName, phone:editForm.phone, skill:editForm.skill })
    if(editForm.restStatus !== undefined) await shopAPI.toggleTechRest(shopId.value, editForm.id, editForm.restStatus)
    ElMessage.success('技师信息已更新')
    showEditTech.value=false; fetchTechStats(); fetchTomorrowRest()
  } catch(e) { ElMessage.error('更新失败') }
  finally { editing.value=false }
}
const openAddTech = async () => {
  try { const r = await authAPI.getNextTechUsername(); if(r.code===200) addForm.username=r.data } catch{ addForm.username='zhang009' }
  addForm.realName=''; addForm.phone=''; addForm.skill=''
  showAddTech.value=true
}
const submitAddTech = async () => {
  if(!addForm.realName) return ElMessage.warning('请输入技师姓名')
  if(!addForm.skill) return ElMessage.warning('请输入专业技能')
  if(addForm.phone && !/^1[3-9]\d{9}$/.test(addForm.phone)) return ElMessage.warning('手机号格式不正确')
  adding.value=true
  try {
    const regR = await authAPI.register({ username:addForm.username, password:addForm.username, realName:addForm.realName, phone:addForm.phone, email:addForm.username+'@moto.com', role:4 })
    if(regR.code!==200) { ElMessage.error(regR.message||'创建账号失败'); adding.value=false; return }
    const loginR = await authAPI.login({ username:addForm.username, password:addForm.username })
    if(loginR.code===200 && loginR.data) {
      await shopAPI.bindTechnician(shopId.value, loginR.data.id)
      ElMessage.success('技师创建成功！账号：'+addForm.username+'，密码：'+addForm.username)
      showAddTech.value=false; fetchTechStats()
    }
  } catch(e) { ElMessage.error('创建失败') }
  finally { adding.value=false }
}
const filteredAvailTechs = computed(() => {
  if (!bindSearch.value) return availTechs.value
  const kw = bindSearch.value.toLowerCase()
  return availTechs.value.filter(t => (t.realName||t.username||'').toLowerCase().includes(kw) || (t.skill||'').toLowerCase().includes(kw))
})
const openBindTech = async () => {
  if (!shopId.value) return ElMessage.warning('请先关联维修店')
  bindLoading.value = true; bindSearch.value = ''; showBindTech.value = true
  try {
    const [allR, boundR] = await Promise.all([shopAPI.getAvailableTechnicians(), shopAPI.getTechnicians(shopId.value)])
    const boundIds = new Set((boundR.code===200?boundR.data:[]).map(t => t.id))
    availTechs.value = (allR.code===200?allR.data:[]).filter(t => !boundIds.has(t.id)).map(t => ({ ...t, _binding: false }))
  } catch { availTechs.value = [] }
  finally { bindLoading.value = false }
}
const doBindTech = async row => {
  row._binding = true
  try {
    await shopAPI.bindTechnician(shopId.value, row.id)
    ElMessage.success(`技师「${row.realName||row.username}」已关联到本店`)
    availTechs.value = availTechs.value.filter(t => t.id !== row.id)
    fetchTechStats()
  } catch { ElMessage.error('关联失败') }
  finally { row._binding = false }
}
const openRestSchedule = async () => {
  if (!shopId.value) return ElMessage.warning('请先关联维修店')
  showRestSchedule.value = true
  await fetchSchedule()
}
const fetchSchedule = async () => {
  scheduleLoading.value = true
  try {
    const r = await shopAPI.getRestSchedule(shopId.value)
    scheduleData.value = r.code === 200 ? r.data : []
    buildScheduleTable()
  } catch { scheduleData.value = [] }
  finally { scheduleLoading.value = false }
}
const buildScheduleTable = () => {
  const days = []
  const today = new Date()
  const tomorrow = new Date(); tomorrow.setDate(tomorrow.getDate()+1)
  const weekNames = ['周日','周一','周二','周三','周四','周五','周六']
  const todayStr = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`
  const tomorrowDateStr = `${tomorrow.getFullYear()}-${String(tomorrow.getMonth()+1).padStart(2,'0')}-${String(tomorrow.getDate()).padStart(2,'0')}`
  const todayDay = today.getDay()
  const mondayOffset = todayDay === 0 ? -6 : 1 - todayDay
  const monday = new Date(today)
  monday.setDate(today.getDate() + mondayOffset)
  const nextMonday = new Date(monday)
  nextMonday.setDate(monday.getDate() + 7)
  const nextWeekEnd = new Date(monday)
  nextWeekEnd.setDate(monday.getDate() + 13)
  const dayCount = Math.round((nextWeekEnd - today) / (1000 * 60 * 60 * 24)) + 1
  for (let i = 0; i < dayCount; i++) {
    const d = new Date(today)
    d.setDate(d.getDate() + i)
    const dateStr = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
    days.push({
      date: dateStr,
      label: `${d.getMonth()+1}/${d.getDate()} ${weekNames[d.getDay()]}`,
      isToday: dateStr === todayStr,
      isTomorrow: dateStr === tomorrowDateStr,
      week: d < nextMonday ? 1 : 2
    })
  }
  scheduleDays.value = days
  const restMap = {}
  scheduleData.value.forEach(s => {
    let dateStr = s.restDate
    if (!dateStr) return
    if (typeof dateStr !== 'string') {
      if (Array.isArray(dateStr)) {
        dateStr = `${dateStr[0]}-${String(dateStr[1]).padStart(2,'0')}-${String(dateStr[2]).padStart(2,'0')}`
      } else { dateStr = String(dateStr) }
    }
    if (dateStr.includes('T')) dateStr = dateStr.split('T')[0]
    if (!restMap[s.userId]) restMap[s.userId] = new Set()
    restMap[s.userId].add(dateStr)
  })
  const table = techStats.value.map(tech => {
    const daysMap = {}
    let restCount = 0, restCount2 = 0
    days.forEach(day => {
      const isRest = restMap[tech.id] && restMap[tech.id].has(day.date)
      daysMap[day.date] = isRest
      if (isRest && day.week === 1) restCount++
      if (isRest && day.week === 2) restCount2++
    })
    return { techId: tech.id, techName: tech.realName, days: daysMap, restCount, restCount2 }
  })
  scheduleTable.value = table
}
const autoSchedule = async () => {
  scheduleLoading.value = true
  try {
    const r = await shopAPI.autoRestSchedule(shopId.value)
    if (r.code === 200) {
      ElMessage.success('自动排休完成！已按两周循环安排，每人每周最多休2天')
      scheduleData.value = r.data || []
      buildScheduleTable()
      fetchTomorrowRest()
      fetchTechStats()
    } else ElMessage.error(r.message || '排休失败')
  } catch { ElMessage.error('排休失败') }
  finally { scheduleLoading.value = false }
}
const clearSchedule = async () => {
  try { await ElMessageBox.confirm('确定清除所有排休计划？', '提示', { type: 'warning' }) } catch { return }
  scheduleLoading.value = true
  try {
    await shopAPI.clearRestSchedule(shopId.value)
    ElMessage.success('已清除')
    scheduleData.value = []
    buildScheduleTable()
    fetchTomorrowRest()
    fetchTechStats()
  } catch { ElMessage.error('清除失败') }
  finally { scheduleLoading.value = false }
}
const handleUnbind = async row => { await ElMessageBox.confirm(`确定解除与技师「${row.realName}」的关联？`,'提示',{type:'warning'}); try { await shopAPI.unbindTechnician(shopId.value, row.id); ElMessage.success('已解除'); fetchTechStats() } catch{} }
const toggleRest = async tech => {
  const newStatus = tech.restStatus === 1 ? 0 : 1
  if (newStatus === 1) {
    const activeTechs = techStats.value.filter(t => t.restStatus !== 1 && t.id !== tech.id)
    const techSkills = (tech.skill||'').split(/[,，、]/).filter(s=>s.trim()&&s!=='司机').map(s=>s.trim())
    const affectedSkills = techSkills.filter(sk => {
      const otherTechsHaveIt = activeTechs.some(t => (t.skill||'').split(/[,，、]/).filter(s=>s.trim()&&s!=='司机').map(s=>s.trim()).includes(sk))
      return !otherTechsHaveIt
    })
    const activeCount = techStats.value.filter(t => t.restStatus !== 1).length
    if (activeCount <= 2) return ElMessage.warning('在岗技师不足2人，无法再设为休息')
    if (affectedSkills.length > 0) {
      try { await ElMessageBox.confirm(`该技师休息后，以下技能将无人覆盖：${affectedSkills.join('、')}，确定设为休息？`, '技能覆盖提醒', { type: 'warning', confirmButtonText: '仍然休息', cancelButtonText: '取消' }) }
      catch { return }
    }
  }
  try {
    await shopAPI.toggleTechRest(shopId.value, tech.id, newStatus)
    ElMessage.success(newStatus === 1 ? '已设为休息' : '已恢复在岗')
    fetchTechStats()
  } catch { ElMessage.error('操作失败') }
}
onMounted(async () => { await fetchShop(); fetchTechStats(); fetchTomorrowRest() })
</script>
<style scoped>
.tech-card{transition:all .2s;cursor:pointer;height:100%}
.tech-card:hover{transform:translateY(-4px);box-shadow:0 6px 16px rgba(0,0,0,.1)}
.tech-card :deep(.el-card__body){padding:16px}
.tech-stat{text-align:center}
.tech-stat-val{font-size:20px;font-weight:700;color:#333}
.tech-stat-label{font-size:12px;color:#999;margin-top:2px}
.tech-popover-header{display:flex;align-items:center;gap:12px}
.tech-popover-info h4{font-size:16px}
.tech-popover-detail{display:flex;flex-direction:column;gap:6px}
.detail-row{display:flex;justify-content:space-between;align-items:center;font-size:13px;color:#333}
.detail-label{color:#666;font-size:12px}
</style>
