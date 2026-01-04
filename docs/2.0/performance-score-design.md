# 성과 점수 및 지연 관리 시스템 설계서

> **작성일**: 2025-01-04
> **버전**: 1.0
> **상태**: 설계 검토 중
> **관련 문서**: category-property-design.md (병합 예정)

---

## 1. 개요

### 1.1 목적
- 업무 완료 성과를 정량적으로 평가
- 지연 업무 관리 및 알림
- 날짜 속성 변경 이력 관리 (간트 차트 등 활용)

### 1.2 핵심 기능
1. **성과 점수 시스템**: 완료 시점 기반 등급 산정 + 가중치 적용
2. **지연 업무 관리**: 미완료 업무의 지연 계산
3. **날짜 속성 이력 관리**: 시작일/마감일 변경 추적

---

## 2. 성과 점수 시스템

### 2.1 시간 기반 점수 등급

| 등급 | 기준 | 설명 |
|------|------|------|
| **Perfect** | 당일 완료 | 완료비율 ≤ 0 |
| **Excellent** | 기간의 50% 이내 | 완료비율 ≤ 0.5 |
| **Great** | 기간의 80% 이내 | 완료비율 ≤ 0.8 |
| **Good** | 마감일 준수 | 완료비율 ≤ 1.0 |
| **Fair** | 2일 이내 초과 | 완료비율 ≤ 1.0 + 2일 |
| **Poor** | 3일 이상 초과 | 그 외 |

### 2.2 완료비율 계산

```
총기간 = 마감일 - 시작일
완료비율 = (완료일 - 시작일) / 총기간

예시:
- 시작일: 1월 1일
- 마감일: 1월 10일 (총 9일)
- 완료일: 1월 5일

완료비율 = (5일 - 1일) / (10일 - 1일) = 4/9 ≈ 0.44 → Excellent
```

### 2.3 가중치 항목

#### 난이도 (Difficulty)
| 레벨 | 가중치 | 설명 | 승인 |
|------|--------|------|------|
| Low | 0.9 | 반복 작업 | - |
| Medium | 1.0 | 일반 업무 | - |
| High | 1.2 | 높은 난이도 | - |
| **Extreme** | 1.4 | 매우 어려움 | PM 승인 필요 |

#### 범위 변경 (Scope Change)
| 레벨 | 가중치 | 설명 | 승인 |
|------|--------|------|------|
| None | 1.0 | 변경 없음 | - |
| Minor | 1.05 | 20% 이내 변경 | - |
| Major | 1.15 | 30% 이상 변경 | - |
| **Chaos** | 1.3 | 완전히 엎어짐 | PM 승인 필요 |

#### 리스크 대응 (Risk Handling)
| 레벨 | 가중치 | 설명 |
|------|--------|------|
| None | 1.0 | 이슈 없음 |
| Mitigated | 1.1 | 이슈 해결 |
| Critical | 1.25 | 실패 복구 |

### 2.4 최종 점수 계산

```
최종 점수 = 시간기반 점수 × 난이도 가중치 × 범위변경 가중치 × 리스크대응 가중치

예시:
- 시간기반: Good (1.0)
- 난이도: High (1.2)
- 범위변경: Minor (1.05)
- 리스크: Mitigated (1.1)

최종 점수 = 1.0 × 1.2 × 1.05 × 1.1 = 1.386
```

### 2.5 PM 승인제

| 항목 | 승인 필요 조건 |
|------|---------------|
| 난이도 | Extreme 선택 시 |
| 범위변경 | Chaos 선택 시 |

```
승인 프로세스:
1. 담당자가 Extreme/Chaos 선택
2. 시스템이 PM에게 승인 요청 발송
3. PM 승인 후 값 적용
4. 미승인 시 이전 값 유지
```

---

## 3. 날짜 속성 정의

### 3.1 날짜 속성 분류

| 속성명 | 속성 유형 | TB_ITEM 컬럼 | 필수 |
|--------|----------|--------------|------|
| **요청일** | 기본 속성 | REQUEST_DATE | - |
| **시작일** | 추가 속성 (글로벌) | - | - |
| **완료일** | 추가 속성 (글로벌) | - | - |
| **마감일** | 기본 속성 | DUE_DATE (= END_TIME) | - |

### 3.2 날짜 선택 제약 조건

