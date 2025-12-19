package com.taskflow.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 댓글 도메인
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    /**
     * 댓글 ID (PK)
     */
    private Long commentId;

    /**
     * 아이템 ID (FK)
     */
    private Long itemId;

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
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 ID
     */
    private Long updatedBy;

    // =============================================
    // 조인 필드
    // =============================================

    /**
     * 아이템 제목
     */
    private String itemTitle;

    /**
     * 작성자 이름
     */
    private String createdByName;

    /**
     * 수정자 이름
     */
    private String updatedByName;
}
