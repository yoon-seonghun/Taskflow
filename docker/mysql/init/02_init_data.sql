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
(1, 'T110', '회장', 1, 'Y', 'admin'),
(2, 'T111', '사장', 2, 'Y', 'admin'),
(3, 'T101', '대표이사', 3, 'Y', 'admin'),
(4, 'T102', '부사장', 4, 'Y', 'admin'),
(5, 'T121', '원장', 5, 'Y', 'admin'),
(6, 'T122', '사업부장', 6, 'Y', 'admin'),
(7, 'T123', '국장', 7, 'Y', 'admin'),
(8, 'T106', '본부장', 8, 'Y', 'admin'),
(9, 'T124', '센터장', 9, 'Y', 'admin'),
(10, 'T125', '지서장', 10, 'Y', 'admin'),
(11, 'T103', '담당임원', 11, 'Y', 'admin'),
(12, 'T126', '그룹장', 12, 'Y', 'admin'),
(13, 'T127', '처장', 13, 'Y', 'admin'),
(14, 'T128', '지점장', 14, 'Y', 'admin'),
(15, 'T129', '실장', 15, 'Y', 'admin'),
(16, 'T104', '팀장', 16, 'Y', 'admin'),
(17, 'T130', '파트장', 17, 'Y', 'admin'),
(18, 'T105', '팀원', 18, 'Y', 'admin'),
(19, 'T131', '총무', 19, 'Y', 'admin'),
(20, 'T120', '고문', 20, 'Y', 'admin');

