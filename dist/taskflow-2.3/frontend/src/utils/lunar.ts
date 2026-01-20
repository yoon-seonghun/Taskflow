/**
 * 음력 변환 유틸리티
 * korean-lunar-calendar 라이브러리 사용
 */
import KoreanLunarCalendar from 'korean-lunar-calendar'

/**
 * 음력 날짜 정보
 */
export interface LunarDate {
  year: number
  month: number
  day: number
  isLeapMonth: boolean
  displayText: string  // "12.5" 또는 "윤4.1"
}

/**
 * 음력 변환 결과 (API 응답용)
 */
export interface LunarConversionResult {
  solar: string
  lunar: LunarDate
}

// 월간 음력 데이터 캐시
const lunarCache = new Map<string, LunarDate>()

/**
 * 양력 → 음력 변환
 * @param solarDate 양력 날짜 (YYYY-MM-DD 형식)
 * @returns 음력 날짜 정보
 */
export function solarToLunar(solarDate: string): LunarDate {
  // 캐시 확인
  if (lunarCache.has(solarDate)) {
    return lunarCache.get(solarDate)!
  }

  try {
    const calendar = new KoreanLunarCalendar()
    const [year, month, day] = solarDate.split('-').map(Number)

    // 유효 범위 확인 (1000-2050)
    if (year < 1000 || year > 2050) {
      return {
        year,
        month,
        day,
        isLeapMonth: false,
        displayText: '-'
      }
    }

    // setSolarDate 반환값으로 유효성 확인
    const isValid = calendar.setSolarDate(year, month, day)
    if (!isValid) {
      console.warn(`Invalid solar date: ${solarDate}`)
      return {
        year,
        month,
        day,
        isLeapMonth: false,
        displayText: '-'
      }
    }

    // getLunarCalendar() 메서드로 음력 정보 가져오기
    const lunarData = calendar.getLunarCalendar()

    const result: LunarDate = {
      year: lunarData.year,
      month: lunarData.month,
      day: lunarData.day,
      isLeapMonth: lunarData.intercalation || false,
      displayText: `${lunarData.intercalation ? '윤' : ''}${lunarData.month}.${lunarData.day}`
    }

    // 캐시에 저장
    lunarCache.set(solarDate, result)

    return result
  } catch (e) {
    console.warn(`Lunar conversion failed for ${solarDate}:`, e)
    return {
      year: 0,
      month: 0,
      day: 0,
      isLeapMonth: false,
      displayText: '-'
    }
  }
}

/**
 * 음력 → 양력 변환
 * @param year 음력 연도
 * @param month 음력 월
 * @param day 음력 일
 * @param isLeapMonth 윤달 여부
 * @returns 양력 날짜 (YYYY-MM-DD 형식)
 */
export function lunarToSolar(
  year: number,
  month: number,
  day: number,
  isLeapMonth: boolean = false
): string | null {
  try {
    // 유효 범위 확인 (음력: 1000-01-01 ~ 2050-11-18)
    if (year < 1000 || year > 2050) {
      console.warn(`Year ${year} is out of range (1000-2050)`)
      return null
    }

    const calendar = new KoreanLunarCalendar()

    // setLunarDate 반환값으로 유효성 확인
    const isValid = calendar.setLunarDate(year, month, day, isLeapMonth)
    if (!isValid) {
      console.warn(`Invalid lunar date: ${year}-${month}-${day} (leap: ${isLeapMonth})`)
      return null
    }

    // getSolarCalendar() 메서드로 양력 정보 가져오기
    const solarData = calendar.getSolarCalendar()

    // 변환 결과 검증
    if (!solarData || !solarData.year || !solarData.month || !solarData.day) {
      console.warn(`Lunar to solar conversion returned invalid:`, solarData)
      return null
    }

    return `${solarData.year}-${String(solarData.month).padStart(2, '0')}-${String(solarData.day).padStart(2, '0')}`
  } catch (e) {
    console.warn(`Solar conversion failed for lunar ${year}-${month}-${day}:`, e)
    return null
  }
}

/**
 * 음력 날짜 유효성 검증
 * @param year 음력 연도
 * @param month 음력 월
 * @param day 음력 일
 * @param isLeapMonth 윤달 여부
 * @returns 유효 여부
 */
export function isValidLunarDate(
  year: number,
  month: number,
  day: number,
  isLeapMonth: boolean = false
): boolean {
  try {
    // 기본 범위 검증
    if (year < 1000 || year > 2050) return false
    if (month < 1 || month > 12) return false
    if (day < 1 || day > 30) return false

    const calendar = new KoreanLunarCalendar()

    // setLunarDate가 false를 반환하면 유효하지 않은 날짜
    return calendar.setLunarDate(year, month, day, isLeapMonth)
  } catch (e) {
    console.warn('Lunar date validation failed:', e)
    return false
  }
}

