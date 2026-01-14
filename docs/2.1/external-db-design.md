# 외부 DB 연결 및 쿼리 기능 설계

## 개요
속성(PropertyDef)의 SELECT/MULTI_SELECT/CHECKBOX 타입에서 외부 DB 쿼리 결과를 동적 옵션으로 사용할 수 있는 기능 구현

### 핵심 기능
1. **외부 DB 연결 설정**: 여러 외부 DB(MySQL, Oracle, MSSQL, Tibero) 정의 및 관리
2. **외부 쿼리 정의**: GLOBAL/MANAGER/USER 소유 유형별 쿼리 관리, 테스트 기능
3. **속성 연동**: 속성에서 외부 쿼리를 데이터 소스로 선택

### 기술 조건
| 항목 | 선택 |
|------|------|
| 지원 DB | MySQL, Oracle, MSSQL, Tibero |
| 암호화 | AES-256-GCM 양방향 암호화 |
| 쿼리 보안 | SELECT만 허용 + readOnly 트랜잭션 |
| 캐싱 | 쿼리별 선택적 TTL 캐싱 (Caffeine) |

---

## 1. ERD

```
┌─────────────────────────┐         ┌─────────────────────────┐
│ TB_EXTERNAL_DATASOURCE  │ 1─────* │   TB_EXTERNAL_QUERY     │
├─────────────────────────┤         ├─────────────────────────┤
│ DATASOURCE_ID (PK)      │         │ QUERY_ID (PK)           │
│ DATASOURCE_CODE (UK)    │         │ DATASOURCE_ID (FK)      │
│ DATASOURCE_NAME         │         │ QUERY_CODE (UK)         │
│ DB_TYPE                 │         │ QUERY_NAME              │
│ HOST / PORT             │         │ QUERY_SQL               │
│ DATABASE_NAME           │         │ OWNER_TYPE              │
│ USERNAME                │         │ OWNER_USERNAME          │
│ PASSWORD_ENCRYPTED      │         │ OWNER_DEPT_CODE         │
│ CONNECTION_TIMEOUT      │         │ VALUE_COLUMN            │
│ QUERY_TIMEOUT           │         │ LABEL_COLUMN            │
│ MAX_POOL_SIZE           │         │ COLOR_COLUMN            │
│ USE_YN                  │         │ CACHE_ENABLED_YN        │
└─────────────────────────┘         │ CACHE_TTL_SECONDS       │
                                    │ USE_YN                  │
                                    └────────────┬────────────┘
                                                 │ 1
                                                 ▼
                                    ┌─────────────────────────┐
                                    │   TB_PROPERTY_DEF       │
                                    │   (기존 + 신규 컬럼)     │
                                    ├─────────────────────────┤
                                    │ + EXTERNAL_QUERY_ID     │
                                    │ + DATA_SOURCE_TYPE      │
                                    │   (INTERNAL/EXTERNAL)   │
                                    └─────────────────────────┘
```

---

## 2. 테이블 정의

### 2.1 TB_EXTERNAL_DATASOURCE (외부 DB 연결 정보)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| DATASOURCE_ID | BIGINT | N | AUTO_INCREMENT | PK |
| DATASOURCE_CODE | VARCHAR(50) | N | | 고유 코드 (UK) |
| DATASOURCE_NAME | VARCHAR(100) | N | | 표시 이름 |
| DB_TYPE | VARCHAR(20) | N | | MYSQL/ORACLE/MSSQL/TIBERO |
| HOST | VARCHAR(255) | N | | 호스트 주소 |
| PORT | INT | N | | 포트 번호 |
| DATABASE_NAME | VARCHAR(100) | N | | 데이터베이스명 |
| SCHEMA_NAME | VARCHAR(100) | Y | | 스키마명 (Oracle/MSSQL) |
| USERNAME | VARCHAR(100) | N | | 접속 계정 |
| PASSWORD_ENCRYPTED | VARCHAR(500) | N | | AES-256 암호화 비밀번호 |
| CONNECTION_TIMEOUT | INT | N | 5000 | 연결 타임아웃 ms |
| QUERY_TIMEOUT | INT | N | 30000 | 쿼리 타임아웃 ms |
| MAX_POOL_SIZE | INT | N | 3 | 최대 풀 크기 |
| USE_YN | CHAR(1) | N | 'Y' | 사용 여부 |
| CREATED_AT | DATETIME | N | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | VARCHAR(50) | N | | 생성자 USERNAME |
| UPDATED_AT | DATETIME | Y | | 수정일시 |
| UPDATED_BY | VARCHAR(50) | Y | | 수정자 USERNAME |
| DELETED_AT | DATETIME | Y | | 삭제일시 |
| DELETED_BY | VARCHAR(50) | Y | | 삭제자 USERNAME |

**인덱스**: UK_EDS_CODE, IDX_EDS_USE, IDX_EDS_TYPE

---

