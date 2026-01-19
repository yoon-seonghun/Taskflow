# 버그 신고 및 개선 요청 시스템 설계

> **상태**: 기획 단계 (보류)
> **작성일**: 2025-01-19
> **목적**: 다른 프로젝트에도 적용 가능한 독립형 피드백 모듈

## 개요
사용자가 버그를 신고하고 개선 요청을 제출할 수 있는 **독립형 피드백 모듈**.
- 다른 프로젝트에 쉽게 적용 가능한 플러그인 구조
- 설정 기반 커스터마이징
- Claude Code에서 바로 분석 가능한 형태로 정보 수집

---

## 독립형 모듈 아키텍처

### 설계 원칙
1. **Zero Dependency**: 호스트 앱의 비즈니스 로직에 의존하지 않음
2. **Configuration-based**: 모든 동작을 설정으로 제어
3. **Event-driven**: 호스트 앱과 이벤트로 통신
4. **Pluggable Storage**: DB 어댑터 패턴으로 저장소 교체 가능

### 모듈 구조
```
feedback-module/
├── frontend/                    # Vue 3 플러그인
│   ├── plugin.ts               # Vue 플러그인 진입점
│   ├── components/             # UI 컴포넌트
│   ├── composables/            # 재사용 로직
│   ├── types/                  # TypeScript 타입
│   └── styles/                 # 스타일 (Tailwind 호환)
│
├── backend/                     # Spring Boot 스타터
│   ├── autoconfigure/          # 자동 설정
│   ├── controller/             # REST API
│   ├── service/                # 비즈니스 로직
│   ├── storage/                # 저장소 어댑터
│   └── notification/           # 알림 어댑터
│
└── shared/                      # 공유 스키마/타입
    ├── schema.sql              # DB 스키마
    └── types.ts                # 공통 타입 정의
```

### 프론트엔드 플러그인 사용법
```typescript
// main.ts (호스트 앱)
import { createApp } from 'vue'
import { FeedbackPlugin } from '@taskflow/feedback-module'

const app = createApp(App)

app.use(FeedbackPlugin, {
  // API 설정
  apiBaseUrl: '/api/feedback',

  // 인증 토큰 제공자
  getAuthToken: () => localStorage.getItem('accessToken'),

  // 사용자 정보 제공자
  getCurrentUser: () => ({
    username: authStore.user?.username,
    name: authStore.user?.name,
    email: authStore.user?.email
  }),

  // 자동 에러 수집
  autoCapture: {
    enabled: import.meta.env.PROD,
    excludePatterns: [/401/, /403/, /network/i]
  },

  // UI 설정
  ui: {
    position: 'bottom-right',
    primaryColor: '#3B82F6',
    zIndex: 9999
  },

  // 커스텀 카테고리
  categories: ['UI', 'API', '성능', '보안', '기타'],

  // 이벤트 훅
  onSubmit: (feedback) => console.log('Submitted:', feedback),
  onError: (error) => console.error('Feedback error:', error)
})
```

### 백엔드 스타터 사용법
```yaml
# application.yml (호스트 앱)
feedback:
  enabled: true
  api-prefix: /api/feedback

  # 저장소 설정
  storage:
    type: mysql  # mysql | postgresql | mongodb
    table-prefix: TB_FEEDBACK_

  # 알림 설정
  notification:
    email:
      enabled: true
      admin-emails:
        - admin@example.com
    webhook:
      enabled: false
      url: https://hooks.slack.com/...

  # 파일 업로드
  upload:
    enabled: true
    max-size: 10MB
    storage-path: /uploads/feedback

  # 자동 리포트 생성
  report:
    format: json  # json | markdown | both
    include-stack-trace: true
```

```java
// 호스트 앱에서 빈 주입
@Configuration
public class FeedbackConfig {

    @Bean
    public FeedbackUserProvider feedbackUserProvider(SecurityUtils securityUtils) {
        return () -> {
            String username = securityUtils.getCurrentUsername();
            User user = userService.findByUsername(username);
            return new FeedbackUser(username, user.getName(), user.getEmail());
        };
    }
}
```

---

## 1. DB 테이블 설계

