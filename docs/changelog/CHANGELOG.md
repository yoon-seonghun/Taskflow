# 프로젝트 지침 변경 이력 (CHANGELOG)

> 📍 이 문서는 CLAUDE.md 및 서브 에이전트 설정의 모든 변경 사항을 기록합니다.
> 
> 형식: [Semantic Versioning](https://semver.org/)
> - Major (X.0.0): 구조 변경, 대규모 규칙 변경
> - Minor (0.X.0): 기능/섹션 추가
> - Patch (0.0.X): 오타 수정, 버그 수정

---

## [Unreleased]
> 다음 버전에 포함될 변경 사항

### 예정된 변경
- 

---

## [v1.0.0] - YYYY-MM-DD

### 🆕 Added (추가)

#### CLAUDE.md
| 섹션 | 내용 | 사유 |
|-----|------|------|
| 프로젝트 개요 | 초기 작성 | 프로젝트 시작 |
| 기술 스택 | Spring Boot, Vue.js, MySQL 등 | 기술 스택 정의 |
| 개발 규칙 | 설계 우선 원칙 | 개발 프로세스 정립 |
| DB 컨벤션 | 테이블/컬럼 네이밍 규칙 | 일관성 확보 |
| API 설계 원칙 | REST API 엔드포인트 | API 표준화 |
| 디버깅 원칙 | 기능 보존 원칙 | 무분별 삭제 방지 |
| 내용 보존 원칙 | 파일 수정 시 규칙 | 누락 방지 |
| 기능 완료 승인 | 지침 정리 프로세스 | 비대화 방지 |

#### 서브 에이전트
| 에이전트 | 역할 | 비고 |
|---------|------|------|
| debugger | 런타임 오류 분석 | Opus |
| mybatis-inspector | MyBatis XML 검증 | Opus |
| vue-inspector | Vue.js 컴포넌트 검증 | Sonnet |
| api-validator | REST API 검증 | Sonnet |
| security-auditor | 보안 취약점 검사 | Sonnet |
| sse-debugger | SSE 실시간 동기화 | Sonnet |
| docker-ops | Docker 컨테이너 관리 | Haiku |
| feature-reviewer | 기능 완성도 검토 | Sonnet |
| test-validator | 테스트 검증 | Sonnet |
| log-analyzer | 로그 분석 | Haiku |
| deployment-docs | 배포 매뉴얼 작성 | Opus |
| user-manual | 사용자 매뉴얼 작성 | Sonnet |
| api-docs | API 명세서 작성 | Opus |
| developer-guide | 개발자 가이드 작성 | Opus |
| admin-manual | 관리자 매뉴얼 작성 | Sonnet |
| project-sync | 지침 동기화/이력 관리 | Opus |

### 승인 정보
- 작성자: 
- 승인자: 
- 승인일: YYYY-MM-DD

---

## 변경 유형 범례

| 아이콘 | 유형 | 설명 |
|-------|------|------|
| 🆕 | Added | 새로운 기능/섹션 추가 |
| ✏️ | Changed | 기존 내용 변경 |
| 🗑️ | Removed | 기존 내용 제거 (승인 필수) |
| 🐛 | Fixed | 버그/오류 수정 |
| 📚 | Archived | 완료 기능 아카이브 |
| 🔒 | Security | 보안 관련 변경 |
| ⚠️ | Deprecated | 향후 제거 예정 |

---

## 이력 관리 규칙

1. **모든 변경은 기록**: 사소한 수정도 Patch 버전으로 기록
2. **사유 필수**: 변경 사유 없는 기록 금지
3. **승인 기록**: 승인자 정보 필수 포함
4. **시간순 정렬**: 최신 버전이 맨 위
5. **영구 보존**: 이력 삭제/수정 금지

---

## 관련 문서

- [CLAUDE.md 상세 이력](./claude_md_history.md)
- [에이전트 상세 이력](./agents_history.md)
- [아카이브 인덱스](../archive/README.md)