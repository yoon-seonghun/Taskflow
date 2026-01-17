# Todo List 기능 - ERD 및 테이블 정의서

> **버전**: 2.3.0
> **작성일**: 2025-01-18
> **상태**: 승인 대기

---

## 1. 기능 개요

### 1.1 목적
- **개인 Todo List**: 업무와 별개로 개인 할 일 목록 관리
- **업무 내 체크리스트**: 각 업무(Item) 내 세부 체크항목 관리

### 1.2 주요 기능
| 기능 | 설명 |
|------|------|
| Todo 생성/수정/삭제 | 개인 할 일 관리 |
| 완료 체크 | 할 일 완료 처리 |
| 마감일/시간 설정 | 기한 관리 |
| 우선순위 | 긴급/높음/보통/낮음 |
| 반복 설정 | 매일/매주/매월/매년 반복 |
| **공유** | 다른 사용자와 Todo 공유 (VIEW/EDIT) |
| **이관** | Todo 소유권 이전 |
| 체크리스트 | 업무 내 세부 체크항목 |

---

## 2. 현재 메뉴 구조

```
┌─────────────────────────────────────────┐
│  TaskFlow                               │
├─────────────────────────────────────────┤
│  📋 업무 페이지                          │
│  📊 전체 업무                            │
│  📤 공유받은 업무                        │
│  ⚠️ 지연 업무                            │
│  ⏸️ 보류 업무                            │
│  ✅ 완료 작업                            │
│  🗑️ 삭제된 작업                          │
│  ─────────────────────                  │
│  ☑️ Todo List              ← 신규 추가   │
│  ─────────────────────                  │
│  📝 작업 등록                            │
│  🕐 이력관리                             │
│  ─────────────────────                  │
│  📁 그룹 관리                            │
│  📋 보드 관리                            │
│  ─────────────────────                  │
│  ⚙️ 설정                                 │
│  ─────────────────────                  │
│  [보드 목록]                             │
│   ├─ 내 보드                            │
│   ├─ 공유해준 보드                       │
│   └─ 공유받은 보드                       │
└─────────────────────────────────────────┘
```

---

## 3. ERD 다이어그램

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Todo List 시스템                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────┐         ┌──────────────────────┐                      │
│  │   TB_USER    │         │      TB_TODO         │                      │
│  ├──────────────┤         ├──────────────────────┤                      │
│  │ USER_ID (PK) │◄────┬───│ OWNER_USER_ID (FK)   │  소유자              │
│  │ USERNAME     │     │   │ TODO_ID (PK)         │                      │
│  └──────────────┘     │   │ TITLE                │                      │
│         ▲             │   │ DESCRIPTION          │                      │
│         │             │   │ PRIORITY             │                      │
│         │             │   │ DUE_DATE / DUE_TIME  │                      │
│         │             │   │ IS_COMPLETED         │                      │
│         │             │   │ REPEAT_TYPE          │                      │
│         │             │   │ ...                  │                      │
│         │             │   │ TRANSFERRED_FROM (FK)│──┐ 이관 출처 사용자   │
│         │             │   │ TRANSFERRED_AT       │  │                   │
│         │             └───│ PARENT_TODO_ID (FK)──┼──┼─┐ 자기참조(반복)   │
│         │                 └──────────────────────┘  │ │                 │
│         │                           ▲               │ │                 │
│         │                           └───────────────┘ │                 │
│         │                           ▲                 │                 │
│         │                           └─────────────────┘                 │
│         │                                                               │
│         │                 ┌──────────────────────┐                      │
│         │                 │    TB_TODO_SHARE     │                      │
│         │                 ├──────────────────────┤                      │
│         └─────────────────│ SHARED_USER_ID (FK)  │  공유받은 사용자      │
│                           │ SHARE_ID (PK)        │                      │
│                           │ TODO_ID (FK) ────────┼──► TB_TODO           │
│                           │ PERMISSION           │  (VIEW/EDIT)         │
│                           │ SHARED_BY (FK)       │  공유해준 사용자      │
│                           └──────────────────────┘                      │
│                                                                         │
│  ┌──────────────┐         ┌──────────────────────┐                      │
│  │   TB_ITEM    │         │  TB_ITEM_CHECKLIST   │                      │
│  ├──────────────┤         ├──────────────────────┤                      │
│  │ ITEM_ID (PK) │◄────────│ ITEM_ID (FK)         │                      │
│  │ TITLE        │         │ CHECKLIST_ID (PK)    │                      │
│  └──────────────┘         │ TITLE                │                      │
│                           │ IS_COMPLETED         │                      │
│                           │ DUE_DATE             │                      │
│                           │ ASSIGNEE_USERNAME    │                      │
│                           │ SORT_ORDER           │                      │
│                           └──────────────────────┘                      │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 4. 테이블 정의서

