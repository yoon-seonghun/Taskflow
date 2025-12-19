package com.taskflow.service.impl;

import com.taskflow.domain.Comment;
import com.taskflow.domain.Item;
import com.taskflow.dto.comment.CommentCreateRequest;
import com.taskflow.dto.comment.CommentResponse;
import com.taskflow.dto.comment.CommentUpdateRequest;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.CommentMapper;
import com.taskflow.mapper.ItemMapper;
import com.taskflow.service.CommentService;
import com.taskflow.sse.SseEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 댓글 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final ItemMapper itemMapper;
    private final SseEventPublisher sseEventPublisher;

    // =============================================
    // 댓글 조회
    // =============================================

    @Override
    public CommentResponse getComment(Long commentId) {
        Comment comment = commentMapper.findById(commentId)
                .orElseThrow(() -> BusinessException.commentNotFound(commentId));

        return CommentResponse.from(comment);
    }

    @Override
    public List<CommentResponse> getCommentsByItemId(Long itemId) {
        // 아이템 존재 확인
        itemMapper.findById(itemId)
                .orElseThrow(() -> BusinessException.itemNotFound(itemId));

        List<Comment> comments = commentMapper.findByItemId(itemId);
        return CommentResponse.fromList(comments);
    }

    @Override
    public int getCommentCount(Long itemId) {
        return commentMapper.countByItemId(itemId);
    }

    // =============================================
    // 댓글 등록/수정/삭제
    // =============================================

    @Override
    @Transactional
    public CommentResponse createComment(Long itemId, CommentCreateRequest request, Long createdBy) {
        log.info("Creating comment: itemId={}", itemId);

        // 아이템 존재 확인
        Item item = itemMapper.findById(itemId)
                .orElseThrow(() -> BusinessException.itemNotFound(itemId));

        // 댓글 엔티티 생성
        Comment comment = Comment.builder()
                .itemId(itemId)
                .content(request.getContent())
                .createdBy(createdBy)
                .build();

        // 댓글 저장
        commentMapper.insert(comment);
        log.info("Comment created: id={}", comment.getCommentId());

        CommentResponse response = getComment(comment.getCommentId());

        // SSE 이벤트 발행
        sseEventPublisher.publishCommentCreated(item.getBoardId(), response, createdBy);

        return response;
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, Long updatedBy) {
        log.info("Updating comment: id={}", commentId);

        // 댓글 존재 확인
        Comment comment = commentMapper.findById(commentId)
                .orElseThrow(() -> BusinessException.commentNotFound(commentId));

        // 작성자 확인 (본인만 수정 가능)
        if (!comment.getCreatedBy().equals(updatedBy)) {
            throw BusinessException.forbidden("본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        // 댓글 수정
        comment.setContent(request.getContent());
        comment.setUpdatedBy(updatedBy);

        commentMapper.update(comment);
        log.info("Comment updated: id={}", commentId);

        return getComment(commentId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long deletedBy) {
        log.info("Deleting comment: id={}", commentId);

        // 댓글 존재 확인
        Comment comment = commentMapper.findById(commentId)
                .orElseThrow(() -> BusinessException.commentNotFound(commentId));

        // 작성자 확인 (본인만 삭제 가능)
        if (!comment.getCreatedBy().equals(deletedBy)) {
            throw BusinessException.forbidden("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        commentMapper.delete(commentId);
        log.info("Comment deleted: id={}", commentId);
    }
}
