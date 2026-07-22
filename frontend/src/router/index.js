import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/Login.vue'),
      meta: { noAuth: true }
    },
    {
      path: '/',
      component: () => import('../components/AppLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('../views/Dashboard.vue'),
          meta: { title: '仪表盘', roles: ['admin', 'operator', 'viewer'] }
        },
        {
          path: 'screens',
          name: 'Screens',
          component: () => import('../views/ScreenManage.vue'),
          meta: { title: '屏幕管理', roles: ['admin', 'operator', 'viewer'] }
        },
        {
          path: 'screen-groups',
          name: 'ScreenGroups',
          component: () => import('../views/ScreenGroupManage.vue'),
          meta: { title: '屏幕分组', roles: ['admin', 'operator'] }
        },
        {
          path: 'contents',
          name: 'Contents',
          component: () => import('../views/ContentManage.vue'),
          meta: { title: '内容管理', roles: ['admin', 'operator', 'viewer'] }
        },
        {
          path: 'programs',
          name: 'Programs',
          component: () => import('../views/ProgramManage.vue'),
          meta: { title: '节目管理', roles: ['admin', 'operator', 'viewer'] }
        },
        {
          path: 'schedules',
          name: 'Schedules',
          component: () => import('../views/ScheduleManage.vue'),
          meta: { title: '定时调度', roles: ['admin', 'operator'] }
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('../views/UserManage.vue'),
          meta: { title: '用户管理', roles: ['admin'] }
        },
        {
          path: 'logs',
          name: 'Logs',
          component: () => import('../views/LogManage.vue'),
          meta: { title: '播放日志', roles: ['admin', 'operator', 'viewer'] }
        },
        {
          path: 'alarm-config',
          name: 'AlarmConfig',
          component: () => import('../views/AlarmConfig.vue'),
          meta: { title: '告警配置', roles: ['admin'] }
        },
        {
          path: 'alarm-records',
          name: 'AlarmRecords',
          component: () => import('../views/AlarmRecords.vue'),
          meta: { title: '告警记录', roles: ['admin', 'operator'] }
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('../views/Settings.vue'),
          meta: { title: '系统设置', roles: ['admin'] }
        },
        {
          path: 'audit-logs',
          name: 'AuditLogs',
          component: () => import('../views/AuditLogManage.vue'),
          meta: { title: '审计日志', roles: ['admin'] }
        }
      ]
    },
    {
      path: '/403',
      name: 'Forbidden',
      component: () => import('../views/Forbidden.vue'),
      meta: { noAuth: true }
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard'
    }
  ]
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.noAuth) {
    if (token && to.name === 'Login') {
      return next('/dashboard')
    }
    return next()
  }
  if (!token) {
    return next('/login')
  }
  if (to.meta.roles) {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      try {
        const user = JSON.parse(userStr)
        if (!to.meta.roles.includes((user.role || '').toLowerCase())) {
          return next('/403')
        }
      } catch (e) {
        return next('/login')
      }
    }
  }
  next()
})

export default router