### 4.1 TB_TODO (개인 Todo)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| TODO_ID | BIGINT | NO | AUTO_INCREMENT | PK |
| OWNER_USER_ID | BIGINT | NO | - | 소유자 (FK → TB_USER) |
| TITLE | VARCHAR(500) | NO | - | Todo 제목 |
| DESCRIPTION | TEXT | YES | NULL | 메모/상세 설명 |
| PRIORITY | VARCHAR(20) | NO | 'NORMAL' | 우선순위 (URGENT/HIGH/NORMAL/LOW) |
| DUE_DATE | DATE | YES | NULL | 마감일 |
| DUE_TIME | TIME | YES | NULL | 마감 시간 |
| IS_COMPLETED | BOOLEAN | NO | FALSE | 완료 여부 |
| COMPLETED_AT | DATETIME | YES | NULL | 완료 일시 |
| SORT_ORDER | INT | NO | 0 | 정렬 순서 |
| REPEAT_TYPE | VARCHAR(20) | YES | NULL | 반복 유형 |
| REPEAT_INTERVAL | INT | YES | 1 | 반복 간격 |
| REPEAT_DAYS | VARCHAR(50) | YES | NULL | 반복 요일 (1,2,3,4,5) |
| REPEAT_END_DATE | DATE | YES | NULL | 반복 종료일 |
| NEXT_DUE_DATE | DATE | YES | NULL | 다음 반복 마감일 |
| PARENT_TODO_ID | BIGINT | YES | NULL | 반복 원본 ID (FK → TB_TODO) |
| TRANSFERRED_FROM_USER_ID | BIGINT | YES | NULL | 이관 출처 사용자 (FK → TB_USER) |
| TRANSFERRED_AT | DATETIME | YES | NULL | 이관 일시 |
| USE_YN | CHAR(1) | NO | 'Y' | 사용 여부 (삭제 시 'N') |
| CREATED_AT | DATETIME | NO | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | VARCHAR(50) | NO | - | 생성자 USERNAME |
| UPDATED_AT | DATETIME | YES | ON UPDATE | 수정일시 |
| UPDATED_BY | VARCHAR(50) | YES | NULL | 수정자 USERNAME |

#### 반복 유형 (REPEAT_TYPE)

| 값 | 설명 | REPEAT_DAYS 사용 | 예시 |
|-----|------|------------------|------|
| NONE | 반복 없음 | - | 일회성 Todo |
| DAILY | 매일 | - | 매일 반복 |
| WEEKLY | 매주 | 요일 지정 (1~7) | 매주 월,수,금 |
| MONTHLY | 매월 | - | 매월 같은 날 |
| YEARLY | 매년 | - | 매년 같은 날 |

#### REPEAT_DAYS 값
- 1 = 월요일
- 2 = 화요일
- 3 = 수요일
- 4 = 목요일
- 5 = 금요일
- 6 = 토요일
- 7 = 일요일
- 예: "1,3,5" = 월, 수, 금

---

