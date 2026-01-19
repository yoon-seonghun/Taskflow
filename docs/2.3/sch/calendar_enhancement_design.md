# 캘린더 추가 개선 설계서

> 버전: v1.0
> 작성일: 2026-01-18
> 상태: 승인 대기
> 기반 문서: calendar_gantt_design.md (v3.0)

---

## 1. 개선 범위 요약

| 구분 | 기능 | 우선순위 | 복잡도 |
|-----|------|---------|--------|
| 1 | 캘린더(이벤트 그룹) 관리 | 상 | 상 |
| 2 | 이벤트 CRUD | 상 | 중 |
| 3 | 이벤트 공유/이관 | 상 | 상 |
| 4 | 셀 표시 개선 (3줄 + 더보기) | 중 | 하 |
| 5 | 개별 항목 호버 툴팁 | 중 | 중 |
| 6 | 더보기 호버 전체 리스트 | 중 | 중 |
| 7 | 음력 날짜 표시 | 중 | 중 |
| 8 | 음력 이벤트 등록 | 중 | 상 |
| 9 | 음력 반복 이벤트 | 하 | 상 |
| 10 | 주간 뷰 (타임그리드) | 하 | 상 |

---

## 2. ERD 및 테이블 정의

### 2.1 테이블 관계도

```
┌──────────────────┐
│    TB_CALENDAR   │  ← 캘린더 (이벤트 그룹/색상)
│  CALENDAR_ID (PK)│
│  OWNER_USERNAME  │
│  CALENDAR_NAME   │
│  COLOR           │
└────────┬─────────┘
         │ 1:N
         ▼
┌──────────────────┐      ┌──────────────────┐
│    TB_EVENT      │      │ TB_EVENT_SHARE   │
│  EVENT_ID (PK)   │◄────►│ EVENT_SHARE_ID   │
│  CALENDAR_ID(FK) │ 1:N  │ EVENT_ID (FK)    │
│  OWNER_USERNAME  │      │ USERNAME         │
│  IS_LUNAR        │      │ SHARE_TYPE       │
│  LUNAR_DATE      │      │ SHARED_BY        │
└──────────────────┘      └──────────────────┘
```

### 2.2 TB_CALENDAR (캘린더/이벤트 그룹)

```sql
CREATE TABLE TB_CALENDAR (
    CALENDAR_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    OWNER_USERNAME VARCHAR(50) NOT NULL COMMENT '소유자 (FK → TB_USER.USERNAME)',
    CALENDAR_NAME VARCHAR(100) NOT NULL COMMENT '캘린더명',
    DESCRIPTION VARCHAR(500) NULL COMMENT '설명',
    COLOR VARCHAR(20) NOT NULL DEFAULT '#3b82f6' COMMENT '표시 색상 (HEX)',
    IS_DEFAULT BOOLEAN NOT NULL DEFAULT FALSE COMMENT '기본 캘린더 여부',
    SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    USE_YN CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR(50) NOT NULL,
    UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    UPDATED_BY VARCHAR(50) NULL,
    PRIMARY KEY (CALENDAR_ID),
    INDEX IDX_CALENDAR_OWNER (OWNER_USERNAME),
    CONSTRAINT FK_CALENDAR_USER FOREIGN KEY (OWNER_USERNAME) REFERENCES TB_USER(USERNAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='캘린더 (이벤트 그룹)';
```

### 2.3 TB_EVENT (이벤트)

```sql
CREATE TABLE TB_EVENT (
    EVENT_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    CALENDAR_ID BIGINT NOT NULL COMMENT 'FK → TB_CALENDAR',
    OWNER_USERNAME VARCHAR(50) NOT NULL COMMENT '소유자 (FK → TB_USER.USERNAME)',
    TITLE VARCHAR(200) NOT NULL COMMENT '제목',
    DESCRIPTION TEXT NULL COMMENT '설명',
    LOCATION VARCHAR(200) NULL COMMENT '장소',

    -- 날짜/시간
    START_DATE DATE NOT NULL COMMENT '시작일',
    END_DATE DATE NULL COMMENT '종료일 (NULL이면 당일)',
    START_TIME TIME NULL COMMENT '시작 시간 (NULL이면 종일)',
    END_TIME TIME NULL COMMENT '종료 시간',
    IS_ALL_DAY BOOLEAN NOT NULL DEFAULT TRUE COMMENT '종일 이벤트 여부',

    -- 음력 관련
    IS_LUNAR BOOLEAN NOT NULL DEFAULT FALSE COMMENT '음력 여부',
    LUNAR_MONTH INT NULL COMMENT '음력 월 (1-12)',
    LUNAR_DAY INT NULL COMMENT '음력 일 (1-30)',
    LUNAR_LEAP_MONTH BOOLEAN NOT NULL DEFAULT FALSE COMMENT '음력 윤달 여부',

    -- 반복 관련
    IS_RECURRING BOOLEAN NOT NULL DEFAULT FALSE COMMENT '반복 여부',
    RECURRENCE_TYPE VARCHAR(20) NULL COMMENT '반복 유형: DAILY, WEEKLY, MONTHLY, YEARLY, MONTHLY_LUNAR, YEARLY_LUNAR',
    RECURRENCE_INTERVAL INT NULL DEFAULT 1 COMMENT '반복 간격 (매 N일/주/월/년)',
    RECURRENCE_END_DATE DATE NULL COMMENT '반복 종료일 (NULL이면 무제한)',
    RECURRENCE_COUNT INT NULL COMMENT '반복 횟수 (NULL이면 무제한)',
    RECURRENCE_DAYS VARCHAR(20) NULL COMMENT '반복 요일 (WEEKLY용): "0,1,2,3,4,5,6" (일~토)',
    PARENT_EVENT_ID BIGINT NULL COMMENT '반복 원본 이벤트 ID',

    -- 상태
    STATUS VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, CANCELLED, DELETED',

    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR(50) NOT NULL,
    UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    UPDATED_BY VARCHAR(50) NULL,

    PRIMARY KEY (EVENT_ID),
    INDEX IDX_EVENT_CALENDAR (CALENDAR_ID),
    INDEX IDX_EVENT_OWNER (OWNER_USERNAME),
    INDEX IDX_EVENT_DATE (START_DATE, END_DATE),
    INDEX IDX_EVENT_PARENT (PARENT_EVENT_ID),
    CONSTRAINT FK_EVENT_CALENDAR FOREIGN KEY (CALENDAR_ID) REFERENCES TB_CALENDAR(CALENDAR_ID),
    CONSTRAINT FK_EVENT_USER FOREIGN KEY (OWNER_USERNAME) REFERENCES TB_USER(USERNAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='이벤트';
```

### 2.4 TB_EVENT_SHARE (이벤트 공유)

```sql
CREATE TABLE TB_EVENT_SHARE (
    EVENT_SHARE_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    EVENT_ID BIGINT NOT NULL COMMENT 'FK → TB_EVENT',
    SHARE_TYPE VARCHAR(20) NOT NULL DEFAULT 'VIEW' COMMENT 'VIEW(조회), EDIT(편집), TRANSFER(이관)',
    USERNAME VARCHAR(50) NOT NULL COMMENT '공유 대상자 (FK → TB_USER.USERNAME)',
    SHARED_BY VARCHAR(50) NOT NULL COMMENT '공유한 사용자 (FK → TB_USER.USERNAME)',
    SHARED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '공유 시간',
    DELETED_AT DATETIME NULL COMMENT '공유 해제 시간',

    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR(50) NOT NULL,
    UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    UPDATED_BY VARCHAR(50) NULL,

    PRIMARY KEY (EVENT_SHARE_ID),
    INDEX IDX_EVENT_SHARE_EVENT (EVENT_ID),
    INDEX IDX_EVENT_SHARE_USER (USERNAME),
    INDEX IDX_EVENT_SHARE_TYPE (SHARE_TYPE),
    UNIQUE KEY UK_EVENT_SHARE (EVENT_ID, USERNAME, DELETED_AT),
    CONSTRAINT FK_EVENT_SHARE_EVENT FOREIGN KEY (EVENT_ID) REFERENCES TB_EVENT(EVENT_ID),
    CONSTRAINT FK_EVENT_SHARE_USER FOREIGN KEY (USERNAME) REFERENCES TB_USER(USERNAME),
    CONSTRAINT FK_EVENT_SHARE_BY FOREIGN KEY (SHARED_BY) REFERENCES TB_USER(USERNAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='이벤트 공유';
```

