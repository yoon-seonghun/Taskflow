# 4. UI/UX 설계

## 4.1 계층 표현 원칙

### 4.1.1 기본 원칙

| 원칙 | 설명 |
|------|------|
| 과도한 indent 지양 | 깊이별 indent를 최소화하여 공간 효율성 유지 |
| 시각적 구분 | 색상, 아이콘, 라인으로 계층 구분 |
| 접기/펼치기 | 하위 업무 숨김/표시로 화면 활용 |
| 애니메이션 | 부드러운 전환으로 UX 개선 |

### 4.1.2 깊이별 스타일

| Depth | Indent | 왼쪽 테두리 | 아이콘 |
|-------|--------|------------|--------|
| 0 (기본) | 0px | 없음 | 없음 |
| 1 (1차 하위) | 16px | 파란색 3px | └ 모양 |
| 2 (2차 하위) | 32px | 보라색 3px | └ 모양 |

---

## 4.2 테이블 뷰 설계

### 4.2.1 기본 구조

```
┌─────────────────────────────────────────────────────────────────────────┐
│ □  ▼  웹사이트 리뉴얼 프로젝트          김팀장  진행중  01/20  ■■■□ 2/3 │
├─────────────────────────────────────────────────────────────────────────┤
│    │  □  └ 디자인 작업                  김디자인 진행중  01/18  ■■□□ 1/2 │
│    │     │  □  └ 메인 페이지 디자인     박주니어 시작전  01/15           │
│    │     │  □  └ 서브 페이지 디자인     박주니어 시작전  01/17           │
│    │  □  └ 퍼블리싱 작업                이퍼블  시작전  01/19           │
│    │  □  └ 백엔드 개발                  최개발  진행중  01/20           │
├─────────────────────────────────────────────────────────────────────────┤
│ □     신규 기능 추가                     최개발  시작전  01/25           │
└─────────────────────────────────────────────────────────────────────────┘

범례:
▼ : 펼침/접힘 토글 버튼
└ : 하위 업무 표시
│ : 계층 연결선
■■■□ 2/3 : 하위 업무 진행률 (완료 2/전체 3)
```

### 4.2.2 행 스타일 상세

```css
/* 기본 업무 (depth 0) */
.item-row.depth-0 {
  font-weight: 500;
  background-color: #ffffff;
}

/* 1차 하위 업무 (depth 1) */
.item-row.depth-1 {
  padding-left: 24px;
  background-color: #f9fafb;
  border-left: 3px solid #60a5fa;
  margin-left: 8px;
}

/* 2차 하위 업무 (depth 2) */
.item-row.depth-2 {
  padding-left: 48px;
  background-color: #f3f4f6;
  border-left: 3px solid #a78bfa;
  margin-left: 8px;
}
```

### 4.2.3 펼침/접힘 애니메이션

```css
/* 하위 업무 영역 애니메이션 */
.children-container {
  overflow: hidden;
  transition: max-height 0.3s ease-out, opacity 0.2s ease-out;
}

.children-container.collapsed {
  max-height: 0;
  opacity: 0;
}

.children-container.expanded {
  max-height: 2000px;  /* 충분히 큰 값 */
  opacity: 1;
}

/* 토글 버튼 회전 */
.toggle-icon {
  transition: transform 0.2s ease;
}

.toggle-icon.expanded {
  transform: rotate(90deg);
}
```

### 4.2.4 계층 연결선 구현

```vue
<template>
  <div class="tree-line-container">
    <!-- 세로선 (부모와 연결) -->
    <div
      v-for="i in depth"
      :key="i"
      class="vertical-line"
      :style="{ left: `${(i - 1) * 24 + 12}px` }"
    />

    <!-- 꺾인선 (현재 노드 연결) -->
    <div
      class="corner-line"
      :style="{ left: `${(depth - 1) * 24 + 12}px` }"
    />
  </div>
</template>

<style scoped>
.tree-line-container {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  pointer-events: none;
}

.vertical-line {
  position: absolute;
  top: 0;
  width: 1px;
  height: 100%;
  background-color: #e5e7eb;
}

.corner-line {
  position: absolute;
  top: 0;
  width: 12px;
  height: 50%;
  border-left: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
  border-radius: 0 0 0 4px;
}
</style>
```