### TB_FEEDBACK (메인 테이블)
```sql
CREATE TABLE TB_FEEDBACK (
    FEEDBACK_ID BIGINT NOT NULL AUTO_INCREMENT,
    FEEDBACK_TYPE VARCHAR(20) NOT NULL COMMENT 'BUG_REPORT | FEATURE_REQUEST',
    TITLE VARCHAR(200) NOT NULL,
    DESCRIPTION TEXT NOT NULL,

    -- 버그 신고 전용
    ERROR_CODE VARCHAR(50) NULL,
    ERROR_MESSAGE TEXT NULL,
    ERROR_STACK TEXT NULL,
    ERROR_CONTEXT JSON NULL COMMENT '에러 발생 컨텍스트',

    -- 환경 정보 (자동 수집)
    USER_AGENT VARCHAR(500) NULL,
    PAGE_URL VARCHAR(1000) NULL,
    SCREEN_SIZE VARCHAR(20) NULL,
    VIEWPORT_SIZE VARCHAR(20) NULL,
    BROWSER_INFO VARCHAR(100) NULL,
    OS_INFO VARCHAR(100) NULL,
    DEVICE_TYPE VARCHAR(20) NULL COMMENT 'DESKTOP|MOBILE|TABLET',

    -- 상태 관리
    STATUS VARCHAR(20) DEFAULT 'OPEN' COMMENT 'OPEN|IN_PROGRESS|RESOLVED|CLOSED',
    PRIORITY VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'LOW|NORMAL|HIGH|CRITICAL',
    SEVERITY VARCHAR(20) NULL COMMENT 'MINOR|MAJOR|CRITICAL|BLOCKER',
    CATEGORY VARCHAR(50) NULL COMMENT 'UI|API|PERFORMANCE|SECURITY|OTHER',

    -- 자동 전달
    AUTO_SUBMITTED CHAR(1) DEFAULT 'N',
    AUTO_SUBMIT_TRIGGER VARCHAR(50) NULL COMMENT 'ERROR_BOUNDARY|UNHANDLED_ERROR|API_ERROR',

    -- Claude Code 리포트
    CLAUDE_REPORT MEDIUMTEXT NULL COMMENT 'JSON/Markdown 형식',

    -- 처리 정보
    ASSIGNEE_USERNAME VARCHAR(50) NULL,
    RESOLVED_AT DATETIME NULL,
    RESOLUTION_NOTE TEXT NULL,

    -- 공통 필드
    CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR(50) NOT NULL,
    UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,
    UPDATED_BY VARCHAR(50) NULL,
    DELETED_AT DATETIME NULL,
    PRIMARY KEY (FEEDBACK_ID)
);
```

### TB_FEEDBACK_ATTACHMENT (첨부 파일)
```sql
CREATE TABLE TB_FEEDBACK_ATTACHMENT (
    ATTACHMENT_ID BIGINT NOT NULL AUTO_INCREMENT,
    FEEDBACK_ID BIGINT NOT NULL,
    FILE_ID BIGINT NOT NULL COMMENT 'FK -> TB_FILE',
    ATTACHMENT_TYPE VARCHAR(30) NOT NULL COMMENT 'SCREENSHOT|ANNOTATED|ATTACHMENT',
    ANNOTATION_DATA JSON NULL COMMENT '그리기 좌표 데이터',
    DISPLAY_ORDER INT DEFAULT 0,
    CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ATTACHMENT_ID)
);
```

### TB_FEEDBACK_ERROR_LOG (에러 로그)
```sql
CREATE TABLE TB_FEEDBACK_ERROR_LOG (
    LOG_ID BIGINT NOT NULL AUTO_INCREMENT,
    FEEDBACK_ID BIGINT NOT NULL,
    ERROR_CODE VARCHAR(50) NOT NULL,
    ERROR_MESSAGE TEXT NOT NULL,
    ERROR_STACK TEXT NULL,
    HTTP_STATUS INT NULL,
    API_URL VARCHAR(500) NULL,
    OCCURRED_AT DATETIME NOT NULL,
    CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (LOG_ID)
);
```

---

## 2. Claude Code 분석용 리포트 구조