### 2.2 TB_EXTERNAL_QUERY (외부 쿼리 정의)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| QUERY_ID | BIGINT | N | AUTO_INCREMENT | PK |
| DATASOURCE_ID | BIGINT | N | | FK → TB_EXTERNAL_DATASOURCE |
| QUERY_CODE | VARCHAR(50) | N | | 쿼리 고유 코드 (UK) |
| QUERY_NAME | VARCHAR(100) | N | | 쿼리 표시명 |
| DESCRIPTION | VARCHAR(500) | Y | | 쿼리 설명 |
| QUERY_SQL | TEXT | N | | SELECT 쿼리문 |
| OWNER_TYPE | VARCHAR(20) | N | 'USER' | GLOBAL/MANAGER/USER |
| OWNER_USERNAME | VARCHAR(50) | Y | | 소유자 USERNAME |
| OWNER_DEPT_CODE | VARCHAR(20) | Y | | 소유자 부서 코드 |
| VALUE_COLUMN | VARCHAR(100) | N | 'value' | 값 컬럼명 |
| LABEL_COLUMN | VARCHAR(100) | N | 'label' | 라벨 컬럼명 |
| COLOR_COLUMN | VARCHAR(100) | Y | | 색상 컬럼명 |
| CACHE_ENABLED_YN | CHAR(1) | N | 'N' | 캐시 사용 여부 |
| CACHE_TTL_SECONDS | INT | Y | 300 | 캐시 TTL 초 |
| SORT_ORDER | INT | N | 0 | 정렬 순서 |
| LAST_EXECUTED_AT | DATETIME | Y | | 마지막 실행 시간 |
| LAST_RESULT_COUNT | INT | Y | | 마지막 결과 건수 |
| USE_YN | CHAR(1) | N | 'Y' | 사용 여부 |
| CREATED_AT/BY, UPDATED_AT/BY, DELETED_AT/BY | | | | 감사 필드 |

**인덱스**: UK_EQ_CODE, IDX_EQ_DATASOURCE, IDX_EQ_OWNER_TYPE, IDX_EQ_OWNER_USER, IDX_EQ_USE

---

### 2.3 TB_PROPERTY_DEF (기존 테이블 컬럼 추가)

| 추가 컬럼 | 타입 | NULL | 기본값 | 설명 |
|-----------|------|------|--------|------|
| EXTERNAL_QUERY_ID | BIGINT | Y | NULL | FK → TB_EXTERNAL_QUERY |
| DATA_SOURCE_TYPE | VARCHAR(20) | N | 'INTERNAL' | INTERNAL/EXTERNAL |

---

## 3. API 명세

### 3.1 외부 DB 관리 API (ADMIN 전용)

| Method | URL | 설명 |
|--------|-----|------|
| GET | /api/external-datasources | 목록 조회 |
| POST | /api/external-datasources | 생성 |
| GET | /api/external-datasources/{id} | 상세 조회 |
| PUT | /api/external-datasources/{id} | 수정 |
| DELETE | /api/external-datasources/{id} | 삭제 |
| POST | /api/external-datasources/{id}/test-connection | 연결 테스트 |

### 3.2 외부 쿼리 관리 API

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/external-queries/global | 글로벌 쿼리 목록 | ALL |
| POST | /api/external-queries/global | 글로벌 쿼리 생성 | ADMIN |
| GET | /api/external-queries/manager | 매니저 쿼리 목록 | MANAGER+ |
| POST | /api/external-queries/manager | 매니저 쿼리 생성 | MANAGER |
| GET | /api/external-queries/user | 사용자 쿼리 목록 | USER |
| POST | /api/external-queries/user | 사용자 쿼리 생성 | USER |
| GET | /api/external-queries/accessible | 접근 가능 전체 | ALL |
| GET | /api/external-queries/{id} | 상세 조회 | 소유자/관리자 |
| PUT | /api/external-queries/{id} | 수정 | 소유자/관리자 |
| DELETE | /api/external-queries/{id} | 삭제 | 소유자/관리자 |
| POST | /api/external-queries/test | 쿼리 테스트 (미저장) | MANAGER+ |
| GET | /api/external-queries/{id}/execute | 쿼리 실행 (옵션 조회) | ALL |

---

## 4. 백엔드 구조

### 4.1 패키지 구조

```
com.taskflow
├── config/
│   ├── EncryptionConfig.java           # AES 암호화 설정
│   └── ExternalDatasourceConfig.java   # 동적 DataSource 설정
├── common/
│   └── enums/
│       ├── DbType.java                 # MYSQL/ORACLE/MSSQL/TIBERO
│       └── DataSourceType.java         # INTERNAL/EXTERNAL
├── domain/
│   ├── ExternalDatasource.java
│   └── ExternalQuery.java
├── dto/external/
│   ├── ExternalDatasourceCreateRequest.java
│   ├── ExternalDatasourceUpdateRequest.java
│   ├── ExternalDatasourceResponse.java
│   ├── ExternalQueryCreateRequest.java
│   ├── ExternalQueryUpdateRequest.java
│   ├── ExternalQueryResponse.java
│   ├── QueryTestRequest.java
│   ├── QueryTestResponse.java
│   ├── QueryExecuteResponse.java
│   └── ConnectionTestResponse.java
├── mapper/
│   ├── ExternalDatasourceMapper.java/.xml
│   └── ExternalQueryMapper.java/.xml
├── service/
│   ├── EncryptionService.java          # AES-256-GCM
│   ├── DynamicDatasourceManager.java   # 동적 DataSource 관리
│   ├── QueryValidationService.java     # SQL 검증
│   ├── QueryCacheService.java          # Caffeine 캐싱
│   ├── ExternalDatasourceService.java
│   └── ExternalQueryService.java
└── controller/
    ├── ExternalDatasourceController.java
    └── ExternalQueryController.java
```

### 4.2 핵심 서비스

#### EncryptionService (AES-256-GCM)
- encrypt(plainText) → Base64(IV + encrypted)
- decrypt(encryptedText) → plainText

#### DynamicDatasourceManager
- createOrUpdateDatasource(config) - HikariCP 풀 생성
- testConnection(config) - 연결 테스트
- executeQuery(datasourceId, sql, maxRows) - 쿼리 실행 (readOnly)
- removeDatasource(datasourceId) - 풀 종료

