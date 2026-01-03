package com.taskflow.service;

import com.taskflow.dto.property.*;

import java.util.List;

/**
 * 속성 정의 서비스 인터페이스
 */
public interface PropertyService {

    // =============================================
    // 속성 정의 조회
    // =============================================

    /**
     * 속성 정의 ID로 조회
     *
     * @param propertyId 속성 정의 ID
     * @return 속성 정의 응답
     */
    PropertyResponse getProperty(Long propertyId);

    /**
     * 보드별 속성 정의 목록 조회
     *
     * @param boardId 보드 ID
     * @param useYn   사용 여부 필터
     * @return 속성 정의 목록
     */
    List<PropertyResponse> getPropertiesByBoardId(Long boardId, String useYn);

    /**
     * 보드별 속성 정의 목록 조회 (캐시 사용)
     *
     * @param boardId 보드 ID
     * @return 속성 정의 목록 (활성화된 것만)
     */
    List<PropertyResponse> getCachedPropertiesByBoardId(Long boardId);

    // =============================================
    // 속성 정의 등록/수정/삭제
    // =============================================

    /**
     * 속성 정의 등록
     *
     * @param boardId   보드 ID
     * @param request   등록 요청
     * @param createdBy 생성자 USERNAME
     * @return 생성된 속성 정의 응답
     */
    PropertyResponse createProperty(Long boardId, PropertyCreateRequest request, String createdBy);

    /**
     * 속성 정의 수정
     *
     * @param propertyId 속성 정의 ID
     * @param request    수정 요청
     * @param updatedBy  수정자 USERNAME
     * @return 수정된 속성 정의 응답
     */
    PropertyResponse updateProperty(Long propertyId, PropertyUpdateRequest request, String updatedBy);

    /**
     * 속성 정의 논리 삭제
     *
     * @param propertyId 속성 정의 ID
     * @param deletedBy  삭제자 USERNAME
     */
    void deleteProperty(Long propertyId, String deletedBy);

    // =============================================
    // 옵션 조회
    // =============================================

    /**
     * 속성별 옵션 목록 조회
     *
     * @param propertyId 속성 정의 ID
     * @param useYn      사용 여부 필터
     * @return 옵션 목록
     */
    List<OptionDetailResponse> getOptionsByPropertyId(Long propertyId, String useYn);

    /**
     * 옵션 ID로 조회
     *
     * @param optionId 옵션 ID
     * @return 옵션 상세 응답
     */
    OptionDetailResponse getOption(Long optionId);

    // =============================================
    // 옵션 등록/수정/삭제
    // =============================================

    /**
     * 옵션 등록
     *
     * @param propertyId 속성 정의 ID
     * @param request    등록 요청
     * @param createdBy  생성자 USERNAME
     * @return 생성된 옵션 응답
     */
    OptionDetailResponse createOption(Long propertyId, OptionCreateRequest request, String createdBy);

    /**
     * 옵션 수정
     *
     * @param optionId  옵션 ID
     * @param request   수정 요청
     * @param updatedBy 수정자 USERNAME
     * @return 수정된 옵션 응답
     */
    OptionDetailResponse updateOption(Long optionId, OptionUpdateRequest request, String updatedBy);

    /**
     * 옵션 논리 삭제
     *
     * @param optionId  옵션 ID
     * @param deletedBy 삭제자 USERNAME
     */
    void deleteOption(Long optionId, String deletedBy);
}
