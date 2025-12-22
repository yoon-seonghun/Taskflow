# TaskFlow - 업무 리스트 실시간 공유 웹앱

## 프로젝트 개요

Notion 스타일의 업무 관리 시스템으로, 팀 구성원 간 업무 현황을 실시간으로 공유하고 협업할 수 있는 웹앱입니다.

### 핵심 특징
- Notion 스타일 Compact/Dense UI
- 동적 속성 시스템 (EAV 패턴)
- 다중 뷰 지원 (칸반/테이블/리스트)
- 인라인 편집 및 실시간 동기화
- 모바일/PC 반응형

---

## 기술 스택

| 구분 | 기술 | 버전/비고 |
|------|------|-----------|
| 플랫폼 | Docker Container | - |
| 데이터베이스 | MySQL | 8.0+ |
| ORM | MyBatis XML Mapper | **JPA 사용 금지** |
| 백엔드 | Spring Boot | 3.x (Java 17+) |
| 프론트엔드 | Vue.js 3 | Composition API |
| 상태관리 | Pinia | - |
| 빌드도구 | Vite | - |
| IDE | VS Code | - |

---

## 개발 규칙 (⚠️ 필수 준수)

### 설계 우선 원칙
```
1. 구현 전 반드시 설계 문서를 먼저 제시하고 승인 필수
2. 설계 승인 없이 코드 작성 금지
3. 설계 변경이 필요하면 먼저 변경안을 제시하고 승인 필수
4. DB/API/COMPONENT 설계는 승인 후 진행 필수
```

### 설계 산출물 순서
```
1단계: ERD 및 테이블 정의서 → 승인 대기
2단계: API 명세서 → 승인 대기
3단계: 컴포넌트 구조 → 승인 대기
4단계: 구현 시작

각 단계마다 "승인해주세요" 라고 항상 요청할 것
```

### 디버깅 및 오류 수정 원칙
```
⚠️ 절대 금지 사항
1. 기능 삭제로 오류 해결 금지
2. 기능 축소로 오류 해결 금지
3. 역할/책임 감소로 오류 해결 금지

✅ 필수 준수 사항
1. 오류는 근본 원인을 찾아 정상 수정할 것
2. 기능 축소/삭제가 불가피한 경우 → 반드시 승인 요청
3. 수정 전 영향 범위 분석 필수:
   - 해당 함수를 호출하는 다른 코드
   - 연관된 API 엔드포인트
   - 프론트엔드 연동 부분
   - 테스트 코드

📋 수정 시 제출 형식
- 수정 대상: [파일:라인]
- 수정 내용: [변경 사항]
- 영향 범위: [연관 기능 목록]
- 테스트 방법: [검증 절차]
```
---

## 데이터베이스 컨벤션

### 테이블 명명 규칙
- 테이블명: **대문자, 스네이크케이스**
- PK: `TB명_ID` (예: `ITEM_ID`, `USER_ID`)
- 날짜: **DATE 타입** (문자열 저장 금지)

### 공통 컬럼
모든 테이블에 아래 컬럼 필수 포함:
```sql
CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
CREATED_BY BIGINT NOT NULL,
UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
UPDATED_BY BIGINT NULL
```

### 핵심 테이블 구조
- 컬렉션(보드) - 아이템 관계
- 동적 속성 정의 테이블 (EAV 패턴)
- 속성 옵션(선택값) 관리
- 코드 테이블 분리

---

## 백엔드 컨벤션

### 패키지 구조
```
com.taskflow
├── config/           # 설정 클래스
├── controller/       # REST 컨트롤러
├── service/          # 비즈니스 로직
├── mapper/           # MyBatis Mapper 인터페이스
├── dto/              # 요청/응답 DTO
├── domain/           # 엔티티 클래스
├── common/           # 공통 유틸, 상수
└── exception/        # 예외 처리
```

### MyBatis 규칙
- XML Mapper 파일 위치: `resources/mapper/**/*.xml`
- Mapper 인터페이스에 `@Mapper` 어노테이션 사용
- SQL문은 XML에 작성 (어노테이션 SQL 지양)
- **JPA 절대 사용 금지**

