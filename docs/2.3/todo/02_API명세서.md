# Todo List 기능 - API 명세서

> **버전**: 2.3.0
> **작성일**: 2025-01-18
> **상태**: 승인 대기
> **이전 단계**: 01_ERD_테이블정의서.md (승인 완료)

---

## 1. API 개요

### 1.1 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL | `/api` |
| 인증 | JWT Bearer Token |
| Content-Type | application/json |

### 1.2 API 목록

| 구분 | 메서드 | 경로 | 설명 |
|------|--------|------|------|
| **Todo** | GET | /todos | 내 Todo 목록 조회 |
| | POST | /todos | Todo 생성 |
| | GET | /todos/{id} | Todo 상세 조회 |
| | PUT | /todos/{id} | Todo 수정 |
| | DELETE | /todos/{id} | Todo 삭제 |
| | PUT | /todos/{id}/complete | Todo 완료/미완료 토글 |
| | PUT | /todos/{id}/order | Todo 순서 변경 |
| **Todo 공유** | GET | /todos/{id}/shares | Todo 공유 목록 |
| | POST | /todos/{id}/shares | Todo 공유 추가 |
| | PUT | /todos/{id}/shares/{shareId} | 공유 권한 변경 |
| | DELETE | /todos/{id}/shares/{shareId} | Todo 공유 해제 |
| | GET | /todos/shared | 공유받은 Todo 목록 |
| **Todo 이관** | PUT | /todos/{id}/transfer | 단일 Todo 이관 |
| | PUT | /todos/transfer/bulk | 일괄 Todo 이관 |
| | GET | /todos/count | 사용자별 Todo 개수 |
| **체크리스트** | GET | /items/{itemId}/checklist | 체크리스트 목록 |
| | POST | /items/{itemId}/checklist | 체크리스트 추가 |
| | PUT | /checklist/{id} | 체크리스트 수정 |
| | DELETE | /checklist/{id} | 체크리스트 삭제 |
| | PUT | /checklist/{id}/complete | 체크리스트 완료 토글 |
| | PUT | /checklist/reorder | 체크리스트 순서 변경 |

---

## 2. Todo API

### 2.1 Todo 목록 조회

내 Todo 및 공유받은 Todo 목록을 조회합니다.

```
GET /api/todos
```

#### 요청 파라미터 (Query)

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| filter | string | N | all | 필터 (all/today/upcoming/overdue/completed) |
| priority | string | N | - | 우선순위 필터 (URGENT/HIGH/NORMAL/LOW) |
| includeShared | boolean | N | true | 공유받은 Todo 포함 여부 |
| includeCompleted | boolean | N | false | 완료된 Todo 포함 여부 |
| keyword | string | N | - | 검색어 (제목, 설명) |
| page | int | N | 0 | 페이지 번호 |
| size | int | N | 50 | 페이지 크기 |
| sort | string | N | dueDate,asc | 정렬 기준 |