-- ============================================
-- 3. 관리자 계정
-- 비밀번호: admin123 (BCrypt 암호화, Spring 호환)
-- DEPARTMENT_CODE: 부서 코드 참조
-- POSITION_CODE: 직급 코드 참조
-- ROLE: 권한 (ADMIN, MANAGER, USER, GUEST)
-- HEAD_YN: 팀장 여부
-- ============================================
INSERT INTO TB_USER (USER_ID, USERNAME, PASSWORD, NAME, DEPARTMENT_CODE, POSITION_CODE, ROLE, HEAD_YN, USE_YN, CREATED_BY) VALUES
(1, 'admin', '$2a$10$XVOOa0vdyud2/ZaoVFNPxOnsj/p6P7l0NIbOPJCW7zFOQo53HOA06', '관리자', 'ROOT', 'T104', 'ADMIN', 'Y', 'Y', 'admin');

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
-- CATEGORY_CODE: 고유 코드, OWNER_USERNAME: 소유자
-- ============================================
INSERT INTO TB_CATEGORY (CATEGORY_ID, CATEGORY_CODE, CATEGORY_NAME, CATEGORY_COLOR, OWNER_USERNAME, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(1, 'DEV', '개발', '#3B82F6', 'admin', 1, 'Y', 'admin'),
(2, 'PLAN', '기획', '#8B5CF6', 'admin', 2, 'Y', 'admin'),
(3, 'DESIGN', '디자인', '#EC4899', 'admin', 3, 'Y', 'admin'),
(4, 'OPS', '운영', '#10B981', 'admin', 4, 'Y', 'admin'),
(5, 'ETC', '기타', '#6B7280', 'admin', 5, 'Y', 'admin');

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
-- REQUEST_DATE/DUE_DATE: 간트 차트용 날짜
-- ============================================
INSERT INTO TB_ITEM (ITEM_ID, BOARD_ID, CONTENT, DESCRIPTION, STATUS, PRIORITY, CATEGORY_ID, GROUP_ID, ASSIGNEE_USERNAME, REQUEST_DATE, DUE_DATE, CREATED_BY) VALUES
(1, 1, '프로젝트 초기 설정', '## 목표\n프로젝트 환경 구성 및 초기 설정 완료\n\n## 체크리스트\n- [x] Git 저장소 생성\n- [x] Docker 환경 구성\n- [x] 개발 환경 세팅', 'COMPLETED', 'HIGH', 1, 3, 'admin', '2025-01-01', '2025-01-07', 'admin'),
(2, 1, 'ERD 설계 및 검토', '## 설계 범위\n- 사용자/부서/그룹 관리\n- 보드 및 아이템 관리\n- 동적 속성 시스템\n\n## 검토 사항\n- 정규화 수준\n- 인덱스 전략', 'COMPLETED', 'HIGH', 1, 3, 'admin', '2025-01-08', '2025-01-15', 'admin'),
(3, 1, 'API 명세서 작성', '## REST API 설계\n\n### 인증 API\n- POST /api/auth/login\n- POST /api/auth/logout\n\n### 업무 API\n- GET /api/boards/{id}/items\n- POST /api/boards/{id}/items', 'IN_PROGRESS', 'NORMAL', 1, 3, 'admin', '2025-01-10', '2025-01-20', 'admin'),
(4, 1, '프론트엔드 레이아웃 구현', NULL, 'NOT_STARTED', 'NORMAL', 1, 3, 'admin', '2025-01-15', '2025-01-25', 'admin'),
(5, 1, '로그인 기능 개발', NULL, 'NOT_STARTED', 'HIGH', 1, 3, 'admin', '2025-01-18', '2025-01-28', 'admin');

-- ============================================
-- 9. 글로벌 속성 정의 (시스템 제공)
-- OWNER_TYPE: GLOBAL = 시스템 전역 속성
-- ============================================

-- 시작일 속성
INSERT INTO TB_PROPERTY_DEF (
    BOARD_ID, OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE,
    PROPERTY_NAME, PROPERTY_TYPE, VISIBLE_YN, USE_YN, SORT_ORDER,
    CREATED_BY
) VALUES (
    NULL, 'GLOBAL', NULL, NULL,
    '시작일', 'DATE', 'Y', 'Y', 1,
    'system'
);

-- 완료일 속성
INSERT INTO TB_PROPERTY_DEF (
    BOARD_ID, OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE,
    PROPERTY_NAME, PROPERTY_TYPE, VISIBLE_YN, USE_YN, SORT_ORDER,
    CREATED_BY
) VALUES (
    NULL, 'GLOBAL', NULL, NULL,
    '완료일', 'DATE', 'Y', 'Y', 2,
    'system'
);

-- 난이도 속성
INSERT INTO TB_PROPERTY_DEF (
    BOARD_ID, OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE,
    PROPERTY_NAME, PROPERTY_TYPE, VISIBLE_YN, USE_YN, SORT_ORDER,
    CREATED_BY
) VALUES (
    NULL, 'GLOBAL', NULL, NULL,
    '난이도', 'SELECT', 'Y', 'Y', 3,
    'system'
);

-- 범위변경 속성
INSERT INTO TB_PROPERTY_DEF (
    BOARD_ID, OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE,
    PROPERTY_NAME, PROPERTY_TYPE, VISIBLE_YN, USE_YN, SORT_ORDER,
    CREATED_BY
) VALUES (
    NULL, 'GLOBAL', NULL, NULL,
    '범위변경', 'SELECT', 'Y', 'Y', 4,
    'system'
);

-- 리스크대응 속성
INSERT INTO TB_PROPERTY_DEF (
    BOARD_ID, OWNER_TYPE, OWNER_USERNAME, OWNER_DEPT_CODE,
    PROPERTY_NAME, PROPERTY_TYPE, VISIBLE_YN, USE_YN, SORT_ORDER,
    CREATED_BY
) VALUES (
    NULL, 'GLOBAL', NULL, NULL,
    '리스크대응', 'SELECT', 'Y', 'Y', 5,
    'system'
);

-- ============================================
-- 10. 글로벌 속성 옵션
-- ============================================

-- 난이도 옵션
INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'Low (반복작업)', '#22C55E', 1, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '난이도' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'Medium (일반)', '#3B82F6', 2, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '난이도' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'High (높음)', '#F59E0B', 3, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '난이도' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'Extreme (어려움)', '#EF4444', 4, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '난이도' AND OWNER_TYPE = 'GLOBAL';

-- 범위변경 옵션
INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'None (변경없음)', '#6B7280', 1, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '범위변경' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'Minor (20%이내)', '#22C55E', 2, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '범위변경' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'Major (30%이상)', '#F59E0B', 3, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '범위변경' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'Chaos (엎어짐)', '#EF4444', 4, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '범위변경' AND OWNER_TYPE = 'GLOBAL';

-- 리스크대응 옵션
INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'None (이슈없음)', '#6B7280', 1, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '리스크대응' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'Mitigated (이슈해결)', '#3B82F6', 2, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '리스크대응' AND OWNER_TYPE = 'GLOBAL';

INSERT INTO TB_PROPERTY_OPTION (PROPERTY_ID, OPTION_LABEL, COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT PROPERTY_ID, 'Critical (실패복구)', '#EF4444', 3, 'Y', 'system'
FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '리스크대응' AND OWNER_TYPE = 'GLOBAL';

-- ============================================
-- 11. SMTP 기본 설정 데이터
-- CONFIG_VALUE_ENCRYPTED: 암호화된 값 (비밀번호 등)
-- ============================================
INSERT INTO TB_SYSTEM_CONFIG (CONFIG_GROUP, CONFIG_KEY, CONFIG_VALUE, DESCRIPTION, CREATED_BY) VALUES
('SMTP', 'HOST', 'mail.sns-at.co.kr', 'SMTP 서버 주소', 'system'),
('SMTP', 'PORT', '587', 'SMTP 포트', 'system'),
('SMTP', 'SECURITY_TYPE', 'STARTTLS', '보안 유형 (STARTTLS, TLS, NONE)', 'system'),
('SMTP', 'USERNAME', 'sns@sns-at.co.kr', 'SMTP 계정', 'system'),
('SMTP', 'FROM_ADDRESS', 'TaskFlow <sns@sns-at.co.kr>', '발신자 주소', 'system'),
('SMTP', 'ADMIN_EMAIL', 'admin@sns-at.co.kr', '관리자 이메일', 'system')
ON DUPLICATE KEY UPDATE UPDATED_AT = CURRENT_TIMESTAMP;

-- PASSWORD는 암호화되어 저장되므로 별도 처리 필요 (애플리케이션에서 설정)
INSERT INTO TB_SYSTEM_CONFIG (CONFIG_GROUP, CONFIG_KEY, CONFIG_VALUE_ENCRYPTED, DESCRIPTION, CREATED_BY) VALUES
('SMTP', 'PASSWORD', NULL, 'SMTP 비밀번호 (암호화)', 'system')
ON DUPLICATE KEY UPDATE UPDATED_AT = CURRENT_TIMESTAMP;

-- ============================================
-- 12. 기본 캘린더 생성
-- 관리자용 기본 캘린더
-- ============================================
INSERT INTO TB_CALENDAR (CALENDAR_ID, OWNER_USERNAME, NAME, DESCRIPTION, COLOR, IS_DEFAULT, SORT_ORDER, USE_YN, CREATED_BY) VALUES
(1, 'admin', '내 캘린더', '기본 캘린더', '#3B82F6', 'Y', 0, 'Y', 'admin'),
(2, 'admin', '업무', '업무 관련 일정', '#10B981', 'N', 1, 'Y', 'admin'),
(3, 'admin', '개인', '개인 일정', '#8B5CF6', 'N', 2, 'Y', 'admin');

-- ============================================
-- 13. 캘린더 날짜 정보 (공휴일)
-- 2025년 ~ 2026년 한국 공휴일
-- LUNAR_MONTH/LUNAR_DAY: 음력 날짜
-- ============================================

-- 2025년 공휴일
INSERT INTO TB_CALENDAR_DATE (DATE_VALUE, LUNAR_YEAR, LUNAR_MONTH, LUNAR_DAY, IS_HOLIDAY, HOLIDAY_NAME, CREATED_BY) VALUES
-- 1월
('2025-01-01', 2024, 12, 2, 'Y', '새해 첫날', 'system'),
('2025-01-28', 2024, 12, 29, 'Y', '설날 연휴', 'system'),
('2025-01-29', 2025, 1, 1, 'Y', '설날', 'system'),
('2025-01-30', 2025, 1, 2, 'Y', '설날 연휴', 'system'),
-- 3월
('2025-03-01', 2025, 2, 2, 'Y', '삼일절', 'system'),
-- 5월
('2025-05-05', 2025, 4, 8, 'Y', '어린이날', 'system'),
('2025-05-06', 2025, 4, 9, 'Y', '부처님오신날', 'system'),
-- 6월
('2025-06-06', 2025, 5, 11, 'Y', '현충일', 'system'),
-- 8월
('2025-08-15', 2025, 6, 22, 'Y', '광복절', 'system'),
-- 10월 (추석)
('2025-10-05', 2025, 8, 14, 'Y', '추석 연휴', 'system'),
('2025-10-06', 2025, 8, 15, 'Y', '추석', 'system'),
('2025-10-07', 2025, 8, 16, 'Y', '추석 연휴', 'system'),
('2025-10-03', 2025, 8, 12, 'Y', '개천절', 'system'),
('2025-10-09', 2025, 8, 18, 'Y', '한글날', 'system'),
-- 12월
('2025-12-25', 2025, 11, 5, 'Y', '성탄절', 'system'),

-- 2026년 공휴일
('2026-01-01', 2025, 11, 12, 'Y', '새해 첫날', 'system'),
('2026-02-16', 2025, 12, 29, 'Y', '설날 연휴', 'system'),
('2026-02-17', 2026, 1, 1, 'Y', '설날', 'system'),
('2026-02-18', 2026, 1, 2, 'Y', '설날 연휴', 'system'),
('2026-03-01', 2026, 1, 12, 'Y', '삼일절', 'system'),
('2026-05-05', 2026, 3, 19, 'Y', '어린이날', 'system'),
('2026-05-24', 2026, 4, 8, 'Y', '부처님오신날', 'system'),
('2026-06-06', 2026, 4, 21, 'Y', '현충일', 'system'),
('2026-08-15', 2026, 7, 3, 'Y', '광복절', 'system'),
('2026-09-24', 2026, 8, 14, 'Y', '추석 연휴', 'system'),
('2026-09-25', 2026, 8, 15, 'Y', '추석', 'system'),
('2026-09-26', 2026, 8, 16, 'Y', '추석 연휴', 'system'),
('2026-10-03', 2026, 8, 22, 'Y', '개천절', 'system'),
('2026-10-09', 2026, 8, 28, 'Y', '한글날', 'system'),
('2026-12-25', 2026, 11, 15, 'Y', '성탄절', 'system');

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
ALTER TABLE TB_CALENDAR AUTO_INCREMENT = 100;
ALTER TABLE TB_EVENT AUTO_INCREMENT = 100;
