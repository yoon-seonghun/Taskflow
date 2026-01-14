package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.domain.Department;
import com.taskflow.domain.User;
import com.taskflow.dto.template.TaskTemplateCreateRequest;
import com.taskflow.dto.template.TaskTemplateResponse;
import com.taskflow.dto.template.TaskTemplateSearchResponse;
import com.taskflow.dto.template.TaskTemplateUpdateRequest;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.DepartmentMapper;
import com.taskflow.mapper.UserMapper;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.TaskTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 작업 템플릿 컨트롤러
 *
 * API:
 * - GET /api/task-templates - 템플릿 목록
 * - POST /api/task-templates - 템플릿 등록
 * - PUT /api/task-templates/{id} - 템플릿 수정
 * - DELETE /api/task-templates/{id} - 템플릿 삭제
 * - GET /api/task-templates/search - 템플릿 검색 (자동완성용)
 * - GET /api/task-templates/global - 글로벌 템플릿 목록
 * - POST /api/task-templates/global - 글로벌 템플릿 등록
 * - GET /api/task-templates/manager - 매니저 템플릿 목록
 * - POST /api/task-templates/manager - 매니저 템플릿 등록
 * - GET /api/task-templates/user - 개인 템플릿 목록
 * - POST /api/task-templates/user - 개인 템플릿 등록
 * - GET /api/task-templates/accessible - 접근 가능 전체 템플릿 목록
 */
@Slf4j
@RestController
@RequestMapping("/api/task-templates")
@RequiredArgsConstructor
public class TaskTemplateController {

    private final TaskTemplateService taskTemplateService;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;

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
    // 소유 유형별 조회
    // =============================================

    /**
     * 글로벌 템플릿 목록 조회
     */
    @GetMapping("/global")
    public ResponseEntity<ApiResponse<List<TaskTemplateResponse>>> getGlobalTemplates() {
        log.debug("Get global task templates");

        List<TaskTemplateResponse> response = taskTemplateService.getGlobalTemplates();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 매니저 템플릿 목록 조회
     */
    @GetMapping("/manager")
    public ResponseEntity<ApiResponse<List<TaskTemplateResponse>>> getManagerTemplates() {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.debug("Get manager task templates: username={}", currentUsername);

        User user = userMapper.findByUsername(currentUsername)
                .orElseThrow(() -> BusinessException.userNotFound(currentUsername));
        List<String> departmentCodes = getAccessibleDepartmentCodes(user.getDepartmentCode());

        List<TaskTemplateResponse> response = taskTemplateService.getManagerTemplates(currentUsername, departmentCodes);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 개인 템플릿 목록 조회
     */
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<TaskTemplateResponse>>> getUserTemplates() {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.debug("Get user task templates: username={}", currentUsername);

        List<TaskTemplateResponse> response = taskTemplateService.getUserTemplates(currentUsername);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 접근 가능한 전체 템플릿 목록 조회
     */
    @GetMapping("/accessible")
    public ResponseEntity<ApiResponse<List<TaskTemplateResponse>>> getAccessibleTemplates() {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.debug("Get accessible task templates: username={}", currentUsername);

        User user = userMapper.findByUsername(currentUsername)
                .orElseThrow(() -> BusinessException.userNotFound(currentUsername));
        List<String> departmentCodes = getAccessibleDepartmentCodes(user.getDepartmentCode());

        List<TaskTemplateResponse> response = taskTemplateService.getAccessibleTemplates(currentUsername, departmentCodes);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =============================================
    // 소유 유형별 등록
    // =============================================

    /**
     * 글로벌 템플릿 등록 (ADMIN 권한 필요)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/global")
    public ResponseEntity<ApiResponse<TaskTemplateResponse>> createGlobalTemplate(
            @Valid @RequestBody TaskTemplateCreateRequest request
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Create global task template: content={}, username={}", request.getContent(), currentUsername);

        TaskTemplateResponse response = taskTemplateService.createGlobalTemplate(request, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "글로벌 템플릿이 등록되었습니다"));
    }

    /**
     * 매니저 템플릿 등록 (MANAGER 권한 필요)
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/manager")
    public ResponseEntity<ApiResponse<TaskTemplateResponse>> createManagerTemplate(
            @Valid @RequestBody TaskTemplateCreateRequest request
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Create manager task template: content={}, username={}", request.getContent(), currentUsername);

        User user = userMapper.findByUsername(currentUsername)
                .orElseThrow(() -> BusinessException.userNotFound(currentUsername));

        TaskTemplateResponse response = taskTemplateService.createManagerTemplate(request, currentUsername, user.getDepartmentCode());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "매니저 템플릿이 등록되었습니다"));
    }

    /**
     * 개인 템플릿 등록
     */
    @PostMapping("/user")
    public ResponseEntity<ApiResponse<TaskTemplateResponse>> createUserTemplate(
            @Valid @RequestBody TaskTemplateCreateRequest request
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Create user task template: content={}, username={}", request.getContent(), currentUsername);

        TaskTemplateResponse response = taskTemplateService.createUserTemplate(request, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "개인 템플릿이 등록되었습니다"));
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
        String currentUsername = SecurityUtils.getCurrentUsername();

        log.info("Create task template: content={}, username={}", request.getContent(), currentUsername);

        TaskTemplateResponse response = taskTemplateService.createTemplate(request, currentUsername);

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
        String currentUsername = SecurityUtils.getCurrentUsername();

        log.info("Update task template: id={}, username={}", templateId, currentUsername);

        TaskTemplateResponse response = taskTemplateService.updateTemplate(templateId, request, currentUsername);

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

    // =============================================
    // 내부 헬퍼 메서드
    // =============================================

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
}
