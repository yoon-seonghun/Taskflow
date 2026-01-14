# 메일 발송 기능 설계서

> **버전**: v2.1.0
> **작성일**: 2026-01-13
> **상태**: 승인 완료

---

## 1. 개요

### 1.1 목적
TaskFlow 시스템에서 업무 관련 알림 및 수동 메일 발송 기능을 제공합니다.

### 1.2 주요 기능
| 기능 | 설명 |
|------|------|
| 업무 할당 알림 | 담당자 지정/변경 시 메일 발송 |
| 업무 이관 알림 | 업무 이관 시 수신자에게 메일 발송 |
| 공유 알림 | 보드/업무 공유 시 메일 발송 |
| 마감일 알림 | D-1, D-day 자동 알림 (스케줄러) |
| 수동 발송 | 사용자가 직접 메일 발송 |

### 1.3 SMTP 설정
```
SMTP_HOST=mail.sns-at.co.kr
SMTP_PORT=587
SMTP_SECURITY_TYPE=STARTTLS
SMTP_USERNAME=sns@sns-at.co.kr
SMTP_PASSWORD=(암호화 저장)
EMAIL_FROM=TaskFlow <sns@sns-at.co.kr>
ADMIN_EMAIL=admin@sns-at.co.kr
```

---

## 2. ERD 및 테이블 정의

### 2.1 ERD

```
┌─────────────────────────┐
│   TB_SYSTEM_CONFIG      │
├─────────────────────────┤
│ CONFIG_ID (PK)          │
│ CONFIG_GROUP            │
│ CONFIG_KEY              │
│ CONFIG_VALUE            │
│ CONFIG_VALUE_ENCRYPTED  │
│ DESCRIPTION             │
│ USE_YN                  │
└─────────────────────────┘

┌─────────────────────────┐         ┌─────────────────────────┐
│     TB_EMAIL_LOG        │         │       TB_USER           │
├─────────────────────────┤         ├─────────────────────────┤
│ EMAIL_LOG_ID (PK)       │    *────│ USERNAME                │
│ EMAIL_TYPE              │         │ + EMAIL (신규)          │
│ RECIPIENT_USERNAME (FK) │─────────│ ...                     │
│ RECIPIENT_EMAIL         │         └─────────────────────────┘
│ SUBJECT                 │
│ CONTENT                 │
│ STATUS                  │
│ RELATED_TYPE            │
│ RELATED_ID              │
└─────────────────────────┘
```

### 2.2 TB_SYSTEM_CONFIG (시스템 설정)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| CONFIG_ID | BIGINT | N | AUTO_INCREMENT | PK |
| CONFIG_GROUP | VARCHAR(50) | N | | 설정 그룹 (SMTP, SYSTEM 등) |
| CONFIG_KEY | VARCHAR(100) | N | | 설정 키 |
| CONFIG_VALUE | VARCHAR(500) | Y | | 설정 값 |
| CONFIG_VALUE_ENCRYPTED | VARCHAR(500) | Y | | 암호화된 설정 값 |
| DESCRIPTION | VARCHAR(200) | Y | | 설정 설명 |
| USE_YN | CHAR(1) | N | 'Y' | 사용 여부 |
| CREATED_AT | DATETIME | N | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | VARCHAR(50) | N | | 생성자 |
| UPDATED_AT | DATETIME | Y | | 수정일시 |
| UPDATED_BY | VARCHAR(50) | Y | | 수정자 |

**인덱스**:
- UK_SYSTEM_CONFIG_GROUP_KEY: (CONFIG_GROUP, CONFIG_KEY) UNIQUE

