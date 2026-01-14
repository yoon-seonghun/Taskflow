-- ============================================
-- TB_ITEM에 SORT_ORDER 컬럼 추가
-- 업무 아이템 순서 관리 기능
-- ============================================

-- SORT_ORDER 컬럼 추가
ALTER TABLE TB_ITEM
ADD COLUMN SORT_ORDER INT NOT NULL DEFAULT 0 COMMENT '정렬 순서'
AFTER TRANSFERRED_AT;

-- 인덱스 추가
CREATE INDEX IDX_ITEM_SORT ON TB_ITEM (BOARD_ID, SORT_ORDER);

-- 기존 아이템들의 SORT_ORDER 초기화 (생성일 기준)
UPDATE TB_ITEM t1
JOIN (
    SELECT ITEM_ID,
           ROW_NUMBER() OVER (PARTITION BY BOARD_ID ORDER BY CREATED_AT ASC) - 1 as new_order
    FROM TB_ITEM
) t2 ON t1.ITEM_ID = t2.ITEM_ID
SET t1.SORT_ORDER = t2.new_order;
