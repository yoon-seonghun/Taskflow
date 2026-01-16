package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.assignment.AssignmentRequest;
import com.taskflow.dto.assignment.AssignmentResponse;
import com.taskflow.dto.item.ItemAccessInfo;
import com.taskflow.dto.item.AncestorResponse;
import com.taskflow.dto.item.IncompleteChildrenResponse;
import com.taskflow.dto.item.ItemCompleteRequest;
import com.taskflow.dto.item.ItemCreateRequest;
import com.taskflow.dto.item.ItemPageResponse;
import com.taskflow.dto.item.ItemPropertySortRequest;
import com.taskflow.dto.item.ItemReorderRequest;
import com.taskflow.dto.item.ItemResponse;
import com.taskflow.dto.item.ItemSearchRequest;
import com.taskflow.dto.item.ItemTransferRequest;
import com.taskflow.dto.item.ItemUpdateRequest;
import com.taskflow.dto.item.SubTaskCreateRequest;
import com.taskflow.dto.item.SubTaskReorderRequest;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.BoardService;
import com.taskflow.service.ItemAssignmentService;
import com.taskflow.service.ItemService;
import com.taskflow.service.ItemShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 아이템 컨트롤러
 *
 * API:
 * - GET /api/boards/{boardId}/items - 아이템 목록 (필터/정렬 지원)
 * - POST /api/boards/{boardId}/items - 아이템 생성
 * - GET /api/boards/{boardId}/items/{id} - 아이템 조회
 * - PUT /api/boards/{boardId}/items/{id} - 아이템 수정
 * - DELETE /api/boards/{boardId}/items/{id} - 아이템 삭제
 * - PUT /api/boards/{boardId}/items/{id}/complete - 완료 처리
 * - PUT /api/boards/{boardId}/items/{id}/restore - 복원 처리
 * - GET /api/boards/{boardId}/items/today-completed - 오늘 완료/삭제된 아이템
 */
