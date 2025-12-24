# TaskFlow v1.1 API 명세서

> 작성일: 2024-12-21
> 버전: 1.1
> 이전 버전: v1.0 (docs/api/api-specification.md)

---

## 목차

1. [개요](#1-개요)
2. [신규 API](#2-신규-api)
   - 2.1 [보드 관리 확장 API](#21-보드-관리-확장-api)
   - 2.2 [업무 공유 API](#22-업무-공유-api)
   - 2.3 [감사 로그 API](#23-감사-로그-api)
3. [수정 API](#3-수정-api)
   - 3.1 [보드 API 수정사항](#31-보드-api-수정사항)
   - 3.2 [보드 공유 API 수정사항](#32-보드-공유-api-수정사항)
4. [DTO 명세](#4-dto-명세)
5. [에러 코드](#5-에러-코드)
6. [변경 요약](#6-변경-요약)

---

## 1. 개요

### 1.1 변경 목적
- 보드 관리 기능 강화 (목록 분리, 정렬, 이관 삭제)
- 세분화된 권한 체계 (VIEW/EDIT/FULL/OWNER)
- 업무 단위 공유 기능
- 통합 감사 로그 조회

### 1.2 API 변경 요약

| 구분 | API | 메소드 | 설명 |
|------|-----|--------|------|
| 신규 | /api/boards/list | GET | 보드 목록 (소유/공유 분리) |
| 신규 | /api/boards/{id}/order | PUT | 보드 순서 변경 |
| 신규 | /api/boards/{id}/with-transfer | DELETE | 보드 삭제 (이관 포함) |
| 신규 | /api/boards/{id}/transfer-preview | GET | 이관 대상 미리보기 |
| 신규 | /api/boards/{id}/shares/{userId} | PUT | 공유 권한 변경 |
| 신규 | /api/items/{id}/shares | GET | 업무 공유 목록 |
| 신규 | /api/items/{id}/shares | POST | 업무 공유 추가 |
| 신규 | /api/items/{id}/shares/{userId} | PUT | 업무 공유 권한 변경 |
| 신규 | /api/items/{id}/shares/{userId} | DELETE | 업무 공유 제거 |
| 신규 | /api/audit-logs | GET | 감사 로그 목록 |
| 신규 | /api/audit-logs/recent | GET | 최근 감사 로그 |
| 신규 | /api/audit-logs/boards/{id} | GET | 보드별 감사 로그 |
| 신규 | /api/audit-logs/items/{id} | GET | 업무별 감사 로그 |
| 수정 | /api/boards | GET | 응답에 sortOrder, color, defaultViewType 추가 |
| 수정 | /api/boards | POST | 요청에 sortOrder, color, defaultViewType 추가 |
| 수정 | /api/boards/{id}/shares | POST | permission 값 변경 |

---

## 2. 신규 API

### 2.1 보드 관리 확장 API

#### 2.1.1 보드 목록 조회 (소유/공유 분리)

내가 소유한 보드와 공유받은 보드를 분리하여 조회

**Request**
```
GET /api/boards/list
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "ownedBoards": [
      {
        "boardId": 1,
        "boardName": "업무 관리",
        "description": "기본 업무 관리 보드",
        "color": "#3B82F6",
        "defaultViewType": "TABLE",
        "sortOrder": 1,
        "useYn": "Y",
        "ownerYn": "Y",
        "itemCount": 25,
        "activeItemCount": 20,
        "shareCount": 3,
        "createdAt": "2024-12-01T09:00:00"
      }
    ],
    "sharedBoards": [
      {
        "boardId": 3,
        "boardName": "팀 프로젝트",
        "description": "팀 공유 보드",
        "color": "#10B981",
        "defaultViewType": "KANBAN",
        "sortOrder": 0,
        "useYn": "Y",
        "ownerYn": "N",
        "ownerId": 2,
        "ownerName": "김철수",
        "permission": "EDIT",
        "itemCount": 15,
        "activeItemCount": 10,
        "createdAt": "2024-12-05T10:00:00"
      }
    ],
    "totalOwned": 2,
    "totalShared": 1
  },
  "message": null
}
```

---

#### 2.1.2 보드 순서 변경

보드 목록에서의 정렬 순서 변경

**Request**
```
PUT /api/boards/{boardId}/order
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "sortOrder": 3
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "boardId": 1,
    "sortOrder": 3
  },
  "message": null
}
```

**Error** `403 Forbidden` (권한 없음)
```json
{
  "success": false,
  "data": null,
  "message": "보드 순서를 변경할 권한이 없습니다"
}
```

---

#### 2.1.3 이관 대상 업무 미리보기

보드 삭제 전 이관 대상 업무 목록 조회

**Request**
```
GET /api/boards/{boardId}/transfer-preview
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "boardId": 1,
    "boardName": "업무 관리",
    "totalItems": 25,
    "completedItems": 20,
    "deletedItems": 2,
    "pendingItems": [
      {
        "itemId": 23,
        "content": "API 문서 작성",
        "status": "IN_PROGRESS",
        "priority": "HIGH",
        "assigneeId": 2,
        "assigneeName": "홍길동",
        "createdAt": "2024-12-10T09:00:00"
      },
      {
        "itemId": 24,
        "content": "테스트 코드 작성",
        "status": "NOT_STARTED",
        "priority": "NORMAL",
        "assigneeId": null,
        "assigneeName": null,
        "createdAt": "2024-12-12T10:00:00"
      },
      {
        "itemId": 25,
        "content": "배포 준비",
        "status": "IN_PROGRESS",
        "priority": "URGENT",
        "assigneeId": 1,
        "assigneeName": "관리자",
        "createdAt": "2024-12-14T11:00:00"
      }
    ],
    "pendingCount": 3
  },
  "message": null
}
```

**Note**
- `pendingItems`: 상태가 COMPLETED 또는 DELETED가 아닌 업무 목록
- 이관 대상이 없으면 `pendingItems`는 빈 배열

---

#### 2.1.4 보드 삭제 (이관 포함)

미완료 업무를 다른 사용자에게 이관 후 보드 삭제 (논리 삭제)

**Request**
```
DELETE /api/boards/{boardId}/with-transfer
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "transferToUserId": 2,
  "transferReason": "담당자 변경으로 업무 이관"
}
```

**Parameters**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| transferToUserId | number | 조건부 | 이관 대상 사용자 ID (미완료 업무 있을 때 필수) |
| transferReason | string | X | 이관 사유 (최대 500자) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "deletedBoardId": 1,
    "deletedBoardName": "업무 관리",
    "transferredItems": 3,
    "transferredToUserId": 2,
    "transferredToUserName": "홍길동",
    "newBoardId": 5,
    "newBoardName": "[이관] 업무 관리 - 2024-12-21"
  },
  "message": "보드가 삭제되고 3건의 업무가 이관되었습니다"
}
```

**Response (이관 대상 없이 삭제)** `200 OK`
```json
{
  "success": true,
  "data": {
    "deletedBoardId": 1,
    "deletedBoardName": "업무 관리",
    "transferredItems": 0,
    "transferredToUserId": null,
    "transferredToUserName": null,
    "newBoardId": null,
    "newBoardName": null
  },
  "message": "보드가 삭제되었습니다"
}
```

**Error** `400 Bad Request` (이관 대상 필요)
```json
{
  "success": false,
  "data": null,
  "message": "미완료 업무가 3건 있습니다. 이관 대상자를 지정해주세요"
}
```

**Error** `403 Forbidden` (권한 없음)
```json
{
  "success": false,
  "data": null,
  "message": "보드 삭제 권한이 없습니다. 보드 소유자만 삭제할 수 있습니다"
}
```

---

#### 2.1.5 보드 공유 권한 변경

공유된 사용자의 권한 변경

**Request**
```
PUT /api/boards/{boardId}/shares/{userId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "permission": "FULL"
}
```

**Parameters**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| permission | string | O | 변경할 권한 (VIEW/EDIT/FULL) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "boardShareId": 5,
    "boardId": 1,
    "userId": 2,
    "userName": "홍길동",
    "permission": "FULL",
    "updatedAt": "2024-12-21T10:00:00"
  },
  "message": null
}
```

**Error** `403 Forbidden` (권한 없음)
```json
{
  "success": false,
  "data": null,
  "message": "공유 권한을 변경할 권한이 없습니다. 보드 소유자만 변경할 수 있습니다"
}
```

---

### 2.2 업무 공유 API

#### 2.2.1 업무 공유 목록 조회

특정 업무를 공유받은 사용자 목록 조회

**Request**
```
GET /api/items/{itemId}/shares
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "itemShareId": 1,
      "itemId": 23,
      "userId": 3,
      "userName": "박영희",
      "username": "user02",
      "departmentName": "개발1팀",
      "permission": "VIEW",
      "createdAt": "2024-12-15T10:00:00",
      "createdByName": "관리자"
    },
    {
      "itemShareId": 2,
      "itemId": 23,
      "userId": 4,
      "userName": "이민수",
      "username": "user03",
      "departmentName": "개발2팀",
      "permission": "EDIT",
      "createdAt": "2024-12-16T14:00:00",
      "createdByName": "관리자"
    }
  ],
  "message": null
}
```

---

#### 2.2.2 업무 공유 추가

특정 업무를 다른 사용자에게 공유

**Request**
```
POST /api/items/{itemId}/shares
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "userId": 5,
  "permission": "EDIT"
}
```

**Parameters**
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | number | O | 공유 대상 사용자 ID |
| permission | string | O | 권한 (VIEW/EDIT/FULL) |

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "itemShareId": 3,
    "itemId": 23,
    "userId": 5,
    "userName": "최준호",
    "username": "user04",
    "permission": "EDIT",
    "createdAt": "2024-12-21T11:00:00"
  },
  "message": null
}
```

**Error** `400 Bad Request` (이미 공유됨)
```json
{
  "success": false,
  "data": null,
  "message": "이미 해당 사용자에게 공유된 업무입니다"
}
```

**Error** `403 Forbidden` (권한 없음)
```json
{
  "success": false,
  "data": null,
  "message": "업무 공유 권한이 없습니다"
}
```

---

#### 2.2.3 업무 공유 권한 변경

공유된 사용자의 업무 권한 변경

**Request**
```
PUT /api/items/{itemId}/shares/{userId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "permission": "FULL"
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "itemShareId": 3,
    "itemId": 23,
    "userId": 5,
    "userName": "최준호",
    "permission": "FULL",
    "updatedAt": "2024-12-21T12:00:00"
  },
  "message": null
}
```

---

#### 2.2.4 업무 공유 제거

업무 공유 해제

**Request**
```
DELETE /api/items/{itemId}/shares/{userId}
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

**Error** `403 Forbidden` (권한 없음)
```json
{
  "success": false,
  "data": null,
  "message": "업무 공유를 제거할 권한이 없습니다"
}
```

---

### 2.3 감사 로그 API

#### 2.3.1 감사 로그 목록 조회

전체 감사 로그 조회 (페이징)

**Request**
```
GET /api/audit-logs
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| targetType | string | 대상 유형 (BOARD/ITEM/BOARD_SHARE/ITEM_SHARE) |
| action | string | 작업 유형 (CREATE/UPDATE/DELETE/TRANSFER/SHARE/UNSHARE) |
| actorId | number | 수행자 ID |
| startDate | string | 시작일 (YYYY-MM-DD) |
| endDate | string | 종료일 (YYYY-MM-DD) |
| page | number | 페이지 번호 (기본: 0) |
| size | number | 페이지 크기 (기본: 20) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "logId": 100,
        "targetType": "BOARD",
        "targetId": 1,
        "targetName": "업무 관리",
        "action": "DELETE",
        "actorId": 1,
        "actorName": "관리자",
        "description": "보드 삭제 (업무 3건 이관)",
        "beforeData": {
          "boardName": "업무 관리",
          "useYn": "Y"
        },
        "afterData": {
          "useYn": "N"
        },
        "relatedUserId": 2,
        "relatedUserName": "홍길동",
        "ipAddress": "192.168.1.100",
        "createdAt": "2024-12-21T10:30:00"
      },
      {
        "logId": 99,
        "targetType": "ITEM",
        "targetId": 23,
        "targetName": "API 문서 작성",
        "action": "TRANSFER",
        "actorId": 1,
        "actorName": "관리자",
        "description": "업무 이관: 업무 관리 → [이관] 업무 관리 - 2024-12-21",
        "beforeData": {
          "boardId": 1,
          "boardName": "업무 관리"
        },
        "afterData": {
          "boardId": 5,
          "boardName": "[이관] 업무 관리 - 2024-12-21"
        },
        "relatedUserId": 2,
        "relatedUserName": "홍길동",
        "ipAddress": "192.168.1.100",
        "createdAt": "2024-12-21T10:30:00"
      }
    ],
    "totalElements": 100,
    "totalPages": 5,
    "size": 20,
    "number": 0
  },
  "message": null
}
```

---

#### 2.3.2 최근 감사 로그 조회

최근 N개의 감사 로그 조회 (페이징 없음)

**Request**
```
GET /api/audit-logs/recent
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| limit | number | 10 | 조회할 개수 (최대 50) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "logId": 100,
      "targetType": "BOARD",
      "targetId": 1,
      "targetName": "업무 관리",
      "action": "DELETE",
      "actorName": "관리자",
      "description": "보드 삭제",
      "createdAt": "2024-12-21T10:30:00"
    },
    {
      "logId": 99,
      "targetType": "BOARD_SHARE",
      "targetId": 1,
      "targetName": "업무 관리",
      "action": "SHARE",
      "actorName": "관리자",
      "description": "홍길동에게 보드 공유 (EDIT)",
      "createdAt": "2024-12-21T10:00:00"
    }
  ],
  "message": null
}
```

---

#### 2.3.3 보드별 감사 로그 조회

특정 보드 관련 감사 로그 조회

**Request**
```
GET /api/audit-logs/boards/{boardId}
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| page | number | 페이지 번호 (기본: 0) |
| size | number | 페이지 크기 (기본: 20) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "logId": 50,
        "targetType": "BOARD",
        "targetId": 1,
        "action": "UPDATE",
        "actorName": "관리자",
        "description": "보드명 변경: 업무관리 → 업무 관리",
        "createdAt": "2024-12-20T15:00:00"
      },
      {
        "logId": 45,
        "targetType": "BOARD_SHARE",
        "targetId": 1,
        "action": "SHARE",
        "actorName": "관리자",
        "description": "홍길동에게 보드 공유 (EDIT)",
        "relatedUserName": "홍길동",
        "createdAt": "2024-12-18T10:00:00"
      },
      {
        "logId": 10,
        "targetType": "BOARD",
        "targetId": 1,
        "action": "CREATE",
        "actorName": "관리자",
        "description": "보드 생성",
        "createdAt": "2024-12-01T09:00:00"
      }
    ],
    "totalElements": 15,
    "totalPages": 1,
    "size": 20,
    "number": 0
  },
  "message": null
}
```

---

#### 2.3.4 업무별 감사 로그 조회

특정 업무 관련 감사 로그 조회

**Request**
```
GET /api/audit-logs/items/{itemId}
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| page | number | 페이지 번호 (기본: 0) |
| size | number | 페이지 크기 (기본: 20) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "logId": 80,
        "targetType": "ITEM_SHARE",
        "targetId": 23,
        "action": "SHARE",
        "actorName": "관리자",
        "description": "박영희에게 업무 공유 (VIEW)",
        "relatedUserName": "박영희",
        "createdAt": "2024-12-15T10:00:00"
      },
      {
        "logId": 60,
        "targetType": "ITEM",
        "targetId": 23,
        "action": "UPDATE",
        "actorName": "홍길동",
        "description": "상태 변경: NOT_STARTED → IN_PROGRESS",
        "createdAt": "2024-12-12T09:00:00"
      },
      {
        "logId": 55,
        "targetType": "ITEM",
        "targetId": 23,
        "action": "CREATE",
        "actorName": "관리자",
        "description": "업무 생성",
        "createdAt": "2024-12-10T09:00:00"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "size": 20,
    "number": 0
  },
  "message": null
}
```

---

## 3. 수정 API

### 3.1 보드 API 수정사항

#### 3.1.1 보드 목록 조회 (GET /api/boards) - 응답 수정

**추가된 응답 필드**
| 필드 | 타입 | 설명 |
|------|------|------|
| sortOrder | number | 정렬 순서 |
| color | string | 보드 색상 (#RRGGBB) |
| defaultViewType | string | 기본 뷰 타입 (TABLE/KANBAN/LIST) |
| ownerYn | string | 소유 여부 (Y/N) |
| permission | string | 권한 (VIEW/EDIT/FULL) - 공유받은 경우 |

**변경된 Response**
```json
{
  "success": true,
  "data": [
    {
      "boardId": 1,
      "boardName": "업무 관리",
      "description": "기본 업무 관리 보드",
      "ownerId": 1,
      "ownerName": "관리자",
      "sortOrder": 1,
      "color": "#3B82F6",
      "defaultViewType": "TABLE",
      "ownerYn": "Y",
      "permission": "OWNER",
      "useYn": "Y",
      "itemCount": 25,
      "createdAt": "2024-12-01T09:00:00"
    },
    {
      "boardId": 3,
      "boardName": "팀 프로젝트",
      "description": "팀 공유 보드",
      "ownerId": 2,
      "ownerName": "홍길동",
      "sortOrder": 0,
      "color": "#10B981",
      "defaultViewType": "KANBAN",
      "ownerYn": "N",
      "permission": "EDIT",
      "useYn": "Y",
      "itemCount": 15,
      "createdAt": "2024-12-05T10:00:00"
    }
  ],
  "message": null
}
```

---

#### 3.1.2 보드 생성 (POST /api/boards) - 요청/응답 수정

**추가된 요청 필드**
| 필드 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| sortOrder | number | X | 0 | 정렬 순서 |
| color | string | X | null | 보드 색상 (#RRGGBB) |
| defaultViewType | string | X | 'TABLE' | 기본 뷰 타입 |

**변경된 Request**
```json
{
  "boardName": "프로젝트 A",
  "description": "프로젝트 A 업무 보드",
  "sortOrder": 5,
  "color": "#8B5CF6",
  "defaultViewType": "KANBAN"
}
```

**변경된 Response**
```json
{
  "success": true,
  "data": {
    "boardId": 2,
    "boardName": "프로젝트 A",
    "description": "프로젝트 A 업무 보드",
    "ownerId": 1,
    "ownerName": "관리자",
    "sortOrder": 5,
    "color": "#8B5CF6",
    "defaultViewType": "KANBAN",
    "ownerYn": "Y",
    "useYn": "Y",
    "createdAt": "2024-12-21T15:00:00"
  },
  "message": null
}
```

---

### 3.2 보드 공유 API 수정사항

#### 3.2.1 공유 사용자 추가 (POST /api/boards/{boardId}/shares) - 요청 수정

**권한 값 변경**

| 이전 값 | 신규 값 | 설명 |
|---------|---------|------|
| VIEWER | VIEW | 조회 전용 |
| MEMBER | EDIT | 편집 가능 |
| (없음) | FULL | 전체 권한 (삭제 포함) |

**변경된 Request**
```json
{
  "userId": 3,
  "permission": "VIEW"
}
```

또는

```json
{
  "userId": 3,
  "permission": "EDIT"
}
```

또는

```json
{
  "userId": 3,
  "permission": "FULL"
}
```

**변경된 Response**
```json
{
  "success": true,
  "data": {
    "boardShareId": 5,
    "boardId": 1,
    "userId": 3,
    "userName": "박영희",
    "username": "user02",
    "permission": "FULL",
    "createdAt": "2024-12-21T11:00:00"
  },
  "message": null
}
```

---

## 4. DTO 명세

### 4.1 요청 DTO

#### BoardOrderRequest
```java
public class BoardOrderRequest {
    @NotNull
    private Integer sortOrder;  // 정렬 순서
}
```

#### BoardDeleteRequest
```java
public class BoardDeleteRequest {
    private Long transferToUserId;     // 이관 대상 사용자 ID

    @Size(max = 500)
    private String transferReason;      // 이관 사유
}
```

#### BoardShareUpdateRequest
```java
public class BoardShareUpdateRequest {
    @NotNull
    @Pattern(regexp = "VIEW|EDIT|FULL")
    private String permission;  // 권한
}
```

#### ItemShareRequest
```java
public class ItemShareRequest {
    @NotNull
    private Long userId;        // 공유 대상 사용자 ID

    @NotNull
    @Pattern(regexp = "VIEW|EDIT|FULL")
    private String permission;  // 권한
}
```

#### ItemShareUpdateRequest
```java
public class ItemShareUpdateRequest {
    @NotNull
    @Pattern(regexp = "VIEW|EDIT|FULL")
    private String permission;  // 권한
}
```

### 4.2 응답 DTO

#### BoardListResponse
```java
public class BoardListResponse {
    private List<BoardResponse> ownedBoards;    // 소유 보드 목록
    private List<BoardResponse> sharedBoards;   // 공유받은 보드 목록
    private int totalOwned;                     // 소유 보드 수
    private int totalShared;                    // 공유받은 보드 수
}
```

#### TransferPreviewResponse
```java
public class TransferPreviewResponse {
    private Long boardId;                       // 보드 ID
    private String boardName;                   // 보드명
    private int totalItems;                     // 전체 업무 수
    private int completedItems;                 // 완료된 업무 수
    private int deletedItems;                   // 삭제된 업무 수
    private int pendingCount;                   // 이관 대상 업무 수
    private List<PendingItemResponse> pendingItems; // 이관 대상 업무 목록
}
```

#### TransferResultResponse
```java
public class TransferResultResponse {
    private Long deletedBoardId;                // 삭제된 보드 ID
    private String deletedBoardName;            // 삭제된 보드명
    private int transferredItems;               // 이관된 업무 수
    private Long transferredToUserId;           // 이관 대상 사용자 ID
    private String transferredToUserName;       // 이관 대상 사용자명
    private Long newBoardId;                    // 새로 생성된 이관 보드 ID
    private String newBoardName;                // 새로 생성된 이관 보드명
}
```

#### ItemShareResponse
```java
public class ItemShareResponse {
    private Long itemShareId;                   // 공유 ID
    private Long itemId;                        // 업무 ID
    private Long userId;                        // 사용자 ID
    private String userName;                    // 사용자명
    private String username;                    // 로그인 ID
    private String departmentName;              // 부서명
    private String permission;                  // 권한
    private LocalDateTime createdAt;            // 생성일시
    private String createdByName;               // 생성자명
    private LocalDateTime updatedAt;            // 수정일시
}
```

#### AuditLogResponse
```java
public class AuditLogResponse {
    private Long logId;                         // 로그 ID
    private String targetType;                  // 대상 유형
    private Long targetId;                      // 대상 ID
    private String targetName;                  // 대상명
    private String action;                      // 작업 유형
    private Long actorId;                       // 수행자 ID
    private String actorName;                   // 수행자명
    private String description;                 // 변경 내용 설명
    private Object beforeData;                  // 변경 전 데이터 (JSON)
    private Object afterData;                   // 변경 후 데이터 (JSON)
    private Long relatedUserId;                 // 관련 사용자 ID
    private String relatedUserName;             // 관련 사용자명
    private String ipAddress;                   // IP 주소
    private LocalDateTime createdAt;            // 생성일시
}
```

---

## 5. 에러 코드

### 5.1 보드 관련 에러

| 코드 | HTTP | 메시지 |
|------|------|--------|
| BOARD_NOT_FOUND | 404 | 보드를 찾을 수 없습니다 |
| BOARD_ACCESS_DENIED | 403 | 보드에 접근할 권한이 없습니다 |
| BOARD_DELETE_DENIED | 403 | 보드 삭제 권한이 없습니다. 보드 소유자만 삭제할 수 있습니다 |
| BOARD_UPDATE_DENIED | 403 | 보드 수정 권한이 없습니다 |
| BOARD_ORDER_DENIED | 403 | 보드 순서를 변경할 권한이 없습니다 |
| BOARD_TRANSFER_REQUIRED | 400 | 미완료 업무가 있습니다. 이관 대상자를 지정해주세요 |
| BOARD_TRANSFER_USER_INVALID | 400 | 이관 대상 사용자를 찾을 수 없습니다 |

### 5.2 공유 관련 에러

| 코드 | HTTP | 메시지 |
|------|------|--------|
| SHARE_ALREADY_EXISTS | 400 | 이미 해당 사용자에게 공유되어 있습니다 |
| SHARE_NOT_FOUND | 404 | 공유 정보를 찾을 수 없습니다 |
| SHARE_PERMISSION_DENIED | 403 | 공유 관리 권한이 없습니다 |
| SHARE_PERMISSION_INVALID | 400 | 유효하지 않은 권한입니다 |
| SHARE_SELF_DENIED | 400 | 자기 자신에게 공유할 수 없습니다 |
| SHARE_OWNER_DENIED | 400 | 보드 소유자에게는 공유할 수 없습니다 |

### 5.3 업무 공유 관련 에러

| 코드 | HTTP | 메시지 |
|------|------|--------|
| ITEM_SHARE_DENIED | 403 | 업무 공유 권한이 없습니다 |
| ITEM_SHARE_NOT_FOUND | 404 | 업무 공유 정보를 찾을 수 없습니다 |

---

## 6. 변경 요약

### 6.1 API 변경 요약

| 구분 | 개수 | 설명 |
|------|------|------|
| 신규 API | 13 | 보드 관리 5, 업무 공유 4, 감사 로그 4 |
| 수정 API | 3 | 보드 목록, 보드 생성, 보드 공유 추가 |
| 삭제 API | 0 | - |
| **합계** | **16** | |

### 6.2 권한 체계 변경

| 이전 | 신규 | 설명 |
|------|------|------|
| VIEWER | VIEW | 조회 전용 |
| MEMBER | EDIT | 편집 가능 |
| - | FULL | 전체 권한 |
| (보드 소유자) | OWNER | 최고 권한 |

### 6.3 영향도 분석

| 영역 | 영향 |
|------|------|
| 백엔드 Controller | BoardController 수정, ItemShareController 추가, AuditLogController 추가 |
| 백엔드 Service | BoardService 수정, ItemShareService 추가, AuditLogService 추가, TransferService 추가 |
| 백엔드 Mapper | BoardShareMapper 수정, ItemShareMapper 추가, AuditLogMapper 추가, ItemMapper 수정 |
| 프론트엔드 API | board.api.ts 수정, itemShare.api.ts 추가, auditLog.api.ts 추가 |
| 프론트엔드 Store | board.ts 수정, itemShare.ts 추가 |
| 프론트엔드 Type | board.ts 수정, itemShare.ts 추가, auditLog.ts 추가 |

---

## 승인

| 항목 | 내용 |
|------|------|
| 문서명 | TaskFlow v1.1 API 명세서 |
| 버전 | 1.1 |
| 작성자 | - |
| 작성일 | 2024-12-21 |
| 상태 | 승인 대기 |

---

*이 문서는 v1.0 API 명세서(docs/api/api-specification.md)를 기반으로 확장된 문서입니다.*
