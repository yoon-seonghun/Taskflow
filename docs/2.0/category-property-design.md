# 카테고리-속성 시스템 설계서

> **작성일**: 2025-01-04
> **버전**: 2.0
> **상태**: 설계 검토 중

---

## 1. 개요

### 1.1 변경 배경
- 기존: 속성이 보드에 직접 귀속 (TB_PROPERTY_DEF.BOARD_ID)
- 문제점: 속성 재사용 불가, 보드별 중복 생성 필요
- 변경: 카테고리 기반 속성 관리 시스템 도입

### 1.2 핵심 변경 사항
1. 속성 소유권을 보드에서 사용자/역할로 변경
2. 카테고리를 속성 그룹 컨테이너로 활용
3. 글로벌/매니저/사용자 3계층 속성 체계 도입
4. 보드/업무 생성 시 카테고리 선택 → 속성 자동 적용

---

## 2. 속성 계층 구조

### 2.1 속성 유형

| 유형 | 생성 권한 | 적용 범위 | 삭제 시 동작 |
|------|----------|----------|-------------|
| **기본 속성** | 시스템 | 전체 (필수) | 삭제 불가 |
| **글로벌 속성** | ADMIN | 전체 사용자 | 기존 업무 값 유지 |
| **매니저 속성** | MANAGER | 본인+하위부서 | 기존 업무 값 유지 |
| **사용자 속성** | USER | 본인만 | 본인 관리 |

### 2.2 기본 속성 (TB_ITEM 고정 컬럼)

| 속성명 | 컬럼 | 타입 | 필수 | 제거 가능 |
|--------|------|------|------|----------|
| 업무내용 | CONTENT | TEXT | O | X |
| 상세설명 | DESCRIPTION | TEXT | - | X |
| 상태 | STATUS | SELECT | O | X |
| 우선순위 | PRIORITY | SELECT | O | X |
| 시작시간 | START_TIME | DATETIME | - | X |
| 완료예정시간 | END_TIME | DATETIME | - | X |
| 담당자 | ASSIGNEE | USER | - | X |
| 그룹 | GROUP_ID | SELECT | - | X |
| 카테고리 | CATEGORY_ID | SELECT | - | X |

### 2.3 속성 접근 권한

```
사용자가 사용 가능한 속성:
1. 글로벌 속성 (전체)
2. 매니저 속성 (본인 부서 및 상위 부서에서 생성된 것)
3. 사용자 속성 (본인이 생성한 것)
```

---

## 3. 업무 흐름

### 3.1 속성 생성 → 카테고리 → 보드 → 업무

```
[1] 속성 생성
    ├─ ADMIN: 글로벌 속성 생성
    ├─ MANAGER: 매니저 속성 생성 (부서/하위부서 범위)
    └─ USER: 사용자 속성 생성 (본인만)

[2] 카테고리 생성 & 속성 배정
    ├─ 사용자가 카테고리 생성
    └─ 카테고리에 속성 등록 (글로벌/매니저/사용자 속성 선택)

[3] 보드 생성
    ├─ 카테고리 선택
    ├─ 카테고리 귀속 속성 + 글로벌 속성 + 매니저 속성 표시
    └─ 사용할 속성 선택 등록 (기본 속성 필수, 나머지 선택)

[4] 업무 생성
    ├─ 보드 카테고리 자동 상속
    ├─ 보드에서 선택된 속성만 표시/사용
    └─ (선택) 다른 카테고리 변경 시:
        └─ 해당 카테고리 전체 속성 표시 → 선택 등록
```

### 3.2 카테고리 변경 시 동작

#### 보드 카테고리 변경
- 기존 등록된 업무에는 영향 없음
- 신규 업무 등록 시 변경된 카테고리 자동 지정

#### 업무 카테고리 변경
- 기존 사용 중인 속성값 폐기
- 신규 카테고리 속성 적용
- 속성명이 동일하면 기존 값 유지

---

## 4. DB 스키마 설계

### 4.1 TB_PROPERTY_DEF 수정

