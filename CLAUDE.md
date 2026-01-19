# TaskFlow - 업무 리스트 실시간 공유 웹앱

## 프로젝트 개요

Notion 스타일의 업무 관리 시스템으로, 팀 구성원 간 업무 현황을 실시간으로 공유하고 협업할 수 있는 웹앱입니다.

### 핵심 특징
- Notion 스타일 Compact/Dense UI
- 동적 속성 시스템 (EAV 패턴)
- 다중 뷰 지원 (칸반/테이블/리스트)
- 인라인 편집 및 실시간 동기화
- 모바일/PC 반응형

---

## 기술 스택

| 구분 | 기술 | 버전/비고 |
|------|------|-----------|
| 플랫폼 | Docker Container | - |
| 데이터베이스 | MySQL | 8.0+ |
| ORM | MyBatis XML Mapper | **JPA 사용 금지** |
| 백엔드 | Spring Boot | 3.x (Java 17+) |
| 프론트엔드 | Vue.js 3 | Composition API |
| 상태관리 | Pinia | - |
| 빌드도구 | Vite | - |
| IDE | VS Code | - |

---

## 개발 규칙 (⚠️ 필수 준수)

### 설계 우선 원칙
```
1. 구현 전 반드시 설계 문서를 먼저 제시하고 승인 필수
2. 설계 승인 없이 코드 작성 금지
3. 설계 변경이 필요하면 먼저 변경안을 제시하고 승인 필수
4. DB/API/COMPONENT 설계는 승인 후 진행 필수
```

### 설계 산출물 순서
```
1단계: ERD 및 테이블 정의서 → 승인 대기
2단계: API 명세서 → 승인 대기
3단계: 컴포넌트 구조 → 승인 대기
4단계: 구현 시작

각 단계마다 "승인해주세요" 라고 항상 요청할 것
```

### 디버깅 및 오류 수정 원칙
```
⚠️ 절대 금지 사항
1. 기능 삭제로 오류 해결 금지
2. 기능 축소로 오류 해결 금지
3. 역할/책임 감소로 오류 해결 금지

✅ 필수 준수 사항
1. 오류는 근본 원인을 찾아 정상 수정할 것
2. 기능 축소/삭제가 불가피한 경우 → 반드시 승인 요청
3. 수정 전 영향 범위 분석 필수:
   - 해당 함수를 호출하는 다른 코드
   - 연관된 API 엔드포인트
   - 프론트엔드 연동 부분
   - 테스트 코드

📋 수정 시 제출 형식
- 수정 대상: [파일:라인]
- 수정 내용: [변경 사항]
- 영향 범위: [연관 기능 목록]
- 테스트 방법: [검증 절차]
```
---

## 디버깅 및 수정 원칙 (⚠️ 필수 준수)

### 기능 보존 원칙
```
⚠️ 절대 금지 사항
1. 기능 삭제로 오류 해결 금지
2. 기능 축소로 오류 해결 금지
3. 역할/책임 감소로 오류 해결 금지

✅ 필수 준수 사항
1. 오류는 근본 원인을 찾아 정상 수정할 것
2. 기능 축소/삭제가 불가피한 경우 → 반드시 승인 요청
3. 수정 전 영향 범위 분석 필수
```

### DB 스키마 일관성 원칙
```
📁 관련 파일
- docker/mysql/init/01_schema.sql  (테이블 생성)
- docker/mysql/init/02_init_data.sql  (초기 데이터)
- backend/src/main/resources/mapper/*.xml  (MyBatis Mapper)

✅ DB 수정 시 필수 체크리스트
1. 현재 운영 DB 스키마와 초기 구축 스크립트 일치 여부 확인
2. 컬럼 추가/변경 시 → 01_schema.sql 동기화
3. 코드/옵션 추가 시 → 02_init_data.sql 동기화
4. 테이블 구조 변경 시 → 관련 Mapper XML 동시 수정
5. DTO/Domain 클래스 필드 동기화

🔄 수정 순서
1. ERD/테이블 정의서 확인
2. 01_schema.sql 수정
3. 02_init_data.sql 수정 (필요시)
4. Mapper XML 수정
5. Domain/DTO 클래스 수정
6. 전체 정합성 검증
```

---

## 파일 수정 시 내용 보존 원칙 (⚠️ 최우선 규칙)

### 🚨 절대 금지 사항
```
1. 기존 내용을 임의로 삭제하거나 축소 금지
2. "정리", "간소화", "최적화" 목적의 내용 제거 금지
3. 관련 없어 보여도 완료되지 않은 항목 제거 금지
4. 수정 범위 외의 섹션 임의 변경 금지
5. 전체 파일 재작성 방식의 수정 금지 (부분 수정만 허용)
```

### ✅ 필수 준수 사항
```
1. 수정 전: 원본 파일 전체 구조 파악 (섹션 수, 라인 수)
2. 수정 중: 해당 부분만 최소 범위로 수정
3. 수정 후: 기존 내용 누락 여부 반드시 검증
4. 제거 필요 시: 반드시 목록 제시 후 승인 요청
```

### 📋 수정 시 필수 보고 형식
```
## 파일 수정 보고

### 수정 파일: [파일명]

### 변경 내역
| 구분 | 항목 |
|-----|------|
| ➕ 추가 | [새로 추가된 내용] |
| ✏️ 변경 | [변경된 내용: 전 → 후] |
| ➖ 제거 | [제거 항목] ⚠️ 승인 필요 |

### 보존 검증
- 수정 전 섹션 수: [N개]
- 수정 후 섹션 수: [M개]
- 누락 항목: [없음 / 있음]

### 제거 승인 요청 (해당 시)
- 제거 대상: [항목]
- 제거 사유: [사유]
```

### 🔴 위반 시 처리
```
- 승인 없이 내용이 제거된 경우 → 즉시 원본 복구
- 반복 위반 시 → 전체 작업 중단 후 검토
```

---

## 기능 완료 승인 및 지침 정리 규칙

### 기능 상태 정의
| 상태 | 표시 | 설명 | CLAUDE.md 지침 |
|-----|------|------|---------------|
| 진행중 | 🔵 | 개발 진행 중 | **상세 유지 (삭제 금지)** |
| 검토중 | 🟡 | 구현 완료, 검증 대기 | **상세 유지** |
| 완료 | 🟢 | 최종 승인 완료 | **요약으로 축소 가능** |
| 보류 | ⚪ | 일시 중단 | **상세 유지** |

### 지침 삭제/축소 조건
```
⚠️ 지침 삭제/축소는 오직 다음 조건에서만 가능:

1. 해당 기능이 🟢 완료 상태일 것
2. 완료 검증 체크리스트 모두 통과
3. 개발자의 명시적 완료 승인
4. 상세 내용은 docs/archive/로 아카이브

위 조건을 충족하지 않는 지침 삭제 = 규칙 위반
```

### 완료 승인 필수 체크리스트
```
□ 요구사항 100% 충족 확인
□ 테스트 통과 (단위/통합)
□ 런타임 에러 없음
□ 연관 기능 영향 없음
□ API 문서 반영 완료
□ 사용자 가이드 반영 완료
```

### 완료 승인 요청 형식
```
"[기능명] 완료 승인 요청합니다."

→ project-sync 에이전트가 검증 후 승인 요청서 생성
→ 개발자 승인 후 지침 아카이브 진행
```

### 아카이브 구조
```
docs/archive/
├── README.md           # 아카이브 인덱스 (완료된 기능 목록)
├── auth.md             # 인증 기능 상세 지침
├── user.md             # 사용자 관리 상세 지침
└── ...
```

### 아카이브 후 CLAUDE.md 잔여 형식
```markdown
### [기능명] 🟢
- 완료일: YYYY-MM-DD
- API: /api/xxx/*
- 테이블: TB_XXX
- 상세: docs/archive/xxx.md
```

### 절대 아카이브 불가 항목
```
다음 항목은 완료 여부와 관계없이 CLAUDE.md에 항상 유지:

1. 전역 개발 규칙 (설계 우선 원칙, JPA 금지 등)
2. 기술 스택 개요
3. 네이밍 컨벤션 (테이블, 클래스, API)
4. 보안 정책
5. 디버깅/수정 원칙
6. 파일 수정 시 내용 보존 원칙
7. 본 규칙 (기능 완료 승인 규칙)
```

---

## 기능 개발 현황

### 🟢 완료된 기능
| 기능 | 완료일 | 아카이브 |
|-----|--------|---------|
| | | |

### 🔵 진행중인 기능
| 기능 | 시작일 | 진행률 | 담당 지침 섹션 |
|-----|--------|--------|--------------|
| | | | |

