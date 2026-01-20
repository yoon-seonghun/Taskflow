package com.taskflow.mapper;

import com.taskflow.domain.TodoShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Todo 공유 Mapper
 */
@Mapper
public interface TodoShareMapper {

    /**
     * Todo 공유 조회
     */
    TodoShare selectById(@Param("shareId") Long shareId);

    /**
     * Todo별 공유 목록 조회
     */
    List<TodoShare> selectByTodoId(@Param("todoId") Long todoId);

    /**
     * 사용자가 공유받은 Todo 목록 조회
     */
    List<TodoShare> selectBySharedUserId(@Param("sharedUserId") Long sharedUserId);

    /**
     * 특정 Todo-사용자 공유 조회
     */
    TodoShare selectByTodoIdAndUserId(
            @Param("todoId") Long todoId,
            @Param("sharedUserId") Long sharedUserId
    );

    /**
     * 공유 추가
     */
    int insert(TodoShare todoShare);

    /**
     * 권한 변경
     */
    int updatePermission(
            @Param("shareId") Long shareId,
            @Param("permission") String permission
    );

    /**
     * 공유 삭제
     */
    int deleteById(@Param("shareId") Long shareId);

    /**
     * Todo의 모든 공유 삭제
     */
    int deleteByTodoId(@Param("todoId") Long todoId);

    /**
     * 특정 Todo-사용자 공유 삭제
     */
    int deleteByTodoIdAndUserId(
            @Param("todoId") Long todoId,
            @Param("sharedUserId") Long sharedUserId
    );

    /**
     * 사용자 관련 공유 삭제 (공유받은 것)
     */
    int deleteBySharedUserId(@Param("sharedUserId") Long sharedUserId);

    /**
     * 중복 체크
     */
    boolean existsByTodoIdAndUserId(
            @Param("todoId") Long todoId,
            @Param("sharedUserId") Long sharedUserId
    );

    /**
     * 사용자의 Todo 접근 권한 확인
     */
    String getPermission(
            @Param("todoId") Long todoId,
            @Param("sharedUserId") Long sharedUserId
    );

    /**
     * 공유 수 조회
     */
    int countByTodoId(@Param("todoId") Long todoId);
}
