# 5. Drag & Drop 영향 분석 및 수정 설계

## 5.1 현재 구현 분석

### 5.1.1 핵심 Composables

| Composable | 위치 | 용도 | 사용처 |
|------------|------|------|--------|
| `useSortableDrag` | composables/useSortableDrag.ts | 인덱스 기반 정렬 | ItemTable, ItemList |
| `useGroupedSortableDrag` | composables/useSortableDrag.ts | 그룹 내 정렬 | PropertiesContent |
| `useItemDrag` | composables/useSortableDrag.ts | 아이템 ID 기반 정렬 | BoardsView, GroupList |

### 5.1.2 현재 D&D 사용 컴포넌트

| 컴포넌트 | 파일 | D&D 방식 | 역할 |
|----------|------|----------|------|
| ItemTable.vue | components/item/ItemTable.vue | useSortableDrag | 테이블 행 순서 변경 |
| ItemList.vue | components/item/ItemList.vue | useSortableDrag | 리스트 행 순서 변경 |
| ItemRow.vue | components/item/ItemRow.vue | 이벤트 emit | 개별 행 드래그 핸들링 |
| ItemCard.vue | components/item/ItemCard.vue | 이벤트 emit | 개별 카드 드래그 핸들링 |
| ItemListRow.vue | components/item/ItemListRow.vue | 이벤트 emit | 리스트 행 드래그 핸들링 |
| ItemKanban.vue | views/ItemKanban.vue | 자체 구현 | 칸반 전체 D&D 관리 |
| KanbanColumn.vue | components/item/KanbanColumn.vue | 자체 구현 | 컬럼 내 카드 D&D |

### 5.1.3 현재 useSortableDrag 핵심 인터페이스

```typescript
export interface SortableDragOptions<T> {
  items: Ref<T[]>
  itemKey?: keyof T
  canDrag?: (item: T, index: number) => boolean       // 드래그 허용 여부
  canDrop?: (draggedItem: T, targetItem: T, draggedIndex: number, targetIndex: number) => boolean  // 드롭 허용 여부
  onReorder?: (reorderedItems: T[], fromIndex: number, toIndex: number) => void | Promise<void>
  onDragStart?: (item: T, index: number) => void
  onDragEnd?: () => void
}
```

---

## 5.2 하위 업무 D&D 제약 규칙

### 5.2.1 이동 제약 규칙

| 드래그 대상 | 드롭 허용 위치 | 드롭 불가 위치 |
|-------------|---------------|---------------|
| 기본 업무 (depth 0) | 다른 기본 업무 사이 | 하위 업무 사이 (중간 삽입 불가) |
| 1차 하위 (depth 1) | **같은 부모 내** 다른 하위 사이 | 다른 부모의 하위 사이, 기본 업무 사이 |
| 2차 하위 (depth 2) | **같은 부모 내** 다른 하위 사이 | 다른 부모의 하위 사이, 상위 레벨 |

### 5.2.2 제약 조건 상세

```typescript
// 하위 업무 D&D 제약 인터페이스
interface SubTaskDragConstraints {
  // 1. 하위 업무는 부모가 같아야 이동 가능
  sameParentOnly: boolean  // true

  // 2. 기본 업무는 하위 업무 중간으로 삽입 불가
  preventRootInsertBetweenChildren: boolean  // true

  // 3. 하위 업무를 다른 부모로 이동 불가
  preventCrossParentMove: boolean  // true

  // 4. 하위 업무를 기본 업무로 승격 불가 (D&D로는)
  preventPromoteToRoot: boolean  // true
}
```

### 5.2.3 canDrop 함수 로직

```typescript
function canDropSubTask(
  draggedItem: Item,
  targetItem: Item,
  draggedIndex: number,
  targetIndex: number
): boolean {
  // 1. 같은 아이템은 무시
  if (draggedItem.itemId === targetItem.itemId) return false

  // 2. 기본 업무 (depth 0)
  if (draggedItem.itemDepth === 0) {
    // 기본 업무는 다른 기본 업무 사이로만 이동 가능
    if (targetItem.itemDepth !== 0) {
      showToast('기본 업무는 다른 기본 업무 사이로만 이동할 수 있습니다', 'warning')
      return false
    }
    return true
  }

  // 3. 하위 업무 (depth 1 또는 2)
  if (draggedItem.parentItemId !== targetItem.parentItemId) {
    showToast('하위 업무는 같은 부모 내에서만 순서를 변경할 수 있습니다', 'warning')
    return false
  }

  return true
}
```

---

## 5.3 컴포넌트별 수정 영향 분석

### 5.3.1 ItemTable.vue 수정

