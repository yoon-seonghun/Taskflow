# TaskFlow REST API 명세서

## 목차
1. [공통 사항](#공통-사항)
2. [인증 API](#1-인증-api)
3. [사용자 API](#2-사용자-api)
4. [부서 API](#3-부서-api)
5. [그룹 API](#4-그룹-api)
6. [보드 API](#5-보드-api)
7. [아이템 API](#6-아이템-api)
8. [댓글 API](#7-댓글-api)
9. [속성 API](#8-속성-api)
10. [옵션 API](#9-옵션-api)
11. [템플릿 API](#10-템플릿-api)
12. [이력 API](#11-이력-api)
13. [SSE API](#12-sse-api)

---

## 공통 사항

### Base URL
```
http://localhost:8080/api
```

### 인증 방식
- JWT Bearer Token
- Header: `Authorization: Bearer {accessToken}`

### 공통 응답 형식
```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

### 에러 응답 형식
```json
{
  "success": false,
  "data": null,
  "message": "에러 메시지"
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

### 공통 쿼리 파라미터 (목록 조회)
| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| page | number | 0 | 페이지 번호 (0부터 시작) |
| size | number | 20 | 페이지 크기 |
| sort | string | createdAt,desc | 정렬 (필드,방향) |

### 페이징 응답 형식
```json
{
  "success": true,
  "data": {
    "content": [...],
    "totalElements": 100,
    "totalPages": 5,
    "size": 20,
    "number": 0
  },
  "message": null
}
```

---

## 1. 인증 API

### 1.1 로그인
사용자 인증 및 토큰 발급

**Request**
```
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "admin123!"
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "userId": 1,
      "username": "admin",
      "name": "관리자",
      "departmentId": 1,
      "departmentName": "본사"
    }
  },
  "message": null
}
```

**Error** `401 Unauthorized`
```json
{
  "success": false,
  "data": null,
  "message": "아이디 또는 비밀번호가 올바르지 않습니다"
}
```

---

### 1.2 로그아웃
토큰 무효화

**Request**
```
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": null,
  "message": null
}
```

---

### 1.3 토큰 갱신
Access Token 갱신

**Request**
```
POST /api/auth/refresh
Cookie: refreshToken={refreshToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  },
  "message": null
}
```

---

### 1.4 내 정보 조회
현재 로그인한 사용자 정보

**Request**
```
GET /api/auth/me
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "username": "admin",
    "name": "관리자",
    "departmentId": 1,
    "departmentName": "본사",
    "groups": [
      { "groupId": 1, "groupName": "기본 그룹" }
    ]
  },
  "message": null
}
```

---

## 2. 사용자 API

### 2.1 사용자 목록 조회

**Request**
```
GET /api/users
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| departmentId | number | 부서 ID 필터 |
| groupId | number | 그룹 ID 필터 |
| useYn | string | 사용 여부 (Y/N) |
| keyword | string | 이름/아이디 검색 |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "userId": 1,
        "username": "admin",
        "name": "관리자",
        "departmentId": 1,
        "departmentName": "본사",
        "useYn": "Y",
        "lastLoginAt": "2024-12-15T10:30:00",
        "createdAt": "2024-12-01T09:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 20,
    "number": 0
  },
  "message": null
}
```

---

### 2.2 사용자 등록

**Request**
```
POST /api/users
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "username": "user01",
  "password": "Password1!",
  "passwordConfirm": "Password1!",
  "name": "홍길동",
  "departmentId": 3
}
```

**Validation**
| 필드 | 규칙 |
|------|------|
| username | 필수, 4~20자, 영문+숫자 |
| password | 필수, 8자 이상, 영문대/소+숫자+특수문자 |
| name | 필수, 2~50자 |

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "userId": 2,
    "username": "user01",
    "name": "홍길동",
    "departmentId": 3,
    "departmentName": "개발1팀",
    "useYn": "Y",
    "createdAt": "2024-12-15T11:00:00"
  },
  "message": null
}
```

---

### 2.3 사용자 조회

**Request**
```
GET /api/users/{userId}
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "userId": 2,
    "username": "user01",
    "name": "홍길동",
    "departmentId": 3,
    "departmentName": "개발1팀",
    "groups": [
      { "groupId": 1, "groupName": "기본 그룹" },
      { "groupId": 3, "groupName": "프로젝트" }
    ],
    "useYn": "Y",
    "lastLoginAt": null,
    "createdAt": "2024-12-15T11:00:00",
    "updatedAt": null
  },
  "message": null
}
```

---

### 2.4 사용자 수정

**Request**
```
PUT /api/users/{userId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "name": "홍길동",
  "departmentId": 4,
  "useYn": "Y"
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "userId": 2,
    "username": "user01",
    "name": "홍길동",
    "departmentId": 4,
    "departmentName": "개발2팀",
    "useYn": "Y",
    "updatedAt": "2024-12-15T12:00:00"
  },
  "message": null
}
```

---

### 2.5 사용자 삭제

**Request**
```
DELETE /api/users/{userId}
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

---

### 2.6 비밀번호 변경

**Request**
```
PUT /api/users/{userId}/password
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "currentPassword": "Password1!",
  "newPassword": "NewPass2@",
  "newPasswordConfirm": "NewPass2@"
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": null,
  "message": "비밀번호가 변경되었습니다"
}
```

---

## 3. 부서 API

### 3.1 부서 목록 조회 (트리 구조)

**Request**
```
GET /api/departments
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| useYn | string | 사용 여부 (Y/N) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "departmentId": 1,
      "departmentCode": "ROOT",
      "departmentName": "본사",
      "parentId": null,
      "sortOrder": 0,
      "useYn": "Y",
      "children": [
        {
          "departmentId": 2,
          "departmentCode": "DEV",
          "departmentName": "개발본부",
          "parentId": 1,
          "sortOrder": 1,
          "useYn": "Y",
          "children": [
            {
              "departmentId": 3,
              "departmentCode": "DEV1",
              "departmentName": "개발1팀",
              "parentId": 2,
              "sortOrder": 1,
              "useYn": "Y",
              "children": []
            }
          ]
        }
      ]
    }
  ],
  "message": null
}
```

---

### 3.2 부서 목록 조회 (평면 구조)

**Request**
```
GET /api/departments/flat
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    { "departmentId": 1, "departmentCode": "ROOT", "departmentName": "본사", "parentId": null, "depth": 0 },
    { "departmentId": 2, "departmentCode": "DEV", "departmentName": "개발본부", "parentId": 1, "depth": 1 },
    { "departmentId": 3, "departmentCode": "DEV1", "departmentName": "개발1팀", "parentId": 2, "depth": 2 }
  ],
  "message": null
}
```

---

### 3.3 부서 생성

**Request**
```
POST /api/departments
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "departmentCode": "DEV3",
  "departmentName": "개발3팀",
  "parentId": 2,
  "sortOrder": 4
}
```

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "departmentId": 9,
    "departmentCode": "DEV3",
    "departmentName": "개발3팀",
    "parentId": 2,
    "sortOrder": 4,
    "useYn": "Y",
    "createdAt": "2024-12-15T14:00:00"
  },
  "message": null
}
```

---

### 3.4 부서 조회

**Request**
```
GET /api/departments/{departmentId}
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "departmentId": 3,
    "departmentCode": "DEV1",
    "departmentName": "개발1팀",
    "parentId": 2,
    "parentName": "개발본부",
    "sortOrder": 1,
    "useYn": "Y",
    "userCount": 5,
    "createdAt": "2024-12-01T09:00:00"
  },
  "message": null
}
```

---

### 3.5 부서 수정

**Request**
```
PUT /api/departments/{departmentId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "departmentName": "개발1팀(신규)",
  "parentId": 2,
  "sortOrder": 1,
  "useYn": "Y"
}
```

**Response** `200 OK`

---

### 3.6 부서 삭제

**Request**
```
DELETE /api/departments/{departmentId}
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

**Error** `400 Bad Request` (하위 부서 또는 소속 사용자 존재 시)
```json
{
  "success": false,
  "data": null,
  "message": "하위 부서가 존재하여 삭제할 수 없습니다"
}
```

---

### 3.7 부서 순서 변경

**Request**
```
PUT /api/departments/{departmentId}/order
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "sortOrder": 2
}
```

**Response** `200 OK`

---

### 3.8 부서별 사용자 목록

**Request**
```
GET /api/departments/{departmentId}/users
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "userId": 2,
      "username": "user01",
      "name": "홍길동",
      "useYn": "Y"
    }
  ],
  "message": null
}
```

---

## 4. 그룹 API

### 4.1 그룹 목록 조회

**Request**
```
GET /api/groups
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| useYn | string | 사용 여부 (Y/N) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "groupId": 1,
      "groupCode": "DEFAULT",
      "groupName": "기본 그룹",
      "description": "기본 업무 그룹",
      "color": "#6B7280",
      "sortOrder": 0,
      "useYn": "Y",
      "memberCount": 5
    }
  ],
  "message": null
}
```

---

### 4.2 그룹 생성

**Request**
```
POST /api/groups
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "groupCode": "TF001",
  "groupName": "신규 TF",
  "description": "신규 프로젝트 TF팀",
  "color": "#8B5CF6",
  "sortOrder": 10
}
```

**Response** `201 Created`

---

### 4.3 그룹 조회

**Request**
```
GET /api/groups/{groupId}
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "groupId": 1,
    "groupCode": "DEFAULT",
    "groupName": "기본 그룹",
    "description": "기본 업무 그룹",
    "color": "#6B7280",
    "sortOrder": 0,
    "useYn": "Y",
    "memberCount": 5,
    "createdAt": "2024-12-01T09:00:00"
  },
  "message": null
}
```

---

### 4.4 그룹 수정

**Request**
```
PUT /api/groups/{groupId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "groupName": "기본 그룹 (수정)",
  "description": "수정된 설명",
  "color": "#3B82F6",
  "useYn": "Y"
}
```

**Response** `200 OK`

---

### 4.5 그룹 삭제

**Request**
```
DELETE /api/groups/{groupId}
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

---

### 4.6 그룹 순서 변경

**Request**
```
PUT /api/groups/{groupId}/order
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "sortOrder": 5
}
```

**Response** `200 OK`

---

### 4.7 그룹 멤버 목록

**Request**
```
GET /api/groups/{groupId}/members
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "userId": 1,
      "username": "admin",
      "name": "관리자",
      "departmentName": "본사"
    }
  ],
  "message": null
}
```

---

### 4.8 그룹 멤버 추가

**Request**
```
POST /api/groups/{groupId}/members
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "userIds": [2, 3, 4]
}
```

**Response** `201 Created`

---

### 4.9 그룹 멤버 제거

**Request**
```
DELETE /api/groups/{groupId}/members/{userId}
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

