# v2.2 하위 업무(Sub-task) 기능 설계서

## 개요

기본 업무에서 파생되는 하위(보조/부가) 업무를 생성하여 계층적으로 관리하는 기능입니다.

## 문서 목록

| 문서 | 설명 |
|------|------|
| [01_ERD.md](./01_ERD.md) | ERD 및 테이블 정의서 |
| [02_API.md](./02_API.md) | API 명세서 |
| [03_COMPONENT.md](./03_COMPONENT.md) | 컴포넌트 구조 |
| [04_UI_UX.md](./04_UI_UX.md) | UI/UX 설계 |
| [05_DRAG_DROP.md](./05_DRAG_DROP.md) | Drag & Drop 영향 분석 |

## 핵심 요구사항

### 기본 규칙
- 최대 깊이: 3단계 (기본 → 하위1 → 하위2)
- 하위 업무는 항상 부모에 귀속
- 하위 업무만 단독 이관 불가 (부모와 함께 이동)
- 하위 업무는 보드 이동 불가, 공유/배당만 가능

### 배당/공유 규칙
- 부모 업무 배당 시 하위 업무도 함께 공유 (구조 유지)
- 하위 업무 개별 배당 가능 (배당받은 사용자에게 개별 업무로 표시)
- 배당받은 업무의 부모 정보는 툴팁으로 표시

### 완료/삭제 규칙
- 부모 업무 완료 시 미완료 하위 업무 있으면 경고 + 선택
- 부모 업무 삭제 시 하위 업무 CASCADE 삭제

### UI 규칙
- 칸반/테이블 뷰에서 계층 표현 (indent 과도하지 않게)
- Drag&Drop 시 부모 노드 하위에서만 순서 변경 가능
- Animation 활용하여 계층 표현 개선

  v2.2 하위 업무 계층 구조 표시 문제를 수정했습니다.

### 변경 내역

  1. ItemTable.vue (frontend/src/components/item/ItemTable.vue)
  - activeItems 대신 activeRootItems를 사용하도록 변경
  - expandedItemIds ref 추가: 확장된 아이템 ID 추적
  - flattenedItems computed 추가: 루트 아이템 + 확장된 부모의 하위 업무를 평탄화하여 표시
  - toggleExpand(), isExpanded() 함수 추가
  - getActiveChildren() 함수 추가: 특정 부모의 활성 하위 업무 조회
  - D&D 제약 적용: createSubTaskCanDrop<Item>() 사용

  2. ItemRow.vue (frontend/src/components/item/ItemRow.vue)
  - expanded, hasChildren props 추가
  - toggleExpand emit 추가
  - 깊이별 왼쪽 테두리 스타일 추가 (depth 1: 파란색, depth 2: 보라색)
  - 확장/축소 토글 버튼 추가 (하위 업무가 있는 경우)
  - 깊이별 인덴트 표시 (24px * depth)

  동작 방식

  1. 테이블 뷰에서 루트 업무만 기본 표시
  2. 하위 업무가 있는 업무는 ▶ 토글 버튼 표시
  3. 토글 클릭 시 하위 업무 펼치기/접기
  4. 하위 업무는 깊이에 따라 indent + 색상 테두리 표시
    - depth 1: 24px indent, 파란색 왼쪽 테두리
    - depth 2: 48px indent, 보라색 왼쪽 테두리
  5. D&D 제약: 같은 부모 내에서만 순서 변경 가능

## 버전 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-15 | - | 최초 작성 |
| 1.1 | 2026-01-15 | - | Drag & Drop 영향 분석 문서 추가 |
| 1.2 | 2026-01-15 | - | 동작방식 변경 |
