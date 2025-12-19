# TaskFlow - Claude Code 개발 지시서

## 📌 사전 준비

### 1. 프로젝트 초기화
```bash
# 프로젝트 루트 디렉토리 생성
mkdir taskflow
cd taskflow

# CLAUDE.md 파일 복사 (프로젝트 루트에 위치)
# 이 파일이 있어야 Claude Code가 프로젝트 컨텍스트를 이해함
```

### 2. Claude Code 실행
```bash
claude
```

---

## 🚀 개발 단계별 지시 내역

---

## Phase 1: 프로젝트 구조 및 환경 설정

### 지시 1-1: 프로젝트 구조 생성 251216
```
TaskFlow 프로젝트의 기본 디렉토리 구조를 생성해줘.

요구사항:
- backend: Spring Boot 3.x 프로젝트 (Gradle)
- frontend: Vue.js 3 + Vite 프로젝트
- docker: Docker Compose 설정
- docs: 설계 문서 폴더

CLAUDE.md 파일의 디렉토리 구조 컨벤션을 따라줘.
코드 작성하지 말고 디렉토리 구조만 생성해줘.
``` 

### 지시 1-2: Docker Compose 설정 251216
```
Docker Compose 설정 파일을 생성해줘.

서비스 구성:
- mysql: MySQL 8.0 (포트 3306)
- backend: Spring Boot (포트 8080)
- frontend: Vue.js (포트 3000)

CLAUDE.md의 Docker Compose 예시를 참고해줘.
```

### 지시 1-3: Backend 프로젝트 초기화 251216
```
Spring Boot 3.x 백엔드 프로젝트를 초기화해줘.

설정:
- Java 17
- Gradle (Kotlin DSL)
- 의존성: Spring Web, MyBatis, MySQL Driver, Lombok, Validation, Security

build.gradle.kts 파일과 application.yml 설정 파일을 생성해줘.
JPA는 절대 사용하지 마.
```

### 지시 1-4: Frontend 프로젝트 초기화 251216
```
Vue.js 3 프론트엔드 프로젝트를 초기화해줘.

설정:
- Vite
- TypeScript
- Vue Router
- Pinia
- Tailwind CSS

package.json과 vite.config.ts를 생성해줘.
CLAUDE.md의 프론트엔드 디렉토리 구조를 따라줘.
```

---

## Phase 2: 데이터베이스 설계 (승인 필수)

### 지시 2-1: ERD 설계 251216
```
TaskFlow의 ERD를 Mermaid 다이어그램으로 설계해줘.

필수 테이블:
1. TB_USER - 사용자
2. TB_DEPARTMENT - 부서 (계층 구조)
3. TB_GROUP - 그룹
4. TB_USER_GROUP - 사용자-그룹 매핑 (N:M)
5. TB_BOARD - 보드(컬렉션)
6. TB_BOARD_SHARE - 보드 공유 사용자
7. TB_ITEM - 업무 아이템
8. TB_PROPERTY_DEF - 속성 정의
9. TB_PROPERTY_OPTION - 속성 옵션(코드)
10. TB_ITEM_PROPERTY - 아이템 속성값
11. TB_ITEM_PROPERTY_MULTI - 다중선택 속성값
12. TB_COMMENT - 댓글
13. TB_TASK_TEMPLATE - 작업 템플릿
14. TB_ITEM_HISTORY - 아이템 이력

관계 정의:
- TB_USER.DEPARTMENT_ID → TB_DEPARTMENT (N:1, 사용자는 하나의 부서 소속)
- TB_USER_GROUP으로 사용자-그룹 N:M 관계
- TB_ITEM.GROUP_ID → TB_GROUP (N:1, 업무는 하나의 그룹에 할당)
- TB_DEPARTMENT.PARENT_ID → TB_DEPARTMENT (자기참조, 계층구조)

CLAUDE.md의 DB 컨벤션을 따라줘.
코드 작성하지 말고 ERD만 제시해줘.

승인해주세요.
```

### 지시 2-2: 테이블 정의서 작성 251216
```
승인된 ERD를 기반으로 테이블 정의서를 작성해줘.

각 테이블별로:
- 컬럼명, 타입, 길이, NULL 여부, 기본값, 설명
- PK, FK 제약조건
- 인덱스

CLAUDE.md의 DB 컨벤션을 따라줘:
- 테이블명: 대문자, 스네이크케이스
- PK: TB명_ID
- 날짜: DATE/DATETIME 타입
- 공통 컬럼: CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY

승인해주세요.
```

