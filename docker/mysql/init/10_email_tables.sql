-- ============================================
-- 10_email_tables.sql
-- 메일 발송 기능 테이블 생성
-- ============================================

-- ============================================
-- 1. TB_SYSTEM_CONFIG (시스템 설정)
-- ============================================
CREATE TABLE IF NOT EXISTS TB_SYSTEM_CONFIG (
    CONFIG_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '설정 ID',
    CONFIG_GROUP VARCHAR(50) NOT NULL COMMENT '설정 그룹 (SMTP, SYSTEM 등)',
    CONFIG_KEY VARCHAR(100) NOT NULL COMMENT '설정 키',
    CONFIG_VALUE VARCHAR(500) NULL COMMENT '설정 값',
    CONFIG_VALUE_ENCRYPTED VARCHAR(500) NULL COMMENT '암호화된 설정 값 (비밀번호 등)',
    DESCRIPTION VARCHAR(200) NULL COMMENT '설정 설명',
    USE_YN CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    CREATED_BY VARCHAR(50) NOT NULL COMMENT '생성자 USERNAME',
    UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    UPDATED_BY VARCHAR(50) NULL COMMENT '수정자 USERNAME',
    PRIMARY KEY (CONFIG_ID),
    UNIQUE KEY UK_SYSTEM_CONFIG_GROUP_KEY (CONFIG_GROUP, CONFIG_KEY),
    INDEX IDX_SYSTEM_CONFIG_GROUP (CONFIG_GROUP)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='시스템 설정';

-- ============================================
-- 2. TB_EMAIL_LOG (메일 발송 이력)
-- ============================================
CREATE TABLE IF NOT EXISTS TB_EMAIL_LOG (
    EMAIL_LOG_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '메일 로그 ID',
    EMAIL_TYPE VARCHAR(50) NOT NULL COMMENT '발송 유형 (TASK_ASSIGN, TASK_TRANSFER, TASK_SHARE, DUE_DATE_ALERT, MANUAL)',
    SUBJECT VARCHAR(500) NOT NULL COMMENT '메일 제목',
    RECIPIENT_EMAIL VARCHAR(200) NOT NULL COMMENT '수신자 이메일',
    RECIPIENT_NAME VARCHAR(100) NULL COMMENT '수신자 이름',
    RECIPIENT_USERNAME VARCHAR(50) NULL COMMENT '수신자 USERNAME (시스템 사용자)',
    CONTENT TEXT NOT NULL COMMENT '메일 본문',
    STATUS VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '발송 상태 (PENDING, SENT, FAILED)',
    ERROR_MESSAGE VARCHAR(500) NULL COMMENT '실패 시 에러 메시지',
    RELATED_TYPE VARCHAR(50) NULL COMMENT '관련 대상 유형 (ITEM, BOARD)',
    RELATED_ID BIGINT NULL COMMENT '관련 대상 ID',
    SENT_AT DATETIME NULL COMMENT '발송일시',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    CREATED_BY VARCHAR(50) NOT NULL COMMENT '생성자 USERNAME',
    PRIMARY KEY (EMAIL_LOG_ID),
    INDEX IDX_EMAIL_LOG_TYPE (EMAIL_TYPE),
    INDEX IDX_EMAIL_LOG_STATUS (STATUS),
    INDEX IDX_EMAIL_LOG_RECIPIENT (RECIPIENT_EMAIL),
    INDEX IDX_EMAIL_LOG_CREATED (CREATED_AT),
    INDEX IDX_EMAIL_LOG_RELATED (RELATED_TYPE, RELATED_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='메일 발송 이력';

-- ============================================
-- 3. SMTP 기본 설정 데이터 삽입
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
-- 4. TB_USER에 EMAIL 컬럼 추가 (없는 경우에만)
-- ============================================
-- 컬럼 존재 여부 확인 후 추가
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'TB_USER'
    AND COLUMN_NAME = 'EMAIL'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE TB_USER ADD COLUMN EMAIL VARCHAR(200) NULL COMMENT ''이메일 주소'' AFTER PHONE',
    'SELECT ''EMAIL column already exists'''
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 5. 확인용 쿼리 (주석 처리)
-- ============================================
-- SELECT * FROM TB_SYSTEM_CONFIG WHERE CONFIG_GROUP = 'SMTP';
-- DESCRIBE TB_USER;
-- DESCRIBE TB_EMAIL_LOG;