#### QueryValidationService
- validateQuery(sql) - SELECT만 허용, 금지 키워드 필터링

#### QueryCacheService (Caffeine)
- get(queryId) - 캐시 조회
- put(queryId, options, ttlSeconds) - 캐시 저장
- evict(queryId) - 캐시 무효화

### 4.3 DB별 JDBC 설정 (버전별 상세)

#### 개요
DB 버전에 따라 JDBC 드라이버 및 URL 패턴이 다를 수 있습니다. 본 시스템은 다양한 버전을 지원하기 위해 아래와 같은 설정을 제공합니다.

---

#### 4.3.1 MySQL

| 버전 | Driver Class | Maven Artifact | URL 패턴 | 비고 |
|------|--------------|----------------|----------|------|
| 5.x | com.mysql.jdbc.Driver | mysql:mysql-connector-java:5.1.49 | jdbc:mysql://host:port/db | Legacy Driver |
| 8.0+ | com.mysql.cj.jdbc.Driver | mysql:mysql-connector-java:8.0.33 | jdbc:mysql://host:port/db | **권장** |

**MySQL 8.0+ 특성:**
- `useSSL=false` 또는 `sslMode=DISABLED` 필요 (비보안 환경)
- `allowPublicKeyRetrieval=true` 필요할 수 있음
- `serverTimezone=Asia/Seoul` 권장
- `useUnicode=true&characterEncoding=UTF-8`

**URL 템플릿:**
```
jdbc:mysql://{host}:{port}/{database}?useSSL=false&serverTimezone=Asia/Seoul&useUnicode=true&characterEncoding=UTF-8
```

---

#### 4.3.2 Oracle

| 버전 | Driver Class | Maven Artifact | URL 패턴 | 비고 |
|------|--------------|----------------|----------|------|
| 11g | oracle.jdbc.driver.OracleDriver | com.oracle.database.jdbc:ojdbc6:11.2.0.4 | jdbc:oracle:thin:@host:port:SID | SID 방식 |
| 12c+ | oracle.jdbc.OracleDriver | com.oracle.database.jdbc:ojdbc8:19.8.0.0 | jdbc:oracle:thin:@//host:port/service | Service Name 방식 |
| 19c/21c | oracle.jdbc.OracleDriver | com.oracle.database.jdbc:ojdbc11:21.9.0.0 | jdbc:oracle:thin:@//host:port/service | **권장** |

**Oracle 특성:**
- 11g: SID 기반 접속 (`@host:port:SID`)
- 12c+: Service Name 기반 접속 (`@//host:port/SERVICE_NAME`)
- TNS 방식도 지원: `jdbc:oracle:thin:@(DESCRIPTION=...)`

**스키마 처리 (선택적):**
- Oracle은 스키마 = 사용자 개념
- 쿼리에서 직접 스키마 지정: `SELECT * FROM SCHEMA1.TABLE1 t1 JOIN SCHEMA2.TABLE2 t2 ON ...`
- SCHEMA_NAME 컬럼은 **선택적**으로 문서화 용도로만 사용

**URL 템플릿:**
```
# SID 방식 (11g 호환)
jdbc:oracle:thin:@{host}:{port}:{sid}

# Service Name 방식 (12c+)
jdbc:oracle:thin:@//{host}:{port}/{serviceName}
```

---

#### 4.3.3 MSSQL (SQL Server)

| 버전 | Driver Class | Maven Artifact | URL 패턴 | 비고 |
|------|--------------|----------------|----------|------|
| 2012-2016 | com.microsoft.sqlserver.jdbc.SQLServerDriver | com.microsoft.sqlserver:mssql-jdbc:9.4.1.jre11 | jdbc:sqlserver://host:port;databaseName=db | |
| 2017+ | com.microsoft.sqlserver.jdbc.SQLServerDriver | com.microsoft.sqlserver:mssql-jdbc:12.4.2.jre11 | jdbc:sqlserver://host:port;databaseName=db;encrypt=true | **권장** |

**MSSQL 특성:**
- 2017+: `encrypt=true` 기본 (필요시 `trustServerCertificate=true` 추가)
- Windows 인증 시: `integratedSecurity=true` (JTDS 드라이버 필요할 수 있음)
- Multi-Schema 지원: 쿼리에서 `[database].[schema].[table]` 형식

**스키마 처리 (선택적):**
- 기본 스키마: `dbo`
- 쿼리에서 직접 지정: `SELECT * FROM schema1.table1 t1 JOIN schema2.table2 t2`
- Cross-Database 조인: `SELECT * FROM db1.dbo.table1 JOIN db2.dbo.table2`

**URL 템플릿:**
```
jdbc:sqlserver://{host}:{port};databaseName={database};encrypt=false;trustServerCertificate=true
```

---

#### 4.3.4 Tibero

| 버전 | Driver Class | Maven Artifact | URL 패턴 | 비고 |
|------|--------------|----------------|----------|------|
| 5.x | com.tmax.tibero.jdbc.TbDriver | (로컬 설치) tibero5-jdbc.jar | jdbc:tibero:thin:@host:port:db | |
| 6.x | com.tmax.tibero.jdbc.TbDriver | (로컬 설치) tibero6-jdbc.jar | jdbc:tibero:thin:@host:port:db | **권장** |
| 7.x | com.tmax.tibero.jdbc.TbDriver | (로컬 설치) tibero7-jdbc.jar | jdbc:tibero:thin:@host:port:db | |