### 🟡 검토중인 기능
| 기능 | 구현완료일 | 검증 대기 항목 |
|-----|----------|--------------|
| | | |

### ⚪ 보류된 기능
| 기능 | 보류일 | 사유 |
|-----|--------|------|
| | | |

---

## 수정 시 제출 형식 (종합)
```
📋 변경 보고서
- 수정 대상: [파일 목록]
- 변경 내용: [상세 내용]
- DB 영향: [스키마/데이터 변경 여부]
- 연관 파일: [Mapper, DTO, Service 등]
- 제거 항목: [없음 / 목록 - 승인 필요]
- 보존 검증: [섹션 수 변화, 누락 여부]
- 테스트 방법: [검증 절차]
```

---

## 지침 변경 이력 관리 규칙

### 이력 관리 대상
| 대상 | 파일 | 이력 기록 위치 |
|-----|------|--------------|
| 프로젝트 지침 | CLAUDE.md | docs/changelog/claude_md_history.md |
| 서브 에이전트 | .claude/agents/*.md | docs/changelog/agents_history.md |
| 전체 요약 | - | docs/changelog/CHANGELOG.md |

### 버전 관리 규칙
```
버전 형식: vX.Y.Z (Semantic Versioning)

X (Major): 구조 변경, 대규모 규칙 변경
Y (Minor): 기능/섹션 추가
Z (Patch): 오타 수정, 버그 수정

예시:
- v1.0.0 → v1.1.0: 새 섹션 추가
- v1.1.0 → v1.1.1: 오타 수정
- v1.1.1 → v2.0.0: 전체 구조 개편
```

### 변경 시 필수 기록 항목
```
□ 버전 번호 (vX.Y.Z)
□ 변경 일자 (YYYY-MM-DD)
□ 변경 유형 (Added/Changed/Removed/Fixed)
□ 변경 내용 상세
□ 변경 사유
□ 변경자/승인자
□ 영향 범위
```

### 변경 유형 표기
| 아이콘 | 유형 | 설명 |
|-------|------|------|
| 🆕 | Added | 새로운 기능/섹션 추가 |
| ✏️ | Changed | 기존 내용 변경 |
| 🗑️ | Removed | 기존 내용 제거 (승인 필수) |
| 🐛 | Fixed | 버그/오류 수정 |
| 📚 | Archived | 완료 기능 아카이브 |

### 이력 보존 정책
```
1. 모든 변경 이력은 영구 보존
2. 이력 파일 삭제 금지
3. 이력 내용 수정 금지 (추가만 가능)
4. Git 커밋과 연동하여 추적 가능하게 유지
```

### 이력 파일 구조
```
docs/changelog/
├── CHANGELOG.md           # 전체 변경 요약 (최신순)
├── claude_md_history.md   # CLAUDE.md 상세 이력
└── agents_history.md      # 에이전트 상세 이력
```

### 변경 이력 조회 명령
```bash
# 전체 이력 조회
cat docs/changelog/CHANGELOG.md

# CLAUDE.md 특정 버전 이력
grep -A 30 "\[v1.2.0\]" docs/changelog/claude_md_history.md

# 특정 에이전트 이력
grep -A 20 "## debugger.md" docs/changelog/agents_history.md
```
---

## 데이터베이스 컨벤션

### 테이블 명명 규칙
- 테이블명: **대문자, 스네이크케이스**
- PK: `TB명_ID` (예: `ITEM_ID`, `USER_ID`)
- 날짜: **DATE 타입** (문자열 저장 금지)

### 공통 컬럼
모든 테이블에 아래 컬럼 필수 포함:
```sql
CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
CREATED_BY VARCHAR(50) NOT NULL COMMENT '생성자 USERNAME',
UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
UPDATED_BY VARCHAR(50) NULL COMMENT '수정자 USERNAME'
```

### 외부 연동 키 정책 (⚠️ 필수 준수)
```
📌 적용 대상 테이블
- TB_USER (사용자)
- TB_DEPARTMENT (부서)
- TB_POSITION (직급)

⚠️ 핵심 원칙
외부 시스템(인사 시스템, AD 등) 연동 시 BIGINT 타입 ID의 불일치 문제를
방지하기 위해 코드형 컬럼을 FK 참조 키로 사용합니다.

✅ FK 참조 키 정책
| 테이블 | 내부 PK (사용 금지) | FK 참조 키 (사용) |
|--------|---------------------|-------------------|
| TB_USER | USER_ID (BIGINT) | USERNAME (VARCHAR) |
| TB_DEPARTMENT | DEPARTMENT_ID (BIGINT) | DEPARTMENT_CODE (VARCHAR) |
| TB_POSITION | POSITION_ID (BIGINT) | POSITION_CODE (VARCHAR) |

✅ API/쿼리 파라미터 규칙
| 잘못된 사용 | 올바른 사용 |
|------------|------------|
| userId (Long) | username (String) |
| assigneeId (Long) | assigneeUsername (String) |
| departmentId (Long) | departmentCode (String) |
| positionId (Long) | positionCode (String) |

✅ 적용 예시
- 업무 담당자 지정: assigneeUsername 사용
- 부서별 필터링: departmentCode 사용
- 공유 사용자 지정: username 사용
- 작성자/수정자 기록: CREATED_BY, UPDATED_BY에 USERNAME 저장

⚠️ 예외 사항
- JWT 토큰 내부: USER_ID 사용 가능 (내부 처리용)
- 그 외 모든 FK 참조, API 파라미터: 코드형 키 사용
```

### 핵심 테이블 구조
- 컬렉션(보드) - 아이템 관계
- 동적 속성 정의 테이블 (EAV 패턴)
- 속성 옵션(선택값) 관리
- 코드 테이블 분리
- 감사 로그 (TB_AUDIT_LOG)

### v2.0 신규 테이블
| 테이블 | 설명 |
|--------|------|
| TB_CATEGORY | 카테고리 (속성 그룹 컨테이너) |
| TB_CATEGORY_SHARE | 카테고리 공유 (USER/DEPARTMENT) |
| TB_CATEGORY_PROPERTY | 카테고리-속성 매핑 |
| TB_BOARD_CATEGORY | 보드-카테고리 매핑 |
| TB_BOARD_PROPERTY | 보드-속성 매핑 (선택된 속성) |
| TB_ITEM_PROPERTY_HISTORY | 업무 속성 변경 이력 |
| TB_ITEM_SCORE | 업무 성과 점수 |

### v2.3 신규 테이블 (Todo List)
| 테이블 | 설명 |
|--------|------|
| TB_TODO | 개인 Todo 목록 (반복, 우선순위, 이관 지원) |
| TB_TODO_SHARE | Todo 공유 (VIEW/EDIT 권한) |
| TB_ITEM_CHECKLIST | 업무 내 체크리스트 항목 |

### TB_AUDIT_LOG (감사 로그)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| LOG_ID | BIGINT | PK, AUTO_INCREMENT |
| TARGET_TYPE | VARCHAR(50) | 대상 유형 (BOARD, ITEM, BOARD_SHARE, ITEM_SHARE) |
| TARGET_ID | BIGINT | 대상 ID |
| TARGET_NAME | VARCHAR(200) | 대상 이름 (보드명, 업무명 등) |
| ACTION | VARCHAR(50) | 액션 (CREATE, UPDATE, DELETE, TRANSFER, SHARE, UNSHARE) |
| ACTOR_ID | BIGINT | 수행자 ID (FK → TB_USER) |
| DESCRIPTION | TEXT | 상세 설명 |
| BEFORE_DATA | JSON | 변경 전 데이터 |
| AFTER_DATA | JSON | 변경 후 데이터 |
| RELATED_USER_ID | BIGINT | 관련 사용자 ID (공유/이관 대상) |
| CREATED_AT | DATETIME | 생성일시 |

---

## 백엔드 컨벤션

### 패키지 구조
```
com.taskflow
├── config/           # 설정 클래스
├── controller/       # REST 컨트롤러
├── service/          # 비즈니스 로직
├── mapper/           # MyBatis Mapper 인터페이스
├── dto/              # 요청/응답 DTO
├── domain/           # 엔티티 클래스
├── common/           # 공통 유틸, 상수
└── exception/        # 예외 처리
```

### MyBatis 규칙
- XML Mapper 파일 위치: `resources/mapper/**/*.xml`
- Mapper 인터페이스에 `@Mapper` 어노테이션 사용
- SQL문은 XML에 작성 (어노테이션 SQL 지양)
- **JPA 절대 사용 금지**

### Lombok 호환성 규칙 (⚠️ 필수 준수)
```
📌 문제 상황
Lombok이 특수 명명 패턴(UserName, Username, ID, Id 등)의 필드에 대해
예상과 다른 getter/setter를 생성하여 다음 문제 발생:
- MyBatis ResultMap 매핑 실패
- Jackson JSON 직렬화 시 필드 누락
- 유사한 필드명 간의 충돌

