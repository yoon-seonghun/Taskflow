# 전체 업무 목록 (All Tasks View) 설계서

> 버전: 1.2
> 작성일: 2025-01-14
> 수정일: 2025-01-17
> 상태: 설계 검토 중

---

## 1. 기능 개요

### 1.1 목적
- 사용자가 접근 가능한 **모든 보드의 업무** + **공유받은 개별 업무** + **배당받은 업무**를 한 곳에서 조회
- 업무 출처(보드명, 공유자, 배당자) 명확히 표시
- **하위 업무** 포함 트리 구조 지원
- 독립적인 정렬/필터 상태 관리로 다른 보드 뷰와 충돌 방지

### 1.2 핵심 요구사항

| 항목 | 요구사항 |
|------|----------|
| 통합 조회 | 소유 보드 + 공유 보드 + 개별 공유 업무 + **배당받은 업무** 통합 |
| 출처 표시 | 보드명, 공유 여부, 공유자, **배당자** 표시 |
| 하위 업무 | 부모-자식 관계 표시, 깊이 인덴트, 하위 업무 수 |
| 독립 정렬 | 전체 업무 전용 정렬/필터 상태 (다른 보드와 분리) |
| 충돌 방지 | 개별 업무 수정 시 전체 목록 상태와 독립적 처리 |
| 읽기 중심 | 조회/필터 중심, 업무 열기는 업무페이지 사이드슬라이드 상세 패널에서 처리 |

---

## 2. 데이터베이스 설계

### 2.1 기존 테이블 활용 (신규 테이블 불필요)

기존 테이블로 충분히 구현 가능:
- `TB_BOARD` - 보드 정보
- `TB_BOARD_SHARE` - 보드 공유 정보
- `TB_ITEM` - 업무 정보 (하위 업무 포함)
- `TB_ITEM_SHARE` - 업무 공유/배당 정보 (SHARE_TYPE으로 구분)

### 2.2 관련 테이블 구조

#### TB_ITEM (하위 업무 관련 컬럼)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| PARENT_ITEM_ID | BIGINT | 부모 업무 ID (NULL=기본 업무) |
| ITEM_DEPTH | INT | 업무 깊이 (0=기본, 1~2=하위) |
| CHILD_SORT_ORDER | INT | 하위 업무 내 정렬 순서 |

#### TB_ITEM_SHARE (배당 관련 컬럼)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| SHARE_TYPE | VARCHAR(20) | 공유 유형 (SHARE/ASSIGN) |
| PERMISSION | VARCHAR(20) | 권한 (VIEW/EDIT/FULL) |
| ASSIGNED_BY | VARCHAR(50) | 배당자 USERNAME (ASSIGN일 경우) |
| ASSIGNED_AT | DATETIME | 배당 일시 |

### 2.3 조회 쿼리 설계