### 2.5 TB_CALENDAR_SHARE (캘린더 공유 - 선택적)

```sql
CREATE TABLE TB_CALENDAR_SHARE (
    CALENDAR_SHARE_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT 'PK',
    CALENDAR_ID BIGINT NOT NULL COMMENT 'FK → TB_CALENDAR',
    SHARE_TYPE VARCHAR(20) NOT NULL DEFAULT 'VIEW' COMMENT 'VIEW(조회), EDIT(편집)',
    USERNAME VARCHAR(50) NOT NULL COMMENT '공유 대상자',
    SHARED_BY VARCHAR(50) NOT NULL COMMENT '공유한 사용자',
    SHARED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    DELETED_AT DATETIME NULL,

    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR(50) NOT NULL,
    UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    UPDATED_BY VARCHAR(50) NULL,

    PRIMARY KEY (CALENDAR_SHARE_ID),
    INDEX IDX_CALENDAR_SHARE_CAL (CALENDAR_ID),
    INDEX IDX_CALENDAR_SHARE_USER (USERNAME),
    UNIQUE KEY UK_CALENDAR_SHARE (CALENDAR_ID, USERNAME, DELETED_AT),
    CONSTRAINT FK_CALENDAR_SHARE_CAL FOREIGN KEY (CALENDAR_ID) REFERENCES TB_CALENDAR(CALENDAR_ID),
    CONSTRAINT FK_CALENDAR_SHARE_USER FOREIGN KEY (USERNAME) REFERENCES TB_USER(USERNAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='캘린더 공유';
```

---

## 3. 이벤트 공유/이관 기능

### 3.1 공유 유형 정의

| 유형 | 코드 | 권한 | 설명 |
|-----|------|------|------|
| 조회 공유 | VIEW | 조회만 가능 | 편집/삭제 불가, 공유받은 캘린더에 표시 |
| 편집 공유 | EDIT | 조회+수정 | 삭제는 불가 |
| 이관 | TRANSFER | 소유권 이전 | 완전 이관, 기존 소유자 접근 불가 |

### 3.2 이관 프로세스

```
[이관 흐름]
1. 소유자가 이벤트 이관 요청 (targetUsername 지정)
2. 검증:
   - 본인 소유 이벤트인지 확인
   - 대상 사용자 존재 여부 확인
3. 처리:
   - TB_EVENT.OWNER_USERNAME 변경
   - TB_EVENT.CALENDAR_ID → 대상자 기본 캘린더로 변경
   - TB_EVENT_SHARE에 TRANSFER 이력 기록
   - 기존 공유 관계 모두 해제 (DELETED_AT 설정)
4. 알림:
   - 이관받는 사용자에게 알림 발송
```

### 3.3 공유받은 이벤트 UI 표현

```
[내 이벤트]
┌────────────────────────┐
│ ● 미팅 일정            │  ← 캘린더 색상 100%
└────────────────────────┘

[공유받은 이벤트 - VIEW]
┌─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┐
│ 👤 미팅 일정           │  ← 점선 테두리, 50% 투명도
│    (홍길동)            │  ← 공유자 이름
└─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─┘

[공유받은 이벤트 - EDIT]
┌────────────────────────┐
│ ✏️ 미팅 일정           │  ← 실선 테두리, 50% 투명도
│    (홍길동)            │  ← 공유자 이름
└────────────────────────┘
```

### 3.4 공유 관계 조회 쿼리

```sql
-- 내 이벤트 + 공유받은 이벤트 조회
SELECT
    e.*,
    c.CALENDAR_NAME,
    c.COLOR,
    CASE
        WHEN e.OWNER_USERNAME = #{username} THEN 'OWNER'
        ELSE es.SHARE_TYPE
    END as accessType,
    CASE
        WHEN e.OWNER_USERNAME != #{username} THEN u.NAME
        ELSE NULL
    END as sharedByUserName,
    CASE
        WHEN e.OWNER_USERNAME != #{username} THEN 1
        ELSE 0
    END as isShared
FROM TB_EVENT e
INNER JOIN TB_CALENDAR c ON e.CALENDAR_ID = c.CALENDAR_ID
LEFT JOIN TB_EVENT_SHARE es ON e.EVENT_ID = es.EVENT_ID
    AND es.USERNAME = #{username}
    AND es.DELETED_AT IS NULL
LEFT JOIN TB_USER u ON e.OWNER_USERNAME = u.USERNAME
WHERE e.STATUS != 'DELETED'
  AND (
      e.OWNER_USERNAME = #{username}
      OR es.EVENT_SHARE_ID IS NOT NULL
  )
  AND e.START_DATE <= #{endDate}
  AND COALESCE(e.END_DATE, e.START_DATE) >= #{startDate}
ORDER BY e.START_DATE, e.START_TIME;
```

---

## 4. 셀 표시 개선

### 4.1 기본 표시 규칙 (3줄 + 더보기)

```
┌────────────────────────────┐
│ 15 (음력 12.5)             │  ← 날짜 + 음력
├────────────────────────────┤
│ ● 미팅 일정                │  ← 1줄: 이벤트 (색상 점)
│ ■ 프로젝트 마감            │  ← 2줄: 업무 (사각형)
│ ○ 보고서 작성              │  ← 3줄: Todo (빈 원)
│ +3개 더보기                │  ← 초과 시 표시
└────────────────────────────┘
```

### 4.2 표시 우선순위

```typescript
// 셀 내 표시 순서 결정
function sortCellItems(items: CellItem[]): CellItem[] {
  return items.sort((a, b) => {
    // 1. 우선순위 (URGENT > HIGH > NORMAL > LOW)
    const priorityOrder = { URGENT: 0, HIGH: 1, NORMAL: 2, LOW: 3 }
    if (priorityOrder[a.priority] !== priorityOrder[b.priority]) {
      return priorityOrder[a.priority] - priorityOrder[b.priority]
    }

    // 2. 타입 (이벤트 > 업무 > Todo)
    const typeOrder = { EVENT: 0, ITEM: 1, TODO: 2 }
    if (typeOrder[a.type] !== typeOrder[b.type]) {
      return typeOrder[a.type] - typeOrder[b.type]
    }

    // 3. 시간 (시작 시간순)
    return (a.startTime || '').localeCompare(b.startTime || '')
  })
}
```

### 4.3 셀 컴포넌트 구조

```vue
<!-- CalendarCell.vue -->
<template>
  <div class="calendar-cell" :class="{ 'is-today': isToday }">
    <!-- 날짜 헤더 -->
    <div class="cell-header">
      <span class="solar-date">{{ day }}</span>
      <span class="lunar-date" v-if="lunarDate">({{ lunarDate }})</span>
    </div>

    <!-- 이벤트 목록 (최대 3개) -->
    <div class="cell-items">
      <CalendarCellItem
        v-for="item in displayItems"
        :key="`${item.type}-${item.id}`"
        :item="item"
        @hover="showItemTooltip"
        @dblclick="openDetail"
      />

      <!-- 더보기 -->
      <div
        v-if="moreCount > 0"
        class="more-link"
        @mouseenter="showAllTooltip"
        @mouseleave="hideAllTooltip"
      >
        +{{ moreCount }}개 더보기
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const displayItems = computed(() => items.value.slice(0, 3))
const moreCount = computed(() => Math.max(0, items.value.length - 3))
</script>
```

---

## 5. 툴팁 시스템

### 5.1 개별 항목 호버 툴팁

```
[이벤트 호버]
┌─────────────────────────────┐
│ 📅 미팅 일정                │  ← 제목 (아이콘+텍스트)
├─────────────────────────────┤
│ 시간: 14:00 - 16:00         │
│ 장소: 회의실 A              │
│ 캘린더: 업무                │
│ 반복: 매주 월요일           │
├─────────────────────────────┤
│ 👤 홍길동님 공유 (조회만)   │  ← 공유받은 경우만
└─────────────────────────────┘

[업무 호버]
┌─────────────────────────────┐
│ 📋 프로젝트 A 마감          │
├─────────────────────────────┤
│ 보드: 개발팀                │
│ 상태: 진행중                │
│ 우선순위: 높음              │
│ 담당자: 김개발              │
│ 마감일: 2024-01-15          │
└─────────────────────────────┘

[Todo 호버]
┌─────────────────────────────┐
│ ✓ 보고서 작성               │
├─────────────────────────────┤
│ 우선순위: 보통              │
│ 마감: 18:00                 │
│ 상태: 미완료                │
└─────────────────────────────┘
```

