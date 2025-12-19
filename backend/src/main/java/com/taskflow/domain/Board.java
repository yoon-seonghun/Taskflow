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
 * 업무 아이템들을 담는 컬렉션(보드)
 * - 소유자가 보드를 생성하고 관리
 * - 다른 사용자에게 공유 가능
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
     * 소유자 ID (FK)
     */
    private Long ownerId;

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
    // 추가 필드 (Mapper에서 설정)
    // =============================================

    /**
     * 소유자명
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
     * 소유자 여부 확인
     */
    public boolean isOwner(Long userId) {
        return ownerId != null && ownerId.equals(userId);
    }
}
