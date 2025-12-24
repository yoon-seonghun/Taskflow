# TaskFlow 백엔드 컴포넌트 구조 설계

## 목차
1. [패키지 구조](#1-패키지-구조)
2. [Auth 컴포넌트](#2-auth-컴포넌트)
3. [User 컴포넌트](#3-user-컴포넌트)
4. [Department 컴포넌트](#4-department-컴포넌트)
5. [Group 컴포넌트](#5-group-컴포넌트)
6. [Board 컴포넌트](#6-board-컴포넌트)
7. [Item 컴포넌트](#7-item-컴포넌트)
8. [Property 컴포넌트](#8-property-컴포넌트)
9. [Comment 컴포넌트](#9-comment-컴포넌트)
10. [TaskTemplate 컴포넌트](#10-tasktemplate-컴포넌트)
11. [History 컴포넌트](#11-history-컴포넌트)
12. [SSE 컴포넌트](#12-sse-컴포넌트)

---

## 1. 패키지 구조

```
com.taskflow
├── TaskflowApplication.java
│
├── config/
│   ├── SecurityConfig.java
│   ├── MyBatisConfig.java
│   ├── WebConfig.java
│   └── JwtConfig.java
│
├── common/
│   ├── ApiResponse.java
│   ├── PageResponse.java
│   ├── BaseEntity.java
│   └── util/
│       ├── SecurityUtil.java
│       └── DateUtil.java
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── BusinessException.java
│   └── ErrorCode.java
│
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── CustomUserDetails.java
│   └── CustomUserDetailsService.java
│
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── DepartmentController.java
│   ├── GroupController.java
│   ├── BoardController.java
│   ├── ItemController.java
│   ├── PropertyController.java
│   ├── OptionController.java
│   ├── CommentController.java
│   ├── TaskTemplateController.java
│   ├── HistoryController.java
│   └── SseController.java
│
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── DepartmentService.java
│   ├── GroupService.java
│   ├── BoardService.java
│   ├── ItemService.java
│   ├── PropertyService.java
│   ├── OptionService.java
│   ├── CommentService.java
│   ├── TaskTemplateService.java
│   ├── HistoryService.java
│   └── SseService.java
│
├── mapper/
│   ├── UserMapper.java
│   ├── DepartmentMapper.java
│   ├── GroupMapper.java
│   ├── UserGroupMapper.java
│   ├── BoardMapper.java
│   ├── BoardShareMapper.java
│   ├── ItemMapper.java
│   ├── ItemPropertyMapper.java
│   ├── PropertyDefMapper.java
│   ├── PropertyOptionMapper.java
│   ├── CommentMapper.java
│   ├── TaskTemplateMapper.java
│   └── ItemHistoryMapper.java
│
├── domain/
│   ├── User.java
│   ├── Department.java
│   ├── Group.java
│   ├── UserGroup.java
│   ├── Board.java
│   ├── BoardShare.java
│   ├── Item.java
│   ├── ItemProperty.java
│   ├── ItemPropertyMulti.java
│   ├── PropertyDef.java
│   ├── PropertyOption.java
│   ├── Comment.java
│   ├── TaskTemplate.java
│   └── ItemHistory.java
│
└── dto/
    ├── common/
    ├── auth/
    ├── user/
    ├── department/
    ├── group/
    ├── board/
    ├── item/
    ├── property/
    ├── option/
    ├── comment/
    ├── template/
    └── history/
```

### resources 구조

```
resources/
├── application.yml
├── mapper/
│   ├── UserMapper.xml
│   ├── DepartmentMapper.xml
│   ├── GroupMapper.xml
│   ├── UserGroupMapper.xml
│   ├── BoardMapper.xml
│   ├── BoardShareMapper.xml
│   ├── ItemMapper.xml
│   ├── ItemPropertyMapper.xml
│   ├── PropertyDefMapper.xml
│   ├── PropertyOptionMapper.xml
│   ├── CommentMapper.xml
│   ├── TaskTemplateMapper.xml
│   └── ItemHistoryMapper.xml
└── messages/
    └── messages.properties
```

---

## 2. Auth 컴포넌트

### 2.1 AuthController

| 메서드 | HTTP | URL | 설명 |
|--------|------|-----|------|
| login() | POST | /api/auth/login | 로그인 |
| logout() | POST | /api/auth/logout | 로그아웃 |
| refresh() | POST | /api/auth/refresh | 토큰 갱신 |
| me() | GET | /api/auth/me | 내 정보 조회 |

### 2.2 AuthService

| 메서드 | 파라미터 | 반환 | 설명 |
|--------|---------|------|------|
| login() | LoginRequest | LoginResponse | 인증 및 토큰 발급 |
| logout() | String token | void | 토큰 무효화 |
| refresh() | String refreshToken | TokenResponse | Access Token 갱신 |
| getCurrentUser() | - | UserInfoResponse | 현재 사용자 정보 |

### 2.3 Security 클래스

**JwtTokenProvider**
| 메서드 | 설명 |
|--------|------|
| createAccessToken() | Access Token 생성 |
| createRefreshToken() | Refresh Token 생성 |
| validateToken() | 토큰 유효성 검증 |
| getUserIdFromToken() | 토큰에서 사용자 ID 추출 |
| getAuthentication() | 인증 객체 생성 |

**JwtAuthenticationFilter**
| 메서드 | 설명 |
|--------|------|
| doFilterInternal() | 요청 헤더에서 토큰 추출 및 인증 처리 |
| resolveToken() | Authorization 헤더에서 토큰 추출 |

---

## 3. User 컴포넌트

### 3.1 UserController

| 메서드 | HTTP | URL | 설명 |
|--------|------|-----|------|
| getUsers() | GET | /api/users | 사용자 목록 조회 |
| createUser() | POST | /api/users | 사용자 등록 |
| getUser() | GET | /api/users/{id} | 사용자 조회 |
| updateUser() | PUT | /api/users/{id} | 사용자 수정 |
| deleteUser() | DELETE | /api/users/{id} | 사용자 삭제 |
| changePassword() | PUT | /api/users/{id}/password | 비밀번호 변경 |

### 3.2 UserService

| 메서드 | 파라미터 | 반환 | 설명 |
|--------|---------|------|------|
| getUsers() | Pageable, 필터 조건 | PageResponse<UserListResponse> | 목록 조회 |
| createUser() | UserCreateRequest | UserResponse | 사용자 생성 |
| getUser() | Long userId | UserResponse | 단건 조회 |
| updateUser() | Long userId, UserUpdateRequest | UserResponse | 사용자 수정 |
| deleteUser() | Long userId | void | 사용자 삭제 |
| changePassword() | Long userId, PasswordChangeRequest | void | 비밀번호 변경 |
| findByUsername() | String username | User | username으로 조회 |

### 3.3 UserMapper

| 메서드 | 설명 |
|--------|------|
| selectUsers() | 사용자 목록 조회 (필터, 페이징) |
| selectUserCount() | 사용자 수 조회 |
| selectUserById() | ID로 조회 |
| selectUserByUsername() | username으로 조회 |
| insertUser() | 사용자 등록 |
| updateUser() | 사용자 수정 |
| updatePassword() | 비밀번호 수정 |
| updateLastLoginAt() | 마지막 로그인 시간 수정 |
| deleteUser() | 사용자 삭제 |

### 3.4 UserMapper.xml

```
<mapper namespace="com.taskflow.mapper.UserMapper">
    <!-- ResultMap -->
    <resultMap id="UserResultMap" type="User">
    <resultMap id="UserWithDepartmentResultMap" type="User">
    <resultMap id="UserWithGroupsResultMap" type="User">

    <!-- SQL Fragments -->
    <sql id="baseColumns">
    <sql id="searchCondition">

    <!-- Select -->
    <select id="selectUsers">
    <select id="selectUserCount">
    <select id="selectUserById">
    <select id="selectUserByUsername">
    <select id="selectUsersByDepartmentId">
    <select id="selectUsersByGroupId">

    <!-- Insert -->
    <insert id="insertUser" useGeneratedKeys="true" keyProperty="userId">

    <!-- Update -->
    <update id="updateUser">
    <update id="updatePassword">
    <update id="updateLastLoginAt">

    <!-- Delete -->
    <delete id="deleteUser">
</mapper>
```

---

## 4. Department 컴포넌트

### 4.1 DepartmentController

| 메서드 | HTTP | URL | 설명 |
|--------|------|-----|------|
| getDepartments() | GET | /api/departments | 부서 목록 (트리) |
| getDepartmentsFlat() | GET | /api/departments/flat | 부서 목록 (평면) |
| createDepartment() | POST | /api/departments | 부서 생성 |
| getDepartment() | GET | /api/departments/{id} | 부서 조회 |
| updateDepartment() | PUT | /api/departments/{id} | 부서 수정 |
| deleteDepartment() | DELETE | /api/departments/{id} | 부서 삭제 |
| updateOrder() | PUT | /api/departments/{id}/order | 순서 변경 |
| getUsers() | GET | /api/departments/{id}/users | 소속 사용자 목록 |

### 4.2 DepartmentService

| 메서드 | 파라미터 | 반환 | 설명 |
|--------|---------|------|------|
| getDepartmentTree() | String useYn | List<DepartmentTreeResponse> | 트리 구조 조회 |
| getDepartmentsFlat() | String useYn | List<DepartmentFlatResponse> | 평면 구조 조회 |
| createDepartment() | DepartmentCreateRequest | DepartmentResponse | 부서 생성 |
| getDepartment() | Long departmentId | DepartmentResponse | 단건 조회 |
| updateDepartment() | Long id, DepartmentUpdateRequest | DepartmentResponse | 부서 수정 |
| deleteDepartment() | Long departmentId | void | 부서 삭제 |
| updateOrder() | Long id, DepartmentOrderRequest | void | 순서 변경 |
| getUsersByDepartment() | Long departmentId | List<UserSimpleResponse> | 소속 사용자 |
| buildTree() | List<Department> | List<DepartmentTreeResponse> | 트리 구조 생성 (private) |

### 4.3 DepartmentMapper

| 메서드 | 설명 |
|--------|------|
| selectAllDepartments() | 전체 부서 조회 |
| selectDepartmentById() | ID로 조회 |
| selectDepartmentByCode() | 코드로 조회 |
| selectChildDepartments() | 하위 부서 조회 |
| selectUserCountByDepartmentId() | 소속 사용자 수 |
| insertDepartment() | 부서 등록 |
| updateDepartment() | 부서 수정 |
| updateSortOrder() | 순서 변경 |
| deleteDepartment() | 부서 삭제 |

### 4.4 DepartmentMapper.xml

```
<mapper namespace="com.taskflow.mapper.DepartmentMapper">
    <!-- ResultMap -->
    <resultMap id="DepartmentResultMap" type="Department">

    <!-- SQL Fragments -->
    <sql id="baseColumns">

    <!-- Select -->
    <select id="selectAllDepartments">
    <select id="selectDepartmentById">
    <select id="selectDepartmentByCode">
    <select id="selectChildDepartments">
    <select id="selectUserCountByDepartmentId">
    <select id="existsByCode">

    <!-- Insert -->
    <insert id="insertDepartment" useGeneratedKeys="true" keyProperty="departmentId">

    <!-- Update -->
    <update id="updateDepartment">
    <update id="updateSortOrder">

    <!-- Delete -->
    <delete id="deleteDepartment">
</mapper>
```

---

## 5. Group 컴포넌트

### 5.1 GroupController

| 메서드 | HTTP | URL | 설명 |
|--------|------|-----|------|
| getGroups() | GET | /api/groups | 그룹 목록 |
| createGroup() | POST | /api/groups | 그룹 생성 |
| getGroup() | GET | /api/groups/{id} | 그룹 조회 |
| updateGroup() | PUT | /api/groups/{id} | 그룹 수정 |
| deleteGroup() | DELETE | /api/groups/{id} | 그룹 삭제 |
| updateOrder() | PUT | /api/groups/{id}/order | 순서 변경 |
| getMembers() | GET | /api/groups/{id}/members | 멤버 목록 |
| addMembers() | POST | /api/groups/{id}/members | 멤버 추가 |
| removeMember() | DELETE | /api/groups/{id}/members/{userId} | 멤버 제거 |

### 5.2 GroupService

| 메서드 | 파라미터 | 반환 | 설명 |
|--------|---------|------|------|
| getGroups() | String useYn | List<GroupResponse> | 그룹 목록 |
| createGroup() | GroupCreateRequest | GroupResponse | 그룹 생성 |
| getGroup() | Long groupId | GroupResponse | 단건 조회 |
| updateGroup() | Long id, GroupUpdateRequest | GroupResponse | 그룹 수정 |
| deleteGroup() | Long groupId | void | 그룹 삭제 |
| updateOrder() | Long id, GroupOrderRequest | void | 순서 변경 |
| getMembers() | Long groupId | List<GroupMemberResponse> | 멤버 목록 |
| addMembers() | Long groupId, GroupMemberRequest | void | 멤버 추가 |
| removeMember() | Long groupId, Long userId | void | 멤버 제거 |

### 5.3 GroupMapper

| 메서드 | 설명 |
|--------|------|
| selectGroups() | 그룹 목록 조회 |
| selectGroupById() | ID로 조회 |
| selectGroupByCode() | 코드로 조회 |
| selectMemberCount() | 멤버 수 조회 |
| insertGroup() | 그룹 등록 |
| updateGroup() | 그룹 수정 |
| updateSortOrder() | 순서 변경 |
| deleteGroup() | 그룹 삭제 |

### 5.4 UserGroupMapper

| 메서드 | 설명 |
|--------|------|
| selectUsersByGroupId() | 그룹별 사용자 목록 |
| selectGroupsByUserId() | 사용자별 그룹 목록 |
| insertUserGroup() | 매핑 추가 |
| insertUserGroups() | 매핑 일괄 추가 |
| deleteUserGroup() | 매핑 삭제 |
| deleteByGroupId() | 그룹의 모든 매핑 삭제 |
| deleteByUserId() | 사용자의 모든 매핑 삭제 |
| exists() | 매핑 존재 여부 |

### 5.5 GroupMapper.xml / UserGroupMapper.xml

```
<mapper namespace="com.taskflow.mapper.GroupMapper">
    <resultMap id="GroupResultMap" type="Group">
    <resultMap id="GroupWithCountResultMap" type="Group">

    <select id="selectGroups">
    <select id="selectGroupById">
    <select id="selectGroupByCode">
    <select id="selectMemberCount">

    <insert id="insertGroup" useGeneratedKeys="true" keyProperty="groupId">
    <update id="updateGroup">
    <update id="updateSortOrder">
    <delete id="deleteGroup">
</mapper>

<mapper namespace="com.taskflow.mapper.UserGroupMapper">
    <resultMap id="UserGroupResultMap" type="UserGroup">

    <select id="selectUsersByGroupId">
    <select id="selectGroupsByUserId">
    <select id="exists">

    <insert id="insertUserGroup">
    <insert id="insertUserGroups">  <!-- foreach -->
    <delete id="deleteUserGroup">
    <delete id="deleteByGroupId">
    <delete id="deleteByUserId">
</mapper>
```

---

## 6. Board 컴포넌트

### 6.1 BoardController

| 메서드 | HTTP | URL | 설명 |
|--------|------|-----|------|
| getBoards() | GET | /api/boards | 보드 목록 |
| createBoard() | POST | /api/boards | 보드 생성 |
| getBoard() | GET | /api/boards/{id} | 보드 조회 |
| updateBoard() | PUT | /api/boards/{id} | 보드 수정 |
| deleteBoard() | DELETE | /api/boards/{id} | 보드 삭제 |
| getShares() | GET | /api/boards/{id}/shares | 공유 사용자 목록 |
| addShare() | POST | /api/boards/{id}/shares | 공유 추가 |
| removeShare() | DELETE | /api/boards/{id}/shares/{userId} | 공유 제거 |

### 6.2 BoardService

| 메서드 | 파라미터 | 반환 | 설명 |
|--------|---------|------|------|
| getBoards() | Long userId | List<BoardResponse> | 접근 가능한 보드 목록 |
| createBoard() | BoardCreateRequest | BoardResponse | 보드 생성 |
| getBoard() | Long boardId | BoardDetailResponse | 단건 조회 (상세) |
| updateBoard() | Long id, BoardUpdateRequest | BoardResponse | 보드 수정 |
| deleteBoard() | Long boardId | void | 보드 삭제 |
| getShares() | Long boardId | List<BoardShareResponse> | 공유 사용자 목록 |
| addShare() | Long boardId, BoardShareRequest | BoardShareResponse | 공유 추가 |
| removeShare() | Long boardId, Long userId | void | 공유 제거 |
| hasAccess() | Long boardId, Long userId | boolean | 접근 권한 확인 |
| hasEditPermission() | Long boardId, Long userId | boolean | 편집 권한 확인 |

### 6.3 BoardMapper / BoardShareMapper

**BoardMapper**
| 메서드 | 설명 |
|--------|------|
| selectBoardsByUserId() | 사용자 접근 가능 보드 |
| selectBoardById() | ID로 조회 |
| selectItemCount() | 아이템 수 조회 |
| insertBoard() | 보드 등록 |
| updateBoard() | 보드 수정 |
| deleteBoard() | 보드 삭제 |

**BoardShareMapper**
| 메서드 | 설명 |
|--------|------|
| selectSharesByBoardId() | 보드별 공유 목록 |
| selectShareByBoardIdAndUserId() | 특정 공유 조회 |
| insertShare() | 공유 추가 |
| deleteShare() | 공유 삭제 |
| deleteByBoardId() | 보드의 모든 공유 삭제 |
| hasAccess() | 접근 권한 확인 |
| getPermission() | 권한 조회 |

### 6.4 BoardMapper.xml / BoardShareMapper.xml

```
<mapper namespace="com.taskflow.mapper.BoardMapper">
    <resultMap id="BoardResultMap" type="Board">
    <resultMap id="BoardWithOwnerResultMap" type="Board">

    <select id="selectBoardsByUserId">
        <!-- 소유 보드 UNION 공유 보드 -->
    <select id="selectBoardById">
    <select id="selectItemCount">

    <insert id="insertBoard" useGeneratedKeys="true" keyProperty="boardId">
    <update id="updateBoard">
    <delete id="deleteBoard">
</mapper>

<mapper namespace="com.taskflow.mapper.BoardShareMapper">
    <resultMap id="BoardShareResultMap" type="BoardShare">

    <select id="selectSharesByBoardId">
    <select id="selectShareByBoardIdAndUserId">
    <select id="hasAccess">
    <select id="getPermission">

    <insert id="insertShare">
    <delete id="deleteShare">
    <delete id="deleteByBoardId">
</mapper>
```

---

## 7. Item 컴포넌트

### 7.1 ItemController

| 메서드 | HTTP | URL | 설명 |
|--------|------|-----|------|
| getItems() | GET | /api/boards/{boardId}/items | 아이템 목록 |
| createItem() | POST | /api/boards/{boardId}/items | 아이템 생성 |
| getItem() | GET | /api/boards/{boardId}/items/{id} | 아이템 조회 |
| updateItem() | PUT | /api/boards/{boardId}/items/{id} | 아이템 수정 |
| deleteItem() | DELETE | /api/boards/{boardId}/items/{id} | 아이템 삭제 |
| completeItem() | PUT | /api/boards/{boardId}/items/{id}/complete | 완료 처리 |
| restoreItem() | PUT | /api/boards/{boardId}/items/{id}/restore | 복원 처리 |
| updateProperties() | PUT | /api/boards/{boardId}/items/{id}/properties | 속성값 일괄 수정 |

### 7.2 ItemService

| 메서드 | 파라미터 | 반환 | 설명 |
|--------|---------|------|------|
| getItems() | Long boardId, ItemSearchCondition, Pageable | PageResponse<ItemListResponse> | 목록 조회 |
| createItem() | Long boardId, ItemCreateRequest | ItemResponse | 아이템 생성 |
| getItem() | Long boardId, Long itemId | ItemResponse | 단건 조회 |
| updateItem() | Long boardId, Long itemId, ItemUpdateRequest | ItemResponse | 아이템 수정 |
| deleteItem() | Long boardId, Long itemId | void | 아이템 삭제 (상태 변경) |
| completeItem() | Long boardId, Long itemId | ItemCompleteResponse | 완료 처리 |
| restoreItem() | Long boardId, Long itemId | ItemResponse | 복원 처리 |
| updateProperties() | Long boardId, Long itemId, ItemPropertyRequest | void | 속성값 일괄 수정 |
| savePropertyValues() | Long itemId, Map<Long, Object> | void | 속성값 저장 (private) |

### 7.3 ItemMapper / ItemPropertyMapper

**ItemMapper**
| 메서드 | 설명 |
|--------|------|
| selectItems() | 아이템 목록 (필터, 페이징) |
| selectItemCount() | 아이템 수 |
| selectItemById() | ID로 조회 |
| selectItemWithProperties() | 속성값 포함 조회 |
| insertItem() | 아이템 등록 |
| updateItem() | 아이템 수정 |
| updateStatus() | 상태 변경 |
| updateEndTime() | 완료 시간 수정 |
| updateDeletedAt() | 삭제 시간 수정 |

**ItemPropertyMapper**
| 메서드 | 설명 |
|--------|------|
| selectPropertiesByItemId() | 아이템별 속성값 목록 |
| selectPropertyValue() | 특정 속성값 조회 |
| insertProperty() | 속성값 등록 |
| updateProperty() | 속성값 수정 |
| deleteProperty() | 속성값 삭제 |
| deleteByItemId() | 아이템의 모든 속성값 삭제 |
| upsertProperty() | 속성값 등록/수정 (ON DUPLICATE KEY) |

**ItemPropertyMultiMapper** (다중선택용)
| 메서드 | 설명 |
|--------|------|
| selectByItemAndProperty() | 다중선택 값 조회 |
| insertMulti() | 다중선택 값 추가 |
| deleteByItemAndProperty() | 다중선택 값 삭제 |

### 7.4 ItemMapper.xml

```
<mapper namespace="com.taskflow.mapper.ItemMapper">
    <resultMap id="ItemResultMap" type="Item">
    <resultMap id="ItemWithDetailsResultMap" type="Item">
        <!-- association: category, group, assignee -->
        <!-- collection: propertyValues -->

    <sql id="baseColumns">
    <sql id="searchCondition">
        <!-- status, priority, assigneeId, groupId, categoryId, keyword, dateRange -->

    <select id="selectItems">
    <select id="selectItemCount">
    <select id="selectItemById">
    <select id="selectItemWithProperties">
    <select id="selectCommentCount">

    <insert id="insertItem" useGeneratedKeys="true" keyProperty="itemId">
    <update id="updateItem">
    <update id="updateStatus">
    <update id="updateEndTime">
    <update id="updateDeletedAt">
</mapper>

<mapper namespace="com.taskflow.mapper.ItemPropertyMapper">
    <resultMap id="ItemPropertyResultMap" type="ItemProperty">
    <resultMap id="ItemPropertyValueResultMap">
        <!-- 속성 타입별 값 매핑 -->

    <select id="selectPropertiesByItemId">
    <select id="selectPropertyValue">

    <insert id="insertProperty">
    <update id="updateProperty">
    <delete id="deleteProperty">
    <delete id="deleteByItemId">

    <!-- MySQL ON DUPLICATE KEY UPDATE -->
    <insert id="upsertProperty">
</mapper>
```

---

## 8. Property 컴포넌트

### 8.1 PropertyController / OptionController

**PropertyController**
| 메서드 | HTTP | URL | 설명 |
|--------|------|-----|------|
| getProperties() | GET | /api/boards/{boardId}/properties | 속성 목록 |
| createProperty() | POST | /api/boards/{boardId}/properties | 속성 생성 |
| updateProperty() | PUT | /api/properties/{id} | 속성 수정 |
| deleteProperty() | DELETE | /api/properties/{id} | 속성 삭제 |

**OptionController**
| 메서드 | HTTP | URL | 설명 |
|--------|------|-----|------|
| getOptions() | GET | /api/properties/{propId}/options | 옵션 목록 |
| createOption() | POST | /api/properties/{propId}/options | 옵션 추가 |
| updateOption() | PUT | /api/options/{id} | 옵션 수정 |
| deleteOption() | DELETE | /api/options/{id} | 옵션 삭제 |

### 8.2 PropertyService / OptionService

**PropertyService**
| 메서드 | 파라미터 | 반환 | 설명 |
|--------|---------|------|------|
| getProperties() | Long boardId | List<PropertyWithOptionsResponse> | 속성 목록 (옵션 포함) |
| createProperty() | Long boardId, PropertyCreateRequest | PropertyResponse | 속성 생성 |
| updateProperty() | Long propertyId, PropertyUpdateRequest | PropertyResponse | 속성 수정 |
| deleteProperty() | Long propertyId | void | 속성 삭제 |

**OptionService**
| 메서드 | 파라미터 | 반환 | 설명 |
|--------|---------|------|------|
| getOptions() | Long propertyId | List<OptionResponse> | 옵션 목록 |
| createOption() | Long propertyId, OptionCreateRequest | OptionResponse | 옵션 추가 |
| updateOption() | Long optionId, OptionUpdateRequest | OptionResponse | 옵션 수정 |
| deleteOption() | Long optionId | void | 옵션 삭제 |
| isOptionInUse() | Long optionId | boolean | 사용 여부 확인 |

### 8.3 PropertyDefMapper / PropertyOptionMapper

**PropertyDefMapper**
| 메서드 | 설명 |
|--------|------|
| selectPropertiesByBoardId() | 보드별 속성 목록 |
| selectPropertyById() | ID로 조회 |
| insertProperty() | 속성 등록 |
| updateProperty() | 속성 수정 |
| deleteProperty() | 속성 삭제 |

**PropertyOptionMapper**
| 메서드 | 설명 |
|--------|------|
| selectOptionsByPropertyId() | 속성별 옵션 목록 |
| selectOptionById() | ID로 조회 |
| selectUsageCount() | 사용 수 조회 |
| insertOption() | 옵션 등록 |
| updateOption() | 옵션 수정 |
| deleteOption() | 옵션 삭제 |
| deleteByPropertyId() | 속성의 모든 옵션 삭제 |

### 8.4 PropertyDefMapper.xml / PropertyOptionMapper.xml

```
<mapper namespace="com.taskflow.mapper.PropertyDefMapper">
    <resultMap id="PropertyDefResultMap" type="PropertyDef">
    <resultMap id="PropertyWithOptionsResultMap" type="PropertyDef">
        <collection property="options" ofType="PropertyOption">

    <select id="selectPropertiesByBoardId">
    <select id="selectPropertyById">

    <insert id="insertProperty" useGeneratedKeys="true" keyProperty="propertyId">
    <update id="updateProperty">
    <delete id="deleteProperty">
</mapper>

<mapper namespace="com.taskflow.mapper.PropertyOptionMapper">
    <resultMap id="PropertyOptionResultMap" type="PropertyOption">

    <select id="selectOptionsByPropertyId">
    <select id="selectOptionById">
    <select id="selectUsageCount">
        <!-- TB_ITEM_PROPERTY, TB_ITEM_PROPERTY_MULTI, TB_ITEM.CATEGORY_ID 조회 -->

    <insert id="insertOption" useGeneratedKeys="true" keyProperty="optionId">
    <update id="updateOption">
    <delete id="deleteOption">
    <delete id="deleteByPropertyId">
</mapper>
```

---

## 9. Comment 컴포넌트

### 9.1 CommentController

| 메서드 | HTTP | URL | 설명 |
|--------|------|-----|------|
| getComments() | GET | /api/items/{itemId}/comments | 댓글 목록 |
| createComment() | POST | /api/items/{itemId}/comments | 댓글 등록 |
| updateComment() | PUT | /api/comments/{id} | 댓글 수정 |
| deleteComment() | DELETE | /api/comments/{id} | 댓글 삭제 |

### 9.2 CommentService

| 메서드 | 파라미터 | 반환 | 설명 |
|--------|---------|------|------|
| getComments() | Long itemId | List<CommentResponse> | 댓글 목록 |
| createComment() | Long itemId, CommentCreateRequest | CommentResponse | 댓글 등록 |
| updateComment() | Long commentId, CommentUpdateRequest | CommentResponse | 댓글 수정 |
| deleteComment() | Long commentId | void | 댓글 삭제 |
| getCommentCount() | Long itemId | int | 댓글 수 |

### 9.3 CommentMapper

| 메서드 | 설명 |
|--------|------|
| selectCommentsByItemId() | 아이템별 댓글 목록 |
| selectCommentById() | ID로 조회 |
| selectCommentCount() | 댓글 수 조회 |
| insertComment() | 댓글 등록 |
| updateComment() | 댓글 수정 |
| deleteComment() | 댓글 삭제 |
| deleteByItemId() | 아이템의 모든 댓글 삭제 |

### 9.4 CommentMapper.xml

```
<mapper namespace="com.taskflow.mapper.CommentMapper">
    <resultMap id="CommentResultMap" type="Comment">
    <resultMap id="CommentWithUserResultMap" type="Comment">
        <association property="createdByUser" javaType="User">

    <select id="selectCommentsByItemId">
    <select id="selectCommentById">
    <select id="selectCommentCount">

    <insert id="insertComment" useGeneratedKeys="true" keyProperty="commentId">
    <update id="updateComment">
    <delete id="deleteComment">
    <delete id="deleteByItemId">
</mapper>
```

---

## 10. TaskTemplate 컴포넌트

### 10.1 TaskTemplateController

| 메서드 | HTTP | URL | 설명 |
|--------|------|-----|------|
| getTemplates() | GET | /api/task-templates | 템플릿 목록 |
| searchTemplates() | GET | /api/task-templates/search | 템플릿 검색 (자동완성) |
| createTemplate() | POST | /api/task-templates | 템플릿 등록 |
| updateTemplate() | PUT | /api/task-templates/{id} | 템플릿 수정 |
| deleteTemplate() | DELETE | /api/task-templates/{id} | 템플릿 삭제 |

### 10.2 TaskTemplateService

| 메서드 | 파라미터 | 반환 | 설명 |
|--------|---------|------|------|
| getTemplates() | String useYn | List<TemplateResponse> | 템플릿 목록 |
| searchTemplates() | String keyword, int limit | List<TemplateSearchResponse> | 검색 (자동완성) |
| createTemplate() | TemplateCreateRequest | TemplateResponse | 템플릿 등록 |
| updateTemplate() | Long templateId, TemplateUpdateRequest | TemplateResponse | 템플릿 수정 |
| deleteTemplate() | Long templateId | void | 템플릿 삭제 |

### 10.3 TaskTemplateMapper

| 메서드 | 설명 |
|--------|------|
| selectTemplates() | 템플릿 목록 |
| selectTemplateById() | ID로 조회 |
| searchTemplates() | 검색 (LIKE) |
| insertTemplate() | 템플릿 등록 |
| updateTemplate() | 템플릿 수정 |
| deleteTemplate() | 템플릿 삭제 |

### 10.4 TaskTemplateMapper.xml

```
<mapper namespace="com.taskflow.mapper.TaskTemplateMapper">
    <resultMap id="TaskTemplateResultMap" type="TaskTemplate">
    <resultMap id="TaskTemplateWithUserResultMap" type="TaskTemplate">

    <select id="selectTemplates">
    <select id="selectTemplateById">
    <select id="searchTemplates">
        WHERE USE_YN = 'Y'
        AND CONTENT LIKE CONCAT('%', #{keyword}, '%')
        LIMIT #{limit}

    <insert id="insertTemplate" useGeneratedKeys="true" keyProperty="templateId">
    <update id="updateTemplate">
    <delete id="deleteTemplate">
</mapper>
```

---

## 11. History 컴포넌트

### 11.1 HistoryController

| 메서드 | HTTP | URL | 설명 |
|--------|------|-----|------|
| getItemHistory() | GET | /api/history/items | 작업 처리 이력 |
| getTemplateHistory() | GET | /api/history/templates | 작업 등록 이력 |

### 11.2 HistoryService

| 메서드 | 파라미터 | 반환 | 설명 |
|--------|---------|------|------|
| getItemHistory() | HistorySearchCondition, Pageable | PageResponse<ItemHistoryResponse> | 작업 처리 이력 |
| getTemplateHistory() | HistorySearchCondition, Pageable | PageResponse<TemplateHistoryResponse> | 작업 등록 이력 |
| recordHistory() | Long itemId, String actionType, String fieldName, String oldValue, String newValue | void | 이력 기록 |
| recordCreate() | Long itemId | void | 생성 이력 |
| recordUpdate() | Long itemId, String fieldName, Object oldValue, Object newValue | void | 수정 이력 |
| recordComplete() | Long itemId | void | 완료 이력 |
| recordDelete() | Long itemId | void | 삭제 이력 |
| recordRestore() | Long itemId | void | 복원 이력 |

### 11.3 ItemHistoryMapper

| 메서드 | 설명 |
|--------|------|
| selectItemHistory() | 작업 처리 이력 (필터, 페이징) |
| selectItemHistoryCount() | 이력 수 |
| selectHistoryByItemId() | 아이템별 이력 |
| insertHistory() | 이력 등록 |

### 11.4 ItemHistoryMapper.xml

```
<mapper namespace="com.taskflow.mapper.ItemHistoryMapper">
    <resultMap id="ItemHistoryResultMap" type="ItemHistory">
    <resultMap id="ItemHistoryWithItemResultMap" type="ItemHistory">
        <association property="item" javaType="Item">

    <sql id="searchCondition">
        <!-- actionType, dateRange, userId -->

    <select id="selectItemHistory">
    <select id="selectItemHistoryCount">
    <select id="selectHistoryByItemId">

    <insert id="insertHistory" useGeneratedKeys="true" keyProperty="historyId">
</mapper>
```

---

## 12. SSE 컴포넌트

### 12.1 SseController

| 메서드 | HTTP | URL | 설명 |
|--------|------|-----|------|
| subscribe() | GET | /api/sse/subscribe | SSE 연결 |

### 12.2 SseService

| 메서드 | 파라미터 | 반환 | 설명 |
|--------|---------|------|------|
| subscribe() | Long userId | SseEmitter | SSE 연결 생성 |
| sendToUser() | Long userId, String eventType, Object data | void | 특정 사용자에게 전송 |
| sendToBoard() | Long boardId, String eventType, Object data | void | 보드 사용자들에게 전송 |
| broadcast() | String eventType, Object data | void | 전체 브로드캐스트 |
| removeEmitter() | Long userId | void | Emitter 제거 |
| sendHeartbeat() | - | void | Heartbeat 전송 (스케줄러) |

### 12.3 SSE 이벤트 발행 위치

| 이벤트 | 발행 위치 | 설명 |
|--------|----------|------|
| item:created | ItemService.createItem() | 아이템 생성 시 |
| item:updated | ItemService.updateItem() | 아이템 수정 시 |
| item:deleted | ItemService.deleteItem() | 아이템 삭제 시 |
| item:completed | ItemService.completeItem() | 아이템 완료 시 |
| property:updated | PropertyService.updateProperty() | 속성 수정 시 |
| option:created | OptionService.createOption() | 옵션 생성 시 |
| option:updated | OptionService.updateOption() | 옵션 수정 시 |
| option:deleted | OptionService.deleteOption() | 옵션 삭제 시 |
| comment:created | CommentService.createComment() | 댓글 등록 시 |

---

## 컴포넌트 요약

| 도메인 | Controller | Service | Mapper | XML |
|--------|------------|---------|--------|-----|
| Auth | 1 | 1 | - | - |
| User | 1 | 1 | 1 | 1 |
| Department | 1 | 1 | 1 | 1 |
| Group | 1 | 1 | 2 | 2 |
| Board | 1 | 1 | 2 | 2 |
| Item | 1 | 1 | 3 | 3 |
| Property | 2 | 2 | 2 | 2 |
| Comment | 1 | 1 | 1 | 1 |
| TaskTemplate | 1 | 1 | 1 | 1 |
| History | 1 | 1 | 1 | 1 |
| SSE | 1 | 1 | - | - |
| **합계** | **12** | **12** | **14** | **14** |

### 파일 수 합계

| 분류 | 파일 수 |
|------|--------|
| Controller | 12 |
| Service | 12 |
| Mapper Interface | 14 |
| Mapper XML | 14 |
| Domain | 14 |
| DTO | 58 |
| Config | 4 |
| Security | 4 |
| Common | 6 |
| Exception | 3 |
| **합계** | **141** |