---

## 5. 보드 API

### 5.1 보드 목록 조회

**Request**
```
GET /api/boards
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
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
      "useYn": "Y",
      "itemCount": 25,
      "createdAt": "2024-12-01T09:00:00"
    }
  ],
  "message": null
}
```

---

### 5.2 보드 생성

**Request**
```
POST /api/boards
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "boardName": "프로젝트 A",
  "description": "프로젝트 A 업무 보드"
}
```

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "boardId": 2,
    "boardName": "프로젝트 A",
    "description": "프로젝트 A 업무 보드",
    "ownerId": 1,
    "ownerName": "관리자",
    "useYn": "Y",
    "createdAt": "2024-12-15T15:00:00"
  },
  "message": null
}
```

---

### 5.3 보드 조회

**Request**
```
GET /api/boards/{boardId}
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "boardId": 1,
    "boardName": "업무 관리",
    "description": "기본 업무 관리 보드",
    "ownerId": 1,
    "ownerName": "관리자",
    "useYn": "Y",
    "itemCount": 25,
    "shares": [
      { "userId": 2, "userName": "홍길동", "permission": "EDIT" }
    ],
    "properties": [
      { "propertyId": 1, "propertyName": "카테고리", "propertyType": "SELECT" }
    ],
    "createdAt": "2024-12-01T09:00:00"
  },
  "message": null
}
```

---

### 5.4 보드 수정

**Request**
```
PUT /api/boards/{boardId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "boardName": "업무 관리 (수정)",
  "description": "수정된 설명",
  "useYn": "Y"
}
```

**Response** `200 OK`

---

### 5.5 보드 삭제

**Request**
```
DELETE /api/boards/{boardId}
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