### 4.2 TB_TODO_SHARE (Todo 공유)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| SHARE_ID | BIGINT | NO | AUTO_INCREMENT | PK |
| TODO_ID | BIGINT | NO | - | Todo ID (FK → TB_TODO) |
| SHARED_USER_ID | BIGINT | NO | - | 공유받은 사용자 (FK → TB_USER) |
| SHARED_BY_USER_ID | BIGINT | NO | - | 공유해준 사용자 (FK → TB_USER) |
| PERMISSION | VARCHAR(20) | NO | 'VIEW' | 권한 (VIEW/EDIT) |
| CREATED_AT | DATETIME | NO | CURRENT_TIMESTAMP | 공유일시 |
| CREATED_BY | VARCHAR(50) | NO | - | 생성자 USERNAME |

#### 권한 (PERMISSION)

| 값 | 설명 |
|-----|------|
| VIEW | 조회만 가능 |
| EDIT | 조회 + 수정 가능 (완료 처리, 내용 수정) |

---

### 4.3 TB_ITEM_CHECKLIST (업무 내 체크리스트)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| CHECKLIST_ID | BIGINT | NO | AUTO_INCREMENT | PK |
| ITEM_ID | BIGINT | NO | - | 업무 ID (FK → TB_ITEM) |
| TITLE | VARCHAR(500) | NO | - | 체크리스트 항목 |
| IS_COMPLETED | BOOLEAN | NO | FALSE | 완료 여부 |
| COMPLETED_AT | DATETIME | YES | NULL | 완료 일시 |
| COMPLETED_BY | VARCHAR(50) | YES | NULL | 완료자 USERNAME |
| SORT_ORDER | INT | NO | 0 | 정렬 순서 |
| DUE_DATE | DATE | YES | NULL | 마감일 |
| ASSIGNEE_USERNAME | VARCHAR(50) | YES | NULL | 담당자 USERNAME |
| CREATED_AT | DATETIME | NO | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | VARCHAR(50) | NO | - | 생성자 |
| UPDATED_AT | DATETIME | YES | ON UPDATE | 수정일시 |
| UPDATED_BY | VARCHAR(50) | YES | NULL | 수정자 |

---

## 5. 인덱스 정의

```sql
-- TB_TODO
CREATE INDEX IDX_TODO_OWNER ON TB_TODO(OWNER_USER_ID);
CREATE INDEX IDX_TODO_DUE_DATE ON TB_TODO(DUE_DATE);
CREATE INDEX IDX_TODO_COMPLETED ON TB_TODO(IS_COMPLETED);
CREATE INDEX IDX_TODO_PARENT ON TB_TODO(PARENT_TODO_ID);
CREATE INDEX IDX_TODO_USE_YN ON TB_TODO(USE_YN);
CREATE INDEX IDX_TODO_TRANSFERRED ON TB_TODO(TRANSFERRED_FROM_USER_ID);

-- TB_TODO_SHARE
CREATE UNIQUE INDEX UQ_TODO_SHARE ON TB_TODO_SHARE(TODO_ID, SHARED_USER_ID);
CREATE INDEX IDX_TODO_SHARE_USER ON TB_TODO_SHARE(SHARED_USER_ID);
CREATE INDEX IDX_TODO_SHARE_BY ON TB_TODO_SHARE(SHARED_BY_USER_ID);

-- TB_ITEM_CHECKLIST
CREATE INDEX IDX_CHECKLIST_ITEM_ID ON TB_ITEM_CHECKLIST(ITEM_ID);
CREATE INDEX IDX_CHECKLIST_ASSIGNEE ON TB_ITEM_CHECKLIST(ASSIGNEE_USERNAME);
CREATE INDEX IDX_CHECKLIST_COMPLETED ON TB_ITEM_CHECKLIST(IS_COMPLETED);
```

---

## 6. DDL 스크립트

