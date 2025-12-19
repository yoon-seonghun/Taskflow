<script setup lang="ts">
/**
 * 설정 뷰 (Settings View)
 * - 애플리케이션 설정
 * - 사용자 환경설정
 * - localStorage 기반 클라이언트 측 저장
 */
import { ref, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const authStore = useAuthStore()
const uiStore = useUiStore()

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

// 컴포넌트 마운트 시 설정 로드
onMounted(() => {
  loadSettings()
})
</script>

<template>
  <div class="settings-view">
    <!-- 헤더 -->
    <div class="mb-6">
      <h1 class="text-xl font-semibold text-gray-900">설정</h1>
      <p class="mt-1 text-sm text-gray-500">
        애플리케이션 설정을 관리합니다.
      </p>
    </div>

    <div class="max-w-2xl space-y-6">
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
              <p class="font-medium text-gray-900">{{ authStore.user?.userName || '사용자' }}</p>
              <p class="text-sm text-gray-500">{{ authStore.user?.loginId || '-' }}</p>
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
  </div>
</template>

<style scoped>
.settings-view {
  @apply max-w-7xl mx-auto;
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
</style>
