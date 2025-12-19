-- ============================================
-- TaskFlow Initial Data
-- MySQL 8.0+
-- ============================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 1. 기본 부서 데이터
-- ============================================
INSERT INTO TB_DEPARTMENT (DEPARTMENT_ID, DEPARTMENT_CODE, DEPARTMENT_NAME, PARENT_ID, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(1, 'ROOT', '본사', NULL, 0, 'Y', 1),
(2, 'DEV', '개발본부', 1, 1, 'Y', 1),
(3, 'DEV1', '개발1팀', 2, 1, 'Y', 1),
(4, 'DEV2', '개발2팀', 2, 2, 'Y', 1),
(5, 'QA', 'QA팀', 2, 3, 'Y', 1),
(6, 'BIZ', '경영지원본부', 1, 2, 'Y', 1),
(7, 'HR', '인사팀', 6, 1, 'Y', 1),
(8, 'FIN', '재무팀', 6, 2, 'Y', 1);

-- ============================================
-- 2. 관리자 계정
-- 비밀번호: admin1234 (BCrypt 암호화, Spring 호환)
-- ============================================
INSERT INTO TB_USER (USER_ID, USERNAME, PASSWORD, NAME, DEPARTMENT_ID, USE_YN, CREATED_BY) VALUES
(1, 'admin', '$2a$10$JOMsEVautEYympGsVZGDrezBwYxwb87yAyCBYNsj7Y/Jo9MR2CY7i', '관리자', 1, 'Y', 1);

-- ============================================
-- 3. 기본 그룹 데이터
-- ============================================
INSERT INTO TB_GROUP (GROUP_ID, GROUP_CODE, GROUP_NAME, DESCRIPTION, GROUP_COLOR, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(1, 'DEFAULT', '기본 그룹', '기본 업무 그룹', '#6B7280', 0, 'Y', 1),
(2, 'DAILY', '일일 업무', '매일 처리해야 하는 정기 업무', '#3B82F6', 1, 'Y', 1),
(3, 'PROJECT', '프로젝트', '프로젝트 관련 업무', '#10B981', 2, 'Y', 1),
(4, 'MEETING', '회의', '회의 및 미팅 관련', '#F59E0B', 3, 'Y', 1),
(5, 'ISSUE', '이슈', '버그/이슈 처리', '#EF4444', 4, 'Y', 1);

-- ============================================
-- 4. 관리자-그룹 매핑
-- ============================================
INSERT INTO TB_USER_GROUP (USER_ID, GROUP_ID, CREATED_BY) VALUES
(1, 1, 1),
(1, 2, 1),
(1, 3, 1);

-- ============================================
-- 5. 기본 보드 생성
-- ============================================
INSERT INTO TB_BOARD (BOARD_ID, BOARD_NAME, DESCRIPTION, OWNER_ID, DEFAULT_VIEW, COLOR, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(1, '업무 관리', '기본 업무 관리 보드', 1, 'TABLE', '#3B82F6', 0, 'Y', 1);

-- ============================================
-- 6. 기본 속성 정의 (대표 속성)
-- ============================================

-- 카테고리 속성
INSERT INTO TB_PROPERTY_DEF (PROPERTY_ID, BOARD_ID, PROPERTY_NAME, PROPERTY_TYPE, REQUIRED_YN, VISIBLE_YN, SORT_ORDER, CREATED_BY) VALUES
(1, 1, '카테고리', 'SELECT', 'N', 'Y', 1, 1);

-- 상태 속성 (시스템 예약, TB_ITEM.STATUS와 연동)
INSERT INTO TB_PROPERTY_DEF (PROPERTY_ID, BOARD_ID, PROPERTY_NAME, PROPERTY_TYPE, REQUIRED_YN, VISIBLE_YN, SORT_ORDER, CREATED_BY) VALUES
(2, 1, '상태', 'SELECT', 'Y', 'Y', 2, 1);

-- 우선순위 속성 (시스템 예약, TB_ITEM.PRIORITY와 연동)
INSERT INTO TB_PROPERTY_DEF (PROPERTY_ID, BOARD_ID, PROPERTY_NAME, PROPERTY_TYPE, REQUIRED_YN, VISIBLE_YN, SORT_ORDER, CREATED_BY) VALUES
(3, 1, '우선순위', 'SELECT', 'Y', 'Y', 3, 1);

-- 담당자 속성
INSERT INTO TB_PROPERTY_DEF (PROPERTY_ID, BOARD_ID, PROPERTY_NAME, PROPERTY_TYPE, REQUIRED_YN, VISIBLE_YN, SORT_ORDER, CREATED_BY) VALUES
(4, 1, '담당자', 'USER', 'N', 'Y', 4, 1);

-- 시작일 속성
INSERT INTO TB_PROPERTY_DEF (PROPERTY_ID, BOARD_ID, PROPERTY_NAME, PROPERTY_TYPE, REQUIRED_YN, VISIBLE_YN, SORT_ORDER, CREATED_BY) VALUES
(5, 1, '시작일', 'DATE', 'N', 'Y', 5, 1);

-- 마감일 속성
INSERT INTO TB_PROPERTY_DEF (PROPERTY_ID, BOARD_ID, PROPERTY_NAME, PROPERTY_TYPE, REQUIRED_YN, VISIBLE_YN, SORT_ORDER, CREATED_BY) VALUES
(6, 1, '마감일', 'DATE', 'N', 'Y', 6, 1);

-- ============================================
-- 7. 카테고리 옵션
-- ============================================
INSERT INTO TB_PROPERTY_OPTION (OPTION_ID, PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(1, 1, '개발', '#3B82F6', 1, 'Y', 1),
(2, 1, '기획', '#8B5CF6', 2, 'Y', 1),
(3, 1, '디자인', '#EC4899', 3, 'Y', 1),
(4, 1, '운영', '#10B981', 4, 'Y', 1),
(5, 1, '기타', '#6B7280', 5, 'Y', 1);

-- ============================================
-- 8. 상태 옵션 (시스템 예약)
-- ============================================
INSERT INTO TB_PROPERTY_OPTION (OPTION_ID, PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(10, 2, '시작전', '#9CA3AF', 1, 'Y', 1),
(11, 2, '진행중', '#3B82F6', 2, 'Y', 1),
(12, 2, '완료', '#10B981', 3, 'Y', 1),
(13, 2, '삭제', '#EF4444', 4, 'Y', 1);

-- ============================================
-- 9. 우선순위 옵션 (시스템 예약)
-- ============================================
INSERT INTO TB_PROPERTY_OPTION (OPTION_ID, PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(20, 3, '긴급', '#DC2626', 1, 'Y', 1),
(21, 3, '높음', '#F97316', 2, 'Y', 1),
(22, 3, '보통', '#3B82F6', 3, 'Y', 1),
(23, 3, '낮음', '#6B7280', 4, 'Y', 1);

-- ============================================
-- 10. 샘플 작업 템플릿
-- ============================================
INSERT INTO TB_TASK_TEMPLATE (TEMPLATE_ID, CONTENT, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(1, '일일 업무 보고', 1, 'Y', 1),
(2, '주간 회의 준비', 2, 'Y', 1),
(3, '코드 리뷰', 3, 'Y', 1),
(4, '버그 수정', 4, 'Y', 1),
(5, '문서 작성', 5, 'Y', 1),
(6, '테스트 수행', 6, 'Y', 1),
(7, '배포 준비', 7, 'Y', 1),
(8, '고객 미팅', 8, 'Y', 1);

-- ============================================
-- 11. 샘플 아이템 (업무)
-- ============================================
INSERT INTO TB_ITEM (ITEM_ID, BOARD_ID, CONTENT, DESCRIPTION, STATUS, PRIORITY, CATEGORY_ID, GROUP_ID, ASSIGNEE_ID, CREATED_BY) VALUES
(1, 1, '프로젝트 초기 설정', '## 목표\n프로젝트 환경 구성 및 초기 설정 완료\n\n## 체크리스트\n- [x] Git 저장소 생성\n- [x] Docker 환경 구성\n- [x] 개발 환경 세팅', 'COMPLETED', 'HIGH', 1, 3, 1, 1),
(2, 1, 'ERD 설계 및 검토', '## 설계 범위\n- 사용자/부서/그룹 관리\n- 보드 및 아이템 관리\n- 동적 속성 시스템\n\n## 검토 사항\n- 정규화 수준\n- 인덱스 전략', 'COMPLETED', 'HIGH', 1, 3, 1, 1),
(3, 1, 'API 명세서 작성', '## REST API 설계\n\n### 인증 API\n- POST /api/auth/login\n- POST /api/auth/logout\n\n### 업무 API\n- GET /api/boards/{id}/items\n- POST /api/boards/{id}/items', 'IN_PROGRESS', 'NORMAL', 1, 3, 1, 1),
(4, 1, '프론트엔드 레이아웃 구현', NULL, 'NOT_STARTED', 'NORMAL', 1, 3, 1, 1),
(5, 1, '로그인 기능 개발', NULL, 'NOT_STARTED', 'HIGH', 1, 3, 1, 1);

-- ============================================
-- AUTO_INCREMENT 재설정
-- ============================================
ALTER TABLE TB_DEPARTMENT AUTO_INCREMENT = 100;
ALTER TABLE TB_USER AUTO_INCREMENT = 100;
ALTER TABLE TB_GROUP AUTO_INCREMENT = 100;
ALTER TABLE TB_USER_GROUP AUTO_INCREMENT = 100;
ALTER TABLE TB_BOARD AUTO_INCREMENT = 100;
ALTER TABLE TB_PROPERTY_DEF AUTO_INCREMENT = 100;
ALTER TABLE TB_PROPERTY_OPTION AUTO_INCREMENT = 100;
ALTER TABLE TB_TASK_TEMPLATE AUTO_INCREMENT = 100;
ALTER TABLE TB_ITEM AUTO_INCREMENT = 100;
