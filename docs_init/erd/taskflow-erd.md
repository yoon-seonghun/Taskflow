# TaskFlow ERD

## ER 다이어그램

```mermaid
erDiagram
    %% ==========================================
    %% 조직 관리 (부서/그룹/사용자)
    %% ==========================================

    TB_DEPARTMENT {
        BIGINT DEPARTMENT_ID PK "부서 ID"
        VARCHAR(20) DEPARTMENT_CODE UK "부서 코드"
        VARCHAR(100) DEPARTMENT_NAME "부서명"
        BIGINT PARENT_ID FK "상위 부서 ID (자기참조)"
        INT SORT_ORDER "정렬 순서"
        CHAR(1) USE_YN "사용 여부 (Y/N)"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
        DATETIME UPDATED_AT "수정일시"
        BIGINT UPDATED_BY "수정자"
    }

    TB_USER {
        BIGINT USER_ID PK "사용자 ID"
        VARCHAR(50) USERNAME UK "로그인 ID"
        VARCHAR(255) PASSWORD "비밀번호 (BCrypt)"
        VARCHAR(100) NAME "사용자 이름"
        BIGINT DEPARTMENT_ID FK "부서 ID"
        CHAR(1) USE_YN "사용 여부 (Y/N)"
        DATETIME LAST_LOGIN_AT "마지막 로그인"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
        DATETIME UPDATED_AT "수정일시"
        BIGINT UPDATED_BY "수정자"
    }

    TB_GROUP {
        BIGINT GROUP_ID PK "그룹 ID"
        VARCHAR(20) GROUP_CODE UK "그룹 코드"
        VARCHAR(100) GROUP_NAME "그룹명"
        VARCHAR(500) DESCRIPTION "그룹 설명"
        VARCHAR(7) COLOR "색상 (#RRGGBB)"
        INT SORT_ORDER "정렬 순서"
        CHAR(1) USE_YN "사용 여부 (Y/N)"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
        DATETIME UPDATED_AT "수정일시"
        BIGINT UPDATED_BY "수정자"
    }

    TB_USER_GROUP {
        BIGINT USER_GROUP_ID PK "매핑 ID"
        BIGINT USER_ID FK "사용자 ID"
        BIGINT GROUP_ID FK "그룹 ID"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
    }

    %% ==========================================
    %% 보드 관리
    %% ==========================================

    TB_BOARD {
        BIGINT BOARD_ID PK "보드 ID"
        VARCHAR(200) BOARD_NAME "보드명"
        VARCHAR(500) DESCRIPTION "보드 설명"
        BIGINT OWNER_ID FK "소유자 ID"
        CHAR(1) USE_YN "사용 여부 (Y/N)"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
        DATETIME UPDATED_AT "수정일시"
        BIGINT UPDATED_BY "수정자"
    }

    TB_BOARD_SHARE {
        BIGINT BOARD_SHARE_ID PK "공유 ID"
        BIGINT BOARD_ID FK "보드 ID"
        BIGINT USER_ID FK "공유 사용자 ID"
        VARCHAR(20) PERMISSION "권한 (EDIT/VIEW)"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
    }

    %% ==========================================
    %% 속성 정의 (EAV 패턴)
    %% ==========================================

    TB_PROPERTY_DEF {
        BIGINT PROPERTY_ID PK "속성 ID"
        BIGINT BOARD_ID FK "보드 ID"
        VARCHAR(100) PROPERTY_NAME "속성명"
        VARCHAR(20) PROPERTY_TYPE "속성 타입"
        CHAR(1) REQUIRED_YN "필수 여부 (Y/N)"
        CHAR(1) VISIBLE_YN "표시 여부 (Y/N)"
        INT SORT_ORDER "정렬 순서"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
        DATETIME UPDATED_AT "수정일시"
        BIGINT UPDATED_BY "수정자"
    }

    TB_PROPERTY_OPTION {
        BIGINT OPTION_ID PK "옵션 ID"
        BIGINT PROPERTY_ID FK "속성 ID"
        VARCHAR(100) OPTION_LABEL "옵션 라벨"
        VARCHAR(7) COLOR "색상 (#RRGGBB)"
        INT SORT_ORDER "정렬 순서"
        CHAR(1) USE_YN "사용 여부 (Y/N)"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
        DATETIME UPDATED_AT "수정일시"
        BIGINT UPDATED_BY "수정자"
    }

    %% ==========================================
    %% 업무 아이템
    %% ==========================================

    TB_ITEM {
        BIGINT ITEM_ID PK "아이템 ID"
        BIGINT BOARD_ID FK "보드 ID"
        VARCHAR(500) CONTENT "업무 내용"
        VARCHAR(20) STATUS "상태"
        VARCHAR(20) PRIORITY "우선순위"
        BIGINT CATEGORY_ID FK "카테고리 (옵션 ID)"
        BIGINT GROUP_ID FK "그룹 ID"
        BIGINT ASSIGNEE_ID FK "담당자 ID"
        DATETIME START_TIME "시작 시간"
        DATETIME END_TIME "완료 시간"
        DATETIME DELETED_AT "삭제 시간"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
        DATETIME UPDATED_AT "수정일시"
        BIGINT UPDATED_BY "수정자"
    }

    TB_ITEM_PROPERTY {
        BIGINT ITEM_PROPERTY_ID PK "속성값 ID"
        BIGINT ITEM_ID FK "아이템 ID"
        BIGINT PROPERTY_ID FK "속성 ID"
        VARCHAR(1000) VALUE_TEXT "텍스트 값"
        DECIMAL(18,4) VALUE_NUMBER "숫자 값"
        DATE VALUE_DATE "날짜 값"
        BIGINT VALUE_OPTION_ID FK "선택 옵션 ID"
        BIGINT VALUE_USER_ID FK "사용자 ID"
        CHAR(1) VALUE_CHECKBOX "체크박스 값 (Y/N)"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
        DATETIME UPDATED_AT "수정일시"
        BIGINT UPDATED_BY "수정자"
    }

    TB_ITEM_PROPERTY_MULTI {
        BIGINT ITEM_PROPERTY_MULTI_ID PK "다중선택 ID"
        BIGINT ITEM_ID FK "아이템 ID"
        BIGINT PROPERTY_ID FK "속성 ID"
        BIGINT OPTION_ID FK "옵션 ID"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
    }

    %% ==========================================
    %% 댓글
    %% ==========================================

    TB_COMMENT {
        BIGINT COMMENT_ID PK "댓글 ID"
        BIGINT ITEM_ID FK "아이템 ID"
        VARCHAR(2000) CONTENT "댓글 내용"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
        DATETIME UPDATED_AT "수정일시"
        BIGINT UPDATED_BY "수정자"
    }

    %% ==========================================
    %% 작업 템플릿
    %% ==========================================

    TB_TASK_TEMPLATE {
        BIGINT TEMPLATE_ID PK "템플릿 ID"
        VARCHAR(500) CONTENT "작업 내용"
        CHAR(1) USE_YN "사용 여부 (Y/N)"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
        DATETIME UPDATED_AT "수정일시"
        BIGINT UPDATED_BY "수정자"
    }

    %% ==========================================
    %% 아이템 이력
    %% ==========================================

    TB_ITEM_HISTORY {
        BIGINT HISTORY_ID PK "이력 ID"
        BIGINT ITEM_ID FK "아이템 ID"
        VARCHAR(50) ACTION_TYPE "액션 타입"
        VARCHAR(100) FIELD_NAME "변경 필드명"
        VARCHAR(1000) OLD_VALUE "이전 값"
        VARCHAR(1000) NEW_VALUE "새 값"
        DATETIME CREATED_AT "생성일시"
        BIGINT CREATED_BY "생성자"
    }

    %% ==========================================
    %% 관계 정의
    %% ==========================================

    %% 부서 계층 (자기참조)
    TB_DEPARTMENT ||--o{ TB_DEPARTMENT : "PARENT_ID"

    %% 사용자-부서 (N:1)
    TB_DEPARTMENT ||--o{ TB_USER : "DEPARTMENT_ID"

    %% 사용자-그룹 (N:M)
    TB_USER ||--o{ TB_USER_GROUP : "USER_ID"
    TB_GROUP ||--o{ TB_USER_GROUP : "GROUP_ID"

    %% 보드
    TB_USER ||--o{ TB_BOARD : "OWNER_ID"
    TB_BOARD ||--o{ TB_BOARD_SHARE : "BOARD_ID"
    TB_USER ||--o{ TB_BOARD_SHARE : "USER_ID"

    %% 속성 정의
    TB_BOARD ||--o{ TB_PROPERTY_DEF : "BOARD_ID"
    TB_PROPERTY_DEF ||--o{ TB_PROPERTY_OPTION : "PROPERTY_ID"

    %% 아이템
    TB_BOARD ||--o{ TB_ITEM : "BOARD_ID"
    TB_GROUP ||--o{ TB_ITEM : "GROUP_ID"
    TB_USER ||--o{ TB_ITEM : "ASSIGNEE_ID"
    TB_PROPERTY_OPTION ||--o{ TB_ITEM : "CATEGORY_ID"

    %% 아이템 속성값
    TB_ITEM ||--o{ TB_ITEM_PROPERTY : "ITEM_ID"
    TB_PROPERTY_DEF ||--o{ TB_ITEM_PROPERTY : "PROPERTY_ID"
    TB_PROPERTY_OPTION ||--o{ TB_ITEM_PROPERTY : "VALUE_OPTION_ID"
    TB_USER ||--o{ TB_ITEM_PROPERTY : "VALUE_USER_ID"

    %% 다중선택 속성값
    TB_ITEM ||--o{ TB_ITEM_PROPERTY_MULTI : "ITEM_ID"
    TB_PROPERTY_DEF ||--o{ TB_ITEM_PROPERTY_MULTI : "PROPERTY_ID"
    TB_PROPERTY_OPTION ||--o{ TB_ITEM_PROPERTY_MULTI : "OPTION_ID"

    %% 댓글
    TB_ITEM ||--o{ TB_COMMENT : "ITEM_ID"

    %% 이력
    TB_ITEM ||--o{ TB_ITEM_HISTORY : "ITEM_ID"
```

