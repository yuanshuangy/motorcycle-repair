<template>
  <div>
    <div class="page-header">
      <h2>服务管理</h2>
      <div style="display:flex;gap:8px">
        <el-button type="success" @click="openTemplate" v-if="list.length===0">从常用模板初始化</el-button>
        <el-button type="primary" @click="handleAdd">新增服务</el-button>
      </div>
    </div>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="serviceName" label="服务名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="price" label="参考价格" width="100"><template #default="{row}">¥{{ row.price }}</template></el-table-column>
        <el-table-column prop="duration" label="时长" width="100"><template #default="{row}">{{ row.duration }}分钟</template></el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{row}"><el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button><el-button size="small" type="danger" @click="handleDel(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="showDlg" :title="isEdit?'编辑服务':'新增服务'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="服务名称">
          <el-select v-if="!isEdit" v-model="form.serviceName" filterable allow-create default-first-option placeholder="选择常用服务或输入自定义名称" style="width:100%">
            <el-option v-for="t in templates" :key="t.id" :label="t.serviceName" :value="t.serviceName">
              <span>{{ t.serviceName }}</span>
              <span style="float:right;color:#999;font-size:12px">参考¥{{ t.price }}</span>
            </el-option>
          </el-select>
          <el-input v-else v-model="form.serviceName" />
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" rows="3" /></el-form-item>
        <el-form-item label="参考价格(元)"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="时长(分钟)"><el-input-number v-model="form.duration" :min="1" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showDlg=false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
    <el-dialog v-model="showTemplate" title="常用服务模板" width="700px">
      <p style="color:#999;margin-bottom:16px">选择需要初始化的服务项目，将自动添加到您的店铺中</p>
      <el-table :data="templates" stripe @selection-change="onTemplateSelect">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="serviceName" label="服务名称" />
        <el-table-column prop="category" label="分类" width="80" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="price" label="参考价格" width="100"><template #default="{row}">¥{{ row.price }}</template></el-table-column>
        <el-table-column prop="duration" label="时长" width="80"><template #default="{row}">{{ row.duration }}分钟</template></el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showTemplate=false">取消</el-button>
        <el-button type="primary" @click="selectAll">全选</el-button>
        <el-button type="success" :loading="initLoading" :disabled="selectedTemplates.length===0" @click="doInitTemplate">初始化选中服务</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { serviceAPI, shopAPI } from '../../api'
import { useUserStore } from '../../store/user'
const userStore = useUserStore()
const list = ref([]), showDlg = ref(false), isEdit = ref(false), shopId = ref(null)
const form = reactive({ id:null, shopId:null, serviceName:'', description:'', price:0, duration:60 })
const showTemplate = ref(false), templates = ref([]), selectedTemplates = ref([]), initLoading = ref(false)
const fetchData = async () => { try { const r=await shopAPI.getByUserId(userStore.userId); if(r.code===200&&r.data) { shopId.value=r.data.id; const sr=await serviceAPI.getByShop(shopId.value); if(sr.code===200) list.value=sr.data } } catch(e){} }
const fetchTemplates = async () => { try { const r=await serviceAPI.getTemplates(); if(r.code===200) templates.value=r.data } catch{} }
const handleAdd = async () => { isEdit.value=false; Object.keys(form).forEach(k=>form[k]=k==='price'?0:k==='duration'?60:''); if(templates.value.length===0) await fetchTemplates(); showDlg.value=true }
const handleEdit = row => { isEdit.value=true; Object.assign(form, row); showDlg.value=true }
const handleDel = async row => { await ElMessageBox.confirm('确定删除？','提示',{type:'warning'}); try { await serviceAPI.delete(row.id); ElMessage.success('已删除'); fetchData() } catch(e){} }
const handleSubmit = async () => {
  if(!form.serviceName) return ElMessage.warning('请填写服务名称')
  if(!isEdit.value && list.value.some(s => s.serviceName === form.serviceName)) return ElMessage.warning('该服务已存在，请勿重复添加')
  form.shopId=shopId.value
  try { if(isEdit.value) await serviceAPI.update(form); else await serviceAPI.add(form); ElMessage.success('操作成功'); showDlg.value=false; fetchData() } catch(e){ ElMessage.error(e.response?.data?.message||'操作失败') }
}
watch(() => form.serviceName, val => {
  if (!isEdit.value && val && templates.value.length) {
    const t = templates.value.find(t => t.serviceName === val)
    if (t) { form.description = t.description || ''; form.price = t.price || 0; form.duration = t.duration || 60 }
  }
})
const openTemplate = async () => { await fetchTemplates(); selectedTemplates.value=[]; showTemplate.value=true }
const onTemplateSelect = val => { selectedTemplates.value = val }
const selectAll = () => { selectedTemplates.value = [...templates.value] }
const doInitTemplate = async () => {
  if (selectedTemplates.value.length === 0) return ElMessage.warning('请选择服务项目')
  const existingNames = new Set(list.value.map(s => s.serviceName))
  const toAdd = selectedTemplates.value.filter(t => !existingNames.has(t.serviceName))
  if (toAdd.length === 0) return ElMessage.warning('所选服务已全部存在，无需重复添加')
  initLoading.value = true
  try {
    for (const t of toAdd) {
      await serviceAPI.add({ shopId: shopId.value, serviceName: t.serviceName, description: t.description, price: t.price, duration: t.duration })
    }
    ElMessage.success(`已初始化${toAdd.length}个服务项目`)
    showTemplate.value = false
    fetchData()
  } catch { ElMessage.error('初始化失败') }
  finally { initLoading.value = false }
}
onMounted(fetchData)
</script>
