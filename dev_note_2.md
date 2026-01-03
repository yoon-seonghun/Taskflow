● 현재 뷰타입 관리 방식

  현재 구현 (localStorage)

  // stores/board.ts

  // 뷰타입 저장
  function setViewType(type: ViewType) {
    viewType.value = type
    localStorage.setItem('taskflow_viewType', type)  // 브라우저에 저장
  }

  // 뷰타입 로드
  function loadViewType() {
    const saved = localStorage.getItem('taskflow_viewType')
    if (saved) viewType.value = saved
  }

  비교

  | 항목      | 현재 (localStorage)   | DB (DEFAULT_VIEW) |
  |-----------|-----------------------|-------------------|
  | 저장 위치 | 브라우저              | 서버 DB           |
  | 적용 범위 | 전역 (모든 보드 동일) | 보드별 개별 설정  |
  | 다른 기기 | 공유 안됨             | 공유됨            |
  | 용도      | 사용자 개인 선호      | 보드 기본값       |

  결론

  DB의 DEFAULT_VIEW는 현재 사용되지 않고 있습니다.

  - 조회 쿼리에는 포함되어 있지만
  - 실제 뷰타입 표시는 localStorage 값을 사용

  설계 의도 추측

  원래 설계는 "보드별 기본 뷰"를 지원하려 했으나, 현재는 "사용자 전역 선호 뷰"로 단순화된 것으로 보입니다. DEFAULT_VIEW 컬럼은 향후 보드별 기본 뷰 기능 구현 시 사용될 수      
  있습니다.

  ● DEFAULT_VIEW 컬럼 위치

  1. DB 스키마 정의

  파일: docker/mysql/init/01_schema.sql
  테이블: TB_BOARD

  CREATE TABLE TB_BOARD (
      BOARD_ID BIGINT NOT NULL AUTO_INCREMENT,
      BOARD_NAME VARCHAR(200) NOT NULL,
      DESCRIPTION VARCHAR(500) NULL,
      OWNER_ID BIGINT NOT NULL,
      DEFAULT_VIEW VARCHAR(20) NULL DEFAULT 'TABLE',  -- ← 여기
      COLOR VARCHAR(7) NULL,
      ...
  );

  2. MyBatis 매퍼

  파일: backend/src/main/resources/mapper/BoardMapper.xml

  <!-- ResultMap -->
  <result property="defaultView" column="DEFAULT_VIEW"/>

  <!-- SELECT 쿼리 -->
  SELECT b.DEFAULT_VIEW, ...

  <!-- INSERT 쿼리 -->
  INSERT INTO TB_BOARD (..., DEFAULT_VIEW, ...)

  <!-- UPDATE 쿼리 -->
  DEFAULT_VIEW = #{defaultView}

  3. 용도

  | 값     | 설명               |
  |--------|--------------------|
  | TABLE  | 테이블 뷰 (기본값) |
  | KANBAN | 칸반 뷰            |
  | LIST   | 리스트 뷰          |

  보드를 열 때 어떤 뷰 타입으로 표시할지 저장하는 컬럼입니다.

--------------------------------------------------------------

● 설계를 업데이트했습니다. 주요 변경사항:

  업무 이관:
  - 본인 보드: 보드 선택 후 이관
  - 다른 사용자: 사용자만 선택 → "업무이관" 보드 자동 생성/사용

  보드 이관:
  - 본인 보드: 다른 보드로 업무 병합
  - 다른 사용자: 사용자만 선택 → 보드명 "보드이관"으로 변경 후 소유권 이전

  설계가 요구사항에 맞게 작성되었습니다. 승인해 주시면 구현을 시작하겠습니다.

─────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────── Ready to code?

 Here is Claude's plan:
╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌ 사용자 검색/선택 공통 컴포넌트 및 이관 기능 개선

 개요

 모든 사용자 선택 기능에서 사용할 공통 컴포넌트 개발 및 업무/보드 이관 기능 개선

 ---
 요구사항 정리

 1. 사용자 검색/선택 공통 컴포넌트

 | 검색 방법     | 설명                                                     |
 |---------------|----------------------------------------------------------|
 | 부서에서 찾기 | 부서 트리 표시 → 부서 선택 → 사용자 리스트 → 사용자 선택 |
 | 이름으로 찾기 | 이름 검색 → 이름, 부서, Email 표시 → 사용자 선택         |

 선택된 사용자 표시 형식: 사용자 이름(부서명, Email)

 2. 업무 이관 기능

 | 선택         | 동작                                                 |
 |--------------|------------------------------------------------------|
 | 본인 (Radio) | 본인 보드 목록에서 선택하여 이관                     |
 | 검색 (Radio) | 사용자만 검색/선택 → 자동으로 "업무이관" 보드로 이관 |

 핵심: 다른 사용자에게 업무 이관 시
 - 받는 사람에게 "업무이관" 보드가 자동 생성됨 (없으면 생성, 있으면 기존 사용)
 - 이관된 업무는 해당 보드로 자동 수신

 3. 보드 이관 기능

 | 선택         | 동작                                             |
 |--------------|--------------------------------------------------|
 | 본인 (Radio) | 본인의 다른 보드로 업무 병합 (기존 방식)         |
 | 검색 (Radio) | 사용자만 검색/선택 → 보드가 해당 사용자에게 이관 |

 핵심: 다른 사용자에게 보드 이관 시
 - 보드명이 "보드이관"으로 자동 변경됨
 - 보드 전체가 해당 사용자에게 소유권 이전

 ---
 이관 방식 상세

 업무 이관 플로우

 [본인 보드로 이관]
 1. 본인 보드 목록 표시
 2. 대상 보드 선택
 3. 업무 이관 실행 (boardId 변경)

 [다른 사용자에게 이관]
 1. 사용자 검색/선택 (UserSearchSelector)
 2. 이관 실행 시:
    a. 받는 사용자의 "업무이관" 보드 조회
    b. 없으면 "업무이관" 보드 자동 생성
    c. 해당 보드로 업무 이관

 보드 이관 플로우

 [본인 다른 보드로 병합]
 1. 본인 보드 목록 표시 (현재 보드 제외)
 2. 대상 보드 선택
 3. 보드 내 업무들을 대상 보드로 이동
 4. 원본 보드 삭제 또는 비활성화

 [다른 사용자에게 이관]
 1. 사용자 검색/선택 (UserSearchSelector)
 2. 이관 실행 시:
    a. 보드명을 "보드이관"으로 변경
    b. 보드 소유자(ownerId)를 받는 사용자로 변경
    c. 기존 공유 설정 유지 또는 초기화

 ---
 현재 상태 분석

 존재하는 것

 - UserSelect.vue - 기본 사용자 드롭다운
 - ShareUserSearch.vue - 사용자 검색 (API 호출)
 - departmentApi.getDepartmentUsers() - 부서별 사용자 조회
 - departmentApi.getDepartments() - 부서 트리 조회
 - ItemTransferModal.vue - 현재 업무 이관 모달 (개선 필요)
 - BoardController.deleteBoardWithTransfer() - 보드 삭제 시 업무 이관 (참고용)

 없는 것 (추가 필요)

 - ❌ Email 필드 (TB_USER 테이블, User 도메인)
 - ❌ 부서 트리 + 사용자 선택 통합 컴포넌트
 - ❌ "업무이관" 보드 자동 생성 로직
 - ❌ 보드 소유권 이전 API

 ---
 구현 계획

 Phase 1: 백엔드 - Email 필드 추가

 1-1. DB 스키마 수정 (운영 DB 직접 실행)

 ALTER TABLE TB_USER ADD COLUMN EMAIL VARCHAR(100) NULL AFTER NAME;

 1-2. 도메인 수정

 파일: backend/src/main/java/com/taskflow/domain/User.java
 private String email;  // 이메일 주소

 1-3. DTO 수정

 파일들:
 - UserResponse.java - email 필드 추가
 - UserCreateRequest.java - email 필드 추가
 - UserUpdateRequest.java - email 필드 추가

 1-4. Mapper 수정

 파일: backend/src/main/resources/mapper/UserMapper.xml
 - SELECT, INSERT, UPDATE 쿼리에 EMAIL 컬럼 추가

 ---
 Phase 2: 백엔드 - 업무 이관 기능 개선

 2-1. "업무이관" 보드 자동 생성 로직

 파일: backend/src/main/java/com/taskflow/service/ItemShareService.java

 /**
  * 다른 사용자에게 업무 이관
  * - 받는 사용자의 "업무이관" 보드로 자동 이관
  * - 보드가 없으면 자동 생성
  */
 public ItemResponse transferItemToUser(Long itemId, Long targetUserId, Long currentUserId) {
     // 1. 받는 사용자의 "업무이관" 보드 조회
     Board transferBoard = boardMapper.findByOwnerIdAndName(targetUserId, "업무이관");

     // 2. 없으면 생성
     if (transferBoard == null) {
         transferBoard = createTransferBoard(targetUserId, currentUserId);
     }

     // 3. 업무 이관 실행
     return transferItem(itemId, transferBoard.getBoardId(), currentUserId);
 }

 2-2. BoardMapper에 메서드 추가

 파일: backend/src/main/resources/mapper/BoardMapper.xml
 <select id="findByOwnerIdAndName">
     SELECT * FROM TB_BOARD
     WHERE OWNER_ID = #{ownerId} AND BOARD_NAME = #{boardName} AND USE_YN = 'Y'
 </select>

 ---
 Phase 3: 백엔드 - 보드 이관 기능

 3-1. 보드 소유권 이전 API

 파일: backend/src/main/java/com/taskflow/controller/BoardController.java

 /**
  * 보드 소유권 이전 (다른 사용자에게)
  * PUT /api/boards/{boardId}/transfer-ownership
  */
 @PutMapping("/{boardId}/transfer-ownership")
 public ResponseEntity<ApiResponse<BoardResponse>> transferBoardOwnership(
     @PathVariable Long boardId,
     @RequestBody BoardTransferRequest request  // targetUserId
 )

 3-2. BoardTransferRequest DTO 생성

 파일: backend/src/main/java/com/taskflow/dto/board/BoardTransferRequest.java
 public class BoardTransferRequest {
     private Long targetUserId;  // 이관받을 사용자 ID
     private String reason;      // 이관 사유 (선택)
 }

 3-3. BoardService에 메서드 추가

 파일: backend/src/main/java/com/taskflow/service/BoardService.java
 /**
  * 보드 소유권 이전
  * - 보드명을 "보드이관"으로 변경
  * - 소유자를 targetUserId로 변경
  */
 BoardResponse transferBoardOwnership(Long boardId, Long targetUserId, Long currentUserId);

 ---
 Phase 4: 프론트엔드 - 타입/API 확장

 4-1. User 타입에 email 추가

 파일: frontend/src/types/user.ts
 export interface User {
   // ... 기존 필드
   email?: string
 }

 4-2. Item API 수정 (사용자에게 이관)

 파일: frontend/src/api/item.ts
 // 다른 사용자에게 업무 이관 (보드 선택 없이)
 transferItemToUser(boardId: number, itemId: number, targetUserId: number)

 4-3. Board API 추가 (소유권 이전)

 파일: frontend/src/api/board.ts
 transferBoardOwnership(boardId: number, targetUserId: number)

 ---
 Phase 5: 프론트엔드 - 공통 컴포넌트 개발

 5-1. UserSearchSelector.vue (핵심 공통 컴포넌트)

 파일: frontend/src/components/common/UserSearchSelector.vue

 UI 구조:
 ┌─────────────────────────────────────────────────┐
 │  [부서에서 찾기] [이름으로 찾기]  ← 탭 전환      │
 ├─────────────────────────────────────────────────┤
 │                                                 │
 │  [부서에서 찾기 탭]                              │
 │  ┌──────────────┬───────────────────────────┐  │
 │  │ 부서 트리     │  사용자 목록               │  │
 │  │              │  ┌─────────────────────┐  │  │
 │  │ ▼ 경영지원본부 │  │ ○ 홍길동            │  │  │
 │  │   ├ 인사팀    │  │   인사팀, hong@...   │  │  │
 │  │   ├ 총무팀    │  │ ○ 김철수            │  │  │
 │  │   └ 재무팀    │  │   인사팀, kim@...    │  │  │
 │  │ ▶ 개발본부    │  └─────────────────────┘  │  │
 │  └──────────────┴───────────────────────────┘  │
 │                                                 │
 │  [이름으로 찾기 탭]                              │
 │  ┌─────────────────────────────────────────┐   │
 │  │ 🔍 사용자 이름 검색...                   │   │
 │  ├─────────────────────────────────────────┤   │
 │  │ ○ 홍길동 (인사팀, hong@example.com)     │   │
 │  │ ○ 홍길순 (개발1팀, gilsun@example.com)  │   │
 │  └─────────────────────────────────────────┘   │
 │                                                 │
 ├─────────────────────────────────────────────────┤
 │  선택됨: 홍길동 (인사팀, hong@example.com)      │
 └─────────────────────────────────────────────────┘

 Props:
 interface Props {
   modelValue?: number | null        // 선택된 userId
   excludeUserIds?: number[]         // 제외할 사용자 ID 목록
   placeholder?: string
   disabled?: boolean
 }

 Emits:
 @update:modelValue (userId: number | null)
 @select (user: User)

 5-2. DepartmentTree.vue (부서 트리 하위 컴포넌트)

 파일: frontend/src/components/common/DepartmentTree.vue

 재귀적 트리 렌더링, 펼침/접힘 기능

 ---
 Phase 6: ItemTransferModal 개선

 파일: frontend/src/components/item/ItemTransferModal.vue

 새로운 UI:
 ┌─────────────────────────────────────────────────┐
 │  업무 이관                              [X]     │
 ├─────────────────────────────────────────────────┤
 │                                                 │
 │  이관할 업무                                    │
 │  ┌─────────────────────────────────────────┐   │
 │  │ 로그인 기능 개발                         │   │
 │  └─────────────────────────────────────────┘   │
 │                                                 │
 │  이관 대상                                      │
 │  ┌─────────────────────────────────────────┐   │
 │  │ ● 본인 보드로 이관                       │   │
 │  │ ○ 다른 사용자에게 이관                   │   │
 │  └─────────────────────────────────────────┘   │
 │                                                 │
 │  [본인 선택 시]                                 │
 │  ┌─────────────────────────────────────────┐   │
 │  │ 보드 선택                                │   │
 │  │ ▼ 보드를 선택하세요                      │   │
 │  │   ○ 내 프로젝트                          │   │
 │  │   ○ 개발 업무                            │   │
 │  └─────────────────────────────────────────┘   │
 │                                                 │
 │  [다른 사용자 선택 시]                          │
 │  ┌─────────────────────────────────────────┐   │
 │  │ UserSearchSelector 컴포넌트              │   │
 │  │ (사용자 선택만 - 보드 선택 없음)          │   │
 │  └─────────────────────────────────────────┘   │
 │  ⓘ 선택한 사용자의 "업무이관" 보드로 이관됩니다 │
 │                                                 │
 ├─────────────────────────────────────────────────┤
 │                        [취소]  [이관]           │
 └─────────────────────────────────────────────────┘

 ---
 Phase 7: 보드 이관 모달 (신규)

 파일: frontend/src/components/board/BoardTransferModal.vue

 UI 구조:
 ┌─────────────────────────────────────────────────┐
 │  보드 이관                              [X]     │
 ├─────────────────────────────────────────────────┤
 │                                                 │
 │  이관할 보드                                    │
 │  ┌─────────────────────────────────────────┐   │
 │  │ 프로젝트 관리 (업무 15건)                │   │
 │  └─────────────────────────────────────────┘   │
 │                                                 │
 │  이관 방법                                      │
 │  ┌─────────────────────────────────────────┐   │
 │  │ ● 다른 보드로 업무 병합                  │   │
 │  │ ○ 다른 사용자에게 보드 이관              │   │
 │  └─────────────────────────────────────────┘   │
 │                                                 │
 │  [병합 선택 시]                                 │
 │  ┌─────────────────────────────────────────┐   │
 │  │ 대상 보드 선택                           │   │
 │  │ ▼ 보드를 선택하세요                      │   │
 │  └─────────────────────────────────────────┘   │
 │  ⚠️ 현재 보드는 병합 후 삭제됩니다             │
 │                                                 │
 │  [사용자 이관 선택 시]                          │
 │  ┌─────────────────────────────────────────┐   │
 │  │ UserSearchSelector 컴포넌트              │   │
 │  └─────────────────────────────────────────┘   │
 │  ⓘ 보드명이 "보드이관"으로 변경되어 이관됩니다  │
 │                                                 │
 ├─────────────────────────────────────────────────┤
 │                        [취소]  [이관]           │
 └─────────────────────────────────────────────────┘

 ---
 수정 대상 파일 목록

 백엔드

 | 파일                                            | 작업                          |
 |-------------------------------------------------|-------------------------------|
 | docker/mysql/init/01_schema.sql                 | EMAIL 컬럼 추가               |
 | backend/.../domain/User.java                    | email 필드 추가               |
 | backend/.../dto/user/UserResponse.java          | email 필드 추가               |
 | backend/.../dto/user/UserCreateRequest.java     | email 필드 추가               |
 | backend/.../dto/user/UserUpdateRequest.java     | email 필드 추가               |
 | backend/.../mapper/UserMapper.xml               | EMAIL 컬럼 매핑               |
 | backend/.../mapper/BoardMapper.xml              | findByOwnerIdAndName 추가     |
 | backend/.../mapper/BoardMapper.java             | 메서드 추가                   |
 | backend/.../dto/board/BoardTransferRequest.java | 신규 DTO                      |
 | backend/.../controller/BoardController.java     | 소유권 이전 API               |
 | backend/.../service/BoardService.java           | transferBoardOwnership 메서드 |
 | backend/.../service/impl/BoardServiceImpl.java  | 구현                          |
 | backend/.../service/ItemShareService.java       | transferItemToUser 메서드     |

 프론트엔드 (타입/API)

 | 파일                       | 작업                               |
 |----------------------------|------------------------------------|
 | frontend/src/types/user.ts | email 필드 추가                    |
 | frontend/src/api/item.ts   | transferItemToUser 메서드 추가     |
 | frontend/src/api/board.ts  | transferBoardOwnership 메서드 추가 |

 프론트엔드 (새 컴포넌트)

 | 파일                                                  | 설명                  |
 |-------------------------------------------------------|-----------------------|
 | frontend/src/components/common/UserSearchSelector.vue | 사용자 검색/선택 통합 |
 | frontend/src/components/common/DepartmentTree.vue     | 부서 트리             |
 | frontend/src/components/board/BoardTransferModal.vue  | 보드 이관 모달        |

 프론트엔드 (수정)

 | 파일                                               | 작업                    |
 |----------------------------------------------------|-------------------------|
 | frontend/src/components/item/ItemTransferModal.vue | 새로운 UI               |
 | frontend/src/components/item/ItemShareModal.vue    | UserSearchSelector 적용 |
 | frontend/src/components/common/index.ts            | 새 컴포넌트 export      |
 | frontend/src/views/UsersView.vue                   | Email 입력 필드 추가    |

 ---
 구현 순서

 1. 백엔드 Email 필드 → DB, Domain, DTO, Mapper
 2. 백엔드 보드 조회 메서드 → BoardMapper.findByOwnerIdAndName
 3. 백엔드 업무 이관 개선 → ItemShareService.transferItemToUser
 4. 백엔드 보드 이관 API → BoardController, BoardService
 5. 프론트엔드 타입/API 확장
 6. DepartmentTree 컴포넌트
 7. UserSearchSelector 컴포넌트
 8. ItemTransferModal 개선
 9. BoardTransferModal 신규
 10. ItemShareModal에 UserSearchSelector 적용
 11. UsersView에 Email 필드 추가
 12. 빌드 및 테스트

 ---
 이관 보드 명명 규칙

 | 이관 유형 | 받는 사용자 보드 처리          |
 |-----------|--------------------------------|
 | 업무 이관 | "업무이관" 보드 자동 생성/사용 |
 | 보드 이관 | 보드명 → "보드이관"으로 변경   |

 ---
 공통 컴포넌트 재사용 대상

 UserSearchSelector 적용 대상:
 - ItemTransferModal (업무 이관 - 사용자 선택)
 - BoardTransferModal (보드 이관 - 사용자 선택)
 - ItemShareModal (업무 공유 - 사용자 선택)
 - ShareUserSearch (보드 공유) - 대체 검토

