# TaskFlow 2.0 REST API 명세서

## 문서 정보
- **버전**: 2.0
- **작성일**: 2025-12-24
- **Base URL**: `http://localhost:8080` (개발), `https://api.taskflow.com` (운영)

---

## 1. API 개요

### 1.1 인증 방식
TaskFlow API는 **JWT Bearer Token** 기반 인증을 사용합니다.

```http
Authorization: Bearer {access_token}
```

#### 토큰 관리
| 토큰 | 저장 위치 | 유효기간 | 용도 |
|------|-----------|----------|------|
| Access Token | localStorage | 30분 | API 요청 인증 |
| Refresh Token | httpOnly Cookie | 7일 | Access Token 갱신 |

#### 토큰 갱신 흐름
```
1. Access Token 만료 시 401 Unauthorized 응답 수신
2. /api/auth/refresh 호출 (Cookie의 Refresh Token 자동 전송)
3. 새로운 Access Token 수신
4. 실패한 요청 재시도
```

### 1.2 공통 응답 형식

#### 성공 응답 (ApiResponse)
```json
{
  "success": true,
  "data": {
    // 응답 데이터
  },
  "message": null
}
```

#### 성공 응답 (메시지 포함)
```json
{
  "success": true,
  "data": null,
  "message": "작업이 완료되었습니다"
}
```

#### 실패 응답
```json
{
  "success": false,
  "data": null,
  "message": "에러 메시지"
}
```

### 1.3 HTTP 상태 코드

| 코드 | 의미 | 설명 |
|------|------|------|
| 200 | OK | 요청 성공 (조회, 수정) |
| 201 | Created | 리소스 생성 성공 |
| 204 | No Content | 요청 성공, 응답 본문 없음 |
| 400 | Bad Request | 잘못된 요청 (유효성 검증 실패) |
| 401 | Unauthorized | 인증 필요 (토큰 없음/만료) |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 충돌 (중복, 동시 수정) |
| 500 | Internal Server Error | 서버 오류 |

### 1.4 페이징 파라미터

목록 조회 API는 공통 페이징 파라미터를 지원합니다.

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| page | Integer | 0 | 페이지 번호 (0부터 시작) |
| size | Integer | 20 | 페이지당 항목 수 |
| sort | String | - | 정렬 (예: `createdAt,desc`) |

#### 페이징 응답 형식 (PageResponse)
```json
{
  "success": true,
  "data": {
    "content": [ /* 항목 배열 */ ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  }
}
```

---

## 2. 인증 API

### 2.1 로그인
사용자 인증을 수행하고 Access Token을 발급합니다.

```http
POST /api/auth/login
```

#### Request Body
```json
{
  "username": "admin",
  "password": "password123!"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| username | String | ✅ | 로그인 아이디 |
| password | String | ✅ | 비밀번호 |

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "userId": 1,
      "username": "admin",
      "name": "관리자",
      "email": "admin@taskflow.com",
      "departmentId": 1,
      "departmentName": "경영지원본부",
      "position": "팀장",
      "useYn": "Y"
    }
  }
}
```

#### Error Responses
- **401 Unauthorized**: 아이디 또는 비밀번호 오류
- **403 Forbidden**: 비활성화된 계정

---

### 2.2 로그아웃
현재 세션을 종료하고 Refresh Token을 무효화합니다.

```http
POST /api/auth/logout
```

#### Request Header
```http
Authorization: Bearer {access_token}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "로그아웃 되었습니다"
}
```

---

### 2.3 토큰 갱신
만료된 Access Token을 갱신합니다.

```http
POST /api/auth/refresh
```

#### Request
Cookie에서 `refreshToken`이 자동으로 전송됩니다.

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

#### Error Responses
- **401 Unauthorized**: Refresh Token이 없거나 만료됨

---

### 2.4 내 정보 조회
현재 로그인한 사용자의 정보를 조회합니다.

```http
GET /api/auth/me
```

#### Request Header
```http
Authorization: Bearer {access_token}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "username": "admin",
    "name": "관리자",
    "email": "admin@taskflow.com",
    "departmentId": 1,
    "departmentName": "경영지원본부",
    "position": "팀장",
    "useYn": "Y",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-12-01T12:00:00"
  }
}
```

---

## 3. 사용자 API

### 3.1 사용자 목록 조회
사용자 목록을 페이징하여 조회합니다.

```http
GET /api/users
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| keyword | String | - | 검색어 (이름, 아이디, 이메일) |
| departmentId | Long | - | 부서 ID 필터 |
| useYn | String | - | 사용 여부 (Y/N) |
| page | Integer | - | 페이지 번호 (기본: 0) |
| size | Integer | - | 페이지 크기 (기본: 20) |

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "userId": 1,
        "username": "admin",
        "name": "관리자",
        "email": "admin@taskflow.com",
        "departmentId": 1,
        "departmentName": "경영지원본부",
        "position": "팀장",
        "useYn": "Y",
        "createdAt": "2024-01-01T00:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3
  }
}
```

---

### 3.2 사용자 등록
새로운 사용자를 등록합니다.

```http
POST /api/users
```

#### Request Body
```json
{
  "username": "user123",
  "password": "Password123!",
  "name": "홍길동",
  "email": "hong@taskflow.com",
  "departmentId": 1,
  "position": "사원",
  "useYn": "Y"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| username | String | ✅ | 로그인 아이디 (4~20자, 영문+숫자) |
| password | String | ✅ | 비밀번호 (최소 8자) |
| name | String | ✅ | 사용자 이름 |
| email | String | ✅ | 이메일 |
| departmentId | Long | - | 부서 ID |
| position | String | - | 직급 |
| useYn | String | - | 사용 여부 (기본: Y) |

#### Response (201 Created)
```json
{
  "success": true,
  "data": {
    "userId": 101,
    "username": "user123",
    "name": "홍길동",
    "email": "hong@taskflow.com",
    "departmentId": 1,
    "departmentName": "경영지원본부",
    "position": "사원",
    "useYn": "Y",
    "createdAt": "2024-12-24T10:30:00"
  },
  "message": "사용자가 등록되었습니다"
}
```

---

### 3.3 사용자 조회
특정 사용자의 상세 정보를 조회합니다.

```http
GET /api/users/{id}
```

#### Path Parameters
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 사용자 ID |

#### Response (200 OK)
사용자 등록 응답과 동일

---

### 3.4 사용자 수정
사용자 정보를 수정합니다.

```http
PUT /api/users/{id}
```

#### Request Body
```json
{
  "name": "홍길동",
  "email": "hong.new@taskflow.com",
  "departmentId": 2,
  "position": "대리",
  "useYn": "Y"
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": { /* 수정된 사용자 정보 */ },
  "message": "사용자 정보가 수정되었습니다"
}
```

---

### 3.5 사용자 삭제
사용자를 삭제합니다.

```http
DELETE /api/users/{id}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "사용자가 삭제되었습니다"
}
```

---

### 3.6 비밀번호 변경
사용자의 비밀번호를 변경합니다.

```http
PUT /api/users/{id}/password
```

#### Request Body
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword123!",
  "confirmPassword": "NewPassword123!"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| currentPassword | String | ✅ | 현재 비밀번호 |
| newPassword | String | ✅ | 새 비밀번호 |
| confirmPassword | String | ✅ | 비밀번호 확인 |

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "비밀번호가 변경되었습니다"
}
```

