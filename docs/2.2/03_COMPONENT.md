# 3. 컴포넌트 구조

## 3.1 신규 컴포넌트

### 3.1.1 컴포넌트 목록

| 컴포넌트 | 위치 | 설명 |
|----------|------|------|
| SubTaskList.vue | components/item/ | 하위 업무 목록 |
| SubTaskForm.vue | components/item/ | 하위 업무 생성/수정 폼 |
| SubTaskRow.vue | components/item/ | 하위 업무 행 (테이블용) |
| SubTaskCard.vue | components/item/ | 하위 업무 카드 (칸반용) |
| ItemTreeView.vue | components/item/ | 업무 트리 뷰 |
| ParentInfoTooltip.vue | components/item/ | 부모 업무 정보 툴팁 |
| ChildProgressBar.vue | components/item/ | 하위 업무 진행률 표시 |

---

## 3.2 컴포넌트 상세

### 3.2.1 SubTaskList.vue

**역할**: 부모 업무의 하위 업무 목록을 표시하고 관리

**Props**
```typescript
interface Props {
  parentItemId: number      // 부모 업무 ID
  boardId: number           // 보드 ID
  parentDepth: number       // 부모 업무 깊이 (하위 생성 가능 여부 판단)
  readonly?: boolean        // 읽기 전용 모드
  collapsed?: boolean       // 접힌 상태 초기값
}
```

**Emits**
```typescript
interface Emits {
  (e: 'created', item: Item): void       // 하위 업무 생성됨
  (e: 'updated', item: Item): void       // 하위 업무 수정됨
  (e: 'deleted', itemId: number): void   // 하위 업무 삭제됨
  (e: 'reordered'): void                 // 순서 변경됨
}
```

**템플릿 구조**
```vue
<template>
  <div class="sub-task-list">
    <!-- 헤더: 접기/펼치기 + 추가 버튼 -->
    <div class="sub-task-header">
      <button @click="toggleCollapse">
        <ChevronIcon :expanded="!collapsed" />
        <span>하위 업무 ({{ children.length }})</span>
      </button>
      <button v-if="canCreateChild" @click="showCreateForm">
        + 추가
      </button>
    </div>

    <!-- 하위 업무 목록 (애니메이션) -->
    <Transition name="collapse">
      <div v-show="!collapsed" class="sub-task-items">
        <Draggable
          v-model="children"
          :disabled="readonly"
          @end="handleReorder"
        >
          <SubTaskRow
            v-for="child in children"
            :key="child.itemId"
            :item="child"
            :depth="child.itemDepth"
            @click="openDetail"
            @update="handleUpdate"
            @delete="handleDelete"
          />
        </Draggable>
      </div>
    </Transition>

    <!-- 인라인 생성 폼 -->
    <SubTaskForm
      v-if="showForm"
      :parent-item-id="parentItemId"
      :board-id="boardId"
      @save="handleCreate"
      @cancel="hideCreateForm"
    />
  </div>
</template>
```

---

### 3.2.2 SubTaskRow.vue

**역할**: 테이블/리스트 뷰에서 하위 업무 행 표시

**Props**
```typescript
interface Props {
  item: Item
  depth: number        // 업무 깊이 (indent 계산용)
  showParentInfo?: boolean  // 부모 정보 툴팁 표시
}
```