```sql
-- 전체 업무 조회 (소유 보드 + 공유 보드 + 개별 공유/배당 업무)
SELECT
    i.*,
    b.BOARD_NAME,
    b.COLOR AS BOARD_COLOR,
    -- 하위 업무 집계
    (SELECT COUNT(*) FROM TB_ITEM c WHERE c.PARENT_ITEM_ID = i.ITEM_ID AND c.DELETED_AT IS NULL) AS CHILD_COUNT,
    (SELECT COUNT(*) FROM TB_ITEM c WHERE c.PARENT_ITEM_ID = i.ITEM_ID AND c.STATUS = 'COMPLETED' AND c.DELETED_AT IS NULL) AS COMPLETED_CHILD_COUNT,
    -- 부모 업무 정보
    p.TITLE AS PARENT_TITLE,
    p.STATUS AS PARENT_STATUS,
    -- 업무 출처 구분
    CASE
        WHEN b.OWNER_USERNAME = #{username} THEN 'OWNED'
        WHEN bs.USERNAME = #{username} THEN 'BOARD_SHARED'
        WHEN ish.USERNAME = #{username} AND ish.SHARE_TYPE = 'SHARE' THEN 'ITEM_SHARED'
        WHEN ish.USERNAME = #{username} AND ish.SHARE_TYPE = 'ASSIGN' THEN 'ASSIGNED'
    END AS SOURCE_TYPE,
    -- 공유/배당자 정보
    CASE
        WHEN bs.USERNAME = #{username} THEN bs.CREATED_BY
        WHEN ish.USERNAME = #{username} THEN COALESCE(ish.ASSIGNED_BY, ish.SHARED_BY)
        ELSE NULL
    END AS SHARED_BY_USERNAME,
    u_sharer.NAME AS SHARED_BY_NAME,
    -- 배당 정보
    ish.SHARE_TYPE,
    ish.PERMISSION AS ITEM_PERMISSION,
    ish.ASSIGNED_BY,
    ish.ASSIGNED_AT,
    u_assigner.NAME AS ASSIGNED_BY_NAME
FROM TB_ITEM i
INNER JOIN TB_BOARD b ON i.BOARD_ID = b.BOARD_ID
LEFT JOIN TB_ITEM p ON i.PARENT_ITEM_ID = p.ITEM_ID
LEFT JOIN TB_BOARD_SHARE bs ON b.BOARD_ID = bs.BOARD_ID
    AND bs.USERNAME = #{username} AND bs.DELETED_AT IS NULL
LEFT JOIN TB_ITEM_SHARE ish ON i.ITEM_ID = ish.ITEM_ID
    AND ish.USERNAME = #{username} AND ish.DELETED_AT IS NULL
LEFT JOIN TB_USER u_sharer ON ...
LEFT JOIN TB_USER u_assigner ON ish.ASSIGNED_BY = u_assigner.USERNAME
WHERE (
    b.OWNER_USERNAME = #{username}                    -- 소유 보드
    OR bs.USERNAME = #{username}                      -- 공유 보드
    OR ish.USERNAME = #{username}                     -- 개별 공유/배당 업무
)
AND b.USE_YN = 'Y'
-- 하위 업무 필터 옵션
<if test="includeChildren == false">
    AND i.PARENT_ITEM_ID IS NULL
</if>
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
| sourceType | String | - | 출처 필터 (OWNED/BOARD_SHARED/ITEM_SHARED/**ASSIGNED**/ALL) |
| boardId | Long | - | 특정 보드 필터 |
| groupId | Long | - | 그룹 필터 |
| assigneeUsername | String | - | 담당자 필터 |
| **assignedByUsername** | String | - | **배당자 필터** |
| startDate | LocalDate | - | 시작일 필터 |
| endDate | LocalDate | - | 종료일 필터 |
| includeCompleted | Boolean | - | 완료 포함 (기본: false) |
| includeDeleted | Boolean | - | 삭제 포함 (기본: false) |
| **includeChildren** | Boolean | - | **하위 업무 포함 (기본: true)** |
| **parentItemId** | Long | - | **특정 부모의 하위 업무만 조회** |
| **depthFilter** | Integer | - | **특정 깊이 필터 (0/1/2)** |
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
    // 기존 ItemResponse 필드 상속 (title, status, priority, dueDate, requestDate 등)

    // 출처 관련 필드
    private String sourceType;        // OWNED, BOARD_SHARED, ITEM_SHARED, ASSIGNED
    private String sharedByUsername;  // 공유자 USERNAME
    private String sharedByName;      // 공유자 이름
    private String boardColor;        // 보드 색상
    private LocalDateTime sharedAt;   // 공유 일시

    // 배당 관련 필드 (v2.1)
    private String shareType;         // SHARE, ASSIGN
    private String permission;        // VIEW, EDIT, FULL
    private String assignedByUsername; // 배당자 USERNAME
    private String assignedByName;    // 배당자 이름
    private LocalDateTime assignedAt; // 배당 일시

    // 하위 업무 관련 필드 (v2.2)
    private Long parentItemId;        // 부모 업무 ID
    private Integer itemDepth;        // 업무 깊이 (0=기본, 1~2=하위)
    private Integer childCount;       // 전체 하위 업무 수
    private Integer completedChildCount; // 완료된 하위 업무 수
    private Boolean hasChildren;      // 하위 업무 존재 여부
    private ParentInfo parentInfo;    // 부모 업무 정보

    // 참고: requestDate는 ItemResponse에서 상속 (요청일 표시용)
}

public class ParentInfo {
    private Long itemId;
    private String title;
    private String status;
}

public class AllItemsStats {
    private int totalCount;
    private int ownedCount;
    private int boardSharedCount;
    private int itemSharedCount;
    private int assignedCount;        // 배당받은 업무 수 (v2.1)
    private int rootTaskCount;        // 기본 업무 수 (depth=0)
    private int childTaskCount;       // 하위 업무 수 (depth>0)
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
│       ├── SourceBadge.vue       # 출처 배지 (소유/공유/배당)
│       └── ChildIndicator.vue    # 하위 업무 인디케이터
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
    assignedByUsername: null,    // 배당자 필터 (v2.1)
    startDate: null,
    endDate: null,
    includeCompleted: false,
    includeDeleted: false,
    includeChildren: true,       // 하위 업무 포함 (v2.2)
    depthFilter: null,           // 깊이 필터 (v2.2)
    childDisplayMode: 'tree',    // 하위 업무 표시 모드 (v1.2 기본값: tree)
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
┌───────────────────────────────────────────────────────────────────────────────────┐
│  전체 업무                                                         [필터 토글]      │
├───────────────────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────────────────────────────┐│
│  │ 통계: 전체 56건 | 내 보드 32건 | 공유 보드 12건 | 공유 4건 | 배당 8건            ││
│  │       기본 40건 | 하위 16건                                                    ││
│  └───────────────────────────────────────────────────────────────────────────────┘│
├───────────────────────────────────────────────────────────────────────────────────┤
│  [필터 패널 - 접힘 가능]                                                            │
│  출처: [전체▼] 보드: [전체▼] 상태: [전체▼] 담당자: [전체▼] 배당자: [전체▼]           │
│  깊이: [전체▼] 기간: [____] ~ [____]  검색: [________________] [검색]               │
├───────────────────────────────────────────────────────────────────────────────────┤
│                                                                                   │
│  ┌────┬──────────┬─────────────────┬──────┬────────┬────────┬────────┬──────────┐│
│  │출처│  보드명   │    업무명        │ 상태 │ 우선순위│ 담당자 │ 요청일  │  마감일  ││
│  ├────┼──────────┼─────────────────┼──────┼────────┼────────┼────────┼──────────┤│
│  │🏠  │ 개발업무  │ API개발 (2/3)   │진행중│  높음   │ 홍길동 │ 01-10  │ 01-20   ││
│  │    │          │  └ DB설계       │완료  │  보통   │ 홍길동 │ 01-10  │ 01-15   ││
│  │    │          │  └ 테스트 작성   │진행중│  보통   │ 김철수 │ 01-12  │ 01-18   ││
│  │📤  │ 기획업무  │ 기획서          │시작전│  보통   │ 김철수 │ 01-15  │ 01-25   ││
│  │📥  │ -        │ 리뷰요청        │진행중│  긴급   │ 나     │ 01-14  │ 01-18   ││
│  │🎯  │ PM업무   │ 보고서 작성      │시작전│  높음   │ 나     │ 01-17  │ 01-22   ││
│  └────┴──────────┴─────────────────┴──────┴────────┴────────┴────────┴──────────┘│
│                                                                                   │
│  [< 이전] 1 / 3 [다음 >]                                                           │
└───────────────────────────────────────────────────────────────────────────────────┘

출처 표시 (SharedItemsView 방식 참조):
- 출처 아바타: 공유자/배당자 이름 첫 글자를 원형 배지로 표시
- 배당 업무: 초록색 배경 (bg-green-100/green-900)
- 공유 업무: 파란색 배경 (bg-blue-100/blue-900)
- 이름 표시: 아바타 옆에 공유자/배당자 이름

출처 아이콘 (ItemBadges 컴포넌트 활용):
🏠 - 내 보드 (OWNED) - 별도 배지 없음
📤 - 공유 보드 (BOARD_SHARED) - 보드 공유 배지
📥 - 공유 업무 (ITEM_SHARED) - 개별 공유 배지
🎯 - 배당 업무 (ASSIGNED) - 배당 배지 (초록색)

하위 업무 표시 (기본: tree 모드):
- 부모 업무: "업무명 (완료수/전체수)" 형태로 진행률 표시
- 하위 업무: " └ 업무명" 형태로 최소 들여쓰기 (1칸 공백 + └)
- 하위 업무 툴팁: ParentInfoTooltip 컴포넌트 활용 (부모 업무 정보 표시)
- 부모-자식 통합 페이징: 기존 ItemTable/SubTaskList 방식 유지
```