---

### 5.6 공유 사용자 목록

**Request**
```
GET /api/boards/{boardId}/shares
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "boardShareId": 1,
      "userId": 2,
      "userName": "홍길동",
      "username": "user01",
      "permission": "EDIT",
      "createdAt": "2024-12-10T10:00:00"
    }
  ],
  "message": null
}
```

---

### 5.7 공유 사용자 추가

**Request**
```
POST /api/boards/{boardId}/shares
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "userId": 3,
  "permission": "EDIT"
}
```

**Response** `201 Created`

---

### 5.8 공유 사용자 제거

**Request**
```
DELETE /api/boards/{boardId}/shares/{userId}
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

---

## 6. 아이템 API

### 6.1 아이템 목록 조회

**Request**
```
GET /api/boards/{boardId}/items
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| status | string | 상태 필터 (NOT_STARTED/IN_PROGRESS/COMPLETED/DELETED) |
| priority | string | 우선순위 필터 (URGENT/HIGH/NORMAL/LOW) |
| assigneeId | number | 담당자 ID |
| groupId | number | 그룹 ID |
| categoryId | number | 카테고리 옵션 ID |
| startDate | string | 시작일 (YYYY-MM-DD) |
| endDate | string | 종료일 (YYYY-MM-DD) |
| keyword | string | 업무 내용 검색 |
| includeCompleted | boolean | 완료 포함 (기본: false) |
| includeDeleted | boolean | 삭제 포함 (기본: false) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "itemId": 1,
        "boardId": 1,
        "content": "프로젝트 초기 설정",
        "status": "COMPLETED",
        "priority": "HIGH",
        "categoryId": 1,
        "categoryName": "개발",
        "categoryColor": "#3B82F6",
        "groupId": 3,
        "groupName": "프로젝트",
        "assigneeId": 1,
        "assigneeName": "관리자",
        "startTime": "2024-12-01T09:00:00",
        "endTime": "2024-12-01T18:00:00",
        "commentCount": 2,
        "createdAt": "2024-12-01T09:00:00",
        "createdByName": "관리자",
        "updatedAt": "2024-12-01T18:00:00",
        "updatedByName": "관리자"
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