| 속성 | 과거 선택 | 미래 선택 | 기타 제약 |
|------|----------|----------|----------|
| 요청일 | O | **X** | 완료일보다 미래 불가 |
| 시작일 | O | O | 완료일보다 미래 불가 (마감일보다는 미래 가능) |
| 완료일 | **X** (요청일/시작일 이후) | **X** | 요청일/시작일보다 과거 불가 |
| 마감일 | O | O | - |

### 3.3 날짜 미등록 시 처리

| 상황 | 처리 방법 | 경고 메시지 |
|------|----------|------------|
| 모든 날짜 미등록 | 상태 "시작전" 유지 | 속성 변경 시 시작일을 등록일로 설정됨 경고 |
| 완료일 미등록 + 완료 처리 | 완료 처리일 = 완료일 | "현재 날짜로 완료됨" 경고 |
| 마감일 미등록 + 완료 처리 | 완료 처리일 기준 계산 | "현재 날짜로 완료됨" 경고 |
| 요청일/시작일 미등록 + 완료 처리 | 등록일 ~ 완료 처리일 기준 | "등록일 ~ 현재 날짜로 계산됨" 경고 |

### 3.4 날짜 계산 알고리즘

```
[완료일 결정 우선순위]
1. 완료일 속성값 (사용자 입력)
2. 완료 처리일 (시스템 자동)

[시작일 결정 우선순위]
1. 시작일 속성값 (사용자 입력)
2. 요청일 속성값
3. 업무 등록일 (CREATED_AT)

[마감일 결정 우선순위]
1. 마감일 속성값 (사용자 입력)
2. NULL (지연 계산 제외)
```

---

## 4. 지연 업무 관리

### 4.1 지연 판정 기준

```
지연 조건:
1. 상태가 COMPLETED, DELETED가 아님 (미완료)
2. 마감일이 설정됨
3. 마감일 < 현재 날짜
```

### 4.2 지연 계산 시점

| 시점 | 대상 |
|------|------|
| 날짜 속성 변경 시 | 해당 업무 |
| 업무 저장 시 | 해당 업무 |
| 사용자 로그인 시 | 보유 중인 미완료 업무 전체 |

### 4.3 지연 일수 계산

```
지연 일수 = 현재 날짜 - 마감일

예시:
- 마감일: 1월 5일
- 현재: 1월 8일
- 지연 일수: 3일
```

### 4.4 지연 등급 표시

| 지연 일수 | 표시 색상 | 등급 |
|----------|----------|------|
| 1~2일 | 노랑 | 경고 |
| 3~6일 | 주황 | 주의 |
| 7일 이상 | 빨강 | 위험 |

---

## 5. 이력 관리

### 5.1 관리 대상 속성

| 속성 | 이력 관리 | 용도 |
|------|----------|------|
| 요청일 | O | 일정 변경 추적 |
| 시작일 | O | 간트 차트, 일정 변경 추적 |
| 완료일 | O | 성과 점수 계산 근거 |
| 마감일 | O | 간트 차트, 일정 변경 추적 |
| 상태 | O | 진행 상황 추적 |
| 난이도 | O | 변경 사유 확인 |
| 범위변경 | O | 변경 사유 확인 |

### 5.2 이벤트 유형

| 이벤트 | 설명 | 기록 내용 |
|--------|------|----------|
| CREATE | 업무 생성 | 초기값 |
| INITIAL | 최초 등록 | 속성 최초 입력 |
| UPDATE | 변경 | 이전값 → 새값 |
| COMPLETE | 완료 | 최종값 |

### 5.3 활용 예시

#### 간트 차트
```
업무 A:
├─ 최초 계획: 1/1 ~ 1/10
├─ 1차 변경: 1/1 ~ 1/15 (마감일 연장)
└─ 실제 완료: 1/1 ~ 1/12

→ 간트 차트에서 계획 변경 이력을 시각화
```

#### 변경 사유 확인
```
업무 B:
├─ 난이도: Medium → High (변경 사유: 요구사항 추가)
├─ 범위변경: None → Major (변경 사유: 기획 변경)
└─ 마감일: 1/10 → 1/20 (변경 사유: 범위 확대)
```

---

## 6. DB 스키마 설계

### 6.1 TB_ITEM 수정

```sql
-- 기존 END_TIME을 DUE_DATE로 명확화 (또는 유지)
-- REQUEST_DATE 추가

ALTER TABLE TB_ITEM
  ADD COLUMN REQUEST_DATE DATE NULL COMMENT '요청일' AFTER GROUP_ID;

-- 기존 END_TIME은 DUE_DATE 역할 (컬럼명 변경 또는 유지)
-- START_TIME은 기본 속성에서 제거 (추가 속성으로 이동)
-- → 기존 데이터 마이그레이션 필요 시 별도 처리
```