**템플릿 구조**
```vue
<template>
  <div
    class="sub-task-row"
    :style="{ paddingLeft: `${depth * 24}px` }"
  >
    <!-- 깊이 표시 라인 -->
    <div class="depth-indicator">
      <div
        v-for="i in depth"
        :key="i"
        class="depth-line"
      />
      <div class="depth-corner" />
    </div>

    <!-- 하위 업무 아이콘 -->
    <SubTaskIcon :depth="depth" />

    <!-- 체크박스 -->
    <Checkbox
      :checked="item.status === 'COMPLETED'"
      @change="toggleComplete"
    />

    <!-- 제목 -->
    <span class="item-title" @click="$emit('click')">
      {{ item.title }}
    </span>

    <!-- 부모 정보 툴팁 (배당받은 업무용) -->
    <ParentInfoTooltip
      v-if="showParentInfo && item.parentInfo"
      :parent="item.parentInfo"
      :root="item.rootInfo"
    />

    <!-- 담당자 -->
    <UserAvatar :name="item.assigneeName" size="sm" />

    <!-- 상태 -->
    <StatusBadge :status="item.status" size="sm" />

    <!-- 마감일 -->
    <span class="due-date">{{ formatDate(item.dueDate) }}</span>

    <!-- 액션 메뉴 -->
    <DropdownMenu>
      <MenuItem @click="openDetail">상세 보기</MenuItem>
      <MenuItem @click="$emit('edit')">수정</MenuItem>
      <MenuItem v-if="canCreateChild" @click="createChild">
        하위 업무 추가
      </MenuItem>
      <MenuDivider />
      <MenuItem danger @click="$emit('delete')">삭제</MenuItem>
    </DropdownMenu>
  </div>
</template>
```

**스타일**
```css
.sub-task-row {
  display: flex;
  align-items: center;
  height: 36px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.15s;
}

.sub-task-row:hover {
  background-color: #f9fafb;
}

/* 깊이 표시 라인 */
.depth-indicator {
  display: flex;
  align-items: stretch;
  height: 100%;
}

.depth-line {
  width: 24px;
  border-left: 1px solid #e5e7eb;
  margin-left: 12px;
}

.depth-corner {
  width: 12px;
  height: 50%;
  border-left: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
  border-radius: 0 0 0 4px;
  margin-left: 12px;
}
```

---

### 3.2.3 SubTaskCard.vue

**역할**: 칸반 뷰에서 하위 업무 카드 표시

**Props**
```typescript
interface Props {
  item: Item
  depth: number
  compact?: boolean      // 컴팩트 모드 (부모 카드 내부 표시)
}
```

**템플릿 구조**
```vue
<template>
  <div
    class="sub-task-card"
    :class="{
      'depth-1': depth === 1,
      'depth-2': depth === 2,
      'compact': compact
    }"
  >
    <!-- 컴팩트 모드: 부모 카드 내부에 축약 표시 -->
    <template v-if="compact">
      <div class="compact-indicator" />
      <span class="compact-title">{{ item.title }}</span>
      <StatusDot :status="item.status" />
    </template>

    <!-- 일반 모드: 독립 카드 -->
    <template v-else>
      <div class="card-header">
        <SubTaskIcon :depth="depth" />
        <span class="item-title">{{ item.title }}</span>
      </div>

      <div class="card-body">
        <UserAvatar :name="item.assigneeName" size="xs" />
        <span class="due-date">{{ formatDate(item.dueDate) }}</span>
      </div>

      <!-- 부모 정보 표시 (배당받은 업무) -->
      <div v-if="item.parentInfo" class="parent-info">
        <span class="parent-label">상위:</span>
        <span class="parent-title">{{ item.parentInfo.title }}</span>
      </div>
    </template>
  </div>
</template>
```

**스타일 (깊이별 indent)**
```css
.sub-task-card {
  margin-left: 0;
  transition: margin-left 0.2s ease;
}

.sub-task-card.depth-1 {
  margin-left: 16px;
  border-left: 3px solid #60a5fa;
}

.sub-task-card.depth-2 {
  margin-left: 32px;
  border-left: 3px solid #a78bfa;
}

/* 컴팩트 모드 */
.sub-task-card.compact {
  padding: 4px 8px;
  margin-left: 8px;
  background-color: #f9fafb;
  border-radius: 4px;
  font-size: 12px;
}
```

---

### 3.2.4 ParentInfoTooltip.vue

**역할**: 하위 업무의 부모/루트 정보를 툴팁으로 표시

**Props**
```typescript
interface Props {
  parent?: ParentInfo      // 직접 부모 정보
  root?: ParentInfo        // 최상위 업무 정보
  description?: string     // 업무 설명 (기존)
}

interface ParentInfo {
  itemId: number
  title: string
  status: string
}
```