---

### 3.7 아이디 중복 확인
사용자명(아이디) 중복 여부를 확인합니다.

```http
GET /api/users/check-username?username=user123
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| username | String | ✅ | 확인할 아이디 |

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "exists": false
  }
}
```

---

## 4. 부서 API

### 4.1 부서 목록 조회 (트리 구조)
부서 목록을 계층 구조로 조회합니다.

```http
GET /api/departments
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| useYn | String | - | 사용 여부 (Y/N) |

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "departmentId": 1,
      "departmentCode": "MNG",
      "departmentName": "경영지원본부",
      "parentId": null,
      "sortOrder": 1,
      "useYn": "Y",
      "children": [
        {
          "departmentId": 2,
          "departmentCode": "HR",
          "departmentName": "인사팀",
          "parentId": 1,
          "sortOrder": 1,
          "useYn": "Y",
          "children": []
        }
      ]
    }
  ]
}
```

---

### 4.2 부서 목록 조회 (평면 구조)
부서 목록을 평면 구조로 조회합니다. (SELECT 박스용)

```http
GET /api/departments/flat
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| useYn | String | - | 사용 여부 (Y/N) |

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "departmentId": 1,
      "departmentCode": "MNG",
      "departmentName": "경영지원본부",
      "fullPath": "경영지원본부",
      "level": 0,
      "useYn": "Y"
    },
    {
      "departmentId": 2,
      "departmentCode": "HR",
      "departmentName": "인사팀",
      "fullPath": "경영지원본부 > 인사팀",
      "level": 1,
      "useYn": "Y"
    }
  ]
}
```

---

### 4.3 부서 생성
새로운 부서를 생성합니다.

```http
POST /api/departments
```

#### Request Body
```json
{
  "departmentCode": "DEV",
  "departmentName": "개발팀",
  "parentId": 1,
  "sortOrder": 3,
  "useYn": "Y"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| departmentCode | String | ✅ | 부서 코드 (고유) |
| departmentName | String | ✅ | 부서명 |
| parentId | Long | - | 상위 부서 ID |
| sortOrder | Integer | - | 정렬 순서 |
| useYn | String | - | 사용 여부 (기본: Y) |

#### Response (201 Created)
```json
{
  "success": true,
  "data": {
    "departmentId": 10,
    "departmentCode": "DEV",
    "departmentName": "개발팀",
    "parentId": 1,
    "sortOrder": 3,
    "useYn": "Y",
    "createdAt": "2024-12-24T10:30:00"
  },
  "message": "부서가 등록되었습니다"
}
```

---

### 4.4 부서 조회
특정 부서의 상세 정보를 조회합니다.

```http
GET /api/departments/{id}
```

---

### 4.5 부서 수정
부서 정보를 수정합니다.

```http
PUT /api/departments/{id}
```

#### Request Body
```json
{
  "departmentName": "개발1팀",
  "parentId": 1,
  "sortOrder": 3,
  "useYn": "Y"
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": { /* 수정된 부서 정보 */ },
  "message": "부서 정보가 수정되었습니다"
}
```

---

### 4.6 부서 삭제
부서를 삭제합니다.

```http
DELETE /api/departments/{id}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "부서가 삭제되었습니다"
}
```

---

### 4.7 부서 순서 변경
부서의 정렬 순서를 변경합니다.

```http
PUT /api/departments/{id}/order
```

#### Request Body
```json
{
  "sortOrder": 5
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": { /* 수정된 부서 정보 */ },
  "message": "부서 순서가 변경되었습니다"
}
```

---

### 4.8 부서별 사용자 목록
특정 부서에 소속된 사용자 목록을 조회합니다.

```http
GET /api/departments/{id}/users
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "userId": 1,
      "username": "admin",
      "name": "관리자",
      "email": "admin@taskflow.com",
      "departmentId": 1,
      "departmentName": "경영지원본부",
      "position": "팀장",
      "useYn": "Y"
    }
  ]
}
```

---

### 4.9 하위 부서 목록
특정 부서의 하위 부서 목록을 조회합니다.

```http
GET /api/departments/{id}/children
```

---

### 4.10 부서 코드 중복 확인
부서 코드 중복 여부를 확인합니다.

```http
GET /api/departments/check-code?code=DEV
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "exists": false
  }
}
```

---

## 5. 그룹 API

### 5.1 그룹 목록 조회
그룹 목록을 조회합니다.

```http
GET /api/groups
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| useYn | String | - | 사용 여부 (Y/N) |

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "groupId": 1,
      "groupCode": "PROJ001",
      "groupName": "신규 서비스 개발",
      "description": "신규 서비스 개발 프로젝트",
      "color": "#3B82F6",
      "sortOrder": 1,
      "useYn": "Y",
      "memberCount": 5,
      "createdAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

### 5.2 그룹 생성
새로운 그룹을 생성합니다.

```http
POST /api/groups
```

