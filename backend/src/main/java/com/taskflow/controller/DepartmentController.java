package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.department.*;
import com.taskflow.dto.user.UserResponse;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 부서 컨트롤러
 *
 * 권한: 모든 인증된 사용자 (향후 ADMIN 역할로 제한 가능)
 *
 * API:
 * - GET /api/departments - 부서 목록 (트리 구조)
 * - GET /api/departments/flat - 부서 목록 (평면 구조)
 * - POST /api/departments - 부서 생성
 * - GET /api/departments/{id} - 부서 조회
 * - PUT /api/departments/{id} - 부서 수정
 * - DELETE /api/departments/{id} - 부서 삭제
 * - PUT /api/departments/{id}/order - 부서 순서 변경
 * - GET /api/departments/{id}/users - 부서별 사용자 목록
 * - GET /api/departments/{id}/children - 하위 부서 목록
 * - GET /api/departments/check-code - 부서 코드 중복 확인
 */
@Slf4j
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 부서 목록 조회 (트리 구조)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentTreeResponse>>> getDepartments(
            @RequestParam(value = "useYn", required = false) String useYn
    ) {
        log.debug("Get departments tree: useYn={}", useYn);

        List<DepartmentTreeResponse> response = departmentService.getDepartmentTree(useYn);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 부서 목록 조회 (평면 구조)
     * SELECT 박스용
     */
    @GetMapping("/flat")
    public ResponseEntity<ApiResponse<List<DepartmentFlatResponse>>> getDepartmentsFlat(
            @RequestParam(value = "useYn", required = false) String useYn
    ) {
        log.debug("Get departments flat: useYn={}", useYn);

        List<DepartmentFlatResponse> response = departmentService.getDepartmentsFlat(useYn);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 부서 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody DepartmentCreateRequest request
    ) {
        log.info("Create department: code={}", request.getDepartmentCode());

        String currentUsername = SecurityUtils.getCurrentUsername();
        DepartmentResponse response = departmentService.createDepartment(request, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "부서가 등록되었습니다"));
    }

    /**
     * 부서 조회 (코드 기반)
     */
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartment(
            @PathVariable("code") String departmentCode
    ) {
        log.debug("Get department: code={}", departmentCode);

        DepartmentResponse response = departmentService.getDepartment(departmentCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 부서 수정
     */
    @PutMapping("/{code}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable("code") String departmentCode,
            @Valid @RequestBody DepartmentUpdateRequest request
    ) {
        log.info("Update department: code={}", departmentCode);

        String currentUsername = SecurityUtils.getCurrentUsername();
        DepartmentResponse response = departmentService.updateDepartment(departmentCode, request, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(response, "부서 정보가 수정되었습니다"));
    }

    /**
     * 부서 삭제
     */
    @DeleteMapping("/{code}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(
            @PathVariable("code") String departmentCode
    ) {
        log.info("Delete department: code={}", departmentCode);

        departmentService.deleteDepartment(departmentCode);
        return ResponseEntity.ok(ApiResponse.successWithMessage("부서가 삭제되었습니다"));
    }

    /**
     * 부서 순서 변경
     */
    @PutMapping("/{code}/order")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartmentOrder(
            @PathVariable("code") String departmentCode,
            @Valid @RequestBody DepartmentOrderRequest request
    ) {
        log.info("Update department order: code={}, order={}", departmentCode, request.getSortOrder());

        String currentUsername = SecurityUtils.getCurrentUsername();
        DepartmentResponse response = departmentService.updateDepartmentOrder(departmentCode, request, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(response, "부서 순서가 변경되었습니다"));
    }

    /**
     * 부서별 사용자 목록 조회
     */
    @GetMapping("/{code}/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getDepartmentUsers(
            @PathVariable("code") String departmentCode
    ) {
        log.debug("Get department users: departmentCode={}", departmentCode);

        List<UserResponse> response = departmentService.getDepartmentUsers(departmentCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 하위 부서 목록 조회
     */
    @GetMapping("/{code}/children")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getChildDepartments(
            @PathVariable("code") String departmentCode
    ) {
        log.debug("Get child departments: parentCode={}", departmentCode);

        List<DepartmentResponse> response = departmentService.getChildDepartments(departmentCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 부서 코드 중복 확인
     */
    @GetMapping("/check-code")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkCode(
            @RequestParam("code") String departmentCode
    ) {
        log.debug("Check department code: {}", departmentCode);

        boolean exists = departmentService.existsByCode(departmentCode);
        return ResponseEntity.ok(ApiResponse.success(Map.of("exists", exists)));
    }
}