### API 설계 원칙
```
# 인증
POST   /api/auth/login          # 로그인
POST   /api/auth/logout         # 로그아웃
POST   /api/auth/refresh        # 토큰 갱신

# 사용자
GET    /api/users               # 사용자 목록
POST   /api/users               # 사용자 등록
GET    /api/users/{id}          # 사용자 조회
PUT    /api/users/{id}          # 사용자 수정
DELETE /api/users/{id}          # 사용자 삭제

# 부서
GET    /api/departments              # 부서 목록 (트리 구조)
GET    /api/departments/flat         # 부서 목록 (평면 구조)
POST   /api/departments              # 부서 생성
GET    /api/departments/{id}         # 부서 조회
PUT    /api/departments/{id}         # 부서 수정
DELETE /api/departments/{id}         # 부서 삭제
PUT    /api/departments/{id}/order   # 부서 순서 변경
GET    /api/departments/{id}/users   # 부서별 사용자 목록

# 그룹
GET    /api/groups                   # 그룹 목록
POST   /api/groups                   # 그룹 생성
GET    /api/groups/{id}              # 그룹 조회
PUT    /api/groups/{id}              # 그룹 수정
DELETE /api/groups/{id}              # 그룹 삭제
PUT    /api/groups/{id}/order        # 그룹 순서 변경
GET    /api/groups/{id}/members      # 그룹 멤버 목록
POST   /api/groups/{id}/members      # 그룹 멤버 추가
DELETE /api/groups/{id}/members/{userId}  # 그룹 멤버 제거

# 보드 (컬렉션)
GET    /api/boards              # 보드 목록
POST   /api/boards              # 보드 생성
GET    /api/boards/{id}         # 보드 조회
PUT    /api/boards/{id}         # 보드 수정
DELETE /api/boards/{id}         # 보드 삭제

# 공유 사용자
GET    /api/boards/{id}/shares         # 공유 사용자 목록
POST   /api/boards/{id}/shares         # 공유 사용자 추가
DELETE /api/boards/{id}/shares/{userId} # 공유 사용자 제거

# 아이템 (업무)
GET    /api/boards/{boardId}/items           # 아이템 목록 (필터/정렬 지원)
POST   /api/boards/{boardId}/items           # 아이템 생성
GET    /api/boards/{boardId}/items/{id}      # 아이템 조회
PUT    /api/boards/{boardId}/items/{id}      # 아이템 수정
DELETE /api/boards/{boardId}/items/{id}      # 아이템 삭제
PUT    /api/boards/{boardId}/items/{id}/complete  # 완료 처리
PUT    /api/boards/{boardId}/items/{id}/restore   # 복원 처리

# 댓글
GET    /api/items/{itemId}/comments     # 댓글 목록
POST   /api/items/{itemId}/comments     # 댓글 등록
PUT    /api/comments/{id}               # 댓글 수정
DELETE /api/comments/{id}               # 댓글 삭제

# 속성 정의
GET    /api/boards/{boardId}/properties      # 속성 정의 목록
POST   /api/boards/{boardId}/properties      # 속성 정의 생성
PUT    /api/properties/{id}                  # 속성 정의 수정
DELETE /api/properties/{id}                  # 속성 정의 삭제

# 속성 옵션 (코드 항목)
GET    /api/properties/{propId}/options      # 옵션 목록
POST   /api/properties/{propId}/options      # 옵션 추가
PUT    /api/options/{id}                     # 옵션 수정
DELETE /api/options/{id}                     # 옵션 삭제

# 작업 템플릿 (작업 등록 메뉴용)
GET    /api/task-templates              # 템플릿 목록
POST   /api/task-templates              # 템플릿 등록
PUT    /api/task-templates/{id}         # 템플릿 수정
DELETE /api/task-templates/{id}         # 템플릿 삭제
GET    /api/task-templates/search       # 템플릿 검색 (자동완성용)

# 이력
GET    /api/history/items               # 작업 처리 이력
GET    /api/history/templates           # 작업 등록 이력

# SSE (실시간 동기화)
GET    /api/sse/subscribe               # SSE 연결
```

### 쿼리 파라미터 (목록 조회)
```
# 페이징
?page=0&size=20

# 정렬
?sort=createdAt,desc

# 필터
?status=IN_PROGRESS
?priority=HIGH
?assigneeId=1
?groupId=1
?departmentId=1
?startDate=2024-01-01&endDate=2024-12-31

# 검색
?keyword=검색어

# 완료/삭제 포함 여부
?includeCompleted=false
?includeDeleted=false

# 사용 여부 (부서/그룹 조회 시)
?useYn=Y
```

### 응답 형식
```java
// 성공 응답
{
  "success": true,
  "data": { ... },
  "message": null
}

// 실패 응답
{
  "success": false,
  "data": null,
  "message": "에러 메시지"
}
```

---

## 프론트엔드 컨벤션

### 디렉토리 구조
```
src/
├── assets/           # 정적 파일
├── components/       # 공통 컴포넌트
│   ├── common/       # 버튼, 인풋 등
│   ├── layout/       # 레이아웃 컴포넌트
│   └── ui/           # UI 컴포넌트
├── composables/      # Composition API 훅
├── router/           # Vue Router 설정
├── stores/           # Pinia 스토어
├── views/            # 페이지 컴포넌트
├── api/              # API 호출 모듈
├── types/            # TypeScript 타입 정의
└── utils/            # 유틸리티 함수
```