#### Request Body
```json
{
  "groupCode": "PROJ002",
  "groupName": "UI 리뉴얼",
  "description": "사용자 인터페이스 개선 프로젝트",
  "color": "#10B981",
  "sortOrder": 2,
  "useYn": "Y"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| groupCode | String | ✅ | 그룹 코드 (고유) |
| groupName | String | ✅ | 그룹명 |
| description | String | - | 그룹 설명 |
| color | String | - | 표시 색상 (HEX) |
| sortOrder | Integer | - | 정렬 순서 |
| useYn | String | - | 사용 여부 (기본: Y) |

#### Response (201 Created)
```json
{
  "success": true,
  "data": {
    "groupId": 10,
    "groupCode": "PROJ002",
    "groupName": "UI 리뉴얼",
    "description": "사용자 인터페이스 개선 프로젝트",
    "color": "#10B981",
    "sortOrder": 2,
    "useYn": "Y",
    "memberCount": 0,
    "createdAt": "2024-12-24T10:30:00"
  },
  "message": "그룹이 등록되었습니다"
}
```

---

### 5.3 그룹 조회
특정 그룹의 상세 정보를 조회합니다.

```http
GET /api/groups/{id}
```

---

### 5.4 그룹 수정
그룹 정보를 수정합니다.

```http
PUT /api/groups/{id}
```

#### Request Body
```json
{
  "groupName": "UI/UX 리뉴얼",
  "description": "사용자 경험 개선 프로젝트",
  "color": "#10B981",
  "sortOrder": 2,
  "useYn": "Y"
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": { /* 수정된 그룹 정보 */ },
  "message": "그룹 정보가 수정되었습니다"
}
```

---

### 5.5 그룹 삭제
그룹을 삭제합니다.

```http
DELETE /api/groups/{id}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "그룹이 삭제되었습니다"
}
```

---

### 5.6 그룹 순서 변경
그룹의 정렬 순서를 변경합니다.

```http
PUT /api/groups/{id}/order
```

#### Request Body
```json
{
  "sortOrder": 5
}
```

---

### 5.7 그룹 멤버 목록
특정 그룹의 멤버 목록을 조회합니다.

```http
GET /api/groups/{id}/members
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "userId": 1,
      "username": "admin",
      "name": "관리자",
      "email": "admin@taskflow.com",
      "departmentName": "경영지원본부",
      "position": "팀장",
      "joinedAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

### 5.8 그룹 멤버 추가
그룹에 새로운 멤버를 추가합니다.

```http
POST /api/groups/{id}/members
```

#### Request Body
```json
{
  "userId": 10
}
```

#### Response (201 Created)
```json
{
  "success": true,
  "data": {
    "userId": 10,
    "username": "user123",
    "name": "홍길동",
    "email": "hong@taskflow.com",
    "departmentName": "개발팀",
    "position": "사원",
    "joinedAt": "2024-12-24T10:30:00"
  },
  "message": "멤버가 추가되었습니다"
}
```

---

### 5.9 그룹 멤버 제거
그룹에서 멤버를 제거합니다.

```http
DELETE /api/groups/{id}/members/{userId}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "멤버가 제거되었습니다"
}
```

---

### 5.10 그룹 코드 중복 확인
그룹 코드 중복 여부를 확인합니다.

```http
GET /api/groups/check-code?code=PROJ002
```

---

### 5.11 사용자별 소속 그룹 목록
특정 사용자가 소속된 그룹 목록을 조회합니다.