**현재 상태:**
- `useSortableDrag`로 전체 아이템 배열 관리
- 평면 목록 (flat list) 기준 정렬

**수정 필요 사항:**

| 항목 | 현재 | 변경 |
|------|------|------|
| 데이터 구조 | 평면 목록 | 트리 구조 (부모-자식 관계) |
| canDrop 로직 | 없음 | 하위 업무 제약 추가 |
| 정렬 대상 | 전체 items | depth별 분리 (root vs children) |
| 시각적 피드백 | 기본 drag-over | 불가 영역 표시 추가 |

**수정 코드 예시:**

```typescript
// ItemTable.vue 내 useSortableDrag 설정
const { /* ... */ } = useSortableDrag({
  items: flattenedItems,
  itemKey: 'itemId',
  canDrop: (dragged, target, fromIdx, toIdx) => {
    // 같은 아이템 무시
    if (dragged.itemId === target.itemId) return false

    // 기본 업무는 기본 업무 사이로만
    if (dragged.itemDepth === 0) {
      return target.itemDepth === 0
    }

    // 하위 업무는 같은 부모 내에서만
    return dragged.parentItemId === target.parentItemId
  },
  onReorder: async (items, fromIndex, toIndex) => {
    const movedItem = items[toIndex]

    if (movedItem.itemDepth === 0) {
      // 기본 업무 순서 변경 API
      await itemStore.reorderRootItem(movedItem.itemId, toIndex)
    } else {
      // 하위 업무 순서 변경 API
      await itemStore.reorderChildItem(movedItem.parentItemId!, movedItem.itemId, toIndex)
    }
  }
})
```

### 5.3.2 ItemList.vue 수정

**수정 필요 사항:**

| 항목 | 현재 | 변경 |
|------|------|------|
| canDrop | 없음 | 동일 제약 적용 |
| 트리 렌더링 | 평면 | 계층 indent 적용 |
| 확장/축소 | 없음 | 하위 업무 토글 추가 |

### 5.3.3 ItemKanban.vue / KanbanColumn.vue 수정

**현재 상태:**
- 컬럼 간 카드 이동 (상태 변경)
- 컬럼 내 카드 순서 변경

**수정 필요 사항:**

| 항목 | 현재 | 변경 |
|------|------|------|
| 카드 이동 | 자유 이동 | 하위 업무 제약 적용 |
| 컬럼 간 이동 | 허용 | 기본 업무만 허용, 하위 업무 제한 |
| 시각적 피드백 | 기본 | 이동 불가 표시 추가 |

**칸반 특수 규칙:**

```typescript
// KanbanColumn.vue 드롭 핸들러 수정
function handleDrop(targetColumn: string, event: DragEvent) {
  const draggedItem = getDraggedItem()

  // 하위 업무는 상태 변경만 가능, 컬럼 내 순서 변경은 같은 부모 내에서만
  if (draggedItem.itemDepth > 0) {
    // 컬럼 간 이동 = 상태 변경 (허용)
    if (targetColumn !== draggedItem.status) {
      updateItemStatus(draggedItem.itemId, targetColumn)
      return
    }

    // 같은 컬럼 내 순서 변경 = 같은 부모 내에서만
    // canDrop 체크 필요
  }
}
```

### 5.3.4 ItemRow.vue / ItemCard.vue 수정

**추가 사항:**

| 항목 | 설명 |
|------|------|
| draggable 속성 | depth 기반 조건부 적용 |
| 드래그 핸들 | 하위 업무는 부모 내에서만 표시 |
| 시각적 피드백 | 이동 불가 시 커서 변경 |

---

## 5.4 시각적 피드백 설계

### 5.4.1 드래그 상태별 스타일

```css
/* 드래그 중인 아이템 */
.sortable-dragging {
  opacity: 0.5;
  background-color: #e0f2fe;
}

/* 드롭 가능 영역 */
.sortable-drag-over.can-drop {
  border-top: 2px solid #3B82F6;
  background-color: #eff6ff;
}

/* 드롭 불가 영역 */
.sortable-drag-over.cannot-drop {
  border-top: 2px solid #ef4444;
  background-color: #fef2f2;
  cursor: not-allowed;
}

/* 하위 업무 드래그 시 부모 영역 하이라이트 */
.parent-highlight {
  background-color: #f0f9ff;
  box-shadow: inset 0 0 0 2px #60a5fa;
}
```

### 5.4.2 드래그 불가 토스트 메시지

| 상황 | 메시지 |
|------|--------|
| 기본 업무를 하위 사이로 | "기본 업무는 다른 기본 업무 사이로만 이동할 수 있습니다" |
| 하위 업무를 다른 부모로 | "하위 업무는 같은 부모 내에서만 순서를 변경할 수 있습니다" |
| depth 2에서 추가 생성 | "최대 3단계까지만 하위 업무를 생성할 수 있습니다" |