### Composition API 패턴
```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useItemStore } from '@/stores/item'

// Props
const props = defineProps<{
  itemId: number
}>()

// Emits
const emit = defineEmits<{
  (e: 'update', value: Item): void
}>()

// Store
const itemStore = useItemStore()

// State
const loading = ref(false)

// Computed
const item = computed(() => itemStore.getItemById(props.itemId))

// Methods
async function fetchItem() {
  loading.value = true
  await itemStore.fetchItem(props.itemId)
  loading.value = false
}

// Lifecycle
onMounted(() => {
  fetchItem()
})
</script>
```

### 상태 관리 (Pinia)
```typescript
// stores/property.ts
export const usePropertyStore = defineStore('property', () => {
  // 속성 정의는 앱 초기화 시 로드하여 Store에 보관
  const propertyDefinitions = ref<PropertyDefinition[]>([])
  
  // 속성 변경 시 Store 갱신 후 API 호출 (Optimistic Update)
  async function updateProperty(id: number, data: Partial<PropertyDefinition>) {
    // 1. Store 먼저 갱신
    const index = propertyDefinitions.value.findIndex(p => p.id === id)
    if (index !== -1) {
      propertyDefinitions.value[index] = { ...propertyDefinitions.value[index], ...data }
    }
    
    // 2. API 호출
    try {
      await api.updateProperty(id, data)
    } catch (error) {
      // 실패 시 롤백
      await fetchProperties()
    }
  }
  
  return { propertyDefinitions, updateProperty }
})
```

---

## UI/UX 설계 조건

### Compact/Dense UI 스펙
| 항목 | 스펙 |
|------|------|
| Row Height | 36px 이하 |
| 컴포넌트 간 Spacing | 8px |
| 폰트 사이즈 | 13~14px |
| 화면 표시 항목 | 최소 15개 이상 |

### 레이아웃 구조
```
┌─────────────────────────────────────────────────────────┐
│                      Header                              │
├──────────┬──────────────────────────────────────────────┤
│          │                                               │
│  Sidebar │              Main Panel                       │
│  (Nav)   │        (칸반/테이블/리스트 뷰)                  │
│          │                                               │
│          │                          ┌──────────────────┐ │
│          │                          │   Slide-over    │ │
│          │                          │   Panel         │ │
│          │                          │   (상세/편집)    │ │
│          │                          └──────────────────┘ │
└──────────┴──────────────────────────────────────────────┘
```

### 인터랙션 패턴
- 아이템 클릭 → 슬라이드오버 패널 오픈
- 컨텍스트 메뉴(⋮) → 수정/삭제/복제
- 패널 외부 클릭 또는 ESC → 패널 닫기
- 속성값 클릭 → 즉시 편집 모드
- 선택형 속성 → 드롭다운 내 신규 옵션 추가 가능

### 반응형 처리
```
Mobile (< 768px):
- 사이드바 숨김 (햄버거 메뉴)
- 아이템 선택 시 전체 화면 편집 페이지

PC (>= 768px):
- 사이드바 표시
- 아이템 선택 시 우측 슬라이드오버 패널
```

---

## 동적 속성 시스템

### 속성 타입
| 타입 | 설명 | 저장 방식 |
|------|------|-----------|
| TEXT | 텍스트 | VARCHAR |
| NUMBER | 숫자 | DECIMAL |
| DATE | 날짜 | DATE |
| SELECT | 단일선택 | FK (옵션 ID) |
| MULTI_SELECT | 다중선택 | 별도 매핑 테이블 |
| CHECKBOX | 체크박스 | BOOLEAN |
| USER | 사용자 | FK (사용자 ID) |

### 대표 속성 (기본 제공)
| 속성명 | 타입 | 기본 옵션 |
|--------|------|-----------|
| 카테고리 | SELECT | 사용자 정의 |
| 상태 | SELECT | 시작전, 진행중, 완료, 삭제 |
| 우선순위 | SELECT | 긴급, 높음, 보통, 낮음 |
| 그룹 | SELECT | 사용자 정의 |

### 속성 관리 UI (컬럼 헤더)

```
[컬럼 헤더 클릭 시 드롭다운 메뉴]
┌─────────────────────┐
│ 🔤 속성 이름 변경    │
│ 📝 속성 타입 변경    │
│ ⬅️ 왼쪽으로 이동     │
│ ➡️ 오른쪽으로 이동   │
│ 🙈 숨기기           │
│ ───────────────     │
│ ➕ 새 속성 추가      │
│ 🗑️ 속성 삭제        │
└─────────────────────┘
```

| 기능 | 설명 |
|------|------|
| 속성 이름 변경 | 인라인 텍스트 편집 |
| 속성 타입 변경 | 타입 선택 드롭다운 (데이터 마이그레이션 경고) |
| 순서 변경 | 드래그 앤 드롭 또는 메뉴에서 이동 |
| 숨기기 | 목록에서 컬럼 숨김 (데이터 유지) |
| 새 속성 추가 | 타입 선택 → 이름 입력 |
| 속성 삭제 | 확인 다이얼로그 후 삭제 |

