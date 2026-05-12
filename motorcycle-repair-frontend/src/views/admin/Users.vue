<template>
  <div>
    <div class="page-header"><h2>用户管理</h2></div>
    <el-card>
      <div style="margin-bottom:16px;display:flex;gap:12px">
        <el-select v-model="roleFilter" placeholder="角色筛选" clearable style="width:140px" @change="fetchData">
          <el-option v-for="r in configStore.userRoleList" :key="r.dictValue" :label="r.dictLabel" :value="parseInt(r.dictValue)" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:140px" @change="fetchData">
          <el-option v-for="s in configStore.userStatusList" :key="s.dictValue" :label="s.dictLabel" :value="parseInt(s.dictValue)" />
        </el-select>
      </div>
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="头像" width="70">
          <template #default="{row}">
            <el-avatar :size="36" :src="row.avatar || defaultAvatar" />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="角色" width="100">
          <template #default="{row}"><el-tag :type="row.role===1?'danger':row.role===2?'warning':row.role===4?'':'success'" size="small">{{roleNames[row.role]||'未知'}}</el-tag></template>
        </el-table-column>
        <el-table-column prop="verified" label="认证" width="80"><template #default="{row}"><el-tag :type="row.verified===1?'success':'info'" size="small">{{row.verified===1?'已认证':'未认证'}}</el-tag></template></el-table-column>
        <el-table-column prop="status" label="状态" width="80"><template #default="{row}"><el-tag :type="row.status===1?'success':'danger'" size="small">{{configStore.userStatusName(row.status)}}</el-tag></template></el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status===1" size="small" type="danger" @click="toggleStatus(row,0)">禁用</el-button>
            <el-button v-else size="small" type="success" @click="toggleStatus(row,1)">启用</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px" v-model:current-page="pn" v-model:page-size="ps" :total="total" layout="total, prev, pager, next" @current-change="fetchData" @size-change="fetchData" />
    </el-card>

    <el-dialog v-model="showEdit" title="编辑用户" width="600px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="ID"><el-input v-model="editForm.id" disabled /></el-form-item>
        <el-form-item label="头像">
          <div style="display:flex;align-items:center;gap:16px">
            <el-avatar :size="64" :src="editForm.avatar || defaultAvatar" />
            <el-upload :auto-upload="false" :show-file-list="false" :on-change="onAvatarChange" accept="image/*">
              <el-button size="small">选择图片</el-button>
            </el-upload>
            <el-button v-if="editForm.avatar" size="small" type="danger" @click="editForm.avatar=''">移除</el-button>
          </div>
        </el-form-item>
        <el-form-item label="用户名" required><el-input v-model="editForm.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="editForm.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="editForm.phone" maxlength="11" placeholder="11位手机号" /></el-form-item>
        <el-form-item label="专业技能" v-if="editForm.role===4"><el-input v-model="editForm.skill" placeholder="用逗号分隔，如：发动机维修,电路检修" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width:100%">
            <el-option v-for="r in configStore.userRoleList" :key="r.dictValue" :label="r.dictLabel" :value="parseInt(r.dictValue)" />
          </el-select>
        </el-form-item>
        <el-form-item label="认证状态">
          <el-switch v-model="editForm.verified" :active-value="1" :inactive-value="0" active-text="已认证" inactive-text="未认证" />
        </el-form-item>
        <el-form-item label="账号状态">
          <el-switch v-model="editForm.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="注册时间"><el-input v-model="editForm.createTime" disabled /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminAPI, authAPI } from '../../api'
import { useConfigStore } from '../../store/config'
const configStore = useConfigStore()
const defaultAvatar = computed(() => configStore.defaultAvatar)
const list = ref([]), pn = ref(1), ps = ref(10), total = ref(0), roleFilter = ref(null), statusFilter = ref(null)
const showEdit = ref(false), saving = ref(false)
const editForm = ref({ id:'', username:'', realName:'', phone:'', avatar:'', role:3, verified:0, status:1, createTime:'', skill:'' })
const roleNames = computed(() => { const m = {}; const list = configStore.dicts['user_role'] || []; list.forEach(d => m[d.dictValue] = d.dictLabel); return m })
const fetchData = async () => { try { const r = await adminAPI.getUserPage({pageNum:pn.value,pageSize:ps.value,role:roleFilter.value,status:statusFilter.value}); if(r.code===200) { list.value=r.data.records; total.value=r.data.total } } catch(e){} }
const toggleStatus = async (row, s) => { try { await adminAPI.updateUserStatus(row.id, s); ElMessage.success('操作成功'); fetchData() } catch(e){} }
const handleDelete = async (row) => {
  if (row.role === 1) return ElMessage.warning('不能删除管理员账号')
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.realName || row.username}」吗？`, '删除确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await adminAPI.deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {}
}
const openEdit = (row) => {
  editForm.value = { id: row.id, username: row.username, realName: row.realName||'', phone: row.phone||'', avatar: row.avatar||'', role: row.role, verified: row.verified, status: row.status, createTime: row.createTime||'', skill: row.skill||'' }
  showEdit.value = true
}
const onAvatarChange = async (uploadFile) => {
  try {
    const r = await adminAPI.uploadFile(uploadFile.raw)
    if (r.code === 200) editForm.value.avatar = r.data
    else ElMessage.error('上传失败')
  } catch { ElMessage.error('上传失败') }
}
const handleSave = async () => {
  if (!editForm.value.username || !editForm.value.username.trim()) {
    return ElMessage.warning('用户名不能为空')
  }
  if (editForm.value.phone && !/^1[3-9]\d{9}$/.test(editForm.value.phone)) {
    return ElMessage.warning('手机号格式不正确，请输入11位手机号')
  }
  saving.value = true
  try {
    await adminAPI.updateUser(editForm.value.id, { realName: editForm.value.realName, phone: editForm.value.phone, avatar: editForm.value.avatar, skill: editForm.value.skill })
    await adminAPI.updateUserRole(editForm.value.id, editForm.value.role)
    await adminAPI.updateUserStatus(editForm.value.id, editForm.value.status)
    if (editForm.value.verified !== undefined) {
      const profileData = { id: editForm.value.id, verified: editForm.value.verified }
      await authAPI.updateProfile(profileData)
    }
    ElMessage.success('保存成功')
    showEdit.value = false
    fetchData()
  } catch(e) { ElMessage.error(e?.response?.data?.message || '保存失败') }
  finally { saving.value = false }
}
onMounted(fetchData)
</script>
