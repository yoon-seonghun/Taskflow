# 2. API 명세서

## 2.1 하위 업무 API 엔드포인트

### 기본 URL 패턴
```
/api/boards/{boardId}/items/{itemId}/children
```

---

## 2.2 API 목록

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | /api/boards/{boardId}/items/{itemId}/children | 하위 업무 목록 조회 |
| POST | /api/boards/{boardId}/items/{itemId}/children | 하위 업무 생성 |
| GET | /api/boards/{boardId}/items/{itemId}/tree | 업무 트리 조회 (전체 계층) |
| PUT | /api/boards/{boardId}/items/{itemId}/children/reorder | 하위 업무 순서 변경 |
| GET | /api/boards/{boardId}/items/{itemId}/parent | 부모 업무 조회 |
| GET | /api/boards/{boardId}/items/{itemId}/ancestors | 상위 계층 조회 (경로) |

---

## 2.3 API 상세

### 2.3.1 하위 업무 목록 조회

```
GET /api/boards/{boardId}/items/{itemId}/children
```

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| boardId | Long | Y | 보드 ID |
| itemId | Long | Y | 부모 업무 ID |

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| includeCompleted | Boolean | N | false | 완료된 하위 업무 포함 여부 |
| includeDeleted | Boolean | N | false | 삭제된 하위 업무 포함 여부 |

**Response**
```json
{
  "success": true,
  "data": [
    {
      "itemId": 10,
      "parentItemId": 1,
      "itemDepth": 1,
      "childSortOrder": 1,
      "title": "디자인 작업",
      "status": "IN_PROGRESS",
      "priority": "HIGH",
      "assigneeName": "김디자인",
      "childCount": 2,
      "completedChildCount": 0,
      "createdAt": "2026-01-15T10:00:00",
      "updatedAt": "2026-01-15T14:30:00"
    }
  ]
}
```

---

### 2.3.2 하위 업무 생성

```
POST /api/boards/{boardId}/items/{itemId}/children
```

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| boardId | Long | Y | 보드 ID |
| itemId | Long | Y | 부모 업무 ID |

**Request Body**
```json
{
  "title": "메인 페이지 디자인",
  "description": "메인 페이지 UI/UX 디자인 작업",
  "priority": "HIGH",
  "assigneeUsername": "designer01",
  "dueDate": "2026-01-20",
  "properties": {
    "101": "디자인",
    "102": "2026-01-18"
  }
}
```

**Validation Rules**
| 필드 | 규칙 |
|------|------|
| title | 필수, 1~200자 |
| 부모 depth | 부모가 depth 2이면 생성 불가 (400 에러) |

**Response**
```json
{
  "success": true,
  "data": {
    "itemId": 100,
    "parentItemId": 10,
    "itemDepth": 2,
    "childSortOrder": 1,
    "title": "메인 페이지 디자인",
    "status": "NOT_STARTED",
    "priority": "HIGH",
    "createdAt": "2026-01-15T15:00:00"
  },
  "message": "하위 업무가 생성되었습니다."
}
```

**Error Response (depth 초과)**
```json
{
  "success": false,
  "message": "최대 깊이(3단계)를 초과하여 하위 업무를 생성할 수 없습니다."
}
```

---

### 2.3.3 업무 트리 조회

```
GET /api/boards/{boardId}/items/{itemId}/tree
```

**Description**
업무와 모든 하위 업무를 계층 구조로 조회합니다.

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| boardId | Long | Y | 보드 ID |
| itemId | Long | Y | 업무 ID (기본 업무 또는 하위 업무) |

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| maxDepth | Integer | N | 3 | 조회할 최대 깊이 |
| includeCompleted | Boolean | N | false | 완료된 업무 포함 |

**Response**
```json
{
  "success": true,
  "data": {
    "itemId": 1,
    "parentItemId": null,
    "itemDepth": 0,
    "title": "웹사이트 리뉴얼 프로젝트",
    "status": "IN_PROGRESS",
    "childCount": 3,
    "completedChildCount": 0,
    "children": [
      {
        "itemId": 10,
        "parentItemId": 1,
        "itemDepth": 1,
        "title": "디자인 작업",
        "status": "IN_PROGRESS",
        "childCount": 2,
        "completedChildCount": 0,
        "children": [
          {
            "itemId": 100,
            "parentItemId": 10,
            "itemDepth": 2,
            "title": "메인 페이지 디자인",
            "status": "NOT_STARTED",
            "childCount": 0,
            "children": []
          },
          {
            "itemId": 101,
            "parentItemId": 10,
            "itemDepth": 2,
            "title": "서브 페이지 디자인",
            "status": "NOT_STARTED",
            "childCount": 0,
            "children": []
          }
        ]
      }
    ]
  }
}
```

---

### 2.3.4 하위 업무 순서 변경