### 코드 항목 관리 (SELECT/MULTI_SELECT 옵션)

#### 인라인 추가
```
[선택형 속성 드롭다운]
┌─────────────────────┐
│ 🔍 검색...          │
├─────────────────────┤
│ ○ 긴급              │
│ ○ 높음              │
│ ○ 보통              │
│ ● 낮음 ✓           │
├─────────────────────┤
│ ➕ "새 옵션" 추가    │  ← 검색어가 없을 때 새 옵션 추가
└─────────────────────┘
```

#### 설정 메뉴 통합 관리
```
[설정 > 코드 관리]
┌─────────────────────────────────────────────┐
│ 코드 관리                                    │
├──────────┬──────────────────────────────────┤
│          │  [상태]                          │
│ 카테고리  │  ┌────────────────────────────┐ │
│ 상태     │  │ 시작전  [색상] [↑] [↓] [🗑] │ │
│ 우선순위  │  │ 진행중  [색상] [↑] [↓] [🗑] │ │
│ 그룹     │  │ 완료    [색상] [↑] [↓] [🗑] │ │
│          │  │ 삭제    [색상] [↑] [↓] [🗑] │ │
│          │  ├────────────────────────────┤ │
│          │  │ ➕ 새 옵션 추가             │ │
│          │  └────────────────────────────┘ │
└──────────┴──────────────────────────────────┘
```

| 기능 | 설명 |
|------|------|
| 옵션 추가 | 새 선택 옵션 생성 |
| 옵션 수정 | 이름, 색상 변경 |
| 옵션 삭제 | 사용 중인 경우 경고 표시 |
| 순서 변경 | 드래그 앤 드롭 또는 화살표 버튼 |
| 색상 지정 | 칸반 뷰, 태그 표시용 색상 |

### 캐시 전략
```
1. 앱 초기화 시 모든 속성 정의 로드
2. Store에 캐시
3. 속성 정의 변경 시:
   - Store 즉시 갱신 (Optimistic Update)
   - API 호출
   - 실패 시 롤백
4. 아이템 조회 시 속성값만 반환 (정의는 캐시 참조)
```

### 캐시 무효화 시점
| 이벤트 | 동작 |
|--------|------|
| 속성 정의 추가 | 전체 속성 정의 재로드 |
| 속성 정의 수정 | 해당 속성만 갱신 |
| 속성 정의 삭제 | 전체 속성 정의 재로드 |
| 옵션 추가/수정/삭제 | 해당 속성의 옵션 목록 갱신 |

---

## 뷰 시스템

### 뷰 타입별 특징
| 뷰 타입 | 설명 | 적합한 용도 |
|---------|------|-------------|
| 테이블 뷰 | 스프레드시트 형태 | 다중 속성 비교, 대량 데이터 |
| 칸반 뷰 | 컬럼별 카드 배치 | 상태 기반 워크플로우 |
| 리스트 뷰 | 단순 목록 형태 | 빠른 스캔, 모바일 최적화 |

### 뷰 전환
```
┌─────────────────────────────────────┐
│ 업무 목록          [테이블▼] [필터] │
├─────────────────────────────────────┤
│  ☰ 테이블                          │
│  ▦ 칸반                            │
│  ≡ 리스트                          │
└─────────────────────────────────────┘
```

### 칸반 뷰 상세
```
[상태 기준 칸반]
┌──────────┬──────────┬──────────┬──────────┐
│  시작전   │  진행중   │   완료    │   삭제   │
│   (3)    │   (5)    │   (12)   │   (2)   │
├──────────┼──────────┼──────────┼──────────┤
│ ┌──────┐ │ ┌──────┐ │ ┌──────┐ │          │
│ │ Task │ │ │ Task │ │ │ Task │ │          │
│ │  1   │ │ │  2   │ │ │  4   │ │          │
│ └──────┘ │ └──────┘ │ └──────┘ │          │
│ ┌──────┐ │ ┌──────┐ │          │          │
│ │ Task │ │ │ Task │ │          │          │
│ │  3   │ │ │  5   │ │          │          │
│ └──────┘ │ └──────┘ │          │          │
└──────────┴──────────┴──────────┴──────────┘

- 카드 드래그 앤 드롭으로 상태 변경
- 그룹 기준 변경 가능 (상태, 우선순위, 담당자 등)
```

---

## 메뉴 구조

```
┌─────────────────────────────────────────┐
│  TaskFlow                               │
├─────────────────────────────────────────┤
│  📋 업무 페이지          ← 메인 화면     │
│  ✅ 완료 작업 메뉴                       │
│  📝 작업 등록 메뉴                       │
│  📊 이력관리 메뉴                        │
│  ─────────────────────                  │
│  👤 사용자 등록 메뉴                     │
│  👥 공유 사용자 등록 메뉴                │
│  ─────────────────────                  │
│  🏢 부서 관리 메뉴                       │
│  📁 그룹 관리 메뉴                       │
└─────────────────────────────────────────┘
```