---

## 4.3 칸반 뷰 설계

### 4.3.1 기본 구조

```
┌─ 시작전 (3) ──────┐  ┌─ 진행중 (2) ──────┐  ┌─ 완료 (1) ────────┐
│                   │  │                   │  │                   │
│ ┌───────────────┐ │  │ ┌───────────────┐ │  │ ┌───────────────┐ │
│ │ 웹사이트 리뉴얼│ │  │ │ 신규 기능     │ │  │ │ 버그 수정     │ │
│ │ 김팀장  01/20 │ │  │ │ 이개발  01/25 │ │  │ │ 박개발  완료  │ │
│ │  ■■□ 2/3     │ │  │ └───────────────┘ │  │ └───────────────┘ │
│ │ ┌───────────┐ │ │  │                   │  │                   │
│ │ │└ 디자인   │ │ │  │ ┌───────────────┐ │  │                   │
│ │ │  김디자인 │ │ │  │ │ └ DB 설계     │ │  │                   │
│ │ └───────────┘ │ │  │ │   최DBA       │ │  │                   │
│ │ ┌───────────┐ │ │  │ └───────────────┘ │  │                   │
│ │ │└ 퍼블리싱 │ │ │  │                   │  │                   │
│ │ │  이퍼블   │ │ │  │                   │  │                   │
│ │ └───────────┘ │ │  │                   │  │                   │
│ └───────────────┘ │  │                   │  │                   │
│                   │  │                   │  │                   │
└───────────────────┘  └───────────────────┘  └───────────────────┘
```

### 4.3.2 카드 표시 옵션

| 옵션 | 설명 |
|------|------|
| 인라인 표시 | 부모 카드 내에 하위 업무 축약 표시 |
| 개별 카드 | 하위 업무를 별도 카드로 표시 (indent 적용) |
| 접힘 표시 | 기본 접힘, 클릭 시 펼침 |

### 4.3.3 인라인 표시 모드

```vue
<template>
  <div class="kanban-card parent-card">
    <!-- 카드 헤더 -->
    <div class="card-header">
      <span class="card-title">{{ item.title }}</span>
      <ChildProgressBar
        :child-count="item.childCount"
        :completed-child-count="item.completedChildCount"
      />
    </div>

    <!-- 카드 바디 -->
    <div class="card-body">
      <UserAvatar :name="item.assigneeName" />
      <span class="due-date">{{ item.dueDate }}</span>
    </div>

    <!-- 하위 업무 인라인 (펼침 상태일 때) -->
    <Transition name="slide-down">
      <div v-if="expanded" class="inline-children">
        <div
          v-for="child in children"
          :key="child.itemId"
          class="inline-child-item"
          :class="`depth-${child.itemDepth}`"
        >
          <SubTaskIcon :depth="child.itemDepth" size="sm" />
          <Checkbox
            :checked="child.status === 'COMPLETED'"
            @change="toggleChildComplete(child)"
          />
          <span class="child-title">{{ child.title }}</span>
          <StatusDot :status="child.status" />
        </div>
      </div>
    </Transition>

    <!-- 펼침/접힘 버튼 -->
    <button
      v-if="item.hasChildren"
      class="expand-toggle"
      @click="toggleExpand"
    >
      <ChevronIcon :direction="expanded ? 'up' : 'down'" />
      <span>{{ item.childCount }}개 하위 업무</span>
    </button>
  </div>
</template>
```

### 4.3.4 개별 카드 모드

```vue
<template>
  <div class="kanban-column">
    <template v-for="item in columnItems" :key="item.itemId">
      <!-- 기본 업무 카드 -->
      <div
        class="kanban-card"
        :class="{ 'has-children': item.hasChildren }"
      >
        <CardContent :item="item" />
      </div>

      <!-- 하위 업무 카드 (깊이별 indent) -->
      <template v-if="item.hasChildren && isExpanded(item.itemId)">
        <div
          v-for="child in getChildren(item.itemId)"
          :key="child.itemId"
          class="kanban-card child-card"
          :style="{ marginLeft: `${child.itemDepth * 16}px` }"
        >
          <div class="depth-indicator" :class="`depth-${child.itemDepth}`" />
          <CardContent :item="child" compact />
        </div>
      </template>
    </template>
  </div>
</template>
```

