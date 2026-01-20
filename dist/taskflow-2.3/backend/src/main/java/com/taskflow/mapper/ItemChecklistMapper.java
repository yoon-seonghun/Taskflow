package com.taskflow.mapper;

import com.taskflow.domain.ItemChecklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 업무 내 체크리스트 Mapper
 */
@Mapper
public interface ItemChecklistMapper {

    /**
     * 체크리스트 조회
     */
    ItemChecklist selectById(@Param("checklistId") Long checklistId);

    /**
     * 업무별 체크리스트 목록 조회
     */
    List<ItemChecklist> selectByItemId(@Param("itemId") Long itemId);

    /**
     * 업무별 체크리스트 수 조회
     */
    int countByItemId(@Param("itemId") Long itemId);

    /**
     * 업무별 완료된 체크리스트 수 조회
     */
    int countCompletedByItemId(@Param("itemId") Long itemId);

    /**
     * 담당자별 체크리스트 목록 조회
     */
    List<ItemChecklist> selectByAssigneeUsername(@Param("assigneeUsername") String assigneeUsername);

    /**
     * 체크리스트 생성
     */
    int insert(ItemChecklist checklist);

    /**
     * 체크리스트 수정
     */
    int update(ItemChecklist checklist);

    /**
     * 체크리스트 완료 상태 변경
     */
    int updateCompleted(
            @Param("checklistId") Long checklistId,
            @Param("isCompleted") Boolean isCompleted,
            @Param("completedBy") String completedBy
    );

    /**
     * 체크리스트 정렬 순서 업데이트
     */
    int updateSortOrder(
            @Param("checklistId") Long checklistId,
            @Param("sortOrder") Integer sortOrder,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 체크리스트 삭제
     */
    int deleteById(@Param("checklistId") Long checklistId);

    /**
     * 업무의 모든 체크리스트 삭제
     */
    int deleteByItemId(@Param("itemId") Long itemId);

    /**
     * 체크리스트 순서 일괄 업데이트
     */
    int batchUpdateSortOrder(
            @Param("checklistIds") List<Long> checklistIds,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 다음 정렬 순서 조회
     */
    Integer getNextSortOrder(@Param("itemId") Long itemId);
}