## 테이블 요약

| 테이블 | 설명 | 주요 관계 |
|--------|------|----------|
| TB_DEPARTMENT | 부서 (계층 구조) | 자기참조 (PARENT_ID) |
| TB_USER | 사용자 | 부서 N:1 |
| TB_GROUP | 그룹 | - |
| TB_USER_GROUP | 사용자-그룹 매핑 | N:M 연결 |
| TB_BOARD | 보드(컬렉션) | 소유자 N:1 |
| TB_BOARD_SHARE | 보드 공유 | 보드-사용자 N:M |
| TB_PROPERTY_DEF | 속성 정의 | 보드 N:1 |
| TB_PROPERTY_OPTION | 속성 옵션(코드) | 속성 N:1 |
| TB_ITEM | 업무 아이템 | 보드 N:1, 그룹 N:1 |
| TB_ITEM_PROPERTY | 아이템 속성값 | EAV 패턴 |
| TB_ITEM_PROPERTY_MULTI | 다중선택 값 | N:M 연결 |
| TB_COMMENT | 댓글 | 아이템 N:1 |
| TB_TASK_TEMPLATE | 작업 템플릿 | 독립 |
| TB_ITEM_HISTORY | 아이템 이력 | 아이템 N:1 |

## 속성 타입 (PROPERTY_TYPE)

| 타입 | 설명 | 저장 컬럼 |
|------|------|----------|
| TEXT | 텍스트 | VALUE_TEXT |
| NUMBER | 숫자 | VALUE_NUMBER |
| DATE | 날짜 | VALUE_DATE |
| SELECT | 단일선택 | VALUE_OPTION_ID |
| MULTI_SELECT | 다중선택 | TB_ITEM_PROPERTY_MULTI |
| CHECKBOX | 체크박스 | VALUE_CHECKBOX |
| USER | 사용자 | VALUE_USER_ID |

## 상태값 (STATUS)

| 값 | 설명 |
|----|------|
| NOT_STARTED | 시작전 |
| IN_PROGRESS | 진행중 |
| COMPLETED | 완료 |
| DELETED | 삭제 |

## 우선순위 (PRIORITY)

| 값 | 설명 |
|----|------|
| URGENT | 긴급 |
| HIGH | 높음 |
| NORMAL | 보통 |
| LOW | 낮음 |

## 이력 액션 타입 (ACTION_TYPE)

| 값 | 설명 |
|----|------|
| CREATE | 생성 |
| UPDATE | 수정 |
| COMPLETE | 완료 |
| DELETE | 삭제 |
| RESTORE | 복원 |
