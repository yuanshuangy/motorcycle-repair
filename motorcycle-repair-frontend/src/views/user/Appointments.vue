<template>
  <div>
    <div class="page-header"><h2>我的预约</h2></div>
    <el-card>
      <div style="margin-bottom:16px"><el-radio-group v-model="statusFilter" @change="onStatusFilterChange"><el-radio-button :value="null">全部</el-radio-button><el-radio-button :value="0">待确认</el-radio-button><el-radio-button :value="1">已确认</el-radio-button><el-radio-button :value="2">维修中</el-radio-button><el-radio-button :value="3">已完成</el-radio-button><el-radio-button :value="4">已取消</el-radio-button><el-radio-button :value="5">爽约</el-radio-button></el-radio-group></div>
      <el-table :data="list" stripe>
        <el-table-column label="订单号" width="180">
          <template #default="{row}">
            <el-link type="primary" @click="showOrderDetail(row)">{{ row.orderNo }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="shopName" label="维修店" />
        <el-table-column label="服务" min-width="140">
          <template #default="{row}">
            {{ row.serviceName }}
            <el-tag v-if="row.towService===1" type="warning" size="small" effect="dark" style="margin-left:4px">🚛拖车</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="motorcycleModel" label="车型" />
        <el-table-column prop="appointmentTime" label="预约时间" width="170" />
        <el-table-column prop="statusName" label="状态" width="100">
          <template #default="{row}"><el-tag :type="statusType(row.status)" size="small">{{ row.statusName }}</el-tag></template>
        </el-table-column>
        <el-table-column label="维修师傅" width="120">
          <template #default="{row}">
            <span v-if="row.employeeName">{{ row.employeeName }}</span>
            <span v-else style="color:#999">待分配</span>
          </template>
        </el-table-column>
        <el-table-column label="预计等待" width="120" v-if="hasPendingOrders">
          <template #default="{row}">
            <template v-if="!row.employeeId && row.estimatedWaitMinutes">
              <el-tag type="warning" size="small" effect="dark">约{{ row.estimatedWaitMinutes }}分钟</el-tag>
            </template>
            <template v-else-if="row.waitDiscountRate && row.waitDiscountRate < 1">
              <el-tag type="danger" size="small" effect="dark">享{{ Math.round(row.waitDiscountRate * 10) }}折</el-tag>
            </template>
            <span v-else style="color:#999">-</span>
          </template>
        </el-table-column>
        <el-table-column label="完成倒计时" width="150" v-if="hasActiveOrders">
          <template #default="{row}">
            <template v-if="(row.status===1 || row.status===2) && row.estimatedCompleteTime">
              <div :style="{color: getCountdownColor(row.id), fontWeight: 700, fontSize: '14px'}">
                ⏱ {{ getCountdownText(row.id) }}
              </div>
              <div style="color:#999;font-size:11px">预计{{ formatTime(row.estimatedCompleteTime) }}</div>
            </template>
            <span v-else style="color:#999">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{row}">
            <el-button v-if="row.employeeName" size="small" type="info" @click="showTechInfo(row)">师傅详情</el-button>
            <el-button v-if="row.status===0||row.status===1" size="small" type="danger" @click="cancel(row)">取消</el-button>
            <el-button v-if="row.status===3 && !row.payStatus" size="small" type="success" @click="openPay(row)">支付</el-button>
            <el-tag v-if="row.payStatus===1" type="success" size="small">已支付</el-tag>
            <el-button v-if="row.status===3 && row.payStatus===1 && !row.hasReview" size="small" type="primary" @click="openReview(row)">评价</el-button>
            <el-tag v-if="row.status===3 && row.payStatus===1 && row.hasReview" type="info" size="small">已评价</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>
    <el-dialog v-model="showReview" title="评价服务" width="550px">
      <el-form label-width="100px">
        <el-form-item label="店铺评分"><el-rate v-model="reviewForm.shopRating" show-text :texts="rateTexts" /></el-form-item>
        <el-form-item v-if="reviewForm.technicianName" :label="'技师('+reviewForm.technicianName+')评分'"><el-rate v-model="reviewForm.techRating" show-text :texts="rateTexts" /></el-form-item>
        <el-form-item label="好评标签" v-if="reviewForm.shopRating>=4 || reviewForm.techRating>=4">
          <div style="display:flex;flex-wrap:wrap;gap:6px">
            <el-check-tag v-for="tag in goodTags" :key="tag" :checked="reviewForm.selectedTags.includes(tag)" @change="toggleTag(tag)" style="cursor:pointer">{{ tag }}</el-check-tag>
          </div>
        </el-form-item>
        <el-form-item label="评价内容"><el-input v-model="reviewForm.content" type="textarea" rows="4" placeholder="请分享您的服务体验..." /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showReview=false">取消</el-button><el-button type="primary" @click="submitReview" :loading="submittingReview">提交评价</el-button></template>
    </el-dialog>
    <el-dialog v-model="showTech" title="师傅信息" width="450px">
      <div v-if="currentTech" class="tech-info-card">
        <div class="tech-header">
          <el-avatar :size="72" :src="currentTech.employeeAvatar||defaultAvatar" />
          <div class="tech-basic">
            <h3>{{ currentTech.employeeName }}</h3>
            <el-tag :type="currentTech.employeeRole==='技师'?'':'info'" size="small" effect="dark">{{ currentTech.employeeRole }}</el-tag>
          </div>
        </div>
        <el-divider />
        <div class="tech-detail">
          <div class="detail-item" v-if="currentTech.employeePhone"><el-icon><Phone /></el-icon><span>联系电话：{{ currentTech.employeePhone }}</span></div>
          <div class="detail-item" v-if="currentTech.employeeSkill"><el-icon><Tools /></el-icon><span>专业技能：{{ currentTech.employeeSkill }}</span></div>
          <div class="detail-item"><el-icon><Shop /></el-icon><span>所属店铺：{{ currentTech.shopName }}</span></div>
          <div class="detail-item"><el-icon><Calendar /></el-icon><span>预约时间：{{ currentTech.appointmentTime }}</span></div>
          <div class="detail-item" v-if="currentTech.serviceName"><el-icon><SetUp /></el-icon><span>服务项目：{{ currentTech.serviceName }}</span></div>
        </div>
      </div>
    </el-dialog>
    <el-dialog v-model="showPay" title="支付费用" width="450px">
      <div v-if="payOrder" style="text-align:center">
        <p style="font-size:16px;margin-bottom:16px">订单号：{{ payOrder.orderNo }}</p>
        <p style="font-size:32px;font-weight:700;color:#f5222d;margin-bottom:24px">¥{{ payOrder.totalAmount||0 }}</p>
        <div style="display:flex;gap:16px;justify-content:center">
          <el-button size="large" style="width:160px;background:#1677ff;color:#fff;border:none" @click="doPay('alipay')">
            <el-icon size="24"><CreditCard /></el-icon><div>支付宝支付</div>
          </el-button>
          <el-button size="large" style="width:160px;background:#07c160;color:#fff;border:none" @click="doPay('wechat')">
            <el-icon size="24"><CreditCard /></el-icon><div>微信支付</div>
          </el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="showOrderDetailDialog" title="订单详情" width="650px">
      <div v-if="currentOrderDetail" class="order-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号" :span="2">{{ currentOrderDetail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="维修店">{{ currentOrderDetail.shopName }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="statusType(currentOrderDetail.status)" size="small">{{ currentOrderDetail.statusName }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="服务">{{ currentOrderDetail.serviceName || '自定义服务' }}</el-descriptions-item>
          <el-descriptions-item label="车型">{{ currentOrderDetail.motorcycleModel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预约时间">{{ currentOrderDetail.appointmentTime }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ currentOrderDetail.totalAmount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="维修师傅">{{ currentOrderDetail.employeeName || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentOrderDetail.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完单时间">{{ currentOrderDetail.completeTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预计完成" v-if="currentOrderDetail.estimatedCompleteTime && (currentOrderDetail.status===1 || currentOrderDetail.status===2)">
            <span style="color:#e6a23c;font-weight:700">{{ currentOrderDetail.estimatedCompleteTime }}</span>
            <el-tag type="warning" size="small" style="margin-left:6px">⏱ {{ getCountdownText(currentOrderDetail.id) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="currentOrderDetail.problemDescription" style="margin-top:12px">
          <strong>问题描述：</strong>
          <p style="color:#666;margin:4px 0 0">{{ currentOrderDetail.problemDescription }}</p>
        </div>
        <div v-if="currentOrderDetail.towService===1" style="margin-top:12px;padding:12px;background:#fdf6ec;border-radius:8px;border:1px solid #faecd8">
          <h4 style="margin:0 0 8px;color:#e6a23c">🚛 拖车服务信息</h4>
          <p style="color:#666;font-size:13px">取车地址：{{ currentOrderDetail.towAddress || '-' }} | 距离：{{ currentOrderDetail.towDistance ? currentOrderDetail.towDistance + '公里' : '-' }} | 拖车费：¥{{ currentOrderDetail.towFee || 0 }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, h } from 'vue'
import { ElMessage, ElMessageBox, ElCheckTag, ElInput } from 'element-plus'
import { Phone, Tools, Shop, Calendar, SetUp, CreditCard } from '@element-plus/icons-vue'
import { appointmentAPI, reviewAPI } from '../../api'
import { useUserStore } from '../../store/user'
import { useConfigStore } from '../../store/config'
const userStore = useUserStore(), configStore = useConfigStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const list = ref([]), statusFilter = ref(null), showReview = ref(false), showTech = ref(false), showPay = ref(false), currentTech = ref(null), payOrder = ref(null), showOrderDetailDialog = ref(false), currentOrderDetail = ref(null)
const pageNum = ref(1), pageSize = ref(10), total = ref(0)
const cancelReason = ref('')
const reviewForm = reactive({ appointmentId:null, userId:null, shopId:null, technicianId:null, technicianName:'', shopRating:5, techRating:5, content:'', selectedTags:[] })
const submittingReview = ref(false)
const goodTags = computed(() => configStore.reviewTags)
const rateTexts = computed(() => configStore.rateTexts)
const toggleTag = tag => {
  const idx = reviewForm.selectedTags.indexOf(tag)
  if (idx >= 0) reviewForm.selectedTags.splice(idx, 1)
  else reviewForm.selectedTags.push(tag)
}
const statusType = s => configStore.statusCss(s)
const hasPendingOrders = computed(() => list.value.some(r => !r.employeeId && r.estimatedWaitMinutes))
const onStatusFilterChange = () => { pageNum.value = 1; fetchData() }
const hasActiveOrders = computed(() => list.value.some(r => (r.status === 1 || r.status === 2) && r.estimatedCompleteTime))
const countdownMap = ref({})
let countdownTimer = null
const updateCountdowns = () => {
  const now = new Date()
  const newMap = {}
  list.value.forEach(r => {
    if ((r.status === 1 || r.status === 2) && r.estimatedCompleteTime) {
      const est = new Date(r.estimatedCompleteTime.replace(/-/g, '/'))
      const diff = est.getTime() - now.getTime()
      if (diff > 0) {
        const totalMin = Math.floor(diff / 60000)
        const hours = Math.floor(totalMin / 60)
        const mins = totalMin % 60
        const secs = Math.floor((diff % 60000) / 1000)
        newMap[r.id] = {
          text: hours > 0 ? `${hours}时${mins}分${secs}秒` : `${mins}分${secs}秒`,
          totalMinutes: totalMin,
          overtime: false
        }
      } else {
        const overMin = Math.floor(-diff / 60000)
        newMap[r.id] = {
          text: `已超时${overMin}分钟`,
          totalMinutes: 0,
          overtime: true
        }
      }
    }
  })
  countdownMap.value = newMap
}
const getCountdownText = (id) => {
  const c = countdownMap.value[id]
  return c ? c.text : '计算中...'
}
const getCountdownColor = (id) => {
  const c = countdownMap.value[id]
  if (!c) return '#409eff'
  if (c.overtime) return '#f56c6c'
  if (c.totalMinutes <= 10) return '#e6a23c'
  return '#409eff'
}
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return timeStr.substring(11, 16)
}
const startCountdown = () => {
  stopCountdown()
  updateCountdowns()
  countdownTimer = setInterval(updateCountdowns, 1000)
}
const stopCountdown = () => {
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
}
const fetchData = async () => { try { const r=await appointmentAPI.getPage({pageNum:pageNum.value,pageSize:pageSize.value,status:statusFilter.value}); if(r.code===200) { list.value=r.data.records; total.value=r.data.total; startCountdown() } } catch(e){} }
const cancel = async row => {
  if (row.status !== 0 && row.status !== 1) {
    return ElMessage.warning('只有待确认或已确认的订单才能取消')
  }
  const presetReasons = ['临时有事无法前往', '时间安排冲突', '已找到其他维修店', '不需要维修了', '价格不合适']
  try {
    const { value } = await ElMessageBox({
      title: '取消预约',
      message: h('div', null, [
        h('p', { style: 'margin-bottom:12px' }, `确认取消预约「${row.orderNo}」？`),
        row.status === 1 ? h('p', { style: 'color:#e6a23c;margin-bottom:12px;font-size:13px' }, '⚠️ 店铺已确认您的预约，取消后需重新预约') : null,
        h('div', {
          style: 'background:linear-gradient(135deg,#fff3e0,#ffe0b2);border:2px solid #ff9800;border-radius:8px;padding:12px 16px;margin-bottom:12px;animation:pulse-border 1.5s infinite'
        }, [
          h('p', { style: 'font-weight:700;margin:0 0 8px;color:#e65100;font-size:15px' }, '⚠️ 请选择取消原因：'),
          h('div', { style: 'display:flex;flex-wrap:wrap;gap:8px' },
            presetReasons.map(r => h(ElCheckTag, {
              checked: cancelReason.value === r,
              onChange: () => { cancelReason.value = cancelReason.value === r ? '' : r },
              style: cancelReason.value === r ? 'border:2px solid #ff5722;box-shadow:0 0 8px rgba(255,87,34,0.3)' : ''
            }, () => r))
          )
        ]),
        h('p', { style: 'font-weight:600;margin-bottom:8px' }, '或自定义原因：'),
        h(ElInput, {
          modelValue: cancelReason.value && !presetReasons.includes(cancelReason.value) ? cancelReason.value : '',
          'onUpdate:modelValue': v => { if (v) cancelReason.value = v },
          placeholder: '请输入取消原因（选填）',
          size: 'default'
        })
      ]),
      showCancelButton: true,
      confirmButtonText: '确认取消',
      cancelButtonText: '再想想',
      type: 'warning',
      beforeClose: (action, instance, done) => {
        if (action === 'confirm' && row.status === 1 && !cancelReason.value) {
          ElMessage.warning('已确认的订单取消时请填写取消原因')
          return
        }
        done()
      }
    })
    await appointmentAPI.cancel(row.id, cancelReason.value)
    ElMessage.success('已取消')
    cancelReason.value = ''
    fetchData()
  } catch { cancelReason.value = '' }
}
const showTechInfo = row => { currentTech.value = row; showTech.value = true }
const showOrderDetail = row => { currentOrderDetail.value = row; showOrderDetailDialog.value = true }
const openPay = row => { payOrder.value = row; showPay.value = true }
const doPay = async method => { try { await appointmentAPI.pay(payOrder.value.id, method); ElMessage.success('支付成功'); showPay.value=false; fetchData() } catch{ ElMessage.error('支付失败') } }
const openReview = row => {
  reviewForm.appointmentId=row.id; reviewForm.userId=userStore.userId; reviewForm.shopId=row.shopId; reviewForm.shopRating=5; reviewForm.techRating=5; reviewForm.content=''; reviewForm.selectedTags=[]
  reviewForm.technicianId=row.employeeId||null; reviewForm.technicianName=row.employeeName||''
  showReview.value=true
}
const submitReview = async () => {
  if(!reviewForm.shopRating) return ElMessage.warning('请给店铺评分')
  submittingReview.value = true
  try {
    let content = reviewForm.content
    if(reviewForm.selectedTags.length > 0) content = '[' + reviewForm.selectedTags.join('、') + '] ' + content
    await reviewAPI.add({ appointmentId:reviewForm.appointmentId, userId:reviewForm.userId, shopId:reviewForm.shopId, rating:reviewForm.shopRating, content, technicianId:reviewForm.technicianId })
    if(reviewForm.technicianId && reviewForm.techRating) {
      await reviewAPI.add({ appointmentId:reviewForm.appointmentId, userId:reviewForm.userId, shopId:reviewForm.shopId, rating:reviewForm.techRating, content:'技师评价：' + content, technicianId:reviewForm.technicianId })
    }
    ElMessage.success('评价成功'); showReview.value=false; fetchData()
  } catch(e) { ElMessage.error('评价失败') }
  finally { submittingReview.value = false }
}
onMounted(fetchData)
onBeforeUnmount(stopCountdown)
</script>
<style scoped>
.tech-info-card{padding:8px}
.tech-header{display:flex;align-items:center;gap:20px}
.tech-basic h3{margin:0 0 6px;font-size:20px}
.tech-detail{display:flex;flex-direction:column;gap:14px}
.detail-item{display:flex;align-items:center;gap:10px;font-size:15px;color:#333}
.detail-item .el-icon{color:var(--el-color-primary);font-size:18px}
</style>
<style>
@keyframes pulse-border{0%,100%{border-color:#ff9800;box-shadow:0 0 4px rgba(255,152,0,0.3)}50%{border-color:#ff5722;box-shadow:0 0 12px rgba(255,87,34,0.5)}}
</style>
