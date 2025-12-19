package com.taskflow.mapper;

import com.taskflow.domain.ItemProperty;
import com.taskflow.domain.ItemPropertyMulti;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 아이템 속성값 Mapper 인터페이스
 */
@Mapper
public interface ItemPropertyMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 아이템 속성값 ID로 조회
     *
     * @param itemPropertyId 아이템 속성값 ID
     * @return 아이템 속성값 (Optional)
     */
    Optional<ItemProperty> findById(@Param("itemPropertyId") Long itemPropertyId);

    /**
     * 아이템별 속성값 목록 조회
     *
     * @param itemId 아이템 ID
     * @return 속성값 목록
     */
    List<ItemProperty> findByItemId(@Param("itemId") Long itemId);

    /**
     * 여러 아이템의 속성값 목록 일괄 조회 (N+1 쿼리 최적화)
     *
     * @param itemIds 아이템 ID 목록
     * @return 속성값 목록
     */
    List<ItemProperty> findByItemIds(@Param("itemIds") List<Long> itemIds);

    /**
     * 아이템의 특정 속성값 조회
     *
     * @param itemId     아이템 ID
     * @param propertyId 속성 정의 ID
     * @return 속성값 (Optional)
     */
    Optional<ItemProperty> findByItemIdAndPropertyId(@Param("itemId") Long itemId,
                                                      @Param("propertyId") Long propertyId);

    /**
     * 아이템의 다중선택 속성값 목록 조회
     *
     * @param itemId     아이템 ID
     * @param propertyId 속성 정의 ID
     * @return 다중선택 값 목록
     */
    List<ItemPropertyMulti> findMultiByItemIdAndPropertyId(@Param("itemId") Long itemId,
                                                            @Param("propertyId") Long propertyId);

    /**
     * 속성별 아이템 속성값 목록 조회
     *
     * @param propertyId 속성 정의 ID
     * @return 속성값 목록
     */
    List<ItemProperty> findByPropertyId(@Param("propertyId") Long propertyId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 아이템 속성값 등록
     *
     * @param itemProperty 속성값 엔티티
     * @return 영향받은 행 수
     */
    int insert(ItemProperty itemProperty);

    /**
     * 아이템 속성값 수정
     *
     * @param itemProperty 속성값 엔티티
     * @return 영향받은 행 수
     */
    int update(ItemProperty itemProperty);

    /**
     * 아이템 속성값 저장 (UPSERT)
     *
     * @param itemProperty 속성값 엔티티
     * @return 영향받은 행 수
     */
    int upsert(ItemProperty itemProperty);

    /**
     * 아이템 속성값 삭제
     *
     * @param itemPropertyId 속성값 ID
     * @return 영향받은 행 수
     */
    int delete(@Param("itemPropertyId") Long itemPropertyId);

    /**
     * 아이템의 모든 속성값 삭제
     *
     * @param itemId 아이템 ID
     * @return 영향받은 행 수
     */
    int deleteByItemId(@Param("itemId") Long itemId);

    /**
     * 아이템의 특정 속성값 삭제
     *
     * @param itemId     아이템 ID
     * @param propertyId 속성 정의 ID
     * @return 영향받은 행 수
     */
    int deleteByItemIdAndPropertyId(@Param("itemId") Long itemId,
                                     @Param("propertyId") Long propertyId);

    // =============================================
    // 다중선택 속성값 (MULTI_SELECT)
    // =============================================

    /**
     * 다중선택 속성값 등록
     *
     * @param multi 다중선택 속성값 엔티티
     * @return 영향받은 행 수
     */
    int insertMulti(ItemPropertyMulti multi);

    /**
     * 아이템의 다중선택 속성값 삭제
     *
     * @param itemId     아이템 ID
     * @param propertyId 속성 정의 ID
     * @return 영향받은 행 수
     */
    int deleteMultiByItemIdAndPropertyId(@Param("itemId") Long itemId,
                                          @Param("propertyId") Long propertyId);

    /**
     * 아이템의 모든 다중선택 속성값 삭제
     *
     * @param itemId 아이템 ID
     * @return 영향받은 행 수
     */
    int deleteMultiByItemId(@Param("itemId") Long itemId);
}