```http
GET /api/users/{userId}/groups
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "userId": 1,
      "groupId": 1,
      "groupName": "신규 서비스 개발",
      "groupColor": "#3B82F6",
      "joinedAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

## 6. 보드 API

### 6.1 보드 목록 조회
현재 사용자가 접근 가능한 보드 목록을 조회합니다.

```http
GET /api/boards
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| useYn | String | - | 사용 여부 (Y/N) |
| owned | Boolean | - | 소유 보드만 조회 (기본: false) |

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "boardId": 1,
      "boardName": "개인 업무",
      "description": "개인 업무 관리 보드",
      "ownerId": 1,
      "ownerName": "관리자",
      "sortOrder": 1,
      "useYn": "Y",
      "isOwner": true,
      "permission": "OWNER",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-12-01T12:00:00"
    }
  ]
}
```

---

### 6.2 보드 목록 조회 (소유/공유 분리)
보드 목록을 소유 보드와 공유 보드로 분리하여 조회합니다.

```http
GET /api/boards/list
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "ownedBoards": [
      {
        "boardId": 1,
        "boardName": "개인 업무",
        "description": "개인 업무 관리 보드",
        "ownerId": 1,
        "ownerName": "관리자",
        "sortOrder": 1,
        "useYn": "Y",
        "isOwner": true,
        "permission": "OWNER",
        "createdAt": "2024-01-01T00:00:00"
      }
    ],
    "sharedBoards": [
      {
        "boardId": 2,
        "boardName": "팀 공유 보드",
        "description": "팀 업무 공유",
        "ownerId": 5,
        "ownerName": "팀장",
        "sortOrder": 1,
        "useYn": "Y",
        "isOwner": false,
        "permission": "EDIT",
        "createdAt": "2024-02-01T00:00:00"
      }
    ]
  }
}
```

---

### 6.3 보드 생성
새로운 보드를 생성합니다.

```http
POST /api/boards
```

#### Request Body
```json
{
  "boardName": "프로젝트 A",
  "description": "프로젝트 A 업무 관리",
  "sortOrder": 5,
  "useYn": "Y"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| boardName | String | ✅ | 보드명 |
| description | String | - | 보드 설명 |
| sortOrder | Integer | - | 정렬 순서 |
| useYn | String | - | 사용 여부 (기본: Y) |

#### Response (201 Created)
```json
{
  "success": true,
  "data": {
    "boardId": 10,
    "boardName": "프로젝트 A",
    "description": "프로젝트 A 업무 관리",
    "ownerId": 1,
    "ownerName": "관리자",
    "sortOrder": 5,
    "useYn": "Y",
    "isOwner": true,
    "permission": "OWNER",
    "createdAt": "2024-12-24T10:30:00"
  },
  "message": "보드가 생성되었습니다"
}
```

---

### 6.4 보드 조회
특정 보드의 상세 정보를 조회합니다.

```http
GET /api/boards/{id}
```

#### Response (200 OK)
보드 생성 응답과 동일

#### Error Responses
- **403 Forbidden**: 보드 접근 권한 없음

---

### 6.5 보드 수정
보드 정보를 수정합니다.

```http
PUT /api/boards/{id}
```

#### Request Body
```json
{
  "boardName": "프로젝트 A (진행중)",
  "description": "프로젝트 A 업무 관리 - 진행중",
  "useYn": "Y"
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": { /* 수정된 보드 정보 */ },
  "message": "보드가 수정되었습니다"
}
```

---

### 6.6 보드 삭제
보드를 삭제합니다.

```http
DELETE /api/boards/{id}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "보드가 삭제되었습니다"
}
```

---

### 6.7 보드 순서 변경
보드의 정렬 순서를 변경합니다.

```http
PUT /api/boards/{id}/order
```

#### Request Body
```json
{
  "sortOrder": 3
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "보드 순서가 변경되었습니다"
}
```

---

### 6.8 보드 삭제 (이관 포함)
보드를 삭제하고 업무를 다른 사용자에게 이관합니다.

```http
DELETE /api/boards/{id}/with-transfer
```

#### Request Body
```json
{
  "targetUserId": 5,
  "forceDelete": false
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| targetUserId | Long | - | 업무 이관 대상 사용자 ID |
| forceDelete | Boolean | - | 강제 삭제 여부 (기본: false) |

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "deletedBoardId": 10,
    "deletedBoardName": "프로젝트 A",
    "transferredCount": 25,
    "targetUserId": 5,
    "targetUserName": "김팀장",
    "targetBoardId": 15,
    "targetBoardName": "김팀장 - 이관된 업무"
  },
  "message": "보드가 삭제되고 업무가 이관되었습니다"
}
```

---

### 6.9 이관 대상 업무 미리보기
보드 삭제 시 이관될 업무 목록을 미리 확인합니다.

```http
GET /api/boards/{id}/transfer-preview
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "boardId": 10,
    "boardName": "프로젝트 A",
    "totalItemCount": 30,
    "activeItemCount": 25,
    "completedItemCount": 5,
    "items": [
      {
        "itemId": 100,
        "title": "UI 설계",
        "status": "IN_PROGRESS",
        "priority": "HIGH",
        "assigneeName": "홍길동"
      }
    ]
  }
}
```

---

### 6.10 보드 소유권 이전
보드를 다른 사용자에게 이관합니다.

```http
PUT /api/boards/{id}/transfer-ownership
```

#### Request Body
```json
{
  "targetUserId": 5
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "boardId": 10,
    "boardName": "보드이관",
    "ownerId": 5,
    "ownerName": "김팀장"
  },
  "message": "보드가 이관되었습니다"
}
```

---

### 6.11 공유 사용자 목록
보드의 공유 사용자 목록을 조회합니다.

```http
GET /api/boards/{id}/shares
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "userId": 10,
      "username": "user123",
      "name": "홍길동",
      "email": "hong@taskflow.com",
      "permission": "EDIT",
      "sharedAt": "2024-12-01T10:00:00"
    }
  ]
}
```

---

### 6.12 공유 사용자 추가
보드에 공유 사용자를 추가합니다.

```http
POST /api/boards/{id}/shares
```

#### Request Body
```json
{
  "userId": 10,
  "permission": "EDIT"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | ✅ | 공유 대상 사용자 ID |
| permission | String | ✅ | 권한 (EDIT/VIEW) |

#### Response (201 Created)
```json
{
  "success": true,
  "data": {
    "userId": 10,
    "username": "user123",
    "name": "홍길동",
    "email": "hong@taskflow.com",
    "permission": "EDIT",
    "sharedAt": "2024-12-24T10:30:00"
  },
  "message": "공유 사용자가 추가되었습니다"
}
```

---

### 6.13 공유 권한 변경
공유 사용자의 권한을 변경합니다.

```http
PUT /api/boards/{id}/shares/{userId}
```

#### Request Body
```json
{
  "permission": "VIEW"
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "권한이 변경되었습니다"
}
```

---

### 6.14 공유 사용자 제거
보드 공유를 해제합니다.

```http
DELETE /api/boards/{id}/shares/{userId}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "공유가 해제되었습니다"
}
```

---

## 7. 아이템 API

### 7.1 아이템 목록 조회
보드 내 아이템 목록을 필터링하여 조회합니다.

```http
GET /api/boards/{boardId}/items
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| keyword | String | - | 검색어 (제목, 내용) |
| status | String | - | 상태 필터 (NOT_STARTED, IN_PROGRESS, COMPLETED, DELETED) |
| priority | String | - | 우선순위 (URGENT, HIGH, NORMAL, LOW) |
| assigneeId | Long | - | 담당자 ID |
| groupId | Long | - | 그룹 ID |
| startDate | Date | - | 시작일 필터 (YYYY-MM-DD) |
| endDate | Date | - | 종료일 필터 (YYYY-MM-DD) |
| includeCompleted | Boolean | - | 완료 항목 포함 (기본: false) |
| includeDeleted | Boolean | - | 삭제 항목 포함 (기본: false) |
| page | Integer | - | 페이지 번호 (기본: 0) |
| size | Integer | - | 페이지 크기 (기본: 20) |
| sort | String | - | 정렬 (기본: createdAt,desc) |

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "itemId": 100,
        "boardId": 1,
        "title": "API 설계",
        "content": "REST API 명세서 작성",
        "status": "IN_PROGRESS",
        "priority": "HIGH",
        "assigneeId": 10,
        "assigneeName": "홍길동",
        "groupId": 1,
        "groupName": "신규 서비스 개발",
        "startDate": "2024-12-20",
        "dueDate": "2024-12-25",
        "completedDate": null,
        "deletedDate": null,
        "commentCount": 3,
        "fileCount": 2,
        "properties": {
          "category": "개발",
          "estimated_hours": 16
        },
        "createdAt": "2024-12-20T09:00:00",
        "createdBy": 1,
        "createdByName": "관리자",
        "updatedAt": "2024-12-24T10:00:00",
        "updatedBy": 10,
        "updatedByName": "홍길동"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3
  }
}
```

---

### 7.2 아이템 생성
새로운 아이템을 생성합니다.

```http
POST /api/boards/{boardId}/items
```

#### Request Body
```json
{
  "title": "데이터베이스 설계",
  "content": "ERD 작성 및 테이블 정의",
  "status": "NOT_STARTED",
  "priority": "HIGH",
  "assigneeId": 10,
  "groupId": 1,
  "startDate": "2024-12-25",
  "dueDate": "2024-12-30",
  "properties": {
    "category": "설계",
    "estimated_hours": 24
  }
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | String | ✅ | 아이템 제목 |
| content | String | - | 아이템 내용 |
| status | String | - | 상태 (기본: NOT_STARTED) |
| priority | String | - | 우선순위 (기본: NORMAL) |
| assigneeId | Long | - | 담당자 ID |
| groupId | Long | - | 그룹 ID |
| startDate | Date | - | 시작일 |
| dueDate | Date | - | 마감일 |
| properties | Object | - | 동적 속성 값 |

#### Response (201 Created)
```json
{
  "success": true,
  "data": {
    "itemId": 101,
    "boardId": 1,
    "title": "데이터베이스 설계",
    "content": "ERD 작성 및 테이블 정의",
    "status": "NOT_STARTED",
    "priority": "HIGH",
    "assigneeId": 10,
    "assigneeName": "홍길동",
    "groupId": 1,
    "groupName": "신규 서비스 개발",
    "startDate": "2024-12-25",
    "dueDate": "2024-12-30",
    "properties": {
      "category": "설계",
      "estimated_hours": 24
    },
    "createdAt": "2024-12-24T10:30:00",
    "createdBy": 1,
    "createdByName": "관리자"
  },
  "message": "아이템이 생성되었습니다"
}
```

---

### 7.3 아이템 조회
특정 아이템의 상세 정보를 조회합니다.

```http
GET /api/boards/{boardId}/items/{id}
```

#### Response (200 OK)
아이템 생성 응답과 동일

---

### 7.4 아이템 수정
아이템 정보를 수정합니다.

```http
PUT /api/boards/{boardId}/items/{id}
```

#### Request Body
```json
{
  "title": "데이터베이스 설계 (수정)",
  "content": "ERD 작성 및 테이블 정의 - 리뷰 반영",
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "assigneeId": 10,
  "groupId": 1,
  "startDate": "2024-12-25",
  "dueDate": "2024-12-31",
  "properties": {
    "category": "설계",
    "estimated_hours": 32
  }
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": { /* 수정된 아이템 정보 */ },
  "message": "아이템이 수정되었습니다"
}
```

---

### 7.5 아이템 삭제 (논리 삭제)
아이템을 논리 삭제합니다. (상태를 DELETED로 변경)

```http
DELETE /api/boards/{boardId}/items/{id}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "itemId": 101,
    "status": "DELETED",
    "deletedDate": "2024-12-24T10:30:00"
  },
  "message": "아이템이 삭제되었습니다"
}
```

---

### 7.6 아이템 완료 처리
아이템의 상태를 완료로 변경합니다.

```http
PUT /api/boards/{boardId}/items/{id}/complete
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "itemId": 101,
    "status": "COMPLETED",
    "completedDate": "2024-12-24T10:30:00"
  },
  "message": "아이템이 완료되었습니다"
}
```

---

### 7.7 아이템 복원
삭제된 아이템을 복원합니다.

```http
PUT /api/boards/{boardId}/items/{id}/restore
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "itemId": 101,
    "status": "IN_PROGRESS",
    "deletedDate": null
  },
  "message": "아이템이 복원되었습니다"
}
```

---

### 7.8 오늘 완료/삭제된 아이템 목록
당일 완료 또는 삭제된 아이템 목록을 조회합니다. (Hidden 처리용)

```http
GET /api/boards/{boardId}/items/today-completed
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "itemId": 100,
      "title": "API 설계",
      "status": "COMPLETED",
      "completedDate": "2024-12-24T09:30:00"
    },
    {
      "itemId": 102,
      "title": "기획서 작성",
      "status": "DELETED",
      "deletedDate": "2024-12-24T14:00:00"
    }
  ]
}
```

---

### 7.9 보드별 활성 아이템 목록
완료/삭제를 제외한 활성 아이템 목록을 조회합니다.

```http
GET /api/boards/{boardId}/items/active
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "itemId": 101,
      "title": "데이터베이스 설계",
      "status": "IN_PROGRESS",
      "priority": "HIGH"
    }
  ]
}
```

---

### 7.10 개별 업무 이관
아이템을 다른 보드 또는 사용자에게 이관합니다.

```http
PUT /api/boards/{boardId}/items/{id}/transfer
```

#### Request Body
```json
{
  "targetBoardId": 5,
  "targetUserId": null
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| targetBoardId | Long | - | 대상 보드 ID |
| targetUserId | Long | - | 대상 사용자 ID (기본 보드로 이관) |

**참고**: targetBoardId 또는 targetUserId 중 하나는 필수

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "itemId": 101,
    "boardId": 5,
    "title": "데이터베이스 설계",
    "transferredAt": "2024-12-24T10:30:00"
  },
  "message": "업무가 이관되었습니다"
}
```

---

### 7.11 업무 이관 가능 여부 확인
현재 사용자가 해당 업무를 이관할 권한이 있는지 확인합니다.

```http
GET /api/boards/{boardId}/items/{id}/can-transfer
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": true
}
```

---

### 7.12 업무 공유 가능 여부 확인
현재 사용자가 해당 업무를 공유할 권한이 있는지 확인합니다.

```http
GET /api/boards/{boardId}/items/{id}/can-share
```

---

### 7.13 업무 권한 조회
현재 사용자의 해당 업무에 대한 권한을 조회합니다.

```http
GET /api/boards/{boardId}/items/{id}/permission
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": "OWNER"
}
```

**권한 종류**: OWNER, EDIT, VIEW

---

## 8. Cross-board 아이템 API

여러 보드에 걸친 아이템을 조회하는 API입니다.

### 8.1 지연 업무 목록
마감일이 지난 업무 목록을 조회합니다.

```http
GET /api/items/overdue
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| keyword | String | - | 검색어 |
| priority | String | - | 우선순위 |
| assigneeId | Long | - | 담당자 ID |
| groupId | Long | - | 그룹 ID |
| boardId | Long | - | 보드 ID 필터 |
| startDate | Date | - | 시작일 |
| endDate | Date | - | 종료일 |
| page | Integer | - | 페이지 번호 |
| size | Integer | - | 페이지 크기 |
| sort | String | - | 정렬 (기본: dueDate,asc) |

#### Response (200 OK)
페이징된 아이템 목록 (7.1과 동일 형식)

---

### 8.2 보류 업무 목록
상태가 PENDING인 업무 목록을 조회합니다.

```http
GET /api/items/pending
```

#### Query Parameters
7.1과 동일

---

### 8.3 활성 업무 목록 (Cross-board)
완료/삭제를 제외한 모든 활성 업무를 조회합니다.

```http
GET /api/items/active
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| keyword | String | - | 검색어 |
| status | String | - | 상태 필터 |
| priority | String | - | 우선순위 |
| assigneeId | Long | - | 담당자 ID |
| groupId | Long | - | 그룹 ID |
| boardId | Long | - | 보드 ID 필터 |
| overdueOnly | Boolean | - | 지연 업무만 (기본: false) |
| startDate | Date | - | 시작일 |
| endDate | Date | - | 종료일 |
| page | Integer | - | 페이지 번호 |
| size | Integer | - | 페이지 크기 |
| sort | String | - | 정렬 (기본: createdAt,desc) |

---

### 8.4 업무 통계
업무 통계 정보를 조회합니다.

```http
GET /api/items/stats
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "totalCount": 150,
    "activeCount": 80,
    "completedCount": 50,
    "deletedCount": 20,
    "overdueCount": 15,
    "pendingCount": 5,
    "byPriority": {
      "URGENT": 10,
      "HIGH": 30,
      "NORMAL": 50,
      "LOW": 10
    },
    "byStatus": {
      "NOT_STARTED": 20,
      "IN_PROGRESS": 50,
      "PENDING": 5,
      "COMPLETED": 50,
      "DELETED": 20
    }
  }
}
```

---

## 9. 댓글 API

### 9.1 댓글 목록 조회
아이템의 댓글 목록을 조회합니다.

```http
GET /api/items/{itemId}/comments
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "commentId": 1,
      "itemId": 100,
      "content": "API 설계 검토 완료했습니다.",
      "createdBy": 10,
      "createdByName": "홍길동",
      "createdAt": "2024-12-24T09:30:00",
      "updatedAt": null
    }
  ]
}
```

---

### 9.2 댓글 등록
아이템에 새로운 댓글을 등록합니다.

```http
POST /api/items/{itemId}/comments
```

#### Request Body
```json
{
  "content": "수정 사항 반영했습니다."
}
```

#### Response (201 Created)
```json
{
  "success": true,
  "data": {
    "commentId": 2,
    "itemId": 100,
    "content": "수정 사항 반영했습니다.",
    "createdBy": 1,
    "createdByName": "관리자",
    "createdAt": "2024-12-24T10:30:00"
  },
  "message": "댓글이 등록되었습니다"
}
```

---

### 9.3 댓글 수정
댓글을 수정합니다.

```http
PUT /api/comments/{id}
```

#### Request Body
```json
{
  "content": "수정 사항 반영 완료했습니다."
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "commentId": 2,
    "content": "수정 사항 반영 완료했습니다.",
    "updatedAt": "2024-12-24T11:00:00"
  },
  "message": "댓글이 수정되었습니다"
}
```

---

### 9.4 댓글 삭제
댓글을 삭제합니다.

```http
DELETE /api/comments/{id}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "댓글이 삭제되었습니다"
}
```

---

## 10. 업무 공유 API

### 10.1 업무 공유 목록 조회
특정 업무의 공유 목록을 조회합니다.

```http
GET /api/items/{itemId}/shares
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "userId": 10,
      "username": "user123",
      "name": "홍길동",
      "email": "hong@taskflow.com",
      "permission": "EDIT",
      "sharedAt": "2024-12-20T10:00:00"
    }
  ]
}
```

---

### 10.2 업무 공유 추가
업무를 다른 사용자와 공유합니다.

```http
POST /api/items/{itemId}/shares
```

#### Request Body
```json
{
  "userId": 15,
  "permission": "EDIT"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | Long | ✅ | 공유 대상 사용자 ID |
| permission | String | ✅ | 권한 (EDIT/VIEW) |

#### Response (201 Created)
```json
{
  "success": true,
  "data": null,
  "message": "공유가 추가되었습니다"
}
```

---

### 10.3 공유 권한 변경
공유 사용자의 권한을 변경합니다.

```http
PUT /api/items/{itemId}/shares/{userId}
```

#### Request Body
```json
{
  "permission": "VIEW"
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "권한이 변경되었습니다"
}
```

---

### 10.4 업무 공유 제거
업무 공유를 해제합니다.

```http
DELETE /api/items/{itemId}/shares/{userId}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "공유가 해제되었습니다"
}
```

---

## 11. 속성 정의 API

### 11.1 속성 정의 목록 조회
보드의 속성 정의 목록을 조회합니다.

```http
GET /api/boards/{boardId}/properties
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| useYn | String | - | 사용 여부 (Y/N) |
| cached | Boolean | - | 캐시 사용 여부 (기본: false) |

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "propertyId": 1,
      "boardId": 1,
      "propertyName": "카테고리",
      "propertyType": "SELECT",
      "isRequired": false,
      "sortOrder": 1,
      "useYn": "Y",
      "options": [
        {
          "optionId": 1,
          "optionName": "개발",
          "color": "#3B82F6",
          "sortOrder": 1,
          "useYn": "Y"
        },
        {
          "optionId": 2,
          "optionName": "설계",
          "color": "#10B981",
          "sortOrder": 2,
          "useYn": "Y"
        }
      ],
      "createdAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

