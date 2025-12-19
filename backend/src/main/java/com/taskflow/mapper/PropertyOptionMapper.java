package com.taskflow.mapper;

import com.taskflow.domain.PropertyOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 속성 옵션 Mapper 인터페이스
 */
@Mapper
public interface PropertyOptionMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 옵션 ID로 조회
     *
     * @param optionId 옵션 ID
     * @return 옵션 (Optional)
     */
    Optional<PropertyOption> findById(@Param("optionId") Long optionId);

    /**
     * 속성별 옵션 목록 조회
     *
     * @param propertyId 속성 정의 ID
     * @param useYn      사용 여부 필터 (null = 전체)
     * @return 옵션 목록
     */
    List<PropertyOption> findByPropertyId(@Param("propertyId") Long propertyId, @Param("useYn") String useYn);

    /**
     * 속성 내 옵션명으로 조회
     *
     * @param propertyId  속성 정의 ID
     * @param optionLabel 옵션명 (OPTION_LABEL)
     * @return 옵션 (Optional)
     */
    Optional<PropertyOption> findByPropertyIdAndLabel(@Param("propertyId") Long propertyId,
                                                       @Param("optionLabel") String optionLabel);

    /**
     * 속성 내 옵션명 중복 확인
     *
     * @param propertyId  속성 정의 ID
     * @param optionLabel 옵션명 (OPTION_LABEL)
     * @return 중복 여부
     */
    boolean existsByPropertyIdAndLabel(@Param("propertyId") Long propertyId,
                                        @Param("optionLabel") String optionLabel);

    /**
     * 속성 내 옵션명 중복 확인 (자신 제외)
     *
     * @param propertyId  속성 정의 ID
     * @param optionLabel 옵션명 (OPTION_LABEL)
     * @param optionId    제외할 옵션 ID
     * @return 중복 여부
     */
    boolean existsByPropertyIdAndLabelAndIdNot(@Param("propertyId") Long propertyId,
                                                @Param("optionLabel") String optionLabel,
                                                @Param("optionId") Long optionId);

    /**
     * 속성 내 최대 정렬 순서 조회
     *
     * @param propertyId 속성 정의 ID
     * @return 최대 정렬 순서
     */
    Integer getMaxSortOrder(@Param("propertyId") Long propertyId);

    /**
     * 옵션이 아이템에 사용 중인지 확인
     *
     * @param optionId 옵션 ID
     * @return 사용 여부
     */
    boolean isOptionInUse(@Param("optionId") Long optionId);

    /**
     * 옵션 사용 횟수 조회
     *
     * @param optionId 옵션 ID
     * @return 사용 횟수
     */
    int getUsageCount(@Param("optionId") Long optionId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 옵션 등록
     *
     * @param option 옵션 엔티티
     * @return 영향받은 행 수
     */
    int insert(PropertyOption option);

    /**
     * 옵션 수정
     *
     * @param option 옵션 엔티티
     * @return 영향받은 행 수
     */
    int update(PropertyOption option);

    /**
     * 옵션 삭제
     *
     * @param optionId 옵션 ID
     * @return 영향받은 행 수
     */
    int delete(@Param("optionId") Long optionId);

    /**
     * 속성의 모든 옵션 삭제
     *
     * @param propertyId 속성 정의 ID
     * @return 영향받은 행 수
     */
    int deleteByPropertyId(@Param("propertyId") Long propertyId);
}
