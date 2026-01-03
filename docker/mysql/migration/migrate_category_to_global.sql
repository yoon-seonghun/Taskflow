-- ============================================
-- 카테고리 전역 테이블 마이그레이션 스크립트
-- TB_PROPERTY_DEF/TB_PROPERTY_OPTION → TB_CATEGORY
--
-- 실행 순서:
-- 1. 백업 생성
-- 2. TB_CATEGORY 테이블 생성
-- 3. 기존 카테고리 데이터 마이그레이션
-- 4. TB_ITEM.CATEGORY_ID FK 변경
-- 5. 기존 카테고리 PropertyDef/Option 정리 (선택)
-- ============================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- 1. 백업 생성 (권장)
-- ============================================
-- CREATE TABLE TB_PROPERTY_DEF_BACKUP AS SELECT * FROM TB_PROPERTY_DEF;
-- CREATE TABLE TB_PROPERTY_OPTION_BACKUP AS SELECT * FROM TB_PROPERTY_OPTION;
-- CREATE TABLE TB_ITEM_BACKUP AS SELECT * FROM TB_ITEM;

-- ============================================
-- 2. TB_CATEGORY 테이블 생성
-- ============================================
CREATE TABLE IF NOT EXISTS TB_CATEGORY (
    CATEGORY_ID BIGINT NOT NULL AUTO_INCREMENT COMMENT '카테고리 ID',
    CATEGORY_NAME VARCHAR(100) NOT NULL COMMENT '카테고리명',
    COLOR VARCHAR(20) NULL DEFAULT '#6B7280' COMMENT '색상',
    SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '정렬 순서',
    USE_YN CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '사용 여부',
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
    CREATED_BY VARCHAR(50) NOT NULL COMMENT '생성자 USERNAME',
    UPDATED_AT DATETIME NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
    UPDATED_BY VARCHAR(50) NULL COMMENT '수정자 USERNAME',
    PRIMARY KEY (CATEGORY_ID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='카테고리';

-- TB_CATEGORY 인덱스 (이미 존재하면 에러 무시)
-- MySQL 8.0에서는 CREATE INDEX IF NOT EXISTS 미지원
SET @idx1 = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'TB_CATEGORY' AND INDEX_NAME = 'IDX_CATEGORY_SORT');
SET @sql1 = IF(@idx1 = 0, 'CREATE INDEX IDX_CATEGORY_SORT ON TB_CATEGORY (SORT_ORDER)', 'SELECT 1');
PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

SET @idx2 = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'TB_CATEGORY' AND INDEX_NAME = 'IDX_CATEGORY_USE');
SET @sql2 = IF(@idx2 = 0, 'CREATE INDEX IDX_CATEGORY_USE ON TB_CATEGORY (USE_YN)', 'SELECT 1');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- ============================================
-- 3. 기존 카테고리 데이터 마이그레이션
-- 첫 번째 보드의 카테고리 옵션을 전역 카테고리로 복사
-- ============================================

-- 기존 카테고리 옵션 중 고유한 것만 삽입 (중복 제거)
INSERT INTO TB_CATEGORY (CATEGORY_NAME, COLOR, SORT_ORDER, USE_YN, CREATED_BY)
SELECT DISTINCT
    po.OPTION_LABEL AS CATEGORY_NAME,
    po.COLOR,
    po.SORT_ORDER,
    po.USE_YN,
    COALESCE(po.CREATED_BY, 'admin') AS CREATED_BY
FROM TB_PROPERTY_OPTION po
INNER JOIN TB_PROPERTY_DEF pd ON po.PROPERTY_ID = pd.PROPERTY_ID
WHERE pd.PROPERTY_NAME = '카테고리'
  AND pd.PROPERTY_TYPE = 'SELECT'
ORDER BY po.SORT_ORDER
ON DUPLICATE KEY UPDATE CATEGORY_NAME = VALUES(CATEGORY_NAME);

-- ============================================
-- 4. TB_ITEM.CATEGORY_ID 값 업데이트
-- 기존 OPTION_ID → 새 CATEGORY_ID 매핑
-- ============================================

-- 임시 매핑 테이블 생성
CREATE TEMPORARY TABLE TEMP_CATEGORY_MAPPING AS
SELECT
    po.OPTION_ID AS OLD_ID,
    c.CATEGORY_ID AS NEW_ID
FROM TB_PROPERTY_OPTION po
INNER JOIN TB_PROPERTY_DEF pd ON po.PROPERTY_ID = pd.PROPERTY_ID
INNER JOIN TB_CATEGORY c ON po.OPTION_LABEL = c.CATEGORY_NAME
WHERE pd.PROPERTY_NAME = '카테고리';

-- TB_ITEM.CATEGORY_ID 업데이트
UPDATE TB_ITEM i
INNER JOIN TEMP_CATEGORY_MAPPING m ON i.CATEGORY_ID = m.OLD_ID
SET i.CATEGORY_ID = m.NEW_ID;

-- 임시 테이블 삭제
DROP TEMPORARY TABLE IF EXISTS TEMP_CATEGORY_MAPPING;

-- ============================================
-- 5. FK 변경
-- ============================================

-- 기존 FK 삭제 (존재하는 경우)
SET @fk_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'TB_ITEM'
    AND CONSTRAINT_NAME = 'FK_ITEM_CATEGORY'
);

SET @sql = IF(@fk_exists > 0,
    'ALTER TABLE TB_ITEM DROP CONSTRAINT FK_ITEM_CATEGORY',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 새 FK 추가
ALTER TABLE TB_ITEM
    ADD CONSTRAINT FK_ITEM_CATEGORY
    FOREIGN KEY (CATEGORY_ID) REFERENCES TB_CATEGORY (CATEGORY_ID)
    ON DELETE SET NULL ON UPDATE CASCADE;

-- ============================================
-- 6. TB_ITEM.CATEGORY_ID 컬럼 주석 업데이트
-- ============================================
ALTER TABLE TB_ITEM MODIFY COLUMN CATEGORY_ID BIGINT NULL COMMENT '카테고리 ID';

-- ============================================
-- 7. 기존 카테고리 PropertyDef/Option 정리 (선택)
-- 주의: 다른 보드에서 사용 중일 수 있으므로 확인 후 실행
-- ============================================

-- 카테고리 PropertyOption 삭제
-- DELETE po FROM TB_PROPERTY_OPTION po
-- INNER JOIN TB_PROPERTY_DEF pd ON po.PROPERTY_ID = pd.PROPERTY_ID
-- WHERE pd.PROPERTY_NAME = '카테고리';

-- 카테고리 PropertyDef 삭제
-- DELETE FROM TB_PROPERTY_DEF WHERE PROPERTY_NAME = '카테고리';

-- ============================================
-- 8. AUTO_INCREMENT 설정
-- ============================================
ALTER TABLE TB_CATEGORY AUTO_INCREMENT = 100;

-- ============================================
-- 9. 검증 쿼리
-- ============================================
SELECT 'TB_CATEGORY 데이터 확인' AS CHECK_ITEM;
SELECT * FROM TB_CATEGORY;

SELECT 'TB_ITEM.CATEGORY_ID 매핑 확인' AS CHECK_ITEM;
SELECT
    i.ITEM_ID,
    i.CONTENT,
    i.CATEGORY_ID,
    c.CATEGORY_NAME
FROM TB_ITEM i
LEFT JOIN TB_CATEGORY c ON i.CATEGORY_ID = c.CATEGORY_ID
LIMIT 10;

SELECT '마이그레이션 완료' AS STATUS;
