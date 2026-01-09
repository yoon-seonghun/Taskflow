# TaskFlow 2.0 통합 설계서

> **작성일**: 2025-01-04
> **버전**: 2.0
> **상태**: 확정
> **병합 문서**: category-property-design.md, performance-score-design.md

---

## 1. 개요

### 1.1 목적
- 카테고리 기반 속성 관리 시스템 도입
- 성과 점수 및 지연 관리 시스템 구현
- 날짜 속성 변경 이력 관리

### 1.2 핵심 변경 사항
1. 속성 소유권을 보드에서 사용자/역할로 변경
2. 카테고리를 속성 그룹 컨테이너로 활용
3. 글로벌/매니저/사용자 3계층 속성 체계 도입
4. 성과 점수 시스템 (시간 기반 등급 + 가중치)
5. 속성 변경 이력 관리

---

## 2. 속성 계층 구조

### 2.1 속성 유형

| 유형 | 생성 권한 | 적용 범위 | 삭제 시 동작 |
|------|----------|----------|-------------|
| **기본 속성** | 시스템 | 전체 (필수) | 삭제 불가 |
| **글로벌 속성** | ADMIN | 전체 사용자 | 기존 업무 값 유지 |
| **매니저 속성** | MANAGER | 본인+하위부서 | 기존 업무 값 유지 |
| **사용자 속성** | USER | 본인 (카테고리 그룹화) | 기존 업무 값 유지 |

> **사용자 속성**: 개인이 생성한 속성. 카테고리에 그룹화되어 보드/업무에 활용됨. 미배정 시 "미사용" 분류.

### 2.2 기본 속성 (TB_ITEM 고정 컬럼)

| 속성명 | 컬럼 | 타입 | 필수 | 제거 가능 |
|--------|------|------|------|----------|
| 업무내용 | CONTENT | TEXT | O | X |
| 상세설명 | DESCRIPTION | TEXT | - | X |
| 상태 | STATUS | SELECT | O | X |
| 우선순위 | PRIORITY | SELECT | O | X |
| 요청일 | REQUEST_DATE | DATE | - | X |
| 마감일 | DUE_DATE | DATE | - | X |
| 담당자 | ASSIGNEE_USERNAME | USER | - | X |
| 그룹 | GROUP_ID | SELECT | - | X |
| 카테고리 | CATEGORY_ID | SELECT | - | X |

### 2.3 글로벌 속성 (시스템 제공)

| 속성명 | 타입 | 용도 | 옵션 |
|--------|------|------|------|
| 시작일 | DATE | 성과점수, 간트차트 | - |
| 완료일 | DATE | 성과점수 계산 | - |
| 난이도 | SELECT | 가중치 | Low(0.9), Medium(1.0), High(1.2), Extreme(1.4) |
| 범위변경 | SELECT | 가중치 | None(1.0), Minor(1.05), Major(1.15), Chaos(1.3) |
| 리스크대응 | SELECT | 가중치 | None(1.0), Mitigated(1.1), Critical(1.25) |

### 2.4 속성 접근 권한

```
사용자가 사용 가능한 속성:
1. 기본 속성 (전체 - 필수)
2. 글로벌 속성 (전체 - 선택)
3. 매니저 속성 (본인 부서 및 상위 부서에서 생성된 것)
4. 사용자 속성 (본인이 생성한 것)
5. 카테고리 속성 (선택한 카테고리에 배정된 속성)
```

---

## 3. 카테고리 시스템

### 3.1 카테고리 정의
- 속성을 그룹화하는 컨테이너
- 사용자별로 생성 및 관리
- 보드 공유와 동일한 방식으로 공유 가능

### 3.2 카테고리 공유
- 보드 공유 UI/UX와 동일한 구성
- 공유 대상: 사용자, 부서
- 공유 시 읽기/편집 권한 선택

### 3.3 카테고리-속성 매핑
- 카테고리에 속성 배정 시 기본값 지정 가능
- 기본값은 보드 → 업무까지 상속