### 4.4 업무 행 표시 정보

| 컬럼 | 설명 |
|------|------|
| 출처 | SharedItemsView 방식: 원형 아바타(공유자/배당자 첫 글자) + 이름 표시 |
| 보드명 | 보드 이름 + 색상 인디케이터 (회색 배경 라운드 배지) |
| 업무명 | ItemBadges(공유/배당 배지) + ParentInfoTooltip(하위 업무 시) + 제목, 클릭 시 상세 패널 오픈, **하위 업무는 최소 들여쓰기(└)**, 부모 업무는 **(완료/전체)** 표시 |
| 상태 | 상태 배지 (getStatusColor 스타일) |
| 우선순위 | Select 드롭다운 (직접 변경 가능) |
| 담당자 | 원형 아바타 + 담당자 이름 |
| 요청일 | 업무 요청 일자 (MM-DD 형식) |
| 마감일 | 업무 마감 일자 (지남 시 빨간색) |

### 4.5 출처별 툴팁 정보

| 출처 타입 | 툴팁 표시 내용 |
|----------|---------------|
| OWNED | "내 보드" |
| BOARD_SHARED | "공유 보드 (공유자: OOO)" |
| ITEM_SHARED | "공유받은 업무 (공유자: OOO, 권한: 편집)" |
| ASSIGNED | "**배당받은 업무 (배당자: OOO, 배당일: 2025-01-15, 권한: 전체)**" |

