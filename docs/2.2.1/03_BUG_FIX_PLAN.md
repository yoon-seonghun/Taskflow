# v2.2.1 버그 수정 계획서

## 개요
error_note.txt에 보고된 6가지 오류에 대한 분석 및 수정 계획

**업데이트:** 2026-01-16 - 다크모드/알림페이지 영향도 분석 추가

---

## 오류 목록 및 분석

### 1. 부서 등록 시 상위 부서 지정 불가

**증상:**
- 부서 등록 시 상위 부서를 선택해도 최상위 부서로 등록됨
- 수정 시에도 상위 부서 변경 안됨

**원인:**
- 프론트엔드: `DepartmentForm.vue`에서 `parentId` (number) 전송
- 백엔드 DTO: `DepartmentUpdateRequest.java`에서 `parentCode` (String) 기대
- 필드명 불일치로 상위 부서가 null로 처리됨

**수정 파일:**
| 파일 | 변경 내용 |
|------|----------|
| frontend/src/components/settings/DepartmentForm.vue | `parentId` → `parentCode` 변경, 타입 number → string |

---

### 2. 사용자 비활성화 시 400 에러

**증상:**
- 사용자 목록에서 비활성화 토글 시 400 에러
- `PUT /api/users/100` with `{"use_yn":"N"}`

**원인:**
- `UserUpdateRequest.java`의 `name` 필드에 `@NotBlank` 검증
- `UsersContent.vue`에서 `useYn`만 전송하여 필수 필드 누락

**수정 방안:**
- 상태 토글 전용 API 사용 또는 현재 사용자 정보 포함 전송

**수정 파일:**
| 파일 | 변경 내용 |
|------|----------|
| frontend/src/components/settings/UsersContent.vue | handleToggleStatus에서 name 필드 포함 |

---

### 3. 환경설정 다크모드 미적용 ⭐ 상세 분석 완료

**증상:**
- 설정에서 다크모드 선택해도 화면 변화 없음

**원인:**
- `SettingsView.vue`에서 `document.documentElement.classList.add('dark')` 실행됨
- 그러나 CSS에 `dark:` 선택자 스타일이 정의되지 않음
- Tailwind `darkMode` 설정 미구성

**영향도 분석 결과:**

| 항목 | 현황 |
|------|------|
| Vue 컴포넌트 수 | 112개 파일 |
| `bg-white` 사용 | 366건 |
| `dark:` 클래스 사용 | 0건 |
| Tailwind darkMode 설정 | 없음 |

**위험성 평가:** 🟠 MEDIUM-HIGH

| 영역 | 위험도 | 설명 |
|------|--------|------|
| 레이아웃 (Header/Sidebar) | 🔴 HIGH | 모든 페이지에 노출 |
| 입력/선택 필드 | 🔴 HIGH | 시인성 문제 |
| 동적 색상 (옵션/카테고리) | 🟠 MEDIUM | CSS 변수 전환 필요 |
| 써드파티 (TipTap 에디터) | 🟡 LOW | 별도 처리 가능 |

**구현 방식:** Tailwind `dark:` 클래스 + CSS 변수 Hybrid

**수정 범위:**

| Tier | 파일 수 | 우선순위 | 내용 |
|------|---------|---------|------|
| TIER 1 (필수) | 11개 | 🔴 필수 | 레이아웃, 공통 UI, main.css |
| TIER 2 (권장) | 8개 | 🟡 권장 | 업무 관련 컴포넌트 |
| TIER 3 (선택) | 30+개 | 🟢 선택 | 설정/기타 컴포넌트 |

**예상 공수:**

| 단계 | 시간 |
|------|------|
| TIER 1 (main.css + 핵심 컴포넌트) | 8-10시간 |
| TIER 2 (업무 컴포넌트) | 4-6시간 |
| TIER 3 (나머지) | 8-12시간 |
| 테스트/디버깅 | 4-6시간 |
| **총계** | **24-34시간 (3-4일)** |

**수정 파일 (TIER 1):**
| 파일 | 변경 내용 |
|------|----------|
| tailwind.config.js | `darkMode: 'class'` 설정 추가 |
| src/assets/main.css | 모든 @layer에 dark: variant 추가 (200줄+) |
| src/components/layout/Header.vue | 헤더 배경/텍스트 |
| src/components/layout/Sidebar.vue | 사이드바 배경/텍스트 |
| src/components/layout/MainLayout.vue | 레이아웃 구조 |
| src/components/common/Modal.vue | 모달 스타일 |
| src/components/common/Input.vue | 입력 필드 |
| src/components/common/Select.vue | 선택 필드 |
| src/components/common/Button.vue | 버튼 variant |
| src/components/ui/SlideOverPanel.vue | 슬라이드 패널 |
| src/components/common/DatePicker.vue | 날짜 선택기 |

