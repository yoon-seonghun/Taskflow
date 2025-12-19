<script setup lang="ts">
/**
 * 옵션(코드) 관리 컴포넌트
 * - 설정 > 코드 관리 화면
 * - 좌측: 속성 목록 (SELECT/MULTI_SELECT)
 * - 우측: 선택된 속성의 옵션 목록 관리
 * Compact UI 적용
 */
import { ref, computed, watch } from 'vue'
import { usePropertyStore } from '@/stores/property'
import { useToast } from '@/composables/useToast'
import { Button, Input, ConfirmDialog } from '@/components/common'
import type { PropertyDef, PropertyOption } from '@/types/property'

interface Props {
  boardId: number
}

const props = defineProps<Props>()

const propertyStore = usePropertyStore()
const toast = useToast()

// 상태
const selectedPropertyId = ref<number | null>(null)
const isAddingOption = ref(false)
const newOptionName = ref('')
const newOptionColor = ref('#6B7280')
const editingOptionId = ref<number | null>(null)
const editedOptionName = ref('')
const editedOptionColor = ref('')
const showDeleteConfirm = ref(false)
const optionToDelete = ref<PropertyOption | null>(null)

// 선택형 속성 목록 (SELECT/MULTI_SELECT)
const selectProperties = computed(() =>
  propertyStore.activeProperties.filter(
    p => p.propertyType === 'SELECT' || p.propertyType === 'MULTI_SELECT'
  )
)

// 선택된 속성
const selectedProperty = computed(() =>
  selectProperties.value.find(p => p.propertyId === selectedPropertyId.value) || null
)

// 선택된 속성의 옵션 목록 (정렬순)
const sortedOptions = computed(() => {
  if (!selectedProperty.value?.options) return []
  return [...selectedProperty.value.options]
    .filter(o => o.useYn !== 'N')
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
})

// 첫 번째 속성 자동 선택
watch(selectProperties, (props) => {
  if (props.length > 0 && !selectedPropertyId.value) {
    selectedPropertyId.value = props[0].propertyId
  }
}, { immediate: true })

// 속성 선택
function handlePropertySelect(propertyId: number) {
  selectedPropertyId.value = propertyId
  cancelAdd()
  cancelEdit()
}

// 옵션 추가 시작
function startAddOption() {
  isAddingOption.value = true
  newOptionName.value = ''
  newOptionColor.value = '#6B7280'
}

// 옵션 추가 저장
async function saveNewOption() {
  if (!selectedPropertyId.value || !newOptionName.value.trim()) {
    toast.error('옵션 이름을 입력해주세요.')
    return
  }

  const option = await propertyStore.createOption(selectedPropertyId.value, {
    optionCode: newOptionName.value.trim().toUpperCase().replace(/\s+/g, '_'),
    optionName: newOptionName.value.trim(),
    color: newOptionColor.value,
    sortOrder: sortedOptions.value.length
  })

  if (option) {
    toast.success('옵션이 추가되었습니다.')
    cancelAdd()
  } else {
    toast.error('옵션 추가에 실패했습니다.')
  }
}

// 옵션 추가 취소
function cancelAdd() {
  isAddingOption.value = false
  newOptionName.value = ''
  newOptionColor.value = '#6B7280'
}

// 옵션 편집 시작
function startEdit(option: PropertyOption) {
  editingOptionId.value = option.optionId
  editedOptionName.value = option.optionName
  editedOptionColor.value = option.color || '#6B7280'
}

// 옵션 편집 저장
async function saveEdit() {
  if (!selectedPropertyId.value || !editingOptionId.value) return

  if (!editedOptionName.value.trim()) {
    toast.error('옵션 이름을 입력해주세요.')
    return
  }

  const success = await propertyStore.updateOption(
    selectedPropertyId.value,
    editingOptionId.value,
    {
      optionName: editedOptionName.value.trim(),
      color: editedOptionColor.value
    }
  )

  if (success) {
    toast.success('옵션이 수정되었습니다.')
    cancelEdit()
  } else {
    toast.error('옵션 수정에 실패했습니다.')
  }
}

// 옵션 편집 취소
function cancelEdit() {
  editingOptionId.value = null
  editedOptionName.value = ''
  editedOptionColor.value = ''
}

