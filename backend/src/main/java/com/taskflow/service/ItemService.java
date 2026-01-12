package com.taskflow.service;

import com.taskflow.dto.item.CrossBoardSearchRequest;
import com.taskflow.dto.item.ItemCreateRequest;
import com.taskflow.dto.item.ItemPageResponse;
import com.taskflow.dto.item.ItemPropertySortRequest;
import com.taskflow.dto.item.ItemResponse;
import com.taskflow.dto.item.ItemSearchRequest;
import com.taskflow.dto.item.ItemUpdateRequest;

import java.util.List;
import java.util.Map;

/**
 * 아이템 서비스 인터페이스
 */
public interface ItemService {

    // =============================================
    // 아이템 조회
    // =============================================

    /**
     * 아이템 ID로 조회
     *
     * @param itemId 아이템 ID
     * @return 아이템 응답
     */
    ItemResponse getItem(Long itemId);

    /**
     * 보드별 아이템 목록 조회 (필터/정렬/페이징)
     *
     * @param boardId 보드 ID
     * @param request 검색 조건
     * @return 페이징된 아이템 목록
     */
    ItemPageResponse getItemsByBoardId(Long boardId, ItemSearchRequest request);

    /**
     * 보드별 활성 아이템 목록 조회
     *
     * @param boardId 보드 ID
     * @return 아이템 목록
     */
    List<ItemResponse> getActiveItemsByBoardId(Long boardId);

    /**
     * 오늘 완료/삭제된 아이템 목록 조회
     *
     * @param boardId 보드 ID
     * @return 아이템 목록
     */
    List<ItemResponse> getTodayCompletedOrDeleted(Long boardId);

    /**
     * 그룹별 아이템 목록 조회
     *
     * @param groupId 그룹 ID
     * @return 아이템 목록
     */
    List<ItemResponse> getItemsByGroupId(Long groupId);

    /**
     * 담당자별 아이템 목록 조회
     *
     * @param assigneeUsername 담당자 사용자명
     * @param boardId          보드 ID (null이면 전체)
     * @return 아이템 목록
     */
    List<ItemResponse> getItemsByAssigneeUsername(String assigneeUsername, Long boardId);

    // =============================================
    // 아이템 등록/수정/삭제
    // =============================================

    /**
     * 아이템 등록
     *
     * @param boardId   보드 ID
     * @param request   등록 요청
     * @param createdBy 생성자 사용자명
     * @return 생성된 아이템 응답
     */
    ItemResponse createItem(Long boardId, ItemCreateRequest request, String createdBy);

    /**
     * 아이템 수정
     *
     * @param itemId    아이템 ID
     * @param request   수정 요청
     * @param updatedBy 수정자 사용자명
     * @return 수정된 아이템 응답
     */
    ItemResponse updateItem(Long itemId, ItemUpdateRequest request, String updatedBy);

    /**
     * 아이템 완료 처리
     *
     * @param itemId      아이템 ID
     * @param completedBy 완료자 사용자명
     * @return 완료된 아이템 응답
     */
    ItemResponse completeItem(Long itemId, String completedBy);

    /**
     * 아이템 삭제 처리 (논리 삭제)
     *
     * @param itemId    아이템 ID
     * @param deletedBy 삭제자 사용자명
     * @return 삭제된 아이템 응답
     */
    ItemResponse deleteItem(Long itemId, String deletedBy);

    /**
     * 아이템 복원
     *
     * @param itemId    아이템 ID
     * @param updatedBy 수정자 사용자명
     * @return 복원된 아이템 응답
     */
    ItemResponse restoreItem(Long itemId, String updatedBy);

    /**
     * 아이템 물리 삭제
     *
     * @param itemId 아이템 ID
     */
    void hardDeleteItem(Long itemId);

    // =============================================
    // Cross-board 조회
    // =============================================

    /**
     * 지연 아이템 목록 조회 (Cross-board)
     *
     * @param username 사용자명
     * @param request  검색 조건
     * @return 페이징된 지연 아이템 목록
     */
    ItemPageResponse getOverdueItems(String username, CrossBoardSearchRequest request);

    /**
     * 보류 아이템 목록 조회 (Cross-board)
     *
     * @param username 사용자명
     * @param request  검색 조건
     * @return 페이징된 보류 아이템 목록
     */
    ItemPageResponse getPendingItems(String username, CrossBoardSearchRequest request);

    /**
     * 활성 아이템 목록 조회 (Cross-board)
     *
     * @param username 사용자명
     * @param request  검색 조건
     * @return 페이징된 활성 아이템 목록
     */
    ItemPageResponse getActiveItemsCrossBoard(String username, CrossBoardSearchRequest request);

    /**
     * 지연/보류 업무 통계 조회 (Cross-board)
     *
     * @param username 사용자명
     * @return 통계 정보 (overdue, pending 등)
     */
    Map<String, Object> getCrossBoardStats(String username);

    // =============================================
    // 아이템 속성 순서 변경
    // =============================================

    /**
     * 아이템 속성 순서 변경
     *
     * @param itemId    아이템 ID
     * @param request   순서 변경 요청
     * @param updatedBy 수정자 사용자명
     */
    void updatePropertySortOrders(Long itemId, ItemPropertySortRequest request, String updatedBy);
}
