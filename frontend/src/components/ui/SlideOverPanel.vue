<script setup lang="ts">
/**
 * 슬라이드오버 패널 컴포넌트
 * - 우측에서 슬라이드되어 나오는 패널
 * - 좌측 가장자리 드래그로 폭 조절 가능
 * - ESC 키 또는 외부 클릭으로 닫기
 */
import { ref, computed, watch, onMounted, onUnmounted, defineAsyncComponent } from 'vue'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()

// 동적 컴포넌트 등록
const dynamicComponents: Record<string, ReturnType<typeof defineAsyncComponent>> = {
  ItemDetail: defineAsyncComponent(() => import('@/components/item/ItemDetailPanel.vue')),
  ItemDetailPanel: defineAsyncComponent(() => import('@/components/item/ItemDetailPanel.vue')),
  ItemEdit: defineAsyncComponent(() => import('@/components/item/ItemDetailPanel.vue')),
  ItemCreate: defineAsyncComponent(() => import('@/components/item/ItemDetailPanel.vue'))
}

// 패널 상태
const isVisible = computed(() => uiStore.slideOverPanel.visible)
const componentName = computed(() => uiStore.slideOverPanel.component)
const resolvedComponent = computed(() => {
  return componentName.value ? dynamicComponents[componentName.value] : null
})
const componentProps = computed(() => uiStore.slideOverPanel.props)

// 리사이즈 관련 상태
const STORAGE_KEY = 'taskflow_panel_width'
const MIN_WIDTH = 700
const MAX_WIDTH_PERCENT = 0.92
const DEFAULT_WIDTH = 1200

const panelWidth = ref(DEFAULT_WIDTH)
const isResizing = ref(false)

// 저장된 폭 불러오기
function loadSavedWidth() {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    const width = parseInt(saved, 10)
    if (!isNaN(width) && width >= MIN_WIDTH) {
      panelWidth.value = Math.min(width, window.innerWidth * MAX_WIDTH_PERCENT)
    }
  }
}

// 폭 저장
function saveWidth() {
  localStorage.setItem(STORAGE_KEY, panelWidth.value.toString())
}

// 패널 닫기
function closePanel() {
  uiStore.closeSlideOver()
}

// ESC 키 핸들러
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && isVisible.value) {
    closePanel()
  }
}

// 오버레이 클릭 핸들러
function handleOverlayClick(event: MouseEvent) {
  // 리사이즈 중이면 무시
  if (isResizing.value) return
  closePanel()
}

// 패널 클릭 시 이벤트 전파 중지
function handlePanelClick(event: Event) {
  event.stopPropagation()
}

// 리사이즈 시작
function startResize(event: MouseEvent) {
  event.preventDefault()
  event.stopPropagation()
  isResizing.value = true
  document.body.style.cursor = 'ew-resize'
  document.body.style.userSelect = 'none'

  document.addEventListener('mousemove', handleResize)
  document.addEventListener('mouseup', stopResize)
}

// 리사이즈 중
function handleResize(event: MouseEvent) {
  if (!isResizing.value) return

  const maxWidth = window.innerWidth * MAX_WIDTH_PERCENT
  const newWidth = window.innerWidth - event.clientX

  panelWidth.value = Math.max(MIN_WIDTH, Math.min(newWidth, maxWidth))
}

// 리사이즈 종료
function stopResize() {
  if (isResizing.value) {
    isResizing.value = false
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    saveWidth()
  }

  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
}

// 더블클릭 시 기본 폭으로 리셋
function resetWidth() {
  panelWidth.value = DEFAULT_WIDTH
  saveWidth()
}

// 윈도우 리사이즈 시 최대 폭 조정
function handleWindowResize() {
  const maxWidth = window.innerWidth * MAX_WIDTH_PERCENT
  if (panelWidth.value > maxWidth) {
    panelWidth.value = maxWidth
  }
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('resize', handleWindowResize)
  loadSavedWidth()
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('resize', handleWindowResize)
  stopResize()
})

// body 스크롤 제어
watch(isVisible, (visible) => {
  if (visible) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-300"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-200"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="isVisible"
        class="fixed inset-0 z-50 overflow-hidden"
        @click="handleOverlayClick"
      >
        <!-- 오버레이 배경 (패널 왼쪽만 덮도록) -->
        <div
          class="absolute inset-0 bg-black/30"
          :style="{ right: `${panelWidth}px` }"
        />

        <!-- 패널 -->
        <div
          class="absolute inset-y-0 right-0 flex bg-white shadow-2xl"
          :style="{ width: `${panelWidth}px` }"
          @click="handlePanelClick"
        >
          <!-- 리사이즈 핸들 (패널 내부 왼쪽) - 진한 색상으로 명확히 표시 -->
          <div
            class="w-5 flex-shrink-0 bg-gray-300 hover:bg-primary-400 cursor-ew-resize flex items-center justify-center group border-r-2 border-gray-400 transition-colors"
            :class="{ 'bg-primary-500': isResizing }"
            @mousedown.stop.prevent="startResize"
            @dblclick.stop="resetWidth"
            @click.stop
          >
            <!-- 그립 점 (더 진하고 크게) -->
            <div class="flex flex-col gap-1.5">
              <div class="w-1.5 h-1.5 rounded-full bg-gray-500 group-hover:bg-white" />
              <div class="w-1.5 h-1.5 rounded-full bg-gray-500 group-hover:bg-white" />
              <div class="w-1.5 h-1.5 rounded-full bg-gray-500 group-hover:bg-white" />
              <div class="w-1.5 h-1.5 rounded-full bg-gray-500 group-hover:bg-white" />
              <div class="w-1.5 h-1.5 rounded-full bg-gray-500 group-hover:bg-white" />
            </div>
          </div>

          <!-- 컨텐츠 영역 -->
          <div class="flex-1 h-full overflow-hidden">
            <component
              :is="resolvedComponent"
              v-if="resolvedComponent"
              v-bind="componentProps"
              @close="closePanel"
            />
          </div>

          <!-- 폭 표시 (리사이즈 중) -->
          <div
            v-if="isResizing"
            class="absolute left-8 top-4 px-2 py-1 bg-gray-800 text-white text-xs rounded shadow-lg z-50"
          >
            {{ panelWidth }}px
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