#### 응답

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "todoId": 1,
        "title": "회의 자료 준비",
        "description": "월요일 회의용 PPT 작성",
        "priority": "URGENT",
        "dueDate": "2025-01-18",
        "dueTime": "14:00:00",
        "isCompleted": false,
        "completedAt": null,
        "sortOrder": 0,
        "repeatType": null,
        "repeatInterval": null,
        "repeatDays": null,
        "repeatEndDate": null,
        "nextDueDate": null,
        "isOwner": true,
        "ownerUserId": 1,
        "ownerUserName": "홍길동",
        "permission": null,
        "sharedByUserName": null,
        "transferredFromUserName": null,
        "transferredAt": null,
        "shareCount": 2,
        "createdAt": "2025-01-17T10:00:00",
        "updatedAt": "2025-01-17T15:30:00"
      },
      {
        "todoId": 5,
        "title": "프로젝트 회의 참석",
        "description": null,
        "priority": "NORMAL",
        "dueDate": "2025-01-20",
        "dueTime": null,
        "isCompleted": false,
        "completedAt": null,
        "sortOrder": 0,
        "isOwner": false,
        "ownerUserId": 2,
        "ownerUserName": "김팀장",
        "permission": "VIEW",
        "sharedByUserName": "김팀장",
        "transferredFromUserName": null,
        "transferredAt": null,
        "shareCount": 0,
        "createdAt": "2025-01-16T09:00:00",
        "updatedAt": null
      }
    ],
    "page": 0,
    "size": 50,
    "totalElements": 15,
    "totalPages": 1
  },
  "message": null
}
```

---

### 2.2 Todo 생성

```
POST /api/todos
```

#### 요청 본문

```json
{
  "title": "주간 보고서 작성",
  "description": "이번 주 진행 상황 정리",
  "priority": "HIGH",
  "dueDate": "2025-01-20",
  "dueTime": "18:00:00",
  "repeatType": "WEEKLY",
  "repeatInterval": 1,
  "repeatDays": "1",
  "repeatEndDate": "2025-12-31"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | string | Y | Todo 제목 (최대 500자) |
| description | string | N | 메모/설명 |
| priority | string | N | 우선순위 (기본: NORMAL) |
| dueDate | date | N | 마감일 (YYYY-MM-DD) |
| dueTime | time | N | 마감 시간 (HH:mm:ss) |
| repeatType | string | N | 반복 유형 |
| repeatInterval | int | N | 반복 간격 (기본: 1) |
| repeatDays | string | N | 반복 요일 (1,2,3,4,5) |
| repeatEndDate | date | N | 반복 종료일 |

#### 응답

```json
{
  "success": true,
  "data": {
    "todoId": 10,
    "title": "주간 보고서 작성",
    "description": "이번 주 진행 상황 정리",
    "priority": "HIGH",
    "dueDate": "2025-01-20",
    "dueTime": "18:00:00",
    "isCompleted": false,
    "repeatType": "WEEKLY",
    "repeatInterval": 1,
    "repeatDays": "1",
    "repeatEndDate": "2025-12-31",
    "nextDueDate": "2025-01-27",
    "createdAt": "2025-01-18T10:00:00"
  },
  "message": "Todo가 생성되었습니다."
}
```

---

### 2.3 Todo 상세 조회

```
GET /api/todos/{id}
```

#### 경로 파라미터

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | long | Todo ID |

#### 응답

```json
{
  "success": true,
  "data": {
    "todoId": 1,
    "title": "회의 자료 준비",
    "description": "월요일 회의용 PPT 작성",
    "priority": "URGENT",
    "dueDate": "2025-01-18",
    "dueTime": "14:00:00",
    "isCompleted": false,
    "completedAt": null,
    "sortOrder": 0,
    "repeatType": null,
    "isOwner": true,
    "ownerUserId": 1,
    "ownerUserName": "홍길동",
    "permission": null,
    "shares": [
      {
        "shareId": 1,
        "sharedUserId": 2,
        "sharedUserName": "김대리",
        "permission": "EDIT",
        "createdAt": "2025-01-17T11:00:00"
      }
    ],
    "createdAt": "2025-01-17T10:00:00",
    "createdBy": "hong",
    "updatedAt": "2025-01-17T15:30:00",
    "updatedBy": "hong"
  },
  "message": null
}
```

---

### 2.4 Todo 수정

```
PUT /api/todos/{id}
```

#### 요청 본문

```json
{
  "title": "회의 자료 준비 (수정)",
  "description": "월요일 오전 회의용 PPT 작성",
  "priority": "HIGH",
  "dueDate": "2025-01-18",
  "dueTime": "10:00:00"
}
```

#### 권한
- 소유자: 모든 필드 수정 가능
- 공유받은 사용자 (EDIT): 완료 상태, 설명만 수정 가능

#### 응답

```json
{
  "success": true,
  "data": {
    "todoId": 1,
    "title": "회의 자료 준비 (수정)",
    "priority": "HIGH",
    "dueDate": "2025-01-18",
    "dueTime": "10:00:00",
    "updatedAt": "2025-01-18T09:00:00"
  },
  "message": "Todo가 수정되었습니다."
}
```

---

### 2.5 Todo 삭제

```
DELETE /api/todos/{id}
```

#### 권한
- 소유자만 삭제 가능

#### 응답

```json
{
  "success": true,
  "data": null,
  "message": "Todo가 삭제되었습니다."
}
```

---

### 2.6 Todo 완료/미완료 토글

```
PUT /api/todos/{id}/complete
```

#### 요청 본문

```json
{
  "isCompleted": true
}
```

#### 응답

```json
{
  "success": true,
  "data": {
    "todoId": 1,
    "isCompleted": true,
    "completedAt": "2025-01-18T14:30:00",
    "nextDueDate": "2025-01-25"
  },
  "message": "Todo가 완료 처리되었습니다."
}
```

#### 반복 Todo 완료 시
- `isCompleted` = true로 변경
- `nextDueDate` 계산하여 업데이트
- 반복 종료일 이전이면 다음 반복 Todo 자동 생성 (선택적)

---

### 2.7 Todo 순서 변경

```
PUT /api/todos/{id}/order
```

#### 요청 본문

```json
{
  "sortOrder": 2,
  "targetDate": "2025-01-18"
}
```

#### 응답

```json
{
  "success": true,
  "data": null,
  "message": "순서가 변경되었습니다."
}
```

---

## 3. Todo 공유 API

### 3.1 공유 목록 조회

```
GET /api/todos/{id}/shares
```

#### 응답

```json
{
  "success": true,
  "data": [
    {
      "shareId": 1,
      "todoId": 1,
      "sharedUserId": 2,
      "sharedUserName": "김대리",
      "sharedUserDepartment": "개발팀",
      "permission": "EDIT",
      "sharedByUserId": 1,
      "sharedByUserName": "홍길동",
      "createdAt": "2025-01-17T11:00:00"
    }
  ],
  "message": null
}
```

---

### 3.2 공유 추가

```
POST /api/todos/{id}/shares
```

#### 요청 본문

```json
{
  "sharedUserId": 3,
  "permission": "VIEW"
}
```

또는 여러 사용자 동시 공유:

```json
{
  "shares": [
    { "sharedUserId": 3, "permission": "VIEW" },
    { "sharedUserId": 4, "permission": "EDIT" }
  ]
}
```

#### 응답

```json
{
  "success": true,
  "data": {
    "shareId": 2,
    "sharedUserId": 3,
    "sharedUserName": "박사원",
    "permission": "VIEW",
    "createdAt": "2025-01-18T10:00:00"
  },
  "message": "Todo가 공유되었습니다."
}
```

---

### 3.3 공유 권한 변경

```
PUT /api/todos/{id}/shares/{shareId}
```

#### 요청 본문

```json
{
  "permission": "EDIT"
}
```

#### 응답

```json
{
  "success": true,
  "data": {
    "shareId": 2,
    "permission": "EDIT"
  },
  "message": "공유 권한이 변경되었습니다."
}
```

---

### 3.4 공유 해제

```
DELETE /api/todos/{id}/shares/{shareId}
```

#### 응답

```json
{
  "success": true,
  "data": null,
  "message": "공유가 해제되었습니다."
}
```

---

### 3.5 공유받은 Todo 목록

```
GET /api/todos/shared
```

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| permission | string | N | - | 권한 필터 (VIEW/EDIT) |
| includeCompleted | boolean | N | false | 완료 포함 |

#### 응답

```json
{
  "success": true,
  "data": [
    {
      "todoId": 5,
      "title": "프로젝트 회의 참석",
      "priority": "NORMAL",
      "dueDate": "2025-01-20",
      "isCompleted": false,
      "ownerUserName": "김팀장",
      "permission": "VIEW",
      "sharedAt": "2025-01-16T09:00:00"
    }
  ],
  "message": null
}
```

---

## 4. Todo 이관 API

### 4.1 단일 Todo 이관

```
PUT /api/todos/{id}/transfer
```

#### 요청 본문

```json
{
  "toUserId": 5
}
```

#### 응답

```json
{
  "success": true,
  "data": {
    "todoId": 1,
    "newOwnerUserId": 5,
    "newOwnerUserName": "이신입",
    "transferredAt": "2025-01-18T10:00:00"
  },
  "message": "Todo가 이관되었습니다."
}
```

---

### 4.2 일괄 Todo 이관 (사용자 삭제 시)

```
PUT /api/todos/transfer/bulk
```

#### 요청 본문

```json
{
  "fromUserId": 1,
  "toUserId": 5,
  "todoIds": [1, 2, 3]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| fromUserId | long | Y | 이관 출처 사용자 ID |
| toUserId | long | Y | 이관 대상 사용자 ID |
| todoIds | long[] | N | 특정 Todo만 이관 (없으면 전체) |

#### 응답

```json
{
  "success": true,
  "data": {
    "transferredCount": 3,
    "toUserName": "이신입"
  },
  "message": "3개의 Todo가 이관되었습니다."
}
```

---

### 4.3 사용자별 Todo 개수 조회

사용자 삭제 전 Todo 존재 여부 확인용

```
GET /api/todos/count
```

#### 요청 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| userId | long | Y | 사용자 ID |

#### 응답

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "userName": "홍길동",
    "totalCount": 5,
    "activeCount": 3,
    "completedCount": 2
  },
  "message": null
}
```

---

## 5. 체크리스트 API

### 5.1 체크리스트 목록 조회

```
GET /api/items/{itemId}/checklist
```

#### 응답

```json
{
  "success": true,
  "data": {
    "itemId": 100,
    "totalCount": 5,
    "completedCount": 2,
    "items": [
      {
        "checklistId": 1,
        "title": "DB 스키마 설계",
        "isCompleted": true,
        "completedAt": "2025-01-15T10:00:00",
        "completedBy": "hong",
        "sortOrder": 0,
        "dueDate": null,
        "assigneeUsername": null,
        "assigneeUserName": null
      },
      {
        "checklistId": 2,
        "title": "API 개발",
        "isCompleted": true,
        "completedAt": "2025-01-16T15:00:00",
        "completedBy": "hong",
        "sortOrder": 1,
        "dueDate": null,
        "assigneeUsername": null,
        "assigneeUserName": null
      },
      {
        "checklistId": 3,
        "title": "프론트엔드 개발",
        "isCompleted": false,
        "completedAt": null,
        "completedBy": null,
        "sortOrder": 2,
        "dueDate": "2025-01-20",
        "assigneeUsername": "kim",
        "assigneeUserName": "김개발"
      }
    ]
  },
  "message": null
}
```

---

### 5.2 체크리스트 추가

```
POST /api/items/{itemId}/checklist
```

#### 요청 본문

```json
{
  "title": "테스트 코드 작성",
  "dueDate": "2025-01-22",
  "assigneeUsername": "park"
}
```

#### 응답

```json
{
  "success": true,
  "data": {
    "checklistId": 4,
    "title": "테스트 코드 작성",
    "isCompleted": false,
    "sortOrder": 3,
    "dueDate": "2025-01-22",
    "assigneeUsername": "park",
    "assigneeUserName": "박테스터",
    "createdAt": "2025-01-18T10:00:00"
  },
  "message": "체크리스트가 추가되었습니다."
}
```

---

### 5.3 체크리스트 수정

```
PUT /api/checklist/{id}
```

#### 요청 본문

```json
{
  "title": "테스트 코드 작성 (수정)",
  "dueDate": "2025-01-25",
  "assigneeUsername": "lee"
}
```

#### 응답

```json
{
  "success": true,
  "data": {
    "checklistId": 4,
    "title": "테스트 코드 작성 (수정)",
    "dueDate": "2025-01-25",
    "assigneeUsername": "lee",
    "assigneeUserName": "이테스터"
  },
  "message": "체크리스트가 수정되었습니다."
}
```

---

### 5.4 체크리스트 삭제

```
DELETE /api/checklist/{id}
```

#### 응답

```json
{
  "success": true,
  "data": null,
  "message": "체크리스트가 삭제되었습니다."
}
```

---

### 5.5 체크리스트 완료 토글

```
PUT /api/checklist/{id}/complete
```

#### 요청 본문

```json
{
  "isCompleted": true
}
```

#### 응답

```json
{
  "success": true,
  "data": {
    "checklistId": 3,
    "isCompleted": true,
    "completedAt": "2025-01-18T14:00:00",
    "completedBy": "hong"
  },
  "message": "체크리스트가 완료 처리되었습니다."
}
```

---

### 5.6 체크리스트 순서 변경

```
PUT /api/checklist/reorder
```

#### 요청 본문

```json
{
  "itemId": 100,
  "orderedIds": [3, 1, 2, 4, 5]
}
```

#### 응답

```json
{
  "success": true,
  "data": null,
  "message": "순서가 변경되었습니다."
}
```

---

## 6. 에러 응답

### 6.1 공통 에러 코드

| HTTP 코드 | 에러 코드 | 설명 |
|-----------|----------|------|
| 400 | INVALID_REQUEST | 잘못된 요청 |
| 401 | UNAUTHORIZED | 인증 필요 |
| 403 | FORBIDDEN | 권한 없음 |
| 404 | NOT_FOUND | 리소스 없음 |
| 409 | CONFLICT | 충돌 (중복 등) |
| 500 | INTERNAL_ERROR | 서버 오류 |

### 6.2 에러 응답 형식

```json
{
  "success": false,
  "data": null,
  "message": "해당 Todo를 찾을 수 없습니다.",
  "errorCode": "NOT_FOUND"
}
```

### 6.3 Todo 관련 에러

| 상황 | HTTP 코드 | 메시지 |
|------|-----------|--------|
| Todo 없음 | 404 | 해당 Todo를 찾을 수 없습니다. |
| 수정 권한 없음 | 403 | Todo 수정 권한이 없습니다. |
| 삭제 권한 없음 | 403 | Todo 삭제는 소유자만 가능합니다. |
| 이미 공유됨 | 409 | 이미 해당 사용자에게 공유되어 있습니다. |
| 자기 자신 공유 | 400 | 본인에게는 공유할 수 없습니다. |
| 이관 대상 없음 | 400 | 이관 대상 사용자를 선택해주세요. |

---

## 7. 승인 요청

**2단계 API 명세서 승인해 주세요.**

- [ ] Todo CRUD API 승인
- [ ] Todo 공유 API 승인
- [ ] Todo 이관 API 승인
- [ ] 체크리스트 API 승인
- [ ] 에러 응답 승인

---

## 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2025-01-18 | Claude | 최초 작성 |
