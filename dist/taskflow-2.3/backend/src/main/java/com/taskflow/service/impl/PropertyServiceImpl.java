package com.taskflow.service.impl;

import com.taskflow.domain.Department;
import com.taskflow.domain.PropertyDef;
import com.taskflow.domain.PropertyOption;
import com.taskflow.domain.User;
import com.taskflow.dto.property.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.BoardMapper;
import com.taskflow.mapper.DepartmentMapper;
import com.taskflow.mapper.PropertyDefMapper;
import com.taskflow.mapper.PropertyOptionMapper;
import com.taskflow.mapper.UserMapper;
import com.taskflow.service.PropertyCacheService;
import com.taskflow.service.PropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 속성 정의 서비스 구현
 *
 * v2.0 변경사항:
 * - requiredYn 참조 제거 (CategoryProperty/BoardProperty로 이동)
 * - ownerType 처리 로직 추가
 * - 글로벌/매니저/사용자 속성 생성/조회 메서드 추가
 * - 사용자(USER) 속성: 개인이 생성, 카테고리에 그룹화되어 보드/업무에 활용
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyServiceImpl implements PropertyService {

    private final PropertyDefMapper propertyDefMapper;
    private final PropertyOptionMapper propertyOptionMapper;
    private final BoardMapper boardMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
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
    // v2.0: 소유 유형별 속성 조회
    // =============================================

    @Override
    public List<PropertyResponse> getGlobalProperties() {
        log.debug("Getting global properties");
        List<PropertyDef> properties = propertyDefMapper.findGlobalProperties();
        return PropertyResponse.fromList(properties);
    }

    @Override
    public List<PropertyResponse> getManagerProperties(String username) {
        log.debug("Getting manager properties for user: {}", username);

        // 사용자 정보 조회
        User user = userMapper.findByUsername(username)
                .orElseThrow(() -> BusinessException.userNotFound(username));

        // 사용자의 부서 및 상위 부서 코드 목록 조회
        List<String> departmentCodes = getAccessibleDepartmentCodes(user.getDepartmentCode());

        List<PropertyDef> properties = propertyDefMapper.findManagerProperties(username, departmentCodes);
        return PropertyResponse.fromList(properties);
    }

    @Override
    public List<PropertyResponse> getUserProperties(String username) {
        log.debug("Getting user properties for user: {}", username);

        List<PropertyDef> properties = propertyDefMapper.findUserProperties(username);
        return PropertyResponse.fromList(properties);
    }

    @Override
    public List<PropertyResponse> getAccessibleProperties(String username) {
        log.debug("Getting accessible properties for user: {}", username);

        // 사용자 정보 조회
        User user = userMapper.findByUsername(username)
                .orElseThrow(() -> BusinessException.userNotFound(username));

        // 사용자의 부서 및 상위 부서 코드 목록 조회
        List<String> departmentCodes = getAccessibleDepartmentCodes(user.getDepartmentCode());

        List<PropertyDef> properties = propertyDefMapper.findAccessibleProperties(username, departmentCodes);
        return PropertyResponse.fromList(properties);
    }

    /**
     * 사용자가 접근 가능한 부서 코드 목록 조회 (본인 부서 + 상위 부서)
     */
    private List<String> getAccessibleDepartmentCodes(String departmentCode) {
        List<String> codes = new ArrayList<>();

        if (departmentCode != null) {
            codes.add(departmentCode);

            // 상위 부서 경로 조회 (루트까지)
            List<Department> ancestors = departmentMapper.findAncestors(departmentCode);
            codes.addAll(ancestors.stream()
                    .map(Department::getDepartmentCode)
                    .collect(Collectors.toList()));
        }

        return codes;
    }

    // =============================================
    // 속성 정의 등록/수정/삭제
    // =============================================

    /**
     * 보드 속성 생성 (레거시 API - 내부적으로 USER 타입 속성 생성)
     *
     * v2.0 변경: 보드 귀속 속성 대신 사용자 속성으로 생성됨
     * 개인 속성 생성 후 해당 보드의 카테고리에 연결해야 사용 가능
     */
    @Override
    @Transactional
    public PropertyResponse createProperty(Long boardId, PropertyCreateRequest request, String createdBy) {
        log.info("Creating property (legacy API): boardId={}, name={}", boardId, request.getPropertyName());

        // 보드 존재 확인
        boardMapper.findById(boardId)
                .orElseThrow(() -> BusinessException.boardNotFound(boardId));

        // v2.0.3: 글로벌/매니저 속성에 동일 이름이 있는지 먼저 확인
        checkDuplicateNameInUpperTypes(request.getPropertyName(), PropertyDef.OWNER_TYPE_USER, createdBy);

        // 사용자 속성명 중복 확인 (같은 사용자가 만든 속성 중에서)
        List<PropertyDef> existingUserProps = propertyDefMapper.findUserProperties(createdBy);
        boolean nameExists = existingUserProps.stream()
                .anyMatch(p -> p.getPropertyName().equals(request.getPropertyName()));
        if (nameExists) {
            throw BusinessException.conflict("이미 동일한 이름의 속성이 존재합니다: " + request.getPropertyName());
        }

        // 정렬 순서 자동 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = existingUserProps.size() + 1;
        }

        // v2.0: 사용자 속성으로 생성 (보드 귀속 대신)
        PropertyDef propertyDef = PropertyDef.builder()
                .boardId(null)  // 사용자 속성은 보드 미귀속
                .ownerType(PropertyDef.OWNER_TYPE_USER)
                .ownerUsername(createdBy)
                .propertyName(request.getPropertyName())
                .propertyType(request.getPropertyType())
                .sortOrder(sortOrder)
                .visibleYn(request.getVisibleYn() != null ? request.getVisibleYn() : "Y")
                // v2.0.6: 외부 쿼리 연동 필드
                .externalQueryId(request.getExternalQueryId())
                .dataSourceType(request.getDataSourceType() != null ? request.getDataSourceType() : "INTERNAL")
                .createdBy(createdBy)
                .build();

        // 저장
        propertyDefMapper.insert(propertyDef);
        log.info("User property created (via legacy API): id={}, name={}, owner={}",
                propertyDef.getPropertyId(), propertyDef.getPropertyName(), createdBy);

        return getProperty(propertyDef.getPropertyId());
    }

    @Override
    @Transactional
    public PropertyResponse createGlobalProperty(PropertyCreateRequest request, String createdBy) {
        log.info("Creating global property: name={}", request.getPropertyName());

        // 글로벌 속성명 중복 확인 (ownerType=GLOBAL인 것들 중에서)
        List<PropertyDef> existingGlobals = propertyDefMapper.findGlobalProperties();
        boolean nameExists = existingGlobals.stream()
                .anyMatch(p -> p.getPropertyName().equals(request.getPropertyName()));
        if (nameExists) {
            throw BusinessException.conflict("이미 동일한 이름의 글로벌 속성이 존재합니다: " + request.getPropertyName());
        }

        // 정렬 순서 자동 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = existingGlobals.size() + 1;
        }

        // 글로벌 속성 정의 엔티티 생성
        PropertyDef propertyDef = PropertyDef.builder()
                .boardId(null)  // 글로벌 속성은 보드 미귀속
                .ownerType(PropertyDef.OWNER_TYPE_GLOBAL)
                .ownerUsername(null)  // 글로벌은 소유자 없음
                .ownerDeptCode(null)
                .propertyName(request.getPropertyName())
                .propertyType(request.getPropertyType())
                .sortOrder(sortOrder)
                .visibleYn(request.getVisibleYn() != null ? request.getVisibleYn() : "Y")
                // v2.0.6: 외부 쿼리 연동 필드
                .externalQueryId(request.getExternalQueryId())
                .dataSourceType(request.getDataSourceType() != null ? request.getDataSourceType() : "INTERNAL")
                .createdBy(createdBy)
                .build();

        // 저장
        propertyDefMapper.insert(propertyDef);
        log.info("Global property created: id={}, name={}", propertyDef.getPropertyId(), propertyDef.getPropertyName());

        return getProperty(propertyDef.getPropertyId());
    }

    @Override
    @Transactional
    public PropertyResponse createManagerProperty(PropertyCreateRequest request, String createdBy) {
        log.info("Creating manager property: name={}, owner={}", request.getPropertyName(), createdBy);

        // v2.0.3: 글로벌 속성에 동일 이름이 있는지 먼저 확인
        checkDuplicateNameInUpperTypes(request.getPropertyName(), PropertyDef.OWNER_TYPE_MANAGER, createdBy);

        // 사용자 정보 조회
        User user = userMapper.findByUsername(createdBy)
                .orElseThrow(() -> BusinessException.userNotFound(createdBy));

        // 부서 코드 (요청에 없으면 사용자 부서 사용)
        String deptCode = request.getOwnerDeptCode();
        if (deptCode == null) {
            deptCode = user.getDepartmentCode();
        }

        // 정렬 순서 자동 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = 1;  // 매니저 속성은 별도 정렬 순서 관리
        }

        // 매니저 속성 정의 엔티티 생성
        PropertyDef propertyDef = PropertyDef.builder()
                .boardId(null)  // 매니저 속성은 보드 미귀속
                .ownerType(PropertyDef.OWNER_TYPE_MANAGER)
                .ownerUsername(createdBy)
                .ownerDeptCode(deptCode)
                .propertyName(request.getPropertyName())
                .propertyType(request.getPropertyType())
                .sortOrder(sortOrder)
                .visibleYn(request.getVisibleYn() != null ? request.getVisibleYn() : "Y")
                // v2.0.6: 외부 쿼리 연동 필드
                .externalQueryId(request.getExternalQueryId())
                .dataSourceType(request.getDataSourceType() != null ? request.getDataSourceType() : "INTERNAL")
                .createdBy(createdBy)
                .build();

        // 저장
        propertyDefMapper.insert(propertyDef);
        log.info("Manager property created: id={}, name={}, owner={}",
                propertyDef.getPropertyId(), propertyDef.getPropertyName(), createdBy);

        return getProperty(propertyDef.getPropertyId());
    }

    @Override
    @Transactional
    public PropertyResponse createUserProperty(PropertyCreateRequest request, String createdBy) {
        log.info("Creating user property: name={}, owner={}", request.getPropertyName(), createdBy);

        // v2.0.3: 글로벌/매니저 속성에 동일 이름이 있는지 먼저 확인
        checkDuplicateNameInUpperTypes(request.getPropertyName(), PropertyDef.OWNER_TYPE_USER, createdBy);

        // 사용자 속성명 중복 확인 (같은 사용자가 만든 속성 중에서)
        List<PropertyDef> existingUserProps = propertyDefMapper.findUserProperties(createdBy);
        boolean nameExists = existingUserProps.stream()
                .anyMatch(p -> p.getPropertyName().equals(request.getPropertyName()));
        if (nameExists) {
            throw BusinessException.conflict("이미 동일한 이름의 속성이 존재합니다: " + request.getPropertyName());
        }

        // 정렬 순서 자동 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = existingUserProps.size() + 1;
        }

        // 사용자 속성 정의 엔티티 생성
        PropertyDef propertyDef = PropertyDef.builder()
                .boardId(null)  // 사용자 속성은 보드 미귀속
                .ownerType(PropertyDef.OWNER_TYPE_USER)
                .ownerUsername(createdBy)  // 생성자가 소유자
                .ownerDeptCode(null)
                .propertyName(request.getPropertyName())
                .propertyType(request.getPropertyType())
                .sortOrder(sortOrder)
                .visibleYn(request.getVisibleYn() != null ? request.getVisibleYn() : "Y")
                // v2.0.6: 외부 쿼리 연동 필드
                .externalQueryId(request.getExternalQueryId())
                .dataSourceType(request.getDataSourceType() != null ? request.getDataSourceType() : "INTERNAL")
                .createdBy(createdBy)
                .build();

        // 저장
        propertyDefMapper.insert(propertyDef);
        log.info("User property created: id={}, name={}, owner={}",
                propertyDef.getPropertyId(), propertyDef.getPropertyName(), createdBy);

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

        // 속성명 중복 확인 (자신 제외) - 보드 속성인 경우만
        if (request.getPropertyName() != null && propertyDef.getBoardId() != null &&
                propertyDefMapper.existsByBoardIdAndNameAndIdNot(propertyDef.getBoardId(), request.getPropertyName(), propertyId)) {
            throw BusinessException.conflict("이미 동일한 이름의 속성이 존재합니다: " + request.getPropertyName());
        }

        // 수정 (v2.0: requiredYn 제거됨 - CategoryProperty/BoardProperty에서 관리)
        if (request.getPropertyName() != null) {
            propertyDef.setPropertyName(request.getPropertyName());
        }
        if (request.getPropertyType() != null) {
            propertyDef.setPropertyType(request.getPropertyType());
        }
        if (request.getSortOrder() != null) {
            propertyDef.setSortOrder(request.getSortOrder());
        }
        if (request.getVisibleYn() != null) {
            propertyDef.setVisibleYn(request.getVisibleYn());
        }
        // v2.0.6: 외부 쿼리 연동 필드
        if (request.getDataSourceType() != null) {
            propertyDef.setDataSourceType(request.getDataSourceType());
        }
        // externalQueryId는 null 설정을 허용 (연동 해제용)
        propertyDef.setExternalQueryId(request.getExternalQueryId());
        propertyDef.setUpdatedBy(updatedBy);

        propertyDefMapper.update(propertyDef);
        log.info("Property updated: id={}", propertyId);

        // 캐시 무효화 (보드 속성인 경우만)
        if (propertyDef.getBoardId() != null) {
            propertyCacheService.evictBoardCache(propertyDef.getBoardId());
        }

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

        // 캐시 무효화 (보드 속성인 경우만)
        if (boardId != null) {
            propertyCacheService.evictBoardCache(boardId);
        }
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

        // 캐시 무효화 (보드 속성인 경우만)
        if (propertyDef.getBoardId() != null) {
            propertyCacheService.evictBoardCache(propertyDef.getBoardId());
        }

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

        // 캐시 무효화 (보드 속성인 경우만)
        if (option.getBoardId() != null) {
            propertyCacheService.evictBoardCache(option.getBoardId());
        }

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

        // 캐시 무효화 (보드 속성인 경우만)
        if (boardId != null) {
            propertyCacheService.evictBoardCache(boardId);
        }
    }

    // =============================================
    // v2.0: 속성 이관 (Transfer)
    // =============================================

    @Override
    @Transactional
    public PropertyResponse transferProperty(Long propertyId, String newOwnerUsername, String transferredBy) {
        log.info("Transferring property: id={}, newOwner={}, by={}",
                propertyId, newOwnerUsername, transferredBy);

        // 1. 속성 존재 확인
        PropertyDef propertyDef = propertyDefMapper.findById(propertyId)
                .orElseThrow(() -> BusinessException.propertyNotFound(propertyId));

        // 2. 이관 대상 사용자 존재 확인
        User newOwner = userMapper.findByUsername(newOwnerUsername)
                .orElseThrow(() -> BusinessException.userNotFound(newOwnerUsername));

        // 3. 글로벌 속성은 이관 불가
        if (PropertyDef.OWNER_TYPE_GLOBAL.equals(propertyDef.getOwnerType())) {
            throw BusinessException.badRequest("글로벌 속성은 이관할 수 없습니다.");
        }

        // 4. 이미 대상 사용자 소유인 경우
        if (newOwnerUsername.equals(propertyDef.getOwnerUsername())) {
            throw BusinessException.badRequest("이미 해당 사용자가 소유한 속성입니다.");
        }

        // 5. 대상 사용자에게 동일 이름 속성이 있는지 확인
        List<PropertyDef> targetUserProps = propertyDefMapper.findUserProperties(newOwnerUsername);
        boolean nameExists = targetUserProps.stream()
                .anyMatch(p -> p.getPropertyName().equals(propertyDef.getPropertyName()));
        if (nameExists) {
            throw BusinessException.conflict(
                    "대상 사용자에게 동일한 이름의 속성이 이미 존재합니다: " + propertyDef.getPropertyName());
        }

        // 6. 소유권 이전
        propertyDef.setOwnerType(PropertyDef.OWNER_TYPE_USER);
        propertyDef.setOwnerUsername(newOwnerUsername);
        propertyDef.setOwnerDeptCode(null);  // 사용자 속성은 부서코드 불필요
        propertyDef.setUpdatedBy(transferredBy);

        propertyDefMapper.update(propertyDef);
        log.info("Property transferred: id={}, from={}, to={}",
                propertyId, propertyDef.getOwnerUsername(), newOwnerUsername);

        return getProperty(propertyId);
    }

    @Override
    @Transactional
    public PropertyResponse copyPropertyToUser(Long propertyId, String newOwnerUsername, String copiedBy) {
        log.info("Copying property to user: id={}, newOwner={}, by={}",
                propertyId, newOwnerUsername, copiedBy);

        // 1. 원본 속성 조회
        PropertyDef originalProperty = propertyDefMapper.findById(propertyId)
                .orElseThrow(() -> BusinessException.propertyNotFound(propertyId));

        // 2. 대상 사용자 존재 확인
        User newOwner = userMapper.findByUsername(newOwnerUsername)
                .orElseThrow(() -> BusinessException.userNotFound(newOwnerUsername));

        // 3. 대상 사용자에게 동일 이름 속성이 있는지 확인
        List<PropertyDef> targetUserProps = propertyDefMapper.findUserProperties(newOwnerUsername);
        final String baseName = originalProperty.getPropertyName();

        // 이름 충돌 여부 확인 및 고유 이름 생성
        Set<String> existingNames = targetUserProps.stream()
                .map(PropertyDef::getPropertyName)
                .collect(Collectors.toSet());

        String newPropertyName = baseName;
        if (existingNames.contains(baseName)) {
            int suffix = 1;
            while (existingNames.contains(baseName + "_copy" + suffix)) {
                suffix++;
            }
            newPropertyName = baseName + "_copy" + suffix;
        }

        // 4. 새 속성 생성
        Integer sortOrder = targetUserProps.size() + 1;

        PropertyDef newProperty = PropertyDef.builder()
                .boardId(null)
                .ownerType(PropertyDef.OWNER_TYPE_USER)
                .ownerUsername(newOwnerUsername)
                .ownerDeptCode(null)
                .propertyName(newPropertyName)
                .propertyType(originalProperty.getPropertyType())
                .sortOrder(sortOrder)
                .visibleYn(originalProperty.getVisibleYn())
                .createdBy(copiedBy)
                .build();

        propertyDefMapper.insert(newProperty);
        log.info("Property copied: originalId={}, newId={}, newOwner={}",
                propertyId, newProperty.getPropertyId(), newOwnerUsername);

        // 5. 옵션 복사 (선택형 속성인 경우)
        if (originalProperty.isSelectType()) {
            List<PropertyOption> originalOptions = propertyOptionMapper.findByPropertyId(propertyId, "Y");
            for (PropertyOption originalOption : originalOptions) {
                PropertyOption newOption = PropertyOption.builder()
                        .propertyId(newProperty.getPropertyId())
                        .optionName(originalOption.getOptionName())
                        .color(originalOption.getColor())
                        .sortOrder(originalOption.getSortOrder())
                        .useYn("Y")
                        .createdBy(copiedBy)
                        .build();
                propertyOptionMapper.insert(newOption);
            }
            log.info("Property options copied: count={}", originalOptions.size());
        }

        return getProperty(newProperty.getPropertyId());
    }

    @Override
    @Transactional
    public List<PropertyResponse> transferProperties(List<Long> propertyIds, String newOwnerUsername, String transferredBy) {
        log.info("Batch transferring properties: count={}, newOwner={}",
                propertyIds.size(), newOwnerUsername);

        List<PropertyResponse> results = new ArrayList<>();

        for (Long propertyId : propertyIds) {
            try {
                PropertyResponse transferred = transferProperty(propertyId, newOwnerUsername, transferredBy);
                results.add(transferred);
            } catch (BusinessException e) {
                log.warn("Failed to transfer property {}: {}", propertyId, e.getMessage());
                // 개별 실패는 건너뛰고 계속 진행
            }
        }

        log.info("Batch transfer completed: success={}/{}", results.size(), propertyIds.size());
        return results;
    }

    // =============================================
    // v2.0.3: 상위 ownerType 동일명 속성 체크
    // =============================================

    /**
     * 상위 ownerType에 동일 이름의 속성이 있는지 확인
     * - USER 생성 시: GLOBAL, MANAGER 속성 중 동일명 확인
     * - MANAGER 생성 시: GLOBAL 속성 중 동일명 확인
     * - GLOBAL 생성 시: 체크 불필요 (최상위)
     *
     * @param propertyName 확인할 속성명
     * @param targetOwnerType 생성하려는 속성의 ownerType
     * @param username 생성자 (매니저 속성 조회 시 사용)
     */
    private void checkDuplicateNameInUpperTypes(String propertyName, String targetOwnerType, String username) {
        if (PropertyDef.OWNER_TYPE_GLOBAL.equals(targetOwnerType)) {
            // 글로벌 속성 생성 시에는 상위 ownerType 체크 불필요
            return;
        }

        // 글로벌 속성 중 동일명 확인
        List<PropertyDef> globalProps = propertyDefMapper.findGlobalProperties();
        boolean existsInGlobal = globalProps.stream()
                .anyMatch(p -> p.getPropertyName().equals(propertyName));
        if (existsInGlobal) {
            throw BusinessException.conflict(
                    "동일한 이름의 글로벌 속성이 이미 존재합니다: " + propertyName);
        }

        // USER 속성 생성 시에는 매니저 속성도 확인
        if (PropertyDef.OWNER_TYPE_USER.equals(targetOwnerType)) {
            // 사용자 정보 조회하여 접근 가능한 매니저 속성 확인
            User user = userMapper.findByUsername(username).orElse(null);
            if (user != null) {
                List<String> departmentCodes = getAccessibleDepartmentCodes(user.getDepartmentCode());
                List<PropertyDef> managerProps = propertyDefMapper.findManagerProperties(username, departmentCodes);
                boolean existsInManager = managerProps.stream()
                        .anyMatch(p -> p.getPropertyName().equals(propertyName));
                if (existsInManager) {
                    throw BusinessException.conflict(
                            "동일한 이름의 매니저 속성이 이미 존재합니다: " + propertyName);
                }
            }
        }

        log.debug("Duplicate name check passed for property: name={}, targetOwnerType={}", propertyName, targetOwnerType);
    }

    // =============================================
    // 속성 순서 변경
    // =============================================

    @Override
    @Transactional
    public void reorderProperties(PropertyReorderRequest request, String updatedBy) {
        log.info("Reordering properties: ownerType={}, count={}",
                request.getOwnerType(), request.getOrders().size());

        String ownerType = request.getOwnerType();

        // 각 속성의 ownerType이 요청과 일치하는지 검증
        for (PropertyReorderRequest.PropertyOrder order : request.getOrders()) {
            PropertyDef propertyDef = propertyDefMapper.findById(order.getPropertyId())
                    .orElseThrow(() -> BusinessException.propertyNotFound(order.getPropertyId()));

            // ownerType 검증: 같은 그룹 내에서만 순서 변경 가능
            if (!ownerType.equals(propertyDef.getOwnerType())) {
                throw BusinessException.badRequest(
                        String.format("속성 ID %d는 %s 유형이 아닙니다. (실제: %s)",
                                order.getPropertyId(), ownerType, propertyDef.getOwnerType()));
            }

            // USER/MANAGER 유형인 경우 소유자 검증 (본인 속성만 순서 변경 가능)
            if (PropertyDef.OWNER_TYPE_USER.equals(ownerType) ||
                PropertyDef.OWNER_TYPE_MANAGER.equals(ownerType)) {
                if (!updatedBy.equals(propertyDef.getOwnerUsername())) {
                    throw BusinessException.forbidden(
                            String.format("속성 ID %d에 대한 순서 변경 권한이 없습니다.", order.getPropertyId()));
                }
            }
        }

        // 순서 업데이트 실행
        for (PropertyReorderRequest.PropertyOrder order : request.getOrders()) {
            propertyDefMapper.updateSortOrder(order.getPropertyId(), order.getSortOrder(), updatedBy);
            log.debug("Property sortOrder updated: id={}, sortOrder={}",
                    order.getPropertyId(), order.getSortOrder());
        }

        log.info("Properties reordered successfully: count={}", request.getOrders().size());
    }
}