### 11.2 속성 정의 생성
새로운 속성 정의를 생성합니다.

```http
POST /api/boards/{boardId}/properties
```

#### Request Body
```json
{
  "propertyName": "예상 시간",
  "propertyType": "NUMBER",
  "isRequired": false,
  "sortOrder": 5,
  "useYn": "Y"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| propertyName | String | ✅ | 속성명 |
| propertyType | String | ✅ | 속성 타입 (TEXT, NUMBER, DATE, SELECT, MULTI_SELECT, CHECKBOX, USER) |
| isRequired | Boolean | - | 필수 여부 (기본: false) |
| sortOrder | Integer | - | 정렬 순서 |
| useYn | String | - | 사용 여부 (기본: Y) |

#### Response (201 Created)
```json
{
  "success": true,
  "data": {
    "propertyId": 10,
    "boardId": 1,
    "propertyName": "예상 시간",
    "propertyType": "NUMBER",
    "isRequired": false,
    "sortOrder": 5,
    "useYn": "Y",
    "options": [],
    "createdAt": "2024-12-24T10:30:00"
  },
  "message": "속성이 생성되었습니다"
}
```

---

### 11.3 속성 정의 조회
특정 속성 정의를 조회합니다.

```http
GET /api/properties/{id}
```

---

### 11.4 속성 정의 수정
속성 정의를 수정합니다.

```http
PUT /api/properties/{id}
```

#### Request Body
```json
{
  "propertyName": "예상 작업 시간",
  "isRequired": true,
  "sortOrder": 3,
  "useYn": "Y"
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": { /* 수정된 속성 정의 */ },
  "message": "속성이 수정되었습니다"
}
```

---

### 11.5 속성 정의 삭제
속성 정의를 삭제합니다.

```http
DELETE /api/properties/{id}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "속성이 삭제되었습니다"
}
```

---

## 12. 속성 옵션 API

### 12.1 옵션 목록 조회
속성의 옵션 목록을 조회합니다.

```http
GET /api/properties/{propId}/options
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| useYn | String | - | 사용 여부 (Y/N) |

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "optionId": 1,
      "propertyId": 1,
      "optionName": "개발",
      "color": "#3B82F6",
      "sortOrder": 1,
      "useYn": "Y",
      "createdAt": "2024-01-01T00:00:00"
    }
  ]
}
```

---

### 12.2 옵션 추가
속성에 새로운 옵션을 추가합니다.

```http
POST /api/properties/{propId}/options
```

#### Request Body
```json
{
  "optionName": "테스트",
  "color": "#F59E0B",
  "sortOrder": 4,
  "useYn": "Y"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| optionName | String | ✅ | 옵션명 |
| color | String | - | 표시 색상 (HEX) |
| sortOrder | Integer | - | 정렬 순서 |
| useYn | String | - | 사용 여부 (기본: Y) |

#### Response (201 Created)
```json
{
  "success": true,
  "data": {
    "optionId": 10,
    "propertyId": 1,
    "optionName": "테스트",
    "color": "#F59E0B",
    "sortOrder": 4,
    "useYn": "Y",
    "createdAt": "2024-12-24T10:30:00"
  },
  "message": "옵션이 추가되었습니다"
}
```

---

### 12.3 옵션 조회
특정 옵션을 조회합니다.

```http
GET /api/options/{id}
```

---

### 12.4 옵션 수정
옵션을 수정합니다.

```http
PUT /api/options/{id}
```

#### Request Body
```json
{
  "optionName": "단위 테스트",
  "color": "#EF4444",
  "sortOrder": 4,
  "useYn": "Y"
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": { /* 수정된 옵션 */ },
  "message": "옵션이 수정되었습니다"
}
```

---

### 12.5 옵션 삭제
옵션을 삭제합니다.

```http
DELETE /api/options/{id}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "옵션이 삭제되었습니다"
}
```

---

## 13. 작업 템플릿 API

### 13.1 템플릿 목록 조회
작업 템플릿 목록을 조회합니다.

```http
GET /api/task-templates
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "templateId": 1,
      "content": "일일 회의록 작성",
      "useCount": 15,
      "createdBy": 1,
      "createdByName": "관리자",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": "2024-12-20T10:00:00"
    }
  ]
}
```

---

### 13.2 템플릿 단건 조회
특정 템플릿을 조회합니다.

```http
GET /api/task-templates/{id}
```

---

### 13.3 템플릿 검색 (자동완성용)
키워드로 템플릿을 검색합니다.

```http
GET /api/task-templates/search?keyword=회의
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| keyword | String | - | 검색 키워드 |

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "templateId": 1,
      "content": "일일 회의록 작성",
      "useCount": 15
    },
    {
      "templateId": 5,
      "content": "주간 회의 준비",
      "useCount": 8
    }
  ]
}
```

---

### 13.4 템플릿 등록
새로운 템플릿을 등록합니다.

```http
POST /api/task-templates
```

#### Request Body
```json
{
  "content": "월간 보고서 작성"
}
```

#### Response (201 Created)
```json
{
  "success": true,
  "data": {
    "templateId": 10,
    "content": "월간 보고서 작성",
    "useCount": 0,
    "createdBy": 1,
    "createdByName": "관리자",
    "createdAt": "2024-12-24T10:30:00"
  },
  "message": "템플릿이 등록되었습니다"
}
```

---

### 13.5 템플릿 수정
템플릿을 수정합니다.

```http
PUT /api/task-templates/{id}
```

#### Request Body
```json
{
  "content": "월간 업무 보고서 작성"
}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": { /* 수정된 템플릿 */ },
  "message": "템플릿이 수정되었습니다"
}
```

---

### 13.6 템플릿 삭제
템플릿을 삭제합니다.

```http
DELETE /api/task-templates/{id}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "템플릿이 삭제되었습니다"
}
```

---

### 13.7 템플릿 사용
템플릿 사용 횟수를 증가시킵니다.

```http
POST /api/task-templates/{id}/use
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null
}
```

---

## 14. 이력 API

### 14.1 작업 처리 이력 조회
완료/삭제된 아이템 이력을 조회합니다.

```http
GET /api/history/items
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| boardId | Long | - | 보드 ID 필터 |
| result | String | - | 작업 결과 (COMPLETED/DELETED) |
| workerId | Long | - | 작업자 ID |
| startDate | Date | - | 시작일 (YYYY-MM-DD) |
| endDate | Date | - | 종료일 (YYYY-MM-DD) |
| keyword | String | - | 검색 키워드 |
| page | Integer | - | 페이지 번호 (기본: 0) |
| size | Integer | - | 페이지 크기 (기본: 20) |
| sort | String | - | 정렬 (기본: completedAt,desc) |

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "itemId": 100,
        "boardId": 1,
        "boardName": "개인 업무",
        "title": "API 설계",
        "content": "REST API 명세서 작성",
        "result": "COMPLETED",
        "workerId": 10,
        "workerName": "홍길동",
        "createdAt": "2024-12-20T09:00:00",
        "startedAt": "2024-12-20T10:00:00",
        "completedAt": "2024-12-24T09:30:00",
        "updatedAt": "2024-12-24T09:30:00",
        "deletedAt": null
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3
  }
}
```

---

### 14.2 작업 등록 이력 조회
템플릿 등록 이력을 조회합니다.

```http
GET /api/history/templates
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| status | String | - | 상태 (활성/비활성) |
| createdBy | Long | - | 등록자 ID |
| startDate | Date | - | 시작일 |
| endDate | Date | - | 종료일 |
| keyword | String | - | 검색 키워드 |
| page | Integer | - | 페이지 번호 |
| size | Integer | - | 페이지 크기 |
| sort | String | - | 정렬 (기본: createdAt,desc) |

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "templateId": 1,
        "content": "일일 회의록 작성",
        "createdBy": 1,
        "createdByName": "관리자",
        "createdAt": "2024-01-01T00:00:00",
        "updatedAt": "2024-12-20T10:00:00",
        "status": "활성",
        "useCount": 15
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 30,
    "totalPages": 2
  }
}
```

---

## 15. 파일 API

### 15.1 파일 업로드
파일을 업로드합니다.

```http
POST /api/files/upload
Content-Type: multipart/form-data
```

#### Request (multipart/form-data)
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| file | File | ✅ | 업로드할 파일 |
| relatedType | String | - | 연관 엔티티 타입 (ITEM, COMMENT) |
| relatedId | Long | - | 연관 엔티티 ID |

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "fileId": 100,
    "originalName": "설계서.pdf",
    "storedName": "20241224_103045_abc123.pdf",
    "filePath": "/uploads/2024/12/24/20241224_103045_abc123.pdf",
    "fileSize": 2048576,
    "mimeType": "application/pdf",
    "relatedType": "ITEM",
    "relatedId": 50,
    "uploadedAt": "2024-12-24T10:30:45"
  }
}
```

---

### 15.2 파일 다운로드/표시
파일을 다운로드하거나 인라인으로 표시합니다.

```http
GET /api/files/{fileId}?download=true
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| download | Boolean | - | 다운로드 여부 (기본: false - 인라인 표시) |

