<script setup lang="ts">
/**
 * 이벤트 생성/수정 모달
 * - 이벤트 기본 정보 입력
 * - 음력/양력 선택
 * - 반복 설정
 */
import { ref, computed, watch, onMounted } from 'vue'
import { calendarApi, type UserCalendarResponse, type EventDetailResponse, type EventRequest } from '@/api/calendar'
import Modal from '@/components/common/Modal.vue'
import Input from '@/components/common/Input.vue'
import Select from '@/components/common/Select.vue'
import LunarDatePicker from '@/components/calendar/LunarDatePicker.vue'
import { solarToLunar } from '@/utils/lunar'

interface Props {
  visible: boolean
  event?: EventDetailResponse | null  // 편집 시
  defaultDate?: string  // 기본 날짜 (YYYY-MM-DD)
  defaultCalendarId?: number  // 기본 캘린더
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  event: null,
  defaultDate: undefined,
  defaultCalendarId: undefined
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved', event: EventDetailResponse): void
  (e: 'deleted', eventId: number): void
}>()

// 캘린더 목록
const calendars = ref<UserCalendarResponse[]>([])
const loadingCalendars = ref(false)

// 폼 데이터
const form = ref<{
  calendarId: number | null
  title: string
  description: string
  location: string
  startDate: string
  endDate: string
  startTime: string
  endTime: string
  isAllDay: boolean
  isLunar: boolean
  lunarMonth: number | null
  lunarDay: number | null
  lunarLeapMonth: boolean
  isRecurring: boolean
  recurrenceType: string
  recurrenceInterval: number
  recurrenceEndDate: string
  recurrenceCount: number | null
}>({
  calendarId: null,
  title: '',
  description: '',
  location: '',
  startDate: '',
  endDate: '',
  startTime: '',
  endTime: '',
  isAllDay: true,
  isLunar: false,
  lunarMonth: null,
  lunarDay: null,
  lunarLeapMonth: false,
  isRecurring: false,
  recurrenceType: 'DAILY',
  recurrenceInterval: 1,
  recurrenceEndDate: '',
  recurrenceCount: null
})

// 저장 중
const saving = ref(false)
const deleting = ref(false)

// 편집 모드 여부
const isEditMode = computed(() => !!props.event)

// 모달 제목
const modalTitle = computed(() => isEditMode.value ? '이벤트 수정' : '새 이벤트')

// 반복 타입 옵션 (음력 체크박스에 따라 자동 처리)
const recurrenceTypeOptions = [
  { value: 'DAILY', label: '매일' },
  { value: 'WEEKLY', label: '매주' },
  { value: 'MONTHLY', label: '매월' },
  { value: 'YEARLY', label: '매년' }
]

/**
 * 실제 recurrenceType 결정 (음력 여부에 따라 변환)
 * - 음력 모드: WEEKLY → WEEKLY_LUNAR, MONTHLY → MONTHLY_LUNAR, YEARLY → YEARLY_LUNAR
 * - DAILY는 음력/양력 구분 없음
 */
function getEffectiveRecurrenceType(baseType: string, isLunar: boolean): string {
  if (!isLunar) return baseType

  switch (baseType) {
    case 'DAILY': return 'DAILY'  // 일간은 음력/양력 구분 없음
    case 'WEEKLY': return 'WEEKLY_LUNAR'
    case 'MONTHLY': return 'MONTHLY_LUNAR'
    case 'YEARLY': return 'YEARLY_LUNAR'
    default: return baseType
  }
}

/**
 * recurrenceType에서 기본 타입 추출 (_LUNAR 접미사 제거)
 * - 편집 모드에서 UI 표시용
 */
function getBaseRecurrenceType(type: string): string {
  if (!type) return 'DAILY'

  switch (type) {
    case 'WEEKLY_LUNAR': return 'WEEKLY'
    case 'MONTHLY_LUNAR': return 'MONTHLY'
    case 'YEARLY_LUNAR': return 'YEARLY'
    default: return type
  }
}

/**
 * 음력 데이터 준비 (혼동 방지 로직)
 * - 체크박스 양력 → 양력 그대로
 * - 체크박스 음력 + 음력 정보 있음 → 그대로 사용
 * - 체크박스 음력 + 음력 정보 없음 → 양력 날짜를 음력으로 변환
 */
