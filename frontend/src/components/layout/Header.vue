<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

defineProps<{
  isMobile?: boolean
}>()

const emit = defineEmits<{
  (e: 'toggle-sidebar'): void
}>()

const authStore = useAuthStore()
const router = useRouter()

async function handleLogout() {
  authStore.clearAuth()
  router.push({ name: 'Login' })
}
</script>

<template>
  <header class="fixed top-0 left-0 right-0 h-14 bg-white border-b border-gray-200 z-50">
    <div class="flex items-center justify-between h-full px-4">
      <div class="flex items-center gap-4">
        <!-- 햄버거 메뉴: 모바일에서만 표시 -->
        <button
          class="p-2 hover:bg-gray-100 rounded-lg md:hidden"
          @click="emit('toggle-sidebar')"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>
        <h1 class="text-lg font-semibold text-gray-900">TaskFlow</h1>
      </div>
      <div class="flex items-center gap-4">
        <span class="text-sm text-gray-600">{{ authStore.user?.name }}</span>
        <button
          class="text-sm text-gray-500 hover:text-gray-700"
          @click="handleLogout"
        >
          로그아웃
        </button>
      </div>
    </div>
  </header>
</template>