### 5.2 더보기 호버 전체 리스트 툴팁

```
[+N개 더보기 호버]
┌─────────────────────────────────┐
│ 2024-01-15 (월) 전체 일정       │
│ 총 8건                          │
├─────────────────────────────────┤
│ 📅 이벤트 (3)                   │
│   ● 미팅 일정                   │
│   ● 팀 회의                     │
│   ● 고객 미팅 👤                │  ← 공유받은 표시
├─────────────────────────────────┤
│ 📋 업무 (3)                     │
│   ■ 프로젝트 A 마감             │
│   ■ API 개발                    │
│   ■ 리뷰 미팅 🔗                │  ← 배당받은 표시
├─────────────────────────────────┤
│ ✓ Todo (2)                      │
│   ○ 보고서 작성                 │
│   ○ 자료 정리                   │
└─────────────────────────────────┘
```

### 5.3 툴팁 컴포넌트

```typescript
// types/calendar.ts
export interface TooltipData {
  type: 'item' | 'all'
  position: { x: number; y: number }
  item?: CellItem
  allItems?: {
    date: string
    lunarDate: string
    events: CalendarEvent[]
    items: CalendarItem[]
    todos: CalendarTodo[]
  }
}
```

```vue
<!-- CalendarTooltip.vue -->
<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="calendar-tooltip"
      :style="{ left: `${position.x}px`, top: `${position.y}px` }"
    >
      <!-- 개별 항목 툴팁 -->
      <ItemTooltipContent v-if="type === 'item'" :item="item" />

      <!-- 전체 리스트 툴팁 -->
      <AllItemsTooltipContent v-else :data="allItems" />
    </div>
  </Teleport>
</template>
```

---

## 6. 음력 기능

### 6.1 음력 변환 라이브러리

```bash
# 설치
npm install korean-lunar-calendar
```

### 6.2 음력 유틸리티 함수

```typescript
// utils/lunar.ts
import { KoreanLunarCalendar } from 'korean-lunar-calendar'

export interface LunarDate {
  year: number
  month: number
  day: number
  isLeapMonth: boolean
  displayText: string  // "12.5" 또는 "윤4.1"
}

/**
 * 양력 → 음력 변환
 */
export function solarToLunar(solarDate: string): LunarDate {
  const calendar = new KoreanLunarCalendar()
  const [year, month, day] = solarDate.split('-').map(Number)
  calendar.setSolarDate(year, month, day)

  const lunarMonth = calendar.lunarMonth
  const lunarDay = calendar.lunarDay
  const isLeapMonth = calendar.isLeapMonth

  return {
    year: calendar.lunarYear,
    month: lunarMonth,
    day: lunarDay,
    isLeapMonth,
    displayText: `${isLeapMonth ? '윤' : ''}${lunarMonth}.${lunarDay}`
  }
}

/**
 * 음력 → 양력 변환
 */
export function lunarToSolar(
  year: number,
  month: number,
  day: number,
  isLeapMonth: boolean = false
): string {
  const calendar = new KoreanLunarCalendar()
  calendar.setLunarDate(year, month, day, isLeapMonth)

  return `${calendar.solarYear}-${String(calendar.solarMonth).padStart(2, '0')}-${String(calendar.solarDay).padStart(2, '0')}`
}

/**
 * 다음 음력 날짜의 양력 계산 (반복 이벤트용)
 */
export function getNextLunarDate(
  baseYear: number,
  lunarMonth: number,
  lunarDay: number,
  isLeapMonth: boolean,
  count: number = 1
): string[] {
  const results: string[] = []

  for (let i = 0; i < count; i++) {
    const targetYear = baseYear + i
    try {
      const solarDate = lunarToSolar(targetYear, lunarMonth, lunarDay, isLeapMonth)
      results.push(solarDate)
    } catch (e) {
      // 해당 연도에 음력 날짜가 없는 경우 (윤달 등)
      console.warn(`Lunar date not available: ${targetYear}-${lunarMonth}-${lunarDay}`)
    }
  }

  return results
}
```

### 6.3 음력 날짜 표시 (셀) - TB_CALENDAR_DATE 활용

> **변경**: 프론트엔드에서 음력 계산하지 않고 `dateInfo`에서 직접 조회

```vue
<template>
  <div class="cell-header">
    <span class="solar-date" :class="dateClass">{{ day }}</span>
    <span class="lunar-date" v-if="dateInfo">
      ({{ dateInfo.lunarDisplay }})
    </span>
    <!-- 공휴일 표시 -->
    <span class="holiday-badge" v-if="dateInfo?.isHoliday">
      {{ dateInfo.holidayName }}
    </span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CalendarDateResponse } from '@/types/calendar'

const props = defineProps<{
  date: string  // "2024-01-15"
  dateInfo?: CalendarDateResponse  // TB_CALENDAR_DATE에서 조회된 데이터
}>()

// 날짜 클래스 (공휴일/주말 스타일링)
const dateClass = computed(() => ({
  'is-holiday': props.dateInfo?.isHoliday,
  'is-weekend': props.dateInfo?.isWeekend,
  'is-workday': props.dateInfo?.isWorkday
}))
</script>

<style scoped>
.lunar-date {
  font-size: 0.7rem;
  color: #9ca3af;
  margin-left: 2px;
}
.holiday-badge {
  font-size: 0.6rem;
  color: #ef4444;
  margin-left: 4px;
}
.is-holiday { color: #ef4444; }
.is-weekend:not(.is-holiday) { color: #3b82f6; }
</style>
```

**dateInfo 구조** (TB_CALENDAR_DATE 응답):
```typescript
interface CalendarDateResponse {
  calDate: string           // "2026-01-18"
  solarYear: number
  solarMonth: number
  solarDay: number
  dayOfWeek: number         // 1=일, 2=월, ..., 7=토
  dayOfWeekName: string     // "일", "월", ...
  lunarYear: number
  lunarMonth: number
  lunarDay: number
  isLeapMonth: boolean
  lunarDisplay: string      // "12.5" 또는 "윤4.1"
  isHoliday: boolean
  holidayName?: string      // "설날", "추석" 등
  holidayType?: string      // "SOLAR_FIXED", "LUNAR_FIXED", "SUBSTITUTE"
  isWorkday: boolean
  isWeekend: boolean
}
```

### 6.4 음력 이벤트 등록 UI

```
[이벤트 등록 폼]
┌─────────────────────────────────────────────────────┐
│ 📅 새 이벤트                                        │
├─────────────────────────────────────────────────────┤
│ 제목: [________________]                            │
│                                                     │
│ 날짜 유형:  ○ 양력  ● 음력                          │
│                                                     │
│ [양력 선택 시]                                      │
│ 시작일: [2024-01-15    ] 종료일: [2024-01-15    ]   │
│                                                     │
│ [음력 선택 시]                                      │
│ 음력: [2024년 ▼] [12월 ▼] [5일 ▼] □ 윤달           │
│ → 양력: 2024-01-15 (자동 표시)                      │
│                                                     │
│ 반복:  ○ 없음                                       │
│        ○ 매일                                       │
│        ○ 매주                                       │
│        ○ 매월 (양력)                                │
│        ○ 매월 (음력)   ← 음력 선택 시               │
│        ○ 매년 (양력)                                │
│        ● 매년 (음력)   ← 예: 정월대보름, 추석        │
│                                                     │
│ 캘린더: [업무 ▼]                                    │
│                                                     │
│         [취소]  [저장]                              │
└─────────────────────────────────────────────────────┘
```

### 6.5 음력 반복 계산 로직