### JSON 형식
```json
{
  "meta": {
    "reportVersion": "1.0",
    "feedbackId": 123,
    "feedbackType": "BUG_REPORT",
    "generatedAt": "2025-01-19T10:30:00Z"
  },
  "summary": {
    "title": "업무 저장 시 500 에러 발생",
    "priority": "HIGH",
    "severity": "MAJOR",
    "category": "API"
  },
  "error": {
    "code": "API_ERROR",
    "message": "Failed to save item",
    "stack": "at ItemService.save...",
    "httpStatus": 500,
    "apiEndpoint": "POST /api/boards/1/items"
  },
  "environment": {
    "browser": { "name": "Chrome", "version": "120" },
    "os": { "name": "Windows", "version": "11" },
    "device": { "type": "DESKTOP", "screenSize": "1920x1080" },
    "pageUrl": "/boards/1/items"
  },
  "errorLogs": [...],
  "attachments": [...]
}
```

---

## 3. 화면 캡처 방안

### 기술: html2canvas 라이브러리
- 현재 화면을 Canvas로 렌더링 후 PNG 변환
- 피드백 모달 제외 옵션 적용
- 크로스 브라우저 지원

```typescript
// composables/useScreenCapture.ts
async function captureScreen(): Promise<Blob | null> {
  const canvas = await html2canvas(document.body, {
    ignoreElements: (el) => el.classList.contains('feedback-modal'),
    useCORS: true
  });
  return new Promise(resolve => canvas.toBlob(resolve, 'image/png'));
}
```

---

## 4. 그리기(어노테이션) 기능

### 지원 도구
| 도구 | 설명 |
|-----|------|
| 화살표 | 지적할 위치 표시 |
| 사각형 | 영역 강조 |
| 원 | 특정 지점 강조 |
| 텍스트 | 설명 추가 |
| 하이라이트 | 반투명 강조 |
| 자유곡선 | 펜 도구 |

### 데이터 저장
```json
{
  "annotations": [
    { "type": "arrow", "start": [100, 100], "end": [200, 150], "color": "#ff0000" },
    { "type": "text", "position": [150, 120], "text": "여기가 문제입니다", "color": "#ff0000" }
  ]
}
```

---

## 5. 자동 에러 전달

### ErrorBoundary 연동
- 미처리 에러 발생 시 자동 버그 리포트 생성
- 화면 캡처 포함 (옵션)
- 최근 에러 로그 10개 첨부

### 제외 패턴
- 네트워크 에러 (일시적)
- 401/403 인증 에러
- 타임아웃 에러

---

## 6. API 설계

```
# 사용자용
POST   /api/feedback                  # 피드백 등록
POST   /api/feedback/auto             # 자동 버그 리포트
POST   /api/feedback/{id}/attachments # 첨부 파일 추가

# 관리자용
GET    /api/feedback                  # 목록 조회
GET    /api/feedback/{id}             # 상세 조회
PUT    /api/feedback/{id}/status      # 상태 변경
PUT    /api/feedback/{id}/assign      # 담당자 배정
GET    /api/feedback/{id}/report      # Claude Code 리포트 다운로드
```

---

## 7. 독립형 프론트엔드 모듈 구조

### 모듈 내부 구조
```
src/feedback-module/
├── index.ts                    # 모듈 진입점 (export all)
├── plugin.ts                   # Vue 플러그인 정의
├── types/
│   ├── feedback.ts             # 피드백 타입
│   ├── config.ts               # 설정 타입
│   └── annotation.ts           # 어노테이션 타입
├── composables/
│   ├── useFeedback.ts          # 피드백 제출 로직
│   ├── useScreenCapture.ts     # 화면 캡처
│   ├── useAnnotation.ts        # 그리기 도구
│   ├── useAutoCapture.ts       # 자동 에러 수집
│   └── useFeedbackConfig.ts    # 설정 접근
├── components/
│   ├── FeedbackButton.vue      # 플로팅 버튼
│   ├── FeedbackModal.vue       # 메인 모달
│   ├── BugReportForm.vue       # 버그 신고 폼
│   ├── FeatureRequestForm.vue  # 개선 요청 폼
│   ├── ScreenshotAnnotator.vue # 캡처 + 그리기
│   ├── AnnotationCanvas.vue    # 캔버스 그리기
│   └── AnnotationToolbar.vue   # 도구 모음
├── api/
│   └── feedback.ts             # API 클라이언트
├── stores/
│   └── feedback.ts             # Pinia 스토어
└── styles/
    └── feedback.css            # 독립 스타일
```