⚠️ 위험한 패턴 (같은 클래스 내)
private String assignedByUsername;   // username (소문자 n)
private String assignedByUserName;   // UserName (대문자 N)
→ Lombok getter 충돌 위험!

✅ 필수 준수 사항
1. UserName, Username, ID, Id 등 유사 패턴 필드가 같은 클래스에 있을 때
   → 반드시 명시적 getter/setter 추가

2. Domain 클래스 (Entity)
   → MyBatis 매핑용 명시적 getter/setter 필수

3. DTO/Response 클래스
   → Jackson 직렬화용 명시적 getter 필수

📋 명시적 getter 추가 예시
// Lombok @Getter가 있어도 명시적으로 추가
public String getAssignedByUserName() {
    return assignedByUserName;
}

public String getSharedByUserName() {
    return sharedByUserName;
}

🔍 영향받는 클래스 목록
- domain/Item.java
- dto/item/ItemResponse.java
- 기타 *UserName, *Username 패턴 필드가 있는 클래스
```

### API 설계 원칙
```
# 인증
POST   /api/auth/login          # 로그인
POST   /api/auth/logout         # 로그아웃
POST   /api/auth/refresh        # 토큰 갱신

# 사용자
GET    /api/users               # 사용자 목록
POST   /api/users               # 사용자 등록
GET    /api/users/{id}          # 사용자 조회
PUT    /api/users/{id}          # 사용자 수정
DELETE /api/users/{id}          # 사용자 삭제

# 부서
GET    /api/departments              # 부서 목록 (트리 구조)
GET    /api/departments/flat         # 부서 목록 (평면 구조)
POST   /api/departments              # 부서 생성
GET    /api/departments/{id}         # 부서 조회
PUT    /api/departments/{id}         # 부서 수정
DELETE /api/departments/{id}         # 부서 삭제
PUT    /api/departments/{id}/order   # 부서 순서 변경
GET    /api/departments/{id}/users   # 부서별 사용자 목록

# 그룹
GET    /api/groups                   # 그룹 목록
POST   /api/groups                   # 그룹 생성
GET    /api/groups/{id}              # 그룹 조회
PUT    /api/groups/{id}              # 그룹 수정
DELETE /api/groups/{id}              # 그룹 삭제
PUT    /api/groups/{id}/order        # 그룹 순서 변경
GET    /api/groups/{id}/members      # 그룹 멤버 목록
POST   /api/groups/{id}/members      # 그룹 멤버 추가
DELETE /api/groups/{id}/members/{userId}  # 그룹 멤버 제거

# 보드 (컬렉션)
GET    /api/boards              # 보드 목록
POST   /api/boards              # 보드 생성
GET    /api/boards/{id}         # 보드 조회
PUT    /api/boards/{id}         # 보드 수정
DELETE /api/boards/{id}         # 보드 삭제

# 공유 사용자
GET    /api/boards/{id}/shares         # 공유 사용자 목록
POST   /api/boards/{id}/shares         # 공유 사용자 추가
DELETE /api/boards/{id}/shares/{userId} # 공유 사용자 제거

# 아이템 (업무)
GET    /api/boards/{boardId}/items           # 아이템 목록 (필터/정렬 지원)
POST   /api/boards/{boardId}/items           # 아이템 생성
GET    /api/boards/{boardId}/items/{id}      # 아이템 조회
PUT    /api/boards/{boardId}/items/{id}      # 아이템 수정
DELETE /api/boards/{boardId}/items/{id}      # 아이템 삭제
PUT    /api/boards/{boardId}/items/{id}/complete  # 완료 처리
PUT    /api/boards/{boardId}/items/{id}/restore   # 복원 처리

# 댓글
GET    /api/items/{itemId}/comments     # 댓글 목록
POST   /api/items/{itemId}/comments     # 댓글 등록
PUT    /api/comments/{id}               # 댓글 수정
DELETE /api/comments/{id}               # 댓글 삭제

# 속성 정의 (기존 - 보드 귀속)
GET    /api/boards/{boardId}/properties      # 속성 정의 목록
POST   /api/boards/{boardId}/properties      # 속성 정의 생성
PUT    /api/properties/{id}                  # 속성 정의 수정
DELETE /api/properties/{id}                  # 속성 정의 삭제

# 글로벌/매니저 속성 (v2.0)
GET    /api/properties/global              # 글로벌 속성 목록
POST   /api/properties/global              # 글로벌 속성 생성 (ADMIN)
PUT    /api/properties/global/{id}         # 글로벌 속성 수정
DELETE /api/properties/global/{id}         # 글로벌 속성 삭제
GET    /api/properties/manager             # 매니저 속성 목록
POST   /api/properties/manager             # 매니저 속성 생성 (MANAGER)
PUT    /api/properties/manager/{id}        # 매니저 속성 수정
DELETE /api/properties/manager/{id}        # 매니저 속성 삭제
GET    /api/properties/available           # 사용 가능한 전체 속성 조회

# 카테고리 (v2.0)
GET    /api/categories                     # 카테고리 목록
POST   /api/categories                     # 카테고리 생성
GET    /api/categories/{id}                # 카테고리 상세
PUT    /api/categories/{id}                # 카테고리 수정
DELETE /api/categories/{id}                # 카테고리 삭제
GET    /api/categories/{id}/properties     # 카테고리 속성 목록
POST   /api/categories/{id}/properties     # 카테고리에 속성 추가
DELETE /api/categories/{id}/properties/{propId}  # 카테고리에서 속성 제거
GET    /api/categories/{id}/shares         # 카테고리 공유 목록
POST   /api/categories/{id}/shares         # 카테고리 공유 추가
DELETE /api/categories/{id}/shares/{shareId}  # 카테고리 공유 해제

# 성과 점수 (v2.0)
GET    /api/items/{itemId}/score          # 업무 성과 점수 조회
POST   /api/items/{itemId}/score/calculate  # 성과 점수 계산
PUT    /api/items/{itemId}/score/weights   # 가중치 수정
POST   /api/items/{itemId}/score/approve   # 승인 처리 (PM)

# 속성 옵션 (코드 항목)
GET    /api/properties/{propId}/options      # 옵션 목록
POST   /api/properties/{propId}/options      # 옵션 추가
PUT    /api/options/{id}                     # 옵션 수정
DELETE /api/options/{id}                     # 옵션 삭제

# 작업 템플릿 (작업 등록 메뉴용)
GET    /api/task-templates              # 템플릿 목록
POST   /api/task-templates              # 템플릿 등록
PUT    /api/task-templates/{id}         # 템플릿 수정
DELETE /api/task-templates/{id}         # 템플릿 삭제
GET    /api/task-templates/search       # 템플릿 검색 (자동완성용)

# 이력
GET    /api/history/items               # 작업 처리 이력
GET    /api/history/templates           # 작업 등록 이력

# 감사 로그 (관리 이력)
GET    /api/audit-logs                  # 관리 이력 목록 (페이징)
GET    /api/audit-logs/recent           # 최근 관리 이력

# SSE (실시간 동기화)
GET    /api/sse/subscribe               # SSE 연결

# Todo List (v2.3)
GET    /api/todos                       # 내 Todo 목록
GET    /api/todos/today                 # 오늘 마감 Todo
GET    /api/todos/overdue               # 지연 Todo
GET    /api/todos/completed             # 완료 Todo
GET    /api/todos/shared                # 공유받은 Todo
GET    /api/todos/transferred           # 이관받은 Todo
POST   /api/todos                       # Todo 생성
GET    /api/todos/{id}                  # Todo 상세
PUT    /api/todos/{id}                  # Todo 수정
DELETE /api/todos/{id}                  # Todo 삭제
PUT    /api/todos/{id}/complete         # 완료 토글
PUT    /api/todos/reorder               # 순서 변경

# Todo 공유 (v2.3)
GET    /api/todos/{todoId}/shares       # 공유 목록
POST   /api/todos/{todoId}/shares       # 공유 추가
PUT    /api/todo-shares/{shareId}       # 공유 권한 수정
DELETE /api/todo-shares/{shareId}       # 공유 해제

# Todo 이관 (v2.3)
POST   /api/todos/{id}/transfer         # Todo 이관
POST   /api/todos/transfer-all          # 일괄 이관 (사용자 삭제용)
DELETE /api/todos/user/{userId}         # 사용자 Todo 전체 삭제

