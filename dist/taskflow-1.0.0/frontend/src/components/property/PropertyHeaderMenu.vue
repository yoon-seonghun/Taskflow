<script setup lang="ts">
/**
 * 속성 헤더 드롭다운 메뉴 컴포넌트
 * - 컬럼 헤더 클릭 시 표시
 * - 이름 변경, 타입 변경, 순서 이동, 숨기기, 삭제
 * Compact UI 적용
 */
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { usePropertyStore } from '@/stores/property'
import { useToast } from '@/composables/useToast'
import { ConfirmDialog } from '@/components/common'
import PropertyTypeSelector from './PropertyTypeSelector.vue'
import type { PropertyDef, PropertyType } from '@/types/property'

interface Props {
  property: PropertyDef
  boardId: number
  isFirst?: boolean
  isLast?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isFirst: false,
  isLast: false
})

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'reorder', direction: 'left' | 'right'): void
  (e: 'addProperty'): void
}>()

const propertyStore = usePropertyStore()
const toast = useToast()

// 상태
const isEditing = ref(false)
const editedName = ref('')
const showTypeSelector = ref(false)
const showDeleteConfirm = ref(false)
const menuRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)

// 시스템 속성 여부 (수정/삭제 제한)
const isSystemProperty = computed(() => props.property.systemYn === 'Y')

// 현재 숨김 상태 (visibleYn 우선, hiddenYn 호환)
const isHidden = computed(() => {
  if (props.property.visibleYn !== undefined) {
    return props.property.visibleYn !== 'Y'
  }
  return props.property.hiddenYn === 'Y'
})

// 메뉴 항목
const menuItems = computed(() => [
  {
    id: 'rename',
    label: '속성 이름 변경',
    icon: 'edit',
    disabled: isSystemProperty.value,
    action: startRename
  },
  {
    id: 'type',
    label: '속성 타입 변경',
    icon: 'type',
    disabled: isSystemProperty.value,
    action: () => showTypeSelector.value = true
  },
  { id: 'divider1', divider: true },
  {
    id: 'moveLeft',
    label: '왼쪽으로 이동',
    icon: 'arrow-left',
    disabled: props.isFirst,
    action: () => handleMove('left')
  },
  {
    id: 'moveRight',
    label: '오른쪽으로 이동',
    icon: 'arrow-right',
    disabled: props.isLast,
    action: () => handleMove('right')
  },
  {
    id: 'hide',
    label: isHidden.value ? '보이기' : '숨기기',
    icon: isHidden.value ? 'eye' : 'eye-off',
    action: handleToggleVisibility
  },
  { id: 'divider2', divider: true },
  {
    id: 'add',
    label: '새 속성 추가',
    icon: 'plus',
    action: () => emit('addProperty')
  },
  {
    id: 'delete',
    label: '속성 삭제',
    icon: 'trash',
    disabled: isSystemProperty.value,
    danger: true,
    action: () => showDeleteConfirm.value = true
  }
])

// 이름 변경 시작
function startRename() {
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
  } else {
    toast.error('속성 이름 변경에 실패했습니다.')
  }
}

// 이름 변경 취소
function cancelRename() {
  isEditing.value = false
  editedName.value = ''
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
  } else {
    toast.error('속성 타입 변경에 실패했습니다.')
  }
}

// 순서 이동
function handleMove(direction: 'left' | 'right') {
  emit('reorder', direction)
  emit('close')
}

// 숨기기/보이기 토글
async function handleToggleVisibility() {
  const success = await propertyStore.togglePropertyVisibility(props.property.propertyId)

  if (success) {
    const message = isHidden.value ? '속성이 표시됩니다.' : '속성이 숨겨졌습니다.'
    toast.success(message)
  } else {
    toast.error('속성 상태 변경에 실패했습니다.')
  }

  emit('close')
}

// 삭제
async function handleDelete() {
  const success = await propertyStore.deleteProperty(props.property.propertyId)

  if (success) {
    toast.success('속성이 삭제되었습니다.')
  } else {
    toast.error('속성 삭제에 실패했습니다.')
  }

  showDeleteConfirm.value = false
  emit('close')
}

