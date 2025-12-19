package com.taskflow.service;

import com.taskflow.dto.template.TaskTemplateCreateRequest;
import com.taskflow.dto.template.TaskTemplateResponse;
import com.taskflow.dto.template.TaskTemplateSearchResponse;
import com.taskflow.dto.template.TaskTemplateUpdateRequest;

import java.util.List;

/**
 * 작업 템플릿 서비스 인터페이스
 */
public interface TaskTemplateService {

    // =============================================
    // 템플릿 조회
    // =============================================

    /**
     * 템플릿 ID로 조회
     *
     * @param templateId 템플릿 ID
     * @return 템플릿 응답
     */
    TaskTemplateResponse getTemplate(Long templateId);

    /**
     * 전체 템플릿 목록 조회
     *
     * @return 템플릿 목록
     */
    List<TaskTemplateResponse> getAllTemplates();

    /**
     * 활성 템플릿 목록 조회
     *
     * @return 활성 템플릿 목록
     */
    List<TaskTemplateResponse> getActiveTemplates();

    /**
     * 키워드로 템플릿 검색 (자동완성용)
     *
     * @param keyword 검색 키워드
     * @return 검색된 템플릿 목록
     */
    List<TaskTemplateSearchResponse> searchTemplates(String keyword);

    // =============================================
    // 템플릿 등록/수정/삭제
    // =============================================

    /**
     * 템플릿 등록
     *
     * @param request   등록 요청
     * @param createdBy 생성자 ID
     * @return 생성된 템플릿 응답
     */
    TaskTemplateResponse createTemplate(TaskTemplateCreateRequest request, Long createdBy);

    /**
     * 템플릿 수정
     *
     * @param templateId 템플릿 ID
     * @param request    수정 요청
     * @param updatedBy  수정자 ID
     * @return 수정된 템플릿 응답
     */
    TaskTemplateResponse updateTemplate(Long templateId, TaskTemplateUpdateRequest request, Long updatedBy);

    /**
     * 템플릿 삭제
     *
     * @param templateId 템플릿 ID
     */
    void deleteTemplate(Long templateId);

    /**
     * 템플릿 사용 횟수 증가
     *
     * @param templateId 템플릿 ID
     */
    void incrementUseCount(Long templateId);
}