● 감사 로그 (관리 이력) 기능 구현 완료

  모든 Phase가 성공적으로 완료되었습니다.

  구현 내용 요약

  | Phase   | 내용                        | 상태    |
  |---------|-----------------------------|---------|
  | Phase 1 | TB_AUDIT_LOG 테이블 생성    | ✅ 완료 |
  | Phase 2 | 백엔드 AuditLogService 연동 | ✅ 완료 |
  | Phase 3 | 프론트엔드 관리 이력 탭     | ✅ 완료 |

  수정된 파일

  백엔드:
  - BoardServiceImpl.java - 보드 생성/수정/공유 시 감사 로그 기록 추가

  프론트엔드:
  - types/history.ts - AuditLog, AuditLogSearchRequest 타입 추가
  - api/history.ts - getManagementHistory, getRecentManagementHistory API 추가
  - HistorySwitch.vue - '관리 이력' 탭 버튼 추가
  - HistoryTable.vue - 관리 이력 테이블 표시 (대상유형, 액션, 대상, 내용, 수행자, 관련사용자, 일시)
  - HistoryView.vue - 관리 이력 조회 로직 및 필터 (대상유형, 액션) 추가

  기록되는 이벤트

  | 대상 유형   | 액션     | 설명           |
  |-------------|----------|----------------|
  | BOARD       | CREATE   | 보드 생성      |
  | BOARD       | UPDATE   | 보드 수정      |
  | BOARD       | DELETE   | 보드 삭제      |
  | BOARD_SHARE | SHARE    | 보드 공유 추가 |
  | BOARD_SHARE | UNSHARE  | 보드 공유 해제 |
  | BOARD_SHARE | UPDATE   | 공유 권한 변경 |
  | ITEM        | TRANSFER | 업무 이관      |
  | ITEM_SHARE  | SHARE    | 업무 공유 추가 |
  | ITEM_SHARE  | UNSHARE  | 업무 공유 해제 |

  테스트 방법

  1. 브라우저에서 http://localhost:3000 접속
  2. 로그인 후 보드 생성/수정/공유 작업 수행
  3. 이력관리 메뉴 클릭
  4. 관리 이력 탭 선택
  5. 대상유형, 액션 필터로 검색

-------------------------------------------------------------

● 이관 기능 개선 작업 완료

  모든 작업이 이미 구현되어 있었습니다. 확인 결과를 정리합니다.

  완료 상태 확인

  | 항목                                | 상태    | 위치                                            |
  |-------------------------------------|---------|-------------------------------------------------|
  | 백엔드 transferItem                 | ✅ 완료 | ItemShareService.java:214-276                   |
  | "업무이관" 보드 자동 생성           | ✅ 완료 | ItemShareService.java:283-311                   |
  | findByOwnerIdAndName                | ✅ 완료 | BoardMapper.xml:269                             |
  | 프론트엔드 API                      | ✅ 완료 | item.ts:82-84, ItemTransferRequest.targetUserId |
  | UserSearchSelector                  | ✅ 완료 | components/common/UserSearchSelector.vue        |
  | DepartmentTree                      | ✅ 완료 | components/common/DepartmentTree.vue            |
  | ItemTransferModal                   | ✅ 완료 | components/item/ItemTransferModal.vue           |
  | BoardTransferModal                  | ✅ 완료 | components/board/BoardTransferModal.vue         |
  | ItemShareModal + UserSearchSelector | ✅ 완료 | components/item/ItemShareModal.vue:9,213-219    |
  | UserForm Email 필드                 | ✅ 완료 | components/user/UserForm.vue:37,380-390         |

  핵심 기능 요약

  1. 업무 이관 (다른 사용자에게)
    - targetUserId 지정 시 자동으로 "업무이관" 보드 생성/조회
    - 받는 사용자의 보드로 업무 자동 이동
  2. 보드 이관
    - 보드 소유권 이전 API (PUT /boards/{id}/transfer-ownership)
    - 보드명 "보드이관"으로 변경 후 대상 사용자에게 이전
  3. 사용자 검색/선택 컴포넌트
    - 부서에서 찾기 (트리 구조)
    - 이름으로 찾기 (검색)
    - Email 표시 지원

  빌드 상태

  ✅ Backend: Built
  ✅ Frontend: Built
  ✅ All containers: Running

  -----------------------------------------------------
● 현재 파일 업로드 제약

  파일 크기

  | 항목           | 제한 |
  |----------------|------|
  | 최대 파일 크기 | 10MB |

  허용 확장자 (16종)

  | 분류   | 확장자                                    |
  |--------|-------------------------------------------|
  | 이미지 | jpg, jpeg, png, gif, webp, svg            |
  | 문서   | pdf, doc, docx, xls, xlsx, ppt, pptx, txt |
  | 압축   | zip                                       |

  ---
  추가로 허용할 확장자가 있으면 말씀해 주세요. 예를 들어:
  - 동영상: mp4, avi, mov
  - 오디오: mp3, wav
  - 압축: rar, 7z, tar, gz
  - 기타: hwp, csv, json, xml 등

● 현재 설정 확인 완료

  이미 모든 형식이 허용되어 있습니다:

  허용 확장자 (프론트엔드 + 백엔드)

  | 분류       | 확장자                         | 아이콘 색상 |
  |------------|--------------------------------|-------------|
  | 이미지     | jpg, jpeg, png, gif, webp, svg | 🟢 녹색     |
  | PDF        | pdf                            | 🔴 빨간색   |
  | Word       | doc, docx                      | 🔵 파란색   |
  | Excel      | xls, xlsx                      | 🟢 에메랄드 |
  | PowerPoint | ppt, pptx                      | 🟠 주황색   |
  | 텍스트     | txt                            | ⚫ 회색     |
  | 압축       | zip                            | 🟡 노란색   |

  파일 아이콘 (getFileIcon 함수)

  // Word (doc, docx) → 같은 아이콘
  if (['doc', 'docx'].includes(ext)) → 파란색 문서 아이콘

  // Excel (xls, xlsx) → 같은 아이콘  
  if (['xls', 'xlsx', 'csv'].includes(ext)) → 에메랄드 차트 아이콘

  // PowerPoint (ppt, pptx) → 같은 아이콘
  if (['ppt', 'pptx'].includes(ext)) → 주황색 문서 아이콘

-----------------------------------

  완료된 작업

  | Phase   | 내용                            | 상태 |
  |---------|---------------------------------|------|
  | Phase 1 | 보드 스토어 연동 및 데이터 로드 | ✅   |
  | Phase 1 | 보드 분류 computed 속성 추가    | ✅   |
  | Phase 2 | 섹션 토글 기능 구현             | ✅   |
  | Phase 2 | 보드 목록 UI 구현               | ✅   |
  | Phase 4 | 빌드 및 Docker 재빌드           | ✅   |

  구현된 기능

  좌측 사이드바 보드 목록:
  ▼ 내 보드 (N)
    ● 개인 업무

  ▼ 공유해준 보드 (N)
    ● 팀 프로젝트     👥3

  ▼ 공유받은 보드 (N)
    ● 마케팅 보드     @홍길동

  | 기능        | 설명                                    |
  |-------------|-----------------------------------------|
  | 보드 분류   | 내 보드 / 공유해준 보드 / 공유받은 보드 |
  | 색상 표시   | 보드에 설정된 색상 도트                 |
  | 공유 정보   | 공유해준: 👥인원수, 공유받은: @소유자명 |
  | 접기/펼치기 | 각 섹션 토글 가능                       |
  | 보드 선택   | 클릭 시 해당 보드 업무 페이지로 이동    |
  | 하이라이트  | 현재 선택된 보드 강조 표시              |

  수정된 파일

  - frontend/src/components/layout/Sidebar.vue

------------------------------------------------
현재 버전은 2.0이 됩니다. 현업 실무에서 쓸수 있을정도로 완성이 되었다고 판단하고 있습니다. 이에 따라 현 시스템 사용 운영에 대한 메뉴얼이 필요합니다.

# 1. 아키텍처/소스 구조 문서
Use the developer-guide subagent to create complete developer documentation

# 2. API 명세서
Use the api-docs subagent to create REST API documentation with examples

# 3. 배포 가이드
Use the deployment-docs subagent to create deployment manual

# 1. 사용자 매뉴얼
Use the user-manual subagent to create end-user guide

# 2. 관리자 매뉴얼
Use the admin-manual subagent to create system administrator guide

---------------------------------------------------------
● SFTP 연결 테스트 방법

  1. API 엔드포인트로 테스트 (권장)

  애플리케이션 실행 후 스토리지 상태 확인 API 호출:

  # 스토리지 연결 상태 확인
  curl -X GET http://localhost:8080/api/files/storage/health

  # 현재 스토리지 타입 확인
  curl -X GET http://localhost:8080/api/files/storage/type

  성공 응답:
  {
    "success": true,
    "data": {
      "healthy": true,
      "storageType": "SFTP",
      "message": "SFTP storage is healthy. Host: sftp.example.com:22, Base path: /data/uploads",
      "responseTimeMs": 245
    }
  }

  실패 응답:
  {
    "success": false,
    "data": null,
    "message": "SFTP connection failed: Auth fail"
  }

  ---
  2. CLI에서 직접 SFTP 연결 테스트

  애플리케이션 실행 전 SFTP 서버 연결 확인:

  # 비밀번호 인증
  sftp -P 22 sftpuser@sftp.example.com

  # SSH 키 인증
  sftp -i /path/to/private_key -P 22 sftpuser@sftp.example.com

  연결 후 기본 명령어:
  sftp> pwd                    # 현재 경로 확인
  sftp> ls -la                 # 파일 목록
  sftp> cd /data/uploads       # 디렉토리 이동
  sftp> mkdir test             # 디렉토리 생성 테스트
  sftp> rmdir test             # 디렉토리 삭제
  sftp> bye                    # 종료

  ---
  3. Docker로 테스트용 SFTP 서버 구성

  테스트용 SFTP 서버를 Docker로 빠르게 구성:

  # SFTP 테스트 서버 실행
  docker run -d \
    --name sftp-test \
    -p 2222:22 \
    -v sftp-data:/home/testuser/uploads \
    atmoz/sftp \
    testuser:testpass:1001:1001:uploads

  # 연결 테스트
  sftp -P 2222 testuser@localhost
  # 비밀번호: testpass

  테스트용 .env 설정:
  FILE_STORAGE_TYPE=SFTP
  SFTP_HOST=localhost
  SFTP_PORT=2222
  SFTP_USERNAME=testuser
  SFTP_PASSWORD=testpass
  SFTP_BASE_PATH=/home/testuser/uploads

  ---
  4. 애플리케이션 로그 확인

  SFTP 연결 상태는 애플리케이션 시작 시 로그로 확인:

  # Docker 환경
  docker compose logs -f backend | grep -i sftp

  # 로컬 환경
  ./gradlew bootRun 2>&1 | grep -i sftp

  성공 로그:
  INFO  - Initializing SFTP file storage: testuser@localhost:2222
  INFO  - SSH private key loaded from: /path/to/key (키 인증 시)
  INFO  - SFTP file storage initialized successfully. Base path: /data/uploads

  실패 로그:
  ERROR - Failed to initialize SFTP storage: Auth fail
  ERROR - Failed to initialize SFTP storage: java.net.ConnectException: Connection refused

  ---
  5. 파일 업로드/다운로드 테스트

  연결 확인 후 실제 파일 작업 테스트:

  # 로그인 (JWT 토큰 획득)
  TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}' \
    | jq -r '.data.accessToken')

  # 파일 업로드
  curl -X POST http://localhost:8080/api/files/upload \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@/path/to/test.txt"

  # 업로드된 파일 확인 (SFTP 서버에서)
  sftp -P 2222 testuser@localhost
  sftp> ls -la /home/testuser/uploads/2024/12/

  ---
  6. 문제 해결 체크리스트

  | 증상               | 확인 사항                                      |
  |--------------------|------------------------------------------------|
  | Connection refused | 호스트/포트 확인, 방화벽 확인                  |
  | Auth fail          | 사용자명/비밀번호 확인, SSH 키 경로 확인       |
  | No such file       | SFTP_BASE_PATH 디렉토리 존재 여부              |
  | Permission denied  | SFTP 사용자 쓰기 권한 확인                     |
  | Connection timeout | 네트워크 연결, SFTP_CONNECTION_TIMEOUT 값 증가 |