#### Response
바이너리 파일 스트림

```http
Content-Type: application/pdf
Content-Disposition: attachment; filename*=UTF-8''설계서.pdf
```

---

### 15.3 파일 정보 조회
파일의 메타데이터를 조회합니다.

```http
GET /api/files/{fileId}/info
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "fileId": 100,
    "originalName": "설계서.pdf",
    "fileSize": 2048576,
    "mimeType": "application/pdf",
    "relatedType": "ITEM",
    "relatedId": 50,
    "uploadedBy": 1,
    "uploadedByName": "관리자",
    "uploadedAt": "2024-12-24T10:30:45",
    "isImage": false
  }
}
```

---

### 15.4 파일 삭제
파일을 삭제합니다.

```http
DELETE /api/files/{fileId}
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null
}
```

---

### 15.5 연관 엔티티의 파일 목록
특정 엔티티에 연결된 파일 목록을 조회합니다.

```http
GET /api/files/related/{relatedType}/{relatedId}
```

#### Example
```http
GET /api/files/related/ITEM/50
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": [
    {
      "fileId": 100,
      "originalName": "설계서.pdf",
      "fileSize": 2048576,
      "mimeType": "application/pdf",
      "uploadedAt": "2024-12-24T10:30:45"
    }
  ]
}
```

