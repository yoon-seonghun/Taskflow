# 외부 DB 연동 설정 가이드

## 개요

TaskFlow는 사용자, 부서, 직급 정보를 외부 데이터베이스에서 관리하는 **External 모드**를 지원합니다.
이 모드에서는 기존 인사 시스템의 데이터를 그대로 활용하며, TaskFlow 내에서는 읽기 전용으로 동작합니다.

### Shadow User 패턴

External 모드에서는 **Shadow User 패턴**을 사용하여 외부 사용자 정보를 내부 DB에 동기화합니다:

- **외부 DB**: 인사 시스템의 원본 사용자/부서/직급 정보 (VIEW 조회 전용)
- **내부 DB**: TaskFlow 업무 데이터의 FK 참조를 위한 Shadow User/Department/Position

```
┌─────────────────┐                  ┌─────────────────┐
│   외부 DB        │   동기화 복제    │   내부 DB        │
│   (인사 시스템)   │  ──────────────► │   (TaskFlow)    │
├─────────────────┤                  ├─────────────────┤
│ VW_TASKFLOW_USER │                  │ TB_USER         │
│ - USER_ID: 1001 │                  │ - USER_ID: 5    │
│ - USERNAME: kim │                  │ - USERNAME: kim │
│                 │                  │ - EXT_USER_ID:  │
│                 │                  │     1001        │
└─────────────────┘                  └─────────────────┘
```

**USERNAME**을 기준으로 매핑하여 외부 USER_ID가 변경되어도 안전하게 동작합니다.

### 실행 모드

TaskFlow는 **단일 JAR**로 두 가지 모드를 지원합니다:

| 모드 | 용도 | 실행 방법 |
|------|------|----------|
| **Service 모드** | 웹 서버 실행 | `java -jar taskflow.jar` |
| **Sync 모드** | 전체 동기화 후 종료 | `java -jar taskflow.jar --sync` |

```bash
# 웹 서비스 실행 (기본)
java -jar taskflow.jar

# CLI 동기화 모드 (Cron 배치용)
java -jar taskflow.jar --sync
```

---

## 설정 방법

### 1. application.yml 설정

```yaml
taskflow:
  user-management:
    mode: external  # internal(기본) 또는 external

    external:
      password-encoder: sha256      # sha256 또는 bcrypt
      sha256-use-salt: false        # salt 사용 여부
      user-crud-enabled: false      # 사용자 CRUD 비활성화
      department-crud-enabled: false # 부서 CRUD 비활성화
      position-crud-enabled: false   # 직급 CRUD 비활성화
      head-management-enabled: false # 팀장 지정 비활성화

# 외부 DataSource 설정
spring:
  datasource:
    external:
      jdbc-url: jdbc:mysql://외부DB주소:3306/외부DB명?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
      username: 외부DB계정
      password: 외부DB비밀번호
      driver-class-name: com.mysql.cj.jdbc.Driver
```

### 2. 환경변수 설정 (Docker/운영 환경)

```bash
# 모드 설정
USER_MANAGEMENT_MODE=external

# 외부 DB 연결 정보
EXTERNAL_DB_URL=jdbc:mysql://외부DB주소:3306/외부DB명?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
EXTERNAL_DB_USERNAME=계정
EXTERNAL_DB_PASSWORD=비밀번호

# VIEW 테이블명 설정 (기본값)
EXTERNAL_USER_TABLE=VW_TASKFLOW_USER
EXTERNAL_DEPARTMENT_TABLE=VW_TASKFLOW_DEPARTMENT
EXTERNAL_POSITION_TABLE=VW_TASKFLOW_POSITION

# 비밀번호 인코더 설정
EXTERNAL_PASSWORD_ENCODER=sha256
EXTERNAL_SHA256_USE_SALT=false
```

### 3. Docker Compose 설정 예시

```yaml
# docker-compose.yml
services:
  backend:
    image: taskflow-backend:latest
    environment:
      # 기본 DB 설정
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/taskflow
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: password

      # External 모드 설정
      USER_MANAGEMENT_MODE: external

      # 외부 DB 연결 정보
      EXTERNAL_DB_URL: jdbc:mysql://192.168.10.55:3306/external_db?useSSL=false&serverTimezone=Asia/Seoul
      EXTERNAL_DB_USERNAME: external_user
      EXTERNAL_DB_PASSWORD: external_password

      # VIEW 테이블명
      EXTERNAL_USER_TABLE: VW_TASKFLOW_USER
      EXTERNAL_DEPARTMENT_TABLE: VW_TASKFLOW_DEPARTMENT
      EXTERNAL_POSITION_TABLE: VW_TASKFLOW_POSITION
```

---

## 외부 DB VIEW 생성 가이드

TaskFlow가 외부 DB의 데이터를 읽기 위해서는 아래 3개의 VIEW를 생성해야 합니다.

---

### VIEW 1: VW_TASKFLOW_USER (사용자)

