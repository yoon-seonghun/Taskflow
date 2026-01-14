# 업무 담당자 배정 및 권한 관리 설계서

## 1. 개요

### 1.1 목적
업무 담당자 지정 시 해당 담당자에게 업무를 배정하고, 권한 수준에 따라 접근을 제어하며, 이메일/알림을 통해 통보하는 기능을 구현합니다.

### 1.2 주요 기능
- 담당자 지정 시 업무 배정/공유 여부 확인 모달
- 권한 수준별 접근 제어 (조회/편집/전체)
- 배정 시 이메일 및 앱 내 알림 발송
- 공유받은 업무 목록에 배정 업무 포함
- 배정 배지 및 배정자 표시

---

## 2. ERD 및 테이블 정의

### 2.1 기존 테이블 수정: TB_ITEM_SHARE

```sql
-- 기존 TB_ITEM_SHARE 테이블에 컬럼 추가
ALTER TABLE TB_ITEM_SHARE
ADD COLUMN SHARE_TYPE VARCHAR(20) NOT NULL DEFAULT 'SHARE' COMMENT '공유 유형 (SHARE: 공유, ASSIGN: 배당)' AFTER ITEM_ID,
ADD COLUMN PERMISSION_LEVEL VARCHAR(20) NOT NULL DEFAULT 'VIEW' COMMENT '권한 수준 (VIEW: 조회, EDIT: 편집, FULL: 전체)' AFTER SHARE_TYPE,
ADD COLUMN ASSIGNED_BY VARCHAR(50) NULL COMMENT '배당자 USERNAME (ASSIGN일 경우)' AFTER PERMISSION_LEVEL,
ADD COLUMN ASSIGNED_AT DATETIME NULL COMMENT '배당 일시' AFTER ASSIGNED_BY;

-- 인덱스 추가
CREATE INDEX IDX_ITEM_SHARE_TYPE ON TB_ITEM_SHARE(SHARE_TYPE);
CREATE INDEX IDX_ITEM_SHARE_ASSIGNED_BY ON TB_ITEM_SHARE(ASSIGNED_BY);
```

### 2.2 권한 수준 정의

| 권한 | 코드 | 조회 | 댓글 | 사용자속성 편집 | 기본속성 편집 | 제목 편집 |
|------|------|------|------|----------------|---------------|-----------|
| 조회 | VIEW | ✅ | ✅ | ❌ | ❌ | ❌ |
| 편집 | EDIT | ✅ | ✅ | ✅ | ❌ | ❌ |
| 전체 | FULL | ✅ | ✅ | ✅ | ✅ | ❌ |
| 소유자 | OWNER | ✅ | ✅ | ✅ | ✅ | ✅ |

### 2.3 공유 유형 정의

| 유형 | 코드 | 설명 | 배지 표시 |
|------|------|------|-----------|
| 공유 | SHARE | 일반 공유 (기존 기능) | [공유] 공유자명 |
| 배당 | ASSIGN | 담당자 배정을 통한 공유 | [배당] 배당자명 |

### 2.4 신규 테이블: TB_NOTIFICATION

```sql
CREATE TABLE TB_NOTIFICATION (
    NOTIFICATION_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    USERNAME VARCHAR(50) NOT NULL COMMENT '수신자 USERNAME',
    NOTIFICATION_TYPE VARCHAR(50) NOT NULL COMMENT '알림 유형',
    TITLE VARCHAR(200) NOT NULL COMMENT '알림 제목',
    MESSAGE TEXT NULL COMMENT '알림 내용',
    RELATED_TYPE VARCHAR(50) NULL COMMENT '관련 대상 유형 (ITEM, BOARD 등)',
    RELATED_ID BIGINT NULL COMMENT '관련 대상 ID',
    RELATED_URL VARCHAR(500) NULL COMMENT '관련 링크 URL',
    IS_READ TINYINT(1) NOT NULL DEFAULT 0 COMMENT '읽음 여부',
    READ_AT DATETIME NULL COMMENT '읽은 시간',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR(50) NOT NULL COMMENT '발신자 USERNAME',

    INDEX IDX_NOTIFICATION_USER (USERNAME, IS_READ, CREATED_AT DESC),
    INDEX IDX_NOTIFICATION_RELATED (RELATED_TYPE, RELATED_ID),
    INDEX IDX_NOTIFICATION_TYPE (NOTIFICATION_TYPE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림';
```

### 2.5 알림 유형 정의