---

## 주요 기능 명세

### 1. 로그인 페이지
| 항목 | 설명 |
|------|------|
| 아이디 입력 | 텍스트 필드 |
| 패스워드 입력 | 패스워드 필드 |
| 로그인 버튼 | 인증 후 업무 페이지로 이동 |
| 인증 방식 | JWT 토큰 기반 |
| 토큰 저장 | localStorage (Access Token), httpOnly Cookie (Refresh Token) |

### 2. 업무 페이지 (메인)

#### 2-1. 신규 업무 등록 창
| 구성요소 | 설명 |
|----------|------|
| 업무내용 입력 | 텍스트 필드 |
| 자동완성 | 직접 입력 시 기존 등록된 작업 내역 자동 검색 |
| 작업 선택 | 드롭다운에서 기존 등록된 작업 선택 가능 |
| 신규등록 버튼 | 업무 목록에 추가 |

```
[자동완성 동작 상세]
1. 사용자가 2글자 이상 입력 시 검색 시작
2. 기존 작업 등록 메뉴의 작업 내역에서 검색
3. 검색 결과를 드롭다운으로 표시 (최대 10개)
4. 선택 시 입력 필드에 자동 채움
5. 디바운스 적용 (300ms)
```

#### 2-2. 현재 업무 목록 창
| 컬럼 | 설명 | 인라인 편집 |
|------|------|-------------|
| 업무내용 | 작업 제목/내용 | ✅ |
| 시작시간 | 작업 시작 시간 | ✅ (DateTimePicker) |
| 완료시간 | 작업 완료 시간 | ✅ (DateTimePicker) |
| 담당자 | 작업 담당자 | ✅ (사용자 선택) |
| 카테고리 | 분류 | ✅ (단일선택) |
| 상태 | 시작전/진행중/완료/삭제 | ✅ (단일선택) |
| 우선순위 | 긴급/높음/보통/낮음 | ✅ (단일선택) |
| 그룹 | 그룹 분류 | ✅ (단일선택) |
| 리플 | 댓글 수 표시, 클릭 시 댓글 패널 | - |
| 완료 버튼 | 상태를 '완료'로 변경 | - |
| 삭제 버튼 | 상태를 '삭제'로 변경 | - |

```
[모바일/PC 동작 차이]
Mobile (< 768px):
  - 업무 행 클릭 → 전체 화면 편집 페이지로 이동
  - 뒤로가기 버튼으로 목록 복귀
  - 하단 고정 버튼: 완료, 삭제, 저장

PC (>= 768px):
  - 업무 행 클릭 → 우측 슬라이드오버 패널 오픈
  - 패널 내에서 모든 속성 편집 가능
  - ESC 또는 외부 클릭으로 패널 닫기
```

#### 2-3. 업무 상세/편집 (슬라이드오버 패널)
| 구성요소 | 설명 |
|----------|------|
| 업무 내용 | 리치 텍스트 편집 가능 |
| 속성 영역 | 모든 속성 인라인 편집 |
| 수정자 표시 | 최종 수정자 이름, 수정 시간 표시 |
| 댓글 영역 | 의견 첨부 기능 (리플) |
| 댓글 입력 | 텍스트 입력 + 등록 버튼 |
| 댓글 목록 | 작성자, 작성시간, 내용 표시 |

#### 2-4. 완료/삭제 업무 Hidden 처리
```
[Hidden 규칙 - 당일 처리 기준]
1. 완료 또는 삭제된 업무는 현재 목록에서 Hidden 처리
2. "당일" 기준: 오늘 날짜에 완료/삭제된 항목만 해당
3. 업무 페이지 맨 하단에 축소 상태로 표시
4. "완료된 업무 (N건)" 형태로 표시
5. 클릭 시 확장되어 완료/삭제 목록 표시
6. 다시 클릭 시 축소

[표시 정보]
- 작업 내용
- 작업 결과 (완료/삭제)
- 처리 시간
```

### 3. 완료 작업 메뉴
| 컬럼 | 설명 |
|------|------|
| 등록시간 | 업무 최초 등록 시간 |
| 완료시간 | 업무 완료 처리 시간 |
| 작업 내용 | 업무 제목/내용 |
| 작업자 | 완료 처리한 담당자 |

```
[필터/정렬]
- 기간 필터: 오늘, 이번주, 이번달, 사용자 지정
- 정렬: 완료시간 내림차순 (기본)
```

### 4. 작업 등록 메뉴
| 구성요소 | 설명 |
|----------|------|
| 작업 내용 | 자주 사용하는 작업 템플릿 등록 |
| 등록 버튼 | 신규 작업 템플릿 등록 |
| 변경 버튼 | 기존 작업 선택 시 등록→변경 버튼으로 전환 |
| 삭제 버튼 | 선택된 작업 템플릿 삭제 |

