<script setup lang="ts">
/**
 * 토스트 알림 컴포넌트
 * Compact UI: font 13px
 */
import { computed } from 'vue'
import { useUiStore, type Toast, type ToastType } from '@/stores/ui'

const uiStore = useUiStore()

const toasts = computed(() => uiStore.toasts)

// 타입별 아이콘과 스타일
const toastConfig: Record<ToastType, { icon: string; class: string }> = {
  success: {
    icon: 'M5 13l4 4L19 7',
    class: 'bg-green-600 text-white'
  },
  error: {
    icon: 'M6 18L18 6M6 6l12 12',
    class: 'bg-red-600 text-white'
  },
  warning: {
    icon: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z',
    class: 'bg-yellow-500 text-white'
  },
  info: {
    icon: 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
    class: 'bg-blue-600 text-white'
  }
}

function removeToast(id: string) {
  uiStore.removeToast(id)
}

function getConfig(type: ToastType) {
  return toastConfig[type] || toastConfig.info
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed bottom-4 right-4 z-toast flex flex-col gap-2 pointer-events-none">
      <TransitionGroup
        enter-active-class="transition duration-300 ease-out"
        enter-from-class="opacity-0 translate-x-full"
        enter-to-class="opacity-100 translate-x-0"
        leave-active-class="transition duration-200 ease-in"
        leave-from-class="opacity-100 translate-x-0"
        leave-to-class="opacity-0 translate-x-full"
        move-class="transition duration-300 ease-out"
      >
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="flex items-start gap-3 px-4 py-3 rounded-lg shadow-lg min-w-[280px] max-w-md pointer-events-auto"
          :class="getConfig(toast.type).class"
        >
          <!-- Icon -->
          <svg
            class="w-5 h-5 flex-shrink-0 mt-0.5"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              :d="getConfig(toast.type).icon"
            />
          </svg>

          <!-- Message -->
          <p class="flex-1 text-[13px] leading-5">
            {{ toast.message }}
          </p>

          <!-- Close Button -->
          <button
            type="button"
            class="flex-shrink-0 p-0.5 hover:bg-white/20 rounded transition-colors"
            @click="removeToast(toast.id)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>