/**
 * 특정 음력 월의 일수 조회
 * @param year 음력 연도
 * @param month 음력 월
 * @param isLeapMonth 윤달 여부
 * @returns 해당 월의 일수 (29 또는 30)
 */
export function getLunarMonthDays(
  year: number,
  month: number,
  isLeapMonth: boolean = false
): number {
  try {
    const calendar = new KoreanLunarCalendar()

    // 30일이 유효한지 확인
    const isValid30 = calendar.setLunarDate(year, month, 30, isLeapMonth)
    if (isValid30) {
      return 30
    }

    // 29일이 유효한지 확인
    const isValid29 = calendar.setLunarDate(year, month, 29, isLeapMonth)
    if (isValid29) {
      return 29
    }

    // 기본값
    return 29
  } catch (e) {
    // 에러 시 기본값 29일 반환
    return 29
  }
}

/**
 * 특정 연도에 윤달이 있는지 확인
 * @param year 음력 연도
 * @param month 음력 월
 * @returns 해당 월에 윤달이 있는지 여부
 */
export function hasLeapMonth(year: number, month: number): boolean {
  try {
    const calendar = new KoreanLunarCalendar()
    // 윤달 1일이 유효하면 해당 월에 윤달이 있는 것
    return calendar.setLunarDate(year, month, 1, true)
  } catch (e) {
    return false
  }
}

/**
 * 월간 뷰용 음력 데이터 사전 계산
 * 캘린더 렌더링 성능 최적화
 * @param year 양력 연도
 * @param month 양력 월 (1-12)
 * @returns 날짜별 음력 정보 맵
 */
export function precomputeMonthLunarDates(
  year: number,
  month: number
): Map<string, LunarDate> {
  const cache = new Map<string, LunarDate>()

  // 이전 달 일부 + 현재 달 + 다음 달 일부 (약 42일)
  for (let offset = -7; offset <= 35; offset++) {
    const date = new Date(year, month - 1, offset + 1)
    const dateStr = formatDateToISO(date)
    const lunarDate = solarToLunar(dateStr)
    cache.set(dateStr, lunarDate)
  }

  return cache
}

/**
 * 다음 음력 날짜의 양력 계산 (반복 이벤트용)
 * @param baseYear 시작 연도
 * @param lunarMonth 음력 월
 * @param lunarDay 음력 일
 * @param isLeapMonth 윤달 여부
 * @param count 생성할 날짜 수
 * @returns 양력 날짜 배열
 */
export function getNextLunarDates(
  baseYear: number,
  lunarMonth: number,
  lunarDay: number,
  isLeapMonth: boolean,
  count: number = 10
): string[] {
  const results: string[] = []

  for (let i = 0; i < count && results.length < count; i++) {
    const targetYear = baseYear + i

    // 해당 연도의 음력 날짜가 유효한지 확인
    if (isValidLunarDate(targetYear, lunarMonth, lunarDay, isLeapMonth)) {
      const solarDate = lunarToSolar(targetYear, lunarMonth, lunarDay, isLeapMonth)
      if (solarDate) {
        results.push(solarDate)
      }
    } else if (isLeapMonth) {
      // 윤달이 없는 연도면 스킵
      console.debug(`Leap month ${lunarMonth} not available in ${targetYear}, skipping`)
    } else if (lunarDay === 30) {
      // 30일이 없는 달이면 29일로 대체
      const fallbackDate = lunarToSolar(targetYear, lunarMonth, 29, isLeapMonth)
      if (fallbackDate) {
        results.push(fallbackDate)
      }
    }
  }

  return results
}

/**
 * 음력 기념일 목록 (참조용)
 */
export const LUNAR_HOLIDAYS = [
  { name: '설날', month: 1, day: 1, isLeapMonth: false },
  { name: '정월대보름', month: 1, day: 15, isLeapMonth: false },
  { name: '단오', month: 5, day: 5, isLeapMonth: false },
  { name: '칠석', month: 7, day: 7, isLeapMonth: false },
  { name: '백중', month: 7, day: 15, isLeapMonth: false },
  { name: '추석', month: 8, day: 15, isLeapMonth: false },
  { name: '중양절', month: 9, day: 9, isLeapMonth: false }
]

/**
 * 날짜 포맷팅 헬퍼 (Date -> YYYY-MM-DD)
 */
function formatDateToISO(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 캐시 초기화 (메모리 관리용)
 */
export function clearLunarCache(): void {
  lunarCache.clear()
}

/**
 * 캐시 크기 조회
 */
export function getLunarCacheSize(): number {
  return lunarCache.size
}