---

### 4. 알림 전체보기 404 에러 ⭐ 상세 분석 완료

**증상:**
- 알림 드롭다운에서 "전체 알림 보기" 클릭 시 404

**원인:**
- `NotificationDropdown.vue`에서 `/notifications` 경로로 이동
- 라우터에 해당 경로 미정의
- `NotificationsView.vue` 파일 부재

**현재 알림 시스템 현황:**

| 구성 | 상태 | 비고 |
|------|------|------|
| NotificationDropdown.vue | ✅ 완성 | 최근 10개만 표시 |
| notification.ts (Store) | ✅ 완성 | 페이징 지원 |
| notification API | ✅ 완성 | 4개 엔드포인트 |
| NotificationsView.vue | ❌ 미존재 | 신규 생성 필요 |
| 라우터 설정 | ❌ 미정의 | 추가 필요 |

**백엔드 API 지원 현황:**

| API | 지원 | 비고 |
|-----|------|------|
| GET /api/notifications | ✅ | 페이징, unreadOnly 필터 |
| PUT /api/notifications/{id}/read | ✅ | 읽음 처리 |
| PUT /api/notifications/read-all | ✅ | 전체 읽음 |
| DELETE /api/notifications | ❌ | 삭제 API 없음 |

**구현 필요성 평가:**

| 항목 | 점수 | 근거 |
|------|------|------|
| 사용성 | 9/10 | 드롭다운 10개 제한으로 과거 알림 확인 불가 |
| UX 완성도 | 8/10 | 알림 센터는 웹앱 필수 기능 |
| 기술 난이도 | 낮음 | 백엔드 API 기본 지원 |

**결론:** ✅ **구현 추천**

**구현 범위별 공수:**

| Phase | 범위 | 공수 | 우선순위 |
|-------|------|------|---------|
| Phase 1 | 기본 목록 + 페이징 + 읽음 필터 | 3-4시간 | 🔴 필수 |
| Phase 2 | + 삭제 기능 (백엔드 API 추가) | 2-3시간 | 🟡 권장 |
| Phase 3 | + 유형별 필터 + 검색 | 5-6시간 | 🟢 선택 |

**Phase 1 (최소 구현) 수정 파일:**
| 파일 | 변경 내용 |
|------|----------|
| frontend/src/views/NotificationsView.vue | 신규 생성 (알림 목록/페이징/필터) |
| frontend/src/router/index.ts | `/notifications` 라우트 추가 |

---

### 5. 좌측 공유사용자 메뉴 비활성화 요청

**요청:**
- 보드관리에서 공유 사용자 관리 기능이 있어 좌측 메뉴와 중복
- 좌측 "공유 사용자" 메뉴 비활성화

**수정 파일:**
| 파일 | 변경 내용 |
|------|----------|
| frontend/src/components/layout/Sidebar.vue | `Shares` 메뉴 항목 제거 또는 숨김 |

---

### 6. 업무상세 내용 입력창 스크롤 없음

**증상:**
- 업무 상세 패널에서 내용이 많아도 스크롤이 없음
- 화면에서 잘림 (cap_1.jpg 참조)

**원인:**
- `ItemDetailPanel.vue`에서 `RichTextEditor`에 `max-height="none"` 전달
- 부모 컨테이너 높이 제약 없이 에디터가 무한 확장
- 내용이 화면을 넘어가면 스크롤 없이 잘림

**수정 파일:**
| 파일 | 변경 내용 |
|------|----------|
| frontend/src/components/item/ItemDetailPanel.vue | PC 레이아웃 에디터 영역 높이 제한 및 스크롤 추가 |
| frontend/src/components/editor/RichTextEditor.vue | 에디터 콘텐츠 영역 스크롤 스타일 개선 |

---

## 수정 계획

### 수정 순서 (우선순위)

| 순서 | 오류 | 영향도 | 난이도 |
|------|------|--------|--------|
| 1 | 부서 상위 지정 불가 | 높음 | 낮음 |
| 2 | 사용자 비활성화 에러 | 높음 | 낮음 |
| 3 | 업무상세 스크롤 없음 | 중간 | 낮음 |
| 4 | 알림 전체보기 404 | 중간 | 중간 |
| 5 | 공유사용자 메뉴 제거 | 낮음 | 낮음 |
| 6 | 다크모드 비활성화 | 낮음 | 낮음 |

---

## 상세 수정 내용

### 1. 부서 상위 지정 수정