| 유형 코드 | 설명 | 제목 예시 |
|-----------|------|-----------|
| ITEM_ASSIGNED | 업무 배당 알림 | "새로운 업무가 배당되었습니다" |
| ITEM_SHARED | 업무 공유 알림 | "업무가 공유되었습니다" |
| ITEM_UPDATED | 배당 업무 수정 알림 | "배당받은 업무가 수정되었습니다" |
| ITEM_COMPLETED | 배당 업무 완료 알림 | "배당한 업무가 완료되었습니다" |

---

## 3. API 명세

### 3.1 담당자 배정 API

```
POST /api/boards/{boardId}/items/{itemId}/assign

Description: 업무에 담당자를 배정하고 선택적으로 공유/알림 처리

Request Body:
{
  "assigneeUsername": "user1",      // 담당자 USERNAME (필수)
  "permissionLevel": "EDIT",        // 권한 수준: VIEW, EDIT, FULL (필수)
  "sendEmail": true,                // 이메일 발송 여부 (기본: true)
  "sendNotification": true          // 앱 알림 발송 여부 (기본: true)
}

Response 200:
{
  "success": true,
  "data": {
    "shareId": 456,
    "itemId": 123,
    "assigneeUsername": "user1",
    "assigneeName": "홍길동",
    "shareType": "ASSIGN",
    "permissionLevel": "EDIT",
    "assignedBy": "admin",
    "assignedByName": "관리자",
    "assignedAt": "2024-01-15T10:30:00",
    "emailSent": true,
    "notificationSent": true
  }
}

Response 400:
- 자기 자신에게 배당 불가
- 업무 소유자에게 배당 불가
- 이미 배당된 사용자

Response 403:
- 업무 소유자만 배정 가능
```

### 3.2 배정 취소 API

```
DELETE /api/boards/{boardId}/items/{itemId}/assign/{username}

Description: 업무 배정 취소 (공유 해제)

Response 204: 성공 (No Content)

Response 403:
- 업무 소유자만 취소 가능
```

### 3.3 배정 권한 수정 API

```
PUT /api/boards/{boardId}/items/{itemId}/assign/{username}

Description: 배정된 사용자의 권한 수준 변경

Request Body:
{
  "permissionLevel": "FULL"
}

Response 200:
{
  "success": true,
  "data": {
    "shareId": 456,
    "permissionLevel": "FULL",
    "updatedAt": "2024-01-15T11:00:00"
  }
}
```

### 3.4 알림 API

```
# 내 알림 목록
GET /api/notifications?page=0&size=20&unreadOnly=false

Response:
{
  "success": true,
  "data": {
    "content": [
      {
        "notificationId": 1,
        "notificationType": "ITEM_ASSIGNED",
        "title": "새로운 업무가 배당되었습니다",
        "message": "관리자님이 '서버 점검 작업' 업무를 배당했습니다.",
        "relatedType": "ITEM",
        "relatedId": 123,
        "relatedUrl": "/boards/1/items/123",
        "isRead": false,
        "createdAt": "2024-01-15T10:30:00",
        "createdBy": "admin",
        "createdByName": "관리자"
      }
    ],
    "totalElements": 50,
    "totalPages": 3,
    "number": 0
  }
}

# 알림 읽음 처리
PUT /api/notifications/{id}/read

Response 200:
{
  "success": true,
  "data": {
    "notificationId": 1,
    "isRead": true,
    "readAt": "2024-01-15T10:35:00"
  }
}

# 전체 읽음 처리
PUT /api/notifications/read-all

Response 200:
{
  "success": true,
  "data": {
    "updatedCount": 15
  }
}

# 읽지 않은 알림 수
GET /api/notifications/unread-count

Response:
{
  "success": true,
  "data": {
    "count": 5
  }
}
```

### 3.5 기존 API 수정

#### 3.5.1 업무 조회 응답 확장

```
GET /api/boards/{boardId}/items/{itemId}

Response 추가 필드:
{
  ...기존 필드...,

  // 현재 사용자의 접근 권한 정보
  "accessInfo": {
    "isOwner": false,
    "shareType": "ASSIGN",           // null(소유자), SHARE, ASSIGN
    "permissionLevel": "EDIT",       // null(소유자), VIEW, EDIT, FULL
    "assignedBy": "admin",           // 배당자 USERNAME (ASSIGN일 경우)
    "assignedByName": "관리자",      // 배당자 이름
    "assignedAt": "2024-01-15T10:30:00",

    // 편집 가능 여부 (프론트엔드 UI 제어용)
    "canEditTitle": false,
    "canEditBasicProperties": true,
    "canEditUserProperties": true,
    "canAssign": false,
    "canDelete": false
  }
}
```

