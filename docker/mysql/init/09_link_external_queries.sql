-- ============================================
-- 09_link_external_queries.sql
-- 속성에 외부 쿼리 연동 설정
-- ============================================

-- 1. 현재 상태 확인 (실행 전 확인용)
-- SELECT
--     p.PROPERTY_ID,
--     p.PROPERTY_NAME,
--     p.PROPERTY_TYPE,
--     p.OWNER_TYPE,
--     p.DATA_SOURCE_TYPE,
--     p.EXTERNAL_QUERY_ID,
--     eq.QUERY_NAME
-- FROM TB_PROPERTY_DEF p
-- LEFT JOIN TB_EXTERNAL_QUERY eq ON p.EXTERNAL_QUERY_ID = eq.QUERY_ID
-- WHERE p.USE_YN = 'Y'
-- ORDER BY p.PROPERTY_ID;

-- 2. 외부 쿼리 목록 확인
-- SELECT QUERY_ID, QUERY_CODE, QUERY_NAME, OWNER_TYPE FROM TB_EXTERNAL_QUERY WHERE USE_YN = 'Y';

-- ============================================
-- 3. 속성에 외부 쿼리 연동 설정
-- ============================================

-- "외부 쿼리" 또는 "외부쿼리" 이름을 가진 속성에 글로벌 외부 쿼리 연동
-- (YEOJU_EO_CD, QUERY_ID = 1로 가정, 실제 ID 확인 후 수정 필요)
UPDATE TB_PROPERTY_DEF
SET
    DATA_SOURCE_TYPE = 'EXTERNAL',
    EXTERNAL_QUERY_ID = (
        SELECT QUERY_ID FROM TB_EXTERNAL_QUERY
        WHERE QUERY_CODE = 'YEOJU_EO_CD' AND USE_YN = 'Y'
        LIMIT 1
    ),
    UPDATED_AT = CURRENT_TIMESTAMP
WHERE PROPERTY_NAME LIKE '%외부%쿼리%'
  AND PROPERTY_TYPE IN ('SELECT', 'MULTI_SELECT')
  AND USE_YN = 'Y'
  AND (DATA_SOURCE_TYPE IS NULL OR DATA_SOURCE_TYPE = 'INTERNAL');

-- 또는 특정 속성 ID로 직접 설정 (정확한 ID 확인 후 사용)
-- UPDATE TB_PROPERTY_DEF
-- SET
--     DATA_SOURCE_TYPE = 'EXTERNAL',
--     EXTERNAL_QUERY_ID = 1,
--     UPDATED_AT = CURRENT_TIMESTAMP
-- WHERE PROPERTY_ID IN (속성ID1, 속성ID2, 속성ID3);

-- ============================================
-- 4. 설정 결과 확인
-- ============================================
-- SELECT
--     p.PROPERTY_ID,
--     p.PROPERTY_NAME,
--     p.DATA_SOURCE_TYPE,
--     p.EXTERNAL_QUERY_ID,
--     eq.QUERY_NAME,
--     eq.QUERY_CODE
-- FROM TB_PROPERTY_DEF p
-- LEFT JOIN TB_EXTERNAL_QUERY eq ON p.EXTERNAL_QUERY_ID = eq.QUERY_ID
-- WHERE p.DATA_SOURCE_TYPE = 'EXTERNAL'
-- ORDER BY p.PROPERTY_ID;