```typescript
// services/recurrence.ts

export type RecurrenceType =
  | 'DAILY'
  | 'WEEKLY'
  | 'MONTHLY'
  | 'YEARLY'
  | 'MONTHLY_LUNAR'
  | 'YEARLY_LUNAR'

interface RecurrenceParams {
  type: RecurrenceType
  startDate: string
  interval: number
  endDate?: string
  count?: number
  // 음력용
  lunarMonth?: number
  lunarDay?: number
  lunarLeapMonth?: boolean
  // 주간용
  weekDays?: number[]  // 0-6 (일-토)
}

/**
 * 반복 일정 날짜 생성
 */
export function generateRecurrenceDates(params: RecurrenceParams): string[] {
  const { type, startDate, interval, endDate, count } = params
  const dates: string[] = []
  const maxDates = count || 100  // 최대 100개

  switch (type) {
    case 'YEARLY_LUNAR':
      // 음력 매년 반복 (예: 정월대보름 1/15, 추석 8/15)
      const baseYear = new Date(startDate).getFullYear()
      for (let i = 0; i < maxDates; i++) {
        const targetYear = baseYear + (i * interval)
        try {
          const solarDate = lunarToSolar(
            targetYear,
            params.lunarMonth!,
            params.lunarDay!,
            params.lunarLeapMonth || false
          )
          if (endDate && solarDate > endDate) break
          dates.push(solarDate)
        } catch (e) {
          // 해당 연도에 음력 날짜 없음 (윤달)
        }
      }
      break

    case 'MONTHLY_LUNAR':
      // 음력 매월 반복 (예: 매월 초하루)
      let currentYear = new Date(startDate).getFullYear()
      let currentMonth = params.lunarMonth!
      for (let i = 0; i < maxDates; i++) {
        try {
          const solarDate = lunarToSolar(
            currentYear,
            currentMonth,
            params.lunarDay!,
            params.lunarLeapMonth || false
          )
          if (endDate && solarDate > endDate) break
          dates.push(solarDate)

          // 다음 음력 월로 이동
          currentMonth += interval
          if (currentMonth > 12) {
            currentYear += Math.floor(currentMonth / 12)
            currentMonth = currentMonth % 12 || 12
          }
        } catch (e) {
          // 해당 월에 음력 날짜 없음
          currentMonth += interval
          if (currentMonth > 12) {
            currentYear++
            currentMonth = currentMonth % 12 || 12
          }
        }
      }
      break

    // ... 기타 양력 반복 타입 처리
  }

  return dates
}
```

---

## 7. API 명세

### 7.1 캘린더 (이벤트 그룹) API

```
# 캘린더 CRUD
GET    /api/calendars                    # 내 캘린더 목록 (공유받은 것 포함)
POST   /api/calendars                    # 캘린더 생성
GET    /api/calendars/{id}               # 캘린더 상세
PUT    /api/calendars/{id}               # 캘린더 수정
DELETE /api/calendars/{id}               # 캘린더 삭제

# 캘린더 공유
GET    /api/calendars/{id}/shares        # 공유 목록
POST   /api/calendars/{id}/shares        # 공유 추가
DELETE /api/calendars/{id}/shares/{username}  # 공유 해제
```

### 7.2 이벤트 API

```
# 이벤트 CRUD
GET    /api/events                       # 이벤트 조회 (기간 필터)
       ?start_date=2024-01-01
       &end_date=2024-01-31
       &calendar_id=1                    # 특정 캘린더만 (선택)
       &include_shared=true              # 공유받은 이벤트 포함
POST   /api/events                       # 이벤트 생성
GET    /api/events/{id}                  # 이벤트 상세
PUT    /api/events/{id}                  # 이벤트 수정
DELETE /api/events/{id}                  # 이벤트 삭제

# 이벤트 공유
GET    /api/events/{id}/shares           # 공유 목록
POST   /api/events/{id}/shares           # 공유 추가
DELETE /api/events/{id}/shares/{username}  # 공유 해제

# 이벤트 이관
POST   /api/events/{id}/transfer         # 이벤트 이관
       Body: { "targetUsername": "user123" }

# 공유받은 이벤트 목록
GET    /api/events/shared                # 내가 공유받은 이벤트 목록
```

### 7.3 음력 변환 API (유틸리티)

```
# 양력 → 음력
GET    /api/calendar/lunar?solar=2024-01-15
Response: {
  "solar": "2024-01-15",
  "lunar": {
    "year": 2023,
    "month": 12,
    "day": 5,
    "isLeapMonth": false,
    "displayText": "12.5"
  }
}

# 음력 → 양력
GET    /api/calendar/solar?year=2024&month=1&day=15&leap_month=false
Response: {
  "lunar": {
    "year": 2024,
    "month": 1,
    "day": 15,
    "isLeapMonth": false
  },
  "solar": "2024-02-24"
}
```

### 7.4 이벤트 생성 요청 예시

```json
// POST /api/events
{
  "calendarId": 1,
  "title": "정월대보름",
  "description": "음력 1월 15일",
  "startDate": "2024-02-24",
  "isAllDay": true,
  "isLunar": true,
  "lunarMonth": 1,
  "lunarDay": 15,
  "lunarLeapMonth": false,
  "isRecurring": true,
  "recurrenceType": "YEARLY_LUNAR",
  "recurrenceInterval": 1,
  "recurrenceEndDate": null,
  "recurrenceCount": 10
}
```

---

## 8. 캘린더 통합 뷰 데이터 조회

### 8.1 통합 API 응답 구조 (TB_CALENDAR_DATE 연동)

```
GET /api/calendar/view
    ?year=2024
    &month=1
    &include_event=true
    &include_item=true
    &include_todo=true
    &calendar_ids=1,2,3        # 특정 캘린더만 (선택)
    &board_ids=1,2             # 특정 보드만 (선택)

# 응답에 포함되는 dateInfo (TB_CALENDAR_DATE 데이터)
# - 해당 월의 모든 날짜에 대한 음력/공휴일/근무일 정보
# - 프론트엔드에서 별도 API 호출 없이 바로 사용 가능
```

```json
{
  "success": true,
  "data": {
    "year": 2024,
    "month": 1,
    "events": { ... },
    "dateInfo": {
      "2024-01-15": {
        "calDate": "2024-01-15",
        "solarYear": 2024,
        "solarMonth": 1,
        "solarDay": 15,
        "dayOfWeek": 2,
        "dayOfWeekName": "월",
        "weekOfYear": 3,
        "weekOfMonth": 3,
        "quarter": 1,
        "lunarYear": 2023,
        "lunarMonth": 12,
        "lunarDay": 5,
        "isLeapMonth": false,
        "lunarDisplay": "12.5",
        "isHoliday": false,
        "holidayName": null,
        "holidayType": null,
        "isWorkday": true,
        "isWeekend": false
      },
      "2024-01-01": {
        "calDate": "2024-01-01",
        "lunarDisplay": "11.20",
        "isHoliday": true,
        "holidayName": "신정",
        "holidayType": "SOLAR_FIXED",
        "isWorkday": false,
        "isWeekend": false
      }
    },
    "days": {
      "2024-01-15": {
        "solarDate": "2024-01-15",
        "events": [
          {
            "id": 1,
            "type": "EVENT",
            "title": "미팅 일정",
            "calendarId": 1,
            "calendarName": "업무",
            "calendarColor": "#3b82f6",
            "startTime": "14:00",
            "endTime": "16:00",
            "isShared": false,
            "accessType": "OWNER"
          },
          {
            "id": 2,
            "type": "EVENT",
            "title": "고객 미팅",
            "calendarId": 2,
            "calendarName": "외부",
            "calendarColor": "#22c55e",
            "startTime": "10:00",
            "endTime": "11:00",
            "isShared": true,
            "sharedByUserName": "홍길동",
            "accessType": "VIEW"
          }
        ],
        "items": [
          {
            "id": 101,
            "type": "ITEM",
            "title": "프로젝트 A 마감",
            "boardId": 1,
            "boardName": "개발팀",
            "status": "IN_PROGRESS",
            "priority": "HIGH",
            "assigneeUserName": "김개발",
            "isAssignedToMe": true
          }
        ],
        "todos": [
          {
            "id": 201,
            "type": "TODO",
            "title": "보고서 작성",
            "priority": "NORMAL",
            "dueTime": "18:00",
            "isCompleted": false
          }
        ]
      }
    },
    "calendars": [
      { "id": 1, "name": "업무", "color": "#3b82f6", "isDefault": true },
      { "id": 2, "name": "외부", "color": "#22c55e", "isDefault": false }
    ]
  }
}
```

---

## 9. 필터링 UI

### 9.1 좌측 패널 (토글 형식)

```
┌─────────────────────────────┐
│ 📅 2024년 1월               │
│ [< ] [오늘] [ >]           │
├─────────────────────────────┤
│ 필터                        │
├─────────────────────────────┤
│ ☑ 이벤트                    │
│   ☑ ● 업무                  │
│   ☑ ● 외부                  │
│   ☐ ● 개인                  │
├─────────────────────────────┤
│ ☑ 업무                      │
│   ☑ ■ 개발팀                │
│   ☑ ■ 기획팀                │
├─────────────────────────────┤
│ ☑ Todo                      │
└─────────────────────────────┘
```

### 9.2 필터 상태 관리