```
PUT /api/boards/{boardId}/items/{itemId}/children/reorder
```

**Path Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| boardId | Long | Y | 보드 ID |
| itemId | Long | Y | 부모 업무 ID |

**Request Body**
```json
{
  "orders": [
    { "itemId": 10, "sortOrder": 1 },
    { "itemId": 11, "sortOrder": 2 },
    { "itemId": 12, "sortOrder": 3 }
  ]
}
```

**Response**
```json
{
  "success": true,
  "message": "순서가 변경되었습니다."
}
```

---

### 2.3.5 부모 업무 조회

```
GET /api/boards/{boardId}/items/{itemId}/parent
```

**Description**
하위 업무의 직접 부모 업무를 조회합니다.

**Response**
```json
{
  "success": true,
  "data": {
    "itemId": 1,
    "parentItemId": null,
    "itemDepth": 0,
    "title": "웹사이트 리뉴얼 프로젝트",
    "status": "IN_PROGRESS",
    "boardId": 100,
    "boardName": "개발팀 업무"
  }
}
```

---

### 2.3.6 상위 계층 조회 (경로)

```
GET /api/boards/{boardId}/items/{itemId}/ancestors
```

**Description**
하위 업무에서 최상위 기본 업무까지의 경로를 조회합니다 (Breadcrumb용).

**Response**
```json
{
  "success": true,
  "data": [
    {
      "itemId": 1,
      "itemDepth": 0,
      "title": "웹사이트 리뉴얼 프로젝트"
    },
    {
      "itemId": 10,
      "itemDepth": 1,
      "title": "디자인 작업"
    },
    {
      "itemId": 100,
      "itemDepth": 2,
      "title": "메인 페이지 디자인"
    }
  ]
}
```

---

## 2.4 기존 API 수정

### 2.4.1 아이템 목록 조회 수정

```
GET /api/boards/{boardId}/items
```

**추가 Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| includeChildren | Boolean | N | true | 하위 업무 포함 여부 |
| flatList | Boolean | N | false | 평면 목록 (계층 무시) |
| rootOnly | Boolean | N | false | 기본 업무만 조회 |

