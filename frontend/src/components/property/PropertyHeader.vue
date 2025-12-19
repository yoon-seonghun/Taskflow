<script setup lang="ts">
/**
 * 속성 컬럼 헤더 컴포넌트
 * - 속성명 표시
 * - 클릭 시 속성 관리 드롭다운
 * - 인라인 이름 편집
 * - 타입 선택기 서브메뉴
 * - 정렬, 숨기기, 삭제 기능
 * Compact UI: height 32px, font 13px
 */
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { usePropertyStore } from '@/stores/property'
import { useToast } from '@/composables/useToast'
import { ConfirmDialog } from '@/components/common'
import PropertyTypeSelector from './PropertyTypeSelector.vue'
import type { PropertyDef, PropertyType } from '@/types/property'

interface Props {
  property: PropertyDef
  boardId?: number
  sortable?: boolean
  resizable?: boolean
  width?: number
  isFirst?: boolean
  isLast?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  boardId: 0,
  sortable: true,
  resizable: true,
  width: 150,
  isFirst: false,
  isLast: false
})

const emit = defineEmits<{
  (e: 'rename', propertyId: number): void
  (e: 'changeType', propertyId: number): void
  (e: 'moveLeft', propertyId: number): void
  (e: 'moveRight', propertyId: number): void
  (e: 'hide', propertyId: number): void
  (e: 'delete', propertyId: number): void
  (e: 'addProperty'): void
  (e: 'sort', propertyId: number, direction: 'asc' | 'desc'): void
  (e: 'resize', propertyId: number, width: number): void
}>()

const propertyStore = usePropertyStore()
const toast = useToast()

const isMenuOpen = ref(false)
const isEditing = ref(false)
const editedName = ref('')
const showTypeSelector = ref(false)
const showDeleteConfirm = ref(false)
const containerRef = ref<HTMLElement | null>(null)
const menuRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)

// 속성 타입 아이콘
const typeIcons: Record<PropertyType, string> = {
  TEXT: 'M4 6h16M4 12h16M4 18h7',
  NUMBER: 'M7 20l4-16m2 16l4-16M6 9h14M4 15h14',
  DATE: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z',
  SELECT: 'M19 9l-7 7-7-7',
  MULTI_SELECT: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2',
  CHECKBOX: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
  USER: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z'
}

const typeIcon = computed(() => typeIcons[props.property.propertyType] || typeIcons.TEXT)

// 시스템 속성 여부
const isSystemProperty = computed(() => props.property.systemYn === 'Y')

function toggleMenu() {
  isMenuOpen.value = !isMenuOpen.value
}

function closeMenu() {
  isMenuOpen.value = false
}

// 이름 변경 시작
function handleRename() {
  editedName.value = props.property.propertyName
  isEditing.value = true
  nextTick(() => {
    inputRef.value?.focus()
    inputRef.value?.select()
  })
}

// 이름 변경 저장
async function saveRename() {
  if (!editedName.value.trim()) {
    toast.error('속성 이름을 입력해주세요.')
    return
  }

  if (editedName.value.trim() === props.property.propertyName) {
    isEditing.value = false
    return
  }

  const success = await propertyStore.updateProperty(props.property.propertyId, {
    propertyName: editedName.value.trim()
  })

  if (success) {
    toast.success('속성 이름이 변경되었습니다.')
    isEditing.value = false
    emit('rename', props.property.propertyId)
  } else {
    toast.error('속성 이름 변경에 실패했습니다.')
  }
}

// 이름 변경 취소
function cancelRename() {
  isEditing.value = false
  editedName.value = ''
}

// 타입 선택기 열기
function handleChangeType() {
  showTypeSelector.value = true
}

// 타입 변경
async function handleTypeChange(newType: PropertyType) {
  if (newType === props.property.propertyType) {
    showTypeSelector.value = false
    return
  }

  const success = await propertyStore.updateProperty(props.property.propertyId, {
    propertyType: newType
  })

  if (success) {
    toast.success('속성 타입이 변경되었습니다.')
    showTypeSelector.value = false
    emit('changeType', props.property.propertyId)
  } else {
    toast.error('속성 타입 변경에 실패했습니다.')
  }
}

function handleMoveLeft() {
  emit('moveLeft', props.property.propertyId)
  closeMenu()
}

function handleMoveRight() {
  emit('moveRight', props.property.propertyId)
  closeMenu()
}

function handleHide() {
  emit('hide', props.property.propertyId)
  closeMenu()
}

function handleDelete() {
  showDeleteConfirm.value = true
}

// 삭제 확인
async function confirmDelete() {
  const success = await propertyStore.deleteProperty(props.property.propertyId)

  if (success) {
    toast.success('속성이 삭제되었습니다.')
    emit('delete', props.property.propertyId)
  } else {
    toast.error('속성 삭제에 실패했습니다.')
  }

  showDeleteConfirm.value = false
  closeMenu()
}

