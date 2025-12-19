import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { setupRouteGuards } from './guards'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: {
      requiresAuth: false,
      title: '로그인'
    }
  },
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'Tasks',
        component: () => import('@/views/TasksView.vue'),
        meta: { title: '업무 목록' }
      },
      {
        path: 'overdue',
        name: 'Overdue',
        component: () => import('@/views/OverdueView.vue'),
        meta: { title: '지연 업무' }
      },
      {
        path: 'pending',
        name: 'Pending',
        component: () => import('@/views/PendingView.vue'),
        meta: { title: '보류 업무' }
      },
      {
        path: 'completed',
        name: 'Completed',
        component: () => import('@/views/CompletedView.vue'),
        meta: { title: '완료된 작업' }
      },
      {
        path: 'deleted',
        name: 'Deleted',
        component: () => import('@/views/DeletedTasksView.vue'),
        meta: { title: '삭제된 작업' }
      },
      {
        path: 'templates',
        name: 'Templates',
        component: () => import('@/views/TemplatesView.vue'),
        meta: { title: '작업 등록' }
      },
      {
        path: 'history',
        name: 'History',
        component: () => import('@/views/HistoryView.vue'),
        meta: { title: '이력 관리' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/UsersView.vue'),
        meta: { title: '사용자 관리' }
      },
      {
        path: 'shares',
        name: 'Shares',
        component: () => import('@/views/ShareUsersView.vue'),
        meta: { title: '공유 사용자 관리' }
      },
      {
        path: 'departments',
        name: 'Departments',
        component: () => import('@/views/DepartmentsView.vue'),
        meta: { title: '부서 관리' }
      },
      {
        path: 'groups',
        name: 'Groups',
        component: () => import('@/views/GroupsView.vue'),
        meta: { title: '그룹 관리' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/SettingsView.vue'),
        meta: { title: '설정' }
      },
      {
        path: 'boards/:boardId/items/:itemId',
        name: 'ItemDetail',
        component: () => import('@/views/ItemDetailView.vue'),
        meta: { title: '업무 상세' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: {
      requiresAuth: false,
      title: '페이지를 찾을 수 없음'
    }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    // 뒤로가기 시 이전 스크롤 위치로 복원
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

// 라우트 가드 설정
setupRouteGuards(router)

export default router
