<template>
  <div>
    <div class="page-header"><h2>维修记录</h2></div>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="userName" label="客户" />
        <el-table-column prop="serviceName" label="服务" />
        <el-table-column prop="statusName" label="状态" width="100"><template #default="{row}"><el-tag :type="st(row.status)" size="small">{{ row.statusName }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="120"><template #default="{row}"><el-button v-if="row.status===2||row.status===3" size="small" type="primary" @click="openRecord(row)">记录</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="showDlg" title="维修记录" width="600px">
      <el-form v-if="!record.id" :model="recForm" label-width="100px">
        <el-form-item label="故障诊断"><el-input v-model="recForm.diagnosis" type="textarea" rows="2" /></el-form-item>
        <el-form-item label="维修项目"><el-input v-model="recForm.repairItems" type="textarea" rows="2" /></el-form-item>
        <el-form-item label="使用配件"><el-input v-model="recForm.partsUsed" /></el-form-item>
        <el-form-item label="配件费用"><el-input-number v-model="recForm.partsCost" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="工时费用"><el-input-number v-model="recForm.laborCost" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="recForm.remark" type="textarea" rows="2" /></el-form-item>
      </el-form>
      <el-descriptions v-else :column="1" border>
        <el-descriptions-item label="故障诊断">{{ record.diagnosis }}</el-descriptions-item>
        <el-descriptions-item label="维修项目">{{ record.repairItems }}</el-descriptions-item>
        <el-descriptions-item label="使用配件">{{ record.partsUsed }}</el-descriptions-item>
        <el-descriptions-item label="配件费用">¥{{ record.partsCost }}</el-descriptions-item>
        <el-descriptions-item label="工时费用">¥{{ record.laborCost }}</el-descriptions-item>
        <el-descriptions-item label="总费用">¥{{ record.totalCost }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ record.remark }}</el-descriptions-item>
      </el-descriptions>
      <template #footer v-if="!record.id"><el-button @click="showDlg=false">取消</el-button><el-button type="primary" @click="saveRecord">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { appointmentAPI, shopAPI } from '../../api'
import { useUserStore } from '../../store/user'
const userStore = useUserStore()
const list = ref([]), showDlg = ref(false), record = ref({}), shopId = ref(null)
const recForm = reactive({ appointmentId:null, shopId:null, employeeId:null, diagnosis:'', repairItems:'', partsUsed:'', partsCost:0, laborCost:0, remark:'' })
const st = s => ({0:'info',1:'warning',2:'primary',3:'success',4:'danger'}[s]||'info')
const fetchData = async () => { try { const r=await shopAPI.getByUserId(userStore.userId); if(r.code===200&&r.data) { shopId.value=r.data.id; const ar=await appointmentAPI.getPage({pageNum:1,pageSize:50,shopId:shopId.value}); if(ar.code===200) list.value=ar.data.records } } catch(e){} }
const openRecord = async row => { try { const r=await appointmentAPI.getRecord(row.id); if(r.code===200&&r.data) { record.value=r.data } else { record.value={}; Object.keys(recForm).forEach(k=>recForm[k]=k.includes('Cost')?0:null); recForm.appointmentId=row.id; recForm.shopId=shopId.value } showDlg.value=true } catch(e){} }
const saveRecord = async () => { recForm.totalCost=recForm.partsCost+recForm.laborCost; try { await appointmentAPI.addRecord(recForm); ElMessage.success('保存成功'); showDlg.value=false; fetchData() } catch(e){} }
onMounted(fetchData)
</script>