### 지시 2-3: DDL 스크립트 생성 251216
```
승인된 테이블 정의서를 기반으로 MySQL DDL 스크립트를 생성해줘.

포함 내용:
- CREATE TABLE 문
- PRIMARY KEY, FOREIGN KEY 제약조건
- INDEX 생성
- 초기 데이터 INSERT (상태, 우선순위 등 기본 코드)

파일 위치: /docker/init/01_schema.sql, 02_init_data.sql
```
  기본 계정
  ID: admin
  PW: admin123!

  실행 순서
  # Docker Compose 실행 시 자동 실행
  docker-compose up -d mysql

  # 또는 수동 실행
  mysql -u root -p taskflow < docker/mysql/init/01_schema.sql
  mysql -u root -p taskflow < docker/mysql/init/02_init_data.sql
---

## Phase 3: API 설계 (승인 필수)

### 지시 3-1: API 명세서 작성 251216
```
TaskFlow의 REST API 명세서를 작성해줘.

API 그룹:
1. 인증 API (/api/auth/*)
2. 사용자 API (/api/users/*)
3. 보드 API (/api/boards/*)
4. 아이템 API (/api/boards/{boardId}/items/*)
5. 댓글 API (/api/items/{itemId}/comments/*)
6. 속성 API (/api/boards/{boardId}/properties/*)
7. 옵션 API (/api/properties/{propId}/options/*)
8. 템플릿 API (/api/task-templates/*)
9. 이력 API (/api/history/*)
10. SSE API (/api/sse/*)

각 API별로:
- Method, URL, 설명
- Request Body (JSON 스키마)
- Response Body (JSON 스키마)
- 상태 코드

CLAUDE.md의 API 설계 원칙을 따라줘.

승인해주세요.
```

### 지시 3-2: DTO 클래스 설계 251216
```
승인된 API 명세서를 기반으로 DTO 클래스 구조를 설계해줘.

분류:
- Request DTO: *CreateRequest, *UpdateRequest
- Response DTO: *Response, *ListResponse
- Common DTO: ApiResponse, PageResponse

각 DTO의 필드와 검증 어노테이션을 명시해줘.
코드는 작성하지 말고 구조만 제시해줘.

승인해주세요.
```

---

## Phase 4: 컴포넌트 설계 (승인 필수)

### 지시 4-1: 백엔드 컴포넌트 구조 251216
```
백엔드 컴포넌트 구조를 설계해줘.

각 도메인별로:
- Controller 클래스 및 메서드
- Service 인터페이스 및 구현체
- Mapper 인터페이스
- MyBatis XML Mapper 구조

도메인 목록:
- User (사용자)
- Department (부서)
- Group (그룹)
- UserGroup (사용자-그룹 매핑)
- Board (보드)
- Item (아이템)
- Property (속성 정의)
- Comment (댓글)
- TaskTemplate (작업 템플릿)
- History (이력)

CLAUDE.md의 패키지 구조를 따라줘.
코드는 작성하지 말고 구조만 제시해줘.

승인해주세요.
```

### 지시 4-2: 프론트엔드 컴포넌트 구조 251216
```
프론트엔드 컴포넌트 구조를 설계해줘.

페이지 (views/):
- LoginView.vue
- BoardView.vue (메인 업무 페이지)
- CompletedTasksView.vue
- TaskTemplatesView.vue
- HistoryView.vue
- UsersView.vue
- ShareUsersView.vue
- DepartmentsView.vue (부서 관리)
- GroupsView.vue (그룹 관리)

공통 컴포넌트 (components/):
- layout/: AppLayout, Sidebar, Header
- common/: Button, Input, Select, DatePicker, Modal, Toast, ColorPicker
- ui/: SlideoverPanel, ContextMenu, InlineEditor, TreeView
- item/: ItemTable, ItemKanban, ItemList, ItemCard, ItemForm
- property/: PropertyHeader, PropertyEditor, OptionManager
- department/: DepartmentTree, DepartmentForm, DepartmentNode
- group/: GroupList, GroupForm, GroupMemberManager

Store (stores/):
- auth.ts, board.ts, item.ts, property.ts, user.ts
- department.ts, group.ts

Composables (composables/):
- useAuth, useItem, useProperty, useSSE, useErrorHandler
- useDepartment, useGroup

CLAUDE.md의 프론트엔드 컨벤션을 따라줘.

승인해주세요.
```

---

## Phase 5: 백엔드 구현