---

## 4. 업무 흐름

### 4.1 속성 생성 → 카테고리 → 보드 → 업무

```
[1] 속성 생성
    ├─ ADMIN: 글로벌 속성 생성
    └─ MANAGER: 매니저 속성 생성 (부서/하위부서 범위)

[2] 카테고리 생성 & 속성 배정
    ├─ 사용자가 카테고리 생성
    ├─ 카테고리에 속성 등록 (글로벌/매니저 속성 선택)
    └─ 속성별 기본값 설정 (선택)

[3] 보드 생성
    ├─ 카테고리 선택
    ├─ 사용할 속성 선택 (기본 속성 필수, 나머지 선택)
    └─ 기본값 상속 (카테고리에서)

[4] 업무 생성
    ├─ 보드 카테고리 자동 상속
    ├─ 보드에서 선택된 속성 자동 적용
    ├─ 속성 편집으로 상속 속성 해제/추가 가능
    └─ 기본값 상속 (보드에서)
```

### 4.2 기본값 상속 흐름

```
카테고리 기본값 설정 (TB_CATEGORY_PROPERTY.DEFAULT_VALUE)
       ↓
보드 생성 (카테고리 선택)
       ↓ 기본값 복사
보드 속성에 기본값 저장 (TB_BOARD_PROPERTY.DEFAULT_VALUE)
       ↓
업무 생성
       ↓ 기본값 적용
업무 속성값으로 자동 입력 (TB_ITEM_PROPERTY.VALUE)
```

---

## 5. 통합 속성 선택 UI

### 5.1 설계 원칙
- 보드 생성/수정, 업무 생성/수정에서 동일한 속성 선택 패널 사용
- 전체 사용 가능한 속성, 선택된 속성, 미선택 속성 통합 표시
- "추가 속성 더보기" 별도 기능 불필요

### 5.2 통합 속성 선택 패널

```
┌─────────────────────────────────────────────────────────────┐
│ 속성 선택                                                    │
├─────────────────────────────────────────────────────────────┤
│ [기본 속성] ─────────────────────────────────────── 필수    │
│   ☑ 상태  ☑ 우선순위  ☑ 요청일  ☑ 마감일  ☑ 담당자  ☑ 그룹 │
│                                                             │
│ [글로벌 속성] ──────────────────────────────────────────── │
│   ☐ 시작일  ☐ 완료일  ☐ 난이도  ☐ 범위변경  ☐ 리스크대응   │
│                                                             │
│ [매니저 속성] ──────────────────────────────────────────── │
│   ☐ 검토상태  ☐ 승인자                                     │
│                                                             │
│ [카테고리 속성] ─────────────────────── "개발업무"          │
│   ☐ 코드리뷰상태 (기본값: 대기)                            │
│   ☐ 브랜치명     (기본값: feature/)                        │
│   ☐ PR링크                                                 │
└─────────────────────────────────────────────────────────────┘
```

### 5.3 컨텍스트별 차이

| 컨텍스트 | 패널 표시 방식 | 버튼 |
|----------|---------------|------|
| 보드 생성 | 생성 폼 내 인라인 | 없음 (생성 버튼으로 통합) |
| 보드 수정 | 모달/슬라이드오버 | [취소] [적용] |
| 업무 생성 | 생성 폼 내 접힘/펼침 | 없음 (등록 버튼으로 통합) |
| 업무 수정 | 모달/슬라이드오버 | [취소] [적용] |

### 5.4 선택 상태 시각적 구분

| 상태 | 표시 | 색상 |
|------|------|------|
| 필수 (기본 속성) | ☑ (비활성) | 회색 배경 |
| 상속 선택됨 | ☑ | 파란색 체크 |
| 상속 해제함 | ☐ | 취소선, 연한 텍스트 |
| 미선택 | ☐ | 기본 |
| 추가 선택 | ☑ | 녹색 체크, 점선 테두리 |

