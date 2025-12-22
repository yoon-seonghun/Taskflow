package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.share.ShareRequest;
import com.taskflow.dto.share.ShareResponse;
import com.taskflow.dto.share.ShareUpdateRequest;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.ItemShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 업무 공유 컨트롤러
 *
 * API:
 * - GET /api/items/{itemId}/shares - 업무 공유 목록 조회
 * - POST /api/items/{itemId}/shares - 업무 공유 추가
 * - PUT /api/items/{itemId}/shares/{userId} - 공유 권한 변경
 * - DELETE /api/items/{itemId}/shares/{userId} - 업무 공유 제거
 */
@Slf4j
@RestController
@RequestMapping("/api/items/{itemId}/shares")
@RequiredArgsConstructor
public class ItemShareController {

    private final ItemShareService itemShareService;

    /**
     * 업무 공유 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShareResponse>>> getShares(
            @PathVariable("itemId") Long itemId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("Get item shares: itemId={}", itemId);

        // 접근 권한 확인
        if (!itemShareService.hasAccess(itemId, currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("업무에 접근 권한이 없습니다"));
        }

        List<ShareResponse> response = itemShareService.getShares(itemId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 업무 공유 추가
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addShare(
            @PathVariable("itemId") Long itemId,
            @Valid @RequestBody ShareRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Add item share: itemId={}, userId={}", itemId, request.getUserId());

        // 수정 권한 확인
        if (!itemShareService.canEdit(itemId, currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("업무 공유 권한이 없습니다"));
        }

        itemShareService.addShare(itemId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("공유가 추가되었습니다"));
    }

    /**
     * 공유 권한 변경
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> updatePermission(
            @PathVariable("itemId") Long itemId,
            @PathVariable("userId") Long userId,
            @Valid @RequestBody ShareUpdateRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Update item share permission: itemId={}, userId={}, permission={}",
                itemId, userId, request.getPermission());

        // 수정 권한 확인
        if (!itemShareService.canEdit(itemId, currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("권한 변경 권한이 없습니다"));
        }

        itemShareService.updatePermission(itemId, userId, request.getPermission(), currentUserId);
        return ResponseEntity.ok(ApiResponse.successWithMessage("권한이 변경되었습니다"));
    }

    /**
     * 업무 공유 제거
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeShare(
            @PathVariable("itemId") Long itemId,
            @PathVariable("userId") Long userId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Remove item share: itemId={}, userId={}", itemId, userId);

        // 삭제 권한 확인
        if (!itemShareService.canEdit(itemId, currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("공유 제거 권한이 없습니다"));
        }

        itemShareService.removeShare(itemId, userId, currentUserId);
        return ResponseEntity.ok(ApiResponse.successWithMessage("공유가 해제되었습니다"));
    }
}
