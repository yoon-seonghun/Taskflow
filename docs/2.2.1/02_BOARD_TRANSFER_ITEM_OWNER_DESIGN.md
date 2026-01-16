# 보드 이관 시 업무 소유권 일괄 이전 설계서

## 1. 개요

### 1.1 배경
- v2.2.1에서 TB_ITEM에 OWNER_USERNAME 컬럼 추가 완료
- 보드 소유권 이전 시 해당 보드 내 업무들의 OWNER_USERNAME도 함께 변경 필요
- 두 가지 이관 케이스 존재: 직접 이관, 사용자 삭제 시 이관

### 1.2 범위
- 보드 직접 이관 시 업무 소유권 일괄 변경
- 사용자 삭제 전 필수 이관 프로세스
- 배정(Assignment) 업무 영향 처리

---

## 2. 현재 FK 제약 조건 분석

### 2.1 관련 FK 현황
| 테이블.컬럼 | FK 대상 | ON DELETE | 설명 |
|-------------|---------|-----------|------|
| TB_BOARD.OWNER_USERNAME | TB_USER | RESTRICT | 삭제 전 이관 필수 |
| TB_ITEM.OWNER_USERNAME | TB_USER | RESTRICT | 삭제 전 이관 필수 |
| TB_ITEM.ASSIGNEE_USERNAME | TB_USER | SET NULL | 자동 NULL 처리 |
| TB_ITEM.CREATED_BY | TB_USER | RESTRICT | 생성자 정보 유지 |
| TB_ITEM_SHARE.USERNAME | TB_USER | CASCADE | 공유 자동 삭제 |
| TB_ITEM_SHARE.ASSIGNED_BY | - | FK 없음 | 배정자 이력 유지 |

### 2.2 FK 정책 의미
- **RESTRICT**: 해당 사용자 삭제 불가 → 삭제 전 이관 필수
- **SET NULL**: 사용자 삭제 시 NULL로 자동 변경
- **CASCADE**: 사용자 삭제 시 관련 레코드 자동 삭제

---

## 3. 이관 케이스별 처리 설계

### 3.1 케이스1: 보드 직접 이관 (사용자 유지)

#### 3.1.1 시나리오
```
[이관 전]
보드 소유자: userA
├── 업무1: ownerUsername=userA, assigneeUsername=userC
│   └── TB_ITEM_SHARE: USERNAME=userC, ASSIGNED_BY=userA
├── 업무2: ownerUsername=userA
└── 업무3: ownerUsername=userA
    └── 하위업무3-1: ownerUsername=userA

[userA → userB 이관 후]
보드 소유자: userB
├── 업무1: ownerUsername=userB, assigneeUsername=userC (유지)
│   └── TB_ITEM_SHARE: USERNAME=userC, ASSIGNED_BY=userA (유지)
├── 업무2: ownerUsername=userB
└── 업무3: ownerUsername=userB
    └── 하위업무3-1: ownerUsername=userB
```

#### 3.1.2 처리 내용
| 항목 | 변경 | 비고 |
|------|------|------|
| TB_BOARD.OWNER_USERNAME | A → B | 기존 로직 |
| TB_ITEM.OWNER_USERNAME | A → B | **신규 추가** |
| TB_ITEM.ASSIGNEE_USERNAME | 유지 | 기존 담당자 유지 |
| TB_ITEM_SHARE.ASSIGNED_BY | 유지 | 배정 이력 보존 |
| TB_ITEM_SHARE (배정 레코드) | 유지 | 배정 관계 유지 |

#### 3.1.3 배정 업무 권한 변경
```
[이관 전]
- userA: 소유자 → 배정 관리 권한
- userC: 배정받은 사용자

[이관 후]
- userB: 새 소유자 → 배정 관리 권한 획득
- userA: 이전 소유자 → 권한 없음
- userC: 배정받은 사용자 → 기존 배정 유지
```

### 3.2 케이스2: 사용자 삭제 시 이관

#### 3.2.1 시나리오
```
[삭제 전 - userA 보유 자산]
├── 보드1 (소유)
│   ├── 업무1: ownerUsername=userA
│   └── 업무2: ownerUsername=userA, 배정→userC
├── 보드2 (소유)
│   └── 업무3: ownerUsername=userA
└── 업무X (타인 보드): userA가 배정받음

[userA 삭제 시 userB에게 이관 후]
├── 보드1 (소유자: userB)
│   ├── 업무1: ownerUsername=userB
│   └── 업무2: ownerUsername=userB, 배정→userC (유지)
├── 보드2 (소유자: userB)
│   └── 업무3: ownerUsername=userB
└── 업무X: userA 배정 자동 삭제 (CASCADE)
```

