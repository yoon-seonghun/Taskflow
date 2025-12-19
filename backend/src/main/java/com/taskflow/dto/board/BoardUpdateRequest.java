package com.taskflow.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 보드 수정 요청 DTO
 */
@Getter
@Setter
public class BoardUpdateRequest {

    /**
     * 보드명
     */
    @NotBlank(message = "보드명은 필수입니다")
    @Size(max = 100, message = "보드명은 100자 이내여야 합니다")
    private String boardName;

    /**
     * 보드 설명
     */
    @Size(max = 500, message = "보드 설명은 500자 이내여야 합니다")
    private String description;

    /**
     * 기본 뷰 타입 (TABLE, KANBAN, LIST)
     */
    @Pattern(regexp = "^(TABLE|KANBAN|LIST)$", message = "뷰 타입은 TABLE, KANBAN, LIST 중 하나여야 합니다")
    private String defaultView;

    /**
     * 표시 색상 (#RRGGBB)
     */
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "색상 형식이 올바르지 않습니다 (예: #FF0000)")
    private String color;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 사용 여부 (Y/N)
     */
    @Pattern(regexp = "^[YN]$", message = "사용 여부는 Y 또는 N이어야 합니다")
    private String useYn;
}
