<script setup lang="ts">
/**
 * 컨텍스트 메뉴 컴포넌트 (⋮ 버튼)
 * Compact UI: font 13px
 */
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'

export interface MenuItem {
  id: string
  label: string
  icon?: string
  disabled?: boolean
  danger?: boolean
  divider?: boolean
}

interface Props {
  items: MenuItem[]
  disabled?: boolean
  position?: 'left' | 'right'
  triggerIcon?: 'dots' | 'chevron'
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  position: 'right',
  triggerIcon: 'dots'
})

const emit = defineEmits<{
  (e: 'select', item: MenuItem): void
}>()

const isOpen = ref(false)
const containerRef = ref<HTMLElement | null>(null)
const menuRef = ref<HTMLElement | null>(null)
const menuPosition = ref({ top: 0, left: 0 })

// 메뉴 위치 계산
function calculatePosition() {
  if (!containerRef.value || !menuRef.value) return

  const triggerRect = containerRef.value.getBoundingClientRect()
  const menuRect = menuRef.value.getBoundingClientRect()
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight

  let top = triggerRect.bottom + 4
  let left = props.position === 'right'
    ? triggerRect.right - menuRect.width
    : triggerRect.left

  // 화면 아래로 넘어가면 위로 표시
  if (top + menuRect.height > viewportHeight - 8) {
    top = triggerRect.top - menuRect.height - 4
  }

  // 화면 오른쪽으로 넘어가면 왼쪽으로 이동
  if (left + menuRect.width > viewportWidth - 8) {
    left = viewportWidth - menuRect.width - 8
  }

  // 화면 왼쪽으로 넘어가면 오른쪽으로 이동
  if (left < 8) {
    left = 8
  }

  menuPosition.value = { top, left }
}

function toggleMenu() {
  if (props.disabled) return
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    nextTick(() => {
      calculatePosition()
    })
  }
}

function closeMenu() {
  isOpen.value = false
}

function handleSelect(item: MenuItem) {
  if (item.disabled || item.divider) return
  emit('select', item)
  closeMenu()
}

// 외부 클릭 감지
function handleClickOutside(event: MouseEvent) {
  if (containerRef.value && !containerRef.value.contains(event.target as Node)) {
    closeMenu()
  }
}

// 스크롤 시 메뉴 닫기
function handleScroll() {
  if (isOpen.value) {
    closeMenu()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('scroll', handleScroll, true)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('scroll', handleScroll, true)
})

// 아이콘 경로
const iconPaths: Record<string, string> = {
  edit: 'M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z',
  delete: 'M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16',
  copy: 'M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z',
  move: 'M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4',
  archive: 'M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4',
  eye: 'M15 12a3 3 0 11-6 0 3 3 0 016 0z M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z',
  'eye-off': 'M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21',
  plus: 'M12 4v16m8-8H4',
  check: 'M5 13l4 4L19 7',
  x: 'M6 18L18 6M6 6l12 12',
  refresh: 'M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15',
  settings: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z'
}

function getIconPath(icon: string): string {
  return iconPaths[icon] || iconPaths.settings
}
</script>

<template>
  <div ref="containerRef" class="relative inline-block">
    <!-- Trigger Button -->
    <button
      type="button"
      class="p-1 rounded hover:bg-gray-100 text-gray-500 hover:text-gray-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      :disabled="disabled"
      @click.stop="toggleMenu"
    >
      <!-- Dots Icon -->
      <svg
        v-if="triggerIcon === 'dots'"
        class="w-5 h-5"
        fill="currentColor"
        viewBox="0 0 24 24"
      >
        <circle cx="12" cy="6" r="1.5" />
        <circle cx="12" cy="12" r="1.5" />
        <circle cx="12" cy="18" r="1.5" />
      </svg>
      <!-- Chevron Icon -->
      <svg
        v-else
        class="w-4 h-4"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
      </svg>
    </button>

    <!-- Menu Dropdown -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-100 ease-out"
        enter-from-class="opacity-0 scale-95"
        enter-to-class="opacity-100 scale-100"
        leave-active-class="transition duration-75 ease-in"
        leave-from-class="opacity-100 scale-100"
        leave-to-class="opacity-0 scale-95"
      >
        <div
          v-if="isOpen"
          ref="menuRef"
          class="fixed z-50 bg-white rounded-lg shadow-lg border border-gray-200 py-1 min-w-[160px]"
          :style="{ top: `${menuPosition.top}px`, left: `${menuPosition.left}px` }"
        >
          <template v-for="item in items" :key="item.id">
            <!-- Divider -->
            <div v-if="item.divider" class="my-1 border-t border-gray-200" />

            <!-- Menu Item -->
            <button
              v-else
              type="button"
              class="w-full px-3 py-2 text-left text-[13px] flex items-center gap-2 transition-colors"
              :class="[
                item.disabled
                  ? 'text-gray-400 cursor-not-allowed'
                  : item.danger
                    ? 'text-red-600 hover:bg-red-50'
                    : 'text-gray-700 hover:bg-gray-100'
              ]"
              :disabled="item.disabled"
              @click="handleSelect(item)"
            >
              <!-- Icon -->
              <svg
                v-if="item.icon"
                class="w-4 h-4 flex-shrink-0"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  :d="getIconPath(item.icon)"
                />
              </svg>
              <span>{{ item.label }}</span>
            </button>
          </template>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
