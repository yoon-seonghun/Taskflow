-- ============================================
-- TaskFlow Initial Data
-- MySQL 8.0+
-- USERNAME/DEPARTMENT_CODE 기반 FK 참조
-- ============================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 1. 기본 부서 데이터
-- PARENT_CODE: 상위 부서 코드 참조
-- CREATED_BY: USERNAME 참조
-- ============================================
INSERT INTO TB_DEPARTMENT (DEPARTMENT_ID, DEPARTMENT_CODE, DEPARTMENT_NAME, PARENT_CODE, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(1, 'ROOT', '본사', NULL, 0, 'Y', 'admin'),
(2, 'DEV', '개발본부', 'ROOT', 1, 'Y', 'admin'),
(3, 'DEV1', '개발1팀', 'DEV', 1, 'Y', 'admin'),
(4, 'DEV2', '개발2팀', 'DEV', 2, 'Y', 'admin'),
(5, 'QA', 'QA팀', 'DEV', 3, 'Y', 'admin'),
(6, 'BIZ', '경영지원본부', 'ROOT', 2, 'Y', 'admin'),
(7, 'HR', '인사팀', 'BIZ', 1, 'Y', 'admin'),
(8, 'FIN', '재무팀', 'BIZ', 2, 'Y', 'admin');

-- ============================================
-- 2. 기본 직급 데이터
-- SORT_ORDER: 낮을수록 높은 직급
-- ============================================
INSERT INTO TB_POSITION (POSITION_ID, POSITION_CODE, POSITION_NAME, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(1, 'CEO', '대표', 1, 'Y', 'admin'),
(2, 'DIRECTOR', '이사', 2, 'Y', 'admin'),
(3, 'GM', '부장', 3, 'Y', 'admin'),
(4, 'DGM', '차장', 4, 'Y', 'admin'),
(5, 'MANAGER', '과장', 5, 'Y', 'admin'),
(6, 'AM', '대리', 6, 'Y', 'admin'),
(7, 'STAFF', '사원', 7, 'Y', 'admin'),
(8, 'INTERN', '인턴', 8, 'Y', 'admin');

-- ============================================
-- 3. 관리자 계정
-- 비밀번호: admin123 (BCrypt 암호화, Spring 호환)
-- DEPARTMENT_CODE: 부서 코드 참조
-- POSITION_CODE: 직급 코드 참조
-- HEAD_YN: 팀장 여부
-- ============================================
INSERT INTO TB_USER (USER_ID, USERNAME, PASSWORD, NAME, DEPARTMENT_CODE, POSITION_CODE, HEAD_YN, USE_YN, CREATED_BY) VALUES
(1, 'admin', '$2a$10$XVOOa0vdyud2/ZaoVFNPxOnsj/p6P7l0NIbOPJCW7zFOQo53HOA06', '관리자', 'ROOT', 'CEO', 'Y', 'Y', 'admin');

-- ============================================
-- 3. 기본 그룹 데이터
-- CREATED_BY: USERNAME 참조
-- ============================================
INSERT INTO TB_GROUP (GROUP_ID, GROUP_CODE, GROUP_NAME, DESCRIPTION, GROUP_COLOR, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(1, 'DEFAULT', '기본 그룹', '기본 업무 그룹', '#6B7280', 0, 'Y', 'admin'),
(2, 'DAILY', '일일 업무', '매일 처리해야 하는 정기 업무', '#3B82F6', 1, 'Y', 'admin'),
(3, 'PROJECT', '프로젝트', '프로젝트 관련 업무', '#10B981', 2, 'Y', 'admin'),
(4, 'MEETING', '회의', '회의 및 미팅 관련', '#F59E0B', 3, 'Y', 'admin'),
(5, 'ISSUE', '이슈', '버그/이슈 처리', '#EF4444', 4, 'Y', 'admin');

-- ============================================
-- 4. 관리자-그룹 매핑
-- USERNAME: 사용자 USERNAME 참조
-- ============================================
INSERT INTO TB_USER_GROUP (USERNAME, GROUP_ID, CREATED_BY) VALUES
('admin', 1, 'admin'),
('admin', 2, 'admin'),
('admin', 3, 'admin');

-- ============================================
-- 5. 카테고리 (전역)
-- ============================================
INSERT INTO TB_CATEGORY (CATEGORY_ID, CATEGORY_NAME, COLOR, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(1, '개발', '#3B82F6', 1, 'Y', 'admin'),
(2, '기획', '#8B5CF6', 2, 'Y', 'admin'),
(3, '디자인', '#EC4899', 3, 'Y', 'admin'),
(4, '운영', '#10B981', 4, 'Y', 'admin'),
(5, '기타', '#6B7280', 5, 'Y', 'admin');

-- ============================================
-- 6. 기본 보드 생성
-- OWNER_USERNAME: 소유자 USERNAME 참조
-- ============================================
INSERT INTO TB_BOARD (BOARD_ID, BOARD_NAME, DESCRIPTION, OWNER_USERNAME, DEFAULT_VIEW, COLOR, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(1, '업무 관리', '기본 업무 관리 보드', 'admin', 'TABLE', '#3B82F6', 0, 'Y', 'admin');

-- ============================================
-- 7. 샘플 작업 템플릿
-- ============================================
INSERT INTO TB_TASK_TEMPLATE (TEMPLATE_ID, CONTENT, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(1, '일일 업무 보고', 1, 'Y', 'admin'),
(2, '주간 회의 준비', 2, 'Y', 'admin'),
(3, '코드 리뷰', 3, 'Y', 'admin'),
(4, '버그 수정', 4, 'Y', 'admin'),
(5, '문서 작성', 5, 'Y', 'admin'),
(6, '테스트 수행', 6, 'Y', 'admin'),
(7, '배포 준비', 7, 'Y', 'admin'),
(8, '고객 미팅', 8, 'Y', 'admin');

-- ============================================
-- 8. 샘플 아이템 (업무)
-- ASSIGNEE_USERNAME: 담당자 USERNAME 참조
-- CATEGORY_ID: TB_CATEGORY 참조
-- ============================================
INSERT INTO TB_ITEM (ITEM_ID, BOARD_ID, CONTENT, DESCRIPTION, STATUS, PRIORITY, CATEGORY_ID, GROUP_ID, ASSIGNEE_USERNAME, CREATED_BY) VALUES
(1, 1, '프로젝트 초기 설정', '## 목표\n프로젝트 환경 구성 및 초기 설정 완료\n\n## 체크리스트\n- [x] Git 저장소 생성\n- [x] Docker 환경 구성\n- [x] 개발 환경 세팅', 'COMPLETED', 'HIGH', 1, 3, 'admin', 'admin'),
(2, 1, 'ERD 설계 및 검토', '## 설계 범위\n- 사용자/부서/그룹 관리\n- 보드 및 아이템 관리\n- 동적 속성 시스템\n\n## 검토 사항\n- 정규화 수준\n- 인덱스 전략', 'COMPLETED', 'HIGH', 1, 3, 'admin', 'admin'),
(3, 1, 'API 명세서 작성', '## REST API 설계\n\n### 인증 API\n- POST /api/auth/login\n- POST /api/auth/logout\n\n### 업무 API\n- GET /api/boards/{id}/items\n- POST /api/boards/{id}/items', 'IN_PROGRESS', 'NORMAL', 1, 3, 'admin', 'admin'),
(4, 1, '프론트엔드 레이아웃 구현', NULL, 'NOT_STARTED', 'NORMAL', 1, 3, 'admin', 'admin'),
(5, 1, '로그인 기능 개발', NULL, 'NOT_STARTED', 'HIGH', 1, 3, 'admin', 'admin');

-- ============================================
-- AUTO_INCREMENT 재설정
-- 100부터 시작하여 Shadow User/Dept 공간 확보
-- ============================================
ALTER TABLE TB_DEPARTMENT AUTO_INCREMENT = 100;
ALTER TABLE TB_USER AUTO_INCREMENT = 100;
ALTER TABLE TB_GROUP AUTO_INCREMENT = 100;
ALTER TABLE TB_CATEGORY AUTO_INCREMENT = 100;
ALTER TABLE TB_BOARD AUTO_INCREMENT = 100;
ALTER TABLE TB_PROPERTY_DEF AUTO_INCREMENT = 100;
ALTER TABLE TB_PROPERTY_OPTION AUTO_INCREMENT = 100;
ALTER TABLE TB_TASK_TEMPLATE AUTO_INCREMENT = 100;
ALTER TABLE TB_ITEM AUTO_INCREMENT = 100;