### 4.3.5 Drag&Drop 제약

```typescript
// useSortableDrag.ts 수정

function handleDragStart(event: DragEvent, item: Item) {
  // 하위 업무인 경우 플래그 설정
  dragState.isChildItem = item.itemDepth > 0
  dragState.parentItemId = item.parentItemId
}

function handleDragOver(event: DragEvent, targetItem: Item) {
  // 하위 업무는 부모 노드 외부로 이동 불가
  if (dragState.isChildItem) {
    // 같은 부모 내에서만 이동 허용
    if (targetItem.parentItemId !== dragState.parentItemId) {
      event.preventDefault()
      showNotAllowedCursor()
      return false
    }
  }

  // 기본 업무도 하위 업무 중간으로 삽입 불가
  if (!dragState.isChildItem && targetItem.itemDepth > 0) {
    event.preventDefault()
    return false
  }
}

function handleDrop(event: DragEvent, targetItem: Item) {
  // 유효성 검증
  if (dragState.isChildItem && targetItem.parentItemId !== dragState.parentItemId) {
    toast.warning('하위 업무는 부모 업무 내에서만 이동할 수 있습니다.')
    return false
  }
}
```

---

## 4.4 상세 패널 설계

### 4.4.1 기본 레이아웃 (기존 3열 구조 유지)

기존 ItemDetailPanel의 3열 레이아웃을 유지하며, 하위 업무 섹션은 중앙 에디터 영역 위에 펼침/접힘 가능한 형태로 추가합니다.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  업무 상세                                              [이관] [공유] [삭제] X │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┬──────────────────────────────┬─────────────────────┐   │
│  │    속성 패널     │         에디터 영역           │     댓글 패널       │   │
│  │    (Left)       │         (Center)             │     (Right)        │   │
│  │                 │                              │                    │   │
│  │ 상태            │ ┌─ 하위 업무 (3) ─── [▼ 접기]┐│ 댓글 입력          │   │
│  │ [진행중 ▼]      │ │ □ └ 디자인 작업   ■■□ 1/2 ││ ┌────────────────┐ │   │
│  │                 │ │ □ └ 퍼블리싱     시작전    ││ │                │ │   │
│  │ 우선순위        │ │ □ └ 백엔드 개발   진행중   ││ │                │ │   │
│  │ [높음 ▼]        │ │           [+ 하위 업무 추가]││ └────────────────┘ │   │
│  │                 │ └──────────────────────────┘│ [등록]             │   │
│  │ 담당자          │                              │                    │   │
│  │ [김팀장 ▼]      │ 제목                         │ ─────────────────  │   │
│  │                 │ ┌────────────────────────┐  │                    │   │
│  │ 요청일          │ │웹사이트 리뉴얼 프로젝트  │  │ 김팀장  01/10 14:30│   │
│  │ [2026-01-10]    │ └────────────────────────┘  │ 디자인 검토 부탁.. │   │
│  │                 │                              │                    │   │
│  │ 마감일          │ ┌─ Rich Text Editor ──────┐  │ ─────────────────  │   │
│  │ [2026-01-20]    │ │                          │  │                    │   │
│  │                 │ │ 회사 웹사이트 전면       │  │ 이개발  01/11 09:00│   │
│  │ 카테고리        │ │ 리뉴얼 프로젝트입니다.   │  │ 확인했습니다.      │   │
│  │ [개발 ▼]        │ │                          │  │                    │   │
│  │                 │ │ [B] [I] [U] [Link] ...   │  │                    │   │
│  │ 그룹            │ │                          │  │                    │   │
│  │ [프로젝트A ▼]   │ └──────────────────────────┘  │                    │   │
│  │                 │                              │                    │   │
│  └─────────────────┴──────────────────────────────┴─────────────────────┘   │
│                                                                              │
│  수정: 김팀장 · 2026-01-15 14:30                                             │
└──────────────────────────────────────────────────────────────────────────────┘