# 업무 체크리스트 (v2.3)
GET    /api/items/{itemId}/checklists           # 체크리스트 목록
GET    /api/items/{itemId}/checklists/progress  # 진행률 조회
POST   /api/items/{itemId}/checklists           # 체크리스트 추가
PUT    /api/checklists/{id}                     # 체크리스트 수정
PUT    /api/checklists/{id}/complete            # 완료 토글
DELETE /api/checklists/{id}                     # 체크리스트 삭제
PUT    /api/items/{itemId}/checklists/reorder   # 순서 변경
GET    /api/checklists/my                       # 내 담당 체크리스트
```

### 쿼리 파라미터 (목록 조회)
```
# 페이징
?page=0&size=20

# 정렬
?sort=createdAt,desc

# 필터 (⚠️ 외부 연동 키 정책 적용)
?status=IN_PROGRESS
?priority=HIGH
?assigneeUsername=admin        # 담당자 (USERNAME 사용)
?groupId=1
?departmentCode=DEV           # 부서 (DEPARTMENT_CODE 사용)
?startDate=2024-01-01&endDate=2024-12-31

# 검색
?keyword=검색어

# 완료/삭제 포함 여부
?includeCompleted=false
?includeDeleted=false

# 사용 여부 (부서/그룹 조회 시)
?useYn=Y
```

### 응답 형식
```java
// 성공 응답
{
  "success": true,
  "data": { ... },
  "message": null
}

// 실패 응답
{
  "success": false,
  "data": null,
  "message": "에러 메시지"
}
```

---

## 프론트엔드 컨벤션

### 디렉토리 구조
```
src/
├── assets/           # 정적 파일
├── components/       # 공통 컴포넌트
│   ├── common/       # 버튼, 인풋 등
│   ├── layout/       # 레이아웃 컴포넌트
│   └── ui/           # UI 컴포넌트
├── composables/      # Composition API 훅
├── router/           # Vue Router 설정
├── stores/           # Pinia 스토어
├── views/            # 페이지 컴포넌트
├── api/              # API 호출 모듈
├── types/            # TypeScript 타입 정의
└── utils/            # 유틸리티 함수
```

### Composition API 패턴
```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useItemStore } from '@/stores/item'

// Props
const props = defineProps<{
  itemId: number
}>()

// Emits
const emit = defineEmits<{
  (e: 'update', value: Item): void
}>()

// Store
const itemStore = useItemStore()

// State
const loading = ref(false)

// Computed
const item = computed(() => itemStore.getItemById(props.itemId))

// Methods
async function fetchItem() {
  loading.value = true
  await itemStore.fetchItem(props.itemId)
  loading.value = false
}

// Lifecycle
onMounted(() => {
  fetchItem()
})
</script>
```

### 상태 관리 (Pinia)
```typescript
// stores/property.ts
export const usePropertyStore = defineStore('property', () => {
  // 속성 정의는 앱 초기화 시 로드하여 Store에 보관
  const propertyDefinitions = ref<PropertyDefinition[]>([])
  
  // 속성 변경 시 Store 갱신 후 API 호출 (Optimistic Update)
  async function updateProperty(id: number, data: Partial<PropertyDefinition>) {
    // 1. Store 먼저 갱신
    const index = propertyDefinitions.value.findIndex(p => p.id === id)
    if (index !== -1) {
      propertyDefinitions.value[index] = { ...propertyDefinitions.value[index], ...data }
    }
    
    // 2. API 호출
    try {
      await api.updateProperty(id, data)
    } catch (error) {
      // 실패 시 롤백
      await fetchProperties()
    }
  }
  
  return { propertyDefinitions, updateProperty }
})
```

---

## UI/UX 설계 조건

### Compact/Dense UI 스펙
| 항목 | 스펙 |
|------|------|
| Row Height | 36px 이하 |
| 컴포넌트 간 Spacing | 8px |
| 폰트 사이즈 | 13~14px |
| 화면 표시 항목 | 최소 15개 이상 |

### 레이아웃 구조
```
┌─────────────────────────────────────────────────────────┐
│                      Header                              │
├──────────┬──────────────────────────────────────────────┤
│          │                                               │
│  Sidebar │              Main Panel                       │
│  (Nav)   │        (칸반/테이블/리스트 뷰)                  │
│          │                                               │
│          │                          ┌──────────────────┐ │
│          │                          │   Slide-over    │ │
│          │                          │   Panel         │ │
│          │                          │   (상세/편집)    │ │
│          │                          └──────────────────┘ │
└──────────┴──────────────────────────────────────────────┘
```

### 인터랙션 패턴
- 아이템 클릭 → 슬라이드오버 패널 오픈
- 컨텍스트 메뉴(⋮) → 수정/삭제/복제
- 패널 외부 클릭 또는 ESC → 패널 닫기
- 속성값 클릭 → 즉시 편집 모드
- 선택형 속성 → 드롭다운 내 신규 옵션 추가 가능

### 반응형 처리
```
Mobile (< 768px):
- 사이드바 숨김 (햄버거 메뉴)
- 아이템 선택 시 전체 화면 편집 페이지

PC (>= 768px):
- 사이드바 표시
- 아이템 선택 시 우측 슬라이드오버 패널
```

---

## 동적 속성 시스템

### 속성 소유 유형 (v2.0)
| 유형 | 생성 권한 | 적용 범위 | 삭제 시 동작 |
|------|----------|----------|-------------|
| **기본 속성** | 시스템 | 전체 (필수) | 삭제 불가 |
| **글로벌 속성** | ADMIN | 전체 사용자 | 기존 업무 값 유지 |
| **매니저 속성** | MANAGER | 본인+하위부서 | 기존 업무 값 유지 |
| **사용자 속성** | USER | 본인 (카테고리 그룹화) | 기존 업무 값 유지 |

> 사용자 속성은 카테고리에 그룹화되어 보드/업무에 활용됩니다.

### 속성 타입
| 타입 | 설명 | 저장 방식 |
|------|------|-----------|
| TEXT | 텍스트 | VARCHAR |
| NUMBER | 숫자 | DECIMAL |
| DATE | 날짜 | DATE |
| SELECT | 단일선택 | FK (옵션 ID) |
| MULTI_SELECT | 다중선택 | 별도 매핑 테이블 |
| CHECKBOX | 체크박스 | BOOLEAN |
| USER | 사용자 | FK (사용자 ID) |

### 대표 속성 (기본 제공)
| 속성명 | 타입 | 기본 옵션 |
|--------|------|-----------|
| 카테고리 | SELECT | 사용자 정의 |
| 상태 | SELECT | 시작전, 진행중, 완료, 삭제 |
| 우선순위 | SELECT | 긴급, 높음, 보통, 낮음 |
| 그룹 | SELECT | 사용자 정의 |

### 속성 관리 UI (컬럼 헤더)

```
[컬럼 헤더 클릭 시 드롭다운 메뉴]
┌─────────────────────┐
│ 🔤 속성 이름 변경    │
│ 📝 속성 타입 변경    │
│ ⬅️ 왼쪽으로 이동     │
│ ➡️ 오른쪽으로 이동   │
│ 🙈 숨기기           │
│ ───────────────     │
│ ➕ 새 속성 추가      │
│ 🗑️ 속성 삭제        │
└─────────────────────┘
```

| 기능 | 설명 |
|------|------|
| 속성 이름 변경 | 인라인 텍스트 편집 |
| 속성 타입 변경 | 타입 선택 드롭다운 (데이터 마이그레이션 경고) |
| 순서 변경 | 드래그 앤 드롭 또는 메뉴에서 이동 |
| 숨기기 | 목록에서 컬럼 숨김 (데이터 유지) |
| 새 속성 추가 | 타입 선택 → 이름 입력 |
| 속성 삭제 | 확인 다이얼로그 후 삭제 |

### 코드 항목 관리 (SELECT/MULTI_SELECT 옵션)

#### 인라인 추가
```
[선택형 속성 드롭다운]
┌─────────────────────┐
│ 🔍 검색...          │
├─────────────────────┤
│ ○ 긴급              │
│ ○ 높음              │
│ ○ 보통              │
│ ● 낮음 ✓           │
├─────────────────────┤
│ ➕ "새 옵션" 추가    │  ← 검색어가 없을 때 새 옵션 추가
└─────────────────────┘
```

#### 설정 메뉴 통합 관리
```
[설정 > 코드 관리]
┌─────────────────────────────────────────────┐
│ 코드 관리                                    │
├──────────┬──────────────────────────────────┤
│          │  [상태]                          │
│ 카테고리  │  ┌────────────────────────────┐ │
│ 상태     │  │ 시작전  [색상] [↑] [↓] [🗑] │ │
│ 우선순위  │  │ 진행중  [색상] [↑] [↓] [🗑] │ │
│ 그룹     │  │ 완료    [색상] [↑] [↓] [🗑] │ │
│          │  │ 삭제    [색상] [↑] [↓] [🗑] │ │
│          │  ├────────────────────────────┤ │
│          │  │ ➕ 새 옵션 추가             │ │
│          │  └────────────────────────────┘ │
└──────────┴──────────────────────────────────┘
```

| 기능 | 설명 |
|------|------|
| 옵션 추가 | 새 선택 옵션 생성 |
| 옵션 수정 | 이름, 색상 변경 |
| 옵션 삭제 | 사용 중인 경우 경고 표시 |
| 순서 변경 | 드래그 앤 드롭 또는 화살표 버튼 |
| 색상 지정 | 칸반 뷰, 태그 표시용 색상 |

### 캐시 전략
```
1. 앱 초기화 시 모든 속성 정의 로드
2. Store에 캐시
3. 속성 정의 변경 시:
   - Store 즉시 갱신 (Optimistic Update)
   - API 호출
   - 실패 시 롤백
