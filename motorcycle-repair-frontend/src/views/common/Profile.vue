<template>
  <div>
    <div class="page-header"><h2>个人中心</h2></div>
    <el-row :gutter="24">
      <el-col :span="8">
        <el-card style="text-align:center;padding:24px 0">
          <div class="avatar-wrapper" @click="triggerUpload">
            <el-avatar :size="100" :src="avatarFullUrl||defaultAvatar" />
            <div class="avatar-overlay"><el-icon size="24"><Camera /></el-icon><span>更换头像</span></div>
          </div>
          <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileChange" />
          <h3 style="margin-top:16px">{{ profile.realName||profile.username }}</h3>
          <el-tag :type="roleTag" style="margin-top:8px" effect="dark">{{ profile.roleName }}</el-tag>
          <div v-if="profile.verified===1" style="margin-top:8px"><el-tag type="success" size="small"><el-icon><Check /></el-icon> 已实名</el-tag></div>
          <div v-else style="margin-top:8px"><el-button type="warning" size="small" @click="showVerify=true">实名认证</el-button></div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <el-tabs v-model="tab">
            <el-tab-pane label="基本信息" name="info">
              <el-form :model="profile" label-width="100px" style="max-width:500px;margin-top:20px">
                <el-form-item label="用户名"><el-input v-model="profile.username" disabled /></el-form-item>
                <el-form-item label="真实姓名"><el-input v-model="profile.realName" /></el-form-item>
                <el-form-item label="手机号"><el-input v-model="profile.phone" /></el-form-item>
                <el-form-item label="邮箱"><el-input v-model="profile.email" /></el-form-item>
                <el-form-item><el-button type="primary" @click="handleSave">保存修改</el-button></el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="修改密码" name="pwd">
              <el-form :model="pwdForm" label-width="100px" style="max-width:500px;margin-top:20px">
                <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
                <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
                <el-form-item label="确认密码"><el-input v-model="pwdForm.confirm" type="password" show-password /></el-form-item>
                <el-form-item><el-button type="primary" @click="handlePwd">修改密码</el-button></el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
    <el-dialog v-model="showAvatarDlg" title="修改头像" width="500px" :close-on-click-modal="false">
      <div style="text-align:center">
        <div v-if="previewUrl" style="margin-bottom:16px">
          <el-avatar :size="120" :src="previewUrl" />
        </div>
        <el-upload :auto-upload="false" :show-file-list="false" :on-change="onUploadChange" accept="image/*">
          <el-button type="primary">选择图片</el-button>
        </el-upload>
        <p style="color:#999;font-size:12px;margin-top:8px">支持 JPG、PNG 格式，建议尺寸 200x200 以上</p>
      </div>
      <template #footer>
        <el-button @click="cancelAvatar">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleAvatar">确认上传</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="showVerify" title="实名认证" width="400px"><el-input v-model="idCard" placeholder="请输入身份证号" /><template #footer><el-button @click="showVerify=false">取消</el-button><el-button type="primary" @click="handleVerify">提交认证</el-button></template></el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, Check } from '@element-plus/icons-vue'
import { authAPI, adminAPI } from '../../api'
import { useUserStore } from '../../store/user'
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const tab = ref('info'), showAvatarDlg = ref(false), showVerify = ref(false), idCard = ref('')
const uploading = ref(false), selectedFile = ref(null), previewUrl = ref('')
const fileInput = ref(null)
const profile = reactive({ id:null, username:'', realName:'', phone:'', email:'', avatar:'', role:null, roleName:'', verified:0 })
const pwdForm = reactive({ oldPassword:'', newPassword:'', confirm:'' })
const roleTag = computed(() => ({1:'danger',2:'warning',3:'success'}[profile.role]||'info'))
const avatarFullUrl = computed(() => profile.avatar || '')
const fetchProfile = async () => { try { const r = await authAPI.getProfile(userStore.userId); if(r.code===200) Object.assign(profile, r.data) } catch(e){} }
const handleSave = async () => { try { await authAPI.updateProfile({id:profile.id,realName:profile.realName,phone:profile.phone,email:profile.email}); ElMessage.success('保存成功'); userStore.realName=profile.realName; localStorage.setItem('realName', profile.realName) } catch(e){ ElMessage.error('保存失败') } }
const triggerUpload = () => { showAvatarDlg.value = true; selectedFile.value = null; previewUrl.value = '' }
const onUploadChange = (uploadFile) => {
  if (!uploadFile.raw.type.startsWith('image/')) { ElMessage.warning('请选择图片文件'); return }
  if (uploadFile.raw.size > 5 * 1024 * 1024) { ElMessage.warning('图片大小不能超过5MB'); return }
  selectedFile.value = uploadFile.raw
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(uploadFile.raw)
}
const onFileChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { ElMessage.warning('请选择图片文件'); return }
  if (file.size > 5 * 1024 * 1024) { ElMessage.warning('图片大小不能超过5MB'); return }
  uploading.value = true
  try {
    const r = await adminAPI.uploadFile(file)
    if (r.code === 200) {
      await authAPI.updateAvatar(userStore.userId, r.data)
      profile.avatar = r.data
      userStore.avatar = r.data
      localStorage.setItem('avatar', r.data)
      ElMessage.success('头像已更新')
    } else { ElMessage.error('上传失败') }
  } catch (err) { ElMessage.error('上传失败') }
  finally { uploading.value = false; e.target.value = '' }
}
const handleAvatar = async () => {
  if (!selectedFile.value) { ElMessage.warning('请先选择图片'); return }
  uploading.value = true
  try {
    const r = await adminAPI.uploadFile(selectedFile.value)
    if (r.code === 200) {
      await authAPI.updateAvatar(userStore.userId, r.data)
      profile.avatar = r.data
      userStore.avatar = r.data
      localStorage.setItem('avatar', r.data)
      showAvatarDlg.value = false
      ElMessage.success('头像已更新')
    } else { ElMessage.error('上传失败') }
  } catch (err) { ElMessage.error('上传失败') }
  finally { uploading.value = false }
}
const cancelAvatar = () => { if (previewUrl.value) URL.revokeObjectURL(previewUrl.value); showAvatarDlg.value = false; selectedFile.value = null; previewUrl.value = '' }
const handlePwd = async () => { if(!pwdForm.oldPassword||!pwdForm.newPassword) return ElMessage.warning('请填写完整'); if(pwdForm.newPassword!==pwdForm.confirm) return ElMessage.warning('密码不一致'); try { await authAPI.updatePassword({userId:userStore.userId,oldPassword:pwdForm.oldPassword,newPassword:pwdForm.newPassword}); ElMessage.success('密码已修改'); pwdForm.oldPassword=''; pwdForm.newPassword=''; pwdForm.confirm='' } catch(e){ ElMessage.error('修改失败') } }
const handleVerify = async () => { if(!idCard.value) return ElMessage.warning('请输入身份证号'); try { await authAPI.verifyUser(userStore.userId, idCard.value); profile.verified=1; showVerify.value=false; ElMessage.success('认证成功') } catch(e){ ElMessage.error('认证失败') } }
onMounted(fetchProfile)
</script>
<style scoped>
.avatar-wrapper{position:relative;display:inline-block;cursor:pointer;border-radius:50%;overflow:hidden}
.avatar-wrapper:hover .avatar-overlay{opacity:1}
.avatar-overlay{position:absolute;inset:0;background:rgba(0,0,0,.5);display:flex;flex-direction:column;align-items:center;justify-content:center;color:#fff;opacity:0;transition:opacity .3s;border-radius:50%}
.avatar-overlay span{font-size:12px;margin-top:4px}
</style>