### 2.3 TB_EMAIL_LOG (메일 발송 이력)

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| EMAIL_LOG_ID | BIGINT | N | AUTO_INCREMENT | PK |
| EMAIL_TYPE | VARCHAR(50) | N | | 발송 유형 |
| SUBJECT | VARCHAR(500) | N | | 메일 제목 |
| RECIPIENT_EMAIL | VARCHAR(200) | N | | 수신자 이메일 |
| RECIPIENT_NAME | VARCHAR(100) | Y | | 수신자 이름 |
| RECIPIENT_USERNAME | VARCHAR(50) | Y | | 수신자 USERNAME |
| CONTENT | TEXT | N | | 메일 본문 |
| STATUS | VARCHAR(20) | N | 'PENDING' | 발송 상태 |
| ERROR_MESSAGE | VARCHAR(500) | Y | | 실패 시 에러 메시지 |
| RELATED_TYPE | VARCHAR(50) | Y | | 관련 대상 유형 |
| RELATED_ID | BIGINT | Y | | 관련 대상 ID |
| SENT_AT | DATETIME | Y | | 발송일시 |
| CREATED_AT | DATETIME | N | CURRENT_TIMESTAMP | 생성일시 |
| CREATED_BY | VARCHAR(50) | N | | 생성자 |

**인덱스**:
- IDX_EMAIL_LOG_TYPE: (EMAIL_TYPE)
- IDX_EMAIL_LOG_STATUS: (STATUS)
- IDX_EMAIL_LOG_RECIPIENT: (RECIPIENT_EMAIL)
- IDX_EMAIL_LOG_CREATED: (CREATED_AT)

### 2.4 TB_USER 컬럼 추가

| 추가 컬럼 | 타입 | NULL | 기본값 | 설명 |
|-----------|------|------|--------|------|
| EMAIL | VARCHAR(200) | Y | NULL | 이메일 주소 |

### 2.5 메일 발송 유형 (EMAIL_TYPE)

| 코드 | 설명 | 트리거 |
|------|------|--------|
| TASK_ASSIGN | 업무 담당자 지정/변경 | 담당자 변경 시 |
| TASK_TRANSFER | 업무 이관 | 업무 이관 시 |
| TASK_SHARE | 업무/보드 공유 | 공유 추가 시 |
| DUE_DATE_ALERT | 마감일 알림 | 스케줄러 (D-1, D-day) |
| MANUAL | 수동 발송 | 사용자 버튼 클릭 |

### 2.6 발송 상태 (STATUS)

| 코드 | 설명 |
|------|------|
| PENDING | 발송 대기 |
| SENT | 발송 완료 |
| FAILED | 발송 실패 |

---

## 3. API 명세

### 3.1 시스템 설정 API

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/system-config/{group} | 그룹별 설정 조회 | ADMIN |
| PUT | /api/system-config/{group} | 그룹별 설정 일괄 수정 | ADMIN |
| POST | /api/system-config/smtp/test | SMTP 연결 테스트 | ADMIN |

#### GET /api/system-config/{group}

**Response**:
```json
{
  "success": true,
  "data": {
    "group": "SMTP",
    "configs": [
      { "key": "HOST", "value": "mail.sns-at.co.kr", "description": "SMTP 서버 주소" },
      { "key": "PORT", "value": "587", "description": "SMTP 포트" },
      { "key": "SECURITY_TYPE", "value": "STARTTLS", "description": "보안 유형" },
      { "key": "USERNAME", "value": "sns@sns-at.co.kr", "description": "계정" },
      { "key": "PASSWORD", "value": "********", "description": "비밀번호 (마스킹)" },
      { "key": "FROM_ADDRESS", "value": "TaskFlow <sns@sns-at.co.kr>", "description": "발신자" },
      { "key": "ADMIN_EMAIL", "value": "admin@sns-at.co.kr", "description": "관리자 이메일" }
    ]
  }
}
```

#### PUT /api/system-config/{group}

**Request**:
```json
{
  "configs": [
    { "key": "HOST", "value": "mail.sns-at.co.kr" },
    { "key": "PORT", "value": "587" },
    { "key": "SECURITY_TYPE", "value": "STARTTLS" },
    { "key": "USERNAME", "value": "sns@sns-at.co.kr" },
    { "key": "PASSWORD", "value": "newpassword" },
    { "key": "FROM_ADDRESS", "value": "TaskFlow <sns@sns-at.co.kr>" },
    { "key": "ADMIN_EMAIL", "value": "admin@sns-at.co.kr" }
  ]
}
```

