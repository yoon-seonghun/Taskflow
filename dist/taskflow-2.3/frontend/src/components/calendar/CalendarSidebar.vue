<script setup lang="ts">
/**
 * 캘린더 사이드바 컴포넌트
 * - 내 캘린더 목록 표시
 * - 캘린더 추가/수정/삭제
 * - 캘린더별 필터링
 */
import { ref, computed, onMounted } from 'vue'
import { calendarApi, type UserCalendarResponse } from '@/api/calendar'
import Spinner from '@/components/common/Spinner.vue'

interface Props {
  selectedCalendarIds?: number[]
}

const props = withDefaults(defineProps<Props>(), {
  selectedCalendarIds: () => []
})

const emit = defineEmits<{
  (e: 'update:selectedCalendarIds', ids: number[]): void
  (e: 'calendarChange'): void
  (e: 'addEvent', calendarId?: number): void
}>()

// 상태
const loading = ref(false)
const calendars = ref<UserCalendarResponse[]>([])
const error = ref<string | null>(null)

// 편집 모드
const editingCalendar = ref<UserCalendarResponse | null>(null)
const isCreating = ref(false)
const editForm = ref({
  calendarName: '',
  description: '',
  color: '#3b82f6'
})

// 색상 옵션
const colorOptions = [
  '#ef4444', '#f97316', '#f59e0b', '#84cc16',
  '#22c55e', '#14b8a6', '#06b6d4', '#3b82f6',
  '#6366f1', '#8b5cf6', '#a855f7', '#ec4899'
]

// 선택된 캘린더 ID Set
const selectedIds = computed({
  get: () => new Set(props.selectedCalendarIds),
  set: (val) => emit('update:selectedCalendarIds', Array.from(val))
})

// 내 캘린더 / 공유받은 캘린더 분리
const myCalendars = computed(() =>
  calendars.value.filter(c => c.accessType === 'OWNER')
)
const sharedCalendars = computed(() =>
  calendars.value.filter(c => c.accessType !== 'OWNER')
)

// 데이터 로드
async function fetchCalendars() {
  loading.value = true
  error.value = null
  try {
    const response = await calendarApi.getMyCalendars()
    calendars.value = response.data || []

    // 초기 선택: 모든 캘린더 선택
    if (props.selectedCalendarIds.length === 0 && calendars.value.length > 0) {
      emit('update:selectedCalendarIds', calendars.value.map(c => c.calendarId))
    }
  } catch (err: any) {
    error.value = err.message || '캘린더 목록을 불러오는데 실패했습니다.'
    console.error('Failed to fetch calendars:', err)
  } finally {
    loading.value = false
  }
}

// 캘린더 선택 토글
function toggleCalendar(calendarId: number) {
  const newSet = new Set(selectedIds.value)
  if (newSet.has(calendarId)) {
    newSet.delete(calendarId)
  } else {
    newSet.add(calendarId)
  }
  emit('update:selectedCalendarIds', Array.from(newSet))
  emit('calendarChange')
}

// 전체 선택/해제
function toggleAll(calendars: UserCalendarResponse[]) {
  const ids = calendars.map(c => c.calendarId)
  const allSelected = ids.every(id => selectedIds.value.has(id))

  const newSet = new Set(selectedIds.value)
  if (allSelected) {
    ids.forEach(id => newSet.delete(id))
  } else {
    ids.forEach(id => newSet.add(id))
  }
  emit('update:selectedCalendarIds', Array.from(newSet))
  emit('calendarChange')
}

// 캘린더 생성 모드
function startCreate() {
  isCreating.value = true
  editingCalendar.value = null
  editForm.value = {
    calendarName: '',
    description: '',
    color: '#3b82f6'
  }
}

// 캘린더 편집 모드
function startEdit(calendar: UserCalendarResponse) {
  isCreating.value = false
  editingCalendar.value = calendar
  editForm.value = {
    calendarName: calendar.calendarName,
    description: calendar.description || '',
    color: calendar.color
  }
}

// 편집 취소
function cancelEdit() {
  isCreating.value = false
  editingCalendar.value = null
}

