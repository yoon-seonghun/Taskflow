# TaskFlow DTO 클래스 구조 설계

## 목차
1. [공통 DTO](#1-공통-dto)
2. [인증 DTO](#2-인증-dto)
3. [사용자 DTO](#3-사용자-dto)
4. [부서 DTO](#4-부서-dto)
5. [그룹 DTO](#5-그룹-dto)
6. [보드 DTO](#6-보드-dto)
7. [아이템 DTO](#7-아이템-dto)
8. [댓글 DTO](#8-댓글-dto)
9. [속성 DTO](#9-속성-dto)
10. [옵션 DTO](#10-옵션-dto)
11. [템플릿 DTO](#11-템플릿-dto)
12. [이력 DTO](#12-이력-dto)

---

## 패키지 구조

```
com.taskflow.dto
├── common/
│   ├── ApiResponse.java
│   └── PageResponse.java
├── auth/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── TokenResponse.java
│   └── UserInfoResponse.java
├── user/
│   ├── UserCreateRequest.java
│   ├── UserUpdateRequest.java
│   ├── PasswordChangeRequest.java
│   ├── UserResponse.java
│   └── UserListResponse.java
├── department/
│   ├── DepartmentCreateRequest.java
│   ├── DepartmentUpdateRequest.java
│   ├── DepartmentOrderRequest.java
│   ├── DepartmentResponse.java
│   └── DepartmentTreeResponse.java
├── group/
│   ├── GroupCreateRequest.java
│   ├── GroupUpdateRequest.java
│   ├── GroupOrderRequest.java
│   ├── GroupMemberRequest.java
│   ├── GroupResponse.java
│   └── GroupMemberResponse.java
├── board/
│   ├── BoardCreateRequest.java
│   ├── BoardUpdateRequest.java
│   ├── BoardShareRequest.java
│   ├── BoardResponse.java
│   └── BoardShareResponse.java
├── item/
│   ├── ItemCreateRequest.java
│   ├── ItemUpdateRequest.java
│   ├── ItemPropertyRequest.java
│   ├── ItemResponse.java
│   └── ItemListResponse.java
├── comment/
│   ├── CommentCreateRequest.java
│   ├── CommentUpdateRequest.java
│   └── CommentResponse.java
├── property/
│   ├── PropertyCreateRequest.java
│   ├── PropertyUpdateRequest.java
│   ├── PropertyResponse.java
│   └── PropertyWithOptionsResponse.java
├── option/
│   ├── OptionCreateRequest.java
│   ├── OptionUpdateRequest.java
│   └── OptionResponse.java
├── template/
│   ├── TemplateCreateRequest.java
│   ├── TemplateUpdateRequest.java
│   ├── TemplateResponse.java
│   └── TemplateSearchResponse.java
└── history/
    ├── ItemHistoryResponse.java
    └── TemplateHistoryResponse.java
```

---

## 1. 공통 DTO

### 1.1 ApiResponse<T>
API 공통 응답 wrapper

| 필드 | 타입 | 설명 |
|------|------|------|
| success | boolean | 성공 여부 |
| data | T | 응답 데이터 |
| message | String | 에러 메시지 (실패 시) |

---

### 1.2 PageResponse<T>
페이징 응답 wrapper

| 필드 | 타입 | 설명 |
|------|------|------|
| content | List<T> | 데이터 목록 |
| totalElements | long | 전체 데이터 수 |
| totalPages | int | 전체 페이지 수 |
| size | int | 페이지 크기 |
| number | int | 현재 페이지 번호 |

---

## 2. 인증 DTO

### 2.1 LoginRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| username | String | @NotBlank, @Size(min=4, max=50) | 로그인 ID |
| password | String | @NotBlank | 비밀번호 |

---

### 2.2 LoginResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| accessToken | String | JWT Access Token |
| tokenType | String | "Bearer" |
| expiresIn | long | 만료 시간 (초) |
| user | UserInfoResponse | 사용자 정보 |

---

### 2.3 TokenResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| accessToken | String | JWT Access Token |
| tokenType | String | "Bearer" |
| expiresIn | long | 만료 시간 (초) |

---

### 2.4 UserInfoResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | Long | 사용자 ID |
| username | String | 로그인 ID |
| name | String | 사용자 이름 |
| departmentId | Long | 부서 ID |
| departmentName | String | 부서명 |
| groups | List<GroupSimpleResponse> | 소속 그룹 목록 |

---

## 3. 사용자 DTO

### 3.1 UserCreateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| username | String | @NotBlank, @Size(min=4, max=50), @Pattern(regexp="^[a-zA-Z0-9]+$") | 로그인 ID |
| password | String | @NotBlank, @Size(min=8), @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$") | 비밀번호 |
| passwordConfirm | String | @NotBlank | 비밀번호 확인 |
| name | String | @NotBlank, @Size(min=2, max=100) | 사용자 이름 |
| departmentId | Long | - | 부서 ID |

---

### 3.2 UserUpdateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| name | String | @NotBlank, @Size(min=2, max=100) | 사용자 이름 |
| departmentId | Long | - | 부서 ID |
| useYn | String | @Pattern(regexp="^[YN]$") | 사용 여부 |

---

### 3.3 PasswordChangeRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| currentPassword | String | @NotBlank | 현재 비밀번호 |
| newPassword | String | @NotBlank, @Size(min=8), @Pattern | 새 비밀번호 |
| newPasswordConfirm | String | @NotBlank | 새 비밀번호 확인 |

---

### 3.4 UserResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | Long | 사용자 ID |
| username | String | 로그인 ID |
| name | String | 사용자 이름 |
| departmentId | Long | 부서 ID |
| departmentName | String | 부서명 |
| groups | List<GroupSimpleResponse> | 소속 그룹 목록 |
| useYn | String | 사용 여부 |
| lastLoginAt | LocalDateTime | 마지막 로그인 |
| createdAt | LocalDateTime | 생성일시 |
| updatedAt | LocalDateTime | 수정일시 |

---

### 3.5 UserListResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | Long | 사용자 ID |
| username | String | 로그인 ID |
| name | String | 사용자 이름 |
| departmentId | Long | 부서 ID |
| departmentName | String | 부서명 |
| useYn | String | 사용 여부 |
| lastLoginAt | LocalDateTime | 마지막 로그인 |
| createdAt | LocalDateTime | 생성일시 |

---

### 3.6 UserSimpleResponse
(드롭다운, 참조용)

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | Long | 사용자 ID |
| username | String | 로그인 ID |
| name | String | 사용자 이름 |

---

## 4. 부서 DTO

### 4.1 DepartmentCreateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| departmentCode | String | @NotBlank, @Size(max=20), @Pattern(regexp="^[A-Z0-9]+$") | 부서 코드 |
| departmentName | String | @NotBlank, @Size(max=100) | 부서명 |
| parentId | Long | - | 상위 부서 ID |
| sortOrder | Integer | @Min(0) | 정렬 순서 |

---

### 4.2 DepartmentUpdateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| departmentName | String | @NotBlank, @Size(max=100) | 부서명 |
| parentId | Long | - | 상위 부서 ID |
| sortOrder | Integer | @Min(0) | 정렬 순서 |
| useYn | String | @Pattern(regexp="^[YN]$") | 사용 여부 |

---

### 4.3 DepartmentOrderRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| sortOrder | Integer | @NotNull, @Min(0) | 정렬 순서 |

---

### 4.4 DepartmentResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| departmentId | Long | 부서 ID |
| departmentCode | String | 부서 코드 |
| departmentName | String | 부서명 |
| parentId | Long | 상위 부서 ID |
| parentName | String | 상위 부서명 |
| sortOrder | Integer | 정렬 순서 |
| useYn | String | 사용 여부 |
| userCount | Integer | 소속 사용자 수 |
| createdAt | LocalDateTime | 생성일시 |
| updatedAt | LocalDateTime | 수정일시 |

---

### 4.5 DepartmentTreeResponse
(트리 구조용)

| 필드 | 타입 | 설명 |
|------|------|------|
| departmentId | Long | 부서 ID |
| departmentCode | String | 부서 코드 |
| departmentName | String | 부서명 |
| parentId | Long | 상위 부서 ID |
| sortOrder | Integer | 정렬 순서 |
| useYn | String | 사용 여부 |
| children | List<DepartmentTreeResponse> | 하위 부서 목록 |

---

### 4.6 DepartmentFlatResponse
(평면 구조용)

| 필드 | 타입 | 설명 |
|------|------|------|
| departmentId | Long | 부서 ID |
| departmentCode | String | 부서 코드 |
| departmentName | String | 부서명 |
| parentId | Long | 상위 부서 ID |
| depth | Integer | 계층 깊이 |

---

## 5. 그룹 DTO

### 5.1 GroupCreateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| groupCode | String | @NotBlank, @Size(max=20), @Pattern(regexp="^[A-Z0-9]+$") | 그룹 코드 |
| groupName | String | @NotBlank, @Size(max=100) | 그룹명 |
| description | String | @Size(max=500) | 그룹 설명 |
| color | String | @Pattern(regexp="^#[0-9A-Fa-f]{6}$") | 색상 |
| sortOrder | Integer | @Min(0) | 정렬 순서 |

---

### 5.2 GroupUpdateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| groupName | String | @NotBlank, @Size(max=100) | 그룹명 |
| description | String | @Size(max=500) | 그룹 설명 |
| color | String | @Pattern(regexp="^#[0-9A-Fa-f]{6}$") | 색상 |
| sortOrder | Integer | @Min(0) | 정렬 순서 |
| useYn | String | @Pattern(regexp="^[YN]$") | 사용 여부 |

---

### 5.3 GroupOrderRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| sortOrder | Integer | @NotNull, @Min(0) | 정렬 순서 |

---

### 5.4 GroupMemberRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| userIds | List<Long> | @NotEmpty | 추가할 사용자 ID 목록 |

---

### 5.5 GroupResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| groupId | Long | 그룹 ID |
| groupCode | String | 그룹 코드 |
| groupName | String | 그룹명 |
| description | String | 그룹 설명 |
| color | String | 색상 |
| sortOrder | Integer | 정렬 순서 |
| useYn | String | 사용 여부 |
| memberCount | Integer | 멤버 수 |
| createdAt | LocalDateTime | 생성일시 |
| updatedAt | LocalDateTime | 수정일시 |

---

### 5.6 GroupSimpleResponse
(참조용)

| 필드 | 타입 | 설명 |
|------|------|------|
| groupId | Long | 그룹 ID |
| groupName | String | 그룹명 |
| color | String | 색상 |

---

### 5.7 GroupMemberResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| userId | Long | 사용자 ID |
| username | String | 로그인 ID |
| name | String | 사용자 이름 |
| departmentName | String | 부서명 |

---

## 6. 보드 DTO

### 6.1 BoardCreateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| boardName | String | @NotBlank, @Size(max=200) | 보드명 |
| description | String | @Size(max=500) | 보드 설명 |

---

### 6.2 BoardUpdateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| boardName | String | @NotBlank, @Size(max=200) | 보드명 |
| description | String | @Size(max=500) | 보드 설명 |
| useYn | String | @Pattern(regexp="^[YN]$") | 사용 여부 |

---

### 6.3 BoardShareRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| userId | Long | @NotNull | 공유할 사용자 ID |
| permission | String | @NotBlank, @Pattern(regexp="^(EDIT\|VIEW)$") | 권한 |

---

### 6.4 BoardResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| boardId | Long | 보드 ID |
| boardName | String | 보드명 |
| description | String | 보드 설명 |
| ownerId | Long | 소유자 ID |
| ownerName | String | 소유자명 |
| useYn | String | 사용 여부 |
| itemCount | Integer | 아이템 수 |
| createdAt | LocalDateTime | 생성일시 |
| updatedAt | LocalDateTime | 수정일시 |

---

### 6.5 BoardDetailResponse
(상세 조회용)

| 필드 | 타입 | 설명 |
|------|------|------|
| boardId | Long | 보드 ID |
| boardName | String | 보드명 |
| description | String | 보드 설명 |
| ownerId | Long | 소유자 ID |
| ownerName | String | 소유자명 |
| useYn | String | 사용 여부 |
| itemCount | Integer | 아이템 수 |
| shares | List<BoardShareResponse> | 공유 사용자 목록 |
| properties | List<PropertySimpleResponse> | 속성 정의 목록 |
| createdAt | LocalDateTime | 생성일시 |
| updatedAt | LocalDateTime | 수정일시 |

---

### 6.6 BoardShareResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| boardShareId | Long | 공유 ID |
| userId | Long | 사용자 ID |
| userName | String | 사용자 이름 |
| username | String | 로그인 ID |
| permission | String | 권한 |
| createdAt | LocalDateTime | 생성일시 |

---

## 7. 아이템 DTO

### 7.1 ItemCreateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| content | String | @NotBlank, @Size(max=500) | 업무 내용 |
| status | String | @Pattern(regexp="^(NOT_STARTED\|IN_PROGRESS\|COMPLETED\|DELETED)$") | 상태 |
| priority | String | @Pattern(regexp="^(URGENT\|HIGH\|NORMAL\|LOW)$") | 우선순위 |
| categoryId | Long | - | 카테고리 옵션 ID |
| groupId | Long | - | 그룹 ID |
| assigneeId | Long | - | 담당자 ID |
| startTime | LocalDateTime | - | 시작 시간 |
| endTime | LocalDateTime | - | 완료 시간 |
| propertyValues | Map<Long, Object> | - | 속성값 (propertyId: value) |

---

### 7.2 ItemUpdateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| content | String | @NotBlank, @Size(max=500) | 업무 내용 |
| status | String | @Pattern | 상태 |
| priority | String | @Pattern | 우선순위 |
| categoryId | Long | - | 카테고리 옵션 ID |
| groupId | Long | - | 그룹 ID |
| assigneeId | Long | - | 담당자 ID |
| startTime | LocalDateTime | - | 시작 시간 |
| endTime | LocalDateTime | - | 완료 시간 |
| propertyValues | Map<Long, Object> | - | 속성값 |

---

### 7.3 ItemPropertyRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| propertyValues | Map<Long, Object> | @NotNull | 속성값 (propertyId: value) |

---

### 7.4 ItemResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| itemId | Long | 아이템 ID |
| boardId | Long | 보드 ID |
| content | String | 업무 내용 |
| status | String | 상태 |
| priority | String | 우선순위 |
| categoryId | Long | 카테고리 옵션 ID |
| categoryName | String | 카테고리명 |
| categoryColor | String | 카테고리 색상 |
| groupId | Long | 그룹 ID |
| groupName | String | 그룹명 |
| groupColor | String | 그룹 색상 |
| assigneeId | Long | 담당자 ID |
| assigneeName | String | 담당자명 |
| startTime | LocalDateTime | 시작 시간 |
| endTime | LocalDateTime | 완료 시간 |
| deletedAt | LocalDateTime | 삭제 시간 |
| propertyValues | List<ItemPropertyValueResponse> | 속성값 목록 |
| commentCount | Integer | 댓글 수 |
| createdAt | LocalDateTime | 생성일시 |
| createdBy | Long | 생성자 ID |
| createdByName | String | 생성자명 |
| updatedAt | LocalDateTime | 수정일시 |
| updatedBy | Long | 수정자 ID |
| updatedByName | String | 수정자명 |

---

### 7.5 ItemListResponse
(목록 조회용, 간소화)

| 필드 | 타입 | 설명 |
|------|------|------|
| itemId | Long | 아이템 ID |
| boardId | Long | 보드 ID |
| content | String | 업무 내용 |
| status | String | 상태 |
| priority | String | 우선순위 |
| categoryId | Long | 카테고리 옵션 ID |
| categoryName | String | 카테고리명 |
| categoryColor | String | 카테고리 색상 |
| groupId | Long | 그룹 ID |
| groupName | String | 그룹명 |
| assigneeId | Long | 담당자 ID |
| assigneeName | String | 담당자명 |
| startTime | LocalDateTime | 시작 시간 |
| endTime | LocalDateTime | 완료 시간 |
| commentCount | Integer | 댓글 수 |
| createdAt | LocalDateTime | 생성일시 |
| createdByName | String | 생성자명 |
| updatedAt | LocalDateTime | 수정일시 |
| updatedByName | String | 수정자명 |

---

### 7.6 ItemPropertyValueResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| propertyId | Long | 속성 ID |
| propertyName | String | 속성명 |
| propertyType | String | 속성 타입 |
| value | Object | 값 (타입에 따라 다름) |
| displayValue | String | 표시용 값 |

---

### 7.7 ItemCompleteResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| itemId | Long | 아이템 ID |
| status | String | 상태 |
| endTime | LocalDateTime | 완료 시간 |

---

## 8. 댓글 DTO

### 8.1 CommentCreateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| content | String | @NotBlank, @Size(max=2000) | 댓글 내용 |

---

### 8.2 CommentUpdateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| content | String | @NotBlank, @Size(max=2000) | 댓글 내용 |

---

### 8.3 CommentResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| commentId | Long | 댓글 ID |
| itemId | Long | 아이템 ID |
| content | String | 댓글 내용 |
| createdAt | LocalDateTime | 생성일시 |
| createdBy | Long | 생성자 ID |
| createdByName | String | 생성자명 |
| updatedAt | LocalDateTime | 수정일시 |

---

## 9. 속성 DTO

### 9.1 PropertyCreateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| propertyName | String | @NotBlank, @Size(max=100) | 속성명 |
| propertyType | String | @NotBlank, @Pattern(regexp="^(TEXT\|NUMBER\|DATE\|SELECT\|MULTI_SELECT\|CHECKBOX\|USER)$") | 속성 타입 |
| requiredYn | String | @Pattern(regexp="^[YN]$") | 필수 여부 |
| visibleYn | String | @Pattern(regexp="^[YN]$") | 표시 여부 |
| sortOrder | Integer | @Min(0) | 정렬 순서 |

---

### 9.2 PropertyUpdateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| propertyName | String | @NotBlank, @Size(max=100) | 속성명 |
| requiredYn | String | @Pattern(regexp="^[YN]$") | 필수 여부 |
| visibleYn | String | @Pattern(regexp="^[YN]$") | 표시 여부 |
| sortOrder | Integer | @Min(0) | 정렬 순서 |

---

### 9.3 PropertyResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| propertyId | Long | 속성 ID |
| boardId | Long | 보드 ID |
| propertyName | String | 속성명 |
| propertyType | String | 속성 타입 |
| requiredYn | String | 필수 여부 |
| visibleYn | String | 표시 여부 |
| sortOrder | Integer | 정렬 순서 |
| createdAt | LocalDateTime | 생성일시 |
| updatedAt | LocalDateTime | 수정일시 |

---

### 9.4 PropertyWithOptionsResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| propertyId | Long | 속성 ID |
| boardId | Long | 보드 ID |
| propertyName | String | 속성명 |
| propertyType | String | 속성 타입 |
| requiredYn | String | 필수 여부 |
| visibleYn | String | 표시 여부 |
| sortOrder | Integer | 정렬 순서 |
| options | List<OptionResponse> | 옵션 목록 (SELECT/MULTI_SELECT) |
| createdAt | LocalDateTime | 생성일시 |
| updatedAt | LocalDateTime | 수정일시 |

---

### 9.5 PropertySimpleResponse
(참조용)

| 필드 | 타입 | 설명 |
|------|------|------|
| propertyId | Long | 속성 ID |
| propertyName | String | 속성명 |
| propertyType | String | 속성 타입 |

---

## 10. 옵션 DTO

### 10.1 OptionCreateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| optionLabel | String | @NotBlank, @Size(max=100) | 옵션 라벨 |
| color | String | @Pattern(regexp="^#[0-9A-Fa-f]{6}$") | 색상 |
| sortOrder | Integer | @Min(0) | 정렬 순서 |

---

### 10.2 OptionUpdateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| optionLabel | String | @NotBlank, @Size(max=100) | 옵션 라벨 |
| color | String | @Pattern(regexp="^#[0-9A-Fa-f]{6}$") | 색상 |
| sortOrder | Integer | @Min(0) | 정렬 순서 |
| useYn | String | @Pattern(regexp="^[YN]$") | 사용 여부 |

---

### 10.3 OptionResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| optionId | Long | 옵션 ID |
| propertyId | Long | 속성 ID |
| optionLabel | String | 옵션 라벨 |
| color | String | 색상 |
| sortOrder | Integer | 정렬 순서 |
| useYn | String | 사용 여부 |

---

## 11. 템플릿 DTO

### 11.1 TemplateCreateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| content | String | @NotBlank, @Size(max=500) | 작업 내용 |

---

### 11.2 TemplateUpdateRequest

| 필드 | 타입 | 검증 | 설명 |
|------|------|------|------|
| content | String | @NotBlank, @Size(max=500) | 작업 내용 |
| useYn | String | @Pattern(regexp="^[YN]$") | 사용 여부 |

---

### 11.3 TemplateResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| templateId | Long | 템플릿 ID |
| content | String | 작업 내용 |
| useYn | String | 사용 여부 |
| createdAt | LocalDateTime | 생성일시 |
| createdBy | Long | 생성자 ID |
| createdByName | String | 생성자명 |
| updatedAt | LocalDateTime | 수정일시 |

---

### 11.4 TemplateSearchResponse
(자동완성용)

| 필드 | 타입 | 설명 |
|------|------|------|
| templateId | Long | 템플릿 ID |
| content | String | 작업 내용 |

---

## 12. 이력 DTO

### 12.1 ItemHistoryResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| historyId | Long | 이력 ID |
| itemId | Long | 아이템 ID |
| content | String | 업무 내용 |
| actionType | String | 액션 타입 |
| fieldName | String | 변경 필드명 |
| oldValue | String | 이전 값 |
| newValue | String | 새 값 |
| createdAt | LocalDateTime | 생성일시 |
| createdBy | Long | 생성자 ID |
| createdByName | String | 생성자명 |
| item | ItemSummaryResponse | 아이템 요약 정보 |

---

### 12.2 ItemSummaryResponse
(이력용 아이템 요약)

| 필드 | 타입 | 설명 |
|------|------|------|
| status | String | 상태 |
| startTime | LocalDateTime | 시작 시간 |
| endTime | LocalDateTime | 완료 시간 |

---

### 12.3 TemplateHistoryResponse

| 필드 | 타입 | 설명 |
|------|------|------|
| templateId | Long | 템플릿 ID |
| content | String | 작업 내용 |
| useYn | String | 사용 여부 |
| createdAt | LocalDateTime | 생성일시 |
| createdBy | Long | 생성자 ID |
| createdByName | String | 생성자명 |
| updatedAt | LocalDateTime | 수정일시 |

---

## DTO 요약

| 분류 | Request | Response | 합계 |
|------|---------|----------|------|
| 공통 | - | 2 | 2 |
| 인증 | 1 | 3 | 4 |
| 사용자 | 3 | 3 | 6 |
| 부서 | 3 | 3 | 6 |
| 그룹 | 4 | 4 | 8 |
| 보드 | 3 | 3 | 6 |
| 아이템 | 3 | 5 | 8 |
| 댓글 | 2 | 1 | 3 |
| 속성 | 2 | 3 | 5 |
| 옵션 | 2 | 1 | 3 |
| 템플릿 | 2 | 2 | 4 |
| 이력 | - | 3 | 3 |
| **합계** | **25** | **33** | **58** |
