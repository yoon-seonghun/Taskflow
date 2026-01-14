# 업무 순서(정렬) 기능 설계서

> **상태**: 완료
> **작성일**: 2026-01-13
> **버전**: v1.0

---

## 1. 개요

### 1.1 목적
업무 리스트에 드래그 앤 드롭 기반 순서 변경 기능을 추가하여 사용자가 원하는 순서로 업무를 정렬할 수 있도록 합니다.

### 1.2 적용 범위
| 뷰 타입 | 정렬 범위 | 비고 |
|---------|----------|------|
| 테이블 뷰 | 보드 전체 기준 | 단순 순서 변경 |
| 리스트 뷰 | 보드 전체 기준 | 단순 순서 변경 |
| 칸반 뷰 | 그룹 기준별 순서 | 상태/그룹/우선순위 등 컬럼 내 순서 |

---

## 2. 데이터베이스 설계

### 2.1 TB_ITEM 컬럼 추가

| 컬럼명 | 타입 | 필수 | 기본값 | 설명 |
|--------|------|------|--------|------|
| SORT_ORDER | INT | Y | 0 | 정렬 순서 |

### 2.2 인덱스 추가

```sql
-- 보드 + 상태별 정렬 (테이블/리스트 뷰)
CREATE INDEX IDX_ITEM_BOARD_STATUS_SORT ON TB_ITEM (BOARD_ID, STATUS, SORT_ORDER);

-- 보드 + 그룹별 정렬 (칸반 뷰 - 그룹 기준)
CREATE INDEX IDX_ITEM_BOARD_GROUP_SORT ON TB_ITEM (BOARD_ID, GROUP_ID, SORT_ORDER);

-- 보드 + 우선순위별 정렬 (칸반 뷰 - 우선순위 기준)
CREATE INDEX IDX_ITEM_BOARD_PRIORITY_SORT ON TB_ITEM (BOARD_ID, PRIORITY, SORT_ORDER);
```

### 2.3 정렬 범위 정의

| 뷰 타입 | 정렬 범위 | 설명 |
|---------|----------|------|
| 테이블/리스트 | (BOARD_ID, SORT_ORDER) | 보드 전체 기준 |
| 칸반 (상태 기준) | (BOARD_ID, STATUS, SORT_ORDER) | 같은 상태 컬럼 내에서만 |
| 칸반 (그룹 기준) | (BOARD_ID, GROUP_ID, SORT_ORDER) | 같은 그룹 컬럼 내에서만 |
| 칸반 (우선순위 기준) | (BOARD_ID, PRIORITY, SORT_ORDER) | 같은 우선순위 내에서만 |

### 2.4 DDL 스크립트

```sql
-- 파일: docker/mysql/init/11_item_sort_order.sql

-- TB_ITEM에 SORT_ORDER 컬럼 추가
ALTER TABLE TB_ITEM
ADD COLUMN SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '정렬 순서' AFTER PRIORITY;

-- 기존 데이터 마이그레이션 (생성일 기준 순서 부여)
SET @row_number = 0;
UPDATE TB_ITEM
SET SORT_ORDER = (@row_number := @row_number + 1)
ORDER BY BOARD_ID, CREATED_AT;

-- 인덱스 추가
CREATE INDEX IDX_ITEM_BOARD_STATUS_SORT ON TB_ITEM (BOARD_ID, STATUS, SORT_ORDER);
CREATE INDEX IDX_ITEM_BOARD_GROUP_SORT ON TB_ITEM (BOARD_ID, GROUP_ID, SORT_ORDER);
CREATE INDEX IDX_ITEM_BOARD_PRIORITY_SORT ON TB_ITEM (BOARD_ID, PRIORITY, SORT_ORDER);
```

---

## 3. API 설계

### 3.1 순서 변경 API

```
PUT /api/boards/{boardId}/items/reorder
```

