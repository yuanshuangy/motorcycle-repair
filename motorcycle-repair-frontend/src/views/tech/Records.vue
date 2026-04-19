<template>
  <div>
    <div class="page-header"><h2>维修记录</h2></div>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="shopName" label="维修店" />
        <el-table-column prop="userName" label="客户" />
        <el-table-column prop="serviceName" label="服务" />
        <el-table-column prop="statusName" label="状态" width="100"><template #default="{row}"><el-tag :type="st(row.status)" size="small">{{ row.statusName }}</el-tag></template></el-table-column>
        <el-table-column label="维修记录" width="120">
          <template #default="{row}">
            <el-button v-if="row.status===2||row.status===3" size="small" type="primary" @click="openRecord(row)">查看/填写</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="showRecord" title="维修记录" width="600px">
      <el-form :model="record" label-width="100px">
        <el-form-item label="诊断"><el-input v-model="record.diagnosis" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="维修项目"><el-input v-model="record.repairItems" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="使用配件"><el-input v-model="record.partsUsed" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="配件费用"><el-input-number v-model="record.partsCost" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="工时费用"><el-input-number v-model="record.laborCost" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="record.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRecord=false">关闭</el-button>
        <el-button type="primary" @click="saveRecord">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { appointmentAPI } from '../../api'
import { useUserStore } from '../../store/user'
const userStore = useUserStore()
const list = ref([]), showRecord = ref(false)
const record = ref({ id:null, appointmentId:null, diagnosis:'', repairItems:'', partsUsed:'', partsCost:0, laborCost:0, remark:'' })
const st = s => ({0:'info',1:'warning',2:'primary',3:'success',4:'danger'}[s]||'info')
const fetchData = async () => { try { const r=await appointmentAPI.getPage({pageNum:1,pageSize:50}); if(r.code===200) list.value=r.data.records } catch{} }
const openRecord = async row => {
  record.value = { id:null, appointmentId:row.id, shopId:row.shopId, diagnosis:'', repairItems:'', partsUsed:'', partsCost:0, laborCost:0, remark:'' }
  try { const r=await appointmentAPI.getRecord(row.id); if(r.code===200&&r.data) Object.assign(record.value, r.data) } catch{}
  showRecord.value=true
}
const saveRecord = async () => { try { if(record.value.id) await appointmentAPI.updateRecord(record.value); else await appointmentAPI.addRecord(record.value); ElMessage.success('保存成功'); showRecord.value=false; fetchData() } catch{ ElMessage.error('保存失败') } }
onMounted(fetchData)
</script>
