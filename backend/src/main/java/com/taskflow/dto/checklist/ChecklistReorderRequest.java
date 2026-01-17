package com.taskflow.dto.checklist;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 체크리스트 순서 변경 요청 DTO
 */
@Getter
@Setter
public class ChecklistReorderRequest {

    /**
     * 체크리스트 ID 순서 목록
     */
    @NotEmpty(message = "체크리스트 ID 목록은 필수입니다")
    private List<Long> checklistIds;
}