**Request Body:**
```json
{
  "items": [
    { "itemId": 1, "sortOrder": 0 },
    { "itemId": 2, "sortOrder": 1 },
    { "itemId": 3, "sortOrder": 2 }
  ],
  "groupType": "status",       // 선택: status, group, priority, assignee (칸반용)
  "groupValue": "IN_PROGRESS"  // 선택: 해당 그룹 값 (칸반용)
}
```

**Response:**
```json
{
  "success": true,
  "message": "순서가 변경되었습니다."
}
```

### 3.2 칸반 컬럼 이동 + 순서 변경 (기존 API 확장)

```
PUT /api/boards/{boardId}/items/{itemId}
```

**Request Body (확장):**
```json
{
  "status": "COMPLETED",    // 컬럼 변경
  "sortOrder": 2            // 해당 컬럼 내 순서
}
```

### 3.3 DTO 정의

#### ItemReorderRequest.java
```java
@Getter
@Setter
public class ItemReorderRequest {

    @NotEmpty(message = "순서 변경 항목이 필요합니다")
    private List<ItemSortOrder> items;

    private String groupType;   // status, group, priority, assignee
    private String groupValue;  // 그룹 값

    @Getter
    @Setter
    public static class ItemSortOrder {
        @NotNull
        private Long itemId;

        @NotNull
        @Min(0)
        private Integer sortOrder;
    }
}
```

---

## 4. 컴포넌트 구조

### 4.1 테이블/리스트 뷰

```
ItemTable.vue / ItemList.vue
├── useSortableDrag 적용
├── 드래그 핸들 (≡ 아이콘) 추가
└── onReorder 콜백 → PUT /api/boards/{boardId}/items/reorder
```

**동작 흐름:**
1. 사용자가 행을 드래그
2. 로컬 배열 재정렬 (Optimistic Update)
3. API 호출로 SORT_ORDER 일괄 업데이트
4. 실패 시 롤백

### 4.2 칸반 뷰

```
ItemKanban.vue
├── KanbanColumn.vue
│   ├── 컬럼 내 아이템 배열 (SORT_ORDER 기준 정렬)
│   ├── 드래그 오버 시 삽입 위치 표시
│   └── 드롭 시: 컬럼 변경 + 순서 변경
└── handleDrop(item, targetColumn, targetIndex)
    ├── 다른 컬럼으로 이동 → 필드 변경 + 순서 할당
    └── 같은 컬럼 내 이동 → 순서만 변경
```

**칸반 동작 흐름:**
1. 카드 드래그 시작
2. 다른 카드 위에 오버하면 삽입 위치 표시
3. 드롭 시:
   - **같은 컬럼**: 순서만 변경
   - **다른 컬럼**: 그룹 필드 변경 + 해당 컬럼 맨 뒤 또는 지정 위치에 삽입

---

## 5. 정책 결정 사항

| 항목 | 옵션 A | 옵션 B (권장) | 결정 |
|------|--------|--------------|------|
| 칸반 컬럼 이동 시 | 맨 뒤에 배치 | 드롭 위치에 배치 | 미정 |
| 신규 아이템 생성 시 | 맨 뒤에 배치 | 맨 앞에 배치 | 미정 |
| 기존 데이터 마이그레이션 | 생성일 기준 순서 부여 | - | 옵션 A |

---

## 6. 변경 파일 목록

### 6.1 백엔드

| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| `docker/mysql/init/11_item_sort_order.sql` | 신규 | DDL 스크립트 |
| `backend/.../domain/Item.java` | 수정 | sortOrder 필드 추가 |
| `backend/.../dto/item/ItemResponse.java` | 수정 | sortOrder 필드 추가 |
| `backend/.../dto/item/ItemReorderRequest.java` | 신규 | 순서 변경 요청 DTO |
| `backend/.../mapper/ItemMapper.xml` | 수정 | 순서 관련 쿼리 추가 |
| `backend/.../mapper/ItemMapper.java` | 수정 | 순서 관련 메서드 추가 |
| `backend/.../service/ItemService.java` | 수정 | reorderItems 메서드 |
| `backend/.../service/impl/ItemServiceImpl.java` | 수정 | reorderItems 구현 |
| `backend/.../controller/ItemController.java` | 수정 | reorder 엔드포인트 |