**DepartmentForm.vue:**
```typescript
// 변경 전
const parentId = ref<number | null>(null)

// 변경 후
const parentCode = ref<string | null>(null)

// 데이터 전송 시
const data = {
  ...
  parentCode: parentCode.value ?? undefined,  // parentId → parentCode
}
```

### 2. 사용자 비활성화 수정

**UsersContent.vue:**
```typescript
// 변경 전
await userApi.updateUser(user.userId, { useYn: newStatus })

// 변경 후
await userApi.updateUser(user.userId, {
  name: user.name,  // 필수 필드 포함
  useYn: newStatus
})
```

### 3. 업무상세 스크롤 수정

**ItemDetailPanel.vue PC 레이아웃:**
```vue
<!-- 변경 전 -->
<div class="flex-1 min-h-0">
  <RichTextEditor ... max-height="none" />
</div>

<!-- 변경 후 -->
<div class="flex-1 min-h-0 overflow-hidden">
  <RichTextEditor ... max-height="100%" />
</div>
```

**RichTextEditor.vue:**
```css
.editor-content-wrapper .ProseMirror {
  min-height: inherit;
  max-height: inherit;
  overflow-y: auto;  /* 스크롤 활성화 */
}
```

### 4. 알림 전체보기 페이지 생성

**router/index.ts:**
```typescript
{
  path: 'notifications',
  name: 'Notifications',
  component: () => import('@/views/NotificationView.vue')
}
```

**NotificationView.vue:** 알림 목록 전체 표시 페이지 신규 생성

### 5. 공유사용자 메뉴 제거

**Sidebar.vue:**
```typescript
// 메뉴 항목에서 제거
// { name: 'Shares', label: '공유 사용자', icon: 'users' },  // 주석 처리
```

### 6. 다크모드 비활성화

**SettingsView.vue:**
```vue
<!-- 테마 설정 옵션 숨김 -->
<div class="hidden">  <!-- 또는 완전 제거 -->
  <!-- 테마 설정 UI -->
</div>
```

---

## 영향 범위

| 수정 항목 | 영향 파일 | 테스트 항목 |
|----------|----------|------------|
| 부서 상위 지정 | DepartmentForm.vue | 부서 등록/수정 시 상위 부서 지정 |
| 사용자 비활성화 | UsersContent.vue | 사용자 활성/비활성 토글 |
| 스크롤 | ItemDetailPanel.vue, RichTextEditor.vue | 긴 내용 입력 시 스크롤 동작 |
| 알림 페이지 | router/index.ts, NotificationView.vue | 전체 알림 보기 링크 동작 |
| 메뉴 제거 | Sidebar.vue | 사이드바 메뉴 구조 |
| 다크모드 | SettingsView.vue | 설정 페이지 UI |

---

## 승인 요청

위 계획서의 수정 방안에 대해 승인을 요청드립니다.

### 확정 사항 (사용자 결정)

| 항목 | 결정 | 비고 |
|------|------|------|
| 다크모드 | ✅ 구현 진행 | TIER별 단계적 진행 |
| 공유사용자 메뉴 | ✅ 숨김 처리 | 완전 제거 X |
| 알림 페이지 | ✅ 구현 진행 | Phase 1 우선 |

### 총 수정 범위 요약

| 오류 | 공수 | 우선순위 |
|------|------|---------|
| 1. 부서 상위 지정 불가 | 0.5시간 | 🔴 |
| 2. 사용자 비활성화 에러 | 0.5시간 | 🔴 |
| 3. 다크모드 (TIER 1) | 8-10시간 | 🟡 |
| 4. 알림 페이지 (Phase 1) | 3-4시간 | 🟡 |
| 5. 공유사용자 메뉴 숨김 | 0.5시간 | 🟢 |
| 6. 업무상세 스크롤 | 1시간 | 🔴 |
| **합계** | **14-17시간** | - |

### 진행 순서 제안

```
1단계 (버그 수정 - 즉시)
├── 부서 상위 지정 수정
├── 사용자 비활성화 수정
├── 업무상세 스크롤 수정
└── 공유사용자 메뉴 숨김

2단계 (신규 기능 - 1일차)
└── 알림 페이지 Phase 1 구현

3단계 (다크모드 - 2-3일차)
├── tailwind.config.js + main.css 수정
├── TIER 1 컴포넌트 수정 (11개)
└── 테스트

4단계 (다크모드 확장 - 선택)
├── TIER 2 컴포넌트 수정
└── TIER 3 컴포넌트 수정
```

---

**작성일:** 2026-01-16
**업데이트:** 2026-01-16 (영향도 분석 추가)
**버전:** v2.2.1
