# 서브 에이전트 변경 상세 이력

> 📍 이 문서는 `.claude/agents/` 디렉토리 내 모든 에이전트 파일의 변경 사항을 기록합니다.

---

## 에이전트 목록 및 현재 버전

| 에이전트 | 파일명 | 현재 버전 | 역할 | 모델 |
|---------|--------|----------|------|------|
| debugger | debugger.md | v1.0.0 | 런타임 오류 분석 | Opus |
| mybatis-inspector | mybatis-inspector.md | v1.0.0 | MyBatis XML 검증 | Opus |
| vue-inspector | vue-inspector.md | v1.0.0 | Vue.js 컴포넌트 검증 | Sonnet |
| api-validator | api-validator.md | v1.0.0 | REST API 검증 | Sonnet |
| security-auditor | security-auditor.md | v1.0.0 | 보안 취약점 검사 | Sonnet |
| sse-debugger | sse-debugger.md | v1.0.0 | SSE 실시간 동기화 | Sonnet |
| docker-ops | docker-ops.md | v1.0.0 | Docker 컨테이너 관리 | Haiku |
| feature-reviewer | feature-reviewer.md | v1.0.0 | 기능 완성도 검토 | Sonnet |
| test-validator | test-validator.md | v1.0.0 | 테스트 검증 | Sonnet |
| log-analyzer | log-analyzer.md | v1.0.0 | 로그 분석 | Haiku |
| deployment-docs | deployment-docs.md | v1.0.0 | 배포 매뉴얼 작성 | Opus |
| user-manual | user-manual.md | v1.0.0 | 사용자 매뉴얼 작성 | Sonnet |
| api-docs | api-docs.md | v1.0.0 | API 명세서 작성 | Opus |
| developer-guide | developer-guide.md | v1.0.0 | 개발자 가이드 작성 | Opus |
| admin-manual | admin-manual.md | v1.0.0 | 관리자 매뉴얼 작성 | Sonnet |
| project-sync | project-sync.md | v1.0.0 | 지침 동기화/이력 관리 | Opus |

---

## debugger.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - Spring Boot 예외 분석
  - MyBatis 오류 진단
  - Vue.js 에러 분석
  - 스택트레이스 분석
- **사유**: 디버깅 전문 에이전트 필요

---

## mybatis-inspector.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - XML Mapper 구문 검증
  - SQL 바인딩 파라미터 검사
  - resultMap/resultType 검증
  - 동적 SQL 검증
- **사유**: MyBatis 전문 검증 필요

---

## vue-inspector.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - Composition API 검증
  - Pinia 스토어 검증
  - 반응성 문제 분석
  - 컴포넌트 구조 검토
- **사유**: Vue.js 전문 검증 필요

---

## api-validator.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - REST API 명세 검증
  - 요청/응답 형식 확인
  - HTTP 상태 코드 검증
  - 쿼리 파라미터 검증
- **사유**: API 표준 준수 확인 필요

---

## security-auditor.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - JWT 인증 검증
  - SQL Injection 검사
  - XSS 취약점 검사
  - CORS 설정 검증
- **사유**: 보안 취약점 점검 필요

---

## sse-debugger.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - SSE 연결 상태 분석
  - 이벤트 전파 검증
  - 재연결 로직 확인
  - 충돌 처리 검증
- **사유**: 실시간 동기화 전문 디버깅 필요

---

## docker-ops.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - 컨테이너 상태 관리
  - 네트워크 진단
  - 볼륨 관리
  - 로그 분석
- **사유**: Docker 운영 전문 지원 필요

---

## feature-reviewer.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - 요구사항 충족 검토
  - 엣지케이스 점검
  - Compact UI 스펙 검증
  - 반응형 검증
- **사유**: 기능 품질 검토 필요

---

## test-validator.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - JUnit 테스트 실행/분석
  - Vitest 테스트 실행/분석
  - 커버리지 확인
  - 실패 원인 분석
- **사유**: 테스트 품질 관리 필요

---

## log-analyzer.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - 에러 로그 추출
  - SQL 로그 분석
  - API 요청 로그 분석
  - 패턴 탐지
- **사유**: 로그 분석 전문 지원 필요

---

## deployment-docs.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - 배포 매뉴얼 작성
  - 설치 가이드 작성
  - 환경 설정 문서화
  - 트러블슈팅 가이드
- **사유**: 배포 문서 자동화 필요

---

## user-manual.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - 사용자 가이드 작성
  - 화면별 설명서
  - FAQ 작성
- **사유**: 최종 사용자 문서 필요

---

## api-docs.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - REST API 명세서 작성
  - 요청/응답 예시 작성
  - Postman 컬렉션 생성
- **사유**: API 문서 자동화 필요

---

## developer-guide.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - 시스템 아키텍처 문서
  - 소스 구조 설명
  - 개발 환경 설정 가이드
  - 코딩 컨벤션 문서
- **사유**: 개발자 온보딩 문서 필요

---

## admin-manual.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - 운영 절차 문서
  - 백업/복구 가이드
  - 장애 대응 매뉴얼
  - 모니터링 가이드
- **사유**: 시스템 관리자 문서 필요

---

## project-sync.md

### 현재 버전: v1.0.0

### [v1.0.0] - YYYY-MM-DD
- **유형**: 🆕 Added
- **내용**: 초기 생성
- **주요 기능**:
  - CLAUDE.md 동기화
  - 에이전트 설정 동기화
  - 기능 완료 승인 프로세스
  - 내용 보존 검증
  - 변경 이력 관리
- **사유**: 지침 일관성 관리 필요

---

## 변경 이력 기록 템플릿

### [vX.Y.Z] - YYYY-MM-DD

```markdown
### [vX.Y.Z] - YYYY-MM-DD
- **유형**: 🆕 Added | ✏️ Changed | 🗑️ Removed | 🐛 Fixed
- **내용**: [변경 내용 요약]
- **변경 전**: 
  ```markdown
  [변경 전 내용]
  ```
- **변경 후**:
  ```markdown
  [변경 후 내용]
  ```
- **사유**: [변경 사유]
- **영향**: [연관 에이전트/기능]
- **승인자**: [이름]
```