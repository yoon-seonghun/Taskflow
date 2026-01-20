package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.board.*;
import com.taskflow.dto.share.ShareUpdateRequest;
import com.taskflow.dto.transfer.TransferPreviewResponse;
import com.taskflow.dto.transfer.TransferResultResponse;
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
 * - GET /api/boards/list - 보드 목록 (소유/공유 분리)
 * - POST /api/boards - 보드 생성
 * - GET /api/boards/{id} - 보드 조회
 * - PUT /api/boards/{id} - 보드 수정
 * - DELETE /api/boards/{id} - 보드 삭제
 * - PUT /api/boards/{id}/order - 보드 순서 변경
 * - DELETE /api/boards/{id}/with-transfer - 보드 삭제 (이관 포함)
 * - GET /api/boards/{id}/transfer-preview - 이관 미리보기
 * - GET /api/boards/{id}/shares - 공유 사용자 목록
 * - POST /api/boards/{id}/shares - 공유 사용자 추가
 * - PUT /api/boards/{id}/shares/{userId} - 공유 권한 변경
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
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.debug("Get boards: username={}, useYn={}, ownedOnly={}", currentUsername, useYn, ownedOnly);

        List<BoardResponse> response;
        if (ownedOnly) {
            response = boardService.getOwnedBoards(currentUsername, useYn);
        } else {
            response = boardService.getAccessibleBoards(currentUsername, useYn);
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 보드 목록 조회 (소유/공유 분리)
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<BoardListResponse>> getBoardList() {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.debug("Get board list: username={}", currentUsername);

        BoardListResponse response = boardService.getBoardList(currentUsername);
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

        String currentUsername = SecurityUtils.getCurrentUsername();
        BoardResponse response = boardService.createBoard(request, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "보드가 생성되었습니다"));
    }

    /**
     * 보드 조회 (권한 정보 포함)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BoardResponse>> getBoard(
            @PathVariable("id") Long boardId
    ) {
        log.debug("Get board: id={}", boardId);

        String currentUsername = SecurityUtils.getCurrentUsername();

        // getBoardWithPermission은 내부에서 접근 권한도 확인하고 isOwner도 설정
        BoardResponse response = boardService.getBoardWithPermission(boardId, currentUsername);
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

        String currentUsername = SecurityUtils.getCurrentUsername();
        BoardResponse response = boardService.updateBoard(boardId, request, currentUsername);

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

        String currentUsername = SecurityUtils.getCurrentUsername();
        boardService.deleteBoard(boardId, currentUsername);

        return ResponseEntity.ok(ApiResponse.successWithMessage("보드가 삭제되었습니다"));
    }

    // =============================================
    // 보드 관리 (신규 기능)
    // =============================================

    /**
     * 보드 순서 변경 (소유 보드)
     */
    @PutMapping("/{id}/order")
    public ResponseEntity<ApiResponse<Void>> updateBoardOrder(
            @PathVariable("id") Long boardId,
            @Valid @RequestBody BoardOrderRequest request
    ) {
        log.info("Update board order: boardId={}, sortOrder={}", boardId, request.getSortOrder());

        String currentUsername = SecurityUtils.getCurrentUsername();
        boardService.updateBoardOrder(boardId, request.getSortOrder(), currentUsername);

        return ResponseEntity.ok(ApiResponse.successWithMessage("보드 순서가 변경되었습니다"));
    }

    /**
     * 공유받은 보드 순서 변경
     */
    @PutMapping("/{id}/shares/order")
    public ResponseEntity<ApiResponse<Void>> updateSharedBoardOrder(
            @PathVariable("id") Long boardId,
            @Valid @RequestBody BoardOrderRequest request
    ) {
        log.info("Update shared board order: boardId={}, sortOrder={}", boardId, request.getSortOrder());

        String currentUsername = SecurityUtils.getCurrentUsername();
        boardService.updateSharedBoardOrder(boardId, request.getSortOrder(), currentUsername);

        return ResponseEntity.ok(ApiResponse.successWithMessage("공유 보드 순서가 변경되었습니다"));
    }

    /**
     * 보드 삭제 (이관 포함)
     */
    @DeleteMapping("/{id}/with-transfer")
    public ResponseEntity<ApiResponse<TransferResultResponse>> deleteBoardWithTransfer(
            @PathVariable("id") Long boardId,
            @Valid @RequestBody BoardDeleteRequest request
    ) {
        log.info("Delete board with transfer: boardId={}, targetUsername={}, forceDelete={}",
                boardId, request.getTargetUsername(), request.isForceDelete());

        String currentUsername = SecurityUtils.getCurrentUsername();
        TransferResultResponse result = boardService.deleteBoardWithTransfer(boardId, request, currentUsername);

        if (result != null) {
            return ResponseEntity.ok(ApiResponse.success(result, "보드가 삭제되고 업무가 이관되었습니다"));
        } else {
            return ResponseEntity.ok(ApiResponse.successWithMessage("보드가 삭제되었습니다"));
        }
    }

    /**
     * 이관 대상 업무 미리보기
     */
    @GetMapping("/{id}/transfer-preview")
    public ResponseEntity<ApiResponse<TransferPreviewResponse>> getTransferPreview(
            @PathVariable("id") Long boardId
    ) {
        log.debug("Get transfer preview: boardId={}", boardId);

        // 접근 권한 확인
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!boardService.isOwner(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드 소유자만 이관 미리보기를 조회할 수 있습니다"));
        }

        TransferPreviewResponse response = boardService.getTransferPreview(boardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 보드 소유권 이전
     * - 보드를 다른 사용자에게 이관
     * - 보드명이 "보드이관"으로 자동 변경됨
     */
    @PutMapping("/{id}/transfer-ownership")
    public ResponseEntity<ApiResponse<BoardResponse>> transferBoardOwnership(
            @PathVariable("id") Long boardId,
            @Valid @RequestBody BoardTransferRequest request
    ) {
        log.info("Transfer board ownership: boardId={}, targetUsername={}", boardId, request.getTargetUsername());

        String currentUsername = SecurityUtils.getCurrentUsername();

        // 소유자만 이관 가능
        if (!boardService.isOwner(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드 소유자만 이관할 수 있습니다"));
        }

        BoardResponse response = boardService.transferBoardOwnership(boardId, request, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(response, "보드가 이관되었습니다"));
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
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!boardService.hasAccess(boardId, currentUsername)) {
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
        log.info("Add board share: boardId={}, username={}", boardId, request.getUsername());

        String currentUsername = SecurityUtils.getCurrentUsername();
        BoardShareResponse response = boardService.addBoardShare(boardId, request, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "공유 사용자가 추가되었습니다"));
    }

    /**
     * 공유 권한 변경
     */
    @PutMapping("/{id}/shares/{username}")
    public ResponseEntity<ApiResponse<Void>> updateBoardSharePermission(
            @PathVariable("id") Long boardId,
            @PathVariable("username") String username,
            @Valid @RequestBody ShareUpdateRequest request
    ) {
        log.info("Update board share permission: boardId={}, username={}, permission={}",
                boardId, username, request.getPermission());

        String currentUsername = SecurityUtils.getCurrentUsername();
        boardService.updateBoardSharePermission(boardId, username, request, currentUsername);

        return ResponseEntity.ok(ApiResponse.successWithMessage("권한이 변경되었습니다"));
    }

    /**
     * 공유 사용자 제거
     */
    @DeleteMapping("/{id}/shares/{username}")
    public ResponseEntity<ApiResponse<Void>> removeBoardShare(
            @PathVariable("id") Long boardId,
            @PathVariable("username") String username
    ) {
        log.info("Remove board share: boardId={}, username={}", boardId, username);

        String currentUsername = SecurityUtils.getCurrentUsername();
        boardService.removeBoardShare(boardId, username, currentUsername);

        return ResponseEntity.ok(ApiResponse.successWithMessage("공유가 해제되었습니다"));
    }

    // =============================================
    // v2.0: 보드 카테고리 관리
    // =============================================

    /**
     * 보드에 연결된 카테고리 목록 조회
     */
    @GetMapping("/{id}/categories")
    public ResponseEntity<ApiResponse<List<BoardCategoryResponse>>> getBoardCategories(
            @PathVariable("id") Long boardId
    ) {
        log.debug("Get board categories: boardId={}", boardId);

        List<BoardCategoryResponse> categories = boardService.getBoardCategories(boardId);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * 보드에 카테고리 추가
     */
    @PostMapping("/{id}/categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> addBoardCategory(
            @PathVariable("id") Long boardId,
            @PathVariable("categoryId") Long categoryId
    ) {
        log.info("Add category to board: boardId={}, categoryId={}", boardId, categoryId);

        String currentUsername = SecurityUtils.getCurrentUsername();
        boardService.addBoardCategory(boardId, categoryId, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("카테고리가 추가되었습니다"));
    }

    /**
     * 보드에서 카테고리 제거
     */
    @DeleteMapping("/{id}/categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> removeBoardCategory(
            @PathVariable("id") Long boardId,
            @PathVariable("categoryId") Long categoryId
    ) {
        log.info("Remove category from board: boardId={}, categoryId={}", boardId, categoryId);

        boardService.removeBoardCategory(boardId, categoryId);

        return ResponseEntity.ok(ApiResponse.successWithMessage("카테고리가 제거되었습니다"));
    }

    /**
     * 보드의 기본 카테고리 설정
     */
    @PatchMapping("/{id}/categories/{categoryId}/default")
    public ResponseEntity<ApiResponse<Void>> setDefaultCategory(
            @PathVariable("id") Long boardId,
            @PathVariable("categoryId") Long categoryId
    ) {
        log.info("Set default category: boardId={}, categoryId={}", boardId, categoryId);

        String currentUsername = SecurityUtils.getCurrentUsername();
        boardService.setDefaultCategory(boardId, categoryId, currentUsername);

        return ResponseEntity.ok(ApiResponse.successWithMessage("기본 카테고리가 설정되었습니다"));
    }

    // =============================================
    // v2.0: 보드 속성 관리
    // =============================================

    /**
     * 보드에 선택된 속성 목록 조회
     */
    @GetMapping("/{id}/properties")
    public ResponseEntity<ApiResponse<List<BoardPropertyResponse>>> getBoardProperties(
            @PathVariable("id") Long boardId
    ) {
        log.debug("Get board properties: boardId={}", boardId);

        // 접근 권한 확인
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!boardService.hasAccess(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드에 접근 권한이 없습니다"));
        }

        List<BoardPropertyResponse> response = boardService.getBoardProperties(boardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 보드에 속성 추가
     */
    @PostMapping("/{id}/properties/{propertyId}")
    public ResponseEntity<ApiResponse<Void>> addBoardProperty(
            @PathVariable("id") Long boardId,
            @PathVariable("propertyId") Long propertyId,
            @RequestBody(required = false) BoardPropertyRequest request
    ) {
        log.info("Add board property: boardId={}, propertyId={}", boardId, propertyId);

        String currentUsername = SecurityUtils.getCurrentUsername();

        // 소유자만 속성 추가 가능
        if (!boardService.isOwner(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드 소유자만 속성을 추가할 수 있습니다"));
        }

        BoardPropertyRequest req = request != null ? request : new BoardPropertyRequest();
        boardService.addBoardProperty(boardId, propertyId, req, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.successWithMessage("속성이 추가되었습니다"));
    }

    /**
     * 보드에서 속성 제거
     */
    @DeleteMapping("/{id}/properties/{propertyId}")
    public ResponseEntity<ApiResponse<Void>> removeBoardProperty(
            @PathVariable("id") Long boardId,
            @PathVariable("propertyId") Long propertyId
    ) {
        log.info("Remove board property: boardId={}, propertyId={}", boardId, propertyId);

        String currentUsername = SecurityUtils.getCurrentUsername();

        // 소유자만 속성 제거 가능
        if (!boardService.isOwner(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드 소유자만 속성을 제거할 수 있습니다"));
        }

        boardService.removeBoardProperty(boardId, propertyId);

        return ResponseEntity.ok(ApiResponse.successWithMessage("속성이 제거되었습니다"));
    }

    /**
     * 보드 속성 설정 수정 (필수여부, 기본값 등)
     */
    @PutMapping("/{id}/properties/{propertyId}")
    public ResponseEntity<ApiResponse<Void>> updateBoardProperty(
            @PathVariable("id") Long boardId,
            @PathVariable("propertyId") Long propertyId,
            @Valid @RequestBody BoardPropertyRequest request
    ) {
        log.info("Update board property: boardId={}, propertyId={}", boardId, propertyId);

        String currentUsername = SecurityUtils.getCurrentUsername();

        // 소유자만 수정 가능
        if (!boardService.isOwner(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드 소유자만 속성 설정을 수정할 수 있습니다"));
        }

        boardService.updateBoardProperty(boardId, propertyId, request, currentUsername);

        return ResponseEntity.ok(ApiResponse.successWithMessage("속성 설정이 수정되었습니다"));
    }
}
