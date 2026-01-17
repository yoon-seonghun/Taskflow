package com.taskflow.dto.checklist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 체크리스트 생성 요청 DTO
 */
@Getter
@Setter
public class ChecklistCreateRequest {

    /**
     * 체크리스트 항목
     */
    @NotBlank(message = "체크리스트 항목은 필수입니다")
    @Size(max = 500, message = "체크리스트 항목은 500자 이내여야 합니다")
    private String title;

    /**
     * 마감일
     */
    private LocalDate dueDate;

    /**
     * 담당자 USERNAME
     */
    private String assigneeUsername;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;
}
