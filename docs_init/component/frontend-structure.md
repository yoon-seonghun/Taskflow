# TaskFlow 프론트엔드 컴포넌트 구조 설계

## 목차
1. [디렉토리 구조](#1-디렉토리-구조)
2. [Pages (Views)](#2-pages-views)
3. [Layout 컴포넌트](#3-layout-컴포넌트)
4. [Common 컴포넌트](#4-common-컴포넌트)
5. [UI 컴포넌트](#5-ui-컴포넌트)
6. [Item 컴포넌트](#6-item-컴포넌트)
7. [Property 컴포넌트](#7-property-컴포넌트)
8. [Department 컴포넌트](#8-department-컴포넌트)
9. [Group 컴포넌트](#9-group-컴포넌트)
10. [Stores (Pinia)](#10-stores-pinia)
11. [Composables](#11-composables)
12. [API 모듈](#12-api-모듈)
13. [Types](#13-types)

---

## 1. 디렉토리 구조

```
src/
├── App.vue
├── main.ts
│
├── assets/
│   ├── main.css
│   └── icons/
│
├── api/
│   ├── client.ts
│   ├── auth.api.ts
│   ├── user.api.ts
│   ├── department.api.ts
│   ├── group.api.ts
│   ├── board.api.ts
│   ├── item.api.ts
│   ├── property.api.ts
│   ├── option.api.ts
│   ├── comment.api.ts
│   ├── template.api.ts
│   └── history.api.ts
│
├── components/
│   ├── layout/
│   │   ├── AppLayout.vue
│   │   ├── AppHeader.vue
│   │   ├── AppSidebar.vue
│   │   └── AppFooter.vue
│   │
│   ├── common/
│   │   ├── BaseButton.vue
│   │   ├── BaseInput.vue
│   │   ├── BaseSelect.vue
│   │   ├── BaseCheckbox.vue
│   │   ├── BaseDatePicker.vue
│   │   ├── BaseModal.vue
│   │   ├── BaseToast.vue
│   │   ├── BaseDropdown.vue
│   │   ├── BaseColorPicker.vue
│   │   ├── BasePagination.vue
│   │   ├── BaseSpinner.vue
│   │   ├── BaseAvatar.vue
│   │   ├── BaseBadge.vue
│   │   └── BaseConfirmDialog.vue
│   │
│   ├── ui/
│   │   ├── SlideoverPanel.vue
│   │   ├── ContextMenu.vue
│   │   ├── InlineEditor.vue
│   │   ├── TreeView.vue
│   │   ├── DataTable.vue
│   │   ├── EmptyState.vue
│   │   ├── SearchInput.vue
│   │   └── FilterBar.vue
│   │
│   ├── item/
│   │   ├── ItemTable.vue
│   │   ├── ItemTableRow.vue
│   │   ├── ItemKanban.vue
│   │   ├── ItemKanbanColumn.vue
│   │   ├── ItemKanbanCard.vue
│   │   ├── ItemList.vue
│   │   ├── ItemListRow.vue
│   │   ├── ItemCard.vue
│   │   ├── ItemForm.vue
│   │   ├── ItemDetail.vue
│   │   ├── ItemCreateForm.vue
│   │   ├── ItemStatusBadge.vue
│   │   ├── ItemPriorityBadge.vue
│   │   └── ItemCompletedSection.vue
│   │
│   ├── property/
│   │   ├── PropertyHeader.vue
│   │   ├── PropertyHeaderMenu.vue
│   │   ├── PropertyEditor.vue
│   │   ├── PropertyValueCell.vue
│   │   ├── PropertyTextEditor.vue
│   │   ├── PropertyNumberEditor.vue
│   │   ├── PropertyDateEditor.vue
│   │   ├── PropertySelectEditor.vue
│   │   ├── PropertyMultiSelectEditor.vue
│   │   ├── PropertyCheckboxEditor.vue
│   │   ├── PropertyUserEditor.vue
│   │   ├── OptionManager.vue
│   │   ├── OptionItem.vue
│   │   └── OptionCreateForm.vue
│   │
│   ├── comment/
│   │   ├── CommentList.vue
│   │   ├── CommentItem.vue
│   │   └── CommentForm.vue
│   │
│   ├── user/
│   │   ├── UserList.vue
│   │   ├── UserForm.vue
│   │   ├── UserSelect.vue
│   │   └── PasswordChangeForm.vue
│   │
│   ├── department/
│   │   ├── DepartmentTree.vue
│   │   ├── DepartmentNode.vue
│   │   ├── DepartmentForm.vue
│   │   └── DepartmentSelect.vue
│   │
│   ├── group/
│   │   ├── GroupList.vue
│   │   ├── GroupCard.vue
│   │   ├── GroupForm.vue
│   │   ├── GroupMemberManager.vue
│   │   ├── GroupMemberList.vue
│   │   └── GroupSelect.vue
│   │
│   └── board/
│       ├── BoardHeader.vue
│       ├── BoardViewSwitcher.vue
│       ├── BoardShareManager.vue
│       └── BoardShareList.vue
│
├── composables/
│   ├── useAuth.ts
│   ├── useBoard.ts
│   ├── useItem.ts
│   ├── useProperty.ts
│   ├── useComment.ts
│   ├── useUser.ts
│   ├── useDepartment.ts
│   ├── useGroup.ts
│   ├── useSSE.ts
│   ├── useToast.ts
│   ├── useModal.ts
│   ├── useConfirm.ts
│   ├── useErrorHandler.ts
│   ├── usePagination.ts
│   └── useDebounce.ts
│
├── router/
│   ├── index.ts
│   └── guards.ts
│
├── stores/
│   ├── auth.ts
│   ├── board.ts
│   ├── item.ts
│   ├── property.ts
│   ├── user.ts
│   ├── department.ts
│   ├── group.ts
│   ├── template.ts
│   └── ui.ts
│
├── types/
│   ├── api.ts
│   ├── auth.ts
│   ├── user.ts
│   ├── department.ts
│   ├── group.ts
│   ├── board.ts
│   ├── item.ts
│   ├── property.ts
│   ├── comment.ts
│   ├── template.ts
│   └── history.ts
│
├── utils/
│   ├── date.ts
│   ├── format.ts
│   ├── validation.ts
│   └── constants.ts
│
└── views/
    ├── LoginView.vue
    ├── BoardView.vue
    ├── CompletedTasksView.vue
    ├── TaskTemplatesView.vue
    ├── HistoryView.vue
    ├── UsersView.vue
    ├── ShareUsersView.vue
    ├── DepartmentsView.vue
    ├── GroupsView.vue
    └── NotFoundView.vue
```

---

## 2. Pages (Views)

### 2.1 LoginView.vue
로그인 페이지

| 섹션 | 설명 |
|------|------|
| 로고 | TaskFlow 로고 |
| 로그인 폼 | 아이디, 비밀번호 입력 |
| 로그인 버튼 | 인증 처리 |
| 에러 메시지 | 인증 실패 시 표시 |

**사용 컴포넌트**
- BaseInput
- BaseButton

**사용 Store**
- authStore

**사용 Composable**
- useAuth

---

### 2.2 BoardView.vue
메인 업무 페이지

| 섹션 | 설명 |
|------|------|
| 신규 업무 등록 | 업무 내용 입력, 자동완성, 등록 버튼 |
| 뷰 전환 | 테이블/칸반/리스트 토글 |
| 필터바 | 상태, 우선순위, 담당자, 그룹 필터 |
| 업무 목록 | 선택된 뷰에 따른 목록 표시 |
| 슬라이드오버 패널 | 업무 상세/편집 (PC) |
| 완료된 업무 섹션 | 당일 완료/삭제 업무 Hidden 처리 |

**사용 컴포넌트**
- ItemCreateForm
- BoardViewSwitcher
- FilterBar
- ItemTable / ItemKanban / ItemList
- SlideoverPanel
- ItemDetail
- ItemCompletedSection

**사용 Store**
- boardStore, itemStore, propertyStore

**사용 Composable**
- useItem, useProperty, useSSE

---

### 2.3 CompletedTasksView.vue
완료 작업 메뉴

| 섹션 | 설명 |
|------|------|
| 기간 필터 | 오늘, 이번주, 이번달, 사용자 지정 |
| 완료 목록 | 등록시간, 완료시간, 작업 내용, 작업자 |

**사용 컴포넌트**
- FilterBar
- DataTable
- BasePagination

---

### 2.4 TaskTemplatesView.vue
작업 등록 메뉴

| 섹션 | 설명 |
|------|------|
| 입력 폼 | 작업 내용 입력 |
| 버튼 | 등록/변경 토글, 삭제 버튼 |
| 템플릿 목록 | 등록된 작업 템플릿 목록 |

**사용 컴포넌트**
- BaseInput
- BaseButton
- DataTable

**사용 Store**
- templateStore

---

### 2.5 HistoryView.vue
이력관리 메뉴

| 섹션 | 설명 |
|------|------|
| 탭 스위치 | 작업 처리 이력 / 작업 등록 이력 |
| 필터 | 기간, 작업자 |
| 이력 목록 | 선택된 탭에 따른 이력 표시 |

**사용 컴포넌트**
- FilterBar
- DataTable
- BasePagination

---

### 2.6 UsersView.vue
사용자 등록 메뉴

| 섹션 | 설명 |
|------|------|
| 사용자 등록 폼 | 이름, 아이디, 비밀번호, 부서 |
| 사용자 목록 | 등록된 사용자 목록 |
| 수정 모달 | 사용자 정보 수정 |

**사용 컴포넌트**
- UserForm
- UserList
- BaseModal
- PasswordChangeForm
- DepartmentSelect

**사용 Store**
- userStore, departmentStore

---

### 2.7 ShareUsersView.vue
공유 사용자 등록 메뉴

| 섹션 | 설명 |
|------|------|
| 사용자 검색 | 아이디로 사용자 검색 |
| 공유 추가 | 권한 선택 후 추가 |
| 공유 목록 | 현재 공유된 사용자 목록 |

**사용 컴포넌트**
- SearchInput
- UserSelect
- BoardShareList
- BoardShareManager

**사용 Store**
- boardStore, userStore

---

### 2.8 DepartmentsView.vue
부서 관리 메뉴

| 섹션 | 설명 |
|------|------|
| 부서 트리 | 계층 구조 트리뷰 |
| 부서 등록/수정 폼 | 부서 코드, 부서명, 상위 부서 |
| 부서 상세 | 소속 사용자 수 등 |

**사용 컴포넌트**
- DepartmentTree
- DepartmentNode
- DepartmentForm
- DepartmentSelect
- TreeView

**사용 Store**
- departmentStore

**사용 Composable**
- useDepartment

---

### 2.9 GroupsView.vue
그룹 관리 메뉴

| 섹션 | 설명 |
|------|------|
| 그룹 목록 | 그룹 카드 형태 목록 |
| 그룹 등록/수정 폼 | 그룹 코드, 그룹명, 색상 |
| 멤버 관리 | 그룹 멤버 추가/제거 |

**사용 컴포넌트**
- GroupList
- GroupCard
- GroupForm
- GroupMemberManager
- GroupMemberList
- BaseColorPicker

**사용 Store**
- groupStore, userStore

**사용 Composable**
- useGroup

---

## 3. Layout 컴포넌트

### 3.1 AppLayout.vue
메인 레이아웃 wrapper

| Slot | 설명 |
|------|------|
| default | 메인 콘텐츠 영역 |

| 구성 | 설명 |
|------|------|
| AppHeader | 상단 헤더 |
| AppSidebar | 좌측 사이드바 |
| main | RouterView 영역 |

**Props**
- 없음

**Emits**
- 없음

---

### 3.2 AppHeader.vue
상단 헤더

| 요소 | 설명 |
|------|------|
| 햄버거 메뉴 | 사이드바 토글 (모바일) |
| 로고 | TaskFlow 로고 |
| 사용자 정보 | 이름, 로그아웃 버튼 |

**Props**
- 없음

**Emits**
| 이벤트 | 설명 |
|--------|------|
| toggle-sidebar | 사이드바 토글 |

---

### 3.3 AppSidebar.vue
좌측 사이드바 (네비게이션)

| 메뉴 항목 | 라우트 |
|----------|--------|
| 업무 페이지 | / |
| 완료 작업 메뉴 | /completed |
| 작업 등록 메뉴 | /templates |
| 이력관리 메뉴 | /history |
| --- | --- |
| 사용자 등록 메뉴 | /users |
| 공유 사용자 등록 메뉴 | /shares |
| --- | --- |
| 부서 관리 메뉴 | /departments |
| 그룹 관리 메뉴 | /groups |

**Props**
| Prop | Type | 설명 |
|------|------|------|
| open | boolean | 사이드바 열림 상태 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| close | 사이드바 닫기 |

---

## 4. Common 컴포넌트

### 4.1 BaseButton.vue

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| type | 'button' \| 'submit' | 'button' | 버튼 타입 |
| variant | 'primary' \| 'secondary' \| 'danger' \| 'ghost' | 'primary' | 스타일 변형 |
| size | 'sm' \| 'md' \| 'lg' | 'md' | 크기 |
| disabled | boolean | false | 비활성화 |
| loading | boolean | false | 로딩 상태 |

**Slots**
| Slot | 설명 |
|------|------|
| default | 버튼 텍스트 |
| icon | 아이콘 |

---

### 4.2 BaseInput.vue

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | string | '' | v-model 값 |
| type | string | 'text' | input 타입 |
| placeholder | string | '' | placeholder |
| disabled | boolean | false | 비활성화 |
| error | string | '' | 에러 메시지 |
| label | string | '' | 라벨 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:modelValue | 값 변경 |

---

### 4.3 BaseSelect.vue

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | any | null | v-model 값 |
| options | Array | [] | 옵션 목록 |
| optionLabel | string | 'label' | 옵션 라벨 필드 |
| optionValue | string | 'value' | 옵션 값 필드 |
| placeholder | string | '선택하세요' | placeholder |
| disabled | boolean | false | 비활성화 |
| searchable | boolean | false | 검색 가능 |
| clearable | boolean | false | 클리어 가능 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:modelValue | 값 변경 |

**Slots**
| Slot | 설명 |
|------|------|
| option | 커스텀 옵션 렌더링 |
| footer | 드롭다운 하단 (신규 추가 등) |

---

### 4.4 BaseDatePicker.vue

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | string \| Date | null | v-model 값 |
| placeholder | string | '' | placeholder |
| format | string | 'YYYY-MM-DD' | 날짜 포맷 |
| disabled | boolean | false | 비활성화 |
| clearable | boolean | true | 클리어 가능 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:modelValue | 값 변경 |

---

### 4.5 BaseModal.vue

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | boolean | false | 열림 상태 |
| title | string | '' | 모달 제목 |
| size | 'sm' \| 'md' \| 'lg' \| 'xl' | 'md' | 크기 |
| closable | boolean | true | 닫기 버튼 표시 |
| closeOnOverlay | boolean | true | 오버레이 클릭 시 닫기 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:modelValue | 열림 상태 변경 |
| close | 닫기 |

**Slots**
| Slot | 설명 |
|------|------|
| default | 모달 본문 |
| footer | 모달 푸터 (버튼 영역) |

---

### 4.6 BaseToast.vue

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| type | 'success' \| 'error' \| 'warning' \| 'info' | 'info' | 토스트 타입 |
| message | string | '' | 메시지 |
| duration | number | 3000 | 표시 시간 (ms) |

---

### 4.7 BaseColorPicker.vue

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | string | '#000000' | 선택된 색상 |
| presetColors | string[] | [...] | 프리셋 색상 목록 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:modelValue | 색상 변경 |

---

### 4.8 BasePagination.vue

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| currentPage | number | 1 | 현재 페이지 |
| totalPages | number | 1 | 전체 페이지 수 |
| totalItems | number | 0 | 전체 아이템 수 |
| pageSize | number | 20 | 페이지 크기 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:currentPage | 페이지 변경 |
| update:pageSize | 페이지 크기 변경 |

---

### 4.9 BaseConfirmDialog.vue

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | boolean | false | 열림 상태 |
| title | string | '확인' | 제목 |
| message | string | '' | 메시지 |
| confirmText | string | '확인' | 확인 버튼 텍스트 |
| cancelText | string | '취소' | 취소 버튼 텍스트 |
| variant | 'primary' \| 'danger' | 'primary' | 확인 버튼 스타일 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| confirm | 확인 |
| cancel | 취소 |

---

## 5. UI 컴포넌트

### 5.1 SlideoverPanel.vue
슬라이드오버 패널 (PC 업무 상세)

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | boolean | false | 열림 상태 |
| title | string | '' | 패널 제목 |
| width | string | '480px' | 패널 너비 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:modelValue | 열림 상태 변경 |
| close | 패널 닫기 |

**Slots**
| Slot | 설명 |
|------|------|
| default | 패널 본문 |
| footer | 패널 푸터 |

---

### 5.2 ContextMenu.vue
컨텍스트 메뉴 (우클릭/⋮ 메뉴)

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| items | MenuItem[] | [] | 메뉴 항목 |
| position | { x, y } | null | 메뉴 위치 |

**MenuItem 타입**
```typescript
interface MenuItem {
  label: string
  icon?: string
  action?: () => void
  divider?: boolean
  disabled?: boolean
  danger?: boolean
}
```

**Emits**
| 이벤트 | 설명 |
|--------|------|
| select | 메뉴 항목 선택 |
| close | 메뉴 닫기 |

---

### 5.3 InlineEditor.vue
인라인 편집기 (클릭 시 편집 모드)

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | any | '' | v-model 값 |
| type | 'text' \| 'number' \| 'select' \| 'date' | 'text' | 편집기 타입 |
| options | Array | [] | select 옵션 |
| placeholder | string | '' | placeholder |
| disabled | boolean | false | 비활성화 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:modelValue | 값 변경 |
| save | 저장 |
| cancel | 취소 |

---

### 5.4 TreeView.vue
트리 뷰 (부서 계층 등)

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| data | TreeNode[] | [] | 트리 데이터 |
| selectedId | any | null | 선택된 노드 ID |
| expandedIds | any[] | [] | 확장된 노드 ID 목록 |
| labelField | string | 'label' | 라벨 필드 |
| childrenField | string | 'children' | 자식 필드 |
| draggable | boolean | false | 드래그 가능 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| select | 노드 선택 |
| toggle | 노드 확장/축소 |
| drop | 드래그 앤 드롭 |

**Slots**
| Slot | 설명 |
|------|------|
| node | 커스텀 노드 렌더링 |

---

### 5.5 DataTable.vue
데이터 테이블

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| columns | Column[] | [] | 컬럼 정의 |
| data | any[] | [] | 데이터 |
| loading | boolean | false | 로딩 상태 |
| sortField | string | '' | 정렬 필드 |
| sortOrder | 'asc' \| 'desc' | 'asc' | 정렬 방향 |
| selectable | boolean | false | 선택 가능 |
| rowHeight | number | 36 | 행 높이 (px) |

**Column 타입**
```typescript
interface Column {
  field: string
  label: string
  width?: string
  sortable?: boolean
  align?: 'left' | 'center' | 'right'
}
```

**Emits**
| 이벤트 | 설명 |
|--------|------|
| sort | 정렬 변경 |
| row-click | 행 클릭 |
| selection-change | 선택 변경 |

**Slots**
| Slot | 설명 |
|------|------|
| cell-{field} | 특정 컬럼 셀 커스텀 렌더링 |
| empty | 빈 상태 |

---

### 5.6 FilterBar.vue
필터 바

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| filters | FilterConfig[] | [] | 필터 설정 |
| modelValue | Record<string, any> | {} | 필터 값 |

**FilterConfig 타입**
```typescript
interface FilterConfig {
  field: string
  label: string
  type: 'select' | 'date' | 'daterange' | 'search'
  options?: any[]
  placeholder?: string
}
```

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:modelValue | 필터 값 변경 |
| search | 검색 실행 |
| reset | 필터 초기화 |

---

### 5.7 SearchInput.vue
검색 입력 (디바운스 적용)

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | string | '' | v-model 값 |
| placeholder | string | '검색...' | placeholder |
| debounce | number | 300 | 디바운스 시간 (ms) |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:modelValue | 값 변경 |
| search | 검색 실행 (디바운스 후) |

---

## 6. Item 컴포넌트

### 6.1 ItemTable.vue
테이블 뷰

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| items | Item[] | [] | 아이템 목록 |
| properties | PropertyDef[] | [] | 속성 정의 |
| loading | boolean | false | 로딩 상태 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| select | 아이템 선택 |
| update | 인라인 수정 |
| complete | 완료 처리 |
| delete | 삭제 처리 |

---

### 6.2 ItemKanban.vue
칸반 뷰

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| items | Item[] | [] | 아이템 목록 |
| groupBy | 'status' \| 'priority' \| 'assignee' | 'status' | 그룹핑 기준 |
| columns | KanbanColumn[] | [] | 컬럼 정의 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| select | 아이템 선택 |
| move | 드래그 앤 드롭 이동 |

---

### 6.3 ItemList.vue
리스트 뷰

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| items | Item[] | [] | 아이템 목록 |
| loading | boolean | false | 로딩 상태 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| select | 아이템 선택 |
| complete | 완료 처리 |
| delete | 삭제 처리 |

---

### 6.4 ItemDetail.vue
아이템 상세 (슬라이드오버 내용)

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| item | Item | null | 아이템 데이터 |
| properties | PropertyDef[] | [] | 속성 정의 |
| editable | boolean | true | 편집 가능 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update | 수정 |
| complete | 완료 처리 |
| delete | 삭제 처리 |
| close | 패널 닫기 |

---

### 6.5 ItemCreateForm.vue
신규 업무 등록 폼

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| templates | Template[] | [] | 작업 템플릿 (자동완성) |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| create | 아이템 생성 |

---

### 6.6 ItemCompletedSection.vue
완료된 업무 축소 섹션

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| items | Item[] | [] | 완료/삭제된 아이템 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| restore | 복원 |
| select | 선택 |

---

### 6.7 ItemStatusBadge.vue
상태 뱃지

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| status | ItemStatus | - | 상태 값 |

---

### 6.8 ItemPriorityBadge.vue
우선순위 뱃지

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| priority | Priority | - | 우선순위 값 |

---

## 7. Property 컴포넌트

### 7.1 PropertyHeader.vue
테이블 컬럼 헤더 (속성명)

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| property | PropertyDef | - | 속성 정의 |
| sortable | boolean | true | 정렬 가능 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| sort | 정렬 |
| menu | 메뉴 열기 |

---

### 7.2 PropertyHeaderMenu.vue
속성 헤더 드롭다운 메뉴

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| property | PropertyDef | - | 속성 정의 |

**메뉴 항목**
- 속성 이름 변경
- 속성 타입 변경
- 왼쪽/오른쪽으로 이동
- 숨기기
- 새 속성 추가
- 속성 삭제

**Emits**
| 이벤트 | 설명 |
|--------|------|
| rename | 이름 변경 |
| change-type | 타입 변경 |
| move | 순서 이동 |
| hide | 숨기기 |
| add | 새 속성 추가 |
| delete | 속성 삭제 |

---

### 7.3 PropertyEditor.vue
속성값 편집기 팩토리

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| property | PropertyDef | - | 속성 정의 |
| modelValue | any | - | 현재 값 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:modelValue | 값 변경 |

내부적으로 propertyType에 따라 적절한 에디터 렌더링:
- TEXT → PropertyTextEditor
- NUMBER → PropertyNumberEditor
- DATE → PropertyDateEditor
- SELECT → PropertySelectEditor
- MULTI_SELECT → PropertyMultiSelectEditor
- CHECKBOX → PropertyCheckboxEditor
- USER → PropertyUserEditor

---

### 7.4 PropertySelectEditor.vue
SELECT 타입 편집기

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | number | null | 선택된 옵션 ID |
| options | PropertyOption[] | [] | 옵션 목록 |
| allowCreate | boolean | true | 신규 옵션 추가 가능 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:modelValue | 값 변경 |
| create-option | 새 옵션 추가 |

---

### 7.5 OptionManager.vue
코드 관리 (설정 메뉴)

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| properties | PropertyDef[] | [] | SELECT/MULTI_SELECT 속성 목록 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update | 옵션 수정 |
| delete | 옵션 삭제 |
| reorder | 순서 변경 |
| add | 옵션 추가 |

---

## 8. Department 컴포넌트

### 8.1 DepartmentTree.vue
부서 트리

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| departments | DepartmentTree[] | [] | 부서 트리 데이터 |
| selectedId | number | null | 선택된 부서 ID |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| select | 부서 선택 |
| edit | 부서 수정 |
| delete | 부서 삭제 |
| reorder | 순서 변경 |

---

### 8.2 DepartmentNode.vue
트리 노드 (재귀 컴포넌트)

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| department | DepartmentTree | - | 부서 데이터 |
| level | number | 0 | 깊이 |
| selected | boolean | false | 선택 상태 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| select | 선택 |
| toggle | 확장/축소 |
| context-menu | 컨텍스트 메뉴 |

---

### 8.3 DepartmentForm.vue
부서 등록/수정 폼

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| department | Department | null | 수정 시 부서 데이터 |
| parentOptions | Department[] | [] | 상위 부서 옵션 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| submit | 저장 |
| cancel | 취소 |

---

### 8.4 DepartmentSelect.vue
부서 선택 드롭다운

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | number | null | 선택된 부서 ID |
| departments | DepartmentFlat[] | [] | 부서 목록 (평면) |
| placeholder | string | '부서 선택' | placeholder |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:modelValue | 선택 변경 |

---

## 9. Group 컴포넌트

### 9.1 GroupList.vue
그룹 목록 (카드 형태)

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| groups | Group[] | [] | 그룹 목록 |
| selectedId | number | null | 선택된 그룹 ID |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| select | 그룹 선택 |
| edit | 그룹 수정 |
| delete | 그룹 삭제 |
| manage-members | 멤버 관리 |

---

### 9.2 GroupCard.vue
그룹 카드

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| group | Group | - | 그룹 데이터 |
| selected | boolean | false | 선택 상태 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| click | 클릭 |
| edit | 수정 |
| delete | 삭제 |

---

### 9.3 GroupForm.vue
그룹 등록/수정 폼

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| group | Group | null | 수정 시 그룹 데이터 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| submit | 저장 |
| cancel | 취소 |

---

### 9.4 GroupMemberManager.vue
그룹 멤버 관리

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| groupId | number | - | 그룹 ID |
| members | User[] | [] | 현재 멤버 목록 |
| availableUsers | User[] | [] | 추가 가능한 사용자 |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| add | 멤버 추가 |
| remove | 멤버 제거 |

---

### 9.5 GroupSelect.vue
그룹 선택 드롭다운

**Props**
| Prop | Type | Default | 설명 |
|------|------|---------|------|
| modelValue | number | null | 선택된 그룹 ID |
| groups | Group[] | [] | 그룹 목록 |
| placeholder | string | '그룹 선택' | placeholder |

**Emits**
| 이벤트 | 설명 |
|--------|------|
| update:modelValue | 선택 변경 |

---

## 10. Stores (Pinia)

### 10.1 authStore

**State**
| 필드 | 타입 | 설명 |
|------|------|------|
| user | User \| null | 현재 사용자 |
| accessToken | string \| null | Access Token |
| isAuthenticated | boolean | 인증 여부 (computed) |

**Actions**
| 메서드 | 설명 |
|--------|------|
| login() | 로그인 |
| logout() | 로그아웃 |
| refreshToken() | 토큰 갱신 |
| setAuth() | 인증 정보 설정 |
| clearAuth() | 인증 정보 제거 |

---

### 10.2 boardStore

**State**
| 필드 | 타입 | 설명 |
|------|------|------|
| boards | Board[] | 보드 목록 |
| currentBoard | Board \| null | 현재 선택된 보드 |
| shares | BoardShare[] | 공유 사용자 목록 |
| loading | boolean | 로딩 상태 |

**Actions**
| 메서드 | 설명 |
|--------|------|
| fetchBoards() | 보드 목록 조회 |
| fetchBoard() | 보드 상세 조회 |
| createBoard() | 보드 생성 |
| updateBoard() | 보드 수정 |
| deleteBoard() | 보드 삭제 |
| addShare() | 공유 추가 |
| removeShare() | 공유 제거 |

---

### 10.3 itemStore

**State**
| 필드 | 타입 | 설명 |
|------|------|------|
| items | Item[] | 아이템 목록 |
| selectedItemId | number \| null | 선택된 아이템 ID |
| selectedItem | Item \| null | 선택된 아이템 (computed) |
| activeItems | Item[] | 활성 아이템 (computed) |
| completedItems | Item[] | 완료/삭제 아이템 (computed) |
| loading | boolean | 로딩 상태 |
| filters | ItemFilters | 필터 조건 |

**Actions**
| 메서드 | 설명 |
|--------|------|
| fetchItems() | 아이템 목록 조회 |
| fetchItem() | 아이템 상세 조회 |
| createItem() | 아이템 생성 |
| updateItem() | 아이템 수정 |
| deleteItem() | 아이템 삭제 |
| completeItem() | 아이템 완료 |
| restoreItem() | 아이템 복원 |
| selectItem() | 아이템 선택 |
| setFilters() | 필터 설정 |
| applySSEEvent() | SSE 이벤트 적용 |

---

### 10.4 propertyStore

**State**
| 필드 | 타입 | 설명 |
|------|------|------|
| properties | PropertyDef[] | 속성 정의 목록 |
| loading | boolean | 로딩 상태 |

**Getters**
| 메서드 | 설명 |
|--------|------|
| getPropertyById() | ID로 속성 조회 |
| getOptionsByPropertyId() | 속성별 옵션 목록 |
| visibleProperties | 표시 속성 목록 |
| selectProperties | SELECT 타입 속성 목록 |

**Actions**
| 메서드 | 설명 |
|--------|------|
| fetchProperties() | 속성 목록 조회 |
| createProperty() | 속성 생성 |
| updateProperty() | 속성 수정 |
| deleteProperty() | 속성 삭제 |
| createOption() | 옵션 추가 |
| updateOption() | 옵션 수정 |
| deleteOption() | 옵션 삭제 |
| applySSEEvent() | SSE 이벤트 적용 |

---

### 10.5 userStore

**State**
| 필드 | 타입 | 설명 |
|------|------|------|
| users | User[] | 사용자 목록 |
| loading | boolean | 로딩 상태 |
| pagination | Pagination | 페이징 정보 |

**Actions**
| 메서드 | 설명 |
|--------|------|
| fetchUsers() | 사용자 목록 조회 |
| createUser() | 사용자 생성 |
| updateUser() | 사용자 수정 |
| deleteUser() | 사용자 삭제 |
| changePassword() | 비밀번호 변경 |

---

### 10.6 departmentStore

**State**
| 필드 | 타입 | 설명 |
|------|------|------|
| departmentTree | DepartmentTree[] | 부서 트리 |
| departmentsFlat | DepartmentFlat[] | 부서 평면 목록 |
| selectedDepartment | Department \| null | 선택된 부서 |
| loading | boolean | 로딩 상태 |

**Actions**
| 메서드 | 설명 |
|--------|------|
| fetchDepartmentTree() | 트리 조회 |
| fetchDepartmentsFlat() | 평면 조회 |
| createDepartment() | 부서 생성 |
| updateDepartment() | 부서 수정 |
| deleteDepartment() | 부서 삭제 |
| updateOrder() | 순서 변경 |

---

### 10.7 groupStore

**State**
| 필드 | 타입 | 설명 |
|------|------|------|
| groups | Group[] | 그룹 목록 |
| selectedGroup | Group \| null | 선택된 그룹 |
| members | User[] | 선택된 그룹의 멤버 |
| loading | boolean | 로딩 상태 |

**Actions**
| 메서드 | 설명 |
|--------|------|
| fetchGroups() | 그룹 목록 조회 |
| createGroup() | 그룹 생성 |
| updateGroup() | 그룹 수정 |
| deleteGroup() | 그룹 삭제 |
| fetchMembers() | 멤버 목록 조회 |
| addMembers() | 멤버 추가 |
| removeMember() | 멤버 제거 |

---

### 10.8 templateStore

**State**
| 필드 | 타입 | 설명 |
|------|------|------|
| templates | Template[] | 템플릿 목록 |
| loading | boolean | 로딩 상태 |

**Actions**
| 메서드 | 설명 |
|--------|------|
| fetchTemplates() | 템플릿 목록 조회 |
| searchTemplates() | 템플릿 검색 |
| createTemplate() | 템플릿 생성 |
| updateTemplate() | 템플릿 수정 |
| deleteTemplate() | 템플릿 삭제 |

---

### 10.9 uiStore

**State**
| 필드 | 타입 | 설명 |
|------|------|------|
| sidebarOpen | boolean | 사이드바 상태 |
| viewMode | 'table' \| 'kanban' \| 'list' | 뷰 모드 |
| slideoverOpen | boolean | 슬라이드오버 상태 |
| toasts | Toast[] | 토스트 목록 |

**Actions**
| 메서드 | 설명 |
|--------|------|
| toggleSidebar() | 사이드바 토글 |
| setViewMode() | 뷰 모드 설정 |
| openSlideover() | 슬라이드오버 열기 |
| closeSlideover() | 슬라이드오버 닫기 |
| showToast() | 토스트 표시 |
| removeToast() | 토스트 제거 |

---

## 11. Composables

### 11.1 useAuth

| 함수/상태 | 타입 | 설명 |
|----------|------|------|
| user | Ref<User> | 현재 사용자 |
| isAuthenticated | Ref<boolean> | 인증 여부 |
| login() | Function | 로그인 |
| logout() | Function | 로그아웃 |
| checkAuth() | Function | 인증 상태 확인 |

---

### 11.2 useItem

| 함수/상태 | 타입 | 설명 |
|----------|------|------|
| items | Ref<Item[]> | 아이템 목록 |
| selectedItem | Ref<Item> | 선택된 아이템 |
| loading | Ref<boolean> | 로딩 상태 |
| fetchItems() | Function | 목록 조회 |
| createItem() | Function | 생성 |
| updateItem() | Function | 수정 |
| deleteItem() | Function | 삭제 |
| completeItem() | Function | 완료 |

---

### 11.3 useProperty

| 함수/상태 | 타입 | 설명 |
|----------|------|------|
| properties | Ref<PropertyDef[]> | 속성 목록 |
| getProperty() | Function | 속성 조회 |
| getOptions() | Function | 옵션 조회 |
| updatePropertyValue() | Function | 속성값 수정 |

---

### 11.4 useSSE

| 함수/상태 | 타입 | 설명 |
|----------|------|------|
| connected | Ref<boolean> | 연결 상태 |
| connect() | Function | SSE 연결 |
| disconnect() | Function | SSE 연결 해제 |
| onEvent() | Function | 이벤트 리스너 등록 |

---

### 11.5 useToast

| 함수/상태 | 타입 | 설명 |
|----------|------|------|
| toasts | Ref<Toast[]> | 토스트 목록 |
| success() | Function | 성공 토스트 |
| error() | Function | 에러 토스트 |
| warning() | Function | 경고 토스트 |
| info() | Function | 정보 토스트 |

---

### 11.6 useModal

| 함수/상태 | 타입 | 설명 |
|----------|------|------|
| isOpen | Ref<boolean> | 열림 상태 |
| open() | Function | 모달 열기 |
| close() | Function | 모달 닫기 |
| toggle() | Function | 토글 |

---

### 11.7 useConfirm

| 함수/상태 | 타입 | 설명 |
|----------|------|------|
| confirm() | Function | 확인 다이얼로그 표시 (Promise) |

---

### 11.8 useErrorHandler

| 함수/상태 | 타입 | 설명 |
|----------|------|------|
| handleError() | Function | 에러 처리 |
| handleApiError() | Function | API 에러 처리 |

---

### 11.9 useDepartment

| 함수/상태 | 타입 | 설명 |
|----------|------|------|
| departmentTree | Ref<DepartmentTree[]> | 부서 트리 |
| departmentsFlat | Ref<DepartmentFlat[]> | 평면 목록 |
| fetchDepartments() | Function | 부서 조회 |
| createDepartment() | Function | 부서 생성 |
| updateDepartment() | Function | 부서 수정 |
| deleteDepartment() | Function | 부서 삭제 |

---

### 11.10 useGroup

| 함수/상태 | 타입 | 설명 |
|----------|------|------|
| groups | Ref<Group[]> | 그룹 목록 |
| fetchGroups() | Function | 그룹 조회 |
| createGroup() | Function | 그룹 생성 |
| updateGroup() | Function | 그룹 수정 |
| deleteGroup() | Function | 그룹 삭제 |
| manageMembers() | Function | 멤버 관리 |

---

### 11.11 usePagination

| 함수/상태 | 타입 | 설명 |
|----------|------|------|
| currentPage | Ref<number> | 현재 페이지 |
| pageSize | Ref<number> | 페이지 크기 |
| totalItems | Ref<number> | 전체 아이템 수 |
| totalPages | Computed<number> | 전체 페이지 수 |
| setPage() | Function | 페이지 설정 |
| nextPage() | Function | 다음 페이지 |
| prevPage() | Function | 이전 페이지 |

---

### 11.12 useDebounce

| 함수/상태 | 타입 | 설명 |
|----------|------|------|
| debouncedValue | Ref<any> | 디바운스된 값 |
| debounce() | Function | 디바운스 함수 |

---

## 12. API 모듈

### 12.1 client.ts
Axios 인스턴스 및 인터셉터

| 함수 | 설명 |
|------|------|
| get<T>() | GET 요청 |
| post<T>() | POST 요청 |
| put<T>() | PUT 요청 |
| del<T>() | DELETE 요청 |

---

### 12.2 각 도메인별 API

**auth.api.ts**
| 함수 | 설명 |
|------|------|
| login() | 로그인 |
| logout() | 로그아웃 |
| refresh() | 토큰 갱신 |
| me() | 내 정보 |

**user.api.ts**
| 함수 | 설명 |
|------|------|
| getUsers() | 목록 조회 |
| getUser() | 단건 조회 |
| createUser() | 생성 |
| updateUser() | 수정 |
| deleteUser() | 삭제 |
| changePassword() | 비밀번호 변경 |

**department.api.ts**
| 함수 | 설명 |
|------|------|
| getDepartments() | 트리 조회 |
| getDepartmentsFlat() | 평면 조회 |
| getDepartment() | 단건 조회 |
| createDepartment() | 생성 |
| updateDepartment() | 수정 |
| deleteDepartment() | 삭제 |
| updateOrder() | 순서 변경 |
| getUsers() | 소속 사용자 |

**group.api.ts**
| 함수 | 설명 |
|------|------|
| getGroups() | 목록 조회 |
| getGroup() | 단건 조회 |
| createGroup() | 생성 |
| updateGroup() | 수정 |
| deleteGroup() | 삭제 |
| getMembers() | 멤버 조회 |
| addMembers() | 멤버 추가 |
| removeMember() | 멤버 제거 |

**item.api.ts**
| 함수 | 설명 |
|------|------|
| getItems() | 목록 조회 |
| getItem() | 단건 조회 |
| createItem() | 생성 |
| updateItem() | 수정 |
| deleteItem() | 삭제 |
| completeItem() | 완료 |
| restoreItem() | 복원 |
| updateProperties() | 속성값 일괄 수정 |

**property.api.ts / option.api.ts**
| 함수 | 설명 |
|------|------|
| getProperties() | 속성 목록 |
| createProperty() | 속성 생성 |
| updateProperty() | 속성 수정 |
| deleteProperty() | 속성 삭제 |
| getOptions() | 옵션 목록 |
| createOption() | 옵션 생성 |
| updateOption() | 옵션 수정 |
| deleteOption() | 옵션 삭제 |

---

## 13. Types

### 13.1 주요 타입 파일

**api.ts**
- ApiResponse<T>
- PageResponse<T>
- PageRequest

**user.ts**
- User
- UserCreateRequest
- UserUpdateRequest
- PasswordChangeRequest

**department.ts**
- Department
- DepartmentTree
- DepartmentFlat
- DepartmentCreateRequest
- DepartmentUpdateRequest

**group.ts**
- Group
- GroupMember
- GroupCreateRequest
- GroupUpdateRequest

**item.ts**
- Item
- ItemStatus
- Priority
- ItemCreateRequest
- ItemUpdateRequest
- ItemFilters

**property.ts**
- PropertyDef
- PropertyType
- PropertyOption
- PropertyValue

---

## 컴포넌트 요약

| 분류 | 파일 수 |
|------|--------|
| Views (Pages) | 10 |
| Layout | 4 |
| Common | 14 |
| UI | 8 |
| Item | 14 |
| Property | 14 |
| Comment | 3 |
| User | 4 |
| Department | 4 |
| Group | 6 |
| Board | 4 |
| **컴포넌트 합계** | **85** |
| Stores | 9 |
| Composables | 12 |
| API 모듈 | 12 |
| Types | 11 |
| Utils | 4 |
| **전체 합계** | **133** |