------------------------------------------------------
  1. 개발자 가이드 문서 작성 ✅                                                                                                                                              ─

  - 파일: /docs/2.0/01_개발자_가이드.md
  - 아키텍처, 소스 구조, DB 설계, 개발 가이드 등 포함

  2. SFTP 외부 스토리지 구현 ✅

  - 수정/생성된 파일:
    - build.gradle.kts - JSch 라이브러리 추가
    - StorageType.java - SFTP 타입 추가
    - FileStorageProperties.java - SFTP 설정 클래스 추가
    - SftpFileStorage.java - SFTP 구현체 (신규)
    - FileStorageService.java - checkHealth() 메서드 추가
    - LocalFileStorage.java - 헬스체크 구현
    - FileController.java - 스토리지 상태 API 추가
    - application.yml - SFTP 설정 추가
    - .env.example - SFTP 환경변수 추가

  3. SFTP 연결 테스트 가이드 ✅

  - API 엔드포인트 테스트 방법
  - CLI 연결 테스트
  - Docker 테스트 SFTP 서버 구성
  - 트러블슈팅 체크리스트

  4. 설정 레퍼런스 문서 작성 ✅

  - 파일: /docs/2.0/06_설정_레퍼런스.md
  - 10개 카테고리로 분류된 전체 설정 항목:
    a. 환경 변수 (.env)
    b. 데이터베이스 설정
    c. 인증/보안 설정
    d. 파일 스토리지 설정 (LOCAL/SFTP)
    e. 서버 포트 설정
    f. CORS 설정
    g. 로깅 설정
    h. 커넥션 풀 설정
    i. Nginx 설정
    j. 프로파일별 설정

-------------------------------------------------
  수정 완료

  build-release.sh (Linux/Mac)

  # 사용법
  ./build-release.sh -h          # 도움말 표시
  ./build-release.sh --help      # 도움말 표시
  ./build-release.sh -v          # 버전 표시
  ./build-release.sh 2.0.0       # 버전 2.0.0으로 빌드
  ./build-release.sh -c 2.0.0    # dist 정리 후 빌드
  ./build-release.sh -s 2.0.0    # npm install 건너뜀

  추가된 옵션:
  | 옵션           | 설명                        |
  |----------------|-----------------------------|
  | -h, --help     | 도움말 표시                 |
  | -v, --version  | 스크립트 버전 표시          |
  | -c, --clean    | dist 폴더 전체 정리 후 빌드 |
  | -s, --skip-npm | npm install 건너뜀          |

  build-release.ps1 (Windows)

  # 사용법
  .\build-release.ps1 -Help                # 도움말 표시
  .\build-release.ps1 -Version 2.0.0       # 버전 2.0.0으로 빌드
  .\build-release.ps1 -Clean -Version 2.0.0  # dist 정리 후 빌드
  .\build-release.ps1 -SkipNpm             # npm install 건너뜀
  Get-Help .\build-release.ps1 -Full       # PowerShell 기본 도움말

  추가된 옵션:
  | 옵션     | 설명                        |
  |----------|-----------------------------|
  | -Help    | 도움말 표시                 |
  | -Clean   | dist 폴더 전체 정리 후 빌드 |
  | -SkipNpm | npm install 건너뜀          |

  PowerShell 스크립트는 <# .SYNOPSIS ... #> 형식의 주석 기반 도움말도 추가되어 Get-Help 명령으로도 도움말을 확인할 수 있습니다.

-------------------------------------------------
  ---
  업무 속성 구조 분석

  화면 구성 (캡처 이미지 기준)

  cap_1.jpg - 업무 리스트 (테이블 뷰)

  | 컬럼     | 설명                            |
  |----------|---------------------------------|
  | 업무내용 | 업무 제목 (title)               |
  | 카테고리 | 동적 속성 (TB_PROPERTY_DEF)     |
  | 상태     | 고정 속성 - TB_ITEM.STATUS      |
  | 우선순위 | 고정 속성 - TB_ITEM.PRIORITY    |
  | 담당자   | 고정 속성 - TB_ITEM.ASSIGNEE_ID |
  | 시작일   | 고정 속성 - TB_ITEM.START_TIME  |
  | 마감일   | 고정 속성 - TB_ITEM.END_TIME    |
  | 액션     | 완료/삭제 버튼                  |

  cap_2.jpg - 업무 상세 패널

  ┌─────────────────────────────────────────────┐
  │ 업무 상세                                    │
  ├─────────────────────────────────────────────┤
  │ [제목] 프론트엔드 레이아웃 구현              │
  ├─────────────────────────────────────────────┤
  │ ┌─ 기본 속성 (TB_ITEM 컬럼) ─────────────┐  │
  │ │ 상태     │ 우선순위 │ 그룹   │ 담당자   │  │
  │ │ 시작일   │ 완료일   │ 마감일 │          │  │
  │ └─────────────────────────────────────────┘  │
  ├─────────────────────────────────────────────┤
  │ ● 추가 속성 (TB_PROPERTY_DEF + EAV)         │
  │ ├ 카테고리                                   │
  │ ├ 상태* (동적 속성으로 추가된 경우)          │
  │ ├ 우선순위* (동적 속성으로 추가된 경우)      │
  │ ├ 담당자                                     │
  │ ├ 시작일                                     │
  │ └ 마감일                                     │
  └─────────────────────────────────────────────┘

  ---
  속성 분류 및 저장 위치

  1. 고정 속성 (기본 속성) - TB_ITEM 테이블 컬럼

  | 속성명   | DB 컬럼         | 타입                    | 설명                                      |
  |----------|-----------------|-------------------------|-------------------------------------------|
  | 제목     | CONTENT (title) | VARCHAR(500)            | 업무 제목                                 |
  | 상세내용 | DESCRIPTION     | TEXT                    | 마크다운 본문                             |
  | 상태     | STATUS          | VARCHAR(20)             | NOT_STARTED/IN_PROGRESS/COMPLETED/DELETED |
  | 우선순위 | PRIORITY        | VARCHAR(20)             | URGENT/HIGH/NORMAL/LOW                    |
  | 그룹     | GROUP_ID        | FK → TB_GROUP           | 업무 그룹                                 |
  | 담당자   | ASSIGNEE_ID     | FK → TB_USER            | 담당자                                    |
  | 시작일   | START_TIME      | DATETIME                | 시작 시간                                 |
  | 완료일   | END_TIME        | DATETIME                | 완료 시간                                 |
  | 카테고리 | CATEGORY_ID     | FK → TB_PROPERTY_OPTION | 카테고리 옵션                             |

  특징:
  - TB_ITEM 테이블에 직접 저장
  - 모든 보드에서 공통으로 사용
  - 성능 최적화됨 (인덱스 적용)
  - 코드에서 Item 객체의 직접 필드로 접근

  // frontend/src/types/item.ts
  interface Item {
    itemId: number
    status: ItemStatus      // 고정 속성
    priority: Priority      // 고정 속성
    assigneeId?: number     // 고정 속성
    startTime?: string      // 고정 속성
    endTime?: string        // 고정 속성
    groupId?: number        // 고정 속성
    // ...
  }

  ---
  2. 동적 속성 (추가 속성) - EAV 패턴

  테이블 구조

  TB_PROPERTY_DEF (속성 정의)
      ├── PROPERTY_ID (PK)
      ├── BOARD_ID (보드별 정의)
      ├── PROPERTY_NAME (속성명)
      ├── PROPERTY_TYPE (TEXT/NUMBER/DATE/SELECT/...)
      └── VISIBLE_YN (표시 여부)

  TB_PROPERTY_OPTION (선택형 옵션)
      ├── OPTION_ID (PK)
      ├── PROPERTY_ID (FK)
      └── OPTION_LABEL (옵션명)

  TB_ITEM_PROPERTY (속성값 - EAV)
      ├── ITEM_ID (FK)
      ├── PROPERTY_ID (FK)
      ├── VALUE_TEXT (텍스트 값)
      ├── VALUE_NUMBER (숫자 값)
      ├── VALUE_DATE (날짜 값)
      ├── VALUE_OPTION_ID (선택 옵션)
      └── VALUE_USER_ID (사용자)

  특징:
  - 보드별로 커스텀 속성 정의 가능
  - systemYn = 'N'인 속성만 "추가 속성"으로 표시
  - EAV 패턴으로 유연한 속성 확장
  - propertyValues 객체로 접근

  // frontend/src/types/item.ts
  interface Item {
    // ...고정 속성...
    properties?: ItemProperty[]           // 속성값 배열
    propertyValues?: Record<number, unknown>  // { propertyId: value }
  }

  ---
  화면별 속성 표시 범위

  리스트 뷰 (테이블/칸반/리스트)

  // ItemTable.vue - 표시 컬럼
  const defaultColumnWidths = {
    title: 250,      // 업무내용 (고정)
    status: 96,      // 상태 (고정)
    priority: 96,    // 우선순위 (고정)
    startTime: 128,  // 시작일 (고정)
    endTime: 128,    // 마감일 (고정)
    assignee: 96,    // 담당자 (고정)
    comments: 64,    // 댓글 수
    actions: 80      // 액션 버튼
  }
  // + 동적 속성 컬럼 (TB_PROPERTY_DEF에서 VISIBLE_YN='Y')

  | 표시 항목 | 속성 유형 | 소스                               |
  |-----------|-----------|------------------------------------|
  | 업무내용  | 고정      | TB_ITEM.CONTENT                    |
  | 카테고리  | 동적      | TB_PROPERTY_DEF → TB_ITEM_PROPERTY |
  | 상태      | 고정      | TB_ITEM.STATUS                     |
  | 우선순위  | 고정      | TB_ITEM.PRIORITY                   |
  | 담당자    | 고정      | TB_ITEM.ASSIGNEE_ID                |
  | 시작일    | 고정      | TB_ITEM.START_TIME                 |
  | 마감일    | 고정      | TB_ITEM.END_TIME                   |

  ---
  상세 패널 (ItemForm.vue)

  <!-- 기본 속성 (고정) -->
  <div class="grid grid-cols-2 gap-3">
    <Select v-model="status" />       <!-- 상태 -->
    <Select v-model="priority" />     <!-- 우선순위 -->
    <Select v-model="groupId" />      <!-- 그룹 -->
    <Select v-model="assigneeId" />   <!-- 담당자 -->
    <DatePicker v-model="startTime" /> <!-- 시작일 -->
    <DatePicker v-model="endTime" />   <!-- 완료일 -->
    <DatePicker v-model="dueDate" />   <!-- 마감일 -->
  </div>

  <!-- 추가 속성 (동적) -->
  <div v-if="customProperties.length > 0">
    <h4>추가 속성</h4>
    <PropertyEditor 
      v-for="property in customProperties"
      :property="property"
      v-model="propertyValues[property.propertyId]"
    />
  </div>

  | 섹션      | 표시 항목                                            | 저장 위치              |
  |-----------|------------------------------------------------------|------------------------|
  | 기본 속성 | 상태, 우선순위, 그룹, 담당자, 시작일, 완료일, 마감일 | TB_ITEM 컬럼           |
  | 추가 속성 | 카테고리, 커스텀 속성들                              | TB_ITEM_PROPERTY (EAV) |

  ---
  연관 구조 다이어그램

  ┌─────────────────────────────────────────────────────────────────┐
  │                          TB_BOARD                                │
  │  (보드 정의)                                                     │
  └───────────────────────────┬─────────────────────────────────────┘
                              │
           ┌──────────────────┼──────────────────┐
           │                  │                  │
           ▼                  ▼                  ▼
  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────────────┐
  │    TB_ITEM      │ │ TB_PROPERTY_DEF │ │       TB_GROUP          │
  │  (업무 아이템)   │ │   (속성 정의)    │ │      (그룹 정의)        │
  ├─────────────────┤ ├─────────────────┤ └─────────────────────────┘
  │ ■ STATUS        │ │ PROPERTY_NAME   │
  │ ■ PRIORITY      │ │ PROPERTY_TYPE   │
  │ ■ ASSIGNEE_ID   │ │ VISIBLE_YN      │
  │ ■ START_TIME    │ └────────┬────────┘
  │ ■ END_TIME      │          │
  │ ■ GROUP_ID      │          ▼
  │ ■ CATEGORY_ID   │ ┌─────────────────────┐
  └────────┬────────┘ │ TB_PROPERTY_OPTION  │
           │          │   (선택 옵션)        │
           │          └─────────────────────┘
           │
           ▼
  ┌─────────────────────────────────────────┐
  │          TB_ITEM_PROPERTY               │
  │            (EAV 속성값)                  │
  ├─────────────────────────────────────────┤
  │ ITEM_ID ──────────► TB_ITEM             │
  │ PROPERTY_ID ──────► TB_PROPERTY_DEF     │
  │ VALUE_TEXT / VALUE_NUMBER / VALUE_DATE  │
  │ VALUE_OPTION_ID ──► TB_PROPERTY_OPTION  │
  │ VALUE_USER_ID ────► TB_USER             │
  └─────────────────────────────────────────┘

  ---
  속성 용도 및 적용 범위 요약

  | 구분      | 속성 유형  | 저장 위치        | 적용 범위      | 용도                                  |
  |-----------|------------|------------------|----------------|---------------------------------------|
  | 기본 속성 | 고정       | TB_ITEM 컬럼     | 전체 보드 공통 | 핵심 업무 관리 (상태, 우선순위, 일정) |
  | 추가 속성 | 동적 (EAV) | TB_ITEM_PROPERTY | 보드별 개별    | 커스텀 분류/관리 (카테고리, 태그 등)  |

  주요 차이점

  | 항목      | 기본 속성        | 추가 속성              |
  |-----------|------------------|------------------------|
  | 정의 위치 | 코드 하드코딩    | TB_PROPERTY_DEF        |
  | 옵션 정의 | 코드 상수        | TB_PROPERTY_OPTION     |
  | 저장 방식 | TB_ITEM 직접     | EAV (TB_ITEM_PROPERTY) |
  | 쿼리 성능 | 빠름 (인덱스)    | 느림 (JOIN 필요)       |
  | 확장성    | 스키마 변경 필요 | 즉시 추가 가능         |
  | UI 표시   | 항상 표시        | VISIBLE_YN='Y'만       |