**템플릿 구조**
```vue
<template>
  <Tooltip placement="top" :delay="300">
    <!-- 트리거 (아이콘) -->
    <template #trigger>
      <div class="parent-indicator">
        <HierarchyIcon class="w-4 h-4 text-gray-400" />
      </div>
    </template>

    <!-- 툴팁 내용 -->
    <template #content>
      <div class="parent-tooltip">
        <!-- 설명 섹션 -->
        <div v-if="description" class="tooltip-section">
          <span class="section-label">설명</span>
          <p class="section-content">{{ description }}</p>
        </div>

        <!-- 구분선 -->
        <div v-if="description && (parent || root)" class="divider" />

        <!-- 상위 업무 정보 섹션 -->
        <div v-if="parent || root" class="tooltip-section">
          <span class="section-label">상위 업무</span>

          <!-- 경로 표시 -->
          <div class="parent-path">
            <template v-if="root && root.itemId !== parent?.itemId">
              <span class="path-item root">{{ root.title }}</span>
              <ChevronRightIcon class="path-separator" />
            </template>
            <span v-if="parent" class="path-item parent">
              {{ parent.title }}
            </span>
          </div>

          <!-- 상태 표시 -->
          <div class="parent-status">
            <StatusBadge
              v-if="parent"
              :status="parent.status"
              size="xs"
            />
          </div>
        </div>
      </div>
    </template>
  </Tooltip>
</template>
```

**스타일**
```css
.parent-tooltip {
  min-width: 200px;
  max-width: 300px;
}

.tooltip-section {
  padding: 8px 0;
}

.section-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  margin-bottom: 4px;
}

.section-content {
  font-size: 13px;
  color: #374151;
  line-height: 1.4;
}

.parent-path {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.path-item {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}

.path-item.root {
  background-color: #dbeafe;
  color: #1d4ed8;
}

.path-item.parent {
  background-color: #e0e7ff;
  color: #4338ca;
}

.path-separator {
  width: 12px;
  height: 12px;
  color: #9ca3af;
}
```

---

### 3.2.5 ChildProgressBar.vue

**역할**: 기본 업무의 하위 업무 완료 진행률 표시

**Props**
```typescript
interface Props {
  childCount: number           // 전체 하위 업무 수
  completedChildCount: number  // 완료된 하위 업무 수
  showCount?: boolean          // 숫자 표시 여부
  size?: 'sm' | 'md'          // 크기
}
```

**템플릿 구조**
```vue
<template>
  <div class="child-progress" :class="`size-${size}`">
    <!-- 프로그레스 바 -->
    <div class="progress-bar">
      <div
        class="progress-fill"
        :style="{ width: `${percentage}%` }"
        :class="progressClass"
      />
    </div>

    <!-- 숫자 표시 -->
    <span v-if="showCount" class="progress-count">
      {{ completedChildCount }}/{{ childCount }}
    </span>
  </div>
</template>

<script setup lang="ts">
const percentage = computed(() => {
  if (props.childCount === 0) return 0
  return Math.round((props.completedChildCount / props.childCount) * 100)
})

const progressClass = computed(() => {
  if (percentage.value === 100) return 'complete'
  if (percentage.value >= 50) return 'half'
  return 'start'
})
</script>
```

---

## 3.3 기존 컴포넌트 수정

### 3.3.1 ItemRow.vue 수정

**추가 Props**
```typescript
// 기존 Props에 추가
hasChildren?: boolean       // 하위 업무 존재 여부
childCount?: number        // 하위 업무 수
completedChildCount?: number  // 완료된 하위 업무 수
expanded?: boolean         // 하위 업무 펼침 상태
```

**수정 사항**
- 확장/축소 토글 버튼 추가
- ChildProgressBar 표시
- 하위 업무 있을 경우 행 하단에 SubTaskList 렌더링

---

### 3.3.2 ItemCard.vue 수정

**추가 Props**
```typescript
// 기존 Props에 추가
hasChildren?: boolean
childCount?: number
completedChildCount?: number
showChildrenInline?: boolean  // 하위 업무 인라인 표시
```

**수정 사항**
- 카드 하단에 하위 업무 요약 표시
- 확장 시 하위 업무 리스트 표시
- ChildProgressBar 추가

---

### 3.3.3 ItemDetailPanel.vue 수정