// 옵션 순서 이동
async function moveOption(option: PropertyOption, direction: 'up' | 'down') {
  if (!selectedPropertyId.value) return

  const currentIndex = sortedOptions.value.findIndex(o => o.optionId === option.optionId)
  const newIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1

  if (newIndex < 0 || newIndex >= sortedOptions.value.length) return

  const targetOption = sortedOptions.value[newIndex]

  // 두 옵션의 sortOrder 교환
  await Promise.all([
    propertyStore.updateOption(selectedPropertyId.value, option.optionId, {
      sortOrder: targetOption.sortOrder
    }),
    propertyStore.updateOption(selectedPropertyId.value, targetOption.optionId, {
      sortOrder: option.sortOrder
    })
  ])
}

// 옵션 삭제 확인
function confirmDelete(option: PropertyOption) {
  optionToDelete.value = option
  showDeleteConfirm.value = true
}

// 옵션 삭제
async function handleDelete() {
  if (!selectedPropertyId.value || !optionToDelete.value) return

  const success = await propertyStore.deleteOption(
    selectedPropertyId.value,
    optionToDelete.value.optionId
  )

  if (success) {
    toast.success('옵션이 삭제되었습니다.')
  } else {
    toast.error('옵션 삭제에 실패했습니다.')
  }

  showDeleteConfirm.value = false
  optionToDelete.value = null
}

// 색상 프리셋
const colorPresets = [
  '#EF4444', '#F97316', '#F59E0B', '#EAB308',
  '#84CC16', '#22C55E', '#10B981', '#14B8A6',
  '#06B6D4', '#0EA5E9', '#3B82F6', '#6366F1',
  '#8B5CF6', '#A855F7', '#D946EF', '#EC4899',
  '#F43F5E', '#6B7280', '#374151', '#111827'
]
</script>

