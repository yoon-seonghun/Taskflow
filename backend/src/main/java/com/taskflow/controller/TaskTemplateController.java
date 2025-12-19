package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.template.TaskTemplateCreateRequest;
import com.taskflow.dto.template.TaskTemplateResponse;
import com.taskflow.dto.template.TaskTemplateSearchResponse;
import com.taskflow.dto.template.TaskTemplateUpdateRequest;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.TaskTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 작업 템플릿 컨트롤러
 *
 * API:
 * - GET /api/task-templates - 템플릿 목록
 * - POST /api/task-templates - 템플릿 등록
 * - PUT /api/task-templates/{id} - 템플릿 수정
 * - DELETE /api/task-templates/{id} - 템플릿 삭제
 * - GET /api/task-templates/search - 템플릿 검색 (자동완성용)
 */
@Slf4j
@RestController
@RequestMapping("/api/task-templates")
@RequiredArgsConstructor
public class TaskTemplateController {

    private final TaskTemplateService taskTemplateService;

    // =============================================
    // 템플릿 조회
    // =============================================

    /**
     * 템플릿 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskTemplateResponse>>> getTemplates() {
        log.debug("Get all task templates");

        List<TaskTemplateResponse> response = taskTemplateService.getAllTemplates();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 템플릿 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskTemplateResponse>> getTemplate(
            @PathVariable("id") Long templateId
    ) {
        log.debug("Get task template: id={}", templateId);

        TaskTemplateResponse response = taskTemplateService.getTemplate(templateId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 템플릿 검색 (자동완성용)
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<TaskTemplateSearchResponse>>> searchTemplates(
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        log.debug("Search task templates: keyword={}", keyword);

        List<TaskTemplateSearchResponse> response = taskTemplateService.searchTemplates(keyword);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =============================================
    // 템플릿 등록/수정/삭제
    // =============================================

    /**
     * 템플릿 등록
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TaskTemplateResponse>> createTemplate(
            @Valid @RequestBody TaskTemplateCreateRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        log.info("Create task template: content={}, userId={}", request.getContent(), currentUserId);

        TaskTemplateResponse response = taskTemplateService.createTemplate(request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "템플릿이 등록되었습니다"));
    }

    /**
     * 템플릿 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskTemplateResponse>> updateTemplate(
            @PathVariable("id") Long templateId,
            @Valid @RequestBody TaskTemplateUpdateRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        log.info("Update task template: id={}, userId={}", templateId, currentUserId);

        TaskTemplateResponse response = taskTemplateService.updateTemplate(templateId, request, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(response, "템플릿이 수정되었습니다"));
    }

    /**
     * 템플릿 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable("id") Long templateId
    ) {
        log.info("Delete task template: id={}", templateId);

        taskTemplateService.deleteTemplate(templateId);

        return ResponseEntity.ok(ApiResponse.success(null, "템플릿이 삭제되었습니다"));
    }

    /**
     * 템플릿 사용 (사용 횟수 증가)
     */
    @PostMapping("/{id}/use")
    public ResponseEntity<ApiResponse<Void>> useTemplate(
            @PathVariable("id") Long templateId
    ) {
        log.debug("Use task template: id={}", templateId);

        taskTemplateService.incrementUseCount(templateId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