```typescript
// stores/calendarFilter.ts
export const useCalendarFilterStore = defineStore('calendarFilter', () => {
  // 이벤트 필터
  const showEvents = ref(true)
  const selectedCalendarIds = ref<number[]>([])  // 빈 배열 = 전체

  // 업무 필터
  const showItems = ref(true)
  const selectedBoardIds = ref<number[]>([])  // 빈 배열 = 전체

  // Todo 필터
  const showTodos = ref(true)

  // 공유받은 항목 표시 여부
  const showSharedEvents = ref(true)
  const showAssignedItems = ref(true)

  return {
    showEvents,
    selectedCalendarIds,
    showItems,
    selectedBoardIds,
    showTodos,
    showSharedEvents,
    showAssignedItems
  }
})
```

---

## 10. 컴포넌트 구조 (확장)

```
src/
├── components/
│   └── calendar/
│       ├── CalendarView.vue           # 메인 컴포넌트
│       ├── CalendarHeader.vue         # 헤더 (년/월 네비게이션)
│       ├── CalendarSidebar.vue        # 좌측 필터 패널
│       ├── CalendarMonthGrid.vue      # 월간 그리드
│       ├── CalendarWeekView.vue       # 주간 뷰 (타임그리드)
│       ├── CalendarCell.vue           # 날짜 셀
│       ├── CalendarCellItem.vue       # 셀 내 개별 항목
│       ├── CalendarTooltip.vue        # 툴팁 (개별/전체)
│       ├── CalendarEventForm.vue      # 이벤트 등록/수정 폼
│       ├── CalendarShareModal.vue     # 공유 관리 모달
│       ├── CalendarTransferModal.vue  # 이관 모달
│       └── CalendarLunarPicker.vue    # 음력 날짜 선택기
│
├── utils/
│   └── lunar.ts                       # 음력 변환 유틸리티
│
├── stores/
│   ├── calendar.ts                    # 캘린더/이벤트 상태
│   └── calendarFilter.ts              # 필터 상태
│
└── api/
    └── calendar.ts                    # API 모듈
```

---

## 11. 구현 단계

### Phase 1: 기반 작업 (DB/API)
| 순번 | 작업 | 상세 |
|-----|------|------|
| 1-1 | 테이블 생성 | TB_CALENDAR, TB_EVENT, TB_EVENT_SHARE |
| 1-2 | Domain/DTO 작성 | Calendar, Event, EventShare |
| 1-3 | Mapper XML 작성 | CalendarMapper, EventMapper |
| 1-4 | Service 구현 | CalendarService, EventService |
| 1-5 | Controller 구현 | CalendarController (확장), EventController |

### Phase 2: 음력 기능
| 순번 | 작업 | 상세 |
|-----|------|------|
| 2-1 | 프론트엔드 라이브러리 설치 | korean-lunar-calendar |
| 2-2 | 음력 유틸리티 작성 | utils/lunar.ts |
| 2-3 | 백엔드 음력 API | /api/calendar/lunar, /api/calendar/solar |
| 2-4 | 음력 날짜 선택기 | CalendarLunarPicker.vue |

### Phase 3: UI 개선 (TB_CALENDAR_DATE 연동)
| 순번 | 작업 | 상세 | TB_CALENDAR_DATE 활용 |
|-----|------|------|----------------------|
| 3-1 | 셀 표시 개선 | 3줄 + 더보기 | isHoliday, isWeekend로 셀 스타일링 |
| 3-2 | 개별 항목 툴팁 | CalendarTooltip.vue (item) | - |
| 3-3 | 전체 리스트 툴팁 | CalendarTooltip.vue (all) | holidayName 표시 |
| 3-4 | 음력 표시 | 날짜 옆 음력 표시 | lunarDisplay 직접 사용 (계산 불필요) |

> **참고**: 캘린더 조회 API 응답의 `dateInfo` 필드에서 TB_CALENDAR_DATE 데이터 제공
> - 프론트엔드에서 음력 변환 계산 불필요 (사전 계산된 데이터 사용)
> - 공휴일/근무일 정보도 dateInfo에서 직접 조회

### Phase 4: 이벤트 관리
| 순번 | 작업 | 상세 |
|-----|------|------|
| 4-1 | 캘린더 CRUD | 캘린더 생성/수정/삭제 UI |
| 4-2 | 이벤트 CRUD | 이벤트 생성/수정/삭제 UI |
| 4-3 | 이벤트 공유 | 공유 모달, 공유 표시 |
| 4-4 | 이벤트 이관 | 이관 모달, 이관 처리 |

### Phase 5: 고급 기능
| 순번 | 작업 | 상세 |
|-----|------|------|
| 5-1 | 음력 반복 이벤트 | YEARLY_LUNAR, MONTHLY_LUNAR |
| 5-2 | 주간 뷰 타임그리드 | CalendarWeekView.vue |
| 5-3 | 필터 패널 | CalendarSidebar.vue |

---

## 12. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|-----|------|----------|
| v1.0 | 2026-01-18 | 최초 작성 |

---

## 13. 예상 문제점 및 고려사항

### 13.1 음력 변환 관련 이슈

| 문제 | 상세 | 해결 방안 |
|-----|------|----------|
| **윤달 처리** | 음력 윤달은 매년 존재하지 않음. 예: 2023년 윤2월 → 2024년에 윤2월 없음 | 반복 시 해당 연도에 윤달이 없으면 스킵하고 다음 해로 진행 |
| **음력 날짜 없음** | 음력 30일이 없는 달 존재 (예: 음력 2월은 29일까지) | 등록 시 유효성 검증, 29일로 fallback 또는 오류 표시 |
| **라이브러리 범위** | korean-lunar-calendar는 1900~2050년 범위만 지원 | 범위 외 날짜 입력 시 경고 표시, 2050년 이후는 양력만 지원 |
| **성능** | 매 셀마다 음력 변환 호출 시 성능 저하 | 월간 뷰 로드 시 한 번에 변환 후 캐싱 (42일 분량) |

```typescript
// 해결: 월간 음력 데이터 사전 계산
function precomputeLunarDates(year: number, month: number): Map<string, LunarDate> {
  const cache = new Map<string, LunarDate>()
  // 이전 달 일부 + 현재 달 + 다음 달 일부 (약 42일)
  for (let offset = -7; offset <= 35; offset++) {
    const date = new Date(year, month - 1, offset + 1)
    const dateStr = formatDate(date)
    cache.set(dateStr, solarToLunar(dateStr))
  }
  return cache
}
```

### 13.2 반복 이벤트 처리

| 문제 | 상세 | 해결 방안 |
|-----|------|----------|
| **대량 인스턴스** | 무한 반복 설정 시 인스턴스 폭발 | 조회 시점에 필요한 범위만 동적 생성, DB에 인스턴스 저장 안함 |
| **개별 수정** | 반복 중 특정 날짜만 수정 시 | PARENT_EVENT_ID로 원본 연결, 개별 인스턴스로 분리 저장 |
| **삭제 범위** | "이 이벤트만/이후 전체/전체" 선택 필요 | 삭제 모드 파라미터 추가, 개별 삭제는 CANCELLED 상태로 변경 |
| **시간대** | 사용자별 시간대 차이 | 서버는 UTC 저장, 프론트에서 로컬 시간대로 변환 |

```sql
-- 해결: 반복 예외 처리를 위한 컬럼 추가
ALTER TABLE TB_EVENT ADD COLUMN EXCEPTION_DATES TEXT NULL
  COMMENT '반복 예외 날짜 목록 (JSON: ["2024-01-15", "2024-02-20"])';

-- 특정 날짜 개별 수정 시
-- 1. PARENT_EVENT_ID에 원본 이벤트 ID 설정
-- 2. 원본의 EXCEPTION_DATES에 해당 날짜 추가
-- 3. 새 레코드로 저장
```

### 13.3 공유/이관 권한 관리

| 문제 | 상세 | 해결 방안 |
|-----|------|----------|
| **캘린더 vs 이벤트 공유 충돌** | 캘린더는 VIEW, 개별 이벤트는 EDIT 권한일 때 | 이벤트 개별 권한이 우선 (더 높은 권한 적용) |
| **이관 후 기존 공유** | 이관 시 기존 공유 관계 처리 | 모든 기존 공유 해제 (DELETED_AT 설정), 새 소유자부터 시작 |
| **순환 공유** | A→B→C→A 순환 공유 발생 가능 | 이벤트 단위 공유이므로 순환 문제 없음 (참조 복사 아님) |
| **기존 ItemShare와 일관성** | 업무 공유(VIEW/EDIT/FULL)와 이벤트 공유(VIEW/EDIT) 차이 | 이벤트는 FULL 권한 불필요 (이관으로 대체), 일관된 패턴 유지 |