**수정 사항**
- 하위 업무 섹션 추가 (SubTaskList 포함)
- 부모 업무 정보 표시 (하위 업무인 경우)
- "하위 업무 추가" 버튼 추가 (depth < 2인 경우)

---

### 3.3.4 ItemKanban.vue 수정

**수정 사항**
- 기본 업무와 하위 업무 계층 표시
- Drag&Drop 제약 추가 (하위 업무는 부모 내에서만)
- 하위 업무 indent 스타일 적용

---

### 3.3.5 ItemTable.vue 수정

**수정 사항**
- 트리 구조 렌더링 지원
- 확장/축소 토글 추가
- 깊이별 indent 표시
- Drag&Drop 제약 추가

---

## 3.4 Composables

### 3.4.1 useSubTask.ts

```typescript
import { ref, computed } from 'vue'
import { itemApi } from '@/api/item'

export function useSubTask(parentItemId: Ref<number>, boardId: Ref<number>) {
  const children = ref<Item[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 하위 업무 로드
  async function fetchChildren() {
    loading.value = true
    try {
      const response = await itemApi.getChildren(boardId.value, parentItemId.value)
      if (response.success) {
        children.value = response.data
      }
    } catch (e) {
      error.value = '하위 업무를 불러오는데 실패했습니다.'
    } finally {
      loading.value = false
    }
  }

  // 하위 업무 생성
  async function createChild(data: ItemCreateRequest): Promise<Item | null> {
    try {
      const response = await itemApi.createChild(
        boardId.value,
        parentItemId.value,
        data
      )
      if (response.success && response.data) {
        children.value.push(response.data)
        return response.data
      }
      return null
    } catch (e) {
      error.value = '하위 업무 생성에 실패했습니다.'
      return null
    }
  }

  // 순서 변경
  async function reorderChildren(orders: { itemId: number; sortOrder: number }[]) {
    try {
      await itemApi.reorderChildren(boardId.value, parentItemId.value, { orders })
      return true
    } catch (e) {
      error.value = '순서 변경에 실패했습니다.'
      return false
    }
  }

  // 하위 업무 생성 가능 여부
  const canCreateChild = computed(() => {
    const parent = /* get parent item */
    return parent && parent.itemDepth < 2
  })

  return {
    children,
    loading,
    error,
    canCreateChild,
    fetchChildren,
    createChild,
    reorderChildren
  }
}
```

---

### 3.4.2 useItemTree.ts

```typescript
export function useItemTree(boardId: Ref<number>) {
  const treeData = ref<ItemTreeNode[]>([])

  // 트리 데이터 로드
  async function fetchTree(itemId?: number) {
    const response = await itemApi.getItemTree(boardId.value, itemId)
    if (response.success) {
      treeData.value = response.data
    }
  }

  // 평면 목록을 트리로 변환
  function buildTree(items: Item[]): ItemTreeNode[] {
    const map = new Map<number, ItemTreeNode>()
    const roots: ItemTreeNode[] = []

    // 1. 맵 생성
    items.forEach(item => {
      map.set(item.itemId, { ...item, children: [] })
    })

    // 2. 부모-자식 연결
    items.forEach(item => {
      const node = map.get(item.itemId)!
      if (item.parentItemId) {
        const parent = map.get(item.parentItemId)
        if (parent) {
          parent.children.push(node)
        }
      } else {
        roots.push(node)
      }
    })

    // 3. 정렬
    const sortChildren = (nodes: ItemTreeNode[]) => {
      nodes.sort((a, b) => (a.childSortOrder ?? 0) - (b.childSortOrder ?? 0))
      nodes.forEach(node => sortChildren(node.children))
    }
    sortChildren(roots)

    return roots
  }

  return {
    treeData,
    fetchTree,
    buildTree
  }
}
```

---

## 3.5 Store 수정

### 3.5.1 item.ts Store 수정