### 지시 5-1: 공통 모듈 구현 251216
```
백엔드 공통 모듈을 구현해줘.

구현 대상:
1. ApiResponse - 공통 응답 래퍼
2. GlobalExceptionHandler - 전역 예외 처리
3. BusinessException - 비즈니스 예외
4. SecurityConfig - Spring Security 설정 (JWT)
5. JwtTokenProvider - JWT 토큰 생성/검증
6. JwtAuthenticationFilter - JWT 인증 필터
7. CorsConfig - CORS 설정
8. MyBatisConfig - MyBatis 설정

CLAUDE.md의 보안 섹션과 에러 처리 섹션을 참고해줘.
```
  사용 예시
  // Controller에서 현재 사용자 조회
  @GetMapping("/me")
  public ApiResponse<UserResponse> getMe(@CurrentUser UserPrincipal user) {
      return ApiResponse.success(userService.getUser(user.getUserId()));
  }

  // 비즈니스 예외 발생
  throw BusinessException.userNotFound(userId);
  throw BusinessException.unauthorized("인증이 필요합니다");
### 지시 5-2: 사용자/인증 모듈 구현 251216
```
사용자 및 인증 모듈을 구현해줘.

구현 대상:
1. User 도메인 클래스 (DEPARTMENT_ID 필드 포함)
2. UserMapper.java + UserMapper.xml
3. UserService + UserServiceImpl
4. AuthController (로그인, 로그아웃, 토큰 갱신)
5. UserController (CRUD)
6. 관련 DTO 클래스

비밀번호는 BCrypt로 암호화해줘.
CLAUDE.md의 비밀번호 정책을 적용해줘.
사용자 등록/수정 시 부서 선택 기능을 포함해줘.
```

### 지시 5-2-1: 부서 관리 모듈 구현 251216
```
부서 관리 모듈을 구현해줘.

구현 대상:
1. Department 도메인 클래스
2. DepartmentMapper.java + DepartmentMapper.xml
3. DepartmentService + DepartmentServiceImpl
4. DepartmentController
5. 관련 DTO 클래스

테이블 구조 (TB_DEPARTMENT):
- DEPARTMENT_ID: PK
- DEPARTMENT_CODE: 부서 코드 (UNIQUE)
- DEPARTMENT_NAME: 부서명
- PARENT_ID: 상위 부서 ID (자기참조 FK, NULL 허용)
- SORT_ORDER: 정렬 순서
- USE_YN: 사용 여부 (Y/N)
- 공통 컬럼 (CREATED_AT, CREATED_BY, UPDATED_AT, UPDATED_BY)

기능:
- 부서 CRUD
- 트리 구조 조회 API (계층형 쿼리)
- 평면 구조 조회 API (SELECT 박스용)
- 순서 변경 API
- 부서별 사용자 목록 조회

MyBatis에서 계층형 쿼리 구현:
- WITH RECURSIVE 또는 CONNECT BY 사용
- 결과를 트리 구조 DTO로 변환
```

### 지시 5-2-2: 그룹 관리 모듈 구현 251216
```
그룹 관리 모듈을 구현해줘.

구현 대상:
1. Group 도메인 클래스
2. UserGroup 도메인 클래스 (매핑 테이블)
3. GroupMapper.java + GroupMapper.xml
4. UserGroupMapper.java + UserGroupMapper.xml
5. GroupService + GroupServiceImpl
6. GroupController
7. 관련 DTO 클래스

테이블 구조 (TB_GROUP):
- GROUP_ID: PK
- GROUP_CODE: 그룹 코드 (UNIQUE)
- GROUP_NAME: 그룹명
- GROUP_DESC: 그룹 설명
- GROUP_COLOR: 표시 색상 (#RRGGBB)
- SORT_ORDER: 정렬 순서
- USE_YN: 사용 여부 (Y/N)
- 공통 컬럼

테이블 구조 (TB_USER_GROUP):
- USER_GROUP_ID: PK
- USER_ID: FK → TB_USER
- GROUP_ID: FK → TB_GROUP
- 공통 컬럼

기능:
- 그룹 CRUD
- 그룹 멤버 관리 (추가/제거)
- 그룹별 멤버 목록 조회
- 사용자별 소속 그룹 목록 조회
- 순서 변경 API
```

### 지시 5-3: 보드 모듈 구현 251216
```
보드(컬렉션) 모듈을 구현해줘.

구현 대상:
1. Board 도메인 클래스
2. BoardShare 도메인 클래스
3. BoardMapper.java + BoardMapper.xml
4. BoardShareMapper.java + BoardShareMapper.xml
5. BoardService + BoardServiceImpl
6. BoardController
7. 관련 DTO 클래스

공유 사용자 관리 기능을 포함해줘.
```