```
[버튼 전환 로직]
1. 초기 상태: [등록] 버튼 표시
2. 목록에서 기존 작업 선택 시: [변경] 버튼으로 전환
3. 입력 필드 클리어 또는 새 입력 시: [등록] 버튼으로 복귀
```

### 5. 이력관리 메뉴

#### 5-1. 스위치 기능
```
┌─────────────────────────────────────┐
│  [작업 처리 이력] | 작업 등록 이력   │  ← 토글 스위치
├─────────────────────────────────────┤
│  (선택된 이력 목록 표시)             │
└─────────────────────────────────────┘
```

#### 5-2. 작업 처리 이력 컬럼
| 컬럼 | 타입 | 설명 |
|------|------|------|
| 작업 내용 | TEXT | 업무 제목 |
| 작업 결과 | SELECT | 완료/삭제 |
| 작업자 | USER | 처리한 담당자 |
| 등록시간 | DATETIME | 최초 등록 시간 |
| 시작시간 | DATETIME | 작업 시작 시간 |
| 완료시간 | DATETIME | 작업 완료 시간 |
| 수정시간 | DATETIME | 마지막 수정 시간 |
| 삭제시간 | DATETIME | 삭제 처리 시간 (삭제된 경우) |

#### 5-3. 작업 등록 이력 컬럼
| 컬럼 | 타입 | 설명 |
|------|------|------|
| 작업 내용 | TEXT | 등록된 작업 템플릿 |
| 등록자 | USER | 템플릿 등록한 사용자 |
| 등록시간 | DATETIME | 템플릿 등록 시간 |
| 수정시간 | DATETIME | 마지막 수정 시간 |
| 상태 | SELECT | 활성/비활성 |

### 6. 사용자 등록 메뉴
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| 사용자 이름 | TEXT | ✅ | 표시명 |
| 아이디 | TEXT | ✅ | 로그인 ID (영문+숫자, 4~20자) |
| 패스워드 | PASSWORD | ✅ | 최소 8자, 영문+숫자+특수문자 |
| 패스워드 확인 | PASSWORD | ✅ | 패스워드 재입력 |

```
[비밀번호 정책]
- 최소 8자 이상
- 영문 대/소문자 포함
- 숫자 포함
- 특수문자 포함 (!@#$%^&*)
- BCrypt 암호화 저장
```

### 7. 공유 사용자 등록 메뉴
| 필드 | 설명 |
|------|------|
| 사용자 아이디 | 공유할 사용자 ID 검색/선택 |
| 사용자 이름 | 자동 표시 |
| 등록 버튼 | 공유 사용자로 추가 |
| 삭제 버튼 | 공유 목록에서 제거 |

```
[공유 범위]
- 공유된 사용자는 해당 보드의 업무를 조회/수정 가능
- 보드 소유자만 공유 사용자 관리 가능
```

### 8. 부서 관리 메뉴
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| 부서 코드 | TEXT | ✅ | 고유 부서 코드 (예: DEV, HR, MKT) |
| 부서명 | TEXT | ✅ | 부서 이름 |
| 상위 부서 | SELECT | - | 상위 부서 선택 (계층 구조) |
| 정렬 순서 | NUMBER | - | 표시 순서 |
| 사용 여부 | CHECKBOX | ✅ | 활성/비활성 |

```
[부서 계층 구조]
- 상위 부서를 선택하여 트리 구조 구성 가능
- 최상위 부서는 상위 부서가 NULL

[부서 트리 예시]
├── 경영지원본부
│   ├── 인사팀
│   ├── 총무팀
│   └── 재무팀
├── 개발본부
│   ├── 개발1팀
│   ├── 개발2팀
│   └── QA팀
└── 영업본부
    ├── 국내영업팀
    └── 해외영업팀

[기능]
- 부서 CRUD
- 드래그 앤 드롭으로 순서 변경
- 부서 비활성화 시 하위 부서도 함께 비활성화 경고
- 사용자 등록 시 부서 선택 가능
```

### 9. 그룹 관리 메뉴
| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| 그룹 코드 | TEXT | ✅ | 고유 그룹 코드 |
| 그룹명 | TEXT | ✅ | 그룹 이름 |
| 그룹 설명 | TEXT | - | 그룹 용도 설명 |
| 그룹 색상 | COLOR | - | 표시용 색상 |
| 정렬 순서 | NUMBER | - | 표시 순서 |
| 사용 여부 | CHECKBOX | ✅ | 활성/비활성 |