// 외부 클릭 감지
function handleClickOutside(event: MouseEvent) {
  if (menuRef.value && !menuRef.value.contains(event.target as Node)) {
    emit('close')
  }
}

// ESC 키 처리
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    if (isEditing.value) {
      cancelRename()
    } else if (showTypeSelector.value) {
      showTypeSelector.value = false
    } else {
      emit('close')
    }
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div
    ref="menuRef"
    class="absolute z-50 top-full left-0 mt-1 w-56 bg-white rounded-lg shadow-lg border border-gray-200 py-1"
    @click.stop
  >
    <!-- 헤더: 속성 이름 (편집 가능) -->
    <div class="px-3 py-2 border-b border-gray-100">
      <template v-if="isEditing">
        <input
          ref="inputRef"
          v-model="editedName"
          type="text"
          class="w-full px-2 py-1 text-[13px] border border-primary-500 rounded focus:outline-none focus:ring-1 focus:ring-primary-500"
          @blur="saveRename"
          @keydown.enter="saveRename"
          @keydown.escape="cancelRename"
        />
      </template>
      <template v-else>
        <div class="flex items-center gap-2">
          <span class="text-[13px] font-medium text-gray-900 truncate">
            {{ property.propertyName }}
          </span>
          <span class="text-[11px] text-gray-400 px-1.5 py-0.5 bg-gray-100 rounded">
            {{ property.propertyType }}
          </span>
        </div>
      </template>
    </div>

    <!-- 메뉴 항목 -->
    <div class="py-1">
      <template v-for="item in menuItems" :key="item.id">
        <!-- 구분선 -->
        <div v-if="item.divider" class="my-1 border-t border-gray-100" />

        <!-- 메뉴 항목 -->
        <button
          v-else
          class="w-full flex items-center gap-2.5 px-3 py-2 text-[13px] transition-colors"
          :class="[
            item.disabled
              ? 'text-gray-300 cursor-not-allowed'
              : item.danger
                ? 'text-red-600 hover:bg-red-50'
                : 'text-gray-700 hover:bg-gray-50'
          ]"
          :disabled="item.disabled"
          @click="!item.disabled && item.action?.()"
        >
          <!-- 아이콘 -->
          <span class="w-4 h-4 flex items-center justify-center">
            <!-- Edit Icon -->
            <svg v-if="item.icon === 'edit'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            <!-- Type Icon -->
            <svg v-else-if="item.icon === 'type'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16m-7 6h7" />
            </svg>
            <!-- Arrow Left Icon -->
            <svg v-else-if="item.icon === 'arrow-left'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            <!-- Arrow Right Icon -->
            <svg v-else-if="item.icon === 'arrow-right'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
            </svg>
            <!-- Eye Icon -->
            <svg v-else-if="item.icon === 'eye'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
            <!-- Eye Off Icon -->
            <svg v-else-if="item.icon === 'eye-off'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
            </svg>
            <!-- Plus Icon -->
            <svg v-else-if="item.icon === 'plus'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            <!-- Trash Icon -->
            <svg v-else-if="item.icon === 'trash'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </span>
          <span>{{ item.label }}</span>
        </button>
      </template>
    </div>

    <!-- 타입 선택기 (서브메뉴) -->
    <Transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="opacity-0 translate-x-2"
      enter-to-class="opacity-100 translate-x-0"
      leave-active-class="transition duration-100 ease-in"
      leave-from-class="opacity-100 translate-x-0"
      leave-to-class="opacity-0 translate-x-2"
    >
      <PropertyTypeSelector
        v-if="showTypeSelector"
        :current-type="property.propertyType"
        class="absolute left-full top-0 ml-1"
        @select="handleTypeChange"
        @close="showTypeSelector = false"
      />
    </Transition>

    <!-- 삭제 확인 다이얼로그 -->
    <ConfirmDialog
      v-model="showDeleteConfirm"
      title="속성 삭제"
      :message="`'${property.propertyName}' 속성을 삭제하시겠습니까?\n이 속성에 저장된 모든 데이터가 삭제됩니다.`"
      confirm-text="삭제"
      cancel-text="취소"
      variant="danger"
      @confirm="handleDelete"
    />
  </div>
</template>