```sql
-- =============================================
-- TB_TODO (개인 Todo)
-- =============================================
CREATE TABLE TB_TODO (
    TODO_ID BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Todo ID',
    OWNER_USER_ID BIGINT NOT NULL COMMENT '소유자 USER_ID',
    TITLE VARCHAR(500) NOT NULL COMMENT 'Todo 제목',
    DESCRIPTION TEXT COMMENT '메모/상세 설명',
    PRIORITY VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '우선순위 (URGENT/HIGH/NORMAL/LOW)',
    DUE_DATE DATE COMMENT '마감일',
    DUE_TIME TIME COMMENT '마감 시간',
    IS_COMPLETED BOOLEAN NOT NULL DEFAULT FALSE COMMENT '완료 여부',
    COMPLETED_AT DATETIME COMMENT '완료 일시',
    SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    REPEAT_TYPE VARCHAR(20) COMMENT '반복 유형 (NONE/DAILY/WEEKLY/MONTHLY/YEARLY)',
    REPEAT_INTERVAL INT DEFAULT 1 COMMENT '반복 간격',
    REPEAT_DAYS VARCHAR(50) COMMENT '반복 요일 (1,2,3,4,5)',
    REPEAT_END_DATE DATE COMMENT '반복 종료일',
    NEXT_DUE_DATE DATE COMMENT '다음 반복 마감일',
    PARENT_TODO_ID BIGINT COMMENT '반복 원본 TODO_ID',
    TRANSFERRED_FROM_USER_ID BIGINT COMMENT '이관 출처 사용자',
    TRANSFERRED_AT DATETIME COMMENT '이관 일시',
    USE_YN CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    CREATED_BY VARCHAR(50) NOT NULL COMMENT '생성자',
    UPDATED_AT DATETIME ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    UPDATED_BY VARCHAR(50) COMMENT '수정자',
    CONSTRAINT FK_TODO_OWNER FOREIGN KEY (OWNER_USER_ID) REFERENCES TB_USER(USER_ID),
    CONSTRAINT FK_TODO_PARENT FOREIGN KEY (PARENT_TODO_ID) REFERENCES TB_TODO(TODO_ID),
    CONSTRAINT FK_TODO_TRANSFERRED_FROM FOREIGN KEY (TRANSFERRED_FROM_USER_ID) REFERENCES TB_USER(USER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='개인 Todo';

-- 인덱스
CREATE INDEX IDX_TODO_OWNER ON TB_TODO(OWNER_USER_ID);
CREATE INDEX IDX_TODO_DUE_DATE ON TB_TODO(DUE_DATE);
CREATE INDEX IDX_TODO_COMPLETED ON TB_TODO(IS_COMPLETED);
CREATE INDEX IDX_TODO_PARENT ON TB_TODO(PARENT_TODO_ID);
CREATE INDEX IDX_TODO_USE_YN ON TB_TODO(USE_YN);
CREATE INDEX IDX_TODO_TRANSFERRED ON TB_TODO(TRANSFERRED_FROM_USER_ID);

-- =============================================
-- TB_TODO_SHARE (Todo 공유)
-- =============================================
CREATE TABLE TB_TODO_SHARE (
    SHARE_ID BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '공유 ID',
    TODO_ID BIGINT NOT NULL COMMENT 'Todo ID',
    SHARED_USER_ID BIGINT NOT NULL COMMENT '공유받은 사용자',
    SHARED_BY_USER_ID BIGINT NOT NULL COMMENT '공유해준 사용자',
    PERMISSION VARCHAR(20) NOT NULL DEFAULT 'VIEW' COMMENT '권한 (VIEW/EDIT)',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '공유일시',
    CREATED_BY VARCHAR(50) NOT NULL COMMENT '생성자',
    CONSTRAINT FK_TODO_SHARE_TODO FOREIGN KEY (TODO_ID) REFERENCES TB_TODO(TODO_ID) ON DELETE CASCADE,
    CONSTRAINT FK_TODO_SHARE_USER FOREIGN KEY (SHARED_USER_ID) REFERENCES TB_USER(USER_ID),
    CONSTRAINT FK_TODO_SHARE_BY FOREIGN KEY (SHARED_BY_USER_ID) REFERENCES TB_USER(USER_ID),
    CONSTRAINT UQ_TODO_SHARE UNIQUE (TODO_ID, SHARED_USER_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Todo 공유';

-- 인덱스
CREATE INDEX IDX_TODO_SHARE_USER ON TB_TODO_SHARE(SHARED_USER_ID);
CREATE INDEX IDX_TODO_SHARE_BY ON TB_TODO_SHARE(SHARED_BY_USER_ID);

-- =============================================
-- TB_ITEM_CHECKLIST (업무 내 체크리스트)
-- =============================================
CREATE TABLE TB_ITEM_CHECKLIST (
    CHECKLIST_ID BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '체크리스트 ID',
    ITEM_ID BIGINT NOT NULL COMMENT '업무 ID',
    TITLE VARCHAR(500) NOT NULL COMMENT '체크리스트 항목',
    IS_COMPLETED BOOLEAN NOT NULL DEFAULT FALSE COMMENT '완료 여부',
    COMPLETED_AT DATETIME COMMENT '완료 일시',
    COMPLETED_BY VARCHAR(50) COMMENT '완료자 USERNAME',
    SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    DUE_DATE DATE COMMENT '마감일',
    ASSIGNEE_USERNAME VARCHAR(50) COMMENT '담당자 USERNAME',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    CREATED_BY VARCHAR(50) NOT NULL COMMENT '생성자',
    UPDATED_AT DATETIME ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    UPDATED_BY VARCHAR(50) COMMENT '수정자',
    CONSTRAINT FK_CHECKLIST_ITEM FOREIGN KEY (ITEM_ID) REFERENCES TB_ITEM(ITEM_ID) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='업무 내 체크리스트';

-- 인덱스
CREATE INDEX IDX_CHECKLIST_ITEM_ID ON TB_ITEM_CHECKLIST(ITEM_ID);
CREATE INDEX IDX_CHECKLIST_ASSIGNEE ON TB_ITEM_CHECKLIST(ASSIGNEE_USERNAME);
CREATE INDEX IDX_CHECKLIST_COMPLETED ON TB_ITEM_CHECKLIST(IS_COMPLETED);
```