### 5.5 업무 생성 시 접힘/펼침

```
[기본 상태 - 접힘]
▶ 속성 편집  (3개 사용 중 / 전체 12개)

[펼침]
▼ 속성 편집  (3개 사용 중 / 전체 12개)
┌─────────────────────────────────────────┐
│ [기본 속성] ...                         │
│ [글로벌 속성] ...                       │
│ ...                                     │
└─────────────────────────────────────────┘
```

---

## 6. 성과 점수 시스템

### 6.1 시간 기반 점수 등급

| 등급 | 기준 | 기본 점수 |
|------|------|----------|
| **Perfect** | 당일 완료 | 100 |
| **Excellent** | 기간의 50% 이내 | 90 |
| **Great** | 기간의 80% 이내 | 80 |
| **Good** | 마감일 준수 | 70 |
| **Fair** | 2일 이내 초과 | 50 |
| **Poor** | 3일 이상 초과 | 30 |

### 6.2 완료비율 계산

```
총기간 = 마감일 - 시작일
완료비율 = (완료일 - 시작일) / 총기간

예시:
- 시작일: 1월 1일
- 마감일: 1월 10일 (총 9일)
- 완료일: 1월 5일
- 완료비율 = 4/9 ≈ 0.44 → Excellent
```

### 6.3 가중치

#### 난이도 (Difficulty)
| 레벨 | 가중치 | 승인 |
|------|--------|------|
| Low | 0.9 | - |
| Medium | 1.0 | - |
| High | 1.2 | - |
| **Extreme** | 1.4 | PM 승인 필요 |

#### 범위 변경 (Scope Change)
| 레벨 | 가중치 | 승인 |
|------|--------|------|
| None | 1.0 | - |
| Minor | 1.05 | - |
| Major | 1.15 | - |
| **Chaos** | 1.3 | PM 승인 필요 |

#### 리스크 대응 (Risk Handling)
| 레벨 | 가중치 |
|------|--------|
| None | 1.0 |
| Mitigated | 1.1 |
| Critical | 1.25 |

### 6.4 최종 점수 계산

```
최종 점수 = 시간기반 점수 × 난이도 × 범위변경 × 리스크대응

예시:
- 시간기반: Good (70)
- 난이도: High (1.2)
- 범위변경: Minor (1.05)
- 리스크: Mitigated (1.1)
- 최종 점수 = 70 × 1.2 × 1.05 × 1.1 = 97.02
```

### 6.5 PM 승인제

| 역할 | 승인 권한 |
|------|----------|
| 팀장 | O |
| MANAGER (role) | O |
| ADMIN (role) | O |

| 항목 | 승인 필요 조건 |
|------|---------------|
| 난이도 | Extreme 선택 시 |
| 범위변경 | Chaos 선택 시 |

### 6.6 점수 공개 범위

| 역할 | 조회 가능 범위 |
|------|---------------|
| 본인 | 본인 점수 |
| 팀장 | 팀원 점수 |
| MANAGER | 부서 점수 |
| ADMIN | 전체 점수 |

---

## 7. 지연 업무 관리

### 7.1 지연 판정 기준

```
지연 조건:
1. 상태가 COMPLETED, DELETED가 아님 (미완료)
2. 마감일이 설정됨
3. 마감일 < 현재 날짜
```

### 7.2 지연 등급 표시

| 지연 일수 | 표시 색상 | 등급 |
|----------|----------|------|
| 1~2일 | 노랑 | 경고 |
| 3~6일 | 주황 | 주의 |
| 7일 이상 | 빨강 | 위험 |

---

## 8. 이력 관리

### 8.1 관리 대상 속성

| 속성 | 이력 관리 | 용도 |
|------|----------|------|
| 요청일 | O | 일정 변경 추적 |
| 시작일 | O | 간트 차트, 일정 변경 |
| 완료일 | O | 성과 점수 계산 근거 |
| 마감일 | O | 간트 차트, 일정 변경 |
| 상태 | O | 진행 상황 추적 |
| 난이도 | O | 변경 사유 확인 |
| 범위변경 | O | 변경 사유 확인 |