### 6.2 TB_ITEM_PROPERTY_HISTORY (신규)

```sql
CREATE TABLE TB_ITEM_PROPERTY_HISTORY (
    HISTORY_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '이력 ID',
    ITEM_ID BIGINT NOT NULL COMMENT '업무 ID',

    -- 속성 정보
    PROPERTY_ID BIGINT NULL COMMENT '속성 ID (NULL이면 기본 속성)',
    PROPERTY_NAME VARCHAR(100) NOT NULL COMMENT '속성명 (스냅샷)',
    PROPERTY_TYPE VARCHAR(20) NOT NULL COMMENT '속성 타입',

    -- 이벤트 정보
    EVENT_TYPE VARCHAR(20) NOT NULL COMMENT 'CREATE, INITIAL, UPDATE, COMPLETE',

    -- 값 정보
    OLD_VALUE_TEXT VARCHAR(1000) NULL COMMENT '이전 텍스트값',
    NEW_VALUE_TEXT VARCHAR(1000) NULL COMMENT '새 텍스트값',
    OLD_VALUE_DATE DATE NULL COMMENT '이전 날짜값',
    NEW_VALUE_DATE DATE NULL COMMENT '새 날짜값',
    OLD_VALUE_NUMBER DECIMAL(18,4) NULL COMMENT '이전 숫자값',
    NEW_VALUE_NUMBER DECIMAL(18,4) NULL COMMENT '새 숫자값',

    -- 변경 정보
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

### 6.3 TB_ITEM_SCORE (신규)

```sql
CREATE TABLE TB_ITEM_SCORE (
    SCORE_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '점수 ID',
    ITEM_ID BIGINT NOT NULL COMMENT '업무 ID',

    -- 시간 기반 점수
    TIME_GRADE VARCHAR(20) NOT NULL COMMENT 'PERFECT, EXCELLENT, GREAT, GOOD, FAIR, POOR',
    TIME_SCORE DECIMAL(5,2) NOT NULL COMMENT '시간 기반 점수 (0~100)',
    COMPLETION_RATIO DECIMAL(5,4) NULL COMMENT '완료비율',

    -- 가중치
    DIFFICULTY_CODE VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW, MEDIUM, HIGH, EXTREME',
    DIFFICULTY_WEIGHT DECIMAL(3,2) NOT NULL DEFAULT 1.0 COMMENT '난이도 가중치',
    SCOPE_CODE VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT 'NONE, MINOR, MAJOR, CHAOS',
    SCOPE_WEIGHT DECIMAL(3,2) NOT NULL DEFAULT 1.0 COMMENT '범위변경 가중치',
    RISK_CODE VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT 'NONE, MITIGATED, CRITICAL',
    RISK_WEIGHT DECIMAL(3,2) NOT NULL DEFAULT 1.0 COMMENT '리스크 가중치',

    -- 최종 점수
    FINAL_SCORE DECIMAL(6,2) NOT NULL COMMENT '최종 점수',

    -- 계산 기준 날짜 (스냅샷)
    REQUEST_DATE DATE NULL COMMENT '요청일',
    START_DATE DATE NULL COMMENT '시작일',
    COMPLETION_DATE DATE NULL COMMENT '완료일',
    DUE_DATE DATE NULL COMMENT '마감일',
    ACTUAL_COMPLETION_AT DATETIME NULL COMMENT '실제 완료 처리일시',

    -- 승인 정보
    REQUIRES_APPROVAL CHAR(1) NOT NULL DEFAULT 'N' COMMENT '승인 필요 여부',
    APPROVED_YN CHAR(1) NULL COMMENT '승인 여부 (Y/N/NULL)',
    APPROVED_BY VARCHAR(50) NULL COMMENT '승인자 USERNAME',
    APPROVED_AT DATETIME NULL COMMENT '승인일시',

    -- 계산 정보
    CALCULATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '계산일시',
    CALCULATED_BY VARCHAR(50) NOT NULL COMMENT '계산자 USERNAME',

    PRIMARY KEY (SCORE_ID),
    UNIQUE KEY UK_ITEM_SCORE (ITEM_ID),
    INDEX IDX_SCORE_GRADE (TIME_GRADE),
    INDEX IDX_SCORE_CALC (CALCULATED_AT),
    FOREIGN KEY (ITEM_ID) REFERENCES TB_ITEM(ITEM_ID) ON DELETE CASCADE
) COMMENT '업무 성과 점수';
```

### 6.4 글로벌 속성 초기 데이터

```sql
-- 성과 관련 글로벌 속성 (02_init_data.sql에 추가)