---

## 7. 사용자 삭제 시 Todo 이관 처리

### 7.1 프로세스 흐름

```
┌─────────────────────────────────────────────────────────────┐
│                 사용자 삭제 프로세스                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 관리자가 사용자 삭제 요청                                 │
│                    ▼                                        │
│  2. 해당 사용자의 Todo 존재 여부 확인                         │
│     - GET /api/todos/count?userId={userId}                  │
│                    ▼                                        │
│  ┌─────────────────────────────────────┐                    │
│  │ Todo가 있는 경우                     │                    │
│  │  → 이관 대상 사용자 선택 모달 표시   │                    │
│  │  → 이관 대상 선택 또는 전체 삭제     │                    │
│  └─────────────────────────────────────┘                    │
│                    ▼                                        │
│  3-A. 이관 선택 시:                                          │
│       PUT /api/todos/transfer                               │
│       - TB_TODO.OWNER_USER_ID 변경                          │
│       - TB_TODO.TRANSFERRED_FROM_USER_ID 기록               │
│       - TB_TODO.TRANSFERRED_AT 기록                         │
│       - TB_TODO_SHARE에서 해당 사용자 공유 삭제              │
│                                                             │
│  3-B. 전체 삭제 선택 시:                                      │
│       DELETE /api/todos/by-user/{userId}                    │
│       - TB_TODO.USE_YN = 'N' 처리                           │
│       - TB_TODO_SHARE 관련 데이터 삭제                       │
│                    ▼                                        │
│  4. 사용자 삭제 완료                                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 이관 API

```
PUT /api/todos/transfer
{
  "fromUserId": 123,        // 삭제될 사용자
  "toUserId": 456,          // 이관받을 사용자
  "todoIds": [1, 2, 3]      // 선택적: 특정 Todo만 이관 (없으면 전체)
}
```

### 7.3 이관 모달 UI

```
┌─────────────────────────────────────────────────────────────┐
│ 사용자 삭제 - Todo 이관                              [X]    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ⚠️ "홍길동" 사용자에게 3개의 Todo가 있습니다.               │
│                                                             │
│  Todo 처리 방법을 선택하세요:                                │
│                                                             │
│  ○ 다른 사용자에게 이관                                      │
│     이관 대상: [사용자 선택 ▼]                               │
│                                                             │
│  ○ 모든 Todo 삭제                                           │
│                                                             │
│  ───────────────────────────────────────────────            │
│  이관될 Todo 목록:                                           │
│  ☑ 회의 자료 준비 (마감: 12/20)                             │
│  ☑ 프로젝트 보고서 (마감: 12/25)                            │
│  ☑ 인수인계 문서 작성 (마감: 12/30)                         │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                              [취소]  [확인]                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. 화면 설계 (개요)

