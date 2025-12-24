# TaskFlow DB-Backend 불일치 분석 보고서

**작성일**: 2025-12-16
**목적**: DB Schema와 Backend Mapper XML 간 불일치 항목 전수 조사 및 표준화 방안 제시

---

## 1. 불일치 유형 분류

| 유형 | 설명 | 조치 방안 |
|------|------|----------|
| **A. 컬럼명 불일치** | 같은 목적의 컬럼이 다른 이름으로 참조됨 | Source를 DB 기준으로 수정 |
| **B. 컬럼 누락 (Source에서 참조하나 DB에 없음)** | Mapper에서 사용하나 DB에 없는 컬럼 | 기능 요구사항 검토 후 DB 추가 또는 Source 제거 |
| **C. 컬럼 누락 (DB에 있으나 Source에서 미사용)** | DB에 존재하나 활용되지 않는 컬럼 | 기능 요구사항에 따라 Source 반영 |
| **D. 테이블명 불일치** | 참조 테이블명이 다름 | Source를 DB 기준으로 수정 |

---

## 2. 테이블별 불일치 상세

### 2.1 TB_BOARD (보드)

#### DB Schema 컬럼
```sql
BOARD_ID, BOARD_NAME, DESCRIPTION, OWNER_ID, USE_YN,
CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY
```

#### BoardMapper.xml 참조 컬럼
```xml
BOARD_ID, BOARD_NAME, BOARD_DESC, OWNER_ID, DEFAULT_VIEW, BOARD_COLOR,
SORT_ORDER, USE_YN, CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY
```

#### 불일치 항목

