package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.board.*;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 보드 컨트롤러
 *
 * API:
 * - GET /api/boards - 보드 목록 (접근 가능한 보드)
 * - POST /api/boards - 보드 생성
 * - GET /api/boards/{id} - 보드 조회
 * - PUT /api/boards/{id} - 보드 수정
 * - DELETE /api/boards/{id} - 보드 삭제
 * - GET /api/boards/{id}/shares - 공유 사용자 목록
 * - POST /api/boards/{id}/shares - 공유 사용자 추가
 * - DELETE /api/boards/{id}/shares/{userId} - 공유 사용자 제거
 */
@Slf4j
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    // =============================================
    // 보드 CRUD
    // =============================================

    /**
     * 보드 목록 조회
     * 현재 사용자가 접근 가능한 보드 목록 (소유 + 공유)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BoardResponse>>> getBoards(
            @RequestParam(value = "useYn", required = false) String useYn,
            @RequestParam(value = "owned", required = false, defaultValue = "false") boolean ownedOnly
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("Get boards: userId={}, useYn={}, ownedOnly={}", currentUserId, useYn, ownedOnly);

        List<BoardResponse> response;
        if (ownedOnly) {
            response = boardService.getOwnedBoards(currentUserId, useYn);
        } else {
            response = boardService.getAccessibleBoards(currentUserId, useYn);
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 보드 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BoardResponse>> createBoard(
            @Valid @RequestBody BoardCreateRequest request
    ) {
        log.info("Create board: name={}", request.getBoardName());

        Long currentUserId = SecurityUtils.getCurrentUserId();
        BoardResponse response = boardService.createBoard(request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "보드가 생성되었습니다"));
    }

    /**
     * 보드 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardResponse>> getBoard(
            @PathVariable("id") Long boardId
    ) {
        log.debug("Get board: id={}", boardId);

        // 접근 권한 확인
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!boardService.hasAccess(boardId, currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드에 접근 권한이 없습니다"));
        }

        BoardResponse response = boardService.getBoard(boardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 보드 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardResponse>> updateBoard(
            @PathVariable("id") Long boardId,
            @Valid @RequestBody BoardUpdateRequest request
    ) {
        log.info("Update board: id={}", boardId);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        BoardResponse response = boardService.updateBoard(boardId, request, currentUserId);

        return ResponseEntity.ok(ApiResponse.success(response, "보드가 수정되었습니다"));
    }

    /**
     * 보드 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBoard(
            @PathVariable("id") Long boardId
    ) {
        log.info("Delete board: id={}", boardId);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        boardService.deleteBoard(boardId, currentUserId);

        return ResponseEntity.ok(ApiResponse.successWithMessage("보드가 삭제되었습니다"));
    }

    // =============================================
    // 보드 공유 관리
    // =============================================

    /**
     * 공유 사용자 목록 조회
     */
    @GetMapping("/{id}/shares")
    public ResponseEntity<ApiResponse<List<BoardShareResponse>>> getBoardShares(
            @PathVariable("id") Long boardId
    ) {
        log.debug("Get board shares: boardId={}", boardId);

        // 접근 권한 확인
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!boardService.hasAccess(boardId, currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드에 접근 권한이 없습니다"));
        }

        List<BoardShareResponse> response = boardService.getBoardShares(boardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 공유 사용자 추가
     */
    @PostMapping("/{id}/shares")
    public ResponseEntity<ApiResponse<BoardShareResponse>> addBoardShare(
            @PathVariable("id") Long boardId,
            @Valid @RequestBody BoardShareRequest request
    ) {
        log.info("Add board share: boardId={}, userId={}", boardId, request.getUserId());

        Long currentUserId = SecurityUtils.getCurrentUserId();
        BoardShareResponse response = boardService.addBoardShare(boardId, request, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "공유 사용자가 추가되었습니다"));
    }

    /**
     * 공유 사용자 제거
     */
    @DeleteMapping("/{id}/shares/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeBoardShare(
            @PathVariable("id") Long boardId,
            @PathVariable("userId") Long userId
    ) {
        log.info("Remove board share: boardId={}, userId={}", boardId, userId);

        Long currentUserId = SecurityUtils.getCurrentUserId();
        boardService.removeBoardShare(boardId, userId, currentUserId);

        return ResponseEntity.ok(ApiResponse.successWithMessage("공유가 해제되었습니다"));
    }
}