---

## 5.5 API 연동

### 5.5.1 순서 변경 API

**기존 API (기본 업무 순서):**
```
PUT /api/boards/{boardId}/items/{itemId}/order
Body: { sortOrder: number }
```

**신규 API (하위 업무 순서):**
```
PUT /api/items/{itemId}/children/reorder
Body: { childItemId: number, newOrder: number }
```

### 5.5.2 Store 메서드 추가

```typescript
// stores/item.ts 추가 메서드
interface ItemStore {
  // 기존
  reorderItem(itemId: number, newOrder: number): Promise<void>

  // 신규
  reorderChildItem(parentId: number, childId: number, newOrder: number): Promise<void>
}
```

---

## 5.6 수정 대상 파일 목록

### 5.6.1 Composables

| 파일 | 수정 내용 | 우선순위 |
|------|----------|---------|
| `composables/useSortableDrag.ts` | canDrop 로직 보강, 불가 피드백 추가 | 높음 |
| `composables/useSubTask.ts` (신규) | 하위 업무 D&D 전용 훅 | 중간 |

### 5.6.2 Components

| 파일 | 수정 내용 | 우선순위 |
|------|----------|---------|
| `components/item/ItemTable.vue` | canDrop 적용, 트리 렌더링 | 높음 |
| `components/item/ItemList.vue` | canDrop 적용, 트리 렌더링 | 높음 |
| `components/item/ItemRow.vue` | 계층 표시, 드래그 제약 | 중간 |
| `components/item/ItemCard.vue` | 계층 표시, 드래그 제약 | 중간 |
| `components/item/ItemKanban.vue` | 하위 업무 D&D 제약 | 높음 |
| `components/item/KanbanColumn.vue` | 컬럼 내 정렬 제약 | 높음 |
| `components/item/SubTaskRow.vue` (신규) | 하위 업무 전용 행 | 중간 |
| `components/item/SubTaskCard.vue` (신규) | 하위 업무 전용 카드 | 중간 |

### 5.6.3 Stores

| 파일 | 수정 내용 | 우선순위 |
|------|----------|---------|
| `stores/item.ts` | reorderChildItem 액션 추가 | 높음 |

---

## 5.7 구현 순서

| 단계 | 작업 | 의존성 |
|------|------|--------|
| 1 | useSortableDrag.ts에 하위 업무 제약 유틸 추가 | 없음 |
| 2 | ItemTable.vue에 canDrop 적용 | 1 |
| 3 | ItemList.vue에 canDrop 적용 | 1 |
| 4 | ItemKanban/KanbanColumn에 제약 적용 | 1 |
| 5 | 시각적 피드백 CSS 추가 | 2, 3, 4 |
| 6 | 토스트 메시지 연동 | 5 |
| 7 | 전체 테스트 및 검증 | 6 |

---

## 5.8 테스트 시나리오

### 5.8.1 테이블 뷰 테스트

| 시나리오 | 기대 결과 |
|----------|----------|
| 기본 업무 A를 기본 업무 B 아래로 드래그 | 순서 변경 성공 |
| 기본 업무 A를 하위 업무 사이로 드래그 | 드롭 불가, 경고 메시지 |
| 하위 업무 A-1을 같은 부모 내 A-2 아래로 | 순서 변경 성공 |
| 하위 업무 A-1을 다른 부모 B의 하위로 | 드롭 불가, 경고 메시지 |
| depth 2 업무를 depth 1 위치로 | 드롭 불가, 경고 메시지 |

### 5.8.2 칸반 뷰 테스트

| 시나리오 | 기대 결과 |
|----------|----------|
| 기본 업무를 다른 컬럼으로 | 상태 변경 성공 |
| 하위 업무를 다른 컬럼으로 | 상태 변경 성공 (위치 유지) |
| 같은 컬럼 내 같은 부모 하위 순서 변경 | 순서 변경 성공 |
| 같은 컬럼 내 다른 부모 하위로 이동 | 드롭 불가, 경고 메시지 |

### 5.8.3 리스트 뷰 테스트

| 시나리오 | 기대 결과 |
|----------|----------|
| 테이블 뷰와 동일한 제약 적용 | 동일 |

---

## 승인 체크리스트

- [x] D&D 제약 규칙 승인 ✅ 2026-01-15
- [x] 시각적 피드백 설계 승인 ✅ 2026-01-15
- [x] 컴포넌트별 수정 범위 승인 ✅ 2026-01-15
- [x] API 연동 방안 승인 ✅ 2026-01-15
- [x] 구현 순서 승인 ✅ 2026-01-15