---

### 15.6 파일 연관 정보 업데이트
업로드된 파일의 연관 정보를 업데이트합니다.

```http
PUT /api/files/{fileId}/related?relatedType=ITEM&relatedId=50
```

#### Query Parameters
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| relatedType | String | ✅ | 연관 엔티티 타입 |
| relatedId | Long | ✅ | 연관 엔티티 ID |

#### Response (200 OK)
```json
{
  "success": true,
  "data": null
}
```

---

### 15.7 이미지 파일 여부 확인
파일이 이미지인지 확인합니다.

```http
GET /api/files/{fileId}/is-image
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": true
}
```

---

## 16. SSE (실시간 동기화) API

### 16.1 SSE 연결 수립
Server-Sent Events 연결을 수립합니다.

```http
GET /api/sse/subscribe
Accept: text/event-stream
```

#### Response (text/event-stream)
```
data: {"type":"connected","message":"SSE 연결 성공"}

data: {"type":"heartbeat","timestamp":"2024-12-24T10:30:00"}

data: {"type":"item:created","data":{"itemId":101,"boardId":1,"title":"새 업무"}}

data: {"type":"item:updated","data":{"itemId":100,"status":"COMPLETED"}}
```

#### 이벤트 타입
| 타입 | 설명 |
|------|------|
| connected | 연결 수립 |
| heartbeat | 하트비트 (3초마다) |
| item:created | 아이템 생성 |
| item:updated | 아이템 수정 |
| item:deleted | 아이템 삭제 |
| property:updated | 속성 정의 변경 |
| comment:created | 댓글 생성 |