### 지시 5-4: 속성 정의 모듈 구현 251216
```
동적 속성 정의 모듈을 구현해줘.

구현 대상:
1. PropertyDef 도메인 클래스
2. PropertyOption 도메인 클래스
3. PropertyDefMapper.java + PropertyDefMapper.xml
4. PropertyOptionMapper.java + PropertyOptionMapper.xml
5. PropertyService + PropertyServiceImpl
6. PropertyController
7. OptionController
8. 관련 DTO 클래스

속성 타입: TEXT, NUMBER, DATE, SELECT, MULTI_SELECT, CHECKBOX, USER
캐시 무효화 로직을 포함해줘.
```

### 지시 5-5: 아이템 모듈 구현 251216
```
업무 아이템 모듈을 구현해줘.

구현 대상:
1. Item 도메인 클래스 (GROUP_ID 필드 포함)
2. ItemProperty 도메인 클래스
3. ItemPropertyMulti 도메인 클래스
4. ItemMapper.java + ItemMapper.xml
5. ItemPropertyMapper.java + ItemPropertyMapper.xml
6. ItemService + ItemServiceImpl
7. ItemController
8. 관련 DTO 클래스

기능:
- 아이템 CRUD
- 동적 속성값 저장/조회
- 완료/삭제 처리
- 필터링, 정렬, 페이징
- 그룹별 필터링 (GROUP_ID)

필터 파라미터:
- status: 상태
- priority: 우선순위
- assigneeId: 담당자
- groupId: 그룹 (TB_GROUP 연동)
- startDate, endDate: 기간

CLAUDE.md의 쿼리 파라미터 규칙을 따라줘.
```

### 지시 5-6: 댓글 모듈 구현 251216
```
댓글(리플) 모듈을 구현해줘.

구현 대상:
1. Comment 도메인 클래스
2. CommentMapper.java + CommentMapper.xml
3. CommentService + CommentServiceImpl
4. CommentController
5. 관련 DTO 클래스

기능:
- 댓글 CRUD
- 아이템별 댓글 목록 조회
```

### 지시 5-7: 작업 템플릿 모듈 구현 251216
```
작업 템플릿 모듈을 구현해줘.

구현 대상:
1. TaskTemplate 도메인 클래스
2. TaskTemplateMapper.java + TaskTemplateMapper.xml
3. TaskTemplateService + TaskTemplateServiceImpl
4. TaskTemplateController
5. 관련 DTO 클래스

기능:
- 템플릿 CRUD
- 자동완성 검색 API (keyword 파라미터)
```

### 지시 5-8: 이력 모듈 구현 251216
```
이력 관리 모듈을 구현해줘.

구현 대상:
1. ItemHistory 도메인 클래스
2. ItemHistoryMapper.java + ItemHistoryMapper.xml
3. HistoryService + HistoryServiceImpl
4. HistoryController
5. 관련 DTO 클래스

기능:
- 작업 처리 이력 조회 (아이템 변경 이력)
- 작업 등록 이력 조회 (템플릿 변경 이력)
- 스위치 기능을 위한 별도 API
```

### 지시 5-9: SSE 모듈 구현 251216
```
실시간 동기화를 위한 SSE 모듈을 구현해줘.

구현 대상:
1. SseEmitterManager - SSE 연결 관리
2. SseController - SSE 구독 엔드포인트
3. SseEventPublisher - 이벤트 발행 서비스

이벤트 타입:
- item:created
- item:updated
- item:deleted
- property:updated
- comment:created

CLAUDE.md의 실시간 동기화 섹션을 참고해줘.
```

---

## Phase 6: 프론트엔드 구현

### 지시 6-1: 공통 모듈 설정
```
프론트엔드 공통 모듈을 구현해줘.

구현 대상:
1. axios 인스턴스 설정 (api/axios.ts)
   - baseURL, 인터셉터, 토큰 자동 첨부
2. Vue Router 설정 (router/index.ts)
   - 라우트 정의, 가드 설정
3. Pinia 설정
4. Tailwind CSS 설정
5. 타입 정의 (types/*.ts)

CLAUDE.md의 프론트엔드 컨벤션을 따라줘.
```

### 지시 6-2: 레이아웃 컴포넌트 구현 251216
```
레이아웃 컴포넌트를 구현해줘.

구현 대상:
1. AppLayout.vue - 전체 레이아웃
2. Sidebar.vue - 좌측 사이드바 (네비게이션, 컬렉션 트리)
3. Header.vue - 상단 헤더
4. SlideoverPanel.vue - 우측 슬라이드오버 패널

CLAUDE.md의 레이아웃 구조를 따라줘.
모바일에서 사이드바는 햄버거 메뉴로 토글되게 해줘.
```