```typescript
// 추가 State
const childrenCache = ref<Map<number, Item[]>>(new Map())

// 추가 Actions
async function fetchChildren(boardId: number, parentItemId: number) {
  const response = await itemApi.getChildren(boardId, parentItemId)
  if (response.success) {
    childrenCache.value.set(parentItemId, response.data)
    return response.data
  }
  return []
}

async function createChildItem(
  boardId: number,
  parentItemId: number,
  data: ItemCreateRequest
) {
  const response = await itemApi.createChild(boardId, parentItemId, data)
  if (response.success && response.data) {
    // 캐시 업데이트
    const cached = childrenCache.value.get(parentItemId) ?? []
    cached.push(response.data)
    childrenCache.value.set(parentItemId, cached)

    // 부모 업무 childCount 업데이트
    _updateItem(parentItemId, {
      childCount: cached.length
    })

    return response.data
  }
  return null
}

function getChildren(parentItemId: number): Item[] {
  return childrenCache.value.get(parentItemId) ?? []
}

function clearChildrenCache() {
  childrenCache.value.clear()
}
```

---

## 3.6 구현 세부사항

### 3.6.1 ItemBreadcrumb.vue 중복 표시 방지

**문제**: ancestors API가 현재 아이템을 포함하여 반환하는데, `breadcrumbItems` computed에서도 현재 아이템을 추가하여 중복 표시됨

**해결**: `loadAncestors()`에서 현재 아이템을 필터링

```typescript
// ItemBreadcrumb.vue
async function loadAncestors() {
  if (!isChildItem.value) return

  loading.value = true
  try {
    const result = await itemStore.fetchAncestors(props.boardId, props.item.itemId)
    // API가 현재 아이템도 포함하므로 제외 (showCurrent에서 별도 추가)
    ancestors.value = result.filter(a => a.itemId !== props.item.itemId)
  } finally {
    loading.value = false
  }
}
```

---

### 3.6.2 SharedItemsView 레이아웃 구조

**요구사항**: 업무 제목은 좌측 정렬, 버튼/아이콘은 우측 정렬

**레이아웃 구조**
```vue
<td class="px-4 h-12">
  <div class="flex items-center justify-between gap-2">
    <!-- 좌측: 배지 + 툴팁 + 제목 -->
    <div class="flex items-center gap-2 flex-1 min-w-0">
      <ItemBadges :item="item" size="sm" :show-owner-name="false" class="flex-shrink-0" />
      <ParentInfoTooltip
        v-if="(item.itemDepth ?? 0) > 0"
        :parent="item.parentInfo"
        :root="item.rootInfo"
        :description="item.description"
        placement="right"
        class="flex-shrink-0"
      />
      <span class="text-[13px] text-gray-900 truncate" :title="item.title">
        {{ item.title }}
      </span>
    </div>
    <!-- 우측: 열기 버튼 + 보드명 -->
    <div class="flex items-center gap-2 flex-shrink-0">
      <button class="...">열기</button>
      <span class="...">{{ item.boardName }}</span>
    </div>
  </div>
</td>
```

**CSS 클래스 설명**
| 클래스 | 역할 |
|--------|------|
| `justify-between` | 좌/우 영역 분리 |
| `flex-1 min-w-0` | 좌측 영역 확장 + truncate 지원 |
| `flex-shrink-0` | 우측 영역 축소 방지 |
| `truncate` | 제목 넘침 시 말줄임 |

---

### 3.6.3 하위 업무 깊이별 들여쓰기

**ItemRow.vue indent 계산**
```vue
<div
  class="w-[50%] px-4 h-12 flex items-center gap-2"
  :style="{
    paddingLeft: `${(item.itemDepth ?? 0) * 24 + 16}px`
  }"
>
```

| Depth | paddingLeft | 설명 |
|-------|-------------|------|
| 0 | 16px | 기본 업무 (root) |
| 1 | 40px | 1단계 하위 업무 |
| 2 | 64px | 2단계 하위 업무 (최대) |

---

## 승인 체크리스트

- [x] 신규 컴포넌트 구조 승인 ✅ 2026-01-15
- [x] 기존 컴포넌트 수정 범위 승인 ✅ 2026-01-15
- [x] Composables 설계 승인 ✅ 2026-01-15
- [x] Store 수정 사항 승인 ✅ 2026-01-15
- [x] 구현 세부사항 반영 ✅ 2026-01-16
