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
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.debug("Get item shares: itemId={}", itemId);

        // 접근 권한 확인
        if (!itemShareService.hasAccess(itemId, currentUsername)) {
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
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Add item share: itemId={}, username={}", itemId, request.getUsername());

        // 수정 권한 확인
        if (!itemShareService.canEdit(itemId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("업무 공유 권한이 없습니다"));
        }

        itemShareService.addShare(itemId, request, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("공유가 추가되었습니다"));
    }

    /**
     * 공유 권한 변경
     */
    @PutMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> updatePermission(
            @PathVariable("itemId") Long itemId,
            @PathVariable("username") String username,
            @Valid @RequestBody ShareUpdateRequest request
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Update item share permission: itemId={}, username={}, permission={}",
                itemId, username, request.getPermission());

        // 수정 권한 확인
        if (!itemShareService.canEdit(itemId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("권한 변경 권한이 없습니다"));
        }

        itemShareService.updatePermission(itemId, username, request.getPermission(), currentUsername);
        return ResponseEntity.ok(ApiResponse.successWithMessage("권한이 변경되었습니다"));
    }

    /**
     * 업무 공유 제거
     */
    @DeleteMapping("/{username}")
    public ResponseEntity<ApiResponse<Void>> removeShare(
            @PathVariable("itemId") Long itemId,
            @PathVariable("username") String username
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Remove item share: itemId={}, username={}", itemId, username);

        // 삭제 권한 확인
        if (!itemShareService.canEdit(itemId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("공유 제거 권한이 없습니다"));
        }

        itemShareService.removeShare(itemId, username, currentUsername);
        return ResponseEntity.ok(ApiResponse.successWithMessage("공유가 해제되었습니다"));
    }
}