### 지시 6-3: 공통 UI 컴포넌트 구현 251216
```
공통 UI 컴포넌트를 구현해줘.

구현 대상:
1. Button.vue - 버튼 (variant: primary, secondary, danger)
2. Input.vue - 입력 필드
3. Select.vue - 선택 드롭다운 (인라인 옵션 추가 지원)
4. DatePicker.vue - 날짜/시간 선택
5. Modal.vue - 모달 다이얼로그
6. Toast.vue - 토스트 알림
7. ContextMenu.vue - 컨텍스트 메뉴 (⋮)
8. InlineEditor.vue - 인라인 편집기

Compact UI 스펙을 적용해줘:
- Row Height: 36px 이하
- Spacing: 8px
- Font Size: 13~14px
```

### 지시 6-4: 인증 모듈 구현 251216
```
인증 관련 모듈을 구현해줘.

구현 대상:
1. stores/auth.ts - 인증 스토어
2. api/auth.api.ts - 인증 API 호출
3. composables/useAuth.ts - 인증 훅
4. views/LoginView.vue - 로그인 페이지
5. router/guards.ts - 라우트 가드

JWT 토큰 관리:
- Access Token: localStorage
- Refresh Token: httpOnly Cookie (백엔드에서 설정)
- 토큰 만료 시 자동 갱신
```

### 지시 6-5: 보드/아이템 스토어 구현 251216
```
보드 및 아이템 스토어를 구현해줘.

구현 대상:
1. stores/board.ts - 보드 스토어
2. stores/item.ts - 아이템 스토어
3. stores/property.ts - 속성 정의 스토어
4. api/board.api.ts
5. api/item.api.ts
6. api/property.api.ts

Optimistic Update 패턴을 적용해줘:
- Store 먼저 갱신
- API 호출
- 실패 시 롤백

CLAUDE.md의 Pinia 예시를 참고해줘.
```

### 지시 6-6: 업무 페이지 구현 - 테이블 뷰 251216
```
업무 페이지의 테이블 뷰를 구현해줘.

구현 대상:
1. views/BoardView.vue - 메인 업무 페이지
2. components/item/ItemTable.vue - 테이블 뷰
3. components/item/ItemRow.vue - 테이블 행
4. components/property/PropertyHeader.vue - 컬럼 헤더 (속성 관리)

기능:
- 컬럼별 인라인 편집
- 컬럼 헤더 클릭 시 속성 관리 드롭다운
- 완료/삭제 버튼
- 행 클릭 시 슬라이드오버 패널 오픈

Compact UI 스펙을 적용해줘.
한 화면에 최소 15개 항목이 보이도록 해줘.
```

### 지시 6-7: 업무 페이지 구현 - 칸반 뷰 251216
```
업무 페이지의 칸반 뷰를 구현해줘.

구현 대상:
1. components/item/ItemKanban.vue - 칸반 뷰
2. components/item/KanbanColumn.vue - 칸반 컬럼
3. components/item/ItemCard.vue - 아이템 카드

기능:
- 상태별 컬럼 그룹핑 (기본)
- 그룹 기준 변경 (상태, 우선순위, 담당자)
- 드래그 앤 드롭으로 상태 변경
- 카드 클릭 시 슬라이드오버 패널 오픈
```

### 지시 6-8: 업무 페이지 구현 - 리스트 뷰 251216
```
업무 페이지의 리스트 뷰를 구현해줘.

구현 대상:
1. components/item/ItemList.vue - 리스트 뷰
2. components/item/ItemListRow.vue - 리스트 항목

기능:
- 단순 목록 형태
- 주요 정보만 표시 (제목, 상태, 담당자)
- 모바일 최적화
```

### 지시 6-9: 아이템 상세/편집 패널 구현 251216
```
아이템 상세/편집 슬라이드오버 패널을 구현해줘.

구현 대상:
1. components/item/ItemDetailPanel.vue - 상세 패널
2. components/item/ItemForm.vue - 편집 폼
3. components/property/PropertyEditor.vue - 속성 편집기
4. components/comment/CommentList.vue - 댓글 목록
5. components/comment/CommentInput.vue - 댓글 입력

기능:
- 모든 속성 인라인 편집
- 수정자 표시 (최종 수정자, 수정 시간)
- 댓글 목록 및 등록

PC: 우측 슬라이드오버 패널
Mobile: 전체 화면 편집 페이지
```