-- 시작일 속성
INSERT INTO TB_PROPERTY_DEF (
    BOARD_ID, OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE,
    PROPERTY_NAME, PROPERTY_TYPE, REQUIRED_YN, VISIBLE_YN, USE_YN, SORT_ORDER,
    CREATED_BY
) VALUES (
    NULL, 'GLOBAL', NULL, NULL,
    '시작일', 'DATE', 'N', 'Y', 'Y', 1,
    'system'
);

-- 완료일 속성
INSERT INTO TB_PROPERTY_DEF (
    BOARD_ID, OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE,
    PROPERTY_NAME, PROPERTY_TYPE, REQUIRED_YN, VISIBLE_YN, USE_YN, SORT_ORDER,
    CREATED_BY
) VALUES (
    NULL, 'GLOBAL', NULL, NULL,
    '완료일', 'DATE', 'N', 'Y', 'Y', 2,
    'system'
);

-- 난이도 속성
INSERT INTO TB_PROPERTY_DEF (
    BOARD_ID, OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE,
    PROPERTY_NAME, PROPERTY_TYPE, REQUIRED_YN, VISIBLE_YN, USE_YN, SORT_ORDER,
    CREATED_BY
) VALUES (
    NULL, 'GLOBAL', NULL, NULL,
    '난이도', 'SELECT', 'N', 'Y', 'Y', 3,
    'system'
);

-- 난이도 옵션
INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_VALUE, OPTION_LABEL, OPTION_COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'LOW', 'Low (반복작업)', '#22C55E', 1, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '난이도' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_VALUE, OPTION_LABEL, OPTION_COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'MEDIUM', 'Medium (일반)', '#3B82F6', 2, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '난이도' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_VALUE, OPTION_LABEL, OPTION_COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'HIGH', 'High (높음)', '#F59E0B', 3, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '난이도' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_VALUE, OPTION_LABEL, OPTION_COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'EXTREME', 'Extreme (어려움)', '#EF4444', 4, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '난이도' AND OWNER_TYPE = 'GLOBAL';

-- 범위변경 속성
INSERT INTO TB_PROPERTY_DEF (
    BOARD_ID, OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE,
    PROPERTY_NAME, PROPERTY_TYPE, REQUIRED_YN, VISIBLE_YN, USE_YN, SORT_ORDER,
    CREATED_BY
) VALUES (
    NULL, 'GLOBAL', NULL, NULL,
    '범위변경', 'SELECT', 'N', 'Y', 'Y', 4,
    'system'
);

-- 범위변경 옵션
INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_VALUE, OPTION_LABEL, OPTION_COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'NONE', 'None (변경없음)', '#6B7280', 1, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '범위변경' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_VALUE, OPTION_LABEL, OPTION_COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'MINOR', 'Minor (20%이내)', '#22C55E', 2, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '범위변경' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_VALUE, OPTION_LABEL, OPTION_COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'MAJOR', 'Major (30%이상)', '#F59E0B', 3, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '범위변경' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_VALUE, OPTION_LABEL, OPTION_COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'CHAOS', 'Chaos (엎어짐)', '#EF4444', 4, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '범위변경' AND OWNER_TYPE = 'GLOBAL';

-- 리스크대응 속성
INSERT INTO TB_PROPERTY_DEF (
    BOARD_ID, OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE,
    PROPERTY_NAME, PROPERTY_TYPE, REQUIRED_YN, VISIBLE_YN, USE_YN, SORT_ORDER,
    CREATED_BY
) VALUES (
    NULL, 'GLOBAL', NULL, NULL,
    '리스크대응', 'SELECT', 'N', 'Y', 'Y', 5,
    'system'
);

-- 리스크대응 옵션
INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_VALUE, OPTION_LABEL, OPTION_COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'NONE', 'None (이슈없음)', '#6B7280', 1, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '리스크대응' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_VALUE, OPTION_LABEL, OPTION_COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'MITIGATED', 'Mitigated (이슈해결)', '#3B82F6', 2, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '리스크대응' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_VALUE, OPTION_LABEL, OPTION_COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'CRITICAL', 'Critical (실패복구)', '#EF4444', 3, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '리스크대응' AND OWNER_TYPE = 'GLOBAL';
```

---

## 7. API 설계

### 7.1 성과 점수 API

```
# 점수 조회
GET    /api/items/{itemId}/score          # 업무 성과 점수 조회