**Response**:
```json
{
  "success": true,
  "message": "설정이 저장되었습니다."
}
```

#### POST /api/system-config/smtp/test

**Request**:
```json
{
  "testEmail": "test@example.com"
}
```

**Response**:
```json
{
  "success": true,
  "message": "테스트 메일이 발송되었습니다."
}
```

### 3.2 메일 발송 API

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| POST | /api/emails/send | 수동 메일 발송 | USER |
| POST | /api/emails/send/item/{itemId} | 업무 관련 메일 발송 | USER |
| GET | /api/emails/logs | 발송 이력 조회 | ADMIN |
| GET | /api/emails/logs/{id} | 발송 상세 조회 | ADMIN |

#### POST /api/emails/send

**Request**:
```json
{
  "recipients": [
    { "email": "user1@example.com", "name": "홍길동" },
    { "email": "user2@example.com", "name": "김철수" }
  ],
  "subject": "업무 협조 요청",
  "content": "안녕하세요, 업무 협조 부탁드립니다.",
  "relatedType": "ITEM",
  "relatedId": 123
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "sentCount": 2,
    "failedCount": 0
  }
}
```

#### POST /api/emails/send/item/{itemId}

**Request**:
```json
{
  "emailType": "TASK_ASSIGN",
  "recipientUsernames": ["user1", "user2"],
  "additionalRecipients": [
    { "email": "external@example.com", "name": "외부인" }
  ],
  "customMessage": "추가 메시지 (선택)"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "sentCount": 3,
    "failedCount": 0
  }
}
```

#### GET /api/emails/logs

**Query Params**:
- emailType: 발송 유형 필터
- status: 상태 필터 (PENDING, SENT, FAILED)
- startDate, endDate: 기간 필터
- page, size: 페이징

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "emailLogId": 1,
        "emailType": "TASK_ASSIGN",
        "subject": "[TaskFlow] 새 업무가 할당되었습니다",
        "recipientEmail": "user@example.com",
        "recipientName": "홍길동",
        "status": "SENT",
        "sentAt": "2026-01-13T10:00:00",
        "createdAt": "2026-01-13T10:00:00"
      }
    ],
    "totalElements": 100,
    "totalPages": 10
  }
}
```

---

## 4. 백엔드 구조

### 4.1 패키지 구조

```
com.taskflow
├── config/
│   └── MailConfig.java
├── domain/
│   ├── SystemConfig.java
│   └── EmailLog.java
├── dto/
│   ├── config/
│   │   ├── SystemConfigResponse.java
│   │   ├── SystemConfigUpdateRequest.java
│   │   └── SmtpTestRequest.java
│   └── email/
│       ├── EmailSendRequest.java
│       ├── ItemEmailRequest.java
│       ├── EmailLogResponse.java
│       └── EmailSendResult.java
├── mapper/
│   ├── SystemConfigMapper.java / .xml
│   └── EmailLogMapper.java / .xml
├── service/
│   ├── SystemConfigService.java
│   ├── EmailService.java
│   └── impl/
│       ├── SystemConfigServiceImpl.java
│       └── EmailServiceImpl.java
├── controller/
│   ├── SystemConfigController.java
│   └── EmailController.java
└── scheduler/
    └── EmailScheduler.java