### 지시 6-10: 신규 업무 등록 구현 251216
```
신규 업무 등록 기능을 구현해줘.

구현 대상:
1. components/item/NewItemInput.vue - 신규 업무 입력
2. components/common/Autocomplete.vue - 자동완성 컴포넌트

기능:
- 업무내용 입력 필드
- 자동완성 (2글자 이상, 디바운스 300ms)
- 기존 작업 템플릿 검색/선택
- 신규등록 버튼
```

### 지시 6-11: 완료 업무 Hidden 영역 구현 251216
```
완료/삭제 업무 Hidden 영역을 구현해줘.

구현 대상:
1. components/item/CompletedItemsCollapse.vue - 축소/확장 영역

기능:
- 당일 완료/삭제된 업무만 표시
- 맨 하단 축소 상태로 표시
- "완료된 업무 (N건)" 텍스트
- 클릭 시 확장/축소
- 작업 내용, 결과(완료/삭제), 처리 시간 표시
```

### 지시 6-12: 속성 관리 UI 구현 251216
```
속성 관리 UI를 구현해줘.

구현 대상:
1. components/property/PropertyHeaderMenu.vue - 헤더 드롭다운 메뉴
2. components/property/PropertyTypeSelector.vue - 타입 선택
3. components/property/OptionManager.vue - 옵션 관리 (설정 메뉴)

기능:
- 컬럼 헤더 클릭 시 드롭다운 메뉴
- 속성 이름 변경 (인라인)
- 속성 타입 변경 (경고 표시)
- 순서 변경 (드래그 앤 드롭)
- 새 속성 추가
- 속성 삭제 (확인 다이얼로그)
- 설정 > 코드 관리 화면
```

### 지시 6-13: 완료 작업 메뉴 구현 251216
```
완료 작업 메뉴를 구현해줘.

구현 대상:
1. views/CompletedTasksView.vue - 완료 작업 페이지

표시 컬럼:
- 등록시간
- 완료시간
- 작업 내용
- 작업자

기능:
- 기간 필터 (오늘, 이번주, 이번달, 사용자 지정)
- 정렬 (완료시간 내림차순 기본)
```
### 지시 6-13-1: 삭제 작업 메뉴 구현 251216
```
삭제 작업 메뉴를 구현해줘.

주의 사항 :
1. 기존 Backend 측 컴포넌트나 기타 처리 과정에 대한 정의 및 기능 사전 검토할것
2. 기존 Frontend 측 컴포넌트나 기타 처리 과정에 대한 정의 및 기능 검토 
3. 추후 관리를 목저으로 삭제전 상태 정보 관리 및 복원기능

누락된 주요 기능이나 역활 구현후  메뉴 구성 완료 할것

구현 대상:
1. views/DeletedTasksView.vue - 완료 작업 페이지
--> 해당 컴포넌트 없음 구현 필요

표시 컬럼:
- 등록시간
- 삭제시간
- 삭제 내용
- 작업자

기능:
- 기간 필터 (오늘, 이번주, 이번달, 사용자 지정)
- 정렬 (등록시간, 삭제시간 내림차순 기본)
```

### 지시 6-14: 작업 등록 메뉴 구현 251216
```
작업 등록 메뉴를 구현해줘.

구현 대상:
1. views/TaskTemplatesView.vue - 작업 템플릿 페이지
2. components/template/TemplateList.vue - 템플릿 목록
3. components/template/TemplateForm.vue - 템플릿 폼

기능:
- 작업 템플릿 목록 표시
- 신규 템플릿 등록
- 기존 템플릿 선택 시 등록→변경 버튼 전환
- 템플릿 삭제
```

### 지시 6-15: 이력관리 메뉴 구현 251216
```
이력관리 메뉴를 구현해줘.

구현 대상:
1. views/HistoryView.vue - 이력관리 페이지
2. components/history/HistoryTable.vue - 이력 테이블
3. components/history/HistorySwitch.vue - 이력 타입 스위치

기능:
- [작업 처리 이력] ↔ [작업 등록 이력] 토글 스위치
- 작업 처리 이력 컬럼: 작업내용, 작업결과, 작업자, 등록시간, 시작시간, 완료시간, 수정시간, 삭제시간
- 작업 등록 이력 컬럼: 작업내용, 등록자, 등록시간, 수정시간, 상태
```

### 지시 6-16: 사용자 관리 메뉴 구현 251216
```
사용자 관리 메뉴를 구현해줘.

구현 대상:
1. views/UsersView.vue - 사용자 등록 페이지
2. components/user/UserList.vue - 사용자 목록
3. components/user/UserForm.vue - 사용자 폼

필드:
- 사용자 이름
- 아이디 (영문+숫자, 4~20자)
- 패스워드 (정책 적용)
- 패스워드 확인

비밀번호 정책 검증을 프론트에서도 수행해줘.
```

