import { defineStore } from 'pinia'
import { authAPI } from '../api'
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') ? parseInt(localStorage.getItem('userId')) : null,
    username: localStorage.getItem('username') || '',
    realName: localStorage.getItem('realName') || '',
    role: localStorage.getItem('role') ? parseInt(localStorage.getItem('role')) : null,
    avatar: localStorage.getItem('avatar') || '',
    unreadCount: 0,
  }),
  actions: {
    setUser(u) {
      this.token = u.token; this.userId = u.id; this.username = u.username;
      this.realName = u.realName; this.role = u.role; this.avatar = u.avatar || '';
      localStorage.setItem('token', u.token); localStorage.setItem('userId', u.id);
      localStorage.setItem('username', u.username); localStorage.setItem('realName', u.realName);
      localStorage.setItem('role', u.role);
      if (u.avatar) localStorage.setItem('avatar', u.avatar);
    },
    logout() {
      this.token = ''; this.userId = null; this.username = ''; this.realName = ''; this.role = null; this.avatar = ''; this.unreadCount = 0;
      const keys = ['token', 'userId', 'username', 'realName', 'role', 'avatar']
      keys.forEach(k => localStorage.removeItem(k))
    },
    async fetchProfile() {
      if (!this.userId) return;
      try {
        const r = await authAPI.getProfile(this.userId);
        if (r.code === 200) { this.realName = r.data.realName; this.avatar = r.data.avatar || ''; localStorage.setItem('realName', this.realName); if (r.data.avatar) localStorage.setItem('avatar', r.data.avatar); }
      } catch (e) { console.error(e) }
    },
    async fetchUnreadCount() {
      if (!this.userId) return;
      try { const r = await authAPI.getUnreadCount(this.userId); if(r.code===200) this.unreadCount = r.data } catch{}
    },
  }
})