// 캘린더 저장
async function saveCalendar() {
  if (!editForm.value.calendarName.trim()) return

  try {
    if (isCreating.value) {
      await calendarApi.createCalendar({
        calendarName: editForm.value.calendarName,
        description: editForm.value.description || undefined,
        color: editForm.value.color
      })
    } else if (editingCalendar.value) {
      await calendarApi.updateCalendar(editingCalendar.value.calendarId, {
        calendarName: editForm.value.calendarName,
        description: editForm.value.description || undefined,
        color: editForm.value.color
      })
    }

    cancelEdit()
    await fetchCalendars()
    emit('calendarChange')
  } catch (err: any) {
    console.error('Failed to save calendar:', err)
    alert(err.message || '캘린더 저장에 실패했습니다.')
  }
}

// 캘린더 삭제
async function deleteCalendar(calendar: UserCalendarResponse) {
  if (!confirm(`"${calendar.calendarName}" 캘린더를 삭제하시겠습니까?\n포함된 모든 이벤트도 함께 삭제됩니다.`)) {
    return
  }

  try {
    await calendarApi.deleteCalendar(calendar.calendarId)
    await fetchCalendars()
    emit('calendarChange')
  } catch (err: any) {
    console.error('Failed to delete calendar:', err)
    alert(err.message || '캘린더 삭제에 실패했습니다.')
  }
}

// 이벤트 추가
function handleAddEvent(calendarId?: number) {
  emit('addEvent', calendarId)
}

onMounted(() => {
  fetchCalendars()
})

// 외부에서 새로고침 가능하도록 expose
defineExpose({ refresh: fetchCalendars })
</script>

