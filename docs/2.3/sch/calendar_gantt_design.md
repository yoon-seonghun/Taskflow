# Calendar & Gantt Chart 기능 설계서

> 버전: v3.0
> 작성일: 2026-01-18
> 상태: 승인 대기

---

## 1. 기능 개요

### 1.1 Calendar View
| 항목 | 설명 |
|-----|------|
| 대상 | **Todo** (날짜 등록된 것만) + **업무(Item)** |
| 표시 기준일 | 마감일 (DUE_DATE) |
| 구분 표시 | Todo: ○ 원형 아이콘 / 업무: ■ 사각 아이콘 |
| 뷰 타입 | 월간, 주간, 일간 |

### 1.2 Gantt Chart View
| 항목 | 설명 |
|-----|------|
| 대상 | **업무(Item)만** (Todo 제외) |
| 기본 표현 | 요청일(REQUEST_DATE) ~ 마감일(DUE_DATE) |
| 추가 표현 | 시작일 ~ 완료일 (동적 속성 존재 시) |
| 하위 업무 | "└" 들여쓰기로 표현 |

---

## 2. 날짜 데이터 구조

### 2.1 날짜 저장 위치
| 날짜 | 저장 위치 | 타입 | 비고 |
|-----|----------|------|------|
| **요청일** (REQUEST_DATE) | TB_ITEM.REQUEST_DATE | 기본 컬럼 | 항상 존재 |
| **마감일** (DUE_DATE) | TB_ITEM.DUE_DATE | 기본 컬럼 | 항상 존재 |
| **시작일** | TB_ITEM_PROPERTY | 동적 속성 | PROPERTY_NAME = "시작일" |
| **완료일** | TB_ITEM_PROPERTY | 동적 속성 | PROPERTY_NAME = "완료일" |

### 2.2 시작일/완료일 속성 규칙
- TB_PROPERTY_DEF에서 PROPERTY_NAME이 "시작일", "완료일"인 속성
- **중복 생성 불가** (시스템에 단일 아이템으로 존재)
- 업무별로 지정 여부가 다름:
  - 미지정
  - 시작일만 지정
  - 완료일만 지정
  - 둘 다 지정

---

## 3. Gantt 표시 규칙

### 3.1 4개 날짜 포인트 표시
```
┌─────────────────────────────────────────────────────────────────────┐
│ 업무명              │ 1/1  1/5  1/10  1/15  1/20  1/25  1/30       │
├─────────────────────┼───────────────────────────────────────────────┤
│ 프로젝트 기획       │  ◆━━━━━━━━━━━━━━━━━━━━━━━━━━━◇                │
│                     │       ●━━━━━━━━━━━━━━━━━━━━○                  │
├─────────────────────┼───────────────────────────────────────────────┤
│ └ 요구사항 분석     │  ◆━━━━━━━━━◇                                  │
│                     │   ●━━━━━━○                                    │
└─────────────────────┴───────────────────────────────────────────────┘

범례: ◆ 요청일  ◇ 마감일  ● 시작일  ○ 완료일
      ━━━ 기간 바
```

### 3.2 날짜 미지정 시 처리
| 상황 | 요청일~마감일 바 | 시작일~완료일 바 |
|-----|----------------|-----------------|
| 요청일 없음 | 생성일(CREATED_AT) 사용 | - |
| 마감일 없음 | 단일 포인트(◆)만 표시 | - |
| 시작일 속성 없음 | - | 바 미표시 |
| 완료일 속성 없음 | - | 시작일만 단일 포인트(●) 표시 |
| 시작일/완료일 둘 다 없음 | - | 두 번째 바 미표시 |

### 3.3 UI 범례 (필수 표시)
```
┌─────────────────────────────────────────────────┐
│ 범례: ◆ 요청일  ◇ 마감일  ● 시작일  ○ 완료일  │
└─────────────────────────────────────────────────┘
```

---

## 4. Gantt 편집 규칙

### 4.1 Drag & Drop 적용 범위
| 날짜 | D&D 지원 | 비고 |
|-----|---------|------|
| **요청일** | ✅ 가능 | 기본 컬럼 |
| **마감일** | ✅ 가능 | 기본 컬럼 |
| **시작일** | ⚠️ 조건부 | 속성이 존재하고 값이 지정된 경우만 |
| **완료일** | ⚠️ 조건부 | 속성이 존재하고 값이 지정된 경우만 |

### 4.2 편집 시나리오별 동작

#### 시나리오 A: 시작일/완료일 속성 둘 다 없음
```
- 요청일~마감일 바: D&D 가능
- 시작일~완료일 바: 표시 안됨, 편집 불가
- 클릭 시: "시작일/완료일 속성이 설정되지 않았습니다" 안내
```

