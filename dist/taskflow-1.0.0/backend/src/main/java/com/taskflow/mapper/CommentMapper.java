package com.taskflow.mapper;

import com.taskflow.domain.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 댓글 Mapper
 */
@Mapper
public interface CommentMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 댓글 ID로 조회
     *
     * @param commentId 댓글 ID
     * @return 댓글
     */
    Optional<Comment> findById(@Param("commentId") Long commentId);

    /**
     * 아이템별 댓글 목록 조회
     *
     * @param itemId 아이템 ID
     * @return 댓글 목록
     */
    List<Comment> findByItemId(@Param("itemId") Long itemId);

    /**
     * 아이템별 댓글 수 조회
     *
     * @param itemId 아이템 ID
     * @return 댓글 수
     */
    int countByItemId(@Param("itemId") Long itemId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 댓글 등록
     *
     * @param comment 댓글
     */
    void insert(Comment comment);

    /**
     * 댓글 수정
     *
     * @param comment 댓글
     */
    void update(Comment comment);

    /**
     * 댓글 삭제
     *
     * @param commentId 댓글 ID
     */
    void delete(@Param("commentId") Long commentId);

    /**
     * 아이템의 모든 댓글 삭제
     *
     * @param itemId 아이템 ID
     */
    void deleteByItemId(@Param("itemId") Long itemId);
}
