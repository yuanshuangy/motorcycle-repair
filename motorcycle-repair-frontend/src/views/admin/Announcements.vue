<template>
  <div>
    <div class="page-header"><h2>公告管理</h2><el-button type="primary" @click="handleAdd">发布公告</el-button></div>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column prop="createTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="160">
          <template #default="{row}"><el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button><el-button size="small" type="danger" @click="handleDel(row)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="showDlg" :title="isEdit?'编辑公告':'发布公告'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" rows="6" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showDlg=false">取消</el-button><el-button type="primary" @click="handleSubmit">确定</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminAPI } from '../../api'
const list = ref([]), showDlg = ref(false), isEdit = ref(false)
const form = reactive({ id:null, title:'', content:'' })
const fetchData = async () => { try { const r = await adminAPI.getAnnouncements({pageNum:1,pageSize:50}); if(r.code===200) list.value=r.data.records } catch(e){} }
const handleAdd = () => { isEdit.value=false; form.id=null; form.title=''; form.content=''; showDlg.value=true }
const handleEdit = row => { isEdit.value=true; Object.assign(form, row); showDlg.value=true }
const handleDel = async row => { await ElMessageBox.confirm('确定删除？','提示',{type:'warning'}); try { await adminAPI.deleteAnnouncement(row.id); ElMessage.success('已删除'); fetchData() } catch(e){} }
const handleSubmit = async () => { try { if(isEdit.value) await adminAPI.updateAnnouncement(form); else await adminAPI.addAnnouncement(form); ElMessage.success('操作成功'); showDlg.value=false; fetchData() } catch(e){} }
onMounted(fetchData)
</script>
