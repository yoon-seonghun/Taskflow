<script setup lang="ts">
/**
 * 캘린더 관리 콘텐츠
 * - 캘린더 생성/수정/삭제
 * - 캘린더 색상 설정
 */
import { ref, computed, onMounted } from 'vue'
import { calendarApi, type CalendarListItem, type CalendarCreateRequest } from '@/api/calendar'
import { useUiStore } from '@/stores/ui'
import Modal from '@/components/common/Modal.vue'
import Button from '@/components/common/Button.vue'
import Input from '@/components/common/Input.vue'

const uiStore = useUiStore()

// 상태
const loading = ref(false)
const calendars = ref<CalendarListItem[]>([])

// 모달 상태
const showFormModal = ref(false)
const editingCalendar = ref<CalendarListItem | null>(null)

// 폼 데이터
const form = ref<CalendarCreateRequest>({
  calendarName: '',
  description: '',
  color: '#3B82F6',
  isDefault: false
})

// 색상 팔레트
const colorPalette = [
  '#3B82F6', // blue
  '#8B5CF6', // purple
  '#EC4899', // pink
  '#EF4444', // red
  '#F97316', // orange
  '#EAB308', // yellow
  '#22C55E', // green
  '#14B8A6', // teal
  '#06B6D4', // cyan
  '#6366F1', // indigo
  '#A855F7', // violet
  '#64748B', // slate
]

