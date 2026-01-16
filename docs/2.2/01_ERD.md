# 1. ERD 및 테이블 정의서

## 1.1 TB_ITEM 테이블 수정

### 추가 컬럼

| 컬럼 | 타입 | 기본값 | NULL | 설명 |
|------|------|--------|------|------|
| PARENT_ITEM_ID | BIGINT | - | Y | 부모 업무 ID (NULL=기본 업무) |
| ITEM_DEPTH | INT | 0 | N | 업무 깊이 (0=기본, 1~2=하위) |
| CHILD_SORT_ORDER | INT | - | Y | 하위 업무 내 정렬 순서 |

### DDL

```sql
-- TB_ITEM 테이블 컬럼 추가
ALTER TABLE TB_ITEM
ADD COLUMN PARENT_ITEM_ID BIGINT NULL COMMENT '부모 업무 ID (NULL=기본 업무)' AFTER CATEGORY_ID,
ADD COLUMN ITEM_DEPTH INT NOT NULL DEFAULT 0 COMMENT '업무 깊이 (0=기본, 1~2=하위)' AFTER PARENT_ITEM_ID,
ADD COLUMN CHILD_SORT_ORDER INT NULL COMMENT '하위 업무 내 정렬 순서' AFTER SORT_ORDER;

-- 인덱스 추가
ALTER TABLE TB_ITEM
ADD INDEX IDX_ITEM_PARENT (PARENT_ITEM_ID),
ADD INDEX IDX_ITEM_DEPTH (ITEM_DEPTH);

-- 외래키 제약 (부모 삭제 시 하위 업무 CASCADE 삭제)
ALTER TABLE TB_ITEM
ADD CONSTRAINT FK_ITEM_PARENT
    FOREIGN KEY (PARENT_ITEM_ID) REFERENCES TB_ITEM(ITEM_ID)
    ON DELETE CASCADE;
```

---

## 1.2 업무 계층 구조도

```
TB_ITEM (기본 업무 A) ─────────────────────────────────────
├── itemId: 1
├── parentItemId: NULL
├── itemDepth: 0
├── sortOrder: 1
│
├── TB_ITEM (하위 업무 A-1) ───────────────────────────────
│   ├── itemId: 10
│   ├── parentItemId: 1
│   ├── itemDepth: 1
│   ├── childSortOrder: 1
│   │
│   ├── TB_ITEM (하위 업무 A-1-1) ─────────────────────────
│   │   ├── itemId: 100
│   │   ├── parentItemId: 10
│   │   ├── itemDepth: 2
│   │   └── childSortOrder: 1
│   │
│   └── TB_ITEM (하위 업무 A-1-2) ─────────────────────────
│       ├── itemId: 101
│       ├── parentItemId: 10
│       ├── itemDepth: 2
│       └── childSortOrder: 2
│
└── TB_ITEM (하위 업무 A-2) ───────────────────────────────
    ├── itemId: 11
    ├── parentItemId: 1
    ├── itemDepth: 1
    └── childSortOrder: 2

TB_ITEM (기본 업무 B) ─────────────────────────────────────
├── itemId: 2
├── parentItemId: NULL
├── itemDepth: 0
└── sortOrder: 2
```

---

## 1.3 깊이(Depth) 정의

| Depth | 명칭 | 설명 | 예시 |
|-------|------|------|------|
| 0 | 기본 업무 | 최상위 업무, 독립적 | "웹사이트 리뉴얼" |
| 1 | 1차 하위 | 기본 업무의 직접 하위 | "디자인 작업" |
| 2 | 2차 하위 | 1차 하위의 하위 (최대) | "메인 페이지 디자인" |

---

## 1.4 비즈니스 규칙

### 생성 규칙

| 규칙 | 설명 |
|------|------|
| 최대 깊이 | 3단계 (depth 0, 1, 2) |
| 깊이 검증 | depth 2 업무에는 하위 업무 생성 불가 |
| 보드 귀속 | 하위 업무는 부모와 동일 보드에 귀속 |
| 그룹 귀속 | 하위 업무는 부모와 동일 그룹에 귀속 |

### 삭제 규칙

| 규칙 | 동작 |
|------|------|
| 부모 삭제 | 모든 하위 업무 CASCADE 삭제 |
| 하위 삭제 | 해당 업무 및 그 하위만 삭제 |
| 소프트 삭제 | 부모 삭제(DELETED) 시 하위도 동일 상태 |