```sql
-- 기존 컬럼 수정
ALTER TABLE TB_PROPERTY_DEF
  MODIFY BOARD_ID BIGINT NULL COMMENT '보드 ID (NULL: 보드 미귀속 속성)';

-- 신규 컬럼 추가
ALTER TABLE TB_PROPERTY_DEF
  ADD COLUMN OWNER_TYPE VARCHAR(20) NOT NULL DEFAULT 'USER'
      COMMENT '소유 유형 (GLOBAL, MANAGER, USER)' AFTER BOARD_ID,
  ADD COLUMN OWNER_USERNAME VARCHAR(50) NULL
      COMMENT '소유자 USERNAME (GLOBAL은 NULL)' AFTER OWNER_TYPE,
  ADD COLUMN OWNER_DEPT_CODE VARCHAR(20) NULL
      COMMENT '매니저 속성의 부서 코드 (적용 범위)' AFTER OWNER_USERNAME;

-- 인덱스 추가
CREATE INDEX IDX_PROP_OWNER_TYPE ON TB_PROPERTY_DEF (OWNER_TYPE);
CREATE INDEX IDX_PROP_OWNER_USER ON TB_PROPERTY_DEF (OWNER_USERNAME);
CREATE INDEX IDX_PROP_OWNER_DEPT ON TB_PROPERTY_DEF (OWNER_DEPT_CODE);
```

**변경된 TB_PROPERTY_DEF 구조:**

| 컬럼 | 타입 | 설명 |
|------|------|------|
| PROPERTY_ID | BIGINT PK | 속성 ID |
| BOARD_ID | BIGINT NULL | 보드 ID (NULL 허용) |
| **OWNER_TYPE** | VARCHAR(20) | GLOBAL / MANAGER / USER |
| **OWNER_USERNAME** | VARCHAR(50) | 소유자 (GLOBAL은 NULL) |
| **OWNER_DEPT_CODE** | VARCHAR(20) | 매니저 속성 적용 부서 |
| PROPERTY_NAME | VARCHAR(100) | 속성명 |
| PROPERTY_TYPE | VARCHAR(20) | TEXT/NUMBER/DATE/SELECT 등 |
| REQUIRED_YN | CHAR(1) | 필수 여부 |
| VISIBLE_YN | CHAR(1) | 표시 여부 |
| USE_YN | CHAR(1) | 사용 여부 |
| SORT_ORDER | INT | 정렬 순서 |
| CREATED_AT | DATETIME | 생성일시 |
| CREATED_BY | VARCHAR(50) | 생성자 |
| UPDATED_AT | DATETIME | 수정일시 |
| UPDATED_BY | VARCHAR(50) | 수정자 |

### 4.2 TB_CATEGORY (신규)

```sql
CREATE TABLE TB_CATEGORY (
    CATEGORY_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '카테고리 ID',
    CATEGORY_CODE VARCHAR(50) NOT NULL COMMENT '카테고리 코드',
    CATEGORY_NAME VARCHAR(100) NOT NULL COMMENT '카테고리명',
    DESCRIPTION VARCHAR(500) NULL COMMENT '설명',
    CATEGORY_COLOR VARCHAR(7) NULL COMMENT '색상 코드 (#RRGGBB)',
    OWNER_USERNAME VARCHAR(50) NOT NULL COMMENT '소유자 USERNAME',
    SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    USE_YN CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR(50) NOT NULL,
    UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    UPDATED_BY VARCHAR(50) NULL,
    PRIMARY KEY (CATEGORY_ID),
    UNIQUE KEY UK_CATEGORY_CODE (OWNER_USERNAME, CATEGORY_CODE),
    INDEX IDX_CATEGORY_OWNER (OWNER_USERNAME, USE_YN),
    FOREIGN KEY (OWNER_USERNAME) REFERENCES TB_USER(USERNAME) ON DELETE CASCADE
) COMMENT '카테고리 (속성 그룹)';
```

### 4.3 TB_CATEGORY_PROPERTY (신규)

```sql
CREATE TABLE TB_CATEGORY_PROPERTY (
    CATEGORY_PROPERTY_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
    CATEGORY_ID BIGINT NOT NULL COMMENT '카테고리 ID',
    PROPERTY_ID BIGINT NOT NULL COMMENT '속성 ID',
    SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '속성 표시 순서',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR(50) NOT NULL,
    PRIMARY KEY (CATEGORY_PROPERTY_ID),
    UNIQUE KEY UK_CAT_PROP (CATEGORY_ID, PROPERTY_ID),
    INDEX IDX_CAT_PROP_CAT (CATEGORY_ID),
    INDEX IDX_CAT_PROP_PROP (PROPERTY_ID),
    FOREIGN KEY (CATEGORY_ID) REFERENCES TB_CATEGORY(CATEGORY_ID) ON DELETE CASCADE,
    FOREIGN KEY (PROPERTY_ID) REFERENCES TB_PROPERTY_DEF(PROPERTY_ID) ON DELETE CASCADE
) COMMENT '카테고리-속성 매핑';
```