```sql
-- ============================================================
-- VIEW: VW_TASKFLOW_USER
-- 설명: TaskFlow 사용자 정보 VIEW
-- 중요: DEPARTMENT_CODE, POSITION_CODE는 FK 참조 기준
-- ============================================================
CREATE OR REPLACE VIEW VW_TASKFLOW_USER AS
SELECT
    -- 필수 컬럼 (NOT NULL)
    emp.EMP_ID          AS USER_ID,           -- 사용자 PK (BIGINT)
    emp.LOGIN_ID        AS USERNAME,          -- 로그인 아이디 (VARCHAR 50)
    emp.PASSWORD        AS PASSWORD,          -- SHA256 해시 비밀번호 (VARCHAR 255)
    emp.EMP_NAME        AS NAME,              -- 사용자 이름 (VARCHAR 100)

    -- 선택 컬럼 (NULL 허용)
    emp.EMAIL           AS EMAIL,             -- 이메일 (VARCHAR 255)

    -- 부서 정보 (부서 테이블 조인)
    dept.DEPT_CD        AS DEPARTMENT_CODE,   -- 부서 코드 (VARCHAR 50)
    dept.DEPT_NM        AS DEPARTMENT_NAME,   -- 부서명 (VARCHAR 100)

    -- 직급 정보 (직급 테이블 조인) - 선택사항
    pos.POS_CD          AS POSITION_CODE,     -- 직급 코드 (VARCHAR 50, NULL 가능)
    pos.POS_NM          AS POSITION_NAME,     -- 직급명 (VARCHAR 100, NULL 가능)
    pos.SORT_ORD        AS POSITION_SORT_ORDER, -- 직급 순서 (INT, NULL 가능)

    -- 팀장 여부 - 선택사항
    CASE
        WHEN emp.IS_HEAD = 1 THEN 'Y'
        ELSE 'N'
    END                 AS HEAD_YN,           -- 팀장 여부 (CHAR 1, 기본 'N')

    -- 사용 여부
    CASE
        WHEN emp.STATUS = 'ACTIVE' THEN 'Y'
        ELSE 'N'
    END                 AS USE_YN,            -- 사용 여부 (CHAR 1)

    -- 감사 컬럼
    emp.LAST_LOGIN_AT   AS LAST_LOGIN_AT,     -- 마지막 로그인 (DATETIME, NULL)
    emp.REG_DATE        AS CREATED_AT,        -- 생성일시 (DATETIME)
    emp.REG_USER_ID     AS CREATED_BY,        -- 생성자 (VARCHAR 50, NULL 가능)
    emp.MOD_DATE        AS UPDATED_AT,        -- 수정일시 (DATETIME, NULL 가능)
    emp.MOD_USER_ID     AS UPDATED_BY         -- 수정자 (VARCHAR 50, NULL 가능)

FROM
    TB_EMPLOYEE emp
LEFT JOIN TB_DEPARTMENT dept ON emp.DEPT_ID = dept.DEPT_ID
LEFT JOIN TB_POSITION pos ON emp.POS_ID = pos.POS_ID
;
```

#### 컬럼 매핑 상세

| TaskFlow 컬럼 | 타입 | 필수 | 설명 | 외부 DB 매핑 예시 |
|--------------|------|:----:|------|------------------|
| USER_ID | BIGINT | O | 사용자 PK | EMP_ID, USER_SEQ |
| USERNAME | VARCHAR(50) | O | 로그인 아이디 (FK 참조 기준) | LOGIN_ID, USER_ID |
| PASSWORD | VARCHAR(255) | O | SHA256 해시 | PASSWORD, PWD |
| NAME | VARCHAR(100) | O | 사용자명 | EMP_NAME, USER_NM |
| EMAIL | VARCHAR(255) | - | 이메일 | EMAIL, MAIL_ADDR |
| DEPARTMENT_CODE | VARCHAR(50) | - | 부서 코드 | DEPT_CD, DEPT_CODE |
| DEPARTMENT_NAME | VARCHAR(100) | - | 부서명 | DEPT_NM, DEPT_NAME |
| POSITION_CODE | VARCHAR(50) | - | 직급 코드 | POS_CD, POSITION_CODE |
| POSITION_NAME | VARCHAR(100) | - | 직급명 | POS_NM, POSITION_NAME |
| POSITION_SORT_ORDER | INT | - | 직급 순서 | SORT_ORD (낮을수록 높은 직급) |
| HEAD_YN | CHAR(1) | - | 팀장 여부 | 'Y'/'N' 변환 필요, 기본 'N' |
| USE_YN | CHAR(1) | O | 사용 여부 | 'Y'/'N' 변환 필요 |
| LAST_LOGIN_AT | DATETIME | - | 최종 로그인 | 없으면 NULL |
| CREATED_AT | DATETIME | O | 생성일시 | REG_DATE, INS_DT |
| CREATED_BY | VARCHAR(50) | - | 생성자 | REG_USER_ID |
| UPDATED_AT | DATETIME | - | 수정일시 | MOD_DATE, UPD_DT |
| UPDATED_BY | VARCHAR(50) | - | 수정자 | MOD_USER_ID |

> **참고**: POSITION_CODE, POSITION_NAME, POSITION_SORT_ORDER, HEAD_YN 컬럼이 외부 DB에 없는 경우 NULL 또는 기본값을 사용합니다.

