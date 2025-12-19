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
    public PropertyResponse createProperty(Long boardId, PropertyCreateRequest request, Long createdBy) {
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
    public PropertyResponse updateProperty(Long propertyId, PropertyUpdateRequest request, Long updatedBy) {
        log.info("Updating property: id={}", propertyId);

        // 속성 존재 확인
        PropertyDef propertyDef = propertyDefMapper.findById(propertyId)
                .orElseThrow(() -> BusinessException.propertyNotFound(propertyId));

        // 타입 변경 시 경고 (값이 존재하는 경우)
        if (request.getPropertyType() != null
                && !propertyDef.getPropertyType().equals(request.getPropertyType())
                && propertyDefMapper.hasPropertyValues(propertyId)) {
            log.warn("Property type change with existing values: id={}, from={}, to={}",
                    propertyId, propertyDef.getPropertyType(), request.getPropertyType());
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
    public void deleteProperty(Long propertyId) {
        log.info("Deleting property: id={}", propertyId);

        // 속성 존재 확인
        PropertyDef propertyDef = propertyDefMapper.findById(propertyId)
                .orElseThrow(() -> BusinessException.propertyNotFound(propertyId));

        // 속성 값 사용 중 확인
        if (propertyDefMapper.hasPropertyValues(propertyId)) {
            throw BusinessException.dataInUse("속성에 저장된 값이 존재하여 삭제할 수 없습니다. 속성을 숨기거나 값을 먼저 삭제해주세요.");
        }

        Long boardId = propertyDef.getBoardId();

        // 옵션 삭제
        propertyOptionMapper.deleteByPropertyId(propertyId);

        // 속성 삭제
        propertyDefMapper.delete(propertyId);
        log.info("Property deleted: id={}", propertyId);

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
    public OptionDetailResponse createOption(Long propertyId, OptionCreateRequest request, Long createdBy) {
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
    public OptionDetailResponse updateOption(Long optionId, OptionUpdateRequest request, Long updatedBy) {
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
    public void deleteOption(Long optionId) {
        log.info("Deleting option: id={}", optionId);

        // 옵션 존재 확인
        PropertyOption option = propertyOptionMapper.findById(optionId)
                .orElseThrow(() -> BusinessException.optionNotFound(optionId));

        // 사용 중인지 확인
        if (propertyOptionMapper.isOptionInUse(optionId)) {
            throw BusinessException.dataInUse("옵션이 사용 중이어서 삭제할 수 없습니다. 옵션을 비활성화하거나 사용 중인 항목을 먼저 변경해주세요.");
        }

        Long boardId = option.getBoardId();

        // 삭제
        propertyOptionMapper.delete(optionId);
        log.info("Option deleted: id={}", optionId);

        // 캐시 무효화
        propertyCacheService.evictBoardCache(boardId);
    }
}
