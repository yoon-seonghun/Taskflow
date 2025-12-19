package com.taskflow.service.impl;

import com.taskflow.domain.TaskTemplate;
import com.taskflow.dto.template.TaskTemplateCreateRequest;
import com.taskflow.dto.template.TaskTemplateResponse;
import com.taskflow.dto.template.TaskTemplateSearchResponse;
import com.taskflow.dto.template.TaskTemplateUpdateRequest;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.TaskTemplateMapper;
import com.taskflow.service.TaskTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        return TaskTemplateResponse.from(template);
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
    // 템플릿 등록/수정/삭제
    // =============================================

    @Override
    @Transactional
    public TaskTemplateResponse createTemplate(TaskTemplateCreateRequest request, Long createdBy) {
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
        Long defaultAssigneeId = request.getDefaultAssigneeId();
        if (defaultAssigneeId == null) {
            defaultAssigneeId = createdBy;
        }

        // 템플릿 엔티티 생성
        TaskTemplate template = TaskTemplate.builder()
                .content(request.getContent())
                .defaultAssigneeId(defaultAssigneeId)
                .defaultItemStatus(defaultItemStatus)
                .status(TaskTemplate.STATUS_ACTIVE)
                .sortOrder(sortOrder)
                .useCount(0)
                .createdBy(createdBy)
                .build();

        // 템플릿 저장
        taskTemplateMapper.insert(template);
        log.info("Task template created: id={}", template.getTemplateId());

        return getTemplate(template.getTemplateId());
    }

    @Override
    @Transactional
    public TaskTemplateResponse updateTemplate(Long templateId, TaskTemplateUpdateRequest request, Long updatedBy) {
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
        if (request.getDefaultAssigneeId() != null) {
            template.setDefaultAssigneeId(request.getDefaultAssigneeId());
        }
        if (request.getDefaultItemStatus() != null) {
            template.setDefaultItemStatus(request.getDefaultItemStatus());
        }
        if (request.getStatus() != null) {
            template.setStatus(request.getStatus());
        }
        if (request.getSortOrder() != null) {
            template.setSortOrder(request.getSortOrder());
        }
        template.setUpdatedBy(updatedBy);

        taskTemplateMapper.update(template);
        log.info("Task template updated: id={}", templateId);

        return getTemplate(templateId);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long templateId) {
        log.info("Deleting task template: id={}", templateId);

        // 템플릿 존재 확인
        taskTemplateMapper.findById(templateId)
                .orElseThrow(() -> BusinessException.templateNotFound(templateId));

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
}
