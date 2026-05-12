import { createRouter, createWebHistory } from 'vue-router'
const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/common/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/common/Register.vue') },
  { path: '/', redirect: '/login' },
  {
    path: '/layout', component: () => import('../views/common/Layout.vue'),
    children: [
      { path: 'home', name: 'Home', component: () => import('../views/common/Home.vue'), meta: { roles: [1, 2, 4] } },
      { path: 'profile', name: 'Profile', component: () => import('../views/common/Profile.vue') },
      { path: 'messages', name: 'Messages', component: () => import('../views/common/Messages.vue') },
      // 用户端
      { path: 'user/shops', name: 'UserShops', component: () => import('../views/user/Shops.vue') },
      { path: 'user/appointments', name: 'UserAppointments', component: () => import('../views/user/Appointments.vue') },
      { path: 'user/reviews', name: 'UserReviews', component: () => import('../views/user/Reviews.vue') },
      // 维修店端
      { path: 'shop/info', name: 'ShopInfo', component: () => import('../views/shop/ShopInfo.vue') },
      { path: 'shop/services', name: 'ShopServices', component: () => import('../views/shop/Services.vue') },
      { path: 'shop/orders', name: 'ShopOrders', component: () => import('../views/shop/Orders.vue') },
      { path: 'shop/employees', name: 'ShopEmployees', component: () => import('../views/shop/Employees.vue') },
      { path: 'shop/records', name: 'ShopRecords', component: () => import('../views/shop/Records.vue') },
      // 技师端
      { path: 'tech/orders', name: 'TechOrders', component: () => import('../views/tech/Orders.vue') },
      { path: 'tech/records', name: 'TechRecords', component: () => import('../views/tech/Records.vue') },
      // 管理员端
      { path: 'admin/users', name: 'AdminUsers', component: () => import('../views/admin/Users.vue') },
      { path: 'admin/shops', name: 'AdminShops', component: () => import('../views/admin/Shops.vue') },
      { path: 'admin/orders', name: 'AdminOrders', component: () => import('../views/admin/Orders.vue') },
      { path: 'admin/announcements', name: 'AdminAnnouncements', component: () => import('../views/admin/Announcements.vue') },
    ]
  }
]
const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && to.path !== '/register' && !token) next('/login')
  else if ((to.path === '/login' || to.path === '/register') && token) {
    const role = parseInt(localStorage.getItem('role'))
    if (role === 3) next('/layout/user/shops')
    else next('/layout/home')
  }
  else if (to.path === '/layout' || to.path === '/layout/') {
    const role = parseInt(localStorage.getItem('role'))
    if (role === 3) next('/layout/user/shops')
    else next('/layout/home')
  }
  else {
    const role = parseInt(localStorage.getItem('role'))
    const roleAccess = {
      1: ['/layout/home', '/layout/profile', '/layout/messages', '/layout/admin/'],
      2: ['/layout/home', '/layout/profile', '/layout/messages', '/layout/shop/'],
      3: ['/layout/profile', '/layout/messages', '/layout/user/'],
      4: ['/layout/home', '/layout/profile', '/layout/messages', '/layout/tech/'],
    }
    const allowed = roleAccess[role] || []
    if (to.path.startsWith('/layout/') && !allowed.some(p => to.path.startsWith(p))) {
      const defaults = { 1: '/layout/home', 2: '/layout/home', 3: '/layout/user/shops', 4: '/layout/home' }
      next(defaults[role] || '/login')
    } else {
      next()
    }
  }
})
export default router