# 점수 계산 (완료 시 자동 또는 수동)
POST   /api/items/{itemId}/score/calculate  # 성과 점수 계산

# 가중치 수정
PUT    /api/items/{itemId}/score/weights   # 가중치 수정 (난이도, 범위, 리스크)

# PM 승인
POST   /api/items/{itemId}/score/approve   # 승인 처리 (PM 전용)
POST   /api/items/{itemId}/score/reject    # 반려 처리 (PM 전용)

# 통계
GET    /api/scores/statistics              # 점수 통계 (기간별, 사용자별)
GET    /api/scores/ranking                 # 순위 조회
```

### 7.2 이력 관리 API

```
# 이력 조회
GET    /api/items/{itemId}/history         # 업무 속성 변경 이력
GET    /api/items/{itemId}/history/dates   # 날짜 속성 변경 이력 (간트용)

# 이력 상세
GET    /api/history/{historyId}            # 이력 상세 조회
```

### 7.3 지연 관리 API

```
# 지연 업무 조회 (기존 확장)
GET    /api/items/overdue                  # 지연 업무 목록
GET    /api/items/overdue/statistics       # 지연 통계

# 지연 알림
GET    /api/items/overdue/alerts           # 지연 알림 목록
PUT    /api/items/{itemId}/overdue/acknowledge  # 지연 확인 처리
```

---

## 8. 관리 메뉴

### 8.1 업무 상태 정보 관리

```
[메뉴 위치]
설정 > 업무 상태 관리

[표시 컬럼]
- 업무내용
- 카테고리
- 담당자
- 요청일
- 시작일
- 완료일
- 마감일
- 상태
- 지연 일수
- 성과 점수

[기능]
- 날짜 속성 일괄 수정
- 상태 일괄 변경
- 지연 재계산
- 성과 점수 재계산
```

### 8.2 성과 점수 대시보드

```
[메뉴 위치]
대시보드 > 성과 현황

[표시 내용]
- 기간별 완료 업무 수
- 등급별 분포 (Perfect ~ Poor)
- 평균 점수 추이
- 상위 성과자 순위
- 지연 업무 현황
```

---

## 9. 점수 계산 로직 (Backend)

### 9.1 ScoreCalculator 서비스

```java
@Service
public class ItemScoreService {

    // 가중치 상수
    private static final Map<String, BigDecimal> DIFFICULTY_WEIGHTS = Map.of(
        "LOW", new BigDecimal("0.9"),
        "MEDIUM", new BigDecimal("1.0"),
        "HIGH", new BigDecimal("1.2"),
        "EXTREME", new BigDecimal("1.4")
    );

    private static final Map<String, BigDecimal> SCOPE_WEIGHTS = Map.of(
        "NONE", new BigDecimal("1.0"),
        "MINOR", new BigDecimal("1.05"),
        "MAJOR", new BigDecimal("1.15"),
        "CHAOS", new BigDecimal("1.3")
    );

    private static final Map<String, BigDecimal> RISK_WEIGHTS = Map.of(
        "NONE", new BigDecimal("1.0"),
        "MITIGATED", new BigDecimal("1.1"),
        "CRITICAL", new BigDecimal("1.25")
    );

