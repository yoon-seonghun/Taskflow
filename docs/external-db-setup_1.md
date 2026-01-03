# 외부 DB 연동 설정 가이드

## 개요

TaskFlow는 사용자 및 부서 정보를 외부 데이터베이스에서 관리하는 **External 모드**를 지원합니다.
이 모드에서는 기존 인사 시스템의 데이터를 그대로 활용하며, TaskFlow 내에서는 읽기 전용으로 동작합니다.

## 설정 방법

```
● 설정 파일 위치

  | 파일               | 경로                                       | 설명                     |
  |--------------------|--------------------------------------------|--------------------------|
  | application.yml    | backend/src/main/resources/application.yml | 메인 설정 파일           |
  | docker-compose.yml | docker-compose.yml                         | Docker 컨테이너 설정     |
  | .env               | .env                                       | 환경변수 설정            |
  | 외부 DB 가이드     | docs/external-db-setup.md                  | VIEW 생성 및 설정 가이드 |

  ---
  2. 설정 변경 방법

  방법 1: 환경변수로 설정 (Docker 권장)

  .env 파일에 추가:

  # 외부 DB 연동 모드 활성화
  USER_MANAGEMENT_MODE=external

  # 외부 DB 연결 정보
  EXTERNAL_DB_URL=jdbc:mysql://외부DB주소:3306/외부DB명?useSSL=false&serverTimezone=Asia/Seoul
  EXTERNAL_DB_USERNAME=readonly_user
  EXTERNAL_DB_PASSWORD=readonly_password

  docker-compose.yml에 환경변수 추가 필요:
```

 ---
  설정 파일 요약

  | 파일               | 경로                                       | 변경 내용                                |
  |--------------------|--------------------------------------------|------------------------------------------|
  | application.yml    | backend/src/main/resources/application.yml | 라인 10-24: 사용자 관리 모드 설정 추가됨 |
  | docker-compose.yml | docker-compose.yml                         | 라인 47-52: 환경변수 추가됨              |
  | .env               | .env                                       | 라인 48-62: 외부 DB 설정 섹션 추가됨     |

  ---
  3. 테스트 방법

  Step 1: Internal 모드 테스트 (현재 기본값)

  # 이미지 재빌드 및 컨테이너 재시작
  docker compose down
  docker compose build backend
  docker compose up -d

  # 로그 확인
  docker logs -f taskflow-backend

  시작 로그에서 확인:
  UserManagementProperties: mode=internal
  PasswordEncoderConfig: Using BCryptPasswordEncoder

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
EXTERNAL_DB_URL=jdbc:mysql://외부DB주소:3306/외부DB명
EXTERNAL_DB_USERNAME=계정
EXTERNAL_DB_PASSWORD=비밀번호
```

---

## 외부 DB VIEW 생성 가이드

TaskFlow가 외부 DB의 데이터를 읽기 위해서는 아래 VIEW를 생성해야 합니다.

### VIEW 1: V_TASKFLOW_USER (사용자)

```sql
-- ============================================================
-- VIEW: V_TASKFLOW_USER
-- 설명: TaskFlow 사용자 정보 VIEW
-- ============================================================
CREATE OR REPLACE VIEW V_TASKFLOW_USER AS
SELECT
    -- 필수 컬럼 (NOT NULL)
    emp.EMP_ID          AS USER_ID,          -- 사용자 PK (BIGINT)
    emp.LOGIN_ID        AS USERNAME,         -- 로그인 아이디 (VARCHAR 50)
    emp.PASSWORD        AS PASSWORD,         -- SHA256 해시 비밀번호 (VARCHAR 255)
    emp.EMP_NAME        AS NAME,             -- 사용자 이름 (VARCHAR 100)

    -- 선택 컬럼 (NULL 허용)
    emp.EMAIL           AS EMAIL,            -- 이메일 (VARCHAR 255, NULL 가능)
    emp.DEPT_ID         AS DEPARTMENT_ID,    -- 부서 FK (BIGINT, NULL 가능)

    -- 사용 여부
    CASE
        WHEN emp.STATUS = 'ACTIVE' THEN 'Y'
        ELSE 'N'
    END                 AS USE_YN,           -- 사용 여부 (CHAR 1)

    -- 감사 컬럼
    emp.REG_DATE        AS CREATED_AT,       -- 생성일시 (DATETIME)
    emp.REG_USER_ID     AS CREATED_BY,       -- 생성자 ID (BIGINT, NULL 가능)
    emp.MOD_DATE        AS UPDATED_AT,       -- 수정일시 (DATETIME, NULL 가능)
    emp.MOD_USER_ID     AS UPDATED_BY,       -- 수정자 ID (BIGINT, NULL 가능)

    -- 마지막 로그인 (외부 DB에 없으면 NULL)
    NULL                AS LAST_LOGIN_AT     -- 마지막 로그인 (DATETIME, NULL)