### 지시 6-17: 공유 사용자 메뉴 구현 251216
```
공유 사용자 관리 메뉴를 구현해줘.

구현 대상:
1. views/ShareUsersView.vue - 공유 사용자 페이지
2. components/share/ShareUserList.vue - 공유 사용자 목록
3. components/share/ShareUserSearch.vue - 사용자 검색/추가

기능:
- 공유 사용자 목록 표시
- 사용자 검색 및 추가
- 공유 사용자 제거
```

### 지시 6-17-1: 부서 관리 메뉴 구현 251216
```
부서 관리 메뉴를 구현해줘.

구현 대상:
1. views/DepartmentsView.vue - 부서 관리 페이지
2. stores/department.ts - 부서 스토어
3. api/department.api.ts - 부서 API
4. components/department/DepartmentTree.vue - 부서 트리 컴포넌트
5. components/department/DepartmentForm.vue - 부서 등록/수정 폼
6. components/department/DepartmentNode.vue - 트리 노드 컴포넌트

필드:
- 부서 코드
- 부서명
- 상위 부서 (트리에서 선택 또는 드롭다운)
- 정렬 순서
- 사용 여부

기능:
- 트리 구조로 부서 표시
- 부서 CRUD
- 드래그 앤 드롭으로 순서/계층 변경
- 부서 클릭 시 소속 사용자 목록 표시
- 비활성 부서는 회색 처리

트리 UI:
- 확장/축소 토글
- 들여쓰기로 계층 표현
- 컨텍스트 메뉴 (수정, 삭제, 하위 부서 추가)
```

### 지시 6-17-2: 그룹 관리 메뉴 구현 251216
```
그룹 관리 메뉴를 구현해줘.

구현 대상:
1. views/GroupsView.vue - 그룹 관리 페이지
2. stores/group.ts - 그룹 스토어
3. api/group.api.ts - 그룹 API
4. components/group/GroupList.vue - 그룹 목록
5. components/group/GroupForm.vue - 그룹 등록/수정 폼
6. components/group/GroupMemberManager.vue - 그룹 멤버 관리

필드:
- 그룹 코드
- 그룹명
- 그룹 설명
- 그룹 색상 (컬러 피커)
- 정렬 순서
- 사용 여부

기능:
- 그룹 목록 표시 (카드 또는 테이블)
- 그룹 CRUD
- 그룹 색상으로 시각적 구분
- 그룹 클릭 시 멤버 관리 패널 오픈
- 멤버 추가: 사용자 검색 후 추가
- 멤버 제거: 목록에서 제거
- 드래그 앤 드롭으로 순서 변경

그룹 카드 UI:
- 그룹 색상 표시 (좌측 바 또는 배경)
- 그룹명, 설명
- 멤버 수 표시
- 수정/삭제 버튼
```

### 지시 6-17-3: 사용자 폼에 부서/그룹 연동 251216
```
사용자 등록/수정 폼에 부서와 그룹 선택 기능을 추가해줘.

수정 대상:
1. components/user/UserForm.vue - 부서/그룹 필드 추가

추가 필드:
- 소속 부서: 드롭다운 (부서 트리에서 선택)
- 소속 그룹: 다중 선택 (체크박스 또는 태그 입력)

기능:
- 부서 선택 시 계층 구조 표시
- 그룹은 여러 개 선택 가능
- 선택된 그룹은 태그 형태로 표시
- 태그 클릭으로 제거 가능
```

### 지시 6-18: SSE 연결 구현 251216
```
실시간 동기화를 위한 SSE 연결을 구현해줘.

구현 대상:
1. composables/useSSE.ts - SSE 훅
2. stores/sse.ts - SSE 상태 관리

기능:
- 로그인 후 SSE 연결 수립
- 이벤트 수신 시 Store 갱신
- 연결 끊김 시 자동 재연결 (3초 후)
- 현재 편집 중인 아이템 충돌 처리

이벤트 타입:
- item:created, item:updated, item:deleted
- property:updated
- comment:created

CLAUDE.md의 실시간 동기화 섹션을 참고해줘.
```

### 지시 6-19: 에러 처리 구현 251216
```
전역 에러 처리를 구현해줘.

구현 대상:
1. composables/useErrorHandler.ts - 에러 핸들러
2. components/common/ErrorBoundary.vue - 에러 바운더리
3. utils/errorTypes.ts - 에러 타입 정의

기능:
- API 에러 처리 (상태 코드별)
- 네트워크 에러 처리
- 토스트 알림
- 콘솔 로깅

CLAUDE.md의 에러 처리 섹션을 참고해줘.
```