#### 3.2.2 자동 처리 (FK CASCADE/SET NULL)
| 테이블 | 컬럼 | 처리 | 결과 |
|--------|------|------|------|
| TB_ITEM_SHARE | USERNAME=userA | CASCADE | 레코드 삭제 |
| TB_ITEM | ASSIGNEE_USERNAME=userA | SET NULL | NULL로 변경 |

#### 3.2.3 수동 이관 필수 (FK RESTRICT)
| 테이블 | 컬럼 | 처리 | 비고 |
|--------|------|------|------|
| TB_BOARD | OWNER_USERNAME=userA | 이관 필수 | 삭제 차단됨 |
| TB_ITEM | OWNER_USERNAME=userA | 이관 필수 | 삭제 차단됨 |
| TB_ITEM | CREATED_BY=userA | **유지** | 생성자 이력 |

#### 3.2.4 삭제 전 필수 절차
```
1. userA 소유 보드 목록 조회
2. 각 보드를 대상 사용자(userB)에게 이관
   - 보드 OWNER_USERNAME 변경
   - 보드 내 모든 업무 OWNER_USERNAME 변경
3. userA 소유 업무 중 보드 외 직접 소유 업무 처리 (해당 시)
4. 이관 완료 후 userA 삭제 가능
```

---

## 4. 배정 업무 영향 상세 분석

### 4.1 배정 관계 데이터
```sql
-- TB_ITEM_SHARE 배정 레코드 구조
ITEM_ID        -- 배정된 업무
USERNAME       -- 배정받은 사용자 (FK CASCADE)
SHARE_TYPE     -- 'ASSIGN'
ASSIGNED_BY    -- 배정한 사용자 (FK 없음)
ASSIGNED_AT    -- 배정 일시
```

### 4.2 케이스별 배정 영향

#### 4.2.1 보드 직접 이관 시 (userA→userB)
| 상황 | 영향 | 처리 |
|------|------|------|
| userA가 userC에게 배정한 업무 | ASSIGNED_BY=userA 유지 | 이력 보존 |
| userB가 새 소유자로서 배정 관리 | 권한 자동 획득 | 추가 처리 불필요 |
| 기존 배정 취소/변경 | userB가 관리 | 정상 동작 |

#### 4.2.2 사용자 삭제 시 (userA 삭제)
| 상황 | 영향 | 처리 |
|------|------|------|
| userA가 배정한 업무 | ASSIGNED_BY=userA 유지 | FK 없어 유지됨 |
| userA가 배정받은 업무 | TB_ITEM_SHARE 레코드 삭제 | CASCADE |
| userA가 담당자인 업무 | ASSIGNEE_USERNAME=NULL | SET NULL |

### 4.3 ASSIGNED_BY 표시 문제
```
[문제]
userA 삭제 후 ASSIGNED_BY=userA인 레코드 조회 시
→ TB_USER 조인 실패 → 배정자 이름 NULL

[해결 방안]
1. LEFT JOIN 사용 (현재 적용됨)
2. 배정자 이름이 NULL일 경우 "삭제된 사용자" 표시
```

---

## 5. 구현 설계

### 5.1 DB 변경사항
```sql
-- 추가 변경 없음 (v2.2.1에서 OWNER_USERNAME 추가 완료)
```

### 5.2 Mapper 변경

#### 5.2.1 ItemMapper.xml - 보드별 업무 소유자 일괄 변경
```xml
<!-- 보드 이관 시 업무 소유자 일괄 변경 (v2.2.1) -->
<update id="updateOwnerByBoardId">
    UPDATE TB_ITEM
    SET OWNER_USERNAME = #{newOwnerUsername},
        UPDATED_BY = #{updatedBy},
        UPDATED_AT = CURRENT_TIMESTAMP
    WHERE BOARD_ID = #{boardId}
</update>
```

#### 5.2.2 ItemMapper.java
```java
/**
 * 보드 이관 시 업무 소유자 일괄 변경 (v2.2.1)
 */
int updateOwnerByBoardId(@Param("boardId") Long boardId,
                         @Param("newOwnerUsername") String newOwnerUsername,
                         @Param("updatedBy") String updatedBy);
```

### 5.3 Service 변경

#### 5.3.1 BoardServiceImpl.transferBoardOwnership 수정
```java
@Override
@Transactional
public BoardResponse transferBoardOwnership(Long boardId, BoardTransferRequest request, String currentUsername) {
    // ... 기존 검증 로직 ...

    // 보드 소유권 이전
    boardMapper.transferOwnership(boardId, request.getTargetUsername(), TRANSFERRED_BOARD_NAME, currentUsername);

    // [v2.2.1 추가] 보드 내 모든 업무 소유자 일괄 변경
    int itemCount = itemMapper.updateOwnerByBoardId(boardId, request.getTargetUsername(), currentUsername);
    log.info("Board transfer - updated {} items owner to {}", itemCount, request.getTargetUsername());

    // ... 나머지 로직 ...
}
```