레이아웃:
- 좌측 패널 (250px): 속성 편집 영역
- 중앙 패널 (flex-1): 하위 업무 + 제목 + Rich Text Editor
- 우측 패널 (280px): 댓글 영역
```

### 4.4.2 하위 업무 섹션 (펼침/접힘)

중앙 에디터 영역 상단에 위치하며, 펼침/접힘이 가능한 형태입니다.

```
┌─ 하위 업무 (3/5) ────────────────────────────────── [▼ 접기] [+ 추가] ─┐
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐  │
│  │ □ └ 디자인 작업               김디자인   진행중  01/18  ■■□ 1/2 │  │
│  │    □ └ 메인 페이지 디자인     박주니어   시작전  01/15          │  │
│  │    □ └ 서브 페이지 디자인     박주니어   시작전  01/17          │  │
│  │ □ └ 퍼블리싱 작업             이퍼블     시작전  01/19          │  │
│  │ □ └ 백엔드 개발               최개발     진행중  01/20          │  │
│  └─────────────────────────────────────────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘

[접힌 상태]
┌─ 하위 업무 (3/5) ────────────────────────────────── [▶ 펼치기] [+ 추가] ─┐
│  진행률: ■■■□□ 3/5 완료                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.4.3 하위 업무 섹션 컴포넌트

```vue
<template>
  <div class="subtask-section">
    <!-- 헤더 -->
    <div class="subtask-header" @click="toggleExpand">
      <div class="header-left">
        <ChevronIcon :direction="expanded ? 'down' : 'right'" />
        <span class="section-title">
          하위 업무 ({{ completedCount }}/{{ totalCount }})
        </span>
      </div>
      <div class="header-right">
        <ChildProgressBar
          v-if="!expanded"
          :completed="completedCount"
          :total="totalCount"
          compact
        />
        <button @click.stop="toggleExpand" class="toggle-btn">
          {{ expanded ? '접기' : '펼치기' }}
        </button>
        <button
          v-if="canCreateChild"
          @click.stop="showAddForm = true"
          class="add-btn"
        >
          + 추가
        </button>
      </div>
    </div>

    <!-- 하위 업무 목록 (펼침 시) -->
    <Transition name="slide-fade">
      <div v-if="expanded" class="subtask-content">
        <!-- 하위 업무 리스트 -->
        <SubTaskList
          :items="children"
          :parent-item-id="itemId"
          @click="onChildClick"
          @complete="onChildComplete"
        />

        <!-- 빠른 추가 폼 -->
        <QuickAddForm
          v-if="showAddForm"
          @submit="createChild"
          @cancel="showAddForm = false"
          placeholder="하위 업무명 입력..."
        />
      </div>
    </Transition>

    <!-- 접힌 상태일 때 진행률 표시 -->
    <div v-if="!expanded && totalCount > 0" class="collapsed-summary">
      <ChildProgressBar
        :completed="completedCount"
        :total="totalCount"
        show-label
      />
    </div>
  </div>
</template>

<style scoped>
.subtask-section {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-bottom: 16px;
  overflow: hidden;
}

.subtask-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: #f9fafb;
  cursor: pointer;
  user-select: none;
}

.subtask-header:hover {
  background-color: #f3f4f6;
}

.subtask-content {
  padding: 12px 16px;
  border-top: 1px solid #e5e7eb;
}

.collapsed-summary {
  padding: 8px 16px;
  background-color: #fafafa;
}

/* 펼침/접힘 애니메이션 */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.2s ease-in;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  max-height: 0;
  opacity: 0;
  padding-top: 0;
  padding-bottom: 0;
}
</style>
```

### 4.4.4 하위 업무 상세 패널 (배당받은 경우)

