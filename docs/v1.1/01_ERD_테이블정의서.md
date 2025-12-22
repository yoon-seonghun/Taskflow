# TaskFlow v1.1 ERD 및 테이블 정의서

> 작성일: 2024-12-21
> 버전: 1.1
> 이전 버전: v1.0 (docs/erd/table-definition.md)

---

## 목차

1. [개요](#1-개요)
2. [ERD 다이어그램](#2-erd-다이어그램)
3. [신규 테이블](#3-신규-테이블)
   - 3.1 [TB_ITEM_SHARE - 업무 공유](#31-tb_item_share---업무-공유)
   - 3.2 [TB_AUDIT_LOG - 통합 감사 로그](#32-tb_audit_log---통합-감사-로그)
4. [수정 테이블](#4-수정-테이블)
   - 4.1 [TB_BOARD - 보드](#41-tb_board---보드)
   - 4.2 [TB_BOARD_SHARE - 보드 공유](#42-tb_board_share---보드-공유)
   - 4.3 [TB_ITEM - 업무 아이템](#43-tb_item---업무-아이템)
5. [마이그레이션 스크립트](#5-마이그레이션-스크립트)
6. [변경 요약](#6-변경-요약)

---

## 1. 개요

### 1.1 변경 목적
- 보드 관리 기능 강화 (정렬, 논리 삭제)
- 세분화된 권한 체계 구축 (VIEW/EDIT/FULL/OWNER)
- 업무 단위 공유 기능 추가
- 업무 이관 기능 지원
- 통합 감사 로그 시스템 구축

### 1.2 영향 범위

| 구분 | 테이블 | 변경 유형 |
|------|--------|----------|
| 신규 | TB_ITEM_SHARE | 생성 |
| 신규 | TB_AUDIT_LOG | 생성 |
| 수정 | TB_BOARD | 컬럼 추가 |
| 수정 | TB_BOARD_SHARE | 컬럼 수정, 추가 |
| 수정 | TB_ITEM | 컬럼 추가 |

---

## 2. ERD 다이어그램

### 2.1 v1.1 신규/수정 테이블 관계도

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              TaskFlow v1.1 ERD                                   │
│                         (신규 및 수정 테이블 중심)                                │
└─────────────────────────────────────────────────────────────────────────────────┘

                           ┌──────────────────┐
                           │     TB_USER      │
                           │  (기존 테이블)    │
                           └────────┬─────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                     │                     │
              ▼                     ▼                     ▼
    ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
    │    TB_BOARD      │  │  TB_BOARD_SHARE  │  │  TB_AUDIT_LOG    │
    │    [수정]        │  │     [수정]       │  │     [신규]       │
    │                  │  │                  │  │                  │
    │ + SORT_ORDER     │◄─┤ BOARD_ID (FK)    │  │ TARGET_TYPE      │
    │ + COLOR          │  │ USER_ID (FK)     │  │ TARGET_ID        │
    │ + DEFAULT_VIEW   │  │ PERMISSION       │  │ ACTION           │
    └────────┬─────────┘  │ (VIEW/EDIT/FULL) │  │ ACTOR_ID (FK)    │
             │            │ + UPDATED_AT     │  │ BEFORE_DATA      │
             │            │ + UPDATED_BY     │  │ AFTER_DATA       │
             ▼            └──────────────────┘  └──────────────────┘
    ┌──────────────────┐
    │     TB_ITEM      │
    │     [수정]       │
    │                  │
    │ + TRANSFERRED_   │
    │   FROM           │◄────────────────────────┐
    │ + TRANSFERRED_   │                         │
    │   AT             │                         │
    └────────┬─────────┘                         │
             │                                   │
             ▼                                   │
    ┌──────────────────┐                         │
    │  TB_ITEM_SHARE   │                         │
    │     [신규]       │                         │
    │                  │                         │
    │ ITEM_SHARE_ID(PK)│                         │
    │ ITEM_ID (FK)─────┼─────────────────────────┘
    │ USER_ID (FK)     │
    │ PERMISSION       │
    │ (VIEW/EDIT/FULL) │
    └──────────────────┘
```

### 2.2 권한 체계

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              권한 체계 (Permission)                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      보드 권한 (TB_BOARD_SHARE)                          │   │
│  │                                                                          │   │
│  │   OWNER ─────► FULL ─────► EDIT ─────► VIEW                             │   │
│  │     │           │           │           │                                │   │
│  │   공유관리    삭제/수정/   수정/조회    조회만                            │   │
│  │   전체권한    조회                                                       │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      업무 권한 (TB_ITEM_SHARE)                           │   │
│  │                                                                          │   │
│  │           FULL ─────► EDIT ─────► VIEW                                   │   │
│  │             │           │           │                                    │   │
│  │         삭제/수정/    수정/조회    조회만                                 │   │
│  │         조회                                                             │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 신규 테이블

### 3.1 TB_ITEM_SHARE - 업무 공유

#### 설명
개별 업무 아이템을 사용자에게 공유할 때 사용하는 테이블. 보드 전체가 아닌 특정 업무만 공유할 수 있는 기능을 지원.

#### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| ITEM_SHARE_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 업무 공유 ID (PK) |
| ITEM_ID | BIGINT | - | NOT NULL | - | 업무 ID (FK) |
| USER_ID | BIGINT | - | NOT NULL | - | 공유받는 사용자 ID (FK) |
| PERMISSION | VARCHAR | 20 | NOT NULL | 'VIEW' | 권한 (VIEW/EDIT/FULL) |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| UPDATED_AT | DATETIME | - | NULL | ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| UPDATED_BY | BIGINT | - | NULL | NULL | 수정자 |

#### PERMISSION 값

| 값 | 설명 | 조회 | 수정 | 삭제 |
|----|------|------|------|------|
| VIEW | 조회 전용 | O | X | X |
| EDIT | 편집 가능 | O | O | X |
| FULL | 전체 권한 | O | O | O |

#### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_ITEM_SHARE | PRIMARY KEY | ITEM_SHARE_ID | - |
| UK_ITEM_SHARE | UNIQUE | ITEM_ID, USER_ID | - |
| FK_ITEM_SHARE_ITEM | FOREIGN KEY | ITEM_ID | TB_ITEM(ITEM_ID) |
| FK_ITEM_SHARE_USER | FOREIGN KEY | USER_ID | TB_USER(USER_ID) |

#### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_ITEM_SHARE_ITEM | ITEM_ID | INDEX | 업무별 공유 사용자 조회 |
| IDX_ITEM_SHARE_USER | USER_ID | INDEX | 사용자별 공유 업무 조회 |

#### DDL

```sql
CREATE TABLE TB_ITEM_SHARE (
    ITEM_SHARE_ID BIGINT NOT NULL AUTO_INCREMENT,
    ITEM_ID BIGINT NOT NULL,
    USER_ID BIGINT NOT NULL,
    PERMISSION VARCHAR(20) NOT NULL DEFAULT 'VIEW',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY BIGINT NOT NULL,
    UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    UPDATED_BY BIGINT NULL,
    PRIMARY KEY (ITEM_SHARE_ID),
    UNIQUE KEY UK_ITEM_SHARE (ITEM_ID, USER_ID),
    CONSTRAINT FK_ITEM_SHARE_ITEM FOREIGN KEY (ITEM_ID) REFERENCES TB_ITEM(ITEM_ID),
    CONSTRAINT FK_ITEM_SHARE_USER FOREIGN KEY (USER_ID) REFERENCES TB_USER(USER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IDX_ITEM_SHARE_ITEM ON TB_ITEM_SHARE(ITEM_ID);
CREATE INDEX IDX_ITEM_SHARE_USER ON TB_ITEM_SHARE(USER_ID);
```

---

### 3.2 TB_AUDIT_LOG - 통합 감사 로그

#### 설명
보드, 업무, 공유 관련 모든 변경 이력을 기록하는 통합 감사 로그 테이블. JSON 형식으로 변경 전/후 데이터를 저장하여 이력 추적 가능.

#### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| LOG_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 로그 ID (PK) |
| TARGET_TYPE | VARCHAR | 30 | NOT NULL | - | 대상 유형 |
| TARGET_ID | BIGINT | NOT NULL | - | - | 대상 ID |
| ACTION | VARCHAR | 30 | NOT NULL | - | 작업 유형 |
| ACTOR_ID | BIGINT | - | NOT NULL | - | 수행자 ID (FK) |
| DESCRIPTION | VARCHAR | 500 | NULL | NULL | 변경 내용 설명 |
| BEFORE_DATA | JSON | - | NULL | NULL | 변경 전 데이터 |
| AFTER_DATA | JSON | - | NULL | NULL | 변경 후 데이터 |
| RELATED_USER_ID | BIGINT | - | NULL | NULL | 관련 사용자 ID (이관/공유 대상) |
| IP_ADDRESS | VARCHAR | 45 | NULL | NULL | 요청 IP 주소 |
| USER_AGENT | VARCHAR | 500 | NULL | NULL | 사용자 에이전트 |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |

#### TARGET_TYPE 값

| 값 | 설명 |
|----|------|
| BOARD | 보드 |
| ITEM | 업무 아이템 |
| BOARD_SHARE | 보드 공유 |
| ITEM_SHARE | 업무 공유 |

#### ACTION 값

| 값 | 설명 | 대상 유형 |
|----|------|----------|
| CREATE | 생성 | BOARD, ITEM |
| UPDATE | 수정 | BOARD, ITEM, BOARD_SHARE, ITEM_SHARE |
| DELETE | 삭제 | BOARD, ITEM |
| TRANSFER | 이관 | ITEM |
| SHARE | 공유 추가 | BOARD_SHARE, ITEM_SHARE |
| UNSHARE | 공유 제거 | BOARD_SHARE, ITEM_SHARE |
| PERMISSION_CHANGE | 권한 변경 | BOARD_SHARE, ITEM_SHARE |

#### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_AUDIT_LOG | PRIMARY KEY | LOG_ID | - |
| FK_AUDIT_LOG_ACTOR | FOREIGN KEY | ACTOR_ID | TB_USER(USER_ID) |

#### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_AUDIT_LOG_TARGET | TARGET_TYPE, TARGET_ID | INDEX | 대상별 이력 조회 |
| IDX_AUDIT_LOG_ACTOR | ACTOR_ID | INDEX | 수행자별 이력 조회 |
| IDX_AUDIT_LOG_ACTION | ACTION, CREATED_AT | INDEX | 작업별 이력 조회 |
| IDX_AUDIT_LOG_DATE | CREATED_AT | INDEX | 날짜별 이력 조회 |
| IDX_AUDIT_LOG_RELATED | RELATED_USER_ID | INDEX | 관련 사용자별 조회 |

#### DDL

```sql
CREATE TABLE TB_AUDIT_LOG (
    LOG_ID BIGINT NOT NULL AUTO_INCREMENT,
    TARGET_TYPE VARCHAR(30) NOT NULL,
    TARGET_ID BIGINT NOT NULL,
    ACTION VARCHAR(30) NOT NULL,
    ACTOR_ID BIGINT NOT NULL,
    DESCRIPTION VARCHAR(500) NULL,
    BEFORE_DATA JSON NULL,
    AFTER_DATA JSON NULL,
    RELATED_USER_ID BIGINT NULL,
    IP_ADDRESS VARCHAR(45) NULL,
    USER_AGENT VARCHAR(500) NULL,
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (LOG_ID),
    CONSTRAINT FK_AUDIT_LOG_ACTOR FOREIGN KEY (ACTOR_ID) REFERENCES TB_USER(USER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IDX_AUDIT_LOG_TARGET ON TB_AUDIT_LOG(TARGET_TYPE, TARGET_ID);
CREATE INDEX IDX_AUDIT_LOG_ACTOR ON TB_AUDIT_LOG(ACTOR_ID);
CREATE INDEX IDX_AUDIT_LOG_ACTION ON TB_AUDIT_LOG(ACTION, CREATED_AT);
CREATE INDEX IDX_AUDIT_LOG_DATE ON TB_AUDIT_LOG(CREATED_AT);
CREATE INDEX IDX_AUDIT_LOG_RELATED ON TB_AUDIT_LOG(RELATED_USER_ID);
```

---

## 4. 수정 테이블

### 4.1 TB_BOARD - 보드

#### 변경 내용

| 변경 유형 | 컬럼명 | 설명 |
|----------|--------|------|
| 추가 | SORT_ORDER | 보드 정렬 순서 |
| 추가 | COLOR | 보드 색상 |
| 추가 | DEFAULT_VIEW_TYPE | 기본 뷰 타입 |

#### 추가 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| SORT_ORDER | INT | - | NOT NULL | 0 | 정렬 순서 |
| COLOR | VARCHAR | 7 | NULL | NULL | 색상 코드 (#RRGGBB) |
| DEFAULT_VIEW_TYPE | VARCHAR | 20 | NOT NULL | 'TABLE' | 기본 뷰 타입 (TABLE/KANBAN/LIST) |

#### DEFAULT_VIEW_TYPE 값

| 값 | 설명 |
|----|------|
| TABLE | 테이블 뷰 |
| KANBAN | 칸반 뷰 |
| LIST | 리스트 뷰 |

#### 수정 후 전체 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| BOARD_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 보드 ID (PK) |
| BOARD_NAME | VARCHAR | 200 | NOT NULL | - | 보드명 |
| DESCRIPTION | VARCHAR | 500 | NULL | NULL | 보드 설명 |
| OWNER_ID | BIGINT | - | NOT NULL | - | 소유자 ID (FK) |
| **SORT_ORDER** | INT | - | NOT NULL | 0 | 정렬 순서 [신규] |
| **COLOR** | VARCHAR | 7 | NULL | NULL | 색상 코드 [신규] |
| **DEFAULT_VIEW_TYPE** | VARCHAR | 20 | NOT NULL | 'TABLE' | 기본 뷰 타입 [신규] |
| USE_YN | CHAR | 1 | NOT NULL | 'Y' | 사용 여부 (Y/N) |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| UPDATED_AT | DATETIME | - | NULL | ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| UPDATED_BY | BIGINT | - | NULL | NULL | 수정자 |

#### 추가 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_BOARD_SORT | OWNER_ID, SORT_ORDER | INDEX | 소유자별 정렬 조회 |

#### ALTER 스크립트

```sql
-- 컬럼 추가
ALTER TABLE TB_BOARD
    ADD COLUMN SORT_ORDER INT NOT NULL DEFAULT 0 AFTER OWNER_ID,
    ADD COLUMN COLOR VARCHAR(7) NULL AFTER SORT_ORDER,
    ADD COLUMN DEFAULT_VIEW_TYPE VARCHAR(20) NOT NULL DEFAULT 'TABLE' AFTER COLOR;

-- 인덱스 추가
CREATE INDEX IDX_BOARD_SORT ON TB_BOARD(OWNER_ID, SORT_ORDER);
```

---

### 4.2 TB_BOARD_SHARE - 보드 공유

#### 변경 내용

| 변경 유형 | 컬럼명 | 설명 |
|----------|--------|------|
| 수정 | PERMISSION | 값 변경: MEMBER/VIEWER → VIEW/EDIT/FULL |
| 추가 | UPDATED_AT | 수정일시 |
| 추가 | UPDATED_BY | 수정자 |

#### PERMISSION 값 변경

| 이전 값 | 신규 값 | 설명 |
|---------|---------|------|
| VIEWER | VIEW | 조회 전용 |
| MEMBER | EDIT | 편집 가능 |
| (없음) | FULL | 전체 권한 |

#### 수정 후 전체 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| BOARD_SHARE_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 공유 ID (PK) |
| BOARD_ID | BIGINT | - | NOT NULL | - | 보드 ID (FK) |
| USER_ID | BIGINT | - | NOT NULL | - | 공유 사용자 ID (FK) |
| PERMISSION | VARCHAR | 20 | NOT NULL | 'VIEW' | 권한 [값 변경] |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| **UPDATED_AT** | DATETIME | - | NULL | ON UPDATE CURRENT_TIMESTAMP | 수정일시 [신규] |
| **UPDATED_BY** | BIGINT | - | NULL | NULL | 수정자 [신규] |

#### ALTER 스크립트

```sql
-- 컬럼 추가
ALTER TABLE TB_BOARD_SHARE
    ADD COLUMN UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP AFTER CREATED_BY,
    ADD COLUMN UPDATED_BY BIGINT NULL AFTER UPDATED_AT;

-- 기존 데이터 마이그레이션
UPDATE TB_BOARD_SHARE SET PERMISSION = 'VIEW' WHERE PERMISSION = 'VIEWER';
UPDATE TB_BOARD_SHARE SET PERMISSION = 'EDIT' WHERE PERMISSION = 'MEMBER';

-- 기본값 변경
ALTER TABLE TB_BOARD_SHARE
    ALTER COLUMN PERMISSION SET DEFAULT 'VIEW';
```

---

### 4.3 TB_ITEM - 업무 아이템

#### 변경 내용

| 변경 유형 | 컬럼명 | 설명 |
|----------|--------|------|
| 추가 | TRANSFERRED_FROM | 이관 원본 보드 ID |
| 추가 | TRANSFERRED_AT | 이관 일시 |

#### 추가 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| TRANSFERRED_FROM | BIGINT | - | NULL | NULL | 이관 원본 보드 ID (FK) |
| TRANSFERRED_AT | DATETIME | - | NULL | NULL | 이관 일시 |

#### 수정 후 전체 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| ITEM_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 아이템 ID (PK) |
| BOARD_ID | BIGINT | - | NOT NULL | - | 보드 ID (FK) |
| CONTENT | VARCHAR | 500 | NOT NULL | - | 업무 내용 |
| STATUS | VARCHAR | 20 | NOT NULL | 'NOT_STARTED' | 상태 |
| PRIORITY | VARCHAR | 20 | NOT NULL | 'NORMAL' | 우선순위 |
| CATEGORY_ID | BIGINT | - | NULL | NULL | 카테고리 옵션 ID (FK) |
| GROUP_ID | BIGINT | - | NULL | NULL | 그룹 ID (FK) |
| ASSIGNEE_ID | BIGINT | - | NULL | NULL | 담당자 ID (FK) |
| START_TIME | DATETIME | - | NULL | NULL | 시작 시간 |
| END_TIME | DATETIME | - | NULL | NULL | 완료 시간 |
| DELETED_AT | DATETIME | - | NULL | NULL | 삭제 시간 |
| **TRANSFERRED_FROM** | BIGINT | - | NULL | NULL | 이관 원본 보드 ID [신규] |
| **TRANSFERRED_AT** | DATETIME | - | NULL | NULL | 이관 일시 [신규] |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| UPDATED_AT | DATETIME | - | NULL | ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| UPDATED_BY | BIGINT | - | NULL | NULL | 수정자 |

#### 추가 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| FK_ITEM_TRANSFERRED | FOREIGN KEY | TRANSFERRED_FROM | TB_BOARD(BOARD_ID) |

#### 추가 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_ITEM_TRANSFERRED | TRANSFERRED_FROM | INDEX | 이관 업무 조회 |

#### ALTER 스크립트

```sql
-- 컬럼 추가
ALTER TABLE TB_ITEM
    ADD COLUMN TRANSFERRED_FROM BIGINT NULL AFTER DELETED_AT,
    ADD COLUMN TRANSFERRED_AT DATETIME NULL AFTER TRANSFERRED_FROM;

-- 외래키 추가
ALTER TABLE TB_ITEM
    ADD CONSTRAINT FK_ITEM_TRANSFERRED FOREIGN KEY (TRANSFERRED_FROM) REFERENCES TB_BOARD(BOARD_ID);

-- 인덱스 추가
CREATE INDEX IDX_ITEM_TRANSFERRED ON TB_ITEM(TRANSFERRED_FROM);
```

---

## 5. 마이그레이션 스크립트

### 5.1 전체 마이그레이션 스크립트

```sql
-- ============================================
-- TaskFlow v1.1 마이그레이션 스크립트
-- 실행 전 백업 필수!
-- ============================================

-- 1. TB_BOARD 수정
ALTER TABLE TB_BOARD
    ADD COLUMN SORT_ORDER INT NOT NULL DEFAULT 0 AFTER OWNER_ID,
    ADD COLUMN COLOR VARCHAR(7) NULL AFTER SORT_ORDER,
    ADD COLUMN DEFAULT_VIEW_TYPE VARCHAR(20) NOT NULL DEFAULT 'TABLE' AFTER COLOR;

CREATE INDEX IDX_BOARD_SORT ON TB_BOARD(OWNER_ID, SORT_ORDER);

-- 기존 보드에 SORT_ORDER 초기화
SET @row_num = 0;
UPDATE TB_BOARD b
SET b.SORT_ORDER = (@row_num := @row_num + 1)
WHERE b.USE_YN = 'Y'
ORDER BY b.CREATED_AT;

-- 2. TB_BOARD_SHARE 수정
ALTER TABLE TB_BOARD_SHARE
    ADD COLUMN UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP AFTER CREATED_BY,
    ADD COLUMN UPDATED_BY BIGINT NULL AFTER UPDATED_AT;

-- 기존 PERMISSION 값 마이그레이션
UPDATE TB_BOARD_SHARE SET PERMISSION = 'VIEW' WHERE PERMISSION = 'VIEWER';
UPDATE TB_BOARD_SHARE SET PERMISSION = 'EDIT' WHERE PERMISSION = 'MEMBER';

-- 3. TB_ITEM 수정
ALTER TABLE TB_ITEM
    ADD COLUMN TRANSFERRED_FROM BIGINT NULL AFTER DELETED_AT,
    ADD COLUMN TRANSFERRED_AT DATETIME NULL AFTER TRANSFERRED_FROM;

ALTER TABLE TB_ITEM
    ADD CONSTRAINT FK_ITEM_TRANSFERRED FOREIGN KEY (TRANSFERRED_FROM) REFERENCES TB_BOARD(BOARD_ID);

CREATE INDEX IDX_ITEM_TRANSFERRED ON TB_ITEM(TRANSFERRED_FROM);

-- 4. TB_ITEM_SHARE 생성
CREATE TABLE TB_ITEM_SHARE (
    ITEM_SHARE_ID BIGINT NOT NULL AUTO_INCREMENT,
    ITEM_ID BIGINT NOT NULL,
    USER_ID BIGINT NOT NULL,
    PERMISSION VARCHAR(20) NOT NULL DEFAULT 'VIEW',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY BIGINT NOT NULL,
    UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    UPDATED_BY BIGINT NULL,
    PRIMARY KEY (ITEM_SHARE_ID),
    UNIQUE KEY UK_ITEM_SHARE (ITEM_ID, USER_ID),
    CONSTRAINT FK_ITEM_SHARE_ITEM FOREIGN KEY (ITEM_ID) REFERENCES TB_ITEM(ITEM_ID),
    CONSTRAINT FK_ITEM_SHARE_USER FOREIGN KEY (USER_ID) REFERENCES TB_USER(USER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IDX_ITEM_SHARE_ITEM ON TB_ITEM_SHARE(ITEM_ID);
CREATE INDEX IDX_ITEM_SHARE_USER ON TB_ITEM_SHARE(USER_ID);

-- 5. TB_AUDIT_LOG 생성
CREATE TABLE TB_AUDIT_LOG (
    LOG_ID BIGINT NOT NULL AUTO_INCREMENT,
    TARGET_TYPE VARCHAR(30) NOT NULL,
    TARGET_ID BIGINT NOT NULL,
    ACTION VARCHAR(30) NOT NULL,
    ACTOR_ID BIGINT NOT NULL,
    DESCRIPTION VARCHAR(500) NULL,
    BEFORE_DATA JSON NULL,
    AFTER_DATA JSON NULL,
    RELATED_USER_ID BIGINT NULL,
    IP_ADDRESS VARCHAR(45) NULL,
    USER_AGENT VARCHAR(500) NULL,
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (LOG_ID),
    CONSTRAINT FK_AUDIT_LOG_ACTOR FOREIGN KEY (ACTOR_ID) REFERENCES TB_USER(USER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IDX_AUDIT_LOG_TARGET ON TB_AUDIT_LOG(TARGET_TYPE, TARGET_ID);
CREATE INDEX IDX_AUDIT_LOG_ACTOR ON TB_AUDIT_LOG(ACTOR_ID);
CREATE INDEX IDX_AUDIT_LOG_ACTION ON TB_AUDIT_LOG(ACTION, CREATED_AT);
CREATE INDEX IDX_AUDIT_LOG_DATE ON TB_AUDIT_LOG(CREATED_AT);
CREATE INDEX IDX_AUDIT_LOG_RELATED ON TB_AUDIT_LOG(RELATED_USER_ID);

-- ============================================
-- 마이그레이션 완료
-- ============================================
```

### 5.2 롤백 스크립트

```sql
-- ============================================
-- TaskFlow v1.1 롤백 스크립트
-- ============================================

-- 1. TB_AUDIT_LOG 삭제
DROP TABLE IF EXISTS TB_AUDIT_LOG;

-- 2. TB_ITEM_SHARE 삭제
DROP TABLE IF EXISTS TB_ITEM_SHARE;

-- 3. TB_ITEM 롤백
DROP INDEX IDX_ITEM_TRANSFERRED ON TB_ITEM;
ALTER TABLE TB_ITEM DROP FOREIGN KEY FK_ITEM_TRANSFERRED;
ALTER TABLE TB_ITEM DROP COLUMN TRANSFERRED_AT;
ALTER TABLE TB_ITEM DROP COLUMN TRANSFERRED_FROM;

-- 4. TB_BOARD_SHARE 롤백
UPDATE TB_BOARD_SHARE SET PERMISSION = 'VIEWER' WHERE PERMISSION = 'VIEW';
UPDATE TB_BOARD_SHARE SET PERMISSION = 'MEMBER' WHERE PERMISSION IN ('EDIT', 'FULL');
ALTER TABLE TB_BOARD_SHARE DROP COLUMN UPDATED_BY;
ALTER TABLE TB_BOARD_SHARE DROP COLUMN UPDATED_AT;

-- 5. TB_BOARD 롤백
DROP INDEX IDX_BOARD_SORT ON TB_BOARD;
ALTER TABLE TB_BOARD DROP COLUMN DEFAULT_VIEW_TYPE;
ALTER TABLE TB_BOARD DROP COLUMN COLOR;
ALTER TABLE TB_BOARD DROP COLUMN SORT_ORDER;

-- ============================================
-- 롤백 완료
-- ============================================
```

---

## 6. 변경 요약

### 6.1 테이블 변경 요약

| # | 테이블명 | 변경 유형 | 추가 컬럼 수 | 수정 컬럼 수 |
|---|----------|----------|-------------|-------------|
| 1 | TB_ITEM_SHARE | 신규 | 8 | - |
| 2 | TB_AUDIT_LOG | 신규 | 12 | - |
| 3 | TB_BOARD | 수정 | 3 | - |
| 4 | TB_BOARD_SHARE | 수정 | 2 | 1 |
| 5 | TB_ITEM | 수정 | 2 | - |
| **합계** | | | **27** | **1** |

### 6.2 인덱스 변경 요약

| # | 테이블명 | 추가 인덱스 |
|---|----------|------------|
| 1 | TB_ITEM_SHARE | 2 |
| 2 | TB_AUDIT_LOG | 5 |
| 3 | TB_BOARD | 1 |
| 4 | TB_ITEM | 1 |
| **합계** | | **9** |

### 6.3 영향도 분석

| 영역 | 영향 |
|------|------|
| 백엔드 Domain | ItemShare, AuditLog 클래스 추가. Board, BoardShare, Item 클래스 수정 |
| 백엔드 Mapper | ItemShareMapper, AuditLogMapper 추가. BoardMapper, ItemMapper 수정 |
| 백엔드 Service | ItemShareService, AuditLogService, TransferService 추가. BoardService 수정 |
| 프론트엔드 Type | ItemShare, AuditLog 타입 추가. Board, BoardShare, Item 타입 수정 |
| 프론트엔드 API | itemShare.api.ts, auditLog.api.ts 추가. board.api.ts 수정 |

---

## 승인

| 항목 | 내용 |
|------|------|
| 문서명 | TaskFlow v1.1 ERD 및 테이블 정의서 |
| 버전 | 1.1 |
| 작성자 | - |
| 작성일 | 2024-12-21 |
| 상태 | 승인 대기 |

---

*이 문서는 v1.0 테이블 정의서(docs/erd/table-definition.md)를 기반으로 확장된 문서입니다.*