```

### 4.2 메일 템플릿

```
resources/templates/email/
├── task-assign.html        # 업무 할당 템플릿
├── task-transfer.html      # 업무 이관 템플릿
├── share-notification.html # 공유 알림 템플릿
├── due-date-alert.html     # 마감일 알림 템플릿
└── manual-email.html       # 수동 발송 템플릿
```

### 4.3 의존성 (build.gradle)

```groovy
dependencies {
    // 메일 발송
    implementation 'org.springframework.boot:spring-boot-starter-mail'

    // 템플릿 엔진 (Thymeleaf)
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
}
```

---

## 5. 프론트엔드 구조

### 5.1 디렉토리 구조

```
frontend/src/
├── api/
│   ├── systemConfig.ts
│   └── email.ts
├── types/
│   ├── systemConfig.ts
│   └── email.ts
├── stores/
│   └── systemConfig.ts
└── components/
    ├── settings/
    │   ├── SystemSettingsContent.vue
    │   └── SmtpSettingsPanel.vue
    ├── email/
    │   ├── EmailSendModal.vue
    │   └── EmailLogList.vue
    └── item/
        └── ItemDetailPanel.vue (수정)
```

### 5.2 설정 메뉴 구조

```
설정 (SettingsView.vue)
├── 기존 탭들...
├── 외부 DB 관리
├── 외부 쿼리 관리
└── 시스템 관리 (신규)
    └── SMTP 설정
```

---

## 6. UI 설계

### 6.1 시스템 관리 > SMTP 설정

```
┌─────────────────────────────────────────────────────────────────┐
│ 시스템 관리                                                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ ▼ SMTP 설정                                                      │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ SMTP 서버    [mail.sns-at.co.kr                          ] │ │
│ │ 포트         [587                                        ] │ │
│ │ 보안 유형    [STARTTLS ▼]  ○ STARTTLS  ○ TLS  ○ 없음      │ │
│ │ 계정         [sns@sns-at.co.kr                           ] │ │
│ │ 비밀번호     [••••••••••••                               ] │ │
│ │ 발신자       [TaskFlow <sns@sns-at.co.kr>                ] │ │
│ │ 관리자 이메일 [admin@sns-at.co.kr                         ] │ │
│ │                                                             │ │
│ │ [테스트 메일 발송]                        [저장]  [초기화]  │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 메일 발송 모달

