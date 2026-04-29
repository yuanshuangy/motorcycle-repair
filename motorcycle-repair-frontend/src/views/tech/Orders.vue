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
        <el-table-column label="金额" width="100">
          <template #default="{row}">
            <span :style="{ color: row.finalPrice != null ? '#67c23a' : '' }">
              ¥{{ row.finalPrice ?? row.totalAmount }}
            </span>
            <el-tag v-if="row.finalPrice != null && row.totalAmount !== row.finalPrice" type="info" size="small" effect="dark" style="margin-left:4px">已调整</el-tag>
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
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="info" @click="showUserInfo(row)">客户信息</el-button>
            <el-button v-if="(row.status===1 || row.status===2) && row.employeeId===currentUserId" size="small" type="warning" @click="openTransferDialog(row)">转单</el-button>
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

    <!-- 客户信息弹窗 -->
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

    <!-- 价格确认弹窗 -->
    <el-dialog v-model="showPriceDialog" title="确认维修费用" width="500px" :close-on-click-modal="false">
      <div v-if="priceRow" class="price-confirm-content">
        <el-descriptions :column="1" border size="small" style="margin-bottom:20px">
          <el-descriptions-item label="订单号">{{ priceRow.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ priceRow.userName }}</el-descriptions-item>
          <el-descriptions-item label="服务项目">{{ priceRow.serviceName || '自定义服务' }}</el-descriptions-item>
          <el-descriptions-item label="参考价格">
            <span style="text-decoration:line-through;color:#999">¥{{ priceRow.totalAmount }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-form :model="priceForm" label-width="110px">
          <el-form-item label="最终费用(元)" required>
            <el-input-number v-model="priceForm.finalPrice" :min="0" :precision="2" :step="10" style="width:200px" />
            <span v-if="priceForm.finalPrice !== priceRow.totalAmount" style="margin-left:12px;font-size:13px" :class="priceForm.finalPrice > (priceRow.totalAmount||0) ? 'color:#f56c6c' : 'color:#67c23a'">
              {{ priceForm.finalPrice > (priceRow.totalAmount||0) ? '+' : '' }}{{ (priceForm.finalPrice - (priceRow.totalAmount||0)).toFixed(2) }}元
            </span>
          </el-form-item>
          <el-form-item label="费用说明备注">
            <el-input v-model="priceForm.priceRemark" type="textarea" rows="3" placeholder="如：更换了额外配件、工时费调整等..." />
          </el-form-item>
        </el-form>
        <div style="padding:12px 16px;background:#e8f4fd;border-radius:8px;margin-top:12px;font-size:13px;color:#409eff">
          💡 提示：参考价格为预估价，实际费用请根据检查结果如实填写，客户将在支付时看到最终费用
        </div>
      </div>
      <template #footer>
        <el-button @click="showPriceDialog=false">取消</el-button>
        <el-button type="primary" @click="confirmAndComplete">确认费用并完成</el-button>
      </template>
    </el-dialog>

    <!-- 完成维修弹窗（无价格修改时） -->
    <el-dialog v-model="showCompleteDialog" title="维修完成" width="450px" :close-on-click-modal="false">
      <div style="text-align:center;padding:20px 0">
        <div style="font-size:64px;margin-bottom:16px">✅</div>
        <h3 style="margin-bottom:12px">车辆维修完成！</h3>
        <p style="color:#666">订单号：{{ completeRow?.orderNo }}</p>
        <p style="color:#666">客户：{{ completeRow?.userName }}</p>
        <p style="color:#666">服务：{{ completeRow?.serviceName }}</p>
        <p style="color:#333;font-weight:bold;font-size:18px;margin-top:12px">费用：¥{{ completeRow?.totalAmount }}</p>
      </div>
      <template #footer>
        <el-button @click="showCompleteDialog=false">关闭</el-button>
        <el-button type="warning" @click="showPriceFromComplete">修改费用</el-button>
        <el-button type="primary" @click="confirmComplete">直接完成</el-button>
      </template>
    </el-dialog>

    <!-- 转单弹窗 -->
    <el-dialog v-model="showTransferDialog" title="转单给其他技师" width="550px" :close-on-click-modal="false">
      <div v-if="transferRow" class="transfer-content">
        <el-alert type="info" :closable="false" style="margin-bottom:16px">
          <p style="margin:0">将订单 <strong>{{ transferRow.orderNo }}</strong> 转交给其他技师处理</p>
          <p style="margin:4px 0 0;color:#999;font-size:12px">当前负责技师：你 | 客户：{{ transferRow.userName }}</p>
        </el-alert>
        <el-form :model="transferForm" label-width="90px">
          <el-form-item label="选择技师" required>
            <div v-loading="transferLoading" style="width:100%">
              <el-select v-model="transferForm.newTechId" placeholder="请选择要转交的技师" style="width:100%" filterable>
                <el-option v-for="t in transferTechs" :key="t.id" :label="t.name" :value="t.id" :disabled="t.hasConflict">
                  <div style="display:flex;justify-content:space-between;align-items:center;width:100%">
                    <span>{{ t.name }}</span>
                    <span style="font-size:12px;color:#999">
                      <el-tag v-if="t.hasConflict" type="danger" size="small" effect="plain">时间冲突</el-tag>
                      <span v-else>当前{{ t.currentOrders }}单</span>
                    </span>
                  </div>
                </el-option>
              </el-select>
            </div>
          </el-form-item>
          <el-form-item label="转单原因">
            <el-input v-model="transferForm.transferReason" type="textarea" rows="3" placeholder="如：临时有事、技能不匹配等（选填）" />
          </el-form-item>
        </el-form>
        <div style="padding:12px 16px;background:#fdf6ec;border-radius:8px;margin-top:12px;font-size:13px;color:#e6a23c">
          ⚠️ 注意：转单后该订单将由新技师负责，您将无法再对该订单进行操作
        </div>
      </div>
      <template #footer>
        <el-button @click="showTransferDialog=false">取消</el-button>
        <el-button type="primary" :loading="transferSubmitting" :disabled="!transferForm.newTechId" @click="doTransfer">确认转单</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情弹窗 -->
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
          <el-descriptions-item label="金额">
            ¥{{ currentOrderDetail.totalAmount }}
            <el-tag v-if="currentOrderDetail.remark && currentOrderDetail.remark.includes('价格备注')" type="info" size="small" effect="plain" style="margin-left:4px">已调整</el-tag>
          </el-descriptions-item>
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
        <div v-if="currentOrderDetail.remark" style="margin-top:12px">
          <strong>备注：</strong>
          <p style="color:#666;margin:4px 0 0">{{ currentOrderDetail.remark }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Phone, Message, Calendar, Van, Document } from '@element-plus/icons-vue'
import { appointmentAPI } from '../../api'
import { useUserStore } from '../../store/user'
const userStore = useUserStore()
const currentUserId = ref(userStore.userId)
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const list = ref([]), statusFilter = ref(null)
const showUserInfoDialog = ref(false), currentUser = ref(null)

const showPriceDialog = ref(false), priceRow = ref(null)
const priceForm = reactive({ finalPrice: 0, priceRemark: '' })

const showCompleteDialog = ref(false), completeRow = ref(null)

const showTransferDialog = ref(false), transferRow = ref(null)
const transferLoading = ref(false), transferSubmitting = ref(false)
const transferForm = reactive({ newTechId: null, transferReason: '' })
const transferTechs = ref([])

const pageNum = ref(1), total = ref(0)
const showOrderDetailDialog = ref(false), currentOrderDetail = ref(null)
const st = s => ({0:'info',1:'warning',2:'primary',3:'success',4:'danger',5:'warning'}[s]||'info')
const pickupTagType = s => ({0:'warning',1:'primary',2:'success',3:'danger'}[s]||'info')
const fetchData = async () => { try { const r=await appointmentAPI.getPage({pageNum:pageNum.value,pageSize:10,status:statusFilter.value}); if(r.code===200) { list.value=r.data.records; total.value=r.data.total } } catch(e){} }
const changeStatus = async (id,s) => { try { await appointmentAPI.updateStatus(id,s); ElMessage.success('操作成功'); fetchData() } catch(e){ ElMessage.error('操作失败') } }
const showUserInfo = row => { currentUser.value = row; showUserInfoDialog.value = true }
const showOrderDetail = row => { currentOrderDetail.value = row; showOrderDetailDialog.value = true }

const completeOrder = row => {
  completeRow.value = row
  priceForm.finalPrice = row.totalAmount || 0
  priceForm.priceRemark = ''
  showCompleteDialog.value = true
}

const showPriceFromComplete = () => {
  if (!completeRow.value) return
  showCompleteDialog.value = false
  priceRow.value = completeRow.value
  priceForm.finalPrice = completeRow.value.totalAmount || 0
  priceForm.priceRemark = ''
  showPriceDialog.value = true
}

const confirmComplete = async () => {
  if (!completeRow.value) return
  try {
    await appointmentAPI.complete(completeRow.value.id)
    ElMessage.success('已标记完成')
    showCompleteDialog.value = false
    fetchData()
  } catch { ElMessage.error('操作失败') }
}

const confirmAndComplete = async () => {
  if (!priceRow.value || !priceForm.finalPrice) return ElMessage.warning('请填写最终费用')
  try {
    await appointmentAPI.confirmPrice(priceRow.value.id, priceForm.finalPrice, priceForm.priceRemark)
    await appointmentAPI.complete(priceRow.value.id)
    ElMessage.success(`费用确认为¥${priceForm.finalPrice}，订单已完成`)
    showPriceDialog.value = false
    fetchData()
  } catch(e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const openTransferDialog = async (row) => {
  transferRow.value = row
  transferForm.newTechId = null
  transferForm.transferReason = ''
  transferTechs.value = []
  showTransferDialog.value = true
  transferLoading.value = true
  try {
    const r = await appointmentAPI.getTransferTechs(row.id)
    if (r.code === 200) {
      transferTechs.value = r.data || []
      if (transferTechs.value.length === 0) {
        ElMessage.warning('暂无可转接的技师')
      }
    }
  } catch { ElMessage.error('获取技师列表失败') }
  finally { transferLoading.value = false }
}

const doTransfer = async () => {
  if (!transferForm.newTechId) return ElMessage.warning('请选择目标技师')
  const targetTech = transferTechs.value.find(t => t.id === transferForm.newTechId)
  if (targetTech && targetTech.hasConflict) return ElMessage.warning('该技师时间冲突，无法转单')

  try {
    await ElMessageBox.confirm(
      `确认将此订单转交给「${targetTech?.name}」？\n转单后将由对方全权负责`,
      '确认转单',
      { type: 'warning', confirmButtonText: '确认转单', cancelButtonText: '取消' }
    )
  } catch { return }

  transferSubmitting.value = true
  try {
    await appointmentAPI.transferOrder(
      transferRow.value.id,
      transferForm.newTechId,
      transferForm.transferReason
    )
    ElMessage.success(`订单已成功转交给${targetTech?.name}`)
    showTransferDialog.value = false
    fetchData()
  } catch(e) {
    ElMessage.error(e.response?.data?.message || '转单失败')
  } finally { transferSubmitting.value = false }
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
.price-confirm-content{}
.transfer-content{}
</style>