FROM
    TB_EMPLOYEE emp  -- 외부 DB 사용자 테이블명으로 변경
;
```

#### 컬럼 매핑 상세

| TaskFlow 컬럼 | 타입 | 필수 | 설명 | 외부 DB 매핑 예시 |
|--------------|------|------|------|------------------|
| USER_ID | BIGINT | O | 사용자 PK | EMP_ID, USER_SEQ |
| USERNAME | VARCHAR(50) | O | 로그인 아이디 | LOGIN_ID, USER_ID |
| PASSWORD | VARCHAR(255) | O | SHA256 해시 | PASSWORD, PWD |
| NAME | VARCHAR(100) | O | 사용자명 | EMP_NAME, USER_NM |
| EMAIL | VARCHAR(255) | X | 이메일 | EMAIL, MAIL_ADDR |
| DEPARTMENT_ID | BIGINT | X | 부서 FK | DEPT_ID, DEPT_CD |
| USE_YN | CHAR(1) | O | 사용 여부 | 'Y'/'N' 변환 필요 |
| CREATED_AT | DATETIME | O | 생성일시 | REG_DATE, INS_DT |
| CREATED_BY | BIGINT | X | 생성자 ID | REG_USER_ID |
| UPDATED_AT | DATETIME | X | 수정일시 | MOD_DATE, UPD_DT |
| UPDATED_BY | BIGINT | X | 수정자 ID | MOD_USER_ID |
| LAST_LOGIN_AT | DATETIME | X | 최종 로그인 | 없으면 NULL |

#### 비밀번호 해시 형식

```sql
-- MySQL sha2() 함수 사용 예시
-- TaskFlow는 아래 형식의 SHA256 해시와 호환됩니다
SELECT sha2('password123', 256);
-- 결과: ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f
```

---

### VIEW 2: V_TASKFLOW_DEPARTMENT (부서)

```sql
-- ============================================================
-- VIEW: V_TASKFLOW_DEPARTMENT
-- 설명: TaskFlow 부서 정보 VIEW
-- ============================================================
CREATE OR REPLACE VIEW V_TASKFLOW_DEPARTMENT AS
SELECT
    -- 필수 컬럼 (NOT NULL)
    dept.DEPT_ID        AS DEPARTMENT_ID,    -- 부서 PK (BIGINT)
    dept.DEPT_CD        AS DEPARTMENT_CODE,  -- 부서 코드 (VARCHAR 50)
    dept.DEPT_NM        AS DEPARTMENT_NAME,  -- 부서명 (VARCHAR 100)

    -- 계층 구조
    dept.PARENT_DEPT_ID AS PARENT_ID,        -- 상위 부서 ID (BIGINT, NULL = 최상위)

    -- 정렬 및 사용 여부
    COALESCE(dept.SORT_ORD, 0) AS SORT_ORDER, -- 정렬 순서 (INT, 기본값 0)
    CASE
        WHEN dept.USE_FLAG = '1' THEN 'Y'
        ELSE 'N'
    END                 AS USE_YN,           -- 사용 여부 (CHAR 1)

    -- 감사 컬럼
    dept.REG_DATE       AS CREATED_AT,       -- 생성일시 (DATETIME)
    dept.REG_USER_ID    AS CREATED_BY,       -- 생성자 ID (BIGINT, NULL 가능)
    dept.MOD_DATE       AS UPDATED_AT,       -- 수정일시 (DATETIME, NULL 가능)
    dept.MOD_USER_ID    AS UPDATED_BY        -- 수정자 ID (BIGINT, NULL 가능)

FROM
    TB_DEPARTMENT dept  -- 외부 DB 부서 테이블명으로 변경