<template>
  <div class="calendar-sidebar h-full flex flex-col bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700">
    <!-- 헤더 -->
    <div class="p-4 border-b border-gray-200 dark:border-gray-700">
      <button
        class="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-primary-600 hover:bg-primary-700 text-white rounded-lg transition-colors font-medium"
        @click="handleAddEvent()"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
        </svg>
        이벤트 추가
      </button>
    </div>

    <!-- 캘린더 목록 -->
    <div class="flex-1 overflow-y-auto p-4">
      <!-- 로딩 -->
      <div v-if="loading" class="flex justify-center py-8">
        <Spinner size="md" />
      </div>

      <!-- 에러 -->
      <div v-else-if="error" class="text-center py-8 text-red-500 text-sm">
        {{ error }}
      </div>

      <template v-else>
        <!-- 내 캘린더 -->
        <div class="mb-6">
          <div class="flex items-center justify-between mb-2">
            <h3 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">
              내 캘린더
            </h3>
            <div class="flex items-center gap-1">
              <button
                class="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded transition-colors"
                title="전체 선택/해제"
                @click="toggleAll(myCalendars)"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
                </svg>
              </button>
              <button
                class="p-1 text-gray-400 hover:text-primary-600 dark:hover:text-primary-400 rounded transition-colors"
                title="캘린더 추가"
                @click="startCreate"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                </svg>
              </button>
            </div>
          </div>

          <!-- 캘린더 생성/편집 폼 -->
          <div
            v-if="isCreating || editingCalendar"
            class="mb-3 p-3 bg-gray-50 dark:bg-gray-900 rounded-lg border border-gray-200 dark:border-gray-700"
          >
            <input
              v-model="editForm.calendarName"
              type="text"
              class="w-full px-2 py-1.5 text-sm border border-gray-300 rounded focus:ring-2 focus:ring-primary-500 focus:border-primary-500 dark:bg-gray-800 dark:border-gray-600 dark:text-gray-100"
              placeholder="캘린더 이름"
              @keyup.enter="saveCalendar"
            />

            <!-- 색상 선택 -->
            <div class="flex flex-wrap gap-1.5 mt-2">
              <button
                v-for="color in colorOptions"
                :key="color"
                class="w-5 h-5 rounded-full border-2 transition-transform hover:scale-110"
                :class="editForm.color === color ? 'border-gray-800 dark:border-white scale-110' : 'border-transparent'"
                :style="{ backgroundColor: color }"
                @click="editForm.color = color"
              />
            </div>

            <!-- 버튼 -->
            <div class="flex justify-end gap-2 mt-3">
              <button
                class="px-3 py-1 text-xs text-gray-600 hover:text-gray-800 dark:text-gray-400 dark:hover:text-gray-200"
                @click="cancelEdit"
              >
                취소
              </button>
              <button
                class="px-3 py-1 text-xs bg-primary-600 text-white rounded hover:bg-primary-700"
                @click="saveCalendar"
              >
                {{ isCreating ? '생성' : '저장' }}
              </button>
            </div>
          </div>

          <!-- 캘린더 목록 -->
          <div class="space-y-1">
            <div
              v-for="calendar in myCalendars"
              :key="calendar.calendarId"
              class="group flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer"
            >
              <!-- 체크박스 -->
              <label class="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  :checked="selectedIds.has(calendar.calendarId)"
                  class="sr-only"
                  @change="toggleCalendar(calendar.calendarId)"
                />
                <span
                  class="w-4 h-4 rounded border-2 flex items-center justify-center transition-colors"
                  :style="{
                    backgroundColor: selectedIds.has(calendar.calendarId) ? calendar.color : 'transparent',
                    borderColor: calendar.color
                  }"
                >
                  <svg
                    v-if="selectedIds.has(calendar.calendarId)"
                    class="w-3 h-3 text-white"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                  </svg>
                </span>
              </label>

              <!-- 캘린더 이름 -->
              <span
                class="flex-1 text-sm text-gray-700 dark:text-gray-300 truncate"
                @click="toggleCalendar(calendar.calendarId)"
              >
                {{ calendar.calendarName }}
              </span>

              <!-- 이벤트 수 -->
              <span
                v-if="calendar.eventCount !== undefined && calendar.eventCount > 0"
                class="text-xs text-gray-400"
              >
                {{ calendar.eventCount }}
              </span>

              <!-- 액션 버튼 -->
              <div class="hidden group-hover:flex items-center gap-0.5">
                <button
                  class="p-1 text-gray-400 hover:text-primary-600 rounded"
                  title="이벤트 추가"
                  @click.stop="handleAddEvent(calendar.calendarId)"
                >
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
                  </svg>
                </button>
                <button
                  class="p-1 text-gray-400 hover:text-gray-600 rounded"
                  title="수정"
                  @click.stop="startEdit(calendar)"
                >
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                  </svg>
                </button>
                <button
                  v-if="!calendar.isDefault"
                  class="p-1 text-gray-400 hover:text-red-500 rounded"
                  title="삭제"
                  @click.stop="deleteCalendar(calendar)"
                >
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          </div>

          <!-- 캘린더 없음 -->
          <div v-if="myCalendars.length === 0 && !isCreating" class="text-center py-4">
            <p class="text-sm text-gray-500 dark:text-gray-400 mb-2">캘린더가 없습니다</p>
            <button
              class="text-sm text-primary-600 hover:text-primary-700 dark:text-primary-400"
              @click="startCreate"
            >
              + 새 캘린더 만들기
            </button>
          </div>
        </div>

        <!-- 공유받은 캘린더 -->
        <div v-if="sharedCalendars.length > 0">
          <div class="flex items-center justify-between mb-2">
            <h3 class="text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider">
              공유받은 캘린더
            </h3>
            <button
              class="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded transition-colors"
              title="전체 선택/해제"
              @click="toggleAll(sharedCalendars)"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
              </svg>
            </button>
          </div>

          <div class="space-y-1">
            <div
              v-for="calendar in sharedCalendars"
              :key="calendar.calendarId"
              class="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer"
              @click="toggleCalendar(calendar.calendarId)"
            >
              <label class="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  :checked="selectedIds.has(calendar.calendarId)"
                  class="sr-only"
                />
                <span
                  class="w-4 h-4 rounded border-2 flex items-center justify-center transition-colors"
                  :style="{
                    backgroundColor: selectedIds.has(calendar.calendarId) ? calendar.color : 'transparent',
                    borderColor: calendar.color
                  }"
                >
                  <svg
                    v-if="selectedIds.has(calendar.calendarId)"
                    class="w-3 h-3 text-white"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7" />
                  </svg>
                </span>
              </label>
              <span class="flex-1 text-sm text-gray-700 dark:text-gray-300 truncate">
                {{ calendar.calendarName }}
              </span>
              <span class="text-xs text-gray-400 truncate">
                {{ calendar.ownerName }}
              </span>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 범례 -->
    <div class="p-4 border-t border-gray-200 dark:border-gray-700">
      <div class="flex flex-wrap gap-3 text-xs text-gray-500 dark:text-gray-400">
        <div class="flex items-center gap-1">
          <span class="w-2 h-2 rounded-full bg-teal-500" />
          <span>Todo</span>
        </div>
        <div class="flex items-center gap-1">
          <span class="w-2 h-2 rounded-sm bg-blue-500" />
          <span>업무</span>
        </div>
        <div class="flex items-center gap-1">
          <span class="w-2 h-2 rounded bg-primary-500" />
          <span>이벤트</span>
        </div>
      </div>
    </div>
  </div>
</template>
