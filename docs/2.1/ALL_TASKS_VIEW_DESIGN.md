# 전체 업무 목록 (All Tasks View) 설계서

> 버전: 1.0
> 작성일: 2025-01-14
> 상태: 설계 검토 중

---

## 1. 기능 개요

### 1.1 목적
- 사용자가 접근 가능한 **모든 보드의 업무** + **공유받은 개별 업무**를 한 곳에서 조회
- 업무 출처(보드명, 공유자) 명확히 표시
- 독립적인 정렬/필터 상태 관리로 다른 보드 뷰와 충돌 방지

### 1.2 핵심 요구사항

| 항목 | 요구사항 |
|------|----------|
| 통합 조회 | 소유 보드 + 공유 보드 + 개별 공유 업무 통합 |
| 출처 표시 | 보드명, 공유 여부, 공유자 표시 |
| 독립 정렬 | 전체 업무 전용 정렬/필터 상태 (다른 보드와 분리) |
| 충돌 방지 | 개별 업무 수정 시 전체 목록 상태와 독립적 처리 |
| 읽기 중심 | 조회/필터 중심, 업무 수정은 상세 패널에서 처리 |

---

## 2. 데이터베이스 설계

### 2.1 기존 테이블 활용 (신규 테이블 불필요)

기존 테이블로 충분히 구현 가능:
- `TB_BOARD` - 보드 정보
- `TB_BOARD_SHARE` - 보드 공유 정보
- `TB_ITEM` - 업무 정보
- `TB_ITEM_SHARE` - 업무 공유 정보

### 2.2 조회 쿼리 설계

```sql
-- 전체 업무 조회 (소유 보드 + 공유 보드 + 개별 공유 업무)
SELECT
    i.*,
    b.BOARD_NAME,
    b.COLOR AS BOARD_COLOR,
    -- 업무 출처 구분
    CASE
        WHEN b.OWNER_USERNAME = #{username} THEN 'OWNED'
        WHEN bs.USERNAME = #{username} THEN 'BOARD_SHARED'
        WHEN ish.USERNAME = #{username} THEN 'ITEM_SHARED'
    END AS SOURCE_TYPE,
    -- 공유자 정보
    CASE
        WHEN bs.USERNAME = #{username} THEN bs.CREATED_BY
        WHEN ish.USERNAME = #{username} THEN ish.SHARED_BY
        ELSE NULL
    END AS SHARED_BY_USERNAME,
    u_sharer.NAME AS SHARED_BY_NAME
FROM TB_ITEM i
INNER JOIN TB_BOARD b ON i.BOARD_ID = b.BOARD_ID
LEFT JOIN TB_BOARD_SHARE bs ON b.BOARD_ID = bs.BOARD_ID
    AND bs.USERNAME = #{username} AND bs.DELETED_AT IS NULL
LEFT JOIN TB_ITEM_SHARE ish ON i.ITEM_ID = ish.ITEM_ID
    AND ish.USERNAME = #{username} AND ish.DELETED_AT IS NULL
LEFT JOIN TB_USER u_sharer ON ...
WHERE (
    b.OWNER_USERNAME = #{username}                    -- 소유 보드
    OR bs.USERNAME = #{username}                      -- 공유 보드
    OR ish.USERNAME = #{username}                     -- 개별 공유 업무
)
AND b.USE_YN = 'Y'
```

---

## 3. API 설계

### 3.1 전체 업무 조회 API

```
GET /api/items/all
```

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| keyword | String | - | 검색어 |
| status | String | - | 상태 필터 |
| priority | String | - | 우선순위 필터 |
| sourceType | String | - | 출처 필터 (OWNED/BOARD_SHARED/ITEM_SHARED/ALL) |
| boardId | Long | - | 특정 보드 필터 |
| groupId | Long | - | 그룹 필터 |
| assigneeUsername | String | - | 담당자 필터 |
| startDate | LocalDate | - | 시작일 필터 |
| endDate | LocalDate | - | 종료일 필터 |
| includeCompleted | Boolean | - | 완료 포함 (기본: false) |
| includeDeleted | Boolean | - | 삭제 포함 (기본: false) |
| sortField | String | - | 정렬 필드 (createdAt, dueDate, priority, boardName 등) |
| sortDirection | String | - | 정렬 방향 (asc/desc) |
| page | Integer | - | 페이지 번호 (기본: 0) |
| size | Integer | - | 페이지 크기 (기본: 50) |

#### 응답 DTO

```java
public class AllItemsPageResponse {
    private List<AllItemResponse> items;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;

    // 통계 정보
    private AllItemsStats stats;
}

public class AllItemResponse extends ItemResponse {
    // 기존 ItemResponse 필드 상속

    // 추가 필드
    private String sourceType;        // OWNED, BOARD_SHARED, ITEM_SHARED
    private String sharedByUsername;  // 공유자 USERNAME
    private String sharedByName;      // 공유자 이름
    private String boardColor;        // 보드 색상
    private LocalDateTime sharedAt;   // 공유 일시
}

public class AllItemsStats {
    private int totalCount;
    private int ownedCount;
    private int boardSharedCount;
    private int itemSharedCount;
    private Map<String, Integer> byStatus;    // 상태별 개수
    private Map<String, Integer> byPriority;  // 우선순위별 개수
}
```

