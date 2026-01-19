<template>
  <div class="lunar-date-picker">
    <!-- 날짜 유형 선택 -->
    <div class="date-type-selector mb-3">
      <label class="inline-flex items-center mr-4 cursor-pointer">
        <input
          type="radio"
          :value="false"
          v-model="isLunar"
          class="form-radio text-blue-600"
        />
        <span class="ml-2 text-sm text-gray-700 dark:text-gray-300">양력</span>
      </label>
      <label class="inline-flex items-center cursor-pointer">
        <input
          type="radio"
          :value="true"
          v-model="isLunar"
          class="form-radio text-blue-600"
        />
        <span class="ml-2 text-sm text-gray-700 dark:text-gray-300">음력</span>
      </label>
    </div>

    <!-- 양력 선택 -->
    <div v-if="!isLunar" class="solar-picker">
      <input
        type="date"
        v-model="solarDate"
        class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md
               bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100
               focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        :min="minDate"
        :max="maxDate"
      />
      <!-- 음력 변환 결과 표시 -->
      <div v-if="convertedLunar" class="mt-2 text-sm text-gray-500 dark:text-gray-400">
        음력: {{ convertedLunar.year }}년 {{ convertedLunar.isLeapMonth ? '윤' : '' }}{{ convertedLunar.month }}월 {{ convertedLunar.day }}일
      </div>
    </div>

    <!-- 음력 선택 -->
    <div v-else class="lunar-picker">
      <div class="flex gap-2">
        <!-- 연도 -->
        <select
          v-model="lunarYear"
          class="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md
                 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100
                 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        >
          <option v-for="year in yearOptions" :key="year" :value="year">
            {{ year }}년
          </option>
        </select>

        <!-- 월 -->
        <select
          v-model="lunarMonth"
          class="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md
                 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100
                 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        >
          <option v-for="month in monthOptions" :key="month" :value="month">
            {{ month }}월
          </option>
        </select>

        <!-- 일 -->
        <select
          v-model="lunarDay"
          class="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md
                 bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100
                 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        >
          <option v-for="day in dayOptions" :key="day" :value="day">
            {{ day }}일
          </option>
        </select>
      </div>

      <!-- 윤달 체크박스 -->
      <div class="mt-2 flex items-center">
        <label class="inline-flex items-center cursor-pointer">
          <input
            type="checkbox"
            v-model="isLeapMonth"
            :disabled="!hasLeapMonthOption"
            class="form-checkbox text-blue-600 rounded"
          />
          <span
            class="ml-2 text-sm"
            :class="hasLeapMonthOption ? 'text-gray-700 dark:text-gray-300' : 'text-gray-400 dark:text-gray-500'"
          >
            윤달
          </span>
        </label>
        <span v-if="leapMonthOfYear" class="ml-2 text-xs text-gray-500 dark:text-gray-400">
          ({{ lunarYear }}년 {{ leapMonthOfYear }}월에 윤달 있음)
        </span>
      </div>

      <!-- 양력 변환 결과 표시 -->
      <div v-if="convertedSolar" class="mt-2 text-sm text-gray-500 dark:text-gray-400">
        양력: {{ convertedSolar }}
      </div>

      <!-- 유효성 오류 -->
      <div v-if="validationError" class="mt-2 text-sm text-red-500">
        {{ validationError }}
      </div>
    </div>

    <!-- 음력 기념일 바로가기 -->
    <div v-if="isLunar && showHolidays" class="mt-3">
      <div class="text-xs text-gray-500 dark:text-gray-400 mb-1">음력 기념일:</div>
      <div class="flex flex-wrap gap-1">
        <button
          v-for="holiday in lunarHolidays"
          :key="holiday.name"
          @click="selectHoliday(holiday)"
          class="px-2 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300
                 rounded hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
        >
          {{ holiday.name }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import {
  solarToLunar,
  lunarToSolar,
  isValidLunarDate,
  getLunarMonthDays,
  hasLeapMonth,
  LUNAR_HOLIDAYS,
  type LunarDate
} from '@/utils/lunar'

// Props
const props = withDefaults(defineProps<{
  modelValue?: string         // 양력 날짜 (YYYY-MM-DD)
  lunarValue?: {              // 음력 날짜
    year: number
    month: number
    day: number
    isLeapMonth: boolean
  }
  isLunarMode?: boolean       // 외부에서 음력 모드 제어
  showHolidays?: boolean      // 음력 기념일 바로가기 표시
  minYear?: number
  maxYear?: number
}>(), {
  isLunarMode: false,
  showHolidays: true,
  minYear: 1950,
  maxYear: 2050
})

// Emits
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'update:lunarValue', value: { year: number; month: number; day: number; isLeapMonth: boolean }): void
  (e: 'update:isLunarMode', value: boolean): void
  (e: 'change', data: { solar: string; lunar: LunarDate; isLunar: boolean }): void
}>()

