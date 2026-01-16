# TB_ITEM OWNER_USERNAME 컬럼 추가 설계서

## 1. 개요

### 1.1 배경
현재 TB_ITEM 테이블은 `CREATED_BY` 컬럼이 생성자이자 소유자 역할을 수행하고 있습니다.
업무 이관 시 소유권이 변경되어야 하지만, `CREATED_BY`는 최초 생성자 정보를 보존해야 하므로
별도의 소유자 컬럼이 필요합니다.

### 1.2 목적
- 업무의 "생성자"와 "현재 소유자"를 명확히 분리
- 이관 시 소유권 변경을 정확히 반영
- 다른 테이블(TB_BOARD, TB_CATEGORY 등)과 일관된 소유권 관리 구조 적용

### 1.3 현황 비교

| 테이블 | 소유자 필드 | 생성자 필드 | 상태 |
|--------|-----------|-----------|------|
| TB_BOARD | OWNER_USERNAME | CREATED_BY | 정상 |
| TB_CATEGORY | OWNER_USERNAME | CREATED_BY | 정상 |
| TB_PROPERTY_DEF | OWNER_USERNAME | CREATED_BY | 정상 |
| TB_TASK_TEMPLATE | OWNER_USERNAME | CREATED_BY | 정상 |
| **TB_ITEM** | **없음** | CREATED_BY | **개선 필요** |

---

## 2. ERD 및 테이블 정의서

### 2.1 TB_ITEM 변경사항

#### 추가 컬럼

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| OWNER_USERNAME | VARCHAR(50) | NOT NULL | - | 현재 소유자 USERNAME (이관 시 변경됨) |

#### 컬럼 위치
- BOARD_ID 다음 위치에 추가

#### 외래키
```sql
CONSTRAINT FK_ITEM_OWNER FOREIGN KEY (OWNER_USERNAME)
    REFERENCES TB_USER(USERNAME)
```

### 2.2 변경 전후 비교

| 구분 | 변경 전 | 변경 후 |
|------|--------|--------|
| 소유권 판단 | CREATED_BY | OWNER_USERNAME |
| 생성자 기록 | CREATED_BY | CREATED_BY (유지) |
| 이관 시 처리 | 소유권 불변 | OWNER_USERNAME 변경 |

### 2.3 스키마 변경 SQL

```sql
-- TB_ITEM 테이블 정의 (01_schema.sql)
CREATE TABLE TB_ITEM (
    ITEM_ID BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '업무 ID',
    BOARD_ID BIGINT NOT NULL COMMENT '보드 ID',
    OWNER_USERNAME VARCHAR(50) NOT NULL COMMENT '현재 소유자 USERNAME',  -- 추가
    GROUP_ID BIGINT NULL COMMENT '그룹 ID',
    CATEGORY_ID BIGINT NULL COMMENT '카테고리 ID',
    -- ... 기존 컬럼들 ...

    -- 외래키 추가
    CONSTRAINT FK_ITEM_OWNER FOREIGN KEY (OWNER_USERNAME) REFERENCES TB_USER(USERNAME),
    -- ... 기존 외래키들 ...
);
```

---

## 3. 업무 흐름 변경

### 3.1 업무 생성 시

```
Before:
  CREATED_BY = 생성자

After:
  CREATED_BY = 생성자
  OWNER_USERNAME = 생성자 (동일)
```

### 3.2 업무 이관 시

```
Before:
  BOARD_ID = 새 보드 ID
  TRANSFERRED_FROM = 원래 보드 ID
  TRANSFERRED_AT = 이관 시간
  CREATED_BY = 원래 생성자 (변경 없음)
  → 문제: 소유권 불일치

After:
  BOARD_ID = 새 보드 ID
  OWNER_USERNAME = 이관 대상자 (변경됨)
  TRANSFERRED_FROM = 원래 보드 ID
  TRANSFERRED_AT = 이관 시간
  CREATED_BY = 원래 생성자 (유지)
  → 해결: 소유권 정확히 반영
```

### 3.3 하위 업무 처리

기본 업무(depth=0) 이관 시 하위 업무도 함께 처리:

```sql
UPDATE TB_ITEM
SET
    BOARD_ID = #{newBoardId},
    OWNER_USERNAME = #{newOwnerUsername},
    TRANSFERRED_FROM = #{originalBoardId},
    TRANSFERRED_AT = CURRENT_TIMESTAMP,
    UPDATED_BY = #{updatedBy},
    UPDATED_AT = CURRENT_TIMESTAMP
WHERE ITEM_ID = #{itemId}
   OR PARENT_ITEM_ID = #{itemId}
```

---

## 4. 권한 체크 로직 변경

### 4.1 소유자 확인

```java
// 변경 전
boolean isOwner = username.equals(item.getCreatedBy());

// 변경 후
boolean isOwner = username.equals(item.getOwnerUsername());
```

### 4.2 영향받는 메서드

| 클래스 | 메서드 | 변경 내용 |
|--------|--------|----------|
| ItemShareService | hasItemAccess() | OWNER_USERNAME 기준 확인 |
| ItemShareService | canEdit() | OWNER_USERNAME 기준 확인 |
| ItemShareService | canDelete() | OWNER_USERNAME 기준 확인 |
| ItemShareService | canTransfer() | OWNER_USERNAME 기준 확인 |
| ItemShareService | canShareItem() | OWNER_USERNAME 기준 확인 |
| ItemAssignmentService | assignItem() | OWNER_USERNAME 기준 확인 |

---

## 5. API 영향

### 5.1 응답 필드 추가

모든 Item 관련 API 응답에 추가:

```json
{
  "itemId": 1,
  "boardId": 1,
  "ownerUsername": "userB",    // 추가: 현재 소유자
  "ownerName": "사용자B",       // 추가: 소유자 이름
  "createdBy": "userA",        // 기존: 생성자
  "createdByName": "사용자A",   // 기존: 생성자 이름
  ...
}
```

### 5.2 이관 API 변경

```
PUT /api/boards/{boardId}/items/{itemId}/transfer

요청: 변경 없음
처리: OWNER_USERNAME도 함께 변경
```

---

## 6. 수정 대상 파일

### 6.1 백엔드

| 파일 | 수정 내용 |
|------|----------|
| `docker/mysql/init/01_schema.sql` | OWNER_USERNAME 컬럼 추가 |
| `backend/.../domain/Item.java` | ownerUsername, ownerName 필드 추가 |
| `backend/.../dto/item/ItemResponse.java` | ownerUsername, ownerName 필드 추가 |
| `backend/.../mapper/ItemMapper.xml` | SELECT, INSERT, UPDATE 쿼리 수정 |
| `backend/.../mapper/ItemMapper.java` | transferToBoard 파라미터 수정 |
| `backend/.../service/impl/ItemServiceImpl.java` | 생성 시 OWNER 설정 |
| `backend/.../service/impl/ItemShareServiceImpl.java` | 이관/권한체크 수정 |
| `backend/.../service/impl/ItemAssignmentServiceImpl.java` | 권한 체크 수정 |

### 6.2 프론트엔드

| 파일 | 수정 내용 |
|------|----------|
| `frontend/src/types/item.ts` | ownerUsername, ownerName 타입 추가 |

---

## 7. 마이그레이션 전략

### 7.1 신규 환경 (Docker 초기화)

01_schema.sql에 OWNER_USERNAME 컬럼 포함하여 생성

### 7.2 기존 운영 환경

```sql
-- Step 1: 컬럼 추가 (NULL 허용)
ALTER TABLE TB_ITEM
ADD COLUMN OWNER_USERNAME VARCHAR(50) NULL
COMMENT '현재 소유자 USERNAME' AFTER BOARD_ID;

-- Step 2: 기존 데이터 마이그레이션 (CREATED_BY 값 복사)
UPDATE TB_ITEM SET OWNER_USERNAME = CREATED_BY;

-- Step 3: NOT NULL 제약 추가
ALTER TABLE TB_ITEM
MODIFY COLUMN OWNER_USERNAME VARCHAR(50) NOT NULL;

-- Step 4: 외래키 추가
ALTER TABLE TB_ITEM
ADD CONSTRAINT FK_ITEM_OWNER FOREIGN KEY (OWNER_USERNAME)
REFERENCES TB_USER(USERNAME);
```

---

## 8. 테스트 시나리오

### 8.1 업무 생성 테스트

| 단계 | 동작 | 예상 결과 |
|------|------|----------|
| 1 | 사용자 A가 업무 생성 | CREATED_BY = A, OWNER_USERNAME = A |
| 2 | 업무 조회 | ownerUsername = A 확인 |

### 8.2 업무 이관 테스트

| 단계 | 동작 | 예상 결과 |
|------|------|----------|
| 1 | 사용자 A가 업무 생성 | OWNER_USERNAME = A |
| 2 | A가 B에게 이관 | OWNER_USERNAME = B, CREATED_BY = A |
| 3 | B가 권한 확인 | isOwner = true |
| 4 | A가 권한 확인 | isOwner = false |

### 8.3 하위 업무 이관 테스트

| 단계 | 동작 | 예상 결과 |
|------|------|----------|
| 1 | 기본 업무 + 하위 업무 2개 생성 | 모두 OWNER = A |
| 2 | 기본 업무 B에게 이관 | 기본 + 하위 모두 OWNER = B |

### 8.4 권한 체크 테스트

| 시나리오 | 예상 결과 |
|----------|----------|
| 소유자가 업무 수정 | 허용 |
| 소유자가 업무 삭제 | 허용 |
| 소유자가 업무 공유 | 허용 |
| 소유자가 업무 이관 | 허용 |
| 생성자(비소유자)가 수정 | 거부 (공유 없는 경우) |
| 공유받은 사용자가 수정 | 권한에 따라 |

---

## 9. 구현 체크리스트

| 순서 | 항목 | 상태 |
|------|------|------|
| 1 | 스키마 변경 (01_schema.sql) | □ 대기 |
| 2 | Domain 클래스 수정 (Item.java) | □ 대기 |
| 3 | DTO 수정 (ItemResponse.java) | □ 대기 |
| 4 | Mapper XML 수정 (ItemMapper.xml) | □ 대기 |
| 5 | Mapper Interface 수정 (ItemMapper.java) | □ 대기 |
| 6 | Service - 생성 로직 (ItemServiceImpl) | □ 대기 |
| 7 | Service - 이관 로직 (ItemShareServiceImpl) | □ 대기 |
| 8 | Service - 권한 체크 (ItemShareServiceImpl) | □ 대기 |
| 9 | Service - 배정 권한 (ItemAssignmentServiceImpl) | □ 대기 |
| 10 | 프론트엔드 타입 (item.ts) | □ 대기 |
| 11 | 하위 업무 일괄 이관 처리 | □ 대기 |
| 12 | 테스트 수행 | □ 대기 |

---

## 10. 문서 정보

| 항목 | 내용 |
|------|------|
| 버전 | v2.2.1 |
| 작성일 | 2025-01-16 |
| 상태 | 설계 완료, 승인 대기 |