-------------------------------------------------
-- 25.12.24 개선 항목
-------------------------------------------------
```
0. 업무별 간트 차트 및 calendar 기능 추가
0. 일정관리 및 Todo List 관리 기능 추가
  └ 기존 업무에서 일정 혹은 Todo List로 연동 등록 과정 (완료,업무관리 전환)
  └ 업무 내역과 일정 , Topdo List 는 별개 관리이며 상호 연동으로 운영 
  └ 일정 공유자 등록 
0. 내용 입력창 별도 모달로 분리 가능하도록 기능 추가
0. 내용 입력창에서 마이크로 음성 늑옴 기능 추가
  └ 녹음된 음성은 AI 연동 요약 및 음성 Text 변환 기록 기능 추가
  └ 녹음된 내용중 해야할일과 일정 관련 내용 확인시 Todo List와 일정관리 기능으로 연동 등록 confirm 기능 추가
  └ 녹음 음성은 mp4 첨부파일로 저장 , 요약은 본문 내용에 기재 , 전문 text는 파일로 기록 첨부파일로 저장
0. 업무관리 속성 및 관리 기능에 회의록 관리 에 필요한 속성 추가
>>>>>>
1. 화면 우측 상단 개인 정보 변경 기능 생성
  └ 사용자 등록시 초기 패스워드 등록 ( 설정 기능으로 초기 패스워드 자동 생성 )
  └ 사용자 스스로 패스워드 변경시 패스워드 규칙 표시 ()
2. 사용자 역활 기능 추가 (관리자 , 사용자)
  └ 사용자 패스워드 초기화 기능 (초기화시 사용차 초기 로그인시 강제 변경 기능)
  └ 사용자 관리 , 부서관리 , 전체 그룹 현황 관리 
  └ 현재 그룹관리 기능은 개인용 그룹관리 기능으로 유지
3. 관리자 전용 공유 , 이관 기능 
  └ 등록 사용자 보드 및 업무 현황 Viewer 
  └ 등록된 타 사용자 보드 및 업무 강제 이관 기능 (관리자 전용 기능)
  └ 등록된 타 사용자 보드 및 업무 강제 공유 기능 (관리자 전용 기능)
4. 설정 기능 생성
  └ 초기 패스워드 등록 (사용차 초기 로그인시 강제 변경 기능)
  └ 사용자 정보 DB 변경 기능 (초기 설정 DB 또는 외부 DB)
  └ 또는 현재 DB 사용자 , 부서 정보를 외부 DB에서 가져와 동기화 기능
  └ 저장소 설정 및 관리 기능 (Local , 외부 저장소) (이관 기능)
  └ 파일 업로드 제약 기능 설정 
  └ 업로드 허용 파일형식 등록 수정 기능 
  └ 서버 연결 Port 설정
  └ 카테고리 항목 관리 기능
  └ 카테고리별 상태 항목 관리 기능
5. Taskflow System log Viewer
6. 시스템 전체 파일 사용량 및 부서별 사용자별 사용 현황 Viewer
  └ 시스템 파일 전체 파일 사용량
  └ 시스템 파일 전체 보드 및 업무 사용량
  └ 부서 또는 사용자별 보드 또는 업무 파일 사용량 

. AI 연동 기능 (회의록 요약 및 일정 관리)
```

현재 업무 현황 확인시 테이블 ,칸반, 리스트 3종류로 구성되어 있는데 여기에 간트 차트와 calendar기능을 추가하고 싶고

요청일 , 마감일
시작일 , 완료일


제거
1. 기본속성 마감일 속성 제거 (현재 : 마감일 속성 선택시 에러 발생)
2. 추가속성의 상태와 , 우선순위 제거 (기본속성과 중복)
위 속성들 기본속성의 "마감일" , 추가속성의 "상태","우선순위" 각 항목별 사용처와 사용하거나 적용된 함수 및 기능,역활 검토 보고서 제출후 대응 모색후 승인 요청
지연 처리에 사용되는 속성과 계산이나 참고에 활용되는속성도 확인 바랍니다. 기본 속성인지 추가속성인지... 그중 어떤 속성들인지... 등등 

방안1로 기본 처리 제안 내용과 더불어 아래 내용은 참조로만 활용
기본속성 UI 마감일 항목 제거, 칸반보드 칸반카드 기본 속성 완료일 기준으로 변경 , 리스트뷰 마감일 항목 기본 속성 완료일 기준으로 변경

지연 업무 
1. 기본 속성 완료일 기준으로 지연 계산 필요
2. 기본 속성 시작일 기준 완료일 초과시 지연 처리
3. 완료일 미등록시 현재일 기준으로 지연 계산 
4. 지연 업무 리스트는 지연업무 메뉴에만 존재하는게 아닌 기본 업무 페이지에 남아서 표시되어야 함

보드 신규 생성후 업무 등록시 업무리스트에서 상태, 우선순위 안나타남 , 속성 페이지에서 기본 속성의 시작일 , 완료일이 등록된후 삭제가 되지 않음 또한 기본속성의 시작일 , 완료일 선택시 선택날짜 이전 날짜가 선택됨
---------------------------------------------------
명칭 변경 
1. 업무내용 표시 "시작일" , "마감일" -> "시작일","완료일"
2. 업무 기본 속성 표시 "시작일" , "완료일" -> "요청일","마감일"
3. 업무 추가 속성 표시 "시작일" , "마감일" -> "시작일" , "완료일"

명칭 변경후 적용 (명칭변경 완료후)
1. 업무 관리 표시될 "시작일" 은 추가속성 "시작일" 없을경우 기본속성 "요청일"로
2. 업무 관리 표시될 "완료일" 은 추가속성 "완료일" 없을경우 기본속성 "마감일"

기능 오류
1. 날짜 속성을 선택하는 항목들이 연속으로 클릭 오픈시 calendar 겹침
  └ 속성은 겹치지 않게 다른 콤포넌트 선택시 현재 오픈된 콤보나 calendar는 닫혀야 함
  └ 현재 해당 겹칩 문제는 calendar에 한정되지 않으며 속성창에서 각 콤보 컴포넌트의 겹침을 해결하기 위함
  └ calendar 가 내용입력창에 가려짐 (cap_3.jpg 참조)
2. 카테고리는 기본속성으로 전환
3. 속성 항목에 "보류" 항목 추가

보드 생성시 추가속성을 자동으로 배치하는 구조라면 추가속성은 생성자가 선택적으로 등록할수 있게 하는게 맞지 않을까?

보드생성시 추가속성 선택이 된다면 생성된 보드 추가속성 변경 기능(추가 혹은 제거) - 기존 생성된 업무에 적용 유무 사용자 승인 요청 , 승인시 기존 업무 속성 변경
업무 이관시 추가 속성이 보드에 따라 없거나 혹은 추가 되어 있을수 있는데 추가속성으로 인해 업무 이관에 문제가 발생할 소지는 어떻게 되는가?