### 8.1 개인 Todo List 화면

```
┌─────────────────────────────────────────────────────────────┐
│ ☑️ Todo List                    [공유받은 Todo ▼] [+ 새 Todo]│
├─────────────────────────────────────────────────────────────┤
│ 📅 오늘 (3)                                                 │
│ ├─ ☐ 회의 자료 준비      🔴 긴급   오늘 14:00    [⋮]       │
│ ├─ ☐ 이메일 답장         🟡 보통   오늘 18:00    [⋮]       │
│ └─ ☑ 점심 약속 확인      ✅ 완료                 [⋮]       │
│                                                             │
│ 📅 내일 (2)                                                 │
│ ├─ ☐ 주간 보고서 작성    🟠 높음   🔄 매주 월    [⋮]       │
│ └─ ☐ 코드 리뷰           🟡 보통                 [⋮]       │
│                                                             │
│ 📅 예정 (5)                                                 │
│ └─ ...                                                      │
│                                                             │
│ 📥 공유받은 Todo (2)                        [접기/펼치기]    │
│ ├─ ☐ 프로젝트 회의 참석   @김팀장   VIEW     12/20         │
│ └─ ☐ 자료 검토            @박대리   EDIT     12/22         │
│                                                             │
│ 📤 이관받은 Todo (1)                        [접기/펼치기]    │
│ └─ ☐ 인수인계 작업        from @이전임자     12/25         │
│                                                             │
│ ☑️ 완료됨 (12)                              [접기/펼치기]    │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 Todo 컨텍스트 메뉴

```
┌─────────────────────┐
│ ✏️ 수정              │
│ 📤 공유             │  ← 공유 기능
│ ↗️ 이관             │  ← 이관 기능
│ 📋 복제             │
│ ───────────────     │
│ 🗑️ 삭제             │
└─────────────────────┘
```

### 8.3 업무 내 체크리스트 (업무 상세 패널)

```
┌─────────────────────────────────────────┐
│ 업무 상세                               │
├─────────────────────────────────────────┤
│ 제목: 신규 기능 개발                     │
│ 상태: 진행중                            │
│ ...                                     │
├─────────────────────────────────────────┤
│ ☑️ 체크리스트 (2/5)        [+ 항목 추가] │
│ ├─ ☑ DB 스키마 설계          완료       │
│ ├─ ☑ API 개발                완료       │
│ ├─ ☐ 프론트엔드 개발         @김개발    │
│ ├─ ☐ 테스트 작성             12/25      │
│ └─ ☐ 문서화                            │
└─────────────────────────────────────────┘
```

---

## 9. 승인 요청

**1단계 ERD 및 테이블 정의서 승인해 주세요.**

- [ ] 테이블 구조 승인
- [ ] 컬럼 정의 승인
- [ ] 인덱스 정의 승인
- [ ] 사용자 삭제 시 이관 프로세스 승인

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2025-01-18 | Claude | 최초 작성 |