### 완료 규칙

| 상황 | 동작 |
|------|------|
| 하위 미완료 시 부모 완료 | 경고 다이얼로그 표시 |
| 경고 내용 | "미완료 하위 업무 N건이 있습니다. 계속 완료 처리하시겠습니까?" |
| 선택지 | [완료 처리] / [취소] |
| 완료 처리 선택 시 | 부모만 완료, 하위는 현재 상태 유지 |

### 이동/이관 규칙

| 동작 | 기본 업무 | 하위 업무 |
|------|----------|----------|
| 보드 이동 | 가능 (하위 포함) | 불가 |
| 그룹 이동 | 가능 (하위 포함) | 불가 |
| 이관 | 가능 (하위 포함) | 불가 (단독 이관 금지) |
| Drag&Drop 순서 변경 | 기본 업무 간 이동 | 부모 내에서만 순서 변경 |

### 배당/공유 규칙

| 동작 | 기본 업무 | 하위 업무 |
|------|----------|----------|
| 공유 | 가능 (하위 포함, 구조 유지) | 가능 (개별) |
| 배당 | 가능 (하위 포함, 구조 유지) | 가능 (개별) |
| 배당 시 표시 | 배당받은 사용자에게 구조 유지 | 개별 업무로 표시 |
| 부모 정보 | - | 툴팁으로 부모 업무 정보 표시 |

---

## 1.5 데이터 예시

### 시나리오: 프로젝트 업무 관리

```sql
-- 기본 업무 (depth 0)
INSERT INTO TB_ITEM (ITEM_ID, BOARD_ID, PARENT_ITEM_ID, ITEM_DEPTH, CONTENT, SORT_ORDER)
VALUES (1, 100, NULL, 0, '웹사이트 리뉴얼 프로젝트', 1);

-- 1차 하위 업무 (depth 1)
INSERT INTO TB_ITEM (ITEM_ID, BOARD_ID, PARENT_ITEM_ID, ITEM_DEPTH, CONTENT, CHILD_SORT_ORDER)
VALUES
(10, 100, 1, 1, '디자인 작업', 1),
(11, 100, 1, 1, '퍼블리싱 작업', 2),
(12, 100, 1, 1, '백엔드 개발', 3);

-- 2차 하위 업무 (depth 2)
INSERT INTO TB_ITEM (ITEM_ID, BOARD_ID, PARENT_ITEM_ID, ITEM_DEPTH, CONTENT, CHILD_SORT_ORDER)
VALUES
(100, 100, 10, 2, '메인 페이지 디자인', 1),
(101, 100, 10, 2, '서브 페이지 디자인', 2),
(110, 100, 11, 2, '메인 페이지 퍼블리싱', 1);
```

### 결과 트리 구조

```
웹사이트 리뉴얼 프로젝트 (depth 0)
├── 디자인 작업 (depth 1)
│   ├── 메인 페이지 디자인 (depth 2)
│   └── 서브 페이지 디자인 (depth 2)
├── 퍼블리싱 작업 (depth 1)
│   └── 메인 페이지 퍼블리싱 (depth 2)
└── 백엔드 개발 (depth 1)
```

---

## 1.6 인덱스 설계

| 인덱스명 | 컬럼 | 용도 |
|----------|------|------|
| IDX_ITEM_PARENT | PARENT_ITEM_ID | 하위 업무 조회 |
| IDX_ITEM_DEPTH | ITEM_DEPTH | 깊이별 조회 |
| IDX_ITEM_PARENT_SORT | PARENT_ITEM_ID, CHILD_SORT_ORDER | 하위 업무 정렬 조회 |

```sql
-- 복합 인덱스 추가
ALTER TABLE TB_ITEM
ADD INDEX IDX_ITEM_PARENT_SORT (PARENT_ITEM_ID, CHILD_SORT_ORDER);
```

---

## 승인 체크리스트

- [x] 테이블 구조 승인 ✅ 2026-01-15
- [x] 깊이 제한 (3단계) 승인 ✅ 2026-01-15
- [x] 비즈니스 규칙 승인 ✅ 2026-01-15
- [x] 인덱스 설계 승인 ✅ 2026-01-15