| # | 유형 | Mapper 참조 | DB 컬럼 | 조치 방안 |
|---|------|-------------|---------|----------|
| 1 | A | `BOARD_DESC` | `DESCRIPTION` | Mapper를 `DESCRIPTION`으로 수정 |
| 2 | B | `DEFAULT_VIEW` | 없음 | DB에 추가 필요 (보드 기본 뷰 설정: TABLE/KANBAN/LIST) |
| 3 | B | `BOARD_COLOR` | 없음 | DB에 추가 필요 (보드 색상: VARCHAR(7) #RRGGBB) |
| 4 | B | `SORT_ORDER` | 없음 | DB에 추가 필요 (정렬 순서: INT) |

**권장 DB 추가 컬럼:**
```sql
ALTER TABLE TB_BOARD ADD COLUMN DEFAULT_VIEW VARCHAR(20) DEFAULT 'TABLE' COMMENT '기본 뷰 타입 (TABLE/KANBAN/LIST)' AFTER OWNER_ID;
ALTER TABLE TB_BOARD ADD COLUMN COLOR VARCHAR(7) NULL COMMENT '보드 색상 (#RRGGBB)' AFTER DEFAULT_VIEW;
ALTER TABLE TB_BOARD ADD COLUMN SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '정렬 순서' AFTER COLOR;
```

---

### 2.2 TB_GROUP (그룹)

#### DB Schema 컬럼
```sql
GROUP_ID, GROUP_CODE, GROUP_NAME, DESCRIPTION, COLOR, SORT_ORDER, USE_YN,
CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY
```

#### GroupMapper.xml 참조 컬럼
```xml
GROUP_ID, GROUP_CODE, GROUP_NAME, GROUP_DESC, GROUP_COLOR, SORT_ORDER, USE_YN,
CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY
```

#### 불일치 항목

| # | 유형 | Mapper 참조 | DB 컬럼 | 조치 방안 |
|---|------|-------------|---------|----------|
| 1 | A | `GROUP_DESC` | `DESCRIPTION` | Mapper를 `DESCRIPTION`으로 수정 |
| 2 | A | `GROUP_COLOR` | `COLOR` | Mapper를 `COLOR`로 수정 |

---

### 2.3 TB_USER (사용자)

#### DB Schema 컬럼
```sql
USER_ID, USERNAME, PASSWORD, NAME, DEPARTMENT_ID, USE_YN, LAST_LOGIN_AT,
CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY
```

#### UserMapper.xml 참조
- 정상: `NAME` 사용 ✓

#### 다른 Mapper에서 JOIN 참조 (ItemMapper.xml, CommentMapper.xml)
```xml
ua.USER_NAME AS ASSIGNEE_NAME   -- ItemMapper.xml
uc.USER_NAME AS CREATED_BY_NAME -- CommentMapper.xml
```

#### 불일치 항목

| # | 유형 | Mapper 참조 | DB 컬럼 | 조치 방안 |
|---|------|-------------|---------|----------|
| 1 | A | `USER_NAME` (ItemMapper, CommentMapper) | `NAME` | Mapper를 `NAME`으로 수정 |

#### 미사용 컬럼 (C유형)

| DB 컬럼 | 상태 | 조치 방안 |
|---------|------|----------|
| `LAST_LOGIN_AT` | Mapper에서 미참조 | 로그인 시 업데이트 로직 추가 권장 |

---

### 2.4 TB_PROPERTY_DEF (속성 정의)

#### DB Schema 컬럼
```sql
PROPERTY_ID, BOARD_ID, PROPERTY_NAME, PROPERTY_TYPE, REQUIRED_YN, VISIBLE_YN, SORT_ORDER,
CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY
```

#### PropertyDefMapper.xml 참조 컬럼
```xml
PROPERTY_ID, BOARD_ID, PROPERTY_NAME, PROPERTY_CODE, PROPERTY_TYPE, REQUIRED_YN,
DEFAULT_VALUE, SORT_ORDER, HIDDEN_YN, SYSTEM_YN, USE_YN,
CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY
```

#### 불일치 항목

| # | 유형 | Mapper 참조 | DB 컬럼 | 조치 방안 |
|---|------|-------------|---------|----------|
| 1 | B | `PROPERTY_CODE` | 없음 | DB에 추가 필요 (속성 코드: VARCHAR(50) UNIQUE) |
| 2 | B | `DEFAULT_VALUE` | 없음 | DB에 추가 필요 (기본값: VARCHAR(500)) |
| 3 | A | `HIDDEN_YN` | `VISIBLE_YN` | 의미 반대 - Mapper를 `VISIBLE_YN`으로 수정, 로직 반전 필요 |
| 4 | B | `SYSTEM_YN` | 없음 | DB에 추가 필요 (시스템 속성 여부: CHAR(1) DEFAULT 'N') |
| 5 | B | `USE_YN` | 없음 | DB에 추가 필요 (사용 여부: CHAR(1) DEFAULT 'Y') |

**권장 DB 추가/수정:**
```sql
ALTER TABLE TB_PROPERTY_DEF ADD COLUMN PROPERTY_CODE VARCHAR(50) NOT NULL COMMENT '속성 코드' AFTER PROPERTY_NAME;
ALTER TABLE TB_PROPERTY_DEF ADD COLUMN DEFAULT_VALUE VARCHAR(500) NULL COMMENT '기본값' AFTER REQUIRED_YN;
ALTER TABLE TB_PROPERTY_DEF ADD COLUMN SYSTEM_YN CHAR(1) NOT NULL DEFAULT 'N' COMMENT '시스템 속성 여부 (Y/N)' AFTER SORT_ORDER;
ALTER TABLE TB_PROPERTY_DEF ADD COLUMN USE_YN CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '사용 여부 (Y/N)' AFTER SYSTEM_YN;
-- VISIBLE_YN은 유지하되 HIDDEN_YN 참조를 VISIBLE_YN으로 변경
```

---

### 2.5 TB_PROPERTY_OPTION (속성 옵션)

#### DB Schema 컬럼
```sql
OPTION_ID, PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN,
CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY
```

#### PropertyOptionMapper.xml 참조 컬럼
```xml
OPTION_ID, PROPERTY_ID, OPTION_CODE, OPTION_NAME, COLOR, SORT_ORDER, USE_YN,
CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY
```

#### 불일치 항목

| # | 유형 | Mapper 참조 | DB 컬럼 | 조치 방안 |
|---|------|-------------|---------|----------|
| 1 | B | `OPTION_CODE` | 없음 | DB에 추가 필요 (옵션 코드: VARCHAR(50)) |
| 2 | A | `OPTION_NAME` | `OPTION_LABEL` | **선택 필요**: Mapper를 `OPTION_LABEL`로 수정 OR DB 컬럼명 변경 |

**권장 DB 추가:**
```sql
ALTER TABLE TB_PROPERTY_OPTION ADD COLUMN OPTION_CODE VARCHAR(50) NULL COMMENT '옵션 코드' AFTER PROPERTY_ID;
-- OPTION_LABEL을 유지 - Mapper에서 OPTION_NAME을 OPTION_LABEL로 변경
```

---

### 2.6 TB_ITEM (업무 아이템)

#### DB Schema 컬럼
```sql
ITEM_ID, BOARD_ID, CONTENT, STATUS, PRIORITY, CATEGORY_ID, GROUP_ID, ASSIGNEE_ID,
START_TIME, END_TIME, DELETED_AT,
CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY
```

#### ItemMapper.xml 참조 컬럼
```xml
ITEM_ID, BOARD_ID, GROUP_ID, TITLE, CONTENT, STATUS, PREVIOUS_STATUS, PRIORITY,
ASSIGNEE_ID, START_TIME, END_TIME, DUE_DATE, SORT_ORDER,
COMPLETED_AT, COMPLETED_BY, DELETED_AT, DELETED_BY,
CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY
```

#### 불일치 항목

| # | 유형 | Mapper 참조 | DB 컬럼 | 조치 방안 |
|---|------|-------------|---------|----------|
| 1 | B | `TITLE` | 없음 | DB에 추가 필요 (업무 제목: VARCHAR(200)) - CONTENT는 상세 내용용 |
| 2 | B | `PREVIOUS_STATUS` | 없음 | DB에 추가 필요 (삭제 전 상태: VARCHAR(20)) - 복원 기능용 |
| 3 | B | `SORT_ORDER` | 없음 | DB에 추가 필요 (정렬 순서: INT) |
| 4 | B | `DUE_DATE` | 없음 | DB에 추가 필요 (마감일: DATE) |
| 5 | B | `COMPLETED_AT` | 없음 | DB에 추가 필요 (완료 시간: DATETIME) |
| 6 | B | `COMPLETED_BY` | 없음 | DB에 추가 필요 (완료 처리자: BIGINT FK) |
| 7 | B | `DELETED_BY` | 없음 | DB에 추가 필요 (삭제 처리자: BIGINT FK) |
| 8 | C | - | `CATEGORY_ID` | Mapper에서 미사용 - 동적 속성으로 대체 검토 |

**권장 DB 추가:**
```sql
ALTER TABLE TB_ITEM ADD COLUMN TITLE VARCHAR(200) NULL COMMENT '업무 제목' AFTER BOARD_ID;
ALTER TABLE TB_ITEM ADD COLUMN PREVIOUS_STATUS VARCHAR(20) NULL COMMENT '삭제 전 상태 (복원용)' AFTER STATUS;
ALTER TABLE TB_ITEM ADD COLUMN SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '정렬 순서' AFTER ASSIGNEE_ID;
ALTER TABLE TB_ITEM ADD COLUMN DUE_DATE DATE NULL COMMENT '마감일' AFTER END_TIME;
ALTER TABLE TB_ITEM ADD COLUMN COMPLETED_AT DATETIME NULL COMMENT '완료 시간' AFTER DUE_DATE;
ALTER TABLE TB_ITEM ADD COLUMN COMPLETED_BY BIGINT NULL COMMENT '완료 처리자 ID' AFTER COMPLETED_AT;
ALTER TABLE TB_ITEM ADD COLUMN DELETED_BY BIGINT NULL COMMENT '삭제 처리자 ID' AFTER DELETED_AT;
```

---

### 2.7 TB_ITEM_PROPERTY (아이템 속성값) - 테이블명 불일치

#### DB Schema
- 테이블명: `TB_ITEM_PROPERTY`

#### Mapper XML 참조 (PropertyDefMapper.xml, PropertyOptionMapper.xml)
- 참조 테이블명: `TB_ITEM_PROPERTY_VALUE`

#### 불일치 항목

| # | 유형 | Mapper 참조 | DB 테이블 | 조치 방안 |
|---|------|-------------|-----------|----------|
| 1 | D | `TB_ITEM_PROPERTY_VALUE` | `TB_ITEM_PROPERTY` | Mapper를 `TB_ITEM_PROPERTY`로 수정 |

---

### 2.8 TB_COMMENT (댓글) - JOIN 컬럼 불일치

#### CommentMapper.xml JOIN 참조
```xml
i.TITLE AS ITEM_TITLE          -- TB_ITEM.TITLE (없음)
uc.USER_NAME AS CREATED_BY_NAME -- TB_USER.USER_NAME (없음, NAME임)
uu.USER_NAME AS UPDATED_BY_NAME -- TB_USER.USER_NAME (없음, NAME임)
```

#### 불일치 항목

| # | 유형 | Mapper 참조 | 실제 DB | 조치 방안 |
|---|------|-------------|---------|----------|
| 1 | B | `i.TITLE` | TB_ITEM에 TITLE 없음 | TB_ITEM에 TITLE 추가 후 사용 OR CONTENT로 대체 |
| 2 | A | `uc.USER_NAME` | `NAME` | Mapper를 `uc.NAME`으로 수정 |
| 3 | A | `uu.USER_NAME` | `NAME` | Mapper를 `uu.NAME`으로 수정 |

---

## 3. 종합 불일치 현황

### 3.1 유형별 집계

| 유형 | 설명 | 건수 |
|------|------|------|
| A | 컬럼명 불일치 | 8건 |
| B | DB에 컬럼 없음 | 14건 |
| C | Source에서 미사용 | 2건 |
| D | 테이블명 불일치 | 1건 |
| **합계** | | **25건** |

### 3.2 조치 우선순위

#### 🔴 긴급 (API 오류 발생)
1. TB_GROUP: `GROUP_DESC` → `DESCRIPTION`, `GROUP_COLOR` → `COLOR`
2. TB_BOARD: `BOARD_DESC` → `DESCRIPTION`
3. TB_USER 참조: `USER_NAME` → `NAME`
4. TB_ITEM_PROPERTY_VALUE → TB_ITEM_PROPERTY (테이블명)
5. TB_PROPERTY_OPTION: `OPTION_NAME` → `OPTION_LABEL`

#### 🟡 중요 (기능 동작 필요)
1. TB_ITEM: TITLE, DUE_DATE, COMPLETED_AT/BY, DELETED_BY, PREVIOUS_STATUS, SORT_ORDER 추가
2. TB_BOARD: DEFAULT_VIEW, COLOR, SORT_ORDER 추가
3. TB_PROPERTY_DEF: PROPERTY_CODE, DEFAULT_VALUE, SYSTEM_YN, USE_YN 추가, HIDDEN_YN→VISIBLE_YN
4. TB_PROPERTY_OPTION: OPTION_CODE 추가

#### 🟢 권장 (품질 향상)
1. TB_USER: LAST_LOGIN_AT 활용 로직 추가
2. TB_ITEM: CATEGORY_ID 활용 여부 결정

---

## 4. 권장 조치 방안

### 4.1 Phase 1: DB Schema 확장 (SQL Migration)

```sql
-- ============================================
-- Phase 1: DB Schema 확장
-- ============================================

-- 1. TB_BOARD 확장
ALTER TABLE TB_BOARD ADD COLUMN DEFAULT_VIEW VARCHAR(20) DEFAULT 'TABLE' COMMENT '기본 뷰 타입 (TABLE/KANBAN/LIST)' AFTER OWNER_ID;
ALTER TABLE TB_BOARD ADD COLUMN COLOR VARCHAR(7) NULL COMMENT '보드 색상 (#RRGGBB)' AFTER DEFAULT_VIEW;
ALTER TABLE TB_BOARD ADD COLUMN SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '정렬 순서' AFTER COLOR;

-- 2. TB_PROPERTY_DEF 확장
ALTER TABLE TB_PROPERTY_DEF ADD COLUMN PROPERTY_CODE VARCHAR(50) NOT NULL DEFAULT '' COMMENT '속성 코드' AFTER PROPERTY_NAME;
ALTER TABLE TB_PROPERTY_DEF ADD COLUMN DEFAULT_VALUE VARCHAR(500) NULL COMMENT '기본값' AFTER REQUIRED_YN;
ALTER TABLE TB_PROPERTY_DEF ADD COLUMN SYSTEM_YN CHAR(1) NOT NULL DEFAULT 'N' COMMENT '시스템 속성 여부 (Y/N)' AFTER SORT_ORDER;
ALTER TABLE TB_PROPERTY_DEF ADD COLUMN USE_YN CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '사용 여부 (Y/N)' AFTER SYSTEM_YN;
-- PROPERTY_CODE 기본값 설정 (기존 데이터용)
UPDATE TB_PROPERTY_DEF SET PROPERTY_CODE = CONCAT('PROP_', PROPERTY_ID) WHERE PROPERTY_CODE = '';

-- 3. TB_PROPERTY_OPTION 확장
ALTER TABLE TB_PROPERTY_OPTION ADD COLUMN OPTION_CODE VARCHAR(50) NULL COMMENT '옵션 코드' AFTER PROPERTY_ID;

-- 4. TB_ITEM 확장
ALTER TABLE TB_ITEM ADD COLUMN TITLE VARCHAR(200) NULL COMMENT '업무 제목' AFTER BOARD_ID;
ALTER TABLE TB_ITEM ADD COLUMN PREVIOUS_STATUS VARCHAR(20) NULL COMMENT '삭제 전 상태 (복원용)' AFTER STATUS;
ALTER TABLE TB_ITEM ADD COLUMN SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '정렬 순서' AFTER ASSIGNEE_ID;
ALTER TABLE TB_ITEM ADD COLUMN DUE_DATE DATE NULL COMMENT '마감일' AFTER END_TIME;
ALTER TABLE TB_ITEM ADD COLUMN COMPLETED_AT DATETIME NULL COMMENT '완료 시간' AFTER DUE_DATE;
ALTER TABLE TB_ITEM ADD COLUMN COMPLETED_BY BIGINT NULL COMMENT '완료 처리자 ID' AFTER COMPLETED_AT;
ALTER TABLE TB_ITEM ADD COLUMN DELETED_BY BIGINT NULL COMMENT '삭제 처리자 ID' AFTER DELETED_AT;
```

### 4.2 Phase 2: Mapper XML 수정

| Mapper 파일 | 수정 항목 |
|-------------|----------|
| BoardMapper.xml | `BOARD_DESC` → `DESCRIPTION` |
| GroupMapper.xml | `GROUP_DESC` → `DESCRIPTION`, `GROUP_COLOR` → `COLOR` |
| ItemMapper.xml | `ua.USER_NAME` → `ua.NAME`, `uc.USER_NAME` → `uc.NAME`, `uu.USER_NAME` → `uu.NAME`, `g.GROUP_COLOR` → `g.COLOR` |
| CommentMapper.xml | `uc.USER_NAME` → `uc.NAME`, `uu.USER_NAME` → `uu.NAME` |
| PropertyDefMapper.xml | `HIDDEN_YN` → `VISIBLE_YN` (로직 반전), `TB_ITEM_PROPERTY_VALUE` → `TB_ITEM_PROPERTY` |
| PropertyOptionMapper.xml | `OPTION_NAME` → `OPTION_LABEL`, `TB_ITEM_PROPERTY_VALUE` → `TB_ITEM_PROPERTY` |

### 4.3 Phase 3: Domain/DTO 수정

| Domain 클래스 | 수정 항목 |
|---------------|----------|
| Board.java | `description` 필드명 유지 (Mapper alias 사용) |
| Group.java | `description`, `color` 필드명 유지 (Mapper alias 사용) |
| PropertyDef.java | `hiddenYn` → `visibleYn` 변경 |
| PropertyOption.java | `optionName` → `optionLabel` 또는 Mapper alias 사용 |

---

## 5. 결론

현재 TaskFlow 프로젝트의 DB Schema와 Backend Mapper XML 간에 **25건의 불일치**가 발견되었습니다.

- **긴급 수정 필요**: 5건 (API 오류 유발)
- **기능 구현 필요**: 14건 (DB 컬럼 추가)
- **품질 개선**: 2건 (미사용 컬럼 활용)
- **테이블명 수정**: 1건

권장하는 조치 순서:
1. **Phase 1**: DB Schema 확장 (SQL Migration 실행)
2. **Phase 2**: Mapper XML 컬럼명 수정
3. **Phase 3**: Domain/DTO 필드 정합성 확인
4. **Phase 4**: 테스트 및 검증

이 보고서의 내용을 기반으로 순차적인 수정 작업을 진행하면 DB-Backend 정합성을 확보할 수 있습니다.