#### 3.5.2 공유받은 업무 목록 확장

```
GET /api/items/shared?shareType=ALL&page=0&size=20

Query Parameters:
- shareType: ALL(전체), SHARE(공유만), ASSIGN(배당만)
- page, size: 페이징

Response:
{
  "success": true,
  "data": {
    "content": [
      {
        ...업무 정보...,
        "shareType": "ASSIGN",
        "permissionLevel": "EDIT",
        "assignedBy": "admin",
        "assignedByName": "관리자",
        "assignedAt": "2024-01-15T10:30:00"
      }
    ]
  }
}
```

---

## 4. 컴포넌트 구조

### 4.1 신규 컴포넌트

```
src/components/
├── item/
│   ├── AssignConfirmModal.vue      # 담당자 배정 확인 모달
│   └── ItemBadges.vue              # 수정 (배당 배지 추가)
│
├── notification/
│   ├── NotificationDropdown.vue    # 헤더 알림 드롭다운
│   ├── NotificationItem.vue        # 개별 알림 아이템
│   └── NotificationList.vue        # 알림 목록 (전체보기용)
│
└── layout/
    └── Header.vue                  # 수정 (알림 아이콘 추가)
```

### 4.2 AssignConfirmModal.vue 상세

```
┌─────────────────────────────────────────────────┐
│  업무 배정                                  [X] │
├─────────────────────────────────────────────────┤
│                                                 │
│  👤 홍길동 님에게 업무를 배정하시겠습니까?        │
│                                                 │
│  📋 업무: [업무 제목 표시]                       │
│                                                 │
│  ┌─ 권한 수준 ───────────────────────────────┐  │
│  │                                           │  │
│  │ ○ 조회                                    │  │
│  │   업무 내용 조회 및 댓글 작성만 가능         │  │
│  │                                           │  │
│  │ ● 편집 (권장)                             │  │
│  │   사용자 정의 속성 편집 가능                │  │
│  │                                           │  │
│  │ ○ 전체                                    │  │
│  │   기본 속성(상태, 우선순위 등) 편집 가능     │  │
│  │   ※ 제목은 소유자만 수정 가능              │  │
│  │                                           │  │
│  └───────────────────────────────────────────┘  │
│                                                 │
│  ☑ 이메일로 알림 발송                           │
│  ☑ 앱 내 알림 발송                              │
│                                                 │
├─────────────────────────────────────────────────┤
│                    [취소]  [배정하기]            │
└─────────────────────────────────────────────────┘
```

### 4.3 NotificationDropdown.vue 상세

```
┌──────────────────────────────────────┐
│  🔔 알림 (5)                    모두 읽음 │
├──────────────────────────────────────┤
│  ┌────────────────────────────────┐  │
│  │ 🔵 새로운 업무가 배당되었습니다   │  │
│  │    관리자님이 '서버 점검' 배당    │  │
│  │    5분 전                       │  │
│  └────────────────────────────────┘  │
│  ┌────────────────────────────────┐  │
│  │ ⚪ 업무가 공유되었습니다         │  │
│  │    홍길동님이 '보고서 작성' 공유  │  │
│  │    1시간 전                     │  │
│  └────────────────────────────────┘  │
│  ┌────────────────────────────────┐  │
│  │ ⚪ 배당한 업무가 완료되었습니다   │  │
│  │    '데이터 정리' 완료            │  │
│  │    3시간 전                     │  │
│  └────────────────────────────────┘  │
├──────────────────────────────────────┤
│           [전체 알림 보기]            │
└──────────────────────────────────────┘
```

### 4.4 배지 표시 (ItemBadges.vue 수정)

```
현재 배지 유형:
┌──────────────────────────────────────────────────┐
│ [🔗 공유] 홍길동    ← 공유받은 업무 (SHARE)        │
│ [↔️ 이관] 홍길동    ← 이관받은 업무 (기존)         │
│ [📌 배당] 홍길동    ← 배당받은 업무 (ASSIGN) 신규  │
└──────────────────────────────────────────────────┘

배지 색상:
- 공유: bg-blue-100 text-blue-700
- 이관: bg-purple-100 text-purple-700
- 배당: bg-green-100 text-green-700
```

---

## 5. 권한별 UI 제어

### 5.1 권한 매트릭스