```
┌─────────────────────────────────────────────────────────────┐
│ 메일 발송                                              [X] │
├─────────────────────────────────────────────────────────────┤
│ 발송 유형:  ○ 업무 할당  ○ 업무 이관  ○ 공유  ● 수동 발송  │
│                                                             │
│ 수신자:                                                     │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 시스템 사용자:  [홍길동 ▼] [+ 추가]                     │ │
│ │   ✓ 홍길동 (hong@example.com)                          │ │
│ │   ✓ 김철수 (kim@example.com)                           │ │
│ │                                                         │ │
│ │ 직접 입력:  [이메일 주소 입력...        ] [+ 추가]      │ │
│ │   ✓ external@partner.com                               │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ 제목:  [[TaskFlow] 신규 기능 개발 - 업무 안내           ]  │
│                                                             │
│ 내용:                                                       │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 안녕하세요,                                             │ │
│ │ 아래 업무에 대해 협조 부탁드립니다.                      │ │
│ │                                                         │ │
│ │ ■ 업무명: 신규 기능 개발                                │ │
│ │ ■ 마감일: 2026-01-20                                   │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│                               [취소]  [발송] (3명에게 발송) │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. 구현 체크리스트

### Phase 1: DB 스키마
- [ ] 10_email_tables.sql 생성
- [ ] 01_schema.sql TB_USER.EMAIL 추가

### Phase 2: 백엔드 Domain/DTO
- [ ] SystemConfig.java
- [ ] EmailLog.java
- [ ] SystemConfigResponse.java
- [ ] SystemConfigUpdateRequest.java
- [ ] SmtpTestRequest.java
- [ ] EmailSendRequest.java
- [ ] ItemEmailRequest.java
- [ ] EmailLogResponse.java
- [ ] EmailSendResult.java

### Phase 3: 백엔드 Mapper
- [ ] SystemConfigMapper.java / .xml
- [ ] EmailLogMapper.java / .xml
- [ ] UserMapper.xml 수정 (EMAIL 컬럼)

### Phase 4: 백엔드 Service/Controller
- [ ] SystemConfigService.java
- [ ] SystemConfigServiceImpl.java
- [ ] EmailService.java
- [ ] EmailServiceImpl.java
- [ ] SystemConfigController.java
- [ ] EmailController.java
- [ ] MailConfig.java
- [ ] EmailScheduler.java

### Phase 5: 메일 템플릿
- [ ] task-assign.html
- [ ] task-transfer.html
- [ ] share-notification.html
- [ ] due-date-alert.html
- [ ] manual-email.html

### Phase 6: 프론트엔드
- [ ] api/systemConfig.ts
- [ ] api/email.ts
- [ ] types/systemConfig.ts
- [ ] types/email.ts
- [ ] stores/systemConfig.ts
- [ ] SystemSettingsContent.vue
- [ ] SmtpSettingsPanel.vue
- [ ] EmailSendModal.vue
- [ ] SettingsView.vue 수정
- [ ] ItemDetailPanel.vue 수정

### Phase 7: 테스트
- [ ] SMTP 연결 테스트
- [ ] 테스트 메일 발송
- [ ] 업무 할당 알림 테스트
- [ ] 마감일 알림 스케줄러 테스트

---

## 8. 파일 목록

### 8.1 신규 생성 파일

| 파일 | 설명 |
|------|------|
| docker/mysql/init/10_email_tables.sql | 테이블 생성 SQL |
| backend/.../domain/SystemConfig.java | 시스템 설정 엔티티 |
| backend/.../domain/EmailLog.java | 메일 이력 엔티티 |
| backend/.../dto/config/*.java | 설정 DTO (3개) |
| backend/.../dto/email/*.java | 메일 DTO (4개) |
| backend/.../mapper/SystemConfigMapper.java | 설정 Mapper 인터페이스 |
| backend/.../mapper/SystemConfigMapper.xml | 설정 Mapper XML |
| backend/.../mapper/EmailLogMapper.java | 메일 이력 Mapper 인터페이스 |
| backend/.../mapper/EmailLogMapper.xml | 메일 이력 Mapper XML |
| backend/.../service/SystemConfigService.java | 설정 서비스 인터페이스 |
| backend/.../service/impl/SystemConfigServiceImpl.java | 설정 서비스 구현 |
| backend/.../service/EmailService.java | 메일 서비스 인터페이스 |
| backend/.../service/impl/EmailServiceImpl.java | 메일 서비스 구현 |
| backend/.../controller/SystemConfigController.java | 설정 API |
| backend/.../controller/EmailController.java | 메일 API |
| backend/.../scheduler/EmailScheduler.java | 마감일 알림 스케줄러 |
| backend/.../config/MailConfig.java | 메일 설정 |
| backend/.../resources/templates/email/*.html | 메일 템플릿 (5개) |
| frontend/src/api/systemConfig.ts | 프론트 설정 API |
| frontend/src/api/email.ts | 프론트 메일 API |
| frontend/src/types/systemConfig.ts | 설정 타입 |
| frontend/src/types/email.ts | 메일 타입 |
| frontend/src/stores/systemConfig.ts | 설정 스토어 |
| frontend/src/components/settings/SystemSettingsContent.vue | 시스템 설정 화면 |
| frontend/src/components/settings/SmtpSettingsPanel.vue | SMTP 설정 패널 |
| frontend/src/components/email/EmailSendModal.vue | 메일 발송 모달 |

### 8.2 수정 파일

| 파일 | 변경 내용 |
|------|---------|
| docker/mysql/init/01_schema.sql | TB_USER.EMAIL 컬럼 추가 |
| backend/.../domain/User.java | email 필드 추가 |
| backend/.../mapper/UserMapper.xml | EMAIL 컬럼 매핑 |
| backend/build.gradle | mail, thymeleaf 의존성 추가 |
| frontend/src/views/SettingsView.vue | 시스템 관리 탭 추가 |
| frontend/src/components/item/ItemDetailPanel.vue | 메일 버튼 추가 |

---

## 9. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-13 | Claude | 최초 작성 |
