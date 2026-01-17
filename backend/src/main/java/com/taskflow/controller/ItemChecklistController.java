package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.checklist.ChecklistCreateRequest;
import com.taskflow.dto.checklist.ChecklistReorderRequest;
import com.taskflow.dto.checklist.ChecklistResponse;
import com.taskflow.dto.checklist.ChecklistUpdateRequest;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.ItemChecklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 업무 내 체크리스트 컨트롤러
 *
 * API:
 * - GET /api/items/{itemId}/checklists - 체크리스트 목록 조회
 * - GET /api/items/{itemId}/checklists/progress - 체크리스트 진행률 조회
 * - POST /api/items/{itemId}/checklists - 체크리스트 생성
 * - PUT /api/checklists/{checklistId} - 체크리스트 수정
 * - PUT /api/checklists/{checklistId}/complete - 완료 상태 토글
 * - DELETE /api/checklists/{checklistId} - 체크리스트 삭제
 * - PUT /api/items/{itemId}/checklists/reorder - 순서 변경
 * - GET /api/checklists/my - 내 담당 체크리스트 목록
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ItemChecklistController {

    private final ItemChecklistService checklistService;

    /**
     * 체크리스트 목록 조회
     */
    @GetMapping("/api/items/{itemId}/checklists")
    public ResponseEntity<ApiResponse<List<ChecklistResponse>>> getChecklists(
            @PathVariable("itemId") Long itemId
    ) {
        log.debug("Get checklists: itemId={}", itemId);

        List<ChecklistResponse> response = checklistService.getChecklists(itemId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 체크리스트 진행률 조회
     */
    @GetMapping("/api/items/{itemId}/checklists/progress")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getProgress(
            @PathVariable("itemId") Long itemId
    ) {
        log.debug("Get checklist progress: itemId={}", itemId);

        int[] progress = checklistService.getProgress(itemId);
        Map<String, Integer> response = Map.of(
                "completed", progress[0],
                "total", progress[1]
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 체크리스트 생성
     */
    @PostMapping("/api/items/{itemId}/checklists")
    public ResponseEntity<ApiResponse<ChecklistResponse>> createChecklist(
            @PathVariable("itemId") Long itemId,
            @Valid @RequestBody ChecklistCreateRequest request
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Create checklist: itemId={}", itemId);

        ChecklistResponse response = checklistService.createChecklist(itemId, request, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 체크리스트 수정
     */
    @PutMapping("/api/checklists/{checklistId}")
    public ResponseEntity<ApiResponse<ChecklistResponse>> updateChecklist(
            @PathVariable("checklistId") Long checklistId,
            @Valid @RequestBody ChecklistUpdateRequest request
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Update checklist: checklistId={}", checklistId);

        ChecklistResponse response = checklistService.updateChecklist(checklistId, request, currentUsername);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 완료 상태 토글
     */
    @PutMapping("/api/checklists/{checklistId}/complete")
    public ResponseEntity<ApiResponse<ChecklistResponse>> toggleComplete(
            @PathVariable("checklistId") Long checklistId
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Toggle checklist complete: checklistId={}", checklistId);

        ChecklistResponse response = checklistService.toggleComplete(checklistId, currentUsername);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 체크리스트 삭제
     */
    @DeleteMapping("/api/checklists/{checklistId}")
    public ResponseEntity<ApiResponse<Void>> deleteChecklist(
            @PathVariable("checklistId") Long checklistId
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Delete checklist: checklistId={}", checklistId);

        checklistService.deleteChecklist(checklistId, currentUsername);
        return ResponseEntity.ok(ApiResponse.successWithMessage("체크리스트가 삭제되었습니다"));
    }

    /**
     * 순서 변경
     */
    @PutMapping("/api/items/{itemId}/checklists/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderChecklists(
            @PathVariable("itemId") Long itemId,
            @Valid @RequestBody ChecklistReorderRequest request
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Reorder checklists: itemId={}", itemId);

        checklistService.reorderChecklists(itemId, request, currentUsername);
        return ResponseEntity.ok(ApiResponse.successWithMessage("순서가 변경되었습니다"));
    }

    /**
     * 내 담당 체크리스트 목록
     */
    @GetMapping("/api/checklists/my")
    public ResponseEntity<ApiResponse<List<ChecklistResponse>>> getMyChecklists() {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.debug("Get my checklists: username={}", currentUsername);

        List<ChecklistResponse> response = checklistService.getChecklistsByAssignee(currentUsername);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