### 4.6 하위 업무 표시 옵션

```typescript
// 하위 업무 표시 모드
type ChildDisplayMode = 'flat' | 'tree' | 'collapse'

// flat: 모든 업무 평면 목록 (깊이 인덴트 표시)
// tree: 부모 업무 아래에 하위 업무 그룹화 ← 기본값
// collapse: 부모 업무만 표시, 클릭 시 하위 업무 펼침

const defaultChildDisplayMode: ChildDisplayMode = 'tree'
```

#### 하위 업무 표시 원칙 (v1.2)

| 항목 | 설명 |
|------|------|
| 기본 모드 | `tree` - 부모 업무 바로 아래에 하위 업무 표시 |
| 들여쓰기 | **최소화** - 1칸 공백 + └ 기호만 사용 (과도한 padding 지양) |
| 페이징 | **부모-자식 통합 페이징** - 부모와 하위 업무가 같은 페이지에 포함 |
| 하위 업무 툴팁 | ParentInfoTooltip 컴포넌트로 부모/루트 업무 정보 표시 |
| 진행률 표시 | 부모 업무에 "(완료/전체)" 형태로 하위 업무 진행률 표시 |

#### 기존 구현 참조

- **SubTaskList.vue** - 하위 업무 목록 표시 방식 참조
- **ItemTable.vue** - 부모-자식 통합 목록 표시 방식 참조
- **SharedItemsView.vue** - 공유/배당 업무 표시 패턴 참조 (아바타, 배지, 툴팁)