#### 비밀번호 해시 형식

```sql
-- MySQL sha2() 함수 사용 예시
-- TaskFlow는 아래 형식의 SHA256 해시와 호환됩니다
SELECT SHA2('password123', 256);
-- 결과: ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f
```

---

### VIEW 2: VW_TASKFLOW_DEPARTMENT (부서)

```sql
-- ============================================================
-- VIEW: VW_TASKFLOW_DEPARTMENT
-- 설명: TaskFlow 부서 정보 VIEW
-- 중요: DEPARTMENT_CODE는 TaskFlow FK 참조 기준값
-- ============================================================
CREATE OR REPLACE VIEW VW_TASKFLOW_DEPARTMENT AS
SELECT
    -- 필수 컬럼 (NOT NULL)
    dept.DEPT_ID        AS DEPARTMENT_ID,     -- 부서 PK (BIGINT)
    dept.DEPT_CD        AS DEPARTMENT_CODE,   -- 부서 코드 (FK 참조 기준, VARCHAR 50)
    dept.DEPT_NM        AS DEPARTMENT_NAME,   -- 부서명 (VARCHAR 100)

    -- 계층 구조 (PARENT_CODE 사용)
    parent.DEPT_CD      AS PARENT_CODE,       -- 상위 부서 코드 (VARCHAR 50, NULL = 최상위)

    -- 정렬 및 사용 여부
    COALESCE(dept.SORT_ORD, 0) AS SORT_ORDER, -- 정렬 순서 (INT, 기본값 0)
    CASE
        WHEN dept.USE_FLAG = '1' THEN 'Y'
        ELSE 'N'
    END                 AS USE_YN,            -- 사용 여부 (CHAR 1)

    -- 감사 컬럼
    dept.REG_DATE       AS CREATED_AT,        -- 생성일시 (DATETIME)
    dept.REG_USER_ID    AS CREATED_BY,        -- 생성자 (VARCHAR 50, NULL 가능)
    dept.MOD_DATE       AS UPDATED_AT,        -- 수정일시 (DATETIME, NULL 가능)
    dept.MOD_USER_ID    AS UPDATED_BY         -- 수정자 (VARCHAR 50, NULL 가능)

FROM
    TB_DEPARTMENT dept
LEFT JOIN TB_DEPARTMENT parent ON dept.PARENT_DEPT_ID = parent.DEPT_ID
;
```

#### 컬럼 매핑 상세

| TaskFlow 컬럼 | 타입 | 필수 | 설명 | 외부 DB 매핑 예시 |
|--------------|------|:----:|------|------------------|
| DEPARTMENT_ID | BIGINT | O | 부서 PK | DEPT_ID, DEPT_SEQ |
| DEPARTMENT_CODE | VARCHAR(50) | O | 부서 코드 (FK 참조 기준) | DEPT_CD, DEPT_CODE |
| DEPARTMENT_NAME | VARCHAR(100) | O | 부서명 | DEPT_NM, DEPT_NAME |
| PARENT_CODE | VARCHAR(50) | - | 상위 부서 코드 (NULL=최상위) | PARENT_DEPT_CD |
| SORT_ORDER | INT | O | 정렬 순서 | SORT_ORD, ORD_NO |
| USE_YN | CHAR(1) | O | 사용 여부 | 'Y'/'N' 변환 필요 |
| CREATED_AT | DATETIME | O | 생성일시 | REG_DATE |
| CREATED_BY | VARCHAR(50) | - | 생성자 | REG_USER_ID |
| UPDATED_AT | DATETIME | - | 수정일시 | MOD_DATE |
| UPDATED_BY | VARCHAR(50) | - | 수정자 | MOD_USER_ID |

> **중요**: TaskFlow는 **DEPARTMENT_CODE** 기반으로 부서를 참조합니다.

---

### VIEW 3: VW_TASKFLOW_POSITION (직급)

```sql
-- ============================================================
-- VIEW: VW_TASKFLOW_POSITION
-- 설명: TaskFlow 직급 정보 VIEW
-- 중요: POSITION_CODE는 TaskFlow FK 참조 기준값
-- ============================================================
CREATE OR REPLACE VIEW VW_TASKFLOW_POSITION AS
SELECT
    -- 필수 컬럼 (NOT NULL)
    pos.POS_ID          AS POSITION_ID,       -- 직급 PK (BIGINT)
    pos.POS_CD          AS POSITION_CODE,     -- 직급 코드 (FK 참조 기준, VARCHAR 50)
    pos.POS_NM          AS POSITION_NAME,     -- 직급명 (VARCHAR 100)

    -- 정렬 및 사용 여부
    COALESCE(pos.SORT_ORD, 999) AS SORT_ORDER, -- 정렬 순서 (INT, 낮을수록 높은 직급)
    CASE
        WHEN pos.USE_FLAG = '1' THEN 'Y'
        ELSE 'N'
    END                 AS USE_YN,            -- 사용 여부 (CHAR 1)

    -- 감사 컬럼
    pos.REG_DATE        AS CREATED_AT,        -- 생성일시 (DATETIME)
    pos.REG_USER_ID     AS CREATED_BY,        -- 생성자 (VARCHAR 50, NULL 가능)
    pos.MOD_DATE        AS UPDATED_AT,        -- 수정일시 (DATETIME, NULL 가능)
    pos.MOD_USER_ID     AS UPDATED_BY         -- 수정자 (VARCHAR 50, NULL 가능)

FROM
    TB_POSITION pos
;
```