#### 시나리오 B: 시작일만 존재, 완료일 없음
```
- 시작일: 포인트 클릭으로 DatePicker 편집 (D&D 불가)
- 완료일: 편집 불가 (속성 없음)
```

#### 시나리오 C: 완료일만 존재, 시작일 없음
```
- 시작일: 편집 불가 (속성 없음)
- 완료일: 포인트 클릭으로 DatePicker 편집 (D&D 불가)
```

#### 시나리오 D: 시작일/완료일 둘 다 존재
```
- 시작일~완료일 바: D&D로 기간 조정 가능
- 각 포인트 개별 드래그 가능
```

### 4.3 편집 UI 피드백
```
┌─────────────────────────────────────────────────────────────────────┐
│  [D&D 가능한 바]                                                    │
│  ◆═══════════════════════◇   커서: grab/grabbing                   │
│                                                                     │
│  [D&D 불가능한 바 - 하나만 있음]                                     │
│  ●                           단일 포인트, 클릭으로 DatePicker       │
│                                                                     │
│  [속성 미존재 시]                                                   │
│  (표시 안됨)                                                        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 5. Gantt 옵션 설정

### 5.1 날짜 표시 단위 (DATE_UNIT)
| 옵션 | 설명 |
|-----|------|
| DAY | 일 단위 눈금 |
| WEEK | 주 단위 눈금 |
| MONTH | 월 단위 눈금 |

### 5.2 날짜 표시 범위 (DATE_RANGE)
| 옵션 | 설명 |
|-----|------|
| FULL | 전체 활성 업무의 가장 빠른 요청일/시작일 ~ 가장 늦은 마감일/완료일 |
| WEEK | 오늘 기준 전후 1주 |
| MONTH | 오늘 기준 전후 1개월 |
| CUSTOM | 사용자 지정 시작일~종료일 |

---

## 6. API 명세

### 6.1 Calendar API

#### 캘린더 데이터 조회
```
GET /api/calendar
  ?year=2024
  &month=1
  &includeTodo=true
  &includeItem=true
  &boardId=1           (선택: 특정 보드만)
```

**응답:**
```json
{
  "success": true,
  "data": {
    "2024-01-15": [
      {
        "type": "TODO",
        "id": 1,
        "title": "회의 준비",
        "priority": "HIGH",
        "isCompleted": false
      },
      {
        "type": "ITEM",
        "id": 101,
        "title": "UI 설계",
        "priority": "NORMAL",
        "status": "IN_PROGRESS",
        "boardId": 1,
        "boardName": "프로젝트A"
      }
    ]
  }
}
```

### 6.2 Gantt API

#### Gantt 데이터 조회
```
GET /api/gantt
  ?boardId=1
  &range=MONTH          (FULL/WEEK/MONTH/CUSTOM)
  &startDate=2024-01-01 (CUSTOM일 때)
  &endDate=2024-01-31   (CUSTOM일 때)
  &includeCompleted=false
