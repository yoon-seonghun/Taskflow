package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.group.*;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.GroupService;
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
 * 그룹 컨트롤러
 *
 * 권한: 모든 인증된 사용자 (향후 ADMIN 역할로 제한 가능)
 *
 * API:
 * - GET /api/groups - 그룹 목록
 * - POST /api/groups - 그룹 생성
 * - GET /api/groups/{id} - 그룹 조회
 * - PUT /api/groups/{id} - 그룹 수정
 * - DELETE /api/groups/{id} - 그룹 삭제
 * - PUT /api/groups/{id}/order - 그룹 순서 변경
 * - GET /api/groups/{id}/members - 그룹 멤버 목록
 * - POST /api/groups/{id}/members - 그룹 멤버 추가
 * - DELETE /api/groups/{id}/members/{userId} - 그룹 멤버 제거
 * - GET /api/groups/check-code - 그룹 코드 중복 확인
 * - GET /api/users/{userId}/groups - 사용자별 소속 그룹 목록
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class GroupController {

    private final GroupService groupService;

    // =============================================
    // 그룹 CRUD
    // =============================================

    /**
     * 그룹 목록 조회
     */
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getGroups(
            @RequestParam(value = "useYn", required = false) String useYn
    ) {
        log.debug("Get groups: useYn={}", useYn);

        List<GroupResponse> response = groupService.getGroups(useYn);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 그룹 생성
     */
    @PostMapping("/groups")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Valid @RequestBody GroupCreateRequest request
    ) {
        log.info("Create group: code={}", request.getGroupCode());

        Long currentUserId = SecurityUtils.getCurrentUserId();
        GroupResponse response = groupService.createGroup(request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "그룹이 등록되었습니다"));
    }

    /**
     * 그룹 조회
     */
    @GetMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<GroupResponse>> getGroup(
            @PathVariable("id") Long groupId
    ) {
        log.debug("Get group: id={}", groupId);

        GroupResponse response = groupService.getGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 그룹 수정
     */
    @PutMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroup(
            @PathVariable("id") Long groupId,
            @Valid @RequestBody GroupUpdateRequest request
    ) {
        log.info("Update group: id={}", groupId);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        GroupResponse response = groupService.updateGroup(groupId, request, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(response, "그룹 정보가 수정되었습니다"));
    }

    /**
     * 그룹 삭제
     */
    @DeleteMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(
            @PathVariable("id") Long groupId
    ) {
        log.info("Delete group: id={}", groupId);

        groupService.deleteGroup(groupId);
        return ResponseEntity.ok(ApiResponse.successWithMessage("그룹이 삭제되었습니다"));
    }

    /**
     * 그룹 순서 변경
     */
    @PutMapping("/groups/{id}/order")
    public ResponseEntity<ApiResponse<GroupResponse>> updateGroupOrder(
            @PathVariable("id") Long groupId,
            @Valid @RequestBody GroupOrderRequest request
    ) {
        log.info("Update group order: id={}, order={}", groupId, request.getSortOrder());

        Long currentUserId = SecurityUtils.getCurrentUserId();
        GroupResponse response = groupService.updateGroupOrder(groupId, request, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(response, "그룹 순서가 변경되었습니다"));
    }

    /**
     * 그룹 코드 중복 확인
     */
    @GetMapping("/groups/check-code")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkCode(
            @RequestParam("code") String groupCode
    ) {
        log.debug("Check group code: {}", groupCode);

        boolean exists = groupService.existsByCode(groupCode);
        return ResponseEntity.ok(ApiResponse.success(Map.of("exists", exists)));
    }

    // =============================================
    // 그룹 멤버 관리
    // =============================================

    /**
     * 그룹 멤버 목록 조회
     */
    @GetMapping("/groups/{id}/members")
    public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> getGroupMembers(
            @PathVariable("id") Long groupId
    ) {
        log.debug("Get group members: groupId={}", groupId);

        List<GroupMemberResponse> response = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 그룹 멤버 추가
     */
    @PostMapping("/groups/{id}/members")
    public ResponseEntity<ApiResponse<GroupMemberResponse>> addGroupMember(
            @PathVariable("id") Long groupId,
            @Valid @RequestBody GroupMemberRequest request
    ) {
        log.info("Add group member: groupId={}, userId={}", groupId, request.getUserId());

        Long currentUserId = SecurityUtils.getCurrentUserId();
        GroupMemberResponse response = groupService.addGroupMember(groupId, request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "멤버가 추가되었습니다"));
    }

    /**
     * 그룹 멤버 제거
     */
    @DeleteMapping("/groups/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeGroupMember(
            @PathVariable("id") Long groupId,
            @PathVariable("userId") Long userId
    ) {
        log.info("Remove group member: groupId={}, userId={}", groupId, userId);

        groupService.removeGroupMember(groupId, userId);
        return ResponseEntity.ok(ApiResponse.successWithMessage("멤버가 제거되었습니다"));
    }

    // =============================================
    // 사용자별 그룹 조회
    // =============================================

    /**
     * 사용자별 소속 그룹 목록 조회
     */
    @GetMapping("/users/{userId}/groups")
    public ResponseEntity<ApiResponse<List<GroupMemberResponse>>> getUserGroups(
            @PathVariable("userId") Long userId
    ) {
        log.debug("Get user groups: userId={}", userId);

        List<GroupMemberResponse> response = groupService.getUserGroups(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