function handleAddProperty() {
  emit('addProperty')
  closeMenu()
}

function handleSort(direction: 'asc' | 'desc') {
  emit('sort', props.property.propertyId, direction)
  closeMenu()
}

// 외부 클릭 감지
function handleClickOutside(event: MouseEvent) {
  if (containerRef.value && !containerRef.value.contains(event.target as Node)) {
    closeMenu()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div
    ref="containerRef"
    class="relative flex items-center h-8 px-2 text-[13px] font-medium text-gray-600 bg-gray-50 border-b border-r border-gray-200 select-none cursor-pointer hover:bg-gray-100 transition-colors"
    :style="{ width: `${width}px`, minWidth: `${width}px` }"
    @click="toggleMenu"
  >
    <!-- 속성 타입 아이콘 -->
    <svg class="w-4 h-4 mr-1.5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="typeIcon" />
    </svg>

    <!-- 속성명 -->
    <span class="truncate flex-1">{{ property.propertyName }}</span>

    <!-- 필수 표시 -->
    <span v-if="property.requiredYn === 'Y'" class="text-red-500 ml-0.5">*</span>

    <!-- 드롭다운 메뉴 -->
    <Transition
      enter-active-class="transition duration-100 ease-out"
      enter-from-class="opacity-0 scale-95"
      enter-to-class="opacity-100 scale-100"
      leave-active-class="transition duration-75 ease-in"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 scale-95"
    >
      <div
        v-if="isMenuOpen"
        ref="menuRef"
        class="absolute top-full left-0 mt-1 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50"
        @click.stop
      >
        <!-- 정렬 -->
        <template v-if="sortable">
          <button
            class="w-full px-3 py-1.5 text-left text-[13px] text-gray-700 hover:bg-gray-50 flex items-center gap-2"
            @click="handleSort('asc')"
          >
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4h13M3 8h9m-9 4h6m4 0l4-4m0 0l4 4m-4-4v12" />
            </svg>
            오름차순 정렬
          </button>
          <button
            class="w-full px-3 py-1.5 text-left text-[13px] text-gray-700 hover:bg-gray-50 flex items-center gap-2"
            @click="handleSort('desc')"
          >
            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4h13M3 8h9m-9 4h9m5-4v12m0 0l-4-4m4 4l4-4" />
            </svg>
            내림차순 정렬
          </button>
          <div class="my-1 border-t border-gray-100" />
        </template>

        <!-- 속성 이름 변경 -->
        <button
          v-if="!isSystemProperty"
          class="w-full px-3 py-1.5 text-left text-[13px] text-gray-700 hover:bg-gray-50 flex items-center gap-2"
          @click="handleRename"
        >
          <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
          </svg>
          속성 이름 변경
        </button>

        <!-- 속성 타입 변경 -->
        <button
          v-if="!isSystemProperty"
          class="w-full px-3 py-1.5 text-left text-[13px] text-gray-700 hover:bg-gray-50 flex items-center gap-2"
          @click="handleChangeType"
        >
          <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
          </svg>
          속성 타입 변경
        </button>

        <div v-if="!isSystemProperty" class="my-1 border-t border-gray-100" />

        <!-- 순서 변경 -->
        <button
          class="w-full px-3 py-1.5 text-left text-[13px] text-gray-700 hover:bg-gray-50 flex items-center gap-2"
          @click="handleMoveLeft"
        >
          <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
          </svg>
          왼쪽으로 이동
        </button>
        <button
          class="w-full px-3 py-1.5 text-left text-[13px] text-gray-700 hover:bg-gray-50 flex items-center gap-2"
          @click="handleMoveRight"
        >
          <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 5l7 7-7 7M5 5l7 7-7 7" />
          </svg>
          오른쪽으로 이동
        </button>

        <div class="my-1 border-t border-gray-100" />

        <!-- 숨기기 -->
        <button
          class="w-full px-3 py-1.5 text-left text-[13px] text-gray-700 hover:bg-gray-50 flex items-center gap-2"
          @click="handleHide"
        >
          <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
          </svg>
          숨기기
        </button>

        <div class="my-1 border-t border-gray-100" />

        <!-- 새 속성 추가 -->
        <button
          class="w-full px-3 py-1.5 text-left text-[13px] text-primary-600 hover:bg-primary-50 flex items-center gap-2"
          @click="handleAddProperty"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          새 속성 추가
        </button>

        <!-- 속성 삭제 -->
        <button
          v-if="!isSystemProperty"
          class="w-full px-3 py-1.5 text-left text-[13px] text-red-600 hover:bg-red-50 flex items-center gap-2"
          @click="handleDelete"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          속성 삭제
        </button>
      </div>
    </Transition>
  </div>
</template>