#### 컬럼 매핑 상세

| TaskFlow 컬럼 | 타입 | 필수 | 설명 | 외부 DB 매핑 예시 |
|--------------|------|:----:|------|------------------|
| POSITION_ID | BIGINT | O | 직급 PK | POS_ID, POSITION_SEQ |
| POSITION_CODE | VARCHAR(50) | O | 직급 코드 (FK 참조 기준) | POS_CD, POSITION_CODE |
| POSITION_NAME | VARCHAR(100) | O | 직급명 | POS_NM, POSITION_NAME |
| SORT_ORDER | INT | O | 정렬 순서 (낮을수록 높은 직급) | SORT_ORD, ORD_NO |
| USE_YN | CHAR(1) | O | 사용 여부 | 'Y'/'N' 변환 필요 |
| CREATED_AT | DATETIME | O | 생성일시 | REG_DATE |
| CREATED_BY | VARCHAR(50) | - | 생성자 | REG_USER_ID |
| UPDATED_AT | DATETIME | - | 수정일시 | MOD_DATE |
| UPDATED_BY | VARCHAR(50) | - | 수정자 | MOD_USER_ID |

> **중요**: SORT_ORDER 값이 낮을수록 높은 직급입니다. (예: 대표=1, 사원=7)

---

## 실제 적용 예시

### 예시 1: 일반적인 인사 시스템 연동

```sql
-- 사용자 VIEW (인사 시스템 기준)
CREATE OR REPLACE VIEW VW_TASKFLOW_USER AS
SELECT
    e.EMPLOYEE_NO       AS USER_ID,
    e.LOGIN_ID          AS USERNAME,
    e.PWD_HASH          AS PASSWORD,
    e.EMP_NAME          AS NAME,
    e.EMAIL_ADDR        AS EMAIL,
    d.DEPT_CD           AS DEPARTMENT_CODE,
    d.DEPT_NM           AS DEPARTMENT_NAME,
    p.POS_CD            AS POSITION_CODE,
    p.POS_NM            AS POSITION_NAME,
    p.SORT_ORD          AS POSITION_SORT_ORDER,
    CASE WHEN e.IS_TEAM_LEADER = 'Y' THEN 'Y' ELSE 'N' END AS HEAD_YN,
    CASE WHEN e.RETIRE_YN = 'N' THEN 'Y' ELSE 'N' END AS USE_YN,
    e.LAST_LOGIN_DT     AS LAST_LOGIN_AT,
    e.HIRE_DATE         AS CREATED_AT,
    'system'            AS CREATED_BY,
    e.LAST_MOD_DATE     AS UPDATED_AT,
    e.LAST_MOD_USER     AS UPDATED_BY
FROM HR_EMPLOYEE e
LEFT JOIN HR_DEPARTMENT d ON e.DEPT_CD = d.DEPT_CD
LEFT JOIN HR_POSITION p ON e.POS_CD = p.POS_CD
WHERE e.EMP_TYPE IN ('REGULAR', 'CONTRACT');

-- 부서 VIEW
CREATE OR REPLACE VIEW VW_TASKFLOW_DEPARTMENT AS
SELECT
    DEPT_ID             AS DEPARTMENT_ID,
    DEPT_CD             AS DEPARTMENT_CODE,
    DEPT_NM             AS DEPARTMENT_NAME,
    UPPER_DEPT_CD       AS PARENT_CODE,
    DISPLAY_ORDER       AS SORT_ORDER,
    CASE WHEN DEL_YN = 'N' THEN 'Y' ELSE 'N' END AS USE_YN,
    REG_DT              AS CREATED_AT,
    REG_USER            AS CREATED_BY,
    MOD_DT              AS UPDATED_AT,
    MOD_USER            AS UPDATED_BY
FROM HR_DEPARTMENT;

-- 직급 VIEW
CREATE OR REPLACE VIEW VW_TASKFLOW_POSITION AS
SELECT
    POS_ID              AS POSITION_ID,
    POS_CD              AS POSITION_CODE,
    POS_NM              AS POSITION_NAME,
    RANK_ORDER          AS SORT_ORDER,
    CASE WHEN USE_YN = 'Y' THEN 'Y' ELSE 'N' END AS USE_YN,
    REG_DT              AS CREATED_AT,
    REG_USER            AS CREATED_BY,
    MOD_DT              AS UPDATED_AT,
    MOD_USER            AS UPDATED_BY
FROM HR_POSITION;
```

### 예시 2: 직급/팀장 정보가 없는 경우

