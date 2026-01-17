package com.taskflow.mapper;

import com.taskflow.domain.Todo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 개인 Todo Mapper
 */
@Mapper
public interface TodoMapper {

    /**
     * Todo 조회
     */
    Todo selectById(@Param("todoId") Long todoId);

    /**
     * 사용자의 Todo 목록 조회
     */
    List<Todo> selectByOwnerUserId(
            @Param("ownerUserId") Long ownerUserId,
            @Param("includeCompleted") Boolean includeCompleted,
            @Param("dueDateFrom") LocalDate dueDateFrom,
            @Param("dueDateTo") LocalDate dueDateTo
    );

    /**
     * 사용자의 완료된 Todo 목록 조회
     */
    List<Todo> selectCompletedByOwnerUserId(
            @Param("ownerUserId") Long ownerUserId,
            @Param("completedDateFrom") LocalDate completedDateFrom,
            @Param("completedDateTo") LocalDate completedDateTo
    );

    /**
     * 사용자의 오늘 마감 Todo 목록 조회
     */
    List<Todo> selectTodayByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    /**
     * 사용자의 지연 Todo 목록 조회
     */
    List<Todo> selectOverdueByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    /**
     * 사용자의 이관받은 Todo 목록 조회
     */
    List<Todo> selectTransferredByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    /**
     * 반복 Todo의 하위 목록 조회
     */
    List<Todo> selectByParentTodoId(@Param("parentTodoId") Long parentTodoId);

    /**
     * 사용자의 Todo 수 조회
     */
    int countByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    /**
     * 사용자의 활성 Todo 수 조회
     */
    int countActiveByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    /**
     * Todo 생성
     */
    int insert(Todo todo);

    /**
     * Todo 수정
     */
    int update(Todo todo);

    /**
     * Todo 완료 처리
     */
    int updateCompleted(
            @Param("todoId") Long todoId,
            @Param("isCompleted") Boolean isCompleted,
            @Param("updatedBy") String updatedBy
    );

    /**
     * Todo 이관 처리
     */
    int updateTransfer(
            @Param("todoId") Long todoId,
            @Param("newOwnerUserId") Long newOwnerUserId,
            @Param("transferredFromUserId") Long transferredFromUserId,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 사용자의 Todo 일괄 이관
     */
    int updateTransferByOwnerUserId(
            @Param("fromUserId") Long fromUserId,
            @Param("toUserId") Long toUserId,
            @Param("updatedBy") String updatedBy
    );

    /**
     * Todo 정렬 순서 업데이트
     */
    int updateSortOrder(
            @Param("todoId") Long todoId,
            @Param("sortOrder") Integer sortOrder,
            @Param("updatedBy") String updatedBy
    );

    /**
     * Todo 삭제 (논리적)
     */
    int updateUseYn(
            @Param("todoId") Long todoId,
            @Param("useYn") String useYn,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 사용자의 Todo 일괄 삭제 (논리적)
     */
    int updateUseYnByOwnerUserId(
            @Param("ownerUserId") Long ownerUserId,
            @Param("useYn") String useYn,
            @Param("updatedBy") String updatedBy
    );

    /**
     * Todo 물리적 삭제
     */
    int deleteById(@Param("todoId") Long todoId);

    /**
     * 다음 반복 마감일 업데이트
     */
    int updateNextDueDate(
            @Param("todoId") Long todoId,
            @Param("nextDueDate") LocalDate nextDueDate,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 반복 처리 대상 Todo 조회
     */
    List<Todo> selectForRepeatProcessing(@Param("targetDate") LocalDate targetDate);
}
