# 음력 반복 이벤트 기능 구현 기록

## 수정일: 2025-01-19

---

## 1. 문제 개요

### 1.1 버그 설명
- 음력 반복 이벤트(MONTHLY_LUNAR, YEARLY_LUNAR) 선택 시 반복이 동작하지 않음
- EventService.calculateNextOccurrence()에서 `return null;`로 처리되어 첫 번째 이벤트만 표시됨

### 1.2 추가 요구사항
- 주단위 음력 반복(WEEKLY_LUNAR) 추가
- UI에서 반복 옵션 단순화 (음력 체크박스에 따라 자동 처리)
- 혼동 방지 로직 (체크박스/라디오버튼 상태에 따른 변환)
- 윤달 대체 시 이벤트 설명에 기록

---

## 2. 수정 내역

### 2.1 Backend: EventService.java

#### 변경 1: LunarCalendarService 주입
```java
private final LunarCalendarService lunarService;  // 추가
```

#### 변경 2: calculateNextOccurrence() 메서드 확장
```java
case "WEEKLY_LUNAR":
    return calculateNextLunarWeekDate(current, interval, event);
case "MONTHLY_LUNAR":
    return calculateNextLunarMonthDate(current, interval, event);
case "YEARLY_LUNAR":
    return calculateNextLunarYearDate(current, interval, event);
```

#### 변경 3: 신규 메서드 추가
| 메서드 | 기능 |
|--------|------|
| `calculateNextLunarWeekDate()` | 음력 7일 기준 다음 발생일 계산 |
| `calculateNextLunarMonthDate()` | 매월 같은 음력 일자의 양력 날짜 계산 |
| `calculateNextLunarYearDate()` | 매년 같은 음력 월/일의 양력 날짜 계산 |
| `getLunarMonthDaysForCalc()` | 음력 월의 일수 조회 (윤달 고려) |
| `appendLeapMonthNote()` | 윤달 대체 시 설명에 기록 |

### 2.2 Frontend: EventFormModal.vue

#### 변경 1: 반복 옵션 단순화
```typescript
// 변경 전
const recurrenceTypeOptions = [
  { value: 'DAILY', label: '매일' },
  { value: 'WEEKLY', label: '매주' },
  { value: 'MONTHLY', label: '매월' },
  { value: 'YEARLY', label: '매년' },
  { value: 'MONTHLY_LUNAR', label: '매월 (음력)' },  // 제거
  { value: 'YEARLY_LUNAR', label: '매년 (음력)' }    // 제거
]

// 변경 후 - 4개 옵션만 (음력 체크박스에 따라 자동 처리)
const recurrenceTypeOptions = [
  { value: 'DAILY', label: '매일' },
  { value: 'WEEKLY', label: '매주' },
  { value: 'MONTHLY', label: '매월' },
  { value: 'YEARLY', label: '매년' }
]
```

#### 변경 2: 신규 함수 추가
| 함수 | 기능 |
|------|------|
| `getEffectiveRecurrenceType()` | 음력 여부에 따라 recurrenceType 변환 (MONTHLY → MONTHLY_LUNAR) |
| `getBaseRecurrenceType()` | _LUNAR 접미사 제거 (편집 시 UI 표시용) |
| `prepareLunarData()` | 음력 데이터 준비 (혼동 방지 로직) |

#### 변경 3: 저장 로직 개선
```typescript
// 음력 정보 준비 (혼동 방지)
const lunarData = prepareLunarData()

// 반복 타입 결정 (음력 체크박스에 따라 자동 변환)
const effectiveRecurrenceType = form.value.isRecurring
  ? getEffectiveRecurrenceType(form.value.recurrenceType, form.value.isLunar)
  : undefined
```

---

## 3. 처리 로직

### 3.1 저장 시
```
1. 음력 체크박스 확인
   ├─ false → 양력 처리, recurrenceType 그대로
   └─ true  → 음력 처리

2. 음력 정보 확인
   ├─ 존재 → 그대로 사용
   └─ 없음 → startDate(양력)를 음력으로 변환

3. recurrenceType 변환 (음력인 경우)
   ├─ DAILY   → DAILY (구분 없음)
   ├─ WEEKLY  → WEEKLY_LUNAR
   ├─ MONTHLY → MONTHLY_LUNAR
   └─ YEARLY  → YEARLY_LUNAR
```

### 3.2 조회 시 (반복 확장)
```
1. recurrenceType별 다음 발생일 계산
   ├─ WEEKLY_LUNAR  → 음력 7*interval일 후 → 양력 변환
   ├─ MONTHLY_LUNAR → 다음 음력 월 같은 일 → 양력 변환
   └─ YEARLY_LUNAR  → 다음 음력 연 같은 월/일 → 양력 변환

2. 윤달 처리
   ├─ 해당 년도 윤달 존재 → 윤달로 계산
   └─ 윤달 없음 → 평달로 대체 + 설명에 기록
```

---

## 4. 윤달 대체 기록 형식

해당 년도에 윤달이 없어 평달로 대체되는 경우 이벤트 설명에 자동 기록:

```
[2027년 6월: 윤달 없음 → 평달로 대체]
```

---

## 5. 테스트 체크리스트

### 5.1 기본 기능
- [ ] 양력 매일 반복
- [ ] 양력 매주 반복
- [ ] 양력 매월 반복
- [ ] 양력 매년 반복
- [ ] 음력 매주 반복 (WEEKLY_LUNAR)
- [ ] 음력 매월 반복 (MONTHLY_LUNAR)
- [ ] 음력 매년 반복 (YEARLY_LUNAR)

### 5.2 UI 동작
- [ ] 음력 체크 시 반복 옵션 4개만 표시
- [ ] 편집 시 기존 _LUNAR 타입이 기본 타입으로 변환되어 표시
- [ ] 저장 시 음력 체크 + 매월 → MONTHLY_LUNAR로 저장

### 5.3 윤달 처리
- [ ] 윤달 이벤트 - 윤달 있는 해 표시
- [ ] 윤달 이벤트 - 윤달 없는 해 평달로 표시
- [ ] 윤달 대체 시 설명에 기록 확인

### 5.4 혼동 방지
- [ ] 체크박스=음력, 라디오버튼=양력 → 양력 날짜가 음력으로 변환되어 저장

---

## 6. 관련 파일

- `backend/src/main/java/com/taskflow/service/EventService.java`
- `frontend/src/components/calendar/EventFormModal.vue`
- `backend/src/main/java/com/taskflow/service/LunarCalendarService.java`