**Tibero 특성:**
- Maven Central에 없음 → 로컬 저장소 또는 직접 설치 필요
- Oracle과 유사한 구조 (스키마 = 사용자)
- 호환성: Oracle SQL 대부분 지원

**스키마 처리 (선택적):**
- Oracle과 동일하게 쿼리에서 직접 스키마 지정
- `SELECT * FROM SCHEMA1.TABLE1 JOIN SCHEMA2.TABLE2`

**URL 템플릿:**
```
jdbc:tibero:thin:@{host}:{port}:{database}
```

---

#### 4.3.5 스키마 처리 정책

| DB | 스키마 개념 | 등록 필수 여부 | Multi-Schema 조인 |
|-----|-----------|---------------|------------------|
| MySQL | Database = Schema | 불필요 (DATABASE_NAME 사용) | 쿼리에서 `db.table` 형식 |
| Oracle | Schema = User | **선택적** (문서화 용도) | ✅ `schema.table` 형식 |
| MSSQL | Database + Schema | **선택적** (기본 dbo) | ✅ `[db].[schema].[table]` |
| Tibero | Schema = User | **선택적** (문서화 용도) | ✅ `schema.table` 형식 |

**핵심 원칙:**
- `SCHEMA_NAME` 컬럼은 선택적 필드 (NULL 허용)
- 쿼리 작성 시 사용자가 직접 스키마/테이블을 Full Path로 지정
- 다중 스키마 조인이 필요한 경우 쿼리 SQL에서 직접 처리

---

#### 4.3.6 JDBC 드라이버 의존성 (build.gradle)

```groovy
dependencies {
    // MySQL 8.0+
    runtimeOnly 'mysql:mysql-connector-java:8.0.33'

    // Oracle 19c/21c
    runtimeOnly 'com.oracle.database.jdbc:ojdbc11:21.9.0.0'

    // MSSQL 2017+
    runtimeOnly 'com.microsoft.sqlserver:mssql-jdbc:12.4.2.jre11'

    // Tibero (로컬 설치 필요)
    // runtimeOnly files('libs/tibero6-jdbc.jar')
}
```

---

## 5. 프론트엔드 구조

### 5.1 디렉토리

```
frontend/src/
├── api/
│   ├── externalDatasource.ts
│   └── externalQuery.ts
├── types/
│   ├── externalDatasource.ts
│   └── externalQuery.ts
├── stores/
│   ├── externalDatasource.ts
│   └── externalQuery.ts
└── components/settings/
    ├── ExternalDatasourceContent.vue   # 데이터소스 관리
    ├── ExternalDatasourceModal.vue     # 데이터소스 추가/수정
    ├── ExternalQueryContent.vue        # 쿼리 관리
    ├── ExternalQueryModal.vue          # 쿼리 추가/수정
    └── QueryTestModal.vue              # 쿼리 테스트
```

### 5.2 설정 메뉴 구조

```
설정
├── 기존 메뉴들...
├── 외부 DB 관리 (ADMIN 전용)    # 신규
└── 외부 쿼리 관리              # 신규
```

### 5.3 UI 목업

#### 외부 DB 관리
```
┌──────────────────────────────────────────────────────────────┐
│ 외부 DB 관리                               [+ 데이터소스 추가] │
├──────────────────────────────────────────────────────────────┤
│ [MySQL]  ERP_PROD                              ● 연결됨      │
│ erp-db.company.com:3306 / erp_database                      │
│ [테스트] [수정] [삭제]                                        │
├──────────────────────────────────────────────────────────────┤
│ [Oracle] HR_DB                                 ○ 미연결      │
│ hr-oracle.company.com:1521 / HRPROD                         │
│ [테스트] [수정] [삭제]                                        │
└──────────────────────────────────────────────────────────────┘
```

#### 외부 쿼리 관리
```
┌───────────┬──────────────────────────────────────────────────┐
│           │ 부서 목록 (DEPT_LIST)              ERP_PROD      │
│ 글로벌 (3) │ SELECT dept_code AS value, dept_name AS label   │
│           │ 캐시: 5분 | 마지막 실행: 32건                     │
│ 매니저 (5) │ [테스트] [실행] [수정] [삭제]                    │
│           ├──────────────────────────────────────────────────┤
│ 개인 (2)  │ ...                                              │
└───────────┴──────────────────────────────────────────────────┘
```

#### 속성 편집 시 외부 쿼리 연동
```
┌──────────────────────────────────────────────────────────────┐
│ 속성명: [담당 부서              ]                            │
│ 타입:   [단일 선택 ▼]                                        │
│                                                              │
│ 옵션 데이터 소스:                                             │
│ [○ 직접 입력]  [● 외부 쿼리]                                 │
│                                                              │
│ 외부 쿼리 선택: [부서 목록 (DEPT_LIST) - ERP_PROD     ▼]     │
│                                                              │
│ 미리보기:                                                     │
│ ┌──────────┬────────────┐                                    │
│ │ DEV      │ 개발부     │                                    │
│ │ HR       │ 인사부     │                                    │
│ └──────────┴────────────┘                                    │
└──────────────────────────────────────────────────────────────┘
```

---

## 6. 보안 고려사항

| 항목 | 대응 |
|------|------|
| 비밀번호 저장 | AES-256-GCM 암호화 (IV 포함) |
| SQL Injection | SELECT만 허용, 금지 키워드 필터링, 다중 쿼리 방지 |
| 읽기 전용 보장 | HikariDataSource.readOnly + Connection.setReadOnly |
| 권한 관리 | GLOBAL(ADMIN), MANAGER(본인+상위부서), USER(본인) |
| 타임아웃 | 연결 5초, 쿼리 30초, maxRows 1000건 |