### 3.2 접근 가능 보드 목록 조회 (필터용)

```
GET /api/boards/accessible
```

#### 응답

```java
public class AccessibleBoardResponse {
    private Long boardId;
    private String boardName;
    private String color;
    private String sourceType;  // OWNED, SHARED
    private int itemCount;
}
```

---

## 4. 프론트엔드 설계

### 4.1 컴포넌트 구조

```
src/
├── views/
│   └── AllTasksView.vue          # 전체 업무 페이지
├── components/
│   └── allTasks/
│       ├── AllTasksFilter.vue    # 필터 패널
│       ├── AllTasksTable.vue     # 테이블 뷰
│       ├── AllTasksStats.vue     # 통계 표시
│       └── SourceBadge.vue       # 출처 배지 (소유/공유)
├── stores/
│   └── allTasks.ts               # 전체 업무 전용 스토어
├── api/
│   └── allTasks.ts               # API 호출 모듈
└── types/
    └── allTasks.ts               # 타입 정의
```

### 4.2 전용 스토어 설계 (독립성 확보)

```typescript
// stores/allTasks.ts
export const useAllTasksStore = defineStore('allTasks', () => {
  // ===== 독립 상태 (다른 스토어와 분리) =====
  const items = ref<AllItemResponse[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 전용 필터 상태 (보드별 필터와 독립)
  const filters = ref<AllItemsFilter>({
    keyword: '',
    status: null,
    priority: null,
    sourceType: 'ALL',
    boardId: null,
    groupId: null,
    assigneeUsername: null,
    startDate: null,
    endDate: null,
    includeCompleted: false,
    includeDeleted: false,
    sortField: 'createdAt',
    sortDirection: 'desc'
  })

  // 페이징 상태
  const pagination = ref({
    page: 0,
    size: 50,
    totalElements: 0,
    totalPages: 0
  })

  // 통계 정보
  const stats = ref<AllItemsStats | null>(null)

  // 접근 가능 보드 목록 (필터용)
  const accessibleBoards = ref<AccessibleBoardResponse[]>([])

  // ===== Actions =====
  async function fetchAllItems() { ... }
  async function fetchAccessibleBoards() { ... }
  function updateFilter(key: string, value: any) { ... }
  function resetFilters() { ... }
  function reset() { ... }

  return { ... }
})
```

### 4.3 UI 레이아웃

```
┌─────────────────────────────────────────────────────────────────┐
│  전체 업무                                      [필터 토글]      │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ 통계: 전체 48건 | 내 보드 32건 | 공유 보드 12건 | 공유 업무 4건 ││
│  └─────────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────────┤
│  [필터 패널 - 접힘 가능]                                          │
│  출처: [전체▼] 보드: [전체▼] 상태: [전체▼] 담당자: [전체▼]         │
│  기간: [____] ~ [____]  검색: [________________] [검색]           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────┬──────────┬────────┬──────┬────────┬────────┬──────────┐ │
│  │출처│  보드명   │ 업무명  │ 상태 │ 우선순위│ 담당자 │  마감일  │ │
│  ├────┼──────────┼────────┼──────┼────────┼────────┼──────────┤ │
│  │🏠  │ 개발업무  │ API개발 │진행중│  높음   │ 홍길동 │ 01-20   │ │
│  │📤  │ 기획업무  │ 기획서  │시작전│  보통   │ 김철수 │ 01-25   │ │
│  │📥  │ -        │ 리뷰요청│진행중│  긴급   │ 나     │ 01-18   │ │
│  └────┴──────────┴────────┴──────┴────────┴────────┴──────────┘ │
│                                                                 │
│  [< 이전] 1 / 3 [다음 >]                                         │
└─────────────────────────────────────────────────────────────────┘

출처 아이콘:
🏠 - 내 보드 (OWNED)
📤 - 공유 보드 (BOARD_SHARED)
📥 - 공유 업무 (ITEM_SHARED)
```

### 4.4 업무 행 표시 정보

| 컬럼 | 설명 |
|------|------|
| 출처 | 아이콘 + 툴팁 (공유자 정보) |
| 보드명 | 보드 이름 + 색상 인디케이터 |
| 업무명 | 클릭 시 상세 패널 오픈 |
| 상태 | 상태 배지 |
| 우선순위 | 우선순위 배지 |
| 담당자 | 담당자 이름 |
| 마감일 | 날짜 표시 (지남 시 빨간색) |

### 4.5 충돌 방지 전략

