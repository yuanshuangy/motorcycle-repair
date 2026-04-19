<template>
  <div>
    <div class="page-header"><h2>工单管理</h2></div>
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
      </div>
      <el-table :data="list" stripe>
        <el-table-column label="订单号" width="180">
          <template #default="{row}">
            <el-link type="primary" @click="showOrderDetail(row)">{{ row.orderNo }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="shopName" label="维修店" width="130" />
        <el-table-column prop="userName" label="客户" width="80" />
        <el-table-column label="服务" min-width="130">
          <template #default="{row}">
            {{ row.serviceName || '自定义服务' }}
            <el-tag v-if="row.towService===1" type="warning" size="small" effect="dark" style="margin-left:4px">🚛拖车</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="motorcycleModel" label="车型" width="90" />
        <el-table-column prop="appointmentTime" label="预约时间" width="170" />
        <el-table-column label="我的角色" width="100">
          <template #default="{row}">
            <el-tag v-if="row.driverId === currentUserId && row.employeeId === currentUserId" type="" size="small">司机+维修</el-tag>
            <el-tag v-else-if="row.driverId === currentUserId" type="warning" size="small">🚛拖车司机</el-tag>
            <el-tag v-else type="primary" size="small">🔧维修技师</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="接车状态" width="100">
          <template #default="{row}">
            <template v-if="row.towService===1">
              <el-tag :type="pickupTagType(row.pickupStatus)" size="small">{{ row.pickupStatusName }}</el-tag>
            </template>
            <span v-else style="color:#ccc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="90">
          <template #default="{row}">
            <el-tag :type="st(row.status)" size="small">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="完单/更新时间" width="170">
          <template #default="{row}">
            <div v-if="row.completeTime" style="color:#67c23a;font-size:12px">完单: {{ row.completeTime }}</div>
            <div v-else-if="row.updateTime" style="color:#999;font-size:12px">更新: {{ row.updateTime }}</div>
            <span v-else style="color:#ccc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="info" @click="showUserInfo(row)">客户信息</el-button>
            <el-button v-if="row.status===1 && row.employeeId===currentUserId" size="small" type="primary" @click="changeStatus(row.id,2)">开始维修</el-button>
            <el-button v-if="row.status===2 && row.employeeId===currentUserId" size="small" type="success" @click="completeOrder(row)">完成维修</el-button>
            <template v-if="row.towService===1 && row.driverId===currentUserId">
              <el-button v-if="row.pickupStatus===0 || row.pickupStatus===null" size="small" type="warning" @click="doConfirmPickup(row)">确认接车</el-button>
              <el-button v-if="row.pickupStatus===0 || row.pickupStatus===null" size="small" type="danger" @click="doPickupFailed(row)">没接到</el-button>
              <el-button v-if="row.pickupStatus===1" size="small" type="success" @click="doVehicleArrived(row)">车辆到店</el-button>
              <el-tag v-if="row.pickupStatus===2" type="success" size="small" effect="dark">✅已到店</el-tag>
              <el-tag v-if="row.pickupStatus===3" type="danger" size="small" effect="dark">❌未接到</el-tag>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        <el-pagination v-model:current-page="pageNum" :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>
    <el-dialog v-model="showUserInfoDialog" title="客户信息" width="500px">
      <div v-if="currentUser" class="user-info-card">
        <div class="user-header">
          <el-avatar :size="72" :src="currentUser.userAvatar||defaultAvatar" />
          <div class="user-basic"><h3>{{ currentUser.userName }}</h3><el-tag type="success" size="small" effect="dark">用户</el-tag></div>
        </div>
        <el-divider />
        <div class="user-detail">
          <div class="detail-item" v-if="currentUser.userPhone"><el-icon><Phone /></el-icon><span>联系电话：{{ currentUser.userPhone }}</span></div>
          <div class="detail-item" v-if="currentUser.userEmail"><el-icon><Message /></el-icon><span>邮箱：{{ currentUser.userEmail }}</span></div>
          <div class="detail-item"><el-icon><Calendar /></el-icon><span>预约时间：{{ currentUser.appointmentTime }}</span></div>
          <div class="detail-item" v-if="currentUser.motorcycleModel"><el-icon><Van /></el-icon><span>车型：{{ currentUser.motorcycleModel }}</span></div>
          <div class="detail-item" v-if="currentUser.problemDescription"><el-icon><Document /></el-icon><span>问题描述：{{ currentUser.problemDescription }}</span></div>
        </div>
        <div v-if="currentUser.towService===1" style="margin-top:16px;padding:12px;background:#fdf6ec;border-radius:8px;border:1px solid #faecd8">
          <h4 style="margin:0 0 8px;color:#e6a23c">🚛 拖车服务信息</h4>
          <div style="font-size:13px;color:#666;line-height:1.8">
            <div>取车地址：{{ currentUser.towAddress || '-' }}</div>
            <div>距离：{{ currentUser.towDistance ? currentUser.towDistance + '公里' : '-' }}</div>
            <div>拖车费用：¥{{ currentUser.towFee || 0 }}</div>
            <div v-if="currentUser.driverName">拖车司机：{{ currentUser.driverName }}</div>
            <div>接车状态：<el-tag :type="pickupTagType(currentUser.pickupStatus)" size="small">{{ currentUser.pickupStatusName }}</el-tag></div>
          </div>
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
          <el-descriptions-item label="创建时间">{{ currentOrderDetail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="完单时间">{{ currentOrderDetail.completeTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最后更新">{{ currentOrderDetail.updateTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="加班(分钟)">{{ currentOrderDetail.overtimeMinutes || 0 }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="currentOrderDetail.towService===1" style="margin-top:16px;padding:12px;background:#fdf6ec;border-radius:8px;border:1px solid #faecd8">
          <h4 style="margin:0 0 8px;color:#e6a23c">🚛 拖车服务信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="拖车司机">{{ currentOrderDetail.driverName || '未分配' }}</el-descriptions-item>
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Phone, Message, Calendar, Van, Document } from '@element-plus/icons-vue'
import { appointmentAPI } from '../../api'
import { useUserStore } from '../../store/user'
const userStore = useUserStore()
const currentUserId = ref(userStore.userId)
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const list = ref([]), statusFilter = ref(null)
const showUserInfoDialog = ref(false), currentUser = ref(null)
const showCompleteDialog = ref(false), completeRow = ref(null)
const pageNum = ref(1), total = ref(0)
const showOrderDetailDialog = ref(false), currentOrderDetail = ref(null)
const st = s => ({0:'info',1:'warning',2:'primary',3:'success',4:'danger',5:'warning'}[s]||'info')
const pickupTagType = s => ({0:'warning',1:'primary',2:'success',3:'danger'}[s]||'info')
const fetchData = async () => { try { const r=await appointmentAPI.getPage({pageNum:pageNum.value,pageSize:10,status:statusFilter.value}); if(r.code===200) { list.value=r.data.records; total.value=r.data.total } } catch(e){} }
const changeStatus = async (id,s) => { try { await appointmentAPI.updateStatus(id,s); ElMessage.success('操作成功'); fetchData() } catch(e){ ElMessage.error('操作失败') } }
const showUserInfo = row => { currentUser.value = row; showUserInfoDialog.value = true }
const showOrderDetail = row => { currentOrderDetail.value = row; showOrderDetailDialog.value = true }
const completeOrder = row => { completeRow.value = row; showCompleteDialog.value = true }
const confirmComplete = async () => {
  if (!completeRow.value) return
  try { await appointmentAPI.complete(completeRow.value.id); ElMessage.success('已标记完成'); showCompleteDialog.value = false; fetchData() } catch { ElMessage.error('操作失败') }
}
const doConfirmPickup = async row => {
  try {
    await ElMessageBox.confirm(`确认已接到客户「${row.userName}」的车辆？`, '确认接车', { type: 'info', confirmButtonText: '确认接车', cancelButtonText: '取消' })
    await appointmentAPI.confirmPickup(row.id)
    ElMessage.success('已确认接车，请尽快将车辆送至维修店')
    fetchData()
  } catch {}
}
const doPickupFailed = async row => {
  try {
    await ElMessageBox.confirm(`确认未能接到客户「${row.userName}」的车辆？`, '未接到车辆', { type: 'warning', confirmButtonText: '确认未接到', cancelButtonText: '取消' })
    await appointmentAPI.pickupFailed(row.id)
    ElMessage.warning('已标记未接到，请联系客户重新安排')
    fetchData()
  } catch {}
}
const doVehicleArrived = async row => {
  try {
    await ElMessageBox.confirm(`确认车辆已送达维修店？`, '车辆到店确认', { type: 'success', confirmButtonText: '确认到店', cancelButtonText: '取消' })
    await appointmentAPI.vehicleArrived(row.id)
    ElMessage.success('车辆已到店，可交给维修技师处理')
    fetchData()
  } catch {}
}
onMounted(fetchData)
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