#### 5.3.2 UserService - 삭제 전 검증 추가
```java
@Override
@Transactional
public void deleteUser(Long userId) {
    User user = userMapper.findById(userId)
            .orElseThrow(() -> BusinessException.userNotFound(userId));

    // [v2.2.1 추가] 소유 보드 확인
    List<Board> ownedBoards = boardMapper.findByOwnerUsername(user.getUsername());
    if (!ownedBoards.isEmpty()) {
        throw BusinessException.badRequest(
            String.format("사용자가 소유한 보드 %d개를 먼저 이관해야 합니다.", ownedBoards.size())
        );
    }

    // [v2.2.1 추가] 소유 업무 확인 (이관되지 않은 업무)
    int ownedItemCount = itemMapper.countByOwnerUsername(user.getUsername());
    if (ownedItemCount > 0) {
        throw BusinessException.badRequest(
            String.format("사용자가 소유한 업무 %d개를 먼저 이관해야 합니다.", ownedItemCount)
        );
    }

    // ... 기존 삭제 로직 ...
}
```

### 5.4 API 변경사항
- 기존 API 스펙 변경 없음
- 내부 로직만 변경

---

## 6. 테스트 시나리오

### 6.1 보드 직접 이관 테스트

| # | 시나리오 | 기대 결과 |
|---|----------|-----------|
| 1 | 업무 없는 보드 이관 | 보드 소유자만 변경 |
| 2 | 업무 있는 보드 이관 | 보드+업무 소유자 모두 변경 |
| 3 | 하위 업무 포함 보드 이관 | 부모+하위 업무 모두 소유자 변경 |
| 4 | 배정된 업무 있는 보드 이관 | 배정 관계 유지, 소유자만 변경 |
| 5 | 이관 후 새 소유자 배정 관리 | 새 소유자가 배정 취소/변경 가능 |

### 6.2 사용자 삭제 테스트

| # | 시나리오 | 기대 결과 |
|---|----------|-----------|
| 1 | 보드 소유 사용자 삭제 시도 | 오류: 이관 필요 메시지 |
| 2 | 보드 이관 후 삭제 | 정상 삭제 |
| 3 | 배정받은 업무 있는 사용자 삭제 | 배정 레코드 CASCADE 삭제 |
| 4 | 담당자로 지정된 업무 있는 사용자 삭제 | ASSIGNEE_USERNAME NULL 처리 |
| 5 | 삭제 후 ASSIGNED_BY 표시 | "삭제된 사용자" 또는 빈 값 표시 |

### 6.3 배정 영향 테스트

| # | 시나리오 | 기대 결과 |
|---|----------|-----------|
| 1 | 이관 후 기존 배정 조회 | ASSIGNED_BY 이력 유지 |
| 2 | 이관 후 새 소유자 배정 추가 | 정상 동작 |
| 3 | 이관 후 기존 배정 취소 | 새 소유자가 취소 가능 |
| 4 | 이관 후 배정 권한 변경 | 새 소유자가 변경 가능 |

---

## 7. 영향 범위

### 7.1 백엔드
| 파일 | 변경 내용 |
|------|----------|
| ItemMapper.xml | updateOwnerByBoardId 쿼리 추가 |
| ItemMapper.java | updateOwnerByBoardId 메서드 추가 |
| BoardServiceImpl.java | transferBoardOwnership 로직 수정 |
| UserServiceImpl.java | deleteUser 검증 로직 추가 |

### 7.2 프론트엔드
- 변경 없음 (API 스펙 동일)

### 7.3 데이터베이스
- 스키마 변경 없음 (v2.2.1 OWNER_USERNAME 추가 완료)

---

## 8. 체크리스트

### 8.1 구현 체크리스트
- [ ] ItemMapper.xml - updateOwnerByBoardId 쿼리 추가
- [ ] ItemMapper.java - updateOwnerByBoardId 메서드 추가
- [ ] BoardServiceImpl - 보드 이관 시 업무 소유자 일괄 변경
- [ ] UserServiceImpl - 삭제 전 소유 보드/업무 검증

### 8.2 테스트 체크리스트
- [ ] 보드 직접 이관 - 업무 소유자 변경 확인
- [ ] 보드 직접 이관 - 하위 업무 소유자 변경 확인
- [ ] 보드 직접 이관 - 배정 관계 유지 확인
- [ ] 사용자 삭제 - 소유 보드 있을 때 차단 확인
- [ ] 사용자 삭제 - 이관 후 삭제 가능 확인
- [ ] 배정 표시 - 삭제된 사용자 처리 확인

---

## 9. 버전 정보

| 항목 | 내용 |
|------|------|
| 문서 버전 | 1.0 |
| 작성일 | 2026-01-16 |
| 관련 버전 | v2.2.1 |
| 선행 작업 | 01_ITEM_OWNER_DESIGN.md |