```sql
-- 직급, 팀장 정보가 없는 외부 DB의 경우 NULL/기본값 사용
CREATE OR REPLACE VIEW VW_TASKFLOW_USER AS
SELECT
    EMP_ID              AS USER_ID,
    LOGIN_ID            AS USERNAME,
    PASSWORD            AS PASSWORD,
    EMP_NAME            AS NAME,
    EMAIL               AS EMAIL,
    DEPT_CD             AS DEPARTMENT_CODE,
    DEPT_NM             AS DEPARTMENT_NAME,
    NULL                AS POSITION_CODE,      -- 직급 없음
    NULL                AS POSITION_NAME,      -- 직급 없음
    999                 AS POSITION_SORT_ORDER, -- 기본값
    'N'                 AS HEAD_YN,            -- 팀장 아님
    CASE WHEN STATUS = 'ACTIVE' THEN 'Y' ELSE 'N' END AS USE_YN,
    NULL                AS LAST_LOGIN_AT,
    REG_DATE            AS CREATED_AT,
    'system'            AS CREATED_BY,
    MOD_DATE            AS UPDATED_AT,
    NULL                AS UPDATED_BY
FROM EMPLOYEE_MASTER e
LEFT JOIN DEPARTMENT_MASTER d ON e.DEPT_ID = d.DEPT_ID;
```

### 예시 3: 퇴사자 포함 (이력 추적용)

```sql
-- 퇴사자도 포함하여 이력 추적 가능
CREATE OR REPLACE VIEW VW_TASKFLOW_USER AS
SELECT
    EMP_ID              AS USER_ID,
    LOGIN_ID            AS USERNAME,
    SHA2(CONCAT(EMP_ID, 'default_pw'), 256) AS PASSWORD,  -- 퇴사자는 기본 비밀번호
    EMP_NAME            AS NAME,
    EMAIL               AS EMAIL,
    DEPT_CD             AS DEPARTMENT_CODE,
    DEPT_NM             AS DEPARTMENT_NAME,
    POS_CD              AS POSITION_CODE,
    POS_NM              AS POSITION_NAME,
    POS_SORT            AS POSITION_SORT_ORDER,
    'N'                 AS HEAD_YN,
    CASE
        WHEN STATUS = 'ACTIVE' THEN 'Y'
        WHEN STATUS = 'RETIRED' THEN 'N'   -- 퇴사자
        WHEN STATUS = 'LEAVE' THEN 'N'     -- 휴직자
        ELSE 'N'
    END                 AS USE_YN,
    NULL                AS LAST_LOGIN_AT,
    REG_DATE            AS CREATED_AT,
    'system'            AS CREATED_BY,
    MOD_DATE            AS UPDATED_AT,
    NULL                AS UPDATED_BY
FROM EMPLOYEE_MASTER;
```

---

## 주의사항

### 1. 비밀번호 호환성

TaskFlow External 모드는 MySQL `SHA2(password, 256)` 함수와 호환됩니다.

```sql
-- 올바른 형식 (소문자 hex, 64자)
ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f

-- 잘못된 형식 (Base64 등)
75K3eLr+dx6JJFuJ7LwIpEpOFmwGZZkRiB84PURz6U8=
```

### 2. USE_YN 변환

외부 시스템의 상태값을 TaskFlow의 'Y'/'N' 형식으로 변환해야 합니다.

```sql
-- 숫자형 플래그
CASE WHEN STATUS_FLAG = 1 THEN 'Y' ELSE 'N' END AS USE_YN

-- 문자형 상태
CASE
    WHEN STATUS IN ('ACTIVE', 'ENABLE', '사용') THEN 'Y'
    ELSE 'N'
END AS USE_YN

-- 퇴사일 기준
CASE WHEN RETIRE_DATE IS NULL THEN 'Y' ELSE 'N' END AS USE_YN
```

### 3. NULL 처리

필수 컬럼이 NULL인 경우 기본값을 설정해야 합니다.

```sql
COALESCE(SORT_ORDER, 0) AS SORT_ORDER,
COALESCE(CREATED_AT, NOW()) AS CREATED_AT,
COALESCE(CREATED_BY, 'system') AS CREATED_BY
```

### 4. 부서 계층 구조

최상위 부서의 PARENT_CODE는 반드시 NULL이어야 합니다.

```sql
CASE WHEN UPPER_DEPT_CD = 'ROOT' THEN NULL ELSE UPPER_DEPT_CD END AS PARENT_CODE
```

### 5. 직급 정렬 순서

SORT_ORDER 값이 낮을수록 높은 직급입니다.

```sql
-- 예시: 대표=1, 이사=2, 부장=3, ... 사원=7, 인턴=8
COALESCE(RANK_ORDER, 999) AS SORT_ORDER  -- 직급 없으면 최하위
```

---

## 기능 제한 사항

External 모드에서는 다음 기능이 비활성화됩니다:

| 기능 | Internal 모드 | External 모드 |
|------|:------------:|:------------:|
| 사용자 등록 | O | X |
| 사용자 수정 | O | X |
| 사용자 삭제 | O | X |
| 비밀번호 변경 | O | X |
| 부서 등록 | O | X |
| 부서 수정 | O | X |
| 부서 삭제 | O | X |
| **직급 등록** | O | X |
| **직급 수정** | O | X |
| **직급 삭제** | O | X |
| **팀장 지정/해제** | O | X |
| 사용자 조회 | O | O |
| 부서 조회 | O | O |
| 직급 조회 | O | O |
| 로그인 | O | O |
| 보드/업무 관리 | O | O |

---

## 연결 테스트

VIEW 생성 후 아래 쿼리로 데이터 정합성을 확인합니다.

```sql
-- 1. 사용자 데이터 확인
SELECT * FROM VW_TASKFLOW_USER LIMIT 10;

-- 2. 활성 사용자 수
SELECT COUNT(*) FROM VW_TASKFLOW_USER WHERE USE_YN = 'Y';

-- 3. 부서 데이터 확인
SELECT * FROM VW_TASKFLOW_DEPARTMENT ORDER BY SORT_ORDER;

-- 4. 최상위 부서
SELECT * FROM VW_TASKFLOW_DEPARTMENT WHERE PARENT_CODE IS NULL;

-- 5. 직급 데이터 확인
SELECT * FROM VW_TASKFLOW_POSITION ORDER BY SORT_ORDER;

-- 6. 부서별 사용자 수
SELECT
    d.DEPARTMENT_NAME,
    COUNT(u.USER_ID) AS USER_COUNT
FROM VW_TASKFLOW_DEPARTMENT d
LEFT JOIN VW_TASKFLOW_USER u ON d.DEPARTMENT_CODE = u.DEPARTMENT_CODE
WHERE d.USE_YN = 'Y'
GROUP BY d.DEPARTMENT_CODE, d.DEPARTMENT_NAME
ORDER BY d.SORT_ORDER;

-- 7. 직급별 사용자 수
SELECT
    COALESCE(u.POSITION_NAME, '(미지정)') AS POSITION_NAME,
    COUNT(*) AS USER_COUNT
FROM VW_TASKFLOW_USER u
WHERE u.USE_YN = 'Y'
GROUP BY u.POSITION_CODE, u.POSITION_NAME
ORDER BY MIN(u.POSITION_SORT_ORDER);

-- 8. 팀장 목록
SELECT USERNAME, NAME, DEPARTMENT_NAME, POSITION_NAME
FROM VW_TASKFLOW_USER
WHERE HEAD_YN = 'Y' AND USE_YN = 'Y';
```

---

## Shadow User 동기화

### 동기화 시점

External 모드에서 사용자/부서/직급 정보는 다음 두 가지 방식으로 동기화됩니다:

| 방식 | 시점 | 범위 | 용도 |
|------|------|------|------|
| **실시간 동기화** | 로그인 시 | 해당 사용자만 | 즉시 반영 |
| **전체 동기화** | CLI 실행 시 | 전체 사용자/부서/직급 | 배치 작업 |

### 실시간 동기화 (로그인 시)

사용자가 로그인할 때 해당 사용자의 Shadow User 정보가 자동으로 동기화됩니다:

```
[로그인 요청]
    │
    ▼
[외부 DB에서 사용자 조회] ──────► 비밀번호 검증
    │
    ▼
[부서 Shadow 동기화] ◄───────── 부서 정보 변경 시 갱신
    │
    ▼
[직급 Shadow 동기화] ◄───────── 직급 정보 변경 시 갱신
    │
    ▼
[사용자 Shadow 동기화] ◄─────── 이름/이메일/직급/팀장 등 변경 시 갱신
    │
    ▼
[JWT 토큰 발급] ──────────────► 내부 USER_ID 사용
```

- 신규 사용자: Shadow User 자동 생성
- 기존 사용자: 정보 변경 시 자동 갱신
- 부서/직급 변경: 관련 부서/직급도 함께 동기화

### 전체 동기화 (CLI 모드)

`--sync` 옵션으로 실행하면 전체 사용자/부서/직급을 일괄 동기화합니다:

```bash
# 전체 동기화 실행
java -jar taskflow.jar --sync

# 로그 파일로 저장
java -jar taskflow.jar --sync >> /var/log/taskflow-sync.log 2>&1
```

**실행 결과 예시:**
```
╔══════════════════════════════════════════════════════════════╗
║            TaskFlow External DB Synchronization              ║
╚══════════════════════════════════════════════════════════════╝

▶ Step 1: Department Synchronization
─────────────────────────────────────────────────────────────────
  Department sync completed:
    - Created:     5
    - Updated:     2
    - Deactivated: 0
    - Skipped:     10
    - Total:       17
    - Duration:    150 ms

▶ Step 2: Position Synchronization
─────────────────────────────────────────────────────────────────
  Position sync completed:
    - Created:     2
    - Updated:     1
    - Deactivated: 0
    - Skipped:     5
    - Total:       8
    - Duration:    80 ms

▶ Step 3: User Synchronization
─────────────────────────────────────────────────────────────────
  User sync completed:
    - Created:     12
    - Updated:     8
    - Deactivated: 3
    - Skipped:     77
    - Total:       100
    - Duration:    820 ms

═══════════════════════════════════════════════════════════════
✓ Synchronization completed successfully
═══════════════════════════════════════════════════════════════
```

