# TaskFlow 테이블 정의서

## 목차
1. [TB_DEPARTMENT - 부서](#1-tb_department---부서)
2. [TB_USER - 사용자](#2-tb_user---사용자)
3. [TB_GROUP - 그룹](#3-tb_group---그룹)
4. [TB_USER_GROUP - 사용자-그룹 매핑](#4-tb_user_group---사용자-그룹-매핑)
5. [TB_BOARD - 보드](#5-tb_board---보드)
6. [TB_BOARD_SHARE - 보드 공유](#6-tb_board_share---보드-공유)
7. [TB_PROPERTY_DEF - 속성 정의](#7-tb_property_def---속성-정의)
8. [TB_PROPERTY_OPTION - 속성 옵션](#8-tb_property_option---속성-옵션)
9. [TB_ITEM - 업무 아이템](#9-tb_item---업무-아이템)
10. [TB_ITEM_PROPERTY - 아이템 속성값](#10-tb_item_property---아이템-속성값)
11. [TB_ITEM_PROPERTY_MULTI - 다중선택 속성값](#11-tb_item_property_multi---다중선택-속성값)
12. [TB_COMMENT - 댓글](#12-tb_comment---댓글)
13. [TB_TASK_TEMPLATE - 작업 템플릿](#13-tb_task_template---작업-템플릿)
14. [TB_ITEM_HISTORY - 아이템 이력](#14-tb_item_history---아이템-이력)

---

## 1. TB_DEPARTMENT - 부서

### 설명
부서 정보를 관리하는 테이블. 자기참조를 통해 계층 구조 지원.

### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| DEPARTMENT_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 부서 ID (PK) |
| DEPARTMENT_CODE | VARCHAR | 20 | NOT NULL | - | 부서 코드 (UK) |
| DEPARTMENT_NAME | VARCHAR | 100 | NOT NULL | - | 부서명 |
| PARENT_ID | BIGINT | - | NULL | NULL | 상위 부서 ID (FK, 자기참조) |
| SORT_ORDER | INT | - | NOT NULL | 0 | 정렬 순서 |
| USE_YN | CHAR | 1 | NOT NULL | 'Y' | 사용 여부 (Y/N) |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| UPDATED_AT | DATETIME | - | NULL | NULL ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| UPDATED_BY | BIGINT | - | NULL | NULL | 수정자 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_DEPARTMENT | PRIMARY KEY | DEPARTMENT_ID | - |
| UK_DEPARTMENT_CODE | UNIQUE | DEPARTMENT_CODE | - |
| FK_DEPARTMENT_PARENT | FOREIGN KEY | PARENT_ID | TB_DEPARTMENT(DEPARTMENT_ID) |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_DEPARTMENT_PARENT | PARENT_ID | INDEX | 상위 부서 조회 |
| IDX_DEPARTMENT_USE | USE_YN, SORT_ORDER | INDEX | 활성 부서 정렬 조회 |

---

## 2. TB_USER - 사용자

### 설명
시스템 사용자 정보를 관리하는 테이블.

### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| USER_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 사용자 ID (PK) |
| USERNAME | VARCHAR | 50 | NOT NULL | - | 로그인 ID (UK) |
| PASSWORD | VARCHAR | 255 | NOT NULL | - | 비밀번호 (BCrypt) |
| NAME | VARCHAR | 100 | NOT NULL | - | 사용자 이름 |
| DEPARTMENT_ID | BIGINT | - | NULL | NULL | 부서 ID (FK) |
| USE_YN | CHAR | 1 | NOT NULL | 'Y' | 사용 여부 (Y/N) |
| LAST_LOGIN_AT | DATETIME | - | NULL | NULL | 마지막 로그인 일시 |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| UPDATED_AT | DATETIME | - | NULL | NULL ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| UPDATED_BY | BIGINT | - | NULL | NULL | 수정자 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_USER | PRIMARY KEY | USER_ID | - |
| UK_USER_USERNAME | UNIQUE | USERNAME | - |
| FK_USER_DEPARTMENT | FOREIGN KEY | DEPARTMENT_ID | TB_DEPARTMENT(DEPARTMENT_ID) |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_USER_DEPARTMENT | DEPARTMENT_ID | INDEX | 부서별 사용자 조회 |
| IDX_USER_USE | USE_YN | INDEX | 활성 사용자 조회 |
| IDX_USER_NAME | NAME | INDEX | 이름 검색 |

---

## 3. TB_GROUP - 그룹

### 설명
프로젝트/업무 그룹 정보를 관리하는 테이블.

### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| GROUP_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 그룹 ID (PK) |
| GROUP_CODE | VARCHAR | 20 | NOT NULL | - | 그룹 코드 (UK) |
| GROUP_NAME | VARCHAR | 100 | NOT NULL | - | 그룹명 |
| DESCRIPTION | VARCHAR | 500 | NULL | NULL | 그룹 설명 |
| COLOR | VARCHAR | 7 | NULL | NULL | 색상 (#RRGGBB) |
| SORT_ORDER | INT | - | NOT NULL | 0 | 정렬 순서 |
| USE_YN | CHAR | 1 | NOT NULL | 'Y' | 사용 여부 (Y/N) |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| UPDATED_AT | DATETIME | - | NULL | NULL ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| UPDATED_BY | BIGINT | - | NULL | NULL | 수정자 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_GROUP | PRIMARY KEY | GROUP_ID | - |
| UK_GROUP_CODE | UNIQUE | GROUP_CODE | - |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_GROUP_USE | USE_YN, SORT_ORDER | INDEX | 활성 그룹 정렬 조회 |

---

## 4. TB_USER_GROUP - 사용자-그룹 매핑

### 설명
사용자와 그룹의 N:M 관계를 관리하는 매핑 테이블.

### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| USER_GROUP_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 매핑 ID (PK) |
| USER_ID | BIGINT | - | NOT NULL | - | 사용자 ID (FK) |
| GROUP_ID | BIGINT | - | NOT NULL | - | 그룹 ID (FK) |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_USER_GROUP | PRIMARY KEY | USER_GROUP_ID | - |
| UK_USER_GROUP | UNIQUE | USER_ID, GROUP_ID | - |
| FK_USER_GROUP_USER | FOREIGN KEY | USER_ID | TB_USER(USER_ID) |
| FK_USER_GROUP_GROUP | FOREIGN KEY | GROUP_ID | TB_GROUP(GROUP_ID) |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_USER_GROUP_USER | USER_ID | INDEX | 사용자별 그룹 조회 |
| IDX_USER_GROUP_GROUP | GROUP_ID | INDEX | 그룹별 사용자 조회 |

---

## 5. TB_BOARD - 보드

### 설명
업무 보드(컬렉션) 정보를 관리하는 테이블.

### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| BOARD_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 보드 ID (PK) |
| BOARD_NAME | VARCHAR | 200 | NOT NULL | - | 보드명 |
| DESCRIPTION | VARCHAR | 500 | NULL | NULL | 보드 설명 |
| OWNER_ID | BIGINT | - | NOT NULL | - | 소유자 ID (FK) |
| USE_YN | CHAR | 1 | NOT NULL | 'Y' | 사용 여부 (Y/N) |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| UPDATED_AT | DATETIME | - | NULL | NULL ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| UPDATED_BY | BIGINT | - | NULL | NULL | 수정자 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_BOARD | PRIMARY KEY | BOARD_ID | - |
| FK_BOARD_OWNER | FOREIGN KEY | OWNER_ID | TB_USER(USER_ID) |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_BOARD_OWNER | OWNER_ID | INDEX | 소유자별 보드 조회 |
| IDX_BOARD_USE | USE_YN | INDEX | 활성 보드 조회 |

---

## 6. TB_BOARD_SHARE - 보드 공유

### 설명
보드 공유 사용자 정보를 관리하는 테이블.

### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| BOARD_SHARE_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 공유 ID (PK) |
| BOARD_ID | BIGINT | - | NOT NULL | - | 보드 ID (FK) |
| USER_ID | BIGINT | - | NOT NULL | - | 공유 사용자 ID (FK) |
| PERMISSION | VARCHAR | 20 | NOT NULL | 'EDIT' | 권한 (EDIT/VIEW) |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_BOARD_SHARE | PRIMARY KEY | BOARD_SHARE_ID | - |
| UK_BOARD_SHARE | UNIQUE | BOARD_ID, USER_ID | - |
| FK_BOARD_SHARE_BOARD | FOREIGN KEY | BOARD_ID | TB_BOARD(BOARD_ID) |
| FK_BOARD_SHARE_USER | FOREIGN KEY | USER_ID | TB_USER(USER_ID) |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_BOARD_SHARE_BOARD | BOARD_ID | INDEX | 보드별 공유 사용자 조회 |
| IDX_BOARD_SHARE_USER | USER_ID | INDEX | 사용자별 공유 보드 조회 |

---

## 7. TB_PROPERTY_DEF - 속성 정의

### 설명
보드별 동적 속성 정의를 관리하는 테이블 (EAV 패턴).

### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| PROPERTY_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 속성 ID (PK) |
| BOARD_ID | BIGINT | - | NOT NULL | - | 보드 ID (FK) |
| PROPERTY_NAME | VARCHAR | 100 | NOT NULL | - | 속성명 |
| PROPERTY_TYPE | VARCHAR | 20 | NOT NULL | - | 속성 타입 |
| REQUIRED_YN | CHAR | 1 | NOT NULL | 'N' | 필수 여부 (Y/N) |
| VISIBLE_YN | CHAR | 1 | NOT NULL | 'Y' | 표시 여부 (Y/N) |
| SORT_ORDER | INT | - | NOT NULL | 0 | 정렬 순서 |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| UPDATED_AT | DATETIME | - | NULL | NULL ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| UPDATED_BY | BIGINT | - | NULL | NULL | 수정자 |

### PROPERTY_TYPE 값

| 값 | 설명 |
|----|------|
| TEXT | 텍스트 |
| NUMBER | 숫자 |
| DATE | 날짜 |
| SELECT | 단일선택 |
| MULTI_SELECT | 다중선택 |
| CHECKBOX | 체크박스 |
| USER | 사용자 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_PROPERTY_DEF | PRIMARY KEY | PROPERTY_ID | - |
| FK_PROPERTY_DEF_BOARD | FOREIGN KEY | BOARD_ID | TB_BOARD(BOARD_ID) |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_PROPERTY_DEF_BOARD | BOARD_ID, SORT_ORDER | INDEX | 보드별 속성 정렬 조회 |
| IDX_PROPERTY_DEF_VISIBLE | BOARD_ID, VISIBLE_YN | INDEX | 보드별 표시 속성 조회 |

---

## 8. TB_PROPERTY_OPTION - 속성 옵션

### 설명
SELECT/MULTI_SELECT 타입 속성의 옵션(코드) 정보를 관리하는 테이블.

### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| OPTION_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 옵션 ID (PK) |
| PROPERTY_ID | BIGINT | - | NOT NULL | - | 속성 ID (FK) |
| OPTION_LABEL | VARCHAR | 100 | NOT NULL | - | 옵션 라벨 |
| COLOR | VARCHAR | 7 | NULL | NULL | 색상 (#RRGGBB) |
| SORT_ORDER | INT | - | NOT NULL | 0 | 정렬 순서 |
| USE_YN | CHAR | 1 | NOT NULL | 'Y' | 사용 여부 (Y/N) |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| UPDATED_AT | DATETIME | - | NULL | NULL ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| UPDATED_BY | BIGINT | - | NULL | NULL | 수정자 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_PROPERTY_OPTION | PRIMARY KEY | OPTION_ID | - |
| FK_PROPERTY_OPTION_PROPERTY | FOREIGN KEY | PROPERTY_ID | TB_PROPERTY_DEF(PROPERTY_ID) |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_PROPERTY_OPTION_PROPERTY | PROPERTY_ID, SORT_ORDER | INDEX | 속성별 옵션 정렬 조회 |
| IDX_PROPERTY_OPTION_USE | PROPERTY_ID, USE_YN | INDEX | 속성별 활성 옵션 조회 |

---

## 9. TB_ITEM - 업무 아이템

### 설명
업무 아이템(태스크) 정보를 관리하는 테이블.

### 컬럼 정의

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
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| UPDATED_AT | DATETIME | - | NULL | NULL ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| UPDATED_BY | BIGINT | - | NULL | NULL | 수정자 |

### STATUS 값

| 값 | 설명 |
|----|------|
| NOT_STARTED | 시작전 |
| IN_PROGRESS | 진행중 |
| COMPLETED | 완료 |
| DELETED | 삭제 |

### PRIORITY 값

| 값 | 설명 |
|----|------|
| URGENT | 긴급 |
| HIGH | 높음 |
| NORMAL | 보통 |
| LOW | 낮음 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_ITEM | PRIMARY KEY | ITEM_ID | - |
| FK_ITEM_BOARD | FOREIGN KEY | BOARD_ID | TB_BOARD(BOARD_ID) |
| FK_ITEM_CATEGORY | FOREIGN KEY | CATEGORY_ID | TB_PROPERTY_OPTION(OPTION_ID) |
| FK_ITEM_GROUP | FOREIGN KEY | GROUP_ID | TB_GROUP(GROUP_ID) |
| FK_ITEM_ASSIGNEE | FOREIGN KEY | ASSIGNEE_ID | TB_USER(USER_ID) |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_ITEM_BOARD_STATUS | BOARD_ID, STATUS | INDEX | 보드별 상태 조회 |
| IDX_ITEM_ASSIGNEE | ASSIGNEE_ID | INDEX | 담당자별 조회 |
| IDX_ITEM_GROUP | GROUP_ID | INDEX | 그룹별 조회 |
| IDX_ITEM_CREATED | BOARD_ID, CREATED_AT | INDEX | 보드별 생성일 정렬 |
| IDX_ITEM_END_TIME | END_TIME | INDEX | 완료시간 조회 |

---

## 10. TB_ITEM_PROPERTY - 아이템 속성값

### 설명
아이템의 동적 속성값을 저장하는 테이블 (EAV 패턴).

### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| ITEM_PROPERTY_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 속성값 ID (PK) |
| ITEM_ID | BIGINT | - | NOT NULL | - | 아이템 ID (FK) |
| PROPERTY_ID | BIGINT | - | NOT NULL | - | 속성 ID (FK) |
| VALUE_TEXT | VARCHAR | 1000 | NULL | NULL | 텍스트 값 (TEXT 타입) |
| VALUE_NUMBER | DECIMAL | 18,4 | NULL | NULL | 숫자 값 (NUMBER 타입) |
| VALUE_DATE | DATE | - | NULL | NULL | 날짜 값 (DATE 타입) |
| VALUE_OPTION_ID | BIGINT | - | NULL | NULL | 선택 옵션 ID (SELECT 타입, FK) |
| VALUE_USER_ID | BIGINT | - | NULL | NULL | 사용자 ID (USER 타입, FK) |
| VALUE_CHECKBOX | CHAR | 1 | NULL | NULL | 체크박스 값 (CHECKBOX 타입, Y/N) |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| UPDATED_AT | DATETIME | - | NULL | NULL ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| UPDATED_BY | BIGINT | - | NULL | NULL | 수정자 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_ITEM_PROPERTY | PRIMARY KEY | ITEM_PROPERTY_ID | - |
| UK_ITEM_PROPERTY | UNIQUE | ITEM_ID, PROPERTY_ID | - |
| FK_ITEM_PROPERTY_ITEM | FOREIGN KEY | ITEM_ID | TB_ITEM(ITEM_ID) |
| FK_ITEM_PROPERTY_PROPERTY | FOREIGN KEY | PROPERTY_ID | TB_PROPERTY_DEF(PROPERTY_ID) |
| FK_ITEM_PROPERTY_OPTION | FOREIGN KEY | VALUE_OPTION_ID | TB_PROPERTY_OPTION(OPTION_ID) |
| FK_ITEM_PROPERTY_USER | FOREIGN KEY | VALUE_USER_ID | TB_USER(USER_ID) |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_ITEM_PROPERTY_ITEM | ITEM_ID | INDEX | 아이템별 속성값 조회 |
| IDX_ITEM_PROPERTY_PROPERTY | PROPERTY_ID | INDEX | 속성별 값 조회 |
| IDX_ITEM_PROPERTY_OPTION | VALUE_OPTION_ID | INDEX | 옵션값 조회 |

---

## 11. TB_ITEM_PROPERTY_MULTI - 다중선택 속성값

### 설명
MULTI_SELECT 타입 속성의 다중 선택값을 저장하는 테이블.

### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| ITEM_PROPERTY_MULTI_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 다중선택 ID (PK) |
| ITEM_ID | BIGINT | - | NOT NULL | - | 아이템 ID (FK) |
| PROPERTY_ID | BIGINT | - | NOT NULL | - | 속성 ID (FK) |
| OPTION_ID | BIGINT | - | NOT NULL | - | 옵션 ID (FK) |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_ITEM_PROPERTY_MULTI | PRIMARY KEY | ITEM_PROPERTY_MULTI_ID | - |
| UK_ITEM_PROPERTY_MULTI | UNIQUE | ITEM_ID, PROPERTY_ID, OPTION_ID | - |
| FK_ITEM_PROP_MULTI_ITEM | FOREIGN KEY | ITEM_ID | TB_ITEM(ITEM_ID) |
| FK_ITEM_PROP_MULTI_PROPERTY | FOREIGN KEY | PROPERTY_ID | TB_PROPERTY_DEF(PROPERTY_ID) |
| FK_ITEM_PROP_MULTI_OPTION | FOREIGN KEY | OPTION_ID | TB_PROPERTY_OPTION(OPTION_ID) |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_ITEM_PROP_MULTI_ITEM | ITEM_ID, PROPERTY_ID | INDEX | 아이템-속성별 조회 |
| IDX_ITEM_PROP_MULTI_OPTION | OPTION_ID | INDEX | 옵션별 조회 |

---

## 12. TB_COMMENT - 댓글

### 설명
아이템에 대한 댓글(리플) 정보를 관리하는 테이블.

### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| COMMENT_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 댓글 ID (PK) |
| ITEM_ID | BIGINT | - | NOT NULL | - | 아이템 ID (FK) |
| CONTENT | VARCHAR | 2000 | NOT NULL | - | 댓글 내용 |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| UPDATED_AT | DATETIME | - | NULL | NULL ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| UPDATED_BY | BIGINT | - | NULL | NULL | 수정자 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_COMMENT | PRIMARY KEY | COMMENT_ID | - |
| FK_COMMENT_ITEM | FOREIGN KEY | ITEM_ID | TB_ITEM(ITEM_ID) |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_COMMENT_ITEM | ITEM_ID, CREATED_AT | INDEX | 아이템별 댓글 정렬 조회 |

---

## 13. TB_TASK_TEMPLATE - 작업 템플릿

### 설명
자주 사용하는 작업 템플릿을 관리하는 테이블.

### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| TEMPLATE_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 템플릿 ID (PK) |
| CONTENT | VARCHAR | 500 | NOT NULL | - | 작업 내용 |
| USE_YN | CHAR | 1 | NOT NULL | 'Y' | 사용 여부 (Y/N) |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |
| UPDATED_AT | DATETIME | - | NULL | NULL ON UPDATE CURRENT_TIMESTAMP | 수정일시 |
| UPDATED_BY | BIGINT | - | NULL | NULL | 수정자 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_TASK_TEMPLATE | PRIMARY KEY | TEMPLATE_ID | - |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_TASK_TEMPLATE_USE | USE_YN | INDEX | 활성 템플릿 조회 |
| IDX_TASK_TEMPLATE_CONTENT | CONTENT | INDEX | 템플릿 검색 (자동완성) |

---

## 14. TB_ITEM_HISTORY - 아이템 이력

### 설명
아이템 변경 이력을 관리하는 테이블.

### 컬럼 정의

| 컬럼명 | 타입 | 길이 | NULL | 기본값 | 설명 |
|--------|------|------|------|--------|------|
| HISTORY_ID | BIGINT | - | NOT NULL | AUTO_INCREMENT | 이력 ID (PK) |
| ITEM_ID | BIGINT | - | NOT NULL | - | 아이템 ID (FK) |
| ACTION_TYPE | VARCHAR | 50 | NOT NULL | - | 액션 타입 |
| FIELD_NAME | VARCHAR | 100 | NULL | NULL | 변경 필드명 |
| OLD_VALUE | VARCHAR | 1000 | NULL | NULL | 이전 값 |
| NEW_VALUE | VARCHAR | 1000 | NULL | NULL | 새 값 |
| CREATED_AT | DATETIME | - | NOT NULL | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | BIGINT | - | NOT NULL | - | 생성자 |

### ACTION_TYPE 값

| 값 | 설명 |
|----|------|
| CREATE | 생성 |
| UPDATE | 수정 |
| COMPLETE | 완료 |
| DELETE | 삭제 |
| RESTORE | 복원 |

### 제약조건

| 제약조건명 | 유형 | 컬럼 | 참조 |
|-----------|------|------|------|
| PK_ITEM_HISTORY | PRIMARY KEY | HISTORY_ID | - |
| FK_ITEM_HISTORY_ITEM | FOREIGN KEY | ITEM_ID | TB_ITEM(ITEM_ID) |

### 인덱스

| 인덱스명 | 컬럼 | 유형 | 설명 |
|----------|------|------|------|
| IDX_ITEM_HISTORY_ITEM | ITEM_ID, CREATED_AT | INDEX | 아이템별 이력 조회 |
| IDX_ITEM_HISTORY_ACTION | ACTION_TYPE, CREATED_AT | INDEX | 액션별 이력 조회 |
| IDX_ITEM_HISTORY_DATE | CREATED_AT | INDEX | 날짜별 이력 조회 |

---

## 테이블 요약

| # | 테이블명 | 설명 | 컬럼 수 | 인덱스 수 |
|---|----------|------|--------|----------|
| 1 | TB_DEPARTMENT | 부서 | 10 | 2 |
| 2 | TB_USER | 사용자 | 11 | 3 |
| 3 | TB_GROUP | 그룹 | 11 | 1 |
| 4 | TB_USER_GROUP | 사용자-그룹 매핑 | 5 | 2 |
| 5 | TB_BOARD | 보드 | 9 | 2 |
| 6 | TB_BOARD_SHARE | 보드 공유 | 6 | 2 |
| 7 | TB_PROPERTY_DEF | 속성 정의 | 11 | 2 |
| 8 | TB_PROPERTY_OPTION | 속성 옵션 | 10 | 2 |
| 9 | TB_ITEM | 업무 아이템 | 15 | 5 |
| 10 | TB_ITEM_PROPERTY | 아이템 속성값 | 12 | 3 |
| 11 | TB_ITEM_PROPERTY_MULTI | 다중선택 속성값 | 6 | 2 |
| 12 | TB_COMMENT | 댓글 | 7 | 1 |
| 13 | TB_TASK_TEMPLATE | 작업 템플릿 | 7 | 2 |
| 14 | TB_ITEM_HISTORY | 아이템 이력 | 8 | 3 |
| **합계** | | | **128** | **32** |