| 기능 | VIEW | EDIT | FULL | OWNER |
|------|------|------|------|-------|
| 업무 조회 | ✅ | ✅ | ✅ | ✅ |
| 댓글 작성/수정 | ✅ | ✅ | ✅ | ✅ |
| 파일 첨부 | ❌ | ✅ | ✅ | ✅ |
| 사용자 속성 편집 | ❌ | ✅ | ✅ | ✅ |
| 상태 변경 | ❌ | ❌ | ✅ | ✅ |
| 우선순위 변경 | ❌ | ❌ | ✅ | ✅ |
| 마감일 변경 | ❌ | ❌ | ✅ | ✅ |
| 카테고리 변경 | ❌ | ❌ | ✅ | ✅ |
| 그룹 변경 | ❌ | ❌ | ✅ | ✅ |
| 제목 편집 | ❌ | ❌ | ❌ | ✅ |
| 담당자 배정 | ❌ | ❌ | ❌ | ✅ |
| 업무 공유 | ❌ | ❌ | ❌ | ✅ |
| 업무 이관 | ❌ | ❌ | ❌ | ✅ |
| 업무 삭제 | ❌ | ❌ | ❌ | ✅ |
| 완료 처리 | ❌ | ✅ | ✅ | ✅ |

### 5.2 기본 속성 정의

다음 속성은 "기본 속성"으로 분류되어 FULL 권한 이상만 편집 가능:
- 상태 (status)
- 우선순위 (priority)
- 요청일 (requestDate)
- 마감일 (dueDate)
- 카테고리 (categoryId)
- 그룹 (groupId)
- 담당자 (assigneeUsername) - 소유자만 변경 가능

### 5.3 프론트엔드 구현 방식

```typescript
// composables/useItemPermission.ts
export function useItemPermission(item: Ref<Item>) {
  const authStore = useAuthStore()

  const accessInfo = computed(() => item.value?.accessInfo)

  const isOwner = computed(() => accessInfo.value?.isOwner ?? false)

  const canEditTitle = computed(() =>
    accessInfo.value?.canEditTitle ?? isOwner.value
  )

  const canEditBasicProperties = computed(() =>
    accessInfo.value?.canEditBasicProperties ?? isOwner.value
  )

  const canEditUserProperties = computed(() =>
    accessInfo.value?.canEditUserProperties ?? isOwner.value
  )

  const canAssign = computed(() =>
    accessInfo.value?.canAssign ?? isOwner.value
  )

  const canDelete = computed(() =>
    accessInfo.value?.canDelete ?? isOwner.value
  )

  // 특정 속성 편집 가능 여부
  const canEditProperty = (propertyCode: string) => {
    const basicProperties = [
      'status', 'priority', 'requestDate', 'dueDate',
      'categoryId', 'groupId', 'assigneeUsername'
    ]

    if (propertyCode === 'title') return canEditTitle.value
    if (basicProperties.includes(propertyCode)) return canEditBasicProperties.value
    return canEditUserProperties.value
  }

  return {
    isOwner,
    canEditTitle,
    canEditBasicProperties,
    canEditUserProperties,
    canAssign,
    canDelete,
    canEditProperty
  }
}
```

---

## 6. 이메일 템플릿

### 6.1 업무 배당 알림 이메일

```
제목: [TaskFlow] 새로운 업무가 배당되었습니다

───────────────────────────────────────

안녕하세요, {수신자명}님

{배당자명}님이 새로운 업무를 배당했습니다.

📋 업무 정보
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• 업무명: {업무 제목}
• 상태: {상태}
• 우선순위: {우선순위}
• 마감일: {마감일}
• 권한: {권한 수준}

[업무 확인하기] ← 링크 버튼

───────────────────────────────────────
이 메일은 TaskFlow에서 자동 발송되었습니다.
```

---

## 7. 시퀀스 다이어그램

### 7.1 담당자 배정 흐름

