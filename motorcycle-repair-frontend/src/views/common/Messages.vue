<template>
  <div>
    <div class="page-header"><h2>消息中心</h2>
      <div style="display:flex;gap:8px">
        <el-button type="primary" size="small" @click="markAllRead">全部已读</el-button>
        <el-button type="success" size="small" @click="openChat">发起聊天</el-button>
      </div>
    </div>
    <el-card>
      <el-table :data="messages" stripe>
        <el-table-column prop="title" label="标题" width="200" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column label="发送者" width="100">
          <template #default="{row}">{{senderLabel(row)}}</template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{row}"><el-tag :type="row.type===0?'info':row.type===1?'warning':row.type===3?'primary':row.type===4?'success':'info'" size="small">{{configStore.messageTypeName(row.type)}}</el-tag></template>
        </el-table-column>
        <el-table-column prop="isRead" label="状态" width="80">
          <template #default="{row}"><el-tag :type="row.isRead===1?'success':'danger'" size="small">{{row.isRead===1?'已读':'未读'}}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button v-if="row.isRead===0" size="small" type="primary" link @click="markRead(row.id)">已读</el-button>
            <el-button v-if="row.type===3" size="small" type="success" link @click="replyMsg(row)">回复</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="showChat" title="发起聊天" width="500px">
      <el-form :model="chatForm" label-width="80px">
        <el-form-item label="接收者">
          <el-select v-model="chatForm.targetUserId" filterable placeholder="搜索用户" style="width:100%">
            <el-option v-for="u in allUsers" :key="u.id" :label="(u.realName||u.username)+' ('+roleName(u.role)+')'" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容"><el-input v-model="chatForm.content" type="textarea" :rows="4" placeholder="输入聊天内容" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showChat=false">取消</el-button>
        <el-button type="primary" :loading="sending" @click="handleChat">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { authAPI, adminAPI, shopAPI } from '../../api'
import { useUserStore } from '../../store/user'
import { useConfigStore } from '../../store/config'
const userStore = useUserStore(), configStore = useConfigStore()
const messages = ref([]), showChat = ref(false), sending = ref(false), allUsers = ref([])
const chatForm = ref({ targetUserId: null, content: '' })
const roleName = r => configStore.roleName(r)
const senderLabel = (row) => {
  if (!row.senderRole) return '系统'
  return configStore.roleName(row.senderRole)
}
const fetchData = async () => { try { const r = await authAPI.getMessages(userStore.userId, 1, 50); if(r.code===200) messages.value = r.data.records; userStore.fetchUnreadCount() } catch(e){} }
const markRead = async id => { try { await authAPI.markRead(id); fetchData() } catch(e){} }
const markAllRead = async () => { try { await authAPI.markAllRead(userStore.userId); ElMessage.success('已全部标记已读'); fetchData() } catch(e){} }
const openChat = async () => {
  chatForm.value = { targetUserId: null, content: '' }
  if (userStore.role === 3) {
    try { const r = await shopAPI.getApprovedShops(); if(r.code===200) allUsers.value = r.data.filter(s=>s.userId).map(s => ({ id: s.userId, realName: s.shopName, role: 2 })) } catch{}
  } else {
    try { const r = await adminAPI.getUserPage({ pageNum: 1, pageSize: 500 }); if(r.code===200) allUsers.value = r.data.records.filter(u => u.id !== userStore.userId) } catch{}
  }
  showChat.value = true
}
const handleChat = async () => {
  if (!chatForm.value.targetUserId) return ElMessage.warning('请选择接收者')
  if (!chatForm.value.content) return ElMessage.warning('请输入内容')
  sending.value = true
  try {
    await authAPI.sendMessage(chatForm.value.targetUserId, userStore.userId, userStore.role, '聊天消息', chatForm.value.content, 3)
    ElMessage.success('发送成功'); showChat.value = false; fetchData()
  } catch { ElMessage.error('发送失败') }
  finally { sending.value = false }
}
const replyMsg = async row => {
  chatForm.value = { targetUserId: row.senderId ?? row.userId, content: '' }
  if (userStore.role === 3) {
    try { const r = await shopAPI.getApprovedShops(); if(r.code===200) allUsers.value = r.data.filter(s=>s.userId).map(s => ({ id: s.userId, realName: s.shopName, role: 2 })) } catch{}
  } else {
    try { const r = await adminAPI.getUserPage({ pageNum: 1, pageSize: 500 }); if(r.code===200) allUsers.value = r.data.records.filter(u => u.id !== userStore.userId) } catch{}
  }
  showChat.value = true
}
onMounted(fetchData)
</script>