**Response 수정**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "itemId": 1,
        "parentItemId": null,
        "itemDepth": 0,
        "title": "웹사이트 리뉴얼 프로젝트",
        "childCount": 3,
        "completedChildCount": 1,
        "hasChildren": true,
        "children": [...]  // includeChildren=true 일 때
      }
    ]
  }
}
```

---

### 2.4.2 아이템 상세 조회 수정

```
GET /api/boards/{boardId}/items/{itemId}
```

**Response 추가 필드**
```json
{
  "success": true,
  "data": {
    "itemId": 100,
    "parentItemId": 10,
    "itemDepth": 2,
    "title": "메인 페이지 디자인",

    "parentInfo": {
      "itemId": 10,
      "title": "디자인 작업",
      "status": "IN_PROGRESS"
    },

    "rootInfo": {
      "itemId": 1,
      "title": "웹사이트 리뉴얼 프로젝트",
      "status": "IN_PROGRESS"
    },

    "childCount": 0,
    "completedChildCount": 0,
    "hasChildren": false,
    "canCreateChild": false
  }
}
```

| 필드 | 설명 |
|------|------|
| parentInfo | 직접 부모 업무 정보 |
| rootInfo | 최상위 기본 업무 정보 |
| childCount | 직접 하위 업무 수 |
| completedChildCount | 완료된 하위 업무 수 |
| hasChildren | 하위 업무 존재 여부 |
| canCreateChild | 하위 업무 생성 가능 여부 (depth < 2) |

---

### 2.4.3 아이템 완료 API 수정

```
PUT /api/boards/{boardId}/items/{itemId}/complete
```

**Request Body 추가**
```json
{
  "forceComplete": false
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| forceComplete | Boolean | 미완료 하위 업무가 있어도 강제 완료 |

**Response (미완료 하위 업무 있을 때)**
```json
{
  "success": false,
  "errorCode": "INCOMPLETE_CHILDREN",
  "message": "미완료 하위 업무가 있습니다.",
  "data": {
    "incompleteChildCount": 3,
    "children": [
      { "itemId": 10, "title": "디자인 작업", "status": "IN_PROGRESS" },
      { "itemId": 11, "title": "퍼블리싱 작업", "status": "NOT_STARTED" }
    ]
  }
}
```

---

### 2.4.4 아이템 이관 API 수정

```
POST /api/boards/{boardId}/items/{itemId}/transfer
```

**Validation 추가**
- 하위 업무(depth > 0)는 이관 불가 → 400 에러

**Error Response**
```json
{
  "success": false,
  "message": "하위 업무는 단독으로 이관할 수 없습니다. 기본 업무를 이관해주세요."
}
```

---

### 2.4.5 아이템 삭제 API 동작 변경

```
DELETE /api/boards/{boardId}/items/{itemId}
```

**동작**
- 기본 업무 삭제 시: 모든 하위 업무도 함께 삭제 (CASCADE)
- 하위 업무 삭제 시: 해당 업무 및 그 하위만 삭제

**Response 추가 정보**
```json
{
  "success": true,
  "message": "업무가 삭제되었습니다.",
  "data": {
    "deletedCount": 5,
    "deletedItems": [1, 10, 11, 100, 101]
  }
}
```

---

## 2.5 배당/공유 API 수정

### 2.5.1 업무 배당 시 하위 업무 처리

```
POST /api/boards/{boardId}/items/{itemId}/assign
```

**동작 변경**
- 기본 업무 배당 시: 모든 하위 업무도 함께 공유 (구조 유지)
- 하위 업무 배당 시: 해당 업무만 배당 (개별 업무로 표시)

**Request Body**
```json
{
  "assigneeUsername": "user01",
  "includeChildren": true,
  "message": "하위 업무도 함께 배당합니다."
}
```

| 필드 | 타입 | 기본값 | 설명 |
|------|------|--------|------|
| includeChildren | Boolean | true | 하위 업무 포함 여부 |

---

## 2.6 권한 처리

### 2.6.1 API 접근 권한 체계

**권한 확인 순서**
1. 보드 소유자/공유 권한 확인 (`boardService.hasAccess`)
2. 아이템 레벨 공유/배당 권한 확인 (`itemShareService.hasItemAccess`)
3. 둘 중 하나라도 만족하면 접근 허용

**적용 대상 API**
| 엔드포인트 | 권한 확인 | 설명 |
|-----------|----------|------|
| GET /{id}/children | Board OR Item | 하위 업무 목록 조회 |
| GET /{id}/tree | Board OR Item | 업무 트리 조회 |
| GET /{id}/ancestors | Board OR Item | 상위 계층 조회 |
| GET /{id}/parent | Board OR Item | 부모 업무 조회 |
| GET /{id}/incomplete-children | Board OR Item | 미완료 하위 업무 조회 |

**권한 확인 코드 패턴**
```java
String currentUsername = SecurityUtils.getCurrentUsername();
boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
boolean hasItemAccess = itemShareService.hasItemAccess(itemId, currentUsername);

if (!hasBoardAccess && !hasItemAccess) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("접근 권한이 없습니다"));
}
```

---

### 2.6.2 공유/배당 업무 접근

**배당받은 업무 접근 시나리오**
```
사용자 A가 업무 "디자인 작업"을 사용자 B에게 배당
→ B는 해당 업무 및 하위 업무의 상위 계층(ancestors),
  하위 목록(children) 등을 조회 가능
→ B는 보드 자체에 대한 권한은 없으나,
  아이템 레벨 공유 권한으로 관련 API 접근 가능
```

---

## 2.7 하위 업무 생성 시 필드 처리

### 2.7.1 담당자(Assignee) 처리

**규칙**: 하위 업무 생성 시 담당자는 **빈칸**으로 처리 (부모 업무로부터 상속하지 않음)

**적용 코드**
```java
// ItemServiceImpl.java - createChildItem()
Item childItem = Item.builder()
    .boardId(parentItem.getBoardId())
    .parentItemId(parentItem.getItemId())
    .rootItemId(rootItemId)
    .itemDepth(parentItem.getItemDepth() + 1)
    .title(request.getTitle())
    .assigneeUsername(request.getAssigneeUsername())  // 명시적 지정된 경우만 설정
    // ... 기타 필드
    .build();
```

**Request Body 참고**
| 필드 | 필수 | 기본값 | 설명 |
|------|------|--------|------|
| assigneeUsername | N | null | 담당자 (지정하지 않으면 빈칸) |

---

## 2.8 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| MAX_DEPTH_EXCEEDED | 400 | 최대 깊이 초과 |
| INCOMPLETE_CHILDREN | 400 | 미완료 하위 업무 존재 |
| CHILD_TRANSFER_NOT_ALLOWED | 400 | 하위 업무 단독 이관 불가 |
| CHILD_MOVE_NOT_ALLOWED | 400 | 하위 업무 보드/그룹 이동 불가 |
| PARENT_NOT_FOUND | 404 | 부모 업무 없음 |
| INVALID_PARENT | 400 | 유효하지 않은 부모 (삭제됨 등) |
| ACCESS_DENIED | 403 | 접근 권한 없음 (보드/아이템 권한 모두 없음) |

---

## 승인 체크리스트

- [x] 하위 업무 CRUD API 승인 ✅ 2026-01-15
- [x] 트리 조회 API 승인 ✅ 2026-01-15
- [x] 기존 API 수정 사항 승인 ✅ 2026-01-15
- [x] 에러 코드 체계 승인 ✅ 2026-01-15
