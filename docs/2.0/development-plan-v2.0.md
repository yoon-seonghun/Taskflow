# TaskFlow 2.0 개발 계획서

> **작성일**: 2025-01-04
> **버전**: 2.0
> **참조 문서**: merged-design-v2.0.md

---

## 1. 개발 개요

### 1.1 목표
- 카테고리 기반 속성 관리 시스템 구현
- 성과 점수 시스템 구현
- 속성 변경 이력 관리 구현

### 1.2 개발 범위

| 구분 | 항목 |
|------|------|
| **신규 테이블** | TB_CATEGORY, TB_CATEGORY_SHARE, TB_CATEGORY_PROPERTY, TB_BOARD_CATEGORY, TB_BOARD_PROPERTY, TB_ITEM_PROPERTY_HISTORY, TB_ITEM_SCORE |
| **수정 테이블** | TB_PROPERTY_DEF |
| **Backend** | Domain, Mapper, Service, Controller |
| **Frontend** | Types, API, Store, Components, Views |

---

## 2. Phase 1: DB 스키마 변경

### 2.1 스키마 마이그레이션 SQL 작성
- [ ] `03_v2.0_schema.sql` 작성
  - TB_PROPERTY_DEF 수정 (OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE)
  - TB_CATEGORY 생성
  - TB_CATEGORY_SHARE 생성
  - TB_CATEGORY_PROPERTY 생성
  - TB_BOARD_CATEGORY 생성
  - TB_BOARD_PROPERTY 생성
  - TB_ITEM_PROPERTY_HISTORY 생성
  - TB_ITEM_SCORE 생성

### 2.2 초기 데이터 SQL 작성
- [ ] `04_v2.0_init_data.sql` 작성
  - 글로벌 속성 (시작일, 완료일, 난이도, 범위변경, 리스크대응)
  - 글로벌 속성 옵션 (난이도, 범위변경, 리스크대응)

---

## 3. Phase 2: Backend - 카테고리 시스템

### 3.1 Domain 클래스

| 파일 | 설명 |
|------|------|
| `Category.java` | 카테고리 엔티티 |
| `CategoryShare.java` | 카테고리 공유 엔티티 |
| `CategoryProperty.java` | 카테고리-속성 매핑 엔티티 |
| `BoardCategory.java` | 보드-카테고리 매핑 엔티티 |
| `BoardProperty.java` | 보드-속성 매핑 엔티티 |

### 3.2 DTO 클래스

| 파일 | 설명 |
|------|------|
| `CategoryCreateRequest.java` | 카테고리 생성 요청 |
| `CategoryUpdateRequest.java` | 카테고리 수정 요청 |
| `CategoryResponse.java` | 카테고리 응답 |
| `CategoryPropertyRequest.java` | 카테고리-속성 매핑 요청 |
| `CategoryShareRequest.java` | 카테고리 공유 요청 |
| `BoardPropertyRequest.java` | 보드-속성 매핑 요청 |

### 3.3 Mapper 인터페이스 및 XML

| 파일 | 설명 |
|------|------|
| `CategoryMapper.java/xml` | 카테고리 CRUD |
| `CategoryShareMapper.java/xml` | 카테고리 공유 CRUD |
| `CategoryPropertyMapper.java/xml` | 카테고리-속성 매핑 |
| `BoardCategoryMapper.java/xml` | 보드-카테고리 매핑 |
| `BoardPropertyMapper.java/xml` | 보드-속성 매핑 |

### 3.4 Service 클래스

| 파일 | 설명 |
|------|------|
| `CategoryService.java` | 카테고리 비즈니스 로직 |
| `CategoryShareService.java` | 카테고리 공유 로직 |
| `BoardPropertyService.java` | 보드-속성 관리 로직 |

### 3.5 Controller 클래스

| 파일 | 설명 |
|------|------|
| `CategoryController.java` | 카테고리 API |

### 3.6 기존 파일 수정

| 파일 | 수정 내용 |
|------|----------|
| `PropertyDef.java` | OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE 필드 추가 |
| `PropertyDefMapper.xml` | 새 필드 매핑, 글로벌/매니저 속성 조회 쿼리 추가 |
| `PropertyService.java` | 글로벌/매니저 속성 관리 메서드 추가 |
| `PropertyController.java` | 글로벌/매니저 속성 API 추가 |
| `BoardService.java` | 보드 생성 시 카테고리/속성 처리 로직 추가 |
| `ItemService.java` | 업무 생성 시 속성 상속 로직 추가 |

---

## 4. Phase 3: Backend - 성과 점수 시스템

### 4.1 Domain 클래스