4. 아이템 조회 시 속성값만 반환 (정의는 캐시 참조)
```

### 캐시 무효화 시점
| 이벤트 | 동작 |
|--------|------|
| 속성 정의 추가 | 전체 속성 정의 재로드 |
| 속성 정의 수정 | 해당 속성만 갱신 |
| 속성 정의 삭제 | 전체 속성 정의 재로드 |
| 옵션 추가/수정/삭제 | 해당 속성의 옵션 목록 갱신 |

---

## 뷰 시스템

### 뷰 타입별 특징
| 뷰 타입 | 설명 | 적합한 용도 |
|---------|------|-------------|
| 테이블 뷰 | 스프레드시트 형태 | 다중 속성 비교, 대량 데이터 |
| 칸반 뷰 | 컬럼별 카드 배치 | 상태 기반 워크플로우 |
| 리스트 뷰 | 단순 목록 형태 | 빠른 스캔, 모바일 최적화 |

### 뷰 전환
```
┌─────────────────────────────────────┐
│ 업무 목록          [테이블▼] [필터] │
├─────────────────────────────────────┤
│  ☰ 테이블                          │
│  ▦ 칸반                            │
│  ≡ 리스트                          │
└─────────────────────────────────────┘
```

### 칸반 뷰 상세
```
[상태 기준 칸반]
┌──────────┬──────────┬──────────┬──────────┐
│  시작전   │  진행중   │   완료    │   삭제   │
│   (3)    │   (5)    │   (12)   │   (2)   │
├──────────┼──────────┼──────────┼──────────┤
│ ┌──────┐ │ ┌──────┐ │ ┌──────┐ │          │
│ │ Task │ │ │ Task │ │ │ Task │ │          │
│ │  1   │ │ │  2   │ │ │  4   │ │          │
│ └──────┘ │ └──────┘ │ └──────┘ │          │
│ ┌──────┐ │ ┌──────┐ │          │          │
│ │ Task │ │ │ Task │ │          │          │
│ │  3   │ │ │  5   │ │          │          │
│ └──────┘ │ └──────┘ │          │          │
└──────────┴──────────┴──────────┴──────────┘

- 카드 드래그 앤 드롭으로 상태 변경
- 그룹 기준 변경 가능 (상태, 우선순위, 담당자 등)
```

---

## 메뉴 구조

```
┌─────────────────────────────────────────┐
│  TaskFlow                               │
├─────────────────────────────────────────┤
│  📋 업무 페이지          ← 메인 화면     │
│  ✅ 완료 작업 메뉴                       │
│  🗑️ 삭제된 작업                         │
│  ─────────────────────                  │
│  ☑️ Todo List            ← v2.3 신규    │
│  ─────────────────────                  │
│  📝 작업 등록 메뉴                       │
│  📊 이력관리 메뉴                        │
│  ─────────────────────                  │
│  👥 그룹 관리 메뉴                       │
│  📁 보드 관리 메뉴                       │
│  ─────────────────────                  │
│  ⚙️ 설정                                │
└─────────────────────────────────────────┘
```

---

## 주요 기능 명세

### 1. 로그인 페이지
| 항목 | 설명 |
|------|------|
| 아이디 입력 | 텍스트 필드 |
| 패스워드 입력 | 패스워드 필드 |
| 로그인 버튼 | 인증 후 업무 페이지로 이동 |
| 인증 방식 | JWT 토큰 기반 |
| 토큰 저장 | localStorage (Access Token), httpOnly Cookie (Refresh Token) |

### 2. 업무 페이지 (메인)

#### 2-1. 신규 업무 등록 창
| 구성요소 | 설명 |
|----------|------|
| 업무내용 입력 | 텍스트 필드 |
| 자동완성 | 직접 입력 시 기존 등록된 작업 내역 자동 검색 |
| 작업 선택 | 드롭다운에서 기존 등록된 작업 선택 가능 |
| 신규등록 버튼 | 업무 목록에 추가 |

```
[자동완성 동작 상세]
1. 사용자가 2글자 이상 입력 시 검색 시작
2. 기존 작업 등록 메뉴의 작업 내역에서 검색
3. 검색 결과를 드롭다운으로 표시 (최대 10개)
4. 선택 시 입력 필드에 자동 채움
5. 디바운스 적용 (300ms)
```

#### 2-2. 현재 업무 목록 창
| 컬럼 | 설명 | 인라인 편집 |
|------|------|-------------|
| 업무내용 | 작업 제목/내용 | ✅ |
| 요청일 | 업무 요청 일자 | ✅ (DatePicker) |
| 마감일 | 업무 마감 일자 | ✅ (DatePicker) |
| 담당자 | 작업 담당자 | ✅ (사용자 선택) |
| 카테고리 | 분류 | ✅ (단일선택) |
| 상태 | 시작전/진행중/완료/삭제 | ✅ (단일선택) |
| 우선순위 | 긴급/높음/보통/낮음 | ✅ (단일선택) |
| 그룹 | 그룹 분류 | ✅ (단일선택) |
| 리플 | 댓글 수 표시, 클릭 시 댓글 패널 | - |
| 완료 버튼 | 상태를 '완료'로 변경 | - |
| 삭제 버튼 | 상태를 '삭제'로 변경 | - |

```
[모바일/PC 동작 차이]
Mobile (< 768px):
  - 업무 행 클릭 → 전체 화면 편집 페이지로 이동
  - 뒤로가기 버튼으로 목록 복귀
  - 하단 고정 버튼: 완료, 삭제, 저장

PC (>= 768px):
  - 업무 행 클릭 → 우측 슬라이드오버 패널 오픈
  - 패널 내에서 모든 속성 편집 가능
  - ESC 또는 외부 클릭으로 패널 닫기
```

#### 2-3. 업무 상세/편집 (슬라이드오버 패널)
| 구성요소 | 설명 |
|----------|------|
| 업무 내용 | 리치 텍스트 편집 가능 |
| 속성 영역 | 모든 속성 인라인 편집 |
| 수정자 표시 | 최종 수정자 이름, 수정 시간 표시 |
| 댓글 영역 | 의견 첨부 기능 (리플) |
| 댓글 입력 | 텍스트 입력 + 등록 버튼 |
| 댓글 목록 | 작성자, 작성시간, 내용 표시 |

#### 2-4. 완료/삭제 업무 Hidden 처리
```
[Hidden 규칙 - 당일 처리 기준]
1. 완료 또는 삭제된 업무는 현재 목록에서 Hidden 처리
2. "당일" 기준: 오늘 날짜에 완료/삭제된 항목만 해당
3. 업무 페이지 맨 하단에 축소 상태로 표시
4. "완료된 업무 (N건)" 형태로 표시
5. 클릭 시 확장되어 완료/삭제 목록 표시
6. 다시 클릭 시 축소