### 금지 키워드 목록
INSERT, UPDATE, DELETE, DROP, CREATE, ALTER, TRUNCATE, GRANT, REVOKE, EXECUTE, EXEC, XP_, SP_, INTO OUTFILE, INTO DUMPFILE, LOAD_FILE

---

## 7. 구현 순서 및 개발 아이템 체크리스트

### Phase 1: 기반 인프라 (DB/설정)

| # | 아이템 | 파일 | 상태 | 비고 |
|---|--------|------|------|------|
| 1.1 | 마이그레이션 SQL 생성 | `docker/mysql/init/08_external_datasource.sql` | ⬜ | TB_EXTERNAL_DATASOURCE, TB_EXTERNAL_QUERY |
| 1.2 | 01_schema.sql 동기화 | `docker/mysql/init/01_schema.sql` | ⬜ | TB_PROPERTY_DEF 컬럼 추가 |
| 1.3 | AES 암호화 설정 | `config/EncryptionConfig.java` | ⬜ | 키 환경변수 |
| 1.4 | AES 암호화 서비스 | `service/EncryptionService.java` | ⬜ | encrypt/decrypt |
| 1.5 | DbType Enum | `common/enums/DbType.java` | ⬜ | MYSQL/ORACLE/MSSQL/TIBERO |
| 1.6 | DataSourceType Enum | `common/enums/DataSourceType.java` | ⬜ | INTERNAL/EXTERNAL |
| 1.7 | build.gradle 의존성 | `build.gradle` | ⬜ | JDBC 드라이버, Caffeine |
| 1.8 | application.yml 설정 | `application.yml` | ⬜ | 암호화 키, 타임아웃 설정 |

---

### Phase 2: Domain/DTO/Mapper

| # | 아이템 | 파일 | 상태 | 비고 |
|---|--------|------|------|------|
| 2.1 | ExternalDatasource Domain | `domain/ExternalDatasource.java` | ⬜ | |
| 2.2 | ExternalQuery Domain | `domain/ExternalQuery.java` | ⬜ | |
| 2.3 | ExternalDatasource DTOs | `dto/external/ExternalDatasource*.java` | ⬜ | Create/Update/Response (3개) |
| 2.4 | ExternalQuery DTOs | `dto/external/ExternalQuery*.java` | ⬜ | Create/Update/Response (3개) |
| 2.5 | 테스트/실행 DTOs | `dto/external/Query*.java, Connection*.java` | ⬜ | Test/Execute Request/Response (4개) |
| 2.6 | ExternalDatasource Mapper | `mapper/ExternalDatasourceMapper.java/.xml` | ⬜ | CRUD |
| 2.7 | ExternalQuery Mapper | `mapper/ExternalQueryMapper.java/.xml` | ⬜ | CRUD + Owner조회 |
| 2.8 | PropertyDef 수정 | `domain/PropertyDef.java` | ⬜ | externalQueryId, dataSourceType 필드 |
| 2.9 | PropertyDefMapper 수정 | `mapper/PropertyDefMapper.xml` | ⬜ | 신규 컬럼 매핑 |

---

### Phase 3: 핵심 서비스

| # | 아이템 | 파일 | 상태 | 비고 |
|---|--------|------|------|------|
| 3.1 | 동적 DataSource 관리자 | `service/DynamicDatasourceManager.java` | ⬜ | HikariCP 풀 동적 생성/제거 |
| 3.2 | DynamicDatasourceManager Impl | `service/impl/DynamicDatasourceManagerImpl.java` | ⬜ | DB별 URL 빌더 포함 |
| 3.3 | 쿼리 검증 서비스 | `service/QueryValidationService.java` | ⬜ | SELECT 검증, 금지 키워드 |
| 3.4 | QueryValidationService Impl | `service/impl/QueryValidationServiceImpl.java` | ⬜ | |
| 3.5 | 쿼리 캐싱 서비스 | `service/QueryCacheService.java` | ⬜ | Caffeine TTL 캐싱 |
| 3.6 | QueryCacheService Impl | `service/impl/QueryCacheServiceImpl.java` | ⬜ | |
| 3.7 | ExternalDatasource 설정 | `config/ExternalDatasourceConfig.java` | ⬜ | 초기화/종료 훅 |

---

### Phase 4: API (Service/Controller)

| # | 아이템 | 파일 | 상태 | 비고 |
|---|--------|------|------|------|
| 4.1 | ExternalDatasourceService | `service/ExternalDatasourceService.java` | ⬜ | 인터페이스 |
| 4.2 | ExternalDatasourceServiceImpl | `service/impl/ExternalDatasourceServiceImpl.java` | ⬜ | CRUD + 연결테스트 |
| 4.3 | ExternalQueryService | `service/ExternalQueryService.java` | ⬜ | 인터페이스 |
| 4.4 | ExternalQueryServiceImpl | `service/impl/ExternalQueryServiceImpl.java` | ⬜ | CRUD + 쿼리테스트/실행 |
| 4.5 | ExternalDatasourceController | `controller/ExternalDatasourceController.java` | ⬜ | REST API |
| 4.6 | ExternalQueryController | `controller/ExternalQueryController.java` | ⬜ | REST API (OWNER_TYPE별) |
| 4.7 | PropertyService 수정 | `service/impl/PropertyServiceImpl.java` | ⬜ | 외부 옵션 조회 통합 |

---

### Phase 5: 프론트엔드 - 기반