```java
// 해결: 권한 계산 로직 통일
public String getEffectivePermission(Long eventId, String username) {
    Event event = eventMapper.findById(eventId);

    // 1. 소유자 확인
    if (username.equals(event.getOwnerUsername())) {
        return "OWNER";
    }

    // 2. 캘린더 공유 권한
    String calendarPerm = calendarShareMapper.getPermission(event.getCalendarId(), username);

    // 3. 이벤트 개별 공유 권한
    String eventPerm = eventShareMapper.getPermission(eventId, username);

    // 4. 더 높은 권한 반환
    return getHigherPermission(calendarPerm, eventPerm);
}
```

### 13.4 성능 이슈

| 문제 | 상세 | 해결 방안 |
|-----|------|----------|
| **월간 뷰 데이터 량** | 이벤트+업무+Todo 동시 조회 시 쿼리 부하 | 병렬 조회 후 병합, 또는 UNION ALL 단일 쿼리 |
| **반복 이벤트 계산** | 매 조회마다 반복 날짜 계산 | Redis 캐시 또는 인메모리 캐시 (5분 TTL) |
| **툴팁 데이터** | 호버마다 API 호출 시 부하 | 월간 데이터 로드 시 툴팁 정보 포함, 클라이언트 캐시 |
| **다중 사용자 공유** | 많은 사용자에게 공유 시 조회 쿼리 복잡 | EXISTS 서브쿼리 사용, 인덱스 최적화 |

```sql
-- 해결: 최적화된 통합 조회 쿼리
SELECT
    'EVENT' as item_type,
    e.EVENT_ID as id,
    e.TITLE as title,
    e.START_DATE as date,
    c.COLOR as color,
    CASE WHEN e.OWNER_USERNAME = #{username} THEN 0 ELSE 1 END as is_shared
FROM TB_EVENT e
JOIN TB_CALENDAR c ON e.CALENDAR_ID = c.CALENDAR_ID
WHERE e.STATUS = 'ACTIVE'
  AND e.START_DATE <= #{endDate}
  AND COALESCE(e.END_DATE, e.START_DATE) >= #{startDate}
  AND (
      e.OWNER_USERNAME = #{username}
      OR EXISTS (SELECT 1 FROM TB_EVENT_SHARE es
                 WHERE es.EVENT_ID = e.EVENT_ID AND es.USERNAME = #{username} AND es.DELETED_AT IS NULL)
      OR EXISTS (SELECT 1 FROM TB_CALENDAR_SHARE cs
                 WHERE cs.CALENDAR_ID = c.CALENDAR_ID AND cs.USERNAME = #{username} AND cs.DELETED_AT IS NULL)
  )
UNION ALL
SELECT
    'ITEM' as item_type,
    i.ITEM_ID as id,
    i.CONTENT as title,
    COALESCE(i.DUE_DATE, i.REQUEST_DATE) as date,
    NULL as color,
    CASE WHEN i.OWNER_USERNAME = #{username} THEN 0 ELSE 1 END as is_shared
FROM TB_ITEM i
-- ... 업무 조회 조건
UNION ALL
SELECT
    'TODO' as item_type,
    t.TODO_ID as id,
    t.TITLE as title,
    t.DUE_DATE as date,
    NULL as color,
    0 as is_shared
FROM TB_TODO t
WHERE t.OWNER_USER_ID = #{userId}
  AND t.DUE_DATE BETWEEN #{startDate} AND #{endDate}
ORDER BY date, item_type;
```

### 13.5 UI/UX 고려사항

| 문제 | 상세 | 해결 방안 |
|-----|------|----------|
| **툴팁 위치** | 셀 경계에서 툴팁이 화면 밖으로 나갈 수 있음 | 뷰포트 경계 감지하여 위치 자동 조정 |
| **모바일 터치** | 호버 기반 툴팁이 모바일에서 동작 안함 | 모바일: 탭으로 툴팁, 롱프레스로 컨텍스트 메뉴 |
| **색상 접근성** | 캘린더 색상이 시각장애인에게 구분 어려움 | 색상+아이콘 병행, 고대비 모드 지원 |
| **공유 표시 혼란** | 공유/배당 구분이 어려울 수 있음 | 범례 항상 표시, 일관된 아이콘 (👤 공유, 🔗 배당) |

```vue
<!-- 해결: 반응형 툴팁 위치 계산 -->
<script setup>
function calculateTooltipPosition(event: MouseEvent, tooltipEl: HTMLElement) {
  const rect = tooltipEl.getBoundingClientRect()
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight

  let x = event.clientX + 10
  let y = event.clientY + 10

  // 오른쪽 경계 체크
  if (x + rect.width > viewportWidth - 20) {
    x = event.clientX - rect.width - 10
  }

  // 하단 경계 체크
  if (y + rect.height > viewportHeight - 20) {
    y = event.clientY - rect.height - 10
  }

  return { x, y }
}
</script>
```

### 13.6 기존 시스템 통합

| 문제 | 상세 | 해결 방안 |
|-----|------|----------|
| **알림 통합** | 이벤트 공유/이관 시 NotificationService 연동 필요 | 기존 패턴 따라 sendEventSharedNotification, sendEventTransferredNotification 추가 |
| **감사 로그** | TB_AUDIT_LOG에 이벤트 관련 로그 기록 | TARGET_TYPE에 'CALENDAR', 'EVENT', 'EVENT_SHARE' 추가 |
| **기존 CalendarService** | 현재 Todo+Item만 조회, Event 추가 필요 | CalendarService 확장 또는 EventService 별도 생성 |
| **SSE 실시간 동기화** | 이벤트 변경 시 다른 사용자에게 알림 | 기존 SSE 이벤트 타입에 'event:created', 'event:updated' 추가 |

```java
// 해결: NotificationService에 이벤트 알림 메서드 추가
public interface NotificationService {
    // 기존 메서드...

    /**
     * 이벤트 공유 알림 생성
     */
    void sendEventSharedNotification(Long eventId, String eventTitle,
                                      String sharedToUsername, String sharedBy);

    /**
     * 이벤트 이관 알림 생성
     */
    void sendEventTransferredNotification(Long eventId, String eventTitle,
                                           String newOwnerUsername, String transferredBy);
}
```

### 13.7 데이터 마이그레이션

| 문제 | 상세 | 해결 방안 |
|-----|------|----------|
| **기본 캘린더 생성** | 기존 사용자에게 기본 캘린더 필요 | 로그인 시 기본 캘린더 없으면 자동 생성 또는 마이그레이션 스크립트 |
| **기존 데이터 없음** | 신규 기능이므로 마이그레이션 데이터 없음 | 초기 데이터 스크립트에 샘플 캘린더/이벤트 추가 (선택) |

```sql
-- 해결: 기본 캘린더 자동 생성 (로그인 시)
INSERT INTO TB_CALENDAR (OWNER_USERNAME, CALENDAR_NAME, COLOR, IS_DEFAULT, CREATED_BY)
SELECT u.USERNAME, '기본', '#3b82f6', TRUE, u.USERNAME
FROM TB_USER u
WHERE u.USE_YN = 'Y'
  AND NOT EXISTS (
      SELECT 1 FROM TB_CALENDAR c
      WHERE c.OWNER_USERNAME = u.USERNAME AND c.IS_DEFAULT = TRUE
  );
```

### 13.8 보안 고려사항

| 문제 | 상세 | 해결 방안 |
|-----|------|----------|
| **권한 우회** | API 직접 호출로 권한 없는 이벤트 수정 시도 | 모든 API에서 권한 검증 필수, AOP 또는 인터셉터 적용 |
| **공유 대상 검증** | 존재하지 않는 사용자에게 공유 시도 | UserService.exists() 검증, 유효한 사용자만 공유 가능 |
| **민감 정보 노출** | 공유받은 이벤트의 다른 공유자 목록 노출 | 공유자 목록은 소유자만 조회 가능, 공유 대상은 자신의 공유 정보만 확인 |