추가 속성에 대한 변경 , 삭제 기능이 추가되고 보드 생성시 추가속성에 대해 유동적으로 관리된다면 업무에 대한 이관 기능은 문제를 일으킬 소지는 없는가?

각 아이템 별로 분석 및 가능성 및 추가 제안 레포트 바랍니다. 
----------------------------------------------------------------

추가속성 등록 , 삭제 관리 기능 필요 
추가속성 관리 기능이 완료 되면 보드 생성시 추가속성 선택 기능 추가

ALTER TABLE TB_BOARD_SHARE
  ADD COLUMN SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '정렬 순서' AFTER PERMISSION;

Backend Compile
cd /mnt/d/SNS_Proj/Taskflow/backend && ./mvnw compile -q 2>&1 | tail -20
Frontend Compile
cd /mnt/d/SNS_Proj/Taskflow/frontend && npm run build 2>&1 | tail -30

위 방식 실패시 (Backend)
cd /mnt/d/SNS_Proj/Taskflow/backend && ls -la *.sh *.bat mvnw* 2>/dev/null || echo "No wrapper found" && ls -la pom.xml 2>/dev/null
which mvn && cd /mnt/d/SNS_Proj/Taskflow/backend && mvn compile -q 2>&1 | tail -20 || echo "Maven not available"
------------------------------------------
docker compose down -v 2>&1  '중지후 삭제'
docker compose build --no-cache 2>&1
docker compose up -d 2>&1
docker compose up -d --build '재빌드후 시작'
docker restart taskflow-backend && sleep 10 && docker logs taskflow-backend --tail 50 2>&1 '재시작후 로그 확인'
------------------------------------------
docker exec taskflow-mysql mysql -u taskflow -ptaskflow123 taskflow --default-character-set=utf8mb4 -e "SELECT PROPERTY_ID, BOARD_ID, PROPERTY_NAME, PROPERTY_TYPE,SORT_ORDER FROM TB_PROPERTY_DEF ORDER BY BOARD_ID, SORT_ORDER;" 2>&1 | cat
docker exec taskflow-mysql mysql -u taskflow -ptaskflow123 taskflow --default-character-set=utf8mb4 -e "SELECT OPTION_ID, PROPERTY_ID, OPTION_LABEL, COLOR FROM TB_PROPERTY_OPTION ORDER BY PROPERTY_ID, SORT_ORDER;" 2>&1 | cat

docker exec taskflow-mysql mysql -u taskflow -ptaskflow123 taskflow --default-character-set=utf8mb4 -e "select * from tb_

TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MSwidHlwZSI6IkFDQ0VTUyIsImlhdCI6MTc2Njg0ODY2NywiZXhwIjoxNzY2ODUwNDY3fQ.__yfOJ2q1EGlWnip7ca83Wyz4QsZ44gTTmkfE   
   XRjzlc"
   curl -s http://localhost:8080/api/boards/1/properties \
     -H "Authorization: Bearer $TOKEN" | python3 -m json.tool 2>/dev/null || cat

TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin1234"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['access_token'])")
   echo "Token: ${TOKEN:0:50}..."
   curl -s http://localhost:8080/api/boards/1/properties \
     -H "Authorization: Bearer $TOKEN" | python3 -m json.tool 2>/dev/null || cat
   Get properties with fresh token

TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin1234"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['access_token'])")
   echo "Token: ${TOKEN:0:50}..."
   curl -s http://localhost:8080/api/boards/1/properties \
     -H "Authorization: Bearer $TOKEN" | python3 -m json.tool 2>/dev/null || cat
   Get properties with fresh token

curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin1234"}' > /tmp/login.json &&
   TOKEN=$(cat /tmp/login.json | grep -o '"access_token":"[^"]*' | sed 's/"access_token":"//') && curl -s http://localhost:8080/api/boards/1/properties -H "Authorization:    
   Bearer $TOKEN"
   Get properties with fresh token

curl -s -X POST "http://localhost:8080/api/auth/login" -H "Content-Type: application/json" -d '{"username":"admin","password":"admin1234"}' 2>&1 | tee /tmp/login.json     
   Login and save response1
---------------------------------------------

docker exec taskflow-backend /bin/bash -c "cd /app && ./gradlew compileJava --no-daemon 2>&1" 2>&1
cd /mnt/d/SNS_Proj/Taskflow && docker run --rm -v "$(pwd)/backend:/project" -w /project gradle:8.5-jdk17 gradle compileJava --no-daemon 2>&1

docker compose up -d --build frontend 2>&1
docker compose build --no-cache frontend 2>&1
docker compose build frontend 2>&1
npm run dev 

-------------------------------------------------------------

CREATE OR REPLACE VIEW VW_TASKFLOW_DEPARTMENT AS 
SELECT 
id AS department_id,uid AS department_code,name AS department_name,
upper_id AS PARENT_CODE,
-- (SELECT ID FROM tb_department B WHERE B.uid = A.upper_id) AS PARENT_ID,
seq AS sort_order,case when del=0 then 'Y' ELSE 'N' END AS use_yn,
created_date AS created_at, 1 AS created_by , modified_Date AS updated_at,NULL AS updated_by
from tb_department A
WHERE A.del=0;

CREATE OR REPLACE VIEW VW_TASKFLOW_USER AS
SELECT `a`.`id` AS `user_id`,`a`.`uid` AS `username`,`a`.`pw` AS `PASSWORD`,`a`.`full_name` AS `NAME`,`a`.`e_mail` AS `email`,
`a`.`department_id` AS `department_code`,
(SELECT `b`.`name` FROM `tb_department` `b` WHERE `b`.`uid` = `a`.`department_id`) AS `department_name`, 
CASE WHEN `a`.`del` = 0 THEN 'Y' ELSE 'N' END AS `USE_YN`, 
NULL AS `last_login_at`,`a`.`created_date` AS `created_at`,'1' AS `created_by`,`a`.`modified_date` AS `updated_at`, NULL AS `updated_by`
FROM `tb_staff` `a`
WHERE `a`.`uid` in (
SELECT `tb_staff`.`uid`
FROM `tb_staff`
WHERE `tb_staff`.`sync_system` LIKE '%SV001%'
GROUP BY `tb_staff`.`uid`)
ORDER BY `a`.`uid`,`a`.`uid`;

CREATE OR REPLACE VIEW VW_TASKFLOW_POSITION AS
SELECT id AS POSITION_ID, uid AS POSITION_CODE, NAME AS POSITION_NAME, 
COALESCE(seq, 999) AS SORT_ORDER,
CASE WHEN `del` = 0 THEN 'Y' ELSE 'N' END AS `USE_YN`, 
`created_date` AS `created_at`,
null AS `created_by`,
`modified_date` AS `updated_at`, 
NULL AS `updated_by`
FROM tb_position;

SELECT * FROM VW_TASKFLOW_USER;

SELECT * FROM VW_TASKFLOW_DEPARTMENT;

python3 -c "
   import bcrypt
   password = 'admin123'.encode('utf-8')
   salt = bcrypt.gensalt(rounds=10)
   hash = bcrypt.hashpw(password, salt)
   print(hash.decode('utf-8'))
   " 2>/dev/null || echo "Python bcrypt not available"