// State
const isLunar = ref(false)
const solarDate = ref('')
const lunarYear = ref(new Date().getFullYear())
const lunarMonth = ref(1)
const lunarDay = ref(1)
const isLeapMonth = ref(false)
const convertedLunar = ref<LunarDate | null>(null)
const convertedSolar = ref<string | null>(null)
const validationError = ref('')
const leapMonthOfYear = ref<number | null>(null)

// Computed
const minDate = computed(() => `${props.minYear}-01-01`)
const maxDate = computed(() => `${props.maxYear}-12-31`)

const yearOptions = computed(() => {
  const years: number[] = []
  for (let y = props.minYear; y <= props.maxYear; y++) {
    years.push(y)
  }
  return years
})

const monthOptions = computed(() => {
  const months: number[] = []
  for (let m = 1; m <= 12; m++) {
    months.push(m)
  }
  return months
})

const dayOptions = computed(() => {
  const days: number[] = []
  const maxDays = getLunarMonthDays(lunarYear.value, lunarMonth.value, isLeapMonth.value)
  for (let d = 1; d <= maxDays; d++) {
    days.push(d)
  }
  return days
})

const hasLeapMonthOption = computed(() => {
  return hasLeapMonth(lunarYear.value, lunarMonth.value)
})

const lunarHolidays = LUNAR_HOLIDAYS

// Watch: 양력 날짜 변경 시 음력 변환
watch(solarDate, (newDate) => {
  if (!newDate || isLunar.value) return

  try {
    convertedLunar.value = solarToLunar(newDate)
    emit('update:modelValue', newDate)
    emit('update:lunarValue', {
      year: convertedLunar.value.year,
      month: convertedLunar.value.month,
      day: convertedLunar.value.day,
      isLeapMonth: convertedLunar.value.isLeapMonth
    })
    emit('change', {
      solar: newDate,
      lunar: convertedLunar.value,
      isLunar: false
    })
  } catch (e) {
    console.warn('Lunar conversion failed:', e)
    convertedLunar.value = null
  }
})

// Watch: 음력 날짜 변경 시 양력 변환
watch([lunarYear, lunarMonth, lunarDay, isLeapMonth], () => {
  if (!isLunar.value) return
  updateLeapMonthInfo()
  convertLunarToSolar()
}, { immediate: true })

// Watch: 연도 변경 시 윤달 정보 업데이트
watch(lunarYear, () => {
  updateLeapMonthInfo()
  // 현재 선택된 월이 윤달이 아니면 윤달 체크 해제
  if (!hasLeapMonthOption.value) {
    isLeapMonth.value = false
  }
})

// Watch: 음력/양력 모드 전환
watch(isLunar, (newValue) => {
  emit('update:isLunarMode', newValue)

  if (newValue) {
    // 양력 → 음력 전환: 현재 양력 날짜를 음력으로 변환
    // props.modelValue 우선, 없으면 solarDate.value, 그것도 없으면 오늘 날짜
    const dateToConvert = props.modelValue || solarDate.value || new Date().toISOString().split('T')[0]
    solarDate.value = dateToConvert

    // 양력을 음력으로 변환
    const lunar = solarToLunar(dateToConvert)

    // 유효한 음력 값인지 확인
    if (lunar.year >= props.minYear && lunar.year <= props.maxYear &&
        lunar.month >= 1 && lunar.month <= 12 &&
        lunar.day >= 1 && lunar.day <= 30) {
      lunarYear.value = lunar.year
      lunarMonth.value = lunar.month
      lunarDay.value = lunar.day
      isLeapMonth.value = lunar.isLeapMonth
    } else {
      // 변환 실패 시 양력 날짜 기반으로 설정
      const [year, month, day] = dateToConvert.split('-').map(Number)
      lunarYear.value = year
      lunarMonth.value = month
      lunarDay.value = Math.min(day, getLunarMonthDays(year, month, false))
      isLeapMonth.value = false
      console.warn('Lunar conversion returned invalid values, using solar date as fallback')
    }

    // 변환된 양력 표시 업데이트
    convertedSolar.value = dateToConvert
    validationError.value = ''
  } else {
    // 음력 → 양력 전환: 현재 음력 날짜를 양력으로 변환
    if (convertedSolar.value) {
      solarDate.value = convertedSolar.value
    }
  }
})

// Watch: 외부 isLunarMode prop 변경 시 동기화
watch(() => props.isLunarMode, (newValue) => {
  if (isLunar.value !== newValue) {
    isLunar.value = newValue
  }
}, { immediate: true })

// Watch: 외부 modelValue (양력 날짜) 변경 시 동기화
watch(() => props.modelValue, (newValue) => {
  if (newValue && newValue !== solarDate.value) {
    solarDate.value = newValue
    // 음력 모드일 때는 음력 값도 업데이트
    if (isLunar.value) {
      const lunar = solarToLunar(newValue)
      if (lunar.year >= props.minYear && lunar.year <= props.maxYear &&
          lunar.month >= 1 && lunar.month <= 12 &&
          lunar.day >= 1 && lunar.day <= 30) {
        lunarYear.value = lunar.year
        lunarMonth.value = lunar.month
        lunarDay.value = lunar.day
        isLeapMonth.value = lunar.isLeapMonth
      }
    }
  }
}, { immediate: true })