<template>
  <div class="h-full flex bg-white rounded-lg border border-gray-200 overflow-hidden">
    <!-- 좌측: 속성 목록 -->
    <div class="w-48 flex-shrink-0 border-r border-gray-200 bg-gray-50">
      <div class="p-3 border-b border-gray-200">
        <h3 class="text-[13px] font-medium text-gray-700">속성 목록</h3>
      </div>

      <div class="overflow-y-auto">
        <button
          v-for="property in selectProperties"
          :key="property.propertyId"
          class="w-full flex items-center gap-2 px-3 py-2.5 text-left text-[13px] transition-colors border-b border-gray-100"
          :class="[
            selectedPropertyId === property.propertyId
              ? 'bg-white text-primary-700 font-medium'
              : 'text-gray-700 hover:bg-gray-100'
          ]"
          @click="handlePropertySelect(property.propertyId)"
        >
          <!-- 타입 아이콘 -->
          <span class="w-4 h-4 flex-shrink-0">
            <svg v-if="property.propertyType === 'SELECT'" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" />
            </svg>
            <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
            </svg>
          </span>
          <span class="truncate">{{ property.propertyName }}</span>
        </button>

        <!-- 빈 상태 -->
        <div
          v-if="selectProperties.length === 0"
          class="px-3 py-6 text-center text-[13px] text-gray-400"
        >
          선택형 속성이 없습니다.
        </div>
      </div>
    </div>

    <!-- 우측: 옵션 목록 -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- 헤더 -->
      <div class="flex items-center justify-between p-3 border-b border-gray-200">
        <div>
          <h3 class="text-[14px] font-medium text-gray-900">
            {{ selectedProperty?.propertyName || '옵션 관리' }}
          </h3>
          <p class="text-[12px] text-gray-500 mt-0.5">
            {{ sortedOptions.length }}개 옵션
          </p>
        </div>

        <Button
          v-if="selectedProperty"
          size="sm"
          @click="startAddOption"
        >
          <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          옵션 추가
        </Button>
      </div>

      <!-- 옵션 목록 -->
      <div class="flex-1 overflow-y-auto p-3">
        <!-- 빈 상태 -->
        <div
          v-if="!selectedProperty"
          class="h-full flex items-center justify-center text-[13px] text-gray-400"
        >
          좌측에서 속성을 선택해주세요.
        </div>

        <div v-else-if="sortedOptions.length === 0 && !isAddingOption" class="h-full flex flex-col items-center justify-center">
          <svg class="w-12 h-12 text-gray-300 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
          <p class="text-[13px] text-gray-500 mb-3">등록된 옵션이 없습니다.</p>
          <Button size="sm" @click="startAddOption">옵션 추가</Button>
        </div>

        <div v-else class="space-y-2">
          <!-- 옵션 항목 -->
          <div
            v-for="(option, index) in sortedOptions"
            :key="option.optionId"
            class="flex items-center gap-2 p-2 bg-gray-50 rounded-lg border border-gray-200 hover:border-gray-300 transition-colors"
          >
            <!-- 편집 모드 -->
            <template v-if="editingOptionId === option.optionId">
              <!-- 색상 선택 -->
              <div class="relative">
                <button
                  class="w-6 h-6 rounded border border-gray-300"
                  :style="{ backgroundColor: editedOptionColor }"
                />
                <input
                  v-model="editedOptionColor"
                  type="color"
                  class="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                />
              </div>

              <!-- 이름 입력 -->
              <input
                v-model="editedOptionName"
                type="text"
                class="flex-1 px-2 py-1 text-[13px] border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
                @keydown.enter="saveEdit"
                @keydown.escape="cancelEdit"
              />

              <!-- 저장/취소 버튼 -->
              <button
                class="p-1 text-green-600 hover:bg-green-50 rounded"
                @click="saveEdit"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                </svg>
              </button>
              <button
                class="p-1 text-gray-400 hover:bg-gray-100 rounded"
                @click="cancelEdit"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </template>

            <!-- 보기 모드 -->
            <template v-else>
              <!-- 색상 표시 -->
              <span
                class="w-4 h-4 rounded flex-shrink-0"
                :style="{ backgroundColor: option.color || '#6B7280' }"
              />

              <!-- 이름 -->
              <span class="flex-1 text-[13px] text-gray-700 truncate">
                {{ option.optionName }}
              </span>

              <!-- 액션 버튼 -->
              <div class="flex items-center gap-0.5">
                <!-- 위로 이동 -->
                <button
                  class="p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-200 rounded disabled:opacity-30 disabled:cursor-not-allowed"
                  :disabled="index === 0"
                  title="위로 이동"
                  @click="moveOption(option, 'up')"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7" />
                  </svg>
                </button>

                <!-- 아래로 이동 -->
                <button
                  class="p-1 text-gray-400 hover:text-gray-600 hover:bg-gray-200 rounded disabled:opacity-30 disabled:cursor-not-allowed"
                  :disabled="index === sortedOptions.length - 1"
                  title="아래로 이동"
                  @click="moveOption(option, 'down')"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                  </svg>
                </button>

                <!-- 편집 -->
                <button
                  class="p-1 text-gray-400 hover:text-primary-600 hover:bg-primary-50 rounded"
                  title="편집"
                  @click="startEdit(option)"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                </button>

                <!-- 삭제 -->
                <button
                  class="p-1 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded"
                  title="삭제"
                  @click="confirmDelete(option)"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </template>
          </div>

          <!-- 새 옵션 추가 폼 -->
          <div
            v-if="isAddingOption"
            class="flex items-center gap-2 p-2 bg-primary-50 rounded-lg border border-primary-200"
          >
            <!-- 색상 선택 -->
            <div class="relative">
              <button
                class="w-6 h-6 rounded border border-gray-300"
                :style="{ backgroundColor: newOptionColor }"
              />
              <input
                v-model="newOptionColor"
                type="color"
                class="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
              />
            </div>

            <!-- 이름 입력 -->
            <input
              v-model="newOptionName"
              type="text"
              placeholder="옵션 이름 입력"
              class="flex-1 px-2 py-1 text-[13px] border border-gray-300 rounded focus:outline-none focus:ring-1 focus:ring-primary-500 focus:border-primary-500"
              @keydown.enter="saveNewOption"
              @keydown.escape="cancelAdd"
              autofocus
            />

            <!-- 저장/취소 버튼 -->
            <button
              class="p-1 text-green-600 hover:bg-green-50 rounded"
              @click="saveNewOption"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
            </button>
            <button
              class="p-1 text-gray-400 hover:bg-gray-100 rounded"
              @click="cancelAdd"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 색상 프리셋 (추가/편집 시) -->
      <div
        v-if="isAddingOption || editingOptionId"
        class="p-3 border-t border-gray-200 bg-gray-50"
      >
        <p class="text-[11px] text-gray-500 mb-2">색상 프리셋</p>
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="color in colorPresets"
            :key="color"
            class="w-5 h-5 rounded border border-gray-200 hover:scale-110 transition-transform"
            :style="{ backgroundColor: color }"
            @click="isAddingOption ? (newOptionColor = color) : (editedOptionColor = color)"
          />
        </div>
      </div>
    </div>

    <!-- 삭제 확인 다이얼로그 -->
    <ConfirmDialog
      v-model="showDeleteConfirm"
      title="옵션 삭제"
      :message="`'${optionToDelete?.optionName}' 옵션을 삭제하시겠습니까?\n이 옵션을 사용하는 아이템의 값이 초기화됩니다.`"
      confirm-text="삭제"
      cancel-text="취소"
      variant="danger"
      @confirm="handleDelete"
    />
  </div>
</template>
