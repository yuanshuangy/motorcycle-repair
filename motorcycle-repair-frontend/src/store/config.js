import { defineStore } from 'pinia'
import { configAPI } from '../api'
export const useConfigStore = defineStore('config', {
  state: () => ({
    system: {},
    dicts: {},
    towPricing: [],
    loaded: false,
  }),
  getters: {
    systemName: state => state.system.system_name || '摩托车维修预约系统',
    shopDisplayName: state => state.system.shop_display_name || '张师傅维修店',
    defaultAvatar: state => state.system.default_avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
    defaultBusinessHours: state => state.system.default_business_hours || '08:00-20:00',
    defaultServiceDuration: state => parseInt(state.system.default_service_duration) || 60,
    reviewTags: state => (state.system.review_tags || '服务态度好,技术专业,速度快,价格合理,沟通顺畅,环境整洁,耐心细致,准时交付').split(','),
    rateTexts: state => (state.system.rate_texts || '很差,较差,一般,满意,非常满意').split(','),
    restRules: state => state.system.rest_rules || '①每人每周休2天 ②不能连续休息 ③每项维修技能至少2人在岗 ④不考虑司机技能（人人都是司机）⑤优先安排休息少的技师',
    brandSlogan: state => state.system.brand_slogan || '专业 · 便捷 · 高效',
    brandFeatures: state => (state.system.brand_features || '在线预约维修服务,实时查看维修进度,专业技师团队保障,透明价格安心消费').split(','),
    loginWelcome: state => state.system.login_welcome || '欢迎回来',
    loginSubtitle: state => state.system.login_subtitle || '请登录您的账号',
    welcomeTitle: state => role => ({1: state.system.admin_welcome_title || '管理员工作台', 2: state.system.shop_welcome_title || '维修店管理台', 4: state.system.tech_welcome_title || '技师工作台'}[role] || state.system.user_welcome_title || '首页'),
    introTitle: state => role => ({2: state.system.shop_intro_title || '店铺运营概览', 4: state.system.tech_intro_title || '维修工作概览'}[role] || ''),
    introText: state => role => ({2: state.system.shop_intro_text || '管理店铺信息、上架维修服务、处理客户预约、分配维修技师、查看维修记录。', 4: state.system.tech_intro_text || '查看派单工单、处理维修任务、填写维修记录、维护专业技能。'}[role] || ''),
    praiseTemplate: state => state.system.praise_template || '恭喜【{name}】在{period}接单排行榜中荣获{rank}！您的高效服务和专业精神深受客户信赖，是团队中的佼佼者。感谢您的努力付出，期待您再创辉煌！',
    rankLabels: state => (state.system.rank_labels || '🥇冠军,🥈亚军,🥉季军').split(','),
    periodLabels: state => (state.system.period_labels || '今日,昨日,本周,本月').split(','),
    revenuePeriods: state => (state.system.revenue_periods || '今日,昨日,本周,本月,本季度,今年').split(','),
    roleName: state => role => {
      const list = state.dicts['user_role'] || []
      const found = list.find(d => d.dictValue === String(role))
      return found ? found.dictLabel : '未知'
    },
    statusName: state => s => {
      const list = state.dicts['appointment_status'] || []
      const found = list.find(d => d.dictValue === String(s))
      return found ? found.dictLabel : '未知'
    },
    statusCss: state => s => {
      const list = state.dicts['appointment_status'] || []
      const found = list.find(d => d.dictValue === String(s))
      return found ? found.cssClass : 'info'
    },
    pickupName: state => s => {
      const list = state.dicts['pickup_status'] || []
      const found = list.find(d => d.dictValue === String(s))
      return found ? found.dictLabel : '未知'
    },
    userStatusName: state => s => {
      const list = state.dicts['user_status'] || []
      const found = list.find(d => d.dictValue === String(s))
      return found ? found.dictLabel : '未知'
    },
    messageTypeName: state => s => {
      const list = state.dicts['message_type'] || []
      const found = list.find(d => d.dictValue === String(s))
      return found ? found.dictLabel : '系统'
    },
    payStatusName: state => s => {
      const list = state.dicts['pay_status'] || []
      const found = list.find(d => d.dictValue === String(s))
      return found ? found.dictLabel : '未知'
    },
    towPrice: state => distance => {
      for (const p of state.towPricing) {
        if (distance > p.minDistance && distance <= p.maxDistance) return p.price
      }
      if (state.towPricing.length) return state.towPricing[state.towPricing.length - 1].price
      return 0
    },
    appointmentStatusList: state => state.dicts['appointment_status'] || [],
    userRoleList: state => state.dicts['user_role'] || [],
    userStatusList: state => state.dicts['user_status'] || [],
  },
  actions: {
    async loadAll() {
      if (this.loaded) return
      try {
        const [sysR, towR] = await Promise.all([
          configAPI.getSystemConfigs(),
          configAPI.getTowPricing(),
        ])
        if (sysR.code === 200) this.system = sysR.data
        if (towR.code === 200) this.towPricing = towR.data
        const dictTypes = ['appointment_status', 'pickup_status', 'user_role', 'user_status', 'audit_status', 'message_type', 'pay_status']
        const dictResults = await Promise.allSettled(dictTypes.map(t => configAPI.getDictData(t)))
        dictTypes.forEach((t, i) => { if (dictResults[i].status === 'fulfilled' && dictResults[i].value.code === 200) this.dicts[t] = dictResults[i].value.data })
        this.loaded = true
      } catch (e) { console.error('Failed to load config:', e) }
    },
  }
})