### 4.4 TB_BOARD_CATEGORY (신규)

```sql
CREATE TABLE TB_BOARD_CATEGORY (
    BOARD_CATEGORY_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
    BOARD_ID BIGINT NOT NULL COMMENT '보드 ID',
    CATEGORY_ID BIGINT NOT NULL COMMENT '카테고리 ID',
    DEFAULT_YN CHAR(1) NOT NULL DEFAULT 'N' COMMENT '기본 카테고리 여부',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR(50) NOT NULL,
    PRIMARY KEY (BOARD_CATEGORY_ID),
    UNIQUE KEY UK_BOARD_CAT (BOARD_ID, CATEGORY_ID),
    INDEX IDX_BOARD_CAT_BOARD (BOARD_ID),
    INDEX IDX_BOARD_CAT_CAT (CATEGORY_ID),
    FOREIGN KEY (BOARD_ID) REFERENCES TB_BOARD(BOARD_ID) ON DELETE CASCADE,
    FOREIGN KEY (CATEGORY_ID) REFERENCES TB_CATEGORY(CATEGORY_ID) ON DELETE RESTRICT
) COMMENT '보드-카테고리 매핑';
```

### 4.5 TB_BOARD_PROPERTY (신규)

```sql
CREATE TABLE TB_BOARD_PROPERTY (
    BOARD_PROPERTY_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
    BOARD_ID BIGINT NOT NULL COMMENT '보드 ID',
    PROPERTY_ID BIGINT NOT NULL COMMENT '속성 ID',
    REQUIRED_YN CHAR(1) NOT NULL DEFAULT 'N' COMMENT '필수 여부 (보드 레벨)',
    VISIBLE_YN CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '표시 여부',
    SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '표시 순서',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR(50) NOT NULL,
    PRIMARY KEY (BOARD_PROPERTY_ID),
    UNIQUE KEY UK_BOARD_PROP (BOARD_ID, PROPERTY_ID),
    INDEX IDX_BOARD_PROP_BOARD (BOARD_ID),
    INDEX IDX_BOARD_PROP_PROP (PROPERTY_ID),
    FOREIGN KEY (BOARD_ID) REFERENCES TB_BOARD(BOARD_ID) ON DELETE CASCADE,
    FOREIGN KEY (PROPERTY_ID) REFERENCES TB_PROPERTY_DEF(PROPERTY_ID) ON DELETE CASCADE
) COMMENT '보드-속성 매핑 (보드에서 선택된 속성)';
```

### 4.6 TB_ITEM 수정

```sql
-- 카테고리 컬럼 추가
ALTER TABLE TB_ITEM
  ADD COLUMN CATEGORY_ID BIGINT NULL COMMENT '업무 카테고리 ID' AFTER GROUP_ID;

-- 외래키 추가
ALTER TABLE TB_ITEM
  ADD CONSTRAINT FK_ITEM_CATEGORY
  FOREIGN KEY (CATEGORY_ID) REFERENCES TB_CATEGORY(CATEGORY_ID) ON DELETE SET NULL;

-- 인덱스 추가
CREATE INDEX IDX_ITEM_CATEGORY ON TB_ITEM (CATEGORY_ID);
```

---

## 5. ERD (Entity Relationship Diagram)

```
TB_USER (사용자)
    │
    ├──< TB_PROPERTY_DEF (속성 정의)
    │       OWNER_TYPE: GLOBAL/MANAGER/USER
    │       OWNER_USERNAME (USER/MANAGER)
    │       OWNER_DEPT_CODE (MANAGER)
    │       │
    │       ├──< TB_PROPERTY_OPTION (속성 옵션)
    │       │
    │       ├──< TB_CATEGORY_PROPERTY (카테고리-속성)
    │       │
    │       ├──< TB_BOARD_PROPERTY (보드-속성)
    │       │
    │       └──< TB_ITEM_PROPERTY (업무-속성값)
    │
    ├──< TB_CATEGORY (카테고리)
    │       OWNER_USERNAME
    │       │
    │       ├──< TB_CATEGORY_PROPERTY (카테고리-속성 매핑)
    │       │       PROPERTY_ID ──> TB_PROPERTY_DEF
    │       │
    │       ├──< TB_BOARD_CATEGORY (보드-카테고리)
    │       │       BOARD_ID ──> TB_BOARD
    │       │
    │       └──< TB_ITEM (업무)
    │               CATEGORY_ID
    │
    └──< TB_BOARD (보드)
            │
            ├──< TB_BOARD_CATEGORY (보드-카테고리)
            │       CATEGORY_ID ──> TB_CATEGORY
            │       DEFAULT_YN
            │
            ├──< TB_BOARD_PROPERTY (보드-속성 선택)
            │       PROPERTY_ID ──> TB_PROPERTY_DEF
            │
            └──< TB_ITEM (업무)
                    CATEGORY_ID ──> TB_CATEGORY
                    │
                    └──< TB_ITEM_PROPERTY (속성값)
                            PROPERTY_ID ──> TB_PROPERTY_DEF
```