```
사용자          Frontend           Backend            DB              Email/Noti
  │                │                  │                │                  │
  │ 담당자 선택     │                  │                │                  │
  ├───────────────>│                  │                │                  │
  │                │                  │                │                  │
  │ 배정 모달 표시  │                  │                │                  │
  │<───────────────┤                  │                │                  │
  │                │                  │                │                  │
  │ 권한/알림 선택  │                  │                │                  │
  │ 배정 확인      │                  │                │                  │
  ├───────────────>│                  │                │                  │
  │                │                  │                │                  │
  │                │ POST /assign     │                │                  │
  │                ├─────────────────>│                │                  │
  │                │                  │                │                  │
  │                │                  │ 담당자 업데이트  │                  │
  │                │                  ├───────────────>│                  │
  │                │                  │                │                  │
  │                │                  │ 공유 레코드 생성 │                  │
  │                │                  ├───────────────>│                  │
  │                │                  │                │                  │
  │                │                  │ 알림 레코드 생성 │                  │
  │                │                  ├───────────────>│                  │
  │                │                  │                │                  │
  │                │                  │ 이메일 발송      │                  │
  │                │                  ├─────────────────────────────────>│
  │                │                  │                │                  │
  │                │                  │ SSE 알림 Push   │                  │
  │                │                  ├─────────────────────────────────>│
  │                │                  │                │                  │
  │                │ Response         │                │                  │
  │                │<─────────────────┤                │                  │
  │                │                  │                │                  │
  │ 성공 메시지    │                  │                │                  │
  │<───────────────┤                  │                │                  │
  │                │                  │                │                  │
```

---

## 8. 구현 순서

### Phase 1: 백엔드 기반 구축
1. DB 스키마 수정 (TB_ITEM_SHARE 컬럼 추가)
2. DB 스키마 생성 (TB_NOTIFICATION)
3. Domain, DTO 클래스 수정/생성
4. Mapper XML 수정/생성
5. Service 구현 (ItemAssignmentService, NotificationService)
6. Controller 구현 (ItemAssignmentController, NotificationController)

### Phase 2: 백엔드 알림 연동
7. 이메일 템플릿 생성
8. 이메일 발송 연동 (기존 EmailService 활용)
9. SSE 알림 Push 구현

### Phase 3: 프론트엔드 구현
10. API 클라이언트 추가 (assignment.ts, notification.ts)
11. 타입 정의 추가
12. Pinia 스토어 추가 (notification.ts)
13. AssignConfirmModal.vue 컴포넌트 생성
14. ItemBadges.vue 수정 (배당 배지)
15. useItemPermission composable 생성
16. ItemDetailPanel.vue 권한 제어 적용
17. NotificationDropdown.vue 컴포넌트 생성
18. Header.vue 알림 아이콘 추가

### Phase 4: 통합 및 테스트
19. 담당자 선택 시 모달 트리거 연동
20. 권한별 UI 제어 테스트
21. 이메일/알림 발송 테스트
22. 공유받은 업무 목록 테스트

---

## 9. 파일 목록

### 9.1 백엔드

```
# 신규 생성
backend/src/main/java/com/taskflow/domain/Notification.java
backend/src/main/java/com/taskflow/dto/assignment/AssignmentRequest.java
backend/src/main/java/com/taskflow/dto/assignment/AssignmentResponse.java
backend/src/main/java/com/taskflow/dto/notification/NotificationResponse.java
backend/src/main/java/com/taskflow/mapper/NotificationMapper.java
backend/src/main/java/com/taskflow/service/ItemAssignmentService.java
backend/src/main/java/com/taskflow/service/NotificationService.java
backend/src/main/java/com/taskflow/service/impl/ItemAssignmentServiceImpl.java
backend/src/main/java/com/taskflow/service/impl/NotificationServiceImpl.java
backend/src/main/java/com/taskflow/controller/NotificationController.java
backend/src/main/resources/mapper/NotificationMapper.xml

# 수정
backend/src/main/java/com/taskflow/domain/ItemShare.java
backend/src/main/java/com/taskflow/dto/item/ItemResponse.java
backend/src/main/java/com/taskflow/mapper/ItemShareMapper.java
backend/src/main/java/com/taskflow/controller/ItemController.java
backend/src/main/resources/mapper/ItemShareMapper.xml

# DB 마이그레이션
docker/mysql/init/12_item_assignment.sql
```

### 9.2 프론트엔드

```
# 신규 생성
frontend/src/api/assignment.ts
frontend/src/api/notification.ts
frontend/src/types/assignment.ts
frontend/src/types/notification.ts
frontend/src/stores/notification.ts
frontend/src/composables/useItemPermission.ts
frontend/src/components/item/AssignConfirmModal.vue
frontend/src/components/notification/NotificationDropdown.vue
frontend/src/components/notification/NotificationItem.vue

# 수정
frontend/src/types/item.ts
frontend/src/components/item/ItemBadges.vue
frontend/src/components/item/ItemDetailPanel.vue
frontend/src/components/layout/Header.vue
```

---

## 10. 버전 정보

| 항목 | 내용 |
|------|------|
| 문서 버전 | 1.0 |
| 작성일 | 2024-01-15 |
| 상태 | 설계 완료, 승인 대기 |