### 8.2 이벤트 유형

| 이벤트 | 설명 |
|--------|------|
| CREATE | 업무 생성 |
| INITIAL | 속성 최초 입력 |
| UPDATE | 값 변경 |
| COMPLETE | 완료 처리 |

---

## 9. DB 스키마

### 9.1 TB_PROPERTY_DEF 수정

```sql
-- 신규 컬럼 추가
ALTER TABLE TB_PROPERTY_DEF
  MODIFY BOARD_ID BIGINT NULL COMMENT '보드 ID (NULL: 보드 미귀속)',
  ADD COLUMN OWNER_TYPE VARCHAR(20) NOT NULL DEFAULT 'USER'
      COMMENT '소유 유형 (GLOBAL, MANAGER, USER)' AFTER BOARD_ID,
  ADD COLUMN OWNER_USERNAME VARCHAR(50) NULL
      COMMENT '소유자 USERNAME (GLOBAL은 NULL)' AFTER OWNER_TYPE,
  ADD COLUMN OWNER_DEPT_CODE VARCHAR(20) NULL
      COMMENT '매니저 속성의 부서 코드' AFTER OWNER_USERNAME;

CREATE INDEX IDX_PROP_OWNER_TYPE ON TB_PROPERTY_DEF (OWNER_TYPE);
CREATE INDEX IDX_PROP_OWNER_USER ON TB_PROPERTY_DEF (OWNER_USERNAME);
CREATE INDEX IDX_PROP_OWNER_DEPT ON TB_PROPERTY_DEF (OWNER_DEPT_CODE);
```

### 9.2 TB_CATEGORY (신규)

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

### 9.3 TB_CATEGORY_SHARE (신규)

```sql
CREATE TABLE TB_CATEGORY_SHARE (
    SHARE_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '공유 ID',
    CATEGORY_ID BIGINT NOT NULL COMMENT '카테고리 ID',
    SHARE_TYPE VARCHAR(20) NOT NULL COMMENT '공유 대상 유형 (USER, DEPARTMENT)',
    SHARE_TARGET VARCHAR(50) NOT NULL COMMENT '공유 대상 (USERNAME 또는 DEPT_CODE)',
    PERMISSION VARCHAR(20) NOT NULL DEFAULT 'READ' COMMENT '권한 (READ, EDIT)',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR(50) NOT NULL,
    PRIMARY KEY (SHARE_ID),
    UNIQUE KEY UK_CAT_SHARE (CATEGORY_ID, SHARE_TYPE, SHARE_TARGET),
    INDEX IDX_CAT_SHARE_TARGET (SHARE_TYPE, SHARE_TARGET),
    FOREIGN KEY (CATEGORY_ID) REFERENCES TB_CATEGORY(CATEGORY_ID) ON DELETE CASCADE
) COMMENT '카테고리 공유';
```

### 9.4 TB_CATEGORY_PROPERTY (신규)

