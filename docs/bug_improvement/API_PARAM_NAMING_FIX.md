# API 파라미터 명명 규칙 수정 기록

## 수정일: 2025-01-19

---

## 1. 문제 개요

### 1.1 근본 원인
- `client.ts`에서 GET 요청의 query params를 snake_case로 자동 변환
- 백엔드 `@RequestParam`은 camelCase 기대
- 결과: `withDetails=true` → `with_details=true`로 변환되어 백엔드에서 인식 불가

### 1.2 영향 범위
- Category 속성 조회 시 `withDetails` 파라미터 무시됨
- Calendar/Event 관련 API 파라미터 불일치

---

## 2. 수정 내역

### 2.1 Backend Controller 수정 (snake_case → camelCase)

#### EventController.java
| 변경 전 | 변경 후 |
|---------|---------|
| `@RequestParam(value = "start_date")` | `@RequestParam(value = "startDate")` |
| `@RequestParam(value = "end_date")` | `@RequestParam(value = "endDate")` |
| `@RequestParam(value = "calendar_id")` | `@RequestParam(value = "calendarId")` |
| `@RequestParam(value = "include_shared")` | `@RequestParam(value = "includeShared")` |

#### CalendarController.java
| 변경 전 | 변경 후 |
|---------|---------|
| `@RequestParam(value = "include_todo")` | `@RequestParam(value = "includeTodo")` |
| `@RequestParam(value = "include_item")` | `@RequestParam(value = "includeItem")` |
| `@RequestParam(value = "board_id")` | `@RequestParam(value = "boardId")` |
| `@RequestParam(value = "start_date")` | `@RequestParam(value = "startDate")` |
| `@RequestParam(value = "end_date")` | `@RequestParam(value = "endDate")` |
| `@RequestParam(value = "include_completed")` | `@RequestParam(value = "includeCompleted")` |

#### UserCalendarController.java
| 변경 전 | 변경 후 |
|---------|---------|
| `@RequestParam("share_type")` | `@RequestParam("shareType")` |

#### LunarController.java
| 변경 전 | 변경 후 |
|---------|---------|
| `@RequestParam(value = "leap_month")` (3곳) | `@RequestParam(value = "leapMonth")` |

### 2.2 Frontend 수정

#### client.ts
```typescript
// 제거된 코드 (요청 인터셉터 내)
// if (config.params && typeof config.params === 'object') {
//   config.params = convertKeysToSnakeCase(config.params)
// }

// 변경 후: GET 요청 params는 변환하지 않음 (Spring @RequestParam은 camelCase 사용)
// JSON body만 snake_case 변환 (Jackson SNAKE_CASE 설정과 일치)
```

#### calendar.ts
| 함수 | 변경 전 | 변경 후 |
|------|---------|---------|
| `getEvents()` | `start_date`, `end_date`, `calendar_id`, `include_shared` | `startDate`, `endDate`, `calendarId`, `includeShared` |
| `lunarToSolar()` | `leap_month` | `leapMonth` |
| `validateLunarDate()` | `leap_month` | `leapMonth` |

#### CalendarView.vue
```typescript
// 변경 전
const response = await calendarApi.getEvents({
  start_date: startDate,
  end_date: endDate,
  include_shared: true
})

// 변경 후
const response = await calendarApi.getEvents({
  startDate: startDate,
  endDate: endDate,
  includeShared: true
})
```

---

## 3. 명명 규칙 표준

### 3.1 확정된 규칙

| 구분 | 명명 규칙 | 예시 |
|------|----------|------|
| **GET Query Params** | camelCase | `?startDate=2025-01-01&includeCompleted=true` |
| **POST/PUT JSON Body** | snake_case | `{ "start_date": "2025-01-01" }` |
| **Backend @RequestParam** | camelCase | `@RequestParam("startDate")` |
| **Backend @RequestBody** | snake_case (Jackson) | Jackson `SNAKE_CASE` 설정 |

### 3.2 Jackson 설정 (application.yml)
```yaml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
```

### 3.3 MyBatis 설정
```yaml
mybatis:
  configuration:
    map-underscore-to-camel-case: true
```

---

## 4. 검증 완료 컨트롤러

| 컨트롤러 | @RequestParam 명명 | 상태 |
|---------|-------------------|------|
| TodoController | camelCase | ✅ |
| BoardController | camelCase | ✅ |
| DepartmentController | camelCase | ✅ |
| UserController | camelCase | ✅ |
| ItemController | camelCase | ✅ |
| EventController | camelCase | ✅ (수정됨) |
| CalendarController | camelCase | ✅ (수정됨) |
| UserCalendarController | camelCase | ✅ (수정됨) |
| LunarController | camelCase | ✅ (수정됨) |

---

## 5. 향후 주의사항

1. **새 API 추가 시**: `@RequestParam`은 반드시 camelCase 사용
2. **프론트엔드 API 호출 시**: query params는 camelCase, JSON body는 snake_case
3. **client.ts 수정 금지**: params 변환 로직 재추가 금지

---

## 6. 관련 파일

- `backend/src/main/java/com/taskflow/controller/EventController.java`
- `backend/src/main/java/com/taskflow/controller/CalendarController.java`
- `backend/src/main/java/com/taskflow/controller/UserCalendarController.java`
- `backend/src/main/java/com/taskflow/controller/LunarController.java`
- `frontend/src/api/client.ts`
- `frontend/src/api/calendar.ts`
- `frontend/src/views/CalendarView.vue`
