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
        <el-checkbox v-model="showTowOnly" @change="filterList">只看拖车订单</el-checkbox>
      </div>
      <el-table :data="filteredList" stripe>
        <el-table-column label="订单号" width="180">
          <template #default="{row}">
            <el-link type="primary" @click="showDetail(row)">{{ row.orderNo }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="shopName" label="维修店" width="160" />
        <el-table-column prop="userName" label="客户" width="100" />
        <el-table-column label="服务" min-width="140">
          <template #default="{row}">
            {{ row.serviceName || '自定义服务' }}
            <el-tag v-if="row.towService===1" type="warning" size="small" effect="dark" style="margin-left:4px">🚛拖车</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="motorcycleModel" label="车型" width="100" />
        <el-table-column prop="appointmentTime" label="预约时间" width="170" />
        <el-table-column label="维修技师" width="100">
          <template #default="{row}">
            <span v-if="row.employeeName">{{ row.employeeName }}</span>
            <span v-else style="color:#999">未派单</span>
          </template>
        </el-table-column>
        <el-table-column label="拖车司机" width="100">
          <template #default="{row}">
            <span v-if="row.driverName">{{ row.driverName }}</span>
            <span v-else style="color:#999">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
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
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="info" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
      </div>
    </el-card>
    <el-dialog v-model="showDetailDialog" title="订单详情" width="600px">
      <div v-if="currentOrder" class="order-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号" :span="2">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="维修店">{{ currentOrder.shopName }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="st(currentOrder.status)" size="small">{{ currentOrder.statusName }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="客户">{{ currentOrder.userName }}</el-descriptions-item>
          <el-descriptions-item label="客户电话">{{ currentOrder.userPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="服务">{{ currentOrder.serviceName || '自定义服务' }}</el-descriptions-item>
          <el-descriptions-item label="车型">{{ currentOrder.motorcycleModel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预约时间">{{ currentOrder.appointmentTime }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ currentOrder.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="维修技师">{{ currentOrder.employeeName || '未派单' }}</el-descriptions-item>
          <el-descriptions-item label="技师技能">{{ currentOrder.employeeSkill || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="完单时间">{{ currentOrder.completeTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最后更新">{{ currentOrder.updateTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="加班(分钟)">{{ currentOrder.overtimeMinutes || 0 }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="currentOrder.towService===1" style="margin-top:16px;padding:12px;background:#fdf6ec;border-radius:8px;border:1px solid #faecd8">
          <h4 style="margin:0 0 8px;color:#e6a23c">🚛 拖车服务信息</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="拖车司机">{{ currentOrder.driverName || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="司机电话">{{ currentOrder.driverPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="取车地址">{{ currentOrder.towAddress || '-' }}</el-descriptions-item>
            <el-descriptions-item label="距离">{{ currentOrder.towDistance ? currentOrder.towDistance + '公里' : '-' }}</el-descriptions-item>
            <el-descriptions-item label="拖车费用">¥{{ currentOrder.towFee || 0 }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div v-if="currentOrder.problemDescription" style="margin-top:12px">
          <strong>问题描述：</strong>
          <p style="color:#666;margin:4px 0 0">{{ currentOrder.problemDescription }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { appointmentAPI } from '../../api'
const list = ref([]), statusFilter = ref(null), showTowOnly = ref(false)
const pageNum = ref(1), pageSize = ref(10), total = ref(0)
const showDetailDialog = ref(false), currentOrder = ref(null)
const st = s => ({0:'info',1:'warning',2:'primary',3:'success',4:'danger',5:'warning'}[s]||'info')
const filteredList = computed(() => {
  if (!showTowOnly.value) return list.value
  return list.value.filter(r => r.towService === 1)
})
const fetchData = async () => {
  try {
    const r = await appointmentAPI.getPage({pageNum: pageNum.value, pageSize: pageSize.value, status: statusFilter.value})
    if (r.code === 200) {
      list.value = r.data.records
      total.value = r.data.total
    }
  } catch (e) {}
}
const showDetail = row => { currentOrder.value = row; showDetailDialog.value = true }
const filterList = () => {}
onMounted(fetchData)
</script>
<style scoped>
.order-detail{padding:4px}
</style>
