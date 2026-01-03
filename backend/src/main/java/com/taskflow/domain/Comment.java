package com.taskflow.domain;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 댓글 도메인
 *
 * 테이블: TB_COMMENT
 *
 * USERNAME 기반 FK 참조 시스템:
 * - CREATED_BY, UPDATED_BY: USERNAME 참조
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
     * 생성자 USERNAME
     */
    private String createdBy;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 USERNAME
     */
    private String updatedBy;

    // =============================================
    // 조인 필드
    // =============================================

    /**
     * 아이템 제목
     */
    private String itemTitle;

    /**
     * 작성자 USER_ID (조인, SSE 이벤트용)
     */
    private Long createdByUserId;

    /**
     * 작성자 이름 (조인)
     */
    private String createdByName;

    /**
     * 수정자 USER_ID (조인)
     */
    private Long updatedByUserId;

    /**
     * 수정자 이름 (조인)
     */
    private String updatedByName;
}