```
[그룹 vs 부서 차이]
- 부서: 조직도 기반의 고정 소속 (1인 1부서)
- 그룹: 프로젝트/업무 기반의 유연한 팀 (1인 다중 그룹 가능)

[그룹 활용 예시]
- 프로젝트 그룹: "신규 서비스 개발", "리뉴얼 프로젝트"
- 업무 그룹: "주간 보고", "월간 회의"
- TF 그룹: "보안 점검 TF", "비용 절감 TF"

[기능]
- 그룹 CRUD
- 그룹 멤버 관리 (사용자 추가/제거)
- 업무 아이템에 그룹 할당
- 그룹별 업무 필터링
```

### 부서/그룹과 사용자 연결
```
[사용자-부서 관계]
- 1:1 관계 (사용자는 하나의 부서에만 소속)
- TB_USER.DEPARTMENT_ID로 연결

[사용자-그룹 관계]
- N:M 관계 (사용자는 여러 그룹에 소속 가능)
- TB_USER_GROUP 매핑 테이블로 연결

[업무-그룹 관계]
- N:1 관계 (업무는 하나의 그룹에 할당)
- TB_ITEM.GROUP_ID로 연결
- 업무 속성의 '그룹' 필드와 연동
```

---

## 파일 네이밍 컨벤션

### 백엔드 (Java)
```
Controller: ItemController.java
Service: ItemService.java, ItemServiceImpl.java
Mapper: ItemMapper.java
DTO: ItemCreateRequest.java, ItemResponse.java
Domain: Item.java
```

### 프론트엔드 (Vue)
```
컴포넌트: ItemList.vue, ItemCard.vue
페이지: ItemsView.vue, LoginView.vue
스토어: item.ts, property.ts
API: item.api.ts, property.api.ts
타입: item.types.ts
```

### SQL/MyBatis
```
Mapper XML: ItemMapper.xml
테이블: TB_ITEM, TB_USER, TB_PROPERTY_DEF
```

---

## Git 컨벤션

### 브랜치 전략
```
main        # 프로덕션
develop     # 개발 통합
feature/*   # 기능 개발
bugfix/*    # 버그 수정
hotfix/*    # 긴급 수정
```

### 커밋 메시지
```
feat: 새로운 기능 추가
fix: 버그 수정
docs: 문서 수정
style: 코드 포맷팅
refactor: 코드 리팩토링
test: 테스트 코드
chore: 빌드, 설정 변경
```

---

## 개발 환경 설정

### Docker Compose
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: taskflow
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/taskflow

  frontend:
    build: ./frontend
    ports:
      - "3000:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

### VS Code 확장
- Vue - Official
- ESLint
- Prettier
- Java Extension Pack
- Spring Boot Extension Pack

---

## 참고 사항

### 금지 사항
- ❌ JPA 사용 금지
- ❌ 날짜를 문자열로 저장 금지
- ❌ 설계 승인 없이 코드 작성 금지
- ❌ Unicode 불릿 사용 금지 (CSS/HTML 리스트 사용)

### 권장 사항
- ✅ MyBatis XML Mapper 사용
- ✅ DATE/DATETIME 타입 사용
- ✅ 설계 문서 선행 작성
- ✅ Optimistic Update 패턴
- ✅ 컴포넌트 재사용성 고려

---

## 보안

### 인증 방식
| 항목 | 설명 |
|------|------|
| 인증 방식 | JWT (JSON Web Token) |
| Access Token | 유효기간 30분, localStorage 저장 |
| Refresh Token | 유효기간 7일, httpOnly Cookie 저장 |
| 토큰 갱신 | Access Token 만료 시 자동 갱신 |

### 비밀번호 정책
```
- 최소 8자 이상
- 영문 대문자 1개 이상
- 영문 소문자 1개 이상
- 숫자 1개 이상
- 특수문자 1개 이상 (!@#$%^&*)
- BCrypt 암호화 (strength: 10)
```

### API 보안
| 항목 | 설명 |
|------|------|
| CORS | 허용된 Origin만 접근 |
| CSRF | SameSite Cookie + CSRF Token |
| Rate Limiting | IP당 100 req/min |
| Input Validation | 서버 측 필수 검증 |

### 권한 관리
| 역할 | 권한 |
|------|------|
| OWNER | 보드 삭제, 공유 사용자 관리 |
| MEMBER | 아이템 CRUD, 속성 편집 |
| VIEWER | 조회만 가능 (향후 확장) |

---

## 실시간 동기화

### 동기화 방식
| 방식 | 설명 | 선택 |
|------|------|------|
| Polling | 주기적 API 호출 | - |
| SSE | Server-Sent Events | ✅ 권장 |
| WebSocket | 양방향 통신 | 향후 확장 |

### SSE 구현 상세
```
[연결 흐름]
1. 로그인 후 SSE 연결 수립
2. 서버에서 이벤트 발생 시 클라이언트로 Push
3. 연결 끊김 시 자동 재연결 (3초 후)

[이벤트 타입]
- item:created   - 새 아이템 생성
- item:updated   - 아이템 수정
- item:deleted   - 아이템 삭제
- property:updated - 속성 정의 변경
- comment:created  - 새 댓글

[클라이언트 처리]
- 이벤트 수신 시 Store 즉시 갱신
- 현재 편집 중인 아이템은 충돌 처리
```