### 4.7 충돌 방지 전략

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
        insertItemSorted(event.data)
      }
      break
    case 'item:assigned':  // 배당 이벤트 (v2.1)
      if (event.data.assigneeUsername === currentUser.username) {
        // 나에게 배당된 업무 추가
        fetchItemAndAdd(event.data.itemId)
      }
      break
    case 'item:child_created':  // 하위 업무 생성 이벤트 (v2.2)
      updateParentChildCount(event.data.parentItemId)
      if (filters.value.includeChildren) {
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
| `dto/item/AllItemResponse.java` | 신규 DTO (배당/하위업무 필드 포함) |
| `dto/item/AllItemsPageResponse.java` | 신규 응답 DTO |
| `dto/item/AllItemsSearchRequest.java` | 신규 요청 DTO (배당자/깊이 필터 추가) |
| `dto/item/AllItemsStats.java` | 통계 DTO (배당/하위업무 통계 추가) |
| `dto/item/ParentInfo.java` | 부모 업무 정보 DTO |
| `controller/AllItemsController.java` | 신규 컨트롤러 |
| `service/AllItemsService.java` | 신규 서비스 인터페이스 |
| `service/impl/AllItemsServiceImpl.java` | 신규 서비스 구현체 |
| `mapper/ItemMapper.xml` | 전체 업무 조회 쿼리 추가 |
| `mapper/ItemMapper.java` | 매퍼 메서드 추가 |

### 6.2 프론트엔드

| 파일 | 작업 |
|------|------|
| `views/AllTasksView.vue` | 신규 페이지 |
| `components/allTasks/AllTasksFilter.vue` | 필터 컴포넌트 (배당자/깊이 필터 추가) |
| `components/allTasks/AllTasksTable.vue` | 테이블 컴포넌트 (하위 업무 인덴트) |
| `components/allTasks/AllTasksStats.vue` | 통계 컴포넌트 (배당/하위업무 통계) |
| `components/allTasks/SourceBadge.vue` | 출처 배지 (ASSIGNED 추가) |
| `components/allTasks/ChildIndicator.vue` | 하위 업무 인디케이터 (신규) |
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
- **하위 업무 조회 시 N+1 문제 방지 (배치 조회)**

### 7.2 독립성 보장
- `allTasks` 스토어는 `item` 스토어와 완전 분리
- 필터/정렬 상태 독립 관리
- SSE 이벤트는 개별 아이템 갱신으로 처리

### 7.3 권한 처리
- 보드 공유 권한에 따른 수정 가능 여부 표시
- **배당 권한(VIEW/EDIT/FULL)에 따른 편집 가능 범위 표시**
- 읽기 전용 업무는 명확히 표시
- 상세 패널에서 권한에 따른 UI 분기

### 7.4 하위 업무 처리
- 부모 업무 삭제 시 하위 업무 연쇄 처리 확인
- 하위 업무 완료 시 부모 업무 진행률 자동 갱신
- 깊이 제한 (최대 2단계) 준수
- **부모-자식 통합 페이징**: 부모와 하위 업무가 동일 페이지 내에 함께 표시
  - 기존 ItemTable/SubTaskList 방식과 동일하게 유지
  - 부모-자식 분리 페이징 불가 (부모 아래 하위 업무 즉시 표시)

---

## 8. 구현 순서

```
1단계: 백엔드 API 구현
   - DTO 클래스 생성 (배당/하위업무 필드 포함)
   - Mapper 쿼리 추가 (배당/하위업무 조인)
   - Service/Controller 구현

2단계: 프론트엔드 기본 구조
   - 스토어/API/타입 생성
   - 기본 뷰 컴포넌트

3단계: UI 컴포넌트 구현
   - 테이블/필터/통계 컴포넌트
   - 출처 배지 (배당 타입 추가)
   - 하위 업무 인디케이터
   - 메뉴 및 라우터 연결

4단계: SSE 연동 및 최적화
   - 실시간 업데이트 처리 (배당/하위업무 이벤트)
   - 성능 최적화
```

---

## 9. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2025-01-14 | - | 최초 작성 |
| 1.1 | 2025-01-17 | - | 배당(ASSIGNED) 기능 추가, 하위 업무 표시 기능 추가 |
| 1.2 | 2025-01-17 | - | 피드백 반영: 하위 업무 tree 모드 기본값, 최소 들여쓰기, 부모-자식 통합 페이징, 요청일 컬럼 추가, SharedItemsView 표시 방식 참조 |


● 전체 업무 기능 구현이 완료되었습니다.

  구현 완료 요약

  Backend

  1. DTO 클래스 (이전 세션에서 완료)
    - AllItemsSearchRequest.java - 검색 요청
    - AllItemsStats.java - 통계
    - AllItemsPageResponse.java - 페이지 응답
    - AllItemResponse.java - 아이템 응답
  2. Mapper/Service/Controller (이전 세션에서 완료)
    - ItemMapper.xml - UNION ALL 쿼리 (4개 출처 통합)
    - AllItemsService.java / AllItemsServiceImpl.java
    - AllItemsController.java - /api/items/all, /api/items/all/stats

  Frontend

  1. 타입 정의 - frontend/src/types/allTasks.ts
    - SourceType, ChildDisplayMode, AllItemResponse, AllItemsStats, AllItemsFilter 등
  2. API 모듈 - frontend/src/api/allTasks.ts
    - getAllItems(), getStats()
  3. 스토어 - frontend/src/stores/allTasks.ts
    - 필터링, 페이지네이션, 트리 확장/축소 관리
  4. 컴포넌트 - frontend/src/components/allTasks/
    - SourceBadge.vue - 출처 배지 (OWNED/BOARD_SHARED/ITEM_SHARED/ASSIGNED)
    - AllTasksStats.vue - 출처별/상태별/우선순위별 통계 카드
    - AllTasksFilter.vue - 필터 (키워드, 상태, 우선순위, 날짜, 하위업무 표시모드)
    - AllTasksTable.vue - 테이블 (트리/평면/접기 모드 지원)
  5. 뷰 페이지 - frontend/src/views/AllTasksView.vue
  6. 라우터/메뉴
    - frontend/src/router/index.ts - /all-tasks 라우트 추가
    - frontend/src/components/layout/Sidebar.vue - "전체 업무" 메뉴 추가

  주요 기능

  - 4가지 출처 통합 조회: 내 보드 / 공유 보드 / 공유 업무 / 배당 업무
  - 출처별 필터링: 통계 카드 클릭으로 출처 필터 전환
  - 하위 업무 표시 모드: tree(기본) / flat / collapse
  - 트리 확장/축소: 모두 확장/축소 버튼
  - 상세/필터: 키워드, 상태, 우선순위, 날짜 범위
  - 페이지네이션: 부모-자식 통합 페이징

● 전체 업무 기능이 완성되었습니다.

  전체 업무 기능 완료 요약

  구현된 기능
  ┌───────────────┬─────────────────────────────────────────────────┐
  │     구분      │                      내용                       │
  ├───────────────┼─────────────────────────────────────────────────┤
  │ 통합 조회     │ 내 보드 + 공유 보드 + 개별 공유 + 배당받은 업무 │
  ├───────────────┼─────────────────────────────────────────────────┤
  │ 출처별 필터   │ ALL, OWNED, BOARD_SHARED, ITEM_SHARED, ASSIGNED │
  ├───────────────┼─────────────────────────────────────────────────┤
  │ 통계 카드     │ 출처별 업무 건수 표시 및 클릭 필터링            │
  ├───────────────┼─────────────────────────────────────────────────┤
  │ 트리 표시     │ 하위 업무 펼침/접기 지원                        │
  ├───────────────┼─────────────────────────────────────────────────┤
  │ 슬라이드 패널 │ 아이템 클릭 시 상세 패널 열기                   │
  └───────────────┴─────────────────────────────────────────────────┘
  수정된 파일

  백엔드:
  - AllItemsSearchRequest.java - ORDER BY 쿼리 수정
  - AllItemResponse.java - completedAt 필드 제거

  프론트엔드:
  - stores/allTasks.ts - 트리 구조 변환 로직 추가
  - AllTasksTable.vue - 이중 네비게이션 제거, 출처 컬럼 조정
  - SourceBadge.vue - 아이콘만 표시 옵션 추가
  - ItemRow.vue - 하위 업무 indent 수정

  라우트

  - /all-tasks - 전체 업무 페이지