| # | 아이템 | 파일 | 상태 | 비고 |
|---|--------|------|------|------|
| 5.1 | ExternalDatasource 타입 | `types/externalDatasource.ts` | ⬜ | |
| 5.2 | ExternalQuery 타입 | `types/externalQuery.ts` | ⬜ | |
| 5.3 | ExternalDatasource API | `api/externalDatasource.ts` | ⬜ | |
| 5.4 | ExternalQuery API | `api/externalQuery.ts` | ⬜ | |
| 5.5 | ExternalDatasource Store | `stores/externalDatasource.ts` | ⬜ | |
| 5.6 | ExternalQuery Store | `stores/externalQuery.ts` | ⬜ | |

---

### Phase 6: 프론트엔드 - 컴포넌트

| # | 아이템 | 파일 | 상태 | 비고 |
|---|--------|------|------|------|
| 6.1 | 데이터소스 목록 | `components/settings/ExternalDatasourceContent.vue` | ⬜ | |
| 6.2 | 데이터소스 모달 | `components/settings/ExternalDatasourceModal.vue` | ⬜ | 추가/수정 |
| 6.3 | 쿼리 목록 | `components/settings/ExternalQueryContent.vue` | ⬜ | Owner타입별 탭 |
| 6.4 | 쿼리 모달 | `components/settings/ExternalQueryModal.vue` | ⬜ | 추가/수정 |
| 6.5 | 쿼리 테스트 모달 | `components/settings/QueryTestModal.vue` | ⬜ | 테스트/결과 미리보기 |
| 6.6 | SettingsView 탭 추가 | `views/SettingsView.vue` | ⬜ | 외부 DB/쿼리 탭 |
| 6.7 | PropertiesContent 수정 | `components/settings/PropertiesContent.vue` | ⬜ | 외부 쿼리 연동 UI |

---

### Phase 7: 테스트 및 마무리

| # | 아이템 | 설명 | 상태 | 비고 |
|---|--------|------|------|------|
| 7.1 | MySQL 연결/쿼리 테스트 | | ⬜ | |
| 7.2 | Oracle 연결/쿼리 테스트 | | ⬜ | (환경 있을 경우) |
| 7.3 | MSSQL 연결/쿼리 테스트 | | ⬜ | (환경 있을 경우) |
| 7.4 | Tibero 연결/쿼리 테스트 | | ⬜ | (환경 있을 경우) |
| 7.5 | SQL Injection 차단 테스트 | | ⬜ | INSERT/UPDATE/DELETE 차단 |
| 7.6 | 권한 테스트 | | ⬜ | GLOBAL/MANAGER/USER |
| 7.7 | 캐시 동작 테스트 | | ⬜ | TTL 만료/무효화 |
| 7.8 | E2E 속성 연동 테스트 | | ⬜ | 외부 쿼리 → 속성 옵션 |

---

### 총 개발 아이템 수

| Phase | 아이템 수 |
|-------|---------|
| Phase 1: 기반 인프라 | 8개 |
| Phase 2: Domain/DTO/Mapper | 9개 |
| Phase 3: 핵심 서비스 | 7개 |
| Phase 4: API | 7개 |
| Phase 5: 프론트엔드 기반 | 6개 |
| Phase 6: 프론트엔드 컴포넌트 | 7개 |
| Phase 7: 테스트 | 8개 |
| **합계** | **52개** |

---

## 8. 수정 파일 목록

### 신규 생성

| 파일 | 설명 |
|------|------|
| docker/mysql/init/08_external_datasource.sql | 마이그레이션 |
| EncryptionConfig.java | AES 설정 |
| EncryptionService.java / Impl | 암호화 |
| DynamicDatasourceManager.java / Impl | 동적 DataSource |
| QueryValidationService.java / Impl | SQL 검증 |
| QueryCacheService.java / Impl | 캐싱 |
| DbType.java / DataSourceType.java | Enum |
| ExternalDatasource.java / ExternalQuery.java | Domain |
| ExternalDatasourceMapper.java/.xml | Mapper |
| ExternalQueryMapper.java/.xml | Mapper |
| ExternalDatasourceService.java / Impl | Service |
| ExternalQueryService.java / Impl | Service |
| ExternalDatasourceController.java | Controller |
| ExternalQueryController.java | Controller |
| DTO 클래스들 (10개) | Request/Response |
| frontend 타입/API/Store (6개 파일) | 프론트엔드 |
| Vue 컴포넌트 (5개) | UI |

### 수정

| 파일 | 변경 내용 |
|------|---------|
| docker/mysql/init/01_schema.sql | TB_PROPERTY_DEF 컬럼 추가 |
| PropertyDef.java | externalQueryId, dataSourceType 필드 |
| PropertyDefMapper.xml | 신규 컬럼 매핑 |
| PropertyService / Impl | 외부 옵션 조회 통합 |
| PropertiesContent.vue | 외부 쿼리 선택 UI |
| SettingsView.vue | 탭 추가 |
| application.yml | 암호화 키 설정 |
| build.gradle | Caffeine 의존성 |

---

## 9. 검증 방법

1. **연결 테스트**: MySQL, Oracle, MSSQL, Tibero 각각 연결 성공/실패 확인
2. **쿼리 검증**: INSERT/UPDATE/DELETE 시도 시 차단 확인
3. **캐싱 테스트**: TTL 경과 전후 캐시 히트/미스 확인
4. **권한 테스트**: OWNER_TYPE별 접근 제한 확인
5. **속성 연동**: SELECT 속성에서 외부 쿼리 옵션 드롭다운 동작 확인
6. **성능 테스트**: 대량 데이터 조회 시 maxRows 제한 확인

