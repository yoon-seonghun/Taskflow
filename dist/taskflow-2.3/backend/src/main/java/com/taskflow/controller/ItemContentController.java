package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.content.ItemContentResponse;
import com.taskflow.dto.content.ItemContentUpdateRequest;
import com.taskflow.exception.BusinessException;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.BoardService;
import com.taskflow.service.ItemContentService;
import com.taskflow.service.ItemShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 업무 상세 내용 컨트롤러
 *
 * API:
 * - GET /api/boards/{boardId}/items/{itemId}/content - 업무 내용 조회
 * - PUT /api/boards/{boardId}/items/{itemId}/content - 업무 내용 저장/수정
 */
@Slf4j
@RestController
@RequestMapping("/api/boards/{boardId}/items/{itemId}/content")
@RequiredArgsConstructor
public class ItemContentController {

    private final ItemContentService itemContentService;
    private final BoardService boardService;
    private final ItemShareService itemShareService;

    /**
     * 업무 내용 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ItemContentResponse>> getContent(
            @PathVariable("boardId") Long boardId,
            @PathVariable("itemId") Long itemId
    ) {
        // 접근 권한 확인: 보드 권한 또는 업무 공유/배당 권한
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
        boolean hasItemAccess = itemShareService.hasItemAccess(itemId, currentUsername);

        if (!hasBoardAccess && !hasItemAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("접근 권한이 없습니다"));
        }

        ItemContentResponse content = itemContentService.getContent(boardId, itemId);
        return ResponseEntity.ok(ApiResponse.success(content));
    }

    /**
     * 업무 내용 저장/수정
     */
    @PutMapping
    public ResponseEntity<ApiResponse<ItemContentResponse>> updateContent(
            @PathVariable("boardId") Long boardId,
            @PathVariable("itemId") Long itemId,
            @Valid @RequestBody ItemContentUpdateRequest request
    ) {
        // 접근 권한 확인: 보드 권한 또는 업무 공유/배당 권한
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
        boolean hasItemAccess = itemShareService.hasItemAccess(itemId, currentUsername);

        if (!hasBoardAccess && !hasItemAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("접근 권한이 없습니다"));
        }

        try {
            ItemContentResponse content = itemContentService.updateContent(boardId, itemId, request);
            return ResponseEntity.ok(ApiResponse.success(content, "저장되었습니다."));
        } catch (BusinessException e) {
            if ("VERSION_CONFLICT".equals(e.getErrorCode())) {
                // 버전 충돌 응답
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error(e.getMessage()));
            }
            throw e;
        }
    }
}
