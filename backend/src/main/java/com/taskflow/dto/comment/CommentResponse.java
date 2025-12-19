package com.taskflow.dto.comment;

import com.taskflow.domain.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 댓글 응답 DTO
 */
@Getter
@Builder
public class CommentResponse {

    /**
     * 댓글 ID
     */
    private Long commentId;

    /**
     * 아이템 ID
     */
    private Long itemId;

    /**
     * 아이템 제목
     */
    private String itemTitle;

    /**
     * 댓글 내용
     */
    private String content;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 ID
     */
    private Long createdBy;

    /**
     * 작성자 이름
     */
    private String createdByName;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 ID
     */
    private Long updatedBy;

    /**
     * 수정자 이름
     */
    private String updatedByName;

    /**
     * 수정 여부
     */
    private boolean edited;

    /**
     * Comment 엔티티를 CommentResponse로 변환
     */
    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .itemId(comment.getItemId())
                .itemTitle(comment.getItemTitle())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .createdBy(comment.getCreatedBy())
                .createdByName(comment.getCreatedByName())
                .updatedAt(comment.getUpdatedAt())
                .updatedBy(comment.getUpdatedBy())
                .updatedByName(comment.getUpdatedByName())
                .edited(comment.getUpdatedAt() != null)
                .build();
    }

    /**
     * Comment 엔티티 리스트를 CommentResponse 리스트로 변환
     */
    public static List<CommentResponse> fromList(List<Comment> comments) {
        return comments.stream()
                .map(CommentResponse::from)
                .collect(Collectors.toList());
    }
}
