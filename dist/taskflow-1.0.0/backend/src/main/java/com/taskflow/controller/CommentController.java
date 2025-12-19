package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.comment.CommentCreateRequest;
import com.taskflow.dto.comment.CommentResponse;
import com.taskflow.dto.comment.CommentUpdateRequest;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 댓글 컨트롤러
 *
 * API:
 * - GET /api/items/{itemId}/comments - 댓글 목록
 * - POST /api/items/{itemId}/comments - 댓글 등록
 * - PUT /api/comments/{id} - 댓글 수정
 * - DELETE /api/comments/{id} - 댓글 삭제
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // =============================================
    // 아이템별 댓글 관리 (/api/items/{itemId}/comments)
    // =============================================

    /**
     * 아이템별 댓글 목록 조회
     */
    @GetMapping("/api/items/{itemId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getCommentsByItemId(
            @PathVariable("itemId") Long itemId
    ) {
        log.debug("Get comments by itemId: {}", itemId);

        List<CommentResponse> response = commentService.getCommentsByItemId(itemId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 댓글 등록
     */
    @PostMapping("/api/items/{itemId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable("itemId") Long itemId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        log.info("Create comment: itemId={}, userId={}", itemId, currentUserId);

        CommentResponse response = commentService.createComment(itemId, request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "댓글이 등록되었습니다"));
    }

    // =============================================
    // 댓글 수정/삭제 (/api/comments/{id})
    // =============================================

    /**
     * 댓글 수정
     */
    @PutMapping("/api/comments/{id}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable("id") Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        log.info("Update comment: id={}, userId={}", commentId, currentUserId);

        CommentResponse response = commentService.updateComment(commentId, request, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(response, "댓글이 수정되었습니다"));
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/api/comments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable("id") Long commentId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        log.info("Delete comment: id={}, userId={}", commentId, currentUserId);

        commentService.deleteComment(commentId, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(null, "댓글이 삭제되었습니다"));
    }
}
