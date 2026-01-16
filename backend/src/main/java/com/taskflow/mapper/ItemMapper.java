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
     * @param assigneeUsername 담당자 USERNAME
     * @param boardId    보드 ID (null이면 전체)
     * @return 아이템 목록
     */
    List<Item> findByAssigneeUsername(@Param("assigneeUsername") String assigneeUsername,
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
     * @param updatedBy 수정자 USERNAME
     * @return 영향받은 행 수
     */
    int updateStatus(@Param("itemId") Long itemId,
                     @Param("status") String status,
                     @Param("updatedBy") String updatedBy);

    /**
     * 아이템 완료 처리
     *
     * @param itemId      아이템 ID
     * @param completedBy 완료자 USERNAME
     * @return 영향받은 행 수
     */
    int complete(@Param("itemId") Long itemId,
                 @Param("completedBy") String completedBy);

    /**
     * 아이템 삭제 처리 (논리 삭제)
     *
     * @param itemId    아이템 ID
     * @param deletedBy 삭제자 USERNAME
     * @return 영향받은 행 수
     */
    int softDelete(@Param("itemId") Long itemId,
                   @Param("deletedBy") String deletedBy);

    /**
     * 아이템 복원
     *
     * @param itemId    아이템 ID
     * @param updatedBy 수정자 USERNAME
     * @return 영향받은 행 수
     */
    int restore(@Param("itemId") Long itemId,
                @Param("updatedBy") String updatedBy);

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
    // 아이템 순서 변경
    // =============================================

    /**
     * 아이템 순서 업데이트
     *
     * @param itemId    아이템 ID
     * @param sortOrder 새 순서
     * @param updatedBy 수정자 USERNAME
     * @return 영향받은 행 수
     */
    int updateSortOrder(@Param("itemId") Long itemId,
                        @Param("sortOrder") Integer sortOrder,
                        @Param("updatedBy") String updatedBy);

    /**
     * 범위 내 아이템 순서 증가 (아이템을 위로 이동 시)
     *
     * @param boardId       보드 ID
     * @param fromOrder     시작 순서
     * @param toOrder       끝 순서
     * @param excludeItemId 제외할 아이템 ID
     * @return 영향받은 행 수
     */
    int incrementSortOrderInRange(@Param("boardId") Long boardId,
                                  @Param("fromOrder") Integer fromOrder,
                                  @Param("toOrder") Integer toOrder,
                                  @Param("excludeItemId") Long excludeItemId);

    /**
     * 범위 내 아이템 순서 감소 (아이템을 아래로 이동 시)
     *
     * @param boardId       보드 ID
     * @param fromOrder     시작 순서
     * @param toOrder       끝 순서
     * @param excludeItemId 제외할 아이템 ID
     * @return 영향받은 행 수
     */
    int decrementSortOrderInRange(@Param("boardId") Long boardId,
                                  @Param("fromOrder") Integer fromOrder,
                                  @Param("toOrder") Integer toOrder,
                                  @Param("excludeItemId") Long excludeItemId);

    /**
     * 보드별 아이템 목록 조회 (순서대로 정렬)
     *
     * @param boardId 보드 ID
     * @return 아이템 목록
     */
    List<Item> findByBoardIdOrderBySortOrder(@Param("boardId") Long boardId);

    // =============================================
    // Cross-board 조회
    // =============================================

    /**
     * 사용자가 접근 가능한 보드의 지연 아이템 목록 조회
     *
     * @param username  사용자 USERNAME
     * @param request 검색 조건
     * @return 지연 아이템 목록
     */
    List<Item> findOverdueItems(@Param("username") String username,
                                 @Param("request") CrossBoardSearchRequest request);

    /**
     * 사용자가 접근 가능한 보드의 지연 아이템 총 개수 조회
     *
     * @param username  사용자 USERNAME
     * @param request 검색 조건
     * @return 지연 아이템 개수
     */
    long countOverdueItems(@Param("username") String username,
                            @Param("request") CrossBoardSearchRequest request);

    /**
     * 사용자가 접근 가능한 보드의 보류 아이템 목록 조회
     *
     * @param username  사용자 USERNAME
     * @param request 검색 조건
     * @return 보류 아이템 목록
     */
    List<Item> findPendingItems(@Param("username") String username,
                                 @Param("request") CrossBoardSearchRequest request);

    /**
     * 사용자가 접근 가능한 보드의 보류 아이템 총 개수 조회
     *
     * @param username  사용자 USERNAME
     * @param request 검색 조건
     * @return 보류 아이템 개수
     */
    long countPendingItems(@Param("username") String username,
                            @Param("request") CrossBoardSearchRequest request);

    /**
     * 사용자가 접근 가능한 보드의 활성 아이템 목록 조회 (Cross-board)
     *
     * @param username  사용자 USERNAME
     * @param request 검색 조건
     * @return 활성 아이템 목록
     */
    List<Item> findActiveItemsCrossBoard(@Param("username") String username,
                                          @Param("request") CrossBoardSearchRequest request);

    /**
     * 사용자가 접근 가능한 보드의 활성 아이템 총 개수 조회 (Cross-board)
     *
     * @param username  사용자 USERNAME
     * @param request 검색 조건
     * @return 활성 아이템 개수
     */
    long countActiveItemsCrossBoard(@Param("username") String username,
                                     @Param("request") CrossBoardSearchRequest request);

    // =============================================
    // 업무 이관 관련
    // =============================================

    /**
     * 업무 보드 이동 (이관)
     * v2.2.1: 소유자 변경 및 하위 업무 함께 이관
     *
     * @param itemId           아이템 ID
     * @param newBoardId       새 보드 ID
     * @param newOwnerUsername 새 소유자 USERNAME
     * @param originalBoardId  원본 보드 ID
     * @param updatedBy        수정자 USERNAME
     * @return 영향받은 행 수
     */
    int transferToBoard(@Param("itemId") Long itemId,
                        @Param("newBoardId") Long newBoardId,
                        @Param("newOwnerUsername") String newOwnerUsername,
                        @Param("originalBoardId") Long originalBoardId,
                        @Param("updatedBy") String updatedBy);

    /**
     * 보드 이관 시 업무 소유자 일괄 변경 (v2.2.1)
     *
     * @param boardId          보드 ID
     * @param newOwnerUsername 새 소유자 USERNAME
     * @param updatedBy        수정자 USERNAME
     * @return 영향받은 행 수
     */
    int updateOwnerByBoardId(@Param("boardId") Long boardId,
                             @Param("newOwnerUsername") String newOwnerUsername,
                             @Param("updatedBy") String updatedBy);

    /**
     * 특정 소유자의 업무 수 조회 (v2.2.1)
     *
     * @param ownerUsername 소유자 USERNAME
     * @return 업무 수
     */
    int countByOwnerUsername(@Param("ownerUsername") String ownerUsername);

    /**
     * 다중 업무 보드 이동 (이관)
     *
     * @param itemIds         아이템 ID 목록
     * @param newBoardId      새 보드 ID
     * @param originalBoardId 원본 보드 ID
     * @param updatedBy       수정자 USERNAME
     * @return 영향받은 행 수
     */
    int transferItemsToBoard(@Param("itemIds") List<Long> itemIds,
                             @Param("newBoardId") Long newBoardId,
                             @Param("originalBoardId") Long originalBoardId,
                             @Param("updatedBy") String updatedBy);

    /**
     * 이관된 업무 목록 조회
     *
     * @param boardId 보드 ID
     * @return 이관된 업무 목록
     */
    List<Item> findTransferredItems(@Param("boardId") Long boardId);

    // =============================================
    // 공유받은 업무 조회
    // =============================================

    /**
     * 공유받은 업무 목록 조회
     *
     * @param username 사용자 USERNAME
     * @param request  검색 조건
     * @return 공유받은 업무 목록
     */
    List<Item> findSharedItems(@Param("username") String username,
                               @Param("request") CrossBoardSearchRequest request);

    /**
     * 공유받은 업무 총 개수 조회
     *
     * @param username 사용자 USERNAME
     * @param request  검색 조건
     * @return 공유받은 업무 개수
     */
    long countSharedItems(@Param("username") String username,
                          @Param("request") CrossBoardSearchRequest request);

    // =============================================
    // 담당자 관련
    // =============================================

    /**
     * 아이템 담당자 업데이트
     *
     * @param itemId           아이템 ID
     * @param assigneeUsername 담당자 USERNAME (null 가능)
     * @param updatedBy        수정자 USERNAME
     * @return 영향받은 행 수
     */
    int updateAssignee(@Param("itemId") Long itemId,
                       @Param("assigneeUsername") String assigneeUsername,
                       @Param("updatedBy") String updatedBy);

    /**
     * 아이템 ID로 조회 (단건, Optional 없이)
     *
     * @param itemId 아이템 ID
     * @return 아이템
     */
    Item selectById(@Param("itemId") Long itemId);

    // =============================================
    // v2.2: 하위 업무(Sub-task) 관련
    // =============================================

    /**
     * 하위 업무 목록 조회
     *
     * @param parentItemId    부모 업무 ID
     * @param includeCompleted 완료된 업무 포함 여부
     * @param includeDeleted   삭제된 업무 포함 여부
     * @return 하위 업무 목록
     */
    List<Item> findChildren(@Param("parentItemId") Long parentItemId,
                            @Param("includeCompleted") Boolean includeCompleted,
                            @Param("includeDeleted") Boolean includeDeleted);

    /**
     * 모든 하위 업무 조회 (재귀)
     *
     * @param parentItemId 부모 업무 ID
     * @return 모든 하위 업무 목록
     */
    List<Item> findAllDescendants(@Param("parentItemId") Long parentItemId);

    /**
     * 상위 계층 조회 (Breadcrumb용)
     *
     * @param itemId 현재 업무 ID
     * @return 상위 계층 목록 (루트부터 현재까지)
     */
    List<Item> findAncestors(@Param("itemId") Long itemId);

    /**
     * 부모 업무 조회
     *
     * @param itemId 현재 업무 ID
     * @return 부모 업무
     */
    Item findParent(@Param("itemId") Long itemId);

    /**
     * 미완료 하위 업무 목록 조회
     *
     * @param parentItemId 부모 업무 ID
     * @return 미완료 하위 업무 목록
     */
    List<Item> findIncompleteChildren(@Param("parentItemId") Long parentItemId);

    /**
     * 모든 미완료 하위 업무 조회 (재귀)
     *
     * @param parentItemId 부모 업무 ID
     * @return 모든 미완료 하위 업무 목록
     */
    List<Item> findAllIncompleteDescendants(@Param("parentItemId") Long parentItemId);

    /**
     * 하위 업무 개수 조회
     *
     * @param parentItemId   부모 업무 ID
     * @param includeDeleted 삭제된 업무 포함 여부
     * @return 하위 업무 개수
     */
    int countChildren(@Param("parentItemId") Long parentItemId,
                      @Param("includeDeleted") Boolean includeDeleted);

    /**
     * 하위 업무 정렬 순서 업데이트
     *
     * @param itemId         업무 ID
     * @param childSortOrder 새 정렬 순서
     * @param updatedBy      수정자 USERNAME
     * @return 영향받은 행 수
     */
    int updateChildSortOrder(@Param("itemId") Long itemId,
                             @Param("childSortOrder") Integer childSortOrder,
                             @Param("updatedBy") String updatedBy);

    /**
     * 부모 내 최대 정렬 순서 조회
     *
     * @param parentItemId 부모 업무 ID
     * @return 최대 정렬 순서
     */
    Integer getMaxChildSortOrder(@Param("parentItemId") Long parentItemId);

    /**
     * 보드별 기본 업무만 조회 (depth=0)
     *
     * @param boardId          보드 ID
     * @param includeCompleted 완료된 업무 포함 여부
     * @param includeDeleted   삭제된 업무 포함 여부
     * @return 기본 업무 목록
     */
    List<Item> findRootItemsByBoardId(@Param("boardId") Long boardId,
                                      @Param("includeCompleted") Boolean includeCompleted,
                                      @Param("includeDeleted") Boolean includeDeleted);

    /**
     * 하위 업무 일괄 삭제 (논리 삭제)
     *
     * @param parentItemId 부모 업무 ID
     * @param deletedBy    삭제자 USERNAME
     * @return 영향받은 행 수
     */
    int softDeleteChildren(@Param("parentItemId") Long parentItemId,
                           @Param("deletedBy") String deletedBy);

    /**
     * 하위 업무 일괄 완료
     *
     * @param parentItemId 부모 업무 ID
     * @param completedBy  완료자 USERNAME
     * @return 영향받은 행 수
     */
    int completeChildren(@Param("parentItemId") Long parentItemId,
                         @Param("completedBy") String completedBy);

    /**
     * 하위 업무 존재 여부 확인
     *
     * @param itemId 업무 ID
     * @return 하위 업무 존재 여부
     */
    boolean hasChildren(@Param("itemId") Long itemId);

    /**
     * 업무 깊이 조회
     *
     * @param itemId 업무 ID
     * @return 업무 깊이
     */
    Integer getItemDepth(@Param("itemId") Long itemId);

    /**
     * 하위 업무 여부 확인
     *
     * @param itemId 업무 ID
     * @return 하위 업무 여부
     */
    boolean isChildItem(@Param("itemId") Long itemId);
}
