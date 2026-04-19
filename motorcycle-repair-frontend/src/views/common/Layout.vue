<template>
  <el-container style="height:100%">
    <el-header class="app-header">
      <div class="header-left">
        <span class="logo-icon">🏍</span>
        <span class="logo-text">{{ configStore.systemName }}</span>
      </div>
      <div class="header-right">
        <el-badge :value="userStore.unreadCount" :hidden="userStore.unreadCount===0" class="msg-badge">
          <el-button :icon="Bell" circle size="small" @click="$router.push('/layout/messages')" />
        </el-badge>
        <el-avatar :size="36" :src="avatarUrl" />
        <div class="user-info">
          <span class="user-name">{{ userStore.realName || userStore.username }}</span>
          <el-tag :type="roleTag" size="small" effect="dark">{{ roleName }}</el-tag>
        </div>
        <el-dropdown @command="handleCommand">
          <el-button :icon="Setting" circle size="small" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-container>
      <el-aside width="240px" class="app-aside">
        <el-menu :default-active="$route.path" router class="side-menu">
          <el-menu-item v-if="userStore.role===1" index="/layout/home"><el-icon><HomeFilled /></el-icon><span>工作台</span></el-menu-item>
          <el-menu-item v-if="userStore.role===2" index="/layout/home"><el-icon><HomeFilled /></el-icon><span>工作台</span></el-menu-item>
          <el-menu-item v-if="userStore.role===4" index="/layout/home"><el-icon><HomeFilled /></el-icon><span>工作台</span></el-menu-item>
          <template v-if="userStore.role===3">
            <el-menu-item index="/layout/user/shops"><el-icon><Shop /></el-icon><span>{{ configStore.shopDisplayName }}</span></el-menu-item>
            <el-menu-item index="/layout/user/appointments"><el-icon><Calendar /></el-icon><span>我的预约</span></el-menu-item>
            <el-menu-item index="/layout/user/reviews"><el-icon><ChatDotRound /></el-icon><span>我的评价</span></el-menu-item>
          </template>
          <template v-if="userStore.role===2">
            <el-menu-item index="/layout/shop/info"><el-icon><Shop /></el-icon><span>店铺信息</span></el-menu-item>
            <el-menu-item index="/layout/shop/services"><el-icon><Tools /></el-icon><span>服务管理</span></el-menu-item>
            <el-menu-item index="/layout/shop/orders"><el-icon><List /></el-icon><span>订单管理</span></el-menu-item>
            <el-menu-item index="/layout/shop/employees"><el-icon><Avatar /></el-icon><span>技师管理</span></el-menu-item>
            <el-menu-item index="/layout/shop/records"><el-icon><Document /></el-icon><span>维修记录</span></el-menu-item>
          </template>
          <template v-if="userStore.role===4">
            <el-menu-item index="/layout/tech/orders"><el-icon><List /></el-icon><span>工单管理</span></el-menu-item>
            <el-menu-item index="/layout/tech/records"><el-icon><Document /></el-icon><span>维修记录</span></el-menu-item>
          </template>
          <template v-if="userStore.role===1">
            <el-menu-item index="/layout/admin/users"><el-icon><UserFilled /></el-icon><span>用户管理</span></el-menu-item>
            <el-menu-item index="/layout/admin/shops"><el-icon><Shop /></el-icon><span>店铺管理</span></el-menu-item>
            <el-menu-item index="/layout/admin/orders"><el-icon><List /></el-icon><span>订单管理</span></el-menu-item>
            <el-menu-item index="/layout/admin/announcements"><el-icon><Bell /></el-icon><span>公告管理</span></el-menu-item>
          </template>
          <el-divider />
          <el-menu-item index="/layout/messages"><el-icon><Message /></el-icon><span>消息中心</span></el-menu-item>
          <el-menu-item index="/layout/profile"><el-icon><User /></el-icon><span>个人中心</span></el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="app-main"><router-view /></el-main>
    </el-container>
    <el-dialog v-model="showAnnouncement" title="📢 系统公告" width="500px" :close-on-click-modal="false">
      <div v-for="a in announcements" :key="a.id" style="margin-bottom:16px;padding-bottom:16px;border-bottom:1px solid #f0f0f0">
        <h3 style="margin:0 0 8px">{{ a.title }}</h3>
        <p style="color:#666;line-height:1.8">{{ a.content }}</p>
        <p style="color:#999;font-size:12px;margin-top:8px">{{ a.createTime }}</p>
      </div>
      <template #footer><el-button type="primary" @click="showAnnouncement=false">已阅</el-button></template>
    </el-dialog>
  </el-container>
</template>
<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../../store/user'
import { useConfigStore } from '../../store/config'
import { authAPI, adminAPI } from '../../api'
import { HomeFilled, Shop, Tools, Calendar, List, ChatDotRound, User, UserFilled, Avatar, Setting, SwitchButton, Bell, Message, Document, Promotion } from '@element-plus/icons-vue'
const router = useRouter(), route = useRoute(), userStore = useUserStore(), configStore = useConfigStore()
const avatarUrl = computed(() => userStore.avatar || configStore.defaultAvatar)
const roleName = computed(() => configStore.roleName(userStore.role))
const roleTag = computed(() => ({1:'danger',2:'warning',3:'success',4:''}[userStore.role]||'info'))
const showAnnouncement = ref(false), announcements = ref([])
const handleCommand = c => { if (c==='profile') router.push('/layout/profile'); else if (c==='logout') { userStore.logout(); router.push('/login') } }
const fetchAnnouncements = async () => {
  if (userStore.role === 3) {
    try { const r = await adminAPI.getLatestAnnouncements(); if(r.code===200 && r.data.records.length) { announcements.value = r.data.records; showAnnouncement.value = true } } catch{}
  }
}
onMounted(() => { configStore.loadAll(); userStore.fetchUnreadCount(); fetchAnnouncements() })
watch(() => route.path, () => userStore.fetchUnreadCount())
</script>
<style scoped>
.app-header{background:var(--primary);color:#fff;display:flex;align-items:center;justify-content:space-between;padding:0 24px;height:60px;box-shadow:0 2px 8px rgba(0,0,0,.15)}
.header-left{display:flex;align-items:center;gap:12px}
.logo-icon{font-size:28px}
.logo-text{font-size:18px;font-weight:700;letter-spacing:2px}
.header-right{display:flex;align-items:center;gap:16px}
.user-info{display:flex;align-items:center;gap:8px}
.user-name{font-size:14px;font-weight:500}
.msg-badge{margin-right:4px}
.app-aside{background:#fff;border-right:1px solid var(--border);overflow-y:auto}
.side-menu{padding:8px}
.side-menu .el-menu-item{border-radius:8px;margin:2px 0;height:44px;line-height:44px}
.side-menu .el-menu-item.is-active{background:linear-gradient(135deg,var(--accent),var(--highlight))!important;color:#fff!important}
.app-main{background:var(--bg);padding:24px;overflow-y:auto}
</style>