// Methods
function updateLeapMonthInfo() {
  const leapMonth = hasLeapMonth(lunarYear.value, lunarMonth.value)
  if (leapMonth) {
    leapMonthOfYear.value = lunarMonth.value
  } else {
    // 해당 연도의 윤달 월 찾기
    for (let m = 1; m <= 12; m++) {
      if (hasLeapMonth(lunarYear.value, m)) {
        leapMonthOfYear.value = m
        return
      }
    }
    leapMonthOfYear.value = null
  }
}

function convertLunarToSolar() {
  validationError.value = ''

  // 기본 범위 검증만 수행
  if (lunarYear.value < props.minYear || lunarYear.value > props.maxYear ||
      lunarMonth.value < 1 || lunarMonth.value > 12 ||
      lunarDay.value < 1 || lunarDay.value > 30) {
    validationError.value = '유효하지 않은 음력 날짜입니다'
    convertedSolar.value = null
    return
  }

  try {
    const solar = lunarToSolar(lunarYear.value, lunarMonth.value, lunarDay.value, isLeapMonth.value)
    if (solar) {
      convertedSolar.value = solar
      emit('update:modelValue', solar)
      emit('update:lunarValue', {
        year: lunarYear.value,
        month: lunarMonth.value,
        day: lunarDay.value,
        isLeapMonth: isLeapMonth.value
      })
      emit('change', {
        solar,
        lunar: {
          year: lunarYear.value,
          month: lunarMonth.value,
          day: lunarDay.value,
          isLeapMonth: isLeapMonth.value,
          displayText: `${isLeapMonth.value ? '윤' : ''}${lunarMonth.value}.${lunarDay.value}`
        },
        isLunar: true
      })
    } else {
      // 변환 실패해도 입력은 허용 (일부 음력 날짜는 라이브러리 한계로 변환 불가)
      convertedSolar.value = null
      validationError.value = '양력 변환을 확인할 수 없습니다. 다른 날짜를 선택해주세요.'
      // emit은 하지 않음 - 사용자가 다른 날짜 선택 유도
    }
  } catch (e) {
    console.warn('Solar conversion failed:', e)
    validationError.value = '양력 변환에 실패했습니다'
    convertedSolar.value = null
  }
}

function selectHoliday(holiday: typeof LUNAR_HOLIDAYS[0]) {
  lunarMonth.value = holiday.month
  lunarDay.value = holiday.day
  isLeapMonth.value = holiday.isLeapMonth
}

// Initialize
onMounted(() => {
  // 기본 양력 날짜 설정
  const today = new Date().toISOString().split('T')[0]

  if (props.modelValue) {
    solarDate.value = props.modelValue
  } else {
    // 기본값: 오늘 날짜
    solarDate.value = today
  }

  // 양력 → 음력 변환 결과 저장
  let lunar: LunarDate
  try {
    lunar = solarToLunar(solarDate.value)

    // 유효한 음력 값인지 확인
    if (lunar.year < props.minYear || lunar.year > props.maxYear ||
        lunar.month < 1 || lunar.month > 12 ||
        lunar.day < 1 || lunar.day > 30) {
      // 변환 실패 시 기본값 사용 (현재 연도, 1월 1일)
      const [year] = solarDate.value.split('-').map(Number)
      lunar = {
        year: year,
        month: 1,
        day: 1,
        isLeapMonth: false,
        displayText: '1.1'
      }
    }
  } catch (e) {
    // 에러 시 기본값 사용 (현재 연도, 1월 1일)
    const [year] = solarDate.value.split('-').map(Number)
    lunar = {
      year: year,
      month: 1,
      day: 1,
      isLeapMonth: false,
      displayText: '1.1'
    }
    console.warn('Initial lunar conversion failed:', e)
  }

  convertedLunar.value = lunar

  if (props.lunarValue) {
    // 외부에서 음력 값이 지정된 경우
    isLunar.value = true
    lunarYear.value = props.lunarValue.year
    lunarMonth.value = props.lunarValue.month
    lunarDay.value = props.lunarValue.day
    isLeapMonth.value = props.lunarValue.isLeapMonth
  } else if (props.isLunarMode) {
    // 외부에서 음력 모드가 지정된 경우 - 현재 양력 날짜의 음력으로 초기화
    isLunar.value = true
    lunarYear.value = lunar.year
    lunarMonth.value = lunar.month
    lunarDay.value = lunar.day
    isLeapMonth.value = lunar.isLeapMonth
    convertedSolar.value = solarDate.value
  }

  updateLeapMonthInfo()
})
</script>

<style scoped>
.lunar-date-picker {
  @apply p-4 bg-white dark:bg-gray-800 rounded-lg;
}

.form-radio {
  @apply h-4 w-4 text-blue-600 border-gray-300 focus:ring-blue-500;
}

.form-checkbox {
  @apply h-4 w-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500;
}
</style>