### 동기화 동작 상세

| 상황 | 동작 |
|------|------|
| 외부에 신규 데이터 추가 | Shadow 데이터 생성 (Created) |
| 외부 데이터 정보 변경 | Shadow 데이터 갱신 (Updated) |
| 외부 데이터 삭제/비활성화 | Shadow 데이터 비활성화 (Deactivated) |
| 변경 없음 | 건너뜀 (Skipped) |

**비활성화 로직:**
- 외부 DB에서 삭제된 사용자는 내부 DB에서 `USE_YN = 'N'`으로 설정
- 기존 업무 데이터의 FK 참조는 유지되므로 이력 추적 가능

---

## Cron 설정 예시

### Linux/Unix Cron

```bash
# crontab -e

# 매일 새벽 2시에 전체 동기화
0 2 * * * /usr/bin/java -jar /app/taskflow.jar --sync >> /var/log/taskflow-sync.log 2>&1

# 매주 월요일 새벽 3시에 동기화
0 3 * * 1 /usr/bin/java -jar /app/taskflow.jar --sync >> /var/log/taskflow-sync.log 2>&1

# 매시간 동기화 (실시간성이 중요한 경우)
0 * * * * /usr/bin/java -jar /app/taskflow.jar --sync >> /var/log/taskflow-sync.log 2>&1
```

### Windows 작업 스케줄러

```powershell
# PowerShell 스크립트 (sync-taskflow.ps1)
$logPath = "C:\TaskFlow\logs\sync-$(Get-Date -Format 'yyyyMMdd').log"
java -jar C:\TaskFlow\taskflow.jar --sync 2>&1 | Tee-Object -FilePath $logPath -Append
```

작업 스케줄러에서:
1. 새 작업 만들기
2. 트리거: 매일 02:00
3. 동작: `powershell.exe -File C:\TaskFlow\sync-taskflow.ps1`

### Docker Compose 환경

```yaml
# docker-compose.yml
services:
  taskflow:
    image: taskflow:latest
    command: ["java", "-jar", "taskflow.jar"]
    # ... 기타 설정

  taskflow-sync:
    image: taskflow:latest
    command: ["java", "-jar", "taskflow.jar", "--sync"]
    profiles: ["sync"]  # docker compose --profile sync up 으로만 실행
```

별도 크론 컨테이너 사용:
```yaml
  taskflow-cron:
    image: taskflow:latest
    command: >
      sh -c "
        echo '0 2 * * * java -jar /app/taskflow.jar --sync >> /var/log/sync.log 2>&1' > /etc/crontabs/root &&
        crond -f
      "
    depends_on:
      - mysql
```

---

## 내부 DB 스키마 (동기화 관련)

External 모드에서는 TB_USER, TB_DEPARTMENT, TB_POSITION에 동기화 관련 컬럼이 추가됩니다:

### TB_USER 추가 컬럼

```sql
EXTERNAL_USER_ID   BIGINT       NULL     COMMENT '외부 DB USER_ID (External 모드)',
SYNC_SOURCE        VARCHAR(20)  NOT NULL DEFAULT 'INTERNAL' COMMENT '데이터 출처 (INTERNAL/EXTERNAL)',
LAST_SYNCED_AT     DATETIME     NULL     COMMENT '마지막 동기화 시간',
```

### TB_DEPARTMENT 추가 컬럼

```sql
EXTERNAL_DEPT_ID   BIGINT       NULL     COMMENT '외부 DB DEPARTMENT_ID (External 모드)',
SYNC_SOURCE        VARCHAR(20)  NOT NULL DEFAULT 'INTERNAL' COMMENT '데이터 출처',
LAST_SYNCED_AT     DATETIME     NULL     COMMENT '마지막 동기화 시간',
```

### TB_POSITION 추가 컬럼

```sql
EXTERNAL_POS_ID    BIGINT       NULL     COMMENT '외부 DB POSITION_ID (External 모드)',
SYNC_SOURCE        VARCHAR(20)  NOT NULL DEFAULT 'INTERNAL' COMMENT '데이터 출처',
LAST_SYNCED_AT     DATETIME     NULL     COMMENT '마지막 동기화 시간',
```

| 컬럼 | 설명 |
|------|------|
| EXTERNAL_*_ID | 외부 DB의 원본 PK |
| SYNC_SOURCE | 'INTERNAL' (자체 생성) 또는 'EXTERNAL' (동기화) |
| LAST_SYNCED_AT | 마지막 동기화 시점 |

---

## 모드 전환 가이드

### Internal → External 전환

1. **외부 DB VIEW 생성**
   - VW_TASKFLOW_USER, VW_TASKFLOW_DEPARTMENT, VW_TASKFLOW_POSITION 생성
   - 데이터 정합성 검증

2. **설정 변경**
   ```yaml
   taskflow:
     user-management:
       mode: external
   ```

3. **초기 동기화 실행**
   ```bash
   java -jar taskflow.jar --sync
   ```