---

### 16.2 보드 구독
특정 보드의 이벤트를 구독합니다.

```http
POST /api/sse/boards/{boardId}/subscribe
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "보드 구독이 완료되었습니다"
}
```

---

### 16.3 보드 구독 해제
특정 보드의 이벤트 구독을 해제합니다.

```http
DELETE /api/sse/boards/{boardId}/subscribe
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "보드 구독이 해제되었습니다"
}
```

---

### 16.4 SSE 연결 상태 조회
현재 SSE 연결 상태를 조회합니다.

```http
GET /api/sse/status
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": {
    "connected": true,
    "totalConnections": 15
  }
}
```

---

### 16.5 SSE 연결 해제
SSE 연결을 수동으로 해제합니다.

```http
DELETE /api/sse/subscribe
```

#### Response (200 OK)
```json
{
  "success": true,
  "data": null,
  "message": "SSE 연결이 해제되었습니다"
}
```

---

## 17. 에러 응답 예시

### 17.1 유효성 검증 실패 (400 Bad Request)
```json
{
  "success": false,
  "data": null,
  "message": "제목은 필수 입력 항목입니다"
}
```

### 17.2 인증 필요 (401 Unauthorized)
```json
{
  "success": false,
  "data": null,
  "message": "인증이 필요합니다"
}
```

### 17.3 권한 없음 (403 Forbidden)
```json
{
  "success": false,
  "data": null,
  "message": "보드에 접근 권한이 없습니다"
}
```

### 17.4 리소스 없음 (404 Not Found)
```json
{
  "success": false,
  "data": null,
  "message": "아이템을 찾을 수 없습니다"
}
```

### 17.5 중복 오류 (409 Conflict)
```json
{
  "success": false,
  "data": null,
  "message": "이미 존재하는 사용자 아이디입니다"
}
```

### 17.6 서버 오류 (500 Internal Server Error)
```json
{
  "success": false,
  "data": null,
  "message": "서버 오류가 발생했습니다"
}
```

---

## 18. 버전 정보

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 2.0 | 2024-12-24 | 최초 작성 - 실제 Controller 기반 API 명세 |

---

## 부록 A. 공통 코드

### A.1 아이템 상태 (Status)
| 코드 | 설명 |
|------|------|
| NOT_STARTED | 시작 전 |
| IN_PROGRESS | 진행 중 |
| PENDING | 보류 |
| COMPLETED | 완료 |
| DELETED | 삭제 |

### A.2 우선순위 (Priority)
| 코드 | 설명 |
|------|------|
| URGENT | 긴급 |
| HIGH | 높음 |
| NORMAL | 보통 |
| LOW | 낮음 |

### A.3 권한 (Permission)
| 코드 | 설명 |
|------|------|
| OWNER | 소유자 (모든 권한) |
| EDIT | 편집 권한 |
| VIEW | 조회 권한 |

### A.4 속성 타입 (PropertyType)
| 코드 | 설명 |
|------|------|
| TEXT | 텍스트 |
| NUMBER | 숫자 |
| DATE | 날짜 |
| SELECT | 단일 선택 |
| MULTI_SELECT | 다중 선택 |
| CHECKBOX | 체크박스 |
| USER | 사용자 |

### A.5 파일 연관 타입 (RelatedType)
| 코드 | 설명 |
|------|------|
| ITEM | 아이템 |
| COMMENT | 댓글 |

---

## 부록 B. 개발 환경

### B.1 로컬 개발 환경
```
Base URL: http://localhost:8080
Frontend: http://localhost:3000
Database: MySQL 8.0 (localhost:3306)
```

### B.2 테스트 계정
```
관리자: admin / Password123!
일반 사용자: user123 / Password123!
```

### B.3 Postman Collection
API 테스트를 위한 Postman Collection은 `/docs/2.0/postman/` 디렉토리에서 확인할 수 있습니다.

---

**문서 끝**