### 6.2 아이템 생성

**Request**
```
POST /api/boards/{boardId}/items
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "content": "신규 기능 개발",
  "status": "NOT_STARTED",
  "priority": "HIGH",
  "categoryId": 1,
  "groupId": 3,
  "assigneeId": 2,
  "startTime": "2024-12-16T09:00:00",
  "endTime": null,
  "propertyValues": {
    "5": "2024-12-16",
    "6": "2024-12-20"
  }
}
```

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "itemId": 6,
    "boardId": 1,
    "content": "신규 기능 개발",
    "status": "NOT_STARTED",
    "priority": "HIGH",
    "categoryId": 1,
    "categoryName": "개발",
    "groupId": 3,
    "groupName": "프로젝트",
    "assigneeId": 2,
    "assigneeName": "홍길동",
    "startTime": "2024-12-16T09:00:00",
    "endTime": null,
    "commentCount": 0,
    "createdAt": "2024-12-15T16:00:00"
  },
  "message": null
}
```

---

### 6.3 아이템 조회

**Request**
```
GET /api/boards/{boardId}/items/{itemId}
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "itemId": 1,
    "boardId": 1,
    "content": "프로젝트 초기 설정",
    "status": "COMPLETED",
    "priority": "HIGH",
    "categoryId": 1,
    "categoryName": "개발",
    "categoryColor": "#3B82F6",
    "groupId": 3,
    "groupName": "프로젝트",
    "groupColor": "#10B981",
    "assigneeId": 1,
    "assigneeName": "관리자",
    "startTime": "2024-12-01T09:00:00",
    "endTime": "2024-12-01T18:00:00",
    "deletedAt": null,
    "propertyValues": [
      {
        "propertyId": 5,
        "propertyName": "시작일",
        "propertyType": "DATE",
        "value": "2024-12-01"
      },
      {
        "propertyId": 6,
        "propertyName": "마감일",
        "propertyType": "DATE",
        "value": "2024-12-05"
      }
    ],
    "commentCount": 2,
    "createdAt": "2024-12-01T09:00:00",
    "createdBy": 1,
    "createdByName": "관리자",
    "updatedAt": "2024-12-01T18:00:00",
    "updatedBy": 1,
    "updatedByName": "관리자"
  },
  "message": null
}
```

---

### 6.4 아이템 수정

**Request**
```
PUT /api/boards/{boardId}/items/{itemId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "content": "프로젝트 초기 설정 (완료)",
  "status": "IN_PROGRESS",
  "priority": "NORMAL",
  "categoryId": 1,
  "groupId": 3,
  "assigneeId": 1,
  "startTime": "2024-12-01T09:00:00",
  "endTime": null,
  "propertyValues": {
    "5": "2024-12-01",
    "6": "2024-12-10"
  }
}
```

**Response** `200 OK`

---

### 6.5 아이템 삭제

**Request**
```
DELETE /api/boards/{boardId}/items/{itemId}
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

