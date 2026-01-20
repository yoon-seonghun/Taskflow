package com.taskflow.service.impl;

import com.taskflow.domain.TaskTemplate;
import com.taskflow.domain.TemplateProperty;
import com.taskflow.dto.template.TaskTemplateCreateRequest;
import com.taskflow.dto.template.TaskTemplateResponse;
import com.taskflow.dto.template.TaskTemplateSearchResponse;
import com.taskflow.dto.template.TaskTemplateUpdateRequest;
import com.taskflow.dto.template.TemplatePropertyRequest;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.TaskTemplateMapper;
import com.taskflow.mapper.TemplatePropertyMapper;
import com.taskflow.service.TaskTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 작업 템플릿 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskTemplateServiceImpl implements TaskTemplateService {

    private final TaskTemplateMapper taskTemplateMapper;
    private final TemplatePropertyMapper templatePropertyMapper;

    /**
     * 자동완성 검색 시 최대 결과 수
     */
    private static final int SEARCH_LIMIT = 10;

    // =============================================
    // 템플릿 조회
    // =============================================

    @Override
    public TaskTemplateResponse getTemplate(Long templateId) {
        TaskTemplate template = taskTemplateMapper.findById(templateId)
                .orElseThrow(() -> BusinessException.templateNotFound(templateId));

        // 템플릿 속성 목록 조회
        List<TemplateProperty> properties = templatePropertyMapper.findByTemplateId(templateId);

        return TaskTemplateResponse.from(template, properties);
    }

    @Override
    public List<TaskTemplateResponse> getAllTemplates() {
        List<TaskTemplate> templates = taskTemplateMapper.findAll();
        return TaskTemplateResponse.fromList(templates);
    }

    @Override
    public List<TaskTemplateResponse> getActiveTemplates() {
        List<TaskTemplate> templates = taskTemplateMapper.findAllActive();
        return TaskTemplateResponse.fromList(templates);
    }

    @Override
    public List<TaskTemplateSearchResponse> searchTemplates(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // 키워드가 없으면 빈 목록 반환
            return List.of();
        }

        List<TaskTemplate> templates = taskTemplateMapper.searchByKeyword(keyword.trim(), SEARCH_LIMIT);
        return TaskTemplateSearchResponse.fromList(templates);
    }

    // =============================================
    // 소유 유형별 조회
    // =============================================

    @Override
    public List<TaskTemplateResponse> getGlobalTemplates() {
        List<TaskTemplate> templates = taskTemplateMapper.findGlobalTemplates();
        return TaskTemplateResponse.fromList(templates);
    }

    @Override
    public List<TaskTemplateResponse> getManagerTemplates(String username, List<String> departmentCodes) {
        List<TaskTemplate> templates = taskTemplateMapper.findManagerTemplates(username, departmentCodes);
        return TaskTemplateResponse.fromList(templates);
    }

    @Override
    public List<TaskTemplateResponse> getUserTemplates(String username) {
        List<TaskTemplate> templates = taskTemplateMapper.findUserTemplates(username);
        return TaskTemplateResponse.fromList(templates);
    }

    @Override
    public List<TaskTemplateResponse> getAccessibleTemplates(String username, List<String> departmentCodes) {
        List<TaskTemplate> templates = taskTemplateMapper.findAccessibleTemplates(username, departmentCodes);
        return TaskTemplateResponse.fromList(templates);
    }

    // =============================================
    // 소유 유형별 등록
    // =============================================

    @Override
    @Transactional
    public TaskTemplateResponse createGlobalTemplate(TaskTemplateCreateRequest request, String createdBy) {
        log.info("Creating global task template: content={}", request.getContent());
        return createTemplateWithOwnerType(request, createdBy, TaskTemplate.OWNER_TYPE_GLOBAL, null, null);
    }

    @Override
    @Transactional
    public TaskTemplateResponse createManagerTemplate(TaskTemplateCreateRequest request, String createdBy, String departmentCode) {
        log.info("Creating manager task template: content={}, deptCode={}", request.getContent(), departmentCode);
        return createTemplateWithOwnerType(request, createdBy, TaskTemplate.OWNER_TYPE_MANAGER, createdBy, departmentCode);
    }

    @Override
    @Transactional
    public TaskTemplateResponse createUserTemplate(TaskTemplateCreateRequest request, String createdBy) {
        log.info("Creating user task template: content={}", request.getContent());
        return createTemplateWithOwnerType(request, createdBy, TaskTemplate.OWNER_TYPE_USER, createdBy, null);
    }

    /**
     * 소유 유형을 지정하여 템플릿 생성
     */
    private TaskTemplateResponse createTemplateWithOwnerType(TaskTemplateCreateRequest request, String createdBy,
                                                              String ownerType, String ownerUsername, String ownerDeptCode) {
        // 소유 유형별 중복 체크
        taskTemplateMapper.findByContentAndOwner(request.getContent(), ownerType, ownerUsername)
                .ifPresent(t -> {
                    throw BusinessException.conflict("이미 동일한 작업 내용이 등록되어 있습니다.");
                });

        // 정렬 순서 자동 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = taskTemplateMapper.getMaxSortOrder() + 1;
        }

        // 기본 업무 상태 (미지정 시 NOT_STARTED)
        String defaultItemStatus = request.getDefaultItemStatus();
        if (defaultItemStatus == null || defaultItemStatus.isEmpty()) {
            defaultItemStatus = "NOT_STARTED";
        }

        // 기본 담당자 (미지정 시 생성자)
        String defaultAssigneeUsername = request.getDefaultAssigneeUsername();
        if (defaultAssigneeUsername == null) {
            defaultAssigneeUsername = createdBy;
        }

        // 템플릿 엔티티 생성
        TaskTemplate template = TaskTemplate.builder()
                .content(request.getContent())
                .defaultAssigneeUsername(defaultAssigneeUsername)
                .defaultItemStatus(defaultItemStatus)
                .categoryId(request.getCategoryId())
                .ownerType(ownerType)
                .ownerUsername(ownerUsername)
                .ownerDeptCode(ownerDeptCode)
                .status(TaskTemplate.STATUS_ACTIVE)
                .sortOrder(sortOrder)
                .useCount(0)
                .createdBy(createdBy)
                .build();

        // 템플릿 저장
        taskTemplateMapper.insert(template);
        log.info("Task template created: id={}, ownerType={}", template.getTemplateId(), ownerType);

        // 템플릿 속성 저장
        saveTemplateProperties(template.getTemplateId(), request.getProperties(), createdBy);

        return getTemplate(template.getTemplateId());
    }

    // =============================================
    // 템플릿 등록/수정/삭제
    // =============================================

    @Override
    @Transactional
    public TaskTemplateResponse createTemplate(TaskTemplateCreateRequest request, String createdBy) {
        log.info("Creating task template: content={}", request.getContent());

        // 중복 체크
        taskTemplateMapper.findByContent(request.getContent())
                .ifPresent(t -> {
                    throw BusinessException.conflict("이미 동일한 작업 내용이 등록되어 있습니다.");
                });

        // 정렬 순서 자동 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = taskTemplateMapper.getMaxSortOrder() + 1;
        }

        // 기본 업무 상태 (미지정 시 NOT_STARTED)
        String defaultItemStatus = request.getDefaultItemStatus();
        if (defaultItemStatus == null || defaultItemStatus.isEmpty()) {
            defaultItemStatus = "NOT_STARTED";
        }

        // 기본 담당자 (미지정 시 생성자)
        String defaultAssigneeUsername = request.getDefaultAssigneeUsername();
        if (defaultAssigneeUsername == null) {
            defaultAssigneeUsername = createdBy;
        }

        // 템플릿 엔티티 생성 (기본: USER 타입)
        TaskTemplate template = TaskTemplate.builder()
                .content(request.getContent())
                .defaultAssigneeUsername(defaultAssigneeUsername)
                .defaultItemStatus(defaultItemStatus)
                .categoryId(request.getCategoryId())
                .ownerType(TaskTemplate.OWNER_TYPE_USER)
                .ownerUsername(createdBy)
                .status(TaskTemplate.STATUS_ACTIVE)
                .sortOrder(sortOrder)
                .useCount(0)
                .createdBy(createdBy)
                .build();

        // 템플릿 저장
        taskTemplateMapper.insert(template);
        log.info("Task template created: id={}, ownerType=USER", template.getTemplateId());

        // 템플릿 속성 저장
        saveTemplateProperties(template.getTemplateId(), request.getProperties(), createdBy);

        return getTemplate(template.getTemplateId());
    }

    @Override
    @Transactional
    public TaskTemplateResponse updateTemplate(Long templateId, TaskTemplateUpdateRequest request, String updatedBy) {
        log.info("Updating task template: id={}", templateId);

        // 템플릿 존재 확인
        TaskTemplate template = taskTemplateMapper.findById(templateId)
                .orElseThrow(() -> BusinessException.templateNotFound(templateId));

        // 중복 체크 (자기 자신 제외)
        taskTemplateMapper.findByContent(request.getContent())
                .ifPresent(t -> {
                    if (!t.getTemplateId().equals(templateId)) {
                        throw BusinessException.conflict("이미 동일한 작업 내용이 등록되어 있습니다.");
                    }
                });

        // 수정
        template.setContent(request.getContent());
        if (request.getDefaultAssigneeUsername() != null) {
            template.setDefaultAssigneeUsername(request.getDefaultAssigneeUsername());
        }
        if (request.getDefaultItemStatus() != null) {
            template.setDefaultItemStatus(request.getDefaultItemStatus());
        }
        // 카테고리는 null도 허용 (카테고리 해제)
        template.setCategoryId(request.getCategoryId());
        if (request.getStatus() != null) {
            template.setStatus(request.getStatus());
        }
        if (request.getSortOrder() != null) {
            template.setSortOrder(request.getSortOrder());
        }
        template.setUpdatedBy(updatedBy);

        taskTemplateMapper.update(template);
        log.info("Task template updated: id={}", templateId);

        // 기존 속성 논리 삭제 후 새 속성 저장
        templatePropertyMapper.softDeleteByTemplateId(templateId, updatedBy);
        saveTemplateProperties(templateId, request.getProperties(), updatedBy);

        return getTemplate(templateId);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long templateId) {
        log.info("Deleting task template: id={}", templateId);

        // 템플릿 존재 확인
        taskTemplateMapper.findById(templateId)
                .orElseThrow(() -> BusinessException.templateNotFound(templateId));

        // 템플릿 속성 삭제
        templatePropertyMapper.deleteByTemplateId(templateId);

        taskTemplateMapper.delete(templateId);
        log.info("Task template deleted: id={}", templateId);
    }

    @Override
    @Transactional
    public void incrementUseCount(Long templateId) {
        log.debug("Incrementing use count: templateId={}", templateId);

        // 템플릿 존재 확인
        taskTemplateMapper.findById(templateId)
                .orElseThrow(() -> BusinessException.templateNotFound(templateId));

        taskTemplateMapper.incrementUseCount(templateId);
    }

    // =============================================
    // 내부 헬퍼 메서드
    // =============================================

    /**
     * 템플릿 속성 저장
     */
    private void saveTemplateProperties(Long templateId, List<TemplatePropertyRequest> propertyRequests, String createdBy) {
        if (propertyRequests == null || propertyRequests.isEmpty()) {
            return;
        }

        List<TemplateProperty> properties = new ArrayList<>();
        int sortOrder = 0;

        for (TemplatePropertyRequest request : propertyRequests) {
            TemplateProperty property = TemplateProperty.builder()
                    .templateId(templateId)
                    .propertyId(request.getPropertyId())
                    .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : sortOrder++)
                    .defaultValue(request.getDefaultValue())
                    .requiredYn(request.getRequiredYn() != null ? request.getRequiredYn() : "N")
                    .createdBy(createdBy)
                    .build();
            properties.add(property);
        }

        if (!properties.isEmpty()) {
            templatePropertyMapper.insertBatch(properties);
            log.debug("Template properties saved: templateId={}, count={}", templateId, properties.size());
        }
    }
}