| 파일 | 설명 |
|------|------|
| `ItemPropertyHistory.java` | 속성 변경 이력 엔티티 |
| `ItemScore.java` | 성과 점수 엔티티 |

### 4.2 DTO 클래스

| 파일 | 설명 |
|------|------|
| `ItemScoreResponse.java` | 점수 응답 |
| `ScoreWeightRequest.java` | 가중치 수정 요청 |
| `ScoreApprovalRequest.java` | 승인 요청 |
| `ScoreStatisticsResponse.java` | 통계 응답 |
| `ItemHistoryResponse.java` | 이력 응답 |

### 4.3 Mapper 인터페이스 및 XML

| 파일 | 설명 |
|------|------|
| `ItemPropertyHistoryMapper.java/xml` | 이력 CRUD |
| `ItemScoreMapper.java/xml` | 점수 CRUD |

### 4.4 Service 클래스

| 파일 | 설명 |
|------|------|
| `ItemScoreService.java` | 점수 계산 및 관리 |
| `ItemHistoryService.java` | 이력 기록 및 조회 |

### 4.5 Controller 클래스

| 파일 | 설명 |
|------|------|
| `ItemScoreController.java` | 점수 API |
| `ItemHistoryController.java` | 이력 API |

### 4.6 기존 파일 수정

| 파일 | 수정 내용 |
|------|----------|
| `ItemService.java` | 완료 처리 시 점수 자동 계산, 속성 변경 시 이력 기록 |

---

## 5. Phase 4: Frontend - 카테고리 시스템

### 5.1 타입 정의

| 파일 | 설명 |
|------|------|
| `types/category.ts` | Category, CategoryShare, CategoryProperty 타입 |
| `types/property.ts` | PropertyDef 확장 (ownerType 등) |

### 5.2 API 클라이언트

| 파일 | 설명 |
|------|------|
| `api/category.ts` | 카테고리 API 호출 |
| `api/property.ts` | 속성 API 확장 |

### 5.3 Store

| 파일 | 설명 |
|------|------|
| `stores/category.ts` | 카테고리 상태 관리 |
| `stores/property.ts` | 속성 상태 관리 확장 |

### 5.4 Components

| 파일 | 설명 |
|------|------|
| `components/category/CategoryList.vue` | 카테고리 목록 |
| `components/category/CategoryForm.vue` | 카테고리 생성/수정 |
| `components/category/CategoryPropertyManager.vue` | 카테고리 속성 관리 |
| `components/category/CategoryShareManager.vue` | 카테고리 공유 관리 |
| `components/property/PropertySelector.vue` | 통합 속성 선택 패널 |
| `components/property/GlobalPropertyManager.vue` | 글로벌 속성 관리 (ADMIN) |

### 5.5 Views/기존 수정

| 파일 | 수정 내용 |
|------|----------|
| `views/SettingsView.vue` | 카테고리 관리 탭 추가 |
| `components/board/BoardForm.vue` | 카테고리 선택, 속성 선택 UI 추가 |
| `components/item/ItemForm.vue` | 속성 편집 접힘/펼침 UI 추가 |

---

## 6. Phase 5: Frontend - 성과 점수 시스템

### 6.1 타입 정의

| 파일 | 설명 |
|------|------|
| `types/score.ts` | ItemScore, ScoreStatistics 타입 |
| `types/history.ts` | ItemPropertyHistory 타입 확장 |

### 6.2 API 클라이언트

| 파일 | 설명 |
|------|------|
| `api/score.ts` | 점수 API 호출 |

### 6.3 Store

| 파일 | 설명 |
|------|------|
| `stores/score.ts` | 점수 상태 관리 |

### 6.4 Components

| 파일 | 설명 |
|------|------|
| `components/score/ScoreDisplay.vue` | 점수 표시 |
| `components/score/ScoreWeightEditor.vue` | 가중치 편집 |
| `components/score/ScoreApprovalPanel.vue` | 승인 패널 |
| `components/score/ScoreStatistics.vue` | 통계 차트 |
| `components/history/PropertyHistoryList.vue` | 속성 변경 이력 |

### 6.5 Views

| 파일 | 설명 |
|------|------|
| `views/ScoreDashboardView.vue` | 성과 대시보드 |

---

## 7. Phase 6: 통합 및 테스트

### 7.1 통합 작업
- [ ] 날짜 속성 변경 시 이력 자동 기록
- [ ] 완료 처리 시 점수 자동 계산
- [ ] 지연 계산 로직과 점수 시스템 통합
- [ ] SSE 이벤트 확장 (카테고리, 점수 변경)

