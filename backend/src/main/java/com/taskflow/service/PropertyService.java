package com.taskflow.service;

import com.taskflow.dto.property.*;

import java.util.List;

/**
 * 속성 정의 서비스 인터페이스
 *
 * v2.0 변경사항:
 * - 글로벌/매니저/사용자 소유 유형별 조회 메서드 추가
 * - 글로벌 속성 생성 메서드 추가
 * - 매니저 속성 생성 메서드 추가
 * - 사용자 속성 생성 메서드 추가 (개인 속성, 카테고리에 그룹화)
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
    // v2.0: 소유 유형별 속성 조회
    // =============================================

    /**
     * 글로벌 속성 목록 조회
     *
     * @return 글로벌 속성 목록
     */
    List<PropertyResponse> getGlobalProperties();

    /**
     * 매니저 속성 목록 조회 (본인 소유 + 상위 부서 속성)
     *
     * @param username 사용자 USERNAME
     * @return 매니저 속성 목록
     */
    List<PropertyResponse> getManagerProperties(String username);

    /**
     * 사용자 속성 목록 조회 (본인이 생성한 속성)
     *
     * @param username 사용자 USERNAME
     * @return 사용자 속성 목록
     */
    List<PropertyResponse> getUserProperties(String username);

    /**
     * 사용자가 접근 가능한 모든 속성 조회 (글로벌 + 매니저 + 본인 속성)
     * 카테고리/보드에서 속성 선택 시 사용
     *
     * @param username 사용자 USERNAME
     * @return 접근 가능한 속성 목록
     */
    List<PropertyResponse> getAccessibleProperties(String username);

    // =============================================
    // 속성 정의 등록/수정/삭제
    // =============================================

    /**
     * 보드 속성 정의 등록 (기존 API 호환)
     *
     * @param boardId   보드 ID
     * @param request   등록 요청
     * @param createdBy 생성자 USERNAME
     * @return 생성된 속성 정의 응답
     */
    PropertyResponse createProperty(Long boardId, PropertyCreateRequest request, String createdBy);

    /**
     * v2.0: 글로벌 속성 생성 (관리자 전용)
     *
     * @param request   등록 요청
     * @param createdBy 생성자 USERNAME
     * @return 생성된 속성 정의 응답
     */
    PropertyResponse createGlobalProperty(PropertyCreateRequest request, String createdBy);

    /**
     * v2.0: 매니저 속성 생성
     *
     * @param request   등록 요청
     * @param createdBy 생성자 USERNAME
     * @return 생성된 속성 정의 응답
     */
    PropertyResponse createManagerProperty(PropertyCreateRequest request, String createdBy);

    /**
     * v2.0: 사용자 속성 생성 (개인 속성)
     * - 사용자가 개인적으로 생성하는 속성
     * - 카테고리에 그룹화되어 보드/업무에 활용됨
     *
     * @param request   등록 요청
     * @param createdBy 생성자 USERNAME (소유자)
     * @return 생성된 속성 정의 응답
     */
    PropertyResponse createUserProperty(PropertyCreateRequest request, String createdBy);

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

    // =============================================
    // v2.0: 속성 이관 (Transfer)
    // =============================================

    /**
     * 속성 소유권 이전
     * - 속성의 소유자를 다른 사용자로 변경
     * - 옵션은 함께 이전됨
     * - 기존 소유자는 접근 불가
     *
     * @param propertyId    속성 ID
     * @param newOwnerUsername 새 소유자 USERNAME
     * @param transferredBy 이전 수행자 USERNAME
     * @return 이전된 속성 응답
     */
    PropertyResponse transferProperty(Long propertyId, String newOwnerUsername, String transferredBy);

    /**
     * 속성 복사 (다른 사용자에게)
     * - 속성 정의와 옵션을 복사하여 새 소유자에게 제공
     * - 원본 속성은 유지됨
     *
     * @param propertyId    원본 속성 ID
     * @param newOwnerUsername 새 소유자 USERNAME
     * @param copiedBy      복사 수행자 USERNAME
     * @return 복사된 새 속성 응답
     */
    PropertyResponse copyPropertyToUser(Long propertyId, String newOwnerUsername, String copiedBy);

    /**
     * 속성 일괄 이전
     * - 여러 속성을 한 번에 다른 사용자에게 이전
     *
     * @param propertyIds      속성 ID 목록
     * @param newOwnerUsername 새 소유자 USERNAME
     * @param transferredBy    이전 수행자 USERNAME
     * @return 이전된 속성 목록
     */
    List<PropertyResponse> transferProperties(List<Long> propertyIds, String newOwnerUsername, String transferredBy);
}