4. **서비스 재시작**
   ```bash
   # 기존 서비스 중지 후
   java -jar taskflow.jar
   ```

### External → Internal 전환

1. **설정 변경**
   ```yaml
   taskflow:
     user-management:
       mode: internal
   ```

2. **서비스 재시작**
   - 기존 Shadow User/Department/Position 데이터는 유지됨
   - 내부 사용자/부서/직급 CRUD 기능 활성화

### 혼합 사용자 환경

External 모드에서도 `SYNC_SOURCE = 'INTERNAL'`인 사용자(시스템 관리자 등)는
내부 DB에서 직접 관리 가능합니다.

```sql
-- 시스템 관리자 계정 (Internal)
INSERT INTO TB_USER (USERNAME, PASSWORD, NAME, SYNC_SOURCE, USE_YN, CREATED_BY)
VALUES ('admin', '$2a$10$...', '시스템관리자', 'INTERNAL', 'Y', 'admin');
```

---

## 문제 해결

### 로그인 실패

1. **비밀번호 해시 형식 확인**
   ```sql
   SELECT USERNAME, PASSWORD, LENGTH(PASSWORD) AS PWD_LEN
   FROM VW_TASKFLOW_USER
   WHERE USERNAME = '테스트계정';
   -- PWD_LEN이 64여야 함 (SHA256 hex)
   ```

2. **대소문자 확인**
   ```sql
   -- SHA256 해시는 소문자여야 함
   SELECT LOWER(SHA2('password', 256));
   ```

### 부서 트리 표시 안됨

1. **PARENT_CODE 확인**
   ```sql
   -- 고아 부서 확인 (PARENT_CODE가 존재하지 않는 부서를 참조)
   SELECT c.*
   FROM VW_TASKFLOW_DEPARTMENT c
   LEFT JOIN VW_TASKFLOW_DEPARTMENT p ON c.PARENT_CODE = p.DEPARTMENT_CODE
   WHERE c.PARENT_CODE IS NOT NULL AND p.DEPARTMENT_CODE IS NULL;
   ```

### 사용자-부서 연결 안됨

1. **DEPARTMENT_CODE 정합성 확인**
   ```sql
   -- 존재하지 않는 부서를 참조하는 사용자
   SELECT u.*
   FROM VW_TASKFLOW_USER u
   LEFT JOIN VW_TASKFLOW_DEPARTMENT d ON u.DEPARTMENT_CODE = d.DEPARTMENT_CODE
   WHERE u.DEPARTMENT_CODE IS NOT NULL AND d.DEPARTMENT_CODE IS NULL;
   ```

### 직급 표시 안됨

1. **POSITION_CODE 정합성 확인**
   ```sql
   -- 존재하지 않는 직급을 참조하는 사용자
   SELECT u.USERNAME, u.NAME, u.POSITION_CODE
   FROM VW_TASKFLOW_USER u
   LEFT JOIN VW_TASKFLOW_POSITION p ON u.POSITION_CODE = p.POSITION_CODE
   WHERE u.POSITION_CODE IS NOT NULL AND p.POSITION_CODE IS NULL;
   ```

### 동기화 실패

1. **외부 DB 연결 확인**
   ```sql
   -- 외부 DB VIEW 접근 가능 여부
   SELECT COUNT(*) FROM VW_TASKFLOW_USER;
   SELECT COUNT(*) FROM VW_TASKFLOW_DEPARTMENT;
   SELECT COUNT(*) FROM VW_TASKFLOW_POSITION;
   ```

2. **로그 확인**
   ```bash
   # 동기화 로그 확인
   grep "sync" /var/log/taskflow-sync.log | tail -50
   ```

3. **개별 사용자 동기화 상태**
   ```sql
   -- Shadow User 동기화 상태 확인
   SELECT USERNAME, EXTERNAL_USER_ID, SYNC_SOURCE, LAST_SYNCED_AT
   FROM TB_USER
   WHERE SYNC_SOURCE = 'EXTERNAL'
   ORDER BY LAST_SYNCED_AT DESC;
   ```

### Exit Code

동기화 CLI 모드의 종료 코드:

| 코드 | 의미 |
|------|------|
| 0 | 성공 |
| 1 | 실패 (부서/직급/사용자 동기화 오류) |

```bash
# 동기화 결과 확인
java -jar taskflow.jar --sync
echo "Exit code: $?"
```

---

## 핵심 기능 요약

```
1. 실시간 동기화: 로그인 시 해당 사용자만 즉시 동기화
2. 배치 동기화: java -jar taskflow.jar --sync로 전체 동기화
3. USERNAME/CODE 기반 매핑: 외부 ID와 독립적으로 안정적 동작
4. JWT 내부 ID 사용: 토큰에는 내부 Shadow User ID 저장
5. 3개 VIEW 필요: 사용자, 부서, 직급
```

---

## 버전 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2024-12-22 | 최초 작성 (사용자, 부서 VIEW) |
| 1.1 | 2024-12-31 | 직급(Position) VIEW 추가, 팀장(HEAD_YN) 컬럼 추가 |