```

**응답:**
```json
{
  "success": true,
  "data": {
    "dateRange": {
      "start": "2024-01-01",
      "end": "2024-01-31"
    },
    "propertyInfo": {
      "startDatePropertyId": 15,
      "completionDatePropertyId": 16
    },
    "items": [
      {
        "itemId": 1,
        "title": "프로젝트 기획",
        "parentItemId": null,
        "itemDepth": 0,
        "requestDate": "2024-01-05",
        "dueDate": "2024-01-20",
        "startDate": "2024-01-07",
        "completionDate": "2024-01-18",
        "hasStartDateProperty": true,
        "hasCompletionDateProperty": true,
        "status": "IN_PROGRESS",
        "priority": "HIGH",
        "assigneeUserName": "홍길동",
        "createdAt": "2024-01-03T10:00:00"
      },
      {
        "itemId": 2,
        "title": "요구사항 분석",
        "parentItemId": 1,
        "itemDepth": 1,
        "requestDate": "2024-01-05",
        "dueDate": "2024-01-10",
        "startDate": null,
        "completionDate": null,
        "hasStartDateProperty": false,
        "hasCompletionDateProperty": false,
        "status": "COMPLETED",
        "priority": "NORMAL",
        "assigneeUserName": "김철수",
        "createdAt": "2024-01-03T11:00:00"
      }
    ]
  }
}
```

### 6.3 날짜 수정 API

#### 기본 날짜 수정 (요청일/마감일)
```
PUT /api/boards/{boardId}/items/{itemId}
{
  "requestDate": "2024-01-05",
  "dueDate": "2024-01-20"
}
```

#### 동적 속성 날짜 수정 (시작일/완료일)
```
PUT /api/boards/{boardId}/items/{itemId}/properties/{propertyId}
{
  "value": "2024-01-07"
}
```

---

## 7. 컴포넌트 구조

### 7.1 디렉토리 구조
```
src/
├── components/
│   ├── calendar/
│   │   ├── CalendarView.vue        # 메인 컴포넌트
│   │   ├── CalendarHeader.vue      # 년/월 네비게이션, 뷰 전환
│   │   ├── CalendarMonthGrid.vue   # 월간 그리드
│   │   ├── CalendarWeekView.vue    # 주간 뷰
│   │   ├── CalendarDayView.vue     # 일간 뷰
│   │   ├── CalendarCell.vue        # 날짜 셀
│   │   └── CalendarEventBadge.vue  # 이벤트 표시 (○ Todo / ■ Item)
│   │
│   └── gantt/
│       ├── GanttChart.vue          # 메인 컴포넌트
│       ├── GanttToolbar.vue        # 옵션 (단위/범위 선택)
│       ├── GanttLegend.vue         # 범례 (◆요청일 ◇마감일 ●시작일 ○완료일)
│       ├── GanttTimeline.vue       # 상단 타임라인 헤더
│       ├── GanttTaskList.vue       # 좌측 업무 목록 (└ 하위업무)
│       ├── GanttRow.vue            # 업무 행
│       ├── GanttBarPlan.vue        # 요청일~마감일 바 (D&D 가능)
│       ├── GanttBarActual.vue      # 시작일~완료일 바 (조건부 D&D)
│       ├── GanttPoint.vue          # 단일 포인트
│       ├── GanttTodayLine.vue      # 오늘 표시선
│       └── GanttTooltip.vue        # 호버 툴팁
│
├── views/
│   ├── CalendarView.vue            # /calendar 페이지
│   └── GanttView.vue               # /gantt 페이지
│
├── stores/
│   └── calendar.ts                 # 캘린더/Gantt 상태관리
│
├── api/
│   └── calendar.api.ts             # 캘린더/Gantt API
│
└── types/
    └── calendar.ts                 # 타입 정의
```

### 7.2 Calendar UI 와이어프레임
```
┌─────────────────────────────────────────────────────────────┐
│  ◀  2024년 1월  ▶              [월간] [주간] [일간]        │
│                                 ☑ Todo ☑ 업무              │
├─────────────────────────────────────────────────────────────┤
│  일     월     화     수     목     금     토               │
├─────────────────────────────────────────────────────────────┤
│       │  1   │  2   │  3   │  4   │  5   │  6             │
│       │      │      │○회의 │      │      │               │
│       │      │      │■설계 │      │      │               │
├───────┼──────┼──────┼──────┼──────┼──────┼──────┤
│  7   │  8   │  9   │ 10  │ 11  │ 12  │ 13             │
│      │○보고 │      │      │■개발 │      │               │
│      │■리뷰 │      │      │      │      │               │
├───────┼──────┼──────┼──────┼──────┼──────┼──────┤
│ 14   │[15]  │ 16  │ 17  │ 18  │ 19  │ 20             │
│      │TODAY │      │      │      │      │               │
└─────────────────────────────────────────────────────────────┘