---

## 10. application.yml 설정 추가

```yaml
# 암호화 설정
encryption:
  aes:
    secret-key: ${AES_SECRET_KEY:base64EncodedKey...}

# 외부 쿼리 설정
external-query:
  default-query-timeout: 30000
  max-result-rows: 1000
  default-cache-ttl: 300
```

---

## 11. DataSource 설정 템플릿 (DB별 예시)

### 11.1 MySQL 템플릿

#### MySQL 8.0+ (권장)
```json
{
  "datasourceCode": "ERP_MYSQL",
  "datasourceName": "ERP MySQL DB",
  "dbType": "MYSQL",
  "host": "mysql-erp.company.com",
  "port": 3306,
  "databaseName": "erp_production",
  "schemaName": null,
  "username": "readonly_user",
  "password": "password123",
  "connectionTimeout": 5000,
  "queryTimeout": 30000,
  "maxPoolSize": 3
}
```

**생성되는 JDBC URL:**
```
jdbc:mysql://mysql-erp.company.com:3306/erp_production?useSSL=false&serverTimezone=Asia/Seoul&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
```

#### MySQL 5.x (레거시)
```json
{
  "datasourceCode": "LEGACY_MYSQL",
  "datasourceName": "Legacy MySQL 5.7",
  "dbType": "MYSQL",
  "driverVersion": "5.x",
  "host": "old-mysql.company.com",
  "port": 3306,
  "databaseName": "legacy_db",
  "username": "readonly_user",
  "password": "password123"
}
```

**생성되는 JDBC URL:**
```
jdbc:mysql://old-mysql.company.com:3306/legacy_db?useSSL=false&useUnicode=true&characterEncoding=UTF-8
```

---

### 11.2 Oracle 템플릿

#### Oracle 19c/21c (Service Name 방식, 권장)
```json
{
  "datasourceCode": "HR_ORACLE",
  "datasourceName": "HR Oracle 19c",
  "dbType": "ORACLE",
  "connectionType": "SERVICE_NAME",
  "host": "oracle-hr.company.com",
  "port": 1521,
  "databaseName": "HRPROD",
  "schemaName": null,
  "username": "hr_reader",
  "password": "password123",
  "connectionTimeout": 5000,
  "queryTimeout": 30000,
  "maxPoolSize": 3
}
```

**생성되는 JDBC URL:**
```
jdbc:oracle:thin:@//oracle-hr.company.com:1521/HRPROD
```

#### Oracle 11g (SID 방식)
```json
{
  "datasourceCode": "LEGACY_ORACLE",
  "datasourceName": "Legacy Oracle 11g",
  "dbType": "ORACLE",
  "connectionType": "SID",
  "host": "old-oracle.company.com",
  "port": 1521,
  "databaseName": "ORCL",
  "username": "legacy_reader",
  "password": "password123"
}
```

**생성되는 JDBC URL:**
```
jdbc:oracle:thin:@old-oracle.company.com:1521:ORCL
```

#### Oracle Multi-Schema 쿼리 예시
```sql
-- 여러 스키마에서 데이터 조인
SELECT
    e.employee_id AS value,
    e.employee_name || ' (' || d.dept_name || ')' AS label
FROM HR_SCHEMA.EMPLOYEES e
JOIN ORG_SCHEMA.DEPARTMENTS d ON e.dept_id = d.dept_id
WHERE e.status = 'ACTIVE'
ORDER BY e.employee_name
```

---

### 11.3 MSSQL 템플릿

#### SQL Server 2017+ (권장)
```json
{
  "datasourceCode": "FIN_MSSQL",
  "datasourceName": "Finance SQL Server",
  "dbType": "MSSQL",
  "host": "mssql-fin.company.com",
  "port": 1433,
  "databaseName": "FinanceDB",
  "schemaName": null,
  "username": "fin_reader",
  "password": "password123",
  "connectionTimeout": 5000,
  "queryTimeout": 30000,
  "maxPoolSize": 3
}
```

**생성되는 JDBC URL:**
```
jdbc:sqlserver://mssql-fin.company.com:1433;databaseName=FinanceDB;encrypt=false;trustServerCertificate=true
```

#### SQL Server 2012-2016 (레거시)
```json
{
  "datasourceCode": "LEGACY_MSSQL",
  "datasourceName": "Legacy SQL Server 2014",
  "dbType": "MSSQL",
  "driverVersion": "2012-2016",
  "host": "old-mssql.company.com",
  "port": 1433,
  "databaseName": "LegacyDB",
  "username": "legacy_reader",
  "password": "password123"
}
```

**생성되는 JDBC URL:**
```
jdbc:sqlserver://old-mssql.company.com:1433;databaseName=LegacyDB
```

#### MSSQL Multi-Schema 쿼리 예시
```sql
-- 동일 DB 내 여러 스키마 조인
SELECT
    c.customer_id AS value,
    c.customer_name AS label
FROM sales.Customers c
JOIN dbo.CustomerTypes ct ON c.type_id = ct.type_id
WHERE ct.is_active = 1

-- Cross-Database 조인
SELECT
    p.project_id AS value,
    p.project_name AS label
FROM ProjectDB.dbo.Projects p
JOIN HRDatabase.dbo.Employees e ON p.manager_id = e.employee_id
WHERE p.status = 'ACTIVE'
```

---

### 11.4 Tibero 템플릿