```sql
CREATE TABLE TB_CATEGORY_PROPERTY (
    CATEGORY_PROPERTY_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
    CATEGORY_ID BIGINT NOT NULL COMMENT '카테고리 ID',
    PROPERTY_ID BIGINT NOT NULL COMMENT '속성 ID',
    SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '속성 표시 순서',
    DEFAULT_VALUE VARCHAR(500) NULL COMMENT '기본값',
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

### 9.5 TB_BOARD_CATEGORY (신규)

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

### 9.6 TB_BOARD_PROPERTY (신규)

```sql
CREATE TABLE TB_BOARD_PROPERTY (
    BOARD_PROPERTY_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '매핑 ID',
    BOARD_ID BIGINT NOT NULL COMMENT '보드 ID',
    PROPERTY_ID BIGINT NOT NULL COMMENT '속성 ID',
    REQUIRED_YN CHAR(1) NOT NULL DEFAULT 'N' COMMENT '필수 여부 (보드 레벨)',
    VISIBLE_YN CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '표시 여부',
    SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '표시 순서',
    DEFAULT_VALUE VARCHAR(500) NULL COMMENT '기본값 (카테고리에서 상속)',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR(50) NOT NULL,
    PRIMARY KEY (BOARD_PROPERTY_ID),
    UNIQUE KEY UK_BOARD_PROP (BOARD_ID, PROPERTY_ID),
    INDEX IDX_BOARD_PROP_BOARD (BOARD_ID),
    INDEX IDX_BOARD_PROP_PROP (PROPERTY_ID),
    FOREIGN KEY (BOARD_ID) REFERENCES TB_BOARD(BOARD_ID) ON DELETE CASCADE,
    FOREIGN KEY (PROPERTY_ID) REFERENCES TB_PROPERTY_DEF(PROPERTY_ID) ON DELETE CASCADE
) COMMENT '보드-속성 매핑';
```

### 9.7 TB_ITEM_PROPERTY_HISTORY (신규)

```sql
CREATE TABLE TB_ITEM_PROPERTY_HISTORY (
    HISTORY_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '이력 ID',
    ITEM_ID BIGINT NOT NULL COMMENT '업무 ID',
    PROPERTY_ID BIGINT NULL COMMENT '속성 ID (NULL이면 기본 속성)',
    PROPERTY_NAME VARCHAR(100) NOT NULL COMMENT '속성명 (스냅샷)',
    PROPERTY_TYPE VARCHAR(20) NOT NULL COMMENT '속성 타입',
    EVENT_TYPE VARCHAR(20) NOT NULL COMMENT 'CREATE, INITIAL, UPDATE, COMPLETE',
    OLD_VALUE_TEXT VARCHAR(1000) NULL COMMENT '이전 텍스트값',
    NEW_VALUE_TEXT VARCHAR(1000) NULL COMMENT '새 텍스트값',
    OLD_VALUE_DATE DATE NULL COMMENT '이전 날짜값',
    NEW_VALUE_DATE DATE NULL COMMENT '새 날짜값',
    OLD_VALUE_NUMBER DECIMAL(18,4) NULL COMMENT '이전 숫자값',
    NEW_VALUE_NUMBER DECIMAL(18,4) NULL COMMENT '새 숫자값',
    CHANGE_REASON VARCHAR(500) NULL COMMENT '변경 사유',
    CHANGED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '변경일시',
    CHANGED_BY VARCHAR(50) NOT NULL COMMENT '변경자 USERNAME',
    PRIMARY KEY (HISTORY_ID),
    INDEX IDX_HIST_ITEM (ITEM_ID, CHANGED_AT),
    INDEX IDX_HIST_PROP (PROPERTY_ID),
    INDEX IDX_HIST_EVENT (EVENT_TYPE),
    FOREIGN KEY (ITEM_ID) REFERENCES TB_ITEM(ITEM_ID) ON DELETE CASCADE
) COMMENT '업무 속성 변경 이력';
```

### 9.8 TB_ITEM_SCORE (신규)

```sql
CREATE TABLE TB_ITEM_SCORE (
    SCORE_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '점수 ID',
    ITEM_ID BIGINT NOT NULL COMMENT '업무 ID',
    TIME_GRADE VARCHAR(20) NOT NULL COMMENT 'PERFECT, EXCELLENT, GREAT, GOOD, FAIR, POOR',
    TIME_SCORE DECIMAL(5,2) NOT NULL COMMENT '시간 기반 점수 (0~100)',
    COMPLETION_RATIO DECIMAL(5,4) NULL COMMENT '완료비율',
    DIFFICULTY_CODE VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW, MEDIUM, HIGH, EXTREME',
    DIFFICULTY_WEIGHT DECIMAL(3,2) NOT NULL DEFAULT 1.0,
    SCOPE_CODE VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT 'NONE, MINOR, MAJOR, CHAOS',
    SCOPE_WEIGHT DECIMAL(3,2) NOT NULL DEFAULT 1.0,
    RISK_CODE VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT 'NONE, MITIGATED, CRITICAL',
    RISK_WEIGHT DECIMAL(3,2) NOT NULL DEFAULT 1.0,
    FINAL_SCORE DECIMAL(6,2) NOT NULL COMMENT '최종 점수',
    REQUEST_DATE DATE NULL COMMENT '요청일',
    START_DATE DATE NULL COMMENT '시작일',
    COMPLETION_DATE DATE NULL COMMENT '완료일',
    DUE_DATE DATE NULL COMMENT '마감일',
    ACTUAL_COMPLETION_AT DATETIME NULL COMMENT '실제 완료 처리일시',
    REQUIRES_APPROVAL CHAR(1) NOT NULL DEFAULT 'N' COMMENT '승인 필요 여부',
    APPROVED_YN CHAR(1) NULL COMMENT '승인 여부',
    APPROVED_BY VARCHAR(50) NULL COMMENT '승인자',
    APPROVED_AT DATETIME NULL COMMENT '승인일시',
    CALCULATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CALCULATED_BY VARCHAR(50) NOT NULL,
    PRIMARY KEY (SCORE_ID),
    UNIQUE KEY UK_ITEM_SCORE (ITEM_ID),
    INDEX IDX_SCORE_GRADE (TIME_GRADE),
    INDEX IDX_SCORE_CALC (CALCULATED_AT),
    FOREIGN KEY (ITEM_ID) REFERENCES TB_ITEM(ITEM_ID) ON DELETE CASCADE
) COMMENT '업무 성과 점수';
```

---

## 10. API 설계

### 10.1 속성 관리 API

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

# 사용 가능한 전체 속성 조회
GET    /api/properties/available           # 현재 사용자가 사용 가능한 모든 속성
```