---

## 6. API 설계

### 6.1 속성 관리 API

```
# 글로벌 속성 (ADMIN 전용)
GET    /api/properties/global              # 글로벌 속성 목록
POST   /api/properties/global              # 글로벌 속성 생성
PUT    /api/properties/global/{id}         # 글로벌 속성 수정
DELETE /api/properties/global/{id}         # 글로벌 속성 삭제

# 매니저 속성 (MANAGER 전용)
GET    /api/properties/manager             # 매니저 속성 목록
POST   /api/properties/manager             # 매니저 속성 생성
PUT    /api/properties/manager/{id}        # 매니저 속성 수정
DELETE /api/properties/manager/{id}        # 매니저 속성 삭제

# 사용자 속성
GET    /api/properties/user                # 사용자 속성 목록
POST   /api/properties/user                # 사용자 속성 생성
PUT    /api/properties/user/{id}           # 사용자 속성 수정
DELETE /api/properties/user/{id}           # 사용자 속성 삭제

# 사용 가능한 전체 속성 조회
GET    /api/properties/available           # 현재 사용자가 사용 가능한 모든 속성
```

### 6.2 카테고리 관리 API

```
GET    /api/categories                     # 카테고리 목록
POST   /api/categories                     # 카테고리 생성
GET    /api/categories/{id}                # 카테고리 상세
PUT    /api/categories/{id}                # 카테고리 수정
DELETE /api/categories/{id}                # 카테고리 삭제

# 카테고리-속성 매핑
GET    /api/categories/{id}/properties     # 카테고리 속성 목록
POST   /api/categories/{id}/properties     # 카테고리에 속성 추가
DELETE /api/categories/{id}/properties/{propId}  # 카테고리에서 속성 제거
PUT    /api/categories/{id}/properties/order     # 속성 순서 변경
```

### 6.3 보드-카테고리/속성 API

```
# 보드-카테고리
GET    /api/boards/{id}/categories         # 보드 카테고리 목록
POST   /api/boards/{id}/categories         # 보드에 카테고리 추가
PUT    /api/boards/{id}/categories/default # 기본 카테고리 설정
DELETE /api/boards/{id}/categories/{catId} # 보드에서 카테고리 제거

# 보드-속성 (선택된 속성)
GET    /api/boards/{id}/properties         # 보드 선택 속성 목록
POST   /api/boards/{id}/properties         # 보드에 속성 선택 등록
DELETE /api/boards/{id}/properties/{propId} # 보드에서 속성 제거
PUT    /api/boards/{id}/properties/order   # 속성 순서 변경
```

---

## 7. 역할별 기능

### 7.1 ADMIN (관리자)

| 기능 | 설명 |
|------|------|
| 글로벌 속성 CRUD | 전체 사용자에게 적용되는 속성 관리 |
| 글로벌 속성 강제 적용 | 특정 부서/사용자의 보드/업무에 속성 강제 등록 |
| 전체 속성 조회 | 모든 유형의 속성 조회 가능 |

### 7.2 MANAGER (매니저)

| 기능 | 설명 |
|------|------|
| 사용자 속성 CRUD | 본인용 속성 관리 |
| 매니저 속성 CRUD | 본인+하위부서 범위 속성 관리 |
| 카테고리 관리 | 본인 카테고리 생성/수정/삭제 |
| 속성/카테고리 이관 | 다른 매니저에게 이관 |

### 7.3 USER (일반 사용자)

| 기능 | 설명 |
|------|------|
| 사용자 속성 CRUD | 본인용 속성 관리 |
| 카테고리 관리 | 본인 카테고리 생성/수정/삭제 |
| 속성 사용 | 글로벌 + 매니저 + 사용자 속성 사용 가능 |

---

## 8. UI/UX 설계

### 8.1 속성 표시 구분

속성 유형별 시각적 구분 (뱃지/색상):