```typescript
// 1. 독립 스토어 사용
// allTasks 스토어는 item 스토어와 완전 분리

// 2. 업무 수정 시 처리
async function onItemUpdated(itemId: number, updatedData: Partial<Item>) {
  // allTasks 목록에서 해당 아이템만 갱신
  const index = items.value.findIndex(i => i.itemId === itemId)
  if (index !== -1) {
    // API로 개별 아이템 정보만 다시 조회
    const response = await allTasksApi.getItem(itemId)
    if (response.success && response.data) {
      items.value[index] = response.data
    }
  }
}

// 3. SSE 이벤트 처리
// 전체 목록 새로고침이 아닌 개별 아이템 갱신으로 처리
function handleSseEvent(event: SseEvent) {
  switch (event.type) {
    case 'item:updated':
      onItemUpdated(event.data.itemId, event.data)
      break
    case 'item:deleted':
      items.value = items.value.filter(i => i.itemId !== event.data.itemId)
      break
    case 'item:created':
      // 신규 아이템이 접근 권한 내에 있으면 추가
      if (isAccessible(event.data)) {
        // 현재 정렬 기준에 맞게 삽입
        insertItemSorted(event.data)
      }
      break
  }
}

// 4. 목록 갱신 최소화
// 필터/정렬 변경 시에만 전체 재조회
// 개별 업무 수정은 해당 아이템만 갱신
```

---

## 5. 메뉴 구조 변경

### 5.1 사이드바 메뉴 추가

```
┌─────────────────────────────────────────┐
│  TaskFlow                               │
├─────────────────────────────────────────┤
│  📋 전체 업무          ← 신규 메뉴       │
│  ─────────────────────                  │
│  📋 업무 페이지          ← 기존 (보드별)  │
│  ✅ 완료 작업 메뉴                       │
│  📝 작업 등록 메뉴                       │
│  📊 이력관리 메뉴                        │
│  ...                                    │
└─────────────────────────────────────────┘
```

### 5.2 라우터 설정

```typescript
{
  path: '/all-tasks',
  name: 'AllTasks',
  component: () => import('@/views/AllTasksView.vue'),
  meta: { requiresAuth: true }
}
```

---

## 6. 파일 목록

### 6.1 백엔드

| 파일 | 작업 |
|------|------|
| `dto/item/AllItemResponse.java` | 신규 DTO |
| `dto/item/AllItemsPageResponse.java` | 신규 응답 DTO |
| `dto/item/AllItemsSearchRequest.java` | 신규 요청 DTO |
| `dto/item/AllItemsStats.java` | 통계 DTO |
| `controller/AllItemsController.java` | 신규 컨트롤러 |
| `service/AllItemsService.java` | 신규 서비스 인터페이스 |
| `service/impl/AllItemsServiceImpl.java` | 신규 서비스 구현체 |
| `mapper/ItemMapper.xml` | 전체 업무 조회 쿼리 추가 |
| `mapper/ItemMapper.java` | 매퍼 메서드 추가 |

### 6.2 프론트엔드

| 파일 | 작업 |
|------|------|
| `views/AllTasksView.vue` | 신규 페이지 |
| `components/allTasks/AllTasksFilter.vue` | 필터 컴포넌트 |
| `components/allTasks/AllTasksTable.vue` | 테이블 컴포넌트 |
| `components/allTasks/AllTasksStats.vue` | 통계 컴포넌트 |
| `components/allTasks/SourceBadge.vue` | 출처 배지 |
| `stores/allTasks.ts` | 전용 스토어 |
| `api/allTasks.ts` | API 모듈 |
| `types/allTasks.ts` | 타입 정의 |
| `router/index.ts` | 라우트 추가 |
| `components/layout/Sidebar.vue` | 메뉴 추가 |

---

## 7. 주요 고려사항

### 7.1 성능 최적화
- 페이징 필수 (대량 데이터 대응)
- 초기 로딩 시 통계만 먼저 표시
- 무한 스크롤 또는 페이지네이션 선택 가능

### 7.2 독립성 보장
- `allTasks` 스토어는 `item` 스토어와 완전 분리
- 필터/정렬 상태 독립 관리
- SSE 이벤트는 개별 아이템 갱신으로 처리

### 7.3 권한 처리
- 보드 공유 권한에 따른 수정 가능 여부 표시
- 읽기 전용 업무는 명확히 표시
- 상세 패널에서 권한에 따른 UI 분기

---

## 8. 구현 순서

```
1단계: 백엔드 API 구현
   - DTO 클래스 생성
   - Mapper 쿼리 추가
   - Service/Controller 구현

2단계: 프론트엔드 기본 구조
   - 스토어/API/타입 생성
   - 기본 뷰 컴포넌트

3단계: UI 컴포넌트 구현
   - 테이블/필터/통계 컴포넌트
   - 메뉴 및 라우터 연결

4단계: SSE 연동 및 최적화
   - 실시간 업데이트 처리
   - 성능 최적화
```

---

## 9. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2025-01-14 | - | 최초 작성 |
