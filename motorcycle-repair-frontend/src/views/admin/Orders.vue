<template>
  <div>
    <div class="page-header"><h2>订单管理</h2></div>
    <el-card>
      <div style="margin-bottom:16px;display:flex;gap:12px;align-items:center;flex-wrap:wrap">
        <el-radio-group v-model="statusFilter" @change="fetchData">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button :value="0">待确认</el-radio-button>
          <el-radio-button :value="1">已确认</el-radio-button>
          <el-radio-button :value="2">维修中</el-radio-button>
          <el-radio-button :value="3">已完成</el-radio-button>
          <el-radio-button :value="4">已取消</el-radio-button>
          <el-radio-button :value="5">爽约</el-radio-button>
        </el-radio-group>
        <el-checkbox v-model="showTowOnly" @change="applyFilter">只看拖车订单</el-checkbox>
      </div>
      <el-table :data="displayList" stripe>
        <el-table-column label="订单号" width="180">
          <template #default="{row}">
            <el-link type="primary" @click="showOrderDetail(row)">{{ row.orderNo }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="shopName" label="维修店" width="160" />
        <el-table-column prop="userName" label="客户" width="90" />
        <el-table-column label="服务" min-width="140">
          <template #default="{row}">
            {{ row.serviceName || '自定义服务' }}
            <el-tag v-if="row.towService===1" type="warning" size="small" effect="dark" style="margin-left:4px">🚛拖车</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="motorcycleModel" label="车型" width="100" />
        <el-table-column prop="appointmentTime" label="预约时间" width="170" />
        <el-table-column label="维修技师" width="90">
          <template #default="{row}">
            <span v-if="row.employeeName">{{ row.employeeName }}</span>
            <span v-else style="color:#999">未派单</span>
          </template>
        </el-table-column>
        <el-table-column label="拖车司机" width="90">
          <template #default="{row}">
            <span v-if="row.driverName">{{ row.driverName }}</span>
            <span v-else style="color:#ccc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="接车状态" width="90">
          <template #default="{row}">
            <template v-if="row.towService===1">
              <el-tag :type="pickupTagType(row.pickupStatus)" size="small">{{ row.pickupStatusName }}</el-tag>
            </template>
            <span v-else style="color:#ccc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{row}">
            <el-tag :type="st(row.status)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="完单/更新时间" width="170">
          <template #default="{row}">
            <div v-if="row.completeTime" style="color:#67c23a;font-size:12px">完单: {{ row.completeTime }}</div>
            <div v-else-if="(row.status===1 || row.status===2) && row.estimatedCompleteTime" style="font-size:12px">
              <div :style="{color: getCountdownColor(row), fontWeight: 700}">⏱ {{ getCountdownText(row) }}</div>
              <div style="color:#999">预计{{ row.estimatedCompleteTime.substring(11,16) }}</div>
            </div>
            <div v-else-if="row.updateTime" style="color:#999;font-size:12px">更新: {{ row.updateTime }}</div>
            <span v-else style="color:#ccc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="info" @click="showUserInfo(row)">客户信息</el-button>
            <el-button v-if="row.status===0" size="small" type="success" @click="changeStatus(row.id,1)">确认</el-button>
            <el-button v-if="row.status===1" size="small" type="warning" @click="openAssign(row)">派单</el-button>
            <el-button v-if="row.status===2" size="small" type="success" @click="completeOrder(row)">完成</el-button>
            <el-button v-if="row.status===0" size="small" type="danger" @click="changeStatus(row.id,4)">拒绝</el-button>
            <el-button v-if="row.status===0||row.status===1" size="small" type="warning" @click="noshowOrder(row)">爽约</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="pageNum" :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>
    <el-dialog v-model="showAssignDialog" title="派单分配" width="600px">
      <el-alert v-if="assignRow && assignRow.towService===1" type="warning" :closable="false" style="margin-bottom:12px">
        <template #title>🚛 此订单包含拖车服务，系统已自动分配司机「{{ assignRow.driverName || '待分配' }}」负责接车送店</template>
      </el-alert>
      <p style="margin-bottom:12px">请选择维修技师：</p>
      <el-table :data="technicians" stripe @row-click="selectAssignTarget($event,'tech')" highlight-current-row>
        <el-table-column label="头像" width="60"><template #default="{row}"><el-avatar :size="32" :src="row.avatar||defaultAvatar" /></template></el-table-column>
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="skill" label="技能" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{row}">
            <el-tag v-if="isDedicatedDriver(row.skill)" type="warning" size="small">专职司机</el-tag>
            <el-tag v-else size="small">维修技师</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" width="100">
          <template #default="{row}">
            <el-tag v-if="tomorrowRestIds.includes(row.id)" type="danger" size="small" effect="dark">明日休息</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="technicians.length === 0" style="color:#f56c6c;text-align:center;padding:20px">
        今日所有技师都已休息，无法派单
      </div>
      <template #footer><el-button @click="showAssignDialog=false">取消</el-button><el-button type="primary" @click="doAssign" :disabled="!assignTargetId">确定派单</el-button></template>
    </el-dialog>
    <el-dialog v-model="showUserInfoDialog" title="客户信息" width="450px">
      <div v-if="currentUser" class="user-info-card">
        <div class="user-header">
          <el-avatar :size="72" :src="currentUser.userAvatar||defaultAvatar" />
          <div class="user-basic">
            <h3>{{ currentUser.userName }}</h3>
            <el-tag type="success" size="small" effect="dark">用户</el-tag>
          </div>
        </div>
        <el-divider />
        <div class="user-detail">
          <div class="detail-item" v-if="currentUser.userPhone"><el-icon><Phone /></el-icon><span>联系电话：{{ currentUser.userPhone }}</span></div>
          <div class="detail-item" v-if="currentUser.userEmail"><el-icon><Message /></el-icon><span>邮箱：{{ currentUser.userEmail }}</span></div>
          <div class="detail-item"><el-icon><Calendar /></el-icon><span>预约时间：{{ currentUser.appointmentTime }}</span></div>
          <div class="detail-item" v-if="currentUser.motorcycleModel"><el-icon><Van /></el-icon><span>车型：{{ currentUser.motorcycleModel }}</span></div>
          <div class="detail-item" v-if="currentUser.problemDescription"><el-icon><Document /></el-icon><span>问题描述：{{ currentUser.problemDescription }}</span></div>
        </div>
      </div>
    </el-dialog>
    <el-dialog v-model="showCompleteDialog" title="维修完成" width="450px" :close-on-click-modal="false">
      <div style="text-align:center;padding:20px 0">
        <div style="font-size:64px;margin-bottom:16px">✅</div>
        <h3 style="margin-bottom:12px">车辆维修完成！</h3>
        <p style="color:#666">订单号：{{ completeRow?.orderNo }}</p>
        <p style="color:#666">客户：{{ completeRow?.userName }}</p>
        <p style="color:#666">服务：{{ completeRow?.serviceName }}</p>
      </div>
      <template #footer>
        <el-button @click="showCompleteDialog=false">关闭</el-button>
        <el-button type="primary" @click="confirmComplete">确认完成</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showOrderDetailDialog" title="订单详情" width="650px">
      <div v-if="currentOrderDetail" class="order-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号" :span="2">{{ currentOrderDetail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="维修店">{{ currentOrderDetail.shopName }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="st(currentOrderDetail.status)" size="small">{{ currentOrderDetail.statusName }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="客户">{{ currentOrderDetail.userName }}</el-descriptions-item>
          <el-descriptions-item label="客户电话">{{ currentOrderDetail.userPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="客户邮箱">{{ currentOrderDetail.userEmail || '-' }}</el-descriptions-item>
          <el-descriptions-item label="服务">{{ currentOrderDetail.serviceName || '自定义服务' }}</el-descriptions-item>
          <el-descriptions-item label="车型">{{ currentOrderDetail.motorcycleModel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预约时间">{{ currentOrderDetail.appointmentTime }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ currentOrderDetail.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="维修技师">{{ currentOrderDetail.employeeName || '未派单' }}</el-descriptions-item>
          <el-descriptions-item label="技师技能">{{ currentOrderDetail.employeeSkill || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentOrderDetail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="完单时间">{{ currentOrderDetail.completeTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最后更新">{{ currentOrderDetail.updateTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="加班(分钟)">{{ currentOrderDetail.overtimeMinutes || 0 }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="currentOrderDetail.towService===1" style="margin-top:16px;padding:12px;background:#fdf6ec;border-radius:8px;border:1px solid #faecd8">
          <h4 style="margin:0 0 8px;color:#e6a23c">🚛 拖车服务信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="拖车司机">{{ currentOrderDetail.driverName || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="司机电话">{{ currentOrderDetail.driverPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="取车地址">{{ currentOrderDetail.towAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="距离">{{ currentOrderDetail.towDistance ? currentOrderDetail.towDistance + '公里' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="拖车费用">¥{{ currentOrderDetail.towFee || 0 }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div v-if="currentOrderDetail.problemDescription" style="margin-top:12px">
          <strong>问题描述：</strong>
          <p style="color:#666;margin:4px 0 0">{{ currentOrderDetail.problemDescription }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Phone, Message, Calendar, Van, Document } from '@element-plus/icons-vue'
import { appointmentAPI, shopAPI } from '../../api'
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const list = ref([]), statusFilter = ref(null), showTowOnly = ref(false)
const pageNum = ref(1), total = ref(0)
const showAssignDialog = ref(false), assignRow = ref(null), assignTargetId = ref(null)
const technicians = ref([])
const tomorrowRestIds = ref([])
const showUserInfoDialog = ref(false), currentUser = ref(null)
const showCompleteDialog = ref(false), completeRow = ref(null)
const showOrderDetailDialog = ref(false), currentOrderDetail = ref(null)
const st = s => ({0:'info',1:'warning',2:'primary',3:'success',4:'danger',5:'warning'}[s]||'info')
const pickupTagType = s => ({0:'warning',1:'primary',2:'success',3:'danger'}[s]||'info')
const isDedicatedDriver = skill => {
  if (!skill) return false
  const skills = skill.split(/[,，、]/)
  return skills.length === 1 && skills[0].trim() === '司机'
}
const displayList = computed(() => {
  if (!showTowOnly.value) return list.value
  return list.value.filter(r => r.towService === 1)
})
const fetchData = async () => {
  try {
    const r = await appointmentAPI.getPage({pageNum: pageNum.value, pageSize: 10, status: statusFilter.value})
    if (r.code === 200) {
      list.value = r.data.records
      total.value = r.data.total
    }
  } catch (e) {}
}
const changeStatus = async (id, s) => {
  const statusMap = {1: '确认', 4: '拒绝'}
  try {
    if (statusMap[s]) {
      await ElMessageBox.confirm(`确定要${statusMap[s]}该订单吗？`, '提示', {type: 'warning'})
    }
    await appointmentAPI.updateStatus(id, s)
    ElMessage.success('操作成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}
const showUserInfo = row => { currentUser.value = row; showUserInfoDialog.value = true }
const showOrderDetail = row => { currentOrderDetail.value = row; showOrderDetailDialog.value = true }
const completeOrder = row => { completeRow.value = row; showCompleteDialog.value = true }
const confirmComplete = async () => {
  if (!completeRow.value) return
  try {
    await appointmentAPI.complete(completeRow.value.id)
    ElMessage.success('已标记完成')
    showCompleteDialog.value = false
    fetchData()
  } catch { ElMessage.error('操作失败') }
}
const noshowOrder = async row => {
  try {
    await ElMessageBox.confirm(`确认将订单「${row.orderNo}」标记为爽约？`, '爽约确认', { type: 'warning' })
    await appointmentAPI.noshow(row.id)
    ElMessage.success('已标记爽约')
    fetchData()
  } catch {}
}
const openAssign = async row => {
  assignRow.value = row
  assignTargetId.value = null
  tomorrowRestIds.value = []
  try {
    const [techR, restR] = await Promise.all([shopAPI.getTechnicians(row.shopId), shopAPI.getTomorrowRestTechs(row.shopId)])
    if (techR.code === 200) {
      technicians.value = (techR.data || []).filter(t => t.restStatus !== 1 && t.status === 1)
    }
    if (restR.code === 200) tomorrowRestIds.value = (restR.data || []).map(t => t.userId)
  } catch {}
  showAssignDialog.value = true
}
const selectAssignTarget = (row, type) => { assignTargetId.value = row.id }
const doAssign = async () => {
  if (!assignTargetId.value) return ElMessage.warning('请选择人员')
  try {
    await appointmentAPI.assign(assignRow.value.id, assignTargetId.value)
    ElMessage.success('派单成功')
    showAssignDialog.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '派单失败，可能存在时间冲突')
  }
}
const applyFilter = () => {}
const countdownTick = ref(Date.now())
let countdownTimer = null
const getCountdownText = (row) => {
  countdownTick.value
  if (!row.estimatedCompleteTime) return '-'
  const est = new Date(row.estimatedCompleteTime.replace(/-/g, '/'))
  const diff = est.getTime() - Date.now()
  if (diff <= 0) {
    const overMin = Math.floor(-diff / 60000)
    return `已超时${overMin}分钟`
  }
  const totalMin = Math.floor(diff / 60000)
  const hours = Math.floor(totalMin / 60)
  const mins = totalMin % 60
  return hours > 0 ? `${hours}时${mins}分` : `${mins}分钟`
}
const getCountdownColor = (row) => {
  countdownTick.value
  if (!row.estimatedCompleteTime) return '#999'
  const est = new Date(row.estimatedCompleteTime.replace(/-/g, '/'))
  const diff = est.getTime() - Date.now()
  if (diff <= 0) return '#f56c6c'
  const totalMin = Math.floor(diff / 60000)
  if (totalMin <= 10) return '#e6a23c'
  return '#409eff'
}
const startCountdown = () => {
  stopCountdown()
  countdownTimer = setInterval(() => { countdownTick.value = Date.now() }, 1000)
}
const stopCountdown = () => {
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
}
onMounted(() => { fetchData(); startCountdown() })
onBeforeUnmount(stopCountdown)
</script>
<style scoped>
.user-info-card{padding:8px}
.user-header{display:flex;align-items:center;gap:20px}
.user-basic h3{margin:0 0 6px;font-size:20px}
.user-detail{display:flex;flex-direction:column;gap:14px}
.detail-item{display:flex;align-items:center;gap:10px;font-size:15px;color:#333}
.detail-item .el-icon{color:var(--el-color-primary);font-size:18px}
.order-detail{padding:4px}
</style>