범례: ○ Todo  ■ 업무  [15] 오늘 하이라이트
```

### 7.3 Gantt UI 와이어프레임
```
┌─────────────────────────────────────────────────────────────────────────┐
│ 보드: [프로젝트A ▼]  단위: [DAY▼] 범위: [MONTH▼] [사용자지정...]       │
├─────────────────────────────────────────────────────────────────────────┤
│ 범례: ◆━━◇ 요청~마감   ●━━○ 시작~완료   ▼ TODAY                       │
├─────────────────────────────────────────────────────────────────────────┤
│                        │ 1/1  1/5  1/10  1/15 ▼1/20  1/25  1/30        │
├────────────────────────┼────────────────────────────────────────────────┤
│ 프로젝트 기획          │  ◆━━━━━━━━━━━━━━━━━━━━━━━━◇                   │
│                        │      ●━━━━━━━━━━━━━━━━━━○                     │
├────────────────────────┼────────────────────────────────────────────────┤
│ └ 요구사항 분석        │  ◆━━━━━━━◇                                    │
│   (시작일만 있음)      │    ●                                          │
├────────────────────────┼────────────────────────────────────────────────┤
│ └ 화면 설계            │      ◆━━━━━━━━━━━◇                            │
│   (시작일/완료일 없음) │      (실제 바 미표시)                          │
├────────────────────────┼────────────────────────────────────────────────┤
│ 마케팅 준비            │                              ◆                 │
│   (마감일 없음)        │                   ●━━━━━━━━━○                  │
└────────────────────────┴────────────────────────────────────────────────┘
```

---

## 8. TODAY 하이라이트

### 8.1 Calendar
- 오늘 날짜 셀 배경색 강조 (파란색 테두리 또는 배경)
- 스크롤 무관하게 항상 표시

### 8.2 Gantt
- 세로 점선으로 TODAY 표시
- 스크롤 시에도 "TODAY" 인디케이터 고정 표시
- 클릭 시 TODAY 위치로 스크롤 이동 버튼

---

## 9. 사이드바 메뉴 추가

```
┌─────────────────────────────────────────┐
│  TaskFlow                               │
├─────────────────────────────────────────┤
│  📋 업무 페이지                          │
│  ✅ 완료 작업 메뉴                       │
│  📝 작업 등록 메뉴                       │
│  📊 이력관리 메뉴                        │
│  ─────────────────────                  │
│  📅 캘린더              ← 신규           │
│  📈 Gantt 차트          ← 신규           │
│  ─────────────────────                  │
│  ✓ Todo List                            │
│  ...                                    │
└─────────────────────────────────────────┘
```

---

## 10. 라우터 추가

```typescript
// router/index.ts
{
  path: '/calendar',
  name: 'Calendar',
  component: () => import('@/views/CalendarView.vue'),
  meta: { requiresAuth: true }
},
{
  path: '/gantt',
  name: 'Gantt',
  component: () => import('@/views/GanttView.vue'),
  meta: { requiresAuth: true }
}
```

---

## 11. 에러 방지 로직

### 11.1 Frontend 처리
```typescript
// 시작일/완료일 편집 가능 여부 판단
function canEditActualDates(item: GanttItem): {
  canEditStartDate: boolean
  canEditCompletionDate: boolean
  canDragActualBar: boolean
} {
  return {
    canEditStartDate: item.hasStartDateProperty,
    canEditCompletionDate: item.hasCompletionDateProperty,
    // 둘 다 있어야 D&D 가능
    canDragActualBar: item.hasStartDateProperty && item.hasCompletionDateProperty
  }
}
```

### 11.2 Backend 처리
```java
// 시작일/완료일 수정 시 속성 존재 여부 확인
public void updateItemPropertyDate(Long itemId, Long propertyId, LocalDate value) {
    // 1. 해당 업무에 해당 속성이 지정되어 있는지 확인
    ItemProperty itemProperty = itemPropertyMapper
        .findByItemIdAndPropertyId(itemId, propertyId);

    if (itemProperty == null) {
        throw new BusinessException("해당 업무에 이 속성이 설정되어 있지 않습니다.");
    }

    // 2. 속성 타입이 DATE인지 확인
    PropertyDef propertyDef = propertyDefMapper.findById(propertyId);
    if (!"DATE".equals(propertyDef.getPropertyType())) {
        throw new BusinessException("날짜 타입 속성이 아닙니다.");
    }

    // 3. 값 업데이트
    itemPropertyMapper.updateValue(itemProperty.getItemPropertyId(), value.toString());
}
```

---

## 12. 구현 단계

### Phase 1: 기반 작업
1. TB_ITEM 컬럼 확인 (REQUEST_DATE 존재 여부)
2. "시작일", "완료일" 속성 조회 로직 구현
3. Backend API 구현 (Calendar, Gantt)
4. 타입 정의 및 API 모듈

### Phase 2: Calendar 구현
1. CalendarView 컴포넌트
2. 월간/주간/일간 뷰
3. Todo(○) / Item(■) 구분 표시
4. TODAY 하이라이트

### Phase 3: Gantt 구현
1. 기본 레이아웃 + 범례
2. 요청일~마감일 바 (D&D)
3. 시작일~완료일 바 (조건부 D&D)
4. 날짜 미지정 시 처리
5. 하위 업무 "└" 표시
6. TODAY 세로선
7. 옵션 (DAY/WEEK/MONTH, 범위 설정)

---

## 13. 제약사항 및 참고

### 13.1 업무 간 선후 관계
- **현재**: 없음
- **향후 계획**: 의존성(dependency) 기능 추가 예정

### 13.2 기술 구현
- **외부 라이브러리**: 사용하지 않음 (직접 구현)
- **이유**: 프로젝트 스타일 일관성, 커스터마이징 자유도

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|-----|------|----------|
| v1.0 | 2026-01-18 | 최초 작성 |
| v2.0 | 2026-01-18 | 요청일 용어 정리, Gantt 옵션 상세화 |
| v3.0 | 2026-01-18 | 시작일/완료일 동적 속성 반영, 편집 규칙 상세화 |