하위 업무를 배당받은 사용자가 볼 때의 상세 패널입니다.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│  업무 상세                                              [공유] [배당] [삭제] X │
├──────────────────────────────────────────────────────────────────────────────┤
│  ┌─ 상위 업무 경로 (Breadcrumb) ───────────────────────────────────────────┐ │
│  │ 📁 웹사이트 리뉴얼 프로젝트  >  📁 디자인 작업  >  현재 업무            │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌─────────────────┬──────────────────────────────┬─────────────────────┐   │
│  │    속성 패널     │         에디터 영역           │     댓글 패널       │   │
│  │    (Left)       │         (Center)             │     (Right)        │   │
│  │                 │                              │                    │   │
│  │ 상태            │ 제목                         │ 댓글 입력          │   │
│  │ [시작전 ▼]      │ ┌────────────────────────┐  │ ┌────────────────┐ │   │
│  │                 │ │메인 페이지 디자인       │  │ │                │ │   │
│  │ 우선순위        │ └────────────────────────┘  │ └────────────────┘ │   │
│  │ [높음 ▼]        │                              │ [등록]             │   │
│  │                 │ ┌─ Rich Text Editor ──────┐  │                    │   │
│  │ 담당자          │ │                          │  │ ─────────────────  │   │
│  │ [박주니어 ▼]    │ │ 메인 페이지 UI/UX       │  │                    │   │
│  │                 │ │ 디자인 작업입니다.      │  │ 김팀장  01/10 14:30│   │
│  │ 마감일          │ │                          │  │ 메인은 심플하게..  │   │
│  │ [2026-01-15]    │ └──────────────────────────┘  │                    │   │
│  │                 │                              │                    │   │
│  │ ─────────────   │                              │                    │   │
│  │ ⚠️ 제약 사항    │                              │                    │   │
│  │ · 보드 이동 불가│                              │                    │   │
│  │ · 이관 불가     │                              │                    │   │
│  │                 │                              │                    │   │
│  │ ─────────────   │                              │                    │   │
│  │ 📬 배당 정보    │                              │                    │   │
│  │ 배당자: 김팀장  │                              │                    │   │
│  │ 01/10 14:30     │                              │                    │   │
│  │                 │                              │                    │   │
│  └─────────────────┴──────────────────────────────┴─────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────┘

특징:
- 상단에 Breadcrumb으로 상위 업무 경로 표시
- 속성 패널 하단에 제약 사항 및 배당 정보 표시
- 하위 업무 섹션 없음 (depth 2이거나 자신의 하위 업무가 없는 경우)
- 댓글 패널은 동일하게 유지
```

### 4.4.5 Breadcrumb 컴포넌트 (상위 경로)

```vue
<template>
  <div class="breadcrumb-container">
    <nav class="breadcrumb">
      <template v-for="(ancestor, index) in ancestors" :key="ancestor.itemId">
        <span
          class="breadcrumb-item"
          :class="{ 'is-current': index === ancestors.length - 1 }"
          @click="navigateToAncestor(ancestor)"
        >
          <FolderIcon v-if="ancestor.itemDepth < currentDepth" />
          {{ ancestor.title }}
        </span>
        <ChevronRightIcon
          v-if="index < ancestors.length - 1"
          class="separator"
        />
      </template>
    </nav>
  </div>
</template>

<style scoped>
.breadcrumb-container {
  padding: 8px 16px;
  background-color: #f0f9ff;
  border-bottom: 1px solid #bae6fd;
  margin-bottom: 16px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 13px;
}

.breadcrumb-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #0369a1;
  cursor: pointer;
}

.breadcrumb-item:hover:not(.is-current) {
  text-decoration: underline;
}

.breadcrumb-item.is-current {
  color: #64748b;
  cursor: default;
}

