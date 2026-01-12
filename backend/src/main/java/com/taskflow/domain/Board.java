package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 보드(컬렉션) 엔티티
 *
 * 테이블: TB_BOARD
 *
 * USERNAME 기반 FK 참조 시스템:
 * - OWNER_USERNAME: 소유자 USERNAME 참조
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board {

    /**
     * 보드 ID (PK)
     */
    private Long boardId;

    /**
     * 보드명
     */
    private String boardName;

    /**
     * 보드 설명
     */
    private String description;

    /**
     * 소유자 USERNAME (FK → TB_USER.USERNAME)
     */
    private String ownerUsername;

    /**
     * 기본 뷰 타입 (TABLE, KANBAN, LIST)
     */
    private String defaultView;

    /**
     * 표시 색상 (#RRGGBB)
     */
    private String color;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 사용 여부 (Y/N)
     */
    private String useYn;

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

    /**
     * 삭제일시 (논리삭제)
     */
    private LocalDateTime deletedAt;

    /**
     * 삭제자 USERNAME
     */
    private String deletedBy;

    // =============================================
    // 추가 필드 (Mapper에서 설정)
    // =============================================

    /**
     * 소유자명 (조인)
     */
    private String ownerName;

    /**
     * 아이템 수
     */
    private Integer itemCount;

    /**
     * 공유 사용자 수
     */
    private Integer shareCount;

    // =============================================
    // 편의 메서드
    // =============================================

    /**
     * 활성 보드 여부
     */
    public boolean isActive() {
        return "Y".equals(useYn);
    }

    /**
     * 소유자 여부 확인 (USERNAME 기준)
     */
    public boolean isOwner(String username) {
        return ownerUsername != null && ownerUsername.equals(username);
    }
}