### 호스트 앱 통합 (TaskFlow)
```
frontend/src/
├── feedback-module/            # 독립 모듈 (위 구조)
├── views/admin/
│   ├── FeedbackListView.vue    # 관리자 목록 (호스트 앱 전용)
│   └── FeedbackDetailView.vue  # 관리자 상세 (호스트 앱 전용)
└── main.ts                     # 플러그인 등록
```

### 플러그인 API
```typescript
// feedback-module/index.ts
export { FeedbackPlugin } from './plugin'
export { useFeedback } from './composables/useFeedback'
export { useScreenCapture } from './composables/useScreenCapture'
export { useAnnotation } from './composables/useAnnotation'
export type { FeedbackConfig, FeedbackSubmission } from './types'

// 컴포넌트 개별 사용 가능
export { default as FeedbackButton } from './components/FeedbackButton.vue'
export { default as FeedbackModal } from './components/FeedbackModal.vue'
export { default as ScreenshotAnnotator } from './components/ScreenshotAnnotator.vue'
```

### 이벤트 시스템
```typescript
// 호스트 앱에서 이벤트 수신
import { useFeedbackEvents } from '@/feedback-module'

const feedbackEvents = useFeedbackEvents()

feedbackEvents.on('feedback:submitted', (feedback) => {
  // 호스트 앱에서 추가 처리 (알림 표시 등)
  notificationStore.add({ type: 'success', message: '피드백이 접수되었습니다.' })
})

feedbackEvents.on('feedback:error', (error) => {
  // 에러 처리
})

feedbackEvents.on('screenshot:captured', (blob) => {
  // 캡처 완료 시
})
```

---

## 8. 독립형 백엔드 모듈 구조

### 저장소 어댑터 패턴
```java
// 인터페이스 정의
public interface FeedbackRepository {
    Feedback save(Feedback feedback);
    Optional<Feedback> findById(Long id);
    Page<Feedback> findAll(FeedbackSearchRequest request, Pageable pageable);
    void delete(Long id);
}

// MySQL 구현 (TaskFlow 기본)
@Repository
@ConditionalOnProperty(name = "feedback.storage.type", havingValue = "mysql")
public class MySqlFeedbackRepository implements FeedbackRepository {
    private final FeedbackMapper feedbackMapper;
    // MyBatis 기반 구현
}

// PostgreSQL 구현 (다른 프로젝트용)
@Repository
@ConditionalOnProperty(name = "feedback.storage.type", havingValue = "postgresql")
public class PostgresFeedbackRepository implements FeedbackRepository {
    // JdbcTemplate 또는 JPA 구현
}
```

### 알림 어댑터 패턴
```java
public interface FeedbackNotifier {
    void notify(Feedback feedback, NotificationType type);
}

@Component
@ConditionalOnProperty(name = "feedback.notification.email.enabled", havingValue = "true")
public class EmailFeedbackNotifier implements FeedbackNotifier {
    // 이메일 발송
}

@Component
@ConditionalOnProperty(name = "feedback.notification.webhook.enabled", havingValue = "true")
public class WebhookFeedbackNotifier implements FeedbackNotifier {
    // Slack, Discord 등 웹훅 발송
}
```

### 사용자 제공자 인터페이스
```java
// 호스트 앱에서 구현해야 하는 인터페이스
@FunctionalInterface
public interface FeedbackUserProvider {
    FeedbackUser getCurrentUser();
}

// TaskFlow에서의 구현
@Bean
public FeedbackUserProvider feedbackUserProvider() {
    return () -> {
        String username = SecurityUtils.getCurrentUsername();
        return new FeedbackUser(username, userMapper.findNameByUsername(username));
    };
}
```

---

## 9. 구현 순서 (독립 모듈 기준)

