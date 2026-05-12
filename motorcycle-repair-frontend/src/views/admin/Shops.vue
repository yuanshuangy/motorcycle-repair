<template>
  <div>
    <div class="page-header"><h2>店铺管理</h2></div>
    <el-card v-if="shop">
      <div style="display:flex;gap:24px;align-items:flex-start">
        <el-avatar :size="100" shape="square" :src="shop.coverImage||defaultCover" />
        <div style="flex:1">
          <h2 style="margin:0 0 12px">{{ shop.shopName }}</h2>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="地址">{{ shop.address }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ shop.phone }}</el-descriptions-item>
            <el-descriptions-item label="营业时间">{{ shop.businessHours }} <el-tag :type="isOpen?'success':'danger'" size="small" effect="dark" style="margin-left:8px">{{ isOpen?'营业中':'非营业' }}</el-tag></el-descriptions-item>
            <el-descriptions-item label="评分"><el-rate v-model="shop.rating" disabled :size="14" /> {{ shop.rating }}</el-descriptions-item>
            <el-descriptions-item label="自动派单"><el-switch v-model="shop.autoAssign" :active-value="1" :inactive-value="0" @change="updateShop" /></el-descriptions-item>
            <el-descriptions-item label="简介" :span="2">{{ shop.description }}</el-descriptions-item>
          </el-descriptions>
          <div style="margin-top:16px"><el-button type="primary" @click="openEditShop">编辑店铺</el-button></div>
        </div>
      </div>
    </el-card>
    <el-card v-else><el-empty description="暂无店铺信息" /></el-card>
    <el-dialog v-model="showEditShop" title="编辑店铺信息" width="600px">
      <el-form :model="shopForm" label-width="100px">
        <el-form-item label="店铺名称"><el-input v-model="shopForm.shopName" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="shopForm.address" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="shopForm.phone" /></el-form-item>
        <el-form-item label="营业时间">
          <div style="display:flex;align-items:center;gap:8px">
            <el-time-select v-model="shopForm.openTime" :max-time="shopForm.closeTime" placeholder="开门时间" start="06:00" step="00:30" end="23:00" style="width:140px" />
            <span>至</span>
            <el-time-select v-model="shopForm.closeTime" :min-time="shopForm.openTime" placeholder="关门时间" start="06:00" step="00:30" end="23:00" style="width:140px" />
          </div>
        </el-form-item>
        <el-form-item label="简介"><el-input v-model="shopForm.description" type="textarea" rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showEditShop=false">取消</el-button><el-button type="primary" @click="saveShop">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { shopAPI } from '../../api'
const defaultCover = 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png'
const shop = ref(null), showEditShop = ref(false)
const shopForm = reactive({ id:null, shopName:'', address:'', phone:'', businessHours:'', description:'', openTime:'', closeTime:'' })
const isOpen = computed(() => {
  if(!shop.value || !shop.value.businessHours) return true
  const bh = shop.value.businessHours
  const match = bh.match(/(\d{1,2}):?(\d{0,2})\s*[-~到至]\s*(\d{1,2}):?(\d{0,2})/)
  if(!match) return true
  const now = new Date(), nowMin = now.getHours()*60+now.getMinutes()
  const openMin = parseInt(match[1])*60+parseInt(match[2]||'0'), closeMin = parseInt(match[3])*60+parseInt(match[4]||'0')
  return nowMin >= openMin && nowMin < closeMin
})
const fetchShop = async () => {
  try { const r = await shopAPI.getPage({pageNum:1,pageSize:1,auditStatus:1}); if(r.code===200 && r.data.records.length) shop.value = r.data.records[0] } catch{}
}
const parseBH = bh => {
  if(!bh) { shopForm.openTime=''; shopForm.closeTime=''; return }
  const match = bh.match(/(\d{1,2}:\d{2})\s*[-~到至]\s*(\d{1,2}:\d{2})/)
  if(match) { shopForm.openTime=match[1]; shopForm.closeTime=match[2] }
}
const openEditShop = () => {
  if (!shop.value) return
  Object.assign(shopForm, { id:shop.value.id, shopName:shop.value.shopName, address:shop.value.address, phone:shop.value.phone, businessHours:shop.value.businessHours, description:shop.value.description })
  parseBH(shop.value.businessHours)
  showEditShop.value = true
}
const saveShop = async () => {
  if(shopForm.openTime && shopForm.closeTime) shopForm.businessHours = shopForm.openTime + '-' + shopForm.closeTime
  try { await shopAPI.update(shopForm); ElMessage.success('保存成功'); showEditShop.value=false; fetchShop() } catch{ ElMessage.error('保存失败') }
}
const updateShop = async () => {
  if (!shop.value) return
  try { await shopAPI.update({id:shop.value.id, autoAssign:shop.value.autoAssign}); ElMessage.success('已更新') } catch{}
}
onMounted(fetchShop)
</script>
