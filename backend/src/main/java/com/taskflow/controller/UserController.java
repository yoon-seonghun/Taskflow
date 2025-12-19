package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.common.LogMaskUtils;
import com.taskflow.common.PageResponse;
import com.taskflow.dto.user.*;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.UserService;
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
 * 사용자 컨트롤러
 *
 * 권한: 모든 인증된 사용자 (향후 ADMIN 역할로 제한 가능)
 *
 * API:
 * - GET /api/users - 사용자 목록 조회
 * - POST /api/users - 사용자 등록
 * - GET /api/users/{id} - 사용자 조회
 * - PUT /api/users/{id} - 사용자 수정
 * - DELETE /api/users/{id} - 사용자 삭제
 * - PUT /api/users/{id}/password - 비밀번호 변경
 * - GET /api/users/check-username - 아이디 중복 확인
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {

    private final UserService userService;

    /**
     * 사용자 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            UserSearchRequest request
    ) {
        log.debug("Get users: page={}, size={}, keyword={}",
                request.getPage(), request.getSize(), request.getKeyword());

        PageResponse<UserResponse> response = userService.getUsers(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 등록
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request
    ) {
        log.info("Create user: username={}", LogMaskUtils.maskUsername(request.getUsername()));

        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserResponse response = userService.createUser(request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "사용자가 등록되었습니다"));
    }

    /**
     * 사용자 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable("id") Long userId
    ) {
        log.debug("Get user: userId={}", userId);

        UserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable("id") Long userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        log.info("Update user: userId={}", userId);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserResponse response = userService.updateUser(userId, request, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(response, "사용자 정보가 수정되었습니다"));
    }

    /**
     * 사용자 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable("id") Long userId
    ) {
        log.info("Delete user: userId={}", userId);

        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.successWithMessage("사용자가 삭제되었습니다"));
    }

    /**
     * 비밀번호 변경
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable("id") Long userId,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        log.info("Change password: userId={}", userId);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        userService.changePassword(userId, request, currentUserId);

        return ResponseEntity.ok(ApiResponse.successWithMessage("비밀번호가 변경되었습니다"));
    }

    /**
     * 아이디 중복 확인
     */
    @GetMapping("/check-username")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkUsername(
            @RequestParam("username") String username
    ) {
        log.debug("Check username: {}", LogMaskUtils.maskUsername(username));

        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(Map.of("exists", exists)));
    }

    /**
     * 부서별 사용자 목록 조회
     *
     * @deprecated DepartmentController의 /api/departments/{id}/users 사용 권장
     * @see com.taskflow.controller.DepartmentController#getUsersByDepartment
     */
    @Deprecated
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByDepartment(
            @PathVariable("departmentId") Long departmentId
    ) {
        log.debug("Get users by department: departmentId={}", departmentId);

        List<UserResponse> response = userService.getUsersByDepartment(departmentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