### 6.2 프론트엔드

| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| `frontend/src/api/item.ts` | 수정 | reorderItems API 함수 |
| `frontend/src/types/item.ts` | 수정 | sortOrder 타입 추가 |
| `frontend/src/components/item/ItemTable.vue` | 수정 | 드래그 앤 드롭 적용 |
| `frontend/src/components/item/ItemRow.vue` | 수정 | 드래그 핸들 추가 |
| `frontend/src/components/item/ItemList.vue` | 수정 | 드래그 앤 드롭 적용 |
| `frontend/src/components/item/ItemListRow.vue` | 수정 | 드래그 핸들 추가 |
| `frontend/src/components/item/ItemKanban.vue` | 수정 | 순서 변경 로직 |
| `frontend/src/components/item/KanbanColumn.vue` | 수정 | 컬럼 내 순서 변경 |
| `frontend/src/components/item/ItemCard.vue` | 수정 | 드래그 이벤트 확장 |

---

## 7. 구현 체크리스트

### Phase 1: 데이터베이스
- [ ] TB_ITEM 테이블에 SORT_ORDER 컬럼 추가
- [ ] 인덱스 추가
- [ ] 기존 데이터 마이그레이션

### Phase 2: 백엔드 - MyBatis
- [ ] getMaxSortOrder 쿼리 구현
- [ ] updateSortOrder 쿼리 구현
- [ ] batchUpdateSortOrder 쿼리 구현
- [ ] 기존 조회 쿼리 SORT_ORDER 기준 정렬로 변경

### Phase 3: 백엔드 - Java
- [ ] Item 도메인에 sortOrder 필드 추가
- [ ] ItemResponse에 sortOrder 필드 추가
- [ ] ItemReorderRequest DTO 생성
- [ ] ItemService.reorderItems 메서드 구현
- [ ] ItemController.reorderItems 엔드포인트 추가

### Phase 4: 프론트엔드 - API
- [ ] item.ts에 reorderItems 함수 추가
- [ ] item.ts 타입에 sortOrder 추가

### Phase 5: 프론트엔드 - 테이블/리스트 뷰
- [ ] ItemRow에 드래그 핸들 UI 추가
- [ ] ItemTable에 useSortableDrag 적용
- [ ] ItemListRow에 드래그 핸들 UI 추가
- [ ] ItemList에 useSortableDrag 적용

### Phase 6: 프론트엔드 - 칸반 뷰
- [ ] KanbanColumn 내 순서 변경 로직 구현
- [ ] ItemKanban handleDrop 확장
- [ ] 컬럼 변경 + 순서 변경 통합 처리

### Phase 7: 테스트
- [ ] 테이블 뷰 드래그 순서 변경 테스트
- [ ] 리스트 뷰 드래그 순서 변경 테스트
- [ ] 칸반 뷰 같은 컬럼 내 순서 변경 테스트
- [ ] 칸반 뷰 컬럼 변경 + 순서 변경 테스트
- [ ] DB SORT_ORDER 값 일관성 검증

---

## 8. 주의사항

1. **GROUP_ID가 NULL일 수 있음** - 정렬 범위 정의 시 고려 필요
2. **상태 변경 시 순서 정책** - 맨 뒤 배치 vs 순서 유지 결정 필요
3. **일괄 순서 변경 트랜잭션** - 무결성 보장 필수
4. **기존 데이터 마이그레이션** - 생성일 기준 초기 순서 부여

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 내용 |
|------|------|--------|------|
| v1.0 | 2026-01-13 | Claude | 최초 작성, 보류 상태 |