// 데이터 로드
async function fetchCalendars() {
  loading.value = true
  try {
    const response = await calendarApi.getCalendars()
    calendars.value = response.data || []
  } catch (err) {
    console.error('Failed to fetch calendars:', err)
    uiStore.showError('캘린더 목록을 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

// 새 캘린더 추가 모달
function openAddModal() {
  editingCalendar.value = null
  form.value = {
    calendarName: '',
    description: '',
    color: '#3B82F6',
    isDefault: false
  }
  showFormModal.value = true
}

// 캘린더 수정 모달
function openEditModal(calendar: CalendarListItem) {
  editingCalendar.value = calendar
  form.value = {
    calendarName: calendar.calendarName,
    description: calendar.description || '',
    color: calendar.color || '#3B82F6',
    isDefault: calendar.isDefault || false
  }
  showFormModal.value = true
}

// 모달 닫기
function closeModal() {
  showFormModal.value = false
  editingCalendar.value = null
}

// 저장
async function handleSave() {
  if (!form.value.calendarName.trim()) {
    uiStore.showWarning('캘린더 이름을 입력해주세요.')
    return
  }

  try {
    if (editingCalendar.value) {
      // 수정
      await calendarApi.updateCalendar(editingCalendar.value.calendarId, form.value)
      uiStore.showSuccess('캘린더가 수정되었습니다.')
    } else {
      // 생성
      await calendarApi.createCalendar(form.value)
      uiStore.showSuccess('캘린더가 생성되었습니다.')
    }
    closeModal()
    fetchCalendars()
  } catch (err: any) {
    uiStore.showError(err.response?.data?.message || '저장에 실패했습니다.')
  }
}

// 삭제
async function handleDelete(calendar: CalendarListItem) {
  if (calendar.isDefault) {
    uiStore.showWarning('기본 캘린더는 삭제할 수 없습니다.')
    return
  }

  if (!confirm(`'${calendar.calendarName}' 캘린더를 삭제하시겠습니까?\n이 캘린더의 모든 이벤트도 함께 삭제됩니다.`)) {
    return
  }

  try {
    await calendarApi.deleteCalendar(calendar.calendarId)
    uiStore.showSuccess('캘린더가 삭제되었습니다.')
    fetchCalendars()
  } catch (err: any) {
    uiStore.showError(err.response?.data?.message || '삭제에 실패했습니다.')
  }
}

// 기본 캘린더 설정
async function handleSetDefault(calendar: CalendarListItem) {
  if (calendar.isDefault) return

  try {
    await calendarApi.updateCalendar(calendar.calendarId, {
      ...calendar,
      isDefault: true
    })
    uiStore.showSuccess(`'${calendar.calendarName}'이(가) 기본 캘린더로 설정되었습니다.`)
    fetchCalendars()
  } catch (err: any) {
    uiStore.showError(err.response?.data?.message || '설정에 실패했습니다.')
  }
}

// 초기 로드
onMounted(() => {
  fetchCalendars()
})
</script>

<template>
  <div class="calendar-content">
    <!-- 헤더 -->
    <div class="flex items-center justify-between mb-4">
      <div>
        <h2 class="text-lg font-semibold text-gray-900 dark:text-white">캘린더 관리</h2>
        <p class="text-sm text-gray-500 dark:text-gray-400">개인 캘린더를 생성하고 관리합니다.</p>
      </div>
      <Button variant="primary" size="sm" @click="openAddModal">
        <svg class="w-4 h-4 mr-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        캘린더 추가
      </Button>
    </div>

    <!-- 로딩 -->
    <div v-if="loading" class="py-12 text-center">
      <svg class="animate-spin h-8 w-8 mx-auto text-gray-400" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500 dark:text-gray-400">캘린더 목록을 불러오는 중...</p>
    </div>

    <!-- 캘린더 목록 -->
    <div v-else-if="calendars.length > 0" class="space-y-2">
      <div
        v-for="calendar in calendars"
        :key="calendar.calendarId"
        class="flex items-center justify-between p-3 bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600 transition-colors"
      >
        <div class="flex items-center gap-3">
          <!-- 색상 표시 -->
          <div
            class="w-4 h-4 rounded-full flex-shrink-0"
            :style="{ backgroundColor: calendar.color || '#3B82F6' }"
          />
          <!-- 캘린더 정보 -->
          <div>
            <div class="flex items-center gap-2">
              <span class="font-medium text-gray-900 dark:text-white">
                {{ calendar.calendarName }}
              </span>
              <span
                v-if="calendar.isDefault"
                class="px-1.5 py-0.5 text-xs bg-primary-100 text-primary-700 dark:bg-primary-900/50 dark:text-primary-400 rounded"
              >
                기본
              </span>
            </div>
            <p v-if="calendar.description" class="text-sm text-gray-500 dark:text-gray-400">
              {{ calendar.description }}
            </p>
          </div>
        </div>

        <!-- 액션 버튼 -->
        <div class="flex items-center gap-1">
          <button
            v-if="!calendar.isDefault"
            class="p-1.5 text-gray-400 hover:text-primary-600 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
            title="기본으로 설정"
            @click="handleSetDefault(calendar)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
            </svg>
          </button>
          <button
            class="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
            title="수정"
            @click="openEditModal(calendar)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
          </button>
          <button
            v-if="!calendar.isDefault"
            class="p-1.5 text-gray-400 hover:text-red-600 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
            title="삭제"
            @click="handleDelete(calendar)"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 빈 상태 -->
    <div v-else class="py-12 text-center">
      <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
      </svg>
      <p class="mt-2 text-sm text-gray-500 dark:text-gray-400">등록된 캘린더가 없습니다.</p>
      <button
        class="mt-3 text-sm text-primary-600 hover:text-primary-700 dark:text-primary-400"
        @click="openAddModal"
      >
        + 첫 번째 캘린더 추가하기
      </button>
    </div>

    <!-- 캘린더 폼 모달 -->
    <Modal
      :show="showFormModal"
      :title="editingCalendar ? '캘린더 수정' : '새 캘린더'"
      size="sm"
      @close="closeModal"
    >
      <div class="space-y-4">
        <!-- 캘린더 이름 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            캘린더 이름 <span class="text-red-500">*</span>
          </label>
          <Input
            v-model="form.calendarName"
            placeholder="예: 업무, 개인, 프로젝트"
          />
        </div>

        <!-- 설명 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
            설명
          </label>
          <Input
            v-model="form.description"
            placeholder="캘린더에 대한 설명"
          />
        </div>

        <!-- 색상 선택 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            색상
          </label>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="color in colorPalette"
              :key="color"
              class="w-8 h-8 rounded-full border-2 transition-all"
              :class="form.color === color ? 'border-gray-900 dark:border-white scale-110' : 'border-transparent hover:scale-105'"
              :style="{ backgroundColor: color }"
              @click="form.color = color"
            />
          </div>
        </div>

        <!-- 기본 캘린더 설정 -->
        <label class="flex items-center gap-2 cursor-pointer">
          <input
            v-model="form.isDefault"
            type="checkbox"
            class="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500 dark:border-gray-600 dark:bg-gray-700"
          />
          <span class="text-sm text-gray-700 dark:text-gray-300">기본 캘린더로 설정</span>
        </label>
      </div>

      <template #footer>
        <div class="flex justify-end gap-2">
          <Button variant="secondary" @click="closeModal">취소</Button>
          <Button variant="primary" @click="handleSave">
            {{ editingCalendar ? '수정' : '생성' }}
          </Button>
        </div>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.calendar-content {
  @apply max-w-3xl;
}
</style>