| 유형 | 뱃지 | 색상 | 제거 가능 |
|------|------|------|----------|
| 기본 속성 | [기본] | 회색 | X |
| 글로벌 속성 | [전역] | 파랑 | O |
| 매니저 속성 | [부서] | 녹색 | O |
| 사용자 속성 | [개인] | 보라 | O |

### 8.2 보드 생성 화면

```
┌─────────────────────────────────────────────────────────┐
│ 새 보드 만들기                                          │
├─────────────────────────────────────────────────────────┤
│ 보드명: [________________________]                      │
│                                                         │
│ 카테고리 선택: [개발업무 ▼]                              │
│                                                         │
│ 적용할 속성:                                            │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ [기본] 상태        ✓ (필수)                         │ │
│ │ [기본] 우선순위    ✓ (필수)                         │ │
│ │ [기본] 시작시간    ✓ (필수)                         │ │
│ │ [기본] 완료예정    ✓ (필수)                         │ │
│ │ [전역] 진행률      ☑ (선택)                         │ │
│ │ [부서] 검토상태    ☑ (선택)                         │ │
│ │ [개인] 메모        ☐ (선택)                         │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                         │
│                              [취소]  [생성]             │
└─────────────────────────────────────────────────────────┘
```

### 8.3 업무 생성 화면 (카테고리 변경 시)

```
┌─────────────────────────────────────────────────────────┐
│ 새 업무 등록                                            │
├─────────────────────────────────────────────────────────┤
│ 카테고리: [개발업무 ▼]  ← 보드 기본 카테고리 상속        │
│                        [다른 카테고리로 변경]            │
│                                                         │
│ 업무 내용: [________________________]                   │
│                                                         │
│ 속성:  (보드에서 선택된 속성만 표시)                     │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ [기본] 상태:       [시작전 ▼]                       │ │
│ │ [기본] 우선순위:   [보통 ▼]                         │ │
│ │ [전역] 진행률:     [0] %                            │ │
│ │ [부서] 검토상태:   [미검토 ▼]                       │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                         │
│                              [취소]  [등록]             │
└─────────────────────────────────────────────────────────┘
```

---

## 9. 이관 기능

### 9.1 속성 이관

```
대상: 사용자 속성, 매니저 속성
조건:
  - 사용자 속성 → 다른 사용자에게 이관
  - 매니저 속성 → 다른 MANAGER 역할 사용자에게 이관
동작:
  - OWNER_USERNAME 변경
  - 매니저 속성의 경우 OWNER_DEPT_CODE도 변경
```

### 9.2 카테고리 이관

```
대상: 사용자 카테고리
조건: 다른 사용자에게 이관
동작:
  - OWNER_USERNAME 변경
  - 카테고리에 연결된 사용자 속성도 함께 이관 (선택)
```

---

## 10. 구현 순서

### Phase 1: DB 스키마 변경
1. TB_PROPERTY_DEF 수정 (OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE 추가)
2. TB_CATEGORY 생성
3. TB_CATEGORY_PROPERTY 생성
4. TB_BOARD_CATEGORY 생성
5. TB_BOARD_PROPERTY 생성
6. TB_ITEM 수정 (CATEGORY_ID 추가)
7. 초기 데이터 (02_init_data.sql) 수정

### Phase 2: Backend 구현
1. Domain 클래스 추가/수정
2. Mapper 인터페이스 및 XML 추가/수정
3. Service 레이어 구현
4. Controller API 구현
5. 권한 체크 로직 구현

### Phase 3: Frontend 구현
1. 타입 정의 추가
2. API 클라이언트 추가
3. Store 추가 (category, property 확장)
4. 속성 관리 UI 수정 (글로벌/매니저/사용자 탭)
5. 카테고리 관리 UI 추가
6. 보드 생성/수정 UI 수정
7. 업무 생성/수정 UI 수정

### Phase 4: 테스트 및 검증
1. 단위 테스트
2. 통합 테스트
3. 권한 테스트
4. UI/UX 검증

---

## 11. 미결 사항

| 항목 | 상태 | 비고 |
|------|------|------|
| 지연 업무 판정 기준 | 별도 프로젝트 | END_TIME 외 확장 방안 검토 중 |
| 속성 옵션 공유 | 미정 | 글로벌 속성의 옵션도 공유할지 |
| 카테고리 공유 | 미정 | 카테고리를 다른 사용자와 공유할지 |

---

## 12. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2025-01-04 | - | 최초 작성 |
