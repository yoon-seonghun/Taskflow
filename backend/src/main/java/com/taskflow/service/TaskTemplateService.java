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
    // 소유 유형별 조회
    // =============================================

    /**
     * 글로벌 템플릿 목록 조회
     *
     * @return 글로벌 템플릿 목록
     */
    List<TaskTemplateResponse> getGlobalTemplates();

    /**
     * 매니저 템플릿 목록 조회
     *
     * @param username        사용자 USERNAME
     * @param departmentCodes 부서 코드 목록
     * @return 매니저 템플릿 목록
     */
    List<TaskTemplateResponse> getManagerTemplates(String username, List<String> departmentCodes);

    /**
     * 개인 템플릿 목록 조회
     *
     * @param username 사용자 USERNAME
     * @return 개인 템플릿 목록
     */
    List<TaskTemplateResponse> getUserTemplates(String username);

    /**
     * 접근 가능한 전체 템플릿 목록 조회
     *
     * @param username        사용자 USERNAME
     * @param departmentCodes 부서 코드 목록
     * @return 접근 가능한 템플릿 목록
     */
    List<TaskTemplateResponse> getAccessibleTemplates(String username, List<String> departmentCodes);

    // =============================================
    // 소유 유형별 등록
    // =============================================

    /**
     * 글로벌 템플릿 등록
     *
     * @param request   등록 요청
     * @param createdBy 생성자 USERNAME
     * @return 생성된 템플릿 응답
     */
    TaskTemplateResponse createGlobalTemplate(TaskTemplateCreateRequest request, String createdBy);

    /**
     * 매니저 템플릿 등록
     *
     * @param request        등록 요청
     * @param createdBy      생성자 USERNAME
     * @param departmentCode 부서 코드
     * @return 생성된 템플릿 응답
     */
    TaskTemplateResponse createManagerTemplate(TaskTemplateCreateRequest request, String createdBy, String departmentCode);

    /**
     * 개인 템플릿 등록
     *
     * @param request   등록 요청
     * @param createdBy 생성자 USERNAME
     * @return 생성된 템플릿 응답
     */
    TaskTemplateResponse createUserTemplate(TaskTemplateCreateRequest request, String createdBy);

    // =============================================
    // 템플릿 등록/수정/삭제
    // =============================================

    /**
     * 템플릿 등록
     *
     * @param request   등록 요청
     * @param createdBy 생성자 USERNAME
     * @return 생성된 템플릿 응답
     */
    TaskTemplateResponse createTemplate(TaskTemplateCreateRequest request, String createdBy);

    /**
     * 템플릿 수정
     *
     * @param templateId 템플릿 ID
     * @param request    수정 요청
     * @param updatedBy  수정자 USERNAME
     * @return 수정된 템플릿 응답
     */
    TaskTemplateResponse updateTemplate(Long templateId, TaskTemplateUpdateRequest request, String updatedBy);

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