```java
// 해결: 권한 검증 AOP
@Aspect
@Component
public class EventAuthorizationAspect {

    @Before("@annotation(RequireEventPermission)")
    public void checkPermission(JoinPoint joinPoint) {
        // 이벤트 ID 추출
        Long eventId = extractEventId(joinPoint);
        String username = SecurityUtils.getCurrentUsername();
        String requiredPermission = getRequiredPermission(joinPoint);

        String actualPermission = eventShareService.getEffectivePermission(eventId, username);

        if (!hasRequiredPermission(actualPermission, requiredPermission)) {
            throw new AccessDeniedException("이벤트에 대한 권한이 없습니다.");
        }
    }
}
```

### 13.9 에러 처리

| 에러 상황 | 처리 방안 |
|----------|----------|
| 음력 변환 실패 | 양력 날짜만 표시, 오류 로그 기록 |
| 반복 이벤트 생성 실패 | 트랜잭션 롤백, 사용자에게 오류 메시지 |
| 공유 대상 탈퇴 | DELETED_AT 자동 설정 (FK 제약 없음) |
| 캘린더 삭제 시 이벤트 | 이벤트도 함께 삭제 (CASCADE) 또는 삭제 차단 |

```java
// 해결: 캘린더 삭제 전 이벤트 확인
@Transactional
public void deleteCalendar(Long calendarId, String username) {
    Calendar calendar = calendarMapper.findById(calendarId)
        .orElseThrow(() -> new BusinessException("캘린더를 찾을 수 없습니다."));

    // 소유자 확인
    if (!username.equals(calendar.getOwnerUsername())) {
        throw new BusinessException("캘린더를 삭제할 권한이 없습니다.");
    }

    // 기본 캘린더 삭제 불가
    if (calendar.getIsDefault()) {
        throw new BusinessException("기본 캘린더는 삭제할 수 없습니다.");
    }

    // 이벤트가 있는 경우 처리 방법 선택
    int eventCount = eventMapper.countByCalendarId(calendarId);
    if (eventCount > 0) {
        throw new BusinessException(
            String.format("캘린더에 %d개의 이벤트가 있습니다. 이벤트를 먼저 삭제하거나 이동해주세요.", eventCount)
        );
    }

    calendarMapper.delete(calendarId);
}
```

### 13.10 테스트 체크리스트

```
□ 음력 변환
  □ 일반 날짜 변환 (양력↔음력)
  □ 윤달 포함 날짜 변환
  □ 경계값 (1900-01-01, 2050-12-31)
  □ 잘못된 음력 날짜 입력 (2월 30일 등)

□ 반복 이벤트
  □ 매일/매주/매월/매년 반복 생성
  □ 음력 반복 (매년 정월대보름 등)
  □ 반복 종료 조건 (횟수, 날짜)
  □ 개별 인스턴스 수정/삭제
  □ 전체 반복 삭제

□ 공유/이관
  □ 캘린더 공유 (VIEW/EDIT)
  □ 이벤트 공유 (VIEW/EDIT)
  □ 이벤트 이관
  □ 공유받은 이벤트 조회 (권한별)
  □ 공유받은 이벤트 수정 시도 (VIEW 권한)

□ UI/UX
  □ 셀 3줄 + 더보기 표시
  □ 개별 항목 툴팁
  □ 전체 리스트 툴팁
  □ 음력 날짜 표시
  □ 공유 이벤트 시각적 구분
  □ 모바일 터치 동작

□ 통합
  □ 기존 업무/Todo와 함께 표시
  □ 알림 발송
  □ 감사 로그 기록
  □ SSE 실시간 동기화
```

---

## 14. 날짜 기준 테이블 (TB_CALENDAR_DATE)

### 14.1 개요

년간 캘린더 데이터를 사전 생성하여 관리하는 기준 테이블입니다.
- **목적**: 음력 변환, 공휴일, 근무일 정보를 사전 계산하여 조회 성능 최적화
- **범위**: 2020-2035년 (초기), 매년 자동 확장
- **활용**: 캘린더 뷰, 근무일수 계산, 음력 표시, 마감일 계산

### 14.2 테이블 정의

```sql
CREATE TABLE TB_CALENDAR_DATE (
    CAL_DATE DATE NOT NULL COMMENT '양력 날짜 (PK)',

    -- 양력 정보
    SOLAR_YEAR SMALLINT NOT NULL COMMENT '양력 연도',
    SOLAR_MONTH TINYINT NOT NULL COMMENT '양력 월',
    SOLAR_DAY TINYINT NOT NULL COMMENT '양력 일',
    DAY_OF_WEEK TINYINT NOT NULL COMMENT '요일 (1=일, 2=월, ..., 7=토)',
    DAY_OF_WEEK_NAME VARCHAR(10) NOT NULL COMMENT '요일명 (일,월,화...)',
    WEEK_OF_YEAR TINYINT NOT NULL COMMENT '연중 주차',
    WEEK_OF_MONTH TINYINT NOT NULL COMMENT '월중 주차',
    QUARTER TINYINT NOT NULL COMMENT '분기 (1-4)',

    -- 음력 정보
    LUNAR_YEAR SMALLINT NOT NULL COMMENT '음력 연도',
    LUNAR_MONTH TINYINT NOT NULL COMMENT '음력 월',
    LUNAR_DAY TINYINT NOT NULL COMMENT '음력 일',
    IS_LEAP_MONTH BOOLEAN NOT NULL DEFAULT FALSE COMMENT '윤달 여부',
    LUNAR_DISPLAY VARCHAR(10) NOT NULL COMMENT '음력 표시 (12.5 or 윤4.1)',

    -- 근무일/휴일 정보
    IS_HOLIDAY BOOLEAN NOT NULL DEFAULT FALSE COMMENT '공휴일 여부',
    HOLIDAY_NAME VARCHAR(50) NULL COMMENT '공휴일명',
    HOLIDAY_TYPE VARCHAR(20) NULL COMMENT '공휴일 유형 (SOLAR_FIXED, LUNAR_FIXED, SUBSTITUTE, TEMPORARY)',
    IS_WORKDAY BOOLEAN NOT NULL DEFAULT TRUE COMMENT '근무일 여부',

    -- 감사 필드
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',

    PRIMARY KEY (CAL_DATE),
    INDEX IDX_CAL_YEAR_MONTH (SOLAR_YEAR, SOLAR_MONTH),
    INDEX IDX_CAL_LUNAR (LUNAR_YEAR, LUNAR_MONTH, LUNAR_DAY),
    INDEX IDX_CAL_WORKDAY (CAL_DATE, IS_WORKDAY),
    INDEX IDX_CAL_HOLIDAY (IS_HOLIDAY, CAL_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='날짜 기준 테이블';
```

### 14.3 생성 기준

#### 양력 정보 계산
| 필드 | 계산 기준 |
|------|----------|
| DAY_OF_WEEK | 1=일요일, 2=월요일, ..., 7=토요일 |
| WEEK_OF_YEAR | ISO-8601 기준 (월요일 시작) |
| WEEK_OF_MONTH | 해당 월 첫 날 기준 주차 |
| QUARTER | (월-1)/3 + 1 |

#### 공휴일 기준

**고정 공휴일 (양력) - SOLAR_FIXED**
| 날짜 | 명칭 | 대체공휴일 |
|------|------|-----------|
| 1/1 | 신정 | X |
| 3/1 | 삼일절 | O |
| 5/5 | 어린이날 | O |
| 6/6 | 현충일 | X |
| 8/15 | 광복절 | O |
| 10/3 | 개천절 | O |
| 10/9 | 한글날 | O |
| 12/25 | 성탄절 | O |

**고정 공휴일 (음력) - LUNAR_FIXED**
| 음력 날짜 | 명칭 | 대체공휴일 |
|----------|------|-----------|
| 12/말일 | 설날 연휴 | O |
| 1/1 | 설날 | O |
| 1/2 | 설날 연휴 | O |
| 4/8 | 부처님오신날 | O |
| 8/14 | 추석 연휴 | O |
| 8/15 | 추석 | O |
| 8/16 | 추석 연휴 | O |

**대체공휴일 규칙 - SUBSTITUTE**
```
1. 적용 대상: 3/1, 어린이날, 광복절, 개천절, 한글날, 성탄절, 설, 추석, 부처님오신날
2. 발동 조건: 공휴일이 토요일 또는 일요일과 겹칠 때
3. 대체일: 공휴일 다음 첫 번째 비공휴일(평일)
4. 연휴 충돌: 설/추석 연휴 중 일요일과 겹치면 연휴 다음 첫 평일
```