### 10.2 카테고리 관리 API

```
GET    /api/categories                     # 카테고리 목록
POST   /api/categories                     # 카테고리 생성
GET    /api/categories/{id}                # 카테고리 상세
PUT    /api/categories/{id}                # 카테고리 수정
DELETE /api/categories/{id}                # 카테고리 삭제

# 카테고리-속성 매핑
GET    /api/categories/{id}/properties     # 카테고리 속성 목록
POST   /api/categories/{id}/properties     # 카테고리에 속성 추가
PUT    /api/categories/{id}/properties/{propId}  # 속성 기본값 수정
DELETE /api/categories/{id}/properties/{propId}  # 카테고리에서 속성 제거
PUT    /api/categories/{id}/properties/order     # 속성 순서 변경

# 카테고리 공유
GET    /api/categories/{id}/shares         # 공유 목록
POST   /api/categories/{id}/shares         # 공유 추가
DELETE /api/categories/{id}/shares/{shareId}  # 공유 해제
```

### 10.3 보드-카테고리/속성 API

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

### 10.4 성과 점수 API

```
GET    /api/items/{itemId}/score          # 업무 성과 점수 조회
POST   /api/items/{itemId}/score/calculate  # 성과 점수 계산
PUT    /api/items/{itemId}/score/weights   # 가중치 수정
POST   /api/items/{itemId}/score/approve   # 승인 처리 (PM)
POST   /api/items/{itemId}/score/reject    # 반려 처리 (PM)
GET    /api/scores/statistics              # 점수 통계
GET    /api/scores/ranking                 # 순위 조회
```

### 10.5 이력 관리 API

```
GET    /api/items/{itemId}/history         # 업무 속성 변경 이력
GET    /api/items/{itemId}/history/dates   # 날짜 속성 변경 이력 (간트용)
```

---

## 11. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 2.0 | 2025-01-04 | 통합 설계서 작성 (category-property + performance-score 병합) |
