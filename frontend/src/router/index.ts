import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/user'

NProgress.configure({ showSpinner: false })

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'news',
        name: 'News',
        component: () => import('@/views/news/index.vue'),
        meta: { title: '资讯中心' }
      },
      {
        path: 'news/:id',
        name: 'NewsDetail',
        component: () => import('@/views/news/detail.vue'),
        meta: { title: '资讯详情' }
      },
      {
        path: 'case',
        name: 'Case',
        component: () => import('@/views/case/index.vue'),
        meta: { title: '案例展示' }
      },
      {
        path: 'case/:id',
        name: 'CaseDetail',
        component: () => import('@/views/case/detail.vue'),
        meta: { title: '案例详情' }
      },
      {
        path: 'challenge',
        name: 'Challenge',
        component: () => import('@/views/challenge/index.vue'),
        meta: { title: '知识闯关' }
      },
      {
        path: 'challenge/scenario/:id',
        name: 'ChallengeScenario',
        component: () => import('@/views/challenge/scenario.vue'),
        meta: { title: '情景模拟' }
      },
      {
        path: 'challenge/agent/:id',
        name: 'ChallengeAgent',
        component: () => import('@/views/challenge/agent.vue'),
        meta: { title: 'Agent模拟挑战' }
      },
      {
        path: 'challenge/:id',
        name: 'ChallengeDetail',
        component: () => import('@/views/challenge/detail.vue'),
        meta: { title: '闯关详情' }
      },
      {
        path: 'ranking',
        name: 'Ranking',
        component: () => import('@/views/challenge/ranking.vue'),
        meta: { title: '排行榜' }
      },
      {
        path: 'forum',
        name: 'Forum',
        component: () => import('@/views/forum/index.vue'),
        meta: { title: '社区' }
      },
      {
        path: 'forum/:id',
        name: 'ForumDetail',
        component: () => import('@/views/forum/detail.vue'),
        meta: { title: '帖子详情' }
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/chat/index.vue'),
        meta: { title: '智能客服' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'score',
        name: 'Score',
        component: () => import('@/views/score/index.vue'),
        meta: { title: '积分中心' }
      },
      {
        path: 'achievement',
        name: 'Achievement',
        component: () => import('@/views/achievement/index.vue'),
        meta: { title: '我的成就' }
      },
      {
        path: 'recommend',
        name: 'Recommend',
        component: () => import('@/views/recommend/index.vue'),
        meta: { title: '智能推荐' }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layout/admin.vue'),
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true, isAdmin: true },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/dashboard.vue'),
        meta: { title: '数据看板' }
      },
      {
        path: 'user',
        name: 'AdminUser',
        component: () => import('@/views/admin/user.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'news',
        name: 'AdminNews',
        component: () => import('@/views/admin/news.vue'),
        meta: { title: '资讯管理' }
      },
      {
        path: 'case',
        name: 'AdminCase',
        component: () => import('@/views/admin/case.vue'),
        meta: { title: '案例管理' }
      },
      {
        path: 'challenge',
        name: 'AdminChallenge',
        component: () => import('@/views/admin/challenge.vue'),
        meta: { title: '关卡管理' }
      },
      {
        path: 'forum',
        name: 'AdminForum',
        component: () => import('@/views/admin/forum.vue'),
        meta: { title: '帖子管理' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, _from, next) => {
  NProgress.start()

  document.title = `${to.meta.title || '反诈骗学习平台'} - 反诈骗学习平台`

  const userStore = useUserStore()

  if (to.meta.requiresAuth !== false) {
    if (!userStore.token && !localStorage.getItem('token')) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }

    if (!userStore.userInfo) {
      const info = await userStore.getUserInfo()
      if (!info) {
        next({ name: 'Login', query: { redirect: to.fullPath } })
        return
      }
    }

    if (to.meta.isAdmin && !userStore.isAdmin) {
      next({ name: 'Home' })
      return
    }
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router