    public ItemScore calculateScore(Item item, ScoreRequest request) {
        // 1. 날짜 결정
        LocalDate startDate = determineStartDate(item);
        LocalDate completionDate = determineCompletionDate(item);
        LocalDate dueDate = item.getDueDate();

        // 2. 완료비율 계산
        BigDecimal completionRatio = calculateCompletionRatio(startDate, completionDate, dueDate);

        // 3. 시간 기반 등급 결정
        String timeGrade = determineTimeGrade(completionRatio, startDate, completionDate, dueDate);

        // 4. 가중치 적용
        BigDecimal difficultyWeight = DIFFICULTY_WEIGHTS.get(request.getDifficulty());
        BigDecimal scopeWeight = SCOPE_WEIGHTS.get(request.getScopeChange());
        BigDecimal riskWeight = RISK_WEIGHTS.get(request.getRiskHandling());

        // 5. 최종 점수 계산
        BigDecimal baseScore = getBaseScore(timeGrade);
        BigDecimal finalScore = baseScore
            .multiply(difficultyWeight)
            .multiply(scopeWeight)
            .multiply(riskWeight);

        // 6. 승인 필요 여부 확인
        boolean requiresApproval = "EXTREME".equals(request.getDifficulty())
            || "CHAOS".equals(request.getScopeChange());

        return ItemScore.builder()
            .itemId(item.getItemId())
            .timeGrade(timeGrade)
            .timeScore(baseScore)
            .completionRatio(completionRatio)
            .difficultyCode(request.getDifficulty())
            .difficultyWeight(difficultyWeight)
            .scopeCode(request.getScopeChange())
            .scopeWeight(scopeWeight)
            .riskCode(request.getRiskHandling())
            .riskWeight(riskWeight)
            .finalScore(finalScore)
            .requiresApproval(requiresApproval ? "Y" : "N")
            .build();
    }

    private String determineTimeGrade(BigDecimal ratio, LocalDate start, LocalDate completion, LocalDate due) {
        if (start.equals(completion)) return "PERFECT";  // 당일 완료
        if (ratio.compareTo(new BigDecimal("0.5")) <= 0) return "EXCELLENT";
        if (ratio.compareTo(new BigDecimal("0.8")) <= 0) return "GREAT";
        if (ratio.compareTo(new BigDecimal("1.0")) <= 0) return "GOOD";

        // 초과 일수 계산
        long overdueDays = ChronoUnit.DAYS.between(due, completion);
        if (overdueDays <= 2) return "FAIR";
        return "POOR";
    }
}
```

---

## 10. 카테고리-속성 설계와의 병합 포인트

### 10.1 기본 속성 재정의 (병합 시)

```
[현재 카테고리-속성 설계의 기본 속성]
- 업무내용, 상세설명, 상태, 우선순위
- 시작시간(START_TIME), 완료예정시간(END_TIME)  ← 변경 필요
- 담당자, 그룹, 카테고리

[병합 후 기본 속성]
- 업무내용, 상세설명, 상태, 우선순위
- 요청일(REQUEST_DATE) ← 신규
- 마감일(DUE_DATE = END_TIME) ← 명칭 변경
- 담당자, 그룹, 카테고리

[병합 후 글로벌 속성으로 이동]
- 시작일 ← 기본에서 이동
- 완료일 ← 신규
- 난이도, 범위변경, 리스크대응 ← 신규
```

### 10.2 테이블 추가 (병합 시)

```
[카테고리-속성 설계 테이블]
- TB_PROPERTY_DEF (수정)
- TB_CATEGORY (신규)
- TB_CATEGORY_PROPERTY (신규)
- TB_BOARD_CATEGORY (신규)
- TB_BOARD_PROPERTY (신규)

[성과 점수 설계 테이블 - 추가]
- TB_ITEM_PROPERTY_HISTORY (신규)
- TB_ITEM_SCORE (신규)
```

---

## 11. 구현 순서

### Phase 1: DB 스키마
1. TB_ITEM에 REQUEST_DATE 추가
2. TB_ITEM_PROPERTY_HISTORY 생성
3. TB_ITEM_SCORE 생성
4. 글로벌 속성 초기 데이터 추가 (시작일, 완료일, 난이도, 범위변경, 리스크대응)

### Phase 2: Backend
1. Domain 클래스 추가 (ItemScore, ItemPropertyHistory)
2. Mapper 추가
3. ScoreService 구현
4. HistoryService 구현
5. Controller API 구현

### Phase 3: Frontend
1. 타입 정의 추가
2. API 클라이언트 추가
3. 점수 표시 컴포넌트
4. 이력 표시 컴포넌트
5. 관리 메뉴 UI

### Phase 4: 통합
1. 날짜 속성 변경 시 이력 자동 기록
2. 완료 처리 시 점수 자동 계산
3. 지연 계산 로직 통합

---

## 12. 미결 사항

| 항목 | 상태 | 비고 |
|------|------|------|
| cap_4.jpg 상세 알고리즘 | 대기 | 이미지 확인 후 반영 필요 |
| PM 역할 정의 | 미정 | MANAGER와 별도? 또는 동일? |
| 점수 공개 범위 | 미정 | 본인만? 팀 전체? |
| 간트 차트 구현 | 별도 | 이력 데이터 활용 |

---

## 13. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2025-01-04 | - | 최초 작성 |