function prepareLunarData(): {
  isLunar: boolean
  lunarMonth: number | undefined
  lunarDay: number | undefined
  lunarLeapMonth: boolean | undefined
} {
  // Case 1: 체크박스 양력 → 양력 그대로
  if (!form.value.isLunar) {
    return {
      isLunar: false,
      lunarMonth: undefined,
      lunarDay: undefined,
      lunarLeapMonth: undefined
    }
  }

  // Case 2: 체크박스 음력 + 음력 정보 있음 → 그대로 사용
  if (form.value.lunarMonth && form.value.lunarDay) {
    return {
      isLunar: true,
      lunarMonth: form.value.lunarMonth,
      lunarDay: form.value.lunarDay,
      lunarLeapMonth: form.value.lunarLeapMonth
    }
  }

  // Case 3: 체크박스 음력 + 음력 정보 없음 (라디오버튼이 양력인 경우)
  // → 양력 날짜를 음력으로 변환
  try {
    const converted = solarToLunar(form.value.startDate)
    return {
      isLunar: true,
      lunarMonth: converted.month,
      lunarDay: converted.day,
      lunarLeapMonth: converted.isLeapMonth
    }
  } catch (e) {
    console.warn('Failed to convert solar to lunar:', e)
    return {
      isLunar: true,
      lunarMonth: undefined,
      lunarDay: undefined,
      lunarLeapMonth: undefined
    }
  }
}

// 캘린더 옵션 (내 캘린더만 - 공유받은 캘린더 제외)
const calendarOptions = computed(() =>
  calendars.value
    .filter(c => !c.isShared)  // 내 캘린더만 (공유받은 캘린더 제외)
    .map(c => ({
      value: c.calendarId,
      label: c.calendarName
    }))
)

// 캘린더 로드
async function loadCalendars() {
  loadingCalendars.value = true
  try {
    const response = await calendarApi.getMyCalendars()
    calendars.value = response.data || []
  } catch (err) {
    console.error('Failed to load calendars:', err)
  } finally {
    loadingCalendars.value = false
  }
}

// 폼 초기화
function initForm() {
  if (props.event) {
    // 편집 모드
    // recurrenceType에서 _LUNAR 접미사 제거 (UI에서는 기본 타입만 표시)
    const baseRecurrenceType = getBaseRecurrenceType(props.event.recurrenceType || 'DAILY')

    form.value = {
      calendarId: props.event.calendarId,
      title: props.event.title,
      description: props.event.description || '',
      location: props.event.location || '',
      startDate: props.event.startDate,
      endDate: props.event.endDate || props.event.startDate,
      startTime: props.event.startTime || '',
      endTime: props.event.endTime || '',
      isAllDay: props.event.isAllDay,
      isLunar: props.event.isLunar,
      lunarMonth: props.event.lunarMonth,
      lunarDay: props.event.lunarDay,
      lunarLeapMonth: props.event.lunarLeapMonth || false,
      isRecurring: props.event.isRecurring,
      recurrenceType: baseRecurrenceType,
      recurrenceInterval: props.event.recurrenceInterval || 1,
      recurrenceEndDate: props.event.recurrenceEndDate || '',
      recurrenceCount: props.event.recurrenceCount
    }
  } else {
    // 생성 모드
    const today = props.defaultDate || new Date().toISOString().split('T')[0]
    const defaultCalendar = props.defaultCalendarId ||
      calendars.value.find(c => c.isDefault)?.calendarId ||
      calendars.value[0]?.calendarId

    form.value = {
      calendarId: defaultCalendar || null,
      title: '',
      description: '',
      location: '',
      startDate: today,
      endDate: today,
      startTime: '',
      endTime: '',
      isAllDay: true,
      isLunar: false,
      lunarMonth: null,
      lunarDay: null,
      lunarLeapMonth: false,
      isRecurring: false,
      recurrenceType: 'DAILY',
      recurrenceInterval: 1,
      recurrenceEndDate: '',
      recurrenceCount: null
    }
  }
}

// 음력 날짜 변경 핸들러
function handleLunarChange(data: { solar: string; lunar: { year: number; month: number; day: number; isLeapMonth: boolean }; isLunar: boolean }) {
  form.value.lunarMonth = data.lunar.month
  form.value.lunarDay = data.lunar.day
  form.value.lunarLeapMonth = data.lunar.isLeapMonth
  form.value.startDate = data.solar
  form.value.endDate = data.solar
}

// 반복 횟수 음수 방지 - 키 입력 차단
function preventNegativeInput(e: KeyboardEvent) {
  // 마이너스(-) 키 입력 차단
  if (e.key === '-' || e.key === 'e' || e.key === 'E') {
    e.preventDefault()
  }
}