.separator {
  color: #94a3b8;
  width: 16px;
  height: 16px;
}
</style>
```

---

## 4.5 툴팁 설계

### 4.5.1 부모 정보 툴팁

```
┌─────────────────────────────────────────┐
│  설명                                    │
│  ─────────────────────────────────────  │
│  메인 페이지 UI/UX 디자인 작업           │
│                                         │
│  상위 업무                               │
│  ─────────────────────────────────────  │
│  ┌───────────────┐     ┌─────────────┐ │
│  │ 웹사이트 리뉴얼 │ ─▶ │ 디자인 작업  │ │
│  │    (기본)     │     │   (하위1)   │ │
│  └───────────────┘     └─────────────┘ │
│                                         │
│  상태: 진행중                            │
└─────────────────────────────────────────┘
```

### 4.5.2 툴팁 컴포넌트 확장

```typescript
// types/tooltip.ts
interface TooltipContent {
  description?: string
  parentInfo?: {
    path: Array<{ itemId: number; title: string; depth: number }>
    status: string
  }
}
```

---

## 4.6 애니메이션 설계

### 4.6.1 CSS 애니메이션

```css
/* 하위 업무 펼침/접힘 */
.children-enter-active {
  animation: slide-down 0.3s ease-out;
}

.children-leave-active {
  animation: slide-up 0.2s ease-in;
}

@keyframes slide-down {
  from {
    max-height: 0;
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    max-height: 1000px;
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slide-up {
  from {
    max-height: 1000px;
    opacity: 1;
    transform: translateY(0);
  }
  to {
    max-height: 0;
    opacity: 0;
    transform: translateY(-10px);
  }
}

/* 하위 업무 생성 시 */
.child-item-enter-active {
  animation: fade-in-scale 0.2s ease-out;
}

@keyframes fade-in-scale {
  from {
    opacity: 0;
    transform: scale(0.95) translateX(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateX(0);
  }
}

/* Drag 시 하이라이트 */
.drag-over-valid {
  animation: pulse-highlight 0.5s ease infinite;
}

@keyframes pulse-highlight {
  0%, 100% { background-color: rgba(59, 130, 246, 0.1); }
  50% { background-color: rgba(59, 130, 246, 0.2); }
}

.drag-over-invalid {
  animation: shake 0.3s ease;
  background-color: rgba(239, 68, 68, 0.1);
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}
```

### 4.6.2 Vue Transition 컴포넌트

```vue
<!-- TransitionGroup으로 하위 업무 리스트 애니메이션 -->
<TransitionGroup name="child-list" tag="div">
  <SubTaskRow
    v-for="child in sortedChildren"
    :key="child.itemId"
    :item="child"
  />
</TransitionGroup>

<style>
.child-list-move {
  transition: transform 0.3s ease;
}

.child-list-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}

.child-list-enter-active {
  transition: all 0.3s ease;
}

.child-list-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.child-list-leave-active {
  transition: all 0.2s ease;
  position: absolute;
}
</style>
```

---

## 4.7 반응형 설계

### 4.7.1 모바일 대응 (< 768px)

| 항목 | 처리 |
|------|------|
| indent | 깊이당 12px로 축소 |
| 계층선 | 단순화 (색상 표시만) |
| 펼침/접힘 | 기본 접힌 상태 |
| 상세 패널 | 전체 화면 표시 |

### 4.7.2 태블릿 대응 (768px ~ 1024px)

| 항목 | 처리 |
|------|------|
| indent | 깊이당 20px |
| 칸반 | 2컬럼 표시 |
| 카드 크기 | 약간 축소 |

---

## 4.8 접근성 고려

| 항목 | 구현 |
|------|------|
| 키보드 탐색 | Tab으로 계층 이동, Enter로 펼침/접힘 |
| 스크린 리더 | aria-expanded, aria-level 속성 |
| 포커스 표시 | 명확한 포커스 링 |
| 색상 대비 | 계층 구분 색상 대비율 4.5:1 이상 |

---

## 승인 체크리스트

- [x] 테이블 뷰 계층 표현 승인 ✅ 2026-01-15
- [x] 칸반 뷰 계층 표현 승인 ✅ 2026-01-15
- [x] 상세 패널 구조 승인 ✅ 2026-01-15
- [x] 툴팁 설계 승인 ✅ 2026-01-15
- [x] 애니메이션 설계 승인 ✅ 2026-01-15
- [x] Drag&Drop 제약 승인 ✅ 2026-01-15