[표시 정보]
- 작업 내용
- 작업 결과 (완료/삭제)
- 처리 시간
```

### 3. 완료 작업 메뉴
| 컬럼 | 설명 |
|------|------|
| 등록시간 | 업무 최초 등록 시간 |
| 완료시간 | 업무 완료 처리 시간 |
| 작업 내용 | 업무 제목/내용 |
| 작업자 | 완료 처리한 담당자 |

```
[필터/정렬]
- 기간 필터: 오늘, 이번주, 이번달, 사용자 지정
- 정렬: 완료시간 내림차순 (기본)
```

### 4. 작업 등록 메뉴
| 구성요소 | 설명 |
|----------|------|
| 작업 내용 | 자주 사용하는 작업 템플릿 등록 |
| 등록 버튼 | 신규 작업 템플릿 등록 |
| 변경 버튼 | 기존 작업 선택 시 등록→변경 버튼으로 전환 |
| 삭제 버튼 | 선택된 작업 템플릿 삭제 |

```
[버튼 전환 로직]
1. 초기 상태: [등록] 버튼 표시
2. 목록에서 기존 작업 선택 시: [변경] 버튼으로 전환
3. 입력 필드 클리어 또는 새 입력 시: [등록] 버튼으로 복귀
```

### 5. 이력관리 메뉴

#### 5-1. 스위치 기능
```
┌───────────────────────────────────────────────────────┐
│  [전체 이력] | 작업 처리 이력 | 작업 등록 이력 | 관리 이력 │  ← 토글 스위치
├───────────────────────────────────────────────────────┤
│  (선택된 이력 목록 표시)                               │
└───────────────────────────────────────────────────────┘
```

#### 5-2. 작업 처리 이력 컬럼
| 컬럼 | 타입 | 설명 |
|------|------|------|
| 작업 내용 | TEXT | 업무 제목 |
| 작업 결과 | SELECT | 완료/삭제 |
| 작업자 | USER | 처리한 담당자 |
| 등록시간 | DATETIME | 최초 등록 시간 |
| 요청일 | DATE | 업무 요청 일자 |
| 마감일 | DATE | 업무 마감 일자 |
| 완료시간 | DATETIME | 완료 처리 시간 |
| 수정시간 | DATETIME | 마지막 수정 시간 |
| 삭제시간 | DATETIME | 삭제 처리 시간 (삭제된 경우) |

#### 5-3. 작업 등록 이력 컬럼
| 컬럼 | 타입 | 설명 |
|------|------|------|
| 작업 내용 | TEXT | 등록된 작업 템플릿 |
| 등록자 | USER | 템플릿 등록한 사용자 |
| 등록시간 | DATETIME | 템플릿 등록 시간 |
| 수정시간 | DATETIME | 마지막 수정 시간 |
| 상태 | SELECT | 활성/비활성 |

#### 5-4. 관리 이력 컬럼 (감사 로그)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| 대상유형 | SELECT | BOARD(보드), ITEM(업무), BOARD_SHARE(보드 공유), ITEM_SHARE(업무 공유) |
| 액션 | SELECT | CREATE(생성), UPDATE(수정), DELETE(삭제), TRANSFER(이관), SHARE(공유), UNSHARE(공유해제) |
| 대상 | TEXT | 대상 이름 (보드명, 업무명 등) |
| 내용 | TEXT | 상세 설명 |
| 수행자 | USER | 작업 수행자 |
| 관련사용자 | USER | 공유/이관 대상 사용자 |
| 일시 | DATETIME | 발생 시간 |

```
[기록되는 이벤트]
- 보드 생성/수정/삭제
- 보드 공유 추가/해제/권한변경
- 업무 이관
- 업무 공유 추가/해제
```

### 6. 사용자 등록 메뉴
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| 사용자 이름 | TEXT | ✅ | 표시명 |
| 아이디 | TEXT | ✅ | 로그인 ID (영문+숫자, 4~20자) |
| 패스워드 | PASSWORD | ✅ | 최소 8자, 영문+숫자+특수문자 |
| 패스워드 확인 | PASSWORD | ✅ | 패스워드 재입력 |

```
[비밀번호 정책]
- 최소 8자 이상
- 영문 대/소문자 포함
- 숫자 포함
- 특수문자 포함 (!@#$%^&*)
- BCrypt 암호화 저장
```

### 7. 공유 사용자 등록 메뉴
| 필드 | 설명 |
|------|------|
| 사용자 아이디 | 공유할 사용자 ID 검색/선택 |
| 사용자 이름 | 자동 표시 |
| 등록 버튼 | 공유 사용자로 추가 |
| 삭제 버튼 | 공유 목록에서 제거 |

```
[공유 범위]
- 공유된 사용자는 해당 보드의 업무를 조회/수정 가능
- 보드 소유자만 공유 사용자 관리 가능
```

### 8. 부서 관리 메뉴
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| 부서 코드 | TEXT | ✅ | 고유 부서 코드 (예: DEV, HR, MKT) |
| 부서명 | TEXT | ✅ | 부서 이름 |
| 상위 부서 | SELECT | - | 상위 부서 선택 (계층 구조) |
| 정렬 순서 | NUMBER | - | 표시 순서 |
| 사용 여부 | CHECKBOX | ✅ | 활성/비활성 |

```
[부서 계층 구조]
- 상위 부서를 선택하여 트리 구조 구성 가능
- 최상위 부서는 상위 부서가 NULL

[부서 트리 예시]
├── 경영지원본부
│   ├── 인사팀
│   ├── 총무팀
│   └── 재무팀
├── 개발본부
│   ├── 개발1팀
│   ├── 개발2팀
│   └── QA팀
└── 영업본부
    ├── 국내영업팀
    └── 해외영업팀

[기능]
- 부서 CRUD
- 드래그 앤 드롭으로 순서 변경
- 부서 비활성화 시 하위 부서도 함께 비활성화 경고
- 사용자 등록 시 부서 선택 가능
```

### 9. 그룹 관리 메뉴
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| 그룹 코드 | TEXT | ✅ | 고유 그룹 코드 |
| 그룹명 | TEXT | ✅ | 그룹 이름 |
| 그룹 설명 | TEXT | - | 그룹 용도 설명 |
| 그룹 색상 | COLOR | - | 표시용 색상 |
| 정렬 순서 | NUMBER | - | 표시 순서 |
| 사용 여부 | CHECKBOX | ✅ | 활성/비활성 |

```
[그룹 vs 부서 차이]
- 부서: 조직도 기반의 고정 소속 (1인 1부서)
- 그룹: 프로젝트/업무 기반의 유연한 팀 (1인 다중 그룹 가능)

[그룹 활용 예시]
- 프로젝트 그룹: "신규 서비스 개발", "리뉴얼 프로젝트"
- 업무 그룹: "주간 보고", "월간 회의"
- TF 그룹: "보안 점검 TF", "비용 절감 TF"

[기능]
- 그룹 CRUD
- 그룹 멤버 관리 (사용자 추가/제거)
- 업무 아이템에 그룹 할당
- 그룹별 업무 필터링
```

### 부서/그룹과 사용자 연결
```
[사용자-부서 관계]
- 1:1 관계 (사용자는 하나의 부서에만 소속)
- TB_USER.DEPARTMENT_ID로 연결

[사용자-그룹 관계]
- N:M 관계 (사용자는 여러 그룹에 소속 가능)
- TB_USER_GROUP 매핑 테이블로 연결

[업무-그룹 관계]
- N:1 관계 (업무는 하나의 그룹에 할당)
- TB_ITEM.GROUP_ID로 연결
- 업무 속성의 '그룹' 필드와 연동
```

### 10. Todo List 메뉴 (v2.3)

#### 10-1. 개요
업무(Item)와 독립적인 개인 할 일 목록 관리 기능

| 구분 | 설명 |
|------|------|
| 목적 | 개인 단위 할 일 관리 (업무와 별도) |
| 특징 | 반복 설정, 우선순위, 마감일/시간, 공유, 이관 지원 |

#### 10-2. Todo 필드
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| 제목 | TEXT | ✅ | Todo 제목 (최대 500자) |
| 내용 | TEXT | - | 상세 설명 |
| 마감일 | DATE | - | 마감 날짜 |
| 마감시간 | TIME | - | 마감 시간 |
| 우선순위 | SELECT | ✅ | URGENT/HIGH/NORMAL/LOW |
| 반복 | SELECT | ✅ | NONE/DAILY/WEEKLY/MONTHLY/YEARLY |
| 완료 여부 | CHECKBOX | ✅ | 완료 체크 |

#### 10-3. 우선순위
| 값 | 표시 | 색상 |
|----|------|------|
| URGENT | 긴급 | 빨강 |
| HIGH | 높음 | 주황 |
| NORMAL | 보통 | 파랑 |
| LOW | 낮음 | 회색 |

#### 10-4. 반복 설정
| 값 | 설명 |
|----|------|
| NONE | 반복 없음 |
| DAILY | 매일 |
| WEEKLY | 매주 |
| MONTHLY | 매월 |
| YEARLY | 매년 |

```
[반복 동작]
1. 반복이 설정된 Todo 완료 시
2. 다음 마감일로 새 Todo 자동 생성
3. 완료된 Todo는 완료 목록으로 이동
```

#### 10-5. 공유 기능
| 권한 | 설명 |
|------|------|
| VIEW | 조회만 가능 |
| EDIT | 조회 + 수정 가능 |

#### 10-6. 이관 기능
```
[이관 시나리오]
1. Todo 단건 이관: 다른 사용자에게 소유권 이전
2. 사용자 삭제 시: 일괄 이관 또는 삭제 선택 모달 표시

[이관 시 기록]
- TRANSFERRED_FROM_USER_ID: 이전 소유자
- TRANSFERRED_AT: 이관 일시
```

#### 10-7. UI 탭 구조
```
┌───────────────────────────────────────────────────┐
│  [오늘] | 전체 | 공유받은 Todo | 완료              │  ← 탭 전환
├───────────────────────────────────────────────────┤
│  (선택된 탭의 Todo 목록 표시)                       │
└───────────────────────────────────────────────────┘
```

### 11. 업무 체크리스트 (v2.3)

업무(Item) 상세 패널 내 체크리스트 기능

| 구성요소 | 설명 |
|----------|------|
| 체크박스 | 완료 여부 토글 |
| 내용 | 체크리스트 항목 텍스트 |
| 담당자 | 체크리스트별 담당자 지정 (선택) |
| 진행률 | 완료/전체 수 표시 (예: 3/5) |

```
[체크리스트 예시]
┌─────────────────────────────────────┐
│ 체크리스트 (3/5)                     │
├─────────────────────────────────────┤
│ ☑ 요구사항 분석 완료        @홍길동  │
│ ☑ 설계 문서 작성           @김철수  │
│ ☑ 개발 완료               @이영희  │
│ ☐ 테스트 진행 중           @박민수  │
│ ☐ 배포 준비               @최지은  │
├─────────────────────────────────────┤
│ + 항목 추가                         │
└─────────────────────────────────────┘
```

### 12. 캘린더 (Phase 5)

#### 12-1. 개요
개인/팀 일정 및 이벤트 관리 기능. 음력 날짜 지원 및 반복 일정 설정 포함.

| 구분 | 설명 |
|------|------|
| 목적 | 개인/팀 일정 관리, 업무와 연계된 스케줄 관리 |
| 특징 | 양력/음력 전환, 반복 일정, 종일 이벤트, 공유 |

#### 12-2. 이벤트 필드
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| 제목 | TEXT | ✅ | 이벤트 제목 |
| 내용 | TEXT | - | 상세 설명 |
| 시작일 | DATE | ✅ | 시작 날짜 |
| 종료일 | DATE | - | 종료 날짜 |
| 시작시간 | TIME | - | 시작 시간 |
| 종료시간 | TIME | - | 종료 시간 |
| 종일 여부 | CHECKBOX | ✅ | 종일 이벤트 여부 |
| 음력 여부 | CHECKBOX | ✅ | 음력 날짜 사용 여부 |
| 반복 설정 | SELECT | - | 반복 유형 |
| 장소 | TEXT | - | 이벤트 장소 |
| 캘린더 | SELECT | ✅ | 소속 캘린더 |

#### 12-3. 음력 변환 기능

**라이브러리**: `korean-lunar-calendar` (npm)

```
[지원 범위]
- 양력: 1000-01-01 ~ 2050-12-31
- 음력: 1000-01-01 ~ 2050-11-18

[API 사용법]
const calendar = new KoreanLunarCalendar()

// 양력 → 음력 변환
calendar.setSolarDate(year, month, day)  // 반환: boolean (유효성)
const lunar = calendar.getLunarCalendar()
// lunar: { year, month, day, intercalation }

// 음력 → 양력 변환
calendar.setLunarDate(year, month, day, isLeapMonth)  // 반환: boolean (유효성)
const solar = calendar.getSolarCalendar()
// solar: { year, month, day }
```

**프론트엔드 유틸리티**: `frontend/src/utils/lunar.ts`
| 함수 | 설명 |
|------|------|
| `solarToLunar(solarDate)` | 양력 → 음력 변환 |
| `lunarToSolar(year, month, day, isLeapMonth)` | 음력 → 양력 변환 |
| `isValidLunarDate(year, month, day, isLeapMonth)` | 음력 날짜 유효성 검증 |
| `getLunarMonthDays(year, month, isLeapMonth)` | 음력 월 일수 조회 (29 또는 30) |
| `hasLeapMonth(year, month)` | 해당 연도/월에 윤달 존재 여부 |
| `precomputeMonthLunarDates(year, month)` | 월간 뷰용 음력 데이터 사전 계산 |

#### 12-4. 음력 기념일 버튼

음력 날짜 선택 시 자주 사용하는 음력 기념일을 빠르게 선택할 수 있는 버튼 제공.

```
[음력 기념일 목록]
┌─────────────────────────────────────┐
│ 🎉 설날 (1월 1일)                    │
│ 🌕 정월대보름 (1월 15일)              │
│ 🎋 단오 (5월 5일)                    │
│ 🌌 칠석 (7월 7일)                    │
│ 🌙 백중 (7월 15일)                   │
│ 🎑 추석 (8월 15일)                   │
│ 🍂 중양절 (9월 9일)                  │
└─────────────────────────────────────┘

[동작]
1. 버튼 클릭 시 해당 음력 월/일 자동 선택
2. 현재 선택된 연도의 음력 날짜로 설정
3. 양력 변환 결과 즉시 표시
```

#### 12-5. 윤달 체크박스

음력 달력에서 윤달(閏月, 윤달)을 선택하기 위한 체크박스.

```
[윤달이란?]
- 음력은 1년이 약 354일로 양력보다 11일 짧음
- 이 차이를 보정하기 위해 약 19년에 7번 윤달 삽입
- 윤달은 해당 월을 한 번 더 반복 (예: 윤4월)

[동작]
1. 특정 연도에 윤달이 있는 월만 체크박스 활성화
2. 체크 시 "윤N월"로 표시 (예: 윤4월)
3. 윤달이 없는 연도/월은 체크박스 비활성화

[예시]
- 2025년: 윤6월 있음
- 2023년: 윤2월 있음
- 일반적으로 모든 월에 매년 윤달이 있는 것은 아님
```

**윤달 검증 함수**:
```typescript
// 해당 연도/월에 윤달이 있는지 확인
function hasLeapMonth(year: number, month: number): boolean {
  const calendar = new KoreanLunarCalendar()
  return calendar.setLunarDate(year, month, 1, true)
}
```

#### 12-6. 반복 일정 설정
| 반복 유형 | 설명 | 옵션 |
|----------|------|------|
| NONE | 반복 없음 | - |
| DAILY | 매일 | 간격(N일마다) |
| WEEKLY | 매주 | 요일 선택, 간격(N주마다) |
| MONTHLY | 매월 | 일자 또는 주차+요일 |
| YEARLY | 매년 | 양력/음력 선택 |

```
[반복 종료 조건]
- 종료일 지정: 특정 날짜까지
- 횟수 지정: N회 반복 후 종료
- 무한 반복: 종료 조건 없음
```

#### 12-7. 캘린더 뷰
| 뷰 타입 | 설명 |
|---------|------|
| 월간 뷰 | 달력 형태로 이벤트 표시 |
| 주간 뷰 | 주 단위 시간표 형태 |
| 일간 뷰 | 하루 시간대별 표시 |
| 목록 뷰 | 이벤트 리스트 형태 |

#### 12-8. 관련 테이블
| 테이블 | 설명 |
|--------|------|
| TB_CALENDAR | 캘린더 정의 |
| TB_EVENT | 이벤트 |
| TB_EVENT_SHARE | 이벤트 공유 |
| TB_CALENDAR_SHARE | 캘린더 공유 |

---

## 파일 네이밍 컨벤션

### 백엔드 (Java)
```
Controller: ItemController.java
Service: ItemService.java, ItemServiceImpl.java
Mapper: ItemMapper.java
DTO: ItemCreateRequest.java, ItemResponse.java
Domain: Item.java
```

### 프론트엔드 (Vue)
```
컴포넌트: ItemList.vue, ItemCard.vue
페이지: ItemsView.vue, LoginView.vue
스토어: item.ts, property.ts
API: item.api.ts, property.api.ts
타입: item.types.ts
```

### SQL/MyBatis
```
Mapper XML: ItemMapper.xml
테이블: TB_ITEM, TB_USER, TB_PROPERTY_DEF
```

---

## Git 컨벤션

### 브랜치 전략
```
main        # 프로덕션
develop     # 개발 통합
feature/*   # 기능 개발
bugfix/*    # 버그 수정
hotfix/*    # 긴급 수정
```

### 커밋 메시지
```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 포맷팅
refactor: 코드 리팩토링
test: 테스트 코드
chore: 빌드, 설정 변경
```

---

## 개발 환경 설정

### Docker Compose
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: taskflow
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/taskflow

  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

### VS Code 확장
- Vue - Official
- ESLint
- Prettier
- Java Extension Pack
- Spring Boot Extension Pack

---

## 참고 사항

### 금지 사항
- ❌ JPA 사용 금지
- ❌ 날짜를 문자열로 저장 금지
- ❌ 설계 승인 없이 코드 작성 금지
- ❌ Unicode 불릿 사용 금지 (CSS/HTML 리스트 사용)

### 권장 사항
- ✅ MyBatis XML Mapper 사용
- ✅ DATE/DATETIME 타입 사용
- ✅ 설계 문서 선행 작성
- ✅ Optimistic Update 패턴
- ✅ 컴포넌트 재사용성 고려

---

## 보안

### 인증 방식
| 항목 | 설명 |
|------|------|
| 인증 방식 | JWT (JSON Web Token) |
| Access Token | 유효기간 30분, localStorage 저장 |
| Refresh Token | 유효기간 7일, httpOnly Cookie 저장 |
| 토큰 갱신 | Access Token 만료 시 자동 갱신 |

### 비밀번호 정책
```
- 최소 8자 이상
- 영문 대문자 1개 이상
- 영문 소문자 1개 이상
- 숫자 1개 이상
- 특수문자 1개 이상 (!@#$%^&*)
- BCrypt 암호화 (strength: 10)
```

### API 보안
| 항목 | 설명 |
|------|------|
| CORS | 허용된 Origin만 접근 |
| CSRF | SameSite Cookie + CSRF Token |
| Rate Limiting | IP당 100 req/min |
| Input Validation | 서버 측 필수 검증 |

### 권한 관리
| 역할 | 권한 |
|------|------|
| OWNER | 보드 삭제, 공유 사용자 관리 |
| MEMBER | 아이템 CRUD, 속성 편집 |
| VIEWER | 조회만 가능 (향후 확장) |

---

## 실시간 동기화

### 동기화 방식
| 방식 | 설명 | 선택 |
|------|------|------|
| Polling | 주기적 API 호출 | - |
| SSE | Server-Sent Events | ✅ 권장 |
| WebSocket | 양방향 통신 | 향후 확장 |

### SSE 구현 상세
```
[연결 흐름]
1. 로그인 후 SSE 연결 수립
2. 서버에서 이벤트 발생 시 클라이언트로 Push
3. 연결 끊김 시 자동 재연결 (3초 후)

[이벤트 타입]
- item:created   - 새 아이템 생성
- item:updated   - 아이템 수정
- item:deleted   - 아이템 삭제
- property:updated - 속성 정의 변경
- comment:created  - 새 댓글

[클라이언트 처리]
- 이벤트 수신 시 Store 즉시 갱신
- 현재 편집 중인 아이템은 충돌 처리
```

### 충돌 처리
```
[동시 편집 충돌 시]
1. 서버 버전과 로컬 버전 비교
2. 충돌 감지 시 사용자에게 알림
3. 선택지 제공:
   - 내 변경사항 유지
   - 서버 버전으로 덮어쓰기
   - 병합 (수동)
```

---

## 에러 처리

### 프론트엔드 에러 처리
```typescript
// composables/useErrorHandler.ts
export function useErrorHandler() {
  const handleError = (error: Error, context?: string) => {
    // 1. 콘솔 로깅
    console.error(`[${context}]`, error)
    
    // 2. 사용자 알림
    if (error instanceof ApiError) {
      toast.error(error.message)
    } else if (error instanceof NetworkError) {
      toast.error('네트워크 연결을 확인해주세요')
    } else {
      toast.error('오류가 발생했습니다')
    }
    
    // 3. 에러 리포팅 (선택)
    // reportError(error)
  }
  
  return { handleError }
}
```

### 에러 바운더리 (Vue)
```vue
<!-- components/ErrorBoundary.vue -->
<template>
  <slot v-if="!error" />
  <div v-else class="error-fallback">
    <p>문제가 발생했습니다</p>
    <button @click="retry">다시 시도</button>
  </div>
</template>
```

### 백엔드 글로벌 예외 처리
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse> handleBusiness(BusinessException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
}
```

### HTTP 상태 코드
| 코드 | 의미 | 사용 |
|------|------|------|
| 200 | OK | 조회/수정 성공 |
| 201 | Created | 생성 성공 |
| 204 | No Content | 삭제 성공 |
| 400 | Bad Request | 유효성 검증 실패 |
| 401 | Unauthorized | 인증 필요 |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 충돌 (동시 수정) |
| 500 | Server Error | 서버 오류 |

---

## 로깅 전략

### 백엔드 로깅
| 레벨 | 용도 |
|------|------|
| ERROR | 예외, 시스템 오류 |
| WARN | 잠재적 문제, 비정상 동작 |
| INFO | 주요 비즈니스 이벤트 |
| DEBUG | 개발/디버깅용 상세 정보 |

### 로그 포맷
```
[%d{yyyy-MM-dd HH:mm:ss}] [%level] [%thread] %logger{36} - %msg%n
```

### 필수 로깅 항목
- API 요청/응답 (INFO)
- 인증 시도 (INFO)
- 인증 실패 (WARN)
- 데이터 변경 (INFO)
- 예외 발생 (ERROR)

---

## 디버깅 및 수정 원칙 (⚠️ 필수 준수)

### 기능 보존 원칙
```
⚠️ 절대 금지 사항
1. 기능 삭제로 오류 해결 금지
2. 기능 축소로 오류 해결 금지
3. 역할/책임 감소로 오류 해결 금지

✅ 필수 준수 사항
1. 오류는 근본 원인을 찾아 정상 수정할 것
2. 기능 축소/삭제가 불가피한 경우 → 반드시 승인 요청
3. 수정 전 영향 범위 분석 필수
```

### DB 스키마 일관성 원칙
```
📁 관련 파일
- docker/mysql/init/01_schema.sql  (테이블 생성)
- docker/mysql/init/02_init_data.sql  (초기 데이터)
- backend/src/main/resources/mapper/*.xml  (MyBatis Mapper)

✅ DB 수정 시 필수 체크리스트
1. 현재 운영 DB 스키마와 초기 구축 스크립트 일치 여부 확인
2. 컬럼 추가/변경 시 → 01_schema.sql 동기화
3. 코드/옵션 추가 시 → 02_init_data.sql 동기화
4. 테이블 구조 변경 시 → 관련 Mapper XML 동시 수정
5. DTO/Domain 클래스 필드 동기화

🔄 수정 순서
1. ERD/테이블 정의서 확인
2. 01_schema.sql 수정
3. 02_init_data.sql 수정 (필요시)
4. Mapper XML 수정
5. Domain/DTO 클래스 수정
6. 전체 정합성 검증
```

### 수정 시 제출 형식
```
📋 변경 보고서
- 수정 대상: [파일 목록]
- 변경 내용: [상세 내용]
- DB 영향: [스키마/데이터 변경 여부]
- 연관 파일: [Mapper, DTO, Service 등]
- 테스트 방법: [검증 절차]
```

---

## 문서 버전

```
| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2024-12-15 | - | 최초 작성 |
| 1.1 | 2024-12-15 | - | 메뉴별 상세 기능 명세 추가, 모바일/PC 동작 차이 상세화, 이력관리 컬럼 명시, 속성 관리 UI 명세 추가, 코드 항목 관리 명세, 보안/실시간동기화/에러처리 섹션 추가, API 명세 상세화 |
| 1.2 | 2024-12-15 | - | 부서 관리 메뉴 추가, 그룹 관리 메뉴 추가, 부서/그룹 API 추가, 사용자-부서-그룹 관계 정의 |
| 1.3 | 2024-12-22 | - | TB_AUDIT_LOG 테이블 추가, 감사 로그 API 추가, 이력관리 메뉴에 관리 이력 탭 추가 |
| 2.3 | 2025-01-18 | - | Todo List 기능 추가 (TB_TODO, TB_TODO_SHARE, TB_ITEM_CHECKLIST), Todo/체크리스트 API 추가, 메뉴 구조 업데이트 |
| 2.4 | 2025-01-19 | - | 캘린더 기능 추가 (Phase 5), 음력 변환 기능 (korean-lunar-calendar), 음력 기념일/윤달 UI 명세, 반복 일정 설정 |
```
---