---

### 6.6 아이템 완료 처리

**Request**
```
PUT /api/boards/{boardId}/items/{itemId}/complete
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "itemId": 3,
    "status": "COMPLETED",
    "endTime": "2024-12-15T17:00:00"
  },
  "message": null
}
```

---

### 6.7 아이템 복원

**Request**
```
PUT /api/boards/{boardId}/items/{itemId}/restore
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "itemId": 3,
    "status": "IN_PROGRESS",
    "deletedAt": null
  },
  "message": null
}
```

---

### 6.8 아이템 속성값 일괄 수정

**Request**
```
PUT /api/boards/{boardId}/items/{itemId}/properties
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "propertyValues": {
    "1": 2,
    "5": "2024-12-16",
    "6": "2024-12-25"
  }
}
```

**Response** `200 OK`

---

## 7. 댓글 API

### 7.1 댓글 목록 조회

**Request**
```
GET /api/items/{itemId}/comments
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "commentId": 1,
      "itemId": 1,
      "content": "확인했습니다.",
      "createdAt": "2024-12-01T10:00:00",
      "createdBy": 1,
      "createdByName": "관리자",
      "updatedAt": null
    },
    {
      "commentId": 2,
      "itemId": 1,
      "content": "처리 완료되었습니다.",
      "createdAt": "2024-12-01T18:00:00",
      "createdBy": 1,
      "createdByName": "관리자",
      "updatedAt": null
    }
  ],
  "message": null
}
```

---

### 7.2 댓글 등록

**Request**
```
POST /api/items/{itemId}/comments
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "content": "작업 진행 중입니다."
}
```

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "commentId": 3,
    "itemId": 1,
    "content": "작업 진행 중입니다.",
    "createdAt": "2024-12-15T17:30:00",
    "createdBy": 1,
    "createdByName": "관리자"
  },
  "message": null
}
```

---

### 7.3 댓글 수정

**Request**
```
PUT /api/comments/{commentId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "content": "작업 진행 중입니다. (수정)"
}
```

**Response** `200 OK`

---

### 7.4 댓글 삭제

**Request**
```
DELETE /api/comments/{commentId}
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

---

## 8. 속성 API

### 8.1 속성 정의 목록 조회

**Request**
```
GET /api/boards/{boardId}/properties
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "propertyId": 1,
      "boardId": 1,
      "propertyName": "카테고리",
      "propertyType": "SELECT",
      "requiredYn": "N",
      "visibleYn": "Y",
      "sortOrder": 1,
      "options": [
        { "optionId": 1, "optionLabel": "개발", "color": "#3B82F6", "sortOrder": 1 },
        { "optionId": 2, "optionLabel": "기획", "color": "#8B5CF6", "sortOrder": 2 }
      ]
    },
    {
      "propertyId": 2,
      "boardId": 1,
      "propertyName": "상태",
      "propertyType": "SELECT",
      "requiredYn": "Y",
      "visibleYn": "Y",
      "sortOrder": 2,
      "options": [
        { "optionId": 10, "optionLabel": "시작전", "color": "#9CA3AF", "sortOrder": 1 },
        { "optionId": 11, "optionLabel": "진행중", "color": "#3B82F6", "sortOrder": 2 },
        { "optionId": 12, "optionLabel": "완료", "color": "#10B981", "sortOrder": 3 },
        { "optionId": 13, "optionLabel": "삭제", "color": "#EF4444", "sortOrder": 4 }
      ]
    }
  ],
  "message": null
}
```

---

### 8.2 속성 정의 생성

**Request**
```
POST /api/boards/{boardId}/properties
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "propertyName": "태그",
  "propertyType": "MULTI_SELECT",
  "requiredYn": "N",
  "visibleYn": "Y",
  "sortOrder": 10
}
```

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "propertyId": 7,
    "boardId": 1,
    "propertyName": "태그",
    "propertyType": "MULTI_SELECT",
    "requiredYn": "N",
    "visibleYn": "Y",
    "sortOrder": 10,
    "options": [],
    "createdAt": "2024-12-15T18:00:00"
  },
  "message": null
}
```

---

### 8.3 속성 정의 수정

**Request**
```
PUT /api/properties/{propertyId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "propertyName": "태그 (수정)",
  "requiredYn": "N",
  "visibleYn": "Y",
  "sortOrder": 10
}
```

**Response** `200 OK`

---

### 8.4 속성 정의 삭제

**Request**
```
DELETE /api/properties/{propertyId}
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