---

## Phase 7: 통합 및 테스트

### 지시 7-1: API 연동 테스트 251217
``` 초회 , 재검 , 전체 점검 , 이슈 확인 수정 , 최종 점검 단계별 반복 수행
``` 전체 점검으로 인해 항상 오류 수정엔 claude.md 디버깅 및 오루 수정 원칙 준수해서 진행 요청
백엔드 API와 프론트엔드 연동을 테스트해줘.

테스트 항목:
1. 로그인/로그아웃
2. 보드 CRUD
3. 아이템 CRUD
4. 속성 관리
5. 댓글 기능
6. SSE 실시간 동기화

각 API 호출이 정상 동작하는지 확인해줘.
```

### 지시 7-2: 반응형 테스트 251217
```
모바일/PC 반응형 동작을 테스트해줘.

테스트 항목:
1. 768px 미만: 사이드바 숨김, 햄버거 메뉴
2. 768px 미만: 아이템 클릭 시 전체 화면 편집
3. 768px 이상: 사이드바 표시
4. 768px 이상: 아이템 클릭 시 슬라이드오버 패널

브레이크포인트별 UI가 정상인지 확인해줘.
```

### 지시 7-3: Docker 빌드 및 실행 251217
```
Docker 환경에서 전체 시스템을 빌드하고 실행해줘.

단계:
1. Backend Dockerfile 작성
2. Frontend Dockerfile 작성
3. docker-compose build
4. docker-compose up

정상 동작 확인:
- MySQL 연결
- Backend API 응답
- Frontend 페이지 로드
```

---

## 📋 체크리스트

### Phase 1: 환경 설정
- [ ] 프로젝트 디렉토리 구조 생성
- [ ] Docker Compose 설정
- [ ] Backend 프로젝트 초기화
- [ ] Frontend 프로젝트 초기화

### Phase 2: DB 설계 (승인 필수)
- [ ] ERD 설계 (부서/그룹 테이블 포함) → **승인**
- [ ] 테이블 정의서 → **승인**
- [ ] DDL 스크립트 생성

### Phase 3: API 설계 (승인 필수)
- [ ] API 명세서 (부서/그룹 API 포함) → **승인**
- [ ] DTO 클래스 설계 → **승인**

### Phase 4: 컴포넌트 설계 (승인 필수)
- [ ] 백엔드 컴포넌트 구조 → **승인**
- [ ] 프론트엔드 컴포넌트 구조 → **승인**

### Phase 5: 백엔드 구현
- [ ] 공통 모듈
- [ ] 사용자/인증 모듈
- [ ] **부서 관리 모듈** ⭐ 신규
- [ ] **그룹 관리 모듈** ⭐ 신규
- [ ] 보드 모듈
- [ ] 속성 정의 모듈
- [ ] 아이템 모듈
- [ ] 댓글 모듈
- [ ] 작업 템플릿 모듈
- [ ] 이력 모듈
- [ ] SSE 모듈

### Phase 6: 프론트엔드 구현
- [ ] 공통 모듈 설정
- [ ] 레이아웃 컴포넌트
- [ ] 공통 UI 컴포넌트
- [ ] 인증 모듈
- [ ] 스토어 구현
- [ ] 테이블 뷰
- [ ] 칸반 뷰
- [ ] 리스트 뷰
- [ ] 아이템 상세 패널
- [ ] 신규 업무 등록
- [ ] 완료 업무 Hidden
- [ ] 속성 관리 UI
- [ ] 완료 작업 메뉴
- [ ] 작업 등록 메뉴
- [ ] 이력관리 메뉴
- [ ] 사용자 관리
- [ ] 공유 사용자 관리
- [ ] **부서 관리 메뉴** ⭐ 신규
- [ ] **그룹 관리 메뉴** ⭐ 신규
- [ ] **사용자 폼 부서/그룹 연동** ⭐ 신규
- [ ] SSE 연결
- [ ] 에러 처리

### Phase 7: 통합 및 테스트
- [ ] API 연동 테스트
- [ ] 반응형 테스트
- [ ] Docker 빌드 및 실행

---

## ⚠️ 주의사항

1. **각 Phase의 설계 단계는 반드시 승인을 받은 후 다음 단계로 진행**
2. **JPA 절대 사용 금지 - MyBatis XML Mapper만 사용**
3. **날짜는 반드시 DATE/DATETIME 타입 사용**
4. **CLAUDE.md 파일을 항상 프로젝트 루트에 유지**
5. **설계 변경이 필요하면 먼저 변경안을 제시하고 승인 받기**