;
```

#### 컬럼 매핑 상세

| TaskFlow 컬럼 | 타입 | 필수 | 설명 | 외부 DB 매핑 예시 |
|--------------|------|------|------|------------------|
| DEPARTMENT_ID | BIGINT | O | 부서 PK | DEPT_ID, DEPT_SEQ |
| DEPARTMENT_CODE | VARCHAR(50) | O | 부서 코드 | DEPT_CD, DEPT_CODE |
| DEPARTMENT_NAME | VARCHAR(100) | O | 부서명 | DEPT_NM, DEPT_NAME |
| PARENT_ID | BIGINT | X | 상위 부서 ID | PARENT_DEPT_ID |
| SORT_ORDER | INT | O | 정렬 순서 | SORT_ORD, ORD_NO |
| USE_YN | CHAR(1) | O | 사용 여부 | 'Y'/'N' 변환 필요 |
| CREATED_AT | DATETIME | O | 생성일시 | REG_DATE |
| CREATED_BY | BIGINT | X | 생성자 ID | REG_USER_ID |
| UPDATED_AT | DATETIME | X | 수정일시 | MOD_DATE |
| UPDATED_BY | BIGINT | X | 수정자 ID | MOD_USER_ID |

---

## 실제 적용 예시

### 예시 1: 일반적인 인사 시스템 연동

```sql
-- 사용자 VIEW (인사 시스템 기준)
CREATE OR REPLACE VIEW V_TASKFLOW_USER AS
SELECT
    e.EMPLOYEE_NO       AS USER_ID,
    e.LOGIN_ID          AS USERNAME,
    e.PWD_HASH          AS PASSWORD,
    e.EMP_NAME          AS NAME,
    e.EMAIL_ADDR        AS EMAIL,
    d.DEPT_ID           AS DEPARTMENT_ID,
    CASE WHEN e.RETIRE_YN = 'N' THEN 'Y' ELSE 'N' END AS USE_YN,
    e.HIRE_DATE         AS CREATED_AT,
    1                   AS CREATED_BY,      -- 시스템 사용자 ID
    e.LAST_MOD_DATE     AS UPDATED_AT,
    e.LAST_MOD_USER     AS UPDATED_BY,
    NULL                AS LAST_LOGIN_AT
FROM HR_EMPLOYEE e
LEFT JOIN HR_DEPARTMENT d ON e.DEPT_CD = d.DEPT_CD
WHERE e.EMP_TYPE IN ('REGULAR', 'CONTRACT');  -- 정규직/계약직만

-- 부서 VIEW
CREATE OR REPLACE VIEW V_TASKFLOW_DEPARTMENT AS
SELECT
    DEPT_ID             AS DEPARTMENT_ID,
    DEPT_CD             AS DEPARTMENT_CODE,
    DEPT_NM             AS DEPARTMENT_NAME,
    UPPER_DEPT_ID       AS PARENT_ID,
    DISPLAY_ORDER       AS SORT_ORDER,
    CASE WHEN DEL_YN = 'N' THEN 'Y' ELSE 'N' END AS USE_YN,
    REG_DT              AS CREATED_AT,
    REG_USER            AS CREATED_BY,
    MOD_DT              AS UPDATED_AT,
    MOD_USER            AS UPDATED_BY
FROM HR_DEPARTMENT;
```

### 예시 2: 삭제된 사용자 포함 (이력 추적용)

```sql
-- 퇴사자도 포함하여 이력 추적 가능
CREATE OR REPLACE VIEW V_TASKFLOW_USER AS
SELECT
    EMP_ID              AS USER_ID,
    LOGIN_ID            AS USERNAME,
    sha2(CONCAT(EMP_ID, 'default_pw'), 256) AS PASSWORD,  -- 퇴사자는 기본 비밀번호
    EMP_NAME            AS NAME,
    EMAIL               AS EMAIL,
    DEPT_ID             AS DEPARTMENT_ID,
    CASE
        WHEN STATUS = 'ACTIVE' THEN 'Y'
        WHEN STATUS = 'RETIRED' THEN 'N'   -- 퇴사자
        WHEN STATUS = 'LEAVE' THEN 'N'     -- 휴직자
        ELSE 'N'
    END                 AS USE_YN,
    REG_DATE            AS CREATED_AT,
    1                   AS CREATED_BY,
    MOD_DATE            AS UPDATED_AT,
    NULL                AS UPDATED_BY,
    NULL                AS LAST_LOGIN_AT
FROM EMPLOYEE_MASTER;
```

---

## 주의사항

### 1. 비밀번호 호환성

TaskFlow External 모드는 MySQL `sha2(password, 256)` 함수와 호환됩니다.

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
COALESCE(CREATED_BY, 1) AS CREATED_BY
```

### 4. 부서 계층 구조

최상위 부서의 PARENT_ID는 반드시 NULL이어야 합니다.

```sql
CASE WHEN UPPER_DEPT_CD = 'ROOT' THEN NULL ELSE UPPER_DEPT_ID END AS PARENT_ID
```

---

## 기능 제한 사항

External 모드에서는 다음 기능이 비활성화됩니다:

| 기능 | Internal 모드 | External 모드 |
|------|--------------|--------------|
| 사용자 등록 | O | X |
| 사용자 수정 | O | X |
| 사용자 삭제 | O | X |
| 비밀번호 변경 | O | X |
| 부서 등록 | O | X |
| 부서 수정 | O | X |
| 부서 삭제 | O | X |
| 사용자 조회 | O | O |
| 부서 조회 | O | O |
| 로그인 | O | O |
| 보드/업무 관리 | O | O |

---

## 연결 테스트

VIEW 생성 후 아래 쿼리로 데이터 정합성을 확인합니다.

```sql
-- 사용자 데이터 확인
SELECT * FROM V_TASKFLOW_USER LIMIT 10;

-- 활성 사용자 수
SELECT COUNT(*) FROM V_TASKFLOW_USER WHERE USE_YN = 'Y';

-- 부서 데이터 확인
SELECT * FROM V_TASKFLOW_DEPARTMENT ORDER BY SORT_ORDER;

-- 최상위 부서
SELECT * FROM V_TASKFLOW_DEPARTMENT WHERE PARENT_ID IS NULL;

-- 부서별 사용자 수
SELECT
    d.DEPARTMENT_NAME,
    COUNT(u.USER_ID) as USER_COUNT
FROM V_TASKFLOW_DEPARTMENT d
LEFT JOIN V_TASKFLOW_USER u ON d.DEPARTMENT_ID = u.DEPARTMENT_ID
WHERE d.USE_YN = 'Y'
GROUP BY d.DEPARTMENT_ID, d.DEPARTMENT_NAME
ORDER BY d.SORT_ORDER;
```

---

## 문제 해결

### 로그인 실패

1. 비밀번호 해시 형식 확인
```sql
SELECT USERNAME, PASSWORD, LENGTH(PASSWORD) as PWD_LEN
FROM V_TASKFLOW_USER
WHERE USERNAME = '테스트계정';
-- PWD_LEN이 64여야 함 (SHA256 hex)
```

2. 대소문자 확인
```sql
-- SHA256 해시는 소문자여야 함
SELECT LOWER(sha2('password', 256));
```

### 부서 트리 표시 안됨

1. PARENT_ID 확인
```sql
-- 고아 부서 확인 (PARENT_ID가 존재하지 않는 부서를 참조)
SELECT c.*
FROM V_TASKFLOW_DEPARTMENT c
LEFT JOIN V_TASKFLOW_DEPARTMENT p ON c.PARENT_ID = p.DEPARTMENT_ID
WHERE c.PARENT_ID IS NOT NULL AND p.DEPARTMENT_ID IS NULL;
```

### 사용자-부서 연결 안됨

1. DEPARTMENT_ID 정합성 확인
```sql
-- 존재하지 않는 부서를 참조하는 사용자
SELECT u.*
FROM V_TASKFLOW_USER u
LEFT JOIN V_TASKFLOW_DEPARTMENT d ON u.DEPARTMENT_ID = d.DEPARTMENT_ID
WHERE u.DEPARTMENT_ID IS NOT NULL AND d.DEPARTMENT_ID IS NULL;
```

  완료된 작업 요약

  | 구분     | 파일                               | 설명                             |
  |----------|------------------------------------|----------------------------------|
  | 설정     | UserManagementProperties.java      | internal/external 모드 전환 설정 |
  |          | PasswordEncoderConfig.java         | BCrypt/SHA256 동적 선택          |
  |          | DataSourceConfig.java              | 외부 DB DataSource 설정          |
  |          | ExternalMapperConfig.java          | 외부 Mapper 스캔 설정            |
  |          | application.yml                    | 전체 설정 옵션 추가              |
  | 보안     | Sha256PasswordEncoder.java         | MySQL sha2() 호환 인코더         |
  | Mapper   | ExternalUserMapper.java/.xml       | 외부 사용자 조회                 |
  |          | ExternalDepartmentMapper.xml/.xml  | 외부 부서 조회                   |
  | Service  | ExternalUserServiceImpl.java       | 읽기 전용 사용자 서비스          |
  |          | ExternalDepartmentServiceImpl.java | 읽기 전용 부서 서비스            |
  | API      | ConfigController.java              | 시스템 설정 API                  |
  |          | SystemConfigResponse.java          | 설정 응답 DTO                    |
  | Frontend | config.ts (api/stores)             | 설정 조회 및 상태 관리           |
  | 문서     | docs/external-db-setup.md          | VIEW 생성 가이드                 |

  사용 방법

  1. Internal 모드 (기본): 기존 방식 그대로 사용
  2. External 모드: application.yml 또는 환경변수로 전환