@Slf4j
@RestController
@RequestMapping("/api/boards/{boardId}/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final BoardService boardService;
    private final ItemShareService itemShareService;
    private final ItemAssignmentService itemAssignmentService;

    // =============================================
    // 아이템 CRUD
    // =============================================

    /**
     * 아이템 목록 조회 (필터/정렬/페이징)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ItemPageResponse>> getItems(
            @PathVariable("boardId") Long boardId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "assigneeUsername", required = false) String assigneeUsername,
            @RequestParam(value = "groupId", required = false) Long groupId,
            @RequestParam(value = "departmentCode", required = false) String departmentCode,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "includeCompleted", required = false, defaultValue = "false") Boolean includeCompleted,
            @RequestParam(value = "includeDeleted", required = false, defaultValue = "false") Boolean includeDeleted,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "createdAt,desc") String sort
    ) {
        // 접근 권한 확인
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!boardService.hasAccess(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드에 접근 권한이 없습니다"));
        }

        // 정렬 파라미터 파싱
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        String sortDirection = sortParts.length > 1 ? sortParts[1] : "desc";

        // 검색 조건 생성
        ItemSearchRequest request = new ItemSearchRequest();
        request.setKeyword(keyword);
        request.setStatus(status);
        request.setPriority(priority);
        request.setAssigneeUsername(assigneeUsername);
        request.setGroupId(groupId);
        request.setDepartmentCode(departmentCode);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setIncludeCompleted(includeCompleted);
        request.setIncludeDeleted(includeDeleted);
        request.setPage(page);
        request.setSize(size);
        request.setSortField(sortField);
        request.setSortDirection(sortDirection);

        log.debug("Get items: boardId={}, request={}", boardId, request);

        ItemPageResponse response = itemService.getItemsByBoardId(boardId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 아이템 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ItemResponse>> createItem(
            @PathVariable("boardId") Long boardId,
            @Valid @RequestBody ItemCreateRequest request
    ) {
        // 접근 권한 확인
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!boardService.hasAccess(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드에 접근 권한이 없습니다"));
        }

        log.info("Create item: boardId={}, title={}", boardId, request.getTitle());

        ItemResponse response = itemService.createItem(boardId, request, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "아이템이 생성되었습니다"));
    }

    /**
     * 아이템 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemResponse>> getItem(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId
    ) {
        // 접근 권한 확인 (보드 접근 권한 또는 아이템 공유 권한)
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
        boolean hasItemAccess = itemShareService.hasItemAccess(itemId, currentUsername);

        if (!hasBoardAccess && !hasItemAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("접근 권한이 없습니다"));
        }

        log.debug("Get item: id={}", itemId);

        ItemResponse response = itemService.getItem(itemId);

        // 보드 일치 확인 (보드 접근 권한이 있는 경우에만)
        if (hasBoardAccess && !boardId.equals(response.getBoardId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("아이템을 찾을 수 없습니다"));
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 아이템 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemResponse>> updateItem(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId,
            @Valid @RequestBody ItemUpdateRequest request
    ) {
        // 접근 권한 확인 (보드 접근 권한 또는 아이템 수정 권한)
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
        boolean canEdit = itemShareService.canEditItem(itemId, currentUsername);

        if (!hasBoardAccess && !canEdit) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("수정 권한이 없습니다"));
        }

        log.info("Update item: id={}", itemId);

        ItemResponse response = itemService.updateItem(itemId, request, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(response, "아이템이 수정되었습니다"));
    }

    /**
     * 아이템 삭제 (논리 삭제)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemResponse>> deleteItem(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId
    ) {
        // 접근 권한 확인 (보드 접근 권한 또는 아이템 삭제 권한)
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
        boolean canDelete = itemShareService.canDeleteItem(itemId, currentUsername);

        if (!hasBoardAccess && !canDelete) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("삭제 권한이 없습니다"));
        }

        log.info("Delete item: id={}", itemId);

        ItemResponse response = itemService.deleteItem(itemId, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(response, "아이템이 삭제되었습니다"));
    }

    // =============================================
    // 상태 변경
    // =============================================

    /**
     * 아이템 완료 처리
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<ItemResponse>> completeItem(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId
    ) {
        // 접근 권한 확인 (보드 접근 권한 또는 아이템 수정 권한)
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
        boolean canEdit = itemShareService.canEditItem(itemId, currentUsername);

        if (!hasBoardAccess && !canEdit) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("완료 처리 권한이 없습니다"));
        }

        log.info("Complete item: id={}", itemId);

        ItemResponse response = itemService.completeItem(itemId, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(response, "아이템이 완료되었습니다"));
    }

    /**
     * 아이템 복원
     */
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<ItemResponse>> restoreItem(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId
    ) {
        // 접근 권한 확인 (보드 접근 권한 또는 아이템 수정 권한)
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
        boolean canEdit = itemShareService.canEditItem(itemId, currentUsername);

        if (!hasBoardAccess && !canEdit) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("복원 권한이 없습니다"));
        }

        log.info("Restore item: id={}", itemId);

        ItemResponse response = itemService.restoreItem(itemId, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(response, "아이템이 복원되었습니다"));
    }

    // =============================================
    // 특수 조회
    // =============================================

    /**
     * 오늘 완료/삭제된 아이템 목록 조회 (Hidden 처리용)
     */
    @GetMapping("/today-completed")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getTodayCompletedOrDeleted(
            @PathVariable("boardId") Long boardId
    ) {
        // 접근 권한 확인
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!boardService.hasAccess(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드에 접근 권한이 없습니다"));
        }

        log.debug("Get today completed/deleted items: boardId={}", boardId);

        List<ItemResponse> response = itemService.getTodayCompletedOrDeleted(boardId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 보드별 활성 아이템 목록 조회 (완료/삭제 제외)
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getActiveItems(
            @PathVariable("boardId") Long boardId
    ) {
        // 접근 권한 확인
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!boardService.hasAccess(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드에 접근 권한이 없습니다"));
        }

        log.debug("Get active items: boardId={}", boardId);

        List<ItemResponse> response = itemService.getActiveItemsByBoardId(boardId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =============================================
    // 업무 이관
    // =============================================

    /**
     * 개별 업무 이관
     * - targetBoardId: 다른 보드로 이관
     * - targetUserId: 다른 사용자의 기본 보드로 이관
     */
    @PutMapping("/{id}/transfer")
    public ResponseEntity<ApiResponse<ItemResponse>> transferItem(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId,
            @Valid @RequestBody ItemTransferRequest request
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Transfer item: itemId={}, targetBoardId={}, targetUsername={}",
                itemId, request.getTargetBoardId(), request.getTargetUsername());

        // 이관 권한 확인
        if (!itemShareService.canTransfer(itemId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("업무를 이관할 권한이 없습니다"));
        }

        ItemResponse response = itemShareService.transferItem(itemId, request, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(response, "업무가 이관되었습니다"));
    }

    /**
     * 업무 이관 가능 여부 확인
     */
    @GetMapping("/{id}/can-transfer")
    public ResponseEntity<ApiResponse<Boolean>> canTransferItem(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();

        boolean canTransfer = itemShareService.canTransfer(itemId, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(canTransfer));
    }

    /**
     * 업무 공유 가능 여부 확인
     */
    @GetMapping("/{id}/can-share")
    public ResponseEntity<ApiResponse<Boolean>> canShareItem(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();

        boolean canShare = itemShareService.canShareItem(itemId, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(canShare));
    }

    /**
     * 업무 권한 조회
     */
    @GetMapping("/{id}/permission")
    public ResponseEntity<ApiResponse<String>> getItemPermission(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();

        String permission = itemShareService.getItemPermission(itemId, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(permission));
    }

    // =============================================
    // 아이템 속성 순서 변경
    // =============================================

    /**
     * 아이템 속성 순서 변경
     */
    @PutMapping("/{id}/properties/sort")
    public ResponseEntity<ApiResponse<Void>> updatePropertySortOrders(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId,
            @Valid @RequestBody ItemPropertySortRequest request
    ) {
        // 접근 권한 확인
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!boardService.hasAccess(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드에 접근 권한이 없습니다"));
        }

        log.info("Update item property sort orders: itemId={}", itemId);

        itemService.updatePropertySortOrders(itemId, request, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(null, "속성 순서가 변경되었습니다"));
    }

    // =============================================
    // 아이템 순서 변경 (Drag & Drop)
    // =============================================

    /**
     * 아이템 순서 변경
     */
    @PutMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderItem(
            @PathVariable("boardId") Long boardId,
            @Valid @RequestBody ItemReorderRequest request
    ) {
        // 접근 권한 확인
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!boardService.hasAccess(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드에 접근 권한이 없습니다"));
        }

        log.info("Reorder item: boardId={}, itemId={}, newOrder={}",
                boardId, request.getItemId(), request.getNewOrder());

        itemService.reorderItem(boardId, request, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(null, "아이템 순서가 변경되었습니다"));
    }

    // =============================================
    // 업무 담당자 배정 (v2.1)
    // =============================================

    /**
     * 업무 담당자 배정
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<AssignmentResponse>> assignItem(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId,
            @Valid @RequestBody AssignmentRequest request
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Assign item: itemId={}, assignee={}", itemId, request.getAssigneeUsername());

        try {
            AssignmentResponse response = itemAssignmentService.assignItem(boardId, itemId, request, currentUsername);
            return ResponseEntity.ok(ApiResponse.success(response, "업무가 배정되었습니다"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 업무 배정 취소
     */
    @DeleteMapping("/{id}/assign/{username}")
    public ResponseEntity<ApiResponse<Void>> cancelAssignment(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId,
            @PathVariable("username") String assigneeUsername
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Cancel assignment: itemId={}, assignee={}", itemId, assigneeUsername);

        try {
            itemAssignmentService.cancelAssignment(boardId, itemId, assigneeUsername, currentUsername);
            return ResponseEntity.ok(ApiResponse.success(null, "배정이 취소되었습니다"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 배정 권한 수정
     */
    @PutMapping("/{id}/assign/{username}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> updateAssignmentPermission(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId,
            @PathVariable("username") String assigneeUsername,
            @RequestBody java.util.Map<String, String> request
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        String permissionLevel = request.get("permissionLevel");
        log.info("Update assignment permission: itemId={}, assignee={}, permission={}", itemId, assigneeUsername, permissionLevel);

        try {
            AssignmentResponse response = itemAssignmentService.updateAssignmentPermission(
                    boardId, itemId, assigneeUsername, permissionLevel, currentUsername
            );
            return ResponseEntity.ok(ApiResponse.success(response, "권한이 수정되었습니다"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 업무 접근 권한 정보 조회
     */
    @GetMapping("/{id}/access-info")
    public ResponseEntity<ApiResponse<ItemAccessInfo>> getAccessInfo(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();

        ItemAccessInfo accessInfo = itemAssignmentService.getAccessInfo(boardId, itemId, currentUsername);
        return ResponseEntity.ok(ApiResponse.success(accessInfo));
    }

    // =============================================
    // v2.2: 하위 업무 (Sub-Task) API
    // =============================================

    /**
     * 하위 업무 목록 조회
     */
    @GetMapping("/{id}/children")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getChildren(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long parentItemId,
            @RequestParam(value = "includeCompleted", required = false, defaultValue = "false") Boolean includeCompleted,
            @RequestParam(value = "includeDeleted", required = false, defaultValue = "false") Boolean includeDeleted
    ) {
        // 접근 권한 확인 (보드 접근 권한 또는 아이템 접근 권한)
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
        boolean hasItemAccess = itemShareService.hasItemAccess(parentItemId, currentUsername);

        if (!hasBoardAccess && !hasItemAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("접근 권한이 없습니다"));
        }

        log.debug("Get children: parentItemId={}, includeCompleted={}, includeDeleted={}",
                parentItemId, includeCompleted, includeDeleted);

        List<ItemResponse> response = itemService.getChildren(parentItemId, includeCompleted, includeDeleted);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 업무 트리 조회 (부모 + 모든 하위)
     */
    @GetMapping("/{id}/tree")
    public ResponseEntity<ApiResponse<ItemResponse>> getItemTree(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId,
            @RequestParam(value = "maxDepth", required = false) Integer maxDepth,
            @RequestParam(value = "includeCompleted", required = false, defaultValue = "false") Boolean includeCompleted
    ) {
        // 접근 권한 확인 (보드 접근 권한 또는 아이템 접근 권한)
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
        boolean hasItemAccess = itemShareService.hasItemAccess(itemId, currentUsername);

        if (!hasBoardAccess && !hasItemAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("접근 권한이 없습니다"));
        }

        log.debug("Get item tree: itemId={}, maxDepth={}, includeCompleted={}", itemId, maxDepth, includeCompleted);

        ItemResponse response = itemService.getItemTree(itemId, maxDepth, includeCompleted);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 상위 계층 조회 (Breadcrumb용)
     */
    @GetMapping("/{id}/ancestors")
    public ResponseEntity<ApiResponse<List<AncestorResponse>>> getAncestors(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId
    ) {
        // 접근 권한 확인 (보드 접근 권한 또는 아이템 접근 권한)
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
        boolean hasItemAccess = itemShareService.hasItemAccess(itemId, currentUsername);

        if (!hasBoardAccess && !hasItemAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("접근 권한이 없습니다"));
        }

        log.debug("Get ancestors: itemId={}", itemId);

        List<AncestorResponse> response = itemService.getAncestors(itemId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 부모 업무 조회
     */
    @GetMapping("/{id}/parent")
    public ResponseEntity<ApiResponse<ItemResponse>> getParent(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId
    ) {
        // 접근 권한 확인 (보드 접근 권한 또는 아이템 접근 권한)
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
        boolean hasItemAccess = itemShareService.hasItemAccess(itemId, currentUsername);

        if (!hasBoardAccess && !hasItemAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("접근 권한이 없습니다"));
        }

        log.debug("Get parent: itemId={}", itemId);

        ItemResponse response = itemService.getParent(itemId);
        if (response == null) {
            return ResponseEntity.ok(ApiResponse.success(null, "부모 업무가 없습니다 (루트 업무)"));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 루트 업무 목록 조회 (보드별)
     */
    @GetMapping("/root")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getRootItems(
            @PathVariable("boardId") Long boardId,
            @RequestParam(value = "includeCompleted", required = false, defaultValue = "false") Boolean includeCompleted,
            @RequestParam(value = "includeDeleted", required = false, defaultValue = "false") Boolean includeDeleted
    ) {
        // 접근 권한 확인
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!boardService.hasAccess(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드에 접근 권한이 없습니다"));
        }

        log.debug("Get root items: boardId={}, includeCompleted={}, includeDeleted={}",
                boardId, includeCompleted, includeDeleted);

        List<ItemResponse> response = itemService.getRootItemsByBoardId(boardId, includeCompleted, includeDeleted);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 하위 업무 등록
     */
    @PostMapping("/{id}/subtask")
    public ResponseEntity<ApiResponse<ItemResponse>> createSubTask(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long parentItemId,
            @Valid @RequestBody SubTaskCreateRequest request
    ) {
        // 접근 권한 확인
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!boardService.hasAccess(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드에 접근 권한이 없습니다"));
        }

        log.info("Create sub-task: boardId={}, parentItemId={}, title={}", boardId, parentItemId, request.getTitle());

        ItemResponse response = itemService.createSubTask(boardId, parentItemId, request, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "하위 업무가 생성되었습니다"));
    }

    /**
     * 하위 업무 순서 변경
     */
    @PutMapping("/{id}/subtask/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderChildren(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long parentItemId,
            @Valid @RequestBody SubTaskReorderRequest request
    ) {
        // 접근 권한 확인
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (!boardService.hasAccess(boardId, currentUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드에 접근 권한이 없습니다"));
        }

        log.info("Reorder children: parentItemId={}", parentItemId);

        itemService.reorderChildren(parentItemId, request, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(null, "하위 업무 순서가 변경되었습니다"));
    }

    /**
     * 업무 완료 처리 (하위 업무 체크 포함, v2.2)
     *
     * @return ItemResponse (성공 시) 또는 IncompleteChildrenResponse (미완료 하위 업무 있을 시)
     */
    @PutMapping("/{id}/complete-with-children")
    public ResponseEntity<ApiResponse<Object>> completeItemWithChildren(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId,
            @RequestBody(required = false) ItemCompleteRequest request
    ) {
        // 접근 권한 확인 (보드 접근 권한 또는 아이템 수정 권한)
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
        boolean canEdit = itemShareService.canEditItem(itemId, currentUsername);

        if (!hasBoardAccess && !canEdit) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("완료 처리 권한이 없습니다"));
        }

        log.info("Complete item with children: itemId={}, forceComplete={}",
                itemId, request != null && request.isForceComplete());

        Object result = itemService.completeItemWithChildren(itemId, request, currentUsername);

        // 결과 타입에 따라 응답 생성
        if (result instanceof IncompleteChildrenResponse) {
            // 미완료 하위 업무가 있는 경우 - 확인 필요
            return ResponseEntity.ok(ApiResponse.success(result, "미완료 하위 업무가 있습니다. 강제 완료하려면 forceComplete를 true로 설정하세요."));
        } else {
            // 완료 성공
            return ResponseEntity.ok(ApiResponse.success(result, "아이템이 완료되었습니다"));
        }
    }

    /**
     * 미완료 하위 업무 조회
     */
    @GetMapping("/{id}/incomplete-children")
    public ResponseEntity<ApiResponse<IncompleteChildrenResponse>> getIncompleteChildren(
            @PathVariable("boardId") Long boardId,
            @PathVariable("id") Long itemId
    ) {
        // 접근 권한 확인 (보드 접근 또는 아이템 공유 권한)
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean hasBoardAccess = boardService.hasAccess(boardId, currentUsername);
        boolean hasItemAccess = itemShareService.hasItemAccess(itemId, currentUsername);

        if (!hasBoardAccess && !hasItemAccess) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("접근 권한이 없습니다"));
        }

        log.debug("Get incomplete children: itemId={}", itemId);

        IncompleteChildrenResponse response = itemService.getIncompleteChildren(itemId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
