-- ============================================
-- TaskFlow Database Migration Script
-- 논리 삭제 컬럼 추가 (DELETED_AT, DELETED_BY)
-- 적용 대상: 기존 운영 DB
-- ============================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- TB_DEPARTMENT - 부서
-- ============================================
ALTER TABLE TB_DEPARTMENT
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_POSITION - 직급
-- ============================================
ALTER TABLE TB_POSITION
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_USER - 사용자
-- ============================================
ALTER TABLE TB_USER
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_GROUP - 그룹
-- ============================================
ALTER TABLE TB_GROUP
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_USER_GROUP - 사용자-그룹 매핑
-- ============================================
ALTER TABLE TB_USER_GROUP
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER CREATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_CATEGORY - 카테고리
-- ============================================
ALTER TABLE TB_CATEGORY
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_CATEGORY_SHARE - 카테고리 공유
-- ============================================
ALTER TABLE TB_CATEGORY_SHARE
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_CATEGORY_PROPERTY - 카테고리 속성 매핑
-- ============================================
ALTER TABLE TB_CATEGORY_PROPERTY
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER CREATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_BOARD_CATEGORY - 보드-카테고리 매핑
-- ============================================
ALTER TABLE TB_BOARD_CATEGORY
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER CREATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_BOARD_PROPERTY - 보드 속성 설정
-- ============================================
ALTER TABLE TB_BOARD_PROPERTY
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_BOARD - 보드
-- ============================================
ALTER TABLE TB_BOARD
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_BOARD_SHARE - 보드 공유
-- ============================================
ALTER TABLE TB_BOARD_SHARE
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER CREATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_PROPERTY_DEF - 속성 정의
-- ============================================
ALTER TABLE TB_PROPERTY_DEF
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_PROPERTY_OPTION - 속성 옵션
-- ============================================
ALTER TABLE TB_PROPERTY_OPTION
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_ITEM_PROPERTY - 아이템 속성값
-- ============================================
ALTER TABLE TB_ITEM_PROPERTY
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_ITEM_PROPERTY_MULTI - 다중선택 속성값
-- ============================================
ALTER TABLE TB_ITEM_PROPERTY_MULTI
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER CREATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_ITEM_SHARE - 업무 공유
-- ============================================
ALTER TABLE TB_ITEM_SHARE
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_COMMENT - 댓글
-- ============================================
ALTER TABLE TB_COMMENT
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_TASK_TEMPLATE - 작업 템플릿
-- ============================================
ALTER TABLE TB_TASK_TEMPLATE
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_FILE - 파일 메타데이터
-- ============================================
ALTER TABLE TB_FILE
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER UPDATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- TB_ITEM_SCORE - 업무 성과 점수
-- ============================================
ALTER TABLE TB_ITEM_SCORE
    ADD COLUMN DELETED_AT DATETIME NULL COMMENT '삭제일시 (논리삭제)' AFTER CALCULATED_BY,
    ADD COLUMN DELETED_BY VARCHAR(50) NULL COMMENT '삭제자 USERNAME' AFTER DELETED_AT;

-- ============================================
-- 제외된 테이블:
-- - TB_ITEM: 이미 DELETED_AT, DELETED_BY 컬럼 존재
-- - TB_AUDIT_LOG: 감사 로그는 논리삭제 대상 아님
-- ============================================
