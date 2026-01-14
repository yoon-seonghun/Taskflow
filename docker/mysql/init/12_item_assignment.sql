-- =====================================================
-- 업무 담당자 배정 및 권한 관리 마이그레이션
-- Version: 1.0
-- Date: 2024-01-15
-- =====================================================

-- -----------------------------------------------------
-- 1. TB_ITEM_SHARE 테이블 컬럼 추가
-- -----------------------------------------------------
-- 기존 PERMISSION 컬럼을 권한 수준으로 사용 (VIEW, EDIT, FULL)
-- 새로 추가: SHARE_TYPE, ASSIGNED_BY, ASSIGNED_AT

-- SHARE_TYPE 컬럼 추가 (공유 유형: SHARE, ASSIGN)
ALTER TABLE TB_ITEM_SHARE
ADD COLUMN SHARE_TYPE VARCHAR(20) NOT NULL DEFAULT 'SHARE' COMMENT '공유 유형 (SHARE: 공유, ASSIGN: 배당)' AFTER USERNAME;

-- ASSIGNED_BY 컬럼 추가 (배당자 USERNAME)
ALTER TABLE TB_ITEM_SHARE
ADD COLUMN ASSIGNED_BY VARCHAR(50) NULL COMMENT '배당자 USERNAME (ASSIGN일 경우)' AFTER PERMISSION;

-- ASSIGNED_AT 컬럼 추가 (배당 일시)
ALTER TABLE TB_ITEM_SHARE
ADD COLUMN ASSIGNED_AT DATETIME NULL COMMENT '배당 일시' AFTER ASSIGNED_BY;

-- 인덱스 추가
CREATE INDEX IDX_ITEM_SHARE_TYPE ON TB_ITEM_SHARE(SHARE_TYPE);
CREATE INDEX IDX_ITEM_SHARE_ASSIGNED_BY ON TB_ITEM_SHARE(ASSIGNED_BY);

-- -----------------------------------------------------
-- 2. TB_NOTIFICATION 테이블 생성
-- -----------------------------------------------------

CREATE TABLE IF NOT EXISTS TB_NOTIFICATION (
    NOTIFICATION_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    USERNAME VARCHAR(50) NOT NULL COMMENT '수신자 USERNAME',
    NOTIFICATION_TYPE VARCHAR(50) NOT NULL COMMENT '알림 유형 (ITEM_ASSIGNED, ITEM_SHARED, ITEM_UPDATED, ITEM_COMPLETED)',
    TITLE VARCHAR(200) NOT NULL COMMENT '알림 제목',
    MESSAGE TEXT NULL COMMENT '알림 내용',
    RELATED_TYPE VARCHAR(50) NULL COMMENT '관련 대상 유형 (ITEM, BOARD 등)',
    RELATED_ID BIGINT NULL COMMENT '관련 대상 ID',
    RELATED_URL VARCHAR(500) NULL COMMENT '관련 링크 URL',
    IS_READ TINYINT(1) NOT NULL DEFAULT 0 COMMENT '읽음 여부',
    READ_AT DATETIME NULL COMMENT '읽은 시간',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY VARCHAR(50) NOT NULL COMMENT '발신자 USERNAME',

    INDEX IDX_NOTIFICATION_USER (USERNAME, IS_READ, CREATED_AT DESC),
    INDEX IDX_NOTIFICATION_RELATED (RELATED_TYPE, RELATED_ID),
    INDEX IDX_NOTIFICATION_TYPE (NOTIFICATION_TYPE),
    INDEX IDX_NOTIFICATION_CREATED (CREATED_AT DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림';

-- -----------------------------------------------------
-- 3. 코드 데이터 추가 (TB_CODE)
-- -----------------------------------------------------

-- 공유 유형 코드
INSERT IGNORE INTO TB_CODE (GROUP_CODE, CODE, CODE_NAME, CODE_ORDER, USE_YN, DESCRIPTION, CREATED_BY)
VALUES
    ('SHARE_TYPE', 'SHARE', '공유', 1, 'Y', '일반 공유', 'system'),
    ('SHARE_TYPE', 'ASSIGN', '배당', 2, 'Y', '담당자 배정을 통한 공유', 'system');

-- 권한 수준 코드
INSERT IGNORE INTO TB_CODE (GROUP_CODE, CODE, CODE_NAME, CODE_ORDER, USE_YN, DESCRIPTION, CREATED_BY)
VALUES
    ('PERMISSION_LEVEL', 'VIEW', '조회', 1, 'Y', '읽기만 가능', 'system'),
    ('PERMISSION_LEVEL', 'EDIT', '편집', 2, 'Y', '사용자 속성 편집 가능', 'system'),
    ('PERMISSION_LEVEL', 'FULL', '전체', 3, 'Y', '기본 속성 편집 가능 (제목 제외)', 'system');

-- 알림 유형 코드
INSERT IGNORE INTO TB_CODE (GROUP_CODE, CODE, CODE_NAME, CODE_ORDER, USE_YN, DESCRIPTION, CREATED_BY)
VALUES
    ('NOTIFICATION_TYPE', 'ITEM_ASSIGNED', '업무 배당', 1, 'Y', '업무가 배당되었을 때', 'system'),
    ('NOTIFICATION_TYPE', 'ITEM_SHARED', '업무 공유', 2, 'Y', '업무가 공유되었을 때', 'system'),
    ('NOTIFICATION_TYPE', 'ITEM_UPDATED', '업무 수정', 3, 'Y', '배당/공유 업무가 수정되었을 때', 'system'),
    ('NOTIFICATION_TYPE', 'ITEM_COMPLETED', '업무 완료', 4, 'Y', '배당한 업무가 완료되었을 때', 'system');