#### Tibero 6.x (권장)
```json
{
  "datasourceCode": "GOV_TIBERO",
  "datasourceName": "Government Tibero DB",
  "dbType": "TIBERO",
  "host": "tibero-gov.company.com",
  "port": 8629,
  "databaseName": "GOVDB",
  "schemaName": null,
  "username": "gov_reader",
  "password": "password123",
  "connectionTimeout": 5000,
  "queryTimeout": 30000,
  "maxPoolSize": 3
}
```

**생성되는 JDBC URL:**
```
jdbc:tibero:thin:@tibero-gov.company.com:8629:GOVDB
```

#### Tibero Multi-Schema 쿼리 예시
```sql
-- Oracle과 동일한 방식으로 스키마 지정
SELECT
    d.dept_code AS value,
    d.dept_name AS label,
    '#3B82F6' AS color
FROM ADMIN_SCHEMA.DEPARTMENTS d
JOIN ORG_SCHEMA.DEPT_HIERARCHY h ON d.dept_code = h.dept_code
WHERE d.use_yn = 'Y'
ORDER BY h.sort_order
```

---

### 11.5 URL 빌더 로직 (Java 구현 참고)

```java
public class JdbcUrlBuilder {

    public static String buildUrl(ExternalDatasource ds) {
        return switch (ds.getDbType()) {
            case MYSQL -> buildMySqlUrl(ds);
            case ORACLE -> buildOracleUrl(ds);
            case MSSQL -> buildMsSqlUrl(ds);
            case TIBERO -> buildTiberoUrl(ds);
        };
    }

    private static String buildMySqlUrl(ExternalDatasource ds) {
        return String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Seoul" +
            "&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true",
            ds.getHost(), ds.getPort(), ds.getDatabaseName()
        );
    }

    private static String buildOracleUrl(ExternalDatasource ds) {
        // Service Name 방식 (기본)
        if ("SID".equals(ds.getConnectionType())) {
            return String.format(
                "jdbc:oracle:thin:@%s:%d:%s",
                ds.getHost(), ds.getPort(), ds.getDatabaseName()
            );
        }
        return String.format(
            "jdbc:oracle:thin:@//%s:%d/%s",
            ds.getHost(), ds.getPort(), ds.getDatabaseName()
        );
    }

    private static String buildMsSqlUrl(ExternalDatasource ds) {
        return String.format(
            "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true",
            ds.getHost(), ds.getPort(), ds.getDatabaseName()
        );
    }

    private static String buildTiberoUrl(ExternalDatasource ds) {
        return String.format(
            "jdbc:tibero:thin:@%s:%d:%s",
            ds.getHost(), ds.getPort(), ds.getDatabaseName()
        );
    }
}
```

---

### 11.6 HikariCP 공통 설정

```java
public HikariDataSource createDataSource(ExternalDatasource ds, String decryptedPassword) {
    HikariConfig config = new HikariConfig();

    // 기본 설정
    config.setJdbcUrl(JdbcUrlBuilder.buildUrl(ds));
    config.setUsername(ds.getUsername());
    config.setPassword(decryptedPassword);
    config.setDriverClassName(getDriverClass(ds.getDbType()));

    // 풀 설정
    config.setMaximumPoolSize(ds.getMaxPoolSize());
    config.setMinimumIdle(1);
    config.setConnectionTimeout(ds.getConnectionTimeout());
    config.setIdleTimeout(60000);  // 1분
    config.setMaxLifetime(300000); // 5분

    // 읽기 전용 설정 (보안)
    config.setReadOnly(true);
    config.setAutoCommit(false);

    // 유효성 검사
    config.setConnectionTestQuery(getValidationQuery(ds.getDbType()));

    // 풀 이름
    config.setPoolName("External-" + ds.getDatasourceCode());

    return new HikariDataSource(config);
}

private String getDriverClass(DbType dbType) {
    return switch (dbType) {
        case MYSQL -> "com.mysql.cj.jdbc.Driver";
        case ORACLE -> "oracle.jdbc.OracleDriver";
        case MSSQL -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        case TIBERO -> "com.tmax.tibero.jdbc.TbDriver";
    };
}

private String getValidationQuery(DbType dbType) {
    return switch (dbType) {
        case MYSQL, TIBERO -> "SELECT 1";
        case ORACLE -> "SELECT 1 FROM DUAL";
        case MSSQL -> "SELECT 1";
    };
}
```

---

## 12. 쿼리 예시 (속성 옵션용)

### 12.1 부서 목록 (SELECT 옵션)
```sql
SELECT
    dept_code AS value,
    dept_name AS label,
    dept_color AS color
FROM TB_DEPARTMENT
WHERE use_yn = 'Y'
ORDER BY sort_order
```

### 12.2 사용자 목록 (담당자 선택)
```sql
SELECT
    u.username AS value,
    u.name || ' (' || d.dept_name || ')' AS label
FROM TB_USER u
LEFT JOIN TB_DEPARTMENT d ON u.dept_code = d.dept_code
WHERE u.status = 'ACTIVE'
ORDER BY u.name
```

### 12.3 코드 테이블 (공통코드)
```sql
SELECT
    code_value AS value,
    code_name AS label,
    '#' || color_hex AS color
FROM TB_COMMON_CODE
WHERE code_group = 'STATUS'
  AND use_yn = 'Y'
ORDER BY sort_order
```

### 12.4 외부 ERP 데이터
```sql
-- ERP 시스템의 프로젝트 목록
SELECT
    project_id AS value,
    project_name || ' [' || project_code || ']' AS label
FROM ERP_SCHEMA.PROJECTS
WHERE status IN ('ACTIVE', 'PLANNING')
  AND end_date >= SYSDATE
ORDER BY start_date DESC
```
