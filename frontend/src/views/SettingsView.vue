<script setup lang="ts">
/**
 * 설정 뷰 (Settings View)
 * - 환경설정
 * - 사용자 관리
 * - 부서 관리
 * - 직급 관리 (Internal: CRUD, External: readonly)
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { useConfigStore } from '@/stores/config'
import { usePositionStore } from '@/stores/position'
import type { Position } from '@/types/position'

// 하위 탭 컴포넌트 (별도 뷰 파일을 임베드)
import UsersContent from '@/components/settings/UsersContent.vue'
import DepartmentsContent from '@/components/settings/DepartmentsContent.vue'
import CategoryContent from '@/components/settings/CategoryContent.vue'
import PropertiesContent from '@/components/settings/PropertiesContent.vue'
import ExternalDatasourceContent from '@/components/settings/ExternalDatasourceContent.vue'
import ExternalQueryContent from '@/components/settings/ExternalQueryContent.vue'
import SmtpSettingsContent from '@/components/settings/SmtpSettingsContent.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()
const configStore = useConfigStore()
const positionStore = usePositionStore()

// 탭 타입
type SettingsTab = 'settings' | 'users' | 'departments' | 'positions' | 'categories' | 'properties' | 'external-datasources' | 'external-queries' | 'smtp'

// 현재 활성 탭
const activeTab = ref<SettingsTab>('settings')

// External 모드 여부
const isExternalMode = computed(() => configStore.isExternalMode)
const positionCrudEnabled = computed(() => configStore.positionCrudEnabled)

// 관리자 여부
const isAdmin = computed(() => authStore.user?.role === 'ADMIN')

// 탭 목록 (역할별 접근 권한)
// - ADMIN: 모든 메뉴
// - MANAGER/USER: 환경설정, 카테고리 관리, 속성 관리, 외부 쿼리
const tabs = computed(() => {
  const allTabs: { id: SettingsTab; label: string; icon: string; adminOnly?: boolean }[] = [
    { id: 'settings', label: '환경설정', icon: 'cog' },
    { id: 'users', label: '사용자 관리', icon: 'user', adminOnly: true },
    { id: 'departments', label: '부서 관리', icon: 'building', adminOnly: true },
    { id: 'positions', label: '직급 관리', icon: 'badge', adminOnly: true },
    { id: 'categories', label: '카테고리 관리', icon: 'tag' },
    { id: 'properties', label: '속성 관리', icon: 'grid' },
    { id: 'external-datasources', label: '외부 DB', icon: 'database', adminOnly: true },
    { id: 'external-queries', label: '외부 쿼리', icon: 'code' },
    { id: 'smtp', label: 'SMTP 설정', icon: 'mail', adminOnly: true }
  ]

  // 관리자가 아니면 adminOnly 탭 제외
  if (!isAdmin.value) {
    return allTabs.filter(tab => !tab.adminOnly)
  }
  return allTabs
})

// 접근 가능한 탭 ID 목록
const accessibleTabIds = computed(() => tabs.value.map(tab => tab.id))

// URL 쿼리로 탭 초기화 및 변경 감지 (권한 체크 포함)
watch(() => route.query.tab, (newTab) => {
  if (newTab && typeof newTab === 'string') {
    const tabId = newTab as SettingsTab
    // 접근 가능한 탭인지 확인
    if (accessibleTabIds.value.includes(tabId)) {
      activeTab.value = tabId
    } else {
      // 권한이 없으면 기본 탭(환경설정)으로
      activeTab.value = 'settings'
      router.replace({ query: { tab: 'settings' } })
    }
  }
}, { immediate: true })

// 탭 변경 시 URL 업데이트
function changeTab(tab: SettingsTab) {
  activeTab.value = tab
  router.replace({ query: { tab } })
}

// 설정 키
const SETTINGS_KEY = 'taskflow_settings'

// 설정 인터페이스
interface AppSettings {
  theme: string
  notificationsEnabled: boolean
  language: string
}

// 기본 설정
const defaultSettings: AppSettings = {
  theme: 'light',
  notificationsEnabled: true,
  language: 'ko'
}

// 설정 상태
const theme = ref('light')
const notificationsEnabled = ref(true)
const language = ref('ko')

// 테마 옵션
const themeOptions = [
  { value: 'light', label: '라이트 모드' },
  { value: 'dark', label: '다크 모드' },
  { value: 'system', label: '시스템 설정 따름' }
]

// 언어 옵션
const languageOptions = [
  { value: 'ko', label: '한국어' },
  { value: 'en', label: 'English' }
]

// === 직급 관리 상태 ===
const positions = computed(() => positionStore.positions)
const positionLoading = computed(() => positionStore.loading)

// 직급 편집 상태
const editingPosition = ref<Position | null>(null)
const newPosition = ref({
  positionCode: '',
  positionName: '',
  sortOrder: 0
})
const showAddForm = ref(false)

// 설정 로드
function loadSettings() {
  try {
    const saved = localStorage.getItem(SETTINGS_KEY)
    if (saved) {
      const settings: AppSettings = JSON.parse(saved)
      theme.value = settings.theme || defaultSettings.theme
      notificationsEnabled.value = settings.notificationsEnabled ?? defaultSettings.notificationsEnabled
      language.value = settings.language || defaultSettings.language
      applyTheme(theme.value)
    }
  } catch (error) {
    console.error('Failed to load settings:', error)
  }
}

// 테마 적용
function applyTheme(themeValue: string) {
  const root = document.documentElement
  if (themeValue === 'dark') {
    root.classList.add('dark')
  } else if (themeValue === 'light') {
    root.classList.remove('dark')
  } else {
    // system: 시스템 다크모드 설정 따름
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
    if (prefersDark) {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }
  }
}

// 설정 저장
function saveSettings() {
  try {
    const settings: AppSettings = {
      theme: theme.value,
      notificationsEnabled: notificationsEnabled.value,
      language: language.value
    }
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings))
    applyTheme(theme.value)
    uiStore.showSuccess('설정이 저장되었습니다.')
  } catch (error) {
    console.error('Failed to save settings:', error)
    uiStore.showError('설정 저장에 실패했습니다.')
  }
}

// 테마 변경 시 즉시 적용
watch(theme, (newTheme) => {
  applyTheme(newTheme)
})

// === 직급 관리 함수 ===

// 직급 추가 폼 표시
function handleShowAddForm() {
  showAddForm.value = true
  // 기본 정렬 순서는 마지막 + 1
  const maxSort = positions.value.length > 0
    ? Math.max(...positions.value.map(p => p.sortOrder))
    : 0
  newPosition.value = {
    positionCode: '',
    positionName: '',
    sortOrder: maxSort + 1
  }
}

// 직급 추가 취소
function handleCancelAdd() {
  showAddForm.value = false
  newPosition.value = { positionCode: '', positionName: '', sortOrder: 0 }
}

// 직급 추가
async function handleAddPosition() {
  if (!newPosition.value.positionCode.trim()) {
    uiStore.showWarning('직급 코드를 입력해주세요.')
    return
  }
  if (!newPosition.value.positionName.trim()) {
    uiStore.showWarning('직급명을 입력해주세요.')
    return
  }

  // 직급 코드는 대문자로 변환
  const positionCode = newPosition.value.positionCode.trim().toUpperCase()

  const result = await positionStore.createPosition({
    positionCode: positionCode,
    positionName: newPosition.value.positionName.trim(),
    sortOrder: newPosition.value.sortOrder
  })

  if (result) {
    uiStore.showSuccess('직급이 추가되었습니다.')
    handleCancelAdd()
  } else {
    uiStore.showError(positionStore.error || '직급 추가에 실패했습니다.')
  }
}

// 직급 편집 시작
function handleEditPosition(position: Position) {
  editingPosition.value = { ...position }
}

// 직급 편집 취소
function handleCancelEdit() {
  editingPosition.value = null
}

// 직급 저장
async function handleSavePosition() {
  if (!editingPosition.value) return

  if (!editingPosition.value.positionName.trim()) {
    uiStore.showWarning('직급명을 입력해주세요.')
    return
  }

  const success = await positionStore.updatePosition(editingPosition.value.positionCode, {
    positionName: editingPosition.value.positionName.trim(),
    sortOrder: editingPosition.value.sortOrder,
    useYn: editingPosition.value.useYn
  })

  if (success) {
    uiStore.showSuccess('직급이 수정되었습니다.')
    editingPosition.value = null
  }
}

// 직급 삭제
async function handleDeletePosition(position: Position) {
  if (!confirm(`'${position.positionName}' 직급을 삭제하시겠습니까?\n이 직급을 사용 중인 사용자가 있으면 삭제할 수 없습니다.`)) {
    return
  }

  const success = await positionStore.deletePosition(position.positionCode)
  if (success) {
    uiStore.showSuccess('직급이 삭제되었습니다.')
  }
}

// 직급 순서 위로
async function handleMoveUp(position: Position, index: number) {
  if (index === 0) return
  const prevPosition = positions.value[index - 1]

  // 순서 교환
  await positionStore.updatePosition(position.positionCode, { sortOrder: prevPosition.sortOrder })
  await positionStore.updatePosition(prevPosition.positionCode, { sortOrder: position.sortOrder })
  await positionStore.fetchPositions()
}

// 직급 순서 아래로
async function handleMoveDown(position: Position, index: number) {
  if (index === positions.value.length - 1) return
  const nextPosition = positions.value[index + 1]

  // 순서 교환
  await positionStore.updatePosition(position.positionCode, { sortOrder: nextPosition.sortOrder })
  await positionStore.updatePosition(nextPosition.positionCode, { sortOrder: position.sortOrder })
  await positionStore.fetchPositions()
}

// 컴포넌트 마운트 시 설정 로드
onMounted(async () => {
  loadSettings()
  // 직급 목록 로드 (Internal: CRUD, External: readonly)
  await positionStore.fetchPositions()
})
</script>

<template>
  <div class="settings-view">
    <!-- 헤더 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">설정</h1>
      <p class="mt-1 text-sm text-gray-500">
        시스템 설정을 관리합니다.
      </p>
    </div>

    <!-- 탭 네비게이션 -->
    <div class="mb-6 border-b border-gray-200">
      <nav class="flex gap-1">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="tab-btn"
          :class="{ 'active': activeTab === tab.id }"
          @click="changeTab(tab.id)"
        >
          <!-- Icon -->
          <span class="tab-icon">
            <svg v-if="tab.icon === 'cog'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <svg v-else-if="tab.icon === 'user'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            <svg v-else-if="tab.icon === 'building'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
            <svg v-else-if="tab.icon === 'badge'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z" />
            </svg>
            <svg v-else-if="tab.icon === 'tag'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
            </svg>
            <svg v-else-if="tab.icon === 'grid'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
            </svg>
            <svg v-else-if="tab.icon === 'database'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
            </svg>
            <svg v-else-if="tab.icon === 'code'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
            </svg>
            <svg v-else-if="tab.icon === 'mail'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
          </span>
          <span>{{ tab.label }}</span>
        </button>
      </nav>
    </div>

    <!-- 환경설정 탭 -->
    <div v-if="activeTab === 'settings'" class="max-w-2xl space-y-6">
      <!-- 사용자 정보 -->
      <div class="settings-card">
        <h2 class="settings-card-title">사용자 정보</h2>
        <div class="settings-card-content">
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-full bg-primary-100 flex items-center justify-center">
              <span class="text-lg font-medium text-primary-600">
                {{ authStore.user?.userName?.charAt(0) || 'U' }}
              </span>
            </div>
            <div>
              <p class="font-medium text-gray-900">
                {{ authStore.user?.userName || '사용자' }}
                <span v-if="authStore.user?.positionName || authStore.user?.departmentName" class="text-gray-400 font-normal">
                  ｜
                  <span v-if="authStore.user?.positionName" class="text-gray-600">{{ authStore.user.positionName }}</span>
                  <span v-if="authStore.user?.positionName && authStore.user?.departmentName" class="text-gray-400"> · </span>
                  <span v-if="authStore.user?.departmentName" class="text-gray-600">{{ authStore.user.departmentName }}</span>
                </span>
              </p>
              <p class="text-sm text-gray-500">{{ authStore.user?.username || '-' }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 테마 설정 -->
      <div class="settings-card">
        <h2 class="settings-card-title">테마</h2>
        <div class="settings-card-content">
          <div class="space-y-3">
            <label v-for="option in themeOptions" :key="option.value" class="flex items-center gap-3 cursor-pointer">
              <input
                type="radio"
                v-model="theme"
                :value="option.value"
                class="w-4 h-4 text-primary-600 border-gray-300 focus:ring-primary-500"
              />
              <span class="text-sm text-gray-700">{{ option.label }}</span>
            </label>
          </div>
        </div>
      </div>

      <!-- 알림 설정 -->
      <div class="settings-card">
        <h2 class="settings-card-title">알림</h2>
        <div class="settings-card-content">
          <label class="flex items-center justify-between cursor-pointer">
            <span class="text-sm text-gray-700">알림 활성화</span>
            <input
              type="checkbox"
              v-model="notificationsEnabled"
              class="w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500"
            />
          </label>
        </div>
      </div>

      <!-- 언어 설정 -->
      <div class="settings-card">
        <h2 class="settings-card-title">언어</h2>
        <div class="settings-card-content">
          <select
            v-model="language"
            class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:ring-primary-500 focus:border-primary-500"
          >
            <option v-for="option in languageOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>
      </div>

      <!-- 저장 버튼 -->
      <div class="flex justify-end">
        <button
          type="button"
          class="btn-primary"
          @click="saveSettings"
        >
          설정 저장
        </button>
      </div>
    </div>

    <!-- 사용자 관리 탭 -->
    <div v-else-if="activeTab === 'users'">
      <UsersContent />
    </div>

    <!-- 부서 관리 탭 -->
    <div v-else-if="activeTab === 'departments'">
      <DepartmentsContent />
    </div>

    <!-- 카테고리 관리 탭 -->
    <div v-else-if="activeTab === 'categories'">
      <CategoryContent />
    </div>

    <!-- 속성 관리 탭 -->
    <div v-else-if="activeTab === 'properties'">
      <PropertiesContent />
    </div>

    <!-- 외부 DB 관리 탭 -->
    <div v-else-if="activeTab === 'external-datasources'">
      <ExternalDatasourceContent />
    </div>

    <!-- 외부 쿼리 관리 탭 -->
    <div v-else-if="activeTab === 'external-queries'">
      <ExternalQueryContent />
    </div>

    <!-- SMTP 설정 탭 -->
    <div v-else-if="activeTab === 'smtp'">
      <SmtpSettingsContent />
    </div>

    <!-- 직급 관리 탭 -->
    <div v-else-if="activeTab === 'positions'" class="max-w-3xl">
      <div class="settings-card">
        <div class="settings-card-title flex items-center justify-between">
          <span>직급 목록</span>
          <button
            v-if="positionCrudEnabled && !showAddForm"
            type="button"
            class="btn-add"
            @click="handleShowAddForm"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            직급 추가
          </button>
        </div>

        <div class="settings-card-content">
          <!-- 로딩 상태 -->
          <div v-if="positionLoading" class="py-8 text-center">
            <svg class="animate-spin h-8 w-8 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
            <p class="mt-2 text-sm text-gray-500">직급 목록을 불러오는 중...</p>
          </div>

          <!-- 직급 추가 폼 -->
          <div v-if="showAddForm" class="mb-4 p-4 bg-gray-50 rounded-lg border border-gray-200">
            <h4 class="text-sm font-medium text-gray-900 mb-3">새 직급 추가</h4>
            <div class="grid grid-cols-3 gap-3">
              <div>
                <label class="block text-xs font-medium text-gray-500 mb-1">직급 코드</label>
                <input
                  v-model="newPosition.positionCode"
                  type="text"
                  class="form-input"
                  placeholder="예: STAFF"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-gray-500 mb-1">직급명</label>
                <input
                  v-model="newPosition.positionName"
                  type="text"
                  class="form-input"
                  placeholder="예: 사원"
                />
              </div>
              <div>
                <label class="block text-xs font-medium text-gray-500 mb-1">정렬 순서</label>
                <input
                  v-model.number="newPosition.sortOrder"
                  type="number"
                  min="0"
                  class="form-input"
                />
              </div>
            </div>
            <div class="mt-3 flex justify-end gap-2">
              <button type="button" class="btn-cancel" @click="handleCancelAdd">
                취소
              </button>
              <button type="button" class="btn-primary" @click="handleAddPosition">
                추가
              </button>
            </div>
          </div>

          <!-- 직급 목록 테이블 -->
          <table v-if="!positionLoading && positions.length > 0" class="position-table">
            <thead>
              <tr>
                <th class="w-[100px]">순서</th>
                <th class="w-[150px]">직급 코드</th>
                <th>직급명</th>
                <th class="w-[80px]">상태</th>
                <th v-if="positionCrudEnabled" class="w-[150px]">관리</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(position, index) in positions" :key="position.positionCode">
                <!-- 편집 모드 -->
                <template v-if="editingPosition?.positionCode === position.positionCode">
                  <td>
                    <input
                      v-model.number="editingPosition.sortOrder"
                      type="number"
                      min="0"
                      class="form-input w-16"
                    />
                  </td>
                  <td class="text-gray-500">
                    {{ position.positionCode }}
                  </td>
                  <td>
                    <input
                      v-model="editingPosition.positionName"
                      type="text"
                      class="form-input"
                    />
                  </td>
                  <td>
                    <select v-model="editingPosition.useYn" class="form-input">
                      <option value="Y">활성</option>
                      <option value="N">비활성</option>
                    </select>
                  </td>
                  <td>
                    <div class="flex items-center gap-1">
                      <button type="button" class="icon-btn text-green-600" title="저장" @click="handleSavePosition">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                        </svg>
                      </button>
                      <button type="button" class="icon-btn text-gray-400" title="취소" @click="handleCancelEdit">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                    </div>
                  </td>
                </template>

                <!-- 조회 모드 -->
                <template v-else>
                  <td>
                    <div class="flex items-center gap-1">
                      <span class="text-gray-600">{{ position.sortOrder }}</span>
                      <div v-if="positionCrudEnabled" class="flex flex-col">
                        <button
                          type="button"
                          class="order-btn"
                          :disabled="index === 0"
                          @click="handleMoveUp(position, index)"
                        >
                          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7" />
                          </svg>
                        </button>
                        <button
                          type="button"
                          class="order-btn"
                          :disabled="index === positions.length - 1"
                          @click="handleMoveDown(position, index)"
                        >
                          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                          </svg>
                        </button>
                      </div>
                    </div>
                  </td>
                  <td class="font-mono text-sm text-gray-500">
                    {{ position.positionCode }}
                  </td>
                  <td class="font-medium text-gray-900">
                    {{ position.positionName }}
                  </td>
                  <td>
                    <span
                      class="status-badge"
                      :class="position.useYn === 'Y' ? 'active' : 'inactive'"
                    >
                      {{ position.useYn === 'Y' ? '활성' : '비활성' }}
                    </span>
                  </td>
                  <td v-if="positionCrudEnabled">
                    <div class="flex items-center gap-1">
                      <button type="button" class="icon-btn" title="수정" @click="handleEditPosition(position)">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                        </svg>
                      </button>
                      <button type="button" class="icon-btn text-red-500" title="삭제" @click="handleDeletePosition(position)">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                        </svg>
                      </button>
                    </div>
                  </td>
                </template>
              </tr>
            </tbody>
          </table>

          <!-- 빈 상태 -->
          <div v-else-if="!positionLoading && positions.length === 0" class="py-8 text-center">
            <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
            <p class="mt-2 text-sm text-gray-500">등록된 직급이 없습니다.</p>
            <button
              v-if="positionCrudEnabled"
              type="button"
              class="mt-3 text-sm text-primary-600 hover:text-primary-700"
              @click="handleShowAddForm"
            >
              + 첫 번째 직급 추가하기
            </button>
          </div>

          <!-- External 모드 안내 -->
          <div v-if="isExternalMode" class="mt-4 p-3 bg-blue-50 rounded-lg">
            <p class="text-sm text-blue-700">
              <svg class="inline w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              External 모드에서는 직급 정보가 외부 시스템에서 동기화됩니다.
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings-view {
  @apply max-w-7xl mx-auto;
}

.tab-btn {
  @apply inline-flex items-center gap-2 px-4 py-3 text-sm font-medium text-gray-500 border-b-2 border-transparent
         hover:text-gray-700 hover:border-gray-300 transition-colors;
}

.tab-btn.active {
  @apply text-primary-600 border-primary-600;
}

.tab-icon {
  @apply flex-shrink-0;
}

.settings-card {
  @apply bg-white rounded-lg border border-gray-200 overflow-hidden;
}

.settings-card-title {
  @apply px-4 py-3 text-base font-medium text-gray-900 bg-gray-50 border-b border-gray-200;
}

.settings-card-content {
  @apply p-4;
}

.btn-primary {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-white bg-primary-600 rounded-lg
         hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors duration-150;
}

.btn-cancel {
  @apply inline-flex items-center justify-center px-4 py-2 text-sm font-medium
         text-gray-700 bg-white border border-gray-300 rounded-lg
         hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
         transition-colors duration-150;
}

.btn-add {
  @apply inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium
         text-primary-600 bg-primary-50 rounded-md
         hover:bg-primary-100 transition-colors;
}

.form-input {
  @apply w-full px-3 py-1.5 text-sm border border-gray-300 rounded-md
         focus:ring-primary-500 focus:border-primary-500;
}

/* 직급 테이블 */
.position-table {
  @apply w-full text-sm;
}

.position-table thead {
  @apply bg-gray-50 border-y border-gray-200;
}

.position-table th {
  @apply px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider;
}

.position-table tbody {
  @apply divide-y divide-gray-100;
}

.position-table td {
  @apply px-4 py-3 whitespace-nowrap;
}

.position-table tbody tr {
  @apply hover:bg-gray-50 transition-colors;
}

.status-badge {
  @apply inline-flex items-center px-2 py-0.5 text-xs font-medium rounded-full;
}

.status-badge.active {
  @apply bg-green-100 text-green-700;
}

.status-badge.inactive {
  @apply bg-gray-100 text-gray-600;
}

.icon-btn {
  @apply p-1.5 rounded hover:bg-gray-100 text-gray-500 transition-colors;
}

.order-btn {
  @apply p-0.5 text-gray-400 hover:text-gray-600 disabled:opacity-30 disabled:cursor-not-allowed;
}
</style>