### 7.2 테스트
- [ ] 카테고리 CRUD 테스트
- [ ] 속성 상속 테스트
- [ ] 점수 계산 테스트
- [ ] 이력 기록 테스트
- [ ] 권한 테스트

---

## 8. 파일 구조 요약

### 8.1 Backend 신규 파일

```
backend/src/main/java/com/taskflow/
├── domain/
│   ├── Category.java
│   ├── CategoryShare.java
│   ├── CategoryProperty.java
│   ├── BoardCategory.java
│   ├── BoardProperty.java
│   ├── ItemPropertyHistory.java
│   └── ItemScore.java
├── dto/
│   ├── category/
│   │   ├── CategoryCreateRequest.java
│   │   ├── CategoryUpdateRequest.java
│   │   ├── CategoryResponse.java
│   │   ├── CategoryPropertyRequest.java
│   │   └── CategoryShareRequest.java
│   ├── property/
│   │   └── BoardPropertyRequest.java
│   └── score/
│       ├── ItemScoreResponse.java
│       ├── ScoreWeightRequest.java
│       ├── ScoreApprovalRequest.java
│       └── ScoreStatisticsResponse.java
├── mapper/
│   ├── CategoryMapper.java
│   ├── CategoryShareMapper.java
│   ├── CategoryPropertyMapper.java
│   ├── BoardCategoryMapper.java
│   ├── BoardPropertyMapper.java
│   ├── ItemPropertyHistoryMapper.java
│   └── ItemScoreMapper.java
├── service/
│   ├── CategoryService.java
│   ├── CategoryShareService.java
│   ├── BoardPropertyService.java
│   ├── ItemScoreService.java
│   ├── ItemHistoryService.java
│   └── impl/
│       ├── CategoryServiceImpl.java
│       ├── CategoryShareServiceImpl.java
│       ├── BoardPropertyServiceImpl.java
│       ├── ItemScoreServiceImpl.java
│       └── ItemHistoryServiceImpl.java
└── controller/
    ├── CategoryController.java
    ├── ItemScoreController.java
    └── ItemHistoryController.java

backend/src/main/resources/mapper/
├── CategoryMapper.xml
├── CategoryShareMapper.xml
├── CategoryPropertyMapper.xml
├── BoardCategoryMapper.xml
├── BoardPropertyMapper.xml
├── ItemPropertyHistoryMapper.xml
└── ItemScoreMapper.xml
```

### 8.2 Frontend 신규 파일

```
frontend/src/
├── types/
│   ├── category.ts
│   └── score.ts
├── api/
│   ├── category.ts
│   └── score.ts
├── stores/
│   ├── category.ts
│   └── score.ts
├── components/
│   ├── category/
│   │   ├── CategoryList.vue
│   │   ├── CategoryForm.vue
│   │   ├── CategoryPropertyManager.vue
│   │   └── CategoryShareManager.vue
│   ├── property/
│   │   ├── PropertySelector.vue
│   │   └── GlobalPropertyManager.vue
│   ├── score/
│   │   ├── ScoreDisplay.vue
│   │   ├── ScoreWeightEditor.vue
│   │   ├── ScoreApprovalPanel.vue
│   │   └── ScoreStatistics.vue
│   └── history/
│       └── PropertyHistoryList.vue
└── views/
    └── ScoreDashboardView.vue
```

### 8.3 SQL 파일

```
docker/mysql/init/
├── 03_v2.0_schema.sql
└── 04_v2.0_init_data.sql
```

---

## 9. 개발 순서

```
1. Phase 1: DB 스키마
   ├── 03_v2.0_schema.sql 작성
   └── 04_v2.0_init_data.sql 작성

2. Phase 2: Backend 카테고리
   ├── Domain 클래스
   ├── DTO 클래스
   ├── Mapper 인터페이스/XML
   ├── Service 클래스
   ├── Controller 클래스
   └── 기존 파일 수정

3. Phase 3: Backend 성과 점수
   ├── Domain 클래스
   ├── DTO 클래스
   ├── Mapper 인터페이스/XML
   ├── Service 클래스
   └── Controller 클래스

4. Phase 4: Frontend 카테고리
   ├── 타입 정의
   ├── API 클라이언트
   ├── Store
   ├── Components
   └── Views 수정

5. Phase 5: Frontend 성과 점수
   ├── 타입 정의
   ├── API 클라이언트
   ├── Store
   ├── Components
   └── Views

6. Phase 6: 통합 및 테스트
   ├── 통합 작업
   └── 테스트
```

---

## 10. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2025-01-04 | 개발 계획서 최초 작성 |
