package com.taskflow.mapper;

import com.taskflow.domain.Item;
import com.taskflow.dto.item.CrossBoardSearchRequest;
import com.taskflow.dto.item.ItemSearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 아이템 Mapper 인터페이스
 */
@Mapper
public interface ItemMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 아이템 ID로 조회
     *
     * @param itemId 아이템 ID
     * @return 아이템 (Optional)
     */
    Optional<Item> findById(@Param("itemId") Long itemId);

    /**
     * 보드별 아이템 목록 조회 (필터/정렬/페이징)
     *
     * @param boardId 보드 ID
     * @param request 검색 조건
     * @return 아이템 목록
     */
    List<Item> findByBoardIdWithFilter(@Param("boardId") Long boardId,
                                        @Param("request") ItemSearchRequest request);

    /**
     * 보드별 아이템 총 개수 조회
     *
     * @param boardId 보드 ID
     * @param request 검색 조건
     * @return 아이템 개수
     */
    long countByBoardIdWithFilter(@Param("boardId") Long boardId,
                                   @Param("request") ItemSearchRequest request);

    /**
     * 보드별 활성 아이템 목록 조회 (완료/삭제 제외)
     *
     * @param boardId 보드 ID
     * @return 아이템 목록
     */
    List<Item> findActiveByBoardId(@Param("boardId") Long boardId);

    /**
     * 그룹별 아이템 목록 조회
     *
     * @param groupId 그룹 ID
     * @return 아이템 목록
     */
    List<Item> findByGroupId(@Param("groupId") Long groupId);

    /**
     * 담당자별 아이템 목록 조회
     *
     * @param assigneeId 담당자 ID
     * @param boardId    보드 ID (null이면 전체)
     * @return 아이템 목록
     */
    List<Item> findByAssigneeId(@Param("assigneeId") Long assigneeId,
                                 @Param("boardId") Long boardId);

    /**
     * 오늘 완료/삭제된 아이템 목록 조회 (Hidden 처리용)
     *
     * @param boardId 보드 ID
     * @return 아이템 목록
     */
    List<Item> findTodayCompletedOrDeleted(@Param("boardId") Long boardId);

    /**
     * 보드 내 최대 정렬 순서 조회
     *
     * @param boardId 보드 ID
     * @return 최대 정렬 순서
     */
    Integer getMaxSortOrder(@Param("boardId") Long boardId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 아이템 등록
     *
     * @param item 아이템 엔티티
     * @return 영향받은 행 수
     */
    int insert(Item item);

    /**
     * 아이템 수정
     *
     * @param item 아이템 엔티티
     * @return 영향받은 행 수
     */
    int update(Item item);

    /**
     * 아이템 상태 변경
     *
     * @param itemId    아이템 ID
     * @param status    상태
     * @param updatedBy 수정자 ID
     * @return 영향받은 행 수
     */
    int updateStatus(@Param("itemId") Long itemId,
                     @Param("status") String status,
                     @Param("updatedBy") Long updatedBy);

    /**
     * 아이템 완료 처리
     *
     * @param itemId      아이템 ID
     * @param completedBy 완료자 ID
     * @return 영향받은 행 수
     */
    int complete(@Param("itemId") Long itemId,
                 @Param("completedBy") Long completedBy);

    /**
     * 아이템 삭제 처리 (논리 삭제)
     *
     * @param itemId    아이템 ID
     * @param deletedBy 삭제자 ID
     * @return 영향받은 행 수
     */
    int softDelete(@Param("itemId") Long itemId,
                   @Param("deletedBy") Long deletedBy);

    /**
     * 아이템 복원
     *
     * @param itemId    아이템 ID
     * @param updatedBy 수정자 ID
     * @return 영향받은 행 수
     */
    int restore(@Param("itemId") Long itemId,
                @Param("updatedBy") Long updatedBy);

    /**
     * 아이템 물리 삭제
     *
     * @param itemId 아이템 ID
     * @return 영향받은 행 수
     */
    int delete(@Param("itemId") Long itemId);

    /**
     * 보드의 모든 아이템 삭제
     *
     * @param boardId 보드 ID
     * @return 영향받은 행 수
     */
    int deleteByBoardId(@Param("boardId") Long boardId);

    // =============================================
    // Cross-board 조회
    // =============================================

    /**
     * 사용자가 접근 가능한 보드의 지연 아이템 목록 조회
     *
     * @param userId  사용자 ID
     * @param request 검색 조건
     * @return 지연 아이템 목록
     */
    List<Item> findOverdueItems(@Param("userId") Long userId,
                                 @Param("request") CrossBoardSearchRequest request);

    /**
     * 사용자가 접근 가능한 보드의 지연 아이템 총 개수 조회
     *
     * @param userId  사용자 ID
     * @param request 검색 조건
     * @return 지연 아이템 개수
     */
    long countOverdueItems(@Param("userId") Long userId,
                            @Param("request") CrossBoardSearchRequest request);

    /**
     * 사용자가 접근 가능한 보드의 보류 아이템 목록 조회
     *
     * @param userId  사용자 ID
     * @param request 검색 조건
     * @return 보류 아이템 목록
     */
    List<Item> findPendingItems(@Param("userId") Long userId,
                                 @Param("request") CrossBoardSearchRequest request);

    /**
     * 사용자가 접근 가능한 보드의 보류 아이템 총 개수 조회
     *
     * @param userId  사용자 ID
     * @param request 검색 조건
     * @return 보류 아이템 개수
     */
    long countPendingItems(@Param("userId") Long userId,
                            @Param("request") CrossBoardSearchRequest request);

    /**
     * 사용자가 접근 가능한 보드의 활성 아이템 목록 조회 (Cross-board)
     *
     * @param userId  사용자 ID
     * @param request 검색 조건
     * @return 활성 아이템 목록
     */
    List<Item> findActiveItemsCrossBoard(@Param("userId") Long userId,
                                          @Param("request") CrossBoardSearchRequest request);

    /**
     * 사용자가 접근 가능한 보드의 활성 아이템 총 개수 조회 (Cross-board)
     *
     * @param userId  사용자 ID
     * @param request 검색 조건
     * @return 활성 아이템 개수
     */
    long countActiveItemsCrossBoard(@Param("userId") Long userId,
                                     @Param("request") CrossBoardSearchRequest request);
}