--------------------------------------------
동기화 기능 추가 개발 
--------------------------------------------
TB_USER 컬럼 추가

  ALTER TABLE TB_USER ADD COLUMN EXTERNAL_USER_ID BIGINT NULL COMMENT '외부 DB USER_ID';
  ALTER TABLE TB_USER ADD COLUMN SYNC_SOURCE VARCHAR(20) DEFAULT 'INTERNAL' COMMENT '데이터 출처 (INTERNAL/EXTERNAL)';
  ALTER TABLE TB_USER ADD COLUMN LAST_SYNCED_AT DATETIME NULL COMMENT '마지막 동기화 시간';

  CREATE UNIQUE INDEX UK_USER_EXTERNAL_ID ON TB_USER(EXTERNAL_USER_ID);

  TB_DEPARTMENT 컬럼 추가

  ALTER TABLE TB_DEPARTMENT ADD COLUMN EXTERNAL_DEPT_ID BIGINT NULL COMMENT '외부 DB DEPARTMENT_ID';
  ALTER TABLE TB_DEPARTMENT ADD COLUMN SYNC_SOURCE VARCHAR(20) DEFAULT 'INTERNAL' COMMENT '데이터 출처';
  ALTER TABLE TB_DEPARTMENT ADD COLUMN LAST_SYNCED_AT DATETIME NULL COMMENT '마지막 동기화 시간';

  CREATE UNIQUE INDEX UK_DEPT_EXTERNAL_ID ON TB_DEPARTMENT(EXTERNAL_DEPT_ID);

    수행된 DB 마이그레이션:
  -- TB_USER에 추가된 컬럼
  EXTERNAL_USER_ID, SYNC_SOURCE, LAST_SYNCED_AT

  -- TB_DEPARTMENT에 추가된 컬럼
  EXTERNAL_DEPT_ID, SYNC_SOURCE, LAST_SYNCED_AT

 3.2 Shadow User 테이블 구조

  TB_USER (수정 후)
  ┌─────────────────┬──────────────┬─────────────────────────────────┐
  │ 컬럼            │ 타입         │ 설명                            │
  ├─────────────────┼──────────────┼─────────────────────────────────┤
  │ USER_ID         │ BIGINT (PK)  │ 내부 자동생성 ID (FK 참조용)    │
  │ USERNAME        │ VARCHAR(50)  │ 로그인 ID (매핑 키, UNIQUE)     │
  │ PASSWORD        │ VARCHAR(255) │ Internal: BCrypt / External: N/A│
  │ NAME            │ VARCHAR(100) │ 사용자 이름                     │
  │ EMAIL           │ VARCHAR(100) │ 이메일                          │
  │ DEPARTMENT_ID   │ BIGINT (FK)  │ 내부 부서 ID (Shadow Dept)      │
  │ USE_YN          │ CHAR(1)      │ 사용 여부                       │
  │ EXTERNAL_USER_ID│ BIGINT       │ 외부 DB의 USER_ID (신규)        │
  │ SYNC_SOURCE     │ VARCHAR(20)  │ INTERNAL / EXTERNAL (신규)      │
  │ LAST_SYNCED_AT  │ DATETIME     │ 마지막 동기화 시간 (신규)       │
  └─────────────────┴──────────────┴─────────────────────────────────┘
  6. 운영 가이드

  6.1 실행 명령

  # 웹 서비스 모드 (기본)
  java -jar taskflow.jar

  # 전체 동기화 모드 (CLI)
  java -jar taskflow.jar --sync

  # 프로파일 지정
  java -jar taskflow.jar --spring.profiles.active=prod --sync

  6.2 Cron 설정 예시

  # 매일 새벽 2시 전체 동기화
  0 2 * * * /usr/bin/java -jar /app/taskflow.jar --sync >> /var/log/taskflow-sync.log 2>&1

  # 매 6시간마다 동기화
  0 */6 * * * /usr/bin/java -jar /app/taskflow.jar --sync >> /var/log/taskflow-sync.log 2>&1

  6.3 모드 전환 (External → Internal)

  External 모드에서 Internal 모드로 전환 시:
  1. SYNC_SOURCE='EXTERNAL'인 사용자들은 그대로 유지
  2. 해당 사용자들의 비밀번호는 NULL 상태
  3. 관리자가 수동으로 비밀번호 초기화 필요
  4. 또는 비밀번호 찾기 기능 활용

  -- 외부 동기화 사용자 비밀번호 초기화 (선택적)
  UPDATE TB_USER
  SET PASSWORD = '$2a$10$...' -- BCrypt 해시
  WHERE SYNC_SOURCE = 'EXTERNAL' AND PASSWORD IS NULL;

  External DB 동기화 기능 구현이 완료되었습니다.

  완료된 작업 요약
  | 구분                  | 작업 내용                                                                     |
  |-----------------------|-------------------------------------------------------------------------------|
  | DB 스키마             | TB_USER, TB_DEPARTMENT에 EXTERNAL_*_ID, SYNC_SOURCE, LAST_SYNCED_AT 컬럼 추가 |
  | Domain                | User.java, Department.java에 동기화 필드 및 헬퍼 메서드 추가                  |
  | Mapper                | Shadow User/Department CRUD 쿼리 추가                                         |
  | DepartmentSyncService | 부서 동기화 서비스 구현 (계층 구조 지원)                                      |
  | UserSyncService       | 사용자 동기화 서비스 구현                                                     |
  | AuthServiceImpl       | 로그인 시 Shadow User 자동 동기화                                             |
  | CLI 모드              | --sync 옵션으로 전체 동기화 배치 실행                                         |
  | 문서                  | external-db-setup.md 갱신                                                     |

 테스트 결과 요약

  | 테스트 항목                     | 결과                     |
  |---------------------------------|--------------------------|
  | 모드 전환 (external → internal) | ✅ 성공                  |
  | admin 로그인 (BCrypt)           | ✅ 성공                  |
  | 사용자 목록 API                 | ✅ 2명 조회              |
  | 부서 목록 API                   | ✅ 계층 구조 정상        |
  | 보드 조회/생성 API              | ✅ 성공                  |
  | 아이템 생성 API                 | ✅ 성공                  |
  | 사용자 생성 (CRUD)              | ✅ testuser2 생성 성공   |
  | 신규 사용자 로그인              | ✅ testuser2 로그인 성공 |

  수정된 파일

  - UserGroupMapper.xml - 메서드 이름 불일치 수정:
    - findByUserId → findByUsername
    - existsByUserIdAndGroupId → existsByUsernameAndGroupId
    - deleteByGroupIdAndUserId → deleteByGroupIdAndUsername
    - deleteByUserId → deleteByUsername

  모드별 동작 차이

  | 기능            | External 모드 | Internal 모드 |
  |-----------------|---------------|---------------|
  | 비밀번호 인코더 | SHA256        | BCrypt        |
  | 사용자 CRUD     | ❌ 비활성화   | ✅ 활성화     |
  | 부서 CRUD       | ❌ 비활성화   | ✅ 활성화     |
  | 외부 DB 연동    | ✅ 활성화     | ❌ 비활성화   |


 공유 사용자 추가시 에러 발생 로그 확인 필요

   ---
  External 모드 VIEW 테이블 생성 가이드

  환경 설정 (.env)

  USER_MANAGEMENT_MODE=external

  # 외부 DB 연결 정보
  EXTERNAL_DB_URL=jdbc:mysql://외부DB주소:3306/외부DB명?useSSL=false&serverTimezone=Asia/Seoul
  EXTERNAL_DB_USERNAME=계정
  EXTERNAL_DB_PASSWORD=비밀번호

  # VIEW 테이블명
  EXTERNAL_USER_TABLE=VW_TASKFLOW_USER
  EXTERNAL_DEPARTMENT_TABLE=VW_TASKFLOW_DEPARTMENT
  EXTERNAL_POSITION_TABLE=VW_TASKFLOW_POSITION

  ---
  VIEW 1: VW_TASKFLOW_USER (사용자)

  -- ============================================================
  -- VIEW: VW_TASKFLOW_USER
  -- 설명: TaskFlow 사용자 정보 VIEW
  -- ============================================================
  CREATE OR REPLACE VIEW VW_TASKFLOW_USER AS
  SELECT
      -- 필수 컬럼 (NOT NULL)
      emp.EMP_ID          AS USER_ID,           -- BIGINT, PK
      emp.LOGIN_ID        AS USERNAME,          -- VARCHAR(50), 로그인 아이디
      emp.PASSWORD        AS PASSWORD,          -- VARCHAR(255), SHA256 해시
      emp.EMP_NAME        AS NAME,              -- VARCHAR(100), 사용자명

      -- 선택 컬럼 (NULL 허용)
      emp.EMAIL           AS EMAIL,             -- VARCHAR(255)
      dept.DEPT_CD        AS DEPARTMENT_CODE,   -- VARCHAR(50), 부서 코드
      dept.DEPT_NM        AS DEPARTMENT_NAME,   -- VARCHAR(100), 부서명

      -- 사용 여부
      CASE
          WHEN emp.STATUS = 'ACTIVE' THEN 'Y'
          ELSE 'N'
      END                 AS USE_YN,            -- CHAR(1)

      -- 감사 컬럼
      emp.LAST_LOGIN_AT   AS LAST_LOGIN_AT,     -- DATETIME
      emp.REG_DATE        AS CREATED_AT,        -- DATETIME
      emp.REG_USER_ID     AS CREATED_BY,        -- VARCHAR(50)
      emp.MOD_DATE        AS UPDATED_AT,        -- DATETIME
      emp.MOD_USER_ID     AS UPDATED_BY         -- VARCHAR(50)

  FROM TB_EMPLOYEE emp
  LEFT JOIN TB_DEPARTMENT dept ON emp.DEPT_ID = dept.DEPT_ID;

  컬럼 명세

  | 컬럼            | 타입         | 필수 | 설명                         |
  |-----------------|--------------|------|------------------------------|
  | USER_ID         | BIGINT       |  O   | 사용자 PK                    |
  | USERNAME        | VARCHAR(50)  |  O   | 로그인 아이디 (FK 참조 기준) |
  | PASSWORD        | VARCHAR(255) |  O   | SHA256 해시 비밀번호         |
  | NAME            | VARCHAR(100) |  O   | 사용자명                     |
  | EMAIL           | VARCHAR(255) |  -   | 이메일                       |
  | DEPARTMENT_CODE | VARCHAR(50)  |  -   | 부서 코드                    |
  | DEPARTMENT_NAME | VARCHAR(100) |  -   | 부서명                       |
  | USE_YN          | CHAR(1)      |  O   | 사용 여부 ('Y'/'N')          |
  | LAST_LOGIN_AT   | DATETIME     |  -   | 최종 로그인 시간             |
  | CREATED_AT      | DATETIME     |  O   | 생성일시                     |
  | CREATED_BY      | VARCHAR(50)  |  -   | 생성자                       |
  | UPDATED_AT      | DATETIME     |  -   | 수정일시                     |
  | UPDATED_BY      | VARCHAR(50)  |  -   | 수정자                       |

  ---
  VIEW 2: VW_TASKFLOW_DEPARTMENT (부서)

  -- ============================================================
  -- VIEW: VW_TASKFLOW_DEPARTMENT
  -- 설명: TaskFlow 부서 정보 VIEW
  -- ============================================================
  CREATE OR REPLACE VIEW VW_TASKFLOW_DEPARTMENT AS
  SELECT
      -- 필수 컬럼 (NOT NULL)
      dept.DEPT_ID        AS DEPARTMENT_ID,     -- BIGINT, PK
      dept.DEPT_CD        AS DEPARTMENT_CODE,   -- VARCHAR(50), 부서 코드 (FK 참조 기준)
      dept.DEPT_NM        AS DEPARTMENT_NAME,   -- VARCHAR(100), 부서명

      -- 계층 구조
      parent.DEPT_CD      AS PARENT_CODE,       -- VARCHAR(50), 상위 부서 코드 (NULL=최상위)

      -- 정렬/사용
      COALESCE(dept.SORT_ORD, 0) AS SORT_ORDER, -- INT, 정렬 순서
      CASE
          WHEN dept.USE_FLAG = '1' THEN 'Y'
          ELSE 'N'
      END                 AS USE_YN,            -- CHAR(1)

      -- 감사 컬럼
      dept.REG_DATE       AS CREATED_AT,        -- DATETIME
      dept.REG_USER_ID    AS CREATED_BY,        -- VARCHAR(50)
      dept.MOD_DATE       AS UPDATED_AT,        -- DATETIME
      dept.MOD_USER_ID    AS UPDATED_BY         -- VARCHAR(50)

  FROM TB_DEPARTMENT dept
  LEFT JOIN TB_DEPARTMENT parent ON dept.PARENT_DEPT_ID = parent.DEPT_ID;

  컬럼 명세

  | 컬럼            | 타입         | 필수 | 설명                         |
  |-----------------|--------------|------|------------------------------|
  | DEPARTMENT_ID   | BIGINT       |  O   | 부서 PK                      |
  | DEPARTMENT_CODE | VARCHAR(50)  |  O   | 부서 코드 (FK 참조 기준)     |
  | DEPARTMENT_NAME | VARCHAR(100) |  O   | 부서명                       |
  | PARENT_CODE     | VARCHAR(50)  |  -   | 상위 부서 코드 (NULL=최상위) |
  | SORT_ORDER      | INT          |  O   | 정렬 순서                    |
  | USE_YN          | CHAR(1)      |  O   | 사용 여부 ('Y'/'N')          |
  | CREATED_AT      | DATETIME     |  O   | 생성일시                     |
  | CREATED_BY      | VARCHAR(50)  |  -   | 생성자                       |
  | UPDATED_AT      | DATETIME     |  -   | 수정일시                     |
  | UPDATED_BY      | VARCHAR(50)  |  -   | 수정자                       |

  ---
  VIEW 3: VW_TASKFLOW_POSITION (직급)

  -- ============================================================
  -- VIEW: VW_TASKFLOW_POSITION
  -- 설명: TaskFlow 직급 정보 VIEW
  -- ============================================================
  CREATE OR REPLACE VIEW VW_TASKFLOW_POSITION AS
  SELECT
      -- 필수 컬럼 (NOT NULL)
      pos.POS_ID          AS POSITION_ID,       -- BIGINT, PK
      pos.POS_CD          AS POSITION_CODE,     -- VARCHAR(50), 직급 코드 (FK 참조 기준)
      pos.POS_NM          AS POSITION_NAME,     -- VARCHAR(100), 직급명

      -- 정렬/사용
      COALESCE(pos.SORT_ORD, 999) AS SORT_ORDER, -- INT, 정렬 순서 (낮을수록 높은 직급)
      CASE
          WHEN pos.USE_FLAG = '1' THEN 'Y'
          ELSE 'N'
      END                 AS USE_YN,            -- CHAR(1)

      -- 감사 컬럼
      pos.REG_DATE        AS CREATED_AT,        -- DATETIME
      pos.REG_USER_ID     AS CREATED_BY,        -- VARCHAR(50)
      pos.MOD_DATE        AS UPDATED_AT,        -- DATETIME
      pos.MOD_USER_ID     AS UPDATED_BY         -- VARCHAR(50)

  FROM TB_POSITION pos;

  컬럼 명세

  | 컬럼          | 타입         | 필수 | 설명                           |
  |---------------|--------------|------|--------------------------------|
  | POSITION_ID   | BIGINT       |  O   | 직급 PK                        |
  | POSITION_CODE | VARCHAR(50)  |  O   | 직급 코드 (FK 참조 기준)       |
  | POSITION_NAME | VARCHAR(100) |  O   | 직급명                         |
  | SORT_ORDER    | INT          |  O   | 정렬 순서 (낮을수록 높은 직급) |
  | USE_YN        | CHAR(1)      |  O   | 사용 여부 ('Y'/'N')            |
  | CREATED_AT    | DATETIME     |  O   | 생성일시                       |
  | CREATED_BY    | VARCHAR(50)  |  -   | 생성자                         |
  | UPDATED_AT    | DATETIME     |  -   | 수정일시                       |
  | UPDATED_BY    | VARCHAR(50)  |  -   | 수정자                         |

  ---
  비밀번호 해시 형식

  -- MySQL SHA256 함수 (소문자 hex, 64자)
  SELECT SHA2('password123', 256);
  -- 결과: ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f

  ---
  연결 테스트 쿼리

  -- 사용자 확인
  SELECT * FROM VW_TASKFLOW_USER WHERE USE_YN = 'Y' LIMIT 5;

  -- 부서 확인
  SELECT * FROM VW_TASKFLOW_DEPARTMENT ORDER BY SORT_ORDER;

  -- 직급 확인
  SELECT * FROM VW_TASKFLOW_POSITION ORDER BY SORT_ORDER;

  -- 부서별 사용자 수
  SELECT
      d.DEPARTMENT_NAME,
      COUNT(u.USER_ID) AS USER_COUNT
  FROM VW_TASKFLOW_DEPARTMENT d
  LEFT JOIN VW_TASKFLOW_USER u ON d.DEPARTMENT_CODE = u.DEPARTMENT_CODE
  WHERE d.USE_YN = 'Y'
  GROUP BY d.DEPARTMENT_CODE, d.DEPARTMENT_NAME
  ORDER BY d.SORT_ORDER;