// 반복 횟수 음수 방지 - 값 보정
function handleRecurrenceCountInput(e: Event) {
  const input = e.target as HTMLInputElement
  const value = parseInt(input.value)
  if (!isNaN(value) && value < 1) {
    form.value.recurrenceCount = 1
  }
}

// 저장
async function handleSave() {
  if (!form.value.title.trim()) {
    alert('이벤트 제목을 입력해주세요.')
    return
  }
  if (!form.value.calendarId) {
    alert('캘린더를 선택해주세요.')
    return
  }

  // 반복 종료일 유효성 검증
  if (form.value.isRecurring && form.value.recurrenceEndDate) {
    const eventEndDate = form.value.endDate || form.value.startDate
    if (form.value.recurrenceEndDate < eventEndDate) {
      alert('반복 종료일은 이벤트 종료일 이후여야 합니다.')
      return
    }
  }

  saving.value = true
  try {
    // 음력 정보 준비 (혼동 방지 로직)
    const lunarData = prepareLunarData()

    // 반복 타입 결정 (음력 체크박스에 따라 자동 변환)
    const effectiveRecurrenceType = form.value.isRecurring
      ? getEffectiveRecurrenceType(form.value.recurrenceType, form.value.isLunar)
      : undefined

    const request: EventRequest = {
      calendarId: form.value.calendarId,
      title: form.value.title.trim(),
      description: form.value.description || undefined,
      location: form.value.location || undefined,
      startDate: form.value.startDate,
      endDate: form.value.endDate || undefined,
      startTime: form.value.isAllDay ? undefined : (form.value.startTime || undefined),
      endTime: form.value.isAllDay ? undefined : (form.value.endTime || undefined),
      isAllDay: form.value.isAllDay,
      isLunar: lunarData.isLunar,
      lunarMonth: lunarData.lunarMonth,
      lunarDay: lunarData.lunarDay,
      lunarLeapMonth: lunarData.lunarLeapMonth,
      isRecurring: form.value.isRecurring,
      recurrenceType: effectiveRecurrenceType,
      recurrenceInterval: form.value.isRecurring ? form.value.recurrenceInterval : undefined,
      recurrenceEndDate: form.value.isRecurring && form.value.recurrenceEndDate ? form.value.recurrenceEndDate : undefined,
      recurrenceCount: form.value.isRecurring && form.value.recurrenceCount ? form.value.recurrenceCount : undefined
    }

    let savedEvent: EventDetailResponse
    if (isEditMode.value && props.event) {
      const response = await calendarApi.updateEvent(props.event.eventId, request)
      savedEvent = response.data
    } else {
      const response = await calendarApi.createEvent(request)
      savedEvent = response.data
    }

    emit('saved', savedEvent)
    emit('update:visible', false)
  } catch (err: any) {
    console.error('Failed to save event:', err)
    alert(err.message || '이벤트 저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

// 삭제
async function handleDelete() {
  if (!props.event) return
  if (!confirm('이 이벤트를 삭제하시겠습니까?')) return

  deleting.value = true
  try {
    await calendarApi.deleteEvent(props.event.eventId)
    emit('deleted', props.event.eventId)
    emit('update:visible', false)
  } catch (err: any) {
    console.error('Failed to delete event:', err)
    alert(err.message || '이벤트 삭제에 실패했습니다.')
  } finally {
    deleting.value = false
  }
}

// 닫기
function handleClose() {
  emit('update:visible', false)
}

// visible 변경 감시
watch(() => props.visible, (newVal) => {
  if (newVal) {
    if (calendars.value.length === 0) {
      loadCalendars().then(() => initForm())
    } else {
      initForm()
    }
  }
})

onMounted(() => {
  if (props.visible) {
    loadCalendars().then(() => initForm())
  }
})
</script>

<template>
  <Modal
    :model-value="visible"
    :title="modalTitle"
    size="lg"
    @close="handleClose"
    @update:model-value="(val) => emit('update:visible', val)"
  >
    <div class="space-y-4">
      <!-- 캘린더 선택 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
          캘린더 <span class="text-red-500">*</span>
        </label>
        <Select
          v-model="form.calendarId"
          :options="calendarOptions"
          placeholder="캘린더 선택"
          :disabled="loadingCalendars"
        />
      </div>

      <!-- 제목 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
          제목 <span class="text-red-500">*</span>
        </label>
        <Input
          v-model="form.title"
          placeholder="이벤트 제목"
        />
      </div>

      <!-- 종일/시간 토글 -->
      <div class="flex items-center gap-4">
        <label class="flex items-center gap-2 cursor-pointer">
          <input
            v-model="form.isAllDay"
            type="checkbox"
            class="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
          />
          <span class="text-sm text-gray-700 dark:text-gray-300">종일</span>
        </label>

        <label class="flex items-center gap-2 cursor-pointer">
          <input
            v-model="form.isLunar"
            type="checkbox"
            class="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
          />
          <span class="text-sm text-gray-700 dark:text-gray-300">음력</span>
        </label>
      </div>

      <!-- 날짜/시간 -->
      <div class="grid grid-cols-2 gap-4">
        <!-- 양력 날짜 입력 -->
        <template v-if="!form.isLunar">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              시작일
            </label>
            <Input v-model="form.startDate" type="date" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              종료일
            </label>
            <Input v-model="form.endDate" type="date" />
          </div>
        </template>

        <!-- 음력 날짜 입력 -->
        <template v-else>
          <div class="col-span-2">
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              음력 날짜
            </label>
            <LunarDatePicker
              :model-value="form.startDate"
              :is-lunar-mode="form.isLunar"
              @update:model-value="(val) => form.startDate = val"
              @change="handleLunarChange"
            />
          </div>
        </template>

        <!-- 시간 (종일이 아닌 경우) -->
        <template v-if="!form.isAllDay">
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              시작 시간
            </label>
            <Input v-model="form.startTime" type="time" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              종료 시간
            </label>
            <Input v-model="form.endTime" type="time" />
          </div>
        </template>
      </div>

      <!-- 장소 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
          장소
        </label>
        <Input v-model="form.location" placeholder="장소 (선택)" />
      </div>

      <!-- 설명 -->
      <div>
        <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
          설명
        </label>
        <textarea
          v-model="form.description"
          rows="3"
          class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
          placeholder="설명 (선택)"
        />
      </div>

      <!-- 반복 설정 -->
      <div class="border-t border-gray-200 dark:border-gray-700 pt-4">
        <label class="flex items-center gap-2 cursor-pointer mb-3">
          <input
            v-model="form.isRecurring"
            type="checkbox"
            class="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
          />
          <span class="text-sm font-medium text-gray-700 dark:text-gray-300">반복</span>
        </label>

        <div v-if="form.isRecurring" class="grid grid-cols-2 gap-4 pl-6">
          <div>
            <label class="block text-sm text-gray-600 dark:text-gray-400 mb-1">
              반복 주기
            </label>
            <Select
              v-model="form.recurrenceType"
              :options="recurrenceTypeOptions"
            />
          </div>
          <div>
            <label class="block text-sm text-gray-600 dark:text-gray-400 mb-1">
              반복 간격
            </label>
            <Input
              v-model.number="form.recurrenceInterval"
              type="number"
              min="1"
              max="99"
            />
          </div>
          <div>
            <label class="block text-sm text-gray-600 dark:text-gray-400 mb-1">
              종료일 (선택)
            </label>
            <Input v-model="form.recurrenceEndDate" type="date" />
          </div>
          <div>
            <label class="block text-sm text-gray-600 dark:text-gray-400 mb-1">
              반복 횟수 (선택)
            </label>
            <input
              v-model.number="form.recurrenceCount"
              type="number"
              min="1"
              placeholder="무제한"
              class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
              @input="handleRecurrenceCountInput"
              @keydown="preventNegativeInput"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 푸터 -->
    <template #footer>
      <div class="flex items-center justify-between">
        <div>
          <button
            v-if="isEditMode"
            type="button"
            class="px-4 py-2 text-sm text-red-600 hover:text-red-700 hover:bg-red-50 rounded-lg transition-colors dark:text-red-400 dark:hover:bg-red-900/30"
            :disabled="deleting"
            @click="handleDelete"
          >
            {{ deleting ? '삭제 중...' : '삭제' }}
          </button>
        </div>
        <div class="flex gap-3">
          <button
            type="button"
            class="px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-lg transition-colors dark:text-gray-300 dark:hover:bg-gray-700"
            @click="handleClose"
          >
            취소
          </button>
          <button
            type="button"
            class="px-4 py-2 text-sm bg-primary-600 text-white hover:bg-primary-700 rounded-lg transition-colors disabled:opacity-50"
            :disabled="saving"
            @click="handleSave"
          >
            {{ saving ? '저장 중...' : (isEditMode ? '수정' : '생성') }}
          </button>
        </div>
      </div>
    </template>
  </Modal>
</template>