### 충돌 처리
```
[동시 편집 충돌 시]
1. 서버 버전과 로컬 버전 비교
2. 충돌 감지 시 사용자에게 알림
3. 선택지 제공:
   - 내 변경사항 유지
   - 서버 버전으로 덮어쓰기
   - 병합 (수동)
```

---

## 에러 처리

### 프론트엔드 에러 처리
```typescript
// composables/useErrorHandler.ts
export function useErrorHandler() {
  const handleError = (error: Error, context?: string) => {
    // 1. 콘솔 로깅
    console.error(`[${context}]`, error)
    
    // 2. 사용자 알림
    if (error instanceof ApiError) {
      toast.error(error.message)
    } else if (error instanceof NetworkError) {
      toast.error('네트워크 연결을 확인해주세요')
    } else {
      toast.error('오류가 발생했습니다')
    }
    
    // 3. 에러 리포팅 (선택)
    // reportError(error)
  }
  
  return { handleError }
}
```

### 에러 바운더리 (Vue)
```vue
<!-- components/ErrorBoundary.vue -->
<template>
  <slot v-if="!error" />
  <div v-else class="error-fallback">
    <p>문제가 발생했습니다</p>
    <button @click="retry">다시 시도</button>
  </div>
</template>
```

### 백엔드 글로벌 예외 처리
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse> handleBusiness(BusinessException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError()
            .body(ApiResponse.error("서버 오류가 발생했습니다"));
    }
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

---

## 로깅 전략

### 백엔드 로깅
| 레벨 | 용도 |
|------|------|
| ERROR | 예외, 시스템 오류 |
| WARN | 잠재적 문제, 비정상 동작 |
| INFO | 주요 비즈니스 이벤트 |
| DEBUG | 개발/디버깅용 상세 정보 |

### 로그 포맷
```
[%d{yyyy-MM-dd HH:mm:ss}] [%level] [%thread] %logger{36} - %msg%n
```

### 필수 로깅 항목
- API 요청/응답 (INFO)
- 인증 시도 (INFO)
- 인증 실패 (WARN)
- 데이터 변경 (INFO)
- 예외 발생 (ERROR)

---

## 디버깅 및 수정 원칙 (⚠️ 필수 준수)

### 기능 보존 원칙
```
⚠️ 절대 금지 사항
1. 기능 삭제로 오류 해결 금지
2. 기능 축소로 오류 해결 금지
3. 역할/책임 감소로 오류 해결 금지

✅ 필수 준수 사항
1. 오류는 근본 원인을 찾아 정상 수정할 것
2. 기능 축소/삭제가 불가피한 경우 → 반드시 승인 요청
3. 수정 전 영향 범위 분석 필수
```

### DB 스키마 일관성 원칙
```
📁 관련 파일
- docker/mysql/init/01_schema.sql  (테이블 생성)
- docker/mysql/init/02_init_data.sql  (초기 데이터)
- backend/src/main/resources/mapper/*.xml  (MyBatis Mapper)

✅ DB 수정 시 필수 체크리스트
1. 현재 운영 DB 스키마와 초기 구축 스크립트 일치 여부 확인
2. 컬럼 추가/변경 시 → 01_schema.sql 동기화
3. 코드/옵션 추가 시 → 02_init_data.sql 동기화
4. 테이블 구조 변경 시 → 관련 Mapper XML 동시 수정
5. DTO/Domain 클래스 필드 동기화

🔄 수정 순서
1. ERD/테이블 정의서 확인
2. 01_schema.sql 수정
3. 02_init_data.sql 수정 (필요시)
4. Mapper XML 수정
5. Domain/DTO 클래스 수정
6. 전체 정합성 검증
```

### 수정 시 제출 형식
```
📋 변경 보고서
- 수정 대상: [파일 목록]
- 변경 내용: [상세 내용]
- DB 영향: [스키마/데이터 변경 여부]
- 연관 파일: [Mapper, DTO, Service 등]
- 테스트 방법: [검증 절차]
```

---

## 문서 버전

```
| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2024-12-15 | - | 최초 작성 |
| 1.1 | 2024-12-15 | - | 메뉴별 상세 기능 명세 추가, 모바일/PC 동작 차이 상세화, 이력관리 컬럼 명시, 속성 관리 UI 명세 추가, 코드 항목 관리 명세, 보안/실시간동기화/에러처리 섹션 추가, API 명세 상세화 |
| 1.2 | 2024-12-15 | - | 부서 관리 메뉴 추가, 그룹 관리 메뉴 추가, 부서/그룹 API 추가, 사용자-부서-그룹 관계 정의 |
```
---