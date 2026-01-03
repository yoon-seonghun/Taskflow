package com.taskflow.service.impl;

import com.taskflow.domain.PropertyDef;
import com.taskflow.domain.PropertyOption;
import com.taskflow.dto.property.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.BoardMapper;
import com.taskflow.mapper.PropertyDefMapper;
import com.taskflow.mapper.PropertyOptionMapper;
import com.taskflow.service.PropertyCacheService;
import com.taskflow.service.PropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 속성 정의 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyServiceImpl implements PropertyService {

    private final PropertyDefMapper propertyDefMapper;
    private final PropertyOptionMapper propertyOptionMapper;
    private final BoardMapper boardMapper;
    private final PropertyCacheService propertyCacheService;

    // =============================================
    // 속성 정의 조회
    // =============================================

    @Override
    public PropertyResponse getProperty(Long propertyId) {
        PropertyDef propertyDef = propertyDefMapper.findById(propertyId)
                .orElseThrow(() -> BusinessException.propertyNotFound(propertyId));

        // 옵션 목록 조회
        if (propertyDef.isSelectType()) {
            List<PropertyOption> options = propertyOptionMapper.findByPropertyId(propertyId, "Y");
            propertyDef.setOptions(options);
        }

        return PropertyResponse.from(propertyDef);
    }

    @Override
    public List<PropertyResponse> getPropertiesByBoardId(Long boardId, String visibleYn) {
        // 보드 존재 확인
        boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        List<PropertyDef> properties = propertyDefMapper.findByBoardIdWithOptions(boardId, visibleYn);
        return PropertyResponse.fromList(properties);
    }

    @Override
    public List<PropertyResponse> getCachedPropertiesByBoardId(Long boardId) {
        List<PropertyDef> properties = propertyCacheService.getPropertiesByBoardId(boardId);
        return PropertyResponse.fromList(properties);
    }

    // =============================================
    // 속성 정의 등록/수정/삭제
    // =============================================

    @Override
    @Transactional
    public PropertyResponse createProperty(Long boardId, PropertyCreateRequest request, String createdBy) {
        log.info("Creating property: boardId={}, name={}", boardId, request.getPropertyName());

        // 보드 존재 확인
        boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // 속성명 중복 확인
        if (propertyDefMapper.existsByBoardIdAndName(boardId, request.getPropertyName())) {
            throw BusinessException.conflict("이미 동일한 이름의 속성이 존재합니다: " + request.getPropertyName());
        }

        // 정렬 순서 자동 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = propertyDefMapper.getMaxSortOrder(boardId) + 1;
        }

        // 속성 정의 엔티티 생성
        PropertyDef propertyDef = PropertyDef.builder()
                .boardId(boardId)
                .propertyName(request.getPropertyName())
                .propertyType(request.getPropertyType())
                .requiredYn(request.getRequiredYn() != null ? request.getRequiredYn() : "N")
                .sortOrder(sortOrder)
                .visibleYn(request.getVisibleYn() != null ? request.getVisibleYn() : "Y")
                .createdBy(createdBy)
                .build();

        // 저장
        propertyDefMapper.insert(propertyDef);
        log.info("Property created: id={}, name={}", propertyDef.getPropertyId(), propertyDef.getPropertyName());

        // 캐시 무효화
        propertyCacheService.evictBoardCache(boardId);

        return getProperty(propertyDef.getPropertyId());
    }

    @Override
    @Transactional
    public PropertyResponse updateProperty(Long propertyId, PropertyUpdateRequest request, String updatedBy) {
        log.info("Updating property: id={}", propertyId);

        // 속성 존재 확인
        PropertyDef propertyDef = propertyDefMapper.findById(propertyId)
                .orElseThrow(() -> BusinessException.propertyNotFound(propertyId));

        // 타입 변경 불가 (값이 존재하는 경우)
        if (request.getPropertyType() != null
                && !propertyDef.getPropertyType().equals(request.getPropertyType())
                && propertyDefMapper.hasPropertyValues(propertyId)) {
            log.warn("Property type change blocked - values exist: id={}, from={}, to={}",
                    propertyId, propertyDef.getPropertyType(), request.getPropertyType());
            throw BusinessException.dataInUse("사용 중인 속성의 타입은 변경할 수 없습니다. 기존 데이터를 먼저 삭제해주세요.");
        }

        // 속성명 중복 확인 (자신 제외)
        if (request.getPropertyName() != null &&
                propertyDefMapper.existsByBoardIdAndNameAndIdNot(propertyDef.getBoardId(), request.getPropertyName(), propertyId)) {
            throw BusinessException.conflict("이미 동일한 이름의 속성이 존재합니다: " + request.getPropertyName());
        }

        // 수정
        if (request.getPropertyName() != null) {
            propertyDef.setPropertyName(request.getPropertyName());
        }
        if (request.getPropertyType() != null) {
            propertyDef.setPropertyType(request.getPropertyType());
        }
        if (request.getRequiredYn() != null) {
            propertyDef.setRequiredYn(request.getRequiredYn());
        }
        if (request.getSortOrder() != null) {
            propertyDef.setSortOrder(request.getSortOrder());
        }
        if (request.getVisibleYn() != null) {
            propertyDef.setVisibleYn(request.getVisibleYn());
        }
        propertyDef.setUpdatedBy(updatedBy);

        propertyDefMapper.update(propertyDef);
        log.info("Property updated: id={}", propertyId);

        // 캐시 무효화
        propertyCacheService.evictBoardCache(propertyDef.getBoardId());

        return getProperty(propertyId);
    }

    @Override
    @Transactional
    public void deleteProperty(Long propertyId, String deletedBy) {
        log.info("Soft deleting property: id={}", propertyId);

        // 속성 존재 확인
        PropertyDef propertyDef = propertyDefMapper.findById(propertyId)
                .orElseThrow(() -> BusinessException.propertyNotFound(propertyId));

        Long boardId = propertyDef.getBoardId();

        // 논리 삭제 (USE_YN = 'N')
        propertyDefMapper.softDelete(propertyId, deletedBy);
        log.info("Property soft deleted: id={}", propertyId);

        // 캐시 무효화
        propertyCacheService.evictBoardCache(boardId);
    }

    // =============================================
    // 옵션 조회
    // =============================================

    @Override
    public List<OptionDetailResponse> getOptionsByPropertyId(Long propertyId, String useYn) {
        // 속성 존재 확인
        PropertyDef propertyDef = propertyDefMapper.findById(propertyId)
                .orElseThrow(() -> BusinessException.propertyNotFound(propertyId));

        // 선택형 속성 확인
        if (!propertyDef.isSelectType()) {
            throw BusinessException.badRequest("선택형 속성(SELECT, MULTI_SELECT)만 옵션을 가질 수 있습니다.");
        }

        List<PropertyOption> options = propertyOptionMapper.findByPropertyId(propertyId, useYn);
        return OptionDetailResponse.fromList(options);
    }

    @Override
    public OptionDetailResponse getOption(Long optionId) {
        PropertyOption option = propertyOptionMapper.findById(optionId)
                .orElseThrow(() -> BusinessException.optionNotFound(optionId));
        return OptionDetailResponse.from(option);
    }

    // =============================================
    // 옵션 등록/수정/삭제
    // =============================================

    @Override
    @Transactional
    public OptionDetailResponse createOption(Long propertyId, OptionCreateRequest request, String createdBy) {
        log.info("Creating option: propertyId={}, name={}", propertyId, request.getOptionName());

        // 속성 존재 확인
        PropertyDef propertyDef = propertyDefMapper.findById(propertyId)
                .orElseThrow(() -> BusinessException.propertyNotFound(propertyId));

        // 선택형 속성 확인
        if (!propertyDef.isSelectType()) {
            throw BusinessException.badRequest("선택형 속성(SELECT, MULTI_SELECT)만 옵션을 가질 수 있습니다.");
        }

        // 옵션명 중복 확인
        if (propertyOptionMapper.existsByPropertyIdAndLabel(propertyId, request.getOptionName())) {
            throw BusinessException.conflict("이미 동일한 이름의 옵션이 존재합니다: " + request.getOptionName());
        }

        // 정렬 순서 자동 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = propertyOptionMapper.getMaxSortOrder(propertyId) + 1;
        }

        // 옵션 엔티티 생성
        PropertyOption option = PropertyOption.builder()
                .propertyId(propertyId)
                .optionName(request.getOptionName())
                .color(request.getColor())
                .sortOrder(sortOrder)
                .useYn("Y")
                .createdBy(createdBy)
                .build();

        // 저장
        propertyOptionMapper.insert(option);
        log.info("Option created: id={}, name={}", option.getOptionId(), option.getOptionName());

        // 캐시 무효화
        propertyCacheService.evictBoardCache(propertyDef.getBoardId());

        return getOption(option.getOptionId());
    }

    @Override
    @Transactional
    public OptionDetailResponse updateOption(Long optionId, OptionUpdateRequest request, String updatedBy) {
        log.info("Updating option: id={}", optionId);

        // 옵션 존재 확인
        PropertyOption option = propertyOptionMapper.findById(optionId)
                .orElseThrow(() -> BusinessException.optionNotFound(optionId));

        // 옵션명 중복 확인 (자신 제외)
        if (request.getOptionName() != null &&
                propertyOptionMapper.existsByPropertyIdAndLabelAndIdNot(
                        option.getPropertyId(), request.getOptionName(), optionId)) {
            throw BusinessException.conflict("이미 동일한 이름의 옵션이 존재합니다: " + request.getOptionName());
        }

        // 수정
        if (request.getOptionName() != null) {
            option.setOptionName(request.getOptionName());
        }
        if (request.getColor() != null) {
            option.setColor(request.getColor());
        }
        if (request.getSortOrder() != null) {
            option.setSortOrder(request.getSortOrder());
        }
        if (request.getUseYn() != null) {
            option.setUseYn(request.getUseYn());
        }
        option.setUpdatedBy(updatedBy);

        propertyOptionMapper.update(option);
        log.info("Option updated: id={}", optionId);

        // 캐시 무효화
        propertyCacheService.evictBoardCache(option.getBoardId());

        return getOption(optionId);
    }

    @Override
    @Transactional
    public void deleteOption(Long optionId, String deletedBy) {
        log.info("Soft deleting option: id={}", optionId);

        // 옵션 존재 확인
        PropertyOption option = propertyOptionMapper.findById(optionId)
                .orElseThrow(() -> BusinessException.optionNotFound(optionId));

        Long boardId = option.getBoardId();

        // 논리 삭제 (USE_YN = 'N')
        propertyOptionMapper.softDelete(optionId, deletedBy);
        log.info("Option soft deleted: id={}", optionId);

        // 캐시 무효화
        propertyCacheService.evictBoardCache(boardId);
    }
}