---

## 9. 옵션 API

### 9.1 옵션 목록 조회

**Request**
```
GET /api/properties/{propertyId}/options
Authorization: Bearer {accessToken}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "optionId": 1,
      "propertyId": 1,
      "optionLabel": "개발",
      "color": "#3B82F6",
      "sortOrder": 1,
      "useYn": "Y"
    },
    {
      "optionId": 2,
      "propertyId": 1,
      "optionLabel": "기획",
      "color": "#8B5CF6",
      "sortOrder": 2,
      "useYn": "Y"
    }
  ],
  "message": null
}
```

---

### 9.2 옵션 추가

**Request**
```
POST /api/properties/{propertyId}/options
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "optionLabel": "테스트",
  "color": "#F59E0B",
  "sortOrder": 6
}
```

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "optionId": 6,
    "propertyId": 1,
    "optionLabel": "테스트",
    "color": "#F59E0B",
    "sortOrder": 6,
    "useYn": "Y"
  },
  "message": null
}
```

---

### 9.3 옵션 수정

**Request**
```
PUT /api/options/{optionId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "optionLabel": "테스트 (수정)",
  "color": "#EF4444",
  "sortOrder": 6,
  "useYn": "Y"
}
```

**Response** `200 OK`

---

### 9.4 옵션 삭제

**Request**
```
DELETE /api/options/{optionId}
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

**Error** `400 Bad Request` (사용 중인 옵션)
```json
{
  "success": false,
  "data": null,
  "message": "해당 옵션을 사용 중인 아이템이 있습니다"
}
```

---

## 10. 템플릿 API

### 10.1 템플릿 목록 조회

**Request**
```
GET /api/task-templates
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| useYn | string | 사용 여부 (Y/N) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "templateId": 1,
      "content": "일일 업무 보고",
      "useYn": "Y",
      "createdAt": "2024-12-01T09:00:00",
      "createdByName": "관리자"
    },
    {
      "templateId": 2,
      "content": "주간 회의 준비",
      "useYn": "Y",
      "createdAt": "2024-12-01T09:00:00",
      "createdByName": "관리자"
    }
  ],
  "message": null
}
```

---

### 10.2 템플릿 검색 (자동완성)

**Request**
```
GET /api/task-templates/search
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| keyword | string | 검색어 (2자 이상) |
| limit | number | 최대 결과 수 (기본: 10) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "templateId": 1,
      "content": "일일 업무 보고"
    },
    {
      "templateId": 3,
      "content": "코드 리뷰"
    }
  ],
  "message": null
}
```

---

### 10.3 템플릿 등록

**Request**
```
POST /api/task-templates
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "content": "새로운 작업 템플릿"
}
```

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "templateId": 9,
    "content": "새로운 작업 템플릿",
    "useYn": "Y",
    "createdAt": "2024-12-15T19:00:00"
  },
  "message": null
}
```

---

### 10.4 템플릿 수정

**Request**
```
PUT /api/task-templates/{templateId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "content": "수정된 작업 템플릿",
  "useYn": "Y"
}
```

**Response** `200 OK`

---

### 10.5 템플릿 삭제

**Request**
```
DELETE /api/task-templates/{templateId}
Authorization: Bearer {accessToken}
```

**Response** `204 No Content`

---

## 11. 이력 API

### 11.1 작업 처리 이력 조회

**Request**
```
GET /api/history/items
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| actionType | string | 액션 타입 (COMPLETE/DELETE) |
| startDate | string | 시작일 (YYYY-MM-DD) |
| endDate | string | 종료일 (YYYY-MM-DD) |
| userId | number | 처리자 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "historyId": 3,
        "itemId": 1,
        "content": "프로젝트 초기 설정",
        "actionType": "COMPLETE",
        "createdAt": "2024-12-01T18:00:00",
        "createdBy": 1,
        "createdByName": "관리자",
        "item": {
          "status": "COMPLETED",
          "startTime": "2024-12-01T09:00:00",
          "endTime": "2024-12-01T18:00:00"
        }
      }
    ],
    "totalElements": 3,
    "totalPages": 1,
    "size": 20,
    "number": 0
  },
  "message": null
}
```

---

### 11.2 작업 등록 이력 조회

**Request**
```
GET /api/history/templates
Authorization: Bearer {accessToken}
```

**Query Parameters**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| startDate | string | 시작일 (YYYY-MM-DD) |
| endDate | string | 종료일 (YYYY-MM-DD) |
| userId | number | 등록자 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "templateId": 1,
        "content": "일일 업무 보고",
        "useYn": "Y",
        "createdAt": "2024-12-01T09:00:00",
        "createdBy": 1,
        "createdByName": "관리자",
        "updatedAt": null
      }
    ],
    "totalElements": 8,
    "totalPages": 1,
    "size": 20,
    "number": 0
  },
  "message": null
}
```