### Phase 1: 코어 모듈 (재사용 가능)
| 단계 | 작업 | 위치 |
|-----|------|------|
| 1-1 | 공유 타입 정의 | feedback-module/types/ |
| 1-2 | DB 스키마 (범용) | feedback-module/schema.sql |
| 1-3 | 백엔드 인터페이스 정의 | FeedbackRepository, FeedbackNotifier |
| 1-4 | 백엔드 서비스 로직 | FeedbackService (저장소 무관) |
| 1-5 | REST Controller | FeedbackController |
| 1-6 | 프론트 composables | useScreenCapture, useAnnotation |
| 1-7 | 프론트 컴포넌트 | ScreenshotAnnotator, Modal, Forms |
| 1-8 | Vue 플러그인 | plugin.ts |

### Phase 2: TaskFlow 통합
| 단계 | 작업 | 위치 |
|-----|------|------|
| 2-1 | MySQL 저장소 구현 | MySqlFeedbackRepository |
| 2-2 | 이메일 알림 구현 | EmailFeedbackNotifier |
| 2-3 | TaskFlow 설정 | application.yml |
| 2-4 | 플러그인 등록 | main.ts |
| 2-5 | 관리자 화면 | FeedbackListView, FeedbackDetailView |
| 2-6 | ErrorBoundary 연동 | 자동 에러 수집 |
| 2-7 | 알림 시스템 연동 | NotificationDropdown 확장 |

---

## 10. 확정된 요구사항

- [x] **자동 에러 전달**: 프로덕션 환경에서만 활성화
- [x] **관리자 알림**: 이메일 + UI 알림 (NotificationDropdown 연동)
- [x] **피드백 관리**: 관리자 전용 별도 경로 (`/admin/feedback`)
- [x] **그리기 도구**: 화살표, 사각형, 원, 텍스트, 하이라이트, 자유곡선

### 관리자 전용 접근 경로
```
/admin/feedback              # 피드백 목록 (관리자만 접근)
/admin/feedback/:id          # 피드백 상세
/admin/feedback/:id/report   # Claude Code 리포트 뷰어
```

### 알림 시스템
1. **이메일 알림**: 새 버그 리포트 발생 시 SMTP로 관리자 그룹에 발송
2. **UI 알림**: NotificationDropdown에 피드백 알림 타입 추가

---

## 11. 다른 프로젝트 적용 가이드

### 최소 요구사항
- **프론트엔드**: Vue 3 + Composition API
- **백엔드**: Spring Boot 2.7+ 또는 3.x
- **데이터베이스**: MySQL 8.0+ / PostgreSQL 12+ / MongoDB 4.4+

### 빠른 적용 (3단계)

**1단계: 모듈 복사**
```bash
# 프론트엔드
cp -r taskflow/frontend/src/feedback-module your-project/frontend/src/

# 백엔드
cp -r taskflow/backend/src/main/java/com/taskflow/feedback your-project/backend/src/.../
```

**2단계: 설정 추가**
```yaml
# application.yml
feedback:
  enabled: true
  storage:
    type: mysql
    table-prefix: YOUR_PREFIX_
```

```typescript
// main.ts
import { FeedbackPlugin } from '@/feedback-module'
app.use(FeedbackPlugin, { apiBaseUrl: '/api/feedback', ... })
```

**3단계: DB 마이그레이션**
```bash
mysql -u root -p your_database < feedback-module/schema.sql
```

### 커스터마이징 포인트
| 영역 | 방법 |
|-----|------|
| UI 테마 | CSS 변수 오버라이드 |
| 카테고리 | config.categories 배열 |
| 저장소 | FeedbackRepository 구현 |
| 알림 | FeedbackNotifier 구현 |
| 인증 | FeedbackUserProvider 구현 |

---

## 12. 검증 방법

1. **버그 신고 테스트**
   - 의도적으로 API 에러 발생시킨 후 버그 신고
   - 화면 캡처 및 어노테이션 기능 확인
   - 자동 수집된 정보 확인

2. **개선 요청 테스트**
   - 정상 화면에서 개선 요청 제출
   - 캡처 및 그리기 기능 확인

3. **자동 전달 테스트**
   - ErrorBoundary 트리거 에러 발생
   - 자동 리포트 생성 확인

4. **Claude Code 리포트 테스트**
   - JSON/Markdown 리포트 다운로드
   - 형식 및 내용 검증
