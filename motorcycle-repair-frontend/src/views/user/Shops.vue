<template>
  <div>
    <div class="page-header"><h2>{{ configStore.shopDisplayName }}</h2></div>
    <el-card style="margin-bottom:20px">
      <div style="display:flex;gap:24px;align-items:flex-start">
        <el-avatar :size="100" shape="square" :src="shop.coverImage||defaultCover" />
        <div style="flex:1">
          <h2 style="margin:0 0 8px">{{ shop.shopName }}</h2>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="地址"><el-icon><Location /></el-icon> {{ shop.address }}</el-descriptions-item>
            <el-descriptions-item label="电话"><el-icon><Phone /></el-icon> {{ shop.phone }}</el-descriptions-item>
            <el-descriptions-item label="营业时间"><el-icon><Clock /></el-icon> {{ shop.businessHours }} <el-tag :type="isOpen?'success':'danger'" size="small" effect="dark" style="margin-left:8px">{{ isOpen?'营业中':'非营业' }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="评分"><el-rate v-model="shop.rating" disabled :size="14" /> {{ shop.rating }}</el-descriptions-item>
            <el-descriptions-item label="简介" :span="2">{{ shop.description }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-card>
    <el-card style="margin-bottom:20px">
      <template #header><span style="font-weight:700">🔧 服务项目</span></template>
      <el-row :gutter="16">
        <el-col :span="8" v-for="svc in services" :key="svc.id" style="margin-bottom:16px">
          <el-card shadow="hover" class="service-card">
            <h4 style="margin:0 0 8px">{{ svc.serviceName }}</h4>
            <p style="color:#999;font-size:13px;margin:4px 0">{{ svc.description }}</p>
            <div style="display:flex;justify-content:space-between;align-items:center;margin-top:12px">
              <span style="color:#f5222d;font-weight:700;font-size:18px">¥{{ svc.price }}</span>
              <span style="color:#999;font-size:13px">{{ svc.duration }}分钟</span>
            </div>
            <el-button type="primary" size="small" style="width:100%;margin-top:12px" @click="openBook(svc)">立即预约</el-button>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
    <el-card>
      <template #header><span style="font-weight:700">👨‍🔧 技师团队</span></template>
      <el-row :gutter="16">
        <el-col :span="6" v-for="tech in sortedTechStats" :key="tech.id" style="margin-bottom:16px;display:flex">
          <el-popover placement="bottom" :width="280" trigger="hover" :show-after="200" style="width:100%">
            <template #reference>
              <el-card shadow="hover" class="tech-card" :style="tech.restStatus===1?'opacity:0.6':''">
                <div style="text-align:center;padding:8px 0;min-height:140px;display:flex;flex-direction:column;align-items:center;justify-content:center">
                  <el-avatar :size="56" :src="tech.avatar||defaultAvatar" />
                  <h4 style="margin:8px 0 4px">{{ tech.realName }}</h4>
                  <div style="margin:6px 0">
                    <el-tag v-for="sk in (tech.skill||'').split(/[,，、]/).filter(s=>s.trim())" :key="sk" type="warning" size="small" style="margin:2px">{{ sk.trim() }}</el-tag>
                  </div>
                  <el-tag :type="tech.restStatus===1?'danger':'success'" size="small" effect="dark" style="margin-top:6px">
                    {{ tech.restStatus===1?'今日休息':'今日在岗' }}
                  </el-tag>
                  <el-tag v-if="tomorrowRestIds.has(tech.id) && tech.restStatus!==1" type="warning" size="small" effect="dark" style="margin-top:4px">明日休息</el-tag>
                </div>
              </el-card>
            </template>
            <div class="tech-popover">
              <div class="tech-popover-header">
                <el-avatar :size="64" :src="tech.avatar||defaultAvatar" />
                <div class="tech-popover-info">
                  <h3 style="margin:0 0 4px">{{ tech.realName }}</h3>
                  <el-tag :type="tech.restStatus===1?'danger':'success'" size="small" effect="dark">{{ tech.restStatus===1?'今日休息':'今日在岗' }}</el-tag>
                  <el-tag v-if="tomorrowRestIds.has(tech.id) && tech.restStatus!==1" type="warning" size="small" effect="dark">明日休息</el-tag>
                </div>
              </div>
              <el-divider style="margin:12px 0" />
              <div class="tech-popover-detail">
                <div class="detail-row" v-if="tech.phone"><span class="detail-label">📞 电话</span><span>{{ tech.phone }}</span></div>
                <div class="detail-row"><span class="detail-label">🔧 专业技能</span><span>{{ tech.skill || '暂无' }}</span></div>
                <div class="detail-row"><span class="detail-label">⭐ 评分</span><span>{{ tech.avgRating || '暂无' }}<template v-if="tech.totalReviewCount"> ({{ tech.totalReviewCount }}评)</template></span></div>
                <div class="detail-row"><span class="detail-label">👍 好评</span><span style="color:#67c23a;font-weight:600">{{ tech.goodReviewCount || 0 }}次</span></div>
              </div>
              <el-divider style="margin:12px 0" />
              <div class="tech-popover-stats">
                <div class="stat-item">
                  <div class="stat-num" style="color:#67c23a">{{ tech.completedCount || 0 }}</div>
                  <div class="stat-label">已完成</div>
                </div>
                <div class="stat-item">
                  <div class="stat-num" style="color:#409eff">{{ tech.processingCount || 0 }}</div>
                  <div class="stat-label">维修中</div>
                </div>
                <div class="stat-item">
                  <div class="stat-num" style="color:#e6a23c">{{ tech.goodReviewCount || 0 }}</div>
                  <div class="stat-label">好评数</div>
                </div>
                <div class="stat-item">
                  <div class="stat-num" style="color:#909399">{{ tech.totalCount || 0 }}</div>
                  <div class="stat-label">总工单</div>
                </div>
              </div>
            </div>
          </el-popover>
        </el-col>
      </el-row>
    </el-card>
    <el-dialog v-model="showBook" title="在线预约" width="620px">
      <el-form :model="bookForm" label-width="110px">
        <el-form-item label="服务项目" required>
          <el-select v-model="bookForm.serviceId" placeholder="请选择服务项目" style="width:100%" @change="onServiceSelect">
            <el-option v-for="svc in services" :key="svc.id" :value="svc.id" :label="svc.serviceName + ' - ¥' + svc.price" />
          </el-select>
        </el-form-item>
        <el-form-item label="附加服务">
          <el-checkbox v-model="bookForm.towService" :true-value="1" :false-value="0">
            🚛 拖车接送服务
          </el-checkbox>
          <div style="color:#999;font-size:12px;margin-top:4px">车辆无法行驶？勾选后我们提供上门拖车接送服务，按距离合理收费</div>
        </el-form-item>
        <template v-if="bookForm.towService===1">
          <el-form-item label="拖车费用说明">
            <div style="display:flex;flex-wrap:wrap;gap:6px">
              <el-tag v-for="p in configStore.towPricing" :key="p.id" type="info" size="small">{{ p.maxDistance >= 999 ? (p.minDistance+'公里+') : (p.minDistance+'-'+p.maxDistance+'公里') }} ¥{{ p.price }}</el-tag>
            </div>
          </el-form-item>
          <el-form-item label="您的位置" required><el-input v-model="bookForm.towAddress" placeholder="请输入您的详细地址" /></el-form-item>
          <el-form-item label="距离(公里)" required>
            <el-input-number v-model="bookForm.towDistance" :min="1" :max="100" :step="1" style="width:200px" />
            <span style="margin-left:12px;color:#999">预估距离</span>
          </el-form-item>
          <el-form-item label="拖车费用">
            <span style="color:#f5222d;font-weight:700;font-size:20px">¥{{ towPrice }}</span>
          </el-form-item>
        </template>
        <el-form-item label="预约时间" required>
          <div style="display:flex;gap:8px;width:100%">
            <el-date-picker v-model="bookForm.appointmentDate" type="date" placeholder="选择日期" style="flex:1" :disabled-date="disableDate" @change="onDateChange" />
            <el-time-select v-model="bookForm.appointmentTimeStr" :start="effectiveStartTime" :end="businessCloseTimeMinusOne" step="00:01" placeholder="选择时间" style="flex:1" :disabled="isTimeDisabled" @change="onTimeSelectChange" />
          </div>
          <div v-if="businessHoursTip" style="color:#f5222d;font-size:12px;margin-top:4px">{{ businessHoursTip }}</div>
        </el-form-item>
        <el-form-item label="指定技师">
          <el-select v-model="bookForm.employeeId" placeholder="不指定则系统自动分配" clearable style="width:100%" @change="onTechChange">
            <el-option v-for="t in recommendedTechs" :key="t.id" :value="t.id" :disabled="t.hasConflict || tomorrowRestIds.has(t.id)" :label="t.name + (t.skill?' ['+t.skill+']':'')">
              <div style="display:flex;justify-content:space-between;align-items:center;width:100%">
                <div><span style="font-weight:600">{{ t.name }}</span><span v-if="t.skill" style="color:#999;font-size:12px;margin-left:6px">{{ t.skill }}</span></div>
                <div style="display:flex;gap:4px">
                  <el-tag v-if="tomorrowRestIds.has(t.id)" type="warning" size="small">明日休息</el-tag>
                  <el-tag v-if="t.skillMatch" type="success" size="small">专业匹配</el-tag>
                  <el-tag v-if="t.hasConflict" type="danger" size="small">时间冲突</el-tag>
                  <el-tag v-if="!t.hasConflict && !tomorrowRestIds.has(t.id)" type="info" size="small">{{ t.currentOrders }}单</el-tag>
                </div>
              </div>
            </el-option>
          </el-select>
          <div v-if="techConflictTip" style="color:#f5222d;font-size:12px;margin-top:4px">{{ techConflictTip }}</div>
        </el-form-item>
        <el-form-item label="摩托车型号"><el-input v-model="bookForm.motorcycleModel" placeholder="如：本田CB400" /></el-form-item>
        <el-form-item label="问题描述"><el-input v-model="bookForm.problemDescription" type="textarea" rows="3" placeholder="请描述您的摩托车故障或需求" /></el-form-item>
        <el-form-item label="费用合计">
          <span style="font-size:20px;font-weight:700;color:#f5222d">¥{{ totalAmount }}</span>
          <span v-if="bookForm.towService===1" style="color:#999;font-size:13px;margin-left:8px">(维修¥{{ servicePrice }} + 拖车¥{{ towPrice }})</span>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="bookForm.remark" placeholder="其他备注信息" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showBook=false">取消</el-button><el-button type="primary" @click="handleBook" :loading="booking">提交预约</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { shopAPI, serviceAPI, appointmentAPI } from '../../api'
import { useUserStore } from '../../store/user'
import { useConfigStore } from '../../store/config'
const userStore = useUserStore(), configStore = useConfigStore()
const defaultCover = 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png'
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const shop = ref({}), services = ref([]), technicians = ref([])
const techStats = ref([])
const tomorrowRestIds = ref(new Set())
const showBook = ref(false), currentService = ref(null)
const recommendedTechs = ref([]), booking = ref(false)
const businessHoursTip = ref(''), techConflictTip = ref('')
const bookForm = reactive({ userId:null, shopId:null, serviceId:null, employeeId:null, motorcycleModel:'', problemDescription:'', appointmentDate:null, appointmentTimeStr:null, remark:'', towService:0, towAddress:'', towDistance:5 })
const businessOpenTime = computed(() => {
  const bh = shop.value.businessHours
  if(!bh) return '08:00'
  const match = bh.match(/(\d{1,2}:\d{2})\s*[-~到至]\s*(\d{1,2}:\d{2})/)
  return match ? match[1] : '08:00'
})
const businessCloseTime = computed(() => {
  const bh = shop.value.businessHours
  if(!bh) return '20:00'
  const match = bh.match(/(\d{1,2}:\d{2})\s*[-~到至]\s*(\d{1,2}:\d{2})/)
  return match ? match[2] : '20:00'
})
const businessCloseTimeMinusOne = computed(() => {
  const ct = businessCloseTime.value
  const [h, m] = ct.split(':').map(Number)
  const totalMin = h * 60 + m - 1
  const nh = Math.floor(totalMin / 60), nm = totalMin % 60
  return String(nh).padStart(2, '0') + ':' + String(nm).padStart(2, '0')
})
const effectiveStartTime = computed(() => {
  if (!bookForm.appointmentDate) return businessOpenTime.value
  const selDate = new Date(bookForm.appointmentDate)
  const today = new Date()
  const isToday = selDate.getFullYear() === today.getFullYear() && selDate.getMonth() === today.getMonth() && selDate.getDate() === today.getDate()
  if (!isToday) return businessOpenTime.value
  const nowMin = today.getHours() * 60 + today.getMinutes() + 30
  const [oh, om] = businessOpenTime.value.split(':').map(Number)
  const openMin = oh * 60 + om
  const startMin = Math.max(nowMin, openMin)
  const [ch, cm] = businessCloseTimeMinusOne.value.split(':').map(Number)
  const closeMin = ch * 60 + cm
  if (startMin >= closeMin) return businessCloseTimeMinusOne.value
  const sh = Math.floor(startMin / 60), sm = startMin % 60
  return String(sh).padStart(2, '0') + ':' + String(sm).padStart(2, '0')
})
const sortedTechStats = computed(() => {
  return [...techStats.value].sort((a, b) => (a.restStatus === 1 ? 1 : 0) - (b.restStatus === 1 ? 1 : 0))
})
const towPrice = computed(() => configStore.towPrice(bookForm.towDistance||0))
const servicePrice = computed(() => {
  if(!bookForm.serviceId) return 0
  const svc = services.value.find(s => s.id === bookForm.serviceId)
  return svc ? svc.price : 0
})
const totalAmount = computed(() => {
  let total = servicePrice.value
  if(bookForm.towService === 1) total += towPrice.value
  return total
})
const isOpen = computed(() => {
  const bh = shop.value.businessHours
  if(!bh) return true
  const match = bh.match(/(\d{1,2}):?(\d{0,2})\s*[-~到至]\s*(\d{1,2}):?(\d{0,2})/)
  if(!match) return true
  const now = new Date(), nowMin = now.getHours()*60+now.getMinutes()
  const openMin = parseInt(match[1])*60+parseInt(match[2]||'0'), closeMin = parseInt(match[3])*60+parseInt(match[4]||'0')
  return nowMin >= openMin && nowMin < closeMin
})
const isTodayPastBusinessHours = computed(() => {
  if (isOpen.value) return false
  const now = new Date()
  const [ch, cm] = businessCloseTime.value.split(':').map(Number)
  const closeMin = ch * 60 + cm
  const nowMin = now.getHours() * 60 + now.getMinutes()
  return nowMin >= closeMin
})
const isTimeDisabled = computed(() => {
  if (!bookForm.appointmentDate) return true
  const selDate = new Date(bookForm.appointmentDate)
  const today = new Date()
  const isToday = selDate.getFullYear() === today.getFullYear() && selDate.getMonth() === today.getMonth() && selDate.getDate() === today.getDate()
  return isToday && isTodayPastBusinessHours.value
})
const disableDate = d => {
  const now = new Date()
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  if (d < todayStart) return true
  const monthEnd = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999)
  if (d > monthEnd) return true
  if (isTodayPastBusinessHours.value) {
    const todayEnd = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59, 999)
    if (d <= todayEnd) return true
  }
  return false
}
const fetchShop = async () => {
  try {
    const r = await shopAPI.getApprovedShops()
    if(r.code===200 && r.data.length) {
      shop.value = r.data[0]
      const [sr, tr, ts] = await Promise.all([serviceAPI.getByShop(shop.value.id), shopAPI.getTechnicians(shop.value.id), shopAPI.getTechnicianStats(shop.value.id)])
      if(sr.code===200) services.value = sr.data
      if(tr.code===200) technicians.value = tr.data
      if(ts.code===200) techStats.value = ts.data
      try {
        const restR = await shopAPI.getTodayRestTechIds(shop.value.id)
        if(restR.code===200 && restR.data) {
          const restIds = new Set(restR.data)
          techStats.value.forEach(t => { if(restIds.has(t.id)) t.restStatus = 1 })
        }
      } catch{}
      try {
        const tmrR = await shopAPI.getTomorrowRestTechs(shop.value.id)
        if(tmrR.code===200 && tmrR.data) {
          tomorrowRestIds.value = new Set(tmrR.data.map(t => t.userId))
        }
      } catch{}
    }
  } catch{}
}
const openBook = async svc => {
  currentService.value = svc
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  let initDate = now
  let initTime = pad(now.getHours()) + ':' + pad(now.getMinutes())
  if (isTodayPastBusinessHours.value) {
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    initDate = tomorrow
    initTime = businessOpenTime.value
    businessHoursTip.value = '今日营业时间已结束，已自动选择明天，请选择预约时间'
  } else {
    businessHoursTip.value = ''
  }
  bookForm.shopId=shop.value.id; bookForm.serviceId=svc.id; bookForm.userId=userStore.userId; bookForm.employeeId=null; bookForm.appointmentDate=initDate; bookForm.appointmentTimeStr=initTime; bookForm.towService=0; bookForm.towAddress=''; bookForm.towDistance=5; bookForm.motorcycleModel=''; bookForm.problemDescription=''; bookForm.remark=''
  techConflictTip.value=''
  try { const r=await appointmentAPI.recommendTech(shop.value.id, svc.id, null); if(r.code===200) recommendedTechs.value=r.data } catch{ recommendedTechs.value=[] }
  showBook.value=true
}
const onServiceSelect = async (serviceId) => {
  const svc = services.value.find(s => s.id === serviceId)
  if(svc) currentService.value = svc
  bookForm.employeeId = null
  techConflictTip.value = ''
  if(serviceId && bookForm.appointmentTimeStr) {
    try {
      const dt = combineDateTime(bookForm.appointmentDate, bookForm.appointmentTimeStr)
      const timeStr = formatDateTime(dt)
      const r = await appointmentAPI.recommendTech(shop.value.id, serviceId, timeStr)
      if(r.code===200) recommendedTechs.value = r.data
    } catch{}
  }
}
const onDateChange = () => { businessHoursTip.value=''; techConflictTip.value='' }
const onTimeSelectChange = async () => {
  businessHoursTip.value=''; techConflictTip.value=''
  if(!bookForm.appointmentDate || !bookForm.appointmentTimeStr) return
  const dt = combineDateTime(bookForm.appointmentDate, bookForm.appointmentTimeStr)
  validateBusinessHours(dt)
  if(bookForm.employeeId) await checkTechConflict()
  if(bookForm.serviceId) {
    try { const timeStr=formatDateTime(dt); const r=await appointmentAPI.recommendTech(shop.value.id, bookForm.serviceId, timeStr); if(r.code===200) recommendedTechs.value=r.data } catch{}
  }
}
const combineDateTime = (date, timeStr) => {
  const d = new Date(date)
  const [h, m] = timeStr.split(':').map(Number)
  d.setHours(h, m, 0, 0)
  return d
}
const onTechChange = async () => { techConflictTip.value=''; if(bookForm.employeeId&&bookForm.appointmentDate&&bookForm.appointmentTimeStr) await checkTechConflict() }
const checkTechConflict = async () => {
  if(!bookForm.employeeId||!bookForm.appointmentDate||!bookForm.appointmentTimeStr) return
  try { const dt=combineDateTime(bookForm.appointmentDate, bookForm.appointmentTimeStr); const timeStr=formatDateTime(dt); const r=await appointmentAPI.checkConflict(shop.value.id, bookForm.employeeId, timeStr, bookForm.serviceId); if(r.code===200&&r.data.hasConflict) techConflictTip.value=r.data.message; else techConflictTip.value='' } catch{}
}
const validateBusinessHours = dt => {
  const bh = shop.value.businessHours
  if(!bh) return
  const time = new Date(dt), hours=time.getHours(), minutes=time.getMinutes()
  const match = bh.match(/(\d{1,2}):?(\d{0,2})\s*[-~到至]\s*(\d{1,2}):?(\d{0,2})/)
  if(match) {
    const openMin=parseInt(match[1])*60+parseInt(match[2]||'0'), closeMin=parseInt(match[3])*60+parseInt(match[4]||'0'), aptMin=hours*60+minutes
    if(aptMin<openMin||aptMin>=closeMin) businessHoursTip.value=`预约时间不在营业时间(${bh})内`
  }
}
const formatDateTime = dt => { const d=new Date(dt), pad=n=>String(n).padStart(2,'0'); return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}` }
const handleBook = async () => {
  if(!bookForm.serviceId) return ElMessage.warning('请选择服务项目')
  if(bookForm.towService===1 && !bookForm.towAddress) return ElMessage.warning('请填写您的位置')
  if(!bookForm.appointmentDate||!bookForm.appointmentTimeStr) return ElMessage.warning('请选择预约时间')
  if(businessHoursTip.value) return ElMessage.warning(businessHoursTip.value)
  if(techConflictTip.value) return ElMessage.warning(techConflictTip.value)
  booking.value=true
  try {
    const dt=combineDateTime(bookForm.appointmentDate, bookForm.appointmentTimeStr)
    const data = {
      userId: bookForm.userId,
      shopId: bookForm.shopId,
      serviceId: bookForm.serviceId,
      employeeId: bookForm.employeeId,
      motorcycleModel: bookForm.motorcycleModel,
      problemDescription: bookForm.problemDescription,
      appointmentTime: formatDateTime(dt),
      remark: bookForm.remark,
      totalAmount: totalAmount.value,
      towService: bookForm.towService,
      towAddress: bookForm.towService===1 ? bookForm.towAddress : null,
      towDistance: bookForm.towService===1 ? bookForm.towDistance : null,
      towFee: bookForm.towService===1 ? towPrice.value : null
    }
    const r=await appointmentAPI.create(data)
    if(r.code===200) {
      let msg = '预约成功！'
      if(bookForm.towService===1) msg = '预约成功！含拖车接送服务，费用¥' + totalAmount.value
      ElMessage.success(msg); showBook.value=false
    }
    else ElMessage.error(r.message||'预约失败')
  } catch(e) {
    const msg = e.response?.data?.message || e.message || '预约失败，请稍后重试'
    ElMessage.error(msg)
  }
  finally { booking.value=false }
}
onMounted(fetchShop)
</script>
<style scoped>
.service-card{transition:all .2s}
.service-card:hover{transform:translateY(-3px);box-shadow:0 6px 16px rgba(0,0,0,.1)}
.tech-card{transition:all .2s;cursor:pointer;flex:1;height:100%}
.tech-card:hover{transform:translateY(-3px);box-shadow:0 6px 16px rgba(0,0,0,.1)}
.tech-popover-header{display:flex;align-items:center;gap:16px}
.tech-popover-info h3{font-size:18px}
.tech-popover-detail{display:flex;flex-direction:column;gap:8px}
.detail-row{display:flex;justify-content:space-between;align-items:center;font-size:14px;color:#333}
.detail-label{color:#666;font-size:13px}
.tech-popover-stats{display:flex;justify-content:space-around;text-align:center}
.stat-item{flex:1}
.stat-num{font-size:22px;font-weight:700;line-height:1.2}
.stat-label{font-size:12px;color:#999;margin-top:4px}
</style>