---

## 12. SSE API

### 12.1 SSE 연결

**Request**
```
GET /api/sse/subscribe
Authorization: Bearer {accessToken}
Accept: text/event-stream
```

**Response** `200 OK`
```
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

### 이벤트 형식

```
event: {eventType}
data: {JSON payload}

```

### 이벤트 타입

| 이벤트 | 설명 | Payload |
|--------|------|---------|
| connected | 연결 성공 | `{ "message": "Connected" }` |
| item:created | 아이템 생성 | `{ "itemId": 1, "boardId": 1, ... }` |
| item:updated | 아이템 수정 | `{ "itemId": 1, "boardId": 1, ... }` |
| item:deleted | 아이템 삭제 | `{ "itemId": 1, "boardId": 1 }` |
| item:completed | 아이템 완료 | `{ "itemId": 1, "boardId": 1 }` |
| property:updated | 속성 정의 변경 | `{ "propertyId": 1, "boardId": 1, ... }` |
| option:created | 옵션 추가 | `{ "optionId": 1, "propertyId": 1, ... }` |
| option:updated | 옵션 수정 | `{ "optionId": 1, "propertyId": 1, ... }` |
| option:deleted | 옵션 삭제 | `{ "optionId": 1, "propertyId": 1 }` |
| comment:created | 댓글 등록 | `{ "commentId": 1, "itemId": 1, ... }` |
| heartbeat | 연결 유지 | `{ "timestamp": 1702648800000 }` |

### 이벤트 예시

```
event: connected
data: {"message":"Connected","userId":1}

event: item:created
data: {"itemId":6,"boardId":1,"content":"신규 업무","status":"NOT_STARTED","createdBy":1,"createdByName":"관리자"}

event: item:updated
data: {"itemId":6,"boardId":1,"content":"신규 업무 (수정)","status":"IN_PROGRESS","updatedBy":1,"updatedByName":"관리자"}

event: heartbeat
data: {"timestamp":1702648800000}

```

### 클라이언트 구현 예시

```javascript
const eventSource = new EventSource('/api/sse/subscribe', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

eventSource.addEventListener('connected', (e) => {
  console.log('SSE Connected:', JSON.parse(e.data));
});

eventSource.addEventListener('item:created', (e) => {
  const item = JSON.parse(e.data);
  // Store에 아이템 추가
  itemStore.addItem(item);
});

eventSource.addEventListener('item:updated', (e) => {
  const item = JSON.parse(e.data);
  // Store에서 아이템 업데이트
  itemStore.updateItem(item.itemId, item);
});

eventSource.onerror = (e) => {
  console.error('SSE Error:', e);
  // 3초 후 재연결
  setTimeout(() => reconnect(), 3000);
};
```

---

## API 요약

| 그룹 | API 수 | 설명 |
|------|--------|------|
| 인증 | 4 | 로그인, 로그아웃, 토큰갱신, 내정보 |
| 사용자 | 6 | CRUD, 비밀번호 변경 |
| 부서 | 8 | CRUD, 트리/평면 조회, 순서변경, 사용자목록 |
| 그룹 | 9 | CRUD, 순서변경, 멤버 관리 |
| 보드 | 8 | CRUD, 공유 사용자 관리 |
| 아이템 | 8 | CRUD, 완료/복원, 속성값 일괄수정 |
| 댓글 | 4 | CRUD |
| 속성 | 4 | CRUD |
| 옵션 | 4 | CRUD |
| 템플릿 | 5 | CRUD, 검색(자동완성) |
| 이력 | 2 | 처리이력, 등록이력 |
| SSE | 1 | 실시간 구독 |
| **합계** | **63** | |
