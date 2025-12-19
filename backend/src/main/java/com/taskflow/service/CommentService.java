package com.taskflow.service;

import com.taskflow.dto.comment.CommentCreateRequest;
import com.taskflow.dto.comment.CommentResponse;
import com.taskflow.dto.comment.CommentUpdateRequest;

import java.util.List;

/**
 * 댓글 서비스 인터페이스
 */
public interface CommentService {

    // =============================================
    // 댓글 조회
    // =============================================

    /**
     * 댓글 ID로 조회
     *
     * @param commentId 댓글 ID
     * @return 댓글 응답
     */
    CommentResponse getComment(Long commentId);

    /**
     * 아이템별 댓글 목록 조회
     *
     * @param itemId 아이템 ID
     * @return 댓글 목록
     */
    List<CommentResponse> getCommentsByItemId(Long itemId);

    /**
     * 아이템별 댓글 수 조회
     *
     * @param itemId 아이템 ID
     * @return 댓글 수
     */
    int getCommentCount(Long itemId);

    // =============================================
    // 댓글 등록/수정/삭제
    // =============================================

    /**
     * 댓글 등록
     *
     * @param itemId    아이템 ID
     * @param request   등록 요청
     * @param createdBy 생성자 ID
     * @return 생성된 댓글 응답
     */
    CommentResponse createComment(Long itemId, CommentCreateRequest request, Long createdBy);

    /**
     * 댓글 수정
     *
     * @param commentId 댓글 ID
     * @param request   수정 요청
     * @param updatedBy 수정자 ID
     * @return 수정된 댓글 응답
     */
    CommentResponse updateComment(Long commentId, CommentUpdateRequest request, Long updatedBy);

    /**
     * 댓글 삭제
     *
     * @param commentId 댓글 ID
     * @param deletedBy 삭제자 ID
     */
    void deleteComment(Long commentId, Long deletedBy);
}