-----------------------------------------------
perfect(당일) , Excellent(종료일 기준 50%) , Great(종료일 기준 80%) , good(마감일 준수) , fair(2일이내 초과) , poor (3일 이상 초과)
총기간 = 종료일 - 시작일
완료비율 = (완료일 - 시작일) / 총기간
완료비율 ≤ 0 → Perfect
≤ 0.5 → Excellent
≤ 0.8 → Great
≤ 1.0 → Good
≤ 1.0 + 2일 → Fair
그 외 → Poor

난이도 : Low(반복작업,0.9) , Medium(일반,1.0) , High(높음,1.2) , Extreme(어려움,1.4)
범위 번경 : None(변경없음,1.0) , Minor(20%이내,1.05) , Major(30%이상,1.15) , Chaos(엎어짐,1.3)
리스크 대응 : None(이슈없음,1.0) , Mitigated(이슈해결,1.1) , Critical(실패복구,1.25)

최종 점수 = 시간기반 점수 × 난이도 가중치 × 범위변경 가중치 × 리스크대응 가중치

Extreme / Chaos는 PM 승인제
-----------------------------------------------

보드 생성시 기본 category 선택 지정 
추가속성은 category 귀속으로 지정 (기존엔 보드 귀속)
기존 컨셉은 카테고리에서 보드 선택 지정 구조 
변경 컨셉은 카테고리에 속성 등록 , 등록된 카테고리를 보드 & 업무에 지정

# 속성 지정 방식
```
사용자 -> 카테고리 -> 보드 (카테고리 선택,속성 선택)-> 업무 (카테고리 선택,속성 선택)

보드생성 -> 카테고리 지정  --> 업무생성 -> 카테고리 지정 (보드 카테고리 기본 지정 , 생성시 변경 가능)
 └ 보드 생성시 자동 등록되는 속성 표시 
 └ 자동 등록되는 속성중 기본 속성은 제거 불가 추가 속성에 대해서 제거 가능 
 └ 제거 기능은 속성명을 라벨 형식으로 표현후 회색 또는 색조 표현으로 적용 , 제거 선택
기존 보드 -> 카테고리 변경
 └ 보드 카테고리 삭제 없음
 └ 보드 카테고리 변경은 기존 등록된 업무에는 영향 없음 
 └ 신규 업무 등록시 변경된 카테고리 자동 지정 
업무 생성 : 보드 카테고리 자동지정 , 다른 카테고리 변경 선택 가능
 └ 기본 지정 보드 카테고리에서 다른 카테고리 선택시 지정 속성 자동 변경
 └ 업무 생성시 카테고리 기준 자동 등록되는 속성 표시 
 └ 자동 등록되는 속성중 기본 속성은 제거 불가, 추가 속성에 대해서 제거 가능 
 └ 제거 기능은 속성명을 라벨 형식으로 표현후 회색 또는 색조 표현으로 적용 , 제거 선택
기존 업무 category  변경시 기존 사용중인 속성 폐기 신규 속성 적용 
```
---
지연 업무 관리시 완료되지 않은 업무에 대해서만 계산에 포함
방안3와 같이 추가 속성중 "완료일" , "마감일" 포함 
단 , 추가속성중 "완료일" , "마감일" 포함하는 경우와 그렇지 않은 경우에 따라 지연 계산은 다르게함
요청일(기본속성) , 시작일(추가속성) , 완료일(추가속성) , 마감일(기본속성)
---
요청일 , 완료일은 현재일 보다 미래 날짜를 선택할수 없음 
시작일 , 마감일은 현재보다 미래날짜 선택 가능
요청일 시작일은 완료일보다 미래 날짜가 될수 없음 (일정 변경등으로 인해 마감일 보다 시작일은 미래 날짜가 될수 있음)
완료일은 요청일 시작일보다 과거 날짜가 될수 없음
---
요청일(기본속성) , 시작일(추가속성) , 완료일(추가속성) , 마감일(기본속성) 모든속성 미등록시 시작전 속성 유지 
  └ 속성 변경할경우 시작일을 업무 등록일로 변경됨을 경고 
마감일 전 완료되었으나 완료일 미등록시 완료처리일 기준 날짜 계산 (현재날짜로 완료됨 경고)
마감일 또는 완료일 미등록시 완료될 경우 완료 처리일 기준 날짜 계산 (현재날짜로 완료됨 경고)
요청일 또는 시작일 미등록시 완료 처리할경우 완료처리일 기준 날짜 계산 (등록날짜 ~ 현재날짜로 완료됨 경고)

자세한 조건에 따른 시작일 종료일 설정 및 계산 요건 cap_4.jpg 참조하여 알고리즘 생성 필요

# 관리용 메뉴 추가 
1. 업무 상태 정보 관리용 메뉴 필요 
  └ 업무내용 , 카테고리 , 담당자 ,요청일 , 시작일 , 완료일 , 마감일 , 상태
  └ 업무 상태 정보 변경시 상태정보 갱신 
  └ 지연 계산은 날짜 관련 속성 변경시 또는 업무 저장시 
  └ 지연 계산은 사용자 로그인시 보유중인 업무 특히 미종료 업무 대상으로 진행

2. 요청일(기본속성) , 시작일(추가속성) , 완료일(추가속성) , 마감일(기본속성) 에 대한 이력관리 필요 
  └ 향후 프로젝트 변경에 따라 시작일 혹은 요청일 변경될경우 변경 기간 등 표현시 활용
  └ 간트 차트 등으로 표현될경우 시작일 , 요청일 , 마감일 등이 변경된 업무의 경우 변경이력을 간트로 표현 가능한 용도등 
  └ 간트에 한정되지 않으며 변경에 따른 난이도 변경 목적 변경등 변경 사유 확인을 위함 
  └ 각 업무에 대해 속성별 값에 대해 변경시 이력 관리 목적 "업무 생성" , "최초 등록" , "변경" , "완료" 

# 속성관리 및 속성에따른 보드 & 업무 배정 적용 방법

## 필요기능
1. 속성관리에서 보드 선택 기능 제거 -> 사용자별 카테고리 귀속으로 변경
  └ 속성관리에서 보드 선택 없음 (cap_1.jpg)
2. 글로벌 속성 관리 기능 필요
  └ 글로벌 속성은 role admin 관리자만 관리할수 있음
  └ 글로벌 속성은 사용자 및 category 귀속되지 않음
3. 글로벌 속성은 신규 보드 생성시 기본 적용
  └ 글로벌 속성은 생성시 자동으로 보드 및 업무에서 활용 가능
  └ 지정 업무에 대해 강제적용 기능 (부서 혹은 사용자 선택후 보드 -> 업무 선택 지정 강제 등록)
  └ 신규 업무 생성시 또는 기존 업무 category 변경시 글로벌 속성 표시 및 적용
4. 매니저 속성 관리 기능 필요
  └ 매니저 role 속성을 가진 사용자가 만든 속성으로 , 매니저 속성으로 생성된 속성은 매니저가 속한 및 하위 부서 사용자가 속성 사용가능
5. 매니저 속성은 4에서 지정한 범위에 대해 3,4번 룰 동일 하게 적용 
6. category 관리 기능은 사용자 업무 이관과 같이 category & 속성 이관 기능 필요 
  └ 속성은 사용자 관리 속성이며 해당 속성은 category에 귀속하며 category는 보드 및 업무에서 활용

## 역활
1. 속성관리는 관리자용 , 매니저용 , 사용자용 역활 추가
  └ 관리자용은 글로벌 속성 및 전체 보드에 대해 속성 배정 삭제 역활
  └ 매니저는 글로벌 속성 미생성 , 부서기준 현재부서 및 하위부서 사용자들의 보드 속성에 대해 배정 및 삭제 역활
  └ 사용자는 자신이 만든 보드 및 속성만 관리 (글로벌 속성 및 매니저 생성 속성 모두 사용가능)
2. 사용자용은 생성자가 자신의 보드에 분배 및 적용 가능 
3. 전체 사용자는 글로벌 속성을 관리 , 글로벌 속성은 일반 사용자에 의해 등록 불가 , 글로벌 속성은 role 속성 admin 권한자만 생성