#### 근무일 기준
| 조건 | IS_WORKDAY |
|------|------------|
| 월~금 AND NOT 공휴일 | TRUE |
| 토, 일 | FALSE |
| 공휴일/대체공휴일 | FALSE |

### 14.4 생성 프로세스

```
┌─────────────────────────────────────────────────────────┐
│                    년간 캘린더 생성                       │
├─────────────────────────────────────────────────────────┤
│  1. 연도 범위 결정 (시작년~종료년)                        │
│  2. 각 날짜별 기본 정보 생성                             │
│     ├─ 양력 정보 (요일, 주차, 분기)                      │
│     └─ 음력 정보 (LunarCalendarService)                 │
│  3. 고정 공휴일 적용                                     │
│     ├─ 양력 공휴일 마킹                                  │
│     └─ 음력 공휴일 → 양력 변환 후 마킹                    │
│  4. 대체공휴일 계산                                      │
│     └─ 공휴일 + 주말 충돌 시 다음 첫 평일에 마킹           │
│  5. 근무일 계산                                          │
│     └─ 평일 AND NOT 공휴일 → IS_WORKDAY = TRUE           │
│  6. DB 저장 (UPSERT)                                    │
└─────────────────────────────────────────────────────────┘
```

### 14.5 활용 쿼리 예시

```sql
-- 월간 캘린더 조회 (음력 포함)
SELECT * FROM TB_CALENDAR_DATE
WHERE SOLAR_YEAR = 2026 AND SOLAR_MONTH = 1
ORDER BY CAL_DATE;

-- 기간 내 근무일수 계산
SELECT COUNT(*) as workdays
FROM TB_CALENDAR_DATE
WHERE CAL_DATE BETWEEN '2026-01-01' AND '2026-01-31'
  AND IS_WORKDAY = TRUE;

-- 음력 기념일 양력 조회 (매년 추석)
SELECT CAL_DATE, SOLAR_YEAR
FROM TB_CALENDAR_DATE
WHERE LUNAR_MONTH = 8 AND LUNAR_DAY = 15 AND IS_LEAP_MONTH = FALSE
ORDER BY CAL_DATE;

-- 이벤트 조회 시 음력 정보 JOIN
SELECT e.*, cd.LUNAR_DISPLAY, cd.IS_HOLIDAY, cd.HOLIDAY_NAME
FROM TB_EVENT e
JOIN TB_CALENDAR_DATE cd ON e.START_DATE = cd.CAL_DATE;
```

### 14.6 자동 확장 스케줄

| 시점 | 동작 |
|------|------|
| 시스템 시작 | 현재년도 ±10년 범위 확인, 부족 시 생성 |
| 매년 1월 1일 00:00 | +5년 추가 생성 (중복 시 스킵) |
| 관리자 수동 | 설정 메뉴에서 연도 지정 생성 |

### 14.7 관리 UI

```
[설정 > 캘린더 날짜 관리]
┌─────────────────────────────────────────────────────────┐
│ 날짜 데이터 현황                                         │
│ ─────────────────────────────────                       │
│ 데이터 범위: 2020년 ~ 2035년 (5,844일)                   │
│                                                         │
│ [연도 추가] ┌────────────────────────┐                  │
│            │ 연도: [2036 ▼] [생성]  │                  │
│            └────────────────────────┘                  │
│                                                         │
│ [공휴일 관리] 연도: [2026 ▼]                             │
│ ┌────────────┬──────────┬────────────┬──────┐          │
│ │ 날짜       │ 공휴일명  │ 유형       │ 관리 │          │
│ ├────────────┼──────────┼────────────┼──────┤          │
│ │ 2026-01-01 │ 신정     │ 양력고정    │      │          │
│ │ 2026-01-28 │ 설날연휴  │ 음력고정    │      │          │
│ │ 2026-01-29 │ 설날     │ 음력고정    │      │          │
│ │ 2026-01-30 │ 설날연휴  │ 음력고정    │      │          │
│ │ 2026-02-02 │ 대체공휴일│ 대체       │      │          │
│ └────────────┴──────────┴────────────┴──────┘          │
│                                                         │
│ [+ 임시공휴일 추가]                                      │
│                                                         │
│ [대체공휴일 재계산] - 공휴일 변경 후 실행                  │
└─────────────────────────────────────────────────────────┘
```

---

## 15. 설계 변경 이력

| 버전 | 날짜 | 변경 내용 |
|-----|------|----------|
| v1.0 | 2026-01-18 | 최초 작성 |
| v1.1 | 2026-01-18 | 예상 문제점 및 고려사항 추가 (13장) |
| v1.2 | 2026-01-18 | 날짜 기준 테이블 설계 추가 (14장) |
| v1.3 | 2026-01-18 | Phase 3에 TB_CALENDAR_DATE 연동 내용 반영 |
| v1.4 | 2026-01-18 | Phase 3 UI 개선 구현 완료 |

---

**승인 완료 - 구현 진행**

---

## 16. 구현 완료 현황

### Phase 1: 기반 작업 ✅ 완료
- TB_CALENDAR, TB_EVENT, TB_EVENT_SHARE, TB_CALENDAR_SHARE 테이블 생성
- Domain, DTO, Mapper, Service, Controller 구현

### Phase 2: 음력 기능 ✅ 완료
- korean-lunar-calendar 프론트엔드 라이브러리 설치
- utils/lunar.ts 유틸리티 작성
- LunarCalendarService, LunarController 백엔드 구현
- LunarDatePicker.vue 컴포넌트 구현

### 추가: TB_CALENDAR_DATE ✅ 완료
- TB_CALENDAR_DATE 테이블 생성 (마이그레이션 V2.4)
- CalendarDate 도메인, CalendarDateResponse DTO
- CalendarDateMapper, CalendarDateService 구현
- CalendarDateController REST API
- 2020-2035년 초기 데이터 자동 생성 (5,844건)
- CalendarServiceImpl.getCalendarData()에 dateInfo 연동

### Phase 3: UI 개선 ✅ 완료
- 프론트엔드 타입 정의 (CalendarDateInfo)
- CalendarView.vue 셀 표시 개선 (3줄 + 더보기)
- 음력 날짜 표시 (dateInfo.lunarDisplay 활용)
- 공휴일 표시 및 스타일링 (holidayName, isHoliday)
- CalendarCellItem.vue 개별 항목 컴포넌트 (호버 툴팁 포함)
- CalendarTooltip.vue 전체 이벤트 목록 팝업 (더보기 클릭 시)
- 범례 업데이트 (Todo/업무/휴일/음력)

---
❯ 음력 기념일과 윤달 체크박스는 역활이 어ㄸ허게 되나요? 

● 음력 기념일 버튼

  자주 사용하는 음력 날짜를 빠르게 선택하는 기능입니다.
  ┌────────────┬───────────┬───────────┐
  │    버튼    │ 음력 날짜 │   용도    │
  ├────────────┼───────────┼───────────┤
  │ 설날       │ 1월 1일   │ 새해 첫날 │
  ├────────────┼───────────┼───────────┤
  │ 정월대보름 │ 1월 15일  │ 첫 보름달 │
  ├────────────┼───────────┼───────────┤
  │ 단오       │ 5월 5일   │ 단오절    │
  ├────────────┼───────────┼───────────┤
  │ 칠석       │ 7월 7일   │ 견우직녀  │
  ├────────────┼───────────┼───────────┤
  │ 백중       │ 7월 15일  │ 백중날    │
  ├────────────┼───────────┼───────────┤
  │ 추석       │ 8월 15일  │ 한가위    │
  ├────────────┼───────────┼───────────┤
  │ 중양절     │ 9월 9일   │ 중양절    │
  └────────────┴───────────┴───────────┘
  동작: 버튼 클릭 → 해당 음력 월/일 자동 입력 → 양력 날짜 자동 변환

  ---
  윤달 체크박스

  음력 달력의 **윤달(閏月)**을 선택하기 위한 체크박스입니다.

  윤달이란?
  - 음력 1년은 약 354일 (양력보다 11일 짧음)
  - 이 차이를 보정하기 위해 19년에 약 7번 윤달 삽입
  - 윤달은 특정 월을 한 번 더 반복 (예: 4월 다음에 윤4월)

  예시:
  2023년: 윤2월 있음 → 2월, 윤2월, 3월 순서
  2025년: 윤6월 있음 → 6월, 윤6월, 7월 순서

  동작:
  - 해당 연도/월에 윤달이 없으면 → 체크박스 비활성화
  - 윤달이 있으면 → 체크 가능, "윤N월"로 표시