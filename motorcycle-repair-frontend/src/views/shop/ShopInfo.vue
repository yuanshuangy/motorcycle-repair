<template>
  <div>
    <div class="page-header"><h2>店铺信息</h2><el-button type="primary" @click="openEdit">{{ shop.id?'编辑信息':'创建店铺' }}</el-button></div>
    <el-card v-if="shop.id">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="店铺名称">{{ shop.shopName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ shop.phone }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ shop.address }}</el-descriptions-item>
        <el-descriptions-item label="营业时间">{{ shop.businessHours }}</el-descriptions-item>
        <el-descriptions-item label="评分"><el-rate v-model="shop.rating" disabled /></el-descriptions-item>
        <el-descriptions-item label="审核状态"><el-tag :type="shop.auditStatus===1?'success':shop.auditStatus===2?'danger':'warning'">{{ shop.auditStatus===1?'已通过':shop.auditStatus===2?'已拒绝':'待审核' }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">{{ shop.description }}</el-descriptions-item>
        <el-descriptions-item v-if="shop.auditRemark" label="审核备注" :span="2">{{ shop.auditRemark }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-card v-else><el-empty description="您还未创建店铺，请点击右上角创建" /></el-card>
    <el-card v-if="shop.id" style="margin-top:20px">
      <template #header><span style="font-weight:700">店铺设置</span></template>
      <el-form label-width="140px">
        <el-form-item label="自动派单">
          <el-switch v-model="shop.autoAssign" :active-value="1" :inactive-value="0" active-text="开启" inactive-text="关闭" @change="updateSetting('autoAssign', $event)" />
          <span style="color:#999;font-size:12px;margin-left:12px">开启后新订单自动分配给关联技师</span>
        </el-form-item>
        <el-form-item label="自动接收技师">
          <el-switch v-model="shop.autoAcceptTech" :active-value="1" :inactive-value="0" active-text="开启" inactive-text="关闭" @change="updateSetting('autoAcceptTech', $event)" />
          <span style="color:#999;font-size:12px;margin-left:12px">开启后技师申请加入自动审核通过</span>
        </el-form-item>
      </el-form>
    </el-card>
    <el-dialog v-model="showEdit" title="编辑店铺" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="店铺名称"><el-input v-model="form.shopName" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="营业时间">
          <div style="display:flex;align-items:center;gap:8px">
            <el-time-select v-model="form.openTime" :max-time="form.closeTime" placeholder="开门时间" start="06:00" step="00:30" end="23:00" style="width:140px" />
            <span>至</span>
            <el-time-select v-model="form.closeTime" :min-time="form.openTime" placeholder="关门时间" start="06:00" step="00:30" end="23:00" style="width:140px" />
          </div>
        </el-form-item>
        <el-form-item label="简介"><el-input v-model="form.description" type="textarea" rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showEdit=false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { shopAPI } from '../../api'
import { useUserStore } from '../../store/user'
const userStore = useUserStore()
const shop = ref({}), showEdit = ref(false)
const form = reactive({ id:null, userId:null, shopName:'', address:'', phone:'', businessHours:'', description:'', openTime:'', closeTime:'' })
const fetchShop = async () => { try { const r=await shopAPI.getByUserId(userStore.userId); if(r.code===200) shop.value=r.data||{} } catch(e){} }
const parseBusinessHours = bh => {
  if(!bh) { form.openTime=''; form.closeTime=''; return }
  const match = bh.match(/(\d{1,2}:\d{2})\s*[-~到至]\s*(\d{1,2}:\d{2})/)
  if(match) { form.openTime=match[1]; form.closeTime=match[2] }
  else { form.openTime=''; form.closeTime='' }
}
const openEdit = () => {
  if(shop.value.id) { Object.assign(form, shop.value); parseBusinessHours(shop.value.businessHours) }
  showEdit.value=true
}
const updateSetting = async (field, val) => {
  try { await shopAPI.update({ id: shop.value.id, [field]: val }); ElMessage.success('设置已更新') }
  catch { ElMessage.error('设置失败'); shop.value[field] = val === 1 ? 0 : 1 }
}
const handleSave = async () => {
  form.userId = userStore.userId
  if(form.openTime && form.closeTime) form.businessHours = form.openTime + '-' + form.closeTime
  try {
    if (shop.value.id) { form.id=shop.value.id; await shopAPI.update(form) }
    else await shopAPI.add(form)
    ElMessage.success('保存成功'); showEdit.value=false; fetchShop()
  } catch(e){ ElMessage.error('保存失败') }
}
onMounted(fetchShop)
</